package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.tabs.TabLayout
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignEnd
import com.tangoplus.tangoq.AlarmActivity
import com.tangoplus.tangoq.adapter.PainPartRVAdpater
import com.tangoplus.tangoq.data.Measurement
import com.tangoplus.tangoq.dialog.MeasurePartDialogFragment
import com.tangoplus.tangoq.listener.OnPartCheckListener
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.MeasureSkeletonActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.GraphVO
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureBinding
import com.tangoplus.tangoq.dialog.PoseViewDialogFragment
import com.tangoplus.tangoq.`object`.DeviceService.isNetworkAvailable
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import org.apache.commons.math3.distribution.NormalDistribution
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import kotlin.math.absoluteValue
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt


class MeasureFragment : Fragment(), OnPartCheckListener {
    lateinit var binding : FragmentMeasureBinding
    val viewModel : MeasureViewModel by activityViewModels()
    val endTime = LocalDateTime.now()
//    val startTime = LocalDateTime.now().minusDays(1)
    var popupWindow : PopupWindow?= null
    // ------! 싱글턴 패턴 객체 가져오기 !------
    private lateinit var singletonInstance: Singleton_t_measure
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasureBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("SetTextI18n", "CommitTransaction")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        singletonInstance = Singleton_t_measure.getInstance(requireContext())

        binding.ibtnMAlarm.setOnClickListener {
            val intent = Intent(requireContext(), AlarmActivity::class.java)
            startActivity(intent)
        }

        when (isNetworkAvailable(requireContext())) {
            true -> {

            }
            false -> {

            }
        }
        // ------! tab & enum class 관리 시작 !------
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val measurement : Measurement = when (tab?.position) {
                    0 -> Measurement.DAILY
                    1 -> Measurement.WEEKLY
                    2 -> Measurement.MONTHLY
                    else -> throw IllegalArgumentException("Invalid Tab Position")
                }
                // enum class 로 값 변경
                when (measurement) {
                    Measurement.DAILY -> {
                        binding.tvMsBalanceScore.text = "미설정"
                    }
                    Measurement.WEEKLY -> {
                        binding.tvMsBalanceScore.text = "미설정"
                    }
                    Measurement.MONTHLY -> {
                        binding.tvMsBalanceScore.text = "미설정"
                    }
                }

            }
            override fun onTabUnselected(p0: TabLayout.Tab?) {}
            override fun onTabReselected(p0: TabLayout.Tab?) {}
        })

        // ------! tab & enum class 관리 끝 !------

        // ------! 공유하기 버튼 시작 !------
        binding.btnMsShare.setOnClickListener {

            // ------! 그래프 캡처 시작 !------
            val bitmap = Bitmap.createBitmap(binding.ClMs.width, binding.ClMs.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            binding.ClMs.draw(canvas)

            val file = File(context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "shared_image.jpg")
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()

            val fileUri = FileProvider.getUriForFile(requireContext(), context?.packageName + ".provider", file)
            // ------! 그래프 캡처 끝 !------

            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/png" // 이곳에서 공유 데이터 변경
            intent.putExtra(Intent.EXTRA_STREAM, fileUri)
            intent.putExtra(Intent.EXTRA_TEXT, "제 밸런스 그래프를 공유하고 싶어요 !")
            startActivity(Intent.createChooser(intent, "밸런스 그래프"))
        } // ------! 공유하기 버튼 끝 !------

        // ------! 측정 버튼 시작 !------
        binding.btnMsMeasurement.setOnClickListener {
            val intent = Intent(requireContext(), MeasureSkeletonActivity::class.java)
            startActivity(intent)
        } // ------! 측정 버튼 끝 !------

        // ------! 통증 부위 관리 시작 !------
        binding.tvMsAddPart.setOnClickListener {
            val dialog = MeasurePartDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "MeasurePartDialogFragment")
        }
        binding.clMsAddPart.setOnClickListener {
            val dialog = MeasurePartDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "MeasurePartDialogFragment")
        } // ------! 통증 부위 관리 끝 !------

        // ------!  이름 + 통증 부위 시작 !------
        val t_userdata = Singleton_t_user.getInstance(requireContext()).jsonObject?.getJSONObject("login_data")

        binding.tvMsUserName.text = t_userdata?.optString("user_name")

        // TODO 1. t_measure등에서 가져오는 결과값이 있다 --> 밸런스 점수, 뭐 측정일자 같은거 업데이트
        // TODO 2. 일단 통증 부위를 일단 넣을 수 있게.
        viewModel.parts.observe(viewLifecycleOwner) { parts ->
            if (parts.isEmpty()) {
                binding.llMsEmpty.visibility = View.VISIBLE
                setPainPartRV(parts)

            } else if (parts.size == 1) {
                binding.llMsEmpty.visibility = View.GONE
                binding.rvMsRight.visibility = View.GONE
                (binding.rvMsLeft.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = 0
                setPainPartRV(parts)
            } else {
                binding.llMsEmpty.visibility = View.GONE
                binding.rvMsRight.visibility = View.VISIBLE
                (binding.rvMsLeft.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = 8

                // ------! 왼쪽 painpart !------
                val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                val leftData = parts.filterIndexed { index, _ -> index % 2 == 0 }.toMutableList()
                val leftadapter = PainPartRVAdpater(this@MeasureFragment, leftData, "Pp", this@MeasureFragment)
                binding.rvMsLeft.adapter = leftadapter
                if (leftData.isNotEmpty()) {
                    binding.rvMsLeft.layoutManager = linearLayoutManager
                }

                // ------! 오른쪽 painpart !------
                val linearLayoutManager2 = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                val rightData = parts.filterIndexed { index, _ -> index % 2 == 1 }.toMutableList()
                val rightAdapter = PainPartRVAdpater(this@MeasureFragment, rightData, "Pp", this@MeasureFragment)
                binding.rvMsRight.adapter = rightAdapter
                if (rightData.isNotEmpty()) {
                    binding.rvMsRight.layoutManager = linearLayoutManager2
                }
            }
        } // ------!  이름 + 통증 부위 끝 !------

        // ------! 리포트 버튼 시작 !------
//        startBounceAnimation(binding.btnMsGetReport)
        binding.btnMsGetReport.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                replace(R.id.flMain, ReportFragment())
                commit()
            }
            requireContext()
        }
        // ------! 리포트 버튼 끝 !------

        binding.btnMsGetRecommend.setOnClickListener {
            // TODO partName에 대해서 가장 높은 점수인것만 가져오는 거지
            /* 근데 말이 안되는게, ExerciseDetailFragment는 대분류로 움직이는 건데, 특정 dialogFragment안에서 추가적으로 어떤 것들만 볼 수 있게. 추천 운동 (전체화면 느낌)
            * r*/
//            val worstPart = viewModel.parts.value?.sortedBy { it.anglesNDistances?.getJSONObject(BigDecimal("10.0").toString())?.optDouble("result_balance_score_angle_ankle") }
//                ?.get(0)
//            goExerciseDetailFragment(Pair(worstPart.))

        }

        // ------! 꺾은선 그래프 시작 !------
        val lineChart = binding.lcMs
        lineChart.setTouchEnabled(false)

        val lcXAxis = lineChart.xAxis
        val lcYAxisLeft = lineChart.axisLeft
        val lcYAxisRight = lineChart.axisRight
        val lcLegend = lineChart.legend

        val lcDataList : MutableList<GraphVO> = mutableListOf()
//        val startMonth = (month + 8) % 12
//        for (i in 0 until 12) {
//            val currentMonth = (startMonth + i) % 12
//            val monthLabel = if (currentMonth == 0) "12월" else "${currentMonth}월"
//            lcDataList.add(ChartVO(monthLabel, Random.nextInt(99)))
//        }
        val weekList = listOf("월", "화", "수", "목", "금", "토", "일")
        for (i in weekList) {
            var y = 88
//            lcDataList.add(GraphVO(i, i.length.p))
            lcDataList.add(GraphVO(i, y))
            y += 1
        }
        val lcEntries : MutableList<Entry> = mutableListOf()
        for (i in lcDataList.indices) {
            // entry는 y축에 넣는 데이터 형식을 말함. Entry의 1번째 인자는 x축의 데이터의 순서, 두 번째 인자는 y값
            lcEntries.add(Entry(i.toFloat(), lcDataList[i].yAxis.toFloat()))
        }
        val lcLineDataSet = LineDataSet(lcEntries, "")
        lcLineDataSet.apply {
            color = resources.getColor(R.color.mainColor, null)
            circleRadius = 4F
            lineWidth = 4F
            mode = LineDataSet.Mode.STEPPED
            valueTextSize = 0F
            setCircleColors(resources.getColor(R.color.mainColor))
            setDrawCircleHole(false)
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.color_gradient_sub_color_300)
        }

        lcXAxis.apply {
            textSize = 14f
            textColor = resources.getColor(R.color.subColor500)
            labelRotationAngle = 2F
            setDrawAxisLine(false)
            setDrawGridLines(false)
            lcXAxis.valueFormatter = (IndexAxisValueFormatter(lcDataList.map { it.xAxis }))
            setLabelCount(lcDataList.size, true)
            lcXAxis.position = XAxis.XAxisPosition.BOTTOM
            axisLineWidth = 1.0f
        }
        lcYAxisLeft.apply {
            setDrawGridLines(false)
            setDrawAxisLine(false)
//            setDrawGridLinesBehindData(true)
//            setDrawZeroLine(false)
            setLabelCount(3, false)
            setDrawLabels(false)
//            textColor = resources.getColor(R.color.subColor500)
//            axisLineWidth = 1.5f
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
            notifyDataSetChanged()
            description.text = ""
            setScaleEnabled(false)
            invalidate()
        }
        // ---- 꺾은선 그래프 코드 끝 ----

        val lineChartND = requireActivity().findViewById<LineChart>(R.id.lcMSNS)
        val mean = 0.0
        val stdDev = 1.0
        val ndText = "70"
        val zScore : Double
//        when (ndText) {
//            "60%" -> zScore = 0.842
//            "70%" -> zScore = 1.04
//            "80%" -> zScore = 1.28
//            "90%" -> zScore = 1.645
//        }
        zScore = 1.04
        val entries = ArrayList<Entry>()
        val entriesHighlighted = ArrayList<Entry>()

        // -------! 사용자 밸런스 점수의 백분위 값 !------
        val userValue = 0.625


        // ------! balloon 시작 !------
        val balloon = Balloon.Builder(requireContext())
            .setWidthRatio(0.6f)
            .setHeight(BalloonSizeSpec.WRAP)
            .setText("밸런스 점수 중간값의 ±${ndText.toInt() / 2}%를\n정상 범위로 판단합니다.")
            .setTextColorResource(R.color.subColor800)
            .setTextSize(15f)
            .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            .setArrowSize(0)
            .setMargin(6)
            .setPadding(12)
            .setCornerRadius(8f)
            .setBackgroundColorResource(R.color.white)
            .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()
        binding.ivMsInfo.setOnClickListener{
            binding.ivMsInfo.showAlignEnd(balloon)
            balloon.dismissWithDelay(2500L)
        }

        // -------! 500개의 범위 !------
        for (x in -250..250) {
            val xValue = x / 100.0
            val yValue = (1 / (stdDev * sqrt(2 * Math.PI))) * exp(-0.5 * ((xValue - mean) / stdDev).pow(2))
            entries.add(Entry(xValue.toFloat(), yValue.toFloat()))

            // 설정된 구간에 따라 색상 변경 zScore로 표준편차 적용 (60%, 70%, 80%, 90%)
            if (xValue > -zScore && xValue < zScore) {
                entriesHighlighted.add(Entry(xValue.toFloat(), yValue.toFloat()))
            }
        }
        val dataSet = LineDataSet(entries, "").apply {
            setCircleColors(resources.getColor(R.color.secondaryColor, null))
            circleSize = 1.5f
            highlightLineWidth = 0f
            highLightColor = Color.TRANSPARENT
        }
//        val cardView = CardView(requireContext()).apply {
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                resources.getDimensionPixelSize(R.dimen.card_height)
//            )
//            radius = resources.getDimension(R.dimen.card_corner_radius)
//            elevation = resources.getDimension(R.dimen.card_corner_elevation)
//
//        }
        val dataSetHighlighted = LineDataSet(entriesHighlighted, "80% Range").apply {
            setCircleColors(resources.getColor(R.color.mainColor, null))
            circleSize = 1.5f
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.color_gradient_sub_color_300)
//            fillColor = resources.getColor(R.color.mainColor)
            highlightLineWidth = 0f
            highLightColor = Color.TRANSPARENT


        }

        val userEntry = Entry(userValue.toFloat(), ((1 / (stdDev * sqrt(2 * Math.PI))) * exp(-0.5 * ((userValue - mean) / stdDev).pow(2))).toFloat())
        val userDataSet = LineDataSet(listOf(userEntry), "User Value")
        userDataSet.apply {
            setDrawCircles(true)
            circleRadius = 8f
            setCircleColors(resources.getColor(R.color.mainColor, null))
            circleHoleRadius = 4f
            setDrawCircleHole(true)
            setDrawFilled(false)
//            fillColor = resources.getColor(R.color.mainColor)
            highlightLineWidth = 0f
            highLightColor = Color.TRANSPARENT
//            fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.color_gradient_main)
//            setDrawValues(true)
//            valueTextSize = 12f
//            valueTextColor = resources.getColor(R.color.subColor800, null)

        }

        val lineData = LineData()
        lineData.addDataSet(dataSet)
        lineData.addDataSet(dataSetHighlighted)
        lineData.addDataSet(userDataSet)



        lineChartND.apply {
            setTouchEnabled(true)
            isDoubleTapToZoomEnabled = false
            data = lineData
            description.isEnabled = false
            legend.isEnabled = false
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener{
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    // ------! info popup 창 시작 !------
                    val ballonText = if (userValue.absoluteValue <= zScore.absoluteValue) {
                        "정상범위에 있습니다"
                    } else if (userValue > zScore) {
                        "평균보다 밸런스 점수가 좋습니다 !계속 유지하세요"
                    } else {
                        "밸런스 점수가 평균보다 낮습니다. 꾸준한 운동이 필요합니다"
                    }
                     val balloon2 = Balloon.Builder(requireContext())
                        .setWidthRatio(0.5f)
                        .setHeight(BalloonSizeSpec.WRAP)
                        .setText("결과값 ${userValue}로\n$ballonText")
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
                    // ------! info popup 창 끝 !------

                    Log.v("showBalloon", "true")
                    lineChartND.showAlignEnd(balloon2)
                    balloon2.dismissWithDelay(3000L)
                }
                override fun onNothingSelected() {}
            })

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawAxisLine(false)
                setDrawGridLines(false)
                setDrawLabels(false)  // X축 레이블 숨김
                axisMinimum = -2f
                axisMaximum = 2f
                labelCount = 10
            }
            axisLeft.apply {
                setDrawGridLines(false)
                setDrawAxisLine(false)
//                setLabelCount(5, false)
                setDrawLabels(false)  // Y축 레이블 숨김
                axisMinimum = 0f
                axisMaximum = 0.5f
            }
            axisRight.apply {
                setDrawGridLines(false)
                setDrawAxisLine(false)
                setDrawLabels(false)
            }
            invalidate()
        }

//        val limitLine = LimitLine(zScore.toFloat(), "")
//        val limitLine2 = LimitLine(-zScore.toFloat(), "")
//        limitLine.lineColor = resources.getColor(R.color.mainColor, null)
//        limitLine.lineWidth = 0f
//        limitLine2.lineWidth = 0f
//        limitLine2.lineColor = resources.getColor(R.color.mainColor, null)
//
//        lineChartND.xAxis.addLimitLine(limitLine)
//        lineChartND.xAxis.addLimitLine(limitLine2)
//        lineChartND.apply {
//            data = lineData
//            setTouchEnabled(false)
//            setScaleEnabled(false)
//        }
//        val bcLabels = mutableListOf<String>()
//        viewModel.steps.observe(viewLifecycleOwner) { steps ->
//            if (steps.isNotEmpty()) {
//                val bcDataList : MutableList<GraphVO> = mutableListOf()
//                for (i in 0 until steps.size) {
////                    val timeLabel = String.format("%02d:00", hour) // 시간 형식을 "00:00"으로 포맷
//                    bcDataList.add(GraphVO((i+1).toString(), steps[i].toInt()))
//                    // 30분 뒤의 시간 추가
////                    val halfHourLabel = String.format("%02d:30", hour)
//                }
//                // ---- 막대 그래프 코드 시작 ----
//                val barChart = binding.bcMs
//                barChart.setTouchEnabled(false)
////        val roundedBarChartRenderer = roundedBar
////        barChart.renderer = RoundedBarChart
//                val bcXAxis = barChart.xAxis
//                val bcYAxisLeft = barChart.axisLeft
//                val bcYAxisRight = barChart.axisRight
//                val bcLegend = barChart.legend
//
//
//                Log.v("steps", "$bcDataList")
//                val rcEntries : MutableList<BarEntry> = mutableListOf()
//                for (i in bcDataList.indices) {
//                    // entry는 y축에 넣는 데이터 형식을 말함. Entry의 1번째 인자는 x축의 데이터의 순서, 두 번째 인자는 y값
//                    rcEntries.add(BarEntry(i.toFloat(), bcDataList[i].yAxis.toFloat()))
//                }
//                val bcLineDataSet = BarDataSet(rcEntries, "")
//                bcLineDataSet.apply {
//                    color = resources.getColor(R.color.mainColor, null)
//                    valueTextSize = 0F
//                }
//
//                bcXAxis.apply {
//                    textSize = 12f
//                    textColor = resources.getColor(R.color.subColor500)
//                    labelRotationAngle = 1.5F
//                    setDrawAxisLine(false)
//                    setDrawGridLines(false)
//                    bcXAxis.valueFormatter = IndexAxisValueFormatter()
//                    setLabelCount(12, false)
//                    bcXAxis.position = XAxis.XAxisPosition.BOTTOM
////            axisLineWidth = 0f
//
//                }
//                bcYAxisLeft.apply {
//                    gridColor = R.color.subColor200
//                    setDrawGridLines(true)
//                    setDrawAxisLine(false)
////            setDrawGridLinesBehindData(true)
////            setDrawZeroLine(false)
//                    setLabelCount(3, false)
//                    setDrawLabels(false)
////            textColor = resources.getColor(R.color.subColor500)
////            axisLineWidth = 1.5f
//                }
//                bcYAxisRight.apply {
//                    setDrawGridLines(false)
//                    setDrawAxisLine(false)
//                    setLabelCount(0, false)
//                    setDrawLabels(false)
//
//                }
//                bcLegend.apply {
//                    bcLegend.formSize = 0f
//                }
//                barChart.apply {
//                    data = BarData(bcLineDataSet)
//                    notifyDataSetChanged()
//                    description.text = ""
//                    setScaleEnabled(false)
//                    invalidate()
//                }
//                // ------! 막대 그래프 코드 끝 !------
//            }
//        }
        // ------! 막대그래프 시간 세팅 시작 !------
//        Log.v("현재 시", "${endTime.hour}")
//        when (endTime.hour) {
//            in  0 .. 5-> {
//                binding.llMs.removeView(binding.tv0000)
//                binding.llMs.addView(binding.tv0000, 3)
//            }
//            in 6 .. 11 -> {
//                binding.llMs.removeView(binding.tv0600)
//                binding.llMs.addView(binding.tv0600, 3)
//                binding.llMs.removeView(binding.tv0000)
//                binding.llMs.addView(binding.tv0000, 2)
//            }
//            in 12 .. 17 -> {
//                binding.llMs.removeView(binding.tv1800)
//                binding.llMs.addView(binding.tv1800, 0)
//            }
//        }
        // ------! 막대그래프 시간 세팅 끝 !------

//        viewModel.totalSteps.observe(viewLifecycleOwner) { totalSteps ->
//            if (totalSteps != null) {
//                binding.tvMsSteps.text = "- ${viewModel.totalSteps.value} 보"
//            } else {
//                binding.tvMsSteps.text = " - 0 보"
//            }
//        }
//        viewModel.calory.observe(viewLifecycleOwner) {calory ->
//            if (calory != null) {
//                binding.tvMsCalory.text = calory
//            }
//        }
    }

    // ------! 추천 운동 받기 시작 !------
    private fun goExerciseDetailFragment(parts: MeasureVO) {
        /** 1. 관절의 데이터 점수를 가져와서
         *  2. 추천 운동은 그 데이터 점수에서 가장 낮은 걸 가져와서 해당 값에 맞게 가야하는거지.
         *  3. 내가 팔꿉에 대한 뭐 통증이 그렇게 나오면, exercise_search= 3
         * */
        val category = Pair(0, "전체")
        val search = Pair(transformJointNum(parts), parts.partName)
        requireActivity().supportFragmentManager.beginTransaction().apply {
            setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
            add(R.id.flMain, ExerciseDetailFragment.newInstance(search, -1))
//            addToBackStack(null)
            commit()
        }
    }

    private fun transformJointNum(part: MeasureVO) : Int {
        return when (part.partName) {
            "손목" -> 1
            "척추" -> 2
            "팔꿉" -> 3
            "목" -> 4
            "발목" -> 5
            "어깨" -> 6
            "무릎" -> 7
            "복부" -> 8
            else -> 0
        }
    }
    // ------! 추천 운동 받기 끝!------



    @SuppressLint("MissingInflatedId", "InflateParams")
    override fun onPartCheck(part: MeasureVO) {
        if (popupWindow?.isShowing == true) {
            popupWindow?.dismiss()
            popupWindow = null
        } else {
            val inflater = LayoutInflater.from(view?.context)
            val popupView = inflater.inflate(R.layout.pw_main_item, null)
            val width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 186f, view?.context?.resources?.displayMetrics).toInt()
            val height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 162f, view?.context?.resources?.displayMetrics).toInt()

            popupWindow = PopupWindow(popupView, width, height)
            popupWindow!!.showAsDropDown(view)

            popupView.findViewById<TextView>(R.id.tvPPP1).setOnClickListener{
                requireActivity().supportFragmentManager.beginTransaction().apply {
                    add(R.id.flMain, ReportDiseaseFragment())
                    addToBackStack(null)
                    commit()
                }
                popupWindow?.dismiss()
            }
            popupView.findViewById<TextView>(R.id.tvPPP2).setOnClickListener{
                val dialog = PoseViewDialogFragment.newInstance(part.drawableName) // TODO drawableName이 아니라 실제 파일 String에 대한 값을 MeasureVO에 추가해야함
                dialog.show(requireActivity().supportFragmentManager, "PoseViewDialogFragment")
                popupWindow?.dismiss()
            }
            popupView.findViewById<TextView>(R.id.tvPPP3).setOnClickListener{
                requireActivity().supportFragmentManager.beginTransaction().apply {
                    add(R.id.flMain, ReportFragment())
                    addToBackStack(null)
                    commit()
                }
                popupWindow?.dismiss()
            }
        }
    }

    private fun setPainPartRV(parts:  MutableList<MeasureVO>) {
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val leftAdapter = PainPartRVAdpater(this@MeasureFragment, parts, "Pp", this@MeasureFragment)
        binding.rvMsLeft.adapter = leftAdapter
        binding.rvMsLeft.layoutManager = linearLayoutManager
    }

//    private fun startBounceAnimation(view: View) {
//        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f).apply {
//            duration = 800
//            repeatCount = ObjectAnimator.INFINITE
//            repeatMode = ObjectAnimator.REVERSE
//        }
//        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f).apply {
//            duration = 800
//            repeatCount = ObjectAnimator.INFINITE
//            repeatMode = ObjectAnimator.REVERSE
//        }
//
//        val animatorSet = AnimatorSet()
//        animatorSet.playTogether(scaleX, scaleY)
//        animatorSet.interpolator = AccelerateDecelerateInterpolator()
//        animatorSet.start()
//    }

//    override fun onPartCheck(part: Triple<String, String, Boolean>) {
//
//    }
    fun calculatePercentile(value: Double, mean: Double, stdDev: Double): Double {
        val normalDistribution = NormalDistribution(mean, stdDev)
        return normalDistribution.cumulativeProbability(value) * 100
    }
}