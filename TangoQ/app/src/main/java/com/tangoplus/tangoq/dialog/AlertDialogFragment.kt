package com.tangoplus.tangoq.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentAlertDialogBinding
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.function.SecurePreferencesManager.logout
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class AlertDialogFragment : DialogFragment() {
    private lateinit var binding : FragmentAlertDialogBinding
    private lateinit var case: String
    companion object {
        const val ARG_ALERT = "arg_alert"
        fun newInstance(case: String) : AlertDialogFragment {
            val fragment = AlertDialogFragment()
            val args = Bundle()
            args.putString(ARG_ALERT, case)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAlertDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        case = arguments?.getString(ARG_ALERT) ?: ""

        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
        when (case) {
            "agree", "disagree", "agreeMk1", "agreeMk2", "disagreeMk1", "disagreeMk2" -> {
                binding.tvMkTitle.text = when (case) {
                    "agreeMk1" -> "문자 메시지 정보 수신 동의"
                    "agreeMk2" -> "이메일 정보 수신 동의"
                    "agree" -> "마케팅 정보 수신 동의"
                    "disagreeMk1" -> "문자 메시지 정보 수신 거부"
                    "disagreeMk2" -> "이메일 정보 수신 거부"
                    "disagree" -> "마케팅 정보 수신 거부"
                    else -> "마케팅 정보 수신"

                }
                binding.tvMkConfirm.setOnSingleClickListener{ dismiss() }
                val message = SpannableString("전송자: TangoPlus (탱고플러스)" + "\n수신동의 날짜: ${today.format(formatter)}\n처리내용: 수신 ${if (case in listOf("agree", "agreeMk1", "agreeMk2")) "동의" else "거부"} 처리 완료")
                val accentIndex = message.indexOf("처리내용")
                when (case) {
                    "agree", "agreeMk1", "agreeMk2" -> message.setSpan(ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.secondaryColor)), accentIndex + 5, message.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    "disagree", "disagreeMk1", "disagreeMk2" -> message.setSpan(ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.deleteColor)), accentIndex + 5, message.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                binding.tvMkContent.text = message
            }
            "logout" -> {
                binding.tvMkTitle.textSize = 18f
                binding.tvMkContent.textSize = 18f
                val title = "다른 환경에서 로그인 시도"
                val message = "다른 환경에서 로그인하려고 합니다.\n현재 기기에서 로그아웃합니다."
                binding.tvMkTitle.text = title
                binding.tvMkContent.text = message

                logout(requireActivity(), 5)
                binding.tvMkConfirm.visibility = View.GONE
            }
        }
    }
//
    override fun onResume() {
        super.onResume()
        isCancelable = false

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}