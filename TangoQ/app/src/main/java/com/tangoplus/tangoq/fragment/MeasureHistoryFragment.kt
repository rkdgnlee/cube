package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.OptIn
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.MeasureHistoryRVAdapter
import com.tangoplus.tangoq.vo.MeasureVO
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureHistoryBinding
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.db.Singleton_t_measure
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MeasureHistoryFragment : Fragment() {
    lateinit var binding : FragmentMeasureHistoryBinding

    private var measures : MutableList<MeasureVO>? = null
    private val viewModel : MeasureViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasureHistoryBinding.inflate(inflater)
        return binding.root
    }

    @OptIn(ExperimentalBadgeUtils::class)
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ibtnMHAlarm.setOnSingleClickListener {
            val dialog = AlarmDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "AlarmDialogFragment")
        }
        binding.ibtnMABack.setOnSingleClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
                replace(R.id.flMain, MeasureFragment())
                commit()
            }
        }

        val filterList = mutableListOf<String>()
        filterList.add("최신순")
        filterList.add("오래된순")
        filterList.add("높은 점수순")
        filterList.add("낮은 점수순")

        measures = Singleton_t_measure.getInstance(requireContext()).measures
        measures?.let {
            setAdapter(measures)
            binding.tvMHCount.text = "총 측정건: ${measures?.size}건"

            // actv연결
            val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, filterList)
            binding.actvMH.setAdapter(adapter)
            binding.actvMH.setOnItemClickListener { _, _, position, _ ->
                binding.actvMH.setText(filterList[position], false)
                measures = when (position) {
                    0 -> measures?.sortedByDescending { LocalDate.parse(it.regDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}?.toMutableList()
                    1 -> measures?.sortedBy { LocalDate.parse(it.regDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}?.toMutableList()
                    2 -> measures?.sortedByDescending { it.overall?.toInt() }?.toMutableList()
                    3 -> measures?.sortedBy { it.overall?.toInt()}?.toMutableList()
                    else -> measures
                }
//                Log.v("정렬", "position: $position, ${measures?.map { it.overall }}")
                setAdapter(measures)
            }
        }

    }

    private fun setAdapter(measures: MutableList<MeasureVO>?) {
        val adapter = MeasureHistoryRVAdapter(this@MeasureHistoryFragment, measures ?: mutableListOf(), viewModel)
        binding.rvMH.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvMH.layoutManager = linearLayoutManager
    }
}