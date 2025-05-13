package com.tangoplus.tangoq.listener

interface OnHistoryDeleteListener {
    fun onHistoryDelete(history: Pair<Int, String>)

}