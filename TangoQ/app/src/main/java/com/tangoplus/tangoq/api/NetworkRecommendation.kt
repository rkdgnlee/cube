package com.tangoplus.tangoq.api

import android.content.Context
import android.util.Log
import com.tangoplus.tangoq.vo.RecommendationVO
import com.tangoplus.tangoq.function.SecurePreferencesManager.getEncryptedAccessJwt
import com.tangoplus.tangoq.api.HttpClientProvider.getClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject

object NetworkRecommendation {
    suspend fun createRecommendProgram(myUrl: String, jo: String, context: Context, callback: (MutableList<RecommendationVO>) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, jo)
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer ${getEncryptedAccessJwt(context)}")
                .build()
            chain.proceed(newRequest)
        }
        val client = getClient(context)
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
            } catch (e: IndexOutOfBoundsException) {
                Log.e("RecommendIndex", "${e.message}")
                withContext(Dispatchers.Main) {
                    callback(mutableListOf())
                }
            } catch (e: IllegalArgumentException) {
                Log.e("RecommendIllegal", "${e.message}")
                withContext(Dispatchers.Main) {
                    callback(mutableListOf())
                }
            } catch (e: IllegalStateException) {
                Log.e("RecommendIllegal", "${e.message}")
                withContext(Dispatchers.Main) {
                    callback(mutableListOf())
                }
            } catch (e: NullPointerException) {
                Log.e("RecommendNull", "${e.message}")
                withContext(Dispatchers.Main) {
                    callback(mutableListOf())
                }
            } catch (e: java.lang.Exception) {
                Log.e("RecommendException", "${e.message}")
                withContext(Dispatchers.Main) {
                    callback(mutableListOf())
                }
            }
        }
    }

    suspend fun getRecommendProgram(myUrl: String, context: Context) : MutableList<RecommendationVO> {
        val client = getClient(context)
        val request = Request.Builder()
            .url(myUrl)
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.v("Get>Recommendation", "$responseBody")
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
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("RecommendIndex", "${e.message}")
                mutableListOf()
            } catch (e: IllegalArgumentException) {
                Log.e("RecommendIllegal", "${e.message}")
                mutableListOf()
            } catch (e: IllegalStateException) {
                Log.e("RecommendIllegal", "${e.message}")
                mutableListOf()
            } catch (e: NullPointerException) {
                Log.e("RecommendNull", "${e.message}")
                mutableListOf()
            } catch (e: java.lang.Exception) {
                Log.e("RecommendException", "${e.message}")
                mutableListOf()
            }

        }
    }

    suspend fun getRecommendationInOneMeasure(myUrl: String, context: Context, measureInfoSn: Int) : MutableList<RecommendationVO> {
        val client = getClient(context)
        val request = Request.Builder()
            .url("${myUrl}?measure_sn=$measureInfoSn")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.v("Get>Recommendation", "$responseBody")
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
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("RecommendIndex", "${e.message}")
                mutableListOf()
            } catch (e: IllegalArgumentException) {
                Log.e("RecommendIllegal", "${e.message}")
                mutableListOf()
            } catch (e: IllegalStateException) {
                Log.e("RecommendIllegal", "${e.message}")
                mutableListOf()
            } catch (e: NullPointerException) {
                Log.e("RecommendNull", "${e.message}")
                mutableListOf()
            } catch (e: java.lang.Exception) {
                Log.e("RecommendException", "${e.message}")
                mutableListOf()
            }

        }
    }

}