package com.tangoplus.tangoq.api

import android.content.Context
import android.util.Log
import com.tangoplus.tangoq.api.HttpClientProvider.getClient
import com.tangoplus.tangoq.vo.ExerciseHistoryVO
import com.tangoplus.tangoq.vo.ExerciseVO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException

object  NetworkExercise {

    // ------! exercise1개 가져오기 !------
    suspend fun fetchExerciseById(myUrl: String, exerciseId : String) : ExerciseVO? {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}/$exerciseId")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()

                    val jo = JSONObject(responseBody.toString())

                    val exerciseInstance = ExerciseVO(
                        exerciseId = jo.optString("exercise_id"),
                        exerciseName = jo.optString("exercise_name"),
                        exerciseTypeId = jo.getString("exercise_type_id"),
                        exerciseTypeName = jo.getString("exercise_type_name"),
                        exerciseCategoryId = jo.getString("exercise_category_id"),
                        exerciseCategoryName = jo.getString("exercise_category_name"),
                        relatedJoint = jo.getString("related_joint"),
                        relatedMuscle = jo.getString("related_muscle"),
                        relatedSymptom = jo.getString("related_symptom"),
                        exerciseStage = jo.getString("exercise_stage"),
                        exerciseFrequency = jo.getString("exercise_frequency"),
                        exerciseIntensity = jo.getString("exercise_intensity"),
                        exerciseInitialPosture = jo.getString("exercise_initial_posture"),
                        exerciseMethod = jo.getString("exercise_method"),
                        exerciseCaution = jo.getString("exercise_caution"),
                        videoFilepath = jo.getString("video_filepath"),
                        duration = (jo.optString("duration").toIntOrNull() ?: 0).toString(),
                        imageFilePath = jo.getString("image_filepath")
                    )
                    exerciseInstance
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("fetchExercise1", "IndexOutOfBoundsException: ${e.message}")
                null
            } catch (e: IllegalArgumentException) {
                Log.e("fetchExercise1", "IllegalArgumentException: ${e.message}")
                null
            } catch (e: IllegalStateException) {
                Log.e("fetchExercise1", "IllegalStateException: ${e.message}")
                null
            }  catch (e: IOException) {
                Log.e("fetchExercise1", "IOException: ${e.message}")
                null
            } catch (e: NullPointerException) {
                Log.e("fetchExercise1", "NullPointerException: ${e.message}")
                null
            } catch (e: java.lang.Exception) {
                Log.e("fetchExercise1", "Exception: ${e.message}")
                null
            }
        }
    }

    suspend fun fetchExerciseJson(myUrl: String): List<ExerciseVO>? {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(myUrl)
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()

                    val exerciseDataList = mutableListOf<ExerciseVO>()
                    val jsonArr = responseBody?.let { JSONArray(it) }

                    if (jsonArr != null) { // 174 ~ 195 번의 exercise_id  250116 기준 현재 0 ~ 203
                        for (i in 0 until jsonArr.length()) {
                            if (i !in 173 .. 194) {
                                val jsonObject = jsonArr.getJSONObject(i)
                                val exerciseData = ExerciseVO(
                                    exerciseId = jsonObject.optString("exercise_id"),
                                    exerciseName = jsonObject.optString("exercise_name"),
                                    exerciseTypeId = jsonObject.getString("exercise_type_id"),
                                    exerciseTypeName = jsonObject.getString("exercise_type_name"),
                                    exerciseCategoryId = jsonObject.getString("exercise_category_id"),
                                    exerciseCategoryName = jsonObject.getString("exercise_category_name"),
                                    relatedJoint = jsonObject.getString("related_joint"),
                                    relatedMuscle = jsonObject.getString("related_muscle"),
                                    relatedSymptom = jsonObject.getString("related_symptom"),
                                    exerciseStage = jsonObject.getString("exercise_stage"),
                                    exerciseFrequency = jsonObject.getString("exercise_frequency"),
                                    exerciseIntensity = jsonObject.getString("exercise_intensity"),
                                    exerciseInitialPosture = jsonObject.getString("exercise_initial_posture"),
                                    exerciseMethod = jsonObject.getString("exercise_method"),
                                    exerciseCaution = jsonObject.getString("exercise_caution"),
                                    videoFilepath = jsonObject.getString("video_filepath"),
                                    duration = (jsonObject.optString("duration").toIntOrNull() ?: 0).toString(),
                                    imageFilePath = jsonObject.getString("image_filepath"),
                                )
                                exerciseDataList.add(exerciseData)
                            }
                        }
                    }
                    exerciseDataList
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("fetchExercise", "IndexOutOfBoundsException: ${e.message}")
                null
            } catch (e: IllegalArgumentException) {
                Log.e("fetchExercise", "IllegalArgumentException: ${e.message}")
                null
            } catch (e: IllegalStateException) {
                Log.e("fetchExercise", "IllegalStateException: ${e.message}")
                null
            }  catch (e: IOException) {
                Log.e("fetchExercise", "IOException: ${e.message}")
                null
            } catch (e: NullPointerException) {
                Log.e("fetchExercise", "NullPointerException: ${e.message}")
                null
            } catch (e: java.lang.Exception) {
                Log.e("fetchExercise", "Exception: ${e.message}")
                null
            }
        }
    }
    // category + type 두 가지로 조회

    fun jsonToExerciseVO(json: JSONObject): ExerciseVO {
        return ExerciseVO(
            exerciseId = json.optString("exercise_id"),
            exerciseName = json.optString("exercise_name"),
            exerciseTypeId = json.getString("exercise_type_id"),
            exerciseTypeName = json.getString("exercise_type_name"),
            exerciseCategoryId = json.getString("exercise_category_id"),
            exerciseCategoryName = json.getString("exercise_category_name"),
            relatedJoint = json.getString("related_joint"),
            relatedMuscle = json.getString("related_muscle"),
            relatedSymptom = json.getString("related_symptom"),
            exerciseStage = json.getString("exercise_stage"),
            exerciseFrequency = json.getString("exercise_frequency"),
            exerciseIntensity = json.getString("exercise_intensity"),
            exerciseInitialPosture = json.getString("exercise_initial_posture"),
            exerciseMethod = json.getString("exercise_method"),
            exerciseCaution = json.getString("exercise_caution"),

            videoFilepath = json.getString("video_filepath"),
            duration = (json.optString("duration").toIntOrNull() ?: 0).toString(),
            imageFilePath = json.getString("image_filepath"),

        )
    }

    // exercise를 봤을 때만.
    suspend fun patchExerciseHistory(context: Context, myUrl: String, exerciseId: String, bodyString: String) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = bodyString.toRequestBody(mediaType)
        val client = getClient(context)
        val request = Request.Builder()
            .url("$myUrl/$exerciseId")
            .patch(body)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.v("ExerciseHistory", "$responseBody")
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("ExerciseHistoryError", "IndexOutOfBounds: ${e.message}")
            } catch (e: IllegalArgumentException) {
                Log.e("ExerciseHistoryError", "IllegalArgument: ${e.message}")
            } catch (e: IllegalStateException) {
                Log.e("ExerciseHistoryError", "IllegalState: ${e.message}")
            }  catch (e: IOException) {
                Log.e("ExerciseHistoryError", "IO: ${e.message}")
            } catch (e: NullPointerException) {
                Log.e("ExerciseHistoryError", "NullPointer: ${e.message}")
            } catch (e: java.lang.Exception) {
                Log.e("ExerciseHistoryError", "Exception: ${e.message}")
            }
        }
    }

    suspend fun getExerciseHistory(context: Context, myUrl: String, categoryId: String) : List<ExerciseHistoryVO>? {
        val client = getClient(context)
        val request = Request.Builder()
            .url("$myUrl?exercise_progress=$categoryId")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    // Log.v("getExerciseHistory", "responseBody: $responseBody")
                    val results = mutableListOf<ExerciseHistoryVO>()
                    if (responseBody.isNullOrEmpty()) {
                        return@use results
                    }

                    // JSON 객체로 오는 경우 처리 (에러 상태로 판단)
                    if (responseBody.startsWith("{")) {
                        val jsonObject = JSONObject(responseBody)
                        val status = jsonObject.optInt("status", 0)
                        val message = jsonObject.optString("message", "Unknown error")

                        // status가 404일 경우 메시지 로그를 남기고 빈 리스트 반환
                        if (status == 404) {
                            // Log.w("getExerciseHistory", "No progress: $message")
                            return@use results
                        }
                    }
                    if (responseBody.trim().startsWith("[")) {
                        val bodyJa = JSONArray(responseBody)
                        for (i in 0 until bodyJa.length()) {
                            val historyUnits = bodyJa.getJSONObject(i)
                            val exerciseHistoryVO = ExerciseHistoryVO(
                                evpSn = historyUnits.optInt("evp_sn"),
                                userSn = historyUnits.optInt("user_sn"),
                                exerciseId = historyUnits.optInt("exercise_id"),
                                exerciseName = historyUnits.optString("exercise_name"),
                                duration = historyUnits.optInt("duration"),
                                progress = historyUnits.optInt("progress"),
                                exerciseTypeId = historyUnits.optInt("exercise_type_id"),
                                exerciseCategoryId = historyUnits.optInt("exercise_category_id"),
                                completed = historyUnits.optInt("completed"),
                                registeredAt = historyUnits.optString("registered_at"),
                                updatedAt = historyUnits.optString("updated_at"),
                            )
                            results.add(exerciseHistoryVO)
                        }
                         Log.v("getExerciseHistory", "results: $results")
                        return@use results.toList()
                    }
                    return@use results.toList()
                }
            } catch (e: SocketTimeoutException) {
                Log.e("ExerciseHistoryError", "GETSocketTimeout: ${e.message}")
                null
            } catch (e: IndexOutOfBoundsException) {
                Log.e("ExerciseHistoryError", "GETIndexOutOfBounds: ${e.message}")
                null
            } catch (e: IllegalArgumentException) {
                Log.e("ExerciseHistoryError", "GETIllegalArgument: ${e.message}")
                null
            } catch (e: IllegalStateException) {
                Log.e("ExerciseHistoryError", "GETIllegalState: ${e.message}")
                null
            } catch (e: IOException) {
                Log.e("ExerciseHistoryError", "GETIO: ${e.message}")
                null
            }  catch (e: NullPointerException) {
                Log.e("ExerciseHistoryError", "GETNullPointer: ${e.message}")
                null
            } catch (e: java.lang.Exception) {
                Log.e("ExerciseHistoryError", "GETException: ${e.message}")
                null
            }
        }
    }
}