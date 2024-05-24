package com.tangoplus.tangoq

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.tangoplus.tangoq.`object`.Singleton_t_measure

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 전역 Context 초기화
        appContext = this

        // Singleton 초기화
        Singleton_t_measure.getInstance(appContext)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var appContext: Context
            private set
    }
}