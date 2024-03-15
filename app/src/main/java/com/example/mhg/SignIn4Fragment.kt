package com.example.mhg

import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.telephony.PhoneNumberFormattingTextWatcher
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.mhg.Dialog.ExerciseLoadDialogFragment
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentSignIn4Binding
import com.example.mhg.`object`.NetworkService
import com.example.mhg.`object`.NetworkService.fetchINSERTJson
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

        //  -----! 전화번호 자동 형변환 !-----

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mobilePattern = "^010-\\d{4}-\\d{4}$"
        val MobilePattern = Pattern.compile(mobilePattern)
        binding.etMobile.addTextChangedListener(PhoneNumberFormattingTextWatcher())
        binding.etMobile.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.mobileCondition.value = MobilePattern.matcher(binding.etMobile.text.toString()).find()
                viewModel.User.value?.put("user_mobile", binding.etMobile.text)
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
                val user_mobile = binding.etMobile.text.toString().replaceFirst("010", "+82 10")
                JsonObj?.put("user_mobile", user_mobile)
                Log.w(TAG, "${JsonObj?.getString("user_mobile")}")
                Log.w(TAG+"VIEWMODEL", "$JsonObj")

                // -----! json에 데이터 추가 후 singleton 담기  !-----
                fetchINSERTJson(getString(R.string.IP_ADDRESS_t_user), JsonObj.toString()) {
                    // -----! 백엔드 작업 후, singleton에 넣을 때 main thread에서 실행 !-----
                    activity?.runOnUiThread{
                        val t_userInstance = context?.let { Singleton_t_user.getInstance(requireContext()) }
                        t_userInstance?.jsonObject = JsonObj
                        Log.e("OKHTTP3>싱글톤", "${t_userInstance?.jsonObject}")

                        // -----! 바로 운동 데이터 DialogFragment펼쳐서 받아오기 !-----

                        val dialogFragment = ExerciseLoadDialogFragment()
                        dialogFragment.show(requireActivity().supportFragmentManager, "DialogFragment")


                    }

                }
            }
        }
    }

}