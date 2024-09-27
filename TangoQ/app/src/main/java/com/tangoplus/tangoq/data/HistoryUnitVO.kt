package com.tangoplus.tangoq.data

data class HistoryUnitVO( // 운동 1개 : 1개의 HistoryUnit
    val exerciseId: String?, // exerciseId -> exerciseVO -->
    val lastPosition: Int?,
    val regDate: String?,

)