package com.tangoplus.tangoq.`object`

import android.content.ContentValues
import android.content.Context
import android.util.Log
import android.widget.Toast
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException

object NetworkMeasure {
    fun insertMeasurePartsByJson(myUrl:String, json: String, callback: () -> Unit, context: Context) {
        val client = OkHttpClient()
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)
        val request = Request.Builder()
            .url("${myUrl}/") // TODO URL 수정 필요
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.v("HTTP>MeasureFetch", "Failed to execute request!!")
                Toast.makeText(context, "데이터 연결이 실패했습니다. 잠시 후에 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.v("HTTP>MeasureFetch", "$responseBody")
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