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
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignTop
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureDashboard1Binding
import com.tangoplus.tangoq.dialog.MeasureTrendDialogFragment
import com.tangoplus.tangoq.dialog.ReportDiseaseDialogFragment
import com.tangoplus.tangoq.`object`.DeviceService.isNetworkAvailable
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class MeasureDashBoard1Fragment : Fragment() {
    lateinit var binding : FragmentMeasureDashboard1Binding
    val viewModel : MeasureViewModel by activityViewModels()
    private var balloon : Balloon? = null
    private var measures : MutableList<MeasureVO>? = null

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

        // ------# 싱글턴 패턴 객체 가져오기 #------
        val singletonMeasure = Singleton_t_measure.getInstance(requireContext())
        measures = singletonMeasure.measures
//        measures = mutableListOf()

        // ------!  이름 + 통증 부위 시작 !------
        val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject

        when (isNetworkAvailable(requireContext())) {
            true -> {
                try {
                    if (measures?.isNotEmpty() == true) {
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
            val url = Uri.parse("https://tangopluscompany.github.io/deep-link-redirect/#/2")
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/png" // 이곳에서 공유 데이터 변경
            intent.putExtra(Intent.EXTRA_STREAM, fileUri)
            intent.putExtra(Intent.EXTRA_TEXT, "제 측정 결과들을 공유하고 싶어요 !\n$url")
            startActivity(Intent.createChooser(intent, "측정 결과"))
        }

        // ------# 자세히 보기 #------
        binding.tvMD1More1.setOnClickListener {
            val dialog = MeasureTrendDialogFragment.newInstance("", "")
            dialog.show(requireActivity().supportFragmentManager, "MeasureTrendDialogFragment")

//            requireActivity().supportFragmentManager.beginTransaction().apply {
//                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
//                replace(R.id.flMain, MeasureHistoryFragment())
//                addToBackStack(null)
//                commit()
//            }
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

        measures?.let { measure ->
            if (measures?.size != 7) {
                for (i in 0 until (7 - measures?.size!!)) {
                    lcDataList.add(Pair("", 0))
                }
            }
            if (!measures.isNullOrEmpty()) {
                for (i in measures!!.size - 1 downTo 0) {
                    val measure = measures!![i]
                    lcDataList.add(Pair(measure.regDate, measure.overall?.toInt()!!))
                }
                Log.v("lcDataList", "${lcDataList}")
                for (i in lcDataList.indices) {
                    startIndex = i
                    break
                }
            } else {
                for (i in 6 downTo 0) {
                    lcDataList.add(Pair("", 0))
                }
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
            var percentage = 0.5f
            if (measures!!.size >= 1) {
                val userPercentile = measures!![0].overall
                percentage = calculatePercentage(userPercentile?.toInt()!!)
            }

            val params = binding.ivMD1Position.layoutParams as ConstraintLayout.LayoutParams
            params.horizontalBias = percentage
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
            binding.clMD1Percent.setOnClickListener {
                balloon!!.dismissWithDelay(2500L)
            }
            binding.ivMD1Position.setOnClickListener{
                binding.ivMD1Position.showAlignTop(balloon!!)
            }
        }

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
    fun calculatePercentage(value: Int): Float {
        // 70~100의 범위를 0~100%로 매핑
        val minInput = 50
        val maxInput = 100
        val percentage = (value - minInput).toDouble() / (maxInput - minInput)

        // 소수점 두 자리까지 반올림
        return String.format("%.3f", percentage).toFloat()
    }
}