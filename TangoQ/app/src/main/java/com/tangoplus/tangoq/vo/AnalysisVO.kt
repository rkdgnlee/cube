package com.tangoplus.tangoq.vo

data class AnalysisVO(
    val indexx: Int = 0,
    val labels : MutableList<AnalysisUnitVO>, // 좌측어깨-우측어깨-코 기울기, (기준값, 오차범위), 정상판단
)