package com.tangoplus.tangoq.fragment

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.tangoplus.tangoq.MeasureSkeletonActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.MeasureVPAdapter
import com.tangoplus.tangoq.databinding.FragmentMeasureBinding
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.dialog.LoginScanDialogFragment
import com.tangoplus.tangoq.dialog.SetupDialogFragment

class MeasureFragment : Fragment() {
    lateinit var binding : FragmentMeasureBinding
    private var pendingTabSelection: Boolean = false

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
            val dialog = LoginScanDialogFragment()
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
            val intent = Intent(requireContext(), MeasureSkeletonActivity::class.java)
            startActivity(intent)
        } // ------! 측정 버튼 끝 !------

        binding.vpMs.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.tlMs.selectTab(binding.tlMs.getTabAt(position))
            }
        })
    }

    // ------# main에서 dashboard2로 옮겨가기
    fun selectDashBoard2() {
        binding.tlMs.getTabAt(1)?.select()
        binding.vpMs.setCurrentItem(1, false)
        Log.v("db2", "select: ${binding.vpMs.currentItem} ")
    }
}