package com.tangoplus.tangoq.ViewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONObject

class SignInViewModel: ViewModel() {
    val User = MutableLiveData(JSONObject())

    init {
        User.value = JSONObject()
    }

    val idCondition = MutableLiveData(false)
    val nameCondition = MutableLiveData(false)
    val mobileCondition = MutableLiveData(false)
    val pwCondition = MutableLiveData(false)
    val pwCompare = MutableLiveData(false)
    val emailCondition = MutableLiveData(false)
    val mobileAuthCondition = MutableLiveData(false)
}