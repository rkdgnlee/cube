package com.tangoplus.tangoq.Adapter

import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.Listener.OnAlarmClickListener
import com.tangoplus.tangoq.ViewModel.RoutingVO

//class AlarmRVAdapter(var alarmList: MutableList<RoutingVO>, private val listener: OnAlarmClickListener) : RecyclerView.Adapter<AlarmRVAdapter.MyViewHolder>() {
//
//    fun removeData(position: Int) {
//        alarmList.removeAt(position)
//        notifyItemRemoved(position)
//    }
//    inner class MyViewHolder(private val binding: RvAlarmListBinding) : RecyclerView.ViewHolder(binding.root) {
//        fun bind(alarm : RoutingVO, listener: OnAlarmClickListener) {
//            binding.tvAlarm.text = alarm.title
//            binding.tvAlarmDelete.setOnClickListener {
//                removeData(this.layoutPosition)
//            }
//            binding.tvAlarm.setOnClickListener {
//                listener.onAlarmClick(alarm.route)
//
//            }
//        }
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder=MyViewHolder(
//        RvAlarmListBinding.inflate (
//            LayoutInflater.from(parent.context),
//            parent,
//            false
//        )
//    )
//
//    override fun getItemCount(): Int {
//        return alarmList.size
//    }
//    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
//        holder.bind(alarmList[position], listener)
//    }
//}