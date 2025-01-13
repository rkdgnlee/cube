package com.tangoplus.tangoq.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.google.android.exoplayer2.ui.PlayerControlView

class PlayerControlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr : Int = 0
) : PlayerControlView(context, attrs, defStyleAttr) {

    private var isProgressBarTouchable = false

    init {
        setProgressbarTouchable(false)
    }
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event)

    }

    fun setProgressbarTouchable(touchable: Boolean) {
        isProgressBarTouchable = touchable
    }

    fun isProgressbarTouchable() : Boolean {
        return isProgressBarTouchable
    }
}