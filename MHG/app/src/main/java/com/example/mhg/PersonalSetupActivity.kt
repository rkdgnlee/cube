package com.example.mhg

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.ActivityPersonalSetupBinding
import com.example.mhg.`object`.NetworkUserService.fetchUserUPDATEJson
import com.example.mhg.`object`.Singleton_t_user
import org.json.JSONObject
import java.net.URLEncoder

class PersonalSetupActivity : AppCompatActivity() {
    lateinit var binding: ActivityPersonalSetupBinding
    val viewModel: UserViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonalSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ---- viewmodel 초기화 및 viewpager2 초기화 ----
//        viewModel = ViewModelProvider(this).get(UserViewModel::class.java)
        initViewPager()
        val t_userData = Singleton_t_user.getInstance(this)


        binding.btnSetupNext.setOnSingleClickListener {

            // ---- 설정 완료 시, 선택한 데이터 저장 및 페이지 이동 코드 시작 ----
            if (binding.btnSetupNext.text == "Finish") {

                // -----! singletom에 넣고, update 통신 !-----
                val user_mobile = t_userData.jsonObject?.optString("user_mobile")
                val JsonObj = JSONObject()
                JsonObj.put("user_gender", viewModel.User.value?.optString("user_gender"))
                Log.w("$TAG, 성별", viewModel.User.value?.optString("user_gender").toString())
                JsonObj.put("user_height", viewModel.User.value?.optString("user_height"))
                Log.w("$TAG, 키", viewModel.User.value?.optString("user_height").toString())
                JsonObj.put("user_weight", viewModel.User.value?.optString("user_weight"))
                Log.w("$TAG, 몸무게", viewModel.User.value?.optString("user_weight").toString())
                Log.w("$TAG, JSON몸통", "$JsonObj")

                if (user_mobile != null) {
                    val encodedUserMobile = URLEncoder.encode(user_mobile, "UTF-8")
                    Log.w(TAG+" encodedUserMobile", encodedUserMobile)
                    fetchUserUPDATEJson(getString(R.string.IP_ADDRESS_t_user), JsonObj.toString(), encodedUserMobile) {
                        t_userData.jsonObject!!.put("user_gender", viewModel.User.value?.optString("user_gender"))
                        t_userData.jsonObject!!.put("user_height", viewModel.User.value?.optString("user_height"))
                        t_userData.jsonObject!!.put("user_weight", viewModel.User.value?.optString("user_weight"))
                        Log.w(TAG+" 싱글톤객체추가", t_userData.jsonObject!!.optString("user_weight"))
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                        ActivityCompat.finishAffinity(this)
                    }
                }
                // -----! view model에 값을 넣기 끝 !-----
            }
            // -----! 설정 완료 시, 선택한 데이터 저장 및 페이지 이동 코드 끝 !-----

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
        // -----! 페이지 변경될 때마다 call back 메소드 시작 !-----
        binding.vp2Setup.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == 3) {
                    binding.btnSetupNext.text = "Finish"
                } else {
                    binding.btnSetupNext.text = "Next"
                }
            }
        })
        // -----! 페이지 변경될 때마다 call back 메소드 끝 !-----
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
private fun View.setOnSingleClickListener(action: (v: View) -> Unit) {
    val listener = View.OnClickListener { action(it) }
    setOnClickListener(OnSingleClickListener(listener))
}