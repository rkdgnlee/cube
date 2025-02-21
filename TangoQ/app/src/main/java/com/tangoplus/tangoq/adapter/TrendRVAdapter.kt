package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.vo.AnalysisVO
import com.tangoplus.tangoq.databinding.RvMeasureTrendItemBinding
import com.tangoplus.tangoq.function.MeasurementManager.matchedTripleIndexes
import com.tangoplus.tangoq.mediapipe.MathHelpers.calculateBoundedScore
import com.tangoplus.tangoq.vo.AnalysisUnitVO
import kotlin.math.abs

class TrendRVAdapter(private val fragment: Fragment,
                     private val leftAnalysises: MutableList<MutableList<AnalysisUnitVO>>?,
                     private val rightAnalysises: MutableList<MutableList<AnalysisUnitVO>>?,
                    private val filteredParts: List<String>?


) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // 그니까 정면을 누르면 -> 정면에 관련된 관절이 전부 나옴 -> 그러면 내가 adapter연결전에 이미 필터링해서 넣으면 되는 거 아님?
    // 들어오는 analysises에는 01234567까지 seq별로 있는 값들이 들어가있음. 그 seq에는 unitVO로 3개의 값들이 들어가있는 거지.

    inner class TrendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivMTI : ImageView = view.findViewById(R.id.ivMTI)
        val tvMTIPart : TextView = view.findViewById(R.id.tvMTIPart)
        val tvMTIComment: TextView = view.findViewById(R.id.tvMTIComment)
        val tvMTI1 : TextView = view.findViewById(R.id.tvMTI1)
        val tvMTIScore1: TextView = view.findViewById(R.id.tvMTIScore1)
        val tvMTI2 : TextView = view.findViewById(R.id.tvMTI2)
        val tvMTIScore2 : TextView = view.findViewById(R.id.tvMTIScore2)
        val ivMTIArrow: ImageView = view.findViewById(R.id.ivMTIArrow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvMeasureTrendItemBinding.inflate(inflater, parent, false)
        return TrendViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return rightAnalysises?.size ?: 0
    }

    @SuppressLint("DiscouragedApi")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TrendViewHolder) {
            holder.tvMTIPart.text = filteredParts?.get(position) ?: ""
            if (leftAnalysises.isNullOrEmpty() && !rightAnalysises.isNullOrEmpty()) {
                val analysisUnits = rightAnalysises[position]
                // 점수 넣기
                val score = calculatePercent(analysisUnits)
                holder.tvMTIScore2.text = "${String.format("%.1f 점", score)}"

                // 설명 넣기
                val collectedComment = createSummary(analysisUnits) + createAdvise(filteredParts?.get(position), score.toInt(), 2)
                holder.tvMTIComment.text = collectedComment

            } else if (!leftAnalysises.isNullOrEmpty() && !rightAnalysises.isNullOrEmpty()) {
                val leftAnalysisUnits = leftAnalysises[position]
                val rightAnalysisUnits = rightAnalysises[position]
//                Log.v("왼쪽", "$leftAnalysisUnits")
                Log.v("오른쪽", "${rightAnalysisUnits.map { it.columnName }}")
                Log.v("오른쪽", "${rightAnalysisUnits.map { it.rawData }}")
                val leftScore = calculatePercent(leftAnalysisUnits)
                val rightScore = calculatePercent(rightAnalysisUnits)
                val state = if (leftScore == rightScore) {
                    2
                } else if (leftScore > rightScore){
                    1
                } else if (rightScore > leftScore) {
                    3
                } else {
                    0
                }
                holder.tvMTIScore1.text = "${String.format("%.1f 점", leftScore)}"
                holder.tvMTIScore2.text = "${String.format("%.1f 점", rightScore)}"

                val collectedComment = createSummary(rightAnalysisUnits) + createAdvise(filteredParts?.get(position), rightScore.toInt(), state)
                holder.tvMTIComment.text = collectedComment

                // 코멘트가 만들어진 후 코멘트와 같이 점수 state 설정
                setStateFlavor(holder, state)

            }

            // image
            val drawableId = when (filteredParts?.get(position)) {
                "목관절" -> R.drawable.drawable_joint_filled_1
                "우측 어깨", "좌측 어깨" -> R.drawable.drawable_joint_filled_2
                "우측 팔꿉", "좌측 팔꿉" -> R.drawable.drawable_joint_filled_3
                "우측 손목", "좌측 손목" -> R.drawable.drawable_joint_filled_4
                "우측 골반", "좌측 골반" -> R.drawable.drawable_joint_filled_5
                "우측 무릎", "좌측 무릎" -> R.drawable.drawable_joint_filled_6
                "우측 발목", "좌측 발목" -> R.drawable.drawable_joint_filled_7
                else -> -1
            }
            val isRight = if (filteredParts?.get(position)?.contains("우측") == true) true else false
            if (drawableId != -1) {
                loadFlippedImage(holder, drawableId, isRight)
            }
        }
    }
    private fun setStateFlavor(holder: TrendViewHolder, state: Int) {
        val startIndex = holder.tvMTIComment.text.indexOfLast { it == '\n' }
        val spannableString = SpannableString.valueOf(holder.tvMTIComment.text.toString())
        when (state) {
            1 -> {
                holder.tvMTI2.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.deleteColor))
                holder.tvMTIScore2.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.deleteColor))
                holder.ivMTIArrow.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_arrow_board))
                holder.ivMTIArrow.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.deleteColor))
                holder.ivMTIArrow.scaleY = 1f
                spannableString.setSpan(ForegroundColorSpan(ContextCompat.getColor(fragment.requireContext(), R.color.deleteColor)), startIndex + 1, spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            }
            2 -> {
                holder.tvMTI2.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.subColor800))
                holder.tvMTIScore2.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.subColor800))
                holder.ivMTIArrow.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_no_change))
                holder.ivMTIArrow.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.subColor800))
                spannableString.setSpan(ForegroundColorSpan(ContextCompat.getColor(fragment.requireContext(), R.color.subColor800)), startIndex + 1, spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            3 -> {
                holder.tvMTI2.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.thirdColor))
                holder.tvMTIScore2.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.thirdColor))
                holder.ivMTIArrow.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_arrow_board))
                holder.ivMTIArrow.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.thirdColor))
                holder.ivMTIArrow.scaleY = -1f
                spannableString.setSpan(ForegroundColorSpan(ContextCompat.getColor(fragment.requireContext(), R.color.thirdColor)), startIndex + 1, spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            }
        }
        holder.tvMTIComment.text = spannableString
    }
//    private fun createAnalysises(analysis: MutableList<MutableList<AnalysisVO>>, position: Int) : MutableList<List<AnalysisUnitVO>> {
//        val processedData = mutableListOf<List<AnalysisUnitVO>>()
//        matchedTripleIndexes.forEach { tripleList ->
//            val analysisUnits = mutableListOf<AnalysisUnitVO>()
//
//            tripleList.forEach { (seq, index, unitIndex) ->
//                // seq로 rightAnalysises에서 해당 리스트를 가져옴
//                val analysisList = analysis.getOrNull(position)
//                // index로 해당 AnalysisVO를 가져옴
//                val analysisVO = analysisList?.getOrNull(index)
//                // unitIndex로 AnalysisUnitVO를 가져옴
//                val analysisUnitVO = analysisVO?.labels?.getOrNull(unitIndex)
////                        Log.v("analysisUniVO", "${analysisUnitVO?.rawDataName}")
//                // null이 아니면 리스트에 추가
//                if (analysisUnitVO != null) {
//                    analysisUnits.add(analysisUnitVO)
//                }
//            }
//
//            // 각 관절마다 추출한 AnalysisUnitVO 리스트를 최종 데이터에 추가
//            processedData.add(analysisUnits)
//        }
//        return processedData
//    }

    private fun calculatePercent(processedData: MutableList<AnalysisUnitVO>) : Float {
        val calculatePercent = processedData.map { calculateBoundedScore(it.columnName, abs(it.rawData), it.rawDataBound)}.map { if (it <= 50f) 50f else it }
        return calculatePercent.average().toFloat()
    }


    private fun loadFlippedImage(holder: TrendViewHolder, resourceId: Int, isRight: Boolean) {
        holder.ivMTI.setImageResource(resourceId)
        if (!isRight) {
            holder.ivMTI.scaleX = -1f
        }
    }
    private fun createSummary(units: List<AnalysisUnitVO>) : String {
        val rawDataNames = units.map { it.rawDataName }
//        val bouncedScores = units.map { calculateBoundedScore(it.columnName, abs(it.rawData), it.rawDataBound) }
        val rawData = units.map { it.rawData }
        val bounds = units.map { it.rawDataBound }

        // 3가지의 데이터 값 합치기 Triple<칼럼명, 백분위값, 경계범위(Triple)>
        val zipDatas = rawDataNames.zip(rawData) { s, f -> Pair(s, f) }.zip(bounds) { (s, f), t -> Triple(s, f, t)}
        val combineStrings = zipDatas.joinToString { "${it.first}(${String.format("%.1f${if (it.first.contains("거리")) "cm" else "°"}", it.second)})\n" }.replace(", ", "")
        val resultString = "각 데이터를 백분위로 산출한 점수입니다.\n${combineStrings}"
        return resultString
    }
    private fun createAdvise(part: String?, score: Int, state: Int) : String {
        val string1 = when (part) {
            "목관절" -> {
                "거북목과 목관절 불균형"
            }
            "좌측 어깨", "우측 어깨" -> {
                "어깨 불균형과 몸의 좌우 쏠림"
            }
            "좌측 팔꿉", "우측 팔꿉" -> {
                "상완과 어깨 근육의 긴장"
            }
            "좌측 손목", "우측 손목" -> {
                "하완과 손목 자세 불균형"
            }
            "좌측 골반", "우측 골반" -> {
                "골반 불균형으로 인한 좌우 쏠림"
            }
            "좌측 무릎", "우측 무릎" -> {
                "하지 부정렬과 발목, 골반의 긴장"
            }
            "좌측 발목", "우측 발목" -> {
                "발의 불균형, 밸런스 무너짐"
            }
            else -> {
                ""
            }
        }

        val string2 = when (state) {
            1 -> {
                when (score) {
                    in 50 .. 65 -> "의 컨디션이 안좋을 수록 주변 근육과 관절에 긴장과 불편함이 올 수 있습니다. ${if (part in listOf("좌측 골반", "우측 골반", "좌측 무릎", "우측 무릎")) "프로그램 진행을 통해 밸런스를 맞춰보세요" else "더욱 꾸준히 프로그램을 진행 해 주세요"}"
                    in 66 .. 80 -> "의 상태가 안좋아질 수 있습니다. 정확한 자세로 프로그램을 다시 진행해주세요"
                    else -> "의 좋은 컨디션을 프로그램을 통해 유지하세요"
                }
            }
            2 -> {
                when (score) {
                    in 50 .. 65 -> "을 위한 프로그램 동안 정확한 자세를 재조명해서 프로그램을 반복하세요"
                    in 66 .. 80 -> "을 위해 강화 운동 프로그램 진행을 추천드립니다."
                    else -> "의 좋은 컨디션을 유지중입니다. 꾸준히 진행해서 일상의 운동 능력을 유지시키세요"
                }

            }
            3 -> {
                when (score) {
                    in 50 .. 65 -> "의 밸런스와 균형에 회복이 있습니다. 정확한 자세에 주목해서 프로그램을 다시 진행해보세요"
                    in 66 .. 80 -> if (part in listOf("좌측 골반", "우측 골반", "좌측 무릎", "우측 무릎")) "의 밸런스를 프로그램을 통해 유지하세요" else "의 꾸준한 운동을 유지해 주세요"
                    else ->  "운동 프로그램을 꾸준히 반복해서 일상 능력을 키워보세요 "
                }
            }
            else -> ""
        }
        return string1+string2
    }

}