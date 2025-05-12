package com.tangoplus.tangobody.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AppViewModel: ViewModel() {
    private val _logoutTrigger = MutableLiveData<Boolean>()
    val logoutTrigger: LiveData<Boolean> = _logoutTrigger

    fun triggerLogout() {
        _logoutTrigger.postValue(true)
    }
    fun resetTrigger() {
        _logoutTrigger.postValue(false)
    }


}