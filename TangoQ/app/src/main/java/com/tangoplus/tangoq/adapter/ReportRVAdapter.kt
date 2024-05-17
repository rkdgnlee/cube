package com.tangoplus.tangoq.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignEnd
import com.skydoves.balloon.showAlignTop
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvRecommendItemBinding
import com.tangoplus.tangoq.databinding.RvReportExpandedItemBinding
import com.tangoplus.tangoq.listener.OnReportClickListener


class ReportRVAdapter(val parts : MutableList<String>, private val fragment: Fragment, private val listener: OnReportClickListener, private val nsv : NestedScrollView) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // TODO REPORT 쏴줄 때의 VO 필요. 8개 TAB LAYOUT에 들어가는 각각의 수치들.

    inner class partViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvREName = view.findViewById<TextView>(R.id.tvREName)
        val clRE = view.findViewById<ConstraintLayout>(R.id.clRE)
        val cvRELine = view.findViewById<CardView>(R.id.cvRELine)
        val ivRE = view.findViewById<ImageView>(R.id.ivRE)
        val ivReArrow = view.findViewById<ImageView>(R.id.ivReArrow)
        val tlRE = view.findViewById<TabLayout>(R.id.tlRE)
        val mcvExpand = view.findViewById<MaterialCardView>(R.id.mcvExpand)
        val btnREShowDetail = view.findViewById<AppCompatButton>(R.id.btnREShowDetail)
        val ibtnREInfo = view.findViewById<ImageButton>(R.id.ibtnREInfo)
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
                    listener.onReportClick(position, nsv, holder.mcvExpand)
                } else {
                    holder.mcvExpand.visibility = View.GONE
                    holder.mcvExpand.animate().apply {
                        duration = 200
                        rotation(0f)
                    }
                    holder.ivReArrow.setImageResource(R.drawable.icon_arrow_down)
                }
            }
            holder.tlRE.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(p0: TabLayout.Tab?) {
                    when (p0?.position) {
                        0 -> { holder.cvRELine.rotation = -10f }
                        1 -> { holder.cvRELine.rotation = 10f }
                        2 -> { holder.cvRELine.rotation = -10f }
                        3 -> { holder.cvRELine.rotation = -5f }
                        4 -> { holder.cvRELine.rotation = 3f }
                        5 -> { holder.cvRELine.rotation = -12f }
                        6 -> { holder.cvRELine.rotation = 0f }
                        7 -> { holder.cvRELine.rotation = 0f }
                    }
                }
                override fun onTabUnselected(p0: TabLayout.Tab?) {}
                override fun onTabReselected(p0: TabLayout.Tab?) {}
            })
            val balloon = Balloon.Builder(fragment.requireContext())
                .setWidthRatio(0.6f)
                .setHeight(BalloonSizeSpec.WRAP)
                .setText("밸런스 선의 기울기가 심할 수록\n교정이 필요합니다")
                .setTextColorResource(R.color.black)
                .setTextSize(15f)
                .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                .setArrowSize(0)
                .setMargin(6)
                .setPadding(12)
                .setCornerRadius(8f)
                .setBackgroundColorResource(R.color.subColor100)
                .setBalloonAnimation(BalloonAnimation.ELASTIC)
                .setLifecycleOwner(fragment.viewLifecycleOwner)
                .build()
            holder.ibtnREInfo.setOnClickListener {
                holder.ibtnREInfo.showAlignEnd(balloon)
                balloon.dismissWithDelay(2500L)
            }

            holder.btnREShowDetail.setOnClickListener {

            }
        }
    }

    override fun getItemCount(): Int {
        return parts.size
    }
}