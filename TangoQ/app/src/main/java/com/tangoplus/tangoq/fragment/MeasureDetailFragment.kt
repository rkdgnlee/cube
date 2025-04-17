package com.tangoplus.tangoq.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.MainPartRVAdapter
import com.tangoplus.tangoq.adapter.MeasureDetailRVAdapter
import com.tangoplus.tangoq.vo.MeasureVO
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureDetailBinding
import com.tangoplus.tangoq.db.Singleton_t_measure
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.function.MeasurementManager.getAnalysisUnits
import com.tangoplus.tangoq.function.MeasurementManager.matchedIndexs
import com.tangoplus.tangoq.function.MeasurementManager.matchedUris
import com.tangoplus.tangoq.listener.OnCategoryClickListener
import com.tangoplus.tangoq.vision.MathHelpers.isTablet
import com.tangoplus.tangoq.viewmodel.AnalysisViewModel
import com.tangoplus.tangoq.vo.AnalysisUnitVO
import com.tangoplus.tangoq.vo.AnalysisVO
import org.json.JSONArray


class MeasureDetailFragment : Fragment(), OnCategoryClickListener {
    private lateinit var binding : FragmentMeasureDetailBinding
    private var singletonMeasure : MutableList<MeasureVO>? = null
    private var measure : MeasureVO? = null
    private val mvm : MeasureViewModel by activityViewModels()
    private val avm : AnalysisViewModel by activityViewModels()
    private lateinit var adapterAnalysises : List<AnalysisVO>
    private lateinit var partStates : MutableList<Pair<String, Float>>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasureDetailBinding.inflate(inflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        singletonMeasure = Singleton_t_measure.getInstance(requireContext()).measures
        val showMeasure = arguments?.getBoolean("showMeasure", false) ?: false
        if (showMeasure) {
            val balloonText = "측정 결과를 확인해보세요\n실제 자세도 함께 볼 수 있습니다."
            val balloonlc1 = Balloon.Builder(requireContext())
                .setWidthRatio(0.5f)
                .setHeight(BalloonSizeSpec.WRAP)
                .setText(balloonText)
                .setTextColorResource(R.color.white)
                .setTextSize(15f)
                .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                .setArrowSize(0)
                .setMargin(10)
                .setPadding(12)
                .setCornerRadius(8f)
                .setBackgroundColorResource(R.color.mainColor)
                .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
                .setLifecycleOwner(viewLifecycleOwner)
                .build()
            balloonlc1.showAlignBottom(binding.tvMDParts)
            arguments?.putBoolean("showMeasure", false)
        }

        binding.ibtnMDAlarm.setOnSingleClickListener {
            val dialog = AlarmDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "AlarmDialogFragment")
        }
        binding.ibtnMDBack.setOnSingleClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
                replace(R.id.flMain, MeasureHistoryFragment())
                commit()
            }
        }
        // ------# measure 에 맞게 UI 수정 #------
        measure = mvm.selectedMeasure
        avm.mdMeasureResult = measure?.measureResult?.optJSONArray(1) ?: JSONArray()
        updateUI()
        setHorizonAdapter()
        // ------# 10각형 레이더 차트 #------

        val bodyParts = listOf("목관절", "우측 어깨", "좌측 어깨", "우측 팔꿉", "좌측 팔꿉","우측 손목","좌측 손목", "우측 골반","좌측 골반", "우측 무릎","좌측 무릎","우측 발목", "좌측 발목")
        val indices = listOf(0, 1, 3, 5, 7, 9, 11, 12, 10, 8, 6, 4, 2)

        // 먼저 모든 점수를 95로 초기화
        val scores = MutableList(bodyParts.size) { 96f }
//        Log.v("raderScores", "${ measure?.dangerParts}")
        // measure.dangerParts에 있는 부위들의 점수만 업데이트
        measure?.dangerParts?.forEach { (part, danger) ->
            val index = bodyParts.indexOf(part)
            if (index != -1) {
                scores[index] = when (danger) {
                    1.0f -> 80f
                    2.0f -> 70f
                    3.0f -> 60f
                    else -> 96f
                }
            }
        }

        // indices를 사용하여 올바른 순서로 raderScores 생성
        val raderScores = indices.map { scores[it] }
        val raderXParts = indices.map { bodyParts[it] }
//        Log.v("raderScores", raderScores.toString())

        val entries = mutableListOf<RadarEntry>()
        for (i in bodyParts.indices) {
            entries.add(RadarEntry(raderScores[i]))
        }
//        Log.v("재조정x축", "$raderXParts")
        val dataSet = RadarDataSet(entries, "신체 부위").apply {
            color = resources.getColor(R.color.mainColor, null)
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.gradient_rader_main_second_90)
            lineWidth = 2f
            setDrawValues(false)  // 값 표시 제거

        }
        val radarData = RadarData(dataSet)

        binding.rcMD.apply {

            data = radarData
            description.isEnabled = false
            legend.isEnabled = false
            webLineWidth = 6f
            webColor = resources.getColor(R.color.white, null)
            webAlpha = 100
            webLineWidthInner = 1f
            setOnChartValueSelectedListener(object  : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    Log.v("값 선택", "값 선택")
                }
                override fun onNothingSelected() {}
            })
            xAxis.apply {
                setDrawGridLines(true)
                valueFormatter = IndexAxisValueFormatter(raderXParts)
                textColor = resources.getColor(R.color.subColor400, null)
                textSize = if (isTablet(requireContext())) 20f else 13f   // 텍스트 크기 증가
                yOffset = 40f

            }
            yAxis.apply {
                setLabelCount(4, true)
                setDrawGridLines(false)
                setDrawLabels(false)
                setDrawTopYLabelEntry(false)
                setTouchEnabled(false)
                data.setDrawValues(false)
                setDrawWeb(true)
                axisMaximum = 100f
                axisMinimum = 0f

            }
            legend.apply {
                xOffset = 40f
                yOffset = 40f
            }
            invalidate() // 차트 갱신
        }
//        val currentMeasureIndex = singletonMeasure?.indexOf(mvm.selectedMeasure)
//        setAdapter(currentMeasureIndex ?: 0)
        Log.v("파트observe", "${avm.currentPart.value}, ${measure?.measureResult}")
        avm.currentPart.observe(viewLifecycleOwner) { part ->
            if (!part.isNullOrEmpty() && measure?.measureResult != JSONArray() ) { //
                setAdapter(part)
                // 초기 상태
                avm.currentIndex = matchedIndexs.indexOf(part)
                Log.v("파트observe", "$part, ${avm.currentPart.value}")
            }
        }
    }
    private fun setAdapter(part: String) {
//        val painPart = avm.currentParts?.find { it == part }
        val seqs = matchedUris[part]
        val groupedAnalyses = mutableMapOf<Int, MutableList<MutableList<AnalysisUnitVO>>>()

        seqs?.forEach { seqIndex ->
            val analyses = getAnalysisUnits(requireContext(), part, seqIndex, measure?.measureResult ?: JSONArray())
            if (!groupedAnalyses.containsKey(seqIndex)) {
                groupedAnalyses[seqIndex] = mutableListOf()
            }
            groupedAnalyses[seqIndex]?.add(analyses)

        }

        adapterAnalysises = groupedAnalyses.map { (indexx, analysesList) ->
            AnalysisVO(
                indexx = indexx,
                labels = analysesList.flatten().toMutableList(),
                url = measure?.fileUris?.get(indexx) ?: ""
            )
        }.sortedBy { it.indexx }

//        Log.v("변환analysis", "${adapterAnalysises.map { it.labels.size }}, ${ adapterAnalysises.map { it } }")
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        val adapter = MeasureDetailRVAdapter(this@MeasureDetailFragment, adapterAnalysises, avm) // avm.currentIndex가 2인데 adapterAnalysises에는 0, 5밖에없어서 indexOutOfBoundException이 나옴.
        binding.rvMD.layoutManager = layoutManager
        binding.rvMD.adapter = adapter
    }

    private fun setHorizonAdapter() {
        partStates = matchedIndexs.map { part ->
            measure?.dangerParts?.find { it.first == part }  ?: (part to 0f)
        }. toMutableList().apply {
            val zeroItem = filter { it.second == 0f }
            removeAll(zeroItem)
            addAll(zeroItem)
        }
        if (avm.mdMeasureResult != JSONArray()) {

            avm.currentPart.value = partStates.get(0).first
        }
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val adapter = MainPartRVAdapter(this@MeasureDetailFragment, partStates, avm, "measureDetail")
        adapter.onCategoryClickListener = this
        binding.rvMDHorizon.layoutManager = layoutManager
        binding.rvMDHorizon.adapter=  adapter

    }

    private fun updateUI() {
        binding.tvMDScore.text = measure?.overall.toString()
        binding.tvMDDate.text = "${measure?.regDate?.substring(0, 10)}" // ${measure?.userName}
//        binding.tvMDParts.text = "우려부위: ${measure?.dangerParts?.map { it.first }?.joinToString(", ")}"
    }

    override fun onCategoryClick(category: String) {
        avm.currentPart.value = category
        Log.v("커런트파트변경", "$category, ${avm.currentPart.value}")
    }
}