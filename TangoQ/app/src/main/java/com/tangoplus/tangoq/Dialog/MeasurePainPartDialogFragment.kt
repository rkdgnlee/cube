package com.tangoplus.tangoq.Dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.Adapter.PainPartRVAdpater
import com.tangoplus.tangoq.Listener.OnPartCheckListener
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.ViewModel.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasurePainPartDialogBinding


class MeasurePainPartDialogFragment : DialogFragment(), OnPartCheckListener {
    lateinit var binding : FragmentMeasurePainPartDialogBinding
    val viewModel : MeasureViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasurePainPartDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ppList = mutableListOf<Pair<String, String>>()
        ppList.add(Pair("drawable_pain1", "손목"))
        ppList.add(Pair("drawable_pain2", "척추"))
        ppList.add(Pair("drawable_pain3", "팔꿉"))
        ppList.add(Pair("drawable_pain4", "목"))
        ppList.add(Pair("drawable_pain5", "발목"))
        ppList.add(Pair("drawable_pain6", "어깨"))
        ppList.add(Pair("drawable_pain7", "무릎"))
        ppList.add(Pair("drawable_pain8", "복부"))

        val adapter = PainPartRVAdpater(ppList, this@MeasurePainPartDialogFragment)
        binding.rvPp.adapter = adapter
        val linearLayoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvPp.layoutManager = linearLayoutManager

        // ------! RV checkbox 취합 시작 !------
        binding.btnPpSet.setOnClickListener {
            viewModel.parts.value // TODO 통증 부위 선택 후 RecyclerView 번갈아가면서 넣기vh
            dismiss()
        }
    }
    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    override fun onPartCheck(part: String, checked: Boolean) {
        if (checked) {
            viewModel.parts.value?.add(part)
        } else {
            viewModel.parts.value?.remove(part)
        }
    }
}