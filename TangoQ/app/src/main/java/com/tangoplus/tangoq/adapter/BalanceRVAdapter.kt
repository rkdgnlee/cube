package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvBalanceItemBinding
import com.tangoplus.tangoq.fragment.MeasureAnalysisFragment

class BalanceRVAdapter(private val fragment: Fragment, private val stages:MutableList<MutableList<String>>, private val degree: MutableList<Pair<Int, Int>>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    inner class balanceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBIName: TextView = view.findViewById(R.id.tvBIName)
        val ivBI: ImageView = view.findViewById(R.id.ivBI)
        val ivBIicon: ImageView = view.findViewById(R.id.ivBIicon)
        val tvBIPredict: TextView = view.findViewById(R.id.tvBIPredict)
        val clBI : ConstraintLayout = view.findViewById(R.id.clBI)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvBalanceItemBinding.inflate(inflater, parent, false)
        return balanceViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return 4
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = stages[position]
        if (holder is balanceViewHolder) {
            setItem(position, holder.ivBI, holder.tvBIName)
            setColor(degree[position].first, holder.tvBIPredict, holder.ivBIicon)

            if (currentItem.size < 2) {
                holder.tvBIPredict.text  = "${currentItem[0]} ${transferDegree(degree[position])}"
            } else if (degree[position].second > 3 || degree[position].second < -2) {
                holder.tvBIPredict.text =  "전체적인 균형이 잘 잡혀 있습니다."
            } else {
                holder.tvBIPredict.text  = "${currentItem[0]}와 ${currentItem[1]}${transferDegree(degree[position])}"
            }

            holder.clBI.setOnClickListener{

                fragment.requireActivity().supportFragmentManager.beginTransaction().apply {
                    replace(R.id.flMain, MeasureAnalysisFragment.newInstance(position))
                    addToBackStack(null)
                    commit()
                }
            }
        }
    }

    private fun transferDegree(degree: Pair<Int, Int>): String {
        return when (degree.second) {
            3 -> "이(가) 과도하게 치우쳐 있습니다."
            2 -> "에서 이상을 감지했습니다."
            1 -> "에 치우침이 있습니다."
            0 -> "에 약간의 불균형이 있습니다."
            -1 -> "에 미세한 불균형이 있습니다."
            -2 -> "는(은) 올바른 균형입니다."
            else -> ""
        }
    }
    private fun setColor(index: Int, tv1: TextView, iv: ImageView) {
        when (index) {
            2 -> {
                tv1.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.deleteColor))
                iv.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_caution))
            }
            1 -> {
                tv1.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.cautionColor))
                iv.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_warning))
            }
            0 -> {
                tv1.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.subColor400))
                iv.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_ball))

            }
        }
    }

    private fun setItem(index: Int, iv: ImageView, tvName: TextView) {
        when (index) {
            0 -> {
                iv.setImageResource(R.drawable.drawable_front)
                tvName.text = "정면 균형"
            }
            1 -> {
                iv.setImageResource(R.drawable.drawable_side)
                tvName.text = "측면 균형"
            }
            2 -> {
                iv.setImageResource(R.drawable.drawable_back)
                tvName.text = "후면 균형"
            }
            3 -> {
                iv.setImageResource(R.drawable.drawable_dynamic)
                tvName.text = "동적 균형"
            }
        }
    }
}