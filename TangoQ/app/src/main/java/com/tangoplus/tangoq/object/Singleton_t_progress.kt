package com.tangoplus.tangoq.`object`

import android.annotation.SuppressLint
import android.content.Context
import com.tangoplus.tangoq.data.ProgressUnitVO
import com.tangoplus.tangoq.data.ProgressVO
import org.json.JSONObject

class Singleton_t_progress private constructor(context: Context){
    var progresses: MutableList<MutableList<MutableList<ProgressUnitVO>>>? = null
    // 1개 측정안 프로그램 단위 > 프로그램 안의 회차 단위 > 회차 안에 프로그램 갯수들.
    init {
        initialize()
    }
    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: Singleton_t_progress? = null

        fun getInstance(_context: Context): Singleton_t_progress {
            return instance ?: synchronized(this) {
                instance ?: Singleton_t_progress(_context).also {
                    instance = it
                }
            }
        }
    }

    private fun initialize() {
        progresses = mutableListOf()
    }
}