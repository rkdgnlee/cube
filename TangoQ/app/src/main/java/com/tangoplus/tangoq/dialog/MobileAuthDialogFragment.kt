package com.tangoplus.tangoq.dialog

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity.INPUT_METHOD_SERVICE
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLoginState
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.api.NetworkUser.fetchUserUPDATEJson
import com.tangoplus.tangoq.api.NetworkUser.storeUserInSingleton
import com.tangoplus.tangoq.databinding.FragmentMobileAuthDialogBinding
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.dialog.bottomsheet.AgreementBSDialogFragment.OnAgreeListener
import com.tangoplus.tangoq.fragment.ExtendedFunctions.dialogFragmentResize
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.function.SecurePreferencesManager.createKey
import com.tangoplus.tangoq.mediapipe.MathHelpers.isTablet
import com.tangoplus.tangoq.mediapipe.MathHelpers.phoneNumber82
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class MobileAuthDialogFragment : DialogFragment() {
    private lateinit var binding : FragmentMobileAuthDialogBinding
    private var verificationId = ""
    val svm : SignInViewModel by activityViewModels()
    private lateinit var auth : FirebaseAuth
    private var userSn = 0
    companion object {
        private const val ARG_USER_SN = "user_sn"
        fun newInstance(userSn: Int) : MobileAuthDialogFragment {
            val fragment = MobileAuthDialogFragment()
            val args = Bundle()
            args.putInt(ARG_USER_SN, userSn)
            fragment.arguments = args
            return fragment
        }
    }


    interface OnAuthFinishListener {
        fun onFinish(agree: Boolean)
    }
    private var listener: OnAuthFinishListener? = null

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

    fun setOnFinishListener(listener: OnAuthFinishListener) {
        this.listener = listener
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
        savedInstanceState: Bundle?): View {
        binding = FragmentMobileAuthDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        auth.setLanguageCode("kr")

        // userSN
        userSn = arguments?.getInt(ARG_USER_SN) ?: 0

        // 종료 버튼
        binding.ibtnMADExit.setOnSingleClickListener {
            Log.v("클릭리스너", "클릭리스너 클릭드")
            MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                setTitle("구글 로그인")
                setMessage("구글 로그인을 취소하시겠습니까?")
                setPositiveButton("예", {_, _ ->
                    if (dialog?.isShowing == true) {
                        if (Firebase.auth.currentUser != null) {
                            Firebase.auth.signOut()
                            Log.d("로그아웃", "Firebase sign out successful")
                        } else if (NaverIdLoginSDK.getState() == NidOAuthLoginState.OK) {
                            NaverIdLoginSDK.logout()
                            Log.d("로그아웃", "Naver sign out successful")
                        } else if (AuthApiClient.instance.hasToken()) {
                            UserApiClient.instance.logout { error->
                                if (error != null) {
                                    Log.e("로그아웃", "KAKAO Sign out failed", error)
                                } else {
                                    Log.e("로그아웃", "KAKAO Sign out successful")
                                }
                            }
                        }

                        dialog?.dismiss()
                    }
                })
                setNegativeButton("아니오", {_, _ ->
                    dismiss()
                })
            }.show()
        }


        binding.etMADMobile.requestFocus()
        binding.etMADMobile.postDelayed({
            val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etMADMobile, InputMethodManager.SHOW_IMPLICIT)
        }, 200)

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
                this@MobileAuthDialogFragment.verificationId = verificationId
                Log.v("onCodeSent", "메시지 발송 성공")
                // -----! 메시지 발송에 성공하면 스낵바 호출 !------
                Toast.makeText(requireContext(), "메시지 발송에 성공했습니다. 잠시만 기다려주세요", Toast.LENGTH_LONG).show()
                binding.btnMADAuthConfirm.isEnabled = true
            }
        }

        binding.btnMADAuthSend.setOnSingleClickListener {
            svm.transformMobile = phoneNumber82(binding.etMADMobile.text.toString())
            Log.v("sViewModel", "${svm.transformMobile}")
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
                    binding.etMADAuthNumber.isEnabled = true
                    binding.btnMADAuthConfirm.visibility = View.VISIBLE

                    val objectAnimator = ObjectAnimator.ofFloat(binding.clMAD, "translationY", 1f)
                    objectAnimator.duration = 1000
                    objectAnimator.start()
                    binding.etMADAuthNumber.requestFocus()
                }
                setNegativeButton("아니오", null)
                show()
            }
        }
        // -----! 휴대폰 인증 끝 !-----

        binding.btnMADAuthConfirm.setOnSingleClickListener {
            val credential = PhoneAuthProvider.getCredential(verificationId, binding.etMADAuthNumber.text.toString())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                signInWithPhoneAuthCredential(credential)
            }
        }  // -----! 인증 문자 확인 끝 !-----
        val mobilePattern = "^010-\\d{4}-\\d{4}\$"
        val mobilePatternCheck = Pattern.compile(mobilePattern)
        // ------! 핸드폰 번호 조건 코드 !-----
        binding.etMADMobile.addTextChangedListener(object: TextWatcher {
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
                svm.mobileCondition.value = mobilePatternCheck.matcher(binding.etMADMobile.text.toString()).find()
                if (svm.mobileCondition.value == true) {
                    binding.btnMADAuthSend.isEnabled = true
                } else {
                    binding.btnMADAuthSend.isEnabled = false
                }
            }
        })
    }
    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.P)
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    lifecycleScope.launch {
                        svm.mobileAuthCondition.value = true

                        // 값에 대해서 보내기
                        // 업데이트를 한다? -> 강제로 회원가입이 된거임. 엄격하게 필수동의항목을 동의하지 않으면 회원가입X이기 때문에 -> 정보를 넣어서 t_user_info에 정보가 있는지에 대해 판단해주는 api가 있으면 수정 가능.
                        val mobile = svm.transformMobile.replace("+8210", "010")
                        svm.transformMobile = mobile
                        Log.v("변환됐는지", svm.transformMobile)
                        val mobileJo = JSONObject().apply {
                            put("mobile", svm.transformMobile)
                        }
                        CoroutineScope(Dispatchers.IO).launch {
                            fetchUserUPDATEJson(requireContext(), getString(R.string.API_user), mobileJo.toString(), userSn.toString())
                        }
                        // 기존 입력 jo + 동의 항목인 jsonObj 통합
                        val jo = svm.googleJo
                        jo.put("mobile", svm.transformMobile)
                        storeUserInSingleton(requireContext(), jo)
                        listener?.onFinish(true)
                        Toast.makeText(requireContext(), "인증에 성공했습니다", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                } else {
                    Toast.makeText(requireContext(), "인증에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    Log.w(ContentValues.TAG, "mobile auth failed.")
                }
            }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
//        dialog?.window?.setDimAmount(0.7f)
//        dialog?.window?.setBackgroundDrawable(resources.getDrawable(R.drawable.bckgnd_rectangle_20, null))
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.setCancelable(false)
//        if (isTablet(requireContext())) {
//            dialog?.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
////            dialogFragmentResize(requireContext(), this, width = 0.7f, height = 0.3f)
//        } else {
//            dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
////            dialogFragmentResize(requireContext(), this, width = 0.9f, height = 0.425f)
//        }
    }

}