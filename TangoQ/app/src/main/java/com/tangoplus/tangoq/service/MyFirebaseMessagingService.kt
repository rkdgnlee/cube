package com.tangoplus.tangoq.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.MyApplication
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.vo.MessageVO
import com.tangoplus.tangoq.function.PreferencesManager

class MyFirebaseMessagingService : FirebaseMessagingService() { // 푸시 알림 채널.

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        preferencesManager = (application as MyApplication).preferencesManager
    }
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // 메시지에 데이터 페이로드가 포함 되어 있는지 확인
        // 페이로드란 전송된 데이터를 의미
        val messageToStore: MessageVO

        val userSn = preferencesManager.getUserSn()
        if (userSn == -1) {
            Log.e("FirebaseMessaging", "User SN not set")
            return
        }
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${message.data}")
            sendNotification(
                message.data["title"].toString(),
                message.data["message"].toString()
            )
            messageToStore = MessageVO(
                message = message.data["message"].toString(),
                timeStamp = System.currentTimeMillis(),
                route = "AlarmActivity"
            )
            preferencesManager.storeAlarm(messageToStore)
        } else {
            // 메시지에 알림 페이로드가 포함되어 있는지 확인
            message.notification?.let {
                sendNotification(
                    message.notification?.title.toString(),
                    message.notification?.body.toString()
                )
                messageToStore = MessageVO(
                    message = message.notification?.body.toString(),
                    timeStamp = System.currentTimeMillis(),
                    route = "AlarmActivity"
                )
                preferencesManager.storeAlarm(messageToStore)
            }
        }
    }

    // 수신 된 FCM 메시지를 포함하는 간단한 알림을 만들고 표시
    private fun sendNotification(title: String, body: String) {

        // ----- 해당 알림의 화면 경로 설정 시작  -----
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        // ------! 해당 알림의 화면 경로 설정 끝 !------
        val channelId = "TangoQ"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 앱의 알림 채널을 시스템에 등록
        val channel = NotificationChannel(
            channelId,
            "Channel human readable title",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        notificationManager.notify(0, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        sendRegistrationToServer(token)
    }
    private fun sendRegistrationToServer(token: String?) {
        Log.d("TAG", "sendRegistrationTokenToServer($token)")
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }

}