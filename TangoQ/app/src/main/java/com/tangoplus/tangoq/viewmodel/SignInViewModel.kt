package com.tangoplus.tangoq.viewmodel

import android.net.Uri
import android.os.CountDownTimer
import android.text.TextWatcher
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONObject

class SignInViewModel: ViewModel() {
    // 회원가입에 담는 user
    val User = MutableLiveData(JSONObject())

    // sns 회원가입 시 담아서 쓰는 데이터들
    var snsJo = JSONObject()

    // ------# 로그인 #------
    var currentEmailCon = MutableLiveData(false)
    var currentPwCon = MutableLiveData(false)
    var emailPwCondition = MutableLiveData(false)
    var emailId = MutableLiveData("")
    var fullEmail = MutableLiveData("")
    var pw = MutableLiveData("")
    var setGender = MutableLiveData("")    // 아이디 비밀번호 찾기
    var selectGender = MutableLiveData("")
    // FindAccount
    val mobileAuthCondition = MutableLiveData(false)
    var countDownTimer : CountDownTimer? = null

    // 회원가입 condition
    // PASS나 문자인증에서 받아온 값들
    val passName = MutableLiveData("")
    val passMobile = MutableLiveData("")
    var nameCondition = MutableLiveData(false)
    val mobileCondition = MutableLiveData(false)
    val emailIdCondition = MutableLiveData(false)
    val domainCondition = MutableLiveData(false)
    val emailVerify = MutableLiveData(false)
    var insertToken = ""

    val pwCondition = MutableLiveData(false)
    val pwCompare = MutableLiveData(false)
    val pwBothTrue = MediatorLiveData<Boolean>().apply {
        value = false
        addSource(pwCondition) { condition ->
            value = condition && (pwCompare.value ?: false)
        }
        addSource(pwCompare) { compare ->
            value = (pwCondition.value ?: false) && compare
        }
    }
    val isNextPage = MutableLiveData(false)

    val passAuthCondition = MediatorLiveData<Boolean>().apply {
        value = false
        addSource(passName) { condition ->
            value = condition != "" && (passMobile.value != "")
        }
        addSource(passMobile) { condition ->
            value = condition != "" && (passName.value != "")
        }
    }

    val emailCondition = MediatorLiveData<Boolean>().apply {
        value = false
        addSource(emailIdCondition) { condition ->
            value = condition && (domainCondition.value ?: false)
        }
        addSource(domainCondition) { condition ->
            value = condition && (emailIdCondition.value ?: false)
        }
    }

    val allTrueLiveData = MediatorLiveData<Boolean>().apply {
        val checkAllTrue = {
            val v1 = emailCondition.value ?: false // 이메일 형식
            val v2 = emailVerify.value ?: false // 이메일 인증
            val v3 = passAuthCondition.value ?: false // 핸드폰 번호 인증
            val v4 = pwBothTrue.value ?: false // 비밀번호 형식
            val v5 = nameCondition.value ?: false // 비밀번호 형식
            value = v1 && v2 && v3 && v4 && v5
        }
        addSource(emailCondition) { checkAllTrue() }
        addSource(emailVerify) { checkAllTrue() }
        addSource(passAuthCondition) { checkAllTrue() }
        addSource(pwBothTrue) { checkAllTrue() }
        addSource(nameCondition) { checkAllTrue() }
    }

    // ------# 약관 동의 #------
    val agreementMk1 = MutableLiveData(false)
    val agreementMk2 = MutableLiveData(false)

    val marketingAgree = MutableLiveData(false)
    val agreementAll3 = MediatorLiveData<Boolean>().apply {
        // 중복되는 MediatorLiveData를 하나로 통합
        addSource(agreementMk1) { updateAgreementAll3State() }
        addSource(agreementMk2) { updateAgreementAll3State() }
    }

    private fun updateAgreementAll3State() {
        // switch2와 switch3 중 하나라도 켜져 있으면 switch1 유지
        agreementAll3.value = agreementMk1.value == true || agreementMk2.value == true
    }
    // ------# profile edit #------
    val ivProfile = MutableLiveData<Uri>()
    var snsCount = 0
    val setWeight = MutableLiveData<Int>()
    val setHeight = MutableLiveData<Int>()
    val setEmail = MutableLiveData<String>()
    val setBirthday = MutableLiveData<String>()
    val setMobile = MutableLiveData<String>()
    val editChangeCondition = MutableLiveData(false)

    val birthdayCondition = MutableLiveData(false)
    val heightCondition = MutableLiveData(false)
    val weightCondition = MutableLiveData(false)

    var verificationId = ""
    init {
        User.value = JSONObject()
        currentEmailCon.observeForever{ updateIdPwCondition() }
        currentPwCon.observeForever{ updateIdPwCondition() }
        fullEmail.value = ""
        pw.value = ""
        emailId.value = ""
        setWeight.value = 0
        setHeight.value = 0
        setEmail.value = ""
    }
    private fun updateIdPwCondition() {
        emailPwCondition.value = currentEmailCon.value == true && currentPwCon.value == true
    }

    // findAccount
    var resetJwt : String = ""
    var saveEmail = ""
    var isFindEmail = MutableLiveData(true)
    var textWatcher : TextWatcher? = null

    // PIN 번호 로그인
    var setPin = ""
    var pinCondition = MutableLiveData(false)
    override fun onCleared() {
        super.onCleared()
        currentEmailCon.removeObserver { updateIdPwCondition() }
        currentPwCon.removeObserver { updateIdPwCondition() }
    }
}