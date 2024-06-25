package com.tangoplus.tangoq.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PlayViewModel : ViewModel() {
    val currentPlaybackPosition = MutableLiveData<Long>()
    val currentWindowIndex = MutableLiveData<Int>()

}