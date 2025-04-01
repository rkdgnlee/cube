package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
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
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.INPUT_METHOD_SERVICE
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shuhart.stepview.StepView
import com.skydoves.progressview.ProgressView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.etc.SpinnerAdapter
import com.tangoplus.tangoq.api.NetworkUser.sendPWCode
import com.tangoplus.tangoq.api.NetworkUser.verifyPWCode
import com.tangoplus.tangoq.databinding.FragmentSignIn1Binding
import com.tangoplus.tangoq.dialog.LoadingDialogFragment
import com.tangoplus.tangoq.dialog.SignInDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.fadeInView
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.regex.Pattern

class SignIn1Fragment : Fragment() {
    private lateinit var binding : FragmentSignIn1Binding
    val svm : SignInViewModel by activityViewModels()
    private var parentDialog: SignInDialogFragment? = null
    private lateinit var loadingDialog : LoadingDialogFragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSignIn1Binding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // dialog 세팅
        loadingDialog = LoadingDialogFragment.newInstance("회원가입확인")
        parentDialog  = (requireParentFragment() as SignInDialogFragment)
        svm.isSnsSignIn = true
        when (svm.isSnsSignIn) {
            true -> {
                setSnsLoginVisibility(true)
                setStep90()
                // 이메일 다 들어감
                binding.etEmailId.setText(svm.snsJo.optString("email"))
                binding.etEmailId.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
                binding.etEmailId.isEnabled = false
                setNextPage()
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.btnSI1Next.visibility = View.VISIBLE
                }, 800)
            }
            false -> {
                setSnsLoginVisibility(false)
                binding.etEmailId.apply {
                    binding.etEmailId.postDelayed({
                        binding.etEmailId.requestFocus()
                        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(binding.etEmailId, InputMethodManager.SHOW_IMPLICIT)
                    }, 250)
                }

                // 버튼 및 초기 셋업
                binding.btnSI1Next.apply {
                    isEnabled = false
                    backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor400, null))
                }
                binding.tvEmailCondition.visibility = View.INVISIBLE
                binding.etEmailId.setText("")
                svm.emailIdCondition.value = false
                svm.emailCondition.value = false
                svm.pwCondition.value = false
                svm.pwCompare.value = false
                disabledSendEmailCode()

                // email id check
                val emailRegex = "^[a-zA-Z0-9_+.-]{4,20}$"
                val emailPatternCheck = Pattern.compile(emailRegex)
                binding.etEmailId.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        if (s.toString().isNotEmpty()) {
                            svm.emailIdCondition.value = emailPatternCheck.matcher(binding.etEmailId.text.toString()).find()
                            binding.tvEmailCondition.visibility = View.VISIBLE
                        } else {
                            binding.tvEmailCondition.visibility = View.INVISIBLE
                        }
                    }
                })

                val domainList = listOf("gmail.com", "naver.com", "kakao.com", "직접입력")
                binding.spinner.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, domainList, 0)
                binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                    @SuppressLint("SetTextI18n")
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long,
                    ) {
                        binding.spinner.getItemAtPosition(position).toString()
                        if (position == 3) {
                            binding.etEmail.visibility = View.VISIBLE
                            binding.spinner.visibility = View.GONE
                            svm.domainCondition.value = false
                            binding.etEmail.setText("")
                        } else {
                            binding.etEmail.visibility = View.GONE
                            binding.spinner.visibility = View.VISIBLE
                            svm.domainCondition.value = true
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
                // image눌러도 spinner가 보이게끔 하기
                binding.ivSpinner.setOnSingleClickListener {
                    binding.spinner.performClick()
                }

                svm.emailCondition.observe(viewLifecycleOwner) {
                    when (it) {
                        true -> {
                            binding.tvEmailCondition.text = "올바른 이메일 형식입니다. 중복 확인을 클릭해주세요"
                            binding.tvEmailCondition.setTextColor(resources.getColor(R.color.successColor, null))
                            binding.btnEmailConfirm.apply {
                                isEnabled = true
                                backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.mainColor, null))
                            }
                        }
                        false -> {
                            if (binding.etEmailId.text.isNotEmpty()) {
                                binding.tvEmailCondition.text = "올바른 이메일 형식을 입력해주세요"
                                binding.tvEmailCondition.setTextColor(resources.getColor(R.color.deleteColor, null))
                                binding.btnEmailConfirm.apply {
                                    isEnabled = false
                                    backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor400, null))
                                }
                            }
                        }
                        else -> {
                            binding.btnEmailConfirm.apply {
                                isEnabled = false
                                backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor400, null))
                            }
                        }
                    }
                }
                // emailId에서 바로 enter쳤을 경우 이메일 중복확인
                binding.etEmailId.setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                        if (svm.emailCondition.value == true) confirmEmail()
                        return@setOnEditorActionListener true
                    }
                    false
                }
                binding.btnEmailConfirm.setOnSingleClickListener {
                    when (binding.btnEmailConfirm.text) {
                        "인증 번호 전송" -> { confirmEmail() }
                        "인증 번호 확인" -> { sendEmailCode() }
                    }
                }

                // email 도메인쪽 패턴 체크
                val domainPattern = "([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}\$"
                val domainPatternCheck = Pattern.compile(domainPattern)
                binding.etEmail.addTextChangedListener(object: TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        if (s.toString().isNotEmpty()) {
                            svm.domainCondition.value = domainPatternCheck.matcher(binding.etEmail.text.toString()).find()
                        }
                    }
                })

                val pwPattern = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[$@$!%*#?&^])[A-Za-z[0-9]$@$!%*#?&^]{8,20}$" // 영문, 특수문자, 숫자 8 ~ 20자 패턴
                val pwPatternCheck = Pattern.compile(pwPattern)
                // ------# 비밀번호 조건 코드 #------
                binding.etPw.addTextChangedListener(object : TextWatcher {
                    @SuppressLint("SetTextI18n")
                    override fun afterTextChanged(s: Editable?) {
                        if (s.toString().isNotEmpty()) {
                            svm.pwCondition.value = pwPatternCheck.matcher(binding.etPw.text.toString()).find()
                            if (svm.pwCondition.value == true) {
                                binding.tvPwCondition.setTextColor(binding.tvPwCondition.resources.getColor(R.color.successColor, null))
                                binding.tvPwCondition.text = "사용 가능합니다"
                            } else {
                                binding.tvPwCondition.setTextColor(binding.tvPwCondition.resources.getColor(R.color.deleteColor, null))
                                binding.tvPwCondition.text = "영문, 숫자, 특수문자( ! @ # $ % ^ & * ?)를 모두 포함해서 8~20자리를 입력해주세요"
                            }
                        }
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })

                // ----- ! 비밀번호 확인 코드 ! -----
                binding.etPwRepeat.imeOptions = EditorInfo.IME_ACTION_DONE
                binding.etPwRepeat.setOnEditorActionListener { v, actionId, event ->
                    if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                        true  // 이벤트 처리가 완료되었음을 반환
                    } else {
                        false // 다른 동작들은 그대로 유지
                    }
                }

                binding.etPwRepeat.addTextChangedListener(object : TextWatcher {
                    @SuppressLint("SetTextI18n")
                    override fun afterTextChanged(s: Editable?) {
                        if (s.toString().isNotEmpty()) {
                            svm.pwCompare.value = (binding.etPw.text.toString() == binding.etPwRepeat.text.toString())
                            if (svm.pwCompare.value == true) {
                                // -----! 뷰모델에 보낼 값들 넣기 !-----
                                svm.User.value?.put("password_app", s.toString())
                                binding.tvPwRepeat.setTextColor(binding.tvPwRepeat.resources.getColor(R.color.successColor, null))
                                binding.tvPwRepeat.text = "일치합니다"
                            } else {
                                binding.tvPwRepeat.setTextColor(binding.tvPwRepeat.resources.getColor(R.color.deleteColor, null))
                                binding.tvPwRepeat.text = "올바르지 않습니다"
                            }

                            binding.btnSI1Next.apply {
                                if (svm.pwCompare.value == true) {
                                    backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.mainColor, null))
                                    isEnabled  = true
                                } else {
                                    backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor400, null))
                                    isEnabled  =false
                                }
                            }
                        }
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                }) //-----! 입력 문자 조건 끝 !-----
            }
        }
        binding.btnSI1Next.setOnSingleClickListener {
            parentDialog?.setonNextPage()
        }
    }
    private fun sendEmailCode() {
        if (parentDialog?.isVisible == true) {
            loadingDialog = LoadingDialogFragment.newInstance("회원가입확인")
            loadingDialog.show(requireActivity().supportFragmentManager, "LoadingDialogFragment")
        }
        val emailId = binding.etEmailId.text.toString()
        svm.fullEmail.value = if (binding.etEmail.isGone) emailId + "@" + binding.spinner.selectedItem.toString() else emailId + "@" + binding.etEmail.text.toString()
        Log.v("이메일 전체", "${svm.fullEmail.value}")
        lifecycleScope.launch(Dispatchers.IO) {
            val jo = JSONObject().apply {
                put("email", svm.fullEmail.value)
            }
            sendPWCode(getString(R.string.API_user), jo.toString()) { code ->
                if (code in listOf(200, 201)) {
                    Toast.makeText(requireContext(), "인증번호를 이메일로 전송했습니다.", Toast.LENGTH_SHORT).show()

                    // 이메일 인증번호 전송
                    binding.etEmailAuthNumber.visibility = View.VISIBLE
                    binding.btnEmailConfirm.text = "인증 번호 확인"
                    setReSendMessage()

                } else {
                    Toast.makeText(requireContext(), "이메일이 올바르지 않습니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun confirmEmail() {
        CoroutineScope(Dispatchers.IO).launch {
            val bodyJo = JSONObject().apply {
                put("email", svm.fullEmail.value)
                put("otp", binding.etEmailAuthNumber.text)
            }
            if (parentDialog?.isAdded == true) {
                loadingDialog.dismiss()
            }
//            if (jo != null) {
//                val code = jo.optInt("status")
//                when (code) {
//                    200 -> {
//                        // 이메일 인증 완료
//                        svm.emailVerify.value = true
//                        binding.btnEmailConfirm.isEnabled = false
//                        binding.btnEmailConfirm.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor400, null))
//                        binding.etEmail.isEnabled = false
//                        binding.etEmailId.isEnabled = false
//                        binding.spinner.isEnabled = false
//                        svm.User.value?.put("email", svm.fullEmail.value)
//
//                        parentDialog?.requireView()?.findViewById<StepView>(R.id.svSignIn)?.go(1, true)
//                        parentDialog?.requireView()?.findViewById<ProgressView>(R.id.pvSignIn)?.let { it.progress = 60f }
//                        parentDialog?.requireView()?.findViewById<TextView>(R.id.tvSignInGuide)?.let { it.text = "비밀번호를 입력해주세요" }
//
//                        // pw로 포커싱
//                        binding.etPw.requestFocus()
//                        binding.etPw.postDelayed({
//                            val imm = requireActivity().getSystemService(
//                                INPUT_METHOD_SERVICE
//                            ) as InputMethodManager
//                            imm.showSoftInput(binding.etPw, InputMethodManager.SHOW_IMPLICIT)
//                        }, 250)
//
//                    }
//                    else -> {
//                        MaterialAlertDialogBuilder(
//                            requireContext(),
//                            R.style.ThemeOverlay_App_MaterialAlertDialog
//                        ).apply {
//                            setTitle("알림")
//                            setMessage("올바르지 않은 인증번호 입니다.")
//                            setNeutralButton("확인") { dialog, _ ->
//                                dialog.dismiss()
//                            }
//                        }.show()
//                    }
//                }
//            } else {
//                Log.e("failed Verified", "failed To VerifiedCode")
//                Toast.makeText(requireContext(), "인증에 실패했습니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
//            }
        }
    }

    private fun setSnsLoginVisibility(isSnsSignIn : Boolean) {
        when (isSnsSignIn) {
            true -> {
                binding.llPwCondition.visibility = View.GONE
                binding.etPwRepeat.visibility  = View.GONE
                binding.llPwRepeat.visibility = View.GONE
                binding.etPw.visibility  = View.GONE
                binding.ImageView49.visibility = View.GONE
                binding.clSI2Domain.visibility = View.GONE
                binding.btnEmailConfirm.visibility = View.GONE
            }
            false -> {
                binding.llPwCondition.visibility = View.VISIBLE
                binding.etPwRepeat.visibility  = View.VISIBLE
                binding.llPwRepeat.visibility = View.VISIBLE
                binding.etPwRepeat.visibility  = View.VISIBLE
                binding.ImageView49.visibility = View.VISIBLE
                binding.clSI2Domain.visibility = View.VISIBLE
                binding.btnEmailConfirm.visibility = View.VISIBLE
            }

        }


    }
    private fun setNextPage() {
        Handler(Looper.getMainLooper()).postDelayed({
            parentDialog?.setonNextPage()
        }, 600)
    }

    private fun setStep90() {
        parentDialog?.requireView()?.findViewById<StepView>(R.id.svSignIn)?.go(2, true)
        parentDialog?.requireView()?.findViewById<ProgressView>(R.id.pvSignIn)
            ?.let { it.progress = 90f }
        parentDialog?.requireView()?.findViewById<TextView>(R.id.tvSignInGuide)
            ?.let { it.text = "휴대폰 인증을 진행해주세요" }
    }

    private fun disabledSendEmailCode() {
        binding.btnEmailConfirm.apply {
            backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor400, null))
            isEnabled = false
        }
    }
    private fun enabledSendEmailCode() {
        binding.btnEmailConfirm.apply {
            backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.mainColor, null))
            isEnabled = true
        }
    }
    private fun setReSendMessage() {
        binding.clEmailVerify.visibility = View.VISIBLE
        val fullText = binding.tvEmailResendMessage.text
        setVerifyCountDown()
        val spnbString = SpannableString(fullText)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                    setTitle("인증번호 재전송")
                    setMessage("${svm.fullEmail.value}\n이메일로 인증번호를 다시 전송하시겠습니까?")
                    setPositiveButton("예", { _, _ ->

                        // 인증번호 다시 보내기
                        lifecycleScope.launch(Dispatchers.IO) {
                            val jo = JSONObject().apply {
                                put("email", svm.fullEmail.value)
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
        binding.tvEmailResendMessage.text = spnbString
        binding.tvEmailResendMessage.apply {
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
                binding.tvEmailCountDown.visibility = View.VISIBLE
                binding.tvEmailCountDown.text = "${minutes}분 ${seconds}초"
            }

            override fun onFinish() {
                binding.tvEmailCountDown.visibility = View.INVISIBLE
            }
        }.start()
    }
}