package com.tangoplus.tangoq.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDate

class ProgressViewModel : ViewModel() {
    // Main-Custom 현재 program의 주차 - 회차 담아놓는 곳
    var currentProgram : ProgramVO? = null

    // 현재 주차
    var selectWeek = MutableLiveData(0)
    var selectedWeek = MutableLiveData(0)

    // 현재 선택된 회차를 말함
    var currentSequence = 0
    var selectedSequence = MutableLiveData<Int>()

    // 현재 선택된 progress, 즉 시청 기록을 담는 곳(모든 회차가 다 들어감)
    var currentProgresses = mutableListOf<MutableList<ProgressUnitVO>>()

    // ----------------------# MD2 에서 사용하는 공간 #---------------------
    var selectedDailyCount = MutableLiveData<Int>()
    var selectedDailyTime = MutableLiveData<Int>()

    var datesClassifiedByDay = mutableListOf<LocalDate>() // history의 날짜만 따로 뺀 LIST -> MD2 달력에다가 동그라미 용도임

    // 달력
    var currentProgressItem : ProgressUnitVO? = null
    init {
        currentProgressItem = ProgressUnitVO(0,0,0,0,0,0,0,0,0, "")
        selectedDailyCount.value = 0
        selectedDailyTime.value = 0
    }
}