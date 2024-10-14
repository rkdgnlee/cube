package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.AlarmRVAdapter
import com.tangoplus.tangoq.callback.SwipeHelperCallback
import com.tangoplus.tangoq.data.MessageVO
import com.tangoplus.tangoq.databinding.FragmentAlarmDialogBinding
import com.tangoplus.tangoq.db.PreferencesManager
import com.tangoplus.tangoq.listener.OnAlarmClickListener
import com.tangoplus.tangoq.listener.OnAlarmDeleteListener
import com.tangoplus.tangoq.`object`.Singleton_t_user
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AlarmDialogFragment : DialogFragment(), OnAlarmClickListener, OnAlarmDeleteListener {
    lateinit var binding : FragmentAlarmDialogBinding
    private lateinit var swipeHelperCallback: SwipeHelperCallback
    private lateinit var alarmRecyclerViewAdapter : AlarmRVAdapter
    private var alarmList = mutableListOf<MessageVO>()
    private lateinit var pm: PreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAlarmDialogBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pm = PreferencesManager(requireContext())

//        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("ko", "KR"))
//
//        val now: Long = System.currentTimeMillis()
//        val date = Date(now)
//        // 30분 전
//        val calendar = Calendar.getInstance()
//        calendar.setTime(date)
//        calendar.add(Calendar.MINUTE, -37)
//        val minute = calendar.time
//        val stringMinute = dateFormat.format(minute)
//        val longMinute = dateFormat.parse(stringMinute)?.time
//
//        calendar.add(Calendar.HOUR, -6)
//        val hour = calendar.time
//        val stringHour = dateFormat.format(hour)
//        val longHour = dateFormat.parse(stringHour)?.time
//
//        calendar.add(Calendar.DATE, -2)
//        val day = calendar.time
//        val stringDay = dateFormat.format(day)
//        val longDay = dateFormat.parse(stringDay)?.time
//
//        // 현재
//        val stringMinuteTime = date
//        val stringTime = dateFormat.format(date)
//        val longTime = dateFormat.parse(stringTime)?.time

        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                alarmList = pm.getAlarms()
                alarmList.reverse()
//                alarmList = mutableListOf(
//                    MessageVO(1, "즉시 시작할 것", timestamp =  longTime ,route = "home_intermediate" ),
//                    MessageVO(2, "미션이 부여됐습니다", timestamp =  longMinute , route = "pick"),
//                    MessageVO(3,"운동 마무리 루틴", timestamp =  longHour ,route = "report_goal"),
//                    MessageVO(4,"기기 연결이 완료 됐습니다.", timestamp =  longDay , route = "profile")
//                )
                // ------! alarm touchhelper 연동 시작 !------
                if (alarmList.isEmpty()) {
                    binding.tvAlarm.visibility = View.VISIBLE
                } else {
                    binding.tvAlarm.visibility = View.GONE
                }

                val alarmRecyclerViewAdapter = AlarmRVAdapter(this@AlarmDialogFragment, alarmList, this@AlarmDialogFragment, this@AlarmDialogFragment)
                swipeHelperCallback = SwipeHelperCallback().apply {
                    setClamp(250f)
                }

                val itemTouchHelper = ItemTouchHelper(swipeHelperCallback)
                itemTouchHelper.attachToRecyclerView(binding.rvAlarm)
                binding.rvAlarm.apply {
                    layoutManager = LinearLayoutManager(requireContext().applicationContext)
                    adapter = alarmRecyclerViewAdapter
                    setOnTouchListener{ _, _ ->
                        swipeHelperCallback.removePreviousClamp(binding.rvAlarm)
                        false
                    }
                } // -----! alarm touchhelper 연동 끝 !-----

            }
        }
    }

    override fun onAlarmClick(fragmentId: String) {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.putExtra("fragmentId", fragmentId)
        intent.putExtra("fromAlarmActivity", true)
        startActivity(intent)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onAlarmDelete(timeStamp: Long?) {
        // ------! 삭제 후 스와이프 초기화 시작 !------
        swipeHelperCallback.removePreviousClamp(binding.rvAlarm)
        binding.rvAlarm.adapter?.notifyDataSetChanged()
        binding.rvAlarm.post {
            binding.rvAlarm.invalidateItemDecorations()
        }
        alarmList.remove(alarmList.find { it.timeStamp == timeStamp })
        pm.deleteAlarm(timeStamp!!)


        // ------! 삭제 후 스와이프 초기화 끝 !------
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
}