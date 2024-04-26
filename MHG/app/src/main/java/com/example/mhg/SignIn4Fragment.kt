package com.example.mhg

import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.telephony.PhoneNumberFormattingTextWatcher
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.example.mhg.Dialog.SignInBottomSheetDialogFragment
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentSignIn4Binding
import com.example.mhg.`object`.NetworkUserService.fetchUserINSERTJson
import com.example.mhg.`object`.Singleton_t_user
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern


class SignIn4Fragment : Fragment() {
    lateinit var binding: FragmentSignIn4Binding
    private lateinit var firebaseAuth : FirebaseAuth
    val viewModel : UserViewModel by activityViewModels()
    val auth = Firebase.auth
    var verificationId = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getActivity()?.getWindow()?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
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

        // ------! 통신사 선택 !-----
        binding.tvTelecom.setOnClickListener {
            showBottomSheetDialog(requireActivity())
        }

        // ------! 전화번호 형식 !-----
        val mobilePattern = "^010\\d{4}\\d{4}$"
        val MobilePattern = Pattern.compile(mobilePattern)
        binding.etMobile.addTextChangedListener(object: TextWatcher, PhoneNumberFormattingTextWatcher() {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.mobileCondition.value = MobilePattern.matcher(binding.etMobile.text.toString()).find()
                if (viewModel.mobileCondition.value == true) {
                    viewModel.User.value?.put("user_mobile", s.toString())
                    binding.btnAuthSend.isEnabled = true

                }
            }
        })

        // -----! 인증문자 발송 & 확인 시작 ! -----
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(p0: PhoneAuthCredential) {}
            override fun onVerificationFailed(p0: FirebaseException) {}
            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                super.onCodeSent(verificationId, token)
                this@SignIn4Fragment.verificationId = verificationId

                // -----! 메시지 발송에 성공하면 스낵바 호출 !------
                Snackbar.make(requireView(), "메시지 발송에 성공했습니다. 잠시만 기다려주세요", Snackbar.LENGTH_LONG).show()
                binding.btnAuthConfirm.isEnabled = true
            }
        }

        binding.btnAuthSend.setOnClickListener {
            val transformMobile = phoneNumber82(binding.etMobile.text.toString())
            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("전화번호 확인")
                .setMessage("${binding.etMobile.text} 번호로 인증번호를 보내시겠습니까?")
                .setPositiveButton("예") { _, _ ->
                    val optionsCompat = PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(transformMobile)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(requireActivity())
                        .setCallbacks(callbacks)
                        .build()
                    Log.w(TAG+"문자", "${PhoneAuthOptions.newBuilder(auth)}")
                    PhoneAuthProvider.verifyPhoneNumber(optionsCompat)
                    auth.setLanguageCode("kr")
                }
                .setNegativeButton("아니오", null)
                .show()

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
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
                fetchUserINSERTJson(getString(R.string.IP_ADDRESS_t_user), JsonObj.toString()) {
                    // -----! 백엔드 작업 후, singleton에 넣을 때 main thread에서 실행 !-----
                    activity?.runOnUiThread{
                        val t_userInstance = context?.let { Singleton_t_user.getInstance(requireContext()) }
                        t_userInstance?.jsonObject = JsonObj
                        Log.e("OKHTTP3>싱글톤", "${t_userInstance?.jsonObject}")

                        val intent = Intent(requireContext() ,PersonalSetupActivity::class.java)
                        startActivity(intent)
                        ActivityCompat.finishAffinity(requireActivity())
                    }
                }
            }
        }
    }

    fun phoneNumber82(msg: String) : String {
        val firstNumber: String = msg.substring(0,3)
        var phoneEdit = msg.substring(3)
        when (firstNumber) {
            "010" -> phoneEdit = "+8210-${phoneEdit.substring(4, 7)}-${phoneEdit.substring(8)}"
            "106" -> phoneEdit = "+8210-${phoneEdit.substring(4, 7)}-${phoneEdit.substring(8)}"
        }
        return phoneEdit
    }
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    activity?.runOnUiThread {
                        viewModel.mobileAuthCondition.value = true
                        binding.etAuthNumber.isEnabled = false
                        binding.etMobile.isEnabled = false
                        val transitionDrawable = binding.btnSignIn.background as? TransitionDrawable
                        transitionDrawable?.startTransition(500)
                        binding.btnSignIn.isEnabled = true
                        val snackbar = Snackbar.make(requireView(), "인증에 성공했습니다 !", Snackbar.LENGTH_SHORT)
                        snackbar.setAction("확인", object: OnClickListener{
                            override fun onClick(v: View?) {
                                snackbar.dismiss()
                            }
                        })
                        snackbar.setActionTextColor(Color.WHITE)
                        snackbar.show()
                    }
                } else {
                    Log.w(TAG, "mobile auth failed.")
                }
            }
    }

    private fun showBottomSheetDialog(context: FragmentActivity) {
        val bottomsheetfragment = SignInBottomSheetDialogFragment()
        bottomsheetfragment.setOnCarrierSelectedListener(object : SignInBottomSheetDialogFragment.onTelecomSelectedListener {
            override fun onTelecomSelected(telecom: String) {
                binding.tvTelecom.text = telecom
            }
        })
        val fragmentManager = context.supportFragmentManager
        bottomsheetfragment.show(fragmentManager, bottomsheetfragment.tag)
    }

    override fun onDestroy() {
        super.onDestroy()
        val firebaseAuth = Firebase.auth
        if (firebaseAuth.currentUser != null) {
            Firebase.auth.signOut()
        }
    }
}