package com.tangoplus.tangoq.listener

interface OnPartCheckListener {
    fun onPartCheck(part: Triple<String,String, Boolean>)
}