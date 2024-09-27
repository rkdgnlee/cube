package com.tangoplus.tangoq.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDate

class HistoryViewModel : ViewModel() {
    // Main-Custom 현재 program의 주차 - 회차 담아놓는 곳
    var currentProgram : ProgramVO? = null
    var programs: MutableList<ProgramVO>? = null
    var currentEpisode = 0

    var selectedEpisode = MutableLiveData<Int>()


    val allHistorys = mutableListOf<HistoryUnitVO>() // MD2 전체 historyUnitVO를 담아놓을 공간
    val classifiedByDay : MutableList<Triple<String, Int, Int>>? = null // MD2 막대그래프에 들어갈수치

    var datesClassifiedByDay = mutableListOf<LocalDate>() // history의 날짜만 따로 뺀 LIST
    var weeklyHistorys : MutableList<Triple<String, Int, Int>>? = null // MD2 막대그래프에 들어갈수치

}