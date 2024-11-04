package com.tangoplus.tangoq.broadcastReceiver

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar
import java.util.TimeZone

class AlarmController(private val context: Context) {

    @SuppressLint("ScheduleExactAlarm")
     fun setNotificationAlarm(title:String, text: String, hour: Int, minute : Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 오늘 저녁 8시의 시간을 설정
        val timeZone = TimeZone.getTimeZone("Asia/Seoul")
        val calendar = Calendar.getInstance(timeZone).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val requestCode = 0 // 이게 같으면 동일 알람임.

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("text", text)
        }
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE)
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)

        Log.v("현재 시간", "${calendar.time}, intent: ${intent.getStringExtra("title")}, ${intent.getStringExtra("text")}")
    }
}