package com.tangoplus.tangoq.db

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.tangoplus.tangoq.data.MessageVO
import java.util.Calendar
import java.util.TimeZone

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("daily_set_prefs", Context.MODE_PRIVATE)

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
    private fun isNewDay(currentDate: Calendar, lastSavedTimestamp: Long): Boolean {
        val lastSavedDate = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
        lastSavedDate.timeInMillis = lastSavedTimestamp

        return currentDate.get(Calendar.YEAR) > lastSavedDate.get(Calendar.YEAR) ||
                (currentDate.get(Calendar.YEAR) == lastSavedDate.get(Calendar.YEAR) &&
                        currentDate.get(Calendar.DAY_OF_YEAR) > lastSavedDate.get(Calendar.DAY_OF_YEAR))
    }
}
