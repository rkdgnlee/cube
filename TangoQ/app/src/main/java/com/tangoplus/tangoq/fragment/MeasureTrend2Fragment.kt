package com.tangoplus.tangoq.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureTrend2Binding


class MeasureTrend2Fragment : Fragment() {
    private lateinit var binding : FragmentMeasureTrend2Binding
    private val mvm : MeasureViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasureTrend2Binding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            if (mvm.trends.isNotEmpty()) {
                mvm.trends[1]
            }



        } catch (e: IllegalStateException) {
            Log.e("오류 수정하기", "${e.printStackTrace()}")
        }

    }

}