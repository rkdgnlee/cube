package com.tangoplus.tangoq.adapter.etc

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.tangoplus.tangoq.fragment.Setup1Fragment
import com.tangoplus.tangoq.fragment.Setup2Fragment
import com.tangoplus.tangoq.fragment.Setup3Fragment

class SetupVPAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
//    private val fragments = listOf(Setup1Fragment(), Setup2Fragment(), Setup3Fragment())
    private val fragments = listOf(Setup1Fragment(), Setup2Fragment())
    override fun getItemCount(): Int {
        return fragments.size
    }
    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}