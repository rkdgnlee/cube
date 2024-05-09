package com.tangoplus.tangoq.ViewModel

import android.annotation.SuppressLint
import android.widget.ImageView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MeasureViewModel : ViewModel() {
    val parts = MutableLiveData(mutableListOf<Pair<String,String>>())
    val steps = MutableLiveData(mutableListOf<Long>())
    val totalSteps = MutableLiveData<String>()
    val calory = MutableLiveData<String>()


    init {
        parts.value = mutableListOf()
        steps.value = mutableListOf()
        totalSteps.value = ""
        calory.value = ""

    }

    @SuppressLint("SuspiciousIndentation")
    fun addPart(part: Pair<String, String>) {
        val updatedPart = parts.value?.toMutableList()
        if (updatedPart?.contains(part) == false)
        updatedPart.add(part)
        parts.value = updatedPart
    }
    fun deletePart(part: Pair<String, String>) {
        val updatedPart = parts.value?.toMutableList()
        updatedPart?.removeAll { it.first == part.first }
        parts.value = updatedPart
    }
}