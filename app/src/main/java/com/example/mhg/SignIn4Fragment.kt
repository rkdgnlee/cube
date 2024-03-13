package com.example.mhg

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentSignIn4Binding
import com.example.mhg.`object`.NetworkService
import com.example.mhg.`object`.Singleton_t_user
import java.util.regex.Pattern

class SignIn4Fragment : Fragment() {
    lateinit var binding: FragmentSignIn4Binding
    val viewModel : UserViewModel by activityViewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignIn4Binding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mobilePattern = "^010[0-9]{8,9}$"
        val MobilePattern = Pattern.compile(mobilePattern)
        binding.etMobile.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.mobileCondition.value = MobilePattern.matcher(binding.etMobile.text.toString()).find()
            }
        })
        viewModel.mobileCondition.observe(viewLifecycleOwner) { condition ->
            binding.btnSignIn.isEnabled = condition
            binding.btnSignIn.setBackgroundColor(
                if (condition) binding.btnSignIn.resources.getColor(R.color.success_green)
                else binding.btnSignIn.resources.getColor(R.color.orange)
            )
        }
        binding.btnSignIn.setOnClickListener {
            if (viewModel.mobileCondition.value == true) {
                val JsonObj = viewModel.User.value
                JsonObj?.put("user_mobile", binding.etMobile.text)

                // -----! json에 데이터 추가 후 singleton 담기 + Main 이동 !-----
                NetworkService.fetchINSERTJson(getString(R.string.IP_ADDRESS_T_USER), JsonObj.toString()) {
                val t_userInstance = context?.let { Singleton_t_user.getInstance(requireContext()) }
                t_userInstance?.jsonObject = JsonObj
                Log.e("OKHTTP3>싱글톤", "${t_userInstance?.jsonObject}")
                val intent = Intent(requireContext(), MainActivity::class.java)
                startActivity(intent)
                }
            }
        }
    }
}