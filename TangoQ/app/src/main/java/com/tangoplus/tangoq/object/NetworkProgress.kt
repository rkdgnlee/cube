package com.tangoplus.tangoq.`object`

import android.content.Context
import android.util.Log
import com.tangoplus.tangoq.data.ProgressUnitVO
import com.tangoplus.tangoq.data.ProgressVO
import com.tangoplus.tangoq.data.RecommendationVO
import com.tangoplus.tangoq.db.SecurePreferencesManager.getEncryptedJwtToken
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
                Log.v("Server>Progress", "${responseBody?.substring(0, 255)}")
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
//    // 시청기록 1개 얻기
//    suspend fun getProgress1Item(myUrl: String, uvpSn: Int, progress: Int,context: Context,) {
//        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
//        val body = RequestBody.create(mediaType, progress.toString())
//        val authInterceptor = Interceptor { chain ->
//            val originalRequest = chain.request()
//            val newRequest = originalRequest.newBuilder()
//                .header("Authorization", "Bearer ${getEncryptedJwtToken(context)}")
//                .build()
//            chain.proceed(newRequest)
//        }
//        val client = OkHttpClient.Builder()
//            .addInterceptor(authInterceptor)
//            .build()
//        val request = Request.Builder()
//            .url("$myUrl/$uvpSn")
//            .patch(body)
//            .build()
//
//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                Log.e("Token응답실패", "Failed to execute request")
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                val responseBody = response.body?.string()?.substringAfter("response: ")
//                Log.e("Server>Progress", "$responseBody")
//
//                try {
//                    val jsonObjects = responseBody?.split("}{")
//
//                    val dataJson = "{${jsonObjects?.get(1)}}"
//                    val jo = JSONObject(dataJson)
//
//                    val uvpSn = jo.getInt("uvp_sn")
//                    val userSn = jo.getInt("user_sn")
//                    val recommendSn = jo.getInt("recommend_sn")
//                    val videoDuration = jo.getInt("video_duration")
//
//                } catch (e: Exception) {
//                    Log.e("JSON Parsing Error", "Error parsing JSON: ${e.message}")
//                }
//            }
//        })
//    }


    // 시청기록 1개 보내기 (서버에 저장)
    fun patchProgress1Item(myUrl: String, uvpSn: Int, progress: Int, context: Context) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, progress.toString())
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
                val responseBody = response.body?.string()?.substringAfter("response: ")
                Log.v("Server>Progress", "$responseBody")
            }
        })
    }


    /* 자 그러면 나는 그냥 second만 넣어서 보내는데? body에 관련된 sn을 3개 넣어서 보냄.
    *  1. history에 관한 data array가 나왔을 떄, 이걸 어디다가 저장헤서 사용할지? singleton?
    *  2. measure는 singleton에 담김. 기록들을 받아와서 바로 singletonMeasure에 담김.
    *  3. 그럼 여기서  history도? singleton에 담는게 좋을 것 같은데? VM에 담았을 경우 -> 보고 나오면 시청기록 사라져있음 -> 다시 api 사용 -> 다시 저장
    *
    *
    *
    * */
}