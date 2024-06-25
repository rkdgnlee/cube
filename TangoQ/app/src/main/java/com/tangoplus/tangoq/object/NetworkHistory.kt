package com.tangoplus.tangoq.`object`

import android.content.Context
import com.tangoplus.tangoq.data.HistoryVO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NetworkHistory {
    suspend fun fetchViewingHistory(context : Context, myUrl: String): List<HistoryVO> {
        val apiService = RetrofitClient.getInstance(context, myUrl)
        return withContext(Dispatchers.IO) {
            apiService.getViewingHistory()
        }
    }

    suspend fun insertViewingHistory(context : Context, myUrl: String, userId : String, historyVO: HistoryVO) {
        val apiService = RetrofitClient.getInstance(context, myUrl)
        return withContext(Dispatchers.IO) {
            apiService.addViewingHistory(userId, historyVO)
        }
    }
}