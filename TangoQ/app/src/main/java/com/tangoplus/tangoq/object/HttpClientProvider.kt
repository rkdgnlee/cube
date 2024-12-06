package com.tangoplus.tangoq.`object`

import android.content.Context
import android.util.Log
import com.tangoplus.tangoq.function.SecurePreferencesManager.getEncryptedAccessJwt
import com.tangoplus.tangoq.function.SecurePreferencesManager.getEncryptedRefreshJwt
import com.tangoplus.tangoq.function.SecurePreferencesManager.saveEncryptedJwtToken
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object HttpClientProvider {
    private lateinit var client: OkHttpClient

    fun getClient(context: Context): OkHttpClient {
        if (!::client.isInitialized) {
            client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    var request = chain.request()
                    val accessToken = getEncryptedAccessJwt(context)
                    request = request.newBuilder()
                        .header("Authorization", "Bearer $accessToken")
                        .build()
                    val response = chain.proceed(request)
                    // 토큰 만료 시 처리
                    if (response.code == 401) {
                        response.close() // 기존 응답 닫기
                        // Refresh Token으로 새 Access Token 발급
                        val refreshToken = getEncryptedRefreshJwt(context)
                        val newToken = refreshAccessToken(refreshToken.toString())

                        if (newToken != null) {
                            saveEncryptedJwtToken(context, newToken)
                            // 새 토큰으로 요청 다시 시도
                            request = request.newBuilder()
                                .header("Authorization", "Bearer ${newToken.getString("access_jwt")}")
                                .build()
                            return@addInterceptor chain.proceed(request)
                        }
                    }
                    response
                }
                .connectTimeout(10, TimeUnit.SECONDS) // 연결 타임아웃
                .readTimeout(30, TimeUnit.SECONDS)    // 읽기 타임아웃
                .writeTimeout(30, TimeUnit.SECONDS)   // 쓰기 타임아웃
                .build()
        }
        return client
    }


    // ------# 만료됐을 때 다시 토큰 발급 받아 저장 #------
    private fun refreshAccessToken(refreshToken: String): JSONObject? {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = JSONObject().put("refresh_token", refreshToken).toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://gym.tangoplus.co.kr/api/refresh.php")
            .post(body)
            .build()

        return try {
            val response = OkHttpClient().newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.v("토큰갱신", "Success to refresh Access Token")
                responseBody?.let { JSONObject(it) }
            } else {
                Log.e("RefreshToken", "Failed to refresh token: ${response.code}")
                null
            }
        } catch (e: IndexOutOfBoundsException) {
            Log.e("RefreshTokenError", "refresh: ${e.message}")
            null
        } catch (e: IllegalArgumentException) {
            Log.e("RefreshTokenError", "refresh: ${e.message}")
            null
        } catch (e: IllegalStateException) {
            Log.e("RefreshTokenError", "refresh: ${e.message}")
            null
        }catch (e: NullPointerException) {
            Log.e("RefreshTokenError", "refresh: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e("RefreshTokenError", "refresh: ${e.message}")
            null
        }
    }
}