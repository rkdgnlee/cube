package com.tangoplus.tangoq.data

import org.json.JSONObject

data class MeasureVO (
    val partName : String,
    val anglesNDistances : JSONObject?
)