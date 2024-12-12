package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context.INPUT_METHOD_SERVICE
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import com.tangoplus.tangoq.databinding.FragmentFindAccountDialogBinding
import com.tangoplus.tangoq.dialog.bottomsheet.SignInBSDialogFragment
import com.tangoplus.tangoq.`object`.NetworkUser.findUserId
//import com.tangoplus.tangoq.`object`.NetworkUser.verifyBeforeResetPw
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern


class FindAccountDialogFragment : DialogFragment() {
    lateinit var binding : FragmentFindAccountDialogBinding
    private lateinit var auth : FirebaseAuth
    val svm : SignInViewModel by viewModels()
    var verifyId = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFindAccountDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        removeAuthInstance()
    }

    override fun onStop() {
        super.onStop()
        removeAuthInstance()
    }


    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ------# 초기 세팅 #------
        binding.clFADId.visibility = View.GONE
        binding.clFADIdResult.visibility = View.GONE
        binding.clFADResetPassword.visibility = View.GONE
        auth = FirebaseAuth.getInstance()
        auth.setLanguageCode("kr")
        binding.btnFADAuthSend.isEnabled = false

        // ------! 탭으로 아이디 비밀번호 레이아웃 나누기 시작 !------
        binding.tlFAD.addOnTabSelectedListener(object : OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val imm = context?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
                imm?.hideSoftInputFromWindow(view.windowToken, 0)
                svm.mobileCondition.value = false
                svm.mobileAuthCondition.value = false
                binding.etFADMobile.isEnabled = true
                binding.etFADAuthNumber.isEnabled = false
                binding.btnFADAuthSend.isEnabled = false
                binding.btnFADConfirm.text = "인증 하기"
                binding.tvFADTelecom.text = ""
                binding.etFADMobile.setText("")
                binding.etFADId.setText("")

                removeAuthInstance() // 파이어베이스 인증 상태 제거
                when(tab?.position) {
                    0 -> {
                        binding.clFADMobile.visibility = View.VISIBLE
                        binding.clFADId.visibility = View.GONE
                        binding.clFADIdResult.visibility = View.GONE
                        binding.clFADResetPassword.visibility = View.GONE
                        binding.btnFADConfirm.isEnabled = false
                        svm.isFindId = true

                    }
                    1 -> {
                        binding.clFADMobile.visibility = View.VISIBLE
                        binding.clFADId.visibility = View.VISIBLE
                        binding.clFADIdResult.visibility = View.GONE
                        binding.clFADResetPassword.visibility = View.GONE
                        binding.btnFADConfirm.isEnabled = false
                        binding.etFADAuthNumber.text = null
                        binding.etFADMobile.text = null
                        svm.isFindId = false
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        // ------! 비밀번호 재설정 시작 !------

        // ------! 인증 문자 확인 시작 !------
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                Log.v("verifyComplete", "PhoneAuthCredential: $p0")
            }
            override fun onVerificationFailed(p0: FirebaseException) {
                Log.e("failedAuth", "$p0")
            }
            @RequiresApi(Build.VERSION_CODES.P)
            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                super.onCodeSent(verificationId, token)
                verifyId = verificationId
                Log.v("onCodeSent", "메시지 발송 성공, verificationId: ${verificationId} ,token: ${token}")
                // -----! 메시지 발송에 성공하면 스낵바 호출 !------
                Snackbar.make(requireView(), "메시지 발송에 성공했습니다. 잠시만 기다려주세요", Toast.LENGTH_LONG).show()
                binding.btnFADConfirm.isEnabled = true

            }
        }
        // ------! 인증 문자 확인 끝 !------

        // ------! 핸드폰 번호 - 시작 !------
        val mobilePattern = "^010-\\d{4}-\\d{4}\$"
        val mobilePatternCheck = Pattern.compile(mobilePattern)
        binding.etFADMobile.addTextChangedListener(object: TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                isFormatting = true
                val cleaned =s.toString().replace("-", "")
                when {
                    cleaned.length <= 3 -> s?.replace(0, s.length, cleaned)
                    cleaned.length <= 7 -> s?.replace(0, s.length, "${cleaned.substring(0, 3)}-${cleaned.substring(3)}")
                    else -> s?.replace(0, s.length, "${cleaned.substring(0, 3)}-${cleaned.substring(3, 7)}-${cleaned.substring(7)}")
                }
                isFormatting = false
                Log.w("전화번호형식", "${mobilePatternCheck.matcher(binding.etFADMobile.text.toString()).find()}")
                svm.mobileCondition.value = mobilePatternCheck.matcher(binding.etFADMobile.text.toString()).find()
                if (svm.mobileCondition.value == true) {
                    svm.User.value?.put("user_mobile", s.toString() )
                    binding.btnFADAuthSend.isEnabled = true
                }

            }
        }) // ------! 핸드폰 번호 - 시작 !------

        binding.btnFADAuthSend.setOnClickListener {
            var transformMobile = phoneNumber82(binding.etFADMobile.text.toString())
            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("📩 문자 인증 ")
                .setMessage("$transformMobile 로 인증 하시겠습니까?")
                .setPositiveButton("예") { _, _ ->
                    transformMobile = transformMobile.replace("-", "")
                    Log.w("전화번호", transformMobile)

                    val optionsCompat = PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(transformMobile)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(requireActivity())
                        .setCallbacks(callbacks)
                        .build()
                    PhoneAuthProvider.verifyPhoneNumber(optionsCompat)
                    auth.setLanguageCode("kr")

                    val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
                    alphaAnimation.duration = 600
                    binding.etFADAuthNumber.isEnabled = true
                    binding.etFADAuthNumber.requestFocus()
                }
                .setNegativeButton("아니오", null)
                .show()

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
        }

        binding.btnFADConfirm.setOnClickListener{
            when (binding.btnFADConfirm.text) {
                "인증 하기" -> {
                    val credential = PhoneAuthProvider.getCredential(verifyId, binding.etFADAuthNumber.text.toString())
                    signInWithPhoneAuthCredential(credential)
                }
                "아이디 찾기" -> {
                    binding.clFADMobile.visibility = View.GONE
                    val jo = JSONObject().apply {
                        put("mobile", binding.etFADMobile.text.toString().replace("-", ""))
                        put("mobile_check", if (svm.mobileAuthCondition.value == true) "checked" else "nonChecked")
                    }
                    Log.v("찾기>핸드폰번호", "$jo")
                    findUserId(requireContext(), getString(R.string.API_user), jo.toString()) { resultString ->
                        if (resultString == "") {
                            requireActivity().runOnUiThread {
                                val dialog = AlertDialog.Builder(requireContext())
                                    .setTitle("알림⚠️")
                                    .setMessage("일치하는 계정이 없습니다.\n다시 시도해주세요")
                                    .setPositiveButton("예") { _, _ -> }
                                    .show()
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
                                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
                            }
                        } else {
                            requireActivity().runOnUiThread{
                                binding.clFADMobile.visibility = View.GONE
                                binding.clFADId.visibility = View.GONE
                                binding.clFADIdResult.visibility = View.VISIBLE
                                val maskedString = resultString.mapIndexed { index, char ->
                                    if (index % 2 == 0) '*' else char
                                }.joinToString("")
                                binding.tvFADIdFinded.text = maskedString
                            }
                        }
                    }
                    binding.btnFADConfirm.text= "초기 화면으로"
                }

                "인증 하기" -> {
                    val credential = PhoneAuthProvider.getCredential(verifyId, binding.etFADAuthNumber.text.toString())
                    signInWithPhoneAuthCredential(credential)
                }
                "비밀번호 재설정" -> {
//                    val jo = JSONObject().apply {
//                        put("user_id", binding.etFADId.text)
//                        put("mobile", binding.etFADMobile.text.toString().replace("-", ""))
//                    }
//                    verifyBeforeResetPw(getString(R.string.API_user), jo.toString()) { result ->
//                        when (result) {
//                            true -> {
//
//                            }
//                            false -> {
//
//                            }
//                        }
//
//                    }

                    // ------! 비밀번호를 찾기를 하면 아이디와 핸드폰 번호를 맞혀서 일치한다는 번호만 있으면 재설정하는 update하기.
//                    val email = when (binding.FADSpinner.selectedItemPosition) {
//                        0, 1, 2 -> {
//                            "${binding.etFADEmailId.text}@${binding.FADSpinner.selectedItem as String}"
//
//                        }
//                        else -> {
//                            "${binding.etFADEmailId.text}@${binding.etFADEmail.text}"
//                        }
//                    }
//                    if (binding.etFADEmailId.text.length != 0) {
//                        firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener{ task ->
//                            if (task.isSuccessful) {
//                                binding.tvFADPw.visibility = View.VISIBLE
//                                binding.clFADPw.visibility = View.GONE
//                                binding.btnFADConfirm.text = "초기 화면으로"
//                            }
//                        }
//                    }
                }
                "초기 화면으로" -> {
                    dismiss()
                }
            }
        }
        // ------! 비밀번호 재설정 끝 !------

//        val domainList = listOf("gmail.com", "naver.com", "kakao.com", "직접입력")
//        binding.FADSpinner.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, domainList)
//        binding.FADSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
//            @SuppressLint("SetTextI18n")
//            override fun onItemSelected(
//                parent: AdapterView<*>?,
//                view: View?,
//                position: Int,
//                id: Long
//            ) {
//                binding.FADSpinner.getItemAtPosition(position).toString()
//                if (position == 3) {
//                    binding.etFADEmail.visibility = View.VISIBLE
//                    binding.FADSpinner.visibility = View.GONE
//                    binding.ivFADSpinner.setOnClickListener{
//                        binding.FADSpinner.performClick()
//                        binding.FADSpinner.visibility = View.VISIBLE
//                    }
//                } else {
//                    binding.etFADEmail.visibility = View.GONE
//                    binding.etFADEmail.setText("")
//                    binding.FADSpinner.visibility = View.VISIBLE
//                }
//            }
//            override fun onNothingSelected(parent: AdapterView<*>?) {}
//        }
        binding.tvFADTelecom.setOnClickListener {
            showTelecomBottomSheetDialog(requireActivity())
        } // -----! 통신사 선택 끝 !-----

    }

    private fun showTelecomBottomSheetDialog(context: FragmentActivity) {
        val bottomsheetfragment = SignInBSDialogFragment()
        bottomsheetfragment.setOnCarrierSelectedListener(object : SignInBSDialogFragment.OnTelecomSelectedListener {
            override fun onTelecomSelected(telecom: String) {
                binding.tvFADTelecom.text = telecom
            }
        })
        val fragmentManager = context.supportFragmentManager
        bottomsheetfragment.show(fragmentManager, bottomsheetfragment.tag)
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.P)
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    requireActivity().runOnUiThread {
                        svm.mobileAuthCondition.value = true
                        binding.etFADAuthNumber.isEnabled = false
                        binding.etFADMobile.isEnabled = false

                        // ------! 번호 인증 완료 !------

                        val snackbar = Snackbar.make(requireView(), "인증에 성공했습니다 !", Snackbar.LENGTH_SHORT)
                        snackbar.setAction("확인") { snackbar.dismiss() }
                        snackbar.setActionTextColor(Color.WHITE)
                        snackbar.show()
                        binding.btnFADConfirm.text = if (svm.isFindId) "아이디 찾기" else "비밀번호 재설정"
                    }
                } else {
                    Log.w(ContentValues.TAG, "mobile auth failed.")
                }
            }
    }

    private fun phoneNumber82(msg: String) : String {
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
    private fun removeAuthInstance() {
        FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                FirebaseAuth.getInstance().signOut()
                val user = FirebaseAuth.getInstance().currentUser
                Log.v("user", "$user")
                user?.delete()
            }
        }
        auth.signOut()
    }
    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

}