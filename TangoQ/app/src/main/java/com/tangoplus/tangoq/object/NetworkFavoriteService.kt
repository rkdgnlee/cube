package com.tangoplus.tangoq.`object`

import android.content.ContentValues
import android.util.Log
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.FavoriteItemVO
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

object NetworkFavoriteService {
    // 즐겨찾기 넣기
    fun insertFavoriteItemJson(myUrl: String, json: String, callback: (JSONObject?) -> Unit) {
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
                val jsonObj__ = responseBody?.let { JSONObject(it) }
                callback(jsonObj__)
            }
        })
    }
    fun updateFavoriteItemJson(myUrl: String, favorite_sn: String, json:String, callback: (JSONObject?) -> Unit) {
        val client = OkHttpClient()
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
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
    fun deleteFavoriteItemSn(myUrl: String, favorite_sn: String, callback: () -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}delete.php?favorite_sn=$favorite_sn")
            .delete()
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("${ContentValues.TAG}, 응답실패", "Failed to execute request!")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.e("${ContentValues.TAG}, 응답성공", "$responseBody")
                callback()
            }
        })

    }
    // 즐겨찾기 목록 조회 (PickItems에 담기)
    suspend fun fetchFavoriteItemsJsonByMobile(myUrl: String, mobile: String): JSONArray? {
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
    suspend fun fetchFavoriteItemJsonBySn(myUrl: String, sn: String): JSONObject? {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}read.php?favorite_sn=$sn")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use {response ->
                val responseBody = response.body?.string().let { JSONObject(it) }
                Log.e("OKHTTP3/favoriteListFetch", "Success to execute request!: $responseBody")
                responseBody
            }
        }
    }
    // exercises 가 전부 들어간 즐겨찾기 한 개
    fun jsonToFavoriteItemVO(json: JSONObject) : FavoriteItemVO {
        val exerciseUnits = mutableListOf<ExerciseVO>()
        val exercises = json.optJSONArray("exercise_detail_data")
        if (exercises != null) {
            for (i in 0 until exercises.length()) {
                exerciseUnits.add(NetworkExerciseService.jsonToExerciseVO(exercises.get(i) as JSONObject))
            }
        }
        Log.w("exerciseUnits", "$exerciseUnits")
        val jsonObj_ = json.optJSONObject("favorite info")
        return FavoriteItemVO(
            imgThumbnailList = mutableListOf(), // TODO 썸네일 리스트 OPT로 받아와야 함
            favoriteSn = jsonObj_!!.optInt("favorite_sn"),
            favoriteName = jsonObj_.optString("favorite_name"),
            favoriteRegDate = jsonObj_.optString("reg_date"),
            favoriteExplain = jsonObj_.optString("favorite_description"),
            exercises = exerciseUnits
        )
    }
}