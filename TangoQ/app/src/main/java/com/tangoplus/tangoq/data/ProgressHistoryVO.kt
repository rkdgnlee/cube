package com.tangoplus.tangoq.data

data class ProgressHistoryVO (
    val sn : Int,
    val userSn : Int,
    val uvpSn : Int,
    val exerciseName: String,
    val recommendationTitle : String,
    val weekNumber : Int,
    val executionDate : String,
    val countSet : Int,
    val completed : Int,
    val expired : Int,
)