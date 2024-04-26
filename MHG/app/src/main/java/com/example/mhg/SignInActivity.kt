package com.example.mhg

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.mhg.Dialog.AgreementBottomSheetDialogFragment
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.ActivitySignInBinding
import org.json.JSONObject

class SignInActivity :
    AppCompatActivity(),
    SignIn1Fragment.OnFragmentInteractionListener,
    SignIn2Fragment.OnFragmentInteractionListener,
    SignIn3Fragment.OnFragmentInteractionListener {
    lateinit var binding : ActivitySignInBinding
    val viewModel: UserViewModel by viewModels()
    lateinit var pagerAdapter: SignInViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        initViewPager()

        pagerAdapter = SignInViewPagerAdapter(this)
        binding.vp2SignIn.adapter = pagerAdapter
        // -----! google login 했을 시 페이지 지정 시작 !-----
        val userString = intent.getStringExtra("google_user")
        if (userString != null) {
            val user = JSONObject(userString)
            viewModel.User.value = user
            if (viewModel.User.value != null) {
                binding.vp2SignIn.setCurrentItem(3)
            }
        } else {
            viewModel.User.value = JSONObject()
        }
//

        // -----! google login 했을 시 페이지 지정 끝 !-----

        // -----! 페이지 변경 callback 메소드 시작 !-----
        binding.vp2SignIn.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            @SuppressLint("SetTextI18n", "CutPasteId")
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> {
                        binding.tvSignInPrevious.visibility = View.GONE
                        binding.tvSignInNext.visibility = View.VISIBLE
                    }
                    1 -> {
                        binding.tvSignInPrevious.visibility = View.VISIBLE
                        binding.tvSignInNext.visibility = View.VISIBLE
                    }
                    2 -> {
                        val fadeIn = ObjectAnimator.ofFloat(findViewById(R.id.tvSignIn3), "alpha", 0f, 1f)
                        fadeIn.duration = 900
                        val moveUp = ObjectAnimator.ofFloat(findViewById(R.id.tvSignIn3), "translationY", 100f, 0f)
                        moveUp.duration = 900
                        val animatorSet = AnimatorSet()
                        animatorSet.playTogether(fadeIn, moveUp)
                        animatorSet.start()
                        binding.tvSignInPrevious.visibility = View.VISIBLE
                        binding.tvSignInNext.visibility = View.VISIBLE
                    }
                    3 -> {
                        val fadeIn = ObjectAnimator.ofFloat(findViewById(R.id.tvSignIn4), "alpha", 0f, 1f)
                        fadeIn.duration = 900
                        val moveUp = ObjectAnimator.ofFloat(findViewById(R.id.tvSignIn4), "translationY", 100f, 0f)
                        moveUp.duration = 900
                        val animatorSet = AnimatorSet()
                        animatorSet.playTogether(fadeIn, moveUp)
                        animatorSet.start()
                        binding.tvSignInNext.visibility = View.GONE
                    }
                }
            }
        })
        binding.tvSignInPrevious.setOnSingleClickListener {
            binding.vp2SignIn.currentItem = binding.vp2SignIn.currentItem - 1
        }
        binding.tvSignInNext.setOnSingleClickListener {
            binding.vp2SignIn.currentItem = binding.vp2SignIn.currentItem + 1
        }

        // -----! 페이지 변경 callback 메소드 끝 !-----
    }
    override fun onFragmentInteraction() {
        binding.tvSignInNext.performClick()
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
    fun getFragment(position: Int): Fragment {
        return fragments[position]
    }
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
