package com.tangoplus.tangoq.db

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.tangoplus.tangoq.data.MessageVO
import org.json.JSONObject
import java.util.Calendar
import java.util.TimeZone

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("daily_set_prefs", Context.MODE_PRIVATE)

    private val LAST_SN_KEY = "last_sn"
    private val prefs2 : SharedPreferences = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)


    fun getLastSn(): Int {
        return prefs2.getInt(LAST_SN_KEY, 0)
    }

    fun incrementAndGetSn(): Int {
        val lastSn = getLastSn()
        val newSn = lastSn + 1
        prefs2.edit().putInt(LAST_SN_KEY, newSn).apply()
        return newSn
    }

    fun setStoredHistory(search: String) {
        prefs2.edit().putString("${incrementAndGetSn()}_stored_history", search).apply()
    }

    fun getStoredHistory(sn: Int) : String {
        return prefs2.getString("${sn}_stored_history", "").toString()
    }

    fun deleteAllHistory() {
        prefs2.edit().clear().apply()
    }

    fun deleteStoredHistory(sn: Int) {
        prefs2.edit().remove("${sn}_stored_history").apply()
    }

    // ------# 간단한 운동 기록 저장 #------
    fun getStoredInt(sn: String) : Int {
        return prefs.getInt("${sn}_stored_int", 0)
    }

    fun setStoredInt(sn: String, value: Int) {
        prefs.edit().putInt("${sn}_stored_int", value).apply()
    }

    fun getLastSavedDate(sn: String) : Long {
        return prefs.getLong("${sn}_last_saved_date", 0L)
    }

    fun setLastSavedDate(sn: String, value: Long) {
        prefs.edit().putLong("${sn}_last_saved_date", value).apply()
    }

    fun clearStoredInt(sn: String) {
        prefs.edit().putInt("${sn}_stored_int", 0).apply()
    }

    fun incrementStoredInt(sn: String) {
        val currentDate = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
        val lastSavedDate = getLastSavedDate(sn)

        if (isNewDay(currentDate, lastSavedDate)) {
            clearStoredInt(sn)
            Log.v("PrefsManager", "Reset count due to new day")
        }

        val currentValue = getStoredInt(sn)
        val newValue = currentValue + 1
        setStoredInt(sn, newValue)
        Log.v("PrefsManager", "Incremented: previous=$currentValue, new=$newValue")
        setLastSavedDate(sn, currentDate.timeInMillis)
    }

    fun storeFilter(filter1: String, filter2 : String) {
        prefs.edit().putString(filter1, filter2).apply()
        Log.v("filter", "$filter1 , $filter2")
    }

    fun getFilter(filter1: String) : String {
        return prefs.getString(filter1, "없음").toString()
    }

    fun hasAnyFilter(): Boolean {
        val filterKeys = listOf(
            "재활 부위 선택",
            "제외 운동",
            "운동 난이도 설정",
            "보유 기구 설정",
            "운동 장소(가능 자세)"
        )
        return filterKeys.any { prefs.contains(it) }
    }

    private fun isNewDay(currentDate: Calendar, lastSavedTimestamp: Long): Boolean {
        val lastSavedDate = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
        lastSavedDate.timeInMillis = lastSavedTimestamp

        return currentDate.get(Calendar.YEAR) > lastSavedDate.get(Calendar.YEAR) ||
                (currentDate.get(Calendar.YEAR) == lastSavedDate.get(Calendar.YEAR) &&
                        currentDate.get(Calendar.DAY_OF_YEAR) > lastSavedDate.get(Calendar.DAY_OF_YEAR))
    }





}
