package com.tangoplus.tangoq.data

import org.json.JSONObject

data class MeasureVO (
    // TODO 데이터 클래스 맞춰서 만든다음, 관리 ㄱㄱ
    val name : String,
    val score: Int,
    val dangerParts : MutableList<String>,
    val drawableName : String = "",
    var select : Boolean = false,
    val anglesNDistances : JSONObject?
)