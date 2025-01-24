package com.tangoplus.tangoq.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvDataDynamicAnalysisItemBinding
import com.tangoplus.tangoq.databinding.RvDataDynamicItemBinding
import com.tangoplus.tangoq.view.TrendCurveView


class DataDynamicRVAdapter(private val data: List<List<Pair<Float, Float>>>, private val titles: List<String>, private val case: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val distinctTitles = titles.map { it.replace("좌측 ", "").replace("우측 ", "") }.distinct()
    inner class MAViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDDI1 : TextView = view.findViewById(com.tangoplus.tangoq.R.id.tvDDI1)
        val cvDDI1 : TrendCurveView = view.findViewById(R.id.cvDDI1)
        val tvDDI2 : TextView = view.findViewById(R.id.tvDDI2)
        val cvDDI2 : TrendCurveView = view.findViewById(R.id.cvDDI2)
        val clDDI2 : ConstraintLayout = view.findViewById(R.id.clDDI2)
        val clDDI1In : ConstraintLayout = view.findViewById(R.id.clDDI1In)
    }
    inner class MPAViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDDAITitle : TextView = view.findViewById(R.id.tvDDAITitle)
        val tvDDAI1 : TextView = view.findViewById(R.id.tvDDAI1)
        val tvDDAI2 : TextView = view.findViewById(R.id.tvDDAI2)
        val cvDDAI1 : TrendCurveView = view.findViewById(R.id.cvDDAI1)
        val cvDDAI2 : TrendCurveView = view.findViewById(R.id.cvDDAI2)
        val clDDAI1 : ConstraintLayout = view.findViewById(R.id.clDDAI1)
        val clDDAI2 : ConstraintLayout = view.findViewById(R.id.clDDAI2)
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
            when (position) {
                1 -> {
                    holder.clDDI2.visibility = View.GONE
                    holder.cvDDI1.setPoints(data[pairStartIndex])
                    holder.tvDDI1.text = titles[pairStartIndex]

                    val layoutParams = holder.clDDI1In.layoutParams as ConstraintLayout.LayoutParams
                    layoutParams.horizontalBias = 0.5f
                    holder.clDDI1In.layoutParams = layoutParams
                }
                else -> {
                    if (leftIndex < data.size) {
                        holder.cvDDI1.setPoints(data[pairStartIndex])
                        holder.tvDDI1.text = titles[pairStartIndex]
                    }
                    if (pairStartIndex < data.size) {
                        holder.cvDDI2.setPoints(data[leftIndex])
                        holder.tvDDI2.text = titles[leftIndex]
                    }
                    if (position == 2) {
                        holder.cvDDI1.setMirrored(true)
                        holder.cvDDI2.setMirrored(true)
                    }
                }
            }

        } else if (holder is MPAViewHolder){
            holder.tvDDAITitle.text = "${distinctTitles[position]} 이동 안정성"
            val pairStartIndex = (position * 2)
            val leftIndex = pairStartIndex + 1
            when (position) {
                1 -> {
                    holder.clDDAI2.visibility = View.GONE
                    holder.tvDDAI2.visibility = View.GONE

                    holder.cvDDAI1.setPoints(data[pairStartIndex])
                    holder.tvDDAI1.text = titles[pairStartIndex]

                    val layoutParams = holder.clDDAI1.layoutParams as ConstraintLayout.LayoutParams
                    layoutParams.horizontalBias = 0.5f
                    holder.clDDAI1.layoutParams = layoutParams
                }
                else -> {
                    if (leftIndex < data.size) {
                        holder.cvDDAI1.setPoints(data[pairStartIndex])
                        holder.tvDDAI1.text = titles[pairStartIndex]
                    }
                    if (pairStartIndex < data.size) {
                        holder.cvDDAI2.setPoints(data[leftIndex])
                        holder.tvDDAI2.text = titles[leftIndex]
                    }

                    if (position == 2) {
                        holder.cvDDAI1.setMirrored(true)
                        holder.cvDDAI2.setMirrored(true)
                    }
                }
            }
        }
    }


}