package com.tangoplus.tangoq.fragment

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.icu.util.Calendar
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.UserViewModel
import com.tangoplus.tangoq.databinding.FragmentSetup1Binding

class Setup1Fragment : Fragment() {
    lateinit var binding : FragmentSetup1Binding
    val viewModel: UserViewModel by activityViewModels()
    private var current = ""
    private val yyyymmdd = "YYYYMMDD"
    private val cal = Calendar.getInstance()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetup1Binding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString("FRAGMENT_TAG")?.let { tag ->
            fragmentManager?.beginTransaction()?.addToBackStack(tag)?.commit()
        }

//        viewModel.step1.observe(viewLifecycleOwner) {
//            when (it) {
//                true -> {
//                    binding.rbtnSquareMale.isChecked = true
//                    binding.rbtnMale.isChecked = true
//                }
//                false -> {
//                    binding.rbtnSquareFemale.isChecked = true
//                    binding.rbtnFemale.isChecked = true
//                }
//                null -> {
//                    binding.rbtnSquareMale.isChecked = false
//                    binding.rbtnMale.isChecked = false
//                    binding.rbtnSquareFemale.isChecked = false
//                    binding.rbtnFemale.isChecked = false
//                }
//            }
//            Log.v("VM>Gender" ,"${viewModel.step1.value}")
//        }
//
//        binding.rbtnSquareMale.setOnClickListener { viewModel.step1.value = true }
//        binding.rbtnMale.setOnClickListener{ viewModel.step1.value = true }
//        binding.rbtnSquareFemale.setOnClickListener { viewModel.step1.value = false }
//        binding.rbtnFemale.setOnClickListener { viewModel.step1.value = false }
        // ------! 라디오 버튼 연동 끝 !------

        val cl = view.findViewById<ConstraintLayout>(R.id.clSetup1)
        val fadeIn = ObjectAnimator.ofFloat(cl, "alpha", 0f, 1f)
        fadeIn.duration = 900

        val moveUp = ObjectAnimator.ofFloat(cl, "translationY", 100f, 0f)
        moveUp.duration = 900
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(fadeIn, moveUp)
        animatorSet.start()
        viewModel.step1.value = false
        binding.etSetup1Birthday.addTextChangedListener(object : TextWatcher{

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (viewModel.step1.value == true) return
                viewModel.step1.value = true

                // 숫자만 남기고 모든 문자를 제거
                val cleaned = s.toString().replace(Regex("[^\\d]"), "")

                val formatted = when {
                    cleaned.length <= 4 -> cleaned
                    cleaned.length <= 6 -> "${cleaned.substring(0, 4)} / ${cleaned.substring(4)}"
                    else -> "${cleaned.substring(0, 4)} / ${cleaned.substring(4, 6)} / ${cleaned.substring(6)}"
                }

                s?.replace(0, s.length, formatted)
                if (cleaned.length == 8) {
                    val year = cleaned.substring(0, 4).toIntOrNull()
                    val month = cleaned.substring(4, 6).toIntOrNull()
                    val day = cleaned.substring(6, 8).toIntOrNull()

                    if (year != null && month != null && day != null) {
                        val isValidDate = isValidDate(year, month, day)
                        if (!isValidDate) {
                            // 유효하지 않은 날짜일 때 처리 (예: 에러 메시지 표시)
                            s?.clear()
                            Log.v("validate", "${s}, isavalidate")
                        } else {
                            Log.v("validate", "${s}, avalidate")
                        }
                    }
                }

                viewModel.step1.value = false
            }
        })


    }
    private fun isValidDate(year: Int, month: Int, day: Int): Boolean {
        return try {
            val calendar = java.util.Calendar.getInstance()
            calendar.setLenient(false)
            calendar.set(year, month - 1, day) // Month는 0부터 시작
            calendar.getTime() // 이 단계에서 예외 발생 시 유효하지 않은 날짜
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun onPause() {
        super.onPause()
//        if (binding.rbtnMale.isChecked) {
//            viewModel.User.value?.put("user_gender", "남자")
//        } else {
//            viewModel.User.value?.put("user_gender", "여자")
//        }

    }
}