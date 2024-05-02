package com.tangoplus.tangoq.ViewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONObject

class SignInViewModel: ViewModel() {
    val User = MutableLiveData(JSONObject())
    // 로그인
    var currentidCon = MutableLiveData(false)
    var currentPwCon = MutableLiveData(false)
    var idPwCondition = MutableLiveData(false)
    var id = ""
    var pw = ""
    init {
        User.value = JSONObject()
        currentidCon.observeForever{ updateIdPwCondition() }
        currentPwCon.observeForever{ updateIdPwCondition() }
    }
    private fun updateIdPwCondition() {
        idPwCondition.value = currentidCon.value == true && currentPwCon.value == true
    }

    val idCondition = MutableLiveData(false)
//    val nameCondition = MutableLiveData(false)
    val mobileCondition = MutableLiveData(false)
    val pwCondition = MutableLiveData(false)
    val pwCompare = MutableLiveData(false)
//    val emailCondition = MutableLiveData(false)
    val mobileAuthCondition = MutableLiveData(false)

    override fun onCleared() {
        super.onCleared()
        currentidCon.removeObserver { updateIdPwCondition() }
        currentPwCon.removeObserver { updateIdPwCondition() }
    }

}