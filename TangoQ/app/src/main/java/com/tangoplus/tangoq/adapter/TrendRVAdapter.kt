package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Index
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.vo.AnalysisVO
import com.tangoplus.tangoq.databinding.RvMeasureTrendItemBinding
import com.tangoplus.tangoq.function.MeasurementManager.matchedIndexs
import com.tangoplus.tangoq.function.MeasurementManager.matchedTripleIndexes
import com.tangoplus.tangoq.mediapipe.MathHelpers.calculateBoundedScore
import com.tangoplus.tangoq.vo.AnalysisUnitVO
import java.text.Format
import kotlin.math.abs
import kotlin.math.roundToInt

class TrendRVAdapter(private val fragment: Fragment,
                     private val leftAnalysises: MutableList<MutableList<AnalysisVO>>?,
                     private val rightAnalysises: MutableList<MutableList<AnalysisVO>>?,
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

                val analysisUnits = createAnalysises(rightAnalysises, position)
                // 점수 넣기
                val score = calculatePercent(analysisUnits, position)
                holder.tvMTIScore2.text = "$score 점"



            } else if (!leftAnalysises.isNullOrEmpty() && !rightAnalysises.isNullOrEmpty()) {
                val leftAnalysisUnits = createAnalysises(leftAnalysises, position)
                val rightAnalysisUnits = createAnalysises(rightAnalysises, position)
                val leftScore = calculatePercent(leftAnalysisUnits, position)
                val rightScore = calculatePercent(rightAnalysisUnits, position)
                val state = if (leftScore == rightScore) {
                    2
                } else if (leftScore > rightScore){
                    1
                } else if (rightScore > leftScore) {
                    3
                } else {
                    0
                }
                setStateFlavor(holder, state)

                holder.tvMTIScore1.text = "${String.format("%.1f 점", leftScore)}"
                holder.tvMTIScore2.text = "${String.format("%.1f 점", rightScore)}"

                // 설명 넣기
                val collectedComment = "${createSummary(rightAnalysisUnits[position])}\n" +
                        createAdvise(filteredParts?.get(position), state)

                holder.tvMTIComment.text = collectedComment


            }
        }
    }
    private fun setStateFlavor(holder: TrendViewHolder, state: Int) {
        when (state) {
            1 -> {
                holder.tvMTI2.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.deleteColor))
                holder.tvMTIScore2.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.deleteColor))
                holder.ivMTIArrow.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_arrow_board))
                holder.ivMTIArrow.scaleY = 1f
            }
            2 -> {
                holder.tvMTI2.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.subColor800))
                holder.tvMTIScore2.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.subColor800))
                holder.ivMTIArrow.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_no_change))
            }
            3 -> {
                holder.tvMTI2.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.thirdColor))
                holder.tvMTIScore2.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.thirdColor))
                holder.ivMTIArrow.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_arrow_board))
                holder.ivMTIArrow.scaleY = -1f
            }
        }
    }
    private fun createAnalysises(analysis: MutableList<MutableList<AnalysisVO>>, position: Int) : MutableList<List<AnalysisUnitVO>> {
        val processedData = mutableListOf<List<AnalysisUnitVO>>()
        matchedTripleIndexes.forEach { tripleList ->
            val analysisUnits = mutableListOf<AnalysisUnitVO>()

            tripleList.forEach { (seq, index, unitIndex) ->
                // seq로 rightAnalysises에서 해당 리스트를 가져옴
                val analysisList = analysis.getOrNull(position)
                // index로 해당 AnalysisVO를 가져옴
                val analysisVO = analysisList?.getOrNull(index)
                // unitIndex로 AnalysisUnitVO를 가져옴
                val analysisUnitVO = analysisVO?.labels?.getOrNull(unitIndex)
//                        Log.v("analysisUniVO", "${analysisUnitVO?.rawDataName}")
                // null이 아니면 리스트에 추가
                if (analysisUnitVO != null) {
                    analysisUnits.add(analysisUnitVO)
                }
            }

            // 각 관절마다 추출한 AnalysisUnitVO 리스트를 최종 데이터에 추가
            processedData.add(analysisUnits)
        }
        return processedData
    }

    private fun calculatePercent(processedData: MutableList<List<AnalysisUnitVO>>, position: Int) : Float {
        val calculatePercent = processedData[position].map { calculateBoundedScore(abs(it.rawData), it.rawDataBound)}.map { if (it <= 50f) 50f else it }
        return calculatePercent.average().toFloat()

    }

    private fun createSummary(units: List<AnalysisUnitVO>) : String {
        val rawDataNames = units.map { it.rawDataName }
        val bouncedScores = units.map { calculateBoundedScore(abs(it.rawData), it.rawDataBound) }
        val combineStrings = rawDataNames.zip(bouncedScores).joinToString { "${it.first}(${it.second})" }
        val resultString = "각 자세별 데이터에서 ${combineStrings}를 종합하여 산출한 점수입니다. 값은 과거 기록과 비교를 위해 간단히 환산한 결과이며, 참고용으로 활용하시기 바랍니다."
        return resultString
    }
    private fun createAdvise(part: String?, state: Int) : String {
        val string1 = when (part) {
            "목관절" -> {
                "목에 대한 "
            }
            "좌측 어깨", "우측 어깨" -> {
                ""
            }
            "좌측 팔꿉", "우측 팔꿉" -> {
                ""
            }
            "좌측 손목", "우측 손목" -> {
                ""
            }
            "좌측 골반", "우측 골반" -> {
                ""
            }
            "좌측 무릎", "우측 무릎" -> {
                ""
            }
            "좌측 발목", "우측 발목" -> {
                ""
            }
            else -> {
                ""
            }
        }
        val string2 = when (state) {
            1 -> {
                ""
            }
            2 -> {
                ""
            }
            3 -> {
                ""
            }
            else -> ""
        }
        return string1+string2
    }

}