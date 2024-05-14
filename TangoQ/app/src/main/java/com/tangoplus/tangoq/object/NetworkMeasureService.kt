package com.tangoplus.tangoq.`object`

import android.content.ContentValues
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException

object NetworkMeasureService {
    fun insertMeasurePartsByJson(myUrl:String, json: String, callback: () -> Unit) {
        val client = OkHttpClient()
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)
        val request = Request.Builder()
            .url("${myUrl}/") // TODO URL 수정 필요
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.v(ContentValues.TAG, "Failed to execute request!!")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.v(ContentValues.TAG, "$responseBody")
                callback()
            }
        })
    }

//    fun updateMeasurePartsByJson(myUrl:String, json: String, callback: () -> Unit) {
//        val client = OkHttpClient()
//        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)
//        val request = Request.Builder()
//            .url("${myUrl}/") // TODO URL 수정 필요
//            .post(body)
//            .build()
//
//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                Log.v(ContentValues.TAG, "Failed to execute request!!")
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                val responseBody = response.body?.string()
//                Log.v(ContentValues.TAG, "$responseBody")
//                callback()
//            }
//        })
//    }

}