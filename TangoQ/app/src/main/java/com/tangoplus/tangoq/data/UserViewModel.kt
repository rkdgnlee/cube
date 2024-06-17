package com.tangoplus.tangoq.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONObject

class UserViewModel: ViewModel() {
    val User = MutableLiveData(JSONObject())

    var setupProgress = 25
    var setupStep = 0



    init {
        User.value = JSONObject()
        setupProgress = 25
        setupStep = 0
    }
}