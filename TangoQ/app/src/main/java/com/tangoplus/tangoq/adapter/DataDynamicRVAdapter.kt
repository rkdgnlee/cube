package com.tangoplus.tangoq.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvDataDynamicAnalysisItemBinding
import com.tangoplus.tangoq.databinding.RvDataDynamicItemBinding
import com.tangoplus.tangoq.view.TrendCurveView
import java.lang.IllegalArgumentException

class DataDynamicRVAdapter(private val data: List<List<Pair<Float, Float>>>, private val titles: List<String>, private val case: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val distinctTitles = titles.map { it.replace("좌측 ", "").replace("우측 ", "") }.distinct()
    inner class MAViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDDI1 : TextView = view.findViewById(R.id.tvDDI1)
        val cvDDI1 : TrendCurveView = view.findViewById(R.id.cvDDI1)
        val tvDDI2 : TextView = view.findViewById(R.id.tvDDI2)
        val cvDDI2 : TrendCurveView = view.findViewById(R.id.cvDDI2)
    }
    inner class MPAViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDDAITitle : TextView = view.findViewById(R.id.tvDDAITitle)
        val tvDDAILeft : TextView = view.findViewById(R.id.tvDDAILeft)
        val tvDDAIRight : TextView = view.findViewById(R.id.tvDDAIRight)
        val cvDDAILeft : TrendCurveView = view.findViewById(R.id.cvDDAILeft)
        val cvDDAIRight : TrendCurveView = view.findViewById(R.id.cvDDAIRight)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val inflater = LayoutInflater.from(parent.context)
        when (case) {
            0 -> {
                val binding = RvDataDynamicItemBinding.inflate(inflater, parent, false)
                return MAViewHolder(binding.root)
            }
            1 -> {
                val binding = RvDataDynamicAnalysisItemBinding.inflate(inflater, parent, false)
                return MPAViewHolder(binding.root)
            }
            else -> throw IllegalArgumentException("invalid view type binding")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (case) {
            0 -> 0
            1 -> 1
            else -> throw IllegalArgumentException("invalied view type")
        }
    }
    override fun getItemCount(): Int {
        return  3
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MAViewHolder) {
            val pairStartIndex = (position * 2)
            val leftIndex = pairStartIndex + 1

            if (leftIndex < data.size) {
                holder.cvDDI1.setPoints(data[leftIndex])
                holder.tvDDI1.text = titles[leftIndex]
            }

            if (pairStartIndex < data.size) {
                holder.cvDDI2.setPoints(data[pairStartIndex])
                holder.tvDDI2.text = titles[pairStartIndex]
            }
        } else if (holder is MPAViewHolder){
            holder.tvDDAITitle.text = "${distinctTitles[position]} 이동 안정성"

            val pairStartIndex = (position * 2)
            val rightIndex = pairStartIndex + 1

            // 인덱스가 유효한지 확인
            if (pairStartIndex < data.size) {
                holder.cvDDAILeft.setPoints(data[pairStartIndex])
                holder.tvDDAILeft.text = titles[pairStartIndex]
            }

            if (rightIndex < data.size) {
                holder.cvDDAIRight.setPoints(data[rightIndex])
                holder.tvDDAIRight.text = titles[rightIndex]
            }
        }
    }


}