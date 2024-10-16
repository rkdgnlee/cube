package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
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
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.annotation.OptIn
import androidx.compose.runtime.mutableStateOf
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignTop
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureDashboard1Binding
import com.tangoplus.tangoq.dialog.ReportDiseaseDialogFragment
import com.tangoplus.tangoq.`object`.DeviceService.isNetworkAvailable
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import okhttp3.internal.format
import org.apache.commons.math3.distribution.NormalDistribution
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale


class MeasureDashBoard1Fragment : Fragment() {
    lateinit var binding : FragmentMeasureDashboard1Binding
    val viewModel : MeasureViewModel by activityViewModels()
    private var balloon : Balloon? = null
    // ------! 싱글턴 패턴 객체 가져오기 !------
    private lateinit var singletonMeasure: Singleton_t_measure

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasureDashboard1Binding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("SetTextI18n", "CommitTransaction")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        singletonMeasure = Singleton_t_measure.getInstance(requireContext())
        // ------!  이름 + 통증 부위 시작 !------
        val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject

        when (isNetworkAvailable(requireContext())) {
            true -> {
                try {
                    if (singletonMeasure.measures?.isNotEmpty() == true) {
                        val hideBadgeFunction = hideBadgeOnClick(binding.tvMD1Badge, binding.clMD1PredictDicease, "${binding.tvMD1Badge.text}", ContextCompat.getColor(requireContext(), R.color.thirdColor))
                        binding.tvMD1TotalScore.text = "${viewModel.selectedMeasure?.overall?: 0}"
                        binding.tvMD1MeasureHistory.text = if (viewModel.selectedMeasure?.regDate == "") "측정기록없음" else "최근 측정 기록 - ${viewModel.selectedMeasure?.regDate?.substring(0, 10)}"
                        binding.tvMD1Name.text = "${userJson?.optString("user_name")}님의 기록"
                        binding.clMD1PredictDicease.setOnClickListener{
                            hideBadgeFunction?.invoke()
                            val dialog = ReportDiseaseDialogFragment()
                            dialog.show(requireActivity().supportFragmentManager, "ReportDiseaseDialogFragment")
                        }
                    } else {
                        binding.tvMD1TotalScore.text = "0"
                        binding.tvMD1MeasureHistory.text = "측정기록없음"
                        binding.tvMD1Name.text = "${userJson?.optString("user_name")}님의 기록"
                        binding.tvMD1Duration.text = "측정 기록 없음"
                        val params = binding.ivMD1Position.layoutParams as ConstraintLayout.LayoutParams
                        params.horizontalBias = 0.5f
                        binding.ivMD1Position.layoutParams = params
                        binding.ivMD1Position.visibility = View.GONE
                    }
                } catch (e: NullPointerException) {
                    Log.e("MD1Error", "$e")
                }


            }
            false -> {

            }
        }

        // ------# 공유하기 버튼 #------
        binding.btnMD1Share.setOnClickListener {

            // ------! 그래프 캡처 시작 !------
            val bitmap = Bitmap.createBitmap(binding.ClMD1.width, binding.ClMD1.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            binding.ClMD1.draw(canvas)

            val file = File(context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "shared_image.jpg")
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()

            val fileUri = FileProvider.getUriForFile(requireContext(), context?.packageName + ".provider", file)
            // ------! 그래프 캡처 끝 !------
            val url = Uri.parse("tangoplus://tangoq/2")
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/png" // 이곳에서 공유 데이터 변경
            intent.putExtra(Intent.EXTRA_STREAM, fileUri)
            intent.putExtra(Intent.EXTRA_TEXT, "제 측정 결과들을 공유하고 싶어요 !\n$url")
            startActivity(Intent.createChooser(intent, "측정 결과"))
        }

        // ------# 자세히 보기 #------
        binding.tvMD1More1.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                replace(R.id.flMain, MeasureHistoryFragment())
                addToBackStack(null)
                commit()
            }
        }

        binding.btnMD1More2.setOnClickListener{
            requireActivity().supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                replace(R.id.flMain, MeasureHistoryFragment())
                addToBackStack(null)
                commit()
            }
        } // ------ ! 자세히 보기 끝 !------

        // ------! 꺾은선 그래프 시작 !------
        val lineChart = binding.lcMD1
        val lcXAxis = lineChart.xAxis
        val lcYAxisLeft = lineChart.axisLeft
        val lcYAxisRight = lineChart.axisRight
        val lcLegend = lineChart.legend
        var startIndex = 0

        val lcDataList: MutableList<Pair<String, Int>> = mutableListOf()

        val measures = singletonMeasure.measures
        if (measures?.size != 7) {
            for (i in 0 until (7 - measures?.size!!)) {
                lcDataList.add(Pair("", 0))
            }
        }

        for (i in measures.size - 1 downTo 0) {
            val measure = measures[i]
            lcDataList.add(Pair(measure.regDate, measure.overall?.toInt()!!))
        }
        Log.v("lcDataList", "${lcDataList}")
        for (i in lcDataList.indices) {
            startIndex = i
            break
        }

        val lcEntries: MutableList<Entry> = mutableListOf()
        for (i in startIndex until lcDataList.size) {
            lcEntries.add(Entry((i - startIndex).toFloat(), lcDataList[i].second.toFloat()))
        }


        val lcLineDataSet = LineDataSet(lcEntries, "")
        lcLineDataSet.apply {
            color = resources.getColor(R.color.thirdColor, null)
            circleRadius = 4F
            lineWidth = 4F
            valueTextSize = 0F
            setCircleColors(resources.getColor(R.color.thirdColor, null))
            setDrawCircleHole(false)
            setDrawFilled(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        lcXAxis.apply {
            isEnabled = false
            textSize = 14f
            textColor = resources.getColor(R.color.subColor500)
            labelRotationAngle = 2F
            setDrawAxisLine(false)
            setDrawGridLines(false)

            setLabelCount(lcDataList.size, true)
            position = XAxis.XAxisPosition.BOTTOM
            axisLineWidth = 1.0f
        }
        lcYAxisLeft.apply {
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
            setTouchEnabled(true)
            isDragEnabled = true
            description.isEnabled = false
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
            data.setDrawValues(false)
            for (set in data.dataSets) {
                if (set is LineDataSet) {
                    set.setDrawHighlightIndicators(false)
                }
            }

            notifyDataSetChanged()
            description.text = ""
            setScaleEnabled(false)
            invalidate()
        }
        // ------! 날짜 기간 가져오기 시작 !------
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val outputFormatter = DateTimeFormatter.ofPattern("MM.dd")

        val datesWithIndex = lcDataList.mapIndexedNotNull { index, pair ->
            if (pair.first.isNotEmpty()) {
                index to LocalDate.parse(pair.first, inputFormatter)
            } else null
        }

        if (datesWithIndex.isNotEmpty()) {
            val oldestDate = datesWithIndex.minByOrNull { it.second }
            val newestDate = datesWithIndex.maxByOrNull { it.second }

            if (oldestDate != null && newestDate != null) {
                binding.tvMD1Duration.text = "${oldestDate.second.format(outputFormatter)} ~ ${newestDate.second.format(outputFormatter)}"
            }
        }
        // ------! 날짜 기간 가져오기 끝 !------

        // ------! 값 클릭 시 벌룬 나오기 시작 !------
        lineChart.setOnChartValueSelectedListener(object: OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                e?.let { entry ->
                    val originalIndex = startIndex + entry.x.toInt()
                    val selectedData = lcDataList[originalIndex]
                    val balloonText = if (selectedData.first != "") "측정날짜: ${selectedData.first.substring(0, 10)}\n" + "점수: ${entry.y.toInt()}점" else "측정 기록이 없습니다."
                    val balloonlc1 = Balloon.Builder(requireContext())
                        .setWidthRatio(0.5f)
                        .setHeight(BalloonSizeSpec.WRAP)
                        .setText(balloonText)
                        .setTextColorResource(R.color.subColor800)
                        .setTextSize(15f)
                        .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                        .setArrowSize(0)
                        .setMargin(10)
                        .setPadding(12)
                        .setCornerRadius(8f)
                        .setBackgroundColorResource(R.color.white)
                        .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
                        .setLifecycleOwner(viewLifecycleOwner)
                        .build()


                    val pts = FloatArray(2)
                    pts[0] = entry.x
                    pts[1] = entry.y
                    lineChart.getTransformer(YAxis.AxisDependency.LEFT).pointValuesToPixel(pts)
                    balloonlc1.showAlignTop(lineChart, pts[0].toInt(), pts[1].toInt())


                }

            }
            override fun onNothingSelected() {}
        })
        // ------! 꺾은선 그래프 코드 끝 !------

        // ------! balloon 시작 !------
        val params = binding.ivMD1Position.layoutParams as ConstraintLayout.LayoutParams
//        params.horizontalBias = 0.731f
        binding.ivMD1Position.layoutParams = params
        val percent = (params.horizontalBias * 100).toInt()

        when (percent) {
            in 0 .. 30 -> {
                createBalloon(userJson, percent)
                binding.vMD1Middle.visibility = View.INVISIBLE
                binding.vMD1Low.visibility = View.VISIBLE
            }
            in 70 .. 100 -> {
                createBalloon(userJson, percent)
                binding.vMD1Middle.visibility = View.INVISIBLE
                binding.vMD1Low.visibility = View.GONE
                binding.vMD1High.visibility = View.VISIBLE
            }
            else -> {
                createBalloon(userJson, percent)
                binding.vMD1Middle.visibility = View.VISIBLE
                binding.vMD1High.visibility = View.GONE
                binding.vMD1Low.visibility = View.GONE
            }
        }

        binding.ivMD1Position.setOnClickListener{
            binding.ivMD1Position.showAlignTop(balloon!!)
            balloon!!.dismissWithDelay(2500L)
        }
//
//        // -------! 500개의 범위 !------
//        for (x in -250..250) {
//            val xValue = x / 100.0
//            val yValue = (1 / (stdDev * sqrt(2 * Math.PI))) * exp(-0.5 * ((xValue - mean) / stdDev).pow(2))
//            entries.add(Entry(xValue.toFloat(), yValue.toFloat()))
//
//            // 설정된 구간에 따라 색상 변경 zScore로 표준편차 적용 (60%, 70%, 80%, 90%)
//            if (xValue > -zScore && xValue < zScore) {
//                entriesHighlighted.add(Entry(xValue.toFloat(), yValue.toFloat()))
//            }
//        }
//        val dataSet = LineDataSet(entries, "").apply {
//            setCircleColors(resources.getColor(R.color.secondaryColor, null))
//            circleSize = 1.5f
//            highlightLineWidth = 0f
//            highLightColor = Color.TRANSPARENT
//        }
//        val dataSetHighlighted = LineDataSet(entriesHighlighted, "80% Range").apply {
//            setCircleColors(resources.getColor(R.color.mainColor, null))
//            circleSize = 1.5f
//            setDrawFilled(true)
//            fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.color_gradient_sub_color_300)
////            fillColor = resources.getColor(R.color.mainColor)
//            highlightLineWidth = 0f
//            highLightColor = Color.TRANSPARENT
//
//
//        }
//
//        val userEntry = Entry(userValue.toFloat(), ((1 / (stdDev * sqrt(2 * Math.PI))) * exp(-0.5 * ((userValue - mean) / stdDev).pow(2))).toFloat())
//        val userDataSet = LineDataSet(listOf(userEntry), "User Value")
//        userDataSet.apply {
//            setDrawCircles(true)
//            circleRadius = 8f
//            setCircleColors(resources.getColor(R.color.mainColor, null))
//            circleHoleRadius = 4f
//            setDrawCircleHole(true)
//            setDrawFilled(false)
////            fillColor = resources.getColor(R.color.mainColor)
//            highlightLineWidth = 0f
//            highLightColor = Color.TRANSPARENT
////            fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.color_gradient_main)
////            setDrawValues(true)
////            valueTextSize = 12f
////            valueTextColor = resources.getColor(R.color.subColor800, null)
//
//        }
//
//        val lineData = LineData()
//        lineData.addDataSet(dataSet)
//        lineData.addDataSet(dataSetHighlighted)
//        lineData.addDataSet(userDataSet)
//
//
//
//        lineChartND.apply {
//            setTouchEnabled(true)
//            isDoubleTapToZoomEnabled = false
//            data = lineData
//            description.isEnabled = false
//            legend.isEnabled = false
//            setOnChartValueSelectedListener(object : OnChartValueSelectedListener{
//                override fun onValueSelected(e: Entry?, h: Highlight?) {
//                    // ------! info popup 창 시작 !------
//                    val ballonText = if (userValue.absoluteValue <= zScore.absoluteValue) {
//                        "정상범위로 판단됩니다."
//                    } else if (userValue > zScore) {
//                        "평균보다 밸런스 점수가 좋습니다. 계속 유지하세요"
//                    } else {
//                        "밸런스 점수가 평균보다 낮습니다. 꾸준한 운동이 필요합니다"
//                    }
//                     val balloon2 = Balloon.Builder(requireContext())
//                        .setWidthRatio(0.5f)
//                        .setHeight(BalloonSizeSpec.WRAP)
//                        .setText("결과값 ${userValue}로\n$ballonText")
//                        .setTextColorResource(R.color.subColor800)
//                        .setTextSize(15f)
//                        .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
//                        .setArrowSize(0)
//                        .setMargin(10)
//                        .setPadding(12)
//                        .setCornerRadius(8f)
//                        .setBackgroundColorResource(R.color.white)
//                        .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
//                        .setLifecycleOwner(viewLifecycleOwner)
//                        .build()
//                    // ------! info popup 창 끝 !------
//
//                    Log.v("showBalloon", "true")
//                    lineChartND.showAlignEnd(balloon2)
//                    balloon2.dismissWithDelay(3000L)
//                }
//                override fun onNothingSelected() {}
//            })
//
//            xAxis.apply {
//                position = XAxis.XAxisPosition.BOTTOM
//                setDrawAxisLine(false)
//                setDrawGridLines(false)
//                setDrawLabels(false)  // X축 레이블 숨김
//                axisMinimum = -2f
//                axisMaximum = 2f
//                labelCount = 10
//            }
//            axisLeft.apply {
//                setDrawGridLines(false)
//                setDrawAxisLine(false)
////                setLabelCount(5, false)
//                setDrawLabels(false)  // Y축 레이블 숨김
//                axisMinimum = 0f
//                axisMaximum = 0.5f
//            }
//            axisRight.apply {
//                setDrawGridLines(false)
//                setDrawAxisLine(false)
//                setDrawLabels(false)
//            }
//            invalidate()
//        }
//    }

    // ------! 추천 운동 받기 시작 !------
//    private fun goExerciseDetailFragment(parts: MeasureVO?) {
//        /** 1. 관절의 데이터 점수를 가져와서
//         *  2. 추천 운동은 그 데이터 점수에서 가장 낮은 걸 가져와서 해당 값에 맞게 가야하는거지.
//         *  3. 내가 팔꿉에 대한 뭐 통증이 그렇게 나오면, exercise_search= 3
//         * */
//        val category = Pair(0, "전체")
//        val search = Pair(transformJointNum(parts!!), parts.partName)
//        requireActivity().supportFragmentManager.beginTransaction().apply {
//            setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
//            add(R.id.flMain, ExerciseDetailFragment.newInstance(search, -1))
////            addToBackStack(null)
//            commit()
//        }
//    }

//    private fun transformJointNum(part: MeasureVO) : Int {
//        return when (part.partName) {
//            "손목" -> 1
//            "척추" -> 2
//            "팔꿉" -> 3
//            "목" -> 4
//            "발목" -> 5
//            "어깨" -> 6
//            "무릎" -> 7
//            "복부" -> 8
//            else -> 0
//        }
    }
    // ------! 추천 운동 받기 끝!------
    private fun createBalloon(userJson: JSONObject?, percent: Int) {

        balloon = Balloon.Builder(requireContext())
            .setWidthRatio(0.6f)
            .setHeight(BalloonSizeSpec.WRAP)
            .setText("${userJson?.optString("user_name")}님 연령대에서\n${if (percent >= 50) "상위 ${100 - percent}" else "하위 ${percent}"}%에 위치합니다.")
            .setTextColorResource(R.color.white)
            .setTextSize(15f)
            .setArrowPositionRules(ArrowPositionRules.ALIGN_BALLOON)
            .setArrowSize(0)
            .setMargin(4)
            .setPadding(8)
            .setCornerRadius(16f)
            .setBackgroundColorResource(when (percent) {
                in 0..30 -> {R.color.deleteColor}
                else -> {R.color.mainColor}
            })
            .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()
    }
}