package com.example.mhg

import android.os.Bundle
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

        val userString = intent.getStringExtra("user")
        val user = userString?.let { JSONObject(it) }

        viewModel.User.value = user // json으로 일단 넣음
        if (viewModel.User.value != null) {
            binding.vp2SignIn.setCurrentItem(3)
        }




    }
    private fun initViewPager() {
        val viewPager = binding.vp2SignIn
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