package com.example.mhg

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentSignIn2Binding


class SignIn2Fragment : Fragment() {
    lateinit var binding: FragmentSignIn2Binding
    val viewModel : UserViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignIn2Binding.inflate(inflater)

        viewModel.User.observe(viewLifecycleOwner) {user ->
            if (user != null) {
                binding.etId.isEnabled = false
                viewModel.idCondition.value = true
                viewModel.pwCondition.value = true
                viewModel.pwCompare.value = true
            } else {
                binding.etId.text.clear()
                binding.etId.isEnabled = true
            }
        }


        return binding.root
    }
}