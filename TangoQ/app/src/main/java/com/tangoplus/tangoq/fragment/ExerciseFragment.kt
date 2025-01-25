package com.tangoplus.tangoq.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.ExerciseCategoryRVAdapter
import com.tangoplus.tangoq.viewmodel.ExerciseViewModel
import com.tangoplus.tangoq.databinding.FragmentExerciseBinding
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.dialog.ExerciseSearchDialogFragment
import com.tangoplus.tangoq.dialog.QRCodeDialogFragment
import com.tangoplus.tangoq.listener.OnCategoryClickListener
import com.tangoplus.tangoq.api.NetworkExercise.fetchExerciseJson
import com.tangoplus.tangoq.api.NetworkProgress.getLatestProgress
import com.tangoplus.tangoq.dialog.ProgramCustomDialogFragment
import com.tangoplus.tangoq.function.MeasurementManager.findCurrentIndex
import com.tangoplus.tangoq.listener.OnSingleClickListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ExerciseFragment : Fragment(), OnCategoryClickListener {
    lateinit var binding : FragmentExerciseBinding
    private val evm : ExerciseViewModel by activityViewModels()

    // ------! 블루투스 변수 !------

    companion object {
        private const val ARG_SN = "SN"
        fun newInstance(sn : Int): ExerciseFragment {
            val fragment = ExerciseFragment()
            val args = Bundle()
            args.putInt(ARG_SN, sn)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExerciseBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.nsvE.isNestedScrollingEnabled = false
        binding.rvEMainCategory.isNestedScrollingEnabled = false
        binding.rvEMainCategory.overScrollMode = 0
        val sn = arguments?.getInt(ARG_SN) ?: -1


        CoroutineScope(Dispatchers.Main).launch {
            try { // ------! rv vertical 시작 !------
                // 상단 최근 한 운동 cardView 보이기
                if (!evm.latestUVP.isNullOrEmpty() || evm.latestProgram != null) {
                    val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_fade_in)
                    binding.clEProgress1.animation = animation
                    val progressResult = getLatestProgress(getString(R.string.API_progress), requireContext())
                    evm.latestUVP = progressResult?.first?.sortedBy { it.uvpSn }?.toMutableList()
                    evm.latestProgram = progressResult?.second

                    val currentIndex = findCurrentIndex(evm.latestUVP)
                    val currentExerciseItem = evm.latestProgram?.exercises?.get(currentIndex)
                    val second = "${currentExerciseItem?.duration?.toInt()?.div(60)}분 ${currentExerciseItem?.duration?.toInt()?.rem(60)}초"

                    // 받아온 데이터로 cvEProgress 채우기
                    Glide.with(requireContext())
                        .load("${currentExerciseItem?.imageFilePath}")
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .override(180)
                        .into(binding.ivEThumbnail)
                    binding.tvEName.text = currentExerciseItem?.exerciseName
                    binding.tvETime.text = second
                    when (currentExerciseItem?.exerciseStage) {
                        "초급" -> {
                            binding.ivEStage.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_stage_1))
                            binding.tvEStage.text = "초급자"
                        }
                        "중급" -> {
                            binding.ivEStage.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_stage_2))
                            binding.tvEStage.text = "중급자"
                        }
                        "고급" -> {
                            binding.ivEStage.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_stage_3))
                            binding.tvEStage.text = "상급자"
                        }
                    }
                    val evpItem = evm.latestUVP?.find { it.exerciseId == currentExerciseItem?.exerciseId?.toInt() }
                    evpItem.let {
                        if (it != null) {
                            binding.hpvE.progress = (it.lastProgress * 100 / it.videoDuration).toFloat()
                        }
                    }
                } else {
                    binding.clEProgress1.visibility = View.GONE
                }

                binding.clEProgress2.setOnSingleClickListener {
                    val programSn = evm.latestProgram?.programSn ?: -1
                    val recSn = evm.latestUVP?.get(0)?.recommendationSn ?: -1
                    ProgramCustomDialogFragment.newInstance(programSn, recSn)
                        .show(requireActivity().supportFragmentManager, "ProgramCustomDialogFragment")
                }

                // 메인 5개 카테고리 연결
                val categoryArrayList = mutableListOf<ArrayList<Int>>()
                categoryArrayList.add(arrayListOf(1, 2)) // 기본 밸런스, 스트레칭
                categoryArrayList.add(arrayListOf(3, 4, 5)) // 기본 하지근육 강화, 기본 스트레칭 의자 활용, 기본 유산소 운동
                categoryArrayList.add(arrayListOf(6, 7, 8, 9)) // 상지 하지 스트레칭 근육 운동
                categoryArrayList.add(arrayListOf(10, 11)) // 근골격계질환 개선 위한 스트레칭 운동
//            categoryArrayList.add(arrayListOf(12)) // 기본 밸런스, 스트레칭
                val adapter = ExerciseCategoryRVAdapter(categoryArrayList, listOf(),this@ExerciseFragment,  sn, "mainCategory" )
                binding.rvEMainCategory.adapter = adapter
                val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                binding.rvEMainCategory.layoutManager = linearLayoutManager
                // ------! rv vertical 끝 !------

                // ------# exercise 전부 미리 다운받아 VM에 넣기  #------
                if (evm.allExercises.isEmpty()) {
                    evm.allExercises = fetchExerciseJson(getString(R.string.API_exercise)).toMutableList()
                    Log.v("VM>AllExercises", "${evm.allExercises.size}")
                }

            } catch (e: IndexOutOfBoundsException) {
                Log.e("EDetailIndex", "${e.message}")
            } catch (e: IllegalArgumentException) {
                Log.e("EDetailIllegal", "${e.message}")
            } catch (e: IllegalStateException) {
                Log.e("EDetailIllegal", "${e.message}")
            } catch (e: NullPointerException) {
                Log.e("EDetailNull", "${e.message}")
            } catch (e: java.lang.Exception) {
                Log.e("EDetailException", "${e.message}")
            }

            binding.linearLayout7.setOnClickListener{
                val dialog = ExerciseSearchDialogFragment()
                dialog.show(requireActivity().supportFragmentManager, "ExerciseSearchDialogFragment")
            }
        }

        binding.ibtnEAlarm.setOnClickListener {
            val dialog = AlarmDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "AlarmDialogFragment")
        }
        binding.ibtnEQRCode.setOnClickListener{
            val dialog = QRCodeDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "LoginScanDialogFragment")
        }
    }

    override fun onCategoryClick(category: String) { }

    private fun View.setOnSingleClickListener(action: (v: View) -> Unit) {
        val listener = View.OnClickListener { action(it) }
        setOnClickListener(OnSingleClickListener(listener))
    }
}