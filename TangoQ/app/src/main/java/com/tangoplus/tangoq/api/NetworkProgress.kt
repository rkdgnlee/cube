package com.tangoplus.tangoq.api

import android.content.Context
import android.util.Log
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.vo.ProgressHistoryVO
import com.tangoplus.tangoq.vo.ProgressUnitVO
import com.tangoplus.tangoq.api.HttpClientProvider.getClient
import com.tangoplus.tangoq.vo.ExerciseVO
import com.tangoplus.tangoq.vo.ProgramVO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object NetworkProgress {

    suspend fun postProgressInCurrentProgram(myUrl: String, sns: Triple<Int, Int, Int>, context: Context, callback: (MutableList<Float>, MutableList<ProgressUnitVO>) -> Unit) {
        val client = getClient(context)
        val request = Request.Builder()
            .url("${myUrl}?recommendation_sn=${sns.first}&weeks=${sns.second}&cycles=${sns.third}")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
//                     Log.v("포스트프로그램싸이클", "$responseBody")
                    val bodyJson = JSONObject(responseBody.toString())
                    val ja = bodyJson.optJSONArray("data")
                    val pv3 = bodyJson.optJSONObject("sum_data_for_each_cycle")
                    val pv3s = mutableListOf<Float>()
                    if (pv3 != null) {
                        for ( i in 0 until pv3.length()) {
                            val jo = pv3.optJSONObject("total_sum_for_cycle_${i+1}")
                            val duration = jo?.optInt("total_duration") ?: 1
                            val progress = jo?.optInt("total_progress") ?: 0

                            val percent = if (duration > 0) {
                                ( progress.toFloat() * 100 ) / duration.toFloat()
                            } else {
                                0f
                            }

                            // Log.v("총,진행시간", "$duration, $progress, percent: $percent")
                            pv3s.add(percent)
                        }
                    }

                    val progresses = mutableListOf<ProgressUnitVO>()
                    if (ja != null) {
                        for (i in 0 until ja.length()) {
                            val jo = ja.optJSONObject(i)
                            val progressUnitVO = ProgressUnitVO(
                                uvpSn = jo.optInt("uvp_sn"),
                                userSn = jo.optInt("user_sn"),
                                exerciseId = jo.optInt("content_sn"),
                                recommendationSn = jo.optInt("recommendation_sn"),
                                weekNumber = jo.optInt("week_number"),
                                weekStartAt = jo.optString("week_start_at"),
                                weekEndAt = jo.optString("week_end_at"),
                                countSet = jo.optInt("count_set"),
                                requiredSet = jo.optInt("required_set"),
                                duration = jo.optInt("duration"),
                                progress = jo.optInt("progress"),
                                updatedAt = jo.optString("updated_at") ?: "",
                                isWatched = jo.optInt("is_watched"),
                                cycleProgress = jo.optInt("cycle_progress")
                            )
//                            Log.v("seq,week계산하기", "$i $progressUnitVO")
                            progresses.add(progressUnitVO)
                        }
                    }
                    callback(pv3s, progresses)
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("ProgressIndex", "Post Progress: ${e.message}")
                callback(mutableListOf(), mutableListOf())
            } catch (e: IllegalArgumentException) {
                Log.e("ProgressIllegal", "Post Progress: ${e.message}")
                callback(mutableListOf(), mutableListOf())
            } catch (e: IllegalStateException) {
                Log.e("ProgressIllegal", "Post Progress: ${e.message}")
                callback(mutableListOf(), mutableListOf())
            }catch (e: NullPointerException) {
                Log.e("ProgressNull", "Post Progress: ${e.message}")
                callback(mutableListOf(), mutableListOf())
            } catch (e: java.lang.Exception) {
                Log.e("ProgressException", "Post Progress: ${e.message}")
                callback(mutableListOf(), mutableListOf())
            }
        }
    }

    // ------# 현재 프로그램 프로그레스만 가져오기 #------

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
//                 Log.v("patch>Progress1Item", "$responseBody")
                val jo = JSONObject(responseBody.toString()).optJSONObject("update_video_progress")
                if (jo != null) {
                    val progressUnitVO = ProgressUnitVO(
                        uvpSn = jo.optInt("uvp_sn"),
                        userSn = jo.optInt("user_sn"),
                        exerciseId = jo.optInt("content_sn"),
                        recommendationSn = jo.optInt("recommendation_sn"),
                        weekNumber = jo.optInt("week_number"),
                        weekStartAt = jo.optString("week_start_at"),
                        weekEndAt = jo.optString("week_end_at"),
                        countSet = jo.optInt("count_set"),
                        requiredSet = jo.optInt("required_set"),
                        duration = jo.optInt("duration"),
                        progress = jo.optInt("progress"),
                        updatedAt = jo.optString("updated_at"),
                        isWatched = jo.optInt("is_watched")
                    )
                    callback(progressUnitVO)
                }
            }
        })
    }
    // 가장 최신 정보를 가져오는게 좋다.
    suspend fun getProgress(myUrl: String, bodySn : JSONObject, context: Context, callback: (Triple<Int, Int, Int>, List<List<ProgressUnitVO>>) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = bodySn.toString().toRequestBody(mediaType)
        val client = getClient(context)
        val request = Request.Builder()
            .url(myUrl)
            .post(body)
            .build()
        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    val bodyJson = JSONObject(responseBody.toString())
                    val latest = bodyJson.optJSONObject("latest")
                    val progressHistorySn = latest?.optInt("progress_history_sn")
                    val currentWeek = latest?.optInt("week_number")
                    val currentCycle = latest?.optInt("cycle")
//                    Log.v("포스트getProgress", "$latest, $progressHistorySn, $currentWeek, $currentCycle")
                    val result = mutableListOf<ProgressUnitVO>()
                    val exerciseCount = bodyJson.optInt("row_count") / 4
                    val data = bodyJson.optJSONArray("data")
                    if (data != null) {
                        for (i in 0 until data.length()) {
                            val jo = data.optJSONObject(i)
                            if (jo != null) {
                                val progressUnitVO = ProgressUnitVO(
                                    uvpSn = jo.optInt("uvp_sn"),
                                    userSn = jo.optInt("user_sn"),
                                    exerciseId = jo.optInt("content_sn"),
                                    recommendationSn = jo.optInt("recommendation_sn"),
                                    weekNumber = jo.optInt("week_number"),
                                    weekStartAt = jo.optString("week_start_at"),
                                    weekEndAt = jo.optString("week_end_at"),
                                    countSet = jo.optInt("count_set"),
                                    requiredSet = jo.optInt("required_set"),
                                    duration = jo.optInt("duration"),
                                    progress = jo.optInt("progress"),
                                    updatedAt = jo.optString("updated_at"),
                                    isWatched = jo.optInt("is_watched")
                                )
//                                Log.v("seq,week계산하기", "$i $progressUnitVO")
                                result.add(progressUnitVO)
                            }
                        }
                        if (result.isNotEmpty()) {
                            val chunckedResult = result.chunked(exerciseCount)
                            callback(
                                Triple(
                                    progressHistorySn ?: -1,
                                    currentWeek ?: -1,
                                    currentCycle ?: -1
                                ),
                                chunckedResult
                            )
                        }
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("ProgressIndex", "Post Progress(latest): ${e.message}")
                callback(Triple(-1, -1, -1), mutableListOf())
            } catch (e: IllegalArgumentException) {
                Log.e("ProgressIllegal", "Post Progress(latest): ${e.message}")
                callback(Triple(-1, -1, -1), mutableListOf())
            } catch (e: IllegalStateException) {
                Log.e("ProgressIllegal", "Post Progress(latest): ${e.message}")
                callback(Triple(-1, -1, -1), mutableListOf())
            } catch (e: NullPointerException) {
                Log.e("ProgressNull", "Post Progress(latest): ${e.message}")
                callback(Triple(-1, -1, -1), mutableListOf())
            } catch (e: java.lang.Exception) {
                Log.e("ProgressException", "Post Progress(latest): ${e.message}")
                callback(Triple(-1, -1, -1), mutableListOf())
            }
        }
    }
    suspend fun getLatestProgresses(myUrl: String, context: Context) : Pair<MutableList<ProgressUnitVO>, ProgramVO>? {
        val client = getClient(context)
        val request = Request.Builder()
            .url("$myUrl?latest_progress")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    // Log.v("latestProgress", "$responseBody")
                    val dataJson = JSONObject(responseBody.toString())

                    // 프로그레스, 프로그램
                    val progressJa = dataJson.optJSONArray("progress_data")
                    val progressUnits = mutableListOf<ProgressUnitVO>()
                    if (progressJa != null) {
                        for (i in 0 until progressJa.length()) {
                            val jo = progressJa.optJSONObject(i)
                            val progressUnitVO = ProgressUnitVO(
                                uvpSn = jo.optInt("uvp_sn"),
                                userSn = jo.optInt("user_sn"),
                                exerciseId = jo.optInt("content_sn"),
                                recommendationSn = jo.optInt("recommendation_sn"),
                                weekNumber = jo.optInt("week_number"),
                                weekStartAt = jo.optString("week_start_at"),
                                weekEndAt = jo.optString("week_end_at"),
                                countSet = jo.optInt("count_set"),
                                requiredSet = jo.optInt("required_set"),
                                duration = jo.optInt("duration"),
                                progress = jo.optInt("progress"),
                                updatedAt = jo.optString("updated_at"),
                                isWatched = jo.optInt("is_watched")
                            )
                            progressUnits.add(progressUnitVO)
                        }
                    }
                    val exerciseJo = dataJson.optJSONArray("exercise_data")
                    val exerciseList = mutableListOf<ExerciseVO>()
                    if (exerciseJo != null) {
                        for (i in 0 until exerciseJo.length()) {
                            val uvpUnit = exerciseJo.optJSONObject(i)
                            val exerciseData = ExerciseVO(
                                exerciseId = uvpUnit.optString("exercise_id"),
                                exerciseName = uvpUnit.optString("exercise_name"),
                                exerciseTypeId = uvpUnit.getString("exercise_type_id"),
                                exerciseTypeName = uvpUnit.getString("exercise_type_name"),
                                exerciseCategoryId = uvpUnit.getString("exercise_category_id"),
                                exerciseCategoryName = uvpUnit.getString("exercise_category_name"),
                                relatedJoint = uvpUnit.getString("related_joint"),
                                relatedMuscle = uvpUnit.getString("related_muscle"),
                                relatedSymptom = uvpUnit.getString("related_symptom"),
                                exerciseStage = uvpUnit.getString("exercise_stage"),
                                exerciseFrequency = uvpUnit.getString("exercise_frequency"),
                                exerciseIntensity = uvpUnit.getString("exercise_intensity"),
                                exerciseInitialPosture = uvpUnit.getString("exercise_initial_posture"),
                                exerciseMethod = uvpUnit.getString("exercise_method"),
                                exerciseCaution = uvpUnit.getString("exercise_caution"),
                                videoFilepath = uvpUnit.getString("video_filepath"),
                                duration = (uvpUnit.optString("duration").toIntOrNull() ?: 0).toString(),
                                imageFilePath = uvpUnit.getString("image_filepath"),
                            )
                            exerciseList.add(exerciseData)
                        }
                    }
                    val pjo = dataJson.optJSONObject("execise_program_data")
                    if (pjo != null) {
                        val programVO = ProgramVO(
                            programSn = pjo.optInt("exercise_program_sn"),
                            programName = pjo.optString("exercise_program_title"),
                            programFrequency = pjo.optInt("exercise_frequency"),
                            programWeek = pjo.optInt("required_week"),
                            programStage = pjo.optString("exercise_stage"),
                            exercises = exerciseList
                        )
                        return@use Pair(progressUnits, programVO)
                    }
                    null
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

    suspend fun getWeekProgress(myUrl: String, context: Context) : MutableList<Pair<String, Int>>? {
        val client = getClient(context)
        val request = Request.Builder()
            .url("$myUrl?week_progress")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    // Log.v("Server>week>Progress", "$responseBody")
                    if (!response.isSuccessful) {
                        return@withContext null
                    }
                    val ja = JSONArray(responseBody.toString())
                    val weekCounts = mutableListOf<Pair<String, Int>>()
                    for (i in 0 until ja.length()) {
                        val unitJo = ja.optJSONObject(i)
                        val executionDate = unitJo.optString("execution_date") ?: ""
                        val totalCountSet = unitJo.optInt("total_count_set") ?: 0
                        val unit = Pair(executionDate, totalCountSet)
                        weekCounts.add(unit)
                    }
                    return@use weekCounts

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

    //  ------# 달력 함수에 맞게 수정 해야함 #------
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
                    // Log.v("Server>Daily>Progress", "$responseBody")

                    val ja = responseBody?.let { JSONObject(it).optJSONArray("data") }
                    val progresses = mutableListOf<ProgressHistoryVO>()
                    if (ja != null) {
                        for (i in 0 until ja.length()) {
                            val item = ja.optJSONObject(i)
                            val progressHistory = ProgressHistoryVO(
                                sn = item.optInt("progress_history_sn"),
                                userSn = item.optInt("user_sn"),
                                uvpSn = item.optInt("uvp_sn"),
                                contentSn = item.optInt("content_sn"),
                                recommendationSn = item.optInt("recommendation_sn"),
                                serverSn =  item.optInt("serverSn"),
                                exerciseName = item.optString("exercise_name"),
                                duration = item.optInt("duration"),
                                imageFilePathReal = item.optString("image_filepath"),
                                recommendationTitle = item.optString("recommendation_title"),
                                weekNumber = item.optInt("week_number"),
                                executionDate = item.optString("execution_date"),
                                countSet = item.optInt("count_set"),
                                completed = item.optInt("completed"),
                                expired = item.optInt("expired"),
                                createdAt = item.optString("created_at")
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

    suspend fun getMonthProgress(myUrl: String, month: String, context: Context) : MutableSet<String>? {
        val client = getClient(context)
        val request = Request.Builder()
            .url("$myUrl?month=$month")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->

                if (!response.isSuccessful) {
                    Log.e("MonthProgressFailed" ,"response is Null")
                    return@withContext null
                }

                val responseBody = response.body?.string()
                try {
                    val bodyJa = responseBody?.let { JSONObject(it).optJSONArray("data") }
                    val result = mutableSetOf<String>()
                    if (bodyJa != null) {
                        for (i in 0 until bodyJa.length()) {
                            val dateString = bodyJa.getJSONObject(i).optString("completed_watch_at")
                            result.add(dateString)
                        }
                    }
                    Log.v("getMonthProgress", "$result")
                    return@use result
                } catch (e: IndexOutOfBoundsException) {
                    Log.e("ProgressIndex", "MonthProgressFailed: ${e.message}")
                    mutableSetOf()
                } catch (e: IllegalArgumentException) {
                    Log.e("ProgressIllegal", "MonthProgressFailed: ${e.message}")
                    mutableSetOf()
                } catch (e: IllegalStateException) {
                    Log.e("ProgressIllegal", "MonthProgressFailed: ${e.message}")
                    mutableSetOf()
                } catch (e: NullPointerException) {
                    Log.e("ProgressNull", "MonthProgressFailed: ${e.message}")
                    mutableSetOf()
                } catch (e: java.lang.Exception) {
                    Log.e("ProgressException", "MonthProgressFailed: ${e.message}")
                    mutableSetOf()
                }

            }
        }
    }
}