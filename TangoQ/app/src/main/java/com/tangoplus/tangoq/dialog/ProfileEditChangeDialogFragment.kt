package com.tangoplus.tangoq.dialog

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentProfileChangeDialogBinding
import com.tangoplus.tangoq.`object`.NetworkUser.fetchUserUPDATEJson
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import org.json.JSONObject
import java.util.regex.Pattern

class ProfileEditChangeDialogFragment : DialogFragment() {
    lateinit var binding: FragmentProfileChangeDialogBinding
    lateinit var arg : String
    val svm : SignInViewModel by activityViewModels()
    private lateinit var userJson : JSONObject
    companion object {
        private const val ARG_EDIT_BS_TITLE = "profileEditTitle"
        private const val ARG_EDIT_BS_VALUE = "profileEditValue"
        fun newInstance(title: String, value: String) : ProfileEditChangeDialogFragment {

            val fragment = ProfileEditChangeDialogFragment()
            val args = Bundle()
            args.putString(ARG_EDIT_BS_TITLE, title)
            args.putString(ARG_EDIT_BS_VALUE, value)
            fragment.arguments = args
            return fragment
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileChangeDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ------# init #-----
        arg = arguments?.getString(ARG_EDIT_BS_TITLE).toString()
        val value = arguments?.getString(ARG_EDIT_BS_VALUE).toString()
        Log.v("arg", "$arg, $value")
        userJson = Singleton_t_user.getInstance(requireContext()).jsonObject ?: JSONObject()

        binding.btnPCDFinish.setOnClickListener {
            val jo = JSONObject().apply {
                when (arg) {
                    "비밀번호" -> put("password", svm.pw.value)
                    "몸무게" -> put("weight", binding.etPCD1.text.toString())
                    "신장" -> put("height", binding.etPCD1.text.toString())
                    "이메일" -> put("email", binding.etPCD1.text.toString())
                }
            }
            fetchUserUPDATEJson(requireContext(), getString(R.string.API_user), jo.toString(), userJson.optInt("sn").toString()
            ) {
                dismiss()
            }
        }

        when (arg) {
            "비밀번호" -> {
                setUIVisibility(true)
                // ------# 비밀번호 재설정 case #------
                val pwPattern = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[$@$!%*#?&^])[A-Za-z[0-9]$@$!%*#?&^]{8,20}$" // 영문, 특수문자, 숫자 8 ~ 20자 패턴
                val pwPatternCheck = Pattern.compile(pwPattern)
                binding.etPCD2.addTextChangedListener(object : TextWatcher{
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        svm.pwCondition.value = pwPatternCheck.matcher(binding.etPCD2.text.toString()).find()
                        if (svm.pwCondition.value == true) {
                            binding.tvPCDPWCondition.setTextColor(binding.tvPCDPWCondition.resources.getColor(R.color.successColor, null))
                            binding.tvPCDPWCondition.text = "사용 가능합니다"
                        } else {
                            binding.tvPCDPWCondition.setTextColor(binding.tvPCDPWCondition.resources.getColor(R.color.mainColor, null))
                            binding.tvPCDPWCondition.text = "영문, 숫자, 특수문자( ! @ # $ % ^ & * ?)를 모두 포함해서\n8~20자리를 입력해주세요"
                        }
                    }
                })

                binding.etPCD3.addTextChangedListener(object : TextWatcher{
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        svm.pwCompare.value = (binding.etPCD2.text.toString() == binding.etPCD3.text.toString())
                        if (svm.pwCompare.value == true) {
                            binding.tvPCDPWVerifyCondition.setTextColor(binding.tvPCDPWVerifyCondition.resources.getColor(com.tangoplus.tangoq.R.color.successColor, null))
                            binding.tvPCDPWVerifyCondition.text = "일치합니다"
                        } else {
                            binding.tvPCDPWVerifyCondition.setTextColor(binding.tvPCDPWVerifyCondition.resources.getColor(com.tangoplus.tangoq.R.color.mainColor, null))
                            binding.tvPCDPWVerifyCondition.text = "올바르지 않습니다"
                        }
                    }
                })

                // ------# 비밀번호 확인 여부 체크 #------
                svm.pwBothTrue.observe(viewLifecycleOwner) {
                    binding.btnPCDFinish.isEnabled = it
                    if (it) {
                        enabledButton()
                        svm.pw.value = binding.etPCD2.text.toString()
                    } else {
                        disabledButton()
                        svm.pw.value = ""
                    }
                }
            }
            "몸무게" -> {
                setUIVisibility(false)
                binding.tvPCD.text = "몸무게 재설정"
                binding.etPCD1.apply {
                    inputType = InputType.TYPE_CLASS_NUMBER
                    setText(value)
                    filters = arrayOf(InputFilter.LengthFilter(3))
                }
            }
            "신장" -> {
                setUIVisibility(false)
                binding.tvPCD.text = "신장 재설정"
                binding.etPCD1.apply {
                    inputType = InputType.TYPE_CLASS_NUMBER
                    setText(value)
                    filters = arrayOf(InputFilter.LengthFilter(3))
                }
            }
            "이메일" -> {
                setUIVisibility(false)
                binding.tvPCD.text = "이메일 재설정"
                binding.etPCD1.apply {
                    inputType = InputType.TYPE_CLASS_TEXT
                    setText(value)
                    filters = arrayOf(InputFilter.LengthFilter(25))
                }
            }
            else -> "미설정"
        }
        // ------# 키보드 올라오기 #------
//        binding.etPEBSD.requestFocus()
//        binding.etPEBSD.postDelayed({
//            context?.let { context ->
//                val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
//                imm.showSoftInput(binding.etPEBSD, InputMethodManager.SHOW_IMPLICIT)
//            }
//        }, 250)


//        binding.btnPEBSDFinish.setOnClickListener {
//            when (arg) {
//                "몸무게" -> svm.setWeight.value = binding.etPEBSD.text.toString().toInt()
//                "신장" -> svm.setHeight.value = binding.etPEBSD.text.toString().toInt()
//                "이메일" -> svm.setEmail.value = binding.etPEBSD.text.toString()
//            }
//            Log.v("뷰모델에 잘담겼는지", "${svm.User.value}")
//            dismiss()
//        }
    }

    private fun enabledButton() {
        binding.btnPCDFinish.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
        binding.btnPCDFinish.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white)))
    }
    private fun disabledButton() {
        binding.btnPCDFinish.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor150))
        binding.btnPCDFinish.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor400)))
    }

    private fun setUIVisibility(isPassword: Boolean) {
        when (isPassword) {
            true -> {
                binding.etPCD1.visibility = View.VISIBLE
                binding.etPCD2.visibility = View.VISIBLE
                binding.etPCD3.visibility = View.VISIBLE
                binding.tvPCD1.visibility = View.VISIBLE
                binding.tvPCD2.visibility = View.VISIBLE
                binding.tvPCD3.visibility = View.VISIBLE
                binding.tvPCDPWCondition.visibility = View.VISIBLE
                binding.tvPCDPWVerifyCondition.visibility = View.VISIBLE
            }
            false -> {
                
                binding.etPCD1.visibility = View.VISIBLE
                binding.etPCD2.visibility = View.GONE
                binding.etPCD3.visibility = View.GONE
                binding.tvPCD1.visibility = View.GONE
                binding.tvPCD2.visibility = View.GONE
                binding.tvPCD3.visibility = View.GONE
                binding.tvPCDPWCondition.visibility = View.GONE
                binding.tvPCDPWVerifyCondition.visibility = View.GONE
            }
        }
    }
}