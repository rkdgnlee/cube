package com.tangoplus.tangobody.vo

data class DateDisplay(
    val fullDateTime: String,  // "2025-02-05 11:00:00"
    val displayDate: String    // "2025-02-05"
) {
    override fun toString(): String = displayDate  // ACTV에 표시될 때는 날짜만 보이도록
}
