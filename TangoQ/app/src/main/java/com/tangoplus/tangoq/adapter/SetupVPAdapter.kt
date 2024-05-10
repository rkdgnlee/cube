package com.tangoplus.tangoq.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.tangoplus.tangoq.fragment.SetupGenderFragment
import com.tangoplus.tangoq.fragment.SetupHeightFragment
import com.tangoplus.tangoq.fragment.SetupPurposeFragment
import com.tangoplus.tangoq.fragment.SetupWeightFragment

class SetupVPAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    private val fragments = listOf(SetupGenderFragment(), SetupHeightFragment(), SetupWeightFragment(), SetupPurposeFragment())
    override fun getItemCount(): Int {
        return fragments.size
    }
    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

}