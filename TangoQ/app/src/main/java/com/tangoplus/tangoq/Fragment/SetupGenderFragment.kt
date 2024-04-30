package com.tangoplus.tangoq.Fragment

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.ViewModel.UserViewModel
import com.tangoplus.tangoq.databinding.FragmentSetupGenderBinding

class SetupGenderFragment : Fragment() {
    lateinit var binding : FragmentSetupGenderBinding
    val viewModel: UserViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetupGenderBinding.inflate(inflater)
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
        val cl = view.findViewById<ConstraintLayout>(R.id.clSuGender)
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