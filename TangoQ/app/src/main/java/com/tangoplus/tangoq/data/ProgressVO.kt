package com.tangoplus.tangoq.data

data class ProgressVO( // 프로그램 하나의 전체 히스토리임.
    val exerciseId: String?, // exerciseId -> exerciseVO -->
    val lastPosition: Int?,
    val regDate: String?,
    )


/* 이 progress 한 개 -> 영상 1개 당 하나의 프로그램 기간 동안의 운동 기준으로 전부 묶인 프로그레스임.
*
*
*
* */