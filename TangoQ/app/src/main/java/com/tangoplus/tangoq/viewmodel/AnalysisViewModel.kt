package com.tangoplus.tangoq.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tangoplus.tangoq.vo.AnalysisUnitVO
import com.tangoplus.tangoq.vo.AnalysisVO
import com.tangoplus.tangoq.vo.DateDisplay
import com.tangoplus.tangoq.vo.MeasureVO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray

class AnalysisViewModel : ViewModel() {
    // MainAnalysisFragment
    var currentParts : List<String>? = null
    // seqBottomSheet
    val currentPart = MutableLiveData<String>()
    var selectPart = MutableLiveData<String>()

    // ------# 비교할 measure 담을 공간 #------
    val leftMeasurement = MutableLiveData<MeasureVO>()
    val rightMeasurement = MutableLiveData<MeasureVO>()

    // trend
    var leftAnalysises : MutableList<MutableList<AnalysisUnitVO>>? = null
    var rightAnalysises : MutableList<MutableList<AnalysisUnitVO>>? = null
    var trendLeftUri: String? = null
    var trendRightUri : String? = null

    // trend(자세별: 0~6 저장), mainAnalysis(seq: 0, 1, 2, 3 저장 ) // measureDetail(seq: 0~6)사용
    var currentIndex : Int = 0

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

    private val _existedMonthProgresses = MutableStateFlow<List<String>>(emptyList())
    val existedMonthProgresses: StateFlow<List<String>> = _existedMonthProgresses

    fun updateMonthProgress(newData: List<String>) {
        _existedMonthProgresses.value = newData
    }
    var mdMeasureResult = JSONArray()
    val dynamicTitles = listOf("좌측 손", "우측 손", "골반 중심", "골반 중심", "좌측 무릎", "우측 무릎") // 0 , 1 , 2


}