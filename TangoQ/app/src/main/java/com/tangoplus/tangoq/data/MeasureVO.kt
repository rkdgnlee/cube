package com.tangoplus.tangoq.data

import org.json.JSONObject

data class MeasureVO (
    val partName : String,
    val drawableName : String = "",
    var select : Boolean = false,
    val anglesNDistances : JSONObject?
)