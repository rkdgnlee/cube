package com.tangoplus.tangoq.vo

data class AnalysisVO(
    val indexx: Int = 0,
    val labels : MutableList<AnalysisUnitVO>, // 좌측어깨-우측어깨-코 기울기, (기준값, 오차범위), 정상판단
    val url: String = ""
)
/*
이 data class는 MeasureAnalysis에서 사용합니다. 그 용도에만 맞게 선언했습니다. indexx는 0, 1, 2, 3 으로만 사용합니다.
trend(비교)에선 indexx가 0, 1, 2, 3, 4, 5, 6까지 증가합니다.
*/