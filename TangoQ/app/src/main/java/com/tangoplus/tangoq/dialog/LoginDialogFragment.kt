package com.tangoplus.tangoq.dialog

import android.app.Activity
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.api.NetworkUser
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import com.tangoplus.tangoq.databinding.FragmentLoginDialogBinding
import com.tangoplus.tangoq.function.PreferencesManager
import com.tangoplus.tangoq.function.SecurePreferencesManager.createKey
import com.tangoplus.tangoq.api.NetworkUser.getUserIdentifyJson
import com.tangoplus.tangoq.function.SaveSingletonManager
import com.tangoplus.tangoq.function.SecurePreferencesManager.saveEncryptedJwtToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.regex.Pattern

class LoginDialogFragment : DialogFragment() {
    lateinit var binding: FragmentLoginDialogBinding
    val viewModel : SignInViewModel by activityViewModels()
    private lateinit var ssm : SaveSingletonManager
    private lateinit var prefs : PreferencesManager
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedElementEnterTransition = TransitionInflater.from(requireContext()).inflateTransition(R.transition.fade_in_out_transition)
        sharedElementReturnTransition = TransitionInflater.from(requireContext()).inflateTransition(R.transition.fade_in_out_transition)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ------# 로그인 count 저장 #------
        prefs = PreferencesManager(requireContext())
        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        ssm = SaveSingletonManager(requireContext(), requireActivity())
        binding.etLDId.requestFocus()
        binding.etLDId.postDelayed({
            imm.showSoftInput(binding.etLDId, InputMethodManager.SHOW_IMPLICIT)
        }, 250)

        imm.hideSoftInputFromWindow(view.windowToken, 0)

        // ------! 로그인 시작 !------
        val idPattern = "^[\\s\\S]{4,16}$" // 영문, 숫자 4 ~ 16자 패턴
        val idPatternCheck = Pattern.compile(idPattern)
        val pwPattern = "^[\\s\\S]{4,20}$" // 영문, 특수문자, 숫자 8 ~ 20자 패턴 ^[a-zA-Z0-9]{6,20}$
        val pwPatternCheck = Pattern.compile(pwPattern)
        binding.etLDId.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.id.value = s.toString()
                viewModel.currentIdCon.value = idPatternCheck.matcher(binding.etLDId.text.toString()).find()
                Log.v("idPw", "${viewModel.currentIdCon.value} ,${viewModel.idPwCondition.value}")
            }
        })
        binding.etLDPw.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.pw.value = s.toString()
                viewModel.currentPwCon.value = pwPatternCheck.matcher(binding.etLDPw.text.toString()).find()
                Log.v("idPw", "${viewModel.currentPwCon.value} ,${viewModel.idPwCondition.value}")
            }
        })
        binding.etLDPw.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                tryLogin()
                true  // 이벤트 처리가 완료되었음을 반환
            } else {
                false // 다른 동작들은 그대로 유지
            }
        }

        // 응답과 관계없이 로그인 시도 5회 실패시 잠금 ( 3분간 )
        // 5회 동안 이렇게 세고, 6회 시도 시 실패 -> 5분 7회 -> 10 8회 -> 20 9회 30분 10회 -> 이제 추가함
        binding.ibtnLDIdClear.setOnClickListener { binding.etLDId.text.clear() }
        binding.ibtnLDPwClear.setOnClickListener { binding.etLDPw.text.clear() }

        binding.btnLDLogin.setOnClickListener {
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            tryLogin()
        } // ------! 로그인 끝 !------

        // ------! 비밀번호 및 아이디 찾기 시작 !------
        binding.tvLDFind.setOnClickListener {
            val dialog = FindAccountDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "FindAccountDialogFragment")
        }
        // ------! 비밀번호 및 아이디 찾기 시작 !------
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    private fun tryLogin() {
        if (viewModel.idPwCondition.value == true) {

            val jsonObject = JSONObject()
            jsonObject.put("user_id", viewModel.id.value)
            jsonObject.put("password", viewModel.pw.value)
            Log.v("idPW", "${viewModel.id.value}, ${viewModel.pw.value}")
            val dialog = LoadingDialogFragment.newInstance("로그인")
            dialog.show(requireActivity().supportFragmentManager, "LoadingDialogFragment")

            lifecycleScope.launch {
                getUserIdentifyJson(getString(R.string.API_user), jsonObject) { jo ->
                    val statusCode = jo?.optInt("status") ?: 0
                    val retryAfter = jo?.optInt("retry_after") ?: 0
                    Log.v("코드", "$statusCode")
                    // TODO status Code 수정
                    if (statusCode == 0) {
                        val jsonObj = JSONObject()
                        jsonObj.put("access_jwt", jo?.optString("access_jwt"))
                        jsonObj.put("refresh_jwt", jo?.optString("refresh_jwt"))
                        saveEncryptedJwtToken(requireContext(), jsonObj)

                        requireActivity().runOnUiThread {
                            binding.btnLDLogin.isEnabled = false
                            viewModel.User.value = null
                            // ------! 싱글턴 + 암호화 저장 시작 !------
                            if (jo != null ) {
                                // ------# 최초 로그인 #------
                                // ------# 기존 로그인 #------
                                NetworkUser.storeUserInSingleton(requireContext(), jo)
                                createKey(getString(R.string.SECURE_KEY_ALIAS))
//                                        saveEncryptedToken(requireContext(), getString(R.string.SECURE_KEY_ALIAS), encryptToken(getString(R.string.SECURE_KEY_ALIAS), jsonObject))
                                val userUUID = Singleton_t_user.getInstance(requireContext()).jsonObject?.optString("user_uuid") ?: ""
                                val userInfoSn =  Singleton_t_user.getInstance(requireContext()).jsonObject?.optString("sn")?.toInt() ?: -1
                                val safeContext = context
                                if (safeContext != null) {
                                    dialog.dismiss()
                                    ssm.getMeasures(userUUID, userInfoSn, CoroutineScope(Dispatchers.IO)) {
                                        Log.v("자체로그인완료", "${Singleton_t_user.getInstance(safeContext).jsonObject}")
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            val intent = Intent(safeContext, MainActivity::class.java)
                                            safeContext.startActivity(intent)
                                            (safeContext as? Activity)?.finishAffinity()
                                        }, 250)
                                    }
                                }
                            }
                            // ------! 싱글턴 + 암호화 저장 끝 !------
                        }
                    } else {
                        requireActivity().runOnUiThread {
                            dialog.dismiss()
                            makeMaterialDialog(
                                when (statusCode) {
                                    401, 404 -> 0
                                    429 -> 1
                                    423 -> 2
                                    else -> 0
                                },
                                retryAfter
                            )
                            binding.etLDPw.text.clear()
                        }
                    }
                }
            }
        }
    }
    private fun makeMaterialDialog(case: Int, retryAfter : Int) {
        val message = when (case) {
            0 -> "비밀번호 또는 아이디가 올바르지 않습니다.\n로그인을 3회 이상 실패했을 경우 일정 시간 제한될 수 있습니다."
            1 -> "반복적인 로그인 실패로, 로그인이 ${retryAfter}초 동안 제한됩니다."
            2 -> "비밀번호를 10회 틀려 계정이 잠겼습니다.\n고객센터로 문의해주세요"
            else -> ""
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setTitle("⚠️ 알림")
            setMessage(
                // 메시지 정하기
                if (case == 0) {
                    val spannableMessage = SpannableString(message)
                    val startIndex = spannableMessage.indexOf("\n") + 1
                    spannableMessage.setSpan(
                        RelativeSizeSpan(0.8f), // 크기 14 (기본 크기의 80%)
                        startIndex,
                        spannableMessage.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannableMessage.setSpan(
                        ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.subColor700)), // 색상 설정 (색상 코드는 실제 사용하는 값으로 변경)
                        startIndex,
                        spannableMessage.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannableMessage
                } else {
                    message
                }
            )
            setPositiveButton("확인") { _, _ -> }
            create()
        }.show()
        when (case) {
            1 -> {
                disabledLogin(retryAfter)
            }
            // 계정이 잠겼을 때 문의하기
            2 -> {
                disabledLogin(-1)
            }
        }
    }

    private fun disabledLogin(retryAfter: Int) {
        binding.btnLDLogin.isEnabled = false
        binding.btnLDLogin.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400))
        binding.tvLDAlert.visibility = View.VISIBLE
        if (retryAfter != -1) {
            object : CountDownTimer((retryAfter * 1000).toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val remainingSeconds = millisUntilFinished / 1000
                    val minutes = remainingSeconds / 60
                    val seconds = remainingSeconds % 60

                    binding.tvLDAlert.text = "반복적인 로그인 시도로 해당 계정 로그인이 제한됩니다.\n남은시간: ${minutes}분 ${seconds}초"
                }
                override fun onFinish() {
                    enabledLogin()
                }
            }.start()
        }

    }
    private fun enabledLogin() {
        binding.btnLDLogin.isEnabled = true
        binding.btnLDLogin.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
        binding.tvLDAlert.visibility = View.GONE
    }
}