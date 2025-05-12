package com.tangoplus.tangobody.vo

data class ExerciseHistoryVO(
    val evpSn: Int,
    val userSn: Int,
    val exerciseId : Int,
    val exerciseName: String,
    val duration : Int,
    val progress: Int,
    val exerciseTypeId: Int,
    val exerciseCategoryId: Int,
    val completed: Int,
    val registeredAt : String,
    val updatedAt : String,
)