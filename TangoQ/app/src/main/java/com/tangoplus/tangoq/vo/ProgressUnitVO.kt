package com.tangoplus.tangoq.vo

data class ProgressUnitVO(

    val uvpSn: Int,
    val exerciseId : Int,
    val recommendationSn : Int,
    val currentWeek: Int,
    val currentSequence: Int,
    val requiredSequence : Int,
    val weekStartAt : String,
    val weekEndAt : String,
    val videoDuration: Int,
    val lastProgress : Int,
    val isCompleted : Int,
    val updateDate : String?,
)