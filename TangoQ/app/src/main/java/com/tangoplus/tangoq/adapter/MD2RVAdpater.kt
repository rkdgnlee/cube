package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.ProgressUnitVO
import com.tangoplus.tangoq.databinding.RvDashboard2ItemBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MD2RVAdpater(private val fragment: Fragment, val data: List<ProgressUnitVO>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class viewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvD2IDate : TextView = view.findViewById(R.id.tvD2IDate)
        val tvD2ITitle : TextView = view.findViewById(R.id.tvD2ITitle)
        val tvD2ICount : TextView = view.findViewById(R.id.tvD2ICount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = RvDashboard2ItemBinding.inflate(layoutInflater, parent, false)
        return viewHolder(binding.root)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = data[position]

        if (holder is viewHolder) {
            holder.tvD2ICount.text = "현재까지 반복한 횟수: ${currentItem.currentWeek}번"
            holder.tvD2IDate.text = "마지막 완료 일자: ${convertDateString(currentItem.currentWeek.toString())}"
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun convertDateString(input: String): String {
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val outputFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        val dateTime = LocalDateTime.parse(input, inputFormatter)

        return dateTime.format(outputFormatter)
    }
}