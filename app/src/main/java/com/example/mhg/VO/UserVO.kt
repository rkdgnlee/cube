package com.example.mhg.VO

import java.io.Serializable

data class UserVO(
    var user_id: String = "",
    var user_name: String = "",
    var user_grade: Int = 0,
    var user_gender: String = "Male",
    var register_date: String = "20240601",
    var mobile: String = "",
    var user_email : String = "",
    var birthday: String = "",
    var height: Double = 0.0,
    var weight: Double = 0.0,
    var address: String = "",
    var address_detail: String = "",
    var login_token: String = "",
    ) : Serializable