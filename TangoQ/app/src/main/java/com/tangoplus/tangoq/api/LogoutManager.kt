package com.tangoplus.tangoq.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

object LogoutManager {
    private val _logoutEvent = MutableSharedFlow<Unit>(replay = 1)
    val logoutEvent = _logoutEvent.asSharedFlow()

    fun notifyLogout() {
        CoroutineScope(Dispatchers.Main).launch {
            _logoutEvent.emit(Unit)
        }
    }
}