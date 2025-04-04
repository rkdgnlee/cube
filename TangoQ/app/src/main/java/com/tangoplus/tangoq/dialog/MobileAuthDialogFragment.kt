package com.tangoplus.tangoq.dialog

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.api.NetworkUser.sendMobileOTP
import com.tangoplus.tangoq.api.NetworkUser.sendMobileOTPToSNS
import com.tangoplus.tangoq.api.NetworkUser.verifyMobileOTP
import com.tangoplus.tangoq.api.NetworkUser.verifyMobileOTPToSNS
import com.tangoplus.tangoq.databinding.FragmentMobileAuthDialogBinding
import com.tangoplus.tangoq.dialog.bottomsheet.AgreementBSDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.function.AuthManager.setRetryAuthMessage
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.regex.Pattern

class MobileAuthDialogFragment : DialogFragment() {
    private lateinit var binding: FragmentMobileAuthDialogBinding
    val svm : SignInViewModel by activityViewModels()
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

        binding.tvMADReAuth.visibility = View.GONE
        binding.tvMADCountDown.visibility = View.GONE
        binding.etMADMobileCode.isEnabled = false
        disabledSignInBtn()

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
            Toast.makeText(requireContext(), "인증에 성공했습니다. 하단에 회원가입 버튼을 눌러주세요", Toast.LENGTH_SHORT).show()
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
            backgroundTintList = null
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bckgnd_12_edit_text)
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
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val response = verifyMobileOTPToSNS(getString(R.string.API_user), bodyJo.toString())
            withContext(Dispatchers.Main) {
                navigateMobileVerify(response)
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
                    val statusCode = sendMobileOTPToSNS(getString(R.string.API_user), bodyJo.toString())
                    withContext(Dispatchers.Main) {
                        when (statusCode) {
                            1 -> {
                                Toast.makeText(
                                    requireContext(),
                                    "인증번호를 전송했습니다. 휴대폰을 확인해주세요",
                                    Toast.LENGTH_SHORT
                                ).show()
                                setReSendMessage()
                            }

                            else -> {
                                Toast.makeText(
                                    requireContext(),
                                    "인증번호 전송에 실패했습니다. 다시 시도해주세요",
                                    Toast.LENGTH_SHORT
                                ).show()
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
            409 -> {
                MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                    setMessage("이미 존재하는 핸드폰 번호입니다. 재확인 후 다시 시도해주세요")
                    setPositiveButton("예") { _, _ ->

                    }

                }.show()
            }
            400 -> {
                Toast.makeText(requireContext(), "휴대폰 번호가 올바르지 않습니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
            422 -> {
                Toast.makeText(requireContext(), "휴대폰번호가 존재하지 않습니다", Toast.LENGTH_SHORT).show()
            }
            429 -> {
                Toast.makeText(requireContext(), "요청 가능 횟수를 초과헀습니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
            500 -> {
                Toast.makeText(requireContext(), "서버 오류 입니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(requireContext(), "인증번호 전송에 실패했습니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun navigateMobileVerify(response : Pair<String, Int>?) {
        when (response?.second) {
            200, 201 -> {

                svm.passAuthCondition.value = true
                svm.countDownTimer?.cancel()
                binding.btnMADSignIn.visibility = View.VISIBLE

                // 완료 후 모바일 인증 잠금
                binding.etMADMobileCode.apply {
                    isEnabled = false
                    backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
                }
                if (response.first.length > 20) {
                    Log.v("토큰받아오기", response.first)
                    svm.insertToken = response.first
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
                    // TODO 동의 후 경로 설정
                }
            }
        })
        val fragmentManager = context.supportFragmentManager
        bottomSheetFragment.show(fragmentManager, bottomSheetFragment.tag)
    }

    override fun onResume() {
        super.onResume()
        isCancelable = false
        dialog?.window?.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bckgnd_rectangle_20))
    }

}