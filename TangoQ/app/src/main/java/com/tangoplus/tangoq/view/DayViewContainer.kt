package com.tangoplus.tangoq.view

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.kizitonwose.calendar.view.ViewContainer
import com.tangoplus.tangoq.databinding.CalendarDayLayoutBinding

class DayViewContainer(view: View) : ViewContainer(view){
    var date = CalendarDayLayoutBinding.bind(view).calendarDayText
    private var badgeView: View? = null
    fun removeBadge() {
        badgeView?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            badgeView = null
        }
    }

    fun addBadge() {
        // 이미 뱃지가 있다면 삭제
        removeBadge()

        // 컨테이너의 레이아웃 - 보통 FrameLayout 또는 ConstraintLayout
        val container = date.parent as ViewGroup

        // 뱃지 생성
        val badge = View(date.context)

        // 4dp 크기 설정
        val size = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            8f,
            date.resources.displayMetrics
        ).toInt()

        // 레이아웃 파라미터 설정
        val params = when (container) {
            is FrameLayout -> {
                FrameLayout.LayoutParams(size, size).apply {
                    // 텍스트뷰 기준으로 위치 설정
                    gravity = Gravity.TOP or Gravity.END
                    // 위치 조정 (약간 오른쪽 상단)
                    topMargin = date.top + dpToPx(0) // 상단 여백 조정
                    rightMargin = dpToPx(0) // 오른쪽 여백 조정
                }
            }
            is ConstraintLayout -> {
                ConstraintLayout.LayoutParams(size, size).apply {
                    // 텍스트뷰에 연결
                    topToTop = date.id
                    endToEnd = date.id
                    // 약간 오프셋
                    topMargin = dpToPx(0) // 상단 위치 조정
                    rightMargin = dpToPx(0) // 오른쪽 위치 조정
                }
            }
            else -> {
                ViewGroup.MarginLayoutParams(size, size)
            }
        }

        // 뱃지 스타일 설정
        badge.layoutParams = params

        // 원형 배지 만들기
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.OVAL
        shape.setColor(Color.parseColor("#36ABFF"))
        badge.background = shape

        // 컨테이너에 뱃지 추가
        container.addView(badge)

        // 참조 저장
        badgeView = badge
    }
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            date.resources.displayMetrics
        ).toInt()
    }

}