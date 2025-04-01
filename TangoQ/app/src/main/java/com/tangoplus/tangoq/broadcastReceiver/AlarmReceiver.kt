package com.tangoplus.tangoq.broadcastReceiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.vo.MessageVO
import com.tangoplus.tangoq.function.PreferencesManager
import com.tangoplus.tangoq.db.Singleton_t_user

class AlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // Create a wake lock to ensure the device stays awake long enough
        val powerManager = context?.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TangoBody:AlarmWakeLock"
        )
        wakeLock.acquire(10*60*1000L) // 10 minutes max

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "0629731260"
            val channelName = "TangoBody"
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)

            val title = intent?.getStringExtra("title") ?: "TangoBody"
            val text = intent?.getStringExtra("text") ?: "당신의 건강 걱정없는 삶으로 한발짝 다가가기 위해 연구하고 노력합니다."

            // Save directly instead of using WorkManager
            try {
                val prefs = PreferencesManager(context)
                // Try to get user SN safely
                val userSn = try {
                    Singleton_t_user.getInstance(context).jsonObject?.optInt("sn") ?: 0
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Error getting user SN: ${e.message}")
                    0
                }

                val message = MessageVO(
                    userSn = userSn,
                    message = text,
                    timeStamp = System.currentTimeMillis()
                )
                Log.v("AlarmReceiver", "알람 수신: $title - $text, message: $message")
                prefs.storeAlarm(message)
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Failed to save alarm: ${e.message}")
                // Still use WorkManager as fallback
                val workManager = WorkManager.getInstance(context)
                val data = workDataOf(
                    "text" to text,
                    "title" to title,
                    "timestamp" to System.currentTimeMillis()
                )
                val workRequest = OneTimeWorkRequestBuilder<SaveAlarmWorker>()
                    .setInputData(data)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
                workManager.enqueue(workRequest)
            }

            val notification: Notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.app_logo)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(0, notification)
        } finally {
            // Release the wake lock
            if (wakeLock.isHeld) wakeLock.release()
        }
    }
}