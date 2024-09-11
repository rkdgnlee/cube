package com.tangoplus.tangoq.data

import android.annotation.SuppressLint
import android.icu.util.Measure
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONObject

class MeasureViewModel : ViewModel() {
    val parts = MutableLiveData(mutableListOf<MeasureVO>())
    val feedbackParts = MutableLiveData(mutableListOf<MeasureVO>())

    // MeasureSkeleton 담을 공간
    val jo = JSONObject()
    var dynamicJo : JSONObject? = null
    var currentMeasureData : MutableList<AnalysisVO> = mutableListOf() //자세별 json float 데이터 넣기

    // MeasureDetail 담을 공간
    var selectedMeasure : MeasureVO? = null

    init {
        selectedMeasure = MeasureVO("", "", null, mutableListOf(), mutableListOf())
    }
}