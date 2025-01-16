package com.tangoplus.tangoq.adapter.etc

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.tangoplus.tangoq.fragment.MeasureDashBoard1Fragment
import com.tangoplus.tangoq.fragment.MeasureDashBoard2Fragment

class MeasureVPAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle){
    private val fragments = listOf(MeasureDashBoard1Fragment(), MeasureDashBoard2Fragment())
    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}