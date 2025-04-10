package com.tangoplus.tangoq.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

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