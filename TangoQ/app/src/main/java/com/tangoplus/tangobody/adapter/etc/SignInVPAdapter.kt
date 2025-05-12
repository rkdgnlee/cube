package com.tangoplus.tangobody.adapter.etc

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.tangoplus.tangobody.fragment.SignIn1Fragment
import com.tangoplus.tangobody.fragment.SignIn2Fragment

class SignInVPAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    private val fragments = listOf(SignIn1Fragment(), SignIn2Fragment())
    override fun getItemCount(): Int {
        return fragments.size
    }
    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}
