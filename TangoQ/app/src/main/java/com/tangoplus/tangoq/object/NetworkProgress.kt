package com.tangoplus.tangoq.`object`

import android.content.Context
import android.util.Log
import com.tangoplus.tangoq.data.ProgressUnitVO
import com.tangoplus.tangoq.data.ProgressVO
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
import org.json.JSONObject
import java.io.IOException

object NetworkProgress {

    fun postProgressInCurrentProgram(myUrl: String, bodySn : JSONObject, context: Context, callback: (MutableList<ProgressUnitVO>) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, bodySn.toString())
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

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Token응답실패", "Failed to execute request")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.v("Server>Progress", "${responseBody}")
                try {
                    val ja = JSONObject(responseBody.toString()).optJSONArray("data")
                    val progresses = mutableListOf<ProgressUnitVO>()
                    if (ja != null) {
                        for (i in 0 until ja.length()) {
                            val progressUnitVO = ProgressUnitVO(
                                uvpSn = ja.optJSONObject(i).optInt("uvp_sn"),
                                exerciseId = ja.optJSONObject(i).optInt("content_sn"),
                                recommendationSn = ja.optJSONObject(i).optInt("recommendation_sn"),
                                currentWeek = ja.optJSONObject(i).optInt("week_number"),
                                currentSequence = ja.optJSONObject(i).optInt("count_set"),
                                requiredSequence = ja.optJSONObject(i).optInt("required_set"),
                                videoDuration = ja.optJSONObject(i).optInt("video_duration"),
                                lastProgress = ja.optJSONObject(i).optInt("progress"),
                                isCompleted = ja.optJSONObject(i).optInt("completed"),
                                updateDate = ja.optJSONObject(i).optString("updated_at")
                            )
                            progresses.add(progressUnitVO)
                        }
                    }
                    callback(progresses)
                } catch (e: Exception) {
                    Log.e("JSON Parsing Error", "Error parsing JSON: ${e.message}")
                }
            }
        })
    }

    fun getProgressInCurrentProgram(myUrl: String, context: Context, callback: (MutableList<ProgressUnitVO>) -> Unit) {

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

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Token응답실패", "Failed to execute request")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.v("Server>Progress", "${responseBody?.substring(0, 20)}")
                try {
                    val ja = JSONObject(responseBody.toString()).optJSONArray("data")
                    val progresses = mutableListOf<ProgressUnitVO>()
                    if (ja != null) {
                        for (i in 0 until ja.length()) {
                            val progressUnitVO = ProgressUnitVO(
                                uvpSn = ja.optJSONObject(i).optInt("uvp_sn"),
                                exerciseId = ja.optJSONObject(i).optInt("content_sn"),
                                recommendationSn = ja.optJSONObject(i).optInt("recommendation_sn"),
                                currentWeek = ja.optJSONObject(i).optInt("week_number"),
                                currentSequence = ja.optJSONObject(i).optInt("count_set"),
                                requiredSequence = ja.optJSONObject(i).optInt("required_set"),
                                videoDuration = ja.optJSONObject(i).optInt("video_duration"),
                                lastProgress = ja.optJSONObject(i).optInt("progress"),
                                isCompleted = ja.optJSONObject(i).optInt("completed"),
                                updateDate = ja.optJSONObject(i).optString("updated_at")

                            )
                            progresses.add(progressUnitVO)
                        }
                    }
                    Log.v("진행길이", "${progresses.size}")
                    callback(progresses)
                } catch (e: Exception) {
                    Log.e("JSON Parsing Error", "Error parsing JSON: ${e.message}")
                }
            }
        })
    }

    // 시청기록 1개 보내기 (서버에 저장)
    fun patchProgress1Item(myUrl: String, uvpSn: Int, body: JSONObject, context: Context, callback: (ProgressUnitVO) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, body.toString())
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
            .url("$myUrl/$uvpSn")
            .patch(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Token응답실패", "Failed to execute request")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.v("Server>Progress", "$responseBody")
                val jo = JSONObject(responseBody.toString()).optJSONObject("update_video_progress")
                if (jo != null) {
                    val progressUnitVO = ProgressUnitVO(
                        uvpSn = jo.optInt("uvp_sn"),
                        exerciseId = jo.optInt("content_sn"),
                        recommendationSn = jo.optInt("recommendation_sn"),
                        currentWeek = jo.optInt("week_number"),
                        currentSequence = jo.optInt("count_set"),
                        requiredSequence = jo.optInt("required_set"),
                        videoDuration = jo.optInt("video_duration"),
                        lastProgress = jo.optInt("progress"),
                        isCompleted = jo.optInt("completed"),
                        updateDate = jo.optString("updated_at")
                    )
                    callback(progressUnitVO)
                }
            }
        })
    }

    fun getProgressAtTime(myUrl: String, date: String, context: Context, callback: () -> Unit) {
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
            .url("$myUrl?date={$date}")
            .get()
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Token응답실패", "Failed to execute request")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.v("Server>Progress", "$responseBody")
            }
        })
    }

    // 가장 최신 정보를 가져오는게 좋다.
    suspend fun getLatestProgress(myUrl: String, recSn: Int, context: Context) : JSONObject {
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
            .url("$myUrl?recommendation_sn=$recSn&latest_progress")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.v("Get>Recommend+Progress", "$responseBody")

                try {
                    val dataJson = JSONObject(responseBody.toString())
                    val progressJo = dataJson.optJSONObject("progress_data")
                    val programJo = dataJson.optJSONObject("program_data")
                    val jo = JSONObject()
                    if (programJo != null && progressJo != null) {
                        jo.put("uvp_sn", progressJo.optInt("uvp_sn"))
                        jo.put("recommendation_sn", progressJo.optInt("recommendation_sn"))
                        jo.put("count_set", progressJo.optInt("count_set"))
                        jo.put("progress", progressJo.optInt("progress"))
                        jo.put("week_number", progressJo.optInt("week_number"))
                        jo.put("measure_sn", progressJo.optInt("measure_sn"))
                        jo.put("exercise_program_sn", programJo.optInt("exercise_program_sn"))
                    } else {
                        JSONObject()
                    }
                } catch (e: Exception) {
                    Log.e("JSON Parsing Error", "Error parsing JSON: ${e.message}")
                }
            } as JSONObject
        }
    }

    suspend fun getWeekProgress(myUrl: String, recSn: Int, week: Int,context: Context) : MutableList<ProgressUnitVO> {

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
            .url("$myUrl?recommendation_sn=$recSn&weeks=$week")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.v("Server>week>Progress", "${responseBody}")
                try {
                    val ja = JSONObject(responseBody.toString()).optJSONArray("data")
                    val progresses = mutableListOf<ProgressUnitVO>()
                    if (ja != null) {
                        for (i in 0 until ja.length()) {
                            val progressUnitVO = ProgressUnitVO(
                                uvpSn = ja.optJSONObject(i).optInt("uvp_sn"),
                                exerciseId = ja.optJSONObject(i).optInt("content_sn"),
                                recommendationSn = ja.optJSONObject(i).optInt("recommendation_sn"),
                                currentWeek = ja.optJSONObject(i).optInt("week_number"),
                                currentSequence = ja.optJSONObject(i).optInt("count_set"),
                                requiredSequence = ja.optJSONObject(i).optInt("required_set"),
                                videoDuration = ja.optJSONObject(i).optInt("video_duration"),
                                lastProgress = ja.optJSONObject(i).optInt("progress"),
                                isCompleted = ja.optJSONObject(i).optInt("completed"),
                                updateDate = ja.optJSONObject(i).optString("updated_at")

                            )
                            progresses.add(progressUnitVO)
                        }
                        Log.v("진행길이", "${progresses.size}")
                        return@use progresses
                    } else {
                        return@use progresses
                    }
                } catch (e: Exception) {
                    Log.e("JSON Parsing Error", "Error parsing JSON: ${e.message}")
                }
            } as MutableList<ProgressUnitVO>
        }
    }
}