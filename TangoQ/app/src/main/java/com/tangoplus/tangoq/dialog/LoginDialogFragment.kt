package com.tangoplus.tangoq.dialog

import android.app.Activity
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.`object`.NetworkUser
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import com.tangoplus.tangoq.databinding.FragmentLoginDialogBinding
import com.tangoplus.tangoq.function.SecurePreferencesManager.createKey
import com.tangoplus.tangoq.`object`.NetworkUser.getUserIdentifyJson
import com.tangoplus.tangoq.function.SaveSingletonManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.regex.Pattern

class LoginDialogFragment : DialogFragment() {
    lateinit var binding: FragmentLoginDialogBinding
    val viewModel : SignInViewModel by activityViewModels()
    private lateinit var ssm : SaveSingletonManager

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


        ssm = SaveSingletonManager(requireContext(), requireActivity())
        binding.etLDId.requestFocus()
        binding.etLDId.postDelayed({
            val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etLDId, InputMethodManager.SHOW_IMPLICIT)
        }, 250)
        val imm = context?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
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

//        viewModel.idPwCondition.observe(viewLifecycleOwner) {
//            viewModel.idPwCondition.value = if (viewModel.currentidCon.value == true && (viewModel.currentPwCon.value == true)) true else false
////            Log.v("idpwcondition", "${viewModel.currentidCon.value}, pw: ${viewModel.currentPwCon.value}, both: ${viewModel.idPwCondition.value}")
//        }

        // 응답과 관계없이 로그인 시도 5회 실패시 잠금 ( 3분간 )
        viewModel.loginTryCount.observe(viewLifecycleOwner) { count ->
            if (count >= 5) {
                disabledLogin()
                Handler(Looper.getMainLooper()).postDelayed({
                    enabledLogin()
                    viewModel.loginTryCount.value = 0
                }, 60000 * 3)
            }
            else {
                enabledLogin()
            }

        }
        binding.ibtnLDIdClear.setOnClickListener { binding.etLDId.text.clear() }
        binding.ibtnLDPwClear.setOnClickListener { binding.etLDPw.text.clear() }

        binding.btnLDLogin.setOnClickListener {
            val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)

            if (viewModel.idPwCondition.value == true) {
                viewModel.loginTryCount.value = viewModel.loginTryCount.value?.plus(1)
                val jsonObject = JSONObject()
                jsonObject.put("user_id", viewModel.id.value)
                jsonObject.put("password", viewModel.pw.value)
                val dialog = LoadingDialogFragment.newInstance("로그인")
                dialog.show(requireActivity().supportFragmentManager, "LoadingDialogFragment")

                lifecycleScope.launch {
                    getUserIdentifyJson(getString(R.string.API_user), jsonObject, requireContext()) { jo, code ->

                        if (jo?.getString("message") == "invalid login data") { // 기존에 정보가 있을 경우 - 로그인 성공
                            requireActivity().runOnUiThread {
                                dialog.dismiss()

                                makeMaterialDialog(
                                    when (code) {
                                        401 -> 0
                                        423 -> 1
                                        424 -> 2
                                        else -> 0
                                    }
                                )
                                binding.etLDPw.text.clear()
                            }
                        } else {
                            requireActivity().runOnUiThread {

                                binding.btnLDLogin.isEnabled = false
                                viewModel.User.value = null

                                // ------! 싱글턴 + 암호화 저장 시작 !------
                                if (jo != null ) {
                                    // ------# 최초 로그인 #------
                                    if (jo.optInt("weight") == 0 && jo.optInt("height") == 0 && jo.optString("gender") == "1") {
                                        val dialog2 = SetupDialogFragment()
                                        dialog2.show(requireActivity().supportFragmentManager, "SetupDialogFragment")
                                    } else {
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
                                                }, 500)
                                            }
                                        }

                                    }
                                }
                                // ------! 싱글턴 + 암호화 저장 끝 !------
                            }
                        } // else 끝
                    }
                }
            }
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

    private fun makeMaterialDialog(case: Int) {
        val message = when (case) {
            0 -> "비밀번호 또는 아이디가 올바르지 않습니다." + if (viewModel.loginTryCount.value!! > 2) "\n시도한 횟수: ${viewModel.loginTryCount.value}번" else ""
            1 -> "비밀번호가 5회 틀렸습니다. 1분 후 다시 시도해주세요."
            2 -> "비밀번호를 10회 틀려 계정이 잠겼습니다.\n고객센터로 문의해주세요"
            else -> ""
        }
        when (case) {
            1 -> {
                binding.btnLDLogin.isEnabled = false
                binding.tvLDAlert.visibility = View.VISIBLE
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.btnLDLogin.isEnabled = true
                    binding.tvLDAlert.visibility = View.GONE
                },  60000)
            }
            2 -> {
                binding.btnLDLogin.isEnabled = false
                binding.tvLDAlert.visibility = View.VISIBLE
            }
        }
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setTitle("⚠️ 알림")
            setMessage(message)
            setPositiveButton("확인") { _, _ -> }
            create()
        }.show()
    }

    private fun disabledLogin() {
        binding.btnLDLogin.isEnabled = false
        binding.btnLDLogin.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400))
        binding.tvLDAlert.visibility = View.VISIBLE
    }
    private fun enabledLogin() {
        binding.btnLDLogin.isEnabled = true
        binding.btnLDLogin.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
        binding.tvLDAlert.visibility = View.GONE
    }
}