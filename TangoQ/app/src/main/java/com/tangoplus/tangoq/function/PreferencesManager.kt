package com.tangoplus.tangoq.function

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.tangoplus.tangoq.data.MessageVO

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
        alarmPrefs.edit().putString("alarmMessage_${messageVO.sn}_${messageVO.timeStamp}", alarm).apply()
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
        alarmPrefs.edit().remove("alarmMessage_${sn}_$timeStamp").apply()
    }

    fun deleteAllAlarms(sn: Int) {
        val editor = alarmPrefs.edit()
        val allEntries = alarmPrefs.all

        for ((key, _) in allEntries) {
            if (key.startsWith("alarmMessage_$sn")) {
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

    private val loginFailedPrefs : SharedPreferences
        get() = getPrefs("loginId")


    fun storeLoginId(loginId: String, tryCount: Int) {
        Log.d("PrefsManager", "Storing loginId for: $loginId")
        loginFailedPrefs.edit()
            .putInt("loginId_${loginId}", tryCount)
            .putLong("loginId_${loginId}_timestamp", System.currentTimeMillis())
            .apply()
    }
    fun getLoginCount(loginId: String) : Int {
        val storedTimestamp = loginFailedPrefs.getLong("loginId_${loginId}_timestamp", 0)
        val currentTime = System.currentTimeMillis()

        // 일주일(7일) 후 만료 체크
        if (currentTime - storedTimestamp > 7 * 24 * 60 * 60 * 1000L) {
            // 만료된 경우 데이터 삭제
            loginFailedPrefs.edit()
                .remove("loginId_${loginId}")
                .remove("loginId_${loginId}_timestamp")
                .apply()
            return 0
        }

        // 만료되지 않은 경우 저장된 로그인 시도 횟수 반환
        return loginFailedPrefs.getInt("loginId_${loginId}", 0)
    }
}