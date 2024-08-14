package com.tangoplus.tangoq.adapter

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.tangoplus.tangoq.fragment.Setup1Fragment
import com.tangoplus.tangoq.fragment.Setup2Fragment
import com.tangoplus.tangoq.fragment.Setup3Fragment

class SetupVPAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle,val action: String) : FragmentStateAdapter(fragmentManager, lifecycle) {
    private val fragments1 = listOf(Setup1Fragment(), Setup2Fragment(), Setup3Fragment())
    private val fragments2 : List<Fragment> = listOf(Setup2Fragment(), Setup3Fragment())

    override fun getItemCount(): Int {
        return when (action) {
            "startSetup" -> {fragments1.size}
            else -> { fragments2.size }
        }
    }


    override fun createFragment(position: Int): Fragment {
        return when (action) {
            "startSetup" -> { fragments1[position] }
            else -> { fragments2[position] }
        }.apply {
            arguments = Bundle().apply {
                putString("FRAGMENT_TAG", "f$position")
            }
        }
    }
}