package com.tangoplus.tangoq.fragment

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.tangoplus.tangoq.viewmodel.UserViewModel
import com.tangoplus.tangoq.databinding.FragmentSetup2Binding
import com.tangoplus.tangoq.listener.WeightVisibilityListener


class Setup2Fragment : Fragment(), WeightVisibilityListener {
    lateinit var binding: FragmentSetup2Binding
    val viewModel: UserViewModel by activityViewModels()
    var height = ""
    var weight = ""
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetup2Binding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ------# 초기에 몸무게 나중에 나오게 하기  #------
        binding.clSetupWeight.visibility = View.GONE

        binding.etSetupHeight.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                height = binding.etSetupHeight.text.toString()
                if (height.isNotEmpty()) {
                    try {
                        val heightValue = height.toInt()
                        viewModel.step21.value = !(heightValue >= 250 || heightValue <= 90)
                        if (viewModel.step21.value == true) {
                            viewModel.User.value?.put("height", heightValue)
                        }
                    } catch (e: NumberFormatException) {
                        viewModel.step21.value = false
                    }
                } else {
                    viewModel.step21.value = false
                }
                Log.v("스텝", "step21: ${viewModel.step21.value}, step2: ${viewModel.step2.value}")
            }
        })

        binding.etSetupWeight.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                weight = s.toString()
                if (weight.isNotEmpty()) {
                    try {
                        val weightValue = weight.toInt()
                        viewModel.step22.value = !(weightValue >= 180 || weightValue <= 30)
                        if (viewModel.step21.value == true) {
                            viewModel.User.value?.put("weight", weightValue)
                        }
                    } catch (e: NumberFormatException) {
                        viewModel.step22.value = false
                    }
                } else {
                    viewModel.step22.value = false
                }
                Log.v("스텝", "step22: ${viewModel.step22.value}, step2: ${viewModel.step2.value}")
            }
        })

//        // 뷰모델에서 step2가 true일 때 실행할 동작들 control
        viewModel.step2.observe(viewLifecycleOwner) {}

        binding.etSetupHeight.setOnKeyListener{v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                if (viewModel.step21.value == true) {
                    visibleWeight()
                    Log.v("keyboard", "hided.")
                } else {
                    Toast.makeText(requireContext(), "정확한 키를 입력해주세요 ", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }
        binding.etSetupWeight.setOnKeyListener{v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                if (viewModel.step21.value == true) {
                    val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.hideSoftInputFromWindow(view.windowToken, 0)
                    Log.v("keyboard", "hided.")
                } else {
                    Toast.makeText(requireContext(), "정확한 키를 입력해주세요 ", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }
    }

    override fun onPause() {
        super.onPause()
//        height = binding.etSetupWeight.text.toString()
//        weight = binding.etSetupWeight.text.toString()
//        viewModel.User.value?.put("user_height", height)
//        viewModel.User.value?.put("user_weight", weight)
//        Log.v("키 , 몸무게", "몸무게: ${viewModel.User.value?.optString("user_weight")}, 키: ${viewModel.User.value?.getString("user_height")}")
    }

    override fun visibleWeight() {
        binding.tvSetupBodySizeGuide.text = "몸무게를 입력해주세요"

        val moveUpHeight = ObjectAnimator.ofFloat(binding.clSetupHeight, "translationY", 0f, -100f)
        moveUpHeight.duration = 700 // clSetupWeight와 동일한 시간으로 설정
        moveUpHeight.interpolator = DecelerateInterpolator() // 부드러운 감속 효과

        // clSetupWeight 애니메이션
        binding.clSetupWeight.visibility = View.VISIBLE
        val fadeIn = ObjectAnimator.ofFloat(binding.clSetupWeight, "alpha", 0f, 1f)
        val moveUpWeight = ObjectAnimator.ofFloat(binding.clSetupWeight, "translationY", 100f, 0f)

        // 모든 애니메이션을 함께 실행
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(moveUpHeight, fadeIn, moveUpWeight)
        animatorSet.duration = 700
        animatorSet.start()

        //
    }
}