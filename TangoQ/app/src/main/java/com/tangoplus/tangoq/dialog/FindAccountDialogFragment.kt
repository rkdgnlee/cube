package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.res.ColorStateList
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.api.NetworkUser.fetchUserUPDATEJson
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import com.tangoplus.tangoq.databinding.FragmentFindAccountDialogBinding
import com.tangoplus.tangoq.api.NetworkUser.findUserId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        binding.tlFAD.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val imm = context?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
                imm?.hideSoftInputFromWindow(view.windowToken, 0)
                svm.mobileCondition.value = false
                svm.mobileAuthCondition.value = false
                binding.etFADMobile.isEnabled = true
                binding.etFADAuthNumber.isEnabled = false
                binding.btnFADAuthSend.isEnabled = false
                binding.btnFADConfirm.text = "인증 하기"
                binding.etFADMobile.setText("")
                binding.etFADId.setText("")

                removeAuthInstance() // 파이어베이스 인증 상태 제거
                when(tab?.position) {
                    0 -> {
                        svm.pwBothTrue.removeObservers(viewLifecycleOwner)
                        binding.clFADMobile.visibility = View.VISIBLE
                        binding.clFADId.visibility = View.GONE
                        binding.clFADIdResult.visibility = View.GONE
                        binding.clFADResetPassword.visibility = View.GONE
                        binding.btnFADConfirm.isEnabled = false
                        svm.isFindId = true

                    }
                    1 -> {
                        // ------# 비밀번호 확인 여부 체크 #------
                        svm.pwBothTrue.observe(viewLifecycleOwner) {
                            binding.btnFADConfirm.isEnabled = it
                            if (it) {
                                enabledButton()
                                svm.pw.value = binding.etFADResetPassword.text.toString()
                            } else {
                                disabledButton()
                                svm.pw.value = ""
                            }
                        }

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
                Log.v("onCodeSent", "메시지 발송 성공, verificationId: $verificationId ,token: $token")
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

        // 비밀번호 재설정 patternCheck
        val pwPattern = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[$@$!%*#?&^])[A-Za-z[0-9]$@$!%*#?&^]{8,20}$" // 영문, 특수문자, 숫자 8 ~ 20자 패턴
        val pwPatternCheck = Pattern.compile(pwPattern)
        binding.etFADResetPassword.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                svm.pwCondition.value = pwPatternCheck.matcher(binding.etFADResetPassword.text.toString()).find()
                if (svm.pwCondition.value == true) {
                    binding.tvFADPWCondition.setTextColor(binding.tvFADPWCondition.resources.getColor(R.color.subColor400, null))
                    binding.tvFADPWCondition.text = "사용 가능합니다"
                } else {
                    binding.tvFADPWCondition.setTextColor(binding.tvFADPWCondition.resources.getColor(R.color.deleteColor, null))
                    binding.tvFADPWCondition.text = "영문, 숫자, 특수문자( ! @ # $ % ^ & * ?)를 모두 포함해서 8~20자리를 입력해주세요"
                }
            }
        })

        binding.etFADResetPasswordConfirm.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                svm.pwCompare.value = (binding.etFADResetPassword.text.toString() == binding.etFADResetPasswordConfirm.text.toString())
                if (svm.pwCompare.value == true) {
                    binding.tvFADPWVerifyCondition.setTextColor(binding.tvFADPWVerifyCondition.resources.getColor(R.color.subColor400, null))
                    binding.tvFADPWVerifyCondition.text = "일치합니다"
                } else {
                    binding.tvFADPWVerifyCondition.setTextColor(binding.tvFADPWVerifyCondition.resources.getColor(R.color.deleteColor, null))
                    binding.tvFADPWVerifyCondition.text = "일치하지 않습니다"
                }
            }
        })
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

                "비밀번호 재설정" -> {
//                    val jo = JSONObject().apply {
//                        put("password", svm.pw.value)
//                    }
//                    // TODO 여기서 아이디가 진짜 있는지에 대한 검증이 필요함 -> 검증에 대한 API에는 전화번호와 아이디가 매치 했을 때 맞을 경우 userSn만 보내줌.
//                    lifecycleScope.launch(Dispatchers.IO) {
//                        val isUpdateFinished = fetchUserUPDATEJson(requireContext(), getString(R.string.API_user), jo.toString(), userJson.optInt("sn").toString())
//                        if (isUpdateFinished == true) {
//                            withContext(Dispatchers.Main) {
//                                if (isAdded) {
//                                    dismiss()
//                                }
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


                        if (svm.isFindId) {
                            binding.btnFADConfirm.text = "아이디 찾기"
                        } else {
                            // TODO 여기서 바로 전화번호와 아이디가 맞는지에 대한 값을 보내줌. -> 바로 검증 실행




//                            // snack bar 내용을 확인하고
//                            val snackbar = Snackbar.make(requireView(), "인증에 성공했습니다 !", Snackbar.LENGTH_SHORT)
//                            snackbar.setAction("확인") { snackbar.dismiss() }
//                            snackbar.setActionTextColor(Color.WHITE)
//                            snackbar.show()
                            // 인증에 실패했을 경우 dialogBuilder를 통해 알리고 다시 시작하게 끔 하기
                            MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                                setTitle("알림")
                                setMessage("존재하지 않는 ID입니다. 다시 확인해주세요")
                                setPositiveButton("예") { _, _ ->
                                    removeAuthInstance()
                                    dismiss()
                                }
                            }.show()
                        }
                        binding.btnFADConfirm.text = if (svm.isFindId) "아이디 찾기" else "비밀번호 재설정"
                    }
                } else {
                    Log.e(ContentValues.TAG, "mobile auth failed.")
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
    private fun enabledButton() {
        binding.btnFADConfirm.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
        binding.btnFADConfirm.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white)))
        binding.btnFADConfirm.isEnabled = true
    }
    private fun disabledButton() {
        binding.btnFADConfirm.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor150))
        binding.btnFADConfirm.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400)))
        binding.btnFADConfirm.isEnabled = false
    }
}