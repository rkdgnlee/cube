package com.tangoplus.tangoq.data

import android.annotation.SuppressLint
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONObject

class MeasureViewModel : ViewModel() {
    val parts = MutableLiveData(mutableListOf<MeasureVO>())
////    val steps = MutableLiveData(mutableListOf<Long>())
////    val totalSteps = MutableLiveData<String>()
////    val calory = MutableLiveData<String>()
////
    val feedbackParts = MutableLiveData(mutableListOf<MeasureVO>())
//
//    init {
//        parts.value = mutableListOf()
////        steps.value = mutableListOf()
////        totalSteps.value = ""
////        calory.value = ""
//        feedbackParts.value = mutableListOf()
//    }
//
//    @SuppressLint("SuspiciousIndentation")
//    fun addPart(part: MeasureVO) {
//        val updatedPart = parts.value?.toMutableList() ?: mutableListOf()
//        if (!updatedPart.contains(part)) {
//            updatedPart.add(part)
//        }
//        parts.value = updatedPart
//    }
//
//    fun deletePart(part: MeasureVO) {
//        val updatedPart = parts.value?.toMutableList() ?: mutableListOf()
//        updatedPart.removeAll { it.partName == part.partName }
//        parts.value = updatedPart
//    }
//
    @SuppressLint("SuspiciousIndentation")
    fun addFeedbackPart(part: MeasureVO) {
        val updatedPart = feedbackParts.value?.toMutableList() ?: mutableListOf()
        if (!updatedPart.contains(part)) {
            updatedPart.add(part)
        }
        feedbackParts.value = updatedPart
    }

    fun deleteFeedbackPart(part: MeasureVO) {
        val updatedPart = feedbackParts.value?.toMutableList() ?: mutableListOf()
        updatedPart.removeAll { it.name == part.name }
        feedbackParts.value = updatedPart
    }

}