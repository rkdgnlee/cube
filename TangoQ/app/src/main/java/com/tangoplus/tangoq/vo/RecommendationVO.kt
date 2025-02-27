package com.tangoplus.tangoq.vo

data class RecommendationVO(
    val recommendationSn : Int,
    val userSn : Int,
    val serverSn : Int,
    val programSn : Int,
    val exerciseTypeId: Int = 0 ,
    val exerciseStage : Int = 0,
    val title: String,
    val totalDuration : Int = 0,
    val numberOfExercise : Int = 0,
    val createdAt : String = "",
    val startAt : String = "",
    val endAt : String = "",
    val expired: Int = 0,
    val totalProgress : Int = 0
)
