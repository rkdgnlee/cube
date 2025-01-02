package com.tangoplus.tangoq.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.ExerciseCategoryRVAdapter
import com.tangoplus.tangoq.viewmodel.ExerciseViewModel
import com.tangoplus.tangoq.databinding.FragmentExerciseBinding
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.dialog.ExerciseSearchDialogFragment
import com.tangoplus.tangoq.dialog.QRCodeDialogFragment
import com.tangoplus.tangoq.listener.OnCategoryClickListener
import com.tangoplus.tangoq.api.NetworkExercise.fetchExerciseJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ExerciseFragment : Fragment(), OnCategoryClickListener {
    lateinit var binding : FragmentExerciseBinding
    private val viewModel : ExerciseViewModel by activityViewModels()

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

        binding.ibtnEAlarm.setOnClickListener {
            val dialog = AlarmDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "AlarmDialogFragment")
        }
        binding.ibtnEQRCode.setOnClickListener{
            val dialog = QRCodeDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "LoginScanDialogFragment")
        }
        CoroutineScope(Dispatchers.Main).launch {

            val categoryArrayList = mutableListOf<ArrayList<Int>>()
            categoryArrayList.add(arrayListOf(1, 2)) // 기본 밸런스, 스트레칭
            categoryArrayList.add(arrayListOf(3, 4, 5)) // 기본 하지근육 강화, 기본 스트레칭 의자 활용, 기본 유산소 운동
            categoryArrayList.add(arrayListOf(6, 7, 8, 9)) // 상지 하지 스트레칭 근육 운동
            categoryArrayList.add(arrayListOf(10, 11)) // 근골격계질환 개선 위한 스트레칭 운동
            categoryArrayList.add(arrayListOf(12)) // 기본 밸런스, 스트레칭

            try { // ------! rv vertical 시작 !------
                val adapter = ExerciseCategoryRVAdapter(categoryArrayList, listOf(),this@ExerciseFragment,  sn, "mainCategory" )
                binding.rvEMainCategory.adapter = adapter
                val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                binding.rvEMainCategory.layoutManager = linearLayoutManager
                // ------! rv vertical 끝 !------

                // ------# exercise 전부 미리 다운받아 VM에 넣기  #------
                if (viewModel.allExercises.isEmpty()) {
                    viewModel.allExercises = fetchExerciseJson(getString(R.string.API_exercise)).toMutableList()
                    Log.v("VM>AllExercises", "${viewModel.allExercises.size}")
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
    }

    override fun onCategoryClick(category: String) {

    }
}