package com.tangoplus.tangoq.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvDataItemBinding
import kotlin.math.roundToLong

class MeasureDataRVAdapter(private val fragment: Fragment, private val data: MutableList<Triple<String, String, String?>>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class viewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDITitle : TextView = view.findViewById(R.id.tvDITitle)
        val tvDI1 : TextView = view.findViewById(R.id.tvDI1)
        val tvDI2 : TextView = view.findViewById(R.id.tvDI2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvDataItemBinding.inflate(inflater, parent, false)
        return viewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = data[position]

        if (holder is viewHolder) {
            holder.tvDITitle.text = currentItem.first
            if (currentItem.third == null) {
                holder.tvDI1.text = currentItem.second
                holder.tvDI2.visibility = View.GONE
            } else {
            holder.tvDI1.text = currentItem.second
            holder.tvDI2.text = currentItem.third
            }
        }
    }
}