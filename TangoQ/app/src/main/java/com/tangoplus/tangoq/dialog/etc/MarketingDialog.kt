package com.tangoplus.tangoq.dialog.etc

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.DialogMarketingBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MarketingDialog(context: Context ,val isAgree: Boolean) : Dialog(context) {
    lateinit var binding : DialogMarketingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogMarketingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // -------# 상위 부모 frameLayout의 layout 설정 #------
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // -------# 버튼 동작 관리 #------
        binding.tvMkConfirm.setOnClickListener{ dismiss() }

        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
        val message = SpannableString("전송자: TangoPlus (탱고플러스)" + "\n수신동의 날짜: ${today.format(formatter)}\n처리내용: 수신 ${if (isAgree) "동의" else "거부"} 처리 완료")
        val accentIndex = message.indexOf("처리내용")

        when (isAgree) {
            true -> message.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.secondaryColor)), accentIndex + 5, message.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            false -> message.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.deleteColor)), accentIndex + 5, message.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        binding.tvMkContent.text = message
    }
}