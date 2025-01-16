package com.tangoplus.tangoq.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.tangoplus.tangoq.PlayFullScreenActivity
import com.tangoplus.tangoq.function.EventBusManager


class ForcedTerminationService  : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        EventBusManager.eventBus.post(TerminationEvent())
    }
}