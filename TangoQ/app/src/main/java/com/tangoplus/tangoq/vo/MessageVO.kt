package com.tangoplus.tangoq.vo


data class MessageVO(
    val sn: Int = 0 ,
    val message : String = "",
    val timeStamp : Long = 0L,
    val route : String = ""
)