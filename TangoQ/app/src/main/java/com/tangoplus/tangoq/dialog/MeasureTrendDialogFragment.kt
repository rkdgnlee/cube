package com.tangoplus.tangoq.dialog

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.MeasureVPAdapter
import com.tangoplus.tangoq.adapter.TrendVPAdapter
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureTrendDialogBinding
import com.tangoplus.tangoq.`object`.Singleton_t_measure

class MeasureTrendDialogFragment : DialogFragment() {
    lateinit var binding : FragmentMeasureTrendDialogBinding
    private lateinit var trend1 : String
    private lateinit var trend2 : String
    private val mvm : MeasureViewModel by activityViewModels()
    private lateinit var measures : MutableList<MeasureVO>
    companion object{
        private const val ARG_COMPARE1 = "arg_compare1"
        private const val ARG_COMPARE2 = "arg_compare2"
        fun newInstance(trend1: String, trend2: String) : MeasureTrendDialogFragment {
            val fragment = MeasureTrendDialogFragment()
            val args = Bundle()
            args.putString(ARG_COMPARE1, trend1)
            args.putString(ARG_COMPARE2, trend2)
            fragment.arguments = args
            return fragment
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasureTrendDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        trend1 = arguments?.getString(ARG_COMPARE1) ?: ""
        trend2 = arguments?. getString(ARG_COMPARE2) ?: ""
        measures = Singleton_t_measure.getInstance(requireContext()).measures ?: mutableListOf()
        try {
            if (measures.isNotEmpty()) {
                val trendOne = measures.find { it.regDate == trend1 }
                if (trendOne != null) {
                    mvm.trends.add(trendOne)
                }
                val trendTwo = measures.find { it.regDate == trend2 }
                if (trendTwo != null) {
                    mvm.trends.add(trendTwo)
                }
            }
            Log.v("MVM>Trend", "$mvm")
        } catch (e: IllegalArgumentException) {
            Log.e("TrendError", "${e.printStackTrace()}")
        }

        binding.tlMTD.getTabAt(0)?.text = trend1
        binding.tlMTD.getTabAt(1)?.text = trend2

        // ------# tabLayout 연결 #------
        binding.vpMTD.adapter = TrendVPAdapter(childFragmentManager, lifecycle)
        binding.vpMTD.isUserInputEnabled = false
        binding.vpMTD.offscreenPageLimit = 1
        binding.tlMTD.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> binding.vpMTD.currentItem = 0
                    1 -> binding.vpMTD.currentItem = 1
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // ------#8080
    }
}