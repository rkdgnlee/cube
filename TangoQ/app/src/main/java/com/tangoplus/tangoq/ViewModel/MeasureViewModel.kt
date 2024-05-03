package com.tangoplus.tangoq.ViewModel

import android.widget.ImageView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MeasureViewModel : ViewModel() {
    val parts = MutableLiveData(mutableListOf<Pair<String,String>>())

    init {
        parts.value = mutableListOf()
    }

    fun addPart(part: Pair<String, String>) {
        parts.value = null // TODO 일단 중복제거는 해야하고, 인체모형에서 클릭시 구현하게끔 결정났을 때 시작.
        val updatedPart = parts.value?.toMutableList()
        updatedPart?.add(part)
        parts.value = updatedPart
    }
    fun deletePart(part: Pair<String, String>) {
        val updatedPart = parts.value?.toMutableList()
        updatedPart?.remove(part)
        parts.value = updatedPart
    }
}