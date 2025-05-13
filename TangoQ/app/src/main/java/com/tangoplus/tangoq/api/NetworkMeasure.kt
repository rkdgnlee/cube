package com.tangoplus.tangoq.api

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.tangoplus.tangoq.db.FileStorageUtil
import com.tangoplus.tangoq.db.FileStorageUtil.saveFileFromUrl
import com.tangoplus.tangoq.db.MeasureDatabase
import com.tangoplus.tangoq.db.MeasureDynamic
import com.tangoplus.tangoq.db.MeasureInfo
import com.tangoplus.tangoq.db.MeasureStatic
import com.tangoplus.tangoq.api.HttpClientProvider.getClient
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException

object NetworkMeasure {

    // ------# 측정 완료 후 최초 전송 #------
    suspend fun sendMeasureData(context: Context, myUrl: String, requestBody: RequestBody, infoSn: Int, staticSns: MutableList<Int>, dynamicSn: Int) : Result<JSONObject> {
        val client = getClient(context)
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
                        val errorBody = response.body?.string()
                        Log.e("전송실패3", "Exception(Failed to fetch data: code - ${response.code} body - $errorBody ")
                        return@withContext Result.failure(Exception("failed to response: ${response.code} ${errorBody}"))
                    }

                    // ------# db 초기화 #------
                    val md = MeasureDatabase.getDatabase(context)
                    val mDao = md.measureDao()

                    val responseBody = response.body?.string()
                    Log.w("getMeasureResult", "$responseBody")

                    val bodyJo = JSONObject(responseBody.toString())
                    val infoJo = bodyJo.optJSONObject("measure_info")

                    // DB에는 이미 저장됐고, 값을 받아와서 DB의 내용을 수정해야함. callback으로 나가게. false로 나가
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

                    return@withContext Result.success(bodyJo)
                }
            } catch (e: SocketTimeoutException) {
                // 타임아웃 처리
                Log.e("getMeasureResultError", "Request timed out: ${e.message}" )

                return@withContext Result.failure(SocketTimeoutException("Request timed out: ${e.message}"))
            } catch (e: IOException) {
                // 네트워크 문제 처리
                Log.e("getMeasureResultError", "Network error: ${e.message}")

                return@withContext Result.failure(IOException("Network error: ${e.message}"))
            } catch (e: Exception) {
                // 일반적인 예외 처리
                Log.e("getMeasureResultError", "Error fetching measure result: ${e.message}")

                return@withContext Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    suspend fun saveAllMeasureInfo(context: Context, myUrl: String, userUUID: String,  mvm: MeasureViewModel, callback : (Boolean) -> Unit) { // 1845가 들어감.
        val client = getClient(context)
        val request = Request.Builder()
            .url(myUrl)
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    // ------# db 초기화 #------
                    val md = MeasureDatabase.getDatabase(context)
                    val mDao = md.measureDao()

                    val responseBody = response.body?.string()
                    val bodyJo = JSONObject(responseBody.toString())

                    if (bodyJo.optInt("row_count") == 0) {
                        Log.v("getAllMeasureOut", "rowCount: ${bodyJo.optInt("row_count")}, stop the getAllMeasures")
                        return@withContext callback(false)
                    }
                    val ja = bodyJo.getJSONArray("data") // 3개가 들어가있음.
                    val roomInfoSns =  mDao.getAllSns(userUUID) // 1845의 server sn인 sn을 가져옴
                    Log.v("룸에저장된info들", "$roomInfoSns")

                    val getInfos = mutableListOf<MeasureInfo>() // info로 변환해서 넣기
                    for (i in 0 until ja.length()) {
                        val jo = ja.optJSONObject(i)
                        getInfos.add(jo.toMeasureInfo())
                    }
                    Log.v("Room>getInfos", "${mDao.getAllInfo(userUUID).size}")

                    val newInfos = getInfos.filter { apiInfo ->
                        apiInfo.sn !in roomInfoSns
                    }

                    // ------# 없는 것들만 필터링 #------
                    Log.v("db없는infoSn", "newInfos: ${newInfos.size}")
                    mvm.totalInfoCount = if (newInfos.isEmpty()) {
                        1
                    } else {
                        newInfos.size
                    }
                    Log.v("db없는infoSn", "newInfos: ${newInfos.map { it.sn }}")
                    newInfos.forEach{ newInfo ->
                        mDao.insertInfo(newInfo)
                        newInfo.sn?.let { getMeasureResult(context, myUrl, it) }
                        mvm.progressInfoCount.postValue(mvm.progressInfoCount.value?.plus(1))
                        Log.v("db없는infoSn", "다운로드진행상황: ${mvm.progressInfoCount.value} / ${newInfos.size}")
                    }
                    return@withContext callback(true)
                }
            } catch (e: IllegalStateException) {
                Log.e("saveAllInfo", "Error IllegalStateException SaveAllMeasureInfo : ${e.message}")
                return@withContext callback(false)
            } catch (e: IllegalArgumentException) {
                Log.e("saveAllInfo", "Error IllegalArgumentException SaveAllMeasureInfo : ${e.message}")
                return@withContext callback(false)
            } catch (e: IOException) {
                Log.e("saveAllInfo", "Error IOException SaveAllMeasureInfo : ${e.message}")
                return@withContext callback(false)
            }  catch (e: SocketTimeoutException) {
                Log.e("saveAllInfo", "Error SocketTimeoutException SaveAllMeasureInfo : ${e.message}")
                return@withContext callback(false)
            } catch (e: Exception) {
                Log.e("saveAllInfo", "Error Exception SaveAllMeasureInfo : ${e.message}")
                return@withContext callback(false)
            }

        }
    }

    // ------# 측정 결과 1개 sequence 전체 가져오기 #------
    private suspend fun getMeasureResult(context: Context, myUrl: String, measureInfoSn: Int) : Result<Unit> {
        val client = getClient(context)
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
//                    Log.w("getMeasureResult", "$responseBody")

                    val motherJo = responseBody?.let { JSONObject(it) }
                    if (motherJo != null) {
                        for (i in 0 until motherJo.optString("count").toInt() - 1) {
                            val staticJo = motherJo.optJSONObject("static_${i+1}")
                            if (staticJo != null) {
                                Log.v("스태틱조1~6", staticJo.optString("measure_server_json_name"))
                                mDao.insertByStatic(staticJo.toMeasureStatic())
                            }
                        }
                        val dynamicJo = motherJo.optJSONObject("dynamic")
                        if (dynamicJo != null) {
                            Log.v("다이나믹", dynamicJo.optString("measure_server_json_name"))
                            mDao.insertByDynamic(dynamicJo.toMeasureDynamic())
                        }
                    }
                    return@withContext Result.success(Unit)
                }

            } catch (e: SocketTimeoutException) {
                // 타임아웃 처리
                Log.e("getMeasureResultError", "Request timed out, ${e.message}")
                return@withContext Result.failure(Exception("Request timed out"))
            } catch (e: IOException) {
                // 네트워크 문제 처리
                Log.e("getMeasureResultError", "Network error ${e.message}")
                return@withContext Result.failure(Exception("Network error: ${e.message}"))
            } catch (e: Exception) {
                // 일반적인 예외 처리
                Log.e("getMeasureResultError", "Error fetching measure result ${e.message}")
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