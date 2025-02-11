package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.vo.ProgressHistoryVO
import com.tangoplus.tangoq.databinding.RvDashboard2ItemBinding

class ProgressHistoryRVAdapter(private val fragment: Fragment, val data: List<ProgressHistoryVO>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class PHViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvD2ITitle : TextView = view.findViewById(R.id.tvD2ITitle)
        val tvD2ITime : TextView = view.findViewById(R.id.tvD2ITime)
        val ivD2IThumbnail : ImageView = view.findViewById(R.id.ivD2IThumbnail)
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

            holder.tvD2ITime.text = "완료한 시간 : ${currentItem.createdAt.substring(10, currentItem.createdAt.length)}"
            holder.tvD2ITitle.text = currentItem.exerciseName
            Glide.with(fragment.requireContext())
                .load(currentItem.imageFilePathReal)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(180)
                .into(holder.ivD2IThumbnail)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

}