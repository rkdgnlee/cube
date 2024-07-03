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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.adapter.PainPartRVAdpater
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.listener.OnPartCheckListener
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasurePartDialogBinding
import kotlinx.coroutines.launch

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

        val ppList = mutableListOf<MeasureVO>()
        val parts = listOf("손목", "척추", "팔꿉", "목", "발목", "어깨", "무릎", "복부")
        for (i in parts.indices) {
            val measureVO = MeasureVO(
                partName = parts[i],
                drawableName = "drawable_pain${i+1}",
                select = false,
                anglesNDistances = null
            )
            ppList.add(measureVO)
        }

        for ( i in ppList.indices ) {
            val matchingPart = viewModel.parts.value?.find { it.partName == ppList[i].partName }
            if (matchingPart != null) {
                ppList[i] = ppList[i].copy(select = matchingPart.select)
            }
        }

        val adapter = PainPartRVAdpater(this@MeasurePartDialogFragment, ppList, "selectPp",this@MeasurePartDialogFragment)
        binding.rvMP.adapter = adapter
        val linearLayoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvMP.layoutManager = linearLayoutManager

        binding.btnMPDSet.setOnClickListener {
//            viewModel.parts.value
            dismiss()
            Log.v("VM>part", "${viewModel.parts.value}")
        } // ------! RV checkbox 취합 끝 !------
        binding.ibtnMPDBack.setOnClickListener { dismiss() }

    }

    override fun onResume() {
        super.onResume()

        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
    override fun onPartCheck(part: MeasureVO) {
        if (part.select) {
            viewModel.addPart(part)
            Log.v("viewModel.part", "${viewModel.parts.value}")
        } else {
            viewModel.deletePart(part)
        }
    }
}