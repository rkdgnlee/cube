package com.tangoplus.tangoq.listener

import com.tangoplus.tangoq.data.MeasureVO

interface OnPartCheckListener {
    fun onPartCheck(part: MeasureVO)
}