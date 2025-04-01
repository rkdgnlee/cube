package com.tangoplus.tangoq.broadcastReceiver

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.function.PreferencesManager
import com.tangoplus.tangoq.vo.MessageVO

class SaveAlarmWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val context = applicationContext
        val text = inputData.getString("text") ?: return Result.failure()
        val timestamp = inputData.getLong("timestamp", System.currentTimeMillis())

        try {
            val prefs = PreferencesManager(context)
            // Try to get user SN safely
            val userSn = try {
                Singleton_t_user.getInstance(context).jsonObject?.optInt("sn") ?: 0
            } catch (e: Exception) {
                Log.e("SaveAlarmWorker", "Error getting user SN: ${e.message}")
                0
            }

            val message = MessageVO(
                userSn = userSn,
                message = text,
                timeStamp = timestamp
            )
            Log.v("alarmStored", "Success to store Alarm: $text")
            prefs.storeAlarm(message)
            return Result.success()
        } catch (e: Exception) {
            Log.e("SaveAlarmWorker", "Failed to save alarm: ${e.message}")
            return Result.failure()
        }
    }
}