package com.tangoplus.tangoq.function

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.tangoplus.tangoq.vo.MessageVO
import androidx.core.content.edit

class PreferencesManager(private val context: Context) {
    private val LAST_SN_KEY = "last_sn"
    private val USER_SN_KEY = "user_sn"
    private val globalPrefs: SharedPreferences = context.getSharedPreferences("global_prefs", Context.MODE_PRIVATE)

    private fun getPrefs(name: String): SharedPreferences {
        val userSn = getUserSn()
        return context.getSharedPreferences("${name}_${userSn}", Context.MODE_PRIVATE)
    }

    fun getUserSn(): Int = globalPrefs.getInt(USER_SN_KEY, -1)

    fun setUserSn(userSn: Int) {
        Log.v("setUserSn", "$userSn")
        globalPrefs.edit { putInt(USER_SN_KEY, userSn) }
    }

    // ------# Latest Recommendation #------
    private val latestRecommendationPrefs: SharedPreferences
        get() = getPrefs("latest_recommendation")

    fun saveLatestRecommendation(sn: Int) {
        latestRecommendationPrefs.edit { putInt("latest_sn", sn) }
    }

    fun getLatestRecommendation(): Int = latestRecommendationPrefs.getInt("latest_sn", -1)

    // ------# Search History #------
    private val searchHistoryPrefs: SharedPreferences
        get() = getPrefs("search_history")

    fun getLastSn(): Int = searchHistoryPrefs.getInt(LAST_SN_KEY, 0)

    fun incrementAndGetSn(): Int {
        val lastSn = getLastSn()
        val newSn = lastSn + 1
        searchHistoryPrefs.edit { putInt(LAST_SN_KEY, newSn) }
        return newSn
    }

    fun setStoredHistory(search: String) {
        searchHistoryPrefs.edit { putString("${incrementAndGetSn()}_stored_history", search) }
    }

    fun getStoredHistory(sn: Int): String = searchHistoryPrefs.getString("${sn}_stored_history", "").orEmpty()

    fun deleteAllHistory() {
        searchHistoryPrefs.edit { clear() }
    }

    fun deleteStoredHistory(sn: Int) {
        searchHistoryPrefs.edit { remove("${sn}_stored_history") }
    }

    // Alarms
    private val alarmPrefs: SharedPreferences
        get() = getPrefs("alarm")

    fun storeAlarm(messageVO: MessageVO) {
        val alarm = Gson().toJson(messageVO)

        alarmPrefs.edit {
            putString(
                "alarmMessage_${messageVO.userSn}_${messageVO.timeStamp}",
                alarm
            )
        }
    }

    fun getAlarms(sn: Int): MutableList<MessageVO> {
        val allEntries = alarmPrefs.all
        val alarmsList = mutableListOf<MessageVO>()
        val gson = Gson()
        Log.v("올엔트리알람", "$allEntries")
        for ((key, value) in allEntries) {
            if (key.startsWith("alarmMessage_$sn")) {
                val alarmJson = value as? String ?: continue
                val messageVO = gson.fromJson(alarmJson, MessageVO::class.java)
                alarmsList.add(messageVO)
            }
        }
        return alarmsList
    }

    fun deleteAlarm(sn: Int, timeStamp: Long) {
        alarmPrefs.edit { remove("alarmMessage_${sn}_$timeStamp") }
    }

    fun deleteAllAlarms(sn: Int) {
        alarmPrefs.edit {
            val allEntries = alarmPrefs.all

            for ((key, _) in allEntries) {
                if (key.startsWith("alarmMessage_$sn")) {
                    remove(key)
                }
            }

        }
    }
}