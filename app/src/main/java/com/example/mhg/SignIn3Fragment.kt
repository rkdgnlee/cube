package com.example.mhg

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentSignIn3Binding
import java.util.regex.Pattern


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
        val emailPattern =  "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$"
        val EmailPattern = Pattern.compile(emailPattern)



        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.emailCondition.value = EmailPattern.matcher(binding.etEmail.text.toString()).find()
                // -----! 뷰모델에 값 넣기 !-----
                viewModel.User.value?.put("user_email", binding.etEmail.text)

            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
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