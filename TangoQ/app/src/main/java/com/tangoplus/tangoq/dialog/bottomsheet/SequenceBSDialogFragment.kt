package com.tangoplus.tangoq.dialog.bottomsheet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.StringRVAdapter
import com.tangoplus.tangoq.databinding.FragmentSequenceBSDialogBinding
import com.tangoplus.tangoq.viewmodel.AnalysisViewModel

class SequenceBSDialogFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentSequenceBSDialogBinding
    private val avm : AnalysisViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSequenceBSDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAdapter()
        binding.ibtnSBSDExit.setOnClickListener { dismiss() }
        binding.btnSBSD.setOnClickListener {
            avm.currentPart.value = avm.selectPart.value
            dismiss()
        }

        // 태블릿 가로모드일 때 바텀시트가 안나오는 현상 고치기.
        val bottomSheet = view.parent as View
        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun setAdapter() {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        val adapter = StringRVAdapter(this@SequenceBSDialogFragment, avm.currentParts?.toMutableList(), null, "seq",avm)
        binding.rvSBSD.layoutManager = layoutManager
        binding.rvSBSD.adapter = adapter
        avm.currentParts?.indexOf(avm.currentPart.value)?.let { binding.rvSBSD.scrollToPosition(it - 1) }
    }
}