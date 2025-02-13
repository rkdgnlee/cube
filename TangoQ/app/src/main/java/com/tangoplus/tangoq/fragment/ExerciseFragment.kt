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
                val categoryArrayList = mutableListOf<ArrayList<Int>>()
                categoryArrayList.add(arrayListOf(1, 2)) // 기본 밸런스, 기본 스트레칭
                categoryArrayList.add(arrayListOf(3, 4, 5)) // 기본 하지 근육 강화운동 , 기본 스트레칭 의자활용 운동, 기본 유산소 운동
                categoryArrayList.add(arrayListOf(6, 7, 8, 9)) // 상지 하지  근육 스트레칭 운동, 상지 하지  근육 강화 운동
                categoryArrayList.add(arrayListOf(10, 11)) // 근골격계질환 개선 위한 강화운동, 근골격계질환 개선 위한 스트레칭 운동
//            categoryArrayList.add(arrayListOf(12)) // 큐브
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

}