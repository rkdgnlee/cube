package com.tangoplus.tangoq

import android.R
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.shuhart.stepview.StepView
import com.tangoplus.tangoq.adapter.SpinnerAdapter
import com.tangoplus.tangoq.data.SignInViewModel
import com.tangoplus.tangoq.databinding.ActivitySignInBinding
import com.tangoplus.tangoq.dialog.AgreementBottomSheetDialogFragment
import com.tangoplus.tangoq.dialog.SignInBSDialogFragment
import com.tangoplus.tangoq.listener.OnSingleClickListener
import com.tangoplus.tangoq.transition.SignInTransition
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class SignInActivity : AppCompatActivity() {
    lateinit var binding : ActivitySignInBinding
    val viewModel : SignInViewModel by viewModels()
    private lateinit var auth : FirebaseAuth

    var verificationId = ""
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        auth = FirebaseAuth.getInstance()
        auth.setLanguageCode("ko")
        // -----! 초기 버튼 숨기기 및 세팅 시작 !-----
        binding.llPwCondition.visibility = View.GONE
        binding.etPw.visibility = View.GONE
        binding.llPwRepeat.visibility = View.GONE
        binding.etPwRepeat.visibility = View.GONE
        binding.btnSignIn.visibility = View.GONE
        binding.llId.visibility = View.GONE
        binding.etId.visibility = View.GONE
        binding.tvNameGuide.visibility = View.GONE
        binding.etName.visibility = View.GONE
        binding.llEmail.visibility = View.GONE

        binding.btnAuthSend.isEnabled = false
        binding.etAuthNumber.isEnabled = false
        binding.btnAuthConfirm.isEnabled = false
        binding.btnSignIn.isEnabled = false

        binding.etMobile.requestFocus()
        binding.etMobile.postDelayed({
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etMobile, InputMethodManager.SHOW_IMPLICIT)
        }, 200) // 100ms 정도의 딜레이를 줍니다.


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
            .animationDuration(getResources().getInteger(R.integer.config_shortAnimTime))
            // other state methods are equal to the corresponding xml attributes
            .commit()
        // -----! 초기 버튼 숨기기 및 세팅 끝 !-----

        // -----! progress bar 시작 !-----
        binding.pvSignIn.progress = 25
        // -----! progress bar 끝 !-----

        // -----! 통신사 선택 시작 !-----
        binding.tvTelecom.setOnSingleClickListener {
            showTelecomBottomSheetDialog(this@SignInActivity)
        } // -----! 통신사 선택 끝 !-----

        // -----! 회원가입 입력 창 anime 시작  !-----
        TransitionManager.beginDelayedTransition(binding.llSignIn, SignInTransition())

        // -----! 휴대폰 인증 시작 !-----

        // -----! 인증 문자 확인 시작 !-----
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
                this@SignInActivity.verificationId = verificationId
                Log.v("onCodeSent", "메시지 발송 성공, verificationId: ${verificationId.toString().substring(0, 5)} ,token: ${token.toString().substring(0, 5)}")
                // -----! 메시지 발송에 성공하면 스낵바 호출 !------
                Snackbar.make(requireViewById(com.tangoplus.tangoq.R.id.clSignIn), "메시지 발송에 성공했습니다. 잠시만 기다려주세요", Snackbar.LENGTH_LONG).show()
                binding.btnAuthConfirm.isEnabled = true
            }
        }

        binding.btnAuthSend.setOnSingleClickListener {
            var transformMobile = phoneNumber82(binding.etMobile.text.toString())
            val dialog = AlertDialog.Builder(this)
                .setTitle("📩 문자 인증 ")
                .setMessage("$transformMobile 로 인증 하시겠습니까?")
                .setPositiveButton("예") { _, _ ->
                    transformMobile = transformMobile.replace("-", "").replace(" ", "")
                    Log.w("전화번호", transformMobile)
                    val optionsCompat = PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(transformMobile)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this@SignInActivity)
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
                .setNegativeButton("아니오", null)
                .show()

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK)

        }
        // -----! 휴대폰 인증 끝 !-----

        binding.btnAuthConfirm.setOnSingleClickListener {
            val credential = PhoneAuthProvider.getCredential(verificationId, binding.etAuthNumber.text.toString())
            signInWithPhoneAuthCredential(credential)

        }  // -----! 인증 문자 확인 끝 !-----

        binding.etName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.etEmail.requestFocus()
                return@setOnEditorActionListener true
            }
            false
        }
        binding.etEmailId.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
                alphaAnimation.duration = 600
                binding.etId.startAnimation(alphaAnimation)
                binding.llId.startAnimation(alphaAnimation)
                binding.llPwCondition.startAnimation(alphaAnimation)
                binding.etPw.startAnimation(alphaAnimation)
                binding.llPwRepeat.startAnimation(alphaAnimation)
                binding.etPwRepeat.startAnimation(alphaAnimation)
                binding.btnSignIn.startAnimation(alphaAnimation)

                binding.etId.visibility = View.VISIBLE
                binding.llId.visibility = View.VISIBLE
                binding.llPwCondition.visibility = View.VISIBLE
                binding.etPw.visibility = View.VISIBLE
                binding.llPwRepeat.visibility = View.VISIBLE
                binding.etPwRepeat.visibility = View.VISIBLE
                binding.btnSignIn.visibility = View.VISIBLE
                val objectAnimator = ObjectAnimator.ofFloat(binding.clMobile, "translationY", 1f)
                objectAnimator.duration = 1000
                objectAnimator.start()
                return@setOnEditorActionListener true
            }
            false
        }
        val domainList = listOf("gmail.com", "naver.com", "kakao.com", "직접입력")
        binding.spinner.adapter = SpinnerAdapter(this, com.tangoplus.tangoq.R.layout.item_spinner, domainList)
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
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
//        binding.psvSIEmail.setSpinnerAdapter(actAdapter)
//        binding.psvSIEmail.setText(domain_list.firstOrNull(), false)

        binding.etId.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
                alphaAnimation.duration = 600




                val objectAnimator = ObjectAnimator.ofFloat(binding.clMobile, "translationY", 1f)
                objectAnimator.duration = 1000
                objectAnimator.start()
                binding.pvSignIn.progress = 75
                binding.etPw.requestFocus()
                binding.svSignIn.go(2, true)
                return@setOnEditorActionListener true
            }
            false
        }

        binding.btnSignIn.setOnSingleClickListener {
            showAgreementBottomSheetDialog(this)

        }
        val mobilePattern = "^010-\\d{4}-\\d{4}\$"
        val mobilePatternCheck = Pattern.compile(mobilePattern)
        val pwPattern = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[$@$!%*#?&.^])[A-Za-z[0-9]$@$!%*#?&.^]{8,20}$" // 영문, 특수문자, 숫자 8 ~ 20자 패턴
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
                val cleaned =s.toString().replace("-", "")
                when {
                    cleaned.length <= 3 -> s?.replace(0, s.length, cleaned)
                    cleaned.length <= 7 -> s?.replace(0, s.length, "${cleaned.substring(0, 3)}-${cleaned.substring(3)}")
                    else -> s?.replace(0, s.length, "${cleaned.substring(0, 3)}-${cleaned.substring(3, 7)}-${cleaned.substring(7)}")
                }
                isFormatting = false
                Log.w("전화번호형식", "${mobilePatternCheck.matcher(binding.etMobile.text.toString()).find()}")
                viewModel.mobileCondition.value = mobilePatternCheck.matcher(binding.etMobile.text.toString()).find()
                if (viewModel.mobileCondition.value == true) {
                    viewModel.User.value?.put("user_mobile", s.toString() )
                    binding.btnAuthSend.isEnabled = true
                }

            }
        })


        // ----- ! ID 조건 코드 ! -----
        binding.etId.addTextChangedListener(object : TextWatcher {
            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(s: Editable?) {
                viewModel.idCondition.value = idPatternCheck.matcher(binding.etId.text.toString()).find()
                if (viewModel.idCondition.value == true) {
                    binding.tvIdCondition.setTextColor(binding.tvIdCondition.resources.getColor(com.tangoplus.tangoq.R.color.successColor, null))
                    binding.tvIdCondition.text = "사용 가능합니다"
                    viewModel.User.value?.put("user_id", s.toString())
                } else {
                    binding.tvIdCondition.setTextColor(binding.tvIdCondition.resources.getColor(com.tangoplus.tangoq.R.color.mainColor, null))
                    binding.tvIdCondition.text = "다시 입력해주세요"
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        // ----- ! 비밀번호 조건 코드 ! -----
        binding.etPw.addTextChangedListener(object : TextWatcher {
            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(s: Editable?) {
                viewModel.pwCondition.value = pwPatternCheck.matcher(binding.etPw.text.toString()).find()
                if (viewModel.pwCondition.value == true) {
                    binding.tvPwCondition.setTextColor(binding.tvPwCondition.resources.getColor(com.tangoplus.tangoq.R.color.successColor, null))
                    binding.tvPwCondition.text = "사용 가능합니다"

                } else {
                    binding.tvPwCondition.setTextColor(binding.tvPwCondition.resources.getColor(com.tangoplus.tangoq.R.color.mainColor, null))
//                    binding.tvPwCondition.text = "영문, 숫자, 특수문자( ! @ # $ % ^ & * ? .)를 모두 포함해서 8~20자리를 입력해주세요"
                    binding.tvPwCondition.text = ""
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        // ----- ! 비밀번호 확인 코드 ! -----
        binding.etPwRepeat.addTextChangedListener(object : TextWatcher {
            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(s: Editable?) {
                viewModel.pwCompare.value = (binding.etPw.text.toString() == binding.etPwRepeat.text.toString())
                if (viewModel.pwCompare.value == true) {
                    binding.tvPwRepeat.setTextColor(binding.tvPwRepeat.resources.getColor(com.tangoplus.tangoq.R.color.successColor, null))
                    binding.tvPwRepeat.text = "일치합니다"
                    binding.pvSignIn.progress = 100
                    binding.svSignIn.go(3, true)
                    binding.tvSignInGuide.text = "하단의 완료 버튼을 눌러주세요"
                    binding.btnSignIn.isEnabled = true
                } else {
                    binding.tvPwRepeat.setTextColor(binding.tvPwRepeat.resources.getColor(com.tangoplus.tangoq.R.color.mainColor, null))
                    binding.tvPwRepeat.text = "다시 입력해주세요"
                }
                // -----! 뷰모델에 보낼 값들 넣기 !-----

                viewModel.User.value?.put("user_password", s.toString())

            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }) //-----! 입력 문자 조건 끝 !-----

        // -----! 뒤로 가기 버튼 시작 !-----
        binding.btnSignInBack.setOnSingleClickListener {
            finish()
        }
    } // -----! 회원가입 입력 창 anime 끝 !-----
    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.P)
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    runOnUiThread {
                        viewModel.mobileAuthCondition.value = true
                        binding.etAuthNumber.isEnabled = false
                        binding.etMobile.isEnabled = false

                        // ------! 번호 인증 완료 !------
                        val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
                        alphaAnimation.duration = 600
                        binding.tvNameGuide.startAnimation(alphaAnimation)
                        binding.etName.startAnimation(alphaAnimation)
                        binding.llEmail.startAnimation(alphaAnimation)

                        binding.tvNameGuide.visibility = View.VISIBLE
                        binding.etName.visibility = View.VISIBLE
                        binding.llEmail.visibility= View.VISIBLE
                        binding.tvSignInGuide.text = "이름과 이메일을 입력해주세요"
                        val objectAnimator = ObjectAnimator.ofFloat(binding.clMobile, "translationY", 1f)
                        objectAnimator.duration = 1000
                        objectAnimator.start()
                        binding.pvSignIn.progress = 50
                        binding.etName.requestFocus()
                        binding.svSignIn.go(1, true)
                        // ------! 번호 인증 완료 !------

                        val snackbar = Snackbar.make(requireViewById(com.tangoplus.tangoq.R.id.clSignIn), "인증에 성공했습니다 !", Snackbar.LENGTH_SHORT)
                        snackbar.setAction("확인") { snackbar.dismiss() }
                        snackbar.setActionTextColor(Color.WHITE)
                        snackbar.show()
                    }
                } else {
                    Toast.makeText(this@SignInActivity, "인증에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    Log.w(ContentValues.TAG, "mobile auth failed.")
                }
            }
    }
    private fun phoneNumber82(msg: String) : String {
        val firstNumber: String = msg.substring(0,3)

        var phoneEdit = msg.substring(3)
        when (firstNumber) {
            "010" -> phoneEdit = "+82 10$phoneEdit"
            "011" -> phoneEdit = "+82 11$phoneEdit"
            "016" -> phoneEdit = "+82 16$phoneEdit"
            "017" -> phoneEdit = "+82 17$phoneEdit"
            "018" -> phoneEdit = "+82 18$phoneEdit"
            "019" -> phoneEdit = "+82 19$phoneEdit"
        }
        return phoneEdit
    }
    private fun showTelecomBottomSheetDialog(context: FragmentActivity) {
        val bottomsheetfragment = SignInBSDialogFragment()
        bottomsheetfragment.setOnCarrierSelectedListener(object : SignInBSDialogFragment.OnTelecomSelectedListener {
            override fun onTelecomSelected(telecom: String) {
                binding.tvTelecom.text = telecom
            }
        })
        val fragmentManager = context.supportFragmentManager
        bottomsheetfragment.show(fragmentManager, bottomsheetfragment.tag)
    }

    private fun showAgreementBottomSheetDialog(context: FragmentActivity) {
        val bottomSheetFragment = AgreementBottomSheetDialogFragment()
        bottomSheetFragment.setOnFinishListener(object : AgreementBottomSheetDialogFragment.OnAgreeListener {
            override fun onFinish(agree: Boolean) {
                if (agree) {
                    // TODO 동의 간주 후, 연락처 보내기
                    viewModel.User.value?.put("user_name", binding.etName.text)
                    when (binding.spinner.selectedItemPosition) {
                        0, 1, 2 -> {
                            viewModel.User.value?.put("user_email", "${binding.etEmailId.text}@${binding.spinner.selectedItem as String}")
                        }
                        else -> {
                            viewModel.User.value?.put("user_email", "${binding.etEmailId.text}@${binding.etEmail.text}")
                        }
                    }
                    // ------! 광고성 넣기 시작 !------
                    val jsonObj = viewModel.User.value
                    jsonObj?.put("sms_receive", if (viewModel.agreementMk1.value == true) 1 else 0)
                    jsonObj?.put("email_receive", if (viewModel.agreementMk2.value == true) 1 else 0)
                    // ------! 광고성 넣기 끝 !------

//                    if (jsonObj != null) {
//                        getTokenByUserJson(getString(com.tangoplus.tangoq.R.string.IP_ADDRESS_t_user), jsonObj) { jo ->
//                            when (jo?.optString("response_code")) {
//                                "200" -> {
//                                    saveServerToken(this@SignInActivity, jo.optString("user_token"))
//                                    getUserIdentifyJson(getString(com.tangoplus.tangoq.R.string.IP_ADDRESS_t_user), jsonObj, getServerToken(this@SignInActivity).toString()) { jo2 ->
//                                        if (jo2 != null) {
//                                            storeUserInSingleton(this@SignInActivity, jo2)
//                                            Log.v("구글>싱글톤", "${Singleton_t_user.getInstance(this@SignInActivity).jsonObject}")
//                                            val intent = Intent(this@SignInActivity, IntroActivity::class.java)
//                                            startActivity(intent)
//                                            finish()
//                                        }
//                                    }
//                                }
//                                "201" -> { // ------! 기존 정보가 있다 !------
//                                    MaterialAlertDialogBuilder(this@SignInActivity, com.tangoplus.tangoq.R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
//                                        setTitle("알림")
//                                        setMessage("기존에 가입된 정보가 있습니다.")
//                                        setPositiveButton("돌아가기") { dialog, _ ->
//                                            val intent = Intent(this@SignInActivity, IntroActivity::class.java)
//                                            startActivity(intent)
//                                            finish()
//                                        }
//                                        create()
//                                    }.show()
//                                }
//                                "404" -> {
//                                    Log.v("responseCode404", "response: $jo")
//                                }
//                            }
//                        }
//                    }
                }

            }
        })
        val fragmentManager = context.supportFragmentManager
        bottomSheetFragment.show(fragmentManager, bottomSheetFragment.tag)
    }

    private fun View.setOnSingleClickListener(action: (v: View) -> Unit) {
        val listener = View.OnClickListener { action(it) }
        setOnClickListener(OnSingleClickListener(listener))
    }
    override fun onResume() {
        super.onResume()
        binding.nsvSignIn.isNestedScrollingEnabled = false
    }
}