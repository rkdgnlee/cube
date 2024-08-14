package com.tangoplus.tangoq.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvDashboard2ItemBinding
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

class MD2RVAdpater(private val fragment: Fragment, val data: MutableList<Triple<String, Int, Int>>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class viewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvD2IName : TextView = view.findViewById(R.id.tvD2IName)
        val tvD2ITime : TextView = view.findViewById(R.id.tvD2ITime)
        val tvD2IExplain : TextView = view.findViewById(R.id.tvD2IExplain)
        val tvD2ICount : TextView = view.findViewById(R.id.tvD2ICount)
        val ivD2I : ImageView = view.findViewById(R.id.ivD2I)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = RvDashboard2ItemBinding.inflate(layoutInflater, parent, false)
        return viewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = data[position]

        if (holder is viewHolder) {
            holder.tvD2IName.text = "${setName(stringToLocalDateTime(currentItem.first))}"

            holder.tvD2ITime.text = "약 ${currentItem.second}분"
            holder.tvD2ICount.text = "총 ${currentItem.third}개"
            holder.tvD2IExplain.text = "${setTimeInExplain(stringToLocalDateTime(currentItem.first))} ${setCountInExplain(currentItem.third)}"
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    private fun stringToLocalDateTime(dateTimeString: String) : LocalDateTime {
        val formatterWithSeconds = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val localDateTime = LocalDateTime.parse(dateTimeString, formatterWithSeconds)
        return localDateTime.withSecond(0).withNano(0)
    }


    private fun setTimeInExplain(dateTime: LocalDateTime) : String {
        val dayOfWeek  = dateTime.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("ko"))
        val time = dateTime.toLocalTime()
        val exerciseType = when {
            time.isAfter(LocalTime.of(18, 0)) && time.isBefore(LocalTime.of(21, 1)) -> "저녁"
            time.isAfter(LocalTime.of(21, 0)) && time.isBefore(LocalTime.of(23, 59)) -> "밤"
            time.isAfter(LocalTime.of(0, 0)) && time.isBefore(LocalTime.of(3, 59)) -> "새벽"
            time.isAfter(LocalTime.of(4, 0)) && time.isBefore(LocalTime.of(7, 59)) -> "아침"
            time.isAfter(LocalTime.of(8, 0)) && time.isBefore(LocalTime.of(11, 59)) -> "오전"
            else -> "오후"
        }
        return "$dayOfWeek $exerciseType"
    }

    private fun setName(dateTime: LocalDateTime) : String {
        val formatter = DateTimeFormatter.ofPattern("yyyy. MM. dd.")
        return dateTime.format(formatter)
    }


    private fun setCountInExplain(count : Int) : String {
        return when (count) {
            in 0 until 3 -> "가벼운 운동"
            in 3 until 5 -> "워밍업"
            in 5 until 7 -> "적당한 운동"
            in 7 until 12 -> "상당한 운동"
            else -> "상당히 격한 운동"
        }
    }
}