package com.example.mhg

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentPersonalSetup4Binding


class PersonalSetup4Fragment : Fragment() {
    lateinit var binding : FragmentPersonalSetup4Binding
    val viewModel: UserViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPersonalSetup4Binding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val fadeIn = ObjectAnimator.ofFloat(binding.clPersonalSetup4, "alpha", 0f, 1f)
//        fadeIn.duration = 900
//        val moveUp = ObjectAnimator.ofFloat(binding.clPersonalSetup4, "translationY", 100f, 0f)
//        moveUp.duration = 900
//        val animatorSet = AnimatorSet()
//        animatorSet.apply {
//            play(fadeIn)
//            play(moveUp)
//        }
//        animatorSet.start()

        // ---- 2*2 (건강, 회복, 근력, 다이어트) 라디오버튼 및 이미지 버튼 연동 시작 ----
        binding.rbtnhealth.setOnClickListener { binding.rbtnStrength.isChecked = false ; binding.rbtnDiet.isChecked = false }
        binding.rbtnRehabil.setOnClickListener { binding.rbtnStrength.isChecked = false ; binding.rbtnDiet.isChecked = false }
        binding.rbtnDiet.setOnClickListener { binding.rbtnhealth.isChecked = false ; binding.rbtnRehabil.isChecked = false }
        binding.rbtnStrength.setOnClickListener { binding.rbtnhealth.isChecked = false; binding.rbtnRehabil.isChecked = false }

        binding.imgbtnHealth.setOnClickListener { binding.rbtnhealth.isChecked = true ; binding.rbtnDiet.isChecked = false ; binding.rbtnStrength.isChecked = false }
        binding.imgbtnRehabil.setOnClickListener { binding.rbtnRehabil.isChecked = true ; binding.rbtnDiet.isChecked = false ; binding.rbtnStrength.isChecked = false }
        binding.imgbtnDiet.setOnClickListener { binding.rbtnDiet.isChecked = true ; binding.rbtnDiet.isChecked = false ; binding.rbtnStrength.isChecked = false }
        binding.imgbtnRehabil.setOnClickListener { binding.rbtnStrength.isChecked = true ; binding.rbtnDiet.isChecked = false ; binding.rbtnStrength.isChecked = false }

        // ---- 2*2 (건강, 회복, 근력, 다이어트) 라디오버튼 및 이미지 버튼 연동 끝 ----
    }

    override fun onPause() {
        super.onPause()
//        if (fragment.binding.rbtnhealth.isChecked) {
////                        viewModel.User.value?.put("","health")
//        } else if (fragment.binding.rbtnDiet.isChecked) {
////                        viewModel.User.value?.exercisePurpose = "diet"
//        } else if (fragment.binding.rbtnRehabil.isChecked) {
////                        viewModel.User.value?.exercisePurpose = "Rehabil"
//        } else {
////                        viewModel.User.value?.exercisePurpose = "strength"
//        }
////                    Log.d("다섯 번째", "${viewModel.User.value?.exercisePurpose}")
    }
}