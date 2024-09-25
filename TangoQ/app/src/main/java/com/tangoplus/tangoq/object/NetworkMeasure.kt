package com.tangoplus.tangoq.`object`

import android.content.ContentValues
import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException

object NetworkMeasure {


    fun insertMeasureData(myUrl:String, json: String, imageFile: File, videoFile: File, callback: () -> Unit, context: Context) {
        val client = OkHttpClient()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("json", json)
            .addFormDataPart("image", imageFile.name, imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull()))
            .addFormDataPart("video", videoFile.name, videoFile.asRequestBody("video/mp4".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url("${myUrl}/") // TODO URL 수정 필요
            .post(requestBody)
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

    // 모바일 측정 결과 넣기.
    fun insertMeasureResultByJson(myUrl: String, json: String, callback: () -> Unit, context: Context) {
        val client = OkHttpClient()
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)
        val request = Request.Builder()
            .url("${myUrl}/insert.php")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("HTTP/MeasureInsert", "Failed to execute request! $e")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.v("HTTP/MeasureInsert", "Success to request, response: $responseBody")
                callback()

            }
        })
    }

    // 측정 결과 목록 가져오기
    suspend fun fetchMeasureResults7(myUrl: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("$myUrl/read.php")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.w("NetMeasure7", "Success to execute request: $responseBody")


            }
        }
    }
    // 가장 최신 측정 1개 가져오기
    suspend fun fetchMeasureResult(myUrl: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("$myUrl/read.php")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.w("NetMeasure7", "Success to execute request: $responseBody")

            }
        }
    }
}