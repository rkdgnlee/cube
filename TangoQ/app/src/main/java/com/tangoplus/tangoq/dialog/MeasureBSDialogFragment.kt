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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.StringRVAdapter
import com.tangoplus.tangoq.data.ExerciseViewModel
import com.tangoplus.tangoq.data.HistoryViewModel
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureBSDialogBinding
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import org.json.JSONObject

class MeasureBSDialogFragment : BottomSheetDialogFragment() {
    lateinit var binding: FragmentMeasureBSDialogBinding
    lateinit var singletonMeasure : Singleton_t_measure
    val viewModel: ExerciseViewModel by activityViewModels()
    val mvm : MeasureViewModel by activityViewModels()

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

        val dates = measures?.let { measure ->
            List(measure.size) { i ->
                measure.get(i).regDate
            }
        } ?: emptyList()

        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvMBSD.layoutManager = layoutManager
        val adapter = StringRVAdapter(this@MeasureBSDialogFragment, dates.toMutableList(), "checkbox",  mvm)
        binding.rvMBSD.adapter = adapter

        setupButtons()
    }

    private fun setupButtons() {
        binding.ibtnMBSDExit.setOnClickListener { dismiss() }
        binding.btnMBSD.setOnClickListener {
            mvm.selectedMeasureDate.value = mvm.selectMeasureDate.value
            mvm.selectedMeasure = singletonMeasure.measures?.find { it.regDate == mvm.selectMeasureDate.value }
            Log.w("selectedMeasureDate", "${mvm.selectedMeasureDate.value}")
            dismiss()
        }
    }
}