package com.tangoplus.tangoq.data

data class RecommendationVO(
    val recommendationSn : Int,
    val serverSn : Int,
    val userSn : Int,
    val programSn : Int,
    val title: String,
    val regDate : String
)
