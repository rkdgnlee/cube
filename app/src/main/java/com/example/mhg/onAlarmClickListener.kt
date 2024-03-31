package com.example.mhg

import androidx.fragment.app.Fragment
import com.example.mhg.VO.PickItemVO

interface OnAlarmClickListener  {
    fun onAlarmClick(fragmentId: String)
}


interface onPickDetailClickListener {

    fun onPickClick(title: String)


}