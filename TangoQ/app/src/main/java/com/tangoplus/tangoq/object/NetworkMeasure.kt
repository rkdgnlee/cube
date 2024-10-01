package com.tangoplus.tangoq.`object`

import android.content.ContentValues
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.tangoplus.tangoq.data.MeasureVO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
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
import java.io.IOException

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



    // 측정 결과 목록 가져오기
    suspend fun fetchMeasureResults7(myUrl: String) : List<MeasureVO> {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("$myUrl/read.php")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.w("NetMeasure7", "Success to execute request: $responseBody")
                val ja = responseBody.let { JSONObject(it.toString()) }.optJSONArray("data")
                val measures = mutableListOf<MeasureVO>()
                if (ja != null) {
//                    val dangerParts = mutableListOf<Pair<String, Int>>()
                    for (i in 0 until ja.length()) {

//                        dangerParts.add(Pair(ja.getJSONObject(i).optString("dangerparts"), ))
                        val measureVO = MeasureVO(
                            measureId = ja.getJSONObject(i).optString("measure_sn"),
                            regDate =  ja.getJSONObject(i).optString("reg_date"),
                            overall = ja.getJSONObject(i).optInt("score"),
                            dangerParts = mutableListOf(),
                            measureResult = JSONArray(),
                            fileUris = mutableListOf(),
                            isMobile = if (ja.getJSONObject(i).optInt("device_sn") == 3) true else false,
                            recommendations = mutableListOf()
                        )
                        measures.add(measureVO)
                    }
                }

                return@use measures
            }
        }
    }


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