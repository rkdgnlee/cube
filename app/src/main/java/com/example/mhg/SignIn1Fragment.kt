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
import com.example.mhg.databinding.FragmentSignIn1Binding
import java.util.regex.Pattern


class SignIn1Fragment : Fragment() {
    lateinit var binding : FragmentSignIn1Binding
    val viewModel : UserViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignIn1Binding.inflate(inflater)
        val namePatternKor =  "^[가-힣]{2,8}\$"
        val namePatternEng = "^[a-zA-Z\\s]{4,20}$"
        val NamePatternKor = Pattern.compile(namePatternKor)
        val NamePatternEng = Pattern.compile(namePatternEng)
                // ----- ! 이름 조건 코드 ! -----
        binding.etName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.nameCondition.value = NamePatternKor.matcher(binding.etName.text.toString()).find() || NamePatternEng.matcher(binding.etName.text.toString()).find()
                viewModel.User.value?.put("user_name", binding.etName.text)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })


        viewModel.User.observe(viewLifecycleOwner) {user ->
            if (user != null) {
                binding.etName.setText(user.getString("user_name"))
                binding.etName.isEnabled = false
                viewModel.nameCondition.value = true
            } else {
                binding.etName.text.clear()
                binding.etName.isEnabled = true
            }
        }


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)





    }
}