package com.tangoplus.tangoq.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvDataDynamicAnalysisItemBinding
import com.tangoplus.tangoq.databinding.RvDataDynamicItemBinding
import com.tangoplus.tangoq.view.TrendCurveView


class DataDynamicRVAdapter(private val data: List<List<Pair<Float, Float>>>, private val titles: List<String>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val distinctTitles = titles.map { it.replace("좌측 ", "").replace("우측 ", "") }.distinct()

    inner class MPAViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDDAITitle : TextView = view.findViewById(R.id.tvDDAITitle)
        val tvDDAI1 : TextView = view.findViewById(R.id.tvDDAI1)
        val tvDDAI2 : TextView = view.findViewById(R.id.tvDDAI2)
        val cvDDAI1 : TrendCurveView = view.findViewById(R.id.cvDDAI1)
        val cvDDAI2 : TrendCurveView = view.findViewById(R.id.cvDDAI2)
        val clDDAI1 : ConstraintLayout = view.findViewById(R.id.clDDAI1)
        val clDDAI2 : ConstraintLayout = view.findViewById(R.id.clDDAI2)
        val llDDAILegend : LinearLayout = view.findViewById(R.id.llDDAILegend)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val inflater = LayoutInflater.from(parent.context)
        val binding = RvDataDynamicAnalysisItemBinding.inflate(inflater, parent, false)
        return MPAViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return  3
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MPAViewHolder){
            holder.tvDDAITitle.text = "${distinctTitles[position]} 이동 안정성"
            val pairStartIndex = (position * 2)
            val leftIndex = pairStartIndex + 1
            when (position) {
                0 -> {
                    holder.llDDAILegend.visibility = View.VISIBLE
                    if (leftIndex < data.size) {
                        holder.cvDDAI1.setPoints(data[pairStartIndex])
                        holder.tvDDAI1.text = titles[pairStartIndex]
                    }
                    if (pairStartIndex < data.size) {
                        holder.cvDDAI2.setPoints(data[leftIndex])
                        holder.tvDDAI2.text = titles[leftIndex]
                    }
                }
                1 -> {
                    holder.clDDAI2.visibility = View.GONE
                    holder.tvDDAI2.visibility = View.GONE
                    holder.llDDAILegend.visibility = View.GONE
                    holder.cvDDAI1.setPoints(data[pairStartIndex])
                    holder.tvDDAI1.text = titles[pairStartIndex]

                    val layoutParams = holder.clDDAI1.layoutParams as ConstraintLayout.LayoutParams
                    layoutParams.horizontalBias = 0.5f
                    holder.clDDAI1.layoutParams = layoutParams
                }
                2 -> {
                    if (leftIndex < data.size) {
                        holder.cvDDAI1.setPoints(data[pairStartIndex])
                        holder.tvDDAI1.text = titles[pairStartIndex]
                    }
                    if (pairStartIndex < data.size) {
                        holder.cvDDAI2.setPoints(data[leftIndex])
                        holder.tvDDAI2.text = titles[leftIndex]
                    }
                    holder.llDDAILegend.visibility = View.GONE
                    holder.cvDDAI1.setMirrored(true)
                    holder.cvDDAI2.setMirrored(true)
                }
            }
        }
    }


}