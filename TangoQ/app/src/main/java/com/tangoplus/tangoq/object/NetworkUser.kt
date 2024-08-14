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
            .url("${myUrl}oauth_create.php")
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
                    Log.v("token" , token.substring(0, 10))
                    saveEncryptedJwtToken(context, token)
                }
                callback(jo)
            }
        })
    }

    // ------! 마케팅 수신 동의 관련 insert문 !------  ###  ### ---> 여기서도 토큰이 생김
    fun getUserIdentifyJson(myUrl: String,  idPw: JSONObject, context: Context, callback: (JSONObject?) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, idPw.toString())
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
            .url("${myUrl}login.php/")
            .post(body)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("${ContentValues.TAG}, 응답실패", "Failed to execute request!")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.v("${ContentValues.TAG}, 응답성공", "$responseBody")
                val jo = responseBody?.let { JSONObject(it) }

                // ------! 토큰 저장 !------
                val token = jo?.optString("jwt")
                if (token != null) {
                    Log.v("token" , token.substring(0, 10))
                    saveEncryptedJwtToken(context, token)
                }
                callback(jo)
            }
        })
    }
    // ------! 마케팅 수신 동의 관련 insert문 !------  ###  ### ---> 여기서도 토큰이 생김
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
                Log.e("${ContentValues.TAG}, 응답실패", "Failed to execute request!")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.e("${ContentValues.TAG}, 응답성공", "$responseBody")
                val jo = responseBody?.let { JSONObject(it) }
                callback(jo)
            }
        })
    }

    /* ------------------------------------------CRUD---------------------------------------------*/
//    fun fetchUserINSERTJson(myUrl : String, json: String, callback: () -> Unit){
//        val client = OkHttpClient()
//        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)
//        val request = Request.Builder()
//            .url("${myUrl}create.php")
//            .post(body) // post방식으로 insert 들어감
//            .build()
//
//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                Log.e("${ContentValues.TAG}, 응답실패", "Failed to execute request!")
//            }
//            override fun onResponse(call: Call, response: Response)  {
//                val responseBody = response.body?.string()
//                Log.v("${ContentValues.TAG}, 응답성공", "$responseBody")
//                callback()
//            }
//        })
//    }

    fun fetchUserUPDATEJson(myUrl : String, json: String, sn: String, callback: () -> Unit) {
        val client = OkHttpClient()
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json )
        val request = Request.Builder()
            .url("${myUrl}update.php?user_sn=$sn")
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
            .url("${myUrl}delete.php?user_sn=$sn")
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
    suspend fun uploadProfileImage(url: String, iv: ImageView) {
        withContext(Dispatchers.IO) {
            try {
                val bitmap = (iv.drawable as BitmapDrawable).bitmap

                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                val imageBytes = byteArrayOutputStream.toByteArray()
                val encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT)

                val requestBody = FormBody.Builder()
                    .add("image", encodedImage)
                    .build()
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = OkHttpClient().newCall(request).execute()
                if (response.isSuccessful) {
                    Log.v("upload", "Upload is Successful")
                } else {
                    Log.v("upload", "Upload is failed")
                }
            } catch (e: Exception) {
                Log.e("error", "${e.message}")
            }
        }
    }

    // ------! 프로필 사진 끝 !------

    fun storeUserInSingleton(context: Context, jsonObj :JSONObject) {
        Singleton_t_user.getInstance(context).jsonObject = jsonObj.optJSONObject("login_data")
    }
}