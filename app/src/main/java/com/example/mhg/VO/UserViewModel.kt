package com.example.mhg.VO

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException

class UserViewModel: ViewModel() {
    val User = MutableLiveData<UserVO>()



    fun fetchJson(myUrl : String, json: String, requestType: String)   {
        val client = OkHttpClient()
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)
        val request: Request
        request = when {
            requestType == "POST" -> Request.Builder().url(myUrl).post(body).build()
            requestType == "PUT" -> Request.Builder().url(myUrl).put(body).build()
            requestType == "DELETE" -> Request.Builder().url(myUrl).delete(body).build()
            else -> Request.Builder().url(myUrl).post(body).build()
        }
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OKHTTP3", "Failed to execute request!")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                val gson = Gson()
                Log.e("OKHTTP3", "Success to execute request! $body")
                // ----- 응답 받은 UserVO형식의 데이터 전처리 시작 -----
                val userVO: UserVO = gson.fromJson(responseBody, UserVO::class.java)
                User.postValue(userVO)
                Log.e("ViewModel", User.value.toString())
                // ----- 응답 받은 UserVO형식의 데이터 전처리 끝 -----
            }
        }
        )
    }
}