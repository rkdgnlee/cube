package com.tangoplus.tangoq.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.adapter.RecommendationRVAdapter
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentProgramSelectBinding
import com.tangoplus.tangoq.dialog.MeasureBSDialogFragment
import com.tangoplus.tangoq.`object`.Singleton_t_measure

class ProgramSelectFragment : Fragment() {
   lateinit var binding : FragmentProgramSelectBinding
    private val vm : MeasureViewModel by activityViewModels()
    private var singletonMeasure : MutableList<MeasureVO>? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProgramSelectBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        singletonMeasure = Singleton_t_measure.getInstance(requireContext()).measures

        vm.selectedMeasureDate.observe(viewLifecycleOwner) { selectedDate ->
            val dateIndex = singletonMeasure?.indexOf(singletonMeasure?.find { it.regDate == selectedDate }!!)
            Log.v("dataIndex", "현재날짜 싱글턴에서 index: ${dateIndex}, vm: ${vm.selectedMeasure?.regDate}")
            binding.tvPSMeasureDate.text = singletonMeasure?.get(dateIndex!!)?.regDate?.substring(0, 10)
            if (dateIndex != null) {
                setAdapter(dateIndex)
            }

        }
        binding.tvPSMeasureDate.setOnClickListener {
            val dialog = MeasureBSDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "MeasureBSDialogFragment")
        }
    }

    private fun setAdapter(dateIndex: Int) {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvPSD.layoutManager = layoutManager
        val adapter = RecommendationRVAdapter(this@ProgramSelectFragment, singletonMeasure?.get(dateIndex)?.recommendations!!)
        Log.v("recommendations", "${singletonMeasure?.get(dateIndex)?.recommendations!!}")
        binding.rvPSD.adapter = adapter
    }
}