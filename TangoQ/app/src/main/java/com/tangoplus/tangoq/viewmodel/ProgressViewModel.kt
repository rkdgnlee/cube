package com.tangoplus.tangoq.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.api.NetworkProgress.postProgressInCurrentProgram
import com.tangoplus.tangoq.vo.ProgramVO
import com.tangoplus.tangoq.vo.ProgressUnitVO
import java.time.LocalDate

class ProgressViewModel : ViewModel() {
    // main
    var fromProgramCustom = false
    var isExpanded = false

    // Main-Custom 현재 program의 주차 - 회차 담아놓는 곳
    var currentProgram : ProgramVO? = null

    // 현재 주차
    var selectWeek = MutableLiveData(0)
    var selectedWeek = MutableLiveData(0)
    var currentWeek = 0

    // 현재 선택된 회차를 말함
    var currentSequence = 0
    var selectedSequence = MutableLiveData<Int>()
    var recommendationSn = 0

    // ProgramCustom에서 쓸 선택된 recommendation
    var selectedRecProgress = 0f

    // 현재 선택된 progress, 즉 시청 기록을 담는 곳(1주의 모든 회차가 다 들어감) 이걸 만약에 1회차에 들어가는 값만 나온다? 그러면 이 list를 줄이고, 거기다가 값을 넣어서 갱신하는 걸로
    var currentProgresses = mutableListOf<ProgressUnitVO>()
    var seqHpvs : List<Float>? = null
    var dailySeqFinished = false

    // ----------------------# AnalysisFragment 에서 사용하는 공간 #---------------------
    var graphProgresses : MutableList<Pair<String, Int>>? = null
    var selectedDate: LocalDate? = LocalDate.now()

    var selectedDailyCount = MutableLiveData<Int>()
    var selectedDailyTime = MutableLiveData<Int>()

    var datesClassifiedByDay = mutableListOf<LocalDate>() // history의 날짜만 따로 뺀 LIST -> MD2 달력에다가 동그라미 용도임

    // 달력
    var currentProgressItem : ProgressUnitVO? = null
    init {
        currentProgressItem = ProgressUnitVO(0, 0,0,0,0,0,"","",0,0,0,0,0, 0,0,0, "")
        selectedDailyCount.value = 0
        selectedDailyTime.value = 0
    }

    fun calculateCurrentSeq(weekIndex: Int): Int {
        return if (weekIndex <= currentWeek) {
            // 현재 1~4주차 까지 다들어간 곳에서 seq를 찾는데, 여기서 해당 값을 통해 받아와야함
//            Log.v("주차에서seq", "$weekIndex, ${currentProgresses},${currentProgresses.minOfOrNull { it.countSet } ?: 0}")
            return currentProgresses.minOfOrNull { it.countSet } ?: 0
        } else {
            0
        }
    }
    suspend fun getProgressData(context: Context) {
        val sns = Triple(recommendationSn, selectedWeek.value?.plus(1) ?: 1, selectedSequence.value?.plus(1) ?: 1)
//        Log.v("현재주차가져오기", "$sns")
        postProgressInCurrentProgram(context.getString(R.string.API_progress), sns, context) { pv3s, progressUnits -> // MutableList<ProgressUnitVO>
            val programExerciseCount = currentProgram?.exercises?.size ?: 0
            currentProgresses = if (progressUnits.size > programExerciseCount) {
                val croppedProgresses = progressUnits.sortedBy { it.uvpSn }.subList(0 , programExerciseCount).toMutableList()
//                Log.v("croppedPRogress", "${croppedProgresses.map { it.uvpSn }}, ${croppedProgresses.map { it.cycleProgress }}")
                croppedProgresses
            } else {
                progressUnits
            }
            seqHpvs = if (progressUnits.size > programExerciseCount) {
                pv3s.map { it * 2 }
            } else pv3s
//            Log.v("현재주차꺼", "$pv3s $seqHpvs ${currentProgresses.map { it.cycleProgress }}")
            // pv3도 넘겨줘야함

        }
    }

}