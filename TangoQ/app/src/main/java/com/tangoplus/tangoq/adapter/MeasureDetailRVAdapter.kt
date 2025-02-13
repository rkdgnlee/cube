package com.tangoplus.tangoq.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvMeasureDetailItemBinding
import com.tangoplus.tangoq.function.MeasurementManager.matchedIndexs
import com.tangoplus.tangoq.mediapipe.MathHelpers.isTablet
import java.util.random.RandomGenerator.StreamableGenerator

class MeasureDetailRVAdapter(private val fragment: Fragment, private val entrys: MutableList<MutableList<Entry>>?, private val dates: List<String>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // entry는 13개 > 각 부위별 최근 측정 5개의 값이 들어간 entry들임. adapter가 연결되는 fragment에서 이를 필터링해서 넣기.

    inner class MDViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMDITitle: TextView = view.findViewById(R.id.tvMDITitle)
        val lcMDI: LineChart = view.findViewById(R.id.lcMDI)
        val tvMDIComment: TextView = view.findViewById(R.id.tvMDIComment)
    }

    override fun getItemCount(): Int {
        return entrys?.size ?: 0
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvMeasureDetailItemBinding.inflate(inflater, parent, false)
        return MDViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MDViewHolder) {
            val part = matchedIndexs[position]
            holder.tvMDITitle.text = part
            holder.tvMDIComment.text = setComment(part)

            // entry를 통해 값 넣기
            val currentItem = entrys?.get(position)
            if (currentItem != null) {
                setGraphUI(holder, currentItem)
            }
        }
    }

    private fun setComment(part: String) : String {
        val string2 = when (part) {
            "목관절" -> "거북목, 목과 머리의 쏠림을 점수로"
            "좌측 어깨" -> "좌측 어깨 주변 근육의 긴장, 상체의 쏠림을 점수로"
            "우측 어깨" -> "우측 어깨 주변 근육의 긴장, 상체의 쏠림을 점수로"
            "좌측 팔꿉" -> "좌측 상완의 긴장과 이어진 좌측 어깨 후면의 긴장을 점수로"
            "우측 팔꿉" -> "상완의 긴장과 이어진 우측 어깨 후면의 긴장을 점수로"
            "좌측 손목" -> "좌측 팔꿉에서 시작하는 손목 근육의 긴장을 점수로 "
            "우측 손목" -> "우측 팔꿉에서 시작하는 손목 근육의 긴장을 점수로 "
            "좌측 골반" -> "골반 틀어짐과 몸의 전방 쏠림을 점수로"
            "우측 골반" -> "골반 틀어짐과 몸의 전방 쏠림을 점수로"
            "좌측 무릎" -> "골반, 발목과 이어지는 좌측 무릎 정렬과 햄스트링 긴장을 점수로"
            "우측 무릎" -> "골반, 발목과 이어지는 우측 무릎 정렬과 햄스트링 긴장을 점수로"
            "좌측 발목" -> "좌측 발목의 정렬과 발 위치에 따른 하지의 밸런스를 점수로"
            "우측 발목" -> "우측 발목의 정렬과 발 위치에 따른 하지의 밸런스를 점수로"
            else -> ""
        }
        return "$string2 환산한 결과입니다. 점수변화를 확인하세요"
    }

    private fun setGraphUI(holder: MDViewHolder, entry: MutableList<Entry>) {

        val lineChart = holder.lcMDI
        val lcXAxis = lineChart.xAxis
        val lcYAxisLeft = lineChart.axisLeft
        val lcYAxisRight = lineChart.axisRight
        val lcLegend = lineChart.legend
        val reverseEntry = entry.reversed().mapIndexed { index, e -> Entry(index.toFloat(), e.y) }
        val lcLineDataSet = LineDataSet(reverseEntry, "")
        lcLineDataSet.apply {

            color = fragment.resources.getColor(R.color.subColor500, null)
            circleRadius = 4F
            lineWidth = 3F
            val textSizee = if (isTablet(fragment.requireContext())) 20f else 16f
            valueTextSize = textSizee
//            setCircleColors(fragment.resources.getColor(R.color.subColor800, null))
            circleColors = List(entry.size) { index ->
                if (index == 4) fragment.resources.getColor(R.color.thirdColor, null)
                else fragment.resources.getColor(R.color.subColor800, null)
            }
            circleRadius = 5f
            setDrawCircleHole(false)
            setDrawFilled(false)
            mode = LineDataSet.Mode.LINEAR


            setDrawValues(true)
            valueTextSize = 14f
            setValueTextColors(List(entry.size) { index ->
                if (index == 4) fragment.resources.getColor(R.color.thirdColor, null)
                else fragment.resources.getColor(R.color.subColor800, null)
            })

            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format("%.0f", value)
                }
            }
        }

        lcXAxis.apply {
            isEnabled = true
            val textSizee = if (isTablet(fragment.requireContext())) 18f else 14f
            textSize = textSizee
            textColor = fragment.resources.getColor(R.color.subColor500, null)
            setDrawAxisLine(true)
            setDrawGridLines(true)
            gridLineWidth = 1f
            gridColor = fragment.resources.getColor(R.color.subColor100, null)
            setLabelCount(entry.size, true)
            position = XAxis.XAxisPosition.BOTTOM
            axisLineWidth = 1.5f
            axisLineColor = fragment.resources.getColor(R.color.subColor200, null)
//            setAvoidFirstLastClipping(true)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index in dates.indices ) {
                        if (dates[index] != "-") {
                            dates[index].substring(2, 10).replace("-", ".")
                        } else {
                            "-"
                        }
                    } else "-"
                }
            }

        }
        lcYAxisLeft.apply {
            axisMinimum = 30f
            axisMaximum = 105f
            setDrawGridLines(false)
            setDrawAxisLine(false)
            setLabelCount(3, false)
            setDrawLabels(false)
        }
        lcYAxisRight.apply {
            setDrawGridLines(false)
            setDrawAxisLine(false)
            setLabelCount(0, false)
            setDrawLabels(false)

        }
        lcLegend.apply {
            lcLegend.formSize = 0f
        }
        lineChart.apply {
            data = LineData(lcLineDataSet)
            setTouchEnabled(false)
            isDragEnabled = true
            description.isEnabled = false
            for (set in data.dataSets) {
                if (set is LineDataSet) {
                    set.setDrawHighlightIndicators(false)
                }
            }
            viewPortHandler.setMaximumScaleX(1.1f)
            notifyDataSetChanged()
            description.text = ""
            isAutoScaleMinMaxEnabled = false
            val margin = if (isTablet(fragment.requireContext())) 80f else 40f
            extraLeftOffset = margin
            extraRightOffset = margin
            extraTopOffset = 10f
            invalidate()

        }
    }
}