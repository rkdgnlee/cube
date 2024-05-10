package com.tangoplus.tangoq.listener

interface BasketItemTouchListener {
    fun onBasketItemQuantityChanged(descriptionId: String, newQuantity: Int)
}