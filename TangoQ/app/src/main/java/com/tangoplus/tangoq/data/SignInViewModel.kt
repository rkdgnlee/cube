package com.tangoplus.tangoq.data

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONObject

class SignInViewModel: ViewModel() {
    // 회원가입에 담는 user
    val User = MutableLiveData(JSONObject())
    // 로그인
    var currentidCon = MutableLiveData(false)
    var currentPwCon = MutableLiveData(false)
    var idPwCondition = MutableLiveData(false)
    var id = MutableLiveData("")
    var pw = MutableLiveData("")
    var emailId = MutableLiveData("")

    val ivProfile = MutableLiveData<Uri>()

    // 구글 로그인에 담을 json
    val googleJson = JSONObject()

    var snsCount = 0


    init {
        User.value = JSONObject()
        currentidCon.observeForever{ updateIdPwCondition() }
        currentPwCon.observeForever{ updateIdPwCondition() }
        id.value = ""
        pw.value = ""
        emailId.value = ""
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