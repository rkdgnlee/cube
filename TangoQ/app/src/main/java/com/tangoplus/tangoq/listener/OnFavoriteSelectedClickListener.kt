package com.tangoplus.tangoq.listener

import com.tangoplus.tangoq.data.FavoriteVO

interface OnFavoriteSelectedClickListener {
    fun onFavoriteSelected(favoriteVO: FavoriteVO)
}