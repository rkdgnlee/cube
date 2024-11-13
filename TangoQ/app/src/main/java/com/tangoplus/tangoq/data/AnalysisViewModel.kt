package com.tangoplus.tangoq.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONArray

class AnalysisViewModel : ViewModel() {

    // ------# 비교할 measure 담을 공간 #------
    val leftMeasurement = MutableLiveData<MeasureVO>()
    val rightMeasurement = MutableLiveData<MeasureVO>()

    // ------# risk part 선택 -> dialog #------
    var relatedAnalyzes = mutableListOf<AnalysisVO>()
    var leftAnalyzes : MutableList<MutableList<AnalysisVO>>? = null
    var rightAnalyzes :MutableList<MutableList<AnalysisVO>>? = null
    // ------# risk part 선택 -> 특정 자세 선택 -> dialog #------
    var selectedSeq = 0
    var selectedPart = ""

    var dynamicJa = JSONArray()
}