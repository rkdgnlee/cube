package com.tangoplus.tangoq.`object`

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import com.google.android.gms.common.api.Api.Client
import com.tangoplus.tangoq.db.SecurePreferencesManager.getEncryptedJwtToken
import com.tangoplus.tangoq.db.SecurePreferencesManager.saveEncryptedJwtToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

object NetworkUser {

    // ------! 토큰 + 사용자 정보로 로그인 유무 확인 !------
    fun getUserBySdk(myUrl: String, userJsonObject: JSONObject, context: Context, callback: (JSONObject?) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, userJsonObject.toString())
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer ${getEncryptedJwtToken(context)}")
                .build()
            chain.proceed(newRequest)
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
        val request = Request.Builder()
            .url("${myUrl}oauth.php")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback{
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Token응답실패", "Failed to execute request")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()?.substringAfter("response: ")
                Log.e("SDK>Server>User", "$responseBody")
                val jo = responseBody?.let { JSONObject(it) }

                // ------! 토큰 저장 !------
                val token = jo?.optString("jwt")
                if (token != null) {
                    Log.v("token" , token)
                    saveEncryptedJwtToken(context, token)
                }
                callback(jo)
            }
        })
    }

    // 자체 로그인
    suspend fun getUserIdentifyJson(myUrl: String,  idPw: JSONObject, context: Context, callback: (JSONObject?) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, idPw.toString())
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}login.php")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("${ContentValues.TAG}, 응답실패", "Failed to execute request!")
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    Log.v("응답성공", "$responseBody")
                    val jo = responseBody?.let { JSONObject(it) }

                    // ------! 토큰 저장 !------
                    val jsonObj = JSONObject()
                    jsonObj.put("jwt", jo?.optString("jwt"))
                    jsonObj.put("refresh_jwt", jo?.optString("refresh_jwt"))
                    saveEncryptedJwtToken(context, jsonObj.toString())

                    callback(jo)
                }
            })
        }
    }

    // ------# 마케팅 수신 동의 관련 insert문 #------
    fun insertMarketingBySn(myUrl: String,  idPw: JSONObject, userToken: String, callback: (JSONObject?) -> Unit) {
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
                Log.e("응답실패", "Failed to execute request!")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.e("응답성공", "$responseBody")
                val jo = responseBody?.let { JSONObject(it) }
                callback(jo)
            }
        })
    }


    fun fetchUserUPDATEJson(myUrl : String, json: String, sn: String, callback: () -> Unit) {
        val client = OkHttpClient()
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("${myUrl}user/$sn")
            .patch(body)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("UPDATE 응답실패", "Failed to execute request!")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.v("UPDATE 응답성공", "$responseBody")
                callback()
            }
        })
    }

    fun fetchUserDeleteJson(myUrl : String, sn:String, callback: () -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}user/$sn")
            .delete()
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("${ContentValues.TAG}, 응답실패", "Failed to execute request!")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.v("${ContentValues.TAG}, 응답성공", "$responseBody")
                callback()
            }
        })
    }

    // ------! 프로필 사진 시작 !------
    suspend fun sendProfileImage(context: Context, myUrl: String, sn: String, requestBody: RequestBody) {
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer ${getEncryptedJwtToken(context)}")
                .build()
            chain.proceed(newRequest)
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
        Log.d("sendProfileImage", "myUrl: $myUrl, sn: $sn")
        val request = Request.Builder()
            .url("${myUrl}users/$sn")
            .post(requestBody)
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                val motherJo = responseBody.let { JSONObject(it.toString()) }
                Log.w("profileImage", "Success to execute request: $responseBody")
            }
        }
    }

    // ------! 프로필 사진 끝 !------

    fun storeUserInSingleton(context: Context, jsonObj :JSONObject) {
        Singleton_t_user.getInstance(context).jsonObject = jsonObj.optJSONObject("login_data")
    }

    // ------# 핀번호 로그인 #------
    suspend fun loginWithPin(myUrl: String,  pinNum: Int, userUUID: String) : Int {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}?category=24&login_pin_number=$pinNum&device_serial_number=SERIALNUMBERTANGOPLUS&user_uuid=$userUUID")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val code = response.code
                val responseBody = response.body?.string()
                Log.v("PIN>ResponseBody", "$responseBody")
                code
            }
        }
    }


    // ------# QRCode 로그인 #------
    suspend fun loginWithQRCode(myUrl: String, userUUID: String) : Int {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}?category=26&device_serial_number=SERIALNUMBERTANGOPLUS&user_uuid:$userUUID")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val statusCode = response.code
                val responseBody = response.body?.string()
                Log.v("QRcode>ResponseBody", "$responseBody")
                statusCode
            }
        }
    }
}