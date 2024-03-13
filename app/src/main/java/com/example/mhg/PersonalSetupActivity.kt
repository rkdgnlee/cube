package com.example.mhg

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.mhg.VO.UserVO
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.ActivityPersonalSetupBinding

class PersonalSetupActivity : AppCompatActivity() {
    lateinit var binding: ActivityPersonalSetupBinding
    private lateinit var viewModel: UserViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonalSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ---- viewmodel 초기화 및 viewpager2 초기화 ----
//        viewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        initViewPager()
        val Next =


        binding.btnSetupNext.setOnSingleClickListener {
            // ---- 설정 완료 시, 선택한 데이터 저장 및 페이지 이동 코드 시작 ----
            if (binding.btnSetupNext.text == "Finish") {

                // ---- view model에 값을 넣기 (4p) 시작 ----
                val fragment =
                    supportFragmentManager.findFragmentByTag("f${binding.vp2Setup.currentItem}")
                if (fragment is PersonalSetup4Fragment) {
                    if (fragment.binding.rbtnhealth.isChecked) {
//                        viewModel.User.value?.exercisePurpose = "health"
                    } else if (fragment.binding.rbtnDiet.isChecked) {
//                        viewModel.User.value?.exercisePurpose = "diet"
                    } else if (fragment.binding.rbtnRehabil.isChecked) {
//                        viewModel.User.value?.exercisePurpose = "Rehabil"
                    } else {
//                        viewModel.User.value?.exercisePurpose = "strength"
                    }
//                    Log.d("다섯 번째", "${viewModel.User.value?.exercisePurpose}")
                }

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
                ActivityCompat.finishAffinity(this)
//                Log.d("최종", "성별: ${viewModel.User.value?.user_gender} 몸무게: ${viewModel.User.value?.height}  이름: ${viewModel.User.value?.user_name} ")
                // ---- view model에 값을 넣기 (4p) 끝 ----
            }
            // ---- 설정 완료 시, 선택한 데이터 저장 및 페이지 이동 코드 끝 ----

            binding.vp2Setup.currentItem = binding.vp2Setup.currentItem + 1
            binding.stepView.go(binding.stepView.currentStep + 1, true)
        }
        binding.btnSetupPrevious.setOnClickListener {
            binding.vp2Setup.currentItem = binding.vp2Setup.currentItem - 1
            binding.stepView.go(binding.stepView.currentStep - 1, true)
        }
        binding.tvSetupSkip.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            ActivityCompat.finishAffinity(this)
        }
        // ---- 페이지 변경될 때마다 call back 메소드 시작 ----
        binding.vp2Setup.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == 4) {
                    binding.btnSetupNext.text = "Finish"
                } else {
                    binding.btnSetupNext.text = "Next"
                }

                // ---- view model에 값을 넣기 (0p ~ 3p) 시작 ----
                val previousPosition = position - 1
                if (previousPosition >= 0) {
                    val fragment = supportFragmentManager.findFragmentByTag("f$previousPosition")

//                        viewModel.User.value = DataInstance
//                        Log.d("첫번째", "${viewModel.User.value?.user_name}")
                    if (fragment is PersonalSetup1Fragment) {
                        if (fragment.binding.rbtnMale.isChecked) {
//                            viewModel.User.value?.user_gender = "male"
                        } else {
//                            viewModel.User.value?.user_gender = "female"
                        }
                    } else if (fragment is PersonalSetup2Fragment) {
//                        viewModel.User.value?.height = fragment.binding.npPersonalSetup2.value.toDouble()
//                        Log.d("세 번째", "${viewModel.User.value?.height}")
                    } else if (fragment is PersonalSetup3Fragment) {
//                        viewModel.User.value?.weight = fragment.binding.npPerssonalSetup3.value.toDouble()
//                        Log.d("네 번째", "${viewModel.User.value?.weight}")
                    } else if (fragment is PersonalSetup4Fragment) {
                        if (fragment.binding.rbtnhealth.isChecked) {
//                            viewModel.User.value?.exercisePurpose = "health"
                        } else if (fragment.binding.rbtnDiet.isChecked) {
//                            viewModel.User.value?.exercisePurpose = "diet"
                        } else if (fragment.binding.rbtnRehabil.isChecked) {
//                            viewModel.User.value?.exercisePurpose = "Rehabil"
                        } else {
//                            viewModel.User.value?.exercisePurpose = "strength"
                        }
                    }
                    // ---- view model에 값을 넣기 (0p ~ 3p) 끝 ----

                }

            }
        })
        // ---- 페이지 변경될 때마다 call back 메소드 끝 ----
    }
    private fun initViewPager() {
        val viewPager = binding.vp2Setup
        viewPager.isUserInputEnabled = false
        viewPager.adapter = SetupViewPagerAdapter(this)

    }
 }


// setup(성별, 키, 몸무게, 운동 목적 4단계 절차)을 viewpager로 연결할 adapter
class SetupViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    private val fragments = listOf(PersonalSetup1Fragment(), PersonalSetup2Fragment(), PersonalSetup3Fragment(), PersonalSetup4Fragment())
    override fun getItemCount(): Int {
        return fragments.size
    }
    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}
// 클릭 방지 리스너
class OnSingleClickListener(private val clickListener: View.OnClickListener) : View.OnClickListener {
    companion object {

        // ---- 클릭 방지 시간 설정 ----
        const val CLICK_INTERVAL : Long = 1000
        const val Tag = "OnSingleClickListener"
    }
    private var clickable = true

    override fun onClick(v: View?) {
        if (clickable) {
            clickable = false
            v?.run {
                postDelayed({ clickable = true}, CLICK_INTERVAL)
                clickListener.onClick(v)
            }
        } else {
            Log.d(Tag, "wainting for a while")
        }
    }
}
fun View.setOnSingleClickListener(action: (v: View) -> Unit) {
    val listener = View.OnClickListener { action(it) }
    setOnClickListener(OnSingleClickListener(listener))
}