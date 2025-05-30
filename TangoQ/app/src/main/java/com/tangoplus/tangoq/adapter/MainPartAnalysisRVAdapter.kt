package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
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
import com.tangoplus.tangoq.databinding.RvMainPartAnalysisItemBinding
import com.tangoplus.tangoq.dialog.PoseDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.viewmodel.AnalysisViewModel
import com.tangoplus.tangoq.vision.MathHelpers.isTablet
import com.tangoplus.tangoq.vo.AnalysisVO

class MainPartAnalysisRVAdapter(private val fragment: Fragment, private var analyze : AnalysisVO, private var avm: AnalysisViewModel): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    inner class MPAViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMPAITitle : TextView = view.findViewById(R.id.tvMPAITitle)
        val vMPAILeft : View = view.findViewById(R.id.vMPAILeft)
        val vMPAIMiddle: View = view.findViewById(R.id.vMPAIMiddle)
        val vMPAIRight : View = view.findViewById(R.id.vMPAIRight)

        val tvMPAILeft : TextView = view.findViewById(R.id.tvMPAILeft)
        val tvMPAIMiddle : TextView = view.findViewById(R.id.tvMPAIMiddle)
        val tvMPAIRight : TextView = view.findViewById(R.id.tvMPAIRight)

        val ivMPAIArrow : ImageView = view.findViewById(R.id.ivMPAIArrow)
        val tvMPAIExplain : TextView = view.findViewById(R.id.tvMPAIExplain)
        val tvMPAIData : TextView = view.findViewById(R.id.tvMPAIData)

        val clMPAI : ConstraintLayout = view.findViewById(R.id.clMPAI)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvMainPartAnalysisItemBinding.inflate(inflater, parent, false)
        return MPAViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return analyze.labels.size
    }

    @SuppressLint("DefaultLocale")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        if (holder is MPAViewHolder) {
            val currentItem = analyze.labels[position]

            holder.tvMPAITitle.text = currentItem.rawDataName
                .replace(" - ", "")
                .replace("정면", "")
                .replace("후면", "")
                .replace("앉은 후면", "")

            setState(holder, currentItem.state)
            val rawDataValue = currentItem.rawData.toDouble() // null인 경우 0으로 대체
            holder.tvMPAIData.text = if (currentItem.columnName.contains("distance")) {
                "${String.format("%.2f", if (rawDataValue.isNaN()) 0.0 else rawDataValue)}cm"
            } else {
                "${String.format("%.2f", if (rawDataValue.isNaN()) 0.0 else rawDataValue)}°"
            }
            val spannableString = SpannableString(currentItem.summary)
            val accentIndex =  holder.tvMPAIExplain.text.indexOf("값")
            if (accentIndex != -1) {
                val endIndex = minOf(accentIndex + 4, currentItem.summary.length)
                val colorSpan = ForegroundColorSpan(ContextCompat.getColor(fragment.requireContext(), R.color.deleteColor))
                spannableString.setSpan(colorSpan, accentIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            holder.tvMPAIExplain.text = spannableString
            if (avm.analysisType == 1) {
                holder.clMPAI.setOnSingleClickListener {
                    val dialog = PoseDialogFragment.newInstance(analyze.indexx)
                    dialog.show(fragment.requireActivity().supportFragmentManager, "MainPartPoseDialogFragment")
                }
            }

        }
    }

    private fun setState(holder: MPAViewHolder, isNormal: Int) {
        val params = holder.ivMPAIArrow.layoutParams as ConstraintLayout.LayoutParams
        when (isNormal) {
            3 -> {
                holder.vMPAILeft.visibility = View.VISIBLE
                holder.vMPAIMiddle.visibility = View.INVISIBLE
                holder.vMPAIRight.visibility = View.INVISIBLE
                holder.tvMPAILeft.setTextColor(ContextCompat.getColorStateList(fragment.requireContext(), R.color.deleteColor))
                holder.tvMPAIMiddle.setTextColor(ContextCompat.getColorStateList(fragment.requireContext(), R.color.subColor400))
                holder.tvMPAIRight.setTextColor(ContextCompat.getColorStateList(fragment.requireContext(), R.color.subColor400))
                holder.tvMPAIData.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.deleteColor))

                params.horizontalBias = if (isTablet(fragment.requireContext())) 0.15f else 0.125f
            }
            2 -> {
                holder.vMPAILeft.visibility = View.INVISIBLE
                holder.vMPAIMiddle.visibility = View.VISIBLE
                holder.vMPAIRight.visibility = View.INVISIBLE
                holder.tvMPAILeft.setTextColor(ContextCompat.getColorStateList(fragment.requireContext(), R.color.subColor400))
                holder.tvMPAIMiddle.setTextColor(ContextCompat.getColorStateList(fragment.requireContext(), R.color.cautionColor))
                holder.tvMPAIRight.setTextColor(ContextCompat.getColorStateList(fragment.requireContext(), R.color.subColor400))
                holder.tvMPAIData.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.cautionColor))

                params.horizontalBias = 0.5f
            }
            1 -> {
                holder.vMPAILeft.visibility = View.INVISIBLE
                holder.vMPAIMiddle.visibility = View.INVISIBLE
                holder.vMPAIRight.visibility = View.VISIBLE
                holder.tvMPAILeft.setTextColor(ContextCompat.getColorStateList(fragment.requireContext(), R.color.subColor400))
                holder.tvMPAIMiddle.setTextColor(ContextCompat.getColorStateList(fragment.requireContext(), R.color.subColor400))
                holder.tvMPAIRight.setTextColor(ContextCompat.getColorStateList(fragment.requireContext(), R.color.thirdColor))
                holder.tvMPAIData.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.thirdColor))

                params.horizontalBias = if (isTablet(fragment.requireContext())) 0.85f else 0.875f
            }
        }
    }
}