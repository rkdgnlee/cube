package com.tangoplus.tangoq.dialog

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.StringIntIntRVAdapter
import com.tangoplus.tangoq.adapter.StringRVAdapter
import com.tangoplus.tangoq.data.HistoryVO
import com.tangoplus.tangoq.data.HistoryViewModel
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentProgramSelectDialogBinding

class ProgramSelectDialogFragment : DialogFragment() {
   lateinit var binding : FragmentProgramSelectDialogBinding
    private val vm : MeasureViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProgramSelectDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // ------# vm.currentPrograms #-------
        // TODO 실제 매칭 로직 구현


        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvPSD.layoutManager = layoutManager
        val adapter = StringIntIntRVAdapter(this@ProgramSelectDialogFragment, vm.selectedMeasure?.matchPrograms!!)
        binding.rvPSD.adapter = adapter

    }

}