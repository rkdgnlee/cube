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
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class SignIn4Fragment : Fragment() {
    lateinit var binding: FragmentSignIn4Binding
    val viewModel : UserViewModel by activityViewModels()
    val auth = Firebase.auth
    var verificationId = ""
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
        // -----! 인증문자 발송 & 확인 시작 ! -----
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                TODO("Not yet implemented")
            }

            override fun onVerificationFailed(p0: FirebaseException) {
                TODO("Not yet implemented")
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                super.onCodeSent(verificationId, token)
                this@SignIn4Fragment.verificationId = verificationId

            }
        }

        binding.btnAuthSend.setOnClickListener {
            val transformMobile = phoneNumber82(binding.etMobile.text.toString())
            val optionsCompat = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(transformMobile)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setCallbacks(callbacks)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(optionsCompat)
            auth.setLanguageCode("kr")
        }
        binding.btnAuthConfirm.setOnClickListener {
            val credential = PhoneAuthProvider.getCredential(verificationId, binding.etAuthNumber.text.toString())
            signInWithPhoneAuthCredential(credential)
        }

        // -----! 인증문자 발송 & 확인 끝 ! -----
        binding.btnSignIn.setOnClickListener {
            if (viewModel.mobileAuthCondition.value == true) {
                val JsonObj = viewModel.User.value
                val user_mobile = binding.etMobile.text.toString().replaceFirst("010", "+8210")
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
    fun phoneNumber82(msg: String) : String {
        val firstNumber: String = msg.substring(0,3)
        var phoneEdit = msg.substring(3)
        when (firstNumber) {
            "010" -> phoneEdit = "+8210$phoneEdit"
            "011" -> phoneEdit = "+8211$phoneEdit"
            "016" -> phoneEdit = "+8216$phoneEdit"
            "017" -> phoneEdit = "+8217$phoneEdit"
            "018" -> phoneEdit = "+8218$phoneEdit"
            "019" -> phoneEdit = "+8219$phoneEdit"
            "106" -> phoneEdit = "+82106$phoneEdit"
        }
        return phoneEdit
    }
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    viewModel.mobileAuthCondition.value = true
                    binding.etAuthNumber.isEnabled = false
                    binding.etMobile.isEnabled = false
                } else {
                    Log.w(TAG, "mobile auth failed.")
                }
            }
    }
}