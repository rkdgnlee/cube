package com.tangoplus.tangoq.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.shuhart.stepview.StepView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentSignInDialogBinding
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import com.tangoplus.tangoq.adapter.etc.SignInVPAdapter

class SignInDialogFragment : DialogFragment() {
    private lateinit var binding : FragmentSignInDialogBinding
    private lateinit var loadingDialog : LoadingDialogFragment
    private var verificationId = ""
    val svm : SignInViewModel by activityViewModels()
    private lateinit var auth : FirebaseAuth

    companion object {
        const val ARG_IS_SNS = "argumentsIsSns"
        fun newInstance(isSnsLogin: Boolean) : SignInDialogFragment {
            val fragment = SignInDialogFragment()
            val args = Bundle()
            args.putBoolean(ARG_IS_SNS, isSnsLogin)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSignInDialogBinding.inflate(inflater)
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
        svm.isSnsSignIn = arguments?.getBoolean(ARG_IS_SNS) ?: false

        binding.pvSignIn.progress = 30f
        binding.vpSignIn.adapter = SignInVPAdapter(this@SignInDialogFragment)
        binding.vpSignIn.isUserInputEnabled = false
        loadingDialog = LoadingDialogFragment.newInstance("회원가입전송")

        binding.ibtnSignInFinish.setOnClickListener {
            if (binding.vpSignIn.currentItem == 1) {
                setonPreviousPage()
            } else {
                MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                    setMessage("회원가입을 종료하시겠습니까?")
                    setPositiveButton("예", {_, _ ->
                        dialog?.dismiss()
                    })
                    setNegativeButton("아니오", {_, _ ->
                        dismiss()
                    })
                    show()
                }
            }
        }

        binding.svSignIn.state
            .animationType(StepView.ANIMATION_CIRCLE)
            .steps(object : ArrayList<String?>() {
                init {
                    add("이메일")
                    add("비밀번호")
                    add("휴대폰 인증")
                }
            })
            .stepsNumber(3)
            .animationDuration(resources.getInteger(android.R.integer.config_shortAnimTime))
            .commit()

    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    fun setonNextPage() {
        binding.vpSignIn.setCurrentItem(1, true)
    }
    fun setonPreviousPage() {
        binding.vpSignIn.setCurrentItem(0, true)
    }
}