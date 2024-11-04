package com.tangoplus.tangoq.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.tangoplus.tangoq.fragment.MeasureTrend1Fragment
import com.tangoplus.tangoq.fragment.MeasureTrend2Fragment

class TrendVPAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
    private val fragments = listOf(MeasureTrend1Fragment(), MeasureTrend2Fragment())


    override fun getItemCount(): Int {
        return fragments.size
    }


    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}