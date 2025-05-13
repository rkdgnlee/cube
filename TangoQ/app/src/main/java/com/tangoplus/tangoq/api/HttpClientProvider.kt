package com.tangoplus.tangoq.api

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.tangoplus.tangoq.MyApplication
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.function.SecurePreferencesManager.getEncryptedAccessJwt
import com.tangoplus.tangoq.function.SecurePreferencesManager.getEncryptedRefreshJwt
import com.tangoplus.tangoq.function.SecurePreferencesManager.saveEncryptedJwtToken
import com.tangoplus.tangoq.function.WifiManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

object HttpClientProvider {
    private lateinit var client: OkHttpClient
    private var processLogoutTrigger = false
    private var isRefreshing = false  // 토큰 갱신 중인지 확인하는 플래그
    private val refreshLock = Any()   // 동기화를 위한 락 객체

    fun getClient(context: Context): OkHttpClient {
        if (!::client.isInitialized) {
            client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val networkType = WifiManager(context).checkNetworkType()
                    if (networkType == "none") {
                        Log.v("networkType", networkType)
                        throw IOException("네트워크 연결이 없습니다")
                    }
                    var request = chain.request()
                    val accessToken = getEncryptedAccessJwt(context)
                    Log.v("액세스토큰", "$accessToken")
                    request = request.newBuilder()
                        .header("Authorization", "Bearer $accessToken")
                        .build()
                    val response = chain.proceed(request)
                    // 토큰 만료 시 처리
                    Log.v("inGetClient", "access expired?: ${response.code}")
                    if (response.code in listOf(400, 401, 404, 500)) {
                        response.close() // 기존 응답 닫기

                        // 중복 토큰 갱신 요청 방지
                        synchronized(refreshLock) {
                            if (!isRefreshing) {
                                isRefreshing = true
                                try {
                                    // Refresh Token으로 새 Access Token 발급
                                    val refreshToken = getEncryptedRefreshJwt(context)
                                    val responseCode = refreshAccessToken(context, refreshToken.toString())

                                    if (responseCode == 200) {
                                        val newAccessToken = getEncryptedAccessJwt(context)
                                        request = request.newBuilder()
                                            .header("Authorization", "Bearer $newAccessToken")
                                            .build()
                                        return@addInterceptor chain.proceed(request)
                                    } else if (responseCode == 400 && !processLogoutTrigger) {
                                        Log.e("RefreshTokenFail", "send to Code to AppViewModel")
                                        (context.applicationContext as MyApplication).appViewModel.triggerLogout()
                                        processLogoutTrigger = true
                                        Handler(Looper.getMainLooper()).postDelayed({ processLogoutTrigger = false }, 2000)
                                    } else {

                                    }
                                } finally {
                                    // 작업 완료 후 플래그 초기화
                                    isRefreshing = false
                                }
                            } else {
                                // 이미 다른 요청이 토큰 갱신 중인 경우 잠시 대기
                                try {
                                    // 최대 5초간 대기
                                    for (i in 1..10) {
                                        Thread.sleep(500) // 0.5초씩 대기
                                        // 토큰 갱신이 완료되었는지 확인
                                        if (!isRefreshing) {
                                            val newAccessToken = getEncryptedAccessJwt(context)
                                            request = request.newBuilder()
                                                .header("Authorization", "Bearer $newAccessToken")
                                                .build()
                                            return@addInterceptor chain.proceed(request)
                                        }
                                    }
                                    // 타임아웃 - 너무 오래 기다렸을 경우
                                    Log.e("RefreshTokenTimeout", "Token refresh timeout after waiting")
                                } catch (e: InterruptedException) {
                                    Log.e("RefreshTokenError", "Waiting interrupted: ${e.message}")
                                }
                            }
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
    private fun refreshAccessToken(context: Context, refreshToken: String): Int {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body =
            JSONObject().put("refresh_token", refreshToken).toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(context.getString(R.string.API_refresh))
            .post(body)
            .build()

        // 토큰 갱신용 클라이언트에 타임아웃 설정 추가
        val tokenRefreshClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)  // 연결 타임아웃 (5초)
            .readTimeout(5, TimeUnit.SECONDS)     // 읽기 타임아웃 (5초)
            .writeTimeout(5, TimeUnit.SECONDS)    // 쓰기 타임아웃 (5초)
            .build()

        return tokenRefreshClient.newCall(request).execute().use { response ->
            try {
                if (!response.isSuccessful) {
                    val code = response.code
                    Log.e("RefreshTokenFail", "Failed to access token: $code")
                    code
                } else if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.v("토큰갱신", "$responseBody")
                    val newToken = responseBody?.let { JSONObject(it) }
                    saveEncryptedJwtToken(context, newToken)
                    200
                } else {
                    Log.e("RefreshTokenElse", "Failed to access token: ${response.code}")
                    400
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("RefreshTokenError", "refresh: ${e.message}")
                400
            } catch (e: IllegalArgumentException) {
                Log.e("RefreshTokenError", "refresh: ${e.message}")
                400
            } catch (e: IllegalStateException) {
                Log.e("RefreshTokenError", "refresh: ${e.message}")
                400
            } catch (e: NullPointerException) {
                Log.e("RefreshTokenError", "refresh: ${e.message}")
                400
            } catch (e: SocketTimeoutException) {
                Log.e("RefreshTokenError", "Timeout: ${e.message}")
                408 // 타임아웃에 대한 별도 코드 반환
            } catch (e: Exception) {
                Log.e("RefreshTokenError", "refresh: ${e.message}")
                400
            }
        }
    }

}