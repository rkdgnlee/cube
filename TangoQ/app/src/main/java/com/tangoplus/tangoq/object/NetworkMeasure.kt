package com.tangoplus.tangoq.`object`

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.db.FileStorageUtil
import com.tangoplus.tangoq.db.FileStorageUtil.saveFileFromUrl
import com.tangoplus.tangoq.db.MeasureDatabase
import com.tangoplus.tangoq.db.MeasureDynamic
import com.tangoplus.tangoq.db.MeasureInfo
import com.tangoplus.tangoq.db.MeasureStatic
import com.tangoplus.tangoq.db.SecurePreferencesManager.getEncryptedJwtToken
import com.tangoplus.tangoq.mediapipe.ImageProcessingUtility.decodeSampledBitmapFromFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object NetworkMeasure {


    fun insertMeasureData(myUrl:String, json: String, files: List<File>, context: Context , callback: () -> Unit) {
        val client = OkHttpClient()

        val requestBodyBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("json", json)

        for (i in 0 until 7) {
            val file = files[i]
            if (i == 1) {
                requestBodyBuilder.addFormDataPart(
                    "image",
                    file.name,
                    file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
            } else {
                requestBodyBuilder.addFormDataPart(
                    "video",
                    file.name,
                    file.asRequestBody("video/mp4".toMediaTypeOrNull())
                )
            }
        }
        val requestBody = requestBodyBuilder.build()

        val request = Request.Builder()
            .url("${myUrl}/") // TODO: URL 수정 필요
            .post(requestBody) // 빌드된 RequestBody 객체 전달
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.v("HTTP>MeasureFetch", "Failed to execute request!!")
                Toast.makeText(context, "데이터 연결이 실패했습니다. 잠시 후에 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.v("HTTP>MeasureFetch", "$responseBody")
                callback()
            }
        })
    }


    suspend fun saveAllMeasureInfo(context: Context, myUrl: String, userUUID: String, userSn: Int ) { // 1845가 들어감.
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
            .url(myUrl)
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                // ------# db 초기화 #------
                val md = MeasureDatabase.getDatabase(context)
                val mDao = md.measureDao()

                val responseBody = response.body?.string()
                Log.w("getAllMeasures", "Success to execute request: $responseBody")

                val ja = responseBody.let { JSONObject(it.toString()).getJSONArray("data") }
                val roomInfoSns =  mDao.getAllSns(userSn) // 1845

                val getInfos = mutableListOf<MeasureInfo>()
                val jo = ja.optJSONObject(1)
                getInfos.add(jo.toMeasureInfo())

                val newInfos = getInfos.filter { apiInfo ->
                    apiInfo.sn !in roomInfoSns
                } // 없는 것들만 필터링
                Log.v("db없는infoSn", "newInfos: $newInfos")
                newInfos.forEach{ newInfo ->
                    mDao.insertInfo(newInfo)
                    getMeasureResult(context, myUrl, newInfo.sn!!, userSn, userUUID)
                }
            }
        }
    }
    // -------------# TODO 전제: 측정 결과 모바일에서 업로드할 때 무조건 sn을 저장해야함 #------------------

    // 측정 결과 1개 sequence 전체 가져오기
    private suspend fun getMeasureResult(context: Context, myUrl: String, infoSn: Int, userSn: Int, userUUID: String) {
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
            .url("${myUrl}/$infoSn")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->

                // ------# db 초기화 #------
                val md = MeasureDatabase.getDatabase(context)
                val mDao = md.measureDao()
                val sn = mDao.getMaxMobileInfoSn(userSn)
                val responseBody = response.body?.string()
                Log.w("getMeasureResult", "$responseBody")
                val motherJo = responseBody.let { JSONObject(it.toString()) }
                for (i in 0 until motherJo.optString("count").toInt() - 1) { // TODO 현재 홍길동의 첫 번째 info는 사용불가.
                    val staticJo = motherJo.optJSONObject("static_${i+1}")
                    if (staticJo != null) {
                        Log.v("스태틱조1~6","${staticJo.optInt("measure_seq")}")
                        mDao.insertWithAutoIncrementStatic(staticJo.toMeasureStatic(), userUUID, measureSn = sn)
                        val fileName = staticJo.optString("measure_server_file_name")
                        val jsonName = staticJo.optString("measure_server_json_name")
                        saveFileFromUrl(context, fileName, FileStorageUtil.FileType.IMAGE)
                        saveFileFromUrl(context, jsonName, FileStorageUtil.FileType.JSON)
                    }
                }
                val dynamicJo = motherJo.optJSONObject("dynamic")
                if (dynamicJo != null) {
                    Log.v("다이나믹","${dynamicJo.optInt("measure_seq")}")
                    mDao.insertWithAutoIncrementDynamic(dynamicJo.toMeasureDynamic(), userUUID, dynamicJo.optInt("result_index"), measureSn = sn)
                    val fileName = dynamicJo.optString("measure_server_file_name")
                    val jsonName = dynamicJo.optString("measure_server_json_name")
                    saveFileFromUrl(context, fileName, FileStorageUtil.FileType.VIDEO)
                    saveFileFromUrl(context, jsonName, FileStorageUtil.FileType.JSON)
                }
            }
        }
    }

    fun JSONObject.toMeasureStatic(): MeasureStatic {
        return Gson().fromJson(this.toString(), MeasureStatic::class.java)
    }
    fun JSONObject.toMeasureDynamic(): MeasureDynamic {
        return Gson().fromJson(this.toString(), MeasureDynamic::class.java)
    }
    fun JSONObject.toMeasureInfo(): MeasureInfo {
        return Gson().fromJson(this.toString(), MeasureInfo::class.java)
    }



    // ------# 결과 받아오면서 db에 저장하면서 #------


    // 가장 최신 측정 1개 가져오기
    suspend fun fetchMeasureResult(myUrl: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("$myUrl/read.php")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.w("NetMeasure7", "Success to execute request: $responseBody")

            }
        }
    }
}