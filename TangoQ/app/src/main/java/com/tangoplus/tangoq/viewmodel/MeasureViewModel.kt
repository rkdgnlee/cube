package com.tangoplus.tangoq.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.db.MeasureDynamic
import com.tangoplus.tangoq.db.MeasureStatic
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

    // room에 저장할 static들이 담기는 곳
    val infoResultJa = JSONArray()
    val statics = mutableListOf<MeasureStatic>() // 1, 3, 4, 5, 6, 7
    var staticjo = JSONObject() // 1 7개의 자세 중 1개를 말하는 json파일

    var dynamicJa = JSONArray() // 2 비디오 프레임당 dynamicJoUnit이  들어가는 곳
    var dynamicJoUnit = JSONObject()
    // room에 저장할 dynamic이 담기는 곳 (1개임)
    var dynamic : MeasureDynamic? = null
    var toSendDynamicJo :  JSONObject? = null
    val staticFiles = mutableListOf<File>()
    var dynamicFile : File? = null

    val staticJsonFiles = mutableListOf<File>()
    var dynamicJsonFile : File? = null

    // ------# 각 부위 데이터들 #------
    var earData = listOf<Pair<Float, Float>>()
    var shoulderData = listOf<Pair<Float, Float>>()
    var elbowData = listOf<Pair<Float, Float>>()
    var wristData = listOf<Pair<Float, Float>>()
    var indexData = listOf<Pair<Float, Float>>()
    var pinkyData = listOf<Pair<Float, Float>>()
    var thumbData = listOf<Pair<Float, Float>>()
    var hipData = listOf<Pair<Float, Float>>()
    var kneeData = listOf<Pair<Float, Float>>()
    var ankleData = listOf<Pair<Float, Float>>()
    var heelData = listOf<Pair<Float, Float>>()
    var toeData = listOf<Pair<Float, Float>>()

    init {
        selectedMeasureDate.value = ""
        selectedMeasure = MeasureVO(-1,-1, "", null, mutableListOf(Pair("", -1f), Pair("", -1f)), JSONArray(), mutableListOf(), true, null)
    }

    // ------# JSONObject로 변경 전 앞에 접두사 안생기게끔 하기 #------
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

    // json 파일 만들 때 사용해야 하는 거임. jsonObject에서
    fun convertToMeasureDynamic(jsonObject: JSONObject): MeasureDynamic {
        val gson = Gson()

        val modifiedJsonObject = JSONObject().apply {
            // 기존 로직: ohs_ 접두사 처리
            for (key in jsonObject.keys()) {
                val newKey = if (!excludedKeys.contains(key) && !key.startsWith("ohs_")) "ohs_$key" else key
                put(newKey, jsonObject.get(key))
            }
            // 기본값들이 없는 경우를 위한 추가
            if (!has("uploaded")) put("uploaded", "0")
            if (!has("upload_date")) put("upload_date", getCurrentDateTime())
            if (!has("result_index")) put("result_index", 0)
            if (!has("uploaded_json")) put("uploaded_json", "0")
            if (!has("uploaded_file")) put("uploaded_file", "0")
            if (!has("uploaded_json_fail")) put("uploaded_json_fail", "0")
            if (!has("used")) put("used", "0")
        }
        return gson.fromJson(modifiedJsonObject.toString(), MeasureDynamic::class.java)
    }
    fun getCurrentDateTime(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

}