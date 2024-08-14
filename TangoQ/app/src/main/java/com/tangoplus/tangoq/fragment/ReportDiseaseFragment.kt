package com.tangoplus.tangoq.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentReportDiseaseBinding


class ReportDiseaseFragment : Fragment() {
    lateinit var binding : FragmentReportDiseaseBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReportDiseaseBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ibtnRD.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                replace(R.id.flMain, MeasureFragment())

                commit()
            }
        }
    }
}