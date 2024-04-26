package com.example.mhg

import com.example.mhg.VO.ExerciseVO

interface BasketItemTouchListener {
    fun onBasketItemQuantityChanged(descriptionId: String, newQuantity: Int)
}