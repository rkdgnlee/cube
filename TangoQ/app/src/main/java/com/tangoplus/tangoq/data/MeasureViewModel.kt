package com.tangoplus.tangoq.data

import android.content.Context
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.gun0912.tedpermission.provider.TedPermissionProvider.context
import com.tangoplus.tangoq.db.FileStorageUtil.getCacheFile
import com.tangoplus.tangoq.db.FileStorageUtil.getFile
import com.tangoplus.tangoq.db.FileStorageUtil.readJsonArrayFile
import com.tangoplus.tangoq.db.FileStorageUtil.readJsonFile
import com.tangoplus.tangoq.db.MeasureDynamic
import com.tangoplus.tangoq.db.MeasureInfo
import com.tangoplus.tangoq.db.MeasureStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MeasureViewModel : ViewModel() {
    val parts = MutableLiveData(mutableListOf<MeasureVO>())
    val feedbackParts = MutableLiveData(mutableListOf<MeasureVO>())

    /// MD1 측정 날짜 선택 담을 공간 index임
    var selectMeasureDate = MutableLiveData<String>()
    var selectedMeasureDate = MutableLiveData<String>()
    var currentMeasureDate = 0

    // 서버에서 받은 측졍 결과 받는 곳
    var measures = mutableListOf<MeasureVO>()
    // MeasureDetail 담을 공간
    var selectedMeasure : MeasureVO? = null

    // MeasureSkeleton 담을 공간
    var staticjo = JSONObject() // 1 7개의 자세 중 1개를 말하는 json파일

    // room에 저장할 static들이 담기는 곳
    val statics = mutableListOf<MeasureStatic>() // 1, 3, 4, 5, 6, 7

    var dynamicJa = JSONArray() // 2 비디오 프레임당 dynamicJoUnit이  들어가는 곳
    var dynamicJoUnit = JSONObject()

    // room에 저장할 dynamic이 담기는 곳 (1개임)
    var dynamic : MeasureDynamic? = null

    val staticFiles = mutableListOf<File>()
    var dynamicFile : File? = null

    val staticJsonFiles = mutableListOf<File>()
    var dynamicJsonFile : File? = null

    init {
        selectedMeasureDate.value = ""
        selectedMeasure = MeasureVO(-1,-1, "", null, mutableListOf(Pair("", -1f), Pair("", -1f)), JSONArray(), mutableListOf(), true, null)
    }

    // -------# 측정 결과 MeasureVO로 변환 #------
    fun extractVideoCoordinates(jsonData: JSONArray) : List<List<Pair<Float,Float>>> { // 200개의 33개의 x,y
        return List(jsonData.length()) { i ->
            val landmarks = jsonData.getJSONObject(i).getJSONArray("pose_landmark")
            List(landmarks.length()) { j ->
                val landmark = landmarks.getJSONObject(j)
                Pair(
                    landmark.getDouble("sx").toFloat(),
                    landmark.getDouble("sy").toFloat()
                )
            }
        }
    }

    private val excludedKeys = setOf(
        "sn", "device_sn", "local_sn", "measure_sn", "user_uuid", "user_sn", "user_name",
        "measure_seq", "measure_type", "reg_date", "measure_start_time", "measure_end_time",
        "measure_photo_file_name", "measure_server_json_name", "measure_server_file_name",
        "measure_overlay_width", "measure_overlay_height", "measure_overlay_scale_factor_x", "measure_overlay_scale_factor_y",
        "uploaded_json_fail", "uploaded", "upload_date", "result_index", "uploaded_json", "uploaded_file", "used",
        "ols_front_left_horizontal_angle_shoulder", "ols_front_left_horizontal_distance_shoulder",
        "ols_front_left_horizontal_angle_hip", "ols_front_left_horizontal_distance_hip",
        "ols_front_left_vertical_angle_hip_knee", "ols_front_left_vertical_angle_knee_toe",
        "ols_front_left_vertical_angle_hip_knee_toe", "ols_front_left_vertical_angle_hip_knee_opposite",
        "ols_front_left_vertical_angle_knee_toe_opposite", "ols_front_left_vertical_angle_hip_knee_toe_opposite",
        "ols_front_left_vertical_distance_toe_opposite_toe", "ols_front_right_horizontal_angle_shoulder",
        "ols_front_right_horizontal_distance_shoulder", "ols_front_right_horizontal_angle_hip",
        "ols_front_right_horizontal_distance_hip", "ols_front_right_vertical_angle_hip_knee",
        "ols_front_right_vertical_angle_knee_toe", "ols_front_right_vertical_angle_hip_knee_toe",
        "ols_front_right_vertical_angle_hip_knee_opposite", "ols_front_right_vertical_angle_knee_toe_opposite",
        "ols_front_right_vertical_angle_hip_knee_toe_opposite", "ols_front_right_vertical_distance_toe_opposite_toe",
    )

    fun convertToMeasureDynamic(jsonObject: JSONObject): MeasureDynamic {
        val gson = Gson()

        // 1. JSON 객체의 모든 키-값 쌍을 순회하며 필요한 경우에만 "ohs_" 접두사 추가
        val modifiedJsonObject = JSONObject()
        for (key in jsonObject.keys()) {
            val newKey = if (!excludedKeys.contains(key) && !key.startsWith("ohs_")) "ohs_$key" else key
            modifiedJsonObject.put(newKey, jsonObject.get(key))
        }

        // 2. 수정된 JSON 객체를 MeasureDynamic 객체로 변환
        return gson.fromJson(modifiedJsonObject.toString(), MeasureDynamic::class.java)
    }

    fun convertJsonToMeasureVO(info: MeasureInfo, statics: MutableList<MeasureStatic>, dynamic: MeasureDynamic) : MeasureVO {
        val ja = JSONArray()
        val uris = mutableListOf<String>()
        val baseUrl = "https://gym.tangostar.co.kr/data/Results/"

        for (i in 0 until 6) { // 총 7개인거임
            if (i == 1) {
                val jsonFile = getCacheFile(context, dynamic.measure_server_json_name?.replace(baseUrl, "")!!)
                val mediaFile = getCacheFile(context, dynamic.measure_server_file_name?.replace(baseUrl, "")!!)
                Log.e("측정후dynamic", "jsonFile: $jsonFile, mediaFile: $mediaFile")
                if (jsonFile != null && mediaFile != null) {
                    ja.put(readJsonArrayFile(jsonFile))
                    uris.add(mediaFile.absolutePath)
                } else {
                    Log.e("측정후dynamicError", "jsonFile: $jsonFile, mediaFile: $mediaFile")
                }
            }

            // ------# static 6개 #-------
            val jsonFile = getCacheFile(context, statics[i].measure_server_json_name.replace(baseUrl, ""))
            val mediaFile = getCacheFile(context, statics[i].measure_server_file_name.replace(baseUrl, ""))
            if (jsonFile != null && mediaFile != null) {
                ja.put(readJsonFile(jsonFile))
                uris.add(mediaFile.absolutePath)
                Log.e("측정후Static$i", "jsonFile: $jsonFile, mediaFile: $mediaFile")
            } else {
                Log.e("측정후Static$i Error", "jsonFile: $jsonFile, mediaFile: $mediaFile")
            }
        }

        Log.v("VMUris", "${uris}")
        return MeasureVO(
            deviceSn = 0,
            measureSn = info.measure_sn, // 아이디도 더미
            regDate = info.measure_date,
            overall = info.t_score, // 종합 점수 더미
            dangerParts = mutableListOf(Pair("골반", 1f), Pair("어깨", 3f)), // 아픈 부위 더미
            measureResult = ja,
            fileUris = uris,
            isMobile = true,
            recommendations = null
        )
    }
}