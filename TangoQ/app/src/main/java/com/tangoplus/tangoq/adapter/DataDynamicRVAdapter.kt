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
import com.tangoplus.tangoq.view.TrendCurveView
import com.tangoplus.tangoq.vo.DataDynamicVO


class DataDynamicRVAdapter(private val dynamicVOs : List<DataDynamicVO>, private val judgeFrontCamera: Boolean = false) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
//    private val distinctTitles = titles.map { it.replace("좌측 ", "").replace("우측 ", "") }.distinct()

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
        return dynamicVOs.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MPAViewHolder){
//            // leftIndex가 + 1인 이유: 좌우 값이 반대로 가야함.
//            val pairStartIndex = (position * 2) // 0, 2, 4
//            val leftIndex = pairStartIndex + 1 // 1, 3, 5

            val currentItem = dynamicVOs[position]
            val distinctTitle = currentItem.title1.replace("좌측 ", "").replace("우측 ", "")
            holder.tvDDAITitle.text = "$distinctTitle 이동 안정성"

            val isHip = currentItem.title2.contains("골반")
            if (isHip) {
                holder.clDDAI2.visibility = View.GONE
                holder.tvDDAI2.visibility = View.GONE
                holder.llDDAILegend.visibility = View.GONE

                // data의 중간값을 계산해서 다시 만들기
                // data는 한 좌표의 연속적인 list값임
                val aa = currentItem.data1
                val bb = currentItem.data2
                val centerPoints = aa.zip(bb) {a, b ->
                    Pair((a.first + b.first) / 2f, (a.second + b.second) / 2f)
                }
                holder.cvDDAI1.setPoints(centerPoints)
                holder.tvDDAI1.text = currentItem.title1

                val layoutParams = holder.clDDAI1.layoutParams as ConstraintLayout.LayoutParams
                layoutParams.horizontalBias = 0.5f
                holder.clDDAI1.layoutParams = layoutParams


            } else {
                if (position == 0) {
                    holder.llDDAILegend.visibility = View.VISIBLE
                } else {
                    holder.llDDAILegend.visibility = View.GONE
                }

                holder.cvDDAI1.setPoints(currentItem.data1)
                holder.tvDDAI1.text = currentItem.title1

                holder.cvDDAI2.setPoints(currentItem.data2)
                holder.tvDDAI2.text = currentItem.title2

                if (currentItem.title1.contains("무릎")) {
                    holder.cvDDAI1.setMirrored(judgeFrontCamera)
                    holder.cvDDAI2.setMirrored(judgeFrontCamera)
                }
            }
        }
    }


}