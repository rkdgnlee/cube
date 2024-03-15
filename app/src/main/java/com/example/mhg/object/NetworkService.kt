package com.example.mhg.`object`

import android.content.ContentValues.TAG
import android.util.Log
import com.google.firebase.inject.Deferred
import com.google.gson.JsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object NetworkService{
    // TODO 매니저님이 짜준 로직 대로, JSON, METHOD 바꿔야함
    fun fetchINSERTJson(myUrl : String, json: String, callback: () -> Unit){
        val client = OkHttpClient()
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)
        val request = Request.Builder()
            .url("${myUrl}create.php")
            .post(body) // post방식으로 insert 들어감
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("$TAG, 응답실패", "Failed to execute request!")
            }
            override fun onResponse(call: Call, response: Response)  {
                val responseBody = response.body?.string()
                Log.e("$TAG, 응답성공", "$responseBody")
                callback()
            }
        })
    }
    fun fetchUPDATEJson(myUrl : String, json: String, user_mobile:String, callback: () -> Unit) {
        val client = OkHttpClient()
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)
        val request = Request.Builder()
            .url("${myUrl}update.php?user_mobile=$user_mobile")
            .patch(body)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("$TAG, 응답실패", "Failed to execute request!")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.e("$TAG, 응답성공", "$responseBody")
                callback()
            }
        })
    }

    fun fetchDeleteJson(myUrl : String, user_mobile:String, callback: () -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}delete.php?user_mobile=$user_mobile")
            .delete()
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("$TAG, 응답실패", "Failed to execute request!")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.e("$TAG, 응답성공", "$responseBody")
                callback()
            }
        })
    }

    suspend fun fetchExerciseJson(myUrl: String): JSONArray? {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}read.php")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.e("OKHTTP3/ExerciseFetch", "Success to execute request!: $responseBody")
                val jsonObj__ = responseBody?.let { JSONObject(it) }

                jsonObj__?.optJSONArray("data")

            }
        }
    }

}