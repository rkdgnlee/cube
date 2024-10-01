package com.tangoplus.tangoq.data

import android.content.Context
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gun0912.tedpermission.provider.TedPermissionProvider.context
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
    var dynamicJa = JSONArray() // 2 비디오 프레임당 dynamicJoUnit이  들어가는 곳
    var dynamicJoUnit = JSONObject()
    val measurejo = JSONArray() // 3. 1번 2번이 전부 들어간 jsonArray json파일을 담은 곳. ( staticjo, dynamicja, staticjo, staticjo ... ) 총 7개
    val measureFiles = mutableListOf<File>()




    init {
        selectedMeasureDate.value = ""
        selectedMeasure = MeasureVO("", "", null, mutableListOf(Pair("", -1), Pair("", -1)), JSONArray(), mutableListOf(), true, null)
    }

    // -------# 측정 결과 MeasureVO로 변환 #------
    fun convertJsonToMeasureVO(ja: JSONArray) : MeasureVO {
        val uris = mutableListOf<String>()
        for (i in 0 until ja.length()) { // 총 7개인거임
            if (i != 1) {
                val jo = ja.optJSONObject(i)
                val fileName = jo.optString("measure_photo_file_name")
                val file = File(context.cacheDir, "$fileName.jpg")
                measureFiles.add(file)
                uris.add(file.absolutePath)
            } else {
                val jo = ja.optJSONArray(1)
                val fileName = jo.optJSONObject(jo.length() - 1).optString("measure_photo_file_name")
                val file = File(context.cacheDir, "$fileName.mp4")
                measureFiles.add(file)
                uris.add(file.absolutePath)
            }
        }
        Log.v("VMUris", "${uris}")
        return MeasureVO(
            measureId = "-1", // 아이디도 더미
            regDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            overall = 95, // 종합 점수 더미
            dangerParts = mutableListOf(Pair("골반", -1), Pair("어깨", -1)), // 아픈 부위 더미
            measureResult = ja,
            fileUris = uris,
            isMobile = true,
            recommendations = null
        )
    }
}