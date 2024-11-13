package com.tangoplus.tangoq.dialog

import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.SignInViewModel
import com.tangoplus.tangoq.data.UserViewModel
import com.tangoplus.tangoq.databinding.FragmentProfileEditBSDialogBinding

class ProfileEditBSDialogFragment : BottomSheetDialogFragment() {
    lateinit var binding: FragmentProfileEditBSDialogBinding
    lateinit var arg : String
    val viewModel : SignInViewModel by activityViewModels()

    companion object {
        private const val ARG_EDIT_BS_TITLE = "profileEditTitle"
        private const val ARG_EDIT_BS_VALUE = "profileEditValue"
        fun newInstance(title: String, value: String) : ProfileEditBSDialogFragment {

            val fragment = ProfileEditBSDialogFragment()
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
        binding = FragmentProfileEditBSDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.ibtnPEBSDExit.setOnClickListener { dismiss() }

        arg = arguments?.getString(ARG_EDIT_BS_TITLE).toString()

        val value = arguments?.getString(ARG_EDIT_BS_VALUE).toString()
        Log.v("arg", "$arg, $value")




        when (arg) {
            "몸무게" -> {
                binding.tvPEBSDTitle.text = "몸무게 재설정"
                binding.etPEBSD.apply {
                    inputType = InputType.TYPE_CLASS_NUMBER
                    setText(value)
                    filters = arrayOf(InputFilter.LengthFilter(3))
                }
            }
            "신장" -> {
                binding.tvPEBSDTitle.text = "신장 재설정"
                binding.etPEBSD.apply {
                    inputType = InputType.TYPE_CLASS_NUMBER
                    setText(value)
                    filters = arrayOf(InputFilter.LengthFilter(3))
                }
            }
            "이메일" -> {
                binding.tvPEBSDTitle.text = "이메일 재설정"
                binding.etPEBSD.apply {
                    inputType = InputType.TYPE_CLASS_TEXT
                    setText(value)
                    filters = arrayOf(InputFilter.LengthFilter(25))
                }

            }
            else -> "미설정"
        }
        // ------# 키보드 올라오기 #------
        binding.etPEBSD.requestFocus()
        binding.etPEBSD.postDelayed({
            context?.let { context ->
                val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.etPEBSD, InputMethodManager.SHOW_IMPLICIT)
            }
        }, 250)


        binding.btnPEBSDFinish.setOnClickListener {
            when (arg) {
                "몸무게" -> viewModel.setWeight.value = binding.etPEBSD.text.toString().toInt()
                "신장" -> viewModel.setHeight.value = binding.etPEBSD.text.toString().toInt()
                "이메일" -> viewModel.setEmail.value = binding.etPEBSD.text.toString()
            }
            Log.v("뷰모델에 잘담겼는지", "${viewModel.User.value}")
            dismiss()
        }


    }
}