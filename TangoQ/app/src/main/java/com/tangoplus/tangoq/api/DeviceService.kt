package com.tangoplus.tangoq.api

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.tangoplus.tangoq.api.HttpClientProvider.getClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
        val body = jo.toString().toRequestBody(mediaType)
        val client = getClient(context)
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
            } catch (e: IndexOutOfBoundsException) {
                Log.e("SSAIDIndex", "${e.message}")
            } catch (e: IllegalArgumentException) {
                Log.e("SSAIDIllegal", "${e.message}")
            } catch (e: IllegalStateException) {
                Log.e("SSAIDIllegal", "${e.message}")
            }catch (e: NullPointerException) {
                Log.e("SSAIDNull", "${e.message}")
            } catch (e: java.lang.Exception) {
                Log.e("SSAIDException", "${e.message}")
            }
        }
    }
    fun extractMobileDeviceUuid(jsonString: String): String {
        val regex = """"mobile_device_uuid":\s*"([^"]+)"""".toRegex()
        return regex.find(jsonString)?.groupValues?.get(1) ?: ""
    }

    fun playIntegrityVerify(myUrl: String, token: String, callback: (JSONObject) -> Unit){
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = token.toRequestBody(mediaType)
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(myUrl)
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string()
            if (responseBody != null) {
                val bodyJo = JSONObject(responseBody)
                Log.v("verifyIntegrity", "Success to execute request: $responseBody")
                callback(bodyJo)
            }
            return@use response.code
        }
    }
    // pose landmark build용 (null 처리) emulator 확인 함수
    fun isEmulator(): Boolean {
        return Build.PRODUCT.contains("sdk") ||
                Build.PRODUCT.contains("emulator") ||
                Build.PRODUCT.contains("android_x86") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu") ||
                Build.MODEL.contains("sdk") ||
                Build.MODEL.contains("emulator") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.MANUFACTURER == "Google" ||
                Build.BRAND.contains("generic") ||
                Build.DEVICE.contains("generic") ||
                Build.FINGERPRINT.contains("generic") ||
                Build.FINGERPRINT.contains("unknown") ||
                Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown")
    }
}