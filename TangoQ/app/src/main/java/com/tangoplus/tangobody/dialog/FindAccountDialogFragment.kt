package com.tangoplus.tangobody.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.tangoplus.tangobody.R
import com.tangoplus.tangobody.viewmodel.SignInViewModel
import com.tangoplus.tangobody.databinding.FragmentFindAccountDialogBinding
import com.tangoplus.tangobody.api.NetworkUser.resetPW
import com.tangoplus.tangobody.api.NetworkUser.sendPWCode
import com.tangoplus.tangobody.api.NetworkUser.verifyPWCode
import com.tangoplus.tangobody.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangobody.function.AuthManager.pwPatternCheck
import com.tangoplus.tangobody.function.AuthManager.setRetryAuthMessage
import com.tangoplus.tangobody.function.AuthManager.setVerifyCountDown
import com.tangoplus.tangobody.function.SecurePreferencesManager.encrypt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.regex.Pattern
import androidx.core.graphics.drawable.toDrawable
import com.tangoplus.tangobody.api.NetworkUser.sendMobileOTPToFindEmail
import com.tangoplus.tangobody.api.NetworkUser.verityMobileOTPToFindEmail
import com.tangoplus.tangobody.function.AuthManager.maskedProfileData


class FindAccountDialogFragment : DialogFragment() {
    lateinit var binding : FragmentFindAccountDialogBinding
    private var loadingDialog : LoadingDialogFragment? = null
    val svm : SignInViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
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
        binding.ibtnFADBack.setOnSingleClickListener { showExitDialog() }
        binding.clFADIdResult.visibility = View.GONE
        binding.clFADResetPassword.visibility = View.GONE
        binding.btnFADAuthSend.isEnabled = false

        setMobileAuth()
        svm.fullEmail.value = ""
        svm.passMobile.value = ""
        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        binding.etFADAuth.postDelayed({
            binding.etFADAuth.requestFocus()
            imm.showSoftInput(binding.etFADAuth, InputMethodManager.SHOW_IMPLICIT)
        }, 250)

        binding.etFADAuth.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                if (svm.isFindEmail.value == true) {
                    if (svm.mobileCondition.value == true) {
                        sendMobileCode()
                    }
                } else {
                    if (svm.emailCondition.value == true) {
                        sendEmailCode()
                    }
                }
                true  // 이벤트 처리가 완료되었음을 반환
            } else {
                false // 다른 동작들은 그대로 유지
            }
        }


        svm.isFindEmail.observe(viewLifecycleOwner) {
            setTextWatcher() // editText(auth) textWatcher 변경
        }
        // ------! 탭으로 아이디 비밀번호 레이아웃 나누기 시작 !------
        binding.tlFAD.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
//                val imm = context?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
//                imm?.hideSoftInputFromWindow(view.windowToken, 0)
                svm.mobileCondition.value = false
                svm.mobileAuthCondition.value = false

                binding.btnFADAuthSend.isEnabled = false
                binding.btnFADConfirm.text = "인증 하기"
                binding.tvFADReAuth.visibility = View.GONE
                binding.tvFADCountDown.visibility = View.GONE
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

                binding.etFADAuth.postDelayed({
                    binding.etFADAuth.requestFocus()
                    imm.showSoftInput(binding.etFADAuth, InputMethodManager.SHOW_IMPLICIT)
                }, 250)

                // 레이아웃 초기화
                binding.clFADAuth.visibility = View.VISIBLE
                binding.clFADIdResult.visibility = View.GONE
                binding.clFADResetPassword.visibility = View.GONE

                // 동작과 input 초기화
                svm.countDownTimer?.cancel()
                enabledInputData()
                binding.etFADAuthNumber.visibility = View.GONE
                binding.tvFADReAuth.visibility = View.GONE
                binding.btnFADConfirm.visibility = View.GONE
                svm.passMobile.value = ""
                svm.fullEmail.value = ""
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.btnFADAuthSend.setOnSingleClickListener {
            when (binding.btnFADAuthSend.text) {
                "휴대폰 인증" -> { sendMobileCode() }
                "이메일 인증" -> { sendEmailCode() }
            }
        }

        binding.etFADAuthNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 6) {
                    when (svm.isFindEmail.value) {
                        true -> { verifyMobileCode() }
                        false -> { verifyEmailCode() }
                        null -> {}
                    }
                }
            }
        })

        // 비밀번호 재설정 patternCheck
        val pwTextWatcher = pwPatternCheck(svm, binding.etFADResetPassword, binding.tvFADPWCondition, false)
        binding.etFADResetPassword.addTextChangedListener(pwTextWatcher)

        val pwRepeatTextWatcher = pwPatternCheck(svm, binding.etFADResetPasswordConfirm, binding.tvFADPWVerifyCondition, true)
        binding.etFADResetPasswordConfirm.addTextChangedListener(pwRepeatTextWatcher)

        binding.btnFADConfirm.setOnSingleClickListener{
            when (binding.btnFADConfirm.text) {
                "비밀번호 재설정" -> {
                    val encryptePW = encrypt(svm.pw.value ?: "", getString(R.string.secret_key), getString(R.string.secret_iv))
                    val bodyJo = JSONObject().apply {
                        put("email", svm.saveEmail)
                        put("new_password", encryptePW)
                    }
                    resetPW(getString(R.string.API_user), svm.resetJwt, bodyJo.toString()) { code ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            when (code) {
                                200 -> {
                                    Toast.makeText(requireContext(), "비밀번호 변경이 완료됐습니다. 로그인해주세요 !", Toast.LENGTH_SHORT).show()
                                    dismiss()
                                }
                                400 -> Toast.makeText(requireContext(), "올바르지 않은 요청입니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                                else -> {
                                    Toast.makeText(requireContext(), "서버 에러입니다. 관리자에게 문의해주세요", Toast.LENGTH_SHORT).show()
                                }
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

                val mobilePattern = "^010-\\d{4}-\\d{4}\$"
                val mobilePatternCheck = Pattern.compile(mobilePattern)
                object: TextWatcher {
                    private var isFormatting = false
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {

                        if (isFormatting) return
                        isFormatting = true
                        val cleaned = s.toString().replace("-", "")
                        val maxDigits = 11
                        val limited = if (cleaned.length > maxDigits) cleaned.substring(0, maxDigits) else cleaned


                        val formatted = when {
                            limited.length <= 3 -> limited
                            limited.length <= 7 -> "${limited.substring(0, 3)}-${limited.substring(3)}"
                            else -> "${limited.substring(0, 3)}-${limited.substring(3, 7)}-${limited.substring(7)}"
                        }

                        // 기존 입력과 다를 때만 업데이트
                        if (s.toString() != formatted) {
                            binding.etFADAuth.setText(formatted) // setText를 사용하여 확실하게 변경
                            binding.etFADAuth.setSelection(formatted.length) // 커서를 마지막 위치로 이동
                        }

                        isFormatting = false
                        svm.mobileCondition.value = mobilePatternCheck.matcher(binding.etFADAuth.text.toString()).find()
                        if (svm.mobileCondition.value == true)  {
                            svm.passMobile.value = s.toString().substring(0, 13)
                            enabledSendButton()
                        } else {
                            svm.passMobile.value = ""
                            disabledSendButton()
                        }
                    }
                }
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
                            svm.fullEmail.value = binding.etFADAuth.text.toString()
                            enabledSendButton()
                        } else {
                            svm.fullEmail.value = ""
                            disabledSendButton()
                        }
                    }
                }
            }
            null -> null
        }
        svm.textWatcher?.let { binding.etFADAuth.addTextChangedListener(it) }
        Log.v("텍스트워처 세팅", "${svm.isFindEmail.value}, ${svm.textWatcher}")
    }

    private fun setMobileAuth() {
        binding.tvFADAuth.text = "휴대폰 인증"
        binding.btnFADAuthSend.text = "휴대폰 인증"
        binding.etFADAuth.apply {
            hint = "휴대폰 번호를 입력해주세요"
            setText("")
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        binding.btnFADConfirm.visibility = View.GONE
        disabledSendButton()
        // 인증번호 창 없앰
        binding.etFADAuthNumber.visibility = View.GONE

        // 결과 UI 제거
        binding.clFADIdResult.visibility = View.GONE
        binding.clFADResetPassword.visibility = View.GONE
        binding.btnFADConfirm.isEnabled = false
        svm.isFindEmail.value = true
    }

    private fun sendMobileCode() {
        disabledInputData()
        val configureMobile = svm.passMobile.value?.replace("-", "")
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setTitle("휴대폰 인증")
            setMessage("${svm.passMobile.value}로 인증하시겠습니까?")
            setPositiveButton("예") { _, _ ->
                val bodyJo = JSONObject().apply {
                    put("mobile", configureMobile)
                }
                lifecycleScope.launch(Dispatchers.IO) {
                    val statusCode = sendMobileOTPToFindEmail(getString(R.string.API_user), bodyJo.toString())
                    withContext(Dispatchers.Main) {
                        when (statusCode) {
                            1 -> {
                                Toast.makeText(
                                    requireContext(),
                                    "인증번호를 전송했습니다. 휴대폰을 확인해주세요",
                                    Toast.LENGTH_SHORT
                                ).show()
                                enabledInputCode()
                                disabledSendButton()
                                // 카운팅
                                binding.etFADAuthNumber.requestFocus()
                                binding.tvFADReAuth.visibility = View.VISIBLE
                                setReSendMessage()
                                binding.etFADAuthNumber.postDelayed({
                                    val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                                    imm.showSoftInput(binding.etFADAuthNumber, InputMethodManager.SHOW_IMPLICIT)
                                }, 250)
                            }
                            404 -> {
                                Toast.makeText(
                                    requireContext(),
                                    "올바르지 않은 휴대폰 번호 입니다. 다시 시도해주세요.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                enabledInputData()
                                binding.etFADAuth.setText("")
                                binding.etFADAuth.apply {
                                    requestFocus()
                                    setText("")
                                    postDelayed({
                                        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                                        imm.showSoftInput(binding.etFADAuth, InputMethodManager.SHOW_IMPLICIT)
                                    }, 250)
                                }
                            }
                            else -> {
                                Toast.makeText(
                                    requireContext(),
                                    "인증번호 전송에 실패했습니다. 잠시 후 다시 시도해주세요",
                                    Toast.LENGTH_SHORT
                                ).show()
                                enabledInputData()
                                binding.etFADAuth.setText("")
                                binding.etFADAuth.apply {
                                    requestFocus()
                                    setText("")
                                    postDelayed({
                                        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                                        imm.showSoftInput(binding.etFADAuth, InputMethodManager.SHOW_IMPLICIT)
                                    }, 250)
                                }
                            }
                        }
                    }
                }
            }
            setNegativeButton("아니오") { _, _ ->
                enabledInputData()
            }
        }.show()
    }

    private fun verifyMobileCode() {
        if (dialog?.isShowing == true) {
            loadingDialog?.show(
                requireActivity().supportFragmentManager,
                "LoadingDialogFragment"
            )
        }
        val configureMobile = svm.passMobile.value?.replace("-", "")
        val bodyJo = JSONObject().apply {
            put("mobile", configureMobile)
            put("otp", binding.etFADAuthNumber.text)
        }
        Log.v("verifyMobileCode","$bodyJo")
        lifecycleScope.launch(Dispatchers.IO) {
            val findEmail = verityMobileOTPToFindEmail(getString(R.string.API_user), bodyJo.toString())
            Log.v("findEmail", "$findEmail")
            withContext(Dispatchers.Main) {

                // 로딩창 닫기
                if (dialog?.isShowing == true) {
                    loadingDialog?.dismiss()
                }

                // 경로 처리
                when (findEmail) {
                    "otpFailed" -> {
                        svm.mobileCondition.value = true
                        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                            setTitle("알림")
                            setMessage("만료 혹은 올바르지 않은 인증번호입니다. 다시 시도해주세요")
                            setPositiveButton("예", {_, _ ->
                                binding.etFADAuthNumber.setText("")
                            })
                        }.show()
                    }
                    "", null -> {
                        svm.mobileCondition.value = true
                        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                            setTitle("알림")
                            setMessage("서버 에러입니다. 잠시 후 다시 시도해주세요")
                            setPositiveButton("예", {_, _ ->
                                binding.etFADAuthNumber.setText("")
                            })

                        }.show()
                    }
                    else -> {
                        Toast.makeText(requireContext(), "인증에 성공했습니다", Toast.LENGTH_SHORT).show()
                        binding.clFADAuth.visibility = View.GONE
                        binding.btnFADConfirm.text = "초기 화면으로"
                        enabledButton()


                        binding.textView128.visibility = View.VISIBLE
                        binding.textView129.visibility = View.VISIBLE

                        // 휴대폰 인증 성공 시
                        disabledSendButton()
                        disabledInputCode()
                        disabledInputData()
                        svm.countDownTimer?.cancel()
                        binding.tvFADCountDown.visibility =View.GONE
                        binding.tvFADReAuth.visibility = View.GONE
                        svm.mobileCondition.value = true

                        binding.clFADAuth.visibility = View.GONE
                        binding.clFADIdResult.visibility = View.VISIBLE
                        val maskedString = maskedProfileData(findEmail)
                        binding.tvFADFindedEmail.text = maskedString
                    }
                }
            }
        }
    }

    private fun setReSendMessage() {
        binding.tvFADCountDown.visibility = View.VISIBLE
        val madb = MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setTitle("인증번호 재전송")
            setMessage(
                if (svm.isFindEmail.value == true)
                    "${svm.passMobile.value}로 인증번호를 다시 전송하시겠습니까?"
                else "${svm.fullEmail.value}로 인증번호를 다시 전송하시겠습니까?")
            setPositiveButton("예") { _, _ ->
                if (svm.isFindEmail.value == true) {
                    val configureMobile = svm.passMobile.value?.replace("-", "")
                    val bodyJo = JSONObject().apply {
                        put("mobile", configureMobile)
                    }
                    // 인증번호 다시 보내기
                    lifecycleScope.launch(Dispatchers.IO) {
                        val statusCode = sendMobileOTPToFindEmail(getString(R.string.API_user), bodyJo.toString())
                        withContext(Dispatchers.Main) {
                            when (statusCode) {
                                1 -> {
                                    Toast.makeText(
                                        requireContext(),
                                        "인증번호를 전송했습니다. 휴대폰을 확인해주세요",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    enabledInputCode()
                                    // 카운팅
                                    binding.etFADAuthNumber.requestFocus()
                                    binding.tvFADReAuth.visibility = View.VISIBLE
                                    setReSendMessage()
                                    binding.etFADAuthNumber.postDelayed({
                                        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                                        imm.showSoftInput(binding.etFADAuthNumber, InputMethodManager.SHOW_IMPLICIT)
                                    }, 250)
                                }

                                else -> {
                                    Toast.makeText(
                                        requireContext(),
                                        "인증번호 전송에 실패했습니다. 휴대폰 번호를 확인해주세요",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    enabledInputData()
                                }
                            }
                        }
                    }
                } else {
                    if (dialog?.isShowing == true) {
                        loadingDialog?.show(
                            requireActivity().supportFragmentManager,
                            "LoadingDialogFragment"
                        )
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
                            Toast.makeText(
                                requireContext(),
                                "인증번호를 이메일로 전송했습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                            // 재전송안내 문구 세팅
                            binding.tvFADReAuth.visibility = View.VISIBLE
                            setVerifyCountDown(binding.tvFADCountDown, 120) {
                                svm.countDownTimer = it
                            }

                            // 버튼 활성화
                            enabledInputCode()
                            enabledButton()

                        } else {
                            Toast.makeText(
                                requireContext(),
                                "이메일이 올바르지 않습니다. 다시 시도해주세요",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    }
                }
            }
            setNegativeButton("아니오") { _, _ -> }
        }
        setRetryAuthMessage(requireContext(), svm, binding.tvFADReAuth, binding.tvFADCountDown, madb)
    }

    private fun sendEmailCode() {
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

                sendPWCode(getString(R.string.API_user), jo.toString()) { code ->
                    // 전송 로딩 창 완료
                    if (dialog?.isShowing == true) {
                        loadingDialog?.dismiss()
                    }
                    // 결과 처리
                    when (code) {
                        201, 201 -> {
                            Toast.makeText(requireContext(), "인증번호를 이메일로 전송했습니다.", Toast.LENGTH_SHORT).show()
                            // 재전송안내 문구 세팅
                            binding.tvFADReAuth.visibility = View.VISIBLE
                            setReSendMessage()

                            // 버튼 활성화
                            disabledSendButton()
                            enabledInputCode()
                            binding.btnFADConfirm.visibility = View.GONE

                            binding.etFADAuthNumber.apply {
                                requestFocus()
                                postDelayed({
                                    val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                                    imm.showSoftInput(binding.etFADAuthNumber, InputMethodManager.SHOW_IMPLICIT)
                                }, 250)
                            }
                        }
                        404 -> {
                            Toast.makeText(requireContext(), "존재하지 않는 계정입니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(requireContext(), "이메일이 올바르지 않습니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            setNegativeButton("아니오", null)
            show()
        }
    }

    private fun verifyEmailCode() {
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
                        Toast.makeText(requireContext(), "인증에 성공했습니다 비밀번호를 다시 설정해주세요", Toast.LENGTH_SHORT).show()

                        svm.resetJwt = jo.optString("jwt_for_pwd")
                        binding.clFADResetPassword.visibility = View.VISIBLE
                        binding.clFADAuth.visibility = View.GONE
                        svm.countDownTimer?.cancel()
                        binding.tvFADCountDown.visibility =View.GONE
                        binding.tvFADReAuth.visibility = View.GONE

                        binding.btnFADConfirm.text = "비밀번호 재설정"
                        binding.btnFADConfirm.visibility = View.VISIBLE
                        binding.etFADResetPassword.apply {
                            requestFocus()
                            val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showSoftInput(binding.etFADAuthNumber, InputMethodManager.SHOW_IMPLICIT)
                        }
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
                        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                            setTitle("알림")
                            setMessage("만료 혹은 올바르지 않은 인증번호입니다. 다시 시도해주세요")
                            setPositiveButton("예", {_, _ ->
                                binding.etFADAuthNumber.setText("")
                            })
                        }.show()
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

    private fun setEmailAuth () {
        binding.tvFADAuth.text = "이메일 인증"
        binding.etFADAuth.apply {
            hint = "이메일을 입력해주세요"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
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


    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
    private fun enabledButton() {
        binding.btnFADConfirm.apply {
            visibility = View.VISIBLE
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
            setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white)))
            isEnabled = true
        }
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
    private fun enabledInputData() {
        binding.etFADAuth.apply {
            isEnabled = true
            backgroundTintList = null
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bckgnd_12_edit_text)
        }
    }
    private fun disabledInputData() {
        binding.etFADAuth.apply {
            isEnabled = false
            backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
        }
    }

    private fun enabledInputCode() {
        binding.etFADAuthNumber.apply {
            visibility = View.VISIBLE
            isEnabled = true
            backgroundTintList = null
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bckgnd_12_edit_text)
        }
    }
    private fun disabledInputCode() {
        binding.etFADAuthNumber.apply {
            isEnabled = false
            backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
        }
    }


    private fun showExitDialog() {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setMessage("회원 정보 찾기를 종료하시겠습니까?")
            setPositiveButton("예") { _, _ ->
                dialog?.dismiss()
            }
            setNegativeButton("아니오") { _, _ -> }
            show()
        }
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCancelable(false)
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                showExitDialog()
                true // 이벤트 소비
            } else {
                false
            }
        }
        return dialog
    }

}