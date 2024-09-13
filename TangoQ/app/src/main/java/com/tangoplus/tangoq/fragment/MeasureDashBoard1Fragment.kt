package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
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
    val endTime = LocalDateTime.now()
//    val startTime = LocalDateTime.now().minusDays(1)
    private var popupWindow : PopupWindow?= null
    private var currentMonth = YearMonth.now()
    private var selectedDate = LocalDate.now()
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

//        binding.tvMsUserName.text = t_userdata?.optString("user_name")
//        binding.ibtnMsAlarm.setOnClickListener {
//            val intent = Intent(requireContext(), AlarmActivity::class.java)
//            startActivity(intent)
//        }

        when (isNetworkAvailable(requireContext())) {
            true -> {
                // ------! 뱃지 및 종합 접수 시작 !------
                setBadgeOnFlR()
                binding.tvMD1TotalScore.text = "${viewModel.selectedMeasure?.overall}"
                binding.tvMD1MeasureHistory.text = "최근 측정 기록 - ${viewModel.selectedMeasure?.regDate}"
                binding.tvMD1Name.text = "${userJson?.optString("user_name")}님의 기록"
                binding.clMD1PredictDicease.setOnClickListener{
                    val dialog = ReportDiseaseDialogFragment()
                    dialog.show(requireActivity().supportFragmentManager, "ReportDiseaseDialogFragment")
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

            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/png" // 이곳에서 공유 데이터 변경
            intent.putExtra(Intent.EXTRA_STREAM, fileUri)
            intent.putExtra(Intent.EXTRA_TEXT, "제 밸런스 그래프를 공유하고 싶어요 !")
            startActivity(Intent.createChooser(intent, "밸런스 그래프"))
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


//        // ------! 통증 부위 관리 시작 !------
//        binding.tvMsAddPart.setOnClickListener {
//            val dialog = MeasurePartDialogFragment()
//            dialog.show(requireActivity().supportFragmentManager, "MeasurePartDialogFragment")
//        }
//        binding.clMsAddPart.setOnClickListener {
//            val dialog = MeasurePartDialogFragment()
//            dialog.show(requireActivity().supportFragmentManager, "MeasurePartDialogFragment")
//        } // ------! 통증 부위 관리 끝 !------


//        // TODO 1. t_measure등에서 가져오는 결과값이 있다 --> 밸런스 점수, 뭐 측정일자 같은거 업데이트
//        // TODO 2. 일단 통증 부위를 일단 넣을 수 있게.
//        viewModel.parts.observe(viewLifecycleOwner) { parts ->
//            if (parts.isEmpty()) {
//                binding.llMsEmpty.visibility = View.VISIBLE
//                setPainPartRV(parts)
//
//            } else if (parts.size == 1) {
//                binding.llMsEmpty.visibility = View.GONE
//                binding.rvMsRight.visibility = View.GONE
//                (binding.rvMsLeft.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = 0
//                setPainPartRV(parts)
//            } else {
//                binding.llMsEmpty.visibility = View.GONE
//                binding.rvMsRight.visibility = View.VISIBLE
//                (binding.rvMsLeft.layoutParams as ViewGroup.MarginLayoutParams).rightMargin = 8
//
//                // ------! 왼쪽 painpart !------
//                val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
//                val leftData = parts.filterIndexed { index, _ -> index % 2 == 0 }.toMutableList()
//                val leftadapter = PainPartRVAdpater(this@MeasureHistory1Fragment, leftData, "Pp", this@MeasureHistory1Fragment)
//                binding.rvMsLeft.adapter = leftadapter
//                if (leftData.isNotEmpty()) {
//                    binding.rvMsLeft.layoutManager = linearLayoutManager
//                }
//
//                // ------! 오른쪽 painpart !------
//                val linearLayoutManager2 = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
//                val rightData = parts.filterIndexed { index, _ -> index % 2 == 1 }.toMutableList()
//                val rightAdapter = PainPartRVAdpater(this@MeasureHistory1Fragment, rightData, "Pp", this@MeasureHistory1Fragment)
//                binding.rvMsRight.adapter = rightAdapter
//                if (rightData.isNotEmpty()) {
//                    binding.rvMsRight.layoutManager = linearLayoutManager2
//                }
//            }
//        } // ------!  이름 + 통증 부위 끝 !------

        // ------! 리포트 버튼 시작 !------
//        startBounceAnimation(binding.btnMsGetReport)
//        binding.btnMsGetReport.setOnClickListener {
//            requireActivity().supportFragmentManager.beginTransaction().apply {
//                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
//                replace(R.id.flMain, ReportFragment())
//                commit()
//            }
//            requireContext()
//        }
//        // ------! 리포트 버튼 끝 !------
//
//        binding.btnMsGetRecommend.setOnClickListener {
//            // TODO partName에 대해서 가장 높은 점수인것만 가져오는 거지
//            /* 근데 말이 안되는게, ExerciseDetailFragment는 대분류로 움직이는 건데, 특정 dialogFragment안에서 추가적으로 어떤 것들만 볼 수 있게. 추천 운동 (전체화면 느낌)
//            * r*/
//            requireActivity().supportFragmentManager.beginTransaction().apply {
//                replace(R.id.flMain, ExerciseDetailFragment.newInstance(Pair(2, "기본 스트레칭 운동"), -1))
//                addToBackStack(null)
//                commit()
//            }
//
//        }

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
            lcDataList.add(Pair(measure.regDate, measure.overall!!))
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
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
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
                    val balloonText = if (selectedData.first != "") "측정날짜: ${selectedData.first}\n" + "점수: ${entry.y.toInt()}점" else "측정 기록이 없습니다."
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

                    Log.v("eeee", "e.x: ${e.x}, e.y: ${e.y}")
                }
                Log.v("eeee", "e.x: ${e?.x}, e.y: ${e?.y}")
            }
            override fun onNothingSelected() {}
        })
        // ------! 꺾은선 그래프 코드 끝 !------


//        val lineChartND = binding.lcMD1NS
//        val mean = 0.0
//        val stdDev = 1.0
//        val ndText = "70"
//        val zScore : Double
////        when (ndText) {
////            "60%" -> zScore = 0.842
////            "70%" -> zScore = 1.04
////            "80%" -> zScore = 1.28
////            "90%" -> zScore = 1.645
////        }
//        zScore = 1.04
//        val entries = ArrayList<Entry>()
//        val entriesHighlighted = ArrayList<Entry>()
//
//        // -------! 사용자 밸런스 점수의 백분위 값 !------
//        val userValue = 0.625
//
//
        // ------! balloon 시작 !------
        val params = binding.ivMD1Position.layoutParams as ConstraintLayout.LayoutParams
        params.horizontalBias = 0.731f
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


//    @SuppressLint("MissingInflatedId", "InflateParams")
//    override fun onPartCheck(part: MeasureVO) {
//        if (popupWindow?.isShowing == true) {
//            popupWindow?.dismiss()
//            popupWindow = null
//        } else {
//            val inflater = LayoutInflater.from(view?.context)
//            val popupView = inflater.inflate(R.layout.pw_main_item, null)
//            val width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 186f, view?.context?.resources?.displayMetrics).toInt()
//            val height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 162f, view?.context?.resources?.displayMetrics).toInt()
//
//            popupWindow = PopupWindow(popupView, width, height)
//            popupWindow!!.showAsDropDown(view)
//
//            popupView.findViewById<TextView>(R.id.tvPPP1).setOnClickListener{
//                requireActivity().supportFragmentManager.beginTransaction().apply {
//                    add(R.id.flMain, ReportDiseaseFragment())
//                    addToBackStack(null)
//                    commit()
//                }
//                popupWindow?.dismiss()
//            }
//            popupView.findViewById<TextView>(R.id.tvPPP2).setOnClickListener{
//                val dialog = PoseViewDialogFragment.newInstance(part.drawableName) // TODO drawableName이 아니라 실제 파일 String에 대한 값을 MeasureVO에 추가해야함
//                dialog.show(requireActivity().supportFragmentManager, "PoseViewDialogFragment")
//                popupWindow?.dismiss()
//            }
//            popupView.findViewById<TextView>(R.id.tvPPP3).setOnClickListener{
//                requireActivity().supportFragmentManager.beginTransaction().apply {
//                    add(R.id.flMain, MeasureHistoryFragment())
//                    addToBackStack(null)
//                    commit()
//                }
//                popupWindow?.dismiss()
//            }
//        }
//    }
//
//    private fun setPainPartRV(parts:  MutableList<MeasureVO>) {
//        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
//        val leftAdapter = PainPartRVAdpater(this@MeasureDashBoard1Fragment, parts, "Pp", this@MeasureDashBoard1Fragment)
////        binding.rvMsLeft.adapter = leftAdapter
////        binding.rvMsLeft.layoutManager = linearLayoutManager
//    }

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

    @OptIn(ExperimentalBadgeUtils::class)
    private fun setBadgeOnFlR() {
        // ------! 분석 뱃지 시작 !------
        val badgeDrawable = BadgeDrawable.create(requireContext()).apply {
            backgroundColor = ContextCompat.getColor(requireContext(), R.color.thirdColor)
            badgeGravity = BadgeDrawable.TOP_START
            horizontalOffset = 12  // 원하는 가로 간격 (픽셀 단위)
            verticalOffset = 12  // 원하는 세로 간격 (픽셀 단위)

        }

        val layoutParams = binding.tvMD1Badge.layoutParams as FrameLayout.LayoutParams
        layoutParams.marginStart = 20  // 오른쪽 마진
        layoutParams.topMargin = 20  // 위쪽 마진
        binding.tvMD1Badge.layoutParams = layoutParams

        // 뱃지를 View에 연결
        binding.flMD1.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            BadgeUtils.attachBadgeDrawable(badgeDrawable, binding.tvMD1Badge, binding.flMD1)
        } // ------! 분석 뱃지 끝 !------
    }

    private fun getCurrentMonthInKorean(month: YearMonth): String {
        return month.month.getDisplayName(TextStyle.FULL, Locale("ko"))
    }
}