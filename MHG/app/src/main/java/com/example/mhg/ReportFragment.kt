package com.example.mhg

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.get
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.mhg.databinding.FragmentReportBinding
import com.google.android.material.tabs.TabLayout


class ReportFragment : Fragment() {
    lateinit var binding: FragmentReportBinding
    companion object {
        fun newInstance(fragmentId: String) : ReportFragment {
            val fragment = ReportFragment()
            val args = Bundle()
            args.putString("fragmentId", fragmentId)
            fragment.arguments = args
            return fragment
        }
    }
    fun setChildFragment(fragmentId: String) {
        when (fragmentId) {
            "report_skeleton" -> {
                binding.vpReport.currentItem = 0
                binding.tlReport.selectTab(binding.tlReport.getTabAt(0))
            }
            "report_detail" -> {
                binding.vpReport.currentItem = 1
                binding.tlReport.getTabAt(1)
                binding.tlReport.selectTab(binding.tlReport.getTabAt(1))
            }
            else -> {
                binding.vpReport.currentItem = 2
                binding.tlReport.getTabAt(2)
                binding.tlReport.selectTab(binding.tlReport.getTabAt(2))
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentReportBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vpReport.adapter = ReportPagerAdapter(childFragmentManager, lifecycle)
        binding.vpReport.isUserInputEnabled = false
        // -----! 알림의 route를 통해 자식 fragment로 이동  !-----
        val fragmentId = arguments?.getString("fragmentId")
        fragmentId?.let { setChildFragment(it) }


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