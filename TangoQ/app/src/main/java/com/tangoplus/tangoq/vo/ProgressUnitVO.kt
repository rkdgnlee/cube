package com.tangoplus.tangoq.vo

data class ProgressUnitVO( // 운동 1개 : 1개의 HistoryUnit 인데 MD2의 달력에 들어갈 용도로 HistorySummaryVO와 통합하기?

    val uvpSn: Int,
    val exerciseId : Int,
    val recommendationSn : Int,
    val currentWeek: Int,
    val currentSequence: Int,
    val requiredSequence : Int,
    val videoDuration: Int,
    val lastProgress : Int,
    val isCompleted : Int,
    val updateDate : String,
)