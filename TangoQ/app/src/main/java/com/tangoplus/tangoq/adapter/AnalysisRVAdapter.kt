package com.tangoplus.tangoq.adapter

import android.content.res.ColorStateList
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
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
import com.tangoplus.tangoq.data.AnalysisUnitVO
import com.tangoplus.tangoq.databinding.RvMainPartAnalysisItemBinding

class AnalysisRVAdapter(private val fragment: Fragment, private val analysisUnits : MutableList<AnalysisUnitVO>? ): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    inner class AnalysisViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMPAITitle : TextView = view.findViewById(R.id.tvMPAITitle)
        val vMPAILeft : View = view.findViewById(R.id.vMPAILeft)
        val vMPAIRight : View = view.findViewById(R.id.vMPAIRight)
        val tvMPAILeft : TextView = view.findViewById(R.id.tvMPAILeft)
        val tvMPAIRight : TextView = view.findViewById(R.id.tvMPAIRight)
        val ivMPAIArrow : ImageView = view.findViewById(R.id.ivMPAIArrow)
        val tvMPAIExplain : TextView = view.findViewById(R.id.tvMPAIExplain)
        val tvMPAIData : TextView = view.findViewById(R.id.tvMPAIData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvMainPartAnalysisItemBinding.inflate(inflater, parent, false)
        return AnalysisViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return analysisUnits?.size ?: 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        if (holder is AnalysisViewHolder) {
            val currentItem = analysisUnits?.get(position)
            if (currentItem != null) {

                holder.tvMPAITitle.text = currentItem.rawDataName
                setState(holder, currentItem.state)
                holder.tvMPAIData.text = if (currentItem.columnName.contains("distance")) "${String.format("%.2f", currentItem.rawData)}cm" else "${String.format("%.2f", currentItem.rawData)}°"
                val spannableString = SpannableString(currentItem.summary)
                val accentIndex =  holder.tvMPAIExplain.text.indexOf("값")
                if (accentIndex != -1) {
                    val endIndex = minOf(accentIndex + 4, currentItem.summary.length)
                    val colorSpan = ForegroundColorSpan(ContextCompat.getColor(fragment.requireContext(), R.color.deleteColor))
                    spannableString.setSpan(colorSpan, accentIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                holder.tvMPAIExplain.text = spannableString
            }

        }
    }

    private fun setState(holder: AnalysisViewHolder, isNormal: Boolean) {
        val params = holder.ivMPAIArrow.layoutParams as ConstraintLayout.LayoutParams
        when (isNormal) {
            false -> {
                holder.vMPAILeft.visibility = View.VISIBLE
                holder.vMPAIRight.visibility = View.INVISIBLE
                holder.tvMPAILeft.setTextColor(ContextCompat.getColorStateList(fragment.requireContext(), R.color.deleteColor))
                holder.tvMPAIRight.setTextColor(ContextCompat.getColorStateList(fragment.requireContext(), R.color.subColor400))
                holder.tvMPAIData.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.deleteColor))

                params.horizontalBias = 0.12f
            }
            true -> {
                holder.vMPAILeft.visibility = View.INVISIBLE
                holder.vMPAIRight.visibility = View.VISIBLE
                holder.tvMPAILeft.setTextColor(ContextCompat.getColorStateList(fragment.requireContext(), R.color.subColor400))
                holder.tvMPAIRight.setTextColor(ContextCompat.getColorStateList(fragment.requireContext(), R.color.thirdColor))
                holder.tvMPAIData.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.thirdColor))

                params.horizontalBias = 0.65f
            }
        }
    }
}