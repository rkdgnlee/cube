package com.example.mhg

import com.example.mhg.VO.HomeRVBeginnerDataClass


interface AddItemTouchListener {
    fun onItemMove(from: Int, to: Int)
    fun onItemSwiped(position: Int)
}