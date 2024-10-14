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
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.TimeUnit

object NetworkMeasure {


    suspend fun sendMeasureData(context: Context, myUrl: String, requestBody: RequestBody) {
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
            .post(requestBody)
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->

                val responseBody = response.body?.string()
                val motherJo = responseBody.let { JSONObject(it.toString()) }
                Log.w("getAllMeasures", "Success to execute request: $responseBody")


                // TODO 현재 어떻게 받아와지는 지에 대해서 알아야 함.
                // ------# db 초기화 #------
                val md = MeasureDatabase.getDatabase(context)
                val mDao = md.measureDao()
                val infoJo = motherJo.getJSONObject("measure_info")
                val userSn = infoJo.optInt("user_sn")
                val userUUID = infoJo.optString("user_uuid")

                val sn = mDao.getMaxMobileInfoSn(userSn) // 앱 내 room에 measureSn을 넣기 위한 sn

                // ------# info 테이블에 넣기 #------
                mDao.insertInfo(infoJo.toMeasureInfo())

                // ------# static 넣기 #------
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

                // ------# dynamic 넣기 #------
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

    suspend fun saveAllMeasureInfo(context: Context, myUrl: String, userUUID: String, userSn: Int, callback : (Boolean) -> Unit) { // 1845가 들어감.
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
                val bodyJo = JSONObject(responseBody.toString())

                // TODO 여기서 body에 측정 결과가 없다고 한다면 빈리스트 반환
                if (bodyJo.optInt("row_count") == 0) {
                    Log.v("getAllMeasureOut", "rowCount: ${bodyJo.optInt("row_count")}, stop the getAllMeasures")
                    return@withContext callback(false)
                }
                val ja = bodyJo.getJSONArray("data") // 3개가 들어가있음.
                val roomInfoSns =  mDao.getAllSns(userSn) // 1845의 server sn인 sn을 가져옴

                val getInfos = mutableListOf<MeasureInfo>()
//                for (i in 0 until ja.length()) {
//                    val jo = ja.optJSONObject(i)
//                    getInfos.add(jo.toMeasureInfo())
//                }
                val jo = ja.optJSONObject(1) // 현재 0번째 index는 7가지 동작이 없음.
                getInfos.add(jo.toMeasureInfo())
                Log.v("Room>getInfos", "$getInfos")

                val newInfos = getInfos.filter { apiInfo ->
                    apiInfo.sn !in roomInfoSns
                }

                // ------# 없는 것들만 필터링 #------
                Log.v("db없는infoSn", "newInfos: ${newInfos}")

                Log.v("db없는infoSn", "newInfos: ${newInfos.map { it.sn }}")
                newInfos.forEach{ newInfo ->
                    mDao.insertInfo(newInfo)
                    getMeasureResult(context, myUrl, newInfo.sn!!, userSn, userUUID)
                }
                return@withContext callback(true)
            }
        }
    }
    // -------------# TODO 전제: 측정 결과 모바일에서 업로드할 때 무조건 sn을 저장해야함 #------------------

    // 측정 결과 1개 sequence 전체 가져오기
    suspend fun getMeasureResult(context: Context, myUrl: String, infoSn: Int, userSn: Int, userUUID: String) : Result<Unit> {
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer ${getEncryptedJwtToken(context)}")
                .build()
            chain.proceed(newRequest)
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS) // 연결 타임아웃
            .readTimeout(30, TimeUnit.SECONDS)    // 읽기 타임아웃
            .writeTimeout(30, TimeUnit.SECONDS)   // 쓰기 타임아웃
            .build()
        val request = Request.Builder()
            .url("${myUrl}/$infoSn")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->

                    if (!response.isSuccessful) {
                        // 서버 응답이 성공하지 않았을 경우 처리
                        return@withContext Result.failure(Exception("Failed to fetch data: ${response.code}"))
                    }

                    // ------# db 초기화 #------
                    val md = MeasureDatabase.getDatabase(context)
                    val mDao = md.measureDao()
                    val sn = mDao.getMaxMobileInfoSn(userSn)
                    val responseBody = response.body?.string()
                    Log.w("getMeasureResult", "$responseBody")

                    val motherJo = responseBody?.let { JSONObject(it) }
                    if (motherJo != null) {
                        for (i in 0 until motherJo.optString("count").toInt() - 1) {
                            val staticJo = motherJo.optJSONObject("static_${i+1}")
                            if (staticJo != null) {
                                Log.v("스태틱조1~6", "${staticJo.optInt("measure_seq")}")
                                mDao.insertWithAutoIncrementStatic(staticJo.toMeasureStatic(), userUUID, measureSn = sn)
                                val fileName = staticJo.optString("measure_server_file_name")
                                val jsonName = staticJo.optString("measure_server_json_name")
                                saveFileFromUrl(context, fileName, FileStorageUtil.FileType.IMAGE)
                                saveFileFromUrl(context, jsonName, FileStorageUtil.FileType.JSON)
                            }
                        }
                        val dynamicJo = motherJo.optJSONObject("dynamic")
                        if (dynamicJo != null) {
                            Log.v("다이나믹", "${dynamicJo.optInt("measure_seq")}")
                            mDao.insertWithAutoIncrementDynamic(dynamicJo.toMeasureDynamic(), userUUID, dynamicJo.optInt("result_index"), measureSn = sn)
                            val fileName = dynamicJo.optString("measure_server_file_name")
                            val jsonName = dynamicJo.optString("measure_server_json_name")
                            saveFileFromUrl(context, fileName, FileStorageUtil.FileType.VIDEO)
                            saveFileFromUrl(context, jsonName, FileStorageUtil.FileType.JSON)
                        }
                    }
                    return@withContext Result.success(Unit)
                }

            } catch (e: SocketTimeoutException) {
                // 타임아웃 처리
                Log.e("getMeasureResultError", "Request timed out", e)
                return@withContext Result.failure(Exception("Request timed out"))
            } catch (e: IOException) {
                // 네트워크 문제 처리
                Log.e("getMeasureResultError", "Network error", e)
                return@withContext Result.failure(Exception("Network error: ${e.message}"))
            } catch (e: Exception) {
                // 일반적인 예외 처리
                Log.e("getMeasureResultError", "Error fetching measure result", e)
                return@withContext Result.failure(e)
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


}