package com.tangoplus.tangoq.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDate

class HistoryViewModel : ViewModel() {
    // Main-Custom 현재 program의 주차 - 회차 담아놓는 곳
    var currentProgram : ProgramVO? = null
    var currentEpisode = 0
    var currentWeek = 0

    var selectedEpisode = MutableLiveData<Int>()
    var selectedWeek = MutableLiveData<Int>()
    var selectWeek = MutableLiveData<Int>()


    val allHistorys = mutableListOf<HistoryUnitVO>() // MD2 전체 historyUnitVO를 담아놓을 공간
    val classifiedByDay = mutableListOf<Triple<String, Int, Int>>() // history에 필요한것들
    var datesClassifiedByDay = mutableListOf<LocalDate>() // history의 날짜만 따로 뺀 LIST
    var weeklyHistorys = mutableListOf<Triple<String, Int, Int>>()

    init {


        currentWeek = 1
        selectedWeek.value = 0
        selectWeek.value = 0

        currentProgram = ProgramVO(-1, "", "", "", 0, 0, 0,mutableListOf(), mutableListOf())
    }
}