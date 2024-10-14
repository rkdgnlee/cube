package com.tangoplus.tangoq.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvDataStaticItemBinding

class DataStaticRVAdapter(private val context: Context, private val data: List<Triple<String, String, String?>>, private val isDangerParts: Boolean) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class viewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDSITitle : TextView = view.findViewById(R.id.tvDSITitle)
        val tvDSI1 : TextView = view.findViewById(R.id.tvDSI1)
        val tvDSI2 : TextView = view.findViewById(R.id.tvDSI2)
        val clDSI: ConstraintLayout = view.findViewById(R.id.clDSI)
        val ivDSI : ImageView = view.findViewById(R.id.ivDSI)
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
            if (isDangerParts) {
                setBackgroundDrawable(holder.clDSI)
                holder.ivDSI.visibility = View.VISIBLE
            }
//            val newDangerParts = dangerParts.map { it.replace("좌측 ", "").replace("우측 ", "") }
//            val containsDangerPart = newDangerParts.any { part -> currentItem.first.contains(part) }
//            if (containsDangerPart) {
//                setBackgroundDrawable(holder.clDSI)
//            }
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
    private fun setBackgroundDrawable(cl : ConstraintLayout) {
        cl.setBackgroundResource(R.drawable.background_stroke_1dp_main_color_12dp)
    }



}   // @drawable/background_stroke_1dp_sub_color