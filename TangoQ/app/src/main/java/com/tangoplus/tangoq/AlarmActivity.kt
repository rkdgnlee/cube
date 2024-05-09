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
import androidx.room.Room
import com.tangoplus.tangoq.Adapter.AlarmRVAdapter
import com.tangoplus.tangoq.Callback.SwipeHelperCallback
import com.tangoplus.tangoq.Listener.OnAlarmClickListener
import com.tangoplus.tangoq.Listener.OnAlarmDeleteListener
import com.tangoplus.tangoq.Room.Database
import com.tangoplus.tangoq.Room.Message
import com.tangoplus.tangoq.Room.MessageDao
import com.tangoplus.tangoq.ViewModel.RoutingVO
import com.tangoplus.tangoq.databinding.ActivityAlarmBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmActivity : AppCompatActivity(), OnAlarmClickListener, OnAlarmDeleteListener {
    lateinit var binding : ActivityAlarmBinding
    lateinit var messageDao: MessageDao
    lateinit var messages : MutableList<Message>
    lateinit var alarmRecyclerViewAdapter : AlarmRVAdapter
    @SuppressLint("ClickableViewAccessibility", "NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imgbtnbckAlarm.setOnClickListener {
            finish()
        }
        val db = Room.databaseBuilder(
            applicationContext,
            Database::class.java, "tangoqDB"
        ).build()
        messageDao = db.messageDao()
        CoroutineScope(Dispatchers.IO).launch {
            val messages = messageDao.getAllMessages().toMutableList()

            withContext(Dispatchers.Main) {
                val alarmList = mutableListOf(
                    Message(1, "즉시 시작할 것", route = "home_intermediate" ),
                    Message(2, "미션이 부여됐습니다", route = "pick"),
                    Message(3,"운동 마무리 루틴", route = "report_goal"),
                    Message(4,"기기 연결이 완료 됐습니다.", route = "profile")
                )
                // -----! alarm touchhelper 연동 시작 !-----
                val alarmRecyclerViewAdapter = AlarmRVAdapter(alarmList, this@AlarmActivity, this@AlarmActivity)
                val SwipeHelperCallback = SwipeHelperCallback().apply {
                    setClamp(275f)
                }

                val itemTouchHelper = ItemTouchHelper(SwipeHelperCallback)
                itemTouchHelper.attachToRecyclerView(binding.rvAlarm)
                binding.rvAlarm.apply {
                    layoutManager = LinearLayoutManager(applicationContext)
                    adapter = alarmRecyclerViewAdapter
                    setOnTouchListener{ _, _ ->
                        SwipeHelperCallback.removePreviousClamp(binding.rvAlarm)
                        false
                    }
                } // -----! alarm touchhelper 연동 끝 !-----
                alarmRecyclerViewAdapter.notifyDataSetChanged()
            }
        }




    }

    override fun onAlarmClick(fragmentId: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("fragmentId", fragmentId)
        intent.putExtra("fromAlarmActivity", true)
        startActivity(intent)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onAlarmDelete(messageId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            messageDao.deleteMessage(messageId)
            withContext(Dispatchers.Main) {
                val dm = messages.find { it.id == messageId }
                messages.removeAt(messages.indexOf(dm))
                alarmRecyclerViewAdapter.notifyDataSetChanged()
            }
        }
    }
}