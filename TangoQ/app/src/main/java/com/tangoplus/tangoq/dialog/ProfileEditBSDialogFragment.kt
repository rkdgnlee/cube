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
        private const val ARG_PEBSD = "ProfileEdit"
        fun newInstance(title: String) : ProfileEditBSDialogFragment {

            val fragment = ProfileEditBSDialogFragment()
            val args = Bundle()
            args.putString(ARG_PEBSD, title)
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

        arg = arguments?.getString(ARG_PEBSD).toString()
        Log.v("arg", arg)




        when (arg) {
            "몸무게" -> {
                binding.tvPEBSDTitle.text = "몸무게 재설정"
                binding.etPEBSD.apply {
                    inputType = InputType.TYPE_CLASS_NUMBER
                    setText(viewModel.User.value?.optString("user_weight"))
                    filters = arrayOf(InputFilter.LengthFilter(3))
                }
            }
            "신장" -> {
                binding.tvPEBSDTitle.text = "신장 재설정"
                binding.etPEBSD.apply {
                    inputType = InputType.TYPE_CLASS_NUMBER
                    setText(viewModel.User.value?.optString("user_height"))
                    filters = arrayOf(InputFilter.LengthFilter(3))
                }
            }
            "이메일" -> {
                binding.tvPEBSDTitle.text = "이메일 재설정"
                binding.etPEBSD.apply {
                    inputType = InputType.TYPE_CLASS_TEXT
                    setText(viewModel.User.value?.optString("user_email"))
                    filters = arrayOf(InputFilter.LengthFilter(25))
                }

            }
            else -> "미설정"
        }
        // ------# 키보드 올라오기 #------
        binding.etPEBSD.requestFocus()
        binding.etPEBSD.postDelayed({
            val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etPEBSD, InputMethodManager.SHOW_IMPLICIT)
        }, 250)
        val imm = context?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
        imm!!.hideSoftInputFromWindow(view.windowToken, 0)


        binding.btnPEBSDFinish.setOnClickListener {
            when (arg) {
                "몸무게" -> {
                    viewModel.User.value?.put("user_weight", binding.etPEBSD.text.toString())
                }
                "신장" -> {
                    viewModel.User.value?.put("user_height", binding.etPEBSD.text.toString())
                }
                "이메일" -> {
                    viewModel.User.value?.put("user_email", binding.etPEBSD.text.toString())
                }
            }
            dismiss()
        }


    }
}