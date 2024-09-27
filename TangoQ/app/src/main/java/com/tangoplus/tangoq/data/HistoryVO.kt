package com.tangoplus.tangoq.data

data class HistoryVO(
    val programId: String?,
    val isFinish : Boolean,
    val doingExercises: MutableList<MutableList<HistoryUnitVO>>, // MutableList가
) // 프로그램 하나의 전체 히스토리임.