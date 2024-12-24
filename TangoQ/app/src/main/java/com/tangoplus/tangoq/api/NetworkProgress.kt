package com.tangoplus.tangoq.api

import android.content.Context
import android.util.Log
import com.tangoplus.tangoq.vo.ProgressHistoryVO
import com.tangoplus.tangoq.vo.ProgressUnitVO
import com.tangoplus.tangoq.api.HttpClientProvider.getClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object NetworkProgress {

    fun postProgressInCurrentProgram(myUrl: String, bodySn : JSONObject, context: Context, callback: (MutableList<ProgressUnitVO>) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = bodySn.toString().toRequestBody(mediaType)
        val client = getClient(context)
        val request = Request.Builder()
            .url(myUrl)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string()
            Log.v("Post>Progress>inserOrSelect", "$responseBody")
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
                            videoDuration = ja.optJSONObject(i).optInt("duration"),
                            lastProgress = ja.optJSONObject(i).optInt("progress"),
                            isCompleted = ja.optJSONObject(i).optInt("completed"),
                            updateDate = ja.optJSONObject(i).optString("updated_at")
                        )
                        progresses.add(progressUnitVO)
                    }
                }
                callback(progresses)
            } catch (e: IndexOutOfBoundsException) {
                Log.e("ProgressIndex", "Post Progress: ${e.message}")
                callback(mutableListOf())
            } catch (e: IllegalArgumentException) {
                Log.e("ProgressIllegal", "Post Progress: ${e.message}")
                callback(mutableListOf())
            } catch (e: IllegalStateException) {
                Log.e("ProgressIllegal", "Post Progress: ${e.message}")
                callback(mutableListOf())
            }catch (e: NullPointerException) {
                Log.e("ProgressNull", "Post Progress: ${e.message}")
                callback(mutableListOf())
            } catch (e: java.lang.Exception) {
                Log.e("ProgressException", "Post Progress: ${e.message}")
                callback(mutableListOf())
            }
        }
    }

    // ------# 현재 프로그램 프로그레스만 가져오기 #------
//    fun getRecProgresses(myUrl: String, context: Context, recSn: Int, callback: (MutableList<ProgressUnitVO>) -> Unit) {
//        val client = getClient(context)
//        val request = Request.Builder()
//            .url("$myUrl?recommendation_sn=$recSn")
//            .get()
//            .build()
//
//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                Log.e("Token응답실패", "Failed to execute request")
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                val responseBody = response.body?.string()
////                Log.v("Server>Progress", "$responseBody")
//                try {
//                    val ja = JSONObject(responseBody.toString()).optJSONArray("data")
//                    val progresses = mutableListOf<ProgressUnitVO>()
//                    if (ja != null) {
//                        for (i in 0 until ja.length()) {
//                            val progressUnitVO = ProgressUnitVO(
//                                uvpSn = ja.optJSONObject(i).optInt("uvp_sn"),
//                                exerciseId = ja.optJSONObject(i).optInt("content_sn"),
//                                recommendationSn = ja.optJSONObject(i).optInt("recommendation_sn"),
//                                currentWeek = ja.optJSONObject(i).optInt("week_number"),
//                                currentSequence = ja.optJSONObject(i).optInt("count_set"),
//                                requiredSequence = ja.optJSONObject(i).optInt("required_set"),
//                                videoDuration = ja.optJSONObject(i).optInt("duration"),
//                                lastProgress = ja.optJSONObject(i).optInt("progress"),
//                                isCompleted = ja.optJSONObject(i).optInt("completed"),
//                                updateDate = ja.optJSONObject(i).optString("updated_at")
//
//                            )
//                            progresses.add(progressUnitVO)
//                        }
//                    }
//                    Log.v("진행길이", "getRecProgress: ${progresses.size}")
//                    callback(progresses)
//                } catch (e: IndexOutOfBoundsException) {
//                    Log.e("ProgressIndex", "getRecProgress: ${e.message}")
//                    callback(mutableListOf())
//                } catch (e: IllegalArgumentException) {
//                    Log.e("ProgressIllegal", "getRecProgress: ${e.message}")
//                    callback(mutableListOf())
//                } catch (e: IllegalStateException) {
//                    Log.e("ProgressIllegal", "getRecProgress: ${e.message}")
//                    callback(mutableListOf())
//                }catch (e: NullPointerException) {
//                    Log.e("ProgressNull", "getRecProgress: ${e.message}")
//                    callback(mutableListOf())
//                } catch (e: java.lang.Exception) {
//                    Log.e("ProgressException", "getRecProgress: ${e.message}")
//                    callback(mutableListOf())
//                }
//            }
//        })
//    }

    // 시청기록 1개 보내기 (서버에 저장)
    fun patchProgress1Item(myUrl: String, uvpSn: Int, bodyJo: JSONObject, context: Context, callback: (ProgressUnitVO) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = bodyJo.toString().toRequestBody(mediaType)
        val client = getClient(context)
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
                Log.v("patch>Progress1Item", "$responseBody")
                val jo = JSONObject(responseBody.toString()).optJSONObject("update_video_progress")
                if (jo != null) {
                    val progressUnitVO = ProgressUnitVO(
                        uvpSn = jo.optInt("uvp_sn"),
                        exerciseId = jo.optInt("content_sn"),
                        recommendationSn = jo.optInt("recommendation_sn"),
                        currentWeek = jo.optInt("week_number"),
                        currentSequence = jo.optInt("count_set"),
                        requiredSequence = jo.optInt("required_set"),
                        videoDuration = jo.optInt("duration"),
                        lastProgress = jo.optInt("progress"),
                        isCompleted = jo.optInt("completed"),
                        updateDate = jo.optString("updated_at")
                    )
                    callback(progressUnitVO)
                }
            }
        })
    }
    // 가장 최신 정보를 가져오는게 좋다.
    suspend fun getLatestProgress(myUrl: String, recSn: Int, context: Context) : JSONObject? {
        val client = getClient(context)
        val request = Request.Builder()
            .url("$myUrl?recommendation_sn=$recSn&latest_progress")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.v("Get>Recommend+Progress", "$responseBody")
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
                        return@use jo
                    } else {
                        return@use jo
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("ProgressIndex", "latestProgress: ${e.message}")
                null
            } catch (e: IllegalArgumentException) {
                Log.e("ProgressIllegal", "latestProgress: ${e.message}")
                null
            } catch (e: IllegalStateException) {
                Log.e("ProgressIllegal", "latestProgress: ${e.message}")
                null
            }catch (e: NullPointerException) {
                Log.e("ProgressNull", "latestProgress: ${e.message}")
                null
            } catch (e: java.lang.Exception) {
                Log.e("ProgressException", "latestProgress: ${e.message}")
                null
            }
        }
    }

    suspend fun getWeekProgress(myUrl: String, recSn: Int, week: Int,context: Context) : MutableList<ProgressUnitVO>? {
        val client = getClient(context)
        val request = Request.Builder()
            .url("$myUrl?recommendation_sn=$recSn&weeks=$week")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.v("Server>week>Progress", "$responseBody")
                    if (!response.isSuccessful) {
                        return@withContext null
                    }
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
                                videoDuration = ja.optJSONObject(i).optInt("duration"),
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
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("ProgressIndex", "getWeek: ${e.message}")
                null
            } catch (e: IllegalArgumentException) {
                Log.e("ProgressIllegal", "getWeek: ${e.message}")
                null
            } catch (e: IllegalStateException) {
                Log.e("ProgressIllegal", "getWeek: ${e.message}")
                null
            }catch (e: NullPointerException) {
                Log.e("ProgressNull", "getWeek: ${e.message}")
                null
            } catch (e: java.lang.Exception) {
                Log.e("ProgressException", "getWeek: ${e.message}")
                null
            }
        }
    }

    // TODO ------# 달력 함수에 맞게 수정 해야함 #------
    suspend fun getDailyProgress(myUrl: String, date: String, context: Context) : MutableList<ProgressHistoryVO>? {
        val client = getClient(context)
        val request = Request.Builder()
            .url("$myUrl?date=$date")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->

                    if (!response.isSuccessful) {
                        Log.e("DailyProgressFailed" ,"response is Null")
                        return@withContext null
                    }
                    val responseBody = response.body?.string()
                    Log.v("Server>Daily>Progress", "$responseBody")

                    val ja = responseBody?.let { JSONObject(it).optJSONArray("data") }
                    val progresses = mutableListOf<ProgressHistoryVO>()
                    if (ja != null) {
                        for (i in 0 until ja.length()) {
                            val item = ja.optJSONObject(i)
                            val progressHistory = ProgressHistoryVO(
                                sn = item.optInt("progress_history_sn"),
                                userSn = item.optInt("user_sn"),
                                uvpSn = item.optInt("uvp_sn"),
                                exerciseName = item.optString("exercise_name"),
                                recommendationTitle = item.optString("recommendation_title"),
                                weekNumber = item.optInt("week_number"),
                                executionDate = item.optString("execution_date"),
                                countSet = item.optInt("count_set"),
                                completed = item.optInt("completed"),
                                expired = item.optInt("expired"),
                            )
                            progresses.add(progressHistory)
                        }
                        Log.v("진행길이", "${progresses.size}")
                    }
                    progresses
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("ProgressIndex", "getDaily: ${e.message}")
                null
            } catch (e: IllegalArgumentException) {
                Log.e("ProgressIllegal", "getDaily: ${e.message}")
                null
            } catch (e: IllegalStateException) {
                Log.e("ProgressIllegal", "getDaily: ${e.message}")
                null
            } catch (e: NullPointerException) {
                Log.e("ProgressNull", "getDaily: ${e.message}")
                null
            } catch (e: java.lang.Exception) {
                Log.e("ProgressException", "getDaily: ${e.message}")
                null
            }
        }
    }
}