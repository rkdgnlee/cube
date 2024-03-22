package com.example.mhg

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.AlarmRecyclerViewAdapter
import com.example.mhg.VO.RoutingVO
import com.example.mhg.databinding.ActivityAlarmBinding



class AlarmActivity : AppCompatActivity(), OnAlarmClickListener {
    lateinit var binding: ActivityAlarmBinding
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        binding.rvAlarm.adapter =
        binding.imgbtnbckAlarm.setOnClickListener {
            finish()
        }

        val alarmList = mutableListOf(
            RoutingVO(" 운동이 시작됐습니다.", "home_intermediate"),
            RoutingVO("미션이 부여됐습니다", "pick"),
            RoutingVO("운동 마무리 루틴", "report_goal"),
            RoutingVO("기기 연결이 완료 됐습니다.", "profile")
        )

        // -----! alarm touchhelper 연동 시작 !-----
        val AlarmRecyclerViewAdapter = AlarmRecyclerViewAdapter(alarmList, this)
        val SwipeHelperCallback = SwipeHelperCallback().apply {
            setClamp(260f)
        }
        val itemTouchHelper = ItemTouchHelper(SwipeHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.rvAlarm)
        binding.rvAlarm.apply {
            layoutManager = LinearLayoutManager(applicationContext)
            adapter = AlarmRecyclerViewAdapter
            setOnTouchListener{ _, _ ->
                SwipeHelperCallback.removePreviousClamp(binding.rvAlarm)
                false
            }
        }
        // -----! alarm touchhelper 연동 끝 !-----

        binding.btnAlarmSetting.setOnClickListener {
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, "default")
            }
            startActivity(intent)
        }
        // ----- recyclerview 스와이프 삭제 끝 -----
    }
    override fun onAlarmClick(fragmentId: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("fragmentId", fragmentId)
        intent.putExtra("fromAlarmActivity", true)
        startActivity(intent)

    }
}


