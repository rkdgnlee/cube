package com.tangoplus.tangoq.data

import android.provider.ContactsContract.CommonDataKinds.StructuredName
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONArray
import org.json.JSONObject

class ProgramViewModel : ViewModel() {
    val filter1 = MutableLiveData(JSONArray())
    val filter2 = MutableLiveData(JSONObject())
    val filter3 = MutableLiveData(JSONObject())
    val filter4 = MutableLiveData(JSONObject())
    val filter5 = MutableLiveData(JSONObject())

    init {
        filter1.value?.put("없음")
        filter2.value?.put("제외 운동", "없음")
        filter3.value?.put("운동 난이도 설정", "없음")
        filter4.value?.put("보유 기구 설정", "없음")
        filter5.value?.put("운동 장소(가능 자세)", "미설정")
    }

    fun removeFromFilter1(part: String) {
        val currentArray = filter1.value ?: JSONArray() // 현재 값 가져오기
        val updatedArray = JSONArray()
        for (i in 0 until currentArray.length()) {
            val item = currentArray.optString(i)
            if (item != part) {
                updatedArray.put(item)
            }
        }
        if (updatedArray.length() == 0) {
            updatedArray.put("없음")
        }
        filter1.value = updatedArray
    }

    fun addToFilter1(part: String) {
        val currentArray = filter1.value ?: JSONArray()
        val updatedArray = JSONArray()
        if (part != "없음") {
            for ( i in 0 until currentArray.length()) {
                val item = currentArray.optString(i)
                if (item != "없음") {
                    updatedArray.put(item)
                }
            }
            if (!updatedArray.toString().contains(part)) {
                updatedArray.put(part)
            }
        } else {
            updatedArray.put("없음")
        }

        if (updatedArray.length() == 0) {
            updatedArray.put("없음")
        }

        filter1.value = updatedArray
    }

    fun updateFilter(filterNumber: Int, selectedItem: String) {
        when (filterNumber) {
            2 -> filter2.postValue(JSONObject().put("제외 운동", selectedItem))
            3 -> filter3.postValue(JSONObject().put("운동 난이도 설정", selectedItem))
            4 -> filter4.postValue(JSONObject().put("보유 기구 설정", selectedItem))
            5 -> filter5.postValue(JSONObject().put("운동 장소(가능 자세)", selectedItem))
        }
    }

    fun getSelectedItem(filterNumber: Int): String {
        return when (filterNumber) {
            2 -> filter2.value?.optString("제외 운동") ?: ""
            3 -> filter3.value?.optString("운동 난이도 설정") ?: ""
            4 -> filter4.value?.optString("보유 기구 설정") ?: ""
            5 -> filter5.value?.optString("운동 장소(가능 자세)") ?: ""
            else -> ""
        }
    }

    fun allFalseInFilter(filterBig: String) {
        when (filterBig) {
            "제외 운동" -> filter2.value = addEmptyString(filterBig)
            "운동 난이도 설정" -> filter3.value = addEmptyString(filterBig)
            "보유 기구 설정" -> filter4.value = addEmptyString(filterBig)
            "운동 장소(가능 자세)" -> filter5.value = addEmptyString(filterBig)
            else -> {
                val ja = JSONArray()
                ja.put("없음")
                filter1.value = ja
            }
        }
    }
    private fun addEmptyString(key: String) : JSONObject {
        val jo = JSONObject()
        jo.put(key, "없음")
        return jo
    }

}