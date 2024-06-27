package com.tangoplus.tangoq.data

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MeasureViewModel : ViewModel() {
    val parts = MutableLiveData(mutableListOf<Triple<String,String, Boolean>>()) // drawable, 부위 이름, 체크여부
    val steps = MutableLiveData(mutableListOf<Long>())
    val totalSteps = MutableLiveData<String>()
    val calory = MutableLiveData<String>()

    val feedbackparts = MutableLiveData(mutableListOf<Triple<String,String, Boolean>>())

    init {
        parts.value = mutableListOf()
        steps.value = mutableListOf()
        totalSteps.value = ""
        calory.value = ""
        feedbackparts.value = mutableListOf()
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

    @SuppressLint("SuspiciousIndentation")
    fun addFeedbackPart(part: Triple<String, String, Boolean>) {
        val updatedPart = feedbackparts.value?.toMutableList() ?: mutableListOf()
        if (!updatedPart.contains(part)) {
            updatedPart.add(part)
        }
        feedbackparts.value = updatedPart
    }

    fun deleteFeedbackPart(part: Triple<String, String, Boolean>) {
        val updatedPart = feedbackparts.value?.toMutableList() ?: mutableListOf()
        updatedPart.removeAll { it.first == part.first }
        feedbackparts.value = updatedPart
    }

}