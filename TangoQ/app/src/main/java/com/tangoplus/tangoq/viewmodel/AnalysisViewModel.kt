package com.tangoplus.tangoq.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tangoplus.tangoq.vo.AnalysisVO
import com.tangoplus.tangoq.vo.DateDisplay
import com.tangoplus.tangoq.vo.MeasureVO
import org.json.JSONArray

class AnalysisViewModel : ViewModel() {
    // MeasureAnalysisFragment
    var currentParts : List<String>? = null
    var currentPartIndex = MutableLiveData<Int>()

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

    // trend
    var leftAnalysises : MutableList<MutableList<AnalysisVO>>? = null
    var rightAnalysises : MutableList<MutableList<AnalysisVO>>? = null
    var trendLeftUri: String? = null
    var trendRightUri : String? = null
    var currentIndex : Int = 0

    fun setTrendIndexToZero() {
        currentIndex = 0
    }
/* 버튼   실제 index
*  0    0
*  1    2
*  2    3
*  3    4
*  4    5
*  5    6
*  6    7 (동적 6 번째 )
* */
    var mafMeasureResult = JSONArray()

    val dynamicTitles = listOf("좌측 손", "우측 손", "골반 중심", "골반 중심", "좌측 무릎", "우측 무릎") // 0 , 1 , 2

}