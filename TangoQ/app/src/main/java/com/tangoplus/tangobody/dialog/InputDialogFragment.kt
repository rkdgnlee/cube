package com.tangoplus.tangobody.dialog

import android.content.Context.INPUT_METHOD_SERVICE
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.tangoplus.tangobody.R
import com.tangoplus.tangobody.api.NetworkUser.verifyPW
import com.tangoplus.tangobody.databinding.FragmentInputDialogBinding
import com.tangoplus.tangobody.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangobody.function.SecurePreferencesManager.encrypt
import com.tangoplus.tangobody.viewmodel.SignInViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.regex.Pattern

class InputDialogFragment : DialogFragment() {
    private lateinit var binding: FragmentInputDialogBinding
    private val svm : SignInViewModel by activityViewModels()
    private var case = -1
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentInputDialogBinding.inflate(inflater)
        return binding.root
    }
    companion object {
        const val ARG_INPUT = "input_arguments"
        fun newInstance(case: Int) : InputDialogFragment {
            val fragment = InputDialogFragment()
            val args = Bundle()
            args.putInt(ARG_INPUT, case)
            fragment.arguments = args
            return fragment
        }
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
        case = arguments?.getInt(ARG_INPUT) ?: -1
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
        binding.etIDPw.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                tryVerifyPW()
                true  // 이벤트 처리가 완료되었음을 반환
            } else {
                false // 다른 동작들은 그대로 유지
            }
        }

        binding.btnIDConfirm.setOnSingleClickListener {
            tryVerifyPW()
        }
    }

    private fun tryVerifyPW() {
        val encryptedPW = encrypt(binding.etIDPw.text.toString(), getString(R.string.secret_key), getString(R.string.secret_iv))
        val jo = JSONObject().apply { put("password_app", encryptedPW) }
        lifecycleScope.launch {
            verifyPW(requireContext(), getString(R.string.API_user), jo) { status ->
                when (status) {
                    200 -> {
                        Toast.makeText(requireContext(), "비밀번호 확인을 완료했습니다", Toast.LENGTH_LONG).show()
                        when (case) {
                            // 비밀번호 변경
                            1 -> {
                                dismiss()
                                val dialog = ProfileEditChangeDialogFragment.newInstance("비밀번호", "")
                                dialog.show(requireActivity().supportFragmentManager, "ProfileEditBSDialogFragment")

                            }
                            // 회원탈퇴
                            2 -> {
                                dismiss()
                                val withDrawalDialog = WithDrawalDialogFragment()
                                withDrawalDialog.show(requireActivity().supportFragmentManager, "withDrawalDialogFragment")
                            }
                            -1 -> {
                                dismiss()
                                Toast.makeText(requireContext(), "올바르지 않은 접근입니다.", Toast.LENGTH_SHORT).show()
                            }
                        }

                    }
                    else -> {
                        Toast.makeText(requireContext(), "적절하지 않은 비밀번호입니다\n다시 시도해주세요", Toast.LENGTH_SHORT).show()
                        binding.etIDPw.setText("")

                    }
                }
            }
        }
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

    override fun onResume() {
        super.onResume()
        dialog?.window?.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bckgnd_rectangle_20))
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