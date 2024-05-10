package com.tangoplus.tangoq.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.adapter.PainPartRVAdpater
import com.tangoplus.tangoq.listener.OnPartCheckListener
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasurePartDialogBinding

class MeasurePartDialogFragment : DialogFragment(), OnPartCheckListener {
    lateinit var binding : FragmentMeasurePartDialogBinding
    val viewModel : MeasureViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasurePartDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ppList = mutableListOf<Triple<String, String, Boolean>>()
        ppList.add(Triple("drawable_pain1", "손목", false))
        ppList.add(Triple("drawable_pain2", "척추", false))
        ppList.add(Triple("drawable_pain3", "팔꿉", false))
        ppList.add(Triple("drawable_pain4", "목", false))
        ppList.add(Triple("drawable_pain5", "발목", false))
        ppList.add(Triple("drawable_pain6", "어깨", false))
        ppList.add(Triple("drawable_pain7", "무릎", false))
        ppList.add(Triple("drawable_pain8", "복부", false))

        for ( i in ppList.indices) {
            val matchingPart = viewModel.parts.value?.find { it.second == ppList[i].second }
            if (matchingPart != null) {
                ppList[i] = ppList[i].copy(third = matchingPart.third)
            }
        }
        val adapter = PainPartRVAdpater(ppList, "selectPp",this@MeasurePartDialogFragment)
        binding.rvMP.adapter = adapter
        val linearLayoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvMP.layoutManager = linearLayoutManager

        binding.btnMPSet.setOnClickListener {
//            viewModel.parts.value // TODO 통증 부위 선택 후 번갈아가면서 넣기vh
            dismiss()
            Log.v("VM>part", "${viewModel.parts.value}")
        } // ------! RV checkbox 취합 끝 !------
        binding.ibtnMPBack.setOnClickListener { dismiss() }
    }

    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
    override fun onPartCheck(part: Triple<String, String, Boolean>) {
        if (part.third) {
            viewModel.addPart(part)
            Log.v("viewModel.part", "${viewModel.parts.value}")
        } else {
            viewModel.deletePart(part)
        }
    }
}