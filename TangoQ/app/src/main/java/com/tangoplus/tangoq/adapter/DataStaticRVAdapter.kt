package com.tangoplus.tangoq.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvDataStaticItemBinding

class DataStaticRVAdapter(private val data: MutableList<Triple<String, String, String?>>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class viewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDSITitle : TextView = view.findViewById(R.id.tvDSITitle)
        val tvDSI1 : TextView = view.findViewById(R.id.tvDSI1)
        val tvDSI2 : TextView = view.findViewById(R.id.tvDSI2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvDataStaticItemBinding.inflate(inflater, parent, false)
        return viewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = data[position]

        if (holder is viewHolder) {
            holder.tvDSITitle.text = currentItem.first
            if (currentItem.third == null) {
                holder.tvDSI1.text = currentItem.second
                holder.tvDSI2.visibility = View.GONE
            } else {
            holder.tvDSI1.text = currentItem.second
            holder.tvDSI2.text = currentItem.third
            }
        }
    }
}