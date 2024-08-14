package com.tangoplus.tangoq.`object`

import com.tangoplus.tangoq.data.HistoryVO
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface ViewingHistory {
    @GET("viewing-history")
    fun getViewingHistory(): List<HistoryVO>

    @POST("viewing-history")

    fun addViewingHistory(@HeaderMap userId : String, @Body history: HistoryVO): Call<Void>
}