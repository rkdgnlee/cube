package com.example.mhg.`object`

import android.content.ContentValues
import android.util.Log
import com.example.mhg.R
import com.example.mhg.VO.ExerciseVO
import com.example.mhg.VO.PickItemVO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

object NetworkExerciseService {
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
                val jsonObj__ = responseBody?.let { JSONObject(it) }

                jsonObj__?.optJSONArray("data")
                val exerciseDataList = mutableListOf<ExerciseVO>()
                val jsonArr = jsonObj__?.optJSONArray("data")
                if (jsonArr != null) {
                    for (i in 0 until jsonArr.length()) {
                        val jsonObject = jsonArr.getJSONObject(i)
                        val exerciseData = ExerciseVO(
                            exerciseName = jsonObject.optString("exercise_name"),
                            exerciseDescription = jsonObject.getString("exercise_description"),
                            exerciseDescriptionId = jsonObject.getInt("exercise_description_id"),
                            relatedJoint = jsonObject.getString("related_joint"),
                            relatedMuscle = jsonObject.getString("related_muscle"),
                            relatedSymptom = jsonObject.getString("related_symptom"),
                            exerciseStage = jsonObject.getString("exercise_stage"),
                            exerciseFequency = jsonObject.getString("exercise_frequency"),
                            exerciseIntensity = jsonObject.getString("exercise_intensity"),
                            exerciseInitialPosture = jsonObject.getString("exercise_initial_posture"),
                            exerciseMethod = jsonObject.getString("exercise_method"),
                            exerciseCaution = jsonObject.getString("exercise_caution"),
                            videoAlternativeName = jsonObject.getString("video_alternative_name"),
                            videoFilepath = jsonObject.getString("video_filepath"),
                            videoTime = jsonObject.getString("video_time"),
                            exerciseTypeId = jsonObject.getString("exercise_type_id"),
                            exerciseTypeName = jsonObject.getString("exercise_type_name")
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
                            exerciseName = jsonObject.optString("exercise_name"),
                            exerciseDescription = jsonObject.getString("exercise_description"),
                            exerciseDescriptionId = jsonObject.getInt("exercise_description_id"),
                            relatedJoint = jsonObject.getString("related_joint"),
                            relatedMuscle = jsonObject.getString("related_muscle"),
                            relatedSymptom = jsonObject.getString("related_symptom"),
                            exerciseStage = jsonObject.getString("exercise_stage"),
                            exerciseFequency = jsonObject.getString("exercise_frequency"),
                            exerciseIntensity = jsonObject.getString("exercise_intensity"),
                            exerciseInitialPosture = jsonObject.getString("exercise_initial_posture"),
                            exerciseMethod = jsonObject.getString("exercise_method"),
                            exerciseCaution = jsonObject.getString("exercise_caution"),
                            videoAlternativeName = jsonObject.getString("video_alternative_name"),
                            videoFilepath = jsonObject.getString("video_filepath"),
                            videoTime = jsonObject.getString("video_time"),
                            exerciseTypeId = jsonObject.getString("exercise_type_id"),
                            exerciseTypeName = jsonObject.getString("exercise_type_name")
                        )
                        exerciseDataList.add(exerciseData)
                    }
                }
                exerciseDataList
            }
        }
    }
    // 즐겨찾기 넣기
    fun insertPickItemJson(myUrl: String, json: String, callback: () -> Unit) {
        val client = OkHttpClient()
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)
        val request = Request.Builder()
            .url("${myUrl}/favorite_add.php")
            .post(body) // post방식으로 insert 들어감
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("${ContentValues.TAG}, 응답실패", "Failed to execute request!")
            }
            override fun onResponse(call: Call, response: Response)  {
                val responseBody = response.body?.string()
                Log.e("${ContentValues.TAG}, 응답성공", "$responseBody")
                callback()
            }
        })
    }
    fun updatePickItemJson(myUrl: String, favorite_sn: String, json:String, callback: (JSONObject?) -> Unit) {
        val client = OkHttpClient()
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)
        val request = Request.Builder()
            .url("$myUrl/update.php?favorite_sn=$favorite_sn")
            .patch(body)
            .build()
        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("${ContentValues.TAG}, 응답실패", "Failed to execute request!")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.e("${ContentValues.TAG}, 응답성공", "$responseBody")
                val jsonObj__ = responseBody?.let { JSONObject(it) }
                callback(jsonObj__)
            }
        })
    }
    // 즐겨찾기 목록 조회 (PickItems에 담기)
    suspend fun fetchPickItemsJsonByMobile(myUrl: String, mobile: String): JSONArray? {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}read.php?user_mobile=$mobile")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use {response ->
                val responseBody = response.body?.string()
                Log.e("OKHTTP3/picklistfetch", "Success to execute request!: $responseBody")
                val jsonObj__ = responseBody?.let { JSONObject(it) }
                val jsonArray = try {
                    jsonObj__?.getJSONArray("data")
                } catch (e: JSONException) {
                    JSONArray()
                }
                jsonArray
            }
        }
    }
    suspend fun fetchPickItemJsonBySn(myUrl: String, sn: String): JSONObject? {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}read.php?favorite_sn=$sn")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use {response ->
                val responseBody = response.body?.string()
                Log.e("OKHTTP3/picklistfetch", "Success to execute request!: $responseBody")
                val jsonObj__ = responseBody?.let { JSONObject(it) }
//                val jsonArray = try {
//                    jsonObj__?.getJSONArray("data")
//                } catch (e: JSONException) {
//                    JSONArray()
//                }
//                jsonArray
                jsonObj__ // TODO BODY에서 가져와서 전부다 쓸껀지, 아니면 EXERCISE JSONArray만 따로 쓸건지 판단해서 수정하기. 그리고 받아와서
            }
        }
    }
}