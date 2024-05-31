package com.tangoplus.tangoq.`object`

import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.FavoriteItemVO
import android.content.ContentValues
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

object NetworkExerciseService {

    // 카테고리 가져오기
    suspend fun fetchExerciseCategory(myUrl: String) : MutableList<Pair<Int, String>> {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}read.php")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.w("OKHTTP3/ExerciseFetch", "Success to execute request!: $responseBody")
                val jsonArr = responseBody?.let { JSONObject(it) }?.optJSONArray("data")
                val categorySet = mutableSetOf<Pair<Int, String>>()
                if (jsonArr != null) {
                    for (i in 0 until jsonArr.length()) {
                        val jsonObject = jsonArr.getJSONObject(i)
                        categorySet.add(Pair(jsonObject.optString("exercise_category_id").toInt(), jsonObject.optString("exercise_category_name")))
                    }
                }
                val categoryList = categorySet.toMutableList()
                categoryList
            }
        }
    }

    // 타입 가져오기
//    suspend fun fetchExerciseType(myUrl: String) : MutableList<Pair<Int, String>> {
//        val client = OkHttpClient()
//        val request = Request.Builder()
//            .url("${myUrl}read.php")
//            .get()
//            .build()
//        return withContext(Dispatchers.IO) {
//            client.newCall(request).execute().use { response ->
//                val responseBody = response.body?.string()
//                Log.w("OKHTTP3/ExerciseFetch", "Success to execute request!: $responseBody")
//                val jsonArr = responseBody?.let { JSONObject(it) }?.optJSONArray("data")
//                val typeSet = mutableSetOf<Pair<Int, String>>()
//                if (jsonArr != null) {
//                    for (i in 0 until jsonArr.length()) {
//                        val jsonObject = jsonArr.getJSONObject(i)
//                        typeSet.add(Pair(jsonObject.optString("exercise_type_id").toInt(), jsonObject.optString("exercise_type_name")))
//                    }
//                }
//                val typeList = typeSet.toMutableList()
//                typeList
//            }
//        }
//    }

    suspend fun fetchCategoryAndSearch(myUrl: String, categoryId: Int, search: Int) : MutableList<ExerciseVO> {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}read.php?exercise_category_id=$categoryId&exercise_search=$search")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.e("OKHTTP3/ExerciseFetch", "Success to execute request!: $responseBody")

                val exerciseDataList = mutableListOf<ExerciseVO>()
                val jsonArr = responseBody?.let { JSONObject(it) }?.optJSONArray("data")
                Log.v("fetchExerciseJson", "$jsonArr")
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
                            videoActualName = jsonObject.getString("video_actual_name"),
                            videoFilepath = jsonObject.getString("video_filepath"),
                            videoDuration = (jsonObject.optString("video_duration").toIntOrNull() ?: 0).toString(),
                            imageFilePathReal = jsonObject.getString("image_filepath_real"),
                            )
                        exerciseDataList.add(exerciseData)
                        }
                    }
                    exerciseDataList
                }
            }
    }

    // 전체 조회
    suspend fun fetchExerciseJson(myUrl: String): List<ExerciseVO> {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}read.php")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.e("OKHTTP3/ExerciseFetch", "Success to execute request!: $responseBody")

                val exerciseDataList = mutableListOf<ExerciseVO>()
                val jsonArr = responseBody?.let { JSONObject(it) }?.optJSONArray("data")
                Log.v("fetchExerciseJson", "$jsonArr")
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
                            videoActualName = jsonObject.getString("video_actual_name"),
                            videoFilepath = jsonObject.getString("video_filepath"),
                            videoDuration = (jsonObject.optString("video_duration").toIntOrNull() ?: 0).toString(),
                            imageFilePathReal = jsonObject.getString("image_filepath_real"),
                        )
                        exerciseDataList.add(exerciseData)
                    }
                }
                exerciseDataList
            }
        }
    }
    /// type 별 조회
    suspend fun fetchExerciseJsonByType(myUrl: String, id: String) : MutableList<ExerciseVO> {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}read.php?exercise_type_id=$id")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.e("OKHTTP3/ExerciseFetch", "Success to execute request!: $responseBody")
                val jsonObj__ = responseBody?.let { JSONObject(it) }
                val exerciseDataList = mutableListOf<ExerciseVO>()
                val jsonArr = jsonObj__?.optJSONArray("data")
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
                            videoActualName = jsonObject.getString("video_actual_name"),
                            videoFilepath = jsonObject.getString("video_filepath"),
                            videoDuration = (jsonObject.optString("video_duration").toIntOrNull() ?: 0).toString(),
                            imageFilePathReal = jsonObject.getString("image_filepath_real"),

                        )
                        exerciseDataList.add(exerciseData)
                    }
                }
                exerciseDataList
            }
        }
    }

    // category + type 두 가지로 조회
    suspend fun fetchExerciseJsonByCategoryAndType(myUrl: String, categoryId: String, typeId: String) : MutableList<ExerciseVO> {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}read.php?exercise_category_id=$categoryId?exercise_type_id=$typeId/")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.e("OKHTTP3/ExerciseFetch", "Success to execute request!: $responseBody")
                val jsonObj__ = responseBody?.let { JSONObject(it) }
                val exerciseDataList = mutableListOf<ExerciseVO>()
                val jsonArr = jsonObj__?.optJSONArray("data")
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
                            videoActualName = jsonObject.getString("video_actual_name"),
                            videoFilepath = jsonObject.getString("video_filepath"),
                            videoDuration = (jsonObject.optString("video_duration").toIntOrNull() ?: 0).toString(),
                            imageFilePathReal = jsonObject.getString("image_filepath_real"),
                            )
                        exerciseDataList.add(exerciseData)
                    }
                }
                exerciseDataList
            }
        }
    }

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
            videoActualName = json.getString("video_actual_name"),
            videoFilepath = json.getString("video_filepath"),
            videoDuration = (json.optString("video_duration").toIntOrNull() ?: 0).toString(),
            imageFilePathReal = json.getString("image_filepath_real"),

        )
    }


}