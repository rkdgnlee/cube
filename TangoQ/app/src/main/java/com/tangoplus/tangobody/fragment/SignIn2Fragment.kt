package com.tangoplus.tangobody.fragment

import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shuhart.stepview.StepView
import com.skydoves.progressview.ProgressView
import com.tangoplus.tangobody.IntroActivity
import com.tangoplus.tangobody.R
import com.tangoplus.tangobody.api.NetworkUser.insertUser
import com.tangoplus.tangobody.api.NetworkUser.sendMobileOTP
import com.tangoplus.tangobody.api.NetworkUser.verifyMobileOTP
import com.tangoplus.tangobody.databinding.FragmentSignIn2Binding
import com.tangoplus.tangobody.dialog.SignInDialogFragment
import com.tangoplus.tangobody.dialog.bottomsheet.AgreementBSDialogFragment
import com.tangoplus.tangobody.fragment.ExtendedFunctions.scrollToView
import com.tangoplus.tangobody.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangobody.function.AuthManager.namePatternCheck
import com.tangoplus.tangobody.function.AuthManager.setRetryAuthMessage
import com.tangoplus.tangobody.function.SecurePreferencesManager.encrypt
import com.tangoplus.tangobody.viewmodel.SignInViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.regex.Pattern

class SignIn2Fragment : Fragment() {
    private lateinit var binding : FragmentSignIn2Binding
    val svm : SignInViewModel by activityViewModels()
    private var parentDialog: SignInDialogFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSignIn2Binding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parentDialog  = (requireParentFragment() as SignInDialogFragment)


        binding.btnAuthSend.setOnSingleClickListener {
//            val passDialog = WebViewDialogFragment()
//            passDialog.show(requireActivity().supportFragmentManager, "WebViewDialogFragment")
            sendMobileCode()
        }

        binding.btnSignIn.setOnSingleClickListener {
            showAgreementBottomSheetDialog(requireActivity())
            storeUserValue()
        }

        binding.etName.isEnabled = true
        binding.etMobile.isEnabled = true
        disabledSendBtn()

        svm.isNextPage.observe(viewLifecycleOwner) {
            if (it) {
                Handler(Looper.getMainLooper()).postDelayed({
                    setNameKeyBoard()

                    parentDialog?.requireView()?.findViewById<StepView>(R.id.svSignIn)?.go(2, true)
                    parentDialog?.requireView()?.findViewById<ProgressView>(R.id.pvSignIn)?.let { it.progress = 90f }
                    parentDialog?.requireView()?.findViewById<TextView>(R.id.tvSignInGuide)?.let { it.text = "휴대폰 인증을 진행해주세요" }
                }, 600)
            }
        }

        // 회원가입 버튼 state 관리
        svm.allTrueLiveData.observe(viewLifecycleOwner) {
            if (binding.btnSignIn.text == "회원가입 완료하기") {
                if (it) {
                    binding.btnSignIn.apply {
                        isEnabled = it
                        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
                    }
                } else {
                    binding.btnSignIn.apply {
                        isEnabled = it
                        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400))
                    }
                }
            }
        }

        // 이름 조건 코드
        val nameTextWatcher = namePatternCheck(svm, binding.etName, binding.tvNameCondition, requireContext())
        binding.etName.addTextChangedListener(nameTextWatcher)
        binding.etName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.etMobile.requestFocus()
                return@setOnEditorActionListener true
            }
            false
        }

        // 인증 완료 후 나와야하는 UI 셋팅
        binding.tvMobileCountDown.visibility = View.GONE
        binding.btnSignIn.visibility = View.GONE

        binding.etMobile.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                if (svm.mobileCondition.value == true) {
                    sendMobileCode()
                }
                true  // 이벤트 처리가 완료되었음을 반환
            } else {
                false // 다른 동작들은 그대로 유지
            }
        }

        binding.etMobileCode.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.length ==  6) {
                    verifyMobileCode()

                }
            }
        })

        // 핸드폰 번호 체크
        val mobilePattern = "^010-\\d{4}-\\d{4}\$"
        val mobilePatternCheck = Pattern.compile(mobilePattern)
        binding.etMobile.addTextChangedListener(object: TextWatcher {
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
                if (s.toString() != formatted && s != null) {
                    binding.etMobile.setText(formatted) // setText를 사용하여 확실하게 변경
                    binding.etMobile.setSelection(formatted.length) // 커서를 마지막 위치로 이동
                }

                isFormatting = false
                svm.mobileCondition.value = mobilePatternCheck.matcher(binding.etMobile.text.toString()).find()
                if (svm.mobileCondition.value == true)  {
                    svm.passMobile.value = s.toString()
                    enabledSendBtn()
                } else {
                    svm.passMobile.value = ""
                    disabledSendBtn()
                }
            }
        })
    }

    private fun sendMobileCode() {
        val configureMobile = svm.passMobile.value?.replace("-", "")
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setTitle("휴대폰 인증")
            setMessage("${svm.passMobile.value}로 인증하시겠습니까?")
            setPositiveButton("예", {_, _ ->
                val bodyJo = JSONObject().apply {
                    put("mobile", configureMobile)
                }
                lifecycleScope.launch(Dispatchers.IO) {
                    val statusCode = sendMobileOTP(getString(R.string.API_user), bodyJo.toString()) ?: 0
                    withContext(Dispatchers.Main) {
                        navigateMobileCode(statusCode)

                    }
                }
            })
            setNegativeButton("아니오", {_, _ ->

            })
        }.show()
    }

    private fun verifyMobileCode() {
        val bodyJo = JSONObject().apply {
            put("mobile", svm.passMobile.value?.replace("-", ""))
            put("otp", binding.etMobileCode.text.toString())
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val response = verifyMobileOTP(getString(R.string.API_user), bodyJo.toString())
            withContext(Dispatchers.Main) {
                navigateMobileVerify(response)
            }
        }
    }

    private fun storeUserValue() {
        svm.User.value?.put("email", svm.fullEmail.value)
        svm.User.value?.put("mobile", svm.passMobile.value?.replace("-", ""))
        svm.User.value?.put("user_name", svm.passName.value)
        Log.v("viewModel에 담기", "${svm.User.value}")
    }

    private fun showAgreementBottomSheetDialog(context: FragmentActivity) {
        val bottomSheetFragment = AgreementBSDialogFragment()
        bottomSheetFragment.setOnFinishListener(object : AgreementBSDialogFragment.OnAgreeListener {
            override fun onFinish(agree: Boolean) {
                if (agree) {
                    val jsonObj = svm.User.value

                    // 자체 회원가입일 경우만 비밀번호 암호화해서 넣기
                    if (svm.pw.value != null && svm.pw.value != "") {
                        val encryptPW = encrypt(svm.pw.value ?: "", getString(R.string.secret_key), getString(R.string.secret_iv))
                        jsonObj?.put("password_app", encryptPW)
                    }

                    // ------! 광고성 넣기 시작 !------
                    jsonObj?.put("sms_receive", if (svm.agreementMk1.value == true) 1 else 0)
                    jsonObj?.put("email_receive", if (svm.agreementMk2.value == true) 1 else 0)
                    jsonObj?.put("device_sn" ,0)
                    jsonObj?.put("user_sn", 0)
                    // ------! 광고성 넣기 끝 !------
                    Log.v("회원가입Json", "$jsonObj")
                    if (jsonObj != null) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            insertUser(getString(R.string.API_user), jsonObj , svm.insertToken) { status ->
                                when (status) {
                                    200, 201 -> {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            val intent = Intent(requireActivity(), IntroActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                            intent.putExtra("SignInFinished", 201)

                                            parentDialog?.dismiss()
                                            startActivity(intent)
                                        }
                                    }
                                    423,  404, 403 -> {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                                                setTitle("회원 가입 실패")
                                                setMessage("올바르지 않은 데이터가 존재합니다\n입력한 정보를 다시 확인해주세요")
                                                setPositiveButton("예") { _, _ ->
                                                }
                                                show()
                                            }
                                        }
                                    }
                                    400 -> Toast.makeText(requireContext(), "사용자 정보가 올바르지 않습니다. 입력한 정보를 확인해주세요", Toast.LENGTH_LONG).show()
                                    500 -> Toast.makeText(requireContext(), "서버 에러입니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_LONG).show()
                                    else -> Toast.makeText(requireContext(), "회원가입을 진행할 수 없습니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
            }
        })
        val fragmentManager = context.supportFragmentManager
        bottomSheetFragment.show(fragmentManager, bottomSheetFragment.tag)
    }

    private fun disabledSendBtn() {
        binding.btnAuthSend.apply {
            isEnabled = false
            backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor400, null))
        }
    }

    private fun enabledSendBtn() {
        binding.btnAuthSend.apply {
            isEnabled = true
            backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.mainColor, null))
        }
    }

    private fun setReSendMessage() {
        binding.clMobileVerify.visibility = View.VISIBLE
        val madb = MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setTitle("인증번호 재전송")
            setMessage("${svm.passMobile.value}로 인증번호를 다시 전송하시겠습니까?")
            setPositiveButton("예", { _, _ ->
                val configureMobile = svm.passMobile.value?.replace("-", "")
                val bodyJo = JSONObject().apply {
                    put("mobile", configureMobile)
                }
                // 인증번호 다시 보내기
                lifecycleScope.launch(Dispatchers.IO) {
                    val statusCode = sendMobileOTP(getString(R.string.API_user), bodyJo.toString())
                    withContext(Dispatchers.Main) {
                        when (statusCode) {
                            1, 200, 201 -> {
                                Toast.makeText(requireContext(), "인증번호를 전송했습니다. 휴대폰을 확인해주세요", Toast.LENGTH_SHORT).show()
                                setReSendMessage()
                            }
                            401 -> {
                                MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                                    setTitle("알림")
                                    setMessage("만료 혹은 올바르지 않은 인증번호입니다. 다시 시도해주세요")
                                    setPositiveButton("예", {_, _ ->
                                        binding.etMobileCode.setText("")
                                    })
                                }.show()
                            }
                            else -> {
                                Toast.makeText(requireContext(), "인증번호 전송에 실패했습니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            })
            setNegativeButton("아니오", {_, _ -> })
        }
        setRetryAuthMessage(requireContext(), svm, binding.tvMobileReAuth, binding.tvMobileCountDown, madb)
    }

    private fun navigateMobileCode(statusCode : Int) {
        when (statusCode) {
            200, 201, 1 -> {
                Toast.makeText(requireContext(), "인증번호를 전송했습니다. 휴대폰을 확인해주세요", Toast.LENGTH_SHORT).show()
                setReSendMessage()
                binding.etMobileCode.postDelayed({
                    binding.etMobileCode.requestFocus()
                    scrollToView(binding.etMobileCode, binding.nsvSI2)
                }, 250)
                binding.tvMobileReAuth.visibility = View.VISIBLE
                disabledSendBtn()
                binding.etMobile.apply {
                    isEnabled = false
                    backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
                }

                // 키보드 올림
                binding.etMobileCode.apply {

                    requestFocus()
                    postDelayed({
                        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(binding.etMobileCode, InputMethodManager.SHOW_IMPLICIT)
                    }, 250)
                }
            }
            409 -> {
                MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                    setMessage("이미 존재하는 핸드폰 번호입니다. 재확인 후 다시 시도해주세요")
                    setPositiveButton("예", { _, _ ->
                        binding.etMobile.setText("")
                    })

                }.show()
            }
            400 -> {
                Toast.makeText(requireContext(), "휴대폰 번호가 올바르지 않습니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
            422 -> {
                Toast.makeText(requireContext(), "휴대폰번호가 존재하지 않습니다", Toast.LENGTH_SHORT).show()
            }
            429 -> {
                Toast.makeText(requireContext(), "요청 가능 횟수를 초과헀습니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
            500 -> {
                Toast.makeText(requireContext(), "서버 오류 입니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(requireContext(), "인증번호 전송에 실패했습니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun navigateMobileVerify(response : Pair<String, Int>?) {
        when (response?.second) {
            200, 201 -> {

                svm.mobileCondition.value = true
                svm.passAuthCondition.value = true
                svm.countDownTimer?.cancel()
                binding.btnSignIn.visibility = View.VISIBLE
                binding.btnSignIn.text =  "회원가입 완료하기"
                // 완료 후 모바일 인증 잠금
                disabledSendBtn()
                if (response.first.length > 20) {
                    Log.v("토큰받아오기", response.first)
                    svm.insertToken = response.first
                }

                binding.tvMobileReAuth.visibility = View.GONE
                binding.tvMobileCountDown.visibility = View.GONE
                binding.etMobile.apply {
                    isEnabled = false
                    backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
                }
                binding.etMobileCode.apply {
                    isEnabled = false
                    backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
                }


                // 인증 검증에 관련된 VIew 멈추고 숨기기
                parentDialog?.requireView()?.findViewById<StepView>(R.id.svSignIn)?.go(2, true)
                parentDialog?.requireView()?.findViewById<ProgressView>(R.id.pvSignIn)?.let { it.progress = 100f }
                parentDialog?.requireView()?.findViewById<TextView>(R.id.tvSignInGuide)?.let { it.text = "가입 버튼을 눌러주세요" }

                // agreement
                if (svm.allTrueLiveData.value == true) {
                    Toast.makeText(requireContext(), "인증에 성공했습니다. 하단에 회원가입 버튼을 눌러주세요", Toast.LENGTH_SHORT).show()
                    showAgreementBottomSheetDialog(requireActivity())
                    storeUserValue()
                } else {
                    Toast.makeText(requireContext(), "인증에 성공했습니다. 필수 정보를 다시 확인해주세요", Toast.LENGTH_LONG).show()
                }
            }
            401 -> {
                MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                    setTitle("알림")
                    setMessage("만료 혹은 올바르지 않은 인증번호입니다. 다시 시도해주세요")
                    setPositiveButton("예", {_, _ ->
                        binding.etMobileCode.setText("")
                    })
                }.show()
            }
            500 -> {
                Toast.makeText(requireContext(), "서버 오류 입니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                binding.etMobileCode.setText("")
            }
            else -> {
                Toast.makeText(requireContext(), "인증에 실패했습니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                binding.etMobileCode.setText("")
            }
        }
    }


    private fun setNameKeyBoard() {
        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        binding.etName.requestFocus()
        imm.showSoftInput(binding.etName, InputMethodManager.SHOW_IMPLICIT)
        scrollToView(binding.etName, binding.nsvSI2)

//        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

}