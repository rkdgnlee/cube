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
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.BalanceRVAdapter
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureDetailBinding
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import java.io.File
import java.io.FileOutputStream


class MeasureDetailFragment : Fragment() {
    lateinit var binding : FragmentMeasureDetailBinding
    lateinit var bodyParts : List<String>
    private lateinit var measure : MeasureVO
    private val viewModel : MeasureViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasureDetailBinding.inflate(inflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        // ------# measure 에 맞게 UI 수정 #------
        measure = viewModel.selectedMeasure!!
        updateUI()

        // ------# 10각형 레이더 차트 #------
        // TODO 레이더 차트에도 값이 들어가야 함.

        val bodyParts = listOf("목관절", "우측 어깨", "좌측 어깨", "우측 팔꿉", "좌측 팔꿉","우측 손목","좌측 손목", "우측 골반","좌측 골반", "우측 무릎","좌측 무릎","우측 발목", "좌측 발목")
        val indices = listOf(0, 1, 3, 5, 7, 9, 11, 12, 10, 8, 6, 4, 2)

        // 먼저 모든 점수를 95로 초기화
        val scores = MutableList(bodyParts.size) { 97f }
        Log.v("raderScores", "${ measure.dangerParts}")
        // measure.dangerParts에 있는 부위들의 점수만 업데이트
        measure.dangerParts.forEach { (part, danger) ->
            val index = bodyParts.indexOf(part)
            if (index != -1) {
                scores[index] = when (danger) {
                    1.0f -> 80f
                    2.0f -> 70f
                    3.0f -> 60f
                    else -> 97f
                }
            }
        }

        // indices를 사용하여 올바른 순서로 raderScores 생성
        val raderScores = indices.map { scores[it] }
        val raderXParts = indices.map { bodyParts[it] }
        Log.v("raderScores", raderScores.toString())

        val entries = mutableListOf<RadarEntry>()
        for (i in 0 until bodyParts.size) {
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
                textSize = 13f  // 텍스트 크기 증가
                yOffset = 0f  // 텍스트를 차트에서 조금 더 멀리 배치


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

        val dangerParts = measure.dangerParts.map { it.first }.toMutableList()
        val stages = mutableListOf<MutableList<String>>()
        val balanceParts1 = mutableListOf("어깨", "골반")
        stages.add(balanceParts1)
        val balanceParts2 = mutableListOf("어깨", "팔꿉", "좌측 전완")
        stages.add(balanceParts2)
        val balanceParts3 = mutableListOf("골반", "좌측 어깨", "목")
        stages.add(balanceParts3)
        val balanceParts4 = mutableListOf("허벅지",  "어깨")
        stages.add(balanceParts4)
        val balanceParts5 = mutableListOf("좌측 허벅지", "좌측 골반", "좌측 어깨")
        stages.add(balanceParts5)

        val degrees =  mutableListOf(Pair(1, 3), Pair(1,0), Pair(1, 2), Pair(2, -1), Pair(0 , -4))
        setAdapter(stages, degrees)
//        val dangerParts = measure.dangerParts.map { it.first }.toMutableList()
//        val stages = mutableListOf<MutableList<String>>()
//        stages.add( dangerParts.subList(0, 2))
//        stages.add( dangerParts.subList(0, 2))
//        stages.add( dangerParts.subList(0, 2))
//        stages.add( dangerParts.subList(2, 3))
//        stages.add( dangerParts.subList(2, 3))
//
//        val dangerDegree = measure.dangerParts.map { it.second }.toMutableList()
//        val degrees = mutableListOf<Pair<Int,Int>>()
//        Log.v("degreesss", "$dangerDegree")
//        for (i in 0 until 5) {
//            val degree = dangerDegree?.getOrNull(i) ?: -1  // null이면 기본값으로 -1 사용
//            when (degree) {
//                1 -> degrees.add(Pair(1, Random.nextInt(2, 4)))
//                2 -> degrees.add(Pair(2, Random.nextInt(-1, 2)))
//                else -> degrees.add(Pair(3, Random.nextInt(-2, -1)))  // 여기서 else는 null 또는 다른 값에 대한 처리
//            }
//        }


        binding.fabtnMD.setOnClickListener {
            (activity as? MainActivity)?.launchMeasureSkeletonActivity()
        }

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
            val url = Uri.parse("tangoplus://tangoq/3")
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/png" // 이곳에서 공유 데이터 변경
            intent.putExtra(Intent.EXTRA_STREAM, fileUri)
            intent.putExtra(Intent.EXTRA_TEXT, "제 밸런스 그래프를 공유하고 싶어요 !\n$url")
            startActivity(Intent.createChooser(intent, "밸런스 그래프"))
        }
    }

    private fun setAdapter(stages: MutableList<MutableList<String>>, degrees: MutableList<Pair<Int,Int>>) {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvMD.layoutManager = layoutManager
        val balanceAdapter = BalanceRVAdapter(this@MeasureDetailFragment, stages, degrees)
        binding.rvMD.adapter = balanceAdapter
    }

    private fun calculateBalanceScore(angle: Float, case: String): Int {
        val normalRange = when (case) {
            "목" -> 0.5
            "우측어깨", "좌측어깨","우측무릎", "좌측무릎" -> 1.0
            else -> 3.0
        }
        val deviationFromNormal = Math.abs(angle) - normalRange

        return when {
            deviationFromNormal <= 0 -> 100
            else -> (100 - (deviationFromNormal * 10)).toInt().coerceAtLeast(0)
        }
    }

    private fun updateUI() {
        binding.tvMDScore.text = measure.overall.toString()
        binding.tvMDDate.text = measure.regDate.substring(0, 10)
        binding.tvMDParts.text = "우려부위: ${measure.dangerParts.map { it.first }.joinToString(", ")}"
    }
}