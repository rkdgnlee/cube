package com.tangoplus.tangoq.data

import org.json.JSONArray

data class MeasureVO (
    val deviceSn : Int,
    val measureSn : Int, // 해당 측정 sn
    val regDate : String, // 측정 날짜
    val overall: String?, // 종합점수
    val dangerParts : MutableList<Pair<String, Int>>, // 취약한 부위, 위험도
    val measureResult : JSONArray, // index로 seq하고, jo, ja, jo, jo, jo, jo, jo
    val fileUris : MutableList<String>,
    val isMobile : Boolean,
    var recommendations : List<RecommendationVO>?
)