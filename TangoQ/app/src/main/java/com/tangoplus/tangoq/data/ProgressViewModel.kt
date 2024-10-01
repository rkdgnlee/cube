package com.tangoplus.tangoq.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDate

class ProgressViewModel : ViewModel() {
    // Main-Custom 현재 program의 주차 - 회차 담아놓는 곳
    var currentProgram : ProgramVO? = null

    // 현재 선택된 progress, 즉 시청 기록을 담는 곳(모든 회차가 다 들어감)
    var currentProgresses = mutableListOf<MutableList<ProgressUnitVO>>()
    var totalSeq = 0
    // 현재 선택된 회차를 말함
    var currentSequence = 0
    var selectedSequence = MutableLiveData<Int>()


    // ------# 프로그램 #------
//    val allHistorys = mutableListOf<ProgressUnitVO>() // MD2 전체 historyUnitVO를 담아놓을 공간
//    val classifiedByDay : MutableList<Triple<String, Int, Int>>? = null // history에 대해서 다 담아놓을 공간.

    /* Triple<String, Int, Int>
    * regDate, 마지막시청일자, 완료한 횟수.
    * */
    var datesClassifiedByDay =
        mutableListOf<LocalDate>() // history의 날짜만 따로 뺀 LIST -> MD2 달력에다가 동그라미 용도임
    var weeklyHistorys : MutableList<Triple<String, Int, Int>>? = null // MD2 막대그래프에 들어갈수치 -> 가장 최근 운동들임 7개가 훨씬 많을수도 있고

}