package com.tangoplus.tangoq.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvRecommendItemBinding
import com.tangoplus.tangoq.databinding.RvReportExpandedItemBinding


class ReportRVAdapter(val parts : MutableList<String>, private val fragment: Fragment) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // TODO REPORT 쏴줄 때의 VO 필요. 8개 TAB LAYOUT에 들어가는 각각의 수치들.

    inner class partViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvREName = view.findViewById<TextView>(R.id.tvREName)
        val clRE = view.findViewById<ConstraintLayout>(R.id.clRE)
        val cvRELine = view.findViewById<CardView>(R.id.cvRELine)
        val ivRE = view.findViewById<ImageView>(R.id.ivRE)
        val ivReArrow = view.findViewById<ImageView>(R.id.ivReArrow)
        val tlRE = view.findViewById<TabLayout>(R.id.tlRE)
        val mcvExpand = view.findViewById<MaterialCardView>(R.id.mcvExpand)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvReportExpandedItemBinding.inflate(inflater, parent, false)
        return partViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = parts[position]
        if (holder is partViewHolder) {
            holder.mcvExpand.visibility = View.GONE
            holder.tvREName.text = currentItem
            holder.clRE.setOnClickListener {
                if (holder.mcvExpand.visibility == View.GONE) {
                    holder.mcvExpand.visibility = View.VISIBLE
                    holder.mcvExpand.animate().apply {
                        duration = 200
                        rotation(0f)
                    }
                    holder.ivReArrow.setImageResource(R.drawable.icon_arrow_up)
                } else {
                    holder.mcvExpand.visibility = View.GONE
                    holder.mcvExpand.animate().apply {
                        duration = 200
                        rotation(0f)
                    }
                    holder.ivReArrow.setImageResource(R.drawable.icon_arrow_down)
                }
            }

        }
    }

    override fun getItemCount(): Int {
        return parts.size
    }
}