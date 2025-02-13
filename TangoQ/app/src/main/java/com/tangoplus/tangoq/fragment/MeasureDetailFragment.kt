package com.tangoplus.tangoq.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.gms.common.util.DeviceProperties.isTablet
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.MeasureDetailRVAdapter
import com.tangoplus.tangoq.vo.MeasureVO
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureDetailBinding
import com.tangoplus.tangoq.db.Singleton_t_measure
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.function.MeasurementManager.getAnalysisUnits
import com.tangoplus.tangoq.function.MeasurementManager.matchedIndexs
import com.tangoplus.tangoq.function.MeasurementManager.matchedTripleIndexes
import com.tangoplus.tangoq.function.MeasurementManager.matchedUris
import com.tangoplus.tangoq.mediapipe.MathHelpers.calculateBoundedScore
import com.tangoplus.tangoq.vo.AnalysisUnitVO
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs


class MeasureDetailFragment : Fragment() {
    private lateinit var binding : FragmentMeasureDetailBinding
    private var singletonMeasure : MutableList<MeasureVO>? = null
    private var measure : MeasureVO? = null
    private val mvm : MeasureViewModel by activityViewModels()
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
            balloonlc1.showAlignBottom(binding.view8)
            arguments?.putBoolean("showMeasure", false)
        }

        binding.ibtnMDAlarm.setOnClickListener {
            val dialog = AlarmDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "AlarmDialogFragment")
        }
        binding.ibtnMDBack.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
                replace(R.id.flMain, MeasureHistoryFragment())
                commit()
            }
        }
        // ------# measure 에 맞게 UI 수정 #------
        measure = mvm.selectedMeasure
        updateUI()

        // ------# 10각형 레이더 차트 #------

        val bodyParts = listOf("목관절", "우측 어깨", "좌측 어깨", "우측 팔꿉", "좌측 팔꿉","우측 손목","좌측 손목", "우측 골반","좌측 골반", "우측 무릎","좌측 무릎","우측 발목", "좌측 발목")
        val indices = listOf(0, 1, 3, 5, 7, 9, 11, 12, 10, 8, 6, 4, 2)

        // 먼저 모든 점수를 95로 초기화
        val scores = MutableList(bodyParts.size) { 96f }
        Log.v("raderScores", "${ measure?.dangerParts}")
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
        Log.v("raderScores", raderScores.toString())

        val entries = mutableListOf<RadarEntry>()
        for (i in bodyParts.indices) {
            entries.add(RadarEntry(raderScores[i]))
        }
        Log.v("재조정x축", "$raderXParts")
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
        val currentMeasureIndex = singletonMeasure?.indexOf(mvm.selectedMeasure)
        setAdapter(currentMeasureIndex ?: 0)

        binding.btnMDShare.setOnClickListener {
            // ------! 그래프 캡처 시작 !------
            val bitmap = Bitmap.createBitmap(binding.rcMD.width, binding.rcMD.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            binding.rcMD.draw(canvas)

            val file = File(context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "shared_image.jpg")
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()

            val fileUri = FileProvider.getUriForFile(requireContext(), context?.packageName + ".provider", file)
            // ------! 그래프 캡처 끝 !------
            val url = Uri.parse("https://tangopluscompany.github.io/deep-link-redirect/#/3")
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/png" // 이곳에서 공유 데이터 변경
            intent.putExtra(Intent.EXTRA_STREAM, fileUri)
            intent.putExtra(Intent.EXTRA_TEXT, "제 밸런스 그래프를 공유하고 싶어요 !\n$url")
            startActivity(Intent.createChooser(intent, "밸런스 그래프"))
        }
    }

    private fun setAdapter(currentMeasureIndex: Int) {
        val singletonSize = singletonMeasure?.size
        val dates = mutableListOf<String>()
        if (singletonSize != null) {
            when (singletonSize) {
                in 0 .. 4 -> {

                    for (i in 0 until singletonSize) {
                        dates.add(singletonMeasure?.get(currentMeasureIndex + i)?.regDate.toString())
                        val result = mutableListOf<MutableList<Float>>()  // 최종 결과 리스트 (13개의 부위별 5개 score)

                        matchedIndexs.forEachIndexed { partIndex, part ->
                            val partScores = mutableListOf<Float>()  // 현재 부위의 5개 measure에 대한 score 리스트

                            // 각 measureIndex(0~4)에 대해 반복
                            for (measureIndex in 0 until singletonSize) {
                                val seqList = matchedUris[part] ?: emptyList()  // 현재 부위에 해당하는 seq 리스트 가져오기
                                // TODO 현재 지난 기록의 singleton이 없기 때문에? 안나오는 것 뿐이었다.
                                val allAnalysisUnits = seqList.mapNotNull { seq ->
                                    singletonMeasure?.get(currentMeasureIndex + measureIndex)?.measureResult?.let {
                                        getAnalysisUnits(requireContext(), part, seq, it)
                                    }
                                }.flatten()  // 각 seq에서 가져온 AnalysisUnitVO를 모두 하나의 리스트로 합침
                                val triples = matchedTripleIndexes[partIndex]

                                // triples로 AnalysisUnitVO 3개 추출
                                val processedData = triples.mapNotNull { triple ->
                                    allAnalysisUnits.getOrNull(triple.first)
                                }.toMutableList()

                                // 3개의 AnalysisUnitVO로 하나의 score 계산
                                val score = if (processedData.size == 3) {
                                    calculatePercent(processedData)  // Float 값을 반환
                                } else {
                                    0f  // 데이터 부족 시 0으로 처리
                                }
                                partScores.add(score)
                            }
                            result.add(partScores)
                        }
                        val entries = convertEntries(result)
                        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                        binding.rvMD.layoutManager = layoutManager
                        val adapter = MeasureDetailRVAdapter(this@MeasureDetailFragment, entries, dates.reversed())
                        binding.rvMD.adapter = adapter
                    }
                    for (i in singletonSize - 1 downTo  1) {
                        mvm.recentAnalysisUnits.add( mutableListOf() )
                        dates.add("-")
                    }
                }
                else -> {
                    val result = mutableListOf<MutableList<Float>>()  // 최종 결과 리스트 (13개의 부위별 5개 score)

                    matchedIndexs.forEachIndexed { partIndex, part ->
                        val partScores = mutableListOf<Float>()  // 현재 부위의 5개 measure에 대한 score 리스트

                        // 각 measureIndex(0~4)에 대해 반복
                        for (measureIndex in 0 until 5) {
                            if (dates.size < 5) {
                                if (currentMeasureIndex + measureIndex < singletonMeasure?.size!!) {
                                    dates.add(singletonMeasure?.get(currentMeasureIndex + measureIndex)?.regDate.toString())
                                } else {
                                    dates.add("-")
                                }

                            }
                            val seqList = matchedUris[part] ?: emptyList()  // 현재 부위에 해당하는 seq 리스트 가져오기

                            // allAnalysisUnits = ErrorBounds를 계산해서 각 seq별로 1차 필터링된 값들)
                            val allAnalysisUnits = if (currentMeasureIndex + measureIndex < singletonMeasure?.size!!) {
                                seqList.mapNotNull { seq ->
                                    singletonMeasure?.get(currentMeasureIndex + measureIndex)?.measureResult?.let {
                                        getAnalysisUnits(requireContext(), part, seq, it)
                                    }
                                }
                            } else {
                                listOf()
                            }

                            val triples = matchedTripleIndexes[partIndex]

                            // triples로 allAnalysisUnits 에서 AnalysisUnitVO 3개 추출
                            val processedData = triples.mapNotNull { triple ->
                                allAnalysisUnits.getOrNull(triple.second)?.getOrNull(triple.third)
                            }.toMutableList()
                            // 3개의 AnalysisUnitVO로 하나의 score 계산

                            val score = if (processedData.size == 3) {
                                 calculatePercent(processedData)  // 1f Float 값을 반환
                            } else {
                                50f  // 데이터 부족 시 0으로 처리
                            }
                            partScores.add(score)
                        }
                        result.add(partScores)
                    }
                    val entries = convertEntries(result)
                    Log.v("entries", "$entries")
                    val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                    binding.rvMD.layoutManager = layoutManager
                    val adapter = MeasureDetailRVAdapter(this@MeasureDetailFragment, entries, dates.reversed())
                    binding.rvMD.adapter = adapter
                }
            }
        }

    }

    // seq안에 // 각각의 데이터들이 들어가 있음.
    private fun convertEntries(result: MutableList<MutableList<Float>>) : MutableList<MutableList<Entry>> {
        val convertedEntries = mutableListOf<MutableList<Entry>>()

        result.forEachIndexed { partIndex, scores ->  // 바깥 리스트(13개의 부위)
            val partEntries = mutableListOf<Entry>()

            scores.forEachIndexed { measureIndex, score ->  // 각 부위의 5개의 measure
                val entry = Entry(measureIndex.toFloat(), score)
                partEntries.add(entry)
            }

            convertedEntries.add(partEntries)  // 각 부위별 Entry 리스트 추가
        }

        return convertedEntries
    }

    // 3개의 progressUnitVO를 가져와서
    private fun calculatePercent(processedData: MutableList<AnalysisUnitVO>) : Float {
        val calculatePercent = processedData.map { calculateBoundedScore(it.columnName, abs(it.rawData), it.rawDataBound) }.map { if (it <= 50f) 50f else it }
        return calculatePercent.average().toFloat()
    }
    private fun updateUI() {
        binding.tvMDScore.text = measure?.overall.toString()
        binding.tvMDDate.text = "${measure?.regDate?.substring(0, 10)} ${measure?.userName}"
        binding.tvMDParts.text = "우려부위: ${measure?.dangerParts?.map { it.first }?.joinToString(", ")}"
    }
}