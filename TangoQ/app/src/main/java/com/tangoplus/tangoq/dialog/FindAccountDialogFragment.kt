package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.widget.AdapterView
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.SpinnerAdapter
import com.tangoplus.tangoq.data.SignInViewModel
import com.tangoplus.tangoq.databinding.FragmentFindAccountDialogBinding
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class FindAccountDialogFragment : DialogFragment() {
    lateinit var binding : FragmentFindAccountDialogBinding
    private lateinit var firebaseAuth : FirebaseAuth
    val viewModel : SignInViewModel by viewModels()
    var verificationId = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFindAccountDialogBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ------! 초기 세팅 시작 !------
        binding.clFADId.visibility = View.GONE
        binding.clFADPw.visibility = View.GONE
        binding.tvFADPw.visibility = View.GONE
        firebaseAuth = Firebase.auth
        binding.btnFADAuthSend.isEnabled = false
        // ------! 초기 세팅 끝 !------

        binding.ibtnFADBack.setOnClickListener { dismiss() }
        // ------! 탭으로 아이디 비밀번호 레이아웃 나누기 시작 !------
        binding.tlFAB.addOnTabSelectedListener(object : OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when(tab?.position) {
                    0 -> {
                        binding.clFADMobile.visibility = View.VISIBLE
                        binding.clFADId.visibility = View.GONE
                        binding.clFADPw.visibility = View.GONE
                        binding.tvFADPw.visibility = View.GONE
                        binding.btnFADConfirm.text = "인증 하기"
                        binding.btnFADConfirm.isEnabled = false
                    }
                    1 -> {
                        binding.clFADMobile.visibility = View.GONE
                        binding.clFADId.visibility = View.GONE
                        binding.clFADPw.visibility = View.VISIBLE
                        binding.tvFADPw.visibility = View.GONE
                        binding.btnFADConfirm.text = "비밀번호 찾기"
                        binding.btnFADConfirm.isEnabled = true
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        // ------! 비밀번호 재설정 시작 !------

        // ------! 핸드폰 번호 - 시작 !------
        val mobilePattern = "^010-\\d{4}-\\d{4}\$"
        val mobilePatternCheck = Pattern.compile(mobilePattern)
        binding.etFADMobile.addTextChangedListener(object: TextWatcher {
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
                Log.w("전화번호형식", "${mobilePatternCheck.matcher(binding.etFADMobile.text.toString()).find()}")
                viewModel.mobileCondition.value = mobilePatternCheck.matcher(binding.etFADMobile.text.toString()).find()
                if (viewModel.mobileCondition.value == true) {
                    viewModel.User.value?.put("user_mobile", s.toString() )
                    binding.btnFADAuthSend.isEnabled = true
                }

            }
        }) // ------! 핸드폰 번호 - 시작 !------






        // ------! 인증 문자 확인 시작 !------
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(p0: PhoneAuthCredential) {}
            override fun onVerificationFailed(p0: FirebaseException) {
                Log.e("failedAuth", "$p0")
            }
            @RequiresApi(Build.VERSION_CODES.P)
            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                super.onCodeSent(verificationId, token)
                this@FindAccountDialogFragment.verificationId = verificationId
                Log.v("onCodeSent", "메시지 발송 성공")
                // -----! 메시지 발송에 성공하면 스낵바 호출 !------
                Snackbar.make(requireActivity().requireViewById(R.id.clIntro), "메시지 발송에 성공했습니다. 잠시만 기다려주세요", Snackbar.LENGTH_LONG).show()
                binding.btnFADConfirm.isEnabled = true
            }
        } // ------! 인증 문자 확인 끝 !------

        binding.btnFADConfirm.setOnClickListener{
            when (binding.btnFADConfirm.text) {
                "인증 하기" -> {
                    var transformMobile = phoneNumber82(binding.etFADMobile.text.toString())
                    val dialog = AlertDialog.Builder(requireContext())
                        .setTitle("📩 문자 인증 ")
                        .setMessage("$transformMobile 로 인증 하시겠습니까?")
                        .setPositiveButton("예") { _, _ ->
                            transformMobile = transformMobile.replace("-", "")
                            Log.w("전화번호", transformMobile)
                            val optionsCompat = PhoneAuthOptions.newBuilder(firebaseAuth)
                                .setPhoneNumber(transformMobile)
                                .setTimeout(60L, TimeUnit.SECONDS)
                                .setActivity(requireActivity())
                                .setCallbacks(callbacks)
                                .build()
                            PhoneAuthProvider.verifyPhoneNumber(optionsCompat)
                            firebaseAuth.setLanguageCode("kr")

                            val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
                            alphaAnimation.duration = 600
                            binding.etFADAuthNumber.isEnabled = true
                            binding.etFADAuthNumber.requestFocus()
                        }
                        .setNegativeButton("아니오", null)
                        .show()

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
                }
                "아이디 찾기" -> {
                    binding.clFADMobile.visibility = View.GONE

//                    getUserSELECTJson(getString(R.string.IP_ADDRESS_t_user), URLEncoder.encode(binding.etFADMobile.text.toString(), "UTF-8")) { jo ->
//                        if (jo?.getInt("status") == 404) {
//                            requireActivity().runOnUiThread {
//                                val dialog = AlertDialog.Builder(requireContext())
//                                    .setTitle("알림⚠️")
//                                    .setMessage("일치하는 계정이 없습니다.\n다시 시도해주세요")
//                                    .setPositiveButton("예") { _, _ -> }
//                                    .show()
//                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
//                                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
//                            }
//                        } else if (jo?.getInt("status") == 200) {
//                            requireActivity().runOnUiThread{
//                                binding.clFADId.visibility = View.VISIBLE
//                                binding.tvFADIdFinded.text = maskString(jo.optJSONObject("data")?.getString("user_id") ?: "")
//                            }
//                        }
//
//                    }
                    binding.btnFADAuthSend.text= "초기 화면으로"
                }
                "비밀번호 찾기" -> {
                    val email = when (binding.FADSpinner.selectedItemPosition) {
                        0, 1, 2 -> {
                            "${binding.etFADEmailId.text}@${binding.FADSpinner.selectedItem as String}"

                        }
                        else -> {
                            "${binding.etFADEmailId.text}@${binding.etFADEmail.text}"
                        }
                    }
                    if (binding.etFADEmailId.text.length != 0) {
                        firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener{ task ->
                            if (task.isSuccessful) {
                                binding.tvFADPw.visibility = View.VISIBLE
                                binding.clFADPw.visibility = View.GONE
                                binding.btnFADConfirm.text = "초기 화면으로"
                            }
                        }
                    }
                }
            }
        }
        // ------! 비밀번호 재설정 끝 !------

        val domainList = listOf("gmail.com", "naver.com", "kakao.com", "직접입력")
        binding.FADSpinner.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, domainList)
        binding.FADSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            @SuppressLint("SetTextI18n")
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                binding.FADSpinner.getItemAtPosition(position).toString()
                if (position == 3) {
                    binding.etFADEmail.visibility = View.VISIBLE
                    binding.FADSpinner.visibility = View.GONE
                    binding.ivFADSpinner.setOnClickListener{
                        binding.FADSpinner.performClick()
                        binding.FADSpinner.visibility = View.VISIBLE
                    }
                } else {
                    binding.etFADEmail.visibility = View.GONE
                    binding.etFADEmail.setText("")
                    binding.FADSpinner.visibility = View.VISIBLE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }


    }
    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.P)
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    requireActivity().runOnUiThread {
                        viewModel.mobileAuthCondition.value = true
                        binding.etFADAuthNumber.isEnabled = false
                        binding.etFADMobile.isEnabled = false

                        // ------! 번호 인증 완료 !------
                        val snackbar = Snackbar.make(requireActivity().requireViewById(R.id.clSignIn), "인증에 성공했습니다 !", Snackbar.LENGTH_SHORT)
                        snackbar.setAction("확인") { snackbar.dismiss() }
                        snackbar.setActionTextColor(Color.WHITE)
                        snackbar.show()
                        binding.btnFADConfirm.text = "아이디 찾기"
                    }
                } else {
                    Log.w(ContentValues.TAG, "mobile auth failed.")
                }
            }
    }

    private fun phoneNumber82(msg: String) : String {
        val firstNumber: String = msg.substring(0,3)
        var phoneEdit = msg.substring(3)
        when (firstNumber) {
            "010" -> phoneEdit = "+8210$phoneEdit"
            "011" -> phoneEdit = "+8211$phoneEdit"
            "016" -> phoneEdit = "+8216$phoneEdit"
            "017" -> phoneEdit = "+8217$phoneEdit"
            "018" -> phoneEdit = "+8218$phoneEdit"
            "019" -> phoneEdit = "+8219$phoneEdit"
            "106" -> phoneEdit = "+82106$phoneEdit"
        }
        return phoneEdit
    }

    private fun maskString(input: String): String {
        return if (input.length > 3) {
            input.replaceRange(3, input.length, "*".repeat(input.length - 3))
        } else {
            input
        }
    }

    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

}