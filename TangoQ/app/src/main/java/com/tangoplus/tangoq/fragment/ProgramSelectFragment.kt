package com.tangoplus.tangoq.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.adapter.RecommendationRVAdapter
import com.tangoplus.tangoq.vo.MeasureVO
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentProgramSelectBinding
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.dialog.bottomsheet.MeasureBSDialogFragment
import com.tangoplus.tangoq.dialog.QRCodeDialogFragment
import com.tangoplus.tangoq.db.Singleton_t_measure

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
            val dateIndex = singletonMeasure?.indexOf(singletonMeasure?.find { it.regDate == selectedDate })
            Log.v("dataIndex", "현재날짜 싱글턴에서 index: ${dateIndex}, vm: ${vm.selectedMeasure?.regDate}")
            binding.tvPSMeasureDate.text = dateIndex?.let { singletonMeasure?.get(it)?.regDate?.substring(0, 10) }
            if (dateIndex != null) {
                setAdapter(dateIndex)
            }

        }
        binding.tvPSMeasureDate.setOnClickListener {
            val dialog = MeasureBSDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "MeasureBSDialogFragment")
        }

        binding.ibtnPSAlarm.setOnClickListener {
            val dialog = AlarmDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "AlarmDialogFragment")
        }

        binding.ibtnPSQRCode.setOnClickListener {
            val dialog = QRCodeDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "LoginScanDialogFragment")
        }
    }
    private val categoryMap = mapOf(
        "목관절" to 1,
        "어깨" to 2,
        "팔꿉" to 3,
        "손목" to 4,
        "몸통전면(복부)" to 5,
        "몸통후면(척추)" to 6,
        "몸통 코어" to 7,
        "엉덩" to 8,
        "고관절" to 8,
        "무릎" to 9,
        "발목" to 10,
    )
    // category의 id값들을 따로 adapter에서 매개변수로 넣어서, drawable id값 매칭.

    private fun setAdapter(dateIndex: Int) {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvPSD.layoutManager = layoutManager

        val categoryNums = singletonMeasure?.get(dateIndex)?.recommendations?.map { categoryMap.entries.find { entry -> it.title.contains(entry.key) }?.value }

//        singletonMeasure?.get(dateIndex)?.recommendations?.mapNotNull { rec ->
//                categoryMap.entries.find { entry -> rec.title.contains(entry.key) }?.value
//            }
        Log.v("categoryNums", "categoryNums: $categoryNums")
        val adapter = categoryNums?.let { RecommendationRVAdapter(this@ProgramSelectFragment, singletonMeasure?.get(dateIndex)?.recommendations ?: mutableListOf(), it ) }
        Log.v("recommendations", "${singletonMeasure?.get(dateIndex)?.recommendations}")
        binding.rvPSD.adapter = adapter
    }
}