package com.tangoplus.tangoq.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tangoplus.tangoq.vo.AnalysisVO
import com.tangoplus.tangoq.vo.DateDisplay
import com.tangoplus.tangoq.vo.MeasureVO
import org.json.JSONArray

class AnalysisViewModel : ViewModel() {

    // ------# 비교할 measure 담을 공간 #------
    val leftMeasurement = MutableLiveData<MeasureVO>()
    val rightMeasurement = MutableLiveData<MeasureVO>()

    // actv에 연결할 날짜들
    var leftMeasureDate = MutableLiveData<DateDisplay>()
    var rightMeasureDate = MutableLiveData<DateDisplay>()
    var measureDisplayDates = listOf<DateDisplay>()
    fun createDateDisplayList(measures: List<MeasureVO>): List<DateDisplay> {
        return measures.map { measure ->
            DateDisplay(
                fullDateTime = measure.regDate,
                displayDate = "${measure.regDate.substring(0, 11)}\n${measure.userName}"
            )
        }
    }
    // 필터링 함수 수정: 좌우에서 선택된 값은 제외
    fun getFilteredDates(excludeDates: List<DateDisplay>): List<DateDisplay> {
        return measureDisplayDates.filter { date ->
            excludeDates.none { it.fullDateTime == date.fullDateTime }
        }
    }
    // ------# risk part 선택 -> dialog #------
    var relatedAnalyzes = mutableListOf<AnalysisVO>()
    // ------# MainPartAnalysis #------
    var currentAnalysis : AnalysisVO? = null

    var leftAnalyzes : MutableList<MutableList<AnalysisVO>>? = null
    var rightAnalyzes :MutableList<MutableList<AnalysisVO>>? = null
    var trendLeftUri: String? = null
    var trendRightUri : String? = null

    var currentIndex : Int = 0



    private var leftPlaybackPosition = 0L
    private var leftwindowIndex = 0
    private var leftplayWhenReady = true
    private var rightPlaybackPosition = 0L
    private var rightWindowIndex = 0
    private var rightPlayWhenReady = true
    // ------# risk part 선택 -> 특정 자세 선택 -> dialog #------
    var selectedSeq = 0
    var selectedPart = ""

    var dynamicJa = JSONArray()

    var mafMeasureResult = JSONArray()

    val dynamicTitles = listOf("좌측 손", "우측 손", "골반 중심", "골반 중심", "좌측 무릎", "우측 무릎") // 0 , 1 , 2

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