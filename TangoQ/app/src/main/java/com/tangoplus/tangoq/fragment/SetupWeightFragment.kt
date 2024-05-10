package com.tangoplus.tangoq.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.tangoplus.tangoq.data.UserViewModel
import com.tangoplus.tangoq.databinding.FragmentSetupWeightBinding


class SetupWeightFragment : Fragment() {
   lateinit var binding : FragmentSetupWeightBinding
    val viewModel: UserViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetupWeightBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
    override fun onPause() {
        super.onPause()
        val weight = binding.etSuWeight.text
        Log.w("몸무게", "$weight")
        viewModel.User.value?.put("user_weight", weight)
    }
}