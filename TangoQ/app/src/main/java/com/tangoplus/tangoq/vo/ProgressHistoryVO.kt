package com.tangoplus.tangoq.vo

data class ProgressHistoryVO (
    val sn : Int,
    val userSn : Int,
    val uvpSn : Int,
    val contentSn : Int,
    val exerciseName: String,
    val duration: Int? = null,
    val imageFilePathReal : String = "",
    val recommendationSn: Int,
    val serverSn : Int,
    val recommendationTitle : String,
    val weekNumber : Int,
    val executionDate : String,
    val countSet : Int,
    val completed : Int,
    val expired : Int,
    val createdAt : String = ""
)