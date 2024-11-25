package com.tangoplus.tangoq.`object`

import android.content.Context
import android.util.Log
import com.tangoplus.tangoq.function.SecurePreferencesManager.getEncryptedJwtToken
import com.tangoplus.tangoq.function.SecurePreferencesManager.getEncryptedRefreshJwtToken
import com.tangoplus.tangoq.function.SecurePreferencesManager.saveEncryptedJwtToken
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object HttpClientProvider {
    private lateinit var client: OkHttpClient

    fun getClient(context: Context): OkHttpClient {
        if (!::client.isInitialized) {
            client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    var request = chain.request()
                    val accessToken = getEncryptedJwtToken(context)

                    // Access Token 추가
                    request = request.newBuilder()
                        .header("Authorization", "Bearer $accessToken")
                        .build()

                    val response = chain.proceed(request)

                    // 토큰 만료 시 처리
                    if (response.code == 401) {
                        response.close() // 기존 응답 닫기

                        // Refresh Token으로 새 Access Token 발급
                        val refreshToken = getEncryptedRefreshJwtToken(context)
                        val newToken = refreshAccessToken(context, refreshToken.toString())

                        if (newToken != null) {
                            saveEncryptedJwtToken(context, newToken)

                            // 새 토큰으로 요청 다시 시도
                            request = request.newBuilder()
                                .header("Authorization", "Bearer ${newToken.getString("jwt_token")}")
                                .build()
                            return@addInterceptor chain.proceed(request)
                        }
                    }

                    response
                }
                .build()
        }
        return client

    }

    // ------# 만료됐을 때 다시 토큰 발급 받아 저장 #------
    private fun refreshAccessToken(context: Context, refreshToken: String): JSONObject? {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = JSONObject().put("refresh_jwt", refreshToken).toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://your.api/refresh_token_endpoint") // TODO Refresh 토큰 요청 URL
            .post(body)
            .build()

        return try {
            val response = OkHttpClient().newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                responseBody?.let { JSONObject(it) }
            } else {
                Log.e("RefreshToken", "Failed to refresh token: ${response.code}")
                null
            }
        } catch (e: Exception) {
            Log.e("RefreshTokenError", "Error refreshing token: ${e.message}")
            null
        }
    }
}