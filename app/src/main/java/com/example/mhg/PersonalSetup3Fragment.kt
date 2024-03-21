package com.example.mhg

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentPersonalSetup3Binding


class PersonalSetup3Fragment : Fragment() {
    lateinit var binding : FragmentPersonalSetup3Binding
    val viewModel: UserViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPersonalSetup3Binding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val fadeIn = ObjectAnimator.ofFloat(binding.clPersonalSetup3, "alpha", 0f, 1f)
//        fadeIn.duration = 900
//        val moveUp = ObjectAnimator.ofFloat(binding.clPersonalSetup3, "translationY", 100f, 0f)
//        moveUp.duration = 900
//        val animatorSet = AnimatorSet()
//        animatorSet.apply {
//            play(fadeIn)
//            play(moveUp)
//        }
//        animatorSet.start()

    }

    override fun onPause() {
        super.onPause()
        val weight = binding.npPerssonalSetup3.value
        Log.w("몸무게", "$weight")
        viewModel.User.value?.put("user_weight", weight)
    }

}