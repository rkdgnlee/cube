package com.example.mhg

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentHomeBinding
import com.google.android.material.tabs.TabLayout


class HomeFragment : Fragment() {

    lateinit var binding : FragmentHomeBinding

    companion object {
        fun newInstance(fragmentId: String) : HomeFragment {
            val fragment = HomeFragment()
            val args = Bundle()
            args.putString("fragmentId", fragmentId)
            fragment.arguments = args
            return fragment
        }
    }
    fun setChildFragment(fragmentId: String) {
        when (fragmentId) {
            "home_beginner" -> {
                binding.vpHome.currentItem = 0
                binding.tlHome.selectTab(binding.tlHome.getTabAt(0))
            }
            "home_intermediate" -> {
                binding.vpHome.currentItem = 1
                binding.tlHome.selectTab(binding.tlHome.getTabAt(1))
            }
            else -> {
                binding.vpHome.currentItem = 2
                binding.tlHome.selectTab(binding.tlHome.getTabAt(2))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(layoutInflater)



        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vpHome.adapter = HomePagerAdapter(childFragmentManager, lifecycle)
        binding.vpHome.isUserInputEnabled = false

        // -----! 알림의 route를 통해 자식 fragment로 이동  !-----
        val fragmentId = arguments?.getString("fragmentId")
        fragmentId?.let { setChildFragment(it) }

        binding.tlHome.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab!!.position) {
                    0 -> binding.vpHome.currentItem = 0
                    1 -> binding.vpHome.currentItem = 1
                    2 -> binding.vpHome.currentItem = 2
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                when (tab!!.position) {
                    0 -> binding.vpHome.currentItem = 0
                    1 -> binding.vpHome.currentItem = 1
                    2 -> binding.vpHome.currentItem = 2
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                when (tab!!.position) {
                    0 -> binding.vpHome.currentItem = 0
                    1 -> binding.vpHome.currentItem = 1
                    2 -> binding.vpHome.currentItem = 2
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