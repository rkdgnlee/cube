package com.tangoplus.tangoq.db

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.tangoplus.tangoq.data.MessageVO
import java.time.LocalDate
import java.time.LocalDateTime

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
        globalPrefs.edit().putInt(USER_SN_KEY, userSn).apply()
    }

    // ------# Latest Recommendation #------
    private val latestRecommendationPrefs: SharedPreferences
        get() = getPrefs("latest_recommendation")

    fun saveLatestRecommendation(sn: Int) {
        latestRecommendationPrefs.edit().putInt("latest_sn", sn).apply()
    }

    fun getLatestRecommendation(): Int = latestRecommendationPrefs.getInt("latest_sn", -1)

    // ------# Search History #------
    private val searchHistoryPrefs: SharedPreferences
        get() = getPrefs("search_history")

    fun getLastSn(): Int = searchHistoryPrefs.getInt(LAST_SN_KEY, 0)

    fun incrementAndGetSn(): Int {
        val lastSn = getLastSn()
        val newSn = lastSn + 1
        searchHistoryPrefs.edit().putInt(LAST_SN_KEY, newSn).apply()
        return newSn
    }

    fun setStoredHistory(search: String) {
        searchHistoryPrefs.edit().putString("${incrementAndGetSn()}_stored_history", search).apply()
    }

    fun getStoredHistory(sn: Int): String = searchHistoryPrefs.getString("${sn}_stored_history", "").orEmpty()

    fun deleteAllHistory() {
        searchHistoryPrefs.edit().clear().apply()
    }

    fun deleteStoredHistory(sn: Int) {
        searchHistoryPrefs.edit().remove("${sn}_stored_history").apply()
    }

    // Alarms
    private val alarmPrefs: SharedPreferences
        get() = getPrefs("alarm")

    fun storeAlarm(messageVO: MessageVO) {
        val alarm = Gson().toJson(messageVO)
        alarmPrefs.edit().putString("alarmMessage_${messageVO.timeStamp}", alarm).apply()
    }

    fun getAlarms(): MutableList<MessageVO> {
        val allEntries = alarmPrefs.all
        val alarmsList = mutableListOf<MessageVO>()
        val gson = Gson()
        Log.v("올엔트리알람", "$allEntries")
        for ((key, value) in allEntries) {
            if (key.startsWith("alarmMessage_")) {
                val alarmJson = value as? String ?: continue
                val messageVO = gson.fromJson(alarmJson, MessageVO::class.java)
                alarmsList.add(messageVO)
            }
        }
        return alarmsList
    }

    fun deleteAlarm(timeStamp: Long) {
        alarmPrefs.edit().remove("alarmMessage_$timeStamp").apply()
    }

    fun deleteAllAlarms() {
        val editor = alarmPrefs.edit()
        val allEntries = alarmPrefs.all

        for ((key, _) in allEntries) {
            if (key.startsWith("alarmMessage_")) {
                editor.remove(key)
            }
        }

        editor.apply()
    }

    // likes
    private val likePrefs : SharedPreferences
        get() = getPrefs("like")

    fun storeLike(exerciseId : String) {
        Log.d("PrefsManager", "Storing like for: $exerciseId")
        likePrefs.edit().putString("likes_$exerciseId", exerciseId).apply()

    }

    fun existLike(exerciseId: String) : Boolean  {
        val exist = likePrefs.getString("likes_$exerciseId", "")
        return if (exist != "") true else false
    }

    fun getLikes(): MutableList<String> {
        val allEntries = likePrefs.all
        val likeList = mutableListOf<String>()
        for ((key, value) in allEntries) {
            if (key.startsWith("likes_")) {
                likeList.add(value.toString())
            }
        }
        return likeList
    }

    fun deleteLike(exerciseId: String) {
        Log.d("PrefsManager", "Deleting like for: $exerciseId")
        likePrefs.edit().remove("likes_$exerciseId").apply()
    }
}