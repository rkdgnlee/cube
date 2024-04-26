package com.tangoplus.tangoq.Listener

interface BasketItemTouchListener {
    fun onBasketItemQuantityChanged(descriptionId: String, newQuantity: Int)
}