package com.tangoplus.tangoq.listener

import java.time.LocalDateTime

interface OnAlarmDeleteListener {
    fun onAlarmDelete(timeStamp: Long?)
}