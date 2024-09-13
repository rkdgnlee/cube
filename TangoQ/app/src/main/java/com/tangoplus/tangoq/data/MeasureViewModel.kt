package com.tangoplus.tangoq.data

import android.annotation.SuppressLint
import android.icu.util.Measure
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONArray
import org.json.JSONObject

class MeasureViewModel : ViewModel() {
    val parts = MutableLiveData(mutableListOf<MeasureVO>())
    val feedbackParts = MutableLiveData(mutableListOf<MeasureVO>())

    /// MD1 측정 날짜 선택 담을 공간
    var selectMeasureDate = MutableLiveData<Int>()
    var selectedMeasureDate = MutableLiveData<Int>()
    var currentMeasureDate = 0

    // MeasureSkeleton 담을 공간
    val staticjo = JSONObject() // 1 해당 자세의 json파일
    var dynamicJa = JSONArray() // 2 비디오 프레임당 dynamicJoUnit이  들어가는 곳
    var dynamicJoUnit = JSONObject()
    val measurejo = JSONArray() // 3. 1번 자세 한 개의 json파일을 담은 곳. ( staticjo, dynamicja, staticjo, staticjo ... ) 총 7개

    // 서버에서 받은 측졍 결과 받는 곳
    var measures = mutableListOf<MeasureVO>()

    // MeasureDetail 담을 공간
    var selectedMeasure : MeasureVO? = null

    init {
        selectedMeasure = MeasureVO("", "", null, mutableListOf(), mutableListOf())
        selectedMeasureDate.value = 0
    }
}