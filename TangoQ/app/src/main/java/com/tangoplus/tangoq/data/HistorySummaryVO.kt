package com.tangoplus.tangoq.data

data class HistorySummaryVO(
    val exerciseId: String,
    var viewCount: Int = 0,
    var lastViewDate : String? = null
)
