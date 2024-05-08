package com.tangoplus.tangoq.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.Listener.OnAlarmClickListener
import com.tangoplus.tangoq.Listener.OnAlarmDeleteListener
import com.tangoplus.tangoq.Room.Message
import com.tangoplus.tangoq.databinding.RvAlarmItemBinding

class AlarmRVAdapter(var alarmList: MutableList<Message>, private val clicklistener: OnAlarmClickListener, private val deletelistener: OnAlarmDeleteListener) : RecyclerView.Adapter<AlarmRVAdapter.MyViewHolder>() {

    inner class MyViewHolder(private val binding: RvAlarmItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            alarm: Message,
            clicklistener: OnAlarmClickListener,
            deletelistener: OnAlarmDeleteListener
        ) {
            binding.tvAlarm.text = alarm.message
            binding.tvAlarmDelete.setOnClickListener {
                deletelistener.onAlarmDelete(alarm.id)
            }
            binding.tvAlarm.setOnClickListener {
                clicklistener.onAlarmClick(alarm.route)
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
        holder.bind(alarmList[position], clicklistener, deletelistener)
    }
}