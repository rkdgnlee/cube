package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.tangoplus.tangoq.IntroActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.api.NetworkUser.findUserEmail
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import com.tangoplus.tangoq.databinding.FragmentFindAccountDialogBinding
import com.tangoplus.tangoq.api.NetworkUser.resetPW
import com.tangoplus.tangoq.api.NetworkUser.sendPWCode
import com.tangoplus.tangoq.api.NetworkUser.verifyPWCode
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.function.SecurePreferencesManager.encrypt
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern


class FindAccountDialogFragment : DialogFragment() {
    lateinit var binding : FragmentFindAccountDialogBinding
    private var loadingDialog : LoadingDialogFragment? = null
    val svm : SignInViewModel by viewModels()
    var verifyId = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFindAccountDialogBinding.inflate(inflater)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // api35이상 화면 크기 조절
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 상태 표시줄 높이만큼 상단 패딩 적용
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ------# 초기 세팅 #------
        loadingDialog = LoadingDialogFragment.newInstance("회원가입전송")
        binding.ibtnFADBack.setOnSingleClickListener { dismiss() }
        binding.clFADIdResult.visibility = View.GONE
        binding.clFADResetPassword.visibility = View.GONE

        binding.btnFADAuthSend.isEnabled = false
        svm.isFindEmail.observe(viewLifecycleOwner) {
            Log.v("currentState", "${svm.isFindEmail.value}")
            setTextWatcher() // editText(auth) textWatcher 변경
        }
        setMobileAuth()
        // ------! 탭으로 아이디 비밀번호 레이아웃 나누기 시작 !------
        binding.tlFAD.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val imm = context?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
                imm?.hideSoftInputFromWindow(view.windowToken, 0)
                svm.mobileCondition.value = false
                svm.mobileAuthCondition.value = false

                binding.btnFADAuthSend.isEnabled = false
                binding.btnFADConfirm.text = "인증 하기"


                svm.pwBothTrue.removeObservers(viewLifecycleOwner)

                when(tab?.position) {
                    0 -> {
                        svm.pwBothTrue.removeObservers(viewLifecycleOwner)
                        setMobileAuth()
                    }
                    1 -> {
                        setEmailAuth()
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.btnFADAuthSend.setOnSingleClickListener {
            when (binding.btnFADAuthSend.text) {
                "휴대폰 인증" -> {
                    // TODO 회원가입과 동일하게 휴대폰 인증을 여기다가 넣음.

                }
                "이메일 인증" -> {
                    sendAuthCode()
                }
            }
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
                    binding.tvFADPWCondition.setTextColor(binding.tvFADPWCondition.resources.getColor(R.color.mainColor, null))
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
                    binding.tvFADPWVerifyCondition.setTextColor(binding.tvFADPWVerifyCondition.resources.getColor(R.color.mainColor, null))
                    binding.tvFADPWVerifyCondition.text = "일치합니다"
                } else {
                    binding.tvFADPWVerifyCondition.setTextColor(binding.tvFADPWVerifyCondition.resources.getColor(R.color.deleteColor, null))
                    binding.tvFADPWVerifyCondition.text = "일치하지 않습니다"
                }
            }
        })

        binding.btnFADConfirm.setOnSingleClickListener{
            when (binding.btnFADConfirm.text) {
                "아이디 찾기" -> {

                    // TODO 이곳에서 휴대폰 인증 결과에 맞게 아이디 찾기 사용해야함.
                    binding.clFADAuth.visibility = View.GONE
                    binding.textView128.visibility = View.VISIBLE
                    binding.textView129.visibility = View.VISIBLE
                    val jo = JSONObject().apply {
                        put("mobile", binding.etFADAuth.text.toString().replace("-", ""))
                        put("mobile_check", if (svm.mobileAuthCondition.value == true) "checked" else "nonChecked")
                    }
//                    Log.v("찾기>핸드폰번호", "$jo")
                    findUserEmail(getString(R.string.API_user), jo.toString()) { resultString ->
                        if (resultString == "") {
                            requireActivity().runOnUiThread {
                                val dialog = AlertDialog.Builder(requireContext())
                                    .setTitle("알림⚠️")
                                    .setMessage("일치하는 계정이 없습니다.\n다시 시도해주세요")
                                    .setPositiveButton("예") { _, _ -> }
                                    .show()
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
                                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK)

                                binding.clFADIdResult.visibility = View.VISIBLE
                                binding.textView128.visibility = View.INVISIBLE
                                binding.textView129.visibility = View.INVISIBLE
                                binding.tvFADFindedEmail.text = "일치하는 계정이 없습니다"

                            }
                        } else {
                            requireActivity().runOnUiThread{
                                binding.clFADAuth.visibility = View.GONE
                                binding.clFADIdResult.visibility = View.VISIBLE
                                val maskedString = resultString.mapIndexed { index, char ->
                                    if (index % 2 == 0) '*' else char
                                }.joinToString("")
                                binding.tvFADFindedEmail.text = maskedString
                            }
                        }
                    }
                    binding.btnFADConfirm.text= "초기 화면으로"
                }
                "이메일 인증 확인" -> {
                    val bodyJo = JSONObject().apply {
                        put("email", svm.saveEmail)
                        put("otp", binding.etFADAuthNumber.text)
                    }
                    Log.v("body", "$bodyJo")
                    verifyPWCode(getString(R.string.API_user), bodyJo.toString()) { jo ->
                        if (jo != null) {
                            val code = jo.optInt("status")
                            when (code) {
                                200 -> {
                                    svm.resetJwt = jo.optString("jwt_for_pwd")
                                    binding.clFADResetPassword.visibility = View.VISIBLE
                                    binding.btnFADConfirm.text = "비밀번호 재설정"
                                    binding.clFADAuth.visibility = View.GONE

                                    // 비밀번호 재설정 창이 나오고 일치 여부 observe
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
                                }
                                400 -> {
                                    Toast.makeText(requireContext(), "올바르지 않은 요청입니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                                }
                                401 -> {
                                    Toast.makeText(requireContext(), "만료된 인증번호 입니다. 인증을 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                                    binding.etFADAuth.isEnabled = true
                                    binding.etFADAuthNumber.isEnabled = true
                                    binding.etFADAuthNumber.setText("")
                                    binding.btnFADAuthSend.isEnabled = true
                                    binding.btnFADConfirm.isEnabled = true
                                }
                                else -> {
                                    Toast.makeText(requireContext(), "서버 에러입니다. 관리자에게 문의해주세요", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Log.e("failed Verified", "failed To VerifiedCode")
                            Toast.makeText(requireContext(), "인증에 실패했습니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                "비밀번호 재설정" -> {
                    val encryptePW = encrypt(svm.pw.value ?: "", getString(R.string.secret_key), getString(R.string.secret_iv))
                    val bodyJo = JSONObject().apply {
                        put("email", svm.saveEmail)
                        put("new_password", encryptePW)
                    }
                    resetPW(getString(R.string.API_user), svm.resetJwt, bodyJo.toString()) { code ->
                        when (code) {
                            200 -> {
                                val intent = Intent(requireActivity(), IntroActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                intent.putExtra("SignInFinished", 202)
                                dismiss()
                                startActivity(intent)
                            }
                            400 -> Toast.makeText(requireContext(), "올바르지 않은 요청입니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                            else -> {
                                Toast.makeText(requireContext(), "서버 에러입니다. 관리자에게 문의해주세요", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                "초기 화면으로" -> {
                    dismiss()
                }
            }
        }
        // ------! 비밀번호 재설정 끝 !------
    }



    private fun setTextWatcher() {
        binding.etFADAuth.removeTextChangedListener(svm.textWatcher)
        svm.textWatcher = when (svm.isFindEmail.value) {
            true -> {
                null
            }
            false -> {
                val emailPattern = "^[a-zA-Z0-9_+.-]{4,20}@([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}\$"
                val emailPatternCheck = Pattern.compile(emailPattern)
                object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        svm.emailCondition.value = emailPatternCheck.matcher(binding.etFADAuth.text.toString()).find()
                        if (svm.emailCondition.value == true) {
                            enabledSendButton()
                        } else {
                            disabledSendButton()
                        }
                    }
                }
            }
            null -> null
        }
        svm.textWatcher?.let { binding.etFADAuth.addTextChangedListener(it) }
    }

    private fun setVerifyCountDown(retryAfter: Int) {
        if (retryAfter != -1) {
            object : CountDownTimer((retryAfter * 1000).toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val remainingSeconds = millisUntilFinished / 1000
                    val minutes = remainingSeconds / 60
                    val seconds = remainingSeconds % 60
                    binding.tvFADCountDown.visibility = View.VISIBLE
                    binding.tvFADCountDown.text = "남은 시간: ${minutes}분 ${seconds}초"
                }
                override fun onFinish() {
                    binding.tvFADCountDown.visibility = View.INVISIBLE
                }
            }.start()
        }
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
    private fun enabledSendButton() {
        binding.btnFADAuthSend.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
        binding.btnFADAuthSend.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white)))
        binding.btnFADAuthSend.isEnabled = true
    }
    private fun disabledSendButton() {
        binding.btnFADAuthSend.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor150))
        binding.btnFADAuthSend.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400)))
        binding.btnFADAuthSend.isEnabled = false
    }
    private fun setMobileAuth() {
        binding.btnFADAuthSend.text = "휴대폰 인증"
        binding.etFADAuth.apply {
            isEnabled = false
            hint = "휴대폰을 인증해주세요"
            backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
            setText("")
        }
        binding.btnFADConfirm.visibility = View.GONE
        enabledSendButton()
        // 인증번호 창 없앰
        binding.etFADAuthNumber.visibility = View.GONE

        // 결과 UI 제거
        binding.clFADIdResult.visibility = View.GONE
        binding.clFADResetPassword.visibility = View.GONE
        binding.btnFADConfirm.isEnabled = false
        svm.isFindEmail.value = true
    }

    private fun setEmailAuth () {
        binding.tvFADAuth.text = "이메일 인증"
        binding.etFADAuth.apply {
            hint = "이메일을 입력해주세요"
            inputType = InputType.TYPE_CLASS_TEXT
            isEnabled = true
            setText("")
            backgroundTintList = null
        }
        binding.btnFADAuthSend.apply {
            text = "이메일 인증"
        }
        disabledSendButton()
        binding.btnFADConfirm.apply {
            isEnabled = false
            text = "이메일 인증 확인"
        }

        binding.clFADIdResult.visibility = View.GONE
        binding.clFADResetPassword.visibility = View.GONE

        binding.etFADAuthNumber.text = null
        svm.isFindEmail.value = false
    }

    private fun sendAuthCode() {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setTitle("이메일 인증 ")
            setMessage("${binding.etFADAuth.text} 로 인증번호를 전송 하시겠습니까?")
            setPositiveButton("예") { _, _ ->
                if (dialog?.isShowing == true) {
                    loadingDialog?.show(requireActivity().supportFragmentManager, "LoadingDialogFragment")
                }

                svm.saveEmail = binding.etFADAuth.text.toString()
                val jo = JSONObject().apply {
                    put("email", svm.saveEmail)
                }
                disabledSendButton()
                sendPWCode(getString(R.string.API_user), jo.toString()) { code ->

                    // 전송 로딩 창 완료
                    if (dialog?.isShowing == true) {
                        loadingDialog?.dismiss()
                    }

                    // 결과 처리
                    if (code in listOf(200, 201)) {
                        Toast.makeText(requireContext(), "인증번호를 이메일로 전송했습니다.", Toast.LENGTH_SHORT).show()
                        // 재전송안내 문구 세팅
                        binding.tvFADReAuth.visibility = View.VISIBLE
                        setVerifyCountDown(120)

                        // 버튼 활성화
                        binding.etFADAuthNumber.apply {
                            visibility = View.VISIBLE
                            isEnabled = true
                        }
                        binding.btnFADConfirm.apply {
                            visibility = View.VISIBLE
                            isEnabled = true
                        }
                    } else {
                        Toast.makeText(requireContext(), "이메일이 올바르지 않습니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                    }

                }
            }
            setNegativeButton("아니오", null)
            show()
        }

    }
}