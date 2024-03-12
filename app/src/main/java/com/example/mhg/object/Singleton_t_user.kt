package com.example.mhg.`object`

import android.annotation.SuppressLint
import android.content.Context
import org.json.JSONObject


class Singleton_t_user private constructor(context: Context) {
    var jsonObject: JSONObject? = null
    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance : Singleton_t_user? = null
        @SuppressLint("StaticFieldLeak")
        private lateinit var context : Context
        fun getInstance(_context: Context): Singleton_t_user {
            return instance ?: synchronized(this) {
                instance ?: Singleton_t_user(_context).also {
                    context = _context
                    instance = it
                }
            }
        }
    }
}