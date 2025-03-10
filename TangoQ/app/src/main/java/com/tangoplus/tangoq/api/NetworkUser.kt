package com.tangoplus.tangoq.api

import android.content.Context
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

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
//                        Log.v("trySelfLogin", "$responseBody")
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
    // (각 플랫폼 sdk를 통해서 자동 로그인까지 동작)
    fun getUserBySdk(myUrl: String, userJsonObject: JSONObject, context: Context, callback: (JSONObject?) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = userJsonObject.toString().toRequestBody(mediaType)
        val client = getClient(context)
        val request = Request.Builder()
            .url("${myUrl}oauth.php")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback{
            override fun onFailure(call: Call, e: IOException) {
                Log.e("failedSdkLogin", "Failed to execute request")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()?.substringAfter("response: ")
                // Log.v("SDK>Server>User", "$responseBody")

                if (response.code == 401) {
                    callback(null)
                } else {
                    val jo = responseBody?.let { JSONObject(it) }
                    // ------! 토큰 저장 !------
                    val token = jo?.optString("jwt")
                    if (token != null) {
                        val jsonObj = JSONObject()
                        jsonObj.put("access_jwt", jo.optString("access_jwt"))
                        jsonObj.put("refresh_jwt", jo.optString("refresh_jwt"))
                        saveEncryptedJwtToken(context, jsonObj)
                    }
                    callback(jo)
                }
            }
        })
    }

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
//                     Log.v("자체로그인Success", "$responseBody")
                    val jo = responseBody?.let { JSONObject(it) }
                    // ------# 저장 후 로그인 정보는 callback으로 반환 #------
                    callback(jo)
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
        val client = OkHttpClient()
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
                // Log.v("verifyPW", "$responseBody")
                val status = responseBody?.let { JSONObject(it).optInt("status") }
                if (status != null) {
                    withContext(Dispatchers.Main) {
                        callback(status)
                    }
                }
            }
        }
    }

    suspend fun insertUser(myUrl: String,  idPw: JSONObject, callback: (Int) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = idPw.toString().toRequestBody(mediaType)
        val client = OkHttpClient()
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
//                         Log.v("응답성공", "code: ${response.code}, body: $responseBody")

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

    suspend fun idDuplicateCheck(myUrl: String, userId: String) : Int? {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("${myUrl}check_user_id.php?user_id=$userId")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("회원update", "ResponseCode: ${response.code}")
                        return@withContext null
                    }

                    response.body?.string()?.let { responseString ->
                        // Log.v("중복확인로그", "code: ${response.code}, body: $responseString")
                        val bodyJo = JSONObject(responseString)
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
                        Log.e("회원update", "ResponseCode: ${response.code}")
                        return@withContext null
                    }
                    val responseBody = response.body?.string()
                    // Log.v("회원update", "$responseBody")
                    return@withContext true
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
                        Log.e("회원탈퇴Error", "ResponseCode: ${response.body.toString()} ${response.code}")
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

    fun findUserId(context: Context, myUrl: String, joBody: String, callback: (String) -> Unit) {

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = joBody.toRequestBody(mediaType)
//        val client = getClient(context)
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
                // Log.v("userId", "$responseBody")
                val jo = responseBody?.let { JSONObject(it) }
                val id = jo?.optString("user_id")
                if (id != null) {
                    callback(id)
                } else {
                    callback("")
                }
            }
        })
    }

    // ------! 프로필 사진 시작 !------
    suspend fun sendProfileImage(context: Context, myUrl: String, sn: String, requestBody: RequestBody, callback: (String) -> Unit) {
        val client = getClient(context)
        Log.d("sendProfileImage", "myUrl: $myUrl, sn: $sn")
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
            client.newCall(request).execute().use { response ->
                val code = response.code
                val responseBody = response.body?.string()
                // Log.v("PIN>ResponseBody", "$responseBody")
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
                // Log.v("QRcode>ResponseBody", "$responseBody")
                statusCode
            }
        }
    }

    private fun extractProfileImageUrl(jsonString: String): String {
        val regex = """"file_path":\s*"([^"]+)"""".toRegex()
        return regex.find(jsonString)?.groupValues?.get(1) ?: ""
    }
}