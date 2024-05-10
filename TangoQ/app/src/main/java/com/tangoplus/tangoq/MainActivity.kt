package com.tangoplus.tangoq

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import com.tangoplus.tangoq.fragment.ExerciseFragment
import com.tangoplus.tangoq.fragment.FavoriteFragment
import com.tangoplus.tangoq.fragment.MainFragment
import com.tangoplus.tangoq.fragment.MeasureFragment
import com.tangoplus.tangoq.fragment.ProfileFragment
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    val MeasureViewModel : MeasureViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ------! 다크모드 메뉴 이름 설정 시작 !------
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isNightMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES
        binding.tvCurrentPage.text = (if (isNightMode) "내정보" else "내정보")
        // ------! 다크모드 메뉴 이름 설정 끝 !------


        // -----! 초기 화면 설정 !-----
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.flMain, MainFragment())
                binding.tvCurrentPage.text = "메인"
                commit()
            }
        }
        binding.bnbMain.itemIconTintList = null
        binding.bnbMain.isItemActiveIndicatorEnabled = false
        binding.bnbMain.setOnItemSelectedListener {


            when(it.itemId) {
                // ---- fragment 경로 지정 시작 ----
                R.id.main -> {
                    setCurrentFragment(MainFragment())
                    binding.tvCurrentPage.text = "메인"
                    setOptiLayout(binding.flMain,  binding.main ,binding.cvCl)
                }
                R.id.exercise -> {
                    setCurrentFragment(ExerciseFragment())
                    binding.tvCurrentPage.text = "운동"
                    setOptiLayout(binding.flMain,  binding.main ,binding.cvCl)
                }
                R.id.measure -> {
                    setCurrentFragment(MeasureFragment())
                    binding.tvCurrentPage.text = "측정"
                    setOptiLayout(binding.flMain,  binding.main, binding.cvCl)
                }
                R.id.favorite -> {
                    setCurrentFragment(FavoriteFragment())
                    binding.tvCurrentPage.text = ""
                    setFullLayout(binding.flMain, binding.main)
                }
                R.id.profile -> {
                    setCurrentFragment(ProfileFragment())
                    binding.tvCurrentPage.text = "내정보"
                    setOptiLayout(binding.flMain,  binding.main, binding.cvCl)
                }
            }
            true
        }
        binding.bnbMain.setOnItemReselectedListener {
            when(it.itemId) {
                // ---- fragment 경로 지정 시작 ----
                R.id.main -> {}
                R.id.exercise -> {}
                R.id.measure -> {}
                R.id.favorite -> {}
                R.id.profile -> {}
            }
        }

        binding.ibtnAlarm.setOnClickListener {
            val intent = Intent(this@MainActivity, AlarmActivity::class.java)
            startActivity(intent)
        }
    }
    fun setCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flMain, fragment)
//            addToBackStack(null)
            commit()
        }

    fun setFullLayout(frame: FrameLayout, const: ConstraintLayout) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(const)
        constraintSet.connect(frame.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        constraintSet.applyTo(const)
        binding.cvCl.visibility = View.GONE

    }
    fun setOptiLayout(frame: FrameLayout, const: ConstraintLayout, cardView: CardView) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(const)
        constraintSet.connect(frame.id, ConstraintSet.TOP, cardView.id, ConstraintSet.BOTTOM, 0)
        constraintSet.applyTo(const)
        binding.cvCl.visibility = View.VISIBLE

    }

    private val onBackPressedCallback = object: OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {

        }
    }
}

