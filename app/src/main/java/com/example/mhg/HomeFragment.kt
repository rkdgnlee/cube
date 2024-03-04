package com.example.mhg

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout


class HomeFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewPager = view.findViewById<ViewPager2>(R.id.vpHome)
        val adapter = HomePagerAdapter(childFragmentManager, lifecycle)
        viewPager?.adapter = adapter
        viewPager?.isUserInputEnabled = false
        val tablayout = view.findViewById<TabLayout>(R.id.tlHome)

        tablayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab!!.position) {
                    0 -> viewPager.currentItem = 0
                    1 -> viewPager.currentItem = 1
                    2 -> viewPager.currentItem = 2
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                when (tab!!.position) {
                    0 -> viewPager.currentItem = 0
                    1 -> viewPager.currentItem = 1
                    2 -> viewPager.currentItem = 2
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                when (tab!!.position) {
                    0 -> viewPager.currentItem = 0
                    1 -> viewPager.currentItem = 1
                    2 -> viewPager.currentItem = 2
                }
            }
        })

    }
}
class HomePagerAdapter(fragmentManager: FragmentManager, lifecycle:Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
    private val fragments = listOf(HomeBeginnerFragment(), HomeIntermediateFragment(), HomeExpertFragment())
    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}