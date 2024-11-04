package com.tangoplus.tangoq.`object`

import android.content.Context
import android.util.Log
import com.tangoplus.tangoq.data.ProgramVO
import com.tangoplus.tangoq.data.ProgressUnitVO
import com.tangoplus.tangoq.data.RecommendationVO
import com.tangoplus.tangoq.db.SecurePreferencesManager.getEncryptedJwtToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object NetworkRecommendation {
    suspend fun createRecommendProgram(myUrl: String, jo: String, context: Context, callback: (MutableList<RecommendationVO>) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, jo)
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer ${getEncryptedJwtToken(context)}")
                .build()
            chain.proceed(newRequest)
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
        val request = Request.Builder()
            .url(myUrl)
            .post(body)
            .build()

        withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.e("Create>Recommendation", "$responseBody")

                    val recommendations = mutableListOf<RecommendationVO>()
                    responseBody?.let {
                        val dataJson = JSONObject(it)
                        val ja = dataJson.optJSONObject("data")?.optJSONArray("data")
                        if (ja != null) {
                            for (i in 0 until ja.length()) {
                                val recommendationVO = RecommendationVO(
                                    recommendationSn = ja.optJSONObject(i).optInt("recommendation_sn"),
                                    serverSn = ja.optJSONObject(i).optInt("server_sn"),
                                    userSn = ja.optJSONObject(i).optInt("user_sn"),
                                    programSn = ja.optJSONObject(i).optInt("exercise_program_sn"),
                                    title = ja.optJSONObject(i).optString("recommendation_title"),
                                    regDate = ja.optJSONObject(i).optString("created_at")
                                )
                                recommendations.add(recommendationVO)
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        callback(recommendations)
                    }
                }
            } catch (e: Exception) {
                Log.e("JSON Parsing Error", "Error parsing JSON: ${e.message}")
                withContext(Dispatchers.Main) {
                    callback(mutableListOf())
                }
            }
        }
    }

    suspend fun getRecommendProgram(myUrl: String, context: Context) : MutableList<RecommendationVO> {
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer ${getEncryptedJwtToken(context)}")
                .build()
            chain.proceed(newRequest)
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
        val request = Request.Builder()
            .url(myUrl)
            .get()
            .build()


        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.v("Get>Recommendation", "$responseBody")

                try {
                    val dataJson = JSONObject(responseBody.toString())
                    val ja = dataJson.optJSONArray("data")
                    val recommendations = mutableListOf<RecommendationVO>()
                    if (ja != null) {

                        for (i in 0 until ja.length()) {
                            val recommendationVO = RecommendationVO(
                                recommendationSn = ja.optJSONObject(i).optInt("recommendation_sn"),
                                serverSn = ja.optJSONObject(i).optInt("server_sn"),
                                userSn = ja.optJSONObject(i).optInt("user_sn"),
                                programSn = ja.optJSONObject(i).optInt("exercise_program_sn"),
                                title = ja.optJSONObject(i).optString("recommendation_title"),
                                regDate = ja.optJSONObject(i).optString("created_at")
                            )
                            recommendations.add(recommendationVO)
                        }
                        return@use recommendations
                    } else {
                        return@use recommendations
                    }

                } catch (e: Exception) {
                    Log.e("JSON Parsing Error", "Error parsing JSON: ${e.message}")
                }
            } as MutableList<RecommendationVO>
        }
    }

    suspend fun getRecommendationInOneMeasure(myUrl: String, context: Context, measureInfoSn: Int) : MutableList<RecommendationVO> {
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer ${getEncryptedJwtToken(context)}")
                .build()
            chain.proceed(newRequest)
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
        val request = Request.Builder()
            .url("${myUrl}?measure_sn=$measureInfoSn")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.v("Get>Recommendation", "$responseBody")

                try {
                    val dataJson = JSONObject(responseBody.toString())
                    val ja = dataJson.optJSONArray("data")
                    val recommendations = mutableListOf<RecommendationVO>()
                    if (ja != null) {

                        for (i in 0 until ja.length()) {
                            val recommendationVO = RecommendationVO(
                                recommendationSn = ja.optJSONObject(i).optInt("recommendation_sn"),
                                serverSn = ja.optJSONObject(i).optInt("server_sn"),
                                userSn = ja.optJSONObject(i).optInt("user_sn"),
                                programSn = ja.optJSONObject(i).optInt("exercise_program_sn"),
                                title = ja.optJSONObject(i).optString("recommendation_title"),
                                regDate = ja.optJSONObject(i).optString("created_at")
                            )
                            recommendations.add(recommendationVO)

                        }
                        return@use recommendations
                    } else {
                        return@use recommendations
                    }

                } catch (e: Exception) {
                    Log.e("JSON Parsing Error", "Error parsing JSON: ${e.message}")
                }
            } as MutableList<RecommendationVO>
        }
    }

}