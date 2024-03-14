package com.example.mhg

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.ActivitySignInBinding
import org.json.JSONObject

class SignInActivity : AppCompatActivity() {
    lateinit var binding : ActivitySignInBinding
    val viewModel: UserViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViewPager()

        // -----! google login 했을 시 페이지 지정 시작 !-----
        val userString = intent.getStringExtra("user")
        val user = userString?.let { JSONObject(it) }

        viewModel.User.value = user // json으로 일단 넣음
        if (viewModel.User.value != null) {
            binding.vp2SignIn.setCurrentItem(3)
        }

        // -----! google login 했을 시 페이지 지정 끝 !-----

        // -----! 페이지 변경 callback 메소드 시작 !-----
        binding.vp2SignIn.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            @SuppressLint("SetTextI18n")
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    4 -> {
                        binding.tvSignInNext.text = ""
                    }
                    else -> {
                        binding.tvSignInNext.text = "다음"
                    }
                }

//                // ---- view model에 값을 넣기 (0p ~ 3p) 시작 ----
//                val previousPosition = position - 1
//                if (previousPosition >= 0) {
//                    val fragment = supportFragmentManager.findFragmentByTag("f$previousPosition")
//
//                    // ---- view model에 값을 넣기 (0p ~ 3p) 끝 ----
//                }
//                binding.vp2SignIn.currentItem = binding.vp2SignIn.currentItem + 1
            }
        })
        binding.tvSignInPrevious.setOnSingleClickListener {
            binding.vp2SignIn.currentItem = binding.vp2SignIn.currentItem - 1
            if (binding.vp2SignIn.currentItem == 0) {

            }
        }
        binding.tvSignInNext.setOnSingleClickListener {
            binding.vp2SignIn.currentItem = binding.vp2SignIn.currentItem + 1
        }
        // -----! 페이지 변경 callback 메소드 끝 !-----
    }
    private fun initViewPager() {
        val viewPager = binding.vp2SignIn
        viewPager.isUserInputEnabled = false
        viewPager.adapter = SignInViewPagerAdapter(this)
        binding.diSignIn.attachTo(viewPager)
//       viewPager.isUserInputEnabled = false
//        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
//            private var lastPosition = 0
//            override fun onPageSelected(position: Int) {
//                super.onPageSelected(position)
//                binding.diSignIn.isInvisible = (position == 2)
//            }
//        })
    }
}

class SignInViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    private val fragments = listOf(SignIn1Fragment(), SignIn2Fragment(), SignIn3Fragment(), SignIn4Fragment())
    override fun getItemCount(): Int {
        return fragments.size
    }
    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}

private fun View.setOnSingleClickListener(action: (v: View) -> Unit) {
    val listener = View.OnClickListener { action(it) }
    setOnClickListener(OnSingleClickListener(listener))
}
