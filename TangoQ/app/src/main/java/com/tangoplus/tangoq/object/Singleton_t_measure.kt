package com.tangoplus.tangoq.`object`

import android.annotation.SuppressLint
import android.content.Context
import com.tangoplus.tangoq.data.MeasureVO
import org.json.JSONArray
import org.json.JSONObject

class Singleton_t_measure private constructor(context: Context){
    var measures : MutableList<MeasureVO>? = null


    init {
        initialize()
    }
    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: Singleton_t_measure? = null

        fun getInstance(_context: Context): Singleton_t_measure {
            return instance ?: synchronized(this) {
                instance ?: Singleton_t_measure(_context).also {
                    instance = it
                }
            }
        }
    }
    private fun initialize() {
        // JSON 초기화 로직을 여기에 추가
        measures = null
    }
}