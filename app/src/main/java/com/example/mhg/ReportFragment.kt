package com.example.mhg

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.mhg.databinding.FragmentReportBinding
import com.google.android.material.tabs.TabLayout


class ReportFragment : Fragment() {
    lateinit var binding: FragmentReportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentReportBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vpReport.adapter = ReportPagerAdapter(childFragmentManager, lifecycle)
        binding.vpReport.isUserInputEnabled = false

        binding.tlReport.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab!!.position) {
                    0 -> binding.vpReport.currentItem = 0
                    1 -> binding.vpReport.currentItem = 1
                    2 -> binding.vpReport.currentItem = 2
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                when (tab!!.position) {
                    0 -> binding.vpReport.currentItem = 0
                    1 -> binding.vpReport.currentItem = 1
                    2 -> binding.vpReport.currentItem = 2
                }
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {
                when (tab!!.position) {
                    0 -> binding.vpReport.currentItem = 0
                    1 -> binding.vpReport.currentItem = 1
                    2 -> binding.vpReport.currentItem = 2
                }
            }
        })

    }
}

class ReportPagerAdapter(fragmentManager: FragmentManager, lifecycle:Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
    private val fragments = listOf(ReportSkeletonFragment(), ReportDetailFragment(), ReportGoalFragment())
    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}