package com.tangoplus.tangoq.api

import android.accounts.NetworkErrorException
import android.content.Context
import android.net.http.NetworkException
import android.util.Log
import com.tangoplus.tangoq.function.SecurePreferencesManager.getEncryptedRefreshJwt
import com.tangoplus.tangoq.function.SecurePreferencesManager.saveEncryptedJwtToken
import com.tangoplus.tangoq.api.HttpClientProvider.getClient
import com.tangoplus.tangoq.db.Singleton_t_user
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

object NetworkUser {
    const val TAG = "NetworkUser"

    // 아이디 비밀번호 자체 로그인
    suspend fun trySelfLogin(myUrl: String, context: Context, refreshToken: String?, callback: (JSONObject?) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body =
            JSONObject().put("refresh_token", refreshToken).toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("${myUrl}auto_login.php")
            .post(body)
            .build()
        return withContext(Dispatchers.IO) {
            OkHttpClient().newCall(request).enqueue(object : Callback{
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("Token응답실패", "Failed to execute request")
                    callback(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val responseBody = response.body?.string()
                        Log.v("trySelfLogin", "$responseBody")
                        val bodyJo = JSONObject(responseBody.toString())
                        if (bodyJo.optJSONObject("login_data") == null) {
                            callback(null)
                        } else {
                            val jwtJo = JSONObject().apply {
                                put("access_jwt", bodyJo.optString("access_jwt"))
                                put("refresh_jwt", bodyJo.optString("refresh_jwt"))
                            }
                            saveEncryptedJwtToken(context, jwtJo)
                            callback(bodyJo)
                        }
                    } catch (e: IndexOutOfBoundsException) {
                        Log.e("autoSelfLogin", "IndexError: ${e.message}")
                    } catch (e: IllegalArgumentException) {
                        Log.e("autoSelfLogin", "IllegalArgumentError: ${e.message}")
                    } catch (e: IllegalStateException) {
                        Log.e("autoSelfLogin", "IllegalStateError: ${e.message}")
                    } catch (e: NullPointerException) {
                        Log.e("autoSelfLogin", "NullPointerError: ${e.message}")
                    } catch (e: java.lang.Exception) {
                        Log.e("autoSelfLogin", "Exception: ${e.message}")
                    }
                }
            })
        }
    }

    // ------! 토큰 + 사용자 정보로 로그인 유무 확인 !------
    // 각 플랫폼 sdk를 통해서 버튼 눌렀을 때 처음 처리 (이메일 확인 후 처리할 건지 말건지)

    fun getUserBySdk(myUrl: String, userJsonObject: JSONObject, context: Context, callback: (JSONObject?) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = userJsonObject.toString().toRequestBody(mediaType)
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS) // 연결 타임아웃
            .readTimeout(5, TimeUnit.SECONDS)    // 읽기 타임아웃
            .writeTimeout(5, TimeUnit.SECONDS)   // 쓰기 타임아웃
            .build()
        val request = Request.Builder()
            .url("${myUrl}oauth.php")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback{
            override fun onFailure(call: Call, e: IOException) {
                Log.e("failedSdkLogin", "Failed to execute request")
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    Log.e("responseCode", "${response.code}")
                    if (response.code == 500) {
                        return callback(null)
                    }
                    val responseBody = response.body?.string()  // ?.substringAfter("response: ")
                    Log.v("oauthResult", "$responseBody")
                    val responseJo = responseBody?.let { JSONObject(it).optInt("status") }
                    Log.v("responseJo", "$responseJo")
                    if (responseJo !in listOf(200, 201, 202)) {

                        // TODO 여기서 oauth.php의 responseCode를 보고 여기서 처리
                        // TODO 1. 최초 회원가입 2. 기존 이메일이 있는 user면 바로 결과처리 (토큰 저장, user정보 callback으로 보내기)
                        return callback(null)
                    } else {
                        val jo = responseBody?.let { JSONObject(it) }
                        // ------! 토큰 저장 !------

                        val jsonObj = JSONObject()
                        jsonObj.put("access_jwt", jo?.optString("access_jwt"))
                        jsonObj.put("refresh_jwt", jo?.optString("refresh_jwt"))
                        saveEncryptedJwtToken(context, jsonObj)

                        return callback(jo)
                    }
                } catch (e: NetworkErrorException) {
                    Log.e("sdkError", "Network Error Login By SDK : ${e.message}")
                } catch (e: IllegalStateException) {
                    Log.e("sdkError", "IllegalState Error Login By SDK : ${e.printStackTrace()}")
                } catch (e: IllegalArgumentException) {
                    Log.e("sdkError", "IllegalArgument Error Login By SDK : ${e.message}")
                } catch (e: SocketTimeoutException) {
                    Log.e("sdkError", "Socket Timeout Error Login By SDK : ${e.message}")
                } catch (e: Exception) {
                    Log.e("sdkError", "Error Login By SDK : ${e.printStackTrace()}")
                }
            }
        })
    }

    // 소셜 로그인에서 이메일만 받아서 보내기
//    suspend fun identifyEmail(myUrl: String, emailBody: JSONObject, callback: (JSONObject?) -> Unit) {
//        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
//        val body = emailBody.toString().toRequestBody(mediaType)
//        val client = OkHttpClient.Builder()
//            .connectTimeout(10, TimeUnit.SECONDS)
//            .readTimeout(10, TimeUnit.SECONDS)
//            .writeTimeout(10, TimeUnit.SECONDS)
//            .build()
//        val request = Request.Builder()
//            .url("${myUrl}.php")
//            .post(body)
//            .build()
//        return withContext(Dispatchers.IO) {
//            try {
//                client.newCall(request).enqueue(object: Callback {
//                    override fun onFailure(call: Call, e: IOException) {
//                        Log.e("FailedToEmail", "failed To identify Email: ${e.message}")
//                        CoroutineScope(Dispatchers.Main).launch {
//                            callback(null)
//                        }
//                    }
//                    override fun onResponse(call: Call, response: Response) {
//                        val responseBody = response.body?.string()
//                        val bodyJo = responseBody?.let { JSONObject(it) }
//                        CoroutineScope(Dispatchers.Main).launch {
//                            callback(bodyJo)
//                        }
//                    }
//                })
//            } catch (e: NetworkErrorException) {
//                Log.e("identifyEmail", "Network Error Login By identifyEmail : ${e.message}")
//                callback(null)
//            } catch (e: IllegalStateException) {
//                Log.e("identifyEmail", "IllegalState Error Login By identifyEmail : ${e.message}")
//                callback(null)
//            } catch (e: IllegalArgumentException) {
//                Log.e("identifyEmail", "IllegalArgument Error Login By identifyEmail : ${e.message}")
//                callback(null)
//            } catch (e: SocketTimeoutException) {
//                Log.e("identifyEmail", "Socket Timeout Error Login By identifyEmail : ${e.message}")
//                callback(null)
//            } catch (e: Exception) {
//                Log.e("identifyEmail", "Error Login By identifyEmail : ${e.message}")
//                callback(null)
//            }
//        }
//    }



    // Id, Pw 로그인
    suspend fun getUserIdentifyJson(myUrl: String,  idPw: JSONObject, callback: (JSONObject?) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = idPw.toString().toRequestBody(mediaType)
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}login.php")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("자체로그인Failed", "Failed to execute request!")
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(null)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
//                    Log.v("자체로그인Success", "$responseBody")
                    val jo = responseBody?.let { JSONObject(it) }
                    // ------# 저장 후 로그인 정보는 callback으로 반환 #------
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(jo)
                    }
                }
            })
        }
    }


    // Id, Pw 자동 로그인 (토큰)
    suspend fun logoutDenyRefreshJwt(myUrl: String, context: Context, callback: (Int) -> Unit) {
        val bodyJo = JSONObject()
        bodyJo.put("refresh_token", getEncryptedRefreshJwt(context))
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = bodyJo.toString().toRequestBody(mediaType)
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS) // 연결 타임아웃
            .readTimeout(5, TimeUnit.SECONDS)    // 읽기 타임아웃
            .writeTimeout(5, TimeUnit.SECONDS)   // 쓰기 타임아웃
            .build()
        val request = Request.Builder()
            .url("$myUrl/logout.php")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                withContext(Dispatchers.Main) {
                    callback(response.code)
                    // Log.v("logout", "${response.code}")
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("UserIndex", "refresh: ${e.message}")
            } catch (e: IllegalArgumentException) {
                Log.e("UserIllegal", "refresh: ${e.message}")
            } catch (e: IllegalStateException) {
                Log.e("UserIllegal", "refresh: ${e.message}")
            } catch (e: NullPointerException) {
                Log.e("UserNull", "refresh: ${e.message}")
            } catch (e: java.lang.Exception) {
                Log.e("UserException", "refresh: ${e.message}")
            }
        }
    }

    suspend fun insertUser(myUrl: String,  idPw: JSONObject, insertToken: String,callback: (Int) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = idPw.toString().toRequestBody(mediaType)
        val client = OkHttpClient.Builder()
            .addInterceptor{ chain ->
                var request = chain.request()
                request = request.newBuilder()
                    .header("Authorization", "Bearer $insertToken")
                    .build()
                chain.proceed(request)
            }
            .build()
        val request = Request.Builder()
            .url("${myUrl}register.php")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e("insertUserFailed", "Failed to execute request!")
                        callback(500) // 실패 시 에러 코드 전달
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseBody = response.body?.string()
                         Log.v("응답성공", "code: ${response.code}, body: $responseBody")

                        if (response.isSuccessful) {
                            Log.v("회원가입로그", "${response.code}")
                            callback(response.code)
                        } else {
                            callback(response.code) // 에러 코드 전달
                            Log.v("회원가입로그", "${response.code}")
                        }
                    }
                })
            } catch (e: IndexOutOfBoundsException) {
                Log.e("UserIndex", "${e.message}")
                callback(500)
            } catch (e: IllegalArgumentException) {
                Log.e("UserIllegal", "${e.message}")
                callback(500)
            } catch (e: IllegalStateException) {
                Log.e("UserIllegal", "${e.message}")
                callback(500)
            } catch (e: NullPointerException) {
                Log.e("UserNull", "${e.message}")
                callback(500)
            } catch (e: java.lang.Exception) {
                Log.e("UserException", "${e.message}")
                callback(500)
            }
        }
    }

    suspend fun fetchUserUPDATEJson(context: Context, myUrl : String, json: String, sn: String) : Boolean? {
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val client = getClient(context)
        val request = Request.Builder()
            .url("${myUrl}users/$sn")
            .patch(body)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val responseBody = response.body?.string()
                        Log.e("회원updateFailed", "ResponseCode: ${responseBody.toString()}")
                        withContext(Dispatchers.Main) {
                            return@withContext null
                        }
                    }
                    val responseBody = response.body?.string()
                    Log.v("회원update", "$responseBody")
                    withContext(Dispatchers.Main) {
                        return@withContext true
                    }
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "${e.message}")
                null
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "${e.message}")
                null
            } catch (e: NullPointerException) {
                Log.e(TAG, "${e.message}")
                null
            } catch (e: ArrayIndexOutOfBoundsException) {
                Log.e(TAG, "${e.message}")
                null
            } catch (e: Exception) {
                Log.e(TAG, "${e.message}")
                null
            }
        }
    }

    suspend fun fetchUserDeleteJson(context : Context, myUrl : String, sn:String) : Int? {
        val client = getClient(context)
        val request = Request.Builder()
            .url("${myUrl}users/$sn")
            .delete()
            .build()
        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("회원탈퇴Error", "Failed to execute request!") //  ${response.body.toString()}
                        return@withContext null
                    }
                    val responseCode = response.code
//                     Log.v("회원탈퇴Success", "${response.body} $responseCode")
                    return@withContext responseCode
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "${e.message}")
                null
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "${e.message}")
                null
            } catch (e: NullPointerException) {
                Log.e(TAG, "${e.message}")
                null
            } catch (e: ArrayIndexOutOfBoundsException) {
                Log.e(TAG, "${e.message}")
                null
            } catch (e: Exception) {
                Log.e(TAG, "${e.message}")
                null
            }
        }
    }

    // 개인정보를 위한 비밀번호 검증
    suspend fun verifyPW(context: Context, myUrl: String, pw: JSONObject, callback: (Int) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = pw.toString().toRequestBody(mediaType)
        val client = getClient(context)
        val request = Request.Builder()
            .url("${myUrl}pwd_check.php")
            .post(body)
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.v("verifyPW", "$responseBody")
                val status = responseBody?.let { JSONObject(it).optInt("status") }
                if (status != null) {
                    withContext(Dispatchers.Main) {
                        callback(status)
                    }
                }
            }
        }
    }

    // 알리고 사용 휴대폰 - 본인인증 확인
    suspend fun sendMobileOTP(myUrl: String, bodyJo: String) : Int? {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = bodyJo.toRequestBody(mediaType)
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS) // 서버 연결 타임아웃 (10초)
            .readTimeout(8, TimeUnit.SECONDS)    // 응답 읽기 타임아웃 (30초)
            .writeTimeout(8, TimeUnit.SECONDS)   // 요청 쓰기 타임아웃 (15초)
            .build()
        val request = Request.Builder()
            .url("${myUrl}sms/curl_send.php")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("mobileOTP실패", "ResponseCode: ${response.code}")
                        return@withContext response.code
                    }

                    response.body?.string()?.let { responseString ->
                        val bodyJo = JSONObject(responseString)
                        Log.v("mobileOTP보내기", "$bodyJo")
                        return@withContext bodyJo.optInt("status")
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("UserIndex", "${e.message}")
                500
            } catch (e: IllegalArgumentException) {
                Log.e("UserIllegal", "${e.message}")
                500
            } catch (e: IllegalStateException) {
                Log.e("UserIllegal", "${e.message}")
                500
            } catch (e: NullPointerException) {
                Log.e("UserNull", "${e.message}")
                500
            } catch (e: java.lang.Exception) {
                Log.e("UserException", "${e.message}")
                500
            }
        }
    }

    suspend fun verifyMobileOTP(myUrl: String, bodyJo: String) : Pair<String, Int>? {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = bodyJo.toRequestBody(mediaType)
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS) // 서버 연결 타임아웃 (10초)
            .readTimeout(8, TimeUnit.SECONDS)    // 응답 읽기 타임아웃 (30초)
            .writeTimeout(8, TimeUnit.SECONDS)   // 요청 쓰기 타임아웃 (15초)
            .build()

        val request = Request.Builder()
            .url("${myUrl}sms/verify_sms.php")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("mobile인증확인", "ResponseCode: ${response.code}")
                        return@withContext null
                    }

                    response.body?.string()?.let { responseString ->
                        val responseJo = JSONObject(responseString)
                        Log.v("mobile인증확인", "$responseJo")
                        return@withContext Pair(responseJo.optString("data"), responseJo.optInt("status"))
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("mobileCondition Error", "IndexOutOfBoundsException: ${e.message}")
                null
            } catch (e: IllegalArgumentException) {
                Log.e("mobileCondition Error", "IllegalArgumentException: ${e.message}")
                null
            } catch (e: IllegalStateException) {
                Log.e("mobileCondition Error", "IllegalStateException: ${e.message}")
                null
            } catch (e: NullPointerException) {
                Log.e("mobileCondition Error", "NullPointerException: ${e.message}")
                null
            } catch (e: java.lang.Exception) {
                Log.e("mobileCondition Error", "Exception: ${e.message}")
                null
            }
        }
    }

    // 소셜 회원가입 시 사용하는 핸드폰 본인확인 인증 - 차이점: 소셜 계정의 email을 받아서
    suspend fun sendMobileOTPToSNS(myUrl: String, bodyJo: String) : Int? {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = bodyJo.toRequestBody(mediaType)
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS) // 서버 연결 타임아웃 (10초)
            .readTimeout(8, TimeUnit.SECONDS)    // 응답 읽기 타임아웃 (30초)
            .writeTimeout(8, TimeUnit.SECONDS)   // 요청 쓰기 타임아웃 (15초)
            .build()
        val request = Request.Builder()
            .url("${myUrl}sms/curl_send.php")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("mobileOTP실패", "ResponseCode: ${response.code}")
                        return@withContext response.code
                    }

                    response.body?.string()?.let { responseString ->
                        val bodyJo = JSONObject(responseString)
                        Log.v("mobileOTP보내기", "$bodyJo")
                        return@withContext bodyJo.optInt("status")
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("UserIndex", "${e.message}")
                500
            } catch (e: IllegalArgumentException) {
                Log.e("UserIllegal", "${e.message}")
                500
            } catch (e: IllegalStateException) {
                Log.e("UserIllegal", "${e.message}")
                500
            } catch (e: NullPointerException) {
                Log.e("UserNull", "${e.message}")
                500
            } catch (e: java.lang.Exception) {
                Log.e("UserException", "${e.message}")
                500
            }
        }
    }

    suspend fun verifyMobileOTPToSNS(myUrl: String, bodyJo: String) : Pair<String, Int>? {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = bodyJo.toRequestBody(mediaType)
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS) // 서버 연결 타임아웃 (10초)
            .readTimeout(8, TimeUnit.SECONDS)    // 응답 읽기 타임아웃 (30초)
            .writeTimeout(8, TimeUnit.SECONDS)   // 요청 쓰기 타임아웃 (15초)
            .build()

        val request = Request.Builder()
            .url("${myUrl}sms/verify_sms.php")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("mobile인증확인", "ResponseCode: ${response.code}")
                        return@withContext null
                    }

                    response.body?.string()?.let { responseString ->
                        val responseJo = JSONObject(responseString)
                        Log.v("mobile인증확인", "$responseJo")
                        return@withContext Pair(responseJo.optString("data"), responseJo.optInt("status"))
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("mobileCondition Error", "IndexOutOfBoundsException: ${e.message}")
                null
            } catch (e: IllegalArgumentException) {
                Log.e("mobileCondition Error", "IllegalArgumentException: ${e.message}")
                null
            } catch (e: IllegalStateException) {
                Log.e("mobileCondition Error", "IllegalStateException: ${e.message}")
                null
            } catch (e: NullPointerException) {
                Log.e("mobileCondition Error", "NullPointerException: ${e.message}")
                null
            } catch (e: java.lang.Exception) {
                Log.e("mobileCondition Error", "Exception: ${e.message}")
                null
            }
        }
    }


    // 아이디 찾기 / 이메일 가져오기
    suspend fun sendMobileOTPToFindEmail(myUrl: String, bodyJo: String) : Int? {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = bodyJo.toRequestBody(mediaType)
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS) // 서버 연결 타임아웃 (10초)
            .readTimeout(8, TimeUnit.SECONDS)    // 응답 읽기 타임아웃 (30초)
            .writeTimeout(8, TimeUnit.SECONDS)   // 요청 쓰기 타임아웃 (15초)
            .build()
        val request = Request.Builder()
            .url("${myUrl}service/find_user_email.php")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("mobileOTP실패", "ResponseCode: ${response.code}")
                        return@withContext null
                    }

                    response.body?.string()?.let { responseString ->
                        val bodyJo = JSONObject(responseString)
                        Log.v("mobileOTP보내기", "$bodyJo")
                        return@withContext bodyJo.optInt("result_code")
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("UserIndex", "${e.message}")
                500
            } catch (e: IllegalArgumentException) {
                Log.e("UserIllegal", "${e.message}")
                500
            } catch (e: IllegalStateException) {
                Log.e("UserIllegal", "${e.message}")
                500
            } catch (e: NullPointerException) {
                Log.e("UserNull", "${e.message}")
                500
            } catch (e: java.lang.Exception) {
                Log.e("UserException", "${e.message}")
                500
            }
        }
    }

    suspend fun verityMobileOTPToFindEmail(myUrl: String, bodyJo: String) : String? {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = bodyJo.toRequestBody(mediaType)
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS) // 서버 연결 타임아웃 (10초)
            .readTimeout(8, TimeUnit.SECONDS)    // 응답 읽기 타임아웃 (30초)
            .writeTimeout(8, TimeUnit.SECONDS)   // 요청 쓰기 타임아웃 (15초)
            .build()

        val request = Request.Builder()
            .url("${myUrl}service/verify_find_email.php")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("mobile인증확인", "ResponseCode: ${response.code}")
                        return@withContext when (response.code) {
                            401 -> "otpFailed"
                            else -> null
                        }
                    }

                    response.body?.string()?.let { responseString ->
                        val responseJo = JSONObject(responseString)
                        Log.v("mobile인증확인", "$responseJo, ${responseJo.optInt("status")}")
                        return@withContext when (responseJo.optInt("status")) {
                            0 -> responseJo.optString("email")
                            401 -> "otpFailed" // otp 만료 및 otp 올바르지 않음
                            else -> null // 이외 서버에러 등
                        }
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("mobileCondition Error", "IndexOutOfBoundsException: ${e.message}")
                null
            } catch (e: IllegalArgumentException) {
                Log.e("mobileCondition Error", "IllegalArgumentException: ${e.message}")
                null
            } catch (e: IllegalStateException) {
                Log.e("mobileCondition Error", "IllegalStateException: ${e.message}")
                null
            } catch (e: NullPointerException) {
                Log.e("mobileCondition Error", "NullPointerException: ${e.message}")
                null
            } catch (e: java.lang.Exception) {
                Log.e("mobileCondition Error", "Exception: ${e.message}")
                null
            }
        }
    }

    suspend fun sendEmailOTP(myUrl: String, bodyJo: String) : Pair<Int, String?>? {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = bodyJo.toRequestBody(mediaType)
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS) // 서버 연결 타임아웃 (10초)
            .readTimeout(8, TimeUnit.SECONDS)    // 응답 읽기 타임아웃 (30초)
            .writeTimeout(8, TimeUnit.SECONDS)   // 요청 쓰기 타임아웃 (15초)
            .build()
        val request = Request.Builder()
            .url("${myUrl}service/verify_duplicate.php")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("emailOTP실패", "ResponseCode: ${response.code}")
                        val responseBody = response.body?.string()
                        val provider = responseBody?.let { JSONObject(it).optString("provider") }
                        return@withContext Pair(response.code, provider)
                    }

                    response.body?.string()?.let { responseString ->
                        val responseJo = JSONObject(responseString)
                        val provider = responseJo.optString("provider") ?: ""
                        Log.v("emailOTP보내기", "$responseJo")
                        return@withContext Pair(responseJo.optInt("status"), provider)
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("UserIndex", "${e.message}")
                Pair(500, null)
            } catch (e: IllegalArgumentException) {
                Log.e("UserIllegal", "${e.message}")
                Pair(500, null)
            } catch (e: IllegalStateException) {
                Log.e("UserIllegal", "${e.message}")
                Pair(500, null)
            } catch (e: NullPointerException) {
                Log.e("UserNull", "${e.message}")
                Pair(500, null)
            } catch (e: java.lang.Exception) {
                Log.e("UserException", "${e.message}")
                Pair(500, null)
            }
        }
    }

    suspend fun verifyEmailOTP(myUrl: String, bodyJo: String) : Int? {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = bodyJo.toRequestBody(mediaType)
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS) // 서버 연결 타임아웃 (10초)
            .readTimeout(8, TimeUnit.SECONDS)    // 응답 읽기 타임아웃 (30초)
            .writeTimeout(8, TimeUnit.SECONDS)   // 요청 쓰기 타임아웃 (15초)
            .build()

        val request = Request.Builder()
            .url("${myUrl}service/verify_reg_otp.php")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("emailVerify", "ResponseCode: ${response.code}")
                        return@withContext null
                    }

                    response.body?.string()?.let { responseString ->
                        val bodyJo = JSONObject(responseString)
                        Log.v("emailVerify", "$bodyJo")
                        return@withContext bodyJo.optInt("status")
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("mobileCondition Error", "IndexOutOfBoundsException: ${e.message}")
                500
            } catch (e: IllegalArgumentException) {
                Log.e("mobileCondition Error", "IllegalArgumentException: ${e.message}")
                500
            } catch (e: IllegalStateException) {
                Log.e("mobileCondition Error", "IllegalStateException: ${e.message}")
                500
            } catch (e: NullPointerException) {
                Log.e("mobileCondition Error", "NullPointerException: ${e.message}")
                500
            } catch (e: java.lang.Exception) {
                Log.e("mobileCondition Error", "Exception: ${e.message}")
                500
            }
        }
    }

    fun findUserEmail(myUrl: String, joBody: String, callback: (String) -> Unit) {

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = joBody.toRequestBody(mediaType)
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}find_user_id.php")
            .post(body)
            .build()
        client.newCall(request).enqueue(object: Callback{
            override fun onFailure(call: Call, e: IOException) {
                Log.e("IDPW찾기Failed", "Failed to execute request!")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                 Log.v("userId", "$responseBody")
                val jo = responseBody?.let { JSONObject(it) }
                val id = jo?.optString("email")
                if (id != null) {
                    callback(id)
                } else {
                    callback("")
                }
            }
        })
    }

    fun verifyPWCode(myUrl: String, joBody: String, callback: (JSONObject?) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = joBody.toRequestBody(mediaType)
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}verify_otp.php")
            .post(body)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("opt인증failed", "Failed to enqueue request")
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val responseBody = response.body?.string()
                    val jo = responseBody?.let{JSONObject(it)}
                    Log.v("opt인증", "$jo")
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(jo)
                    }
                } catch (e: NetworkErrorException) {
                    Log.e("verifyPWError", "Error by verifyPW NetworkErrorException : ${e.message}")
                } catch (e: IllegalStateException) {
                    Log.e("verifyPWError", "Error by verifyPW IllegalStateException : ${e.message}")
                } catch (e: IllegalArgumentException) {
                    Log.e("verifyPWError", "Error by verifyPW IllegalArgumentException : ${e.message}")
                } catch (e: SocketTimeoutException) {
                    Log.e("verifyPWError", "Error by verifyPW SocketTimeoutException : ${e.message}")
                } catch (e: Exception) {
                    Log.e("verifyPWError", "Error by verifyPW Exception : ${e.message}")
                }
            }
        })
    }

    fun sendPWCode(myUrl: String, joBody: String, callback: (Int) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = joBody.toRequestBody(mediaType)
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}send_otp.php")
            .post(body)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("이메일보내기failed", "Failed to enqueue request")
                callback(500)
            }

            override fun onResponse(call: Call, response: Response) {
               try {
                   val responseBody = response.body?.string()
                   val jo = responseBody?.let{ JSONObject(it) }
                   Log.v("이메일보내기", "${jo}")
                   val code = jo?.optInt("status")
                   if (code != null) {
                       CoroutineScope(Dispatchers.Main).launch {
                           callback(code)
                       }
                   }

               } catch (e: NetworkErrorException) {
                   Log.e("sendPWError", "Error by verifyPW NetworkErrorException : ${e.message}")
               } catch (e: IllegalStateException) {
                   Log.e("sendPWError", "Error by verifyPW IllegalStateException : ${e.message}")
               } catch (e: IllegalArgumentException) {
                   Log.e("sendPWError", "Error by verifyPW IllegalArgumentException : ${e.message}")
               } catch (e: SocketTimeoutException) {
                   Log.e("sendPWError", "Error by verifyPW SocketTimeoutException : ${e.message}")
               } catch (e: Exception) {
                   Log.e("sendPWError", "Error by verifyPW Exception : ${e.message}")
               }
            }
        })
    }

    fun resetPW(myUrl: String, token: String, body: String, callback: (Int?) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val joBody = body.toRequestBody(mediaType)

        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(newRequest)
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
        val request = Request.Builder()
            .url("${myUrl}reset_pwd.php")
            .post(joBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("resetPw", "Failed to resetPw")
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                val jo = responseBody?.let{ JSONObject(it) }
                val code = jo?.optInt("status")
                if (code != null) {
                    callback(code)
                }
            }
        })
    }
    fun resetLock(myUrl: String, token: String, body: String, callback: (Int?) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val joBody = body.toRequestBody(mediaType)

        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(newRequest)
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
        val request = Request.Builder()
            .url("${myUrl}reset_lock.php")
            .post(joBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("resetLock", "Failed to resetPw")
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                val jo = responseBody?.let{ JSONObject(it) }
                Log.v("응답값", jo.toString())
                val code = jo?.optInt("status")
                if (code != null) {
                    callback(code)
                }
            }
        })
    }
    
    // ------# webView에 PASS창 띄우기 #------
    fun sendMOKResult(myUrl: String, bodyJo: String ,callback: (JSONObject?) -> Unit )  {
        val mediaType = "application/json charset=utf-8".toMediaTypeOrNull()
        val body = bodyJo.toRequestBody(mediaType)
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url("${myUrl}sendMOKResult")
            .post(body)
            .build()
        client.newCall(request).enqueue(object: Callback{
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Token응답실패", "Failed to execute request")
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    Log.v("MOKLog", responseBody)
                }
                val jo = JSONObject(responseBody.toString())
                callback(jo)
            }
        })
    }
    
    
    // ------! 프로필 사진 시작 !------
    suspend fun sendProfileImage(context: Context, myUrl: String, sn: String, requestBody: RequestBody, callback: (String) -> Unit) {
        val client = getClient(context)
//        Log.d("sendProfileImage", "myUrl: $myUrl, sn: $sn")
        val request = Request.Builder()
            .url("${myUrl}users/$sn")
            .post(requestBody)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        // Log.w("profileImage", "Success to execute request: $responseBody")
                        callback(extractProfileImageUrl(responseBody))
                    }
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "${e.message}")
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "${e.message}")
            } catch (e: NullPointerException) {
                Log.e(TAG, "${e.message}")
            } catch (e: ArrayIndexOutOfBoundsException) {
                Log.e(TAG, "${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "${e.message}")
            }
        }
    }
    // ------! 프로필 사진 끝 !------
    fun storeUserInSingleton(context: Context, jsonObj :JSONObject) {
        Singleton_t_user.getInstance(context).jsonObject = jsonObj.optJSONObject("login_data")
        Singleton_t_user.getInstance(context).jsonObject?.put("profile_file_path", jsonObj.optJSONObject("profile_file_path")?.optString("file_path"))
    }

    // ------# 핀번호 로그인 #------
    suspend fun loginWithPin(myUrl: String,  pinNum: Int, userUUID: String) : Int {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}?category=24&login_pin_number=$pinNum&device_serial_number=SERIALNUMBERTANGOPLUS&user_uuid=$userUUID")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val code = response.code
                    val responseBody = response.body?.string()
                    Log.v("PIN>ResponseBody", "$responseBody, $code")
                    code
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "${e.message}")
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "${e.message}")
            } catch (e: NullPointerException) {
                Log.e(TAG, "${e.message}")
            } catch (e: ArrayIndexOutOfBoundsException) {
                Log.e(TAG, "${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "${e.message}")
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
            try {
                client.newCall(request).execute().use { response ->
                    val statusCode = response.code
                    val responseBody = response.body?.string()
                     Log.v("QRcode>ResponseBody", "$responseBody, $statusCode")
                    statusCode
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "${e.message}")
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "${e.message}")
            } catch (e: NullPointerException) {
                Log.e(TAG, "${e.message}")
            } catch (e: ArrayIndexOutOfBoundsException) {
                Log.e(TAG, "${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "${e.message}")
            }

        }
    }

    private fun extractProfileImageUrl(jsonString: String): String {
        val regex = """"file_path":\s*"([^"]+)"""".toRegex()
        return regex.find(jsonString)?.groupValues?.get(1) ?: ""
    }

}