package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.progressview.ProgressView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.vo.AnalysisVO
import com.tangoplus.tangoq.databinding.RvMeasureTrendItemBinding
import com.tangoplus.tangoq.function.MeasurementManager.matchedTripleIndexes
import com.tangoplus.tangoq.function.MeasurementManager.setLabels
import com.tangoplus.tangoq.mediapipe.MathHelpers.calculateBoundedScore
import kotlin.math.abs

class TrendRVAdapter(private val fragment: Fragment,
                     private val analyzes1: MutableList<MutableList<AnalysisVO>>?,
                     private val analyzes2: MutableList<MutableList<AnalysisVO>>?,
                     private val filteredIndexes: List<Int>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    inner class TrendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMTIPart : TextView = view.findViewById(R.id.tvMTIPart)
        val ivMTI : ImageView = view.findViewById(R.id.ivMTI)
        private val clMTI1 : ConstraintLayout = view.findViewById(R.id.clMTI1)
        private val clMTI2 : ConstraintLayout = view.findViewById(R.id.clMTI2)
        private val clMTI3 : ConstraintLayout = view.findViewById(R.id.clMTI3)
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
        val cls = listOf(clMTI1, clMTI2, clMTI3)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvMeasureTrendItemBinding.inflate(inflater, parent, false)
        return TrendViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return filteredIndexes.size
    }

    @SuppressLint("DiscouragedApi")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TrendViewHolder) {
            if (analyzes2?.isNotEmpty() == true) {
                // 필터링된 index list가 있을 때 넣기
                // 필터링 index를 가져와서 currentItemRight를 접근
                val index = filteredIndexes[position]
                val currentItemRight = analyzes2[index] // analysisVO가 들어가있는 부위 별임.
                val selectAnalysisRight = matchedTripleIndexes[index]

                holder.tvMTIPart.text = matchedParts[index]
                val jointIndex = if (index == 0) 1 else (index + 1) / 2 + 1

                // ------# 부위 drawable 좌우 반전 #------
                val resourceName = "@drawable/icon_part$jointIndex"
                val resourceId = holder.ivMTI.context.resources.getIdentifier(
                    resourceName, "drawable", holder.ivMTI.context.packageName
                )

                holder.ivMTI.setImageResource(resourceId)
                // 2~7에 해당하는 홀수 번째 항목일 때만 좌우 반전 적용
                if (index != 0 && index % 2 != 0) {
                    holder.ivMTI.scaleX = -1f // 좌우 반전
                } else {
                    holder.ivMTI.scaleX = 1f // 원래 상태
                }

                // -------# 부위 선택 #------
                val normalCount = selectAnalysisRight.count { indexTriple ->
                    currentItemRight[indexTriple.second].labels[indexTriple.third].state == 0 || currentItemRight[indexTriple.second].labels[indexTriple.third].state == 1
                }
                if (normalCount >= 2) { // 3개 중 2개 이상이 normal이면 true
                    setPartState(holder, 1)
                } else if (normalCount == 1) {
                    setPartState(holder, 2)
                } else {
                    setPartState(holder, 3)
                }
                // 왼쪽 설정 안됐을 때 우측 비교
                selectAnalysisRight.forEachIndexed { index, indexTriple -> // 3, 5, 5 //  0, 0, 1
                    holder.tvSeqs[index].text = setSeqString(indexTriple.first)
                    val unit = currentItemRight[indexTriple.second].labels[indexTriple.third]
                    val score = calculateBoundedScore(abs(unit.rawData), unit.rawDataBound)
                    holder.pvs[index][1].progress = score
                    val title = unit.rawDataName
                        .replace("양", "")
                        .replace("과", "")
                        .replace("와", "")
                        .replace("에서", "-")
                        .replace("좌측 ", "")
                        .replace("우측 ", "")
                    holder.tvPoses[index].text = title

                    setState(holder, index, splitState(score))
                    // 오른쪽 balloon comment init
                    val rightRawData = String.format("%.2f", unit.rawData) + if (unit.columnName.contains("angle")) "°" else "cm"
                    val rightComment = "우측: $rightRawData\n$title\n${setLabels(unit)}"
                    setBalloon(holder.cls[index], index, rightComment)
                }

                // 좌측 비교
                if (analyzes1 != null) {
                    val currentItemLeft = analyzes1[index] // analysisVO가 들어가있는 부위 별임.
                    val selectAnalysisLeft = matchedTripleIndexes[index]
                    selectAnalysisRight.zip(selectAnalysisLeft).forEachIndexed { indexIn, (rightTriple, leftTriple) ->
                        // 오른쪽
                        val rightUnit = currentItemRight[rightTriple.second].labels[rightTriple.third]
                        val rightScore = calculateBoundedScore(rightUnit.rawData, rightUnit.rawDataBound)
                        holder.pvs[indexIn][1].progress = rightScore
                        holder.tvPoses[indexIn].text = rightUnit.rawDataName
                            .replace("양", "")
                            .replace("과", "")
                            .replace("와", "")
                            .replace("에서", "-")
                            .replace("좌측 ", "")
                            .replace("우측 ", "")

                        setState(holder, indexIn, splitState(rightScore))

                        // 왼쪽
                        val leftUnit = currentItemLeft[leftTriple.second].labels[leftTriple.third]
                        val leftScore = calculateBoundedScore(leftUnit.rawData, leftUnit.rawDataBound)
                        holder.pvs[indexIn][0].progress = leftScore

                        // balloon에 통합 comment 넣기
                        val leftRawData = String.format("%.2f", leftUnit.rawData) + if (leftUnit.columnName.contains("angle")) "°" else "cm"
                        val leftComment = "좌측: $leftRawData"

                        // 오른쪽 balloon comment init
                        val rightRawData = String.format("%.2f", rightUnit.rawData) + if (rightUnit.columnName.contains("angle")) "°" else "cm"
                        val rightComment = "우측: $rightRawData\n${setLabels(rightUnit)}"
                        setBalloon(holder.cls[indexIn], indexIn, "$leftComment $rightComment")
                    }
                }
            }
        }
    }

    private val matchedParts = listOf(
        "목관절" , "좌측 어깨", "우측 어깨", "좌측 팔꿉", "우측 팔꿉", "좌측 손목" , "우측 손목" , "좌측 골반", "우측 골반" , "좌측 무릎" , "우측 무릎" , "좌측 발목", "우측 발목")

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

    private fun setPartState(holder: TrendViewHolder, state: Int) {
        when (state) {
            1 -> { holder.cvMTI.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.subColor500))) }
            2 -> { holder.cvMTI.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.cautionColor))) }
            3 -> { holder.cvMTI.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.deleteColor))) }
        }
    }

    private fun setBalloon(cl: ConstraintLayout, index: Int, comment: String) {
        val balloon = Balloon.Builder(fragment.requireContext())
            .setWidthRatio(0.64f)
            .setHeight(BalloonSizeSpec.WRAP)
            .setText(comment)
            .setTextColorResource(R.color.subColor800)
            .setTextSize(14f)
            .setTextLineSpacing(5f)
            .setArrowPositionRules(ArrowPositionRules.ALIGN_BALLOON)
            .setArrowSize(0)
            .setMargin(4)
            .setPadding(12)
            .setCornerRadius(12f)
            .setBackgroundColorResource(R.color.subColor100)
            .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
            .setLifecycleOwner(fragment.viewLifecycleOwner)
            .build()

        cl.setOnClickListener {
            when (index) {
                0 -> balloon.showAlignEnd(cl)
                1 -> balloon.showAlignBottom(cl)
                2 -> balloon.showAlignStart(cl)
            }
        }
    }

}