package com.tangoplus.tangoq.broadcastReceiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.MessageVO
import com.tangoplus.tangoq.db.PreferencesManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AlarmReceiver: BroadcastReceiver() { // 인앱알림 채널
    override fun onReceive(context: Context?, intent: Intent?) {
        val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "0629731260"
        val channelName = "TangoQ"
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        val title = intent?.getStringExtra("title") ?: "TangoQ"
        val text = intent?.getStringExtra("text") ?: "당신의 건강 걱정없는 삶으로 한발짝 다가가기 위해 연구하고 노력합니다."

        val notification: Notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.app_logo)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)  // 알림 클릭 시 자동으로 삭제
            .build()

        val prefs = PreferencesManager(context)
        val message = MessageVO(
            message = text,
            timeStamp = System.currentTimeMillis(),
        )
        Log.v("AlarmReceiver", "알람 수신: $title - $text, message: $message")
        prefs.storeAlarm(message)


        notificationManager.notify(0, notification)


        notification
    }
}