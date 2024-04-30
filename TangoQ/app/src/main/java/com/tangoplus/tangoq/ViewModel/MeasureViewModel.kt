package com.tangoplus.tangoq.ViewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MeasureViewModel : ViewModel() {
    val parts = MutableLiveData(mutableListOf<String>())
}