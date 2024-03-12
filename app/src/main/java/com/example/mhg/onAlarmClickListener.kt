package com.example.mhg

import androidx.fragment.app.Fragment

interface OnAlarmClickListener  {
    fun onAlarmClick(fragmentId: String)
}


interface onPickDetailClickListener {
    fun onPickClick(title: String)
}