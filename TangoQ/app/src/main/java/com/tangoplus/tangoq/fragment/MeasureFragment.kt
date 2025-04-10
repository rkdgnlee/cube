package com.tangoplus.tangoq.fragment

import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignTop
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.api.DeviceService.isNetworkAvailable
import com.tangoplus.tangoq.databinding.FragmentMeasureBinding
import com.tangoplus.tangoq.db.Singleton_t_measure
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.dialog.LoadingDialogFragment
import com.tangoplus.tangoq.dialog.MeasureTrendDialogFragment
import com.tangoplus.tangoq.dialog.QRCodeDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.hideBadgeOnClick
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.function.SaveSingletonManager
import com.tangoplus.tangoq.function.WifiManager
import com.tangoplus.tangoq.listener.OnSingleClickListener
import com.tangoplus.tangoq.mediapipe.MathHelpers.isTablet
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.vo.DateDisplay
import com.tangoplus.tangoq.vo.MeasureVO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MeasureFragment : Fragment() {
    lateinit var binding : FragmentMeasureBinding
    val mvm : MeasureViewModel by activityViewModels()
    private var balloon : Balloon? = null
    private var measures : MutableList<MeasureVO>? = null
    private lateinit var ssm : SaveSingletonManager
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasureBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ibtnMsAlarm.setOnSingleClickListener {
            val dialog = AlarmDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "AlarmDialogFragment")
        }

        binding.ibtnMsQRCode.setOnSingleClickListener {
            val dialog = QRCodeDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "LoginScanDialogFragment")
        }
        val dateTvs = listOf(binding.tvM1,binding.tvM2,binding.tvM3,binding.tvM4,binding.tvM5 )
        val scoreTvs = listOf(binding.tvM6,binding.tvM7,binding.tvM8,binding.tvM9,binding.tvM10 )
        dateTvs.forEachIndexed { index, tv->
            tv.setOnClickListener {
                mvm.previousMeasureIndex = mvm.selectedMeasureIndex.value ?: 0
                mvm.selectedMeasureIndex.value = index
            }
        }
        scoreTvs.forEachIndexed { index, tv ->
            tv.setOnClickListener {
                mvm.previousMeasureIndex = mvm.selectedMeasureIndex.value ?: 0
                mvm.selectedMeasureIndex.value = index
            }
        }

        binding.tvMScoreGuide.setOnSingleClickListener {
            val balloonGuide = Balloon.Builder(requireContext())
                .setWidthRatio(0.6f)
                .setHeight(BalloonSizeSpec.WRAP)
                .setText("지난 5개의 측정 지표인 종합 점수의 흐름을 판단하는 그래프입니다.\n지난 측정과 자세를 비교해보세요")
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
            balloonGuide.showAlignEnd(binding.tvMScoreGuide)
        }

        // ------# 싱글턴 패턴 객체 가져오기 #------
        val singletonMeasure = Singleton_t_measure.getInstance(requireContext())
        measures = singletonMeasure.measures ?: mutableListOf()

        // ------!  이름 + 통증 부위 시작 !------
        val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject

        when (isNetworkAvailable(requireContext())) {
            true -> {
                try {
                    binding.btnMStart.setOnClickListener {
                        (activity as? MainActivity)?.launchMeasureSkeletonActivity()
                    }
                    if (measures?.isNotEmpty() == true) {
//                        val hideBadgeFunction = hideBadgeOnClick(
//                            binding.tvMBadge,
//                            binding.llMPredictDisease,
//                            "${binding.tvMBadge.text}",
//                            ContextCompat.getColor(requireContext(), R.color.thirdColor)
//                        )
                        binding.tvMEmptyGraph.visibility = View.GONE
                        val historyInitString = "최근 측정 기록: ${measures?.get(0)?.regDate?.substring(0, 10)}" // ?.replace("-", ". ")
                        binding.tvMMeasureHistory.text =historyInitString
                        val userString = "${userJson?.optString("user_name")}님의 기록"
                        binding.tvMName.text = userString
                        binding.llMPredictDisease.setOnSingleClickListener {
                            val dialog = LoadingDialogFragment.newInstance("측정파일")
                            dialog.show(activity?.supportFragmentManager ?: return@setOnSingleClickListener, "LoadingDialogFragment")

//                            hideBadgeFunction?.invoke()
                            ssm = SaveSingletonManager(requireContext(), requireActivity())
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val measureIndex = 4 - (mvm.selectedMeasureIndex.value ?: 0)
                                    val currentMeasure = singletonMeasure.measures?.getOrNull(measureIndex)
                                    val uriTuples = currentMeasure?.sn?.let { it -> ssm.get1MeasureUrls(it) }
                                    if (uriTuples != null) {
                                        ssm.downloadFiles(uriTuples)
                                        val editedMeasure = ssm.insertUrlToMeasureVO(uriTuples, currentMeasure)
                                        // singleton의 인덱스 찾아서 ja와 값 넣기
                                        val singletonIndex = singletonMeasure.measures?.indexOfLast { it.regDate == currentMeasure.regDate }
                                        if (singletonIndex != null && singletonIndex >= 0) {
                                            withContext(Dispatchers.Main) {
                                                singletonMeasure.measures?.set(singletonIndex, editedMeasure)
                                                mvm.selectedMeasure = editedMeasure
                                                mvm.selectedMeasureDate.value = DateDisplay(currentMeasure.regDate, currentMeasure.regDate.substring(0, 11))
                                                mvm.selectMeasureDate.value = DateDisplay(currentMeasure.regDate, currentMeasure.regDate.substring(0, 11))

                                                Log.v("수정완료", "index: $singletonIndex, rec: ${editedMeasure.recommendations?.map { it.createdAt }}")
                                                requireActivity().supportFragmentManager.beginTransaction().apply {
                                                    replace(R.id.flMain, MeasureDetailFragment())
                                                    commit()
                                                }
                                                dialog.dismiss()
                                            }
                                        }
                                    }
                                } catch (e: IllegalStateException) {
                                    Log.e("measureSelectError", "MeasureBSIllegalState: ${e.message}")
                                } catch (e: IllegalArgumentException) {
                                    Log.e("measureSelectError", "MeasureBSIllegalArgument: ${e.message}")
                                } catch (e: NullPointerException) {
                                    Log.e("measureSelectError", "MeasureBSNullPointer: ${e.message}")
                                } catch (e: InterruptedException) {
                                    Log.e("measureSelectError", "MeasureBSInterrupted: ${e.message}")
                                } catch (e: IndexOutOfBoundsException) {
                                    Log.e("measureSelectError", "MeasureBSIndexOutOfBounds: ${e.message}")
                                } catch (e: Exception) {
                                    Log.e("measureSelectError", "MeasureBS: ${e.message}")
                                } finally {
                                    withContext(Dispatchers.Main) {
                                        if (dialog.isAdded && dialog.isVisible) {
                                            dialog.dismiss()
                                        }
                                    }
                                }
                            }
                        }
                        binding.tvM1Trend.isEnabled = true
                    } else {
                        binding.tvMEmptyGraph.visibility = View.VISIBLE
                        binding.tvMTotalScore.text = "-"
                        binding.tvMMeasureHistory.text = "측정기록없음"
                        binding.tvMName.text = "${userJson?.optString("user_name")}님의 기록"
                        binding.tvMDuration.text = "측정 기록 없음"
                        val params =
                            binding.ivMPosition.layoutParams as ConstraintLayout.LayoutParams
                        params.horizontalBias = 0.5f
                        binding.ivMPosition.layoutParams = params
                        binding.btnM2.isEnabled = false
                        binding.tvM1Trend.isEnabled = false
                    }
                } catch (e: ClassNotFoundException) {
                    Log.e("MError", "ClassNotFound: ${e.message}")
                } catch (e: NullPointerException) {
                    Log.e("MError", "NullPointer: ${e.message}")
                } catch (e: IllegalStateException) {
                    Log.e("MError", "IllegalState: ${e.message}")
                } catch (e: IllegalArgumentException) {
                    Log.e("MError", "IllegalArgument: ${e.message}")
                } catch (e: Exception) {
                    Log.e("MError", "Exception: ${e.message}")
                }
            }
            false -> {

            }
        }

        binding.btnM2.setOnSingleClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
                replace(R.id.flMain, MeasureHistoryFragment())
                commit()
            }
        }
        // ------# 자세히 보기 #------
        binding.tvM1Trend.setOnSingleClickListener {
            if (WifiManager(requireContext()).checkNetworkType() != "NONE") {
                val dialog = MeasureTrendDialogFragment()
                dialog.show(requireActivity().supportFragmentManager, "MeasureTrendDialogFragment")
            } else {
                Toast.makeText(requireContext(), "인터넷 연결이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }

        // ------! 꺾은선 그래프 시작 !------
        val lineChart = binding.lcM
        val lcXAxis = lineChart.xAxis
        val lcYAxisLeft = lineChart.axisLeft
        val lcYAxisRight = lineChart.axisRight
        val lcLegend = lineChart.legend
        var startIndex = 0

        val lcDataList: MutableList<Pair<String, Int>> = mutableListOf()

        // measures 꺾은선 그래프
        measures?.let {
            val measureSize = measures?.size
//            Log.v("measureSize", "$measureSize")
            if (!measures.isNullOrEmpty()) {
                // 조건 처리 1 총 measures가 7 이하.
                if (measureSize == 0) {
                    for (i in 4 downTo 0) {
                        lcDataList.add(Pair("", 50))
                    }

                } else if (measureSize != null && measureSize <= 5 && measureSize >= 1) {
                    for (i in 0 until (5 - measureSize)) {
                        lcDataList.add(Pair("", 50))
                    }
                    for (i in measureSize - 1 downTo 0) {
                        val measureUnit = measures?.get(i)
                        val regDate = measureUnit?.regDate
                        val overall = measureUnit?.overall?.toInt()
                        if (regDate != null && overall != null) {
                            lcDataList.add(Pair(regDate, overall))
                        }
                    }
                } else {
                    for (i in 4 downTo 0) {
                        val measureUnit = measures?.get(i)
                        val regDate = measureUnit?.regDate
                        val overall = measureUnit?.overall?.toInt()
                        if (regDate != null && overall != null) {
                            lcDataList.add(Pair(regDate, overall))
                        }
                    }
//                    Log.v("lcDataList", "$lcDataList")
                    for (i in lcDataList.indices) {
                        startIndex = i
                        break
                    }
                }
            } else {
                for (i in 4 downTo 0) {
                    lcDataList.add(Pair("", 50))
                }
            }
            // 선택시 값을 움직일 index 저장
            if (mvm.selectedMeasureIndex.value == null) {
                mvm.selectedMeasureIndex.value = 4
            }

            val lcEntries: MutableList<Entry> = mutableListOf()
            for (i in startIndex until lcDataList.size) {
                lcEntries.add(Entry((i - startIndex).toFloat(), lcDataList[i].second.toFloat()))
            }
            // lcdatalist 5개
//            Log.v("lcDataList", "$lcDataList")
            val lcLineDataSet = LineDataSet(lcEntries, "")
            lcLineDataSet.apply {
                color = resources.getColor(R.color.thirdColor, null)
                circleRadius = 4F
                lineWidth = 4F
                valueTextSize = 0F
                setCircleColors(resources.getColor(R.color.thirdColor, null))
                circleRadius = 5f
                setDrawCircleHole(false)
                setDrawFilled(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            lcXAxis.apply {
                isEnabled = false
                textSize = 14f
                textColor = resources.getColor(R.color.subColor500, null)
                labelRotationAngle = 2F
                setDrawAxisLine(false)
                setDrawGridLines(false)

                setLabelCount(lcDataList.size, true)
                position = XAxis.XAxisPosition.BOTTOM
                axisLineWidth = 1.0f
            }
            lcYAxisLeft.apply {
                axisMinimum = 45f
                axisMaximum = 100f
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
                if (isTablet(requireContext())) setExtraOffsets(22f, 0f ,22f ,0f)
                data = LineData(lcLineDataSet)
//                animateX(1000, Easing.EaseInOutBack)
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
//                animateChart(lineChart, lcLineDataSet, lcEntries)
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

                binding.tvMDuration.text = if (oldestDate != null && newestDate != null) {
                    "${oldestDate.second.format(outputFormatter)} ~ ${
                        newestDate.second.format(
                            outputFormatter
                        )
                    }"
                } else {
                    "측정 기록 없음"
                }
            }
            // ------! 날짜 기간 가져오기 끝 !------

            // ------! 값 클릭 시 벌룬 나오기 시작 !------+
            setScoresDates(lcDataList)

            lineChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    e?.let { entry ->
                        val originalIndex = startIndex + entry.x.toInt()
                        val selectedData = lcDataList[originalIndex]
                        val balloonText = if (selectedData.first != "") "측정날짜: ${
                            selectedData.first.substring(
                                0,
                                10
                            )
                        }\n" + "점수: ${entry.y.toInt()}점" else "측정 기록이 없습니다."
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
//                        Log.v("originalIndex", "$originalIndex")
                        if ( selectedData.second > 50) {
                            mvm.previousMeasureIndex = mvm.selectedMeasureIndex.value ?: 0
                            mvm.selectedMeasureIndex.value = originalIndex
                        }
                    }

                }

                override fun onNothingSelected() {}
            })
            // ------! 꺾은선 그래프 코드 끝 !------

            // ------! balloon 시작 !------


            var percentage = 0.5f
            mvm.selectedMeasureIndex.observe(viewLifecycleOwner) {

                val measuresSize = measures?.size
                if (measuresSize != null && measuresSize >= 1) {
                    val userPercentile = measures?.get(4 - it)?.overall?.toInt() ?: 0 // index가 4, 3, 2, 1, 0으로 들어감.

                    percentage = calculatePercentage(userPercentile)
//                    Log.v("userPercentile", "$userPercentile, $percentage")
                    // 상단 평균 분포 움직이기
                    when (percentage) {
                        in 0f..0.33f -> {
                            binding.vMMiddle.visibility = View.INVISIBLE
                            binding.vMLow.visibility = View.VISIBLE
                            binding.vMHigh.visibility = View.INVISIBLE
                        }

                        in 0.66f..1.0f -> {
                            binding.vMMiddle.visibility = View.INVISIBLE
                            binding.vMLow.visibility = View.GONE
                            binding.vMHigh.visibility = View.VISIBLE
                        }

                        else -> {
                            binding.vMMiddle.visibility = View.VISIBLE
                            binding.vMHigh.visibility = View.GONE
                            binding.vMLow.visibility = View.GONE
                        }
                    }
                    animateArrowToPercentage(percentage)
                    createBalloon(userJson, percentage)

                    // 종합점수와 텍스트 변경
                    val historyText = if (it == 4) {
                        "최근 측정 기록: "
                    } else {
                        "선택된 날짜: "
                    } + "${measures?.get(4 - it)?.regDate?.substring(0, 10)}" // ?.replace("-", ". ")
                    binding.tvMMeasureHistory.text = historyText
                    binding.tvMTotalScore.text = userPercentile.toString()


                }
                animateCardViewToPercentage(it)
            }

            binding.clMPercent.setOnClickListener {
                balloon?.let { it1 -> binding.ivMPosition.showAlignTop(it1) }
            }
            binding.ivMPosition.setOnClickListener {
                balloon?.let { it1 -> binding.ivMPosition.showAlignTop(it1) }
            }
        }
    }
    // ------! 추천 운동 받기 끝!------
    private fun createBalloon(userJson: JSONObject?, percent: Float) {
        balloon = Balloon.Builder(requireContext())
            .setWidthRatio(0.6f)
            .setHeight(BalloonSizeSpec.WRAP)
            .setText("${userJson?.optString("user_name")}님은 평균 백분위\n${if (percent >= 0.5f) "${((1.0f - percent) * 100).toInt()}" else "${(percent * 100).toInt()}"}%에 위치합니다.")
            .setTextColorResource(R.color.whiteText)
            .setTextSize(15f)
            .setArrowPositionRules(ArrowPositionRules.ALIGN_BALLOON)
            .setArrowSize(0)
            .setMargin(4)
            .setPadding(8)
            .setCornerRadius(16f)
            .setBackgroundColorResource(when (percent) {
                in 0f..0.3f -> {R.color.deleteColor}
                else -> {R.color.thirdColor}
            })
            .setOnBalloonClickListener { balloon?.dismiss() }
            .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()
    }

    private fun calculatePercentage(value: Int?): Float {
        val minInput = 40
        val maxInput = 100
        val percentage = ((value?.minus(minInput))?.toDouble() ?: 0.0) / (maxInput - minInput)

        // 소수점 두 자리까지 반올림
        return String.format("%.3f", percentage).toFloat()
    }

    private fun animateArrowToPercentage(percent: Float) {
        val params = binding.ivMPosition.layoutParams as ConstraintLayout.LayoutParams
        // 현재 horizontalBias
        val startBias = params.horizontalBias
        val endBias = when (percent) {
            in 0f .. 0.33f -> 0.135f
            in 0.33f .. 0.66f -> 0.5f
            in 0.66f .. 1f -> 0.875f
            else -> 0.5f
        } // 이동할 목표 위치

        val tvCase = when (percent) {
            in 0f .. 0.33f -> 0
            in 0.33f .. 0.66f -> 1
            in 0.66f .. 1f -> 2
            else -> -1
        } // 이동할 목표 위치

        val percentList = listOf(binding.tvMLow, binding.tvMMiddle, binding.tvMHigh)
        if (tvCase != -1) {
            percentList.forEachIndexed { index, tv ->
                if (index == tvCase) tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.subColor800))
                else tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.subColor400))
            }
        }
        // ValueAnimator 생성
        val animator = ValueAnimator.ofFloat(startBias, endBias).apply {
            duration = 1000L // 1초 동안 애니메이션
            interpolator = AccelerateDecelerateInterpolator() // 가속/감속 효과

            addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Float
                params.horizontalBias = animatedValue
                binding.ivMPosition.layoutParams = params
            }
        }
        animator.start()
    }

    private fun animateCardViewToPercentage(index: Int) {
        val params = binding.cvM.layoutParams as ConstraintLayout.LayoutParams
        Log.v("눌렀을 때", "${mvm.previousMeasureIndex}, ${mvm.selectedMeasureIndex.value}")
        val startBias = when(mvm.previousMeasureIndex) {
            4 -> 1.0f
            3 -> 0.75f
            1 -> 0.25f
            0 -> 0.0f
            else -> 0.5f
        }
        val endBias = when (index) {
            4 -> 1.0f
            3 -> 0.75f
            1 -> 0.25f
            0 -> 0.0f
            else -> 0.5f
        } // 이동할 목표 위치

        val durations = when (index) {
            1, 3 -> 750L
            else -> 1000L
        }
        val animator = ValueAnimator.ofFloat(startBias, endBias).apply {
            duration = durations // 1초 동안 애니메이션
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Float
                params.horizontalBias = animatedValue
                binding.cvM.layoutParams = params
            }
        }
        animator.start()
    }

    private fun setScoresDates(entries:  MutableList<Pair<String, Int>>) {
        val tvMDates = listOf(binding.tvM1, binding.tvM2, binding.tvM3, binding.tvM4, binding.tvM5)
        val tvMScores = listOf(binding.tvM6, binding.tvM7, binding.tvM8, binding.tvM9, binding.tvM10)
        for (i in 0 until 5) {
            val selectedData = entries[i]
            if (selectedData.first != "") {
                tvMDates[i].text = selectedData.first.substring(5, 10).replace("-",".")
            } else {
                val bodyText =  "-"
                tvMDates[i].text =bodyText
            }

            if (selectedData.first != "") {
                tvMScores[i].text = "${selectedData.second}"

            } else {
                val bodyText =  "-"
                tvMScores[i].text =bodyText
            }

//            tvMDates[i].textSize = if (isTablet(requireContext())) 18f else 15f
        }
    }
}