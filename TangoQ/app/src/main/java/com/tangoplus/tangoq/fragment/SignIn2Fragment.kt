package com.tangoplus.tangoq.fragment

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.skydoves.progressview.ProgressView
import com.tangoplus.tangoq.IntroActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.api.NetworkUser.insertUser
import com.tangoplus.tangoq.api.NetworkUser.sendMobileOTP
import com.tangoplus.tangoq.api.NetworkUser.verifyMobileOTP
import com.tangoplus.tangoq.databinding.FragmentSignIn2Binding
import com.tangoplus.tangoq.dialog.SignInDialogFragment
import com.tangoplus.tangoq.dialog.WebViewDialogFragment
import com.tangoplus.tangoq.dialog.bottomsheet.AgreementBSDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.function.SecurePreferencesManager.encrypt
import com.tangoplus.tangoq.viewmodel.SignInViewModel
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
            when (binding.btnSignIn.text) {
                "인증번호 확인" -> {
                    verifyMobileCode()
                }
                "회원가입 완료하기" -> {
                    showAgreementBottomSheetDialog(requireActivity())
                    storeUserValue()
                }
            }
        }


        when (svm.isSnsSignIn) {
            true -> {
                binding.etName.setText(svm.snsJo.optString("user_name"))
                binding.etName.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
                binding.etName.isEnabled = false
                binding.etMobile.isEnabled = true
                disabledSendBtn()
            }
            false -> {
//                svm.passName.observe(viewLifecycleOwner) {
//                    if (it != "") {
//                        binding.clName.visibility = View.VISIBLE
//                        binding.etName.setText(it)
//                        binding.etName.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
//                    } else {
//                        binding.etName.text = null
//                        binding.etName.backgroundTintList = null
//                    }
//                }
//                svm.passMobile.observe(viewLifecycleOwner) {
//                    if (it != "") {
//                        binding.etMobile.setText(it)
//                        binding.etMobile.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
//                    } else {
//                        binding.etMobile.text = null
//                        binding.etMobile.backgroundTintList = null
//                    }
//                }
//                svm.passAuthCondition.observe(viewLifecycleOwner) {
//                    if (it) {
//                        // 전부 check가 됐을 떄
//
//                    } else {
//                        clearUserValue()
//                    }
//                }

                // 회원가입 버튼 state 관리
                svm.allTrueLiveData.observe(viewLifecycleOwner) {
                    if (binding.btnSignIn.text == "회원가입 완료하기") {
                        if (it) {
                            parentDialog?.requireView()?.findViewById<ProgressView>(R.id.pvSignIn)?.let { it.progress = 100f }
                            parentDialog?.requireView()?.findViewById<TextView>(R.id.tvSignInGuide)?.let { it.text = "가입 버튼을 눌러주세요" }
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
                val nameRegex = "^[가-힣]{2,5}$|^[a-zA-Z]{2,20}$"
                val namePatternCheck = Pattern.compile(nameRegex)
                binding.etName.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        svm.nameCondition.value = namePatternCheck.matcher(binding.etName.text.toString()).find()
                        if (svm.nameCondition.value == true) {
                            binding.tvNameCondition.setTextColor(resources.getColor(R.color.successColor, null))
                            binding.tvNameCondition.text = "올바른 형식입니다"
                        } else {
                            binding.tvNameCondition.setTextColor(resources.getColor(R.color.deleteColor, null))
                            binding.tvNameCondition.text = "올바른 이름을 입력해주세요"
                        }
                    }
                })
                binding.etName.setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                        binding.etMobile.requestFocus()
                        return@setOnEditorActionListener true
                    }
                    false
                }
            }
        }


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
                    s.replace(0, s.length, formatted)
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
                    val statusCode = sendMobileOTP(getString(R.string.API_user), bodyJo.toString())
                    withContext(Dispatchers.Main) {
                        when (statusCode) {
                            1 -> {
                                Toast.makeText(requireContext(), "인증번호를 전송했습니다. 휴대폰을 확인해주세요", Toast.LENGTH_SHORT).show()
                                setReSendMessage()


                            }
                            else -> {
                                Toast.makeText(requireContext(), "인증번호 전송에 실패했습니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            })
            setNegativeButton("아니오", {_, _ ->

            })
        }.show()
    }

    private fun verifyMobileCode() {
        val bodyJo = JSONObject().apply {
            put("mobile", svm.passMobile.value)
            put("otp", binding.etMobileCode.text)
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val statusCode = verifyMobileOTP(getString(R.string.API_user), bodyJo.toString())
            when (statusCode) {
                200, 201 -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "인증에 성공했습니다.", Toast.LENGTH_SHORT).show()
                        svm.mobileCondition.value = true
                        binding.btnSignIn.text =  "회원가입 완료하기"

                        // 완료 후 모바일 인증 잠금
                        disabledSendBtn()

                        binding.etMobile.apply {
                            isEnabled = false
                            backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
                        }
                    }
                }
                else -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "인증에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        svm.mobileCondition.value = true
                    }
                }
            }
        }
    }

    private fun storeUserValue() {
        svm.User.value?.put("mobile", svm.passMobile.value)
        svm.User.value?.put("user_name", svm.passName.value)
        // 담고 옮기기
    }


    private fun clearUserValue() {
        svm.User.value?.put("mobile", "")
        svm.User.value?.put("user_name", "")
    }

    private fun showAgreementBottomSheetDialog(context: FragmentActivity) {
        val bottomSheetFragment = AgreementBSDialogFragment()
        bottomSheetFragment.setOnFinishListener(object : AgreementBSDialogFragment.OnAgreeListener {
            override fun onFinish(agree: Boolean) {
                if (agree) {
                    svm.User.value?.put("email", "${svm.fullEmail.value}")
                    val jsonObj = svm.User.value
                    // 암호화된 비밀번호 넣기
                    val encryptPW = encrypt(jsonObj?.optString("password_app").toString(), getString(R.string.secret_key), getString(R.string.secret_iv))
                    jsonObj?.put("password_app", encryptPW)
                    // ------! 광고성 넣기 시작 !------
                    jsonObj?.put("sms_receive", if (svm.agreementMk1.value == true) 1 else 0)
                    jsonObj?.put("email_receive", if (svm.agreementMk2.value == true) 1 else 0)
                    jsonObj?.put("device_sn" ,0)
                    jsonObj?.put("user_sn", 0)
                    // ------! 광고성 넣기 끝 !------
//                    Log.v("회원가입JSon", "$jsonObj")
                    if (jsonObj != null) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            insertUser(getString(R.string.API_user), jsonObj ) { status ->
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
                                    409 -> {

                                    }
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
        val fullText = binding.tvMobileResendMessage.text
        setVerifyCountDown()
        val spnbString = SpannableString(fullText)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                    setTitle("인증번호 재전송")
                    setMessage("${svm.passMobile.value}\n이메일로 인증번호를 다시 전송하시겠습니까?")
                    setPositiveButton("예", { _, _ ->
                        val configureMobile = svm.passMobile.value?.replace("-", "")
                        val bodyJo = JSONObject().apply {
                            put("mobile", configureMobile)
                        }
                        // 인증번호 다시 보내기
                        lifecycleScope.launch(Dispatchers.IO) {
                            val statusCode = sendMobileOTP(getString(R.string.API_user), bodyJo.toString())
                            when (statusCode) {
                                1 -> {
                                    Toast.makeText(requireContext(), "인증번호를 전송했습니다. 휴대폰을 확인해주세요", Toast.LENGTH_SHORT).show()
                                    setReSendMessage()
                                }
                                else -> {
                                    Toast.makeText(requireContext(), "인증번호 전송에 실패했습니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    })
                    setNegativeButton("아니오", {_, _ -> })
                }.show()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = resources.getColor(R.color.thirdColor, null)
            }
        }
        val startIndex = fullText.indexOf("재전송")
        val endIndex = startIndex + "재전송".length
        spnbString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvMobileResendMessage.text = spnbString
        binding.tvMobileResendMessage.apply {
            text = spnbString
            movementMethod = LinkMovementMethod.getInstance()
            isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed({
                isEnabled = true
            } ,5000)
        }
    }
    private fun setVerifyCountDown() {
        object : CountDownTimer((50 * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val remainingSeconds = millisUntilFinished / 1000
                val minutes = remainingSeconds / 60
                val seconds = remainingSeconds % 60
                binding.tvMobileCountDown.visibility = View.VISIBLE
                binding.tvMobileCountDown.text = "남은시간: ${minutes}분 ${seconds}초"
            }

            override fun onFinish() {
                binding.tvMobileCountDown.visibility = View.INVISIBLE
            }
        }.start()
    }

}