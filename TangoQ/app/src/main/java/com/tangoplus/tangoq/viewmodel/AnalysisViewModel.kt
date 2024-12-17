package com.tangoplus.tangoq.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tangoplus.tangoq.vo.AnalysisVO
import com.tangoplus.tangoq.vo.MeasureVO
import org.json.JSONArray

class AnalysisViewModel : ViewModel() {

    // ------# 비교할 measure 담을 공간 #------
    val leftMeasurement = MutableLiveData<MeasureVO>()
    val rightMeasurement = MutableLiveData<MeasureVO>()

    // ------# risk part 선택 -> dialog #------
    var relatedAnalyzes = mutableListOf<AnalysisVO>()
    // ------# MainPartAnalysis #------
    var currentAnalysis : AnalysisVO? = null

    var leftAnalyzes : MutableList<MutableList<AnalysisVO>>? = null
    var rightAnalyzes :MutableList<MutableList<AnalysisVO>>? = null
    // ------# risk part 선택 -> 특정 자세 선택 -> dialog #------
    var selectedSeq = 0
    var selectedPart = ""

    var dynamicJa = JSONArray()

    var mafMeasureResult = JSONArray()

    val DynamicTitles = listOf("좌측 손", "우측 손", "좌측 골반", "우측 골반", "좌측 무릎", "우측 무릎") // 0 , 1 , 2

    fun setSeqString(seq: Int?) :String {
        return when (seq) {
            0 -> "정면 분석"
            1 -> "동적 분석"
            2 -> "팔꿉 분석"
            3 -> "왼쪽 측면"
            4 -> "오른쪽 측면"
            5 -> "후면 분석"
            6 -> "앉아 후면"
            else -> ""
        }
    }
}