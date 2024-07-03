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

class AlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "0629731260"
        val channelName = "TangoQ"
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        val title = intent?.getStringExtra("title") ?: "제목"
        val text = intent?.getStringExtra("text") ?: "본문"

        val notification: Notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.app_logo)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)  // 알림 클릭 시 자동으로 삭제
            .build()

        Log.d("AlarmReceiver", "알람 수신: $title - $text")
        notificationManager.notify(0, notification)


        notification
    }
}