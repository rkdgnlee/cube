package com.tangoplus.tangoq.data

import org.json.JSONObject

data class MeasureVO (
    val measureId : String, // 해당 측정 sn
    val regDate : String, // 측정 날짜
    val overall: Int?, // 종합점수
    val dangerParts : MutableList<Pair<String, Int>>, // 취약한 부위, 위험도
    val measureResult : MutableList<AnalysisVO>
)