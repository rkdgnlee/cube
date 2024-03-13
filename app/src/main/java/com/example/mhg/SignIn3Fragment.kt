package com.example.mhg

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentSignIn3Binding


class SignIn3Fragment : Fragment() {
    lateinit var binding : FragmentSignIn3Binding
    val viewModel : UserViewModel by activityViewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignIn3Binding.inflate(inflater)

        viewModel.User.observe(viewLifecycleOwner) {user ->
            if (user != null) {
                binding.etEmail.setText(user.getString("user_email"))
                binding.etEmail.isEnabled = false
                viewModel.emailCondition.value = true
            } else {
                binding.etEmail.text.clear()
                binding.etEmail.isEnabled = true
            }
        }
        return binding.root
    }

}