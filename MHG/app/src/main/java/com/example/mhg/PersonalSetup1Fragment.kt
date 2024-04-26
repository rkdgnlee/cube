package com.example.mhg

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentPersonalSetup1Binding


class PersonalSetup1Fragment : Fragment() {
    lateinit var binding : FragmentPersonalSetup1Binding
    val viewModel: UserViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPersonalSetup1Binding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ---- 라디오버튼 연동 시작 ----
        val rbtnsquaremale = view.findViewById<RadioButton>(R.id.rbtnSquareMale)
        val rbtnmale = view.findViewById<RadioButton>(R.id.rbtnMale)
        rbtnsquaremale.setOnClickListener {
            rbtnmale.isChecked = true
        }
        rbtnmale.setOnClickListener {
            rbtnsquaremale.isChecked = true
        }

        val rbtnsquarefemale = view.findViewById<RadioButton>(R.id.rbtnSquareFemale)
        val rbtnfemale = view.findViewById<RadioButton>(R.id.rbtnFemale)

        rbtnsquarefemale.setOnClickListener {
            rbtnfemale.isChecked = true
        }
        rbtnfemale.setOnClickListener {
            rbtnsquarefemale.isChecked = true
        }
        // ---- 라디오 버튼 연동 끝 ----

        val spannableStringBuilder = SpannableStringBuilder(rbtnsquaremale.text)
        spannableStringBuilder.setSpan(
            RelativeSizeSpan(2.2f),
            0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        rbtnsquaremale.setLineSpacing(1.1f, 1.2f)
        rbtnsquarefemale.setLineSpacing(1.1f, 1.2f)
        rbtnsquaremale.text = spannableStringBuilder
        val spannableStringBuilder2 = SpannableStringBuilder(rbtnsquarefemale.text)
        spannableStringBuilder2.setSpan(
            RelativeSizeSpan(2.2f), 0, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        rbtnsquarefemale.text = spannableStringBuilder2


//        val fadeOut = ObjectAnimator.ofFloat(cl, "alpha", 1f, 0f)
//        fadeOut.duration = 1000
//        fadeOut.start()
        val cl = view.findViewById<ConstraintLayout>(R.id.clPersonalSetup1)
        val fadeIn = ObjectAnimator.ofFloat(cl, "alpha", 0f, 1f)
        fadeIn.duration = 900

        val moveUp = ObjectAnimator.ofFloat(cl, "translationY", 100f, 0f)
        moveUp.duration = 900
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(fadeIn, moveUp)
        animatorSet.start()
    }

    override fun onPause() {
        super.onPause()
        if (binding.rbtnMale.isChecked) {
            viewModel.User.value?.put("user_gender", "남자")
        } else {
            viewModel.User.value?.put("user_gender", "여자")
        }
    }
}