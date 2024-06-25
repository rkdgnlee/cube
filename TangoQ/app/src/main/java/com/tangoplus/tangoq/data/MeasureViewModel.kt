package com.tangoplus.tangoq.data

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MeasureViewModel : ViewModel() {
    val parts = MutableLiveData(mutableListOf<Triple<String,String, Boolean>>()) // drawable, 부위 이름, 체크여부
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
    fun addPart(part: Triple<String, String, Boolean>) {
        val updatedPart = parts.value?.toMutableList() ?: mutableListOf()
        if (!updatedPart.contains(part)) {
            updatedPart.add(part)
        }
        parts.value = updatedPart
    }

    fun deletePart(part: Triple<String, String, Boolean>) {
        val updatedPart = parts.value?.toMutableList() ?: mutableListOf()
        updatedPart.removeAll { it.first == part.first }
        parts.value = updatedPart
    }

}