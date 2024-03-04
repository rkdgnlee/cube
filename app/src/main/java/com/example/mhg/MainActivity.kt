package com.example.mhg

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.mhg.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        // 화면 초기화
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.flMain, HomeFragment())
                commit()
            }
        }

        binding.bnbMain
//        binding.bnbMain.setOnNavigationItemReselectedListener(null)
        binding.bnbMain.setOnItemSelectedListener {
            when(it.itemId) {
                // ---- fragment 경로 지정 시작 ----
                R.id.home -> setCurrentFragment(HomeFragment())
                R.id.report -> setCurrentFragment(ReportFragment())
                R.id.pick -> setCurrentFragment(PickFragment())
                R.id.profile -> setCurrentFragment(ProfileFragment())
            }
            true
        }
        binding.bnbMain.setOnItemReselectedListener {
            when(it.itemId) {
                // ---- fragment 경로 지정 시작 ----
                R.id.home -> {}
                R.id.report -> {}
                R.id.pick -> {}
                R.id.profile -> {}
            }
        }
        // ---- fragment 경로 지정  끝----
        binding.imgbtnAlarm.setOnClickListener {
            val intent = Intent(this, AlarmActivity::class.java)
            startActivity(intent)
        }

    }
    fun setCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            add(R.id.flMain, fragment)
            addToBackStack(null)
            commit()
        }


    private val onBackPressedCallback = object:OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {

        }
    }


}