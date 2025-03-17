package com.tangoplus.tangoq.dialog

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
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
import com.tangoplus.tangoq.IntroActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.etc.SpinnerAdapter
import com.tangoplus.tangoq.api.NetworkUser.idDuplicateCheck
import com.tangoplus.tangoq.api.NetworkUser.insertUser
import com.tangoplus.tangoq.databinding.FragmentSignInDialogBinding
import com.tangoplus.tangoq.dialog.bottomsheet.AgreementBSDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.listener.OnSingleClickListener
import com.tangoplus.tangoq.mediapipe.MathHelpers.phoneNumber82
import com.tangoplus.tangoq.transition.SignInTransition
import com.tangoplus.tangoq.viewmodel.SignInViewModel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class SignInDialogFragment : DialogFragment() {
    private lateinit var binding : FragmentSignInDialogBinding

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


        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        auth = FirebaseAuth.getInstance()
        auth.setLanguageCode("kr")
        binding.ibtnSignInFinish.setOnClickListener { dismiss() }
        // -----! 초기 버튼 숨기기 및 세팅 시작 !-----
        binding.llPwCondition.visibility = View.GONE
        binding.etPw.visibility = View.GONE
        binding.llPwRepeat.visibility = View.GONE
        binding.etPwRepeat.visibility = View.GONE
        binding.btnSignIn.visibility = View.GONE
        binding.llId.visibility = View.GONE
        binding.llIdCondition.visibility = View.GONE
        binding.tvNameGuide.visibility = View.GONE
        binding.etName.visibility = View.GONE
        binding.llEmail.visibility = View.GONE
        binding.btnEmailNext.visibility = View.GONE
        binding.btnAuthSend.isEnabled = false
        binding.etAuthNumber.isEnabled = false
        binding.btnAuthConfirm.isEnabled = false
        binding.btnSignIn.isEnabled = false
        binding.btnSignIn.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400))


        binding.etMobile.requestFocus()
        binding.etMobile.postDelayed({
            val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etMobile, InputMethodManager.SHOW_IMPLICIT)
        }, 200)


        binding.svSignIn.state
            .animationType(StepView.ANIMATION_CIRCLE)
            .steps(object : ArrayList<String?>() {
                init {
                    add("휴대폰 인증")
                    add("이름, 이메일")
                    add("아이디")
                    add("비밀번호")
                }
            })
            .stepsNumber(4)
            .animationDuration(getResources().getInteger(android.R.integer.config_shortAnimTime))
            // other state methods are equal to the corresponding xml attributes
            .commit()
        // -----! 초기 버튼 숨기기 및 세팅 끝 !-----

        // -----! progress bar 시작 !-----
        binding.pvSignIn.progress = 25f

        // -----! 회원가입 입력 창 anime 시작  !-----
        TransitionManager.beginDelayedTransition(binding.llSignIn, SignInTransition())

        // -----! 휴대폰 인증 시작 !-----
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
                this@SignInDialogFragment.verificationId = verificationId
                Log.v("onCodeSent", "메시지 발송 성공")
                // -----! 메시지 발송에 성공하면 스낵바 호출 !------
                Toast.makeText(requireContext(), "메시지 발송에 성공했습니다. 잠시만 기다려주세요", Toast.LENGTH_LONG).show()
                binding.btnAuthConfirm.isEnabled = true
            }
        }

        binding.btnAuthSend.setOnSingleClickListener {
            svm.transformMobile = phoneNumber82(binding.etMobile.text.toString())
            MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                setTitle("📩 문자 인증 ")
                setMessage("${svm.transformMobile}로 인증 하시겠습니까?")
                setPositiveButton("예") { _, _ ->
                    svm.transformMobile = svm.transformMobile.replace("-", "").replace(" ", "")
                    Log.w("전화번호", svm.transformMobile)

                    val optionsCompat = PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(svm.transformMobile)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(requireActivity())
                        .setCallbacks(callbacks)
                        .build()

                    PhoneAuthProvider.verifyPhoneNumber(optionsCompat)
                    Log.d("PhoneAuth", "verifyPhoneNumber called")

                    val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
                    alphaAnimation.duration = 600
                    binding.etAuthNumber.isEnabled = true
                    binding.btnAuthConfirm.visibility = View.VISIBLE

                    val objectAnimator = ObjectAnimator.ofFloat(binding.clMobile, "translationY", 1f)
                    objectAnimator.duration = 1000
                    objectAnimator.start()
                    binding.etAuthNumber.requestFocus()
                }
                setNegativeButton("아니오", null)
                show()
            }
        }
        // -----! 휴대폰 인증 끝 !-----

        binding.btnAuthConfirm.setOnSingleClickListener {
            val credential = PhoneAuthProvider.getCredential(verificationId, binding.etAuthNumber.text.toString())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                signInWithPhoneAuthCredential(credential)
            }
        }  // -----! 인증 문자 확인 끝 !-----

        // 이름 조건
        val nameRegex = "^[가-힣]{2,5}$|^[a-zA-Z]{2,20}$"
        val namePatternCheck = Pattern.compile(nameRegex)
        binding.etName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                svm.nameCondition.value = namePatternCheck.matcher(binding.etName.text.toString()).find()
                if (svm.nameCondition.value == true) {
                    binding.tvNameCondition.setTextColor(binding.tvIdCondition.resources.getColor(R.color.successColor, null))
                    binding.tvNameCondition.text = "올바른 형식입니다"
                } else {
                    binding.tvNameCondition.setTextColor(binding.tvIdCondition.resources.getColor(R.color.deleteColor, null))
                    binding.tvNameCondition.text = "올바른 이름을 입력해주세요"
                }
            }
        })
        binding.etName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.etEmail.requestFocus()
                return@setOnEditorActionListener true
            }
            false
        }

        // email id check
        val emailRegex = "^[a-zA-Z0-9]{4,16}$"
        val emailPatternCheck = Pattern.compile(emailRegex)
        binding.etEmailId.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                svm.emailCondition.value = emailPatternCheck.matcher(binding.etEmailId.text.toString()).find()
            }
        })
        binding.etEmailId.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
                alphaAnimation.duration = 750
                binding.etId.startAnimation(alphaAnimation)
                binding.llId.startAnimation(alphaAnimation)
                binding.llPwCondition.startAnimation(alphaAnimation)
                binding.etPw.startAnimation(alphaAnimation)
                binding.llPwRepeat.startAnimation(alphaAnimation)
                binding.etPwRepeat.startAnimation(alphaAnimation)
                binding.btnSignIn.startAnimation(alphaAnimation)

                binding.llIdCondition.visibility = View.VISIBLE
                binding.llId.visibility = View.VISIBLE
                binding.llPwCondition.visibility = View.VISIBLE
                binding.etPw.visibility = View.VISIBLE
                binding.llPwRepeat.visibility = View.VISIBLE
                binding.etPwRepeat.visibility = View.VISIBLE
                binding.btnSignIn.visibility = View.VISIBLE
                val objectAnimator = ObjectAnimator.ofFloat(binding.clMobile, "translationY", 1f)
                objectAnimator.duration = 1000
                objectAnimator.start()

                Handler(Looper.getMainLooper()).postDelayed({ binding.etId.requestFocus() }, 750)

                binding.pvSignIn.progress = 75f
                binding.svSignIn.go(2, true)
                return@setOnEditorActionListener true
            }
            false
        }

        binding.btnEmailNext.setOnSingleClickListener {
            val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
            alphaAnimation.duration = 750
            binding.etId.startAnimation(alphaAnimation)
            binding.llId.startAnimation(alphaAnimation)
            binding.llPwCondition.startAnimation(alphaAnimation)
            binding.etPw.startAnimation(alphaAnimation)
            binding.llPwRepeat.startAnimation(alphaAnimation)
            binding.etPwRepeat.startAnimation(alphaAnimation)
            binding.btnSignIn.startAnimation(alphaAnimation)

            binding.llIdCondition.visibility = View.VISIBLE
            binding.llId.visibility = View.VISIBLE
            binding.llPwCondition.visibility = View.VISIBLE
            binding.etPw.visibility = View.VISIBLE
            binding.llPwRepeat.visibility = View.VISIBLE
            binding.etPwRepeat.visibility = View.VISIBLE
            binding.btnSignIn.visibility = View.VISIBLE
            val objectAnimator = ObjectAnimator.ofFloat(binding.clMobile, "translationY", 1f)
            objectAnimator.duration = 1000
            objectAnimator.start()

            binding.pvSignIn.progress = 75f
            binding.svSignIn.go(2, true)
            Handler(Looper.getMainLooper()).postDelayed({ binding.etId.requestFocus() }, 750)
        }


        val domainList = listOf("gmail.com", "naver.com", "kakao.com", "직접입력")
        binding.spinner.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, domainList, 0)
        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            @SuppressLint("SetTextI18n")
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                binding.spinner.getItemAtPosition(position).toString()
                if (position == 3) {
                    binding.etEmail.visibility = View.VISIBLE
                    binding.spinner.visibility = View.GONE
                    binding.ivSpinner.setOnClickListener{
                        binding.spinner.performClick()
                        binding.spinner.visibility = View.VISIBLE
                    }

                } else {
                    binding.etEmail.visibility = View.GONE
                    binding.etEmail.setText("")
                    binding.spinner.visibility = View.VISIBLE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        binding.btnIdCondition.setOnClickListener {
            val id = binding.etId.text.toString()
            CoroutineScope(Dispatchers.IO).launch {
                val responseCode = idDuplicateCheck(getString(R.string.API_user), id)
                withContext(Dispatchers.Main) {
                    when (responseCode) {
                        201 -> {
                            MaterialAlertDialogBuilder(
                                requireContext(),
                                R.style.ThemeOverlay_App_MaterialAlertDialog
                            ).apply {
                                setTitle("알림")
                                setMessage("사용가능한 아이디입니다.\n이 아이디를 사용하시겠습니까?")
                                setPositiveButton("예") { _, _ ->
                                    binding.btnIdCondition.isEnabled = false
                                    binding.btnIdCondition.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor400, null))
                                    binding.etId.isEnabled = false
                                    svm.User.value?.put("user_id", id)
//                                    Log.v("id들어감", "${svm.User.value?.getString("user_id")}")
                                    binding.pvSignIn.progress = 100f
                                    binding.svSignIn.go(3, true)
                                    binding.tvSignInGuide.text = "비밀번호를 입력해주세요"
                                    svm.invalidIdCondition.value = true
                                }
                                setNegativeButton("아니오") { dialog, _ ->
                                    dialog.dismiss()
                                }
                            }.show()
                        }
                        else -> {
                            MaterialAlertDialogBuilder(
                                requireContext(),
                                R.style.ThemeOverlay_App_MaterialAlertDialog
                            ).apply {
                                setTitle("알림")
                                setMessage("이미 사용중인 아이디입니다.")
                                setNeutralButton("확인") { dialog, _ ->
                                    binding.tvIdCondition.text = "사용중인 아이디입니다. 새로운 아이디를 만들어주세요"
                                    binding.tvIdCondition.setTextColor(ContextCompat.getColor(requireContext(), R.color.deleteColor))
                                    dialog.dismiss()
                                }
                            }.show()
                        }
                    }
                }
            }
        }

        binding.etId.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
                alphaAnimation.duration = 600

                val objectAnimator = ObjectAnimator.ofFloat(binding.clMobile, "translationY", 1f)
                objectAnimator.duration = 1000
                objectAnimator.start()

                return@setOnEditorActionListener true
            }
            false
        }

        binding.btnSignIn.setOnSingleClickListener {
            showAgreementBottomSheetDialog(requireActivity())
        }

        val mobilePattern = "^010-\\d{4}-\\d{4}\$"
        val mobilePatternCheck = Pattern.compile(mobilePattern)
        val pwPattern = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[$@$!%*#?&^])[A-Za-z[0-9]$@$!%*#?&^]{8,20}$" // 영문, 특수문자, 숫자 8 ~ 20자 패턴
        val idPattern = "^[a-zA-Z0-9]{4,16}$" // 영문, 숫자 4 ~ 16자 패턴
        val idPatternCheck = Pattern.compile(idPattern)
        val pwPatternCheck = Pattern.compile(pwPattern)

        // ------! 핸드폰 번호 조건 코드 !-----
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
                if (svm.mobileCondition.value == true) {
                    binding.btnAuthSend.isEnabled = true
                } else {
                    binding.btnAuthSend.isEnabled = false
                }
            }
        })

        // ----- ! ID 조건 코드 ! -----
        binding.etId.addTextChangedListener(object : TextWatcher {
            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(s: Editable?) {
                svm.idCondition.value = idPatternCheck.matcher(binding.etId.text.toString()).find()
                if (svm.idCondition.value == true) {
                    binding.tvIdCondition.setTextColor(binding.tvIdCondition.resources.getColor(R.color.successColor, null))
                    binding.tvIdCondition.text = "사용 가능합니다"
                    binding.btnIdCondition.apply {
                        isEnabled = true
                        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
                    }
                } else {
                    binding.tvIdCondition.setTextColor(binding.tvIdCondition.resources.getColor(R.color.deleteColor, null))
                    binding.tvIdCondition.text = "영문, 숫자를 포함해서 4자리~16자리를 입력해주세요"
                    binding.btnIdCondition.apply {
                        isEnabled = false
                        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400))
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // ------# 비밀번호 조건 코드 #------
        binding.etPw.addTextChangedListener(object : TextWatcher {
            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(s: Editable?) {

                svm.pwCondition.value = pwPatternCheck.matcher(binding.etPw.text.toString()).find()
                if (svm.pwCondition.value == true) {
                    binding.tvPwCondition.setTextColor(binding.tvPwCondition.resources.getColor(R.color.successColor, null))
                    binding.tvPwCondition.text = "사용 가능합니다"
                } else {
                    binding.tvPwCondition.setTextColor(binding.tvPwCondition.resources.getColor(R.color.deleteColor, null))
                    binding.tvPwCondition.text = "영문, 숫자, 특수문자( ! @ # $ % ^ & * ?)를 모두 포함해서 8~20자리를 입력해주세요"
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // ----- ! 비밀번호 확인 코드 ! -----
        binding.etPwRepeat.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                showAgreementBottomSheetDialog(requireActivity())
                true  // 이벤트 처리가 완료되었음을 반환
            } else {
                false // 다른 동작들은 그대로 유지
            }
        }



        binding.etPwRepeat.addTextChangedListener(object : TextWatcher {
            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(s: Editable?) {
                binding.tvSignInGuide.text = "모두 완료됐습니다."
                svm.pwCompare.value = (binding.etPw.text.toString() == binding.etPwRepeat.text.toString())
                if (svm.pwCompare.value == true) {
                    binding.tvPwRepeat.setTextColor(binding.tvPwRepeat.resources.getColor(R.color.successColor, null))
                    binding.tvPwRepeat.text = "일치합니다"
                } else {
                    binding.tvPwRepeat.setTextColor(binding.tvPwRepeat.resources.getColor(R.color.deleteColor, null))
                    binding.tvPwRepeat.text = "올바르지 않습니다"
                }
                // -----! 뷰모델에 보낼 값들 넣기 !-----

                svm.User.value?.put("password", s.toString())

            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }) //-----! 입력 문자 조건 끝 !-----

        svm.allTrueLiveData.observe(this) {
            if (svm.pwBothTrue.value == true) {
                binding.tvFinalCheck.visibility = View.VISIBLE
            }
            if (it) {
                binding.tvFinalCheck.apply {
                    text = "회원가입 버튼을 눌러주세요"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.successColor))
                }
                binding.btnSignIn.apply {
                    isEnabled = it
                    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
                }
            } else {
                binding.tvFinalCheck.apply {
                    text = "이름, 이메일을 올바르게 입력해주세요"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.deleteColor))
                }
                binding.btnSignIn.apply {
                    isEnabled = it
                    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400))
                }
            }

        }
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.P)
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    lifecycleScope.launch {
                        svm.mobileAuthCondition.value = true
                        binding.etAuthNumber.isEnabled = false
                        binding.etMobile.isEnabled = false
                        binding.btnAuthSend.apply {
                            isEnabled = false
                            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400))
                        }
                        binding.btnAuthConfirm.apply {
                            isEnabled = false
                            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400))
                        }
                        svm.phoneCondition.value = true
                        // ------! 번호 인증 완료 !------
                        val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
                        alphaAnimation.duration = 600
                        binding.tvNameGuide.startAnimation(alphaAnimation)
                        binding.etName.startAnimation(alphaAnimation)
                        binding.llEmail.startAnimation(alphaAnimation)

                        binding.tvNameGuide.visibility = View.VISIBLE
                        binding.etName.visibility = View.VISIBLE
                        binding.llEmail.visibility= View.VISIBLE
                        binding.btnEmailNext.visibility = View.VISIBLE
                        binding.tvSignInGuide.text = "이름과 이메일을 입력해주세요"
                        val objectAnimator = ObjectAnimator.ofFloat(binding.clMobile, "translationY", 1f)
                        objectAnimator.duration = 1000
                        objectAnimator.start()
                        binding.pvSignIn.progress = 50f
                        binding.etName.requestFocus()
                        binding.svSignIn.go(1, true)
                        // ------! 번호 인증 완료 !------

                        Toast.makeText(requireContext(), "인증에 성공했습니다 !", Toast.LENGTH_SHORT).show()


                        binding.btnAuthSend.isEnabled = false
                        binding.btnAuthConfirm.isEnabled = false

                        if (svm.mobileCondition.value == true) {

                            // 전화번호 "-" 표시 전부 없애기
                            val mobile = binding.etMobile.text.toString().replace("-", "").replace(" ", "")
//                            Log.v("모바일인증완료", mobile)
                            svm.User.value?.put("mobile", mobile)
                            binding.btnAuthSend.isEnabled = true
                        } else {
                            binding.btnAuthSend.isEnabled = false
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "인증에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    Log.w(ContentValues.TAG, "mobile auth failed.")
                }
            }
    }



    private fun showAgreementBottomSheetDialog(context: FragmentActivity) {
        val bottomSheetFragment = AgreementBSDialogFragment()
        bottomSheetFragment.setOnFinishListener(object : AgreementBSDialogFragment.OnAgreeListener {
            override fun onFinish(agree: Boolean) {
                if (agree) {

                    svm.User.value?.put("user_name", binding.etName.text)
                    when (binding.spinner.selectedItemPosition) {
                        0, 1, 2 -> {
                            svm.User.value?.put("email", "${binding.etEmailId.text}@${binding.spinner.selectedItem as String}")
                        }
                        else -> {
                            svm.User.value?.put("email", "${binding.etEmailId.text}@${binding.etEmail.text}")
                        }
                    }

                    // ------! 광고성 넣기 시작 !------
                    val jsonObj = svm.User.value
                    jsonObj?.put("sms_receive", if (svm.agreementMk1.value == true) 1 else 0)
                    jsonObj?.put("email_receive", if (svm.agreementMk2.value == true) 1 else 0)
                    jsonObj?.put("device_sn" ,0)
                    jsonObj?.put("user_sn", 0)
                    // ------! 광고성 넣기 끝 !------
//                    Log.v("회원가입JSon", "$jsonObj")
                    if (jsonObj != null) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            insertUser(getString(com.tangoplus.tangoq.R.string.API_user), jsonObj ) { status ->
//                                Log.v("insertStatus", "status: $status")
                                when (status) {
                                    200, 201 -> {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            val intent = Intent(requireActivity(), IntroActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                            intent.putExtra("SignInFinished", 201)
                                            dismiss()
                                            startActivity(intent)
                                        }
                                    }
                                    423, 409, 404, 403 -> {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            MaterialAlertDialogBuilder(context, com.tangoplus.tangoq.R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                                                setTitle("회원 가입 실패")
                                                setMessage("올바르지 않은 데이터가 존재합니다\n입력한 정보를 다시 확인해주세요")
                                                setPositiveButton("예") { _, _ ->
                                                    binding.etEmail.isEnabled = true
                                                    binding.etPw.isEnabled = true
                                                    binding.etPwRepeat.isEnabled = true
                                                    binding.etId.isEnabled = true
                                                    binding.etAuthNumber.isEnabled = true
                                                    binding.etMobile.isEnabled = true
                                                    binding.btnAuthConfirm.isEnabled = true
                                                    binding.btnAuthSend.isEnabled = true
                                                    binding.btnEmailNext.isEnabled = true
                                                    binding.btnIdCondition.isEnabled = true
                                                    binding.btnAuthConfirm.backgroundTintList  = ColorStateList.valueOf(resources.getColor(com.tangoplus.tangoq.R.color.mainColor, null))
                                                    binding.btnAuthSend.backgroundTintList = ColorStateList.valueOf(resources.getColor(com.tangoplus.tangoq.R.color.mainColor, null))
                                                    binding.btnEmailNext.backgroundTintList = ColorStateList.valueOf(resources.getColor(com.tangoplus.tangoq.R.color.mainColor, null))
                                                    binding.btnIdCondition.backgroundTintList = ColorStateList.valueOf(resources.getColor(com.tangoplus.tangoq.R.color.mainColor, null))
                                                }
                                                show()
                                            }
                                        }
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

    override fun onResume() {
        super.onResume()
        binding.nsvSignIn.isNestedScrollingEnabled = false
    }
}