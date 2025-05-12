package com.tangoplus.tangobody.dialog.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tangoplus.tangobody.adapter.StringRVAdapter
import com.tangoplus.tangobody.viewmodel.ProgressViewModel
import com.tangoplus.tangobody.databinding.FragmentProgramWeekBSDialogBinding
import com.tangoplus.tangobody.fragment.ExtendedFunctions.setOnSingleClickListener

class ProgramWeekBSDialogFragment : BottomSheetDialogFragment() {
    lateinit var binding : FragmentProgramWeekBSDialogBinding
    private val pvm : ProgressViewModel by activityViewModels()
    private var totalWeek = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProgramWeekBSDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        totalWeek = pvm.currentProgram?.programWeek ?: 0
        val weeks = mutableListOf<String>()
        for (i in 0 until totalWeek) {
            val txt = "${i+1}주차"
            weeks.add(txt)
        }

        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvWBSD.layoutManager = layoutManager
        val adapter = StringRVAdapter(this@ProgramWeekBSDialogFragment, weeks, null, "week",  pvm)
        binding.rvWBSD.adapter = adapter

        binding.ibtnWBSDExit.setOnSingleClickListener { dismiss() }

        binding.btnWBSD.setOnSingleClickListener {
            pvm.selectedWeek.value = pvm.selectWeek.value
            if (pvm.currentWeek != pvm.selectedWeek.value) {
                pvm.selectedSequence.value = 0
                pvm.currentSequence = 0
            }
            dismiss()
        }

        val bottomSheet = view.parent as View
        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }
}