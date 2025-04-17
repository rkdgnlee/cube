package com.tangoplus.tangoq.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shuhart.stepview.StepView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentSignInDialogBinding
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import com.tangoplus.tangoq.adapter.etc.SignInVPAdapter
import androidx.core.graphics.drawable.toDrawable
import com.tangoplus.tangoq.db.MeasureDatabase
import com.tangoplus.tangoq.fragment.MainFragment

class SignInDialogFragment : DialogFragment() {
    private lateinit var binding : FragmentSignInDialogBinding
    private lateinit var loadingDialog : LoadingDialogFragment
    val svm : SignInViewModel by activityViewModels()

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

        binding.pvSignIn.progress = 30f
        binding.vpSignIn.adapter = SignInVPAdapter(this@SignInDialogFragment)
        binding.vpSignIn.isUserInputEnabled = false
        loadingDialog = LoadingDialogFragment.newInstance("회원가입전송")

        binding.ibtnSignInFinish.setOnClickListener {
            if (binding.vpSignIn.currentItem == 1) {
                setonPreviousPage()
            } else {
                showExitDialog()
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
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    fun setonNextPage() {
        binding.vpSignIn.setCurrentItem(1, true)
    }
    private fun setonPreviousPage() {
        binding.vpSignIn.setCurrentItem(0, true)
    }
    private fun showExitDialog() {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setMessage("회원가입을 종료하시겠습니까?")
            setPositiveButton("예") { _, _ ->
                dialog?.dismiss()
            }
            setNegativeButton("아니오") { _, _ -> }
            show()
        }
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCancelable(false)
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                showExitDialog()
                true // 이벤트 소비
            } else {
                false
            }
        }
        return dialog
    }
}