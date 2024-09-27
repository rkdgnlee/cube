package com.tangoplus.tangoq.data

data class HistorySummaryVO( // 대시보드 2에서 쓰기 위해 재구성한 하루 기준 data class 임
    val exerciseId: String,
    var viewCount: Int = 0,
    var lastViewDate : String? = null
)
