package com.tangoplus.tangoq.api

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tangoplus.tangoq.R
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
                        val responseCode = refreshAccessToken(context, refreshToken.toString())
                        val newAccessToken = getEncryptedAccessJwt(context)
                        if (responseCode == 200) {
                            request = request.newBuilder()
                                .header("Authorization", "Bearer $newAccessToken")
                                .build()
                            return@addInterceptor chain.proceed(request)
                        } else if (responseCode == 400) {
                            LogoutManager.notifyLogout()
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
    fun refreshAccessToken(context: Context, refreshToken: String) : Int {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body =
            JSONObject().put("refresh_token", refreshToken).toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(context.getString(R.string.API_refresh))
            .post(body)
            .build()

        return OkHttpClient().newCall(request).execute().use { response ->
            Log.v("TryAccessToken", "accessToken시도")
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
            } catch (e: Exception) {
                Log.e("RefreshTokenError", "refresh: ${e.message}")
                400
            }
        }
    }

    fun scheduleTokenCheck(context: Context) {
        val tokenCheckRequest = PeriodicWorkRequestBuilder<TokenCheckWorker>(15, TimeUnit.MINUTES)
//            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST) // 강제 실행
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // 네트워크 필요 없음
                    .setRequiresBatteryNotLow(false) // 배터리 부족 시 실행 허용
                    .setRequiresCharging(false) // 충전 중이 아니어도 실행
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "TokenCheckWork",
            ExistingPeriodicWorkPolicy.UPDATE
            ,tokenCheckRequest)
    }
}