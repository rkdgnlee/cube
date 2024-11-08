package com.tangoplus.tangoq.data

data class AnalysisUnitVO(
    val columnName: String = "",
    val rawDataName : String = "",
    val rawData : Float = 0f,
    val rawDataBound : Pair<Float, Float>,
    var summary: String = "",
    val state : Boolean
)
