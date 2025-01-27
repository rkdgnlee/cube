package com.tangoplus.tangoq.api

import com.tangoplus.tangoq.vo.ExerciseVO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

object  NetworkExercise {

    // ------! exercise1개 가져오기 !------
    suspend fun fetchExerciseById(myUrl: String, exerciseId : String) : ExerciseVO {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}/$exerciseId")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
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
        }
    }

    suspend fun fetchExerciseJson(myUrl: String): List<ExerciseVO> {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(myUrl)
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()

                val exerciseDataList = mutableListOf<ExerciseVO>()
                val jsonArr = responseBody?.let { JSONArray(it) }

                if (jsonArr != null) {
                    for (i in 0 until jsonArr.length()) {
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
                exerciseDataList
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


}