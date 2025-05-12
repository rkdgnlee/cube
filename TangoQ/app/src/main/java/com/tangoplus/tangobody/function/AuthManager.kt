package com.tangoplus.tangobody.function

import android.annotation.SuppressLint
import android.content.Context
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangoplus.tangobody.R
import com.tangoplus.tangobody.viewmodel.SignInViewModel
import java.util.regex.Pattern

object AuthManager {
    fun setVerifyCountDown(tv: TextView, retryAfter: Int, setCountDownTimer : (CountDownTimer) -> Unit) {
        if (retryAfter != -1) {
            val newCountDownTimer = object : CountDownTimer((retryAfter * 1000).toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val remainingSeconds = millisUntilFinished / 1000
                    val minutes = remainingSeconds / 60
                    val seconds = remainingSeconds % 60
                    tv.visibility = View.VISIBLE
                    tv.text = "남은 시간: ${minutes}분 ${seconds}초"
                }
                override fun onFinish() {
                    tv.visibility = View.INVISIBLE
                }
            }.start()

            setCountDownTimer(newCountDownTimer)
        }
    }

    fun namePatternCheck(svm: SignInViewModel, input: EditText, tv: TextView, context: Context) : TextWatcher {
        val nameRegex = "^[가-힣]{2,5}$|^[a-zA-Z]{2,20}$"
        val namePatternCheck = Pattern.compile(nameRegex)
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                svm.nameCondition.value = namePatternCheck.matcher(input.text.toString()).find()
                if (svm.nameCondition.value == true) {
                    tv.setTextColor(context.resources.getColor(R.color.successColor, null))
                    tv.text = "올바른 형식입니다"
                    svm.passName.value = input.text.toString()
                } else {
                    tv.setTextColor(context.resources.getColor(R.color.deleteColor, null))
                    tv.text = "올바른 이름을 입력해주세요"
                    svm.passName.value = ""
                }
            }
        }
    }

    fun pwPatternCheck(svm: SignInViewModel, input: EditText, tv: TextView, isRepeat: Boolean) :TextWatcher {
        val pwPattern = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[$@$!%*#?&^])[A-Za-z[0-9]$@$!%*#?&^]{8,20}$" // 영문, 특수문자, 숫자 8 ~ 20자 패턴
        val pwPatternCheck = Pattern.compile(pwPattern)
        return object : TextWatcher {
            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(s: Editable?) {
                if (s.toString().isNotEmpty()) {
                    when (isRepeat) {
                        false -> {
                            svm.pwCondition.value = pwPatternCheck.matcher(input.text.toString()).find()
                            if (svm.pwCondition.value == true) {
                                tv.setTextColor(tv.resources.getColor(R.color.successColor, null))
                                tv.text = "사용 가능합니다"
                            } else {
                                tv.setTextColor(tv.resources.getColor(R.color.deleteColor, null))
                                tv.text = "영문, 숫자, 특수문자( ! @ # $ % ^ & * ?)를 모두 포함해서 8~20자리를 입력해주세요"
                            }
                        }
                        true -> {
                            svm.pwCompare.value = pwPatternCheck.matcher(input.text.toString()).find()
                            if (svm.pwCompare.value == true) {
                                tv.setTextColor(tv.resources.getColor(R.color.successColor, null))
                                tv.text = "일치합니다"
                            } else {
                                tv.setTextColor(tv.resources.getColor(R.color.deleteColor, null))
                                tv.text = "올바르지 않습니다"
                            }
                        }
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
    }

    fun setRetryAuthMessage(context: Context, svm: SignInViewModel, tv: TextView, tvCountDown: TextView, madb: MaterialAlertDialogBuilder) {
        val fullText = tv.text
        svm.countDownTimer?.cancel()
        setVerifyCountDown(tvCountDown, 300) {
            svm.countDownTimer = it
        }
        val spnbString = SpannableString(fullText)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                madb.show()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = context.resources.getColor(R.color.thirdColor, null)
            }
        }
        val startIndex = fullText.indexOf("재전송")
        val endIndex = startIndex + "재전송".length
        spnbString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tv.text = spnbString
        tv.apply {
            text = spnbString
            movementMethod = LinkMovementMethod.getInstance()
            isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed({
                isEnabled = true
            } ,5000)
        }
    }
    fun maskedProfileData(resultString: String) : String {
        val atIndex = resultString.indexOf('@')
        if (atIndex == -1) {
            val except010String = resultString.substring(3, resultString.length).replace("-", "")
            val maskedString = except010String.mapIndexed{ index, char ->
                when {
                    index % 6 == 0 || index % 6 == 2 || index % 6 == 3 -> char
                    index % 6 == 1 || index % 6 == 4 || index % 6 == 5 -> '*'
                    else -> char
                }
            }.joinToString("")

            return maskedString
        }  // @ 기호가 없으면 원본 그대로 반환

        val username = resultString.substring(0, atIndex)
        val domain = resultString.substring(atIndex)
        val maskedUsername = username.mapIndexed { index, char ->

            when {
                index % 6 == 0 || index % 6 == 2 || index % 6 == 3 -> char
                index % 6 == 1 || index % 6 == 4 || index % 6 == 5 -> '*'
                else -> char
            }
        }.joinToString("")

        val maskedDomain = domain.mapIndexed { index, char ->
            when {
                index % 6 == 0 || index % 6 == 2 || index % 6 == 3 -> char
                index % 6 == 1 || index % 6 == 4 || index % 6 == 5 -> '*'
                else -> char
            }
        }.joinToString("")

        return maskedUsername + maskedDomain
    }
}