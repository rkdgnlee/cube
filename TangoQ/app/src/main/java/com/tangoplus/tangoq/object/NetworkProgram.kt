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
    suspend fun fetchProgramVOBySn(myUrl: String, sn: String): ProgramVO {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}read.php?exercise_program_sn=$sn")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()?.substringAfter("db conn ok")

                val jsonInfo = responseBody?.let { JSONObject(it) }?.optJSONObject("exercise_program_info")
                val jsonExerciseArr = responseBody?.let { JSONObject(it) }?.getJSONArray("program_detail_data")

                val exercises = jsonExerciseArr?.let { distributeExercises(it) } ?: mutableListOf()
                val exerciseTimes = mutableListOf<Pair<String,Int>>()
                for (i in 0 until jsonExerciseArr?.length()!!) {
                    val exerciseTime = Pair((jsonExerciseArr[i] as JSONObject).optString("exercise_id"), (jsonExerciseArr[i] as JSONObject).optString("video_duration").toInt())
                    exerciseTimes.add(exerciseTime)
                }
                val programVO = ProgramVO(
                    programSn = jsonInfo?.optString("exercise_program_sn")!!.toInt(),
                    programName = jsonInfo.optString("exercise_program_name"),
                    programTime = JSONObject(responseBody).optInt("total_video_time"),
                    programStage = "",
                    programCount = "${jsonInfo.optString("exercise_ids").split(", ").count()}",
                    programEpisode = 4, // jsonInfo.optInt("exercises_weeks")
                    programWeek = 4,
                    exercises = exercises,
                    exerciseTimes = exerciseTimes
                )

                return@use programVO
            }
        }
    }

    private fun distributeExercises(jsonExerciseArr: JSONArray): MutableList<MutableList<ExerciseVO>> {
        val totalExercises = jsonExerciseArr.length()
        val exercises = mutableListOf<MutableList<ExerciseVO>>()

        val weeksCount = when {
            totalExercises >= 56 -> 8
            totalExercises >= 42 -> 6
            else -> 4
        }

        val baseExercisesPerWeek = totalExercises / weeksCount
        var remainingExercises = totalExercises % weeksCount

        for (week in 0 until weeksCount) {
            val weekExercises = mutableListOf<ExerciseVO>()
            val exercisesThisWeek = if (week == weeksCount - 1) {
                baseExercisesPerWeek + remainingExercises
            } else {
                baseExercisesPerWeek + if (remainingExercises > 0) 1 else 0
            }

            val startIndex = week * baseExercisesPerWeek + minOf(week, remainingExercises)
            for (i in startIndex until (startIndex + exercisesThisWeek)) {
                weekExercises.add(jsonToExerciseVO(jsonExerciseArr.getJSONObject(i)))
            }

            exercises.add(weekExercises)
            if (week != weeksCount - 1 && remainingExercises > 0) remainingExercises--
        }

        return exercises
    }

}