package com.tangoplus.tangoq.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tangoplus.tangoq.adapter.StringRVAdapter
import com.tangoplus.tangoq.data.ExerciseViewModel
import com.tangoplus.tangoq.data.HistoryViewModel
import com.tangoplus.tangoq.databinding.FragmentProgramWeeklyBSDialogBinding


class ProgramWeeklyBSDialogFragment : BottomSheetDialogFragment() {
    lateinit var binding : FragmentProgramWeeklyBSDialogBinding
    val viewModel : ExerciseViewModel by activityViewModels()
    val hViewModel: HistoryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProgramWeeklyBSDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvEWBSD.setText(hViewModel.currentProgram?.programName)
//        setCheckboxes()
        setupButtons()

        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvEWBSD.layoutManager = layoutManager

        val weeks = (0 until hViewModel.currentProgram?.programWeek!!).map { week ->
            "$week"
        }.toMutableList()
        val adapter = StringRVAdapter(this@ProgramWeeklyBSDialogFragment, weeks, "checkbox", "program", hViewModel)
        binding.rvEWBSD.adapter = adapter
//        hViewModel.selectedWeek.observe(viewLifecycleOwner) { setCheckbox(it) }
    }

//    private fun setCheckboxes() {
//        checkboxes.forEachIndexed {index, checkbox ->
//            checkbox.setOnClickListener{ setCheckbox(index) }
//            setupCheckboxTextColor(checkbox)
//        }
//
//        // 초기 - 현재 주차
//
//        setCheckbox(hViewModel.currentWeek)
//        Log.v("현재주차", "${hViewModel.currentWeek}")
//    }
    private fun setupButtons() {
        binding.ibtnEWBSDExit.setOnClickListener {
            hViewModel.selectWeek.value = hViewModel.currentWeek
            dismiss()
        }


        binding.btnEWBSD.setOnClickListener {
            hViewModel.selectedWeek.value = hViewModel.selectWeek.value
            dismiss()
            hViewModel.selectedEpisode.value = if (hViewModel.selectedWeek.value == hViewModel.currentWeek) 2 else 0
        }
    }

//    private fun setCheckbox(currentNum : Int) {
//        hViewModel.selectWeek.value = currentNum
//        checkboxes.forEachIndexed{ index, checkbox ->
//            checkbox.isChecked = index == currentNum
//            updateCheckboxTextColor(checkbox)
//        }
//        Log.v("현재주차setCheckbox", "selectWeek: ${hViewModel.selectWeek.value}, currentWeek : ${hViewModel.currentWeek}")
//    }




}