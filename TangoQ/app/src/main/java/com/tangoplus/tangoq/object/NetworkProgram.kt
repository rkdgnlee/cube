package com.tangoplus.tangoq.`object`

import android.util.Log
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.ProgramVO
import com.tangoplus.tangoq.`object`.NetworkExercise.jsonToExerciseVO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object NetworkProgram {
    suspend fun fetchProgramVOBySn(myUrl: String, sn: String) : ProgramVO {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}read.php?exercise_program_sn=$sn")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use {response ->
                val responseBody = response.body?.string()?.substringAfter("db conn ok")
                Log.v("http3/programFetch", "Success to execute request!: $responseBody")

                val jsonInfo = responseBody?.let { JSONObject(it) }?.optJSONObject("exercise_program_info")
                val jsonExerciseArr = responseBody?.let { JSONObject(it) }?.getJSONArray("program_detail_data")
                var time = 0
                val exercises = mutableListOf<ExerciseVO>()
                val imgUrls = mutableSetOf<String>()
                for (i in 0 until jsonExerciseArr?.length()!!) {
                    // ------! 시간 + 운동 + imgUrl 넣기 !------
//                    time += jsonExerciseArr.getJSONObject(i).getString("video_duration").toInt()
                    exercises.add(jsonToExerciseVO(jsonExerciseArr.getJSONObject(i)))
                    imgUrls.add(jsonExerciseArr.getJSONObject(i).getString("image_filepath_real"))

                }
                val programVO = ProgramVO(
                    programSn = jsonInfo?.optString("exercise_program_sn")!!.toInt(),
                    imgThumbnails = imgUrls.toMutableList(),
                    programName = jsonInfo.optString("exercise_program_name"),
                    programTime = JSONObject(responseBody).optInt("total_video_time"),
                    programStage = "",
                    programCount = "${jsonInfo.getString("exercise_ids").split(", ").count()}",
//                    programVideoUrl = jsonExerciseArr.getJSONObject(1).optString("video_filepath"),
                    exercises = exercises
                )
                return@use programVO
            }
        }
    }
}