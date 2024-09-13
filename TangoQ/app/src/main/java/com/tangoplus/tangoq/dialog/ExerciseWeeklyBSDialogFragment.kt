package com.tangoplus.tangoq.dialog

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.ExerciseViewModel
import com.tangoplus.tangoq.data.HistoryViewModel
import com.tangoplus.tangoq.databinding.FragmentExerciseWeeklyBSDialogBinding


class ExerciseWeeklyBSDialogFragment : BottomSheetDialogFragment() {
    lateinit var binding : FragmentExerciseWeeklyBSDialogBinding
    val viewModel : ExerciseViewModel by activityViewModels()
    val hViewModel: HistoryViewModel by activityViewModels()

    private val checkboxes by lazy {
        listOf(binding.cbEWBSD1, binding.cbEWBSD2, binding.cbEWBSD3, binding.cbEWBSD4)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExerciseWeeklyBSDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvEWBSD.setText(hViewModel.currentProgram?.programName)
        setCheckboxes()
        setupButtons()
        hViewModel.selectedWeek.observe(viewLifecycleOwner) { setCheckbox(it) }
    }

    private fun setCheckboxes() {
        checkboxes.forEachIndexed {index, checkbox ->
            checkbox.setOnClickListener{ setCheckbox(index) }
            setupCheckboxTextColor(checkbox)
        }

        // 초기 - 현재 주차

        setCheckbox(hViewModel.currentWeek)
        Log.v("현재주차", "${hViewModel.currentWeek}")
    }
    private fun setupButtons() {
        binding.ibtnEWBSDExit.setOnClickListener {
            hViewModel.selectWeek.value = hViewModel.currentWeek
            dismiss()
        }


        binding.btnEWBSD.setOnClickListener {
            hViewModel.selectedWeek.value = hViewModel.selectWeek.value
            dismiss()

            // TODO if문의 2가 현재의 회차로 변경되야 함
            hViewModel.selectedEpisode.value = if (hViewModel.selectedWeek.value == hViewModel.currentWeek) 2 else 0
            Log.v("주차및회차초기화", "selectedWeek: ${hViewModel.selectedWeek.value}, currentWeek: ${hViewModel.currentWeek}, selectedEpisode: ${hViewModel.selectedEpisode.value}")
        }
    }

    private fun setCheckbox(currentNum : Int) {
        hViewModel.selectWeek.value = currentNum
        checkboxes.forEachIndexed{ index, checkbox ->
            checkbox.isChecked = index == currentNum
            updateCheckboxTextColor(checkbox)
        }
        Log.v("현재주차setCheckbox", "selectWeek: ${hViewModel.selectWeek.value}, currentWeek : ${hViewModel.currentWeek}")
    }

    private fun setupCheckboxTextColor(checkbox:CheckBox) {
        checkbox.setOnCheckedChangeListener{ _, isChecked ->
            updateCheckboxTextColor(checkbox, isChecked)
        }
    }

    private fun updateCheckboxTextColor(checkbox: CheckBox, isChecked: Boolean = checkbox.isChecked) {
        val colorResId = if (isChecked) R.color.mainColor else R.color.subColor800
        checkbox.setTextColor(ContextCompat.getColor(requireContext(), colorResId))
    }
}