package com.tangoplus.tangoq.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.data.MessageVO
import com.tangoplus.tangoq.listener.OnAlarmClickListener
import com.tangoplus.tangoq.listener.OnAlarmDeleteListener
import com.tangoplus.tangoq.databinding.RvAlarmItemBinding
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

class AlarmRVAdapter(var alarmList: MutableList<MessageVO>, private val clicklistener: OnAlarmClickListener, private val deleteListener: OnAlarmDeleteListener) : RecyclerView.Adapter<AlarmRVAdapter.MyViewHolder>() {

    inner class MyViewHolder(private val binding: RvAlarmItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            alarm: MessageVO,
            clickListener: OnAlarmClickListener,
            deleteListener: OnAlarmDeleteListener
        ) {
            binding.tvAlarmMessage.text = alarm.message
            binding.tvAlarmRemove.setOnClickListener {
                deleteListener.onAlarmDelete(alarm.sn)
            }
            binding.tvAlarmMessage.setOnClickListener {
                clickListener.onAlarmClick(alarm.route)
            }
            val timeStampMillis = Duration.between(Instant.now(), Instant.ofEpochMilli(alarm.timestamp!!)).abs().toMillis()

            binding.tvAlarmTime.text = when {
                TimeUnit.MILLISECONDS.toSeconds(timeStampMillis) < 60 -> "1분 전"
                TimeUnit.MILLISECONDS.toMinutes(timeStampMillis) < 60 -> "${TimeUnit.MILLISECONDS.toMinutes(timeStampMillis)}분 전"
                TimeUnit.MILLISECONDS.toHours(timeStampMillis) < 24 -> "${TimeUnit.MILLISECONDS.toHours(timeStampMillis)}시간 전"
                else -> "${TimeUnit.MILLISECONDS.toDays(timeStampMillis)}일 전"
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