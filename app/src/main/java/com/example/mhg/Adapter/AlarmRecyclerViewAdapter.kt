package com.example.mhg.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mhg.VO.RoutingVO
import com.example.mhg.databinding.AlarmlistBinding


class AlarmRecyclerViewAdapter(var alarmList: MutableList<RoutingVO>) : RecyclerView.Adapter<AlarmRecyclerViewAdapter.MyViewHolder>() {

    fun removeData(position: Int) {
        alarmList.removeAt(position)
        notifyItemRemoved(position)
    }
    inner class MyViewHolder(private val binding: AlarmlistBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(alarm : RoutingVO) {
            binding.tvAlarm.text = alarm.title
            binding.tvAlarmDelete.setOnClickListener {
                removeData(this.layoutPosition)
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
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) = holder.bind(alarmList[position])



}