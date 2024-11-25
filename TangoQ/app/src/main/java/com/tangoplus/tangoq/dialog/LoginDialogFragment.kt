package com.tangoplus.tangoq.dialog

import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
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

        binding.ibtnLDIdClear.setOnClickListener { binding.etLDId.text.clear() }
        binding.ibtnLDPwClear.setOnClickListener { binding.etLDPw.text.clear() }

        binding.btnLDLogin.setOnClickListener {
            val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)

            if (viewModel.idPwCondition.value == true) {
                val jsonObject = JSONObject()
                jsonObject.put("user_id", viewModel.id.value)
                jsonObject.put("password", viewModel.pw.value)
                val dialog = LoadingDialogFragment.newInstance("로그인")
                dialog.show(requireActivity().supportFragmentManager, "LoadingDialogFragment")

                lifecycleScope.launch {
                    getUserIdentifyJson(getString(R.string.API_user), jsonObject, requireContext()) { jo ->

                        if (jo?.getString("message") == "invalid login data") { // 기존에 정보가 있을 경우 - 로그인 성공
                            requireActivity().runOnUiThread {
                                dialog.dismiss()
                                MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                                    setTitle("⚠️ 알림")
                                    setMessage("아이디 또는 비밀번호가 올바르지 않습니다.")
                                    setPositiveButton("확인") { _, _ ->
                                        binding.etLDPw.text.clear()
                                    }
                                    create()
                                }.show()
                            }
                        } else {
                            requireActivity().runOnUiThread {

                                binding.btnLDLogin.isEnabled = false
                                viewModel.User.value = null

                                // ------! 싱글턴 + 암호화 저장 시작 !------
                                if (jo != null ) {
                                    if (jo.optInt("weight") == 0 && jo.optInt("height") == 0 && jo.optString("gender") == "1") {
                                        val dialog2 = SetupDialogFragment()
                                        dialog2.show(requireActivity().supportFragmentManager, "SetupDialogFragment")
                                    } else {
                                        NetworkUser.storeUserInSingleton(requireContext(), jo)
                                        createKey(getString(R.string.SECURE_KEY_ALIAS))
//                                        saveEncryptedToken(requireContext(), getString(R.string.SECURE_KEY_ALIAS), encryptToken(getString(R.string.SECURE_KEY_ALIAS), jsonObject))
                                        lifecycleScope.launch {
                                            val userUUID = Singleton_t_user.getInstance(requireContext()).jsonObject?.optString("user_uuid") ?: ""
                                            val userInfoSn =  Singleton_t_user.getInstance(requireContext()).jsonObject?.optString("sn")?.toInt() ?: -1

                                            dialog.dismiss()
                                            ssm.getMeasures(userUUID, userInfoSn, CoroutineScope(Dispatchers.IO)) {
                                                Log.v("자체로그인완료", "${Singleton_t_user.getInstance(requireContext()).jsonObject}")
                                                Handler(Looper.getMainLooper()).postDelayed({
                                                    val intent = Intent(requireContext(), MainActivity::class.java)
                                                    startActivity(intent)
                                                    requireActivity().finishAffinity()
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

}