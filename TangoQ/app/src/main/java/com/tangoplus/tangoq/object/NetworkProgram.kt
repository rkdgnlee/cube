package com.tangoplus.tangoq.`object`

import android.util.Log
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.ProgramVO
import com.tangoplus.tangoq.`object`.NetworkExercise.jsonToExerciseVO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

object NetworkProgram {
    suspend fun fetchProgram(myUrl: String, sn: String): ProgramVO {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}?exercise_program_sn=$sn")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()?.substringAfter("db conn ok")

                val jsonInfo = responseBody?.let { JSONObject(it) }?.optJSONObject("exercise_program_info")
                val jsonExerciseArr = responseBody?.let { JSONObject(it) }?.getJSONArray("program_detail_data")
                val exercises = mutableListOf<ExerciseVO>()
                val exerciseTimes = mutableListOf<Pair<String,Int>>()
                for (i in 0 until jsonExerciseArr?.length()!!) {
                    exercises.add(jsonToExerciseVO(jsonExerciseArr.optJSONObject(i)))
                    val exerciseTime = Pair((jsonExerciseArr[i] as JSONObject).optString("exercise_id"), (jsonExerciseArr[i] as JSONObject).optString("video_duration").toInt())
                    exerciseTimes.add(exerciseTime)
                }
                val programVO = ProgramVO(
                    programSn = jsonInfo?.optString("exercise_program_sn")!!.toInt(),
                    programName = jsonInfo.optString("exercise_program_title"),
                    programTime = JSONObject(responseBody).optInt("total_video_time"),
                    programStage = "",
                    programCount = "${jsonInfo.optString("exercise_ids").split(", ").count()}",
                    programFrequency = jsonInfo.optInt("required_week"),
                    programWeek = jsonInfo.optInt("exercise_frequency"),
                    exercises = exercises,
                    exerciseTimes = exerciseTimes
                )

                return@use programVO
            }
        }
    }
    suspend fun fetchPrograms(myUrl: String, sn: String): ProgramVO {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}/$sn")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()?.substringAfter("db conn ok")

                val jsonInfo = responseBody?.let { JSONObject(it) }?.optJSONObject("exercise_program_info")
                val jsonExerciseArr = responseBody?.let { JSONObject(it) }?.getJSONArray("program_detail_data")
                val exercises = mutableListOf<ExerciseVO>()
                val exerciseTimes = mutableListOf<Pair<String,Int>>()
                for (i in 0 until jsonExerciseArr?.length()!!) {
                    exercises.add(jsonToExerciseVO(jsonExerciseArr.optJSONObject(i)))
                    val exerciseTime = Pair((jsonExerciseArr[i] as JSONObject).optString("exercise_id"), (jsonExerciseArr[i] as JSONObject).optString("video_duration").toInt())
                    exerciseTimes.add(exerciseTime)
                }
                val programVO = ProgramVO(
                    programSn = jsonInfo?.optString("exercise_program_sn")!!.toInt(),
                    programName = jsonInfo.optString("exercise_program_name"),
                    programTime = JSONObject(responseBody).optInt("total_video_time"),
                    programStage = "",
                    programCount = "${jsonInfo.optString("exercise_ids").split(", ").count()}",
                    programFrequency = jsonInfo.optInt("exercise_frequency"),
                    programWeek = jsonInfo.optInt("required_week"),
                    exercises = exercises,
                    exerciseTimes = exerciseTimes
                )

                return@use programVO
            }
        }
    }

}