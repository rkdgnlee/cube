package com.tangoplus.tangoq.data

data class AnalysisUnitVO(
    val columnName: String = "",
    val rawDataName : String = "",
    val rawData : Float = 0f,
    val rawDataBound : Triple<Float, Float, Float>,
    var summary: String = "",
    val state : Int
)
