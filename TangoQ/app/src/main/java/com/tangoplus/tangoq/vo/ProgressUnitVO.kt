package com.tangoplus.tangoq.vo

data class ProgressUnitVO(
    val uvpSn: Int,
    val userSn : Int,
    val exerciseId : Int,
    val recommendationSn : Int,
    val weekNumber: Int,
    val weekStartAt : String,
    val weekEndAt : String,
    val exerciseTypeId: Int? = null,
    val exerciseStage : Int? = null,
    val duration: Int,
    val progress : Int,
    val countSet: Int,
    val requiredSet: Int,
    val isWatched : Int,
    val updatedAt : String?,
    val cycleProgress: Int = 0,
)