package com.example.mhg

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.ActivityMainBinding
import com.example.mhg.`object`.Singleton_t_user

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding


    @SuppressLint("CommitTransaction")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        val t_userData = Singleton_t_user.getInstance(this)


        viewModel.User.value = t_userData.jsonObject
        Log.e("싱글톤>뷰모델", "${viewModel.User.value}")



//        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        // -----! 화면 초기화 !-----
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.flMain, HomeFragment())
                commit()
            }
        }

        // -----! 알람 경로 설정 !-----
        val fromAlarmActivity = intent.getBooleanExtra("fromAlarmActivity", false)
        Log.v("TAG", "$fromAlarmActivity")
        if (fromAlarmActivity) {
            val fragmentId = intent.getStringExtra("fragmentId")
            Log.v("TAG", "$fragmentId")
            if (fragmentId != null) {
                val fragment = FragmentFactory.createFragmentById(fragmentId)
                Log.v("TAG", "$fragment")
                when (fragmentId) {
//                    fragment에서 bnb 색 변하게 하기
                    "home_beginner", "home_expert", "home_intermediate" -> {
                        val homeFragment = HomeFragment.newInstance(fragmentId)
                        binding.bnbMain.selectedItemId = R.id.home
                        setCurrentFragment(homeFragment)

                    }
                    "report_skeleton", "report_detail", "report_goal" -> {
                        val reportFragment = ReportFragment.newInstance(fragmentId)
                        binding.bnbMain.selectedItemId = R.id.report
                        setCurrentFragment(reportFragment)
                    }
                    "pick" -> {
                        binding.bnbMain.selectedItemId = R.id.pick
                        setCurrentFragment(fragment)
                    }
                    "profile" -> {
                        binding.bnbMain.selectedItemId = R.id.profile
                        setCurrentFragment(fragment)
                    }
                }
            }
        }


        // -----! 바텀 네비 바 경로 설정 -----!
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
            replace(R.id.flMain, fragment)
//            addToBackStack(null)
            commit()
        }



    private val onBackPressedCallback = object:OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {

        }
    }
}