package com.tangoplus.tangoq.fragment

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentSignIn1Binding
import com.tangoplus.tangoq.dialog.SignInDialogFragment
import com.tangoplus.tangoq.dialog.WebViewDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.viewmodel.SignInViewModel

class SignIn1Fragment : Fragment() {
    private lateinit var binding : FragmentSignIn1Binding
    val svm : SignInViewModel by activityViewModels()
    private var parentDialog: SignInDialogFragment? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSignIn1Binding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parentDialog  = (requireParentFragment() as SignInDialogFragment)
        // 기본 셋팅
        binding.clName.visibility = View.GONE

        binding.btnAuthSend.setOnSingleClickListener {
            val passDialog = WebViewDialogFragment()
            passDialog.show(requireActivity().supportFragmentManager, "WebViewDialogFragment")
        }
//        binding.etMobile.setOnSingleClickListener {
//            storeUserValue()
//        }

        svm.passName.observe(viewLifecycleOwner) {
            if (it != "") {
                binding.clName.visibility = View.VISIBLE
                binding.etName.setText(it)
                binding.etName.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
            } else {
                binding.etName.text = null
                binding.etName.backgroundTintList = null
            }
        }
        svm.passMobile.observe(viewLifecycleOwner) {
            if (it != "") {
                binding.etMobile.setText(it)
                binding.etMobile.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.subColor100, null))
            } else {
                binding.etMobile.text = null
                binding.etMobile.backgroundTintList = null
            }
        }
        svm.passAuthCondition.observe(viewLifecycleOwner) {
            if (it) {
                storeUserValue()
            } else {
                clearUserValue()
            }
        }
    }

    private fun storeUserValue() {
        svm.User.value?.put("mobile", svm.passMobile.value)
        svm.User.value?.put("user_name", svm.passName.value)
        Handler(Looper.getMainLooper()).postDelayed({
            parentDialog?.setonNextPage()
        }, 600)
    }

    private fun clearUserValue() {
        svm.User.value?.put("mobile", "")
        svm.User.value?.put("user_name", "")
    }
}