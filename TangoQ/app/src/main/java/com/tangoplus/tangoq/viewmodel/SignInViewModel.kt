package com.tangoplus.tangoq.viewmodel

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONObject

class SignInViewModel: ViewModel() {
    // 회원가입에 담는 user
    val User = MutableLiveData(JSONObject())
    // ------# 로그인 #------
    var currentIdCon = MutableLiveData(false)
    var currentPwCon = MutableLiveData(false)
    var idPwCondition = MutableLiveData(false)
    var id = MutableLiveData("")
    var pw = MutableLiveData("")
    var emailId = MutableLiveData("")
    var isFindId = true
    var invalidIdCondition = MutableLiveData(false)
    var loginTryCount = MutableLiveData(0)

    // ------# 약관 동의 #------
    val agreementMk1 = MutableLiveData(false)
    val agreementMk2 = MutableLiveData(false)
    val marketingAgree = MutableLiveData(false)

    // ------# profile edit #------
    val ivProfile = MutableLiveData<Uri>()
    var snsCount = 0
    val setWeight = MutableLiveData<Int>()
    val setHeight = MutableLiveData<Int>()
    val setEmail = MutableLiveData<String>()

    init {
        User.value = JSONObject()
        currentIdCon.observeForever{ updateIdPwCondition() }
        currentPwCon.observeForever{ updateIdPwCondition() }
        id.value = ""
        pw.value = ""
        emailId.value = ""
        setWeight.value = 0
        setHeight.value = 0
        setEmail.value = ""
    }
    private fun updateIdPwCondition() {
        idPwCondition.value = currentIdCon.value == true && currentPwCon.value == true
    }

    val idCondition = MutableLiveData(false)
    val mobileCondition = MutableLiveData(false)
    val pwCondition = MutableLiveData(false)
    val pwCompare = MutableLiveData(false)
    val mobileAuthCondition = MutableLiveData(false)

    override fun onCleared() {
        super.onCleared()
        currentIdCon.removeObserver { updateIdPwCondition() }
        currentPwCon.removeObserver { updateIdPwCondition() }
    }

}