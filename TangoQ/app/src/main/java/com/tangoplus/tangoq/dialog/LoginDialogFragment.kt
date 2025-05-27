package com.tangoplus.tangoq.dialog

import android.app.Activity
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.api.NetworkUser
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import com.tangoplus.tangoq.databinding.FragmentLoginDialogBinding
import com.tangoplus.tangoq.function.PreferencesManager
import com.tangoplus.tangoq.function.SecurePreferencesManager.createKey
import com.tangoplus.tangoq.api.NetworkUser.getUserIdentifyJson
import com.tangoplus.tangoq.api.NetworkUser.resetLock
import com.tangoplus.tangoq.api.NetworkUser.resetPW
import com.tangoplus.tangoq.api.NetworkUser.sendPWCode
import com.tangoplus.tangoq.api.NetworkUser.verifyPWCode
import com.tangoplus.tangoq.fragment.ExtendedFunctions.fadeInView
import com.tangoplus.tangoq.fragment.ExtendedFunctions.scrollToView
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.function.AuthManager
import com.tangoplus.tangoq.function.AuthManager.setRetryAuthMessage
import com.tangoplus.tangoq.function.SaveSingletonManager
import com.tangoplus.tangoq.function.SecurePreferencesManager.encrypt
import com.tangoplus.tangoq.function.SecurePreferencesManager.saveEncryptedJwtToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.regex.Pattern
import androidx.core.graphics.drawable.toDrawable
import com.tangoplus.tangoq.viewmodel.MeasureViewModel

class LoginDialogFragment : DialogFragment() {
    lateinit var binding: FragmentLoginDialogBinding
    val viewModel : SignInViewModel by activityViewModels()
    val mvm : MeasureViewModel by activityViewModels()
    private lateinit var loadingDialog : LoadingDialogFragment
    private lateinit var ssm : SaveSingletonManager
    private lateinit var prefs : PreferencesManager
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentLoginDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.slide_top)
        sharedElementReturnTransition = TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.slide_top)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // api35이상 화면 크기 조절
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 상태 표시줄 높이만큼 상단 패딩 적용
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ------# 로그인 count 저장 #------
        loadingDialog = LoadingDialogFragment.newInstance("회원가입전송")
        viewModel.fullEmail.value = ""
        prefs = PreferencesManager(requireContext())
        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        ssm = SaveSingletonManager(requireContext(), requireActivity(), mvm)
        binding.etLDEmail.requestFocus()
        binding.etLDEmail.postDelayed({
            imm.showSoftInput(binding.etLDEmail, InputMethodManager.SHOW_IMPLICIT)
            scrollToView(binding.etLDEmail, binding.nsvLogin)
        }, 250)

        imm.hideSoftInputFromWindow(view.windowToken, 0)

        // ------! 로그인 시작 !------
        val emailPattern = "^[a-zA-Z0-9_+.-]{4,20}@([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}\$" // 영문, 숫자 4 ~ 16자 패턴
        val emailPatternCheck = Pattern.compile(emailPattern)
        val pwPattern = "^[\\s\\S]{4,20}$" // 영문, 특수문자, 숫자 8 ~ 20자 패턴 ^[a-zA-Z0-9]{6,20}$
        val pwPatternCheck = Pattern.compile(pwPattern)
        binding.etLDEmail.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.fullEmail.value = s.toString()
                viewModel.currentEmailCon.value = emailPatternCheck.matcher(binding.etLDEmail.text.toString()).find()
            }
        })
        binding.etLDPw.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.pw.value = s.toString()
                viewModel.currentPwCon.value = pwPatternCheck.matcher(binding.etLDPw.text.toString()).find()
            }
        })
        binding.etLDPw.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                tryLogin()
                true  // 이벤트 처리가 완료되었음을 반환
            } else {
                false // 다른 동작들은 그대로 유지
            }
        }

        // 응답과 관계없이 로그인 시도 5회 실패시 잠금 ( 3분간 )
        // 5회 동안 이렇게 세고, 6회 시도 시 실패 -> 5분 7회 -> 10 8회 -> 20 9회 30분 10회 -> 이제 추가함
        binding.ibtnLDIdClear.setOnSingleClickListener { binding.etLDEmail.text.clear() }
        binding.ibtnLDPwClear.setOnSingleClickListener { binding.etLDPw.text.clear() }

        binding.btnLDLogin.setOnSingleClickListener {
            when (binding.btnLDLogin.text) {
                "로그인" -> {
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                    tryLogin()
                }
                "코드 인증" -> { verifyEmailCode() }
                "비밀번호 변경" -> { resetPassword() }
            }

        } // ------! 로그인 끝 !------

        // ------! 비밀번호 및 아이디 찾기 시작 !------
        binding.tvLDFind.setOnSingleClickListener {
            val dialog = FindAccountDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "FindAccountDialogFragment")
        }
        // ------! 비밀번호 및 아이디 찾기 시작 !------
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    private fun tryLogin() {
        if (viewModel.emailPwCondition.value == true) {

            // pw 암호화
            val encryptedPW = encrypt(viewModel.pw.value.toString(), getString(R.string.secret_key), getString(R.string.secret_iv))

            val jsonObject = JSONObject()
            jsonObject.put("email", viewModel.fullEmail.value)
            jsonObject.put("password_app", encryptedPW)
            val dialog = LoadingDialogFragment.newInstance("로그인")
            dialog.show(requireActivity().supportFragmentManager, "LoadingDialogFragment")

            lifecycleScope.launch {
                getUserIdentifyJson(getString(R.string.API_user), jsonObject) { jo ->
//                    Log.v("loginResult", "$jo")
                    val isAccessEmpty = jo?.optString("access_jwt")?.isBlank()
                    if (jo == null) {
                        dialog.dismiss()
                        Handler(Looper.getMainLooper()).post {
                            if (!dialog.isVisible) { // `isVisible` 대신 `isAdded`를 쓰는 것도 고려 가능
                                MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                                    setTitle("알림")
                                    setMessage("네트워크 문제로 로그인할 수 없습니다.\n나중에 다시 이용해주세요")
                                    setPositiveButton("예") { _, _ ->
                                        enabledLogin()
                                    }
                                    show()
                                }
                            }
                        }
                    } else if (isAccessEmpty != true) {
                        val jsonObj = JSONObject()
                        jsonObj.put("access_jwt", jo.optString("access_jwt"))
                        jsonObj.put("refresh_jwt", jo.optString("refresh_jwt"))
                        saveEncryptedJwtToken(requireContext(), jsonObj)

                        requireActivity().runOnUiThread {
                            binding.btnLDLogin.isEnabled = false
                            viewModel.User.value = null
                            // ------! 싱글턴 + 암호화 저장 시작 !------
                            // ------# 최초 로그인 #------
                            // ------# 기존 로그인 #------
                            NetworkUser.storeUserInSingleton(requireContext(), jo)
                            createKey(getString(R.string.SECURE_KEY_ALIAS))
//                                        saveEncryptedToken(requireContext(), getString(R.string.SECURE_KEY_ALIAS), encryptToken(getString(R.string.SECURE_KEY_ALIAS), jsonObject))
                            val userUUID = Singleton_t_user.getInstance(requireContext()).jsonObject?.optString("user_uuid") ?: ""
                            val userInfoSn =  Singleton_t_user.getInstance(requireContext()).jsonObject?.optString("sn")?.toInt() ?: -1
                            val safeContext = context
                            if (safeContext != null) {
                                dialog.dismiss()
                                ssm.getMeasures(userUUID, userInfoSn, CoroutineScope(Dispatchers.IO)) {
//                                        Log.v("자체로그인완료", "${Singleton_t_user.getInstance(safeContext).jsonObject}")
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        val intent = Intent(safeContext, MainActivity::class.java)
                                        safeContext.startActivity(intent)
                                        (safeContext as? Activity)?.finishAffinity()
                                    }, 250)
                                }
                            }
                            // ------! 싱글턴 + 암호화 저장 끝 !------
                        }
                    } else {
                        val errorCode = jo.optInt("line")
                        val statusCode = if (errorCode != 0) {
                            jo.optInt("code")
                        } else {
                            jo.optInt("status")
                        }
                        val socialAccount = jo.optString("social_account") ?: ""
                        val retryAfter = jo.optInt("retry_after") ?: 0
                        requireActivity().runOnUiThread {
                            dialog.dismiss()
                            makeMaterialDialog(
                                when (statusCode) {
                                    401, 404 -> 0
                                    429 -> 1
                                    423 -> 2
                                    422 -> 3
                                    else -> -1
                                },
                                retryAfter,
                                socialAccount
                            )

                            binding.etLDPw.text.clear()
                        }
                    }
                }
            }
        }
    }
    private fun makeMaterialDialog(case: Int, retryAfter : Int, socialAccount: String = "") {

        val message = when (case) {
            0 -> "비밀번호 또는 아이디가 올바르지 않습니다.\n로그인을 3회 이상 실패했을 경우 일정 시간 제한될 수 있습니다."
            1 -> "반복적인 로그인 실패로, 로그인이 ${retryAfter}초 동안 제한됩니다."
            2 -> "비밀번호를 5회 틀려 계정이 잠겼습니다.\n이메일 인증을 통해 잠금을 해제해주세요"
            3 -> "소셜 회원가입 계정입니다. $socialAccount 로그인을 진행 해주세요"
            -1 -> "서버 오류입니다. 잠시 후 다시 시도해주세요"
            else -> ""
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setTitle("알림")
            setMessage(
                // 메시지 정하기
                if (case == 0) {
                    val spannableMessage = SpannableString(message)
                    val startIndex = spannableMessage.indexOf("\n") + 1
                    spannableMessage.setSpan(
                        RelativeSizeSpan(0.8f), // 크기 14 (기본 크기의 80%)
                        startIndex,
                        spannableMessage.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannableMessage.setSpan(
                        ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.subColor700)), // 색상 설정 (색상 코드는 실제 사용하는 값으로 변경)
                        startIndex,
                        spannableMessage.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannableMessage
                } else {
                    message
                }
            )
            setPositiveButton("확인") { _, _ ->
                binding.etLDLockEmail.apply {
                    setText("${binding.etLDEmail.text}")
                    binding.etLDLockEmail.postDelayed({
                        requestFocus()
                        setSelection(binding.etLDLockEmail.length())
                        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(binding.etLDLockEmail, InputMethodManager.SHOW_IMPLICIT)
                        scrollToView(binding.etLDLockEmail, binding.nsvLogin)
                    }, 250)
                }


            }
            create()
        }.show()

        when (case) {
            1 -> {
                disabledLogin(retryAfter)
            }
            // 계정이 잠겼을 때 잠금 해제 > 비밀번호 재설정
            2 -> {
                disabledLogin(-1)
                binding.clLDResetLock.visibility = View.VISIBLE
                binding.btnLDLogin.text = "코드 인증"

                fadeInView(binding.clLDResetLock)
                setLockFunc()
            }
            -1 -> {
                enabledLogin()
            }
        }
    }

    private fun setLockFunc() {
        setTextChangedListener()
        binding.etLDLockEmail.setText("")
        binding.etLDCode.setText("")
        // ------# 키보드 focus자동 이동 #------
        binding.etLDLockEmail.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                sendEmailAuthCode()
                true  // 이벤트 처리가 완료되었음을 반환
            } else {
                false // 다른 동작들은 그대로 유지
            }
        }
        binding.btnLDCodeSend.setOnSingleClickListener {
            sendEmailAuthCode()
        }
        val emailPattern = "^[a-zA-Z0-9_+.-]{4,20}@([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}\$"
        val emailPatternCheck = Pattern.compile(emailPattern)
        binding.etLDLockEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.emailCondition.value = emailPatternCheck.matcher(binding.etLDEmail.text.toString()).find()
                if (viewModel.emailCondition.value == true) {
                    viewModel.saveEmail = s.toString()
                    binding.btnLDCodeSend.isEnabled = true
                } else {
                    binding.btnLDCodeSend.isEnabled = false
                    viewModel.saveEmail = ""
                }
//                Log.v("VMEmail", viewModel.saveEmail)
            }
        })
    }

    private fun disabledLogin(retryAfter: Int) {
        binding.btnLDLogin.isEnabled = false
        binding.btnLDLogin.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400))
        binding.tvLDAlert.visibility = View.VISIBLE
        binding.tvLDAlert.text = "반복적인 로그인 시도로 해당 계정 로그인이 제한됩니다\n잠금을 위해 이메일 인증을 진행해주세요"

        if (retryAfter != -1) {
            viewModel.countDownTimer = null
            viewModel.countDownTimer = object : CountDownTimer((retryAfter * 1000).toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val remainingSeconds = millisUntilFinished / 1000
                    val minutes = remainingSeconds / 60
                    val seconds = remainingSeconds % 60
                    binding.tvLDAlert.text = "반복적인 로그인 시도로 해당 계정 로그인이 제한됩니다.\n남은시간: ${minutes}분 ${seconds}초"
                }
                override fun onFinish() {
                    enabledLogin()
                }
            }
            (viewModel.countDownTimer as CountDownTimer).start()
        }
    }

    private fun enabledLogin() {
        binding.btnLDLogin.isEnabled = true
        binding.btnLDLogin.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
        binding.tvLDAlert.visibility = View.GONE
    }

    private fun setTextChangedListener() {
        binding.etLDCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 6) {
                    verifyEmailCode()
                }
            }
        })

        binding.etLDResetPasswordConfirm.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                resetPassword()
                true  // 이벤트 처리가 완료되었음을 반환
            } else {
                false // 다른 동작들은 그대로 유지
            }
        }
    }
    private fun verifyEmailCode() {
        val bodyJo = JSONObject().apply {
            put("email", viewModel.saveEmail)
            put("otp", binding.etLDCode.text)
        }
        verifyPWCode(getString(R.string.API_user), bodyJo.toString()) { jo ->
            if (jo != null) {
                val code = jo.optInt("status")
                when (code) {
                    200 -> {
                        viewModel.resetJwt = jo.optString("jwt_for_pwd")
                        val emailJo = JSONObject().apply {
                            put("email", viewModel.saveEmail)
                        }
                        resetLock(getString(R.string.API_user), viewModel.resetJwt, emailJo.toString()) { innerCode ->
                            when (innerCode) {
                                200 -> {
                                    lifecycleScope.launch (Dispatchers.Main) {
                                        Toast.makeText(requireContext(), "인증에 성공했습니다. 비밀번호를 재설정해주세요", Toast.LENGTH_LONG).show()

                                        // 비밀번호 재설정 절차
                                        binding.btnLDLogin.text = "비밀번호 변경"
                                        binding.clLDResetLock.visibility = View.GONE
                                        fadeInView(binding.clLDResetPassword)
                                        setResetPWCheck()

                                        // focus
                                        binding.etLDResetPassword.apply {
                                            requestFocus()
                                            val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                                            postDelayed({
                                                imm.showSoftInput(binding.etLDResetPassword, InputMethodManager.SHOW_IMPLICIT)
                                                scrollToView(binding.etLDResetPassword, binding.nsvLogin)
                                            }, 250)
                                        }
                                    }
                                }
                                401 -> {
                                    lifecycleScope.launch (Dispatchers.Main) {
                                        Toast.makeText(requireContext(), "올바르지 않은 요청입니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                    401 -> {
                        Toast.makeText(requireContext(), "올바르지 않은 OTP입니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                        binding.etLDCode.setText("")

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
    private fun sendEmailAuthCode() {
        if (dialog?.isShowing == true) {
            MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                setMessage("${viewModel.saveEmail}로 인증하시겠습니까?")
                setPositiveButton("예") { _, _ ->
                    if (dialog?.isShowing == true) {
                        loadingDialog.show(requireActivity().supportFragmentManager, "loadingDialogFragment")
                    }
                    val jo = JSONObject().apply {
                        put("email", viewModel.saveEmail)
                    }
                    sendPWCode(getString(R.string.API_user), jo.toString()) {
                        Toast.makeText(requireContext(), "인증번호를 이메일로 전송했습니다.", Toast.LENGTH_SHORT).show()
                        if (dialog?.isShowing == true) {
                            loadingDialog.dismiss()
                        }
                        viewModel.saveEmail = binding.etLDEmail.text.toString()
                        // 재전송안내 문구 세팅
                        binding.tvLDReAuth.visibility = View.VISIBLE
                        setReSendMessage()
                        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        binding.etLDCode.requestFocus()
                        binding.etLDCode.postDelayed({
                            imm.showSoftInput(binding.etLDCode, InputMethodManager.SHOW_IMPLICIT)
                            scrollToView(binding.etLDCode, binding.nsvLogin)
                        }, 250)
                        // 버튼 활성화
                        binding.etLDCode.isEnabled = true
                        binding.btnLDLogin.apply {
                            isEnabled = true
                            backgroundTintList =
                                ColorStateList.valueOf(resources.getColor(R.color.mainColor, null))
                        }
                    }
                }
                setNegativeButton("아니오") { _, _ ->
                }
                show()
            }
        }
    }
    private fun setReSendMessage() {
        binding.tvLDReAuth.visibility = View.VISIBLE
        val madb = MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setTitle("인증번호 재전송")
            setMessage("${viewModel.fullEmail.value}\n이메일로 인증번호를 다시 전송하시겠습니까?")
            setPositiveButton("예") { _, _ ->
                if (dialog?.isShowing == true) {
                    loadingDialog.show(requireActivity().supportFragmentManager, "loadingDialogFragment")
                }
                val jo = JSONObject().apply {
                    put("email", viewModel.saveEmail)
                }
                sendPWCode(getString(R.string.API_user), jo.toString()) {
                    Toast.makeText(requireContext(), "인증번호를 이메일로 전송했습니다.", Toast.LENGTH_SHORT).show()
                    if (dialog?.isShowing == true) {
                        loadingDialog.dismiss()
                    }
                    viewModel.saveEmail = binding.etLDEmail.text.toString()
                    // 재전송안내 문구 세팅
                    binding.tvLDReAuth.visibility = View.VISIBLE
                    setReSendMessage()
                    val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    binding.etLDCode.requestFocus()
                    binding.etLDCode.postDelayed({
                        imm.showSoftInput(binding.etLDCode, InputMethodManager.SHOW_IMPLICIT)
                        scrollToView(binding.etLDCode, binding.nsvLogin)
                    }, 250)
                    // 버튼 활성화
                    binding.etLDCode.isEnabled = true
                    binding.btnLDLogin.apply {
                        isEnabled = true
                        backgroundTintList =
                            ColorStateList.valueOf(resources.getColor(R.color.mainColor, null))
                    }
                }
            }
            setNegativeButton("아니오") { _, _ -> }
        }
        setRetryAuthMessage(requireContext(), viewModel, binding.tvLDReAuth, binding.tvLDCountDown, madb)
    }
    private fun setResetPWCheck() {
        // 로그인 버튼 변경
        binding.btnLDLogin.text = "비밀번호 변경"


        // 비밀번호 재설정 patternCheck
        val pwTextWatcher = AuthManager.pwPatternCheck(viewModel, binding.etLDResetPassword, binding.tvLDPWCondition, false)
        binding.etLDResetPassword.addTextChangedListener(pwTextWatcher)

        val pwRepeatTextWatcher = AuthManager.pwPatternCheck(viewModel, binding.etLDResetPasswordConfirm, binding.tvLDPWVerifyCondition, true)
        binding.etLDResetPasswordConfirm.addTextChangedListener(pwRepeatTextWatcher)

        viewModel.pwBothTrue.observe(viewLifecycleOwner) {
            binding.btnLDLogin.isEnabled = it
            if (it) {
                enabledButton()
                viewModel.pw.value = binding.etLDResetPasswordConfirm.text.toString()
            } else {
                disabledButton()
                viewModel.pw.value = ""
            }
        }


    }
    private fun enabledButton() {
        if (binding.btnLDLogin.text == "비밀번호 변경") {
            binding.btnLDLogin.apply {
                backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.mainColor, null))
                isEnabled = true
            }
        }
    }
    private fun disabledButton() {
        if (binding.btnLDLogin.text == "비밀번호 변경") {
            binding.btnLDLogin.apply {
                backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor400, null))
                isEnabled = false
            }
        }
    }
    private fun resetPassword() {
        val encryptPW = encrypt(viewModel.pw.value ?: "", getString(R.string.secret_key), getString(R.string.secret_iv))
        val bodyJo = JSONObject().apply {
            put("email", binding.etLDLockEmail.text)
            put("new_password", encryptPW)
        }
        resetPW(getString(R.string.API_user), viewModel.resetJwt, bodyJo.toString()) { code ->
            lifecycleScope.launch(Dispatchers.Main) {
                when (code) {
                    200 -> {
                        // 비밀번호 초기화 성공
                        viewModel.pwBothTrue.removeObservers(viewLifecycleOwner)
                        viewModel.pwCompare.removeObservers(viewLifecycleOwner)
                        viewModel.pwCondition.removeObservers(viewLifecycleOwner)
                        viewModel.emailCondition.removeObservers(viewLifecycleOwner)

                        // 입력된 EditText + 버튼 초기화
                        binding.btnLDLogin.text = "로그인"
                        binding.etLDResetPassword.setText("")
                        binding.etLDResetPasswordConfirm.setText("")
                        binding.etLDLockEmail.setText("")
                        binding.etLDCode.setText("")

                        binding.tvLDAlert.visibility = View.GONE
                        binding.clLDResetPassword.visibility = View.GONE
                        Toast.makeText(requireContext(), "비밀번호 변경이 완료됐습니다. 다시 로그인해주세요", Toast.LENGTH_SHORT).show()

                    }
                    400 -> Toast.makeText(requireContext(), "올바르지 않은 요청입니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                    else -> {
                        Toast.makeText(requireContext(), "서버 에러입니다. 관리자에게 문의해주세요", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.countDownTimer?.cancel()
        viewModel.countDownTimer = null
    }
}