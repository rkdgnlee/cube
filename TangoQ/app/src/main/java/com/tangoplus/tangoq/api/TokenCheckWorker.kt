package com.tangoplus.tangoq.api

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.tangoplus.tangoq.api.HttpClientProvider.refreshAccessToken
import com.tangoplus.tangoq.function.SecurePreferencesManager.getEncryptedRefreshJwt

class TokenCheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val refreshToken = getEncryptedRefreshJwt(applicationContext)
        val response = refreshAccessToken(applicationContext, refreshToken.toString())

        // 로그아웃 처리
        if (response != 200) {
            Log.v("TokenCheckWorker", "다른 기기에서 로그인됨. 로그아웃 처리")
            return Result.failure()
        }
        Log.d("TokenCheckWorker", "토큰 유효성 확인 완료")
        return Result.success()
    }
}