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