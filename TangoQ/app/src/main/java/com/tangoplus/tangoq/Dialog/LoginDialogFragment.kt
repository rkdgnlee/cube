package com.tangoplus.tangoq.Dialog

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangoplus.tangoq.IntroActivity
import com.tangoplus.tangoq.Object.NetworkUserService
import com.tangoplus.tangoq.Object.Singleton_t_user
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.ViewModel.SignInViewModel
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
        val idCondition = IdPattern.matcher(binding.etLiId.text.toString()).find()
        val pwPattern = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[$@$!%*#?&.^])[A-Za-z[0-9]$@$!%*#?&.^]{8,20}$" // 영문, 특수문자, 숫자 8 ~ 20자 패턴
        val Pwpattern = Pattern.compile(pwPattern)
        val pwCondition = Pwpattern.matcher(binding.etLiPw.text.toString()).find()
        binding.etLiId.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.id = s.toString()
                viewModel.idPwCondition.value = (idCondition && pwCondition)

            }
        })
        binding.etLiPw.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.pw = s.toString()
                viewModel.idPwCondition.value = (idCondition && pwCondition)
            }
        })

        binding.btnLiLogin.setOnClickListener {
            if (viewModel.idPwCondition.value == true) {
                NetworkUserService.getUserIdentifyJson(getString(R.string.IP_ADDRESS_t_user), viewModel.id, viewModel.pw) { jsonObj ->
                    if (jsonObj?.getInt("status") != 201) {
                        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                            setTitle("⚠️ 알림")
                            setMessage("아이디 또는 비밀번호가 올바르지 않습니다.")
                            setNeutralButton("확인") { dialog, _ ->
                                dismiss()
                            }
                        }
                         // 기존에 정보가 있을 경우 - 로그인 성공
                    } else {
                        viewModel.User.value = null
                        NetworkUserService.StoreUserInSingleton(requireContext(), jsonObj)
                        Log.e("로그인>싱글톤", "${Singleton_t_user.getInstance(requireContext()).jsonObject}")
                        requireActivity().finishAffinity()
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