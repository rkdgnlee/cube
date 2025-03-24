package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
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
import androidx.appcompat.app.AppCompatActivity.INPUT_METHOD_SERVICE
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shuhart.stepview.StepView
import com.skydoves.progressview.ProgressView
import com.tangoplus.tangoq.IntroActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.etc.SpinnerAdapter
import com.tangoplus.tangoq.api.NetworkUser.emailDuplicateCheck
import com.tangoplus.tangoq.api.NetworkUser.insertUser
import com.tangoplus.tangoq.databinding.FragmentSignIn2Binding
import com.tangoplus.tangoq.dialog.LoadingDialogFragment
import com.tangoplus.tangoq.dialog.SignInDialogFragment
import com.tangoplus.tangoq.dialog.bottomsheet.AgreementBSDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.function.SecurePreferencesManager.encrypt
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class SignIn2Fragment : Fragment() {
    private lateinit var binding : FragmentSignIn2Binding
    val svm : SignInViewModel by activityViewModels()
    private var parentDialog: SignInDialogFragment? = null
    private lateinit var loadingDialog : LoadingDialogFragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSignIn2Binding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // dialog 세팅
        loadingDialog = LoadingDialogFragment.newInstance("회원가입확인")
        parentDialog  = (requireParentFragment() as SignInDialogFragment)
        binding.etEmailId.apply {
            binding.etEmailId.postDelayed({
                binding.etEmailId.requestFocus()
                val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.etEmailId, InputMethodManager.SHOW_IMPLICIT)
            }, 250)
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    parentDialog?.requireView()?.findViewById<StepView>(R.id.svSignIn)?.go(1, true)
                    parentDialog?.requireView()?.findViewById<ProgressView>(R.id.pvSignIn)
                        ?.let { it.progress = 60f }
                    parentDialog?.requireView()?.findViewById<TextView>(R.id.tvSignInGuide)
                        ?.let { it.text = "이메일을 입력해주세요" }
                }
            }
        }

        // email id check
        val emailRegex = "^[a-zA-Z0-9]{4,16}$"
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
            }
        }
        // emailId에서 바로 enter쳤을 경우 이메일 중복확인
        binding.etEmailId.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                confirmEmail()
                return@setOnEditorActionListener true
            }
            false
        }
        binding.btnEmailConfirm.setOnSingleClickListener {
            confirmEmail()
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
                if (binding.btnSignIn.isEnabled) {
                    showAgreementBottomSheetDialog(requireActivity())
                }
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
                        binding.tvPwRepeat.setTextColor(binding.tvPwRepeat.resources.getColor(R.color.successColor, null))
                        binding.tvPwRepeat.text = "일치합니다"
                    } else {
                        binding.tvPwRepeat.setTextColor(binding.tvPwRepeat.resources.getColor(R.color.deleteColor, null))
                        binding.tvPwRepeat.text = "올바르지 않습니다"
                    }
                    // -----! 뷰모델에 보낼 값들 넣기 !-----
                    svm.User.value?.put("password_app", s.toString())
                }



            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }) //-----! 입력 문자 조건 끝 !-----

        // 회원가입 버튼 state 관리
        svm.allTrueLiveData.observe(viewLifecycleOwner) {
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
                                            MaterialAlertDialogBuilder(context, com.tangoplus.tangoq.R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                                                setTitle("회원 가입 실패")
                                                setMessage("올바르지 않은 데이터가 존재합니다\n입력한 정보를 다시 확인해주세요")
                                                setPositiveButton("예") { _, _ ->
                                                    binding.etEmail.isEnabled = true
                                                    binding.etPw.isEnabled = true
                                                    binding.etPwRepeat.isEnabled = true
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
    private fun confirmEmail() {
        binding.btnEmailConfirm.isEnabled = false
        if (parentDialog?.isVisible == true) {
            loadingDialog = LoadingDialogFragment.newInstance("회원가입확인")
            loadingDialog.show(requireActivity().supportFragmentManager, "LoadingDialogFragment")
        }
        val emailId = binding.etEmailId.text.toString()
        svm.fullEmail.value = if (binding.etEmail.isGone) emailId + "@" + binding.spinner.selectedItem.toString() else emailId + "@" + binding.etEmail.text.toString()
        Log.v("이메일 전체", "${svm.fullEmail.value}")
        CoroutineScope(Dispatchers.IO).launch {
            val responseCode = emailDuplicateCheck(getString(R.string.API_user), svm.fullEmail.value ?: "")
            withContext(Dispatchers.Main) {
                if (parentDialog?.isAdded == true) {
                    loadingDialog.dismiss()
                }
                when (responseCode) {
                    200 -> {
                        MaterialAlertDialogBuilder(
                            requireContext(),
                            R.style.ThemeOverlay_App_MaterialAlertDialog
                        ).apply {
                            setTitle("알림")
                            setMessage("사용가능한 이메일입니다.\n이 이메일을 사용하시겠습니까?")
                            setPositiveButton("예") { _, _ ->
                                svm.emailVerify.value = true
                                binding.btnEmailConfirm.isEnabled = false
                                binding.btnEmailConfirm.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor400, null))
                                binding.etEmail.isEnabled = false
                                binding.etEmailId.isEnabled = false
                                binding.spinner.isEnabled = false
                                svm.User.value?.put("email", svm.fullEmail.value)
//                                    Log.v("id들어감", "${svm.User.value?.getString("user_id")}")
                                parentDialog?.requireView()?.findViewById<StepView>(R.id.svSignIn)?.go(2, true)
                                parentDialog?.requireView()?.findViewById<ProgressView>(R.id.pvSignIn)?.let { it.progress = 90f }
                                parentDialog?.requireView()?.findViewById<TextView>(R.id.tvSignInGuide)?.let { it.text = "비밀번호를 입력해주세요" }

                                // pw로 포커싱
                                binding.etPw.requestFocus()
                                binding.etPw.postDelayed({
                                    val imm = requireActivity().getSystemService(
                                        INPUT_METHOD_SERVICE
                                    ) as InputMethodManager
                                    imm.showSoftInput(binding.etPw, InputMethodManager.SHOW_IMPLICIT)
                                }, 250)
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
                            setMessage("이미 사용중인 이메일입니다.")
                            setNeutralButton("확인") { dialog, _ ->
                                binding.tvEmailCondition.text = "사용중인 이메일입니다. 다른 이메일을 입력해주세요"
                                binding.tvEmailCondition.setTextColor(ContextCompat.getColor(requireContext(), R.color.deleteColor))
                                dialog.dismiss()
                            }
                        }.show()
                        binding.btnEmailConfirm.isEnabled = true
                    }
                }
            }
        }
    }

}