package com.tangoplus.tangoq.db

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.tangoplus.tangoq.data.MessageVO

class PreferencesManager(context: Context, userSn: Int) {
    private val LAST_SN_KEY = "last_sn"
    private val prefs2 : SharedPreferences = context.getSharedPreferences("search_history_${userSn}", Context.MODE_PRIVATE)
    private val prefs : SharedPreferences = context.getSharedPreferences("lastest_recommendation", Context.MODE_PRIVATE)
    private val prefs3 : SharedPreferences = context.getSharedPreferences("alarm_${userSn}", Context.MODE_PRIVATE)

    fun saveLatestRecommendation(sn: Int) {
        prefs.edit().apply {
            putInt("latest_sn", sn)
            apply()
        }
    }
    fun getLatestRecommendation(): Int {
        return prefs.getInt("latest_sn", -1)
    }



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

    fun storeAlarm(messageVO: MessageVO) {
        val alarm = Gson().toJson(messageVO)

        prefs3.edit().putString("alarmMessage_${messageVO.timeStamp}", alarm).apply()
    }

    fun getAlarms(): MutableList<MessageVO> {
        val allEntries = prefs3.all // Get all entries in SharedPreferences
        val alarmsList = mutableListOf<MessageVO>()
        val gson = Gson()

        for ((key, value) in allEntries) {
            // Filter keys that start with "alarmMessage_"
            if (key.startsWith("alarmMessage_")) {
                // Convert the JSON string back to a MessageVO object
                val alarmJson = value as? String ?: continue
                val messageVO = gson.fromJson(alarmJson, MessageVO::class.java)
                alarmsList.add(messageVO)
            }
        }

        return alarmsList
    }

    fun deleteAlarm(timeStamp: Long) {
        prefs3.edit().remove("alarmMessage_$timeStamp").apply()
    }

    fun deleteAllAlarms() {
        val editor = prefs3.edit()
        val allEntries = prefs3.all

        for ((key, _) in allEntries) {
            // Filter and remove keys that start with "alarmMessage_"
            if (key.startsWith("alarmMessage_")) {
                editor.remove(key)
            }
        }

        editor.apply() // Apply changes after looping through all keys
    }

}
