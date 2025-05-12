package com.tangoplus.tangobody.vo


data class MessageVO(
    val userSn: Int = 0,
    val message : String = "",
    val timeStamp : Long = 0L,
    val route : String = ""
)