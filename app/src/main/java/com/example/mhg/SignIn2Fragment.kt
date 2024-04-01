package com.example.mhg

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.activityViewModels
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentSignIn2Binding
import java.util.regex.Pattern


class SignIn2Fragment : Fragment() {
    interface OnFragmentInteractionListener {
        fun onFragmentInteraction()
    }
    private var listener : OnFragmentInteractionListener? = null
    lateinit var binding: FragmentSignIn2Binding
    val viewModel : UserViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignIn2Binding.inflate(inflater)
        viewModel.User.observe(viewLifecycleOwner) {user ->
            if (user != null && user.has("user_id")) {
                viewModel.idCondition.value = true
                viewModel.pwCondition.value = true
                viewModel.pwCompare.value = true
            } else {
                binding.etId.text.clear()
            }
        }
        // -----! 글자 입력해주세요 애니메이션!-----
        val fadeIn = ObjectAnimator.ofFloat(binding.tvSignIn2, "alpha", 0f, 1f)
        fadeIn.duration = 900

        val moveUp = ObjectAnimator.ofFloat(binding.tvSignIn2, "translationY", 50f, 0f)
        moveUp.duration = 900
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(fadeIn, moveUp)
        animatorSet.start()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val pwPattern = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[$@$!%*#?&.^])[A-Za-z[0-9]$@$!%*#?&.^]{8,20}$" // 영문, 특수문자, 숫자 8 ~ 20자 패턴
        val idPattern = "^[a-zA-Z0-9]{4,16}$" // 영문, 숫자 4 ~ 16자 패턴
        val IdPattern = Pattern.compile(idPattern)
        val Pwpattern = Pattern.compile(pwPattern)
        // ----- ! ID 조건 코드 ! -----
        binding.etId.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.idCondition.value = IdPattern.matcher(binding.etId.text.toString()).find()
                if (viewModel.idCondition.value == true) {
                    binding.tvIdCondition.setTextColor(binding.tvIdCondition.resources.getColor(R.color.success_green))
                    binding.tvIdCondition.text = "조건에 일치합니다."
                    viewModel.User.value?.put("user_id", s.toString())
                    Log.w(ContentValues.TAG, "${viewModel.User.value?.getString("user_id")}")
                } else {
                    binding.tvIdCondition.setTextColor(binding.tvIdCondition.resources.getColor(R.color.mainColor))
                    binding.tvIdCondition.text = "조건에 일치하지 않습니다"
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        binding.etId.setOnFocusChangeListener { _, hasFocus ->
            val transitionDrawable = binding.etId.background as? TransitionDrawable
            if (hasFocus) {
                transitionDrawable?.startTransition(500)
            } else {
                transitionDrawable?.reverseTransition(500)
            }
        }
        // ----- ! 비밀번호 조건 코드 ! -----
        binding.etPw.addTextChangedListener(object : TextWatcher {
            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(s: Editable?) {
                viewModel.pwCondition.value = Pwpattern.matcher(binding.etPw.text.toString()).find()
                if (viewModel.pwCondition.value == true) {
                    binding.tvPwCondition.setTextColor(binding.tvPwCondition.resources.getColor(R.color.success_green))
                    binding.tvPwCondition.text = "조건에 일치합니다"

                } else {
                    binding.tvPwCondition.setTextColor(binding.tvPwCondition.resources.getColor(R.color.mainColor))
                    binding.tvPwCondition.text = "영문, 숫자, 특수문자( ! @ # $ % ^ & * ? .)를 모두 포함해서 8~20자리를 입력해주세요"

                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        binding.etPw.setOnFocusChangeListener { _, hasFocus ->
            val transitionDrawable = binding.etPw.background as? TransitionDrawable
            if (hasFocus) {
                transitionDrawable?.startTransition(500)
            } else {
                transitionDrawable?.reverseTransition(500)
            }
        }
        // ----- ! 비밀번호 확인 코드 ! -----
        binding.etPwRepeat.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.pwCompare.value = (binding.etPw.text.toString() == binding.etPwRepeat.text.toString())
                if (viewModel.pwCompare.value == true) {
                    binding.tvPwCompare.setTextColor(binding.tvPwCompare.resources.getColor(R.color.success_green))
                    binding.tvPwCompare.text = "비밀번호가 일치합니다"
                } else {
                    binding.tvPwCompare.setTextColor(binding.tvPwCompare.resources.getColor(R.color.mainColor))
                    binding.tvPwCompare.text = "비밀번호가 일치하지 않습니다"
                }
                // -----! 뷰모델에 보낼 값들 넣기 !-----

                viewModel.User.value?.put("user_password", s.toString())

            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        binding.etPwRepeat.setOnFocusChangeListener { _, hasFocus ->
            val transitionDrawable = binding.etPwRepeat.background as? TransitionDrawable
            if (hasFocus) {
                transitionDrawable?.startTransition(500)
            } else {
                transitionDrawable?.reverseTransition(500)
            }
        }




        // -----! 자판에서 다음 눌렀을 때 페이지 넘어가기 !-----
        binding.etPwRepeat.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                listener?.onFragmentInteraction()
                true
            } else {
                false
            }
        }

    }
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }
}