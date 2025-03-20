package com.tangoplus.tangoq.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentSignIn1Binding
import com.tangoplus.tangoq.dialog.SignInDialogFragment
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
            // TODO 이곳에서 인증에 대한 거 처리
            parentDialog?.setonNextPage()
        }


        binding
    }
}