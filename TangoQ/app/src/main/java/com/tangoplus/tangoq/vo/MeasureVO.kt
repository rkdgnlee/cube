package com.tangoplus.tangoq.vo

import org.json.JSONArray

data class MeasureVO (
    val deviceSn : Int,
    val sn : Int, // info_sn
    val regDate : String, // 측정 날짜
    val overall: String?, // 종합점수
    val dangerParts : MutableList<Pair<String, Float>>, // 취약한 부위, 위험도
    var measureResult : JSONArray?, // index로 seq하고, jo, ja, jo, jo, jo, jo, jo
    var fileUris : MutableList<String>?,
    val isMobile : Boolean,
    var recommendations : List<RecommendationVO>?
)