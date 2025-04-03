package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
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
import com.tangoplus.tangoq.IntroActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.etc.SpinnerAdapter
import com.tangoplus.tangoq.api.NetworkUser.sendEmailOTP
import com.tangoplus.tangoq.api.NetworkUser.verifyEmailOTP
import com.tangoplus.tangoq.databinding.FragmentSignIn1Binding
import com.tangoplus.tangoq.dialog.LoadingDialogFragment
import com.tangoplus.tangoq.dialog.SignInDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.scrollToView
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.function.AuthManager.pwPatternCheck
import com.tangoplus.tangoq.function.AuthManager.setRetryAuthMessage
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


        loadingDialog = LoadingDialogFragment.newInstance("회원가입확인")
        parentDialog  = (requireParentFragment() as SignInDialogFragment)
        svm.fullEmail.value = ""
        svm.passMobile.value = ""
        svm.emailVerify.value = false
        svm.pwCompare.value = false
        svm.pwCondition.value = false
        svm.nameCondition.value = false
        svm.emailCondition.value = false
        svm.emailIdCondition.value = false
        svm.domainCondition.value = false
        binding.clPw.visibility = View.GONE


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
        val emailIdRegex = "^[a-zA-Z0-9][a-zA-Z0-9%_-]*(\\.(?!\\.)[a-zA-Z0-9%_-]+)*[a-zA-Z0-9%_-]\$"
        val emailIdPatternCheck = Pattern.compile(emailIdRegex)
        binding.etEmailId.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.toString().isNotEmpty()) {
                    svm.emailIdCondition.value = emailIdPatternCheck.matcher(binding.etEmailId.text.toString()).find()
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
                Log.v("이메일", "$position ${binding.spinner.selectedItem}")
                if (position == 3) {
                    binding.etEmail.visibility = View.VISIBLE
                    binding.spinner.visibility = View.INVISIBLE
                    svm.domainCondition.value = false
                    binding.etEmail.setText("")
                } else {
                    binding.etEmail.visibility = View.GONE
                    binding.spinner.visibility = View.VISIBLE
                    svm.domainCondition.value = true
                    binding.spinner.setSelection(position)

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
                    binding.btnEmailSend.apply {
                        isEnabled = true
                        backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.mainColor, null))
                    }
                    enabledSendEmailCode()
                }
                false -> {
                    if (binding.etEmailId.text.isNotEmpty()) {
                        binding.tvEmailCondition.text = "올바른 이메일 형식을 입력해주세요"
                        binding.tvEmailCondition.setTextColor(resources.getColor(R.color.deleteColor, null))
                        binding.btnEmailSend.apply {
                            isEnabled = false
                            backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor400, null))
                        }
                    }
                    disabledSendEmailCode()
                }
                else -> {
                    binding.btnEmailSend.apply {
                        isEnabled = false
                        backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor400, null))
                    }
                    disabledSendEmailCode()
                }
            }
        }
        // emailId에서 바로 enter쳤을 경우 이메일 중복확인
        binding.etEmailId.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                if (svm.emailCondition.value == true) sendEmailCode()
                return@setOnEditorActionListener true
            }
            false
        }
        binding.btnEmailSend.setOnSingleClickListener {
            sendEmailCode()
        }
        binding.etEmailAuthNumber.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int, ) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int, ) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.length ==6) {
                    confirmEmail()
                }
            }
        })

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

        val pwTextWatcher = pwPatternCheck(svm, binding.etPw, binding.tvPwCondition, false)
        binding.etPw.addTextChangedListener(pwTextWatcher)

        // 비밀번호 확인
        val pwRepeatTextWatcher = pwPatternCheck(svm, binding.etPwRepeat, binding.tvPwRepeat, true)
        binding.etPwRepeat.addTextChangedListener(pwRepeatTextWatcher)

        binding.etPwRepeat.imeOptions = EditorInfo.IME_ACTION_DONE
        binding.etPwRepeat.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                if (svm.pwBothTrue.value == true) {
                    parentDialog?.setonNextPage()
                    svm.isNextPage.value = true
                }
                true  // 이벤트 처리가 완료되었음을 반환
            } else {
                false // 다른 동작들은 그대로 유지
            }
        }

        svm.pwBothTrue.observe(viewLifecycleOwner) {
            if (it) {
                svm.pw.value = binding.etPwRepeat.text.toString()
                binding.btnSI1Next.apply {
                    isEnabled = true
                    backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.mainColor, null))
                }
            } else {
                binding.btnSI1Next.apply {
                    isEnabled = false
                    backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor400, null))
                }
            }
        }
        binding.btnSI1Next.setOnSingleClickListener {
            parentDialog?.setonNextPage()
            svm.isNextPage.value = true
        }
    }
    private fun sendEmailCode() {
        val emailId = binding.etEmailId.text.toString()
        svm.fullEmail.value = if (binding.etEmail.isGone) emailId + "@" + binding.spinner.selectedItem.toString() else emailId + "@" + binding.etEmail.text.toString()
        Log.v("이메일 전체", "${svm.fullEmail.value}")

        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setTitle("인증번호 전송")
            setMessage("${svm.fullEmail.value}로 인증하시겠습니까?")
            setPositiveButton("예") { _, _ ->
                if (parentDialog?.isVisible == true) {
                    loadingDialog = LoadingDialogFragment.newInstance("회원가입확인")
                    loadingDialog.show(requireActivity().supportFragmentManager, "LoadingDialogFragment")
                }
                lifecycleScope.launch(Dispatchers.IO) {
                    val jo = JSONObject().apply {
                        put("email", svm.fullEmail.value)
                    }
                    val responsePair = sendEmailOTP(getString(R.string.API_user), jo.toString())
                    withContext(Dispatchers.Main) {
                        if (loadingDialog.isAdded || loadingDialog.isVisible) {
                            loadingDialog.dismiss()
                        }
                        navigateEmailCodeCase(responsePair)
                    }
                }
            }
            setNegativeButton("아니오") { _, _ -> }
        }.show()
    }
    private fun confirmEmail() {
        CoroutineScope(Dispatchers.IO).launch {
            val bodyJo = JSONObject().apply {
                put("email", svm.fullEmail.value)
                put("otp", binding.etEmailAuthNumber.text.toString())
            }
            if (parentDialog?.isAdded == true) {
                loadingDialog.dismiss()
            }
            val statusCode = verifyEmailOTP(getString(R.string.API_user), bodyJo.toString()) ?: 0
            withContext(Dispatchers.Main) {
                navigateEmailVerifyCase(statusCode)
            }
        }
    }

    private fun setSnsLoginVisibility(isSnsSignIn : Boolean) {
        when (isSnsSignIn) {
            true -> {
                binding.tvPwTitle.visibility = View.GONE
                binding.tvPwRepeat.visibility = View.GONE
                binding.tvPwCondition.visibility = View.GONE
                binding.tvPwRepeatTitle.visibility = View.GONE
                binding.etPwRepeat.visibility  = View.GONE
                binding.etPw.visibility  = View.GONE
                binding.ImageView49.visibility = View.GONE
                binding.clSI2Domain.visibility = View.GONE
                binding.btnEmailSend.visibility = View.GONE
            }
            false -> {
                binding.tvPwTitle.visibility = View.VISIBLE
                binding.tvPwRepeat.visibility = View.VISIBLE
                binding.tvPwCondition.visibility = View.VISIBLE
                binding.tvPwRepeatTitle.visibility = View.VISIBLE
                binding.etPwRepeat.visibility  = View.VISIBLE
                binding.etPwRepeat.visibility  = View.VISIBLE
                binding.ImageView49.visibility = View.VISIBLE
                binding.clSI2Domain.visibility = View.VISIBLE
                binding.btnEmailSend.visibility = View.VISIBLE
            }
        }
    }

    private fun disabledSendEmailCode() {
        binding.btnEmailSend.apply {
            backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor400, null))
            isEnabled = false
        }
    }
    private fun enabledSendEmailCode() {
        binding.btnEmailSend.apply {
            backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.mainColor, null))
            isEnabled = true
        }
    }
    private fun setReSendMessage() {
        svm.countDownTimer?.cancel()
        binding.clEmailVerify.visibility = View.VISIBLE
        val madb = MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setTitle("인증번호 재전송")
            setMessage("${svm.fullEmail.value}\n이메일로 인증번호를 다시 전송하시겠습니까?")
            setPositiveButton("예") { _, _ ->
                if (parentDialog?.isVisible == true) {
                    loadingDialog = LoadingDialogFragment.newInstance("회원가입확인")
                    loadingDialog.show(requireActivity().supportFragmentManager, "LoadingDialogFragment")
                }
                lifecycleScope.launch(Dispatchers.IO) {
                    val jo = JSONObject().apply {
                        put("email", svm.fullEmail.value)
                    }
                    val responsePair = sendEmailOTP(getString(R.string.API_user), jo.toString())
                    withContext(Dispatchers.Main) {
                        if (loadingDialog.isAdded || loadingDialog.isVisible) {
                            loadingDialog.dismiss()
                        }
                        navigateEmailCodeCase(responsePair)
                    }
                }
            }
            setNegativeButton("아니오") { _, _ -> }
        }
        setRetryAuthMessage(requireActivity(), svm, binding.tvEmailResendMessage, binding.tvEmailCountDown, madb)
    }

    private fun navigateEmailCodeCase(responsePair : Pair<Int, String?>?) {
        when (responsePair?.first) {
            200, 201 -> {
                // stepView 넘기
                parentDialog?.requireView()?.findViewById<ProgressView>(R.id.pvSignIn)?.let { it.progress = 40f }
                parentDialog?.requireView()?.findViewById<TextView>(R.id.tvSignInGuide)?.let { it.text = "인증번호를 입력해주세요" }


                Toast.makeText(requireContext(), "인증번호를 전송했습니다. 이메일을 확인해주세요", Toast.LENGTH_LONG).show()
                setReSendMessage()
                binding.etEmailId.apply {
                    isEnabled = false
                    backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
                }
                binding.etEmail.apply {
                    isEnabled = false
                    backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
                }
                binding.btnEmailSend.apply {
                    isEnabled = false
                    backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor400, null))
                }
                binding.spinner.isEnabled = false
                binding.ivSpinner.isEnabled = false

                binding.etEmailAuthNumber.apply {
                    requestFocus()

                }

            }
            404 -> {
                Toast.makeText(requireContext(), "올바르지 않은 이메일입니다", Toast.LENGTH_LONG).show()
            }
            429 -> {
                Toast.makeText(requireContext(), "요청 가능 횟수를 초과했습니다. 잠시후 다시 시도해주세요.", Toast.LENGTH_LONG).show()
            }
            409 -> {
                val loginCase = when (responsePair.second) {
                    "google" -> "구글"
                    "naver" -> "네이버"
                    "kakao" -> "카카오"
                    else -> ""
                }
                val intentCase = when (responsePair.second) {
                    "google" -> "1"
                    "naver" -> "2"
                    "kakao" -> "3"
                    else -> "0"
                }
                svm.emailCondition.value = false
                MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                    setMessage("이미 존재하는 회원입니다. ${loginCase} 로그인을 진행하시겠습니까?")
                    setPositiveButton("예", { _, _ ->
                        val intent = Intent(requireActivity(), IntroActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        intent.putExtra("SignInFinished", when (intentCase) {
                            "1" -> 4091
                            "2" -> 4092
                            "3" -> 4093
                            else -> 409
                        })

                        parentDialog?.dismiss()
                        startActivity(intent)
                    })
                    setNegativeButton("아니오", {_, _ ->
                        binding.etEmailId.apply {
                            setText("")
                            requestFocus()
                            postDelayed({
                                val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                                imm.showSoftInput(binding.etEmailId, InputMethodManager.SHOW_IMPLICIT)
                            }, 250)
                        }
                    })
                }.show()
            }
            500 -> {
                Toast.makeText(requireContext(), "서버 오류 입니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_LONG).show()
            }
            else -> {
                Toast.makeText(requireContext(), "전송이 실패했습니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun navigateEmailVerifyCase(statusCode : Int) {
        when (statusCode) {
            200, 201 -> {
                binding.clPw.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "인증이 완료됐습니다 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show()
                // 인증 검증에 관련된 VIew 멈추고 숨기기
                svm.countDownTimer?.cancel()

                binding.etEmailId.apply {
                    isEnabled = false
                    backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
                }
                binding.etEmailAuthNumber.apply {
                    isEnabled = false
                    backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
                }
                binding.spinner.isEnabled = false
                binding.ivSpinner.isEnabled = false
                binding.tvEmailResendMessage.visibility = View.GONE
                binding.tvEmailCountDown.visibility = View.GONE
                svm.emailVerify.value = true
                // 다음 단계로 포커싱
                binding.etPw.postDelayed({
                    binding.etPw.requestFocus()
                    val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(binding.etPw, InputMethodManager.SHOW_IMPLICIT)
                    scrollToView(binding.etPw, binding.nsvSI1)
                }, 250)
                parentDialog?.requireView()?.findViewById<StepView>(R.id.svSignIn)?.go(1, true)
                parentDialog?.requireView()?.findViewById<ProgressView>(R.id.pvSignIn)?.let { it.progress = 66f }
                parentDialog?.requireView()?.findViewById<TextView>(R.id.tvSignInGuide)?.let { it.text = "비밀번호를 입력해주세요" }

            }
            401 -> {
                MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                    setTitle("알림")
                    setMessage("만료 혹은 올바르지 않은 인증번호입니다. 다시 시도해주세요")
                    setPositiveButton("예", {_, _ ->
                        binding.etEmailAuthNumber.setText("")
                    })
                }.show()
            }
            500 -> {
                Toast.makeText(requireContext(), "서버 오류 입니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(requireContext(), "인증에 실패했습니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

}