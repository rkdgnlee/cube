package com.tangoplus.tangoq.dialog

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
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
import android.transition.TransitionManager
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity.INPUT_METHOD_SERVICE
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.shuhart.stepview.StepView
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.tangoplus.tangoq.IntroActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.etc.SpinnerAdapter
import com.tangoplus.tangoq.api.NetworkUser.emailDuplicateCheck
import com.tangoplus.tangoq.api.NetworkUser.insertUser
import com.tangoplus.tangoq.databinding.FragmentSignInDialogBinding
import com.tangoplus.tangoq.dialog.bottomsheet.AgreementBSDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.function.SecurePreferencesManager.encrypt
import com.tangoplus.tangoq.mediapipe.MathHelpers.phoneNumber82
import com.tangoplus.tangoq.transition.SignInTransition
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import androidx.core.view.isGone
import com.tangoplus.tangoq.api.NetworkUser.mobileDuplicateCheck
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import com.tangoplus.tangoq.adapter.etc.SignInVPAdapter

class SignInDialogFragment : DialogFragment() {
    private lateinit var binding : FragmentSignInDialogBinding
    private lateinit var loadingDialog : LoadingDialogFragment
    private var verificationId = ""
    val svm : SignInViewModel by viewModels()
    private lateinit var auth : FirebaseAuth

    override fun onDestroy() {
        super.onDestroy()
        FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                FirebaseAuth.getInstance().signOut()
                val user = FirebaseAuth.getInstance().currentUser

                user?.delete()
            }
        }
        auth.signOut()
    }

    override fun onStop() {
        super.onStop()
        FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                FirebaseAuth.getInstance().signOut()
                val user = FirebaseAuth.getInstance().currentUser
//                Log.v("user", "$user")
                user?.delete()
            }
        }
        auth.signOut()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSignInDialogBinding.inflate(inflater)
        return binding.root
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

        binding.vpSignIn.adapter = SignInVPAdapter(this@SignInDialogFragment)
        binding.vpSignIn.isUserInputEnabled = false
        loadingDialog = LoadingDialogFragment.newInstance("회원가입전송")

        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        auth = FirebaseAuth.getInstance()
        auth.setLanguageCode("kr")
        binding.ibtnSignInFinish.setOnClickListener {
            if (binding.vpSignIn.currentItem == 1) {
                setonPreviousPage()
            } else {
                MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                    setMessage("회원가입을 종료하시겠습니까?")
                    setPositiveButton("예", {_, _ ->
                        dialog?.dismiss()
                    })
                    setNegativeButton("아니오", {_, _ ->
                        dismiss()
                    })
                    show()
                }
            }
        }
        // -----! 초기 버튼 숨기기 및 세팅 시작 !-----

        // step 2
//        binding.llName.visibility = View.GONE
//        binding.etName.visibility = View.GONE
//        binding.llEmailCondition.visibility = View.GONE
//        binding.llEmail.visibility = View.GONE
//        binding.btnEmailConfirm.visibility = View.GONE
//        binding.llPwCondition.visibility = View.GONE
//        binding.etPw.visibility = View.GONE
//        binding.llPwRepeat.visibility = View.GONE
//        binding.etPwRepeat.visibility = View.GONE
//        binding.btnSignIn.visibility = View.GONE
//        binding.tvEmailCondition.visibility = View.INVISIBLE
//
//        // step1
//        binding.btnAuthConfirm.visibility = View.GONE
//        binding.tvAuthCheck.visibility = View.INVISIBLE
//        binding.tvAuthCountDown.visibility = View.INVISIBLE
//        binding.ibtnAuthAlert.visibility = View.INVISIBLE
//
//        binding.btnAuthSend.isEnabled = false
//        binding.etAuthNumber.isEnabled = false
//        binding.btnAuthConfirm.isEnabled = false
//        binding.btnSignIn.isEnabled = false
//        binding.btnSignIn.backgroundTintList =
//            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400))
//
//        binding.etMobile.requestFocus()
//        binding.etMobile.postDelayed({
//            val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.showSoftInput(binding.etMobile, InputMethodManager.SHOW_IMPLICIT)
//        }, 200)


        binding.svSignIn.state
            .animationType(StepView.ANIMATION_CIRCLE)
            .steps(object : ArrayList<String?>() {
                init {
                    add("휴대폰 인증")
                    add("이메일")
                    add("비밀번호")
                }
            })
            .stepsNumber(4)
            .animationDuration(getResources().getInteger(android.R.integer.config_shortAnimTime))
            .commit()
        // -----! 초기 버튼 숨기기 및 세팅 끝 !-----

        // -----! progress bar 시작 !-----
        binding.pvSignIn.progress = 25f

        // -----! 회원가입 입력 창 anime 시작  !-----
//        TransitionManager.beginDelayedTransition(binding.llSignIn, SignInTransition())

        // -----! 휴대폰 인증 시작 !-----
        val callbacks = object : OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(p0: PhoneAuthCredential) { Log.v("verifyComplete", "PhoneAuthCredential: $p0") }
            override fun onVerificationFailed(p0: FirebaseException) { Log.e("failedAuth", "$p0") }
            @RequiresApi(Build.VERSION_CODES.P)
            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                super.onCodeSent(verificationId, token)
                this@SignInDialogFragment.verificationId = verificationId
                Log.v("onCodeSent", "메시지 발송 성공")
                // -----! 메시지 발송에 성공하면 스낵바 호출 !------
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "메시지 발송에 성공했습니다. 잠시만 기다려주세요", Toast.LENGTH_LONG).show()
//                authBalloonCallback {
//                    binding.etAuthNumber.postDelayed({
//                        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
//                        imm.showSoftInput(binding.etAuthNumber, InputMethodManager.SHOW_IMPLICIT)
//                    }, 250)
//                }
//                binding.btnAuthConfirm.isEnabled = true
//                binding.btnAuthConfirm.visibility = View.VISIBLE
//                binding.ibtnAuthAlert.visibility = View.VISIBLE
//                binding.tvAuthCheck.visibility = View.VISIBLE
//                binding.btnAuthSend.apply {
//                    isEnabled = false
//                    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400))
//                }
//                // 다시 보내기 요청
//                setVerifyCountDown(120)
//                binding.etAuthNumber.requestFocus()
            }
        }

//        binding.ibtnAuthAlert.setOnSingleClickListener {
//            authBalloonCallback { }
//        }
//        binding.btnAuthSend.setOnSingleClickListener {
//            // 유효한 mobile 인지 확인
//            svm.transformMobile = phoneNumber82(binding.etMobile.text.toString())
//            val transformMobile = svm.transformMobile.replace("-", "").replace(" ", "")
//            verifyMobile(callbacks, transformMobile)
//        }
        // -----! 휴대폰 인증 끝 !-----
//        // TODO 어차피 NICE 쓸꺼면 이건 살려두고 재전송을 바꿔야함
//        val fullText = binding.tvAuthCheck.text
//        val spnbString = SpannableString(fullText)
//        val clickableSpan = object : ClickableSpan() {
//            override fun onClick(widget: View) {
//                sendAuthCode(callbacks)
//                Handler(Looper.getMainLooper()).postDelayed({
//                    loadingDialog.dismiss()
//                }, 1000)
//            }
//
//            override fun updateDrawState(ds: TextPaint) {
//                super.updateDrawState(ds)
//                ds.color = resources.getColor(R.color.thirdColor, null)
//            }
//        }
//        val startIndex = fullText.indexOf("재전송")
//        val endIndex = startIndex + "재전송".length
//        spnbString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
//        binding.tvAuthCheck.text = spnbString
//        binding.tvAuthCheck.movementMethod = LinkMovementMethod.getInstance()
//        binding.etAuthNumber.setOnEditorActionListener { _, actionId, event ->
//            if (actionId == EditorInfo.IME_ACTION_DONE ||
//                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
//                if (binding.btnAuthConfirm.isVisible) {
//                    clickBtnAuthConfirm()
//                }
//                true  // 이벤트 처리가 완료되었음을 반환
//            } else {
//                false // 다른 동작들은 그대로 유지
//            }
//        }
//        binding.btnAuthConfirm.setOnSingleClickListener {
//            clickBtnAuthConfirm()
//        }  // -----! 인증 문자 확인 끝 !-----
//
//        // 이름 조건
//        val nameRegex = "^[가-힣]{2,5}$|^[a-zA-Z]{2,20}$"
//        val namePatternCheck = Pattern.compile(nameRegex)
//        binding.etName.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                svm.nameCondition.value = namePatternCheck.matcher(binding.etName.text.toString()).find()
//                if (svm.nameCondition.value == true) {
//                    binding.tvNameCondition.setTextColor(binding.tvNameCondition.resources.getColor(R.color.successColor, null))
//                    binding.tvNameCondition.text = "올바른 형식입니다"
//                } else {
//                    binding.tvNameCondition.setTextColor(binding.tvNameCondition.resources.getColor(R.color.deleteColor, null))
//                    binding.tvNameCondition.text = "올바른 이름을 입력해주세요"
//                }
//            }
//        })
//        binding.etName.setOnEditorActionListener { _, actionId, _ ->
//            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
//                binding.etEmailId.requestFocus()
//                return@setOnEditorActionListener true
//            }
//            false
//        }
//
//        // ----------------------------------# 비밀번호 패턴 체크 #----------------------------------
//        val mobilePattern = "^010-\\d{4}-\\d{4}\$"
//        val mobilePatternCheck = Pattern.compile(mobilePattern)
//
//        // ------! 핸드폰 번호 조건 코드 !-----
//        binding.etMobile.addTextChangedListener(object: TextWatcher {
//            private var isFormatting = false
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//
//                if (isFormatting) return
//                isFormatting = true
//                val cleaned = s.toString().replace("-", "")
//                val maxDigits = 11
//                val limited = if (cleaned.length > maxDigits) cleaned.substring(0, maxDigits) else cleaned
//
//
//                val formatted = when {
//                    limited.length <= 3 -> limited
//                    limited.length <= 7 -> "${limited.substring(0, 3)}-${limited.substring(3)}"
//                    else -> "${limited.substring(0, 3)}-${limited.substring(3, 7)}-${limited.substring(7)}"
//                }
//
//                // 기존 입력과 다를 때만 업데이트
//                if (s.toString() != formatted && s != null) {
//                    s.replace(0, s.length, formatted)
//                }
//
//                isFormatting = false
//                svm.mobileCondition.value = mobilePatternCheck.matcher(binding.etMobile.text.toString()).find()
//                if (svm.mobileCondition.value == true) {
//                    binding.btnAuthSend.isEnabled = true
//                } else {
//                    binding.btnAuthSend.isEnabled = false
//                }
//            }
//        })
//        binding.etMobile.setOnEditorActionListener { v, actionId, event ->
//            if (actionId == EditorInfo.IME_ACTION_DONE ||
//                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
//                sendAuthCode(callbacks)
//                true  // 이벤트 처리가 완료되었음을 반환
//            } else {
//                false // 다른 동작들은 그대로 유지
//            }
//        }
//
//
//        svm.allTrueLiveData.observe(this) {
//            if (it) {
//                binding.pvSignIn.progress = 100f
//                binding.tvSignInGuide.text = "가입을 위해 버튼을 눌러주세요"
//                binding.btnSignIn.apply {
//                    isEnabled = it
//                    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
//                }
//            } else {
//                binding.btnSignIn.apply {
//                    isEnabled = it
//                    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400))
//                }
//            }
//        }
//
//        // 버튼 누르면 회원가입 창 나오기
//        binding.btnSignIn.setOnSingleClickListener {
//            showAgreementBottomSheetDialog(requireActivity())
//        }
    }
    private fun clickBtnAuthConfirm() {
//        val credential = PhoneAuthProvider.getCredential(verificationId, binding.etAuthNumber.text.toString())
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            signInWithPhoneAuthCredential(credential)
//        }
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.P)
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                svm.countDownTimer?.cancel()
                lifecycleScope.launch {
                    svm.mobileAuthCondition.value = true
                    firebaseAuthDelete()
                    // 전화번호 절차 통과
//                    binding.llName.visibility = View.VISIBLE
//                    binding.etName.visibility = View.VISIBLE
//                    binding.llEmailCondition.visibility = View.VISIBLE
//                    binding.llEmail.visibility = View.VISIBLE
//                    binding.btnEmailConfirm.visibility = View.VISIBLE
//                    binding.llPwCondition.visibility = View.VISIBLE
//                    binding.etPw.visibility = View.VISIBLE
//                    binding.llPwRepeat.visibility = View.VISIBLE
//                    binding.etPwRepeat.visibility = View.VISIBLE
//                    binding.btnSignIn.visibility = View.VISIBLE
//
//                    // 카운트다운 삭제
//                    svm.countDownTimer?.cancel()
//                    binding.tvAuthCountDown.visibility = View.GONE
//                    binding.tvAuthCheck.visibility = View.GONE
//                    binding.ibtnAuthAlert.visibility = View.GONE
//                    binding.etAuthNumber.isEnabled = false
//                    binding.etMobile.isEnabled = false
//                    binding.btnAuthSend.apply {
//                        isEnabled = false
//                        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400))
//                    }
//                    binding.btnAuthConfirm.apply {
//                        isEnabled = false
//                        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400))
//                    }
//                    svm.phoneCondition.value = true
//                    // ------! 번호 인증 완료 !------
//
//
//                    val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
//                    alphaAnimation.duration = 600
//                    binding.llName.startAnimation(alphaAnimation)
//                    binding.etName.startAnimation(alphaAnimation)
//                    binding.llEmailCondition.startAnimation(alphaAnimation)
//                    binding.llEmail.startAnimation(alphaAnimation)
//                    binding.btnEmailConfirm.startAnimation(alphaAnimation)
//                    binding.llPwCondition.startAnimation(alphaAnimation)
//                    binding.etPw.startAnimation(alphaAnimation)
//                    binding.llPwRepeat.startAnimation(alphaAnimation)
//                    binding.etPwRepeat.startAnimation(alphaAnimation)
//                    binding.btnSignIn.startAnimation(alphaAnimation)
//
//                    binding.tvNameGuide.startAnimation(alphaAnimation)
//                    binding.etName.startAnimation(alphaAnimation)
//                    binding.llEmail.startAnimation(alphaAnimation)
//                    binding.tvSignInGuide.text = "이름을 입력해주세요"
//                    val objectAnimator = ObjectAnimator.ofFloat(binding.clMobile, "translationY", 1f)
//                    objectAnimator.duration = 1000
//                    objectAnimator.start()
//                    binding.pvSignIn.progress = 50f
//                    binding.svSignIn.go(1, true)
//                    // ------! 번호 인증 완료 !------
//
//                    Toast.makeText(requireContext(), "인증에 성공했습니다 !", Toast.LENGTH_SHORT).show()
//
//                    // 이름에 키보드 올리기
//                    Handler(Looper.getMainLooper()).postDelayed({
//                        binding.etName.requestFocus()
//                        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
//                        imm.showSoftInput(binding.etName, InputMethodManager.SHOW_IMPLICIT)
//                    }, 1050)
//
//                    binding.btnAuthSend.isEnabled = false
//                    binding.btnAuthConfirm.isEnabled = false
//                    if (svm.mobileCondition.value == true) {
//                        // 전화번호 "-" 표시 전부 없애기
//                        val mobile = binding.etMobile.text.toString().replace("-", "").replace(" ", "")
////                            Log.v("모바일인증완료", mobile)
//                        svm.User.value?.put("mobile", mobile)
//                    }
                }
            } else {
                Toast.makeText(requireContext(), "인증에 실패했습니다.", Toast.LENGTH_SHORT).show()
                Log.w(ContentValues.TAG, "mobile auth failed.")
            }
            }
    }



    private fun setVerifyCountDown(retryAfter: Int) {
        if (retryAfter != -1) {
            svm.countDownTimer?.cancel() // 기존 카운트다운 취소
//            svm.countDownTimer = object : CountDownTimer((retryAfter * 1000).toLong(), 1000) {
//                override fun onTick(millisUntilFinished: Long) {
//                    val remainingSeconds = millisUntilFinished / 1000
//                    val minutes = remainingSeconds / 60
//                    val seconds = remainingSeconds % 60
//                    binding.tvAuthCountDown.visibility = View.VISIBLE
//                    binding.tvAuthCountDown.text = "${minutes}분 ${seconds}초"
//                }
//                override fun onFinish() {
//                    binding.tvAuthCountDown.visibility = View.INVISIBLE
//                }
//            }.start()
        }
    }
    private fun verifyMobile (callbacks: OnVerificationStateChangedCallbacks, mobile: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val statusCode = mobileDuplicateCheck(getString(R.string.API_user), mobile )
            withContext(Dispatchers.Main) {
                when (statusCode) {
                    200 -> sendAuthCode(callbacks)
                    409 -> {
                        withContext(Dispatchers.Main) {
                            MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                                setTitle("회원 가입 실패")
                                setMessage("이미 존재하는 회원 정보입니다. 로그인을 진행해주세요")
                                setPositiveButton("예") { _, _ ->
//                                    binding.etAuthNumber.isEnabled = true
//                                    binding.etMobile.isEnabled = true
//                                    binding.btnAuthConfirm.isEnabled = true
//                                    binding.btnAuthSend.isEnabled = true
//                                    binding.btnAuthConfirm.backgroundTintList  = ColorStateList.valueOf(resources.getColor(R.color.mainColor, null))
//                                    binding.btnAuthSend.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.mainColor, null))
                                }
                                show()
                            }
                        }
                    }
                    else -> Toast.makeText(requireContext(), "네트워크 오류입니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun sendAuthCode(callbacks: OnVerificationStateChangedCallbacks) {
//        svm.transformMobile = phoneNumber82(binding.etMobile.text.toString())
//
//        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
//            setTitle("📩 문자 인증 ")
//            setMessage("${svm.transformMobile}로 인증 하시겠습니까?")
//            setPositiveButton("예") { _, _ ->
//                svm.transformMobile = svm.transformMobile.replace("-", "").replace(" ", "")
//                Log.w("전화번호", svm.transformMobile)
//
//                val optionsCompat = PhoneAuthOptions.newBuilder(auth)
//                    .setPhoneNumber(svm.transformMobile)
//                    .setTimeout(120L, TimeUnit.SECONDS)
//                    .setActivity(requireActivity())
//                    .setCallbacks(callbacks)
//                    .build()
//                PhoneAuthProvider.verifyPhoneNumber(optionsCompat)
//                Log.d("PhoneAuth", "verifyPhoneNumber called")
//
//                // 문자보내고 난 후 UI 떠오르기 등
//                binding.tvSignInGuide.text = "인증번호를 입력해주세요"
//                val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
//                alphaAnimation.duration = 600
//                binding.etAuthNumber.isEnabled = true
//
//                val objectAnimator = ObjectAnimator.ofFloat(binding.clMobile, "translationY", 1f)
//                objectAnimator.duration = 1000
//                objectAnimator.start()
//                binding.etAuthNumber.requestFocus()
//                loadingDialog.show(requireActivity().supportFragmentManager, "LoadingDialogFragment")
//            }
//            setNegativeButton("아니오", null)
//            show()
//        }
    }



    private fun cancelCountDown() {
//        svm.countDownTimer?.cancel()
//        binding.tvAuthCountDown.visibility = View.INVISIBLE
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    private fun firebaseAuthDelete() {
        FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                FirebaseAuth.getInstance().signOut()
                val user = FirebaseAuth.getInstance().currentUser

                user?.delete()
            }
        }
        auth.signOut()
    }

    private fun authBalloonCallback(callback : () -> Unit) {
        // ------# 인증번호 balloon #------
//        val balloon = Balloon.Builder(requireContext())
//            .setWidthRatio(0.5f)
//            .setHeight(BalloonSizeSpec.WRAP)
//            .setText("인증번호가 오지 않는다면 스팸함 및 국외발신 차단함을 확인해주세요")
//            .setTextColorResource(R.color.subColor800)
//            .setTextSize(15f)
//            .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
//            .setArrowSize(0)
//            .setMargin(10)
//            .setPadding(12)
//            .setCornerRadius(8f)
//            .setBackgroundColorResource(R.color.white)
//            .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
//            .setLifecycleOwner(viewLifecycleOwner)
//            .setOnBalloonDismissListener { callback() }
//            .build()
//        balloon.setOnBalloonClickListener { balloon.dismiss() }
//        balloon.showAlignTop(binding.ibtnAuthAlert)
//        balloon.dismissWithDelay(3000L)

    }
    fun setonNextPage() {
        binding.vpSignIn.setCurrentItem(1, true)
    }
    fun setonPreviousPage() {
        binding.vpSignIn.setCurrentItem(0, true)
    }
}