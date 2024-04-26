package com.example.mhg

import android.app.UiModeManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.mhg.databinding.ActivityIntroBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class IntroActivity : AppCompatActivity() {
    lateinit var binding: ActivityIntroBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViewPager()



    }
    private fun initViewPager() {
       val viewPager = binding.vp2Intro
//       viewPager.isUserInputEnabled = false
        viewPager.adapter = IntroViewPagerAdapter(this)
        binding.diIntro.attachTo(viewPager)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            private var lastPosition = 0
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (lastPosition ==2 && position ==3) {
                    viewPager.currentItem = lastPosition
                } else {
                    lastPosition = position
                    binding.diIntro.isInvisible = (position == 2)
                }
            }
        })
    }
}

class IntroViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    private val fragments = listOf(Intro1Fragment(),Intro2Fragment(), Intro3Fragment())
    override fun getItemCount(): Int {
        return fragments.size
    }
    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}