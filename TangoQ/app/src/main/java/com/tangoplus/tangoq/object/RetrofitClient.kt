package com.tangoplus.tangoq.`object`


import android.content.Context
import com.tangoplus.tangoq.R
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitClient {
    fun getInstance(context: Context, myUrl: String): ViewingHistory {
        val retrofit = Retrofit.Builder()
            .baseUrl("${context.getString(R.string.IP_ADDRESS_t_viewing_history)}/$myUrl/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ViewingHistory::class.java)
    }
}
