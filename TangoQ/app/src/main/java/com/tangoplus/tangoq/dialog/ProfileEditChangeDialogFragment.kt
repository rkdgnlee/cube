package com.tangoplus.tangoq.dialog

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentProfileChangeDialogBinding
import com.tangoplus.tangoq.listener.OnSingleClickListener
import com.tangoplus.tangoq.mediapipe.MathHelpers.phoneNumber82
import com.tangoplus.tangoq.api.NetworkUser.fetchUserUPDATEJson
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class ProfileEditChangeDialogFragment : DialogFragment() {
    lateinit var binding: FragmentProfileChangeDialogBinding
    lateinit var arg : String
    val svm : SignInViewModel by activityViewModels()
    private lateinit var userJson : JSONObject
    private lateinit var auth : FirebaseAuth

    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    companion object {
        private const val ARG_EDIT_BS_TITLE = "profileEditTitle"
        private const val ARG_EDIT_BS_VALUE = "profileEditValue"
        fun newInstance(title: String, value: String) : ProfileEditChangeDialogFragment {

            val fragment = ProfileEditChangeDialogFragment()
            val args = Bundle()
            args.putString(ARG_EDIT_BS_TITLE, title)
            args.putString(ARG_EDIT_BS_VALUE, value)
            fragment.arguments = args
            return fragment
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileChangeDialogBinding.inflate(inflater)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ------# init #-----
        arg = arguments?.getString(ARG_EDIT_BS_TITLE).toString()
        val value = arguments?.getString(ARG_EDIT_BS_VALUE).toString()
        Log.v("arg", "$arg, $value")
        userJson = Singleton_t_user.getInstance(requireContext()).jsonObject ?: JSONObject()
        auth = FirebaseAuth.getInstance()
        auth.setLanguageCode("kr")
        disabledButton()
        // ------# 키보드 #------
        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        Handler(Looper.getMainLooper()).postDelayed({
            binding.etPCD1.requestFocus()
            binding.etPCD1.postDelayed({
                imm.showSoftInput(binding.etPCD1, InputMethodManager.SHOW_IMPLICIT)
            }, 0)
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }, 250)


        when (arg) {
            "비밀번호" -> {
                setUIVisibility(0)
                // ------# 비밀번호 재설정 case #------
                val pwPattern = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[$@$!%*#?&^])[A-Za-z[0-9]$@$!%*#?&^]{8,20}$" // 영문, 특수문자, 숫자 8 ~ 20자 패턴
                val pwPatternCheck = Pattern.compile(pwPattern)
                binding.etPCD2.addTextChangedListener(object : TextWatcher{
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        svm.pwCondition.value = pwPatternCheck.matcher(binding.etPCD2.text.toString()).find()
                        if (svm.pwCondition.value == true) {
                            binding.tvPCDPWCondition.setTextColor(binding.tvPCDPWCondition.resources.getColor(R.color.subColor400, null))
                            binding.tvPCDPWCondition.text = "사용 가능합니다"
                        } else {
                            binding.tvPCDPWCondition.setTextColor(binding.tvPCDPWCondition.resources.getColor(R.color.deleteColor, null))
                            binding.tvPCDPWCondition.text = "영문, 숫자, 특수문자( ! @ # $ % ^ & * ?)를 모두 포함해서 8~20자리를 입력해주세요"
                        }
                    }
                })

                binding.etPCD3.addTextChangedListener(object : TextWatcher{
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        svm.pwCompare.value = (binding.etPCD2.text.toString() == binding.etPCD3.text.toString())
                        if (svm.pwCompare.value == true) {
                            binding.tvPCDPWVerifyCondition.setTextColor(binding.tvPCDPWVerifyCondition.resources.getColor(R.color.subColor400, null))
                            binding.tvPCDPWVerifyCondition.text = "일치합니다"
                        } else {
                            binding.tvPCDPWVerifyCondition.setTextColor(binding.tvPCDPWVerifyCondition.resources.getColor(R.color.deleteColor, null))
                            binding.tvPCDPWVerifyCondition.text = "일치하지 않습니다"
                        }
                    }
                })

                // ------# 비밀번호 확인 여부 체크 #------
                svm.pwBothTrue.observe(viewLifecycleOwner) {
                    binding.btnPCDFinish.isEnabled = it
                    if (it) {
                        enabledButton()
                        svm.pw.value = binding.etPCD2.text.toString()
                    } else {
                        disabledButton()
                        svm.pw.value = ""
                    }
                }
            }

            "몸무게" -> {
                val weightPattern = "^(2\\d|[3-9]\\d|[1]\\d{2}|200)\$"
                val weightPatternCheck = Pattern.compile(weightPattern)
                binding.etPCD1.addTextChangedListener(object: TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        svm.weightCondition.value = weightPatternCheck.matcher(binding.etPCD1.text.toString()).find()
                        if (svm.weightCondition.value == true) {
                            enabledButton()
                        } else {
                            disabledButton()
                        }
                    }
                })
                setUIVisibility(1)
                binding.tvPCD.text = "몸무게 재설정"
                binding.tvPCDGuide.text = "몸무게을 다시 설정해주세요"
                binding.etPCD1.apply {
                    inputType = InputType.TYPE_CLASS_NUMBER
                    setText(value)
                    filters = arrayOf(InputFilter.LengthFilter(3))
                }
            }
            "신장" -> {
                val heightPattern = "^(8\\d|[9]\\d|[1-2]\\d{2}|250)\$"
                val heightPatternCheck = Pattern.compile(heightPattern)
                binding.etPCD1.addTextChangedListener(object: TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        svm.heightCondition.value = heightPatternCheck.matcher(binding.etPCD1.text.toString()).find()
                        if (svm.heightCondition.value == true) {
                            enabledButton()
                        } else {
                            disabledButton()
                        }
                    }
                })
                setUIVisibility(1)
                binding.tvPCD.text = "신장 재설정"
                binding.tvPCDGuide.text = "신장을 다시 설정해주세요"
                binding.etPCD1.apply {
                    inputType = InputType.TYPE_CLASS_NUMBER
                    setText(value)
                    filters = arrayOf(InputFilter.LengthFilter(3))
                }
            }
            "이메일" -> {
                setUIVisibility(1)
                binding.tvPCD.text = "이메일 재설정"
                binding.tvPCDGuide.text = "이메일을 다시 설정해주세요"
                binding.etPCD1.apply {
                    inputType = InputType.TYPE_CLASS_TEXT
                    filters = arrayOf(InputFilter.LengthFilter(25))
                }

                val emailPattern = "^[a-z0-9]{4,16}@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
                val emailPatternCheck = Pattern.compile(emailPattern)
                binding.etPCD1.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int, ) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int, ) {}
                    override fun afterTextChanged(s: Editable?) {
                        svm.editChangeCondition.value = emailPatternCheck.matcher(s.toString()).find()
                        if (svm.editChangeCondition.value == true) {
                            binding.tvPCD1Condition.text = "올바른 이메일 형식입니다"
                            binding.btnPCDFinish.isEnabled = true
                            enabledButton()
                        } else {
                            binding.tvPCD1Condition.text = "올바르지 않은 이메일 형식입니다. 다시 확인해 주세요"
                            binding.btnPCDFinish.isEnabled = false
                            disabledButton()
                        }
                    }
                })
            }
            "생년월일" -> {
                binding.etPCD1.hint = "19950223"
                val birthdayPattern = "^(19|20)\\d{2}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])\$"
                val birthdayPatternCheck = Pattern.compile(birthdayPattern)
                binding.etPCD1.addTextChangedListener(object: TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        svm.birthdayCondition.value = birthdayPatternCheck.matcher(binding.etPCD1.text.toString()).find()
                        if (svm.birthdayCondition.value == true) {
                            enabledButton()
                        } else {
                            disabledButton()
                        }
                    }
                })
                setUIVisibility(1)
                binding.tvPCD.text = "생년월일 설정"
                binding.tvPCDGuide.text = "생년월일을 설정해주세요"
                binding.etPCD1.apply {
                    inputType = InputType.TYPE_CLASS_NUMBER
                    filters = arrayOf(InputFilter.LengthFilter(8))
                }
            }
            "전화번호" -> {
                // ------# 초기 세팅 #------
                auth = FirebaseAuth.getInstance()
                auth.setLanguageCode("kr")
                setUIVisibility(2)
                binding.tvPCD.text = "전화번호 재설정"
                binding.tvPCDGuide.text = "전화번호 재설정을 위해 인증을 진행합니다"
                binding.etPCD1.apply {
                    inputType = InputType.TYPE_CLASS_NUMBER
                    filters = arrayOf(InputFilter.LengthFilter(12))
                }
                val mobilePattern = "^010-\\d{4}-\\d{4}\$"
                val mobilePatternCheck = Pattern.compile(mobilePattern)
                // ------# 전화번호 firebase 설정 #------
                binding.etPCDMobile.addTextChangedListener(object: TextWatcher {
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
//                        Log.w("전화번호형식", "${mobilePatternCheck.matcher(binding.etPCDMobile.text.toString()).find()}")
                        svm.mobileCondition.value = mobilePatternCheck.matcher(binding.etPCDMobile.text.toString()).find()
                        if (svm.mobileCondition.value == true) {
                            binding.btnPCDAuthSend.isEnabled = true
                            binding.btnPCDAuthSend.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
                        } else {
                            binding.btnPCDAuthSend.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400))
                        }
                    }
                })


                val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(p0: PhoneAuthCredential) {
//                        Log.v("verifyComplete", "PhoneAuthCredential: $p0")

                    }
                    override fun onVerificationFailed(p0: FirebaseException) {
                        Log.e("failedAuth", "verify failed")
                    }
                    @RequiresApi(Build.VERSION_CODES.P)
                    override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                        super.onCodeSent(verificationId, token)
                        svm.verificationId = verificationId
//                        Log.v("onCodeSent", "메시지 발송 성공, verificationId: ${verificationId} ,token: ${token}")
                        // -----! 메시지 발송에 성공하면 스낵바 호출 !------
                        Toast.makeText(requireContext(), "메시지 발송에 성공했습니다. 잠시만 기다려주세요", Toast.LENGTH_LONG).show()
                        binding.btnPCDAuthConfirm.isEnabled = true
                    }
                }

                binding.btnPCDAuthSend.setOnSingleClickListener {
                    var transformMobile = phoneNumber82(binding.etPCDMobile.text.toString())
                    val dialog = AlertDialog.Builder(requireContext())
                        .setTitle("📩 문자 인증 ")
                        .setMessage("${transformMobile}로 인증 하시겠습니까?")
                        .setPositiveButton("예") { _, _ ->
                            transformMobile = transformMobile.replace("-", "").replace(" ", "")
//                            Log.w("전화번호", transformMobile)

                            val optionsCompat = PhoneAuthOptions.newBuilder(auth)
                                .setPhoneNumber(transformMobile)
                                .setTimeout(60L, TimeUnit.SECONDS)
                                .setActivity(requireActivity())
                                .setCallbacks(callbacks)
                                .build()

                            PhoneAuthProvider.verifyPhoneNumber(optionsCompat)
                            Log.d("PhoneAuth", "verifyPhoneNumber called")

                            val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
                            alphaAnimation.duration = 600
                            binding.etPCDAuth.isEnabled = true
                            val objectAnimator = ObjectAnimator.ofFloat(binding.clPCDMobile, "translationY", 1f)
                            objectAnimator.duration = 1000
                            objectAnimator.start()
                            binding.etPCDAuth.requestFocus()
                        }
                        .setNegativeButton("아니오", null)
                        .show()

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
                }
                binding.btnPCDAuthConfirm.setOnSingleClickListener {
                    val credential = PhoneAuthProvider.getCredential(svm.verificationId, binding.etPCDAuth.text.toString())
                    signInWithPhoneAuthCredential(credential)
                    enabledButton()
                }
            }
            "성별" -> {
                setUIVisibility(3)
                binding.tvPCD.text = "성별 선택"
                binding.tvPCDGuide.text = "성별을 선택해주세요."
                setGenderButton( if (svm.setGender.value == 0) false else true)
                binding.ivPCDMale.setOnSingleClickListener { setGenderButton(true) }
                binding.mrbPCDMale.setOnSingleClickListener { setGenderButton(true) }
                binding.ivPCDFemale.setOnSingleClickListener { setGenderButton(false) }
                binding.mrbPCDFemale.setOnSingleClickListener { setGenderButton(false) }
                enabledButton()
            }

        }

        // ------# 키보드 올라오기 #------
//        binding.etPEBSD.requestFocus()
//        binding.etPEBSD.postDelayed({
//            context?.let { context ->
//                val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
//                imm.showSoftInput(binding.etPEBSD, InputMethodManager.SHOW_IMPLICIT)
//            }
//        }, 250)


//        binding.btnPEBSDFinish.setOnClickListener {
//            when (arg) {
//                "몸무게" -> svm.setWeight.value = binding.etPEBSD.text.toString().toInt()
//                "신장" -> svm.setHeight.value = binding.etPEBSD.text.toString().toInt()
//                "이메일" -> svm.setEmail.value = binding.etPEBSD.text.toString()
//            }
//            Log.v("뷰모델에 잘담겼는지", "${svm.User.value}")
//            dismiss()
//        }
        // ------# 변경 #------
        binding.btnPCDFinish.setOnClickListener {
            val updatedItem = binding.etPCD1.text.toString()
            val jo = JSONObject().apply {
                when (arg) {
                    "비밀번호" -> put("password", svm.pw.value)
                    "전화번호" -> put("mobile", binding.etPCDMobile.text.toString())
                    "몸무게" -> put("weight", updatedItem)
                    "신장" -> put("height", updatedItem)
                    "이메일" -> put("email", updatedItem)
                    "생년월일" -> put("birthday", updatedItem)
                    "성별" -> put("gender", svm.setGender.value)
                }
            }
            lifecycleScope.launch(Dispatchers.IO) {
                val isUpdateFinished = fetchUserUPDATEJson(requireContext(), getString(R.string.API_user), jo.toString(), userJson.optInt("sn").toString())
                if (isUpdateFinished == true) {
                    when (arg) {
                        "몸무게" -> {
                            userJson.put("weight", updatedItem)
                            withContext(Dispatchers.Main) {
                                svm.setWeight.value = updatedItem.toInt()
                            }
                        }
                        "전화번호" -> {
                            userJson.put("mobile", binding.etPCDMobile.text.toString())
                            withContext(Dispatchers.Main) {
                                svm.setMobile.value = binding.etPCDMobile.text.toString()
                                Log.v("svm.setMobile", "${svm.setMobile.value}")
                            }
                        }
                        "신장" -> {
                            userJson.put("height", updatedItem)
                            withContext(Dispatchers.Main) {
                                svm.setHeight.value = updatedItem.toInt()
                            }
                        }
                        "이메일" -> {
                            userJson.put("email", updatedItem)
                            withContext(Dispatchers.Main) {
                                svm.setEmail.value = updatedItem
                            }
                        }
                        "생년월일" -> {
                            userJson.put("birthday", updatedItem)
                            withContext(Dispatchers.Main) {
                                svm.setBirthday.value = updatedItem
                            }
                        }
                        "성별" -> {
                            userJson.put("gender", svm.setGender)
                            withContext(Dispatchers.Main) {
                                svm.setBirthday.value = updatedItem
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        if (isAdded) {
                            dismiss()
                        }
                    }
                }
            }
        }

        // ------# clear listener #------
        binding.ibtnPCD1Clear.setOnClickListener{ binding.etPCD1.setText("") }
        binding.ibtnPCD2Clear.setOnClickListener{ binding.etPCD2.setText("") }
        binding.ibtnPCD3Clear.setOnClickListener{ binding.etPCD3.setText("") }

    }
    private fun setGenderButton(isMale: Boolean) {
        when {
            isMale -> {
                binding.mrgPCD.check(binding.mrbPCDMale.id)
                binding.mrbPCDMale.isSelected = true
                binding.mrbPCDFemale.isSelected = false
                binding.ivPCDMale.isSelected = true
                binding.ivPCDFemale.isSelected = false
                svm.setGender.value = 1
            }
            !isMale -> {
                binding.mrgPCD.check(binding.mrbPCDFemale.id)
                binding.mrbPCDMale.isSelected = false
                binding.mrbPCDFemale.isSelected = true
                binding.ivPCDMale.isSelected = false
                binding.ivPCDFemale.isSelected = true
                svm.setGender.value = 0
            }
        }
    }


    private fun enabledButton() {
        binding.btnPCDFinish.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
        binding.btnPCDFinish.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white)))
        binding.btnPCDFinish.isEnabled = true
    }
    private fun disabledButton() {
        binding.btnPCDFinish.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor150))
        binding.btnPCDFinish.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400)))
        binding.btnPCDFinish.isEnabled = false
    }
    private fun View.setOnSingleClickListener(action: (v: View) -> Unit) {
        val listener = View.OnClickListener { action(it) }
        setOnClickListener(OnSingleClickListener(listener))
    }

    private fun setUIVisibility(case: Int) {
        when (case) {
            // 0 -> 비밀번호,  1 -> 그 외 수정, 2 -> 휴대폰 인증 3 -> 젠더
            0 -> {
                binding.etPCD1.visibility = View.VISIBLE
                binding.etPCD2.visibility = View.VISIBLE
                binding.etPCD3.visibility = View.VISIBLE
                binding.tvPCD1.visibility = View.VISIBLE
                binding.tvPCD2.visibility = View.VISIBLE
                binding.tvPCD3.visibility = View.VISIBLE
                binding.tvPCDPWCondition.visibility = View.VISIBLE
                binding.tvPCDPWVerifyCondition.visibility = View.VISIBLE
                binding.tvPCDSkip.visibility = View.VISIBLE
                binding.clPCDMobile.visibility = View.GONE
                binding.ibtnPCD2Clear.visibility = View.VISIBLE
                binding.ibtnPCD3Clear.visibility = View.VISIBLE
                binding.clPCDGender.visibility = View.GONE
            }
            1 -> {
                binding.etPCD1.visibility = View.VISIBLE
                binding.etPCD2.visibility = View.GONE
                binding.etPCD3.visibility = View.GONE
                binding.tvPCD1.visibility = View.GONE
                binding.tvPCD2.visibility = View.GONE
                binding.tvPCD3.visibility = View.GONE
                binding.tvPCDPWCondition.visibility = View.GONE
                binding.tvPCDPWVerifyCondition.visibility = View.GONE
                binding.tvPCDSkip.visibility = View.GONE
                binding.clPCDMobile.visibility = View.GONE
                binding.ibtnPCD2Clear.visibility = View.GONE
                binding.ibtnPCD3Clear.visibility = View.GONE
                binding.clPCDGender.visibility = View.GONE

            }
            2 -> {
                binding.etPCD1.visibility = View.GONE
                binding.etPCD2.visibility = View.GONE
                binding.etPCD3.visibility = View.GONE
                binding.tvPCD1.visibility = View.GONE
                binding.tvPCD2.visibility = View.GONE
                binding.tvPCD3.visibility = View.GONE
                binding.tvPCDPWCondition.visibility = View.GONE
                binding.tvPCDPWVerifyCondition.visibility = View.GONE
                binding.tvPCDSkip.visibility = View.GONE
                binding.clPCDMobile.visibility = View.VISIBLE
                binding.ibtnPCD2Clear.visibility = View.GONE
                binding.ibtnPCD3Clear.visibility = View.GONE
                binding.etPCDMobile.isEnabled = true
                binding.btnPCDAuthSend.isEnabled = false
                binding.btnPCDAuthConfirm.isEnabled = false
                binding.btnPCDFinish.isEnabled = false
                binding.clPCDGender.visibility = View.GONE

            }
            3 -> {
                binding.etPCD1.visibility = View.GONE
                binding.etPCD2.visibility = View.GONE
                binding.etPCD3.visibility = View.GONE
                binding.tvPCD1.visibility = View.GONE
                binding.tvPCD2.visibility = View.GONE
                binding.tvPCD3.visibility = View.GONE
                binding.tvPCDPWCondition.visibility = View.GONE
                binding.tvPCDPWVerifyCondition.visibility = View.GONE
                binding.tvPCDSkip.visibility = View.GONE
                binding.clPCDMobile.visibility = View.GONE
                binding.ibtnPCD2Clear.visibility = View.GONE
                binding.ibtnPCD3Clear.visibility = View.GONE
                binding.etPCDMobile.isEnabled = false
                binding.btnPCDAuthSend.isEnabled = false
                binding.btnPCDAuthConfirm.isEnabled = false
                binding.clPCDGender.visibility = View.VISIBLE

            }
        }
        binding.tvPCD1Condition.text = ""
    }
    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.P)
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "인증에 성공했습니다. 전화번호 변경을 진행해주세요", Toast.LENGTH_LONG).show()
                    binding.btnPCDFinish.isEnabled = true
                } else {
                    Toast.makeText(requireContext(), "인증에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    Log.w(ContentValues.TAG, "mobile auth failed.")
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                FirebaseAuth.getInstance().signOut()
                val user = FirebaseAuth.getInstance().currentUser
                Log.v("user", "$user")
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
                Log.v("user", "$user")
                user?.delete()
            }
        }
        auth.signOut()
    }
}