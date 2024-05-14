package com.tangoplus.tangoq.dialog

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.`object`.NetworkUserService
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.SignInViewModel
import com.tangoplus.tangoq.databinding.FragmentLoginDialogBinding
import java.util.regex.Pattern

class LoginDialogFragment : DialogFragment() {
    lateinit var binding: FragmentLoginDialogBinding
    val viewModel : SignInViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // ------! 로그인 시작 !------
        val idPattern = "^[a-zA-Z0-9]{4,16}$" // 영문, 숫자 4 ~ 16자 패턴
        val IdPattern = Pattern.compile(idPattern)
        val pwPattern = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[$@$!%*#?&.^])[A-Za-z[0-9]$@$!%*#?&.^]{8,20}$" // 영문, 특수문자, 숫자 8 ~ 20자 패턴
        val Pwpattern = Pattern.compile(pwPattern)
        binding.etLiId.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.id = s.toString()
                viewModel.currentidCon.value = IdPattern.matcher(binding.etLiId.text.toString()).find()
                Log.v("아이디비밀번호", "${viewModel.currentidCon.value} ,${viewModel.idPwCondition.value}")

            }
        })
        binding.etLiPw.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.pw = s.toString()
                viewModel.currentPwCon.value = Pwpattern.matcher(binding.etLiPw.text.toString()).find()
                Log.v("아이디비밀번호", "${viewModel.currentPwCon.value} ,${viewModel.idPwCondition.value}")
            }
        })

//        viewModel.idPwCondition.observe(viewLifecycleOwner) {
//            viewModel.idPwCondition.value = if (viewModel.currentidCon.value == true && (viewModel.currentPwCon.value == true)) true else false
////            Log.v("idpwcondition", "${viewModel.currentidCon.value}, pw: ${viewModel.currentPwCon.value}, both: ${viewModel.idPwCondition.value}")
//        }
        binding.btnLiLogin.setOnClickListener {
            if (viewModel.idPwCondition.value == true) {
                NetworkUserService.getUserIdentifyJson(getString(R.string.IP_ADDRESS_t_user), viewModel.id, viewModel.pw) { jsonObj ->
                    Log.v("json", "${jsonObj?.getInt("status")}")
                    if (jsonObj?.getInt("status") == 201) { // 기존에 정보가 있을 경우 - 로그인 성공
                        requireActivity().runOnUiThread {
                            viewModel.User.value = null
                            NetworkUserService.StoreUserInSingleton(requireContext(), jsonObj)
                            Log.e("로그인>싱글톤", "${Singleton_t_user.getInstance(requireContext()).jsonObject}")
                            val intent = Intent(requireContext(), MainActivity::class.java)
                            startActivity(intent)
                            requireActivity().finishAffinity()
                        }
                    } else {
                        requireActivity().runOnUiThread {
                            MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                                setTitle("⚠️ 알림")
                                setMessage("아이디 또는 비밀번호가 올바르지 않습니다.")
                                setPositiveButton("확인") { dialog, _ ->
                                    binding.etLiPw.text.clear()
                                }
                                create()
                            }.show()
                        }
                    }
                }
            }
        } // ------! 로그인 끝 !------

        // ------! 비밀번호 및 아이디 찾기 시작 !------

        // ------! 비밀번호 및 아이디 찾기 시작 !------
    }


    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

}