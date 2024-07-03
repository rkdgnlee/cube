package com.tangoplus.tangoq.db

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.tangoplus.tangoq.data.MessageVO

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("daily_set_prefs", Context.MODE_PRIVATE)

    fun getStoredInt(email: String) : Int {
        return prefs.getInt("${email}_stored_int", 0)
    }

    fun setStoredInt(email: String, value: Int) {
        prefs.edit().putInt("${email}_stored_int", value).apply()
    }

    fun getLastSavedDate(email: String) : Long {
        return prefs.getLong("${email}_last_saved_date", 0L)
    }

    fun setLastSavedDate(email: String, value: Long) {
        prefs.edit().putLong("${email}_last_saved_date", value).apply()
    }

    fun clearStoredInt(email: String) {
        prefs.edit().putInt("${email}_stored_int", 0).apply()
    }

    fun incrementStoredInt(email: String) {
        val currentDate = System.currentTimeMillis()
        val lastSavedDate = getLastSavedDate(email)

        if (currentDate - lastSavedDate <= 84600000) {
            clearStoredInt(email)
            setLastSavedDate(email, currentDate)
        }

        val currentValue = getStoredInt(email)
        setStoredInt(email, currentValue + 1)
        Log.v("storedInt", "$currentValue, ${getStoredInt(email)}, $email")
        setLastSavedDate(email, System.currentTimeMillis())
    }
}
