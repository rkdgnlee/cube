package com.tangoplus.tangoq.`object`

import android.annotation.SuppressLint
import android.content.Context
import com.tangoplus.tangoq.data.HistoryVO
import org.json.JSONObject

class Singleton_t_history private constructor(context: Context){
    var jsonObject : JSONObject? = null
    var historys: MutableList<HistoryVO>? = null

    init {
        initialize()
    }
    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: Singleton_t_history? = null

        fun getInstance(_context: Context): Singleton_t_history {
            return instance ?: synchronized(this) {
                instance ?: Singleton_t_history(_context).also {
                    instance = it
                }
            }
        }
    }

    private fun initialize() {
        // JSON 초기화
        jsonObject = JSONObject()
        historys = mutableListOf()
    }
}