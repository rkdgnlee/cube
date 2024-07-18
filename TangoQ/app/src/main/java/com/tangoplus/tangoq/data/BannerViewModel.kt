package com.tangoplus.tangoq.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BannerViewModel : ViewModel() {
    val bannerList = arrayListOf<String>()
    val dailyProgress = MutableLiveData<Int>()
}