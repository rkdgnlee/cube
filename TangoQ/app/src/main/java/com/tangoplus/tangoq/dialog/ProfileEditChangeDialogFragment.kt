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
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.function.SecurePreferencesManager.encrypt
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
        // api35이상 화면 크기 조절
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 상태 표시줄 높이만큼 상단 패딩 적용
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ------# init #-----
        arg = arguments?.getString(ARG_EDIT_BS_TITLE).toString()
        val value = arguments?.getString(ARG_EDIT_BS_VALUE).toString()
        Log.v("arg", "$arg, $value")
        userJson = Singleton_t_user.getInstance(requireContext()).jsonObject ?: JSONObject()

        disabledButton()
        // ------# 키보드 #------
        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
//        Handler(Looper.getMainLooper()).postDelayed({
//            imm.hideSoftInputFromWindow(view.windowToken, 0)
//        }, 250)
        binding.etPCD1.postDelayed({
            binding.etPCD1.requestFocus()
            binding.etPCD1.setSelection(binding.etPCD1.length())
            imm.showSoftInput(binding.etPCD1, InputMethodManager.SHOW_IMPLICIT)
        }, 250)

        binding.ibtnPCDBack2.setOnSingleClickListener{ dismiss() }
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
                            binding.tvPCDPWCondition.setTextColor(binding.tvPCDPWCondition.resources.getColor(R.color.mainColor, null))
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
                            binding.tvPCDPWVerifyCondition.setTextColor(binding.tvPCDPWVerifyCondition.resources.getColor(R.color.mainColor, null))
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
                binding.etPCD1.imeOptions = EditorInfo.IME_ACTION_DONE
                binding.etPCD1.setOnEditorActionListener { v, actionId, event ->
                    if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                        if (binding.btnPCDFinish.isEnabled) {
                            updateUserData()
                        }
                        true  // 이벤트 처리가 완료되었음을 반환
                    } else {
                        false // 다른 동작들은 그대로 유지
                    }
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
                binding.etPCD1.imeOptions = EditorInfo.IME_ACTION_DONE
                binding.etPCD1.setOnEditorActionListener { v, actionId, event ->
                    if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                        if (binding.btnPCDFinish.isEnabled) {
                            updateUserData()
                        }
                        true  // 이벤트 처리가 완료되었음을 반환
                    } else {
                        false // 다른 동작들은 그대로 유지
                    }
                }
            }
            "생년월일" -> {
                binding.etPCD1.hint = "19950812"
                binding.etPCD1.setHintTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400)))
                val birthdayPattern = "^(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])\$"
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
                binding.etPCD1.imeOptions = EditorInfo.IME_ACTION_DONE
                binding.etPCD1.setOnEditorActionListener { v, actionId, event ->
                    if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                        if (binding.btnPCDFinish.isEnabled) {
                            updateUserData()
                        }
                        true  // 이벤트 처리가 완료되었음을 반환
                    } else {
                        false // 다른 동작들은 그대로 유지
                    }
                }
            }
            "전화번호" -> {
                // ------# 초기 세팅 #------
                setUIVisibility(2)
                binding.tvPCD.text = "전화번호 재설정"
                binding.tvPCDGuide.text = "전화번호 재설정을 위해 인증을 진행합니다"
                binding.etPCD1.apply {
                    inputType = InputType.TYPE_CLASS_NUMBER
                    filters = arrayOf(InputFilter.LengthFilter(12))
                }

                binding.btnPCDAuthSend.setOnSingleClickListener {
                    // TODO PASS 본인인증 창이 나와야 함
                }
                binding.btnPCDAuthConfirm.setOnSingleClickListener {
                    // TODO PASS 누르면 바로 변경 완료는 아니고 로직에 따라 이 버튼을 쓸지, 하단 버튼으로 바로 변경 완료할지 판단해야함.
                }
            }
            "성별" -> {
                setUIVisibility(3)
                binding.tvPCD.text = "성별 선택"
                binding.tvPCDGuide.text = "성별을 선택해주세요."
                setGenderButton( if (svm.setGender.value == "남자") false else true)
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
            updateUserData()
        }

        // ------# clear listener #------
        binding.ibtnPCD1Clear.setOnClickListener{ binding.etPCD1.setText("") }
        binding.ibtnPCD2Clear.setOnClickListener{ binding.etPCD2.setText("") }
        binding.ibtnPCD3Clear.setOnClickListener{ binding.etPCD3.setText("") }

    }

    private fun updateUserData() {
        val updatedItem = binding.etPCD1.text.toString()
        val encryptedPW = encrypt(svm.pw.value.toString(), getString(R.string.secret_key), getString(R.string.secret_iv))
        val jo = JSONObject().apply {
            when (arg) {
                "비밀번호" -> put("password_app", encryptedPW)
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
                        val addHyphenString = updatedItem.substring(0,4) + "-" + updatedItem.substring(4,6) + "-" + updatedItem.substring(6)
                        userJson.put("birthday", addHyphenString)
                        withContext(Dispatchers.Main) {
                            svm.setBirthday.value = addHyphenString
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
            } else if (isUpdateFinished == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "인터넷 연결이 필요합니다", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        }
    }

    private fun setGenderButton(isMale: Boolean) {
        when {
            isMale -> {
                binding.mrgPCD.check(binding.mrbPCDMale.id)
                binding.mrbPCDMale.isSelected = true
                binding.mrbPCDFemale.isSelected = false
                binding.ivPCDMale.isSelected = true
                binding.ivPCDFemale.isSelected = false
                svm.setGender.value = "남자"
            }
            !isMale -> {
                binding.mrgPCD.check(binding.mrbPCDFemale.id)
                binding.mrbPCDMale.isSelected = false
                binding.mrbPCDFemale.isSelected = true
                binding.ivPCDMale.isSelected = false
                binding.ivPCDFemale.isSelected = true
                svm.setGender.value = "여자"
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
                binding.tvPCDSkip.visibility = View.GONE
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
}