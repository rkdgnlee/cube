package com.tangoplus.tangoq.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.MessageVO
import com.tangoplus.tangoq.listener.OnAlarmClickListener
import com.tangoplus.tangoq.listener.OnAlarmDeleteListener
import com.tangoplus.tangoq.databinding.RvAlarmItemBinding
import com.tangoplus.tangoq.fragment.hideBadgeOnClick
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class AlarmRVAdapter(private val fragment : Fragment, var alarmList: MutableList<MessageVO>, private val clicklistener: OnAlarmClickListener, private val deleteListener: OnAlarmDeleteListener) : RecyclerView.Adapter<AlarmRVAdapter.MyViewHolder>() {

    inner class MyViewHolder(private val binding: RvAlarmItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            alarm: MessageVO,
            clickListener: OnAlarmClickListener,
            deleteListener: OnAlarmDeleteListener
        ) {
            val hideBadgeFunction = fragment.hideBadgeOnClick(
                binding.tvAlarmMessage,
                binding.clAlarm,
                "${alarm.message}",
                ContextCompat.getColor(fragment.requireContext(), R.color.thirdColor))

            binding.tvAlarmMessage.text = alarm.message
            binding.tvAlarmRemove.setOnClickListener {
                deleteListener.onAlarmDelete(alarm.timeStamp)
            }
            binding.tvAlarmMessage.setOnClickListener {
                clickListener.onAlarmClick(alarm.route)
                hideBadgeFunction?.invoke()
            }

            val now = System.currentTimeMillis()
            val timeStamp  = alarm.timeStamp
            val duration = Duration.between(Instant.ofEpochMilli(timeStamp), Instant.ofEpochMilli(now))

            val seconds = duration.seconds
            val minutes = duration.toMinutes()
            val hours = duration.toHours()
            val days = duration.toDays()
            binding.tvAlarmTime.text = when {
                seconds < 60 -> "1분 전"
                minutes < 60 -> "${minutes}분 전"
                hours < 24 -> "${hours}시간 전"
                else -> "${days}일 전"
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder=MyViewHolder(
        RvAlarmItemBinding.inflate (
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun getItemCount(): Int {
        return alarmList.size
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(alarmList[position], clicklistener, deleteListener)
    }
}