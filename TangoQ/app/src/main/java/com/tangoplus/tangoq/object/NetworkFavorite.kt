package com.tangoplus.tangoq.`object`

import android.content.ContentValues
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.FavoriteVO
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

object NetworkFavorite {
    // 즐겨찾기 넣기
    fun insertFavoriteItemJson(myUrl: String, json: String, context: Context ,callback: (JSONObject?) -> Unit) {
        val client = OkHttpClient()
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)
        val request = Request.Builder()
            .url("${myUrl}/favorite_add.php")
            .post(body) // post방식으로 insert 들어감
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("http>Response", "Failed to execute request!")
                Toast.makeText(context, "데이터 연결이 실패했습니다. 잠시 후에 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
            override fun onResponse(call: Call, response: Response)  {
                val responseBody = response.body?.string()
                Log.e("http>Response", "$responseBody")
                val jsonObj__ = responseBody?.let { JSONObject(it) }
                callback(jsonObj__)
            }
        })
    }
    fun updateFavoriteItemJson(myUrl: String, favoriteSn: String, json:String, context: Context ,callback: (JSONObject?) -> Unit) {
        val client = OkHttpClient()
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("$myUrl/update.php?favorite_sn=$favoriteSn")
            .patch(body)
            .build()
        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("http>Response", "Failed to execute request!")
                Toast.makeText(context, "데이터 연결이 실패했습니다. 잠시 후에 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.e("http>Response", "$responseBody")
                val jsonObj__ = responseBody?.let { JSONObject(it) }
                callback(jsonObj__)
            }
        })
    }
    fun deleteFavoriteItemSn(myUrl: String, favoriteSn: String, context: Context ,callback: () -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}delete.php?favorite_sn=$favoriteSn")
            .delete()
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("http>Response", "Failed to execute request!")
                Toast.makeText(context, "데이터 연결이 실패했습니다. 잠시 후에 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.e("http>Response", "$responseBody")
                callback()
            }
        })

    }
    // 즐겨찾기 목록 조회 (PickItems에 담기) token + jsonObject로 조회?
    suspend fun fetchFavoriteItemsJsonByEmail(myUrl: String, email: String): MutableList<FavoriteVO> {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}read.php?user_mobile=$email")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use {response ->
                val responseBody = response.body?.string()
                Log.e("HTTP>favoriteFetch", "Success to execute request")
                val jsonArr = responseBody?.let { JSONObject(it) }?.optJSONArray("data")
                val favoriteList = mutableListOf<FavoriteVO>()
                if (jsonArr != null) {
                    for (i in 0 until jsonArr.length()) {
                        val jsonObject = jsonArr.getJSONObject(i)
                        val favoriteItem = FavoriteVO(
                            imgThumbnails = mutableListOf(),
                            favoriteSn = jsonObject.optInt("favorite_sn"),
                            favoriteName = jsonObject.optString("favorite_name"),
                            favoriteRegDate = jsonObject.optString("reg_date"),
                            favoriteExplain = jsonObject.optString("favorite_description"),
                            favoriteTotalCount = (if ((jsonObject.optString("exercise_ids")) == "null") 0 else (jsonObject.optString("exercise_ids").split(",").size)).toString(),
                            exercises = mutableListOf()
                        )
                        favoriteList.add(favoriteItem)
                    }
                }
                favoriteList
            }
        }
    }
    suspend fun fetchFavoriteItemJsonBySn(myUrl: String, sn: String): JSONObject {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}read.php?favorite_sn=$sn")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use {response ->
                val responseBody = response.body?.string().let { JSONObject(it) }
                Log.e("HTTP>favoriteFetch", "Success to execute request!: $responseBody")
                responseBody
            }
        }
    }
    // exercises 가 전부 들어간 즐겨찾기 한 개
    fun jsonToFavoriteItemVO(json: JSONObject) : FavoriteVO {
        val exerciseUnits = mutableListOf<ExerciseVO>()
        val exercises = json.optJSONArray("exercise_detail_data")
        if (exercises != null) {
            for (i in 0 until exercises.length()) {
                exerciseUnits.add(NetworkExercise.jsonToExerciseVO(exercises.get(i) as JSONObject))
            }
        }
        Log.w("exerciseUnits", "$exerciseUnits")
        val jsonObj_ = json.optJSONObject("favorite info")
        return FavoriteVO(
            imgThumbnails = mutableListOf(), // TODO 썸네일 리스트 OPT로 받아와야 함
            favoriteSn = jsonObj_!!.optInt("favorite_sn"),
            favoriteName = jsonObj_.optString("favorite_name"),
            favoriteRegDate = jsonObj_.optString("reg_date"),
            favoriteExplain = jsonObj_.optString("favorite_description"),
            exercises = exerciseUnits
        )
    }
}