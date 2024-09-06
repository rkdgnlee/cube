package com.tangoplus.tangoq.data

data class AnalysisVO (
    val sequence: Int?, // 1~7단계
    val toVerti: Boolean, // horizon, verti
    val key: String,
    val value: Double
)