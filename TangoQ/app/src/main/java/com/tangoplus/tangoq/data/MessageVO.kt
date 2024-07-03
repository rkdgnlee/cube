package com.tangoplus.tangoq.data

data class MessageVO(
    val sn: Long = 0L,
    val message : String = "",
    val timestamp : Long? = 0L,
    val route : String = ""
)