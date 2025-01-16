package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.vo.ProgressHistoryVO
import com.tangoplus.tangoq.databinding.RvDashboard2ItemBinding

class ProgressHistoryRVAdapter(val data: List<ProgressHistoryVO>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class PHViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvD2IProgram : TextView = view.findViewById(R.id.tvD2IProgram)
        val tvD2ITitle : TextView = view.findViewById(R.id.tvD2ITitle)
        val tvD2IWeek : TextView = view.findViewById(R.id.tvD2IWeek)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = RvDashboard2ItemBinding.inflate(layoutInflater, parent, false)
        return PHViewHolder(binding.root)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = data[position]

        if (holder is PHViewHolder) {
            holder.tvD2IWeek.text = "${currentItem.weekNumber}주차 진행"
            holder.tvD2IProgram.text = currentItem.recommendationTitle
            holder.tvD2ITitle.text = currentItem.exerciseName
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

}