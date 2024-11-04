package com.tangoplus.tangoq.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tangoplus.tangoq.adapter.StringRVAdapter
import com.tangoplus.tangoq.data.ProgressViewModel
import com.tangoplus.tangoq.databinding.FragmentProgramWeekBSDialogBinding

class ProgramWeekBSDialogFragment : BottomSheetDialogFragment() {
    lateinit var binding : FragmentProgramWeekBSDialogBinding
    val pvm : ProgressViewModel by activityViewModels()
    var totalWeek = 0
//    override fun onResume() {
//        super.onResume()
//        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
//        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
//    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProgramWeekBSDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        totalWeek = pvm.currentProgram?.programWeek!!
        val weeks = mutableListOf<String>()
        for (i in 0 until totalWeek) {
            val txt = "${i+1}주차"
            weeks.add(txt)
        }

        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvWBSD.layoutManager = layoutManager
        val adapter = StringRVAdapter(this@ProgramWeekBSDialogFragment, weeks, "week",  pvm)
        binding.rvWBSD.adapter = adapter


        binding.btnWBSD.setOnClickListener {
            pvm.selectedWeek.value = pvm.selectWeek.value
            if (pvm.currentWeek != pvm.selectedWeek.value) {
                pvm.selectedSequence.value = 0
                pvm.currentSequence = 0
            } else {

            }
            dismiss()
        }
    }
}