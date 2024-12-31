package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.skydoves.progressview.ProgressView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.vo.AnalysisVO
import com.tangoplus.tangoq.databinding.RvMeasureTrendItemBinding
import com.tangoplus.tangoq.mediapipe.MathHelpers.calculateBoundedScore
import kotlin.math.abs

class TrendRVAdapter(private val fragment: Fragment, private val analyzes1: MutableList<MutableList<AnalysisVO>>?, private val analyzes2: MutableList<MutableList<AnalysisVO>>?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    inner class TrendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMTIPart : TextView = view.findViewById(R.id.tvMTIPart)
        val ivMTI : ImageView = view.findViewById(R.id.ivMTI)
        private val tvMTI1 : TextView = view.findViewById(R.id.tvMTI1)
        private val tvMTI2 : TextView = view.findViewById(R.id.tvMTI2)
        private val tvMTI3 : TextView = view.findViewById(R.id.tvMTI3)
        private val tvMTISeq1 : TextView = view.findViewById(R.id.tvMTISeq1)
        private val tvMTISeq2 : TextView = view.findViewById(R.id.tvMTISeq2)
        private val tvMTISeq3 : TextView = view.findViewById(R.id.tvMTISeq3)
        private val pvMTI1Left : ProgressView = view.findViewById(R.id.pvMTI1Left)
        private val pvMTI1Right : ProgressView = view.findViewById(R.id.pvMTI1Right)
        private val pvMTI2Left : ProgressView = view.findViewById(R.id.pvMTI2Left)
        private val pvMTI2Right : ProgressView = view.findViewById(R.id.pvMTI2Right)
        private val pvMTI3Left : ProgressView = view.findViewById(R.id.pvMTI3Left)
        private val pvMTI3Right : ProgressView = view.findViewById(R.id.pvMTI3Right)
        val cvMTI : CardView = view.findViewById(R.id.cvMTI)
        val tvSeqs = listOf(tvMTISeq1, tvMTISeq2, tvMTISeq3)
        val pvs = listOf(listOf(pvMTI1Left, pvMTI1Right), listOf(pvMTI2Left, pvMTI2Right), listOf(pvMTI3Left, pvMTI3Right))
        val tvPoses = listOf(tvMTI1, tvMTI2, tvMTI3)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvMeasureTrendItemBinding.inflate(inflater, parent, false)
        return TrendViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return analyzes2?.size ?: 0
    }

    @SuppressLint("DiscouragedApi")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TrendViewHolder) {
            if (analyzes2?.isNotEmpty() == true) {
                val currentItem = analyzes2[position] // analysisVO가 들어가있는 부위 별임.
                holder.tvMTIPart.text = matchedParts[position]

                val jointIndex = if (position == 0) 1 else (position + 1) / 2 + 1

                // ------# 부위 drawable 좌우 반전 #------
                val resourceName = "@drawable/icon_part$jointIndex"
                val resourceId = holder.ivMTI.context.resources.getIdentifier(
                    resourceName, "drawable", holder.ivMTI.context.packageName
                )

                holder.ivMTI.setImageResource(resourceId)
                // 2~7에 해당하는 홀수 번째 항목일 때만 좌우 반전 적용
                if (position != 0 && position % 2 != 0) {
                    holder.ivMTI.scaleX = -1f // 좌우 반전
                } else {
                    holder.ivMTI.scaleX = 1f // 원래 상태
                }

                // -------# 부위 선택 #------
                val selectAnalysisIndex = matchedIndexs[position]

                val normalCount = selectAnalysisIndex.count { indexTriple ->
                    currentItem[indexTriple.second].labels[indexTriple.third].state == 0 || currentItem[indexTriple.second].labels[indexTriple.third].state == 1
                }

                Log.v("선택된 항목 판단", "$selectAnalysisIndex,선택된 3개 중 normal 개수: $normalCount")
                if (normalCount >= 2) { // 3개 중 2개 이상이 normal이면 true
                    setPartState(holder, true)
                } else {
                    setPartState(holder, false)
                }

                selectAnalysisIndex.forEachIndexed { index, indexTriple -> // 3, 5, 5 //  0, 0, 1
                    holder.tvSeqs[index].text = setSeqString(indexTriple.first)

                    val unit = currentItem[indexTriple.second].labels[indexTriple.third]
                    val score = calculateBoundedScore(abs(unit.rawData), unit.rawDataBound)
                    holder.pvs[index][1].progress = score

                    holder.tvPoses[index].text = unit.rawDataName
                        .replace("양", "")
                        .replace("과", "")
                        .replace("와", "")
                        .replace("에서", "-")
                        .replace("좌측 ", "")
                        .replace("우측 ", "")

                    setState(holder, index, splitState(score))
                }

            }
            if (analyzes1 != null) {
                val currentItem = analyzes1[position] // analysisVO가 들어가있는 부위 별임.
                val selectAnalysisIndex = matchedIndexs[position]
                selectAnalysisIndex.forEachIndexed { index, indexTriple -> // 3, 5, 5 //  0, 0, 1
                    val unit = currentItem[indexTriple.second].labels[indexTriple.third]
                    Log.v("각 점수들", "측정명: ${unit.rawDataName}, 값: ${unit.rawData}, 범위: ${unit.rawDataBound}")
                    val score = calculateBoundedScore(unit.rawData, unit.rawDataBound)
                    holder.pvs[index][0].progress = score
                }
            }
        }
    }

    private val matchedParts = listOf(
        "목관절" , "좌측 어깨", "우측 어깨", "좌측 팔꿉", "우측 팔꿉", "좌측 손목" , "우측 손목" , "좌측 골반", "우측 골반" , "좌측 무릎" , "우측 무릎" , "좌측 발목", "우측 발목")

    // first: seq / second: matchedUris의 index / third: 가장 작은 index

    private val matchedIndexs = listOf(
        listOf(Triple(3,1,0), Triple(5, 3, 0), Triple(6, 4, 0)),
        // 어깨
        listOf(Triple(0,0,1), Triple(3, 1, 0), Triple(5, 2, 1)),
        listOf(Triple(0,0,1), Triple(4, 1, 0), Triple(5, 2, 1)),
        // 팔꿉
        listOf(Triple(0,0,0), Triple(2, 1, 0), Triple(3, 2, 2)),
        listOf(Triple(0,0,0), Triple(2, 1, 0), Triple(4, 2, 2)),
        // 손목
        listOf(Triple(0,0,1), Triple(2, 1, 0), Triple(3, 2, 0)),
        listOf(Triple(0,0,1), Triple(2, 1, 0), Triple(4, 2, 0)),
        // 골반
        listOf(Triple(0, 0, 1), Triple(3,1,1), Triple(5, 2, 0)),
        listOf(Triple(0, 0, 1), Triple(4,1,1), Triple(5, 2, 0)),
        // 무릎
        listOf(Triple(0,0,0), Triple(0, 0, 1), Triple(5, 1, 1)),
        listOf(Triple(0,0,0), Triple(0, 0, 1), Triple(5, 1, 1)),
        // 발목
        listOf(Triple(0,0,2), Triple(5, 1, 0), Triple(5, 1, 1)),
        listOf(Triple(0,0,2), Triple(5, 1, 0), Triple(5, 1, 1)),
    )

    private fun setSeqString(seq: Int?) : String {
        return when (seq) {
            0 -> "정면 분석"
            1 -> "동적 분석"
            2 -> "팔꿉 분석"
            3 -> "좌측 분석"
            4 -> "우측 분석"
            5 -> "후면 분석"
            6 -> "앉아 후면"
            else -> ""
        }
    }

    private fun splitState(score : Float) : Boolean {
        return when {
            score in 66f .. 100f -> true
            else -> false
        }
    }

    private fun setState(holder: TrendViewHolder, index: Int,state: Boolean) {
        when (state) {
            true -> {
                holder.tvSeqs[index].backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.thirdColor))
                holder.pvs[index][1].highlightView.color = ContextCompat.getColor(fragment.requireContext(), R.color.thirdColor)
            }
            else -> {
                holder.tvSeqs[index].backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.deleteColor))
                holder.pvs[index][1].highlightView.color = ContextCompat.getColor(fragment.requireContext(), R.color.deleteColor)
            }
        }
    }
    private fun setPartState(holder: TrendViewHolder, state: Boolean) {
        when (state) {
            true -> {
                holder.tvMTIPart.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.subColor100))
                holder.tvMTIPart.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.subColor800)))
                holder.cvMTI.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.subColor500)))
            }
            else -> {
                holder.tvMTIPart.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.deleteContainerColor))
                holder.tvMTIPart.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.deleteColor)))
                holder.cvMTI.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.deleteColor)))
            }
        }
    }

//    private fun setPartState(holder: TrendViewHolder, state: Int) {
//        when (state) {
//            0 -> {
//                holder.tvMTIPart.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.subColor100))
//                holder.tvMTIPart.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.subColor800)))
//                holder.cvMTI.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.subColor500)))
//            }
//            1 -> {
//                holder.tvMTIPart.backgroundTintList = ContextCompat.getColorStateList(fragment.requireContext(), R.color.cautionContainerColor)
//                holder.tvMTIPart.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.cautionColor))
//                holder.cvMTI.setCardBackgroundColor(fragment.resources.getColor(R.color.cautionColor, null))
//            }
//            2 -> {
//                holder.tvMTIPart.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.deleteContainerColor))
//                holder.tvMTIPart.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.deleteColor)))
//                holder.cvMTI.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.deleteColor)))
//            }
//        }
//    }
}