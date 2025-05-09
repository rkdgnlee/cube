package com.tangoplus.tangoq.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.graphics.toColorInt
import com.tangoplus.tangoq.R

class BadgeButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {

    private val badgeDangerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FF5449".toColorInt()
        style = Paint.Style.FILL
    }

    private val badgeWarningPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#FF971D".toColorInt()
        style = Paint.Style.FILL
    }

    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 24f
    }
    private var currentBadgePaint = badgeDangerPaint

    var badgeCount: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    var badgeRadius = 8f
        set(value) {
            field = value
            invalidate()
        }

    var showBadge = false
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (showBadge) {
            val textBounds = Rect()
            paint.getTextBounds(text.toString(), 0, text.length, textBounds)

            val textWidth = textBounds.width()
            val textHeight = textBounds.height()

            // 텍스트의 위치 계산 (버튼 중앙)
            val textX = width / 2f
            val textY = height / 2f + textHeight / 2f

            // 뱃지를 텍스트 오른쪽 상단에 위치시킴
            val badgeX = textX + textWidth / 2f + 10
            val badgeY = textY - textHeight

            // 뱃지 그리기
            canvas.drawCircle(badgeX, badgeY, badgeRadius, currentBadgePaint)
        }
    }

    // 명시적인 상태 설정을 위한 추가 함수들
    fun setDangerState() {
        currentBadgePaint = badgeDangerPaint
        invalidate()
    }

    fun setWarningState() {
        currentBadgePaint = badgeWarningPaint
        invalidate()
    }


}