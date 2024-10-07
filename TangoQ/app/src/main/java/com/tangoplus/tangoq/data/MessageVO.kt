package com.tangoplus.tangoq.data

data class MessageVO(
    val sn: Long = 0L,
    val message : String = "",
    val timeStamp : Long? = 0L,
    val route : String = ""
)