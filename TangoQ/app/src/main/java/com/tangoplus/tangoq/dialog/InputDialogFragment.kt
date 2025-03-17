package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.api.NetworkUser.verifyPW
import com.tangoplus.tangoq.databinding.FragmentInputDialogBinding
import com.tangoplus.tangoq.fragment.ExtendedFunctions.dialogFragmentResize
import com.tangoplus.tangoq.listener.OnSingleClickListener
import com.tangoplus.tangoq.mediapipe.MathHelpers.isTablet
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.regex.Pattern

class InputDialogFragment : DialogFragment() {
    private lateinit var binding: FragmentInputDialogBinding
    private val svm : SignInViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentInputDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // api35이상 화면 크기 조절
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 상태 표시줄 높이만큼 상단 패딩 적용
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val pwPattern = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[$@$!%*#?&^])[A-Za-z[0-9]$@$!%*#?&^]{8,20}$" // 영문, 특수문자, 숫자 8 ~ 20자 패턴
        val pwPatternCheck = Pattern.compile(pwPattern)
        disabledButton()
        visibleKeyboard()

        binding.etIDPw.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                svm.currentPwCon.value = pwPatternCheck.matcher(binding.etIDPw.text.toString()).find()
                if (svm.currentPwCon.value == true) enabledButton() else disabledButton()
            }
        })

        binding.btnIDConfirm.setOnSingleClickListener {
            val jo = JSONObject().apply { put("password", binding.etIDPw.text.toString()) }
            lifecycleScope.launch {
                verifyPW(requireContext(), getString(R.string.API_user), jo) { status ->
                    when (status) {
                        200 -> {
                            val dialog = ProfileEditChangeDialogFragment.newInstance("비밀번호", "")
                            dialog.show(requireActivity().supportFragmentManager, "ProfileEditBSDialogFragment")
                            dismiss()
                        }
                        else -> {
                            Toast.makeText(requireContext(), "적절하지 않은 비밀번호입니다\n다시 시도해주세요", Toast.LENGTH_SHORT).show()
                            binding.etIDPw.setText("")
                            visibleKeyboard()
                        }
                    }
                }
            }
        }
    }

    private fun View.setOnSingleClickListener(action: (v: View) -> Unit) {
        val listener = View.OnClickListener {action(it) }
        setOnClickListener(OnSingleClickListener(listener))
    }

    private fun enabledButton() {
        binding.btnIDConfirm.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
        binding.btnIDConfirm.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white)))
        binding.btnIDConfirm.isEnabled =  true
    }
    private fun disabledButton() {
        binding.btnIDConfirm.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor150))
        binding.btnIDConfirm.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400)))
        binding.btnIDConfirm.isEnabled = false
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setDimAmount(0.7f)
        dialog?.window?.setBackgroundDrawable(resources.getDrawable(R.drawable.bckgnd_rectangle_20, null))
//        dialog?.setCancelable(false)
        if (isTablet(requireContext())) {
            dialogFragmentResize(requireContext(), this, width =  0.7f, height = 0.25f)
        } else {
            dialogFragmentResize(requireContext(), this, width =  0.9f, height = 0.35f)
        }
    }

    private fun visibleKeyboard() {
        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        Handler(Looper.getMainLooper()).postDelayed({
            binding.etIDPw.requestFocus()
            binding.etIDPw.postDelayed({
                imm.showSoftInput(binding.etIDPw, InputMethodManager.SHOW_IMPLICIT)
            }, 0)
            imm.hideSoftInputFromWindow(view?.windowToken, 0)
        }, 250)
    }

}