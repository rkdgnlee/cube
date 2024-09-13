package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.annotation.OptIn
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.tangoplus.tangoq.MeasureSkeletonActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.MeasureHistoryRVAdapter
import com.tangoplus.tangoq.adapter.SpinnerAdapter
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureHistoryBinding
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import org.json.JSONObject
import java.time.LocalDate
import kotlin.random.Random

class MeasureHistoryFragment : Fragment() {
    lateinit var binding : FragmentMeasureHistoryBinding

    private var singletonMeasure : MutableList<MeasureVO>? = null
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


        binding.ibtnMHAlarm.setOnClickListener {
            val dialog = AlarmDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "AlarmDialogFragment")
        }

        singletonMeasure = Singleton_t_measure.getInstance(requireContext()).measures
        setAdpater(singletonMeasure!!)
        binding.tvMHCount.text = "총 측정건: ${singletonMeasure!!.size}건"

        // -----! spinner 연결 시작 !-----
        val filterList = arrayListOf<String>()
        filterList.add("최신순")
        filterList.add("인기순")
        filterList.add("추천순")
        binding.spnrMH.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, filterList)
        binding.spnrMH.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                when (position) {
                    0 -> {
                        singletonMeasure!!.sortedBy { it.regDate }
                    }
                    1 -> {
                    }
                    2 -> {
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // ------! spinner 연결 끝 !------

        binding.fabtnMH.setOnClickListener{
            val intent = Intent(requireContext(), MeasureSkeletonActivity::class.java)
            startActivity(intent)
        }

    }

    private fun setAdpater(measures: MutableList<MeasureVO>) {
        val adapter = MeasureHistoryRVAdapter(this@MeasureHistoryFragment, measures, viewModel)
        binding.rvMH.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvMH.layoutManager = linearLayoutManager
    }
}