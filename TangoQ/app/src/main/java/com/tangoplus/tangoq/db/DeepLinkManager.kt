package com.tangoplus.tangoq.db

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.tangoplus.tangoq.MainActivity

object DeepLinkManager {
    const val DEEP_LINK_PATH_KEY = "DEEPLINK_PATH_KEY"
    const val EXERCISE_ID_KEY = "EXERCISE_ID_KEY"

    fun handleDeepLink(context: Context, uri: Uri) {

        // 스킴과 호스트가 맞는지 확인
        if (uri.scheme != "tangoplus" || uri.host != "tangoq") {
            handleUnknownLink(context)
            return
        }
        val deepLinkPath = when (uri.path) {
            "/1" -> {
                val exerciseId = uri.getQueryParameter("exerciseId")
                if (exerciseId != null) "PT" else ""
            }
            "/2" -> "MD1"
            "/3" -> "MD"
            "/4" -> "RD"
            else -> "DEFAULT"
        }
        // MainActivity로 딥링크 정보 전달
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(DEEP_LINK_PATH_KEY, deepLinkPath)
            if (deepLinkPath == "PT") {
                val exerciseId = uri.getQueryParameter("exerciseId")
                putExtra(EXERCISE_ID_KEY, exerciseId)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }

    private fun handleUnknownLink(context: Context) {
        // 알 수 없는 딥링크 처리: 에러 화면 또는 메인 화면으로 이동
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }



}