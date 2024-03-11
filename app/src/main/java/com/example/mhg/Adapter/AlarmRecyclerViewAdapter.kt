package com.example.mhg.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mhg.OnAlarmClickListener
import com.example.mhg.VO.RoutingVO
import com.example.mhg.databinding.AlarmlistBinding


class AlarmRecyclerViewAdapter(var alarmList: MutableList<RoutingVO>, private val listener: OnAlarmClickListener) : RecyclerView.Adapter<AlarmRecyclerViewAdapter.MyViewHolder>() {

    fun removeData(position: Int) {
        alarmList.removeAt(position)
        notifyItemRemoved(position)
    }
    inner class MyViewHolder(private val binding: AlarmlistBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(alarm : RoutingVO, listener: OnAlarmClickListener) {
            binding.tvAlarm.text = alarm.title
            binding.tvAlarmDelete.setOnClickListener {
                removeData(this.layoutPosition)
            }
            binding.tvAlarm.setOnClickListener {
                listener.onAlarmClick(alarm.route)

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder=MyViewHolder(
        AlarmlistBinding.inflate (
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun getItemCount(): Int {
        return alarmList.size
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(alarmList[position], listener)
    }
}