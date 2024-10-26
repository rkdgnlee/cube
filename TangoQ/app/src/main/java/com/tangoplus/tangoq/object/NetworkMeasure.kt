package com.tangoplus.tangoq.`object`

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.db.FileStorageUtil
import com.tangoplus.tangoq.db.FileStorageUtil.getFile
import com.tangoplus.tangoq.db.FileStorageUtil.readJsonArrayFile
import com.tangoplus.tangoq.db.FileStorageUtil.readJsonFile
import com.tangoplus.tangoq.db.FileStorageUtil.saveFileFromUrl
import com.tangoplus.tangoq.db.MeasureDatabase
import com.tangoplus.tangoq.db.MeasureDynamic
import com.tangoplus.tangoq.db.MeasureInfo
import com.tangoplus.tangoq.db.MeasureStatic
import com.tangoplus.tangoq.db.MeasurementManager.getDangerParts
import com.tangoplus.tangoq.db.SecurePreferencesManager.getEncryptedJwtToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

object NetworkMeasure {

    suspend fun sendMeasureData(context: Context, myUrl: String, requestBody: RequestBody, infoSn: Int, staticSns: MutableList<Int>, dynamicSn: Int, callback: (JSONObject) -> Unit) : Result<Unit> {
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
        Log.v("sendMeasureData", "Try to send MultipartBody")
        return withContext(Dispatchers.IO) {

            try {
                client.newCall(request).execute().use { response ->

                    if (!response.isSuccessful) {
                        // 서버 응답이 성공하지 않았을 경우 처리
                        Log.e("전송실패3", "$response")
                        Log.e("전송실패3", "body: ${response.body?.string()}")
                        callback(JSONObject())
                        return@withContext Result.failure(Exception("Failed to fetch data: ${response.code}"))

                    }

                    // ------# db 초기화 #------
                    val md = MeasureDatabase.getDatabase(context)
                    val mDao = md.measureDao()

                    val responseBody = response.body?.string()
                    Log.w("getMeasureResult", "$responseBody")

                    val bodyJo = JSONObject(responseBody.toString())
                    val infoJo = bodyJo.optJSONObject("measure_info")

                    // TODO DB에는 이미 저장됐고, 값을 받아와서 DB의 내용을 수정해야함. callback으로 나가게. false로 나가
                    val measureInfoSn = infoJo?.optInt("sn")
                    val infoUploaded = infoJo?.optString("uploaded")
                    val uploadDate = infoJo?.optString("upload_date")
                    mDao.updateAndGetInfo(infoSn, measureInfoSn, infoUploaded, uploadDate)

                    // ------# static 테이블 수정 #------
                    for (i in 0 until staticSns.size) {
                        val staticJo = bodyJo.optJSONObject("static_${i+1}")
                        if (staticJo != null) {
                            val serverSn = staticJo.optInt("server_sn")
                            val staticUploaded = staticJo.optString("uploaded")
                            val staticUploadDate = staticJo.optString("upload_date")
                            val staticUploadJson = staticJo.optString("uploaded_json")
                            val staticUploadFile = staticJo.optString("uploaded_file")

                            mDao.updateAndGetStatic(staticSns[i], serverSn, staticUploaded, staticUploadDate, staticUploadJson, staticUploadFile) // TODO body에서 static에서 부여받은 값넣기.
                        }
                    }

                    val dynamicJo = bodyJo.optJSONObject("dynamic")
                    if (dynamicJo != null) {
                        val serverSn = dynamicJo.optInt("server_sn")
                        val dynamicUploaded = dynamicJo.optString("uploaded")
                        val dynamicUploadDate = dynamicJo.optString("upload_date")
                        val dynamicUploadJson = dynamicJo.optString("uploaded_json")
                        val dynamicUploadFile = dynamicJo.optString("uploaded_file")
                        mDao.updateAndGetDynamic(dynamicSn, serverSn, dynamicUploaded, dynamicUploadDate, dynamicUploadJson, dynamicUploadFile)
                    }


//                    // ------# info로 변환 #------
//                    val info = infoJo?.toMeasureInfo()
//                    if (info != null) {
//                        mDao.insertInfo(info)
//                    }
//
//                    // ------# static, dynamic 싱글턴에 넣을 변수 #------
//                    val ja = JSONArray()
//                    val uris = mutableListOf<String>()
//                    val baseUrl = "https://gym.tangostar.co.kr/data/Results/"
//
//                    // ------# static, dynamic 넣기 #------
//                    for (i in 0 until bodyJo.optString("count").toInt()) { // 0 ~ 7
//                        val jsonFile: File?
//                        val mediaFile: File?
//                        if (i == 0) {
//                            val staticJo = bodyJo.optJSONObject("static_${i+1}")
//                            if (staticJo != null) {
//                                Log.v("스태틱조1~6", "${staticJo.optInt("measure_seq")}")
//                                mDao.insertWithAutoIncrementStatic(staticJo.toMeasureStatic(), userUUID)
//                                val fileName = staticJo.optString("measure_server_file_name")
//                                val jsonName = staticJo.optString("measure_server_json_name")
//                                saveFileFromUrl(context, fileName, FileStorageUtil.FileType.IMAGE)
//                                saveFileFromUrl(context, jsonName, FileStorageUtil.FileType.JSON)
//
//                                jsonFile = getFile(context, jsonName.replace(baseUrl, ""))
//                                mediaFile = getFile(context, fileName.replace(baseUrl, ""))
//                                if (jsonFile != null && mediaFile != null) {
//                                    ja.put(readJsonFile(jsonFile))
//                                    uris.add(mediaFile.absolutePath)
//                                    Log.v("URI,FILENAME0", "uris: ${uris}, jsonFile: $jsonFile, mediaFile: $mediaFile")
//                                }
//                            }
//                        } else if (i == 1) {
//                            val dynamicJo = bodyJo.optJSONObject("dynamic")
//                            if (dynamicJo != null) {
//                                Log.v("다이나믹", "${dynamicJo.optInt("measure_seq")}")
//                                mDao.insertWithAutoIncrementDynamic(dynamicJo.toMeasureDynamic(), userUUID, dynamicJo.optInt("result_index"))
//                                val fileName = dynamicJo.optString("measure_server_file_name")
//                                val jsonName = dynamicJo.optString("measure_server_json_name")
//                                saveFileFromUrl(context, fileName, FileStorageUtil.FileType.VIDEO)
//                                saveFileFromUrl(context, jsonName, FileStorageUtil.FileType.JSON)
//
//                                jsonFile = getFile(context, jsonName.replace(baseUrl, ""))
//                                mediaFile = getFile(context, fileName.replace(baseUrl, ""))
//                                if (jsonFile != null && mediaFile != null) {
//                                    ja.put(readJsonArrayFile(jsonFile))
//                                    uris.add(mediaFile.absolutePath)
//                                    Log.v("URI,FILENAME1", "uris: ${uris}, jsonFile: $jsonFile, mediaFile: $mediaFile")
//                                }
//                            }
//                        } else {
//                            val staticJo = bodyJo.optJSONObject("static_${i}")
//                            if (staticJo != null) {
//                                Log.v("스태틱조1~6", "${staticJo.optInt("measure_seq")}")
//                                mDao.insertWithAutoIncrementStatic(staticJo.toMeasureStatic(), userUUID)
//                                val fileName = staticJo.optString("measure_server_file_name")
//                                val jsonName = staticJo.optString("measure_server_json_name")
//                                saveFileFromUrl(context, fileName, FileStorageUtil.FileType.IMAGE)
//                                saveFileFromUrl(context, jsonName, FileStorageUtil.FileType.JSON)
//
//                                jsonFile = getFile(context, jsonName.replace(baseUrl, ""))
//                                mediaFile = getFile(context, fileName.replace(baseUrl, ""))
//                                if (jsonFile != null && mediaFile != null) {
//                                    ja.put(readJsonFile(jsonFile))
//                                    uris.add(mediaFile.absolutePath)
//                                    Log.v("URI,FILENAME2~6", "uris: ${uris}, jsonFile: $jsonFile, mediaFile: $mediaFile")
//                                }
//                            }
//                        }
//                    }
//                    val dangerParts = if (info != null) getDangerParts(info) else emptyList()
//                    val measureVO = MeasureVO(
//                        deviceSn = 0,
//                        sn = info?.sn!!,
//                        regDate = info.measure_date,
//                        overall = info.t_score,
//                        dangerParts = dangerParts.toMutableList(),
//                        measureResult = ja,
//                        fileUris = uris,
//                        isMobile = info.device_sn == 0,
//                        recommendations = null
//                    )
//                    Log.v("measureAdd전", "${measureVO}, 변경전 갯수: ${Singleton_t_measure.getInstance(context).measures?.size}")
//                    Singleton_t_measure.getInstance(context).measures?.add(0, measureVO)
//                    Log.v("measureAdd후", "변경후 갯수: ${Singleton_t_measure.getInstance(context).measures?.size}")
                    callback(bodyJo)

                    return@withContext Result.success(Unit)
                }

            } catch (e: SocketTimeoutException) {
                // 타임아웃 처리
                Log.e("getMeasureResultError", "Request timed out", e)
                callback(JSONObject())
                return@withContext Result.failure(Exception("Request timed out"))
            } catch (e: IOException) {
                // 네트워크 문제 처리
                Log.e("getMeasureResultError", "Network error", e)
                callback(JSONObject())
                return@withContext Result.failure(Exception("Network error: ${e.message}"))
            } catch (e: Exception) {
                // 일반적인 예외 처리
                Log.e("getMeasureResultError", "Error fetching measure result", e)
                callback(JSONObject())
                return@withContext Result.failure(e)
            }
        }
    }

    suspend fun saveAllMeasureInfo(context: Context, myUrl: String, userUUID: String,  callback : (Boolean) -> Unit) { // 1845가 들어감.
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
//                Log.w("getAllMeasures", "Success to execute request: $responseBody")
                val bodyJo = JSONObject(responseBody.toString())

                // TODO 여기서 body에 측정 결과가 없다고 한다면 빈리스트 반환
                if (bodyJo.optInt("row_count") == 0) {
                    Log.v("getAllMeasureOut", "rowCount: ${bodyJo.optInt("row_count")}, stop the getAllMeasures")
                    return@withContext callback(false)
                }
                val ja = bodyJo.getJSONArray("data") // 3개가 들어가있음.
                val roomInfoSns =  mDao.getAllSns(userUUID) // 1845의 server sn인 sn을 가져옴
                Log.v("룸에저장된info들", "$roomInfoSns")
//                Log.v("룸에저장된static", "${mDao.getAllStatic(userUUID)}")
                val getInfos = mutableListOf<MeasureInfo>()
//                for (i in 0 until ja.length()) {
//                    val jo = ja.optJSONObject(i)
//                    getInfos.add(jo.toMeasureInfo())
//                }
                val jo = ja.optJSONObject(1) // 현재 0번째 index는 7가지 동작이 없음.
                getInfos.add(jo.toMeasureInfo())
                Log.v("Room>getInfos", "${mDao.getAllInfo(userUUID)}")

                val newInfos = getInfos.filter { apiInfo ->
                    apiInfo.sn !in roomInfoSns
                }

                // ------# 없는 것들만 필터링 #------
                Log.v("db없는infoSn", "newInfos: ${newInfos}")

                Log.v("db없는infoSn", "newInfos: ${newInfos.map { it.sn }}")
                newInfos.forEach{ newInfo ->
                    mDao.insertInfo(newInfo)
                    getMeasureResult(context, myUrl, newInfo.sn!!,  userUUID)
                }
                return@withContext callback(true)
            }
        }
    }
    // -------------# TODO 전제: 측정 결과 모바일에서 업로드할 때 무조건 sn을 저장해야함 #------------------

    // 측정 결과 1개 sequence 전체 가져오기
    suspend fun getMeasureResult(context: Context, myUrl: String, measureInfoSn: Int, userUUID: String) : Result<Unit> {
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
            .url("${myUrl}/$measureInfoSn")
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
                    val responseBody = response.body?.string()
                    Log.w("getMeasureResult", "$responseBody")

                    val motherJo = responseBody?.let { JSONObject(it) }
                    if (motherJo != null) {
                        for (i in 0 until motherJo.optString("count").toInt() - 1) {
                            val staticJo = motherJo.optJSONObject("static_${i+1}")
                            if (staticJo != null) {
                                Log.v("스태틱조1~6", "${staticJo.optInt("measure_seq")}")
                                mDao.insertByStatic(staticJo.toMeasureStatic())
                                val fileName = staticJo.optString("measure_server_file_name")
                                val jsonName = staticJo.optString("measure_server_json_name")
                                saveFileFromUrl(context, fileName, FileStorageUtil.FileType.IMAGE)
                                saveFileFromUrl(context, jsonName, FileStorageUtil.FileType.JSON)
                            }
                        }
                        val dynamicJo = motherJo.optJSONObject("dynamic")
                        if (dynamicJo != null) {
                            Log.v("다이나믹", "${dynamicJo.optInt("measure_seq")}")
                            mDao.insertByDynamic(dynamicJo.toMeasureDynamic())
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