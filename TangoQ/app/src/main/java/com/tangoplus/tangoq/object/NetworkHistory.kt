package com.tangoplus.tangoq.`object`

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object NetworkHistory {

    // 현재 진행중인 프로그램의 history 가져오기
    suspend fun fetchCurrentProgramHistory(myUrl: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("$myUrl/read.php")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.w("NetHistory", "Success to execute request!: $responseBody")

                // TODO MainActivity의 history넣는 반복문을 여기서 사용
            }
        }
    }


    // 1년 간의 운동 기록 전부 가져오기
    suspend fun fetchExerciseHistoryByYear(myUrl: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("$myUrl/read.php/duration")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.w("NetHistory1Year", "Success to execute request: $responseBody")
            }
        }
    }
}