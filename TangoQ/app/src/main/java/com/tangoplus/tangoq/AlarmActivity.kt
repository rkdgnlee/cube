package com.tangoplus.tangoq

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.Adapter.AlarmRVAdapter
import com.tangoplus.tangoq.Callback.SwipeHelperCallback
import com.tangoplus.tangoq.Listener.OnAlarmClickListener
import com.tangoplus.tangoq.ViewModel.RoutingVO
import com.tangoplus.tangoq.databinding.ActivityAlarmBinding

class AlarmActivity : AppCompatActivity(), OnAlarmClickListener {
    lateinit var binding : ActivityAlarmBinding
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        val AlarmRecyclerViewAdapter = AlarmRVAdapter(alarmList, this@AlarmActivity)
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
    }

    override fun onAlarmClick(fragmentId: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("fragmentId", fragmentId)
        intent.putExtra("fromAlarmActivity", true)
        startActivity(intent)
    }
}