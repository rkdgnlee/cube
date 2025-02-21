package com.tangoplus.tangoq.api

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.tangoplus.tangoq.api.HttpClientProvider.refreshAccessToken
import com.tangoplus.tangoq.function.SecurePreferencesManager.getEncryptedRefreshJwt

class TokenCheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Log.d("TokenCheckWorker", "Worker 실행 시작")
        val refreshToken = getEncryptedRefreshJwt(applicationContext)
        Log.d("TokenCheckWorker", "Refresh Token: $refreshToken") // 🔍 확인용 로그 추가
        val response = refreshAccessToken(applicationContext, refreshToken.toString())
        Log.d("TokenCheckWorker", "Refresh token response code: $response") // 🔍 응답 코드 확인

        // 로그아웃 처리
        if (response != 200) {
            Log.v("TokenCheckWorker", "inappropriate response code: $response")
            return Result.failure()
        }
        Log.d("TokenCheckWorker", "토큰 유효성 확인 완료")
        return Result.success()
    }

    override fun onStopped() {
        super.onStopped()
        Log.e("TokenCheckWorker", "Worker가 중단됨! WorkManager가 강제로 종료한 가능성 있음")
    }
}