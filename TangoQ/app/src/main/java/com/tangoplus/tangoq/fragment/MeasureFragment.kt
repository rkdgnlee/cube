package com.tangoplus.tangoq.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.adapter.etc.MeasureVPAdapter
import com.tangoplus.tangoq.databinding.FragmentMeasureBinding
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.dialog.QRCodeDialogFragment

class MeasureFragment : Fragment() {
    lateinit var binding : FragmentMeasureBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasureBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vpMs.adapter = MeasureVPAdapter(childFragmentManager, lifecycle)
        binding.vpMs.isUserInputEnabled = false
        binding.vpMs.offscreenPageLimit = 1
        binding.ibtnMsAlarm.setOnClickListener {
            val dialog = AlarmDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "AlarmDialogFragment")
        }

        binding.ibtnMsQRCode.setOnClickListener {
            val dialog = QRCodeDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "LoginScanDialogFragment")
        }

        binding.tlMs.addOnTabSelectedListener(object : OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> binding.vpMs.currentItem = 0
                    1 -> binding.vpMs.currentItem = 1
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // ------! 측정 버튼 시작 !------
        binding.fabtnMs.setOnClickListener {
            (activity as? MainActivity)?.launchMeasureSkeletonActivity()
        } // ------! 측정 버튼 끝 !------

        binding.vpMs.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.tlMs.selectTab(binding.tlMs.getTabAt(position))
            }
        })
    }
}