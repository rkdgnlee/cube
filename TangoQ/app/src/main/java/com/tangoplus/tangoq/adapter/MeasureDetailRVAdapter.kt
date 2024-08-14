package com.tangoplus.tangoq.adapter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvMeasureDetailItemBinding
import com.tangoplus.tangoq.listener.OnReportClickListener
import org.json.JSONObject

class MeasureDetailRVAdapter(private val fragment: Fragment, private val scoreJson: JSONObject, private val parts: MutableList<String>, val listener: OnReportClickListener,) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class viewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val mcvMDI: MaterialCardView = view.findViewById(R.id.mcvMDI)
        val mcvMDIExpand: MaterialCardView = view.findViewById(R.id.mcvMDIExpand)
        val clMDI : ConstraintLayout = view.findViewById(R.id.clMDI)
        val tvMDIScore : TextView = view.findViewById(R.id.tvMDIScore)
        val tvMDIName : TextView = view.findViewById(R.id.tvMDIName)
        val ivMDI : ImageView = view.findViewById(R.id.ivMDI)
        val ivMDIArrow : ImageView = view.findViewById(R.id.ivMDIArrow)
        val tvMDIResult : TextView = view.findViewById(R.id.tvMDIResult)

        val tvMDIFrontScore: TextView = view.findViewById(R.id.tvMDIFrontScore)
        val tvMDIBackScore: TextView = view.findViewById(R.id.tvMDIBackScore)
        val tvMDILeftScore: TextView = view.findViewById(R.id.tvMDILeftScore)
        val tvMDIRightScore: TextView = view.findViewById(R.id.tvMDIRightScore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = RvMeasureDetailItemBinding.inflate(layoutInflater, parent, false)
        return viewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return parts.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = parts[position]

        if (holder is viewHolder) {
            holder.mcvMDIExpand.visibility = View.GONE
            holder.tvMDIName.text = currentItem

            holder.clMDI.setOnClickListener {
                if (holder.mcvMDIExpand.visibility == View.GONE) {
                    holder.mcvMDIExpand.visibility = View.VISIBLE
                    Glide.with(fragment)
                        .load(fragment.resources.getIdentifier("drawable_posture_${position + 1}_enabled", "drawable", fragment.requireActivity().packageName))
                        .into(holder.ivMDI)

                    holder.mcvMDIExpand.animate().apply {
                        duration = 150
                        rotation(0f)
                    }
                    holder.ivMDIArrow.setImageResource(R.drawable.icon_arrow_up)
                    Handler(Looper.getMainLooper()).postDelayed({
                        listener.onReportScroll(holder.clMDI)
                    }, 200)
                } else {
                    holder.mcvMDIExpand.visibility = View.GONE
                    Glide.with(fragment)
                        .load(fragment.resources.getIdentifier("drawable_posture_${position + 1}_disabled", "drawable", fragment.requireActivity().packageName))
                        .into(holder.ivMDI)
                    holder.mcvMDIExpand.animate().apply {
                        duration = 150
                        rotation(0f)
                    }
                    holder.ivMDIArrow.setImageResource(R.drawable.icon_arrow_down)
                }

            }
        }
    }
}