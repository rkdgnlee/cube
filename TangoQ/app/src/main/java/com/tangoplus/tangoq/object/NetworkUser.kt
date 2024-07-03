package com.tangoplus.tangoq.`object`

import android.content.ContentValues
import android.content.Context
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object NetworkUser {

    // ------! 간편 로그인 정보 주고 토큰 발급 받기 !------
    fun getTokenByUserJson(myUrl: String, userJsonObject: JSONObject,callback: (JSONObject?) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, userJsonObject.toString())
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(myUrl)
            .post(body)
            .build()
        client.newCall(request).enqueue(object : Callback{
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Token응답실패", "Failed to execute request")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.e("Token응답성공", "$responseBody")
                val jo = responseBody?.let { JSONObject(it) }
                callback(jo)
                /**  TODO responseBody의 응답 코드에 맞게 처리 하기 (만료됐으니 갱신 된건 뭐 201, 정상 처리는 200, 올바르지 않은 토큰 404 등)
                 TODO 갱신된 토큰은 응답을 감지해서 이를 저장하고 요청해야 한다.
                */
            }
        })
    }



//    // ------! 자체 로그인에서 정보 가져오기 !------
//    fun getUserSELECTJson(myUrl: String, userToken: String, userJsonObject: JSONObject, callback: (JSONObject?) -> Unit) {
//        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
//        val body = RequestBody.create(mediaType, userJsonObject.toString())
//        val client = OkHttpClient()
//        val request = Request.Builder()
//            .url("${myUrl}read.php")
//            .header("token", userToken)
//            .post(body)
//            .build()
//
//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                Log.e("응답실패", "Failed to execute request!")
//            }
//            override fun onResponse(call: Call, response: Response)  {
//                val responseBody = response.body?.string()
//                Log.e("응답성공", "$responseBody")
//                val jsonObj__ = responseBody?.let { JSONObject(it) }
//                callback(jsonObj__)
//            }
//        })
//    }

    // ------! 토큰 + 사용자 정보로 로그인 유무 확인 !------  ###  ### ---> 여기서도 토큰이 생김
    fun getUserIdentifyJson(myUrl: String,  idPw: JSONObject, userToken: String, callback: (JSONObject?) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, idPw.toString())
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}read.php")
            .header("user_token", userToken)
            .post(body)
            .build()
        client.newCall(request).enqueue(object : Callback {
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







    /* ------------------------------------------CRUD---------------------------------------------*/
    fun fetchUserINSERTJson(myUrl : String, json: String, callback: () -> Unit){
        val client = OkHttpClient()
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)
        val request = Request.Builder()
            .url("${myUrl}create.php")
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

    fun fetchUserUPDATEJson(myUrl : String, json: String, email: String, callback: () -> Unit) {
        val client = OkHttpClient()
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json )
        val request = Request.Builder()
            .url("${myUrl}update.php?user_mobile=$email")
            .patch(body)
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

    fun fetchUserDeleteJson(myUrl : String, email:String, callback: () -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}delete.php?user_email=$email")
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


    fun storeUserInSingleton(context: Context, jsonObj :JSONObject) {
        Singleton_t_user.getInstance(context).jsonObject = jsonObj
    }
}