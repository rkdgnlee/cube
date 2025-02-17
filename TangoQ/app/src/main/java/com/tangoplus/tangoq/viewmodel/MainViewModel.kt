package com.tangoplus.tangoq.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangoplus.tangoq.api.LogoutManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainViewModel: ViewModel() {
    private val _showLogoutDialog = MutableLiveData<Boolean>()
    val showLogoutDialog : LiveData<Boolean> = _showLogoutDialog

    init {
        viewModelScope.launch {
            LogoutManager.logoutEvent.collect{
                _showLogoutDialog.postValue(true)
            }
        }
    }

    fun resetLogoutDialog() {
        _showLogoutDialog.value = false
    }
}