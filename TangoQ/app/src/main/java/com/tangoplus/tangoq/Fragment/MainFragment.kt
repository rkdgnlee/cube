package com.tangoplus.tangoq.Fragment

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.Adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.Adapter.SpinnerAdapter
import com.tangoplus.tangoq.Object.NetworkExerciseService.fetchExerciseJson
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentMainBinding
import kotlinx.coroutines.launch


class MainFragment : Fragment() {
    lateinit var binding: FragmentMainBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // -----! 스크롤 관리 !-----
        binding.nsvM.isNestedScrollingEnabled = false
        binding.rvMain.isNestedScrollingEnabled = false
        binding.rvMain.overScrollMode = 0
        // -----! spinner 연결 시작 !-----
        val filterList = arrayListOf<String>()
        filterList.add("최신순")
        filterList.add("인기순")
        filterList.add("추천순")
        binding.spnMFilter.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, filterList)
        binding.spnMFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                binding.spnMFilter.getItemAtPosition(position).toString()
                // TODO 필터로 recyclerview 순서 변경
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val timeList = arrayListOf<String>()
        timeList.add("10분 내")
        timeList.add("20분")
        timeList.add("30분")
        timeList.add("40분")
        timeList.add("50분")
        timeList.add("60분")
        binding.spnMTime.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, timeList)
        binding.spnMTime.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                binding.spnMTime.getItemAtPosition(position).toString()
                // TODO 필터로 recyclerview 순서 변경
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val purposeList = arrayListOf<String>()
        purposeList.add("근력 증가")
        purposeList.add("체력 증가")
        purposeList.add("자세 교정")
        purposeList.add("재활 운동")
        binding.spnMPurpose.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, purposeList)
        binding.spnMPurpose.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                binding.spnMPurpose.getItemAtPosition(position).toString()
                // TODO 필터로 recyclerview 순서 변경
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val partList = arrayListOf<String>()
        partList.add("상체")
        partList.add("하체")
        partList.add("관절")
        partList.add("전신")
        binding.spnMPart.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, partList)
        binding.spnMPart.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                binding.spnMPart.getItemAtPosition(position).toString()
                // TODO 필터로 recyclerview 순서 변경
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        // -----! spinner 연결 끝 !-----


        // -----! 하단 RV Adapter 시작 !-----
        lifecycleScope.launch {
            // -----! db에서 받아서 뿌려주기 시작 !-----
            val verticalDataList = fetchExerciseJson(getString(R.string.IP_ADDRESS_t_Exercise_Description)).toMutableList()
            try {
                val adapter = ExerciseRVAdapter(verticalDataList,"main" )
                adapter.exerciseList = verticalDataList
                binding.rvMain.adapter = adapter
                val linearLayoutManager =
                    LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                binding.rvMain.layoutManager = linearLayoutManager
                // -----! vertical 어댑터 끝 !-----
            } catch (e: Exception) {
                Log.e(ContentValues.TAG, "Error storing exercises", e)
            }
        }
        // -----! 하단 RV Adapter 끝 !-----

    }
}