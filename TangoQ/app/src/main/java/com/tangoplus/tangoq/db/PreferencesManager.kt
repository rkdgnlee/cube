package com.tangoplus.tangoq.db

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
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

    }
