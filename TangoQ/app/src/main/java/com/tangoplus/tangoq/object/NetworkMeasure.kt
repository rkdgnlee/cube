package com.tangoplus.tangoq.`object`

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.tangoplus.tangoq.db.FileStorageUtil
import com.tangoplus.tangoq.db.FileStorageUtil.saveFileFromUrl
import com.tangoplus.tangoq.db.MeasureDatabase
import com.tangoplus.tangoq.db.MeasureDynamic
import com.tangoplus.tangoq.db.MeasureInfo
import com.tangoplus.tangoq.db.MeasureStatic
import com.tangoplus.tangoq.db.SecurePreferencesManager.getEncryptedJwtToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

object NetworkMeasure {

    // ------# 전송 실패된 항목 POST #------
    suspend fun resendMeasureFile(context: Context, myUrl: String, requestBody: RequestBody, isStatic: Boolean, serverMeasureSn: Int, mobileDbSn: Int,  callback: (Pair<String,String>?) -> Unit) : Result<Unit> {
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
            .url("$myUrl/$serverMeasureSn")
            .post(requestBody)
            .build()
        Log.v("sendMeasureData", "Try to send MultipartBody")

        return withContext(Dispatchers.IO) {

            try {
                client.newCall(request).execute().use { response ->
                    if (response.code == 500) {
                        // 서버 응답이 성공하지 않았을 경우 처리
                        Log.e("전송실패3", "$response")
                        Log.e("전송실패3", "body: ${response.body?.string()}")
                        callback(null)
                        return@withContext Result.failure(Exception("Failed to fetch data: ${response.code}"))
                    }

                    // ------# db 초기화 #------
                    val md = MeasureDatabase.getDatabase(context)
                    val mDao = md.measureDao()

                    val responseBody = response.body?.string()
                    Log.w("getMeasureResult", "$responseBody")

                    val bodyJo = JSONObject(responseBody.toString())
                    val fileSuccess = bodyJo.optString("reupload_file")
                    val jsonSuccess = bodyJo.optString("reupload_json")
                    val serverJsonName = bodyJo.optString("measure_server_json_name")
                    val serverFileName = bodyJo.optString("measure_server_file_name")

                    if (jsonSuccess == "1") {
                        when (isStatic) {
                            true -> mDao.updateAndGetStatic(mobileDbSn, uploadedJson = jsonSuccess, serverJsonName = serverJsonName)
                            false -> mDao.updateAndGetDynamic(mobileDbSn, uploadedJson = jsonSuccess, serverJsonName = serverJsonName)
                        }
                        saveFileFromUrl(context, serverJsonName, FileStorageUtil.FileType.JSON)
                    }
                    if (fileSuccess == "1") {
                        when (isStatic) {
                            true -> {
                                mDao.updateAndGetStatic(mobileDbSn, uploadedFile = fileSuccess,serverFileName = serverFileName)
                                saveFileFromUrl(context, serverFileName, FileStorageUtil.FileType.IMAGE)
                            }
                            false -> {
                                mDao.updateAndGetDynamic(mobileDbSn, uploadedFile = fileSuccess, serverFileName = serverFileName)
                                saveFileFromUrl(context, serverFileName, FileStorageUtil.FileType.VIDEO)
                            }
                        }
                    }

                    callback(Pair(jsonSuccess, fileSuccess))
                    return@withContext Result.success(Unit)
                    /* 앱 내 DB 수정 완료했음. 그러면 measureInfo와 static, dynamic이 들어가있는데 사진만 저장이 안된 상황.
                    * 현재 로그인 시 measure 인포 전부 가져오기 및 db저장. 여기서는 인포 저장됨. db에도 저장됨. 그러면? 전부 1일때만? 파일들 서버에서 받아오기, 그리고 확정된 파일 이름에 맞게 저장하고 cache 비우기 하면 됨.
                    * 근데 전부 안들어갔을 때는을 대비해서. 그냥 업로드 된 항목들 2*2 로 나눠서 하나라도 되면 그거 파일 값 저장하기로 넘어가야함. 근데 일부가 안들어갔을 때, 그냥 계속 보내기? ㅋㅋㅋ
                    * */
                }

            } catch (e: SocketTimeoutException) {
                // 타임아웃 처리
                Log.e("getMeasureResultError", "Request timed out", e)
                callback(null)
                return@withContext Result.failure(Exception("Request timed out"))
            } catch (e: IOException) {
                // 네트워크 문제 처리
                Log.e("getMeasureResultError", "Network error", e)
                callback(null)
                return@withContext Result.failure(Exception("Network error: ${e.message}"))
            } catch (e: Exception) {
                // 일반적인 예외 처리
                Log.e("getMeasureResultError", "Error fetching measure result", e)
                callback(null)
                return@withContext Result.failure(e)
            }
        }
    }

    // ------# 측정 완료 후 최초 전송 #------
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
                    // 서버 응답이 성공하지 않았을 경우 처리
                    if (response.code == 500) {
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
                            val staticServerJsonName = staticJo.optString("measure_server_json_name")
                            val staticServerFileName = staticJo.optString("measure_server_file_name")
                            mDao.updateAndGetStatic(staticSns[i], serverSn, staticUploaded, staticUploadDate, staticUploadJson, staticUploadFile, staticServerJsonName, staticServerFileName)
                            saveFileFromUrl(context, staticServerJsonName, FileStorageUtil.FileType.JSON)
                            saveFileFromUrl(context, staticServerFileName, FileStorageUtil.FileType.IMAGE)
                        }
                    }

                    val dynamicJo = bodyJo.optJSONObject("dynamic")
                    if (dynamicJo != null) {
                        val serverSn = dynamicJo.optInt("server_sn")
                        val dynamicUploaded = dynamicJo.optString("uploaded")
                        val dynamicUploadDate = dynamicJo.optString("upload_date")
                        val dynamicUploadJson = dynamicJo.optString("uploaded_json")
                        val dynamicUploadFile = dynamicJo.optString("uploaded_file")
                        val dynamicServerJsonName = dynamicJo.optString("measure_server_json_name")
                        val dynamicServerFileName = dynamicJo.optString("measure_server_file_name")
                        mDao.updateAndGetDynamic(dynamicSn, serverSn, dynamicUploaded, dynamicUploadDate, dynamicUploadJson, dynamicUploadFile, dynamicServerJsonName, dynamicServerFileName)
                        saveFileFromUrl(context, dynamicServerJsonName, FileStorageUtil.FileType.JSON)
                        saveFileFromUrl(context, dynamicServerFileName, FileStorageUtil.FileType.VIDEO)
                    }
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

                val getInfos = mutableListOf<MeasureInfo>()
                for (i in 1 until ja.length()) {
                    val jo = ja.optJSONObject(i)
                    getInfos.add(jo.toMeasureInfo())
                }
//                val jo = ja.optJSONObject(1) // 현재 0번째 index는 7가지 동작이 없음.
//                getInfos.add(jo.toMeasureInfo())
                Log.v("Room>getInfos", "${mDao.getAllInfo(userUUID)}")

                val newInfos = getInfos.filter { apiInfo ->
                    apiInfo.sn !in roomInfoSns
                }

                // ------# 없는 것들만 필터링 #------
                Log.v("db없는infoSn", "newInfos: $newInfos")

                Log.v("db없는infoSn", "newInfos: ${newInfos.map { it.sn }}")
                newInfos.forEach{ newInfo ->
                    mDao.insertInfo(newInfo)
                    getMeasureResult(context, myUrl, newInfo.sn!!)
                }
                return@withContext callback(true)
            }
        }
    }
    // -------------# TODO 전제: 측정 결과 모바일에서 업로드할 때 무조건 sn을 저장해야함 #------------------

    // 측정 결과 1개 sequence 전체 가져오기
    private suspend fun getMeasureResult(context: Context, myUrl: String, measureInfoSn: Int) : Result<Unit> {
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