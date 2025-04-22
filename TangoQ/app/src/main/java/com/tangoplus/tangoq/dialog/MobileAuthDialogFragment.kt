package com.tangoplus.tangoq.dialog

import android.app.Dialog
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.exception.NaverIdLoginSDKNotInitializedException
import com.navercorp.nid.oauth.NidOAuthLoginState
import com.tangoplus.tangoq.IntroActivity
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.api.NetworkUser.insertSNSUser
import com.tangoplus.tangoq.api.NetworkUser.linkOAuthAccount
import com.tangoplus.tangoq.api.NetworkUser.logoutDenyRefreshJwt
import com.tangoplus.tangoq.api.NetworkUser.sendMobileOTP
import com.tangoplus.tangoq.api.NetworkUser.sendMobileOTPToSNS
import com.tangoplus.tangoq.api.NetworkUser.storeUserInSingleton
import com.tangoplus.tangoq.api.NetworkUser.verifyMobileOTPToSNS
import com.tangoplus.tangoq.databinding.FragmentMobileAuthDialogBinding
import com.tangoplus.tangoq.db.Singleton_t_measure
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.dialog.bottomsheet.AgreementBSDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.function.AuthManager.setRetryAuthMessage
import com.tangoplus.tangoq.function.SaveSingletonManager
import com.tangoplus.tangoq.function.SecurePreferencesManager.createKey
import com.tangoplus.tangoq.function.SecurePreferencesManager.logout
import com.tangoplus.tangoq.function.SecurePreferencesManager.saveEncryptedJwtToken
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.lang.RuntimeException
import java.util.regex.Pattern

class MobileAuthDialogFragment : DialogFragment() {
    private lateinit var binding: FragmentMobileAuthDialogBinding
    val svm : SignInViewModel by activityViewModels()
    private lateinit var ssm : SaveSingletonManager
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMobileAuthDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 초기설정
        binding.ibtnMADExit.setOnSingleClickListener {
            MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                setTitle("연동 해제")
                setMessage("휴대폰 인증을 종료하시겠습니까?")
                setPositiveButton("예") {_, _ ->
                    dismiss()
                    activity?.lifecycleScope?.launch {
                        logoutDenyRefreshJwt(getString(R.string.API_user), requireContext()) { code -> }
                    }
                    WorkManager.getInstance(requireContext()).cancelUniqueWork("TokenCheckWork")
                    try {
                        if (Firebase.auth.currentUser != null) {
                            Firebase.auth.signOut()
                            Log.d("로그아웃", "Firebase sign out successful")
                        } else if (NaverIdLoginSDK.getState() == NidOAuthLoginState.OK) {
                            if (NaverIdLoginSDK.isInitialized()) {
                                NaverIdLoginSDK.logout()
                            }
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
                        saveEncryptedJwtToken(requireContext(), null)

                        // 싱글턴에 들어갔던거 전부 비우기
                        Singleton_t_user.getInstance(requireContext()).jsonObject = null
                        Singleton_t_measure.getInstance(requireContext()).measures = null
                    } catch (e: NaverIdLoginSDKNotInitializedException) {
                        Log.e("NILError", "네아로 is not initialized. (${e.message})")
                    } catch (e: RuntimeException) {
                        Log.e("네아로오류", "ThreadException : ${e.message}")
                    } catch (e: UninitializedPropertyAccessException) {
                        Log.e("카카오오류", "KaKao UninitializedProperty Exception : ${e.message}")
                    } catch (e: Exception) {
                        Log.e("카카오오류", "KaKao UninitializedProperty Exception : ${e.message}")
                    } catch (e: IllegalStateException) {
                        Log.e("logoutError", "LogoutIllegalState: ${e.message}")
                    } catch (e: java.lang.IllegalArgumentException) {
                        Log.e("logoutError", "LogoutIllegalArgument: ${e.message}")
                    } catch (e: NullPointerException) {
                        Log.e("logoutError", "LogoutNullPointer: ${e.message}")
                    } catch (e: InterruptedException) {
                        Log.e("logoutError", "LogoutInterrupted: ${e.message}")
                    } catch (e: IndexOutOfBoundsException) {
                        Log.e("logoutError", "LogoutIndexOutOfBounds: ${e.message}")
                    } catch (e: Exception) {
                        Log.e("logoutError", "Logout: ${e.message}")
                    }
                }
                setNegativeButton("아니오") {_, _ -> }
                show()
            }
        }

        binding.etMADMobile.postDelayed({
            binding.etMADMobile.requestFocus()
            val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etMADMobile, InputMethodManager.SHOW_IMPLICIT)
        }, 250)

        ssm = SaveSingletonManager(requireContext(), requireActivity())
        binding.etMADMobileCode.visibility = View.GONE
        binding.tvMADReAuth.visibility = View.GONE
        binding.tvMADCountDown.visibility = View.GONE
        binding.etMADMobileCode.isEnabled = false
        disabledSignInBtn()
        binding.btnMADSignIn.visibility = View.GONE

        svm.countDownTimer?.cancel()
        svm.mobileCondition.value = false
        svm.mobileAuthCondition.value = false
        svm.passMobile.value = ""

        val mobilePattern = "^010-\\d{4}-\\d{4}\$"
        val mobilePatternCheck = Pattern.compile(mobilePattern)
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
                    binding.etMADMobile.setText(formatted) // setText를 사용하여 확실하게 변경
                    binding.etMADMobile.setSelection(formatted.length) // 커서를 마지막 위치로 이동
                }

                isFormatting = false
                svm.mobileCondition.value = mobilePatternCheck.matcher(binding.etMADMobile.text.toString()).find()
                if (svm.mobileCondition.value == true)  {
                    svm.passMobile.value = s.toString()
                    enabledSendBtn()
                } else {
                    svm.passMobile.value = ""
                    disabledSendBtn()
                }
            }
        })

        // 키보드 엔터 눌렀을 때 전송 알림창 보이기
        binding.btnMADAuthSend.setOnSingleClickListener { sendMobileCode() }
        binding.etMADMobile.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                if (svm.mobileCondition.value == true) {
                    sendMobileCode()
                }
                true  // 이벤트 처리가 완료되었음을 반환
            } else {
                false // 다른 동작들은 그대로 유지
            }
        }

        // 6자리 넣었을 떄 자동 검증
        binding.etMADMobileCode.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 6) {
                    verifyMobileCode()
                }
            }
        })

        binding.btnMADSignIn.setOnSingleClickListener {
            showAgreementBottomSheetDialog(requireActivity())
        }
    }
    private fun enabledSendBtn() {
        binding.btnMADAuthSend.apply { 
            visibility = View.VISIBLE
            isEnabled = true
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
        }
    }
    private fun disabledSendBtn() {
        binding.btnMADAuthSend.apply {
            isEnabled = false
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400))
        }
    }

    private fun enabledSignInBtn() {
        binding.btnMADSignIn.apply {
            visibility = View.VISIBLE
            isEnabled = true
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
        }
    }
    private fun disabledSignInBtn() {
        binding.btnMADSignIn.apply {
            isEnabled = false
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400))
        }
    }


    private fun sendMobileCode() {
        val configureMobile = svm.passMobile.value?.replace("-", "")
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setTitle("휴대폰 인증")
            setMessage("${svm.passMobile.value}로 인증하시겠습니까?")
            setPositiveButton("예") { _, _ ->
                val bodyJo = JSONObject().apply {
                    put("mobile", configureMobile)
                }
                lifecycleScope.launch(Dispatchers.IO) {
                    val statusCode =
                        sendMobileOTPToSNS(getString(R.string.API_user), bodyJo.toString()) ?: 0
                    withContext(Dispatchers.Main) {
                        navigateMobileCode(statusCode)
                    }
                }
            }
            setNegativeButton("아니오") {_, _ ->

            }
        }.show()
    }

    private fun verifyMobileCode() {
        val bodyJo = JSONObject().apply {
            put("mobile", svm.passMobile.value?.replace("-", ""))
            put("otp", binding.etMADMobileCode.text)
            put("temp_id", svm.tempId)
            put("email", svm.fullEmail.value)
        }
        Log.v("bodyJo", "$bodyJo")
        lifecycleScope.launch(Dispatchers.IO) {
            val response = verifyMobileOTPToSNS(getString(R.string.API_user), bodyJo.toString())
            withContext(Dispatchers.Main) {
                if (response != null) {
                    navigateMobileVerify(response)
                }
            }
        }
    }

    private fun setReSendMessage() {
        binding.tvMADReAuth.visibility = View.VISIBLE
        binding.tvMADCountDown.visibility = View.VISIBLE
        binding.etMADMobileCode.visibility = View.VISIBLE
        val madb = MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setTitle("인증번호 재전송")
            setMessage("${svm.passMobile.value}로 인증번호를 다시 전송하시겠습니까?")
            setPositiveButton("예") { _, _ ->
                val configureMobile = svm.passMobile.value?.replace("-", "")
                val bodyJo = JSONObject().apply {
                    put("mobile", configureMobile)
                }
                // 인증번호 다시 보내기
                lifecycleScope.launch(Dispatchers.IO) {
                    val statusCode =
                        sendMobileOTPToSNS(getString(R.string.API_user), bodyJo.toString()) ?: 0
                    withContext(Dispatchers.Main) {
                        when (statusCode) {
                            200, 201, 1 -> {
                                Toast.makeText(requireContext(), "인증번호를 전송했습니다. 휴대폰을 확인해주세요", Toast.LENGTH_SHORT).show()
                                setReSendMessage()
                                binding.etMADMobileCode.postDelayed({
                                    binding.etMADMobileCode.requestFocus()
                                }, 250)
                                binding.tvMADReAuth.visibility = View.VISIBLE

                                disabledSendBtn()
                                binding.etMADMobile.apply {
                                    isEnabled = false
                                    backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
                                }
                            }
                            else -> {
                                Toast.makeText(requireContext(), "인증번호 전송에 실패했습니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            setNegativeButton("아니오") { _, _ -> }
        }
        setRetryAuthMessage(requireContext(), svm, binding.tvMADReAuth, binding.tvMADCountDown, madb)
    }

    private fun navigateMobileCode(statusCode : Int) {
        when (statusCode) {
            200, 201, 1 -> {
                Toast.makeText(requireContext(), "인증번호를 전송했습니다. 휴대폰을 확인해주세요", Toast.LENGTH_SHORT).show()
                setReSendMessage()
                binding.etMADMobileCode.apply {
                    visibility = View.VISIBLE
                    isEnabled = true
                    postDelayed({
                        binding.etMADMobileCode.requestFocus()
                        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(binding.etMADMobileCode, InputMethodManager.SHOW_IMPLICIT)
                    }, 250)
                }

                binding.tvMADReAuth.visibility = View.VISIBLE

                disabledSendBtn()
                binding.etMADMobile.apply {
                    isEnabled = false
                    backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
                }
            }
            else -> {
                Toast.makeText(requireContext(), "인증번호 전송에 실패했습니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun navigateMobileVerify(response : JSONObject) {
        when (response.optInt("status")) {
            200, 201 -> {
                svm.insertToken = response.optString("jwt")
                svm.mobileAuthCondition.value = true

                binding.btnMADSignIn.visibility = View.VISIBLE

                // 완료 후 모바일 인증 잠금
                enabledSignInBtn()
                binding.etMADMobileCode.apply {
                    isEnabled = false
                    backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
                }

                // agreement
                if (svm.mobileAuthCondition.value == true) {
                    enabledSignInBtn()
                    Toast.makeText(requireContext(), "인증에 성공했습니다. 하단에 회원가입 버튼을 눌러주세요", Toast.LENGTH_SHORT).show()
                    showAgreementBottomSheetDialog(requireActivity())
                } else {
                    Toast.makeText(requireContext(), "인증에 성공했습니다. 필수 정보를 다시 확인해주세요", Toast.LENGTH_LONG).show()
                }
            }
            401 -> {
                MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                    setTitle("알림")
                    setMessage("만료 혹은 올바르지 않은 인증번호입니다. 다시 시도해주세요")
                    setPositiveButton("예", {_, _ ->
                        binding.etMADMobileCode.setText("")
                    })
                }.show()
            }
            409 -> {
                when (response.optBoolean("linkage")) {
                    true -> {
                        svm.insertToken = response.optString("jwt")
                        MaterialAlertDialogBuilder( requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                            setTitle("연동 여부")
                            setMessage("현재 존재하는 계정입니다. 기존 계정과 연동하시겠습니까?")
                            setPositiveButton("예") {_, _ ->
                                val jo = JSONObject().apply {
                                    put("email", svm.fullEmail.value)
                                    put("mobile", svm.passMobile.value?.replace("-", ""))
                                    put("provider", svm.provider)
                                    put("access_token", svm.sdkToken)
                                }
                                Log.v("409Json", "$jo, ${svm.insertToken}, ${svm.sdkToken}")
                                linkOAuthAccount(getString(R.string.API_user), svm.insertToken, jo.toString(), requireContext()) { responseJo ->
                                    if (responseJo != null) {
                                        navigateOAuthLink(responseJo)
                                    }
                                }
                            }
                            setNegativeButton("아니오") {_,_ ->
                                dismiss()
                                Toast.makeText(requireContext(), "로그인을 진행해주세요", Toast.LENGTH_LONG).show()
                            }
                            setCancelable(false)
                            show()
                        }
                    }
                    false -> {
                        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                            setTitle("알림")
                            setMessage("이미 회원가입 한 회원입니다. 로그인 혹은 이메일 찾기를 진행해주세요")
                            setPositiveButton("예") {_, _ ->
                                Toast.makeText(requireContext(), "로그인을 진행해주세요", Toast.LENGTH_SHORT).show()
                                dismiss()
                            }
                            show()
                        }
                    }
                }
                svm.countDownTimer?.cancel()
            }
            500 -> {
                Toast.makeText(requireContext(), "서버 오류 입니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
            else -> {
                MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                    setTitle("알림")
                    setMessage("만료 혹은 올바르지 않은 인증번호입니다. 다시 시도해주세요")
                    setPositiveButton("예", {_, _ ->
                        binding.etMADMobileCode.setText("")
                    })
                }.show()
            }
        }
    }
    private fun navigateSNSInsert(responseJo : JSONObject) {
        val responseCode = responseJo.optInt("status")
        when (responseCode) {
            200, 201 -> {
                storeUserInSingleton(requireContext(), responseJo)
                createKey(getString(R.string.SECURE_KEY_ALIAS))
                Log.v("SDK>싱글톤", "${Singleton_t_user.getInstance(requireContext()).jsonObject}")
                val userUUID = Singleton_t_user.getInstance(requireContext()).jsonObject?.optString("user_uuid")
                val userInfoSn =  Singleton_t_user.getInstance(requireContext()).jsonObject?.optString("sn")?.toInt()
                if (userUUID != null && userInfoSn != null) {
                    ssm.getMeasures(userUUID, userInfoSn,  CoroutineScope(Dispatchers.IO)) {
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        startActivity(intent)
                        requireActivity().finishAffinity()
                    }
                }
            }
            409 -> {
                when (responseJo.optBoolean("linkage")) {
                    true -> {
                        svm.insertToken = responseJo.optString("jwt")
                        MaterialAlertDialogBuilder( requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                            setTitle("연동 여부")
                            setMessage("현재 존재하는 계정입니다. 기존 계정과 연동하시겠습니까?")
                            setPositiveButton("예") {_, _ ->
                                val jo = JSONObject().apply {
                                    put("email", svm.fullEmail.value)
                                    put("mobile", svm.passMobile.value?.replace("-", ""))
                                    put("provider", svm.provider)
                                    put("access_token", svm.sdkToken)
                                }
                                Log.v("409Json", "$jo, ${svm.insertToken} ${svm.sdkToken}")
                                linkOAuthAccount(getString(R.string.API_user), svm.insertToken, jo.toString(), requireContext()) { responseJo ->
                                    if (responseJo != null) {
                                        navigateOAuthLink(responseJo)
                                    }
                                }
                            }
                            setNegativeButton("아니오") {_,_ ->  }
                            setCancelable(false)
                            show()
                        }

                    }
                    false -> {
                        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                            setTitle("알림")
                            setMessage("이미 회원가입 한 회원입니다. 로그인 혹은 이메일 찾기를 진행해주세요")
                            setPositiveButton("예") {_, _ ->
                                Toast.makeText(requireContext(), "로그인을 진행해주세요", Toast.LENGTH_SHORT).show()
                                dismiss()
                            }
                            show()
                        }
                    }
                }
                svm.countDownTimer?.cancel()
            }
            500 -> {
                Toast.makeText(requireContext(), "서버 오류 입니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(requireContext(), "인증에 실패했습니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAgreementBottomSheetDialog(context: FragmentActivity) {
        val bottomSheetFragment = AgreementBSDialogFragment()
        bottomSheetFragment.setOnFinishListener(object : AgreementBSDialogFragment.OnAgreeListener {
            override fun onFinish(agree: Boolean) {
                if (agree) {
                    val jo = JSONObject().apply {
                        put("mobile", svm.passMobile.value?.replace("-", ""))
                        put("provider", svm.provider)
                        put("user_sn", 0)
                        put("device_sn", 0)
                        put("sms_receive", if (svm.agreementMk1.value == true) 1 else 0)
                        put("email_receive", if (svm.agreementMk2.value == true) 1 else 0)
                    }
                    Log.v("insertSNSUser", "$jo, ${svm.insertToken}")
                    insertSNSUser(getString(R.string.API_user), svm.insertToken, jo.toString(), requireContext()) { responseJo ->
                        if (responseJo != null) {
                            navigateSNSInsert(responseJo)
                        } else {
                            Toast.makeText(requireContext(), "로그인에 실패했습니다\n관리자 문의가 필요합니다", Toast.LENGTH_LONG).show()
                            logout(requireActivity(), 0)
                        }
                    }
                }
            }
        })
        val fragmentManager = context.supportFragmentManager
        bottomSheetFragment.show(fragmentManager, bottomSheetFragment.tag)
    }

    private fun navigateOAuthLink(responseJo : JSONObject) {
        val responseCode = responseJo.optInt("status")
        when (responseCode) {
            200, 201 -> {
                storeUserInSingleton(requireContext(), responseJo)
                createKey(getString(R.string.SECURE_KEY_ALIAS))
                Log.v("SDK>싱글톤", "${Singleton_t_user.getInstance(requireContext()).jsonObject}")
                val userUUID = Singleton_t_user.getInstance(requireContext()).jsonObject?.optString("user_uuid")
                val userInfoSn =  Singleton_t_user.getInstance(requireContext()).jsonObject?.optString("sn")?.toInt()
                if (userUUID != null && userInfoSn != null) {
                    ssm.getMeasures(userUUID, userInfoSn,  CoroutineScope(Dispatchers.IO)) {
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        startActivity(intent)
                        requireActivity().finishAffinity()
                    }
                }
            }

            400 -> {
                Toast.makeText(requireContext(), "올바르지 않은 이메일 입니다. 다시 확인해주세요", Toast.LENGTH_SHORT).show()
            }
            404 -> {
                Toast.makeText(requireContext(), "존재하지 않는 사용자입니다. 정보를 다시 확인해주세요", Toast.LENGTH_SHORT).show()
            }
            500 -> {
                Toast.makeText(requireContext(), "서버 오류 입니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(requireContext(), "인증에 실패했습니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bckgnd_rectangle))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
    private fun showExitDialog() {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setMessage("휴대폰인증을 종료하시겠습니까?")
            setPositiveButton("예") { _, _ ->
                dialog?.dismiss()
            }
            setNegativeButton("아니오") { _, _ -> }
            show()
        }
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCancelable(false)
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                showExitDialog()
                true // 이벤트 소비
            } else {
                false
            }
        }
        return dialog
    }
}