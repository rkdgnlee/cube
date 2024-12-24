package com.tangoplus.tangoq.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvDataStaticItemBinding

class DataStaticRVAdapter(private val context: Context, private val data: List<Triple<String, String, String?>>, private val isDangerParts: Boolean) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class StaticViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDSITitle : TextView = view.findViewById(R.id.tvDSITitle)
        val tvDSI1 : TextView = view.findViewById(R.id.tvDSI1)
        val tvDSI2 : TextView = view.findViewById(R.id.tvDSI2)
        val clDSI: ConstraintLayout = view.findViewById(R.id.clDSI)
        val ivDSI : ImageView = view.findViewById(R.id.ivDSI)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvDataStaticItemBinding.inflate(inflater, parent, false)
        return StaticViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = data[position]
        if (holder is StaticViewHolder) {
            if (isDangerParts) {
                setBackgroundDrawable(holder.clDSI)
                holder.ivDSI.visibility = View.VISIBLE
            }
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
        cl.setBackgroundResource(R.drawable.bckgnd_rectangle_12)
        cl.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context,R.color.subColor100))
    }
}