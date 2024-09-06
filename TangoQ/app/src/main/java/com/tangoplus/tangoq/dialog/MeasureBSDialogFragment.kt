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
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.ExerciseViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureBSDialogBinding
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import org.json.JSONObject

class MeasureBSDialogFragment : BottomSheetDialogFragment() {
    lateinit var binding: FragmentMeasureBSDialogBinding
    lateinit var singletonMeasure : Singleton_t_measure
    val viewModel: ExerciseViewModel by activityViewModels()
    private val checkboxes by lazy {
        listOf(binding.cbMBSD1, binding.cbMBSD2, binding.cbMBSD3, binding.cbMBSD4)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasureBSDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        singletonMeasure = Singleton_t_measure.getInstance(requireContext())
        val measures = singletonMeasure.measures
        val dates = measures?.let { array ->
            List(array.size) { i ->
                array.get(i).regDate
            }
        } ?: emptyList()

        checkboxes.forEachIndexed{ index, checkBox ->
            if (index < dates.size) {
                checkBox.apply {
                    visibility = View.VISIBLE
                    text = dates[index]
                }
            } else {
                checkBox.visibility = View.GONE
            }
        }



        setCheckboxes()
        setupButtons()
        viewModel.selectedMeasureDate.observe(viewLifecycleOwner) { setCheckbox(it) }
    }
    private fun setCheckboxes() {
        checkboxes.forEachIndexed {index, checkbox ->
            checkbox.setOnClickListener{ setCheckbox(index) }
            setupCheckboxTextColor(checkbox)
        }

        // 초기 - 현재 주차

        setCheckbox(viewModel.currentMeasureDate)
        Log.v("현재index", "currentMeasureDate: ${viewModel.currentMeasureDate} selectedMeasureDate: ${viewModel.selectedMeasureDate.value} selectMeasureDate: ${viewModel.selectMeasureDate.value}")
    }

    private fun setupButtons() {
        binding.ibtnMBSDExit.setOnClickListener { dismiss() }
        binding.btnMBSD.setOnClickListener {
            viewModel.selectedMeasureDate.value = viewModel.selectMeasureDate.value
            Log.w("selectedMeasureDate", "${viewModel.selectedMeasureDate.value}")
            dismiss()
        }
    }

    private fun setCheckbox(currentNum : Int) {
        viewModel.selectMeasureDate.value = currentNum
        checkboxes.forEachIndexed{ index, checkbox ->
            checkbox.isChecked = index == currentNum
            updateCheckboxTextColor(checkbox)
        }
        Log.v("현재주차setCheckbox", "selectWeek: ${viewModel.selectWeek.value}, currentWeek : ${viewModel.currentWeek}")
    }

    private fun setupCheckboxTextColor(checkbox: CheckBox) {
        checkbox.setOnCheckedChangeListener{ _, isChecked ->
            updateCheckboxTextColor(checkbox, isChecked)
        }
    }

    private fun updateCheckboxTextColor(checkbox: CheckBox, isChecked: Boolean = checkbox.isChecked) {
        val colorResId = if (isChecked) R.color.mainColor else R.color.subColor800
        checkbox.setTextColor(ContextCompat.getColor(requireContext(), colorResId))
    }
}