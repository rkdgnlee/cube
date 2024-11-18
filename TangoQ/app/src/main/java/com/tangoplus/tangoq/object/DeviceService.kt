package com.tangoplus.tangoq.`object`

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.tangoplus.tangoq.function.SecurePreferencesManager.getEncryptedJwtToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object DeviceService {
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
            else -> false
        }
    }

    @SuppressLint("HardwareIds")
    fun getSSAID(fragmentActivity: FragmentActivity): String {
        val ssaid = Settings.Secure.getString(
            fragmentActivity.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        return ssaid
    }
    // ------# device UUID 받는 API #------
    suspend fun getDeviceUUID(myUrl: String, context: Context, jo : JSONObject, callback: (String) -> Unit) {
        val mediaType = "application/json; chartset=utf-8".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, jo.toString())
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer ${getEncryptedJwtToken(context)}")
                .build()
            chain.proceed(newRequest)
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
        val request = Request.Builder()
            .url("${myUrl}users")
            .post(body)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).enqueue(object : Callback  {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e("응답실패", "$e")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            callback(extractMobileDeviceUuid(responseBody))
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("SSAIDError", e.stackTraceToString())
            }
        }
    }
    fun extractMobileDeviceUuid(jsonString: String): String {
        val regex = """"mobile_device_uuid":\s*"([^"]+)"""".toRegex()
        return regex.find(jsonString)?.groupValues?.get(1) ?: ""
    }
}