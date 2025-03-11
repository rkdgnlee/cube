package com.tangoplus.tangoq.fragment

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.gms.common.util.DeviceProperties.isTablet
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.yearMonth
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.ProgressHistoryRVAdapter
import com.tangoplus.tangoq.vo.ProgressHistoryVO
import com.tangoplus.tangoq.viewmodel.ProgressViewModel
import com.tangoplus.tangoq.api.NetworkProgress.getDailyProgress
import com.tangoplus.tangoq.api.NetworkProgress.getLatestProgresses
import com.tangoplus.tangoq.api.NetworkProgress.getMonthProgress
import com.tangoplus.tangoq.api.NetworkProgress.getWeekProgress
import com.tangoplus.tangoq.databinding.FragmentAnalyzeBinding
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.dialog.ProgramCustomDialogFragment
import com.tangoplus.tangoq.dialog.QRCodeDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.hideBadgeOnClick
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.listener.OnSingleClickListener
import com.tangoplus.tangoq.view.BarChartRender
import com.tangoplus.tangoq.view.DayViewContainer
import com.tangoplus.tangoq.view.MonthHeaderViewContainer
import com.tangoplus.tangoq.viewmodel.AnalysisViewModel
import com.tangoplus.tangoq.viewmodel.ExerciseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale


class AnalyzeFragment : Fragment() {
    lateinit var binding : FragmentAnalyzeBinding
    var currentMonth = YearMonth.now()
    private val avm : AnalysisViewModel by activityViewModels()
    private val pvm : ProgressViewModel by activityViewModels()
    private val evm : ExerciseViewModel by activityViewModels()
    private lateinit var  todayInWeek : List<Int>

    override fun onStop() {
        super.onStop()
        pvm.selectedDailyTime.value = 0
        pvm.selectedDailyCount.value = 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAnalyzeBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ------# 운동 기록 API 공간 #------
        todayInWeek = sortTodayInWeek()
        pvm.selectedDate = LocalDate.now()

        // 클릭 리스너 달기
        binding.ibtnAAlarm.setOnSingleClickListener {
            val dialog = AlarmDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "AlarmDialogFragment")
        }

        binding.ibtnAQRCode.setOnSingleClickListener {
            val dialog = QRCodeDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "LoginScanDialogFragment")
        }
        val btns = listOf(binding.vA1, binding.vA2, binding.vA3, binding.vA4, binding.vA5, binding.vA6, binding.vA7)
        btns.forEachIndexed { index, view ->
            view.setOnClickListener {

                showDailyProgress(index)
                animateCardViewToPercentage( index)
                Handler(Looper.getMainLooper()).postDelayed({
                    scrollToView(binding.rvA)
                }, 400)
            }
        }

        // ------# 그래프에 들어갈 가장 최근 일주일간 수치 넣기 #------
        lifecycleScope.launch(Dispatchers.Main) {
            setShimmer(true)
            if (pvm.graphProgresses.isNullOrEmpty()) {
                pvm.graphProgresses = getWeekProgress(getString(R.string.API_progress), requireContext())
            }
            setGraph()
            setShimmer(false)
//            if (pvm.graphProgresses != null) {
//                setGraph()
//            } else {
//
//                setShimmer(false)
//            }
            // 상단 프로그레스 받아오기
            val progressResult = getLatestProgresses(getString(R.string.API_progress), requireContext())
            if (progressResult != null) {
                Log.v("progressResult", "${progressResult.first}")
                evm.latestUVP = progressResult.first // .sortedBy { it.uvpSn }.toMutableList()
                evm.latestProgram = progressResult.second
//            Log.v("latestUVP", "${evm.latestUVP}, ${evm.latestProgram}")
                if (!evm.latestUVP.isNullOrEmpty() && evm.latestProgram != null) {
                    binding.tvAProgressGuide.visibility = View.VISIBLE
                    binding.cvAProgress.visibility = View.VISIBLE
                } else {
                    binding.tvAProgressGuide.visibility = View.GONE
                    binding.cvAProgress.visibility = View.GONE
                }
//                val currentIndex = findCurrentIndex(evm.latestUVP)
                if (!evm.latestUVP.isNullOrEmpty()) {
                    val currentEId = evm.latestUVP?.get(0)?.exerciseId
                    val currentExerciseItem = evm.latestProgram?.exercises?.find { it.exerciseId?.toInt() == currentEId }
                    val second = "${currentExerciseItem?.duration?.toInt()?.div(60)}분 ${currentExerciseItem?.duration?.toInt()?.rem(60)}초"

                    // 받아온 데이터로 cvEProgress 채우기
                    Glide.with(requireContext())
                        .load("${currentExerciseItem?.imageFilePath}")
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .override(180)
                        .into(binding.ivAThumbnail)
                    binding.tvAExerciseName.text = currentExerciseItem?.exerciseName
                    binding.tvAExerciseTime.text = second

                    when (currentExerciseItem?.exerciseStage) {
                        "초급" -> {
                            binding.ivAExerciseStage.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_stage_1))
                            binding.tvAExerciseStage.text = "초급자"
                        }
                        "중급" -> {
                            binding.ivAExerciseStage.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_stage_2))
                            binding.tvAExerciseStage.text = "중급자"
                        }
                        "고급" -> {
                            binding.ivAExerciseStage.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_stage_3))
                            binding.tvAExerciseStage.text = "상급자"
                        }
                    }
                    val evpItem = evm.latestUVP?.find { it.exerciseId == currentExerciseItem?.exerciseId?.toInt() }
                    evpItem.let {
                        if (it != null) {
                            binding.hpvA.progress = (it.progress * 100 / it.duration).toFloat()
                        }
                    }
                    binding.cvAProgress.setOnSingleClickListener {
                        val programSn = evm.latestProgram?.programSn ?: -1
                        val recSn = evm.latestUVP?.get(0)?.recommendationSn ?: -1
                        ProgramCustomDialogFragment.newInstance(programSn, recSn)
                            .show(requireActivity().supportFragmentManager, "ProgramCustomDialogFragment")
                    }
//                Log.v("현재날짜", "${currentMonth.year}-${String.format("%02d", currentMonth.monthValue)}")
                    updateMonthProgress("${currentMonth.year}-${String.format("%02d", currentMonth.monthValue)}")
                    avm.existedMonthProgresses.collectLatest { dates ->
                        binding.cvACalendar.notifyCalendarChanged()
                    }
                }
                setShimmer(false)
            } else {
                setGraph()
                setShimmer(false)
                binding.tvAProgressGuide.visibility = View.GONE
                binding.cvAProgress.visibility = View.GONE
            }
        }

        // ------! calendar 시작 !------
        binding.monthText.text = "${YearMonth.now().year}월 ${getCurrentMonthInKorean(currentMonth)}"

        // ------# 날짜와 운동 기록 보여주기 #------
        binding.tvADate.text = "${pvm.selectedDate?.year}년 ${getCurrentMonthInKorean(pvm.selectedDate?.yearMonth)} ${getCurrentDayInKorean(pvm.selectedDate)} 운동 정보"

        pvm.selectedDailyTime.observe(viewLifecycleOwner) {
            binding.tvADailyTime.text = if (it != null) {
                "${it.div(60)}분 ${it.rem(60) ?: 0}초"
            } else {
                "0분 0초"
            }
        }

        pvm.selectedDailyCount.observe(viewLifecycleOwner) {
            binding.tvADailyCount.text = if (it != null) {
                "${it}개"
            } else {
                "0개"
            }
        }
        binding.cvACalendar.apply {
            setup(currentMonth.minusMonths(24), currentMonth.plusMonths(0), DayOfWeek.SUNDAY)
            scrollToMonth(currentMonth)

            monthScrollListener = { month ->
                currentMonth = month.yearMonth
                binding.monthText.text = "${currentMonth.year}년 ${getCurrentMonthInKorean(currentMonth)}"
            }
            // ------# 월별로 운동 기록 필터링 #------
//            val filteredExercises = viewModel.allHistorys.filter { history ->
//                val historyDate = stringToLocalDate(history.regDate)
//                historyDate.month == currentMonth?.month && historyDate.year == currentMonth?.year
//            }.toMutableList()
//            setAdapter(filteredExercises)
        }

        binding.nextMonthButton.setOnClickListener {
            // 선택된 날짜 초기화
            initMonthData()
            if (currentMonth != YearMonth.now()) {
                currentMonth = currentMonth.plusMonths(1)
                updateMonthView()
                updateMonthProgress("${currentMonth.year}-${String.format("%02d", currentMonth.monthValue)}")
                setAdapter(listOf())
                pvm.selectedDate = null
            }
        }

        binding.previousMonthButton.setOnClickListener {
            // 선택된 날짜 초기화
            initMonthData()
            if (currentMonth > YearMonth.now().minusMonths(12)) {
                currentMonth = currentMonth.minusMonths(1)
                updateMonthView()
                updateMonthProgress("${currentMonth.year}-${String.format("%02d", currentMonth.monthValue)}")
                setAdapter(listOf())
                pvm.selectedDate = null
            }
        }

        binding.monthText.setOnClickListener {
            initMonthData()
            updateMonthProgress("${currentMonth.year}-${String.format("%02d", currentMonth.monthValue)}")
        }

        // ------# 운동 기록 날짜 받아오기 #------
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        showDailyProgress(6)

        binding.cvACalendar.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthHeaderViewContainer> {
            override fun create(view: View) = MonthHeaderViewContainer(view)
            override fun bind(container: MonthHeaderViewContainer, data: CalendarMonth) {
                container.tvSUN.text = "일"
                container.tvMON.text = "월"
                container.tvTUE.text = "화"
                container.tvWEN.text = "수"
                container.tvTUR.text = "목"
                container.tvFRI.text = "금"
                container.tvSAT.text = "토"
            }
        }

        binding.cvACalendar.dayBinder = object : MonthDayBinder<DayViewContainer> {
            @SuppressLint("UseCompatLoadingForDrawables")
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.date.apply {
                    if (day.date.month == currentMonth.month) {
                        text = day.date.dayOfMonth.toString()
                        textSize = if (isTablet(requireContext())) 24f else 20f
                        when (day.date.dayOfMonth) {
                            in 2 .. 9 -> {
                                setPadding(32, 16, 32, 16)
                            }
                            in 12 .. 19 -> {
                                setPadding(26, 20, 26, 20)
                            }
                            in 22 .. 29 -> {
                                setPadding(26, 23, 26, 23)
                            }
                            30 -> {
                                setPadding(24, 24, 24, 24)
                            }
                            11, 21, 31 -> {
                                setPadding(28, 22, 28, 22)
                            }
                            1 -> {
                                setPadding(36, 14, 36, 14)
                            }
                            else -> {
                                setPadding(24)
                            }
                        }
                        setDateStyle(container, day)
                        container.date.setOnClickListener {
                            // 선택된 날짜를 업데이트 + UI 갱신
                            if (day.date <= LocalDate.now() ) {
                                val oldDate = pvm.selectedDate
                                pvm.selectedDate = day.date
                                oldDate?.let { binding.cvACalendar.notifyDateChanged(it) }
                                pvm.selectedDate?.let { binding.cvACalendar.notifyDateChanged(it) }
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
//                                        Log.v("selectedDate", "${pvm.selectedDate}")
                                        pvm.selectedDate?.let { selectedDate ->
                                            val date = selectedDate.format(formatter)
//                                            Log.v("date", date)
                                            val currentProgresses = getDailyProgress(getString(R.string.API_progress), date, requireContext())
                                            withContext(Dispatchers.Main) {
                                                if (!currentProgresses.isNullOrEmpty()) {
                                                    setAdapter(currentProgresses)
                                                } else {
                                                    setAdapter(listOf())
                                                }
                                                binding.tvADate.text = "${selectedDate.year}년 ${getCurrentMonthInKorean(selectedDate.yearMonth)} ${getCurrentDayInKorean(selectedDate)} 운동 정보"
                                            }
                                        } ?: run {
//                                            Log.e("DateSelection", "Selected date is null")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        setDateStyle(container, day)
                        text = day.date.dayOfMonth.toString()
                        textSize = 20f
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.subColor200))
                        setOnClickListener { null }
                    }

                }
            }
            override fun create(view: View): DayViewContainer {
                return DayViewContainer(view)
            }
        } // ------! calendar 끝 !------

        binding.btnAExercise.setOnSingleClickListener {
            if (evm.latestUVP != null) {
                val programSn = evm.latestProgram?.programSn ?: -1
                val recSn = evm.latestUVP?.get(0)?.recommendationSn ?: -1
                ProgramCustomDialogFragment.newInstance(programSn, recSn)
                    .show(requireActivity().supportFragmentManager, "ProgramCustomDialogFragment")
            } else {
                MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                    setTitle("알림")
                    setMessage("프로그램 기록이 없습니다.\n측정으로 이동합니다.")
                    setPositiveButton("확인", { _ , _ ->
                        (activity as MainActivity).launchMeasureSkeletonActivity()
                    })
                    setNegativeButton("취소", {_, _ ->

                    })
                }.show()
            }
        }
    }

    private fun calculatePercent(count: Int?) : Float {
        if (count != null) {
            return when {
                count >= 7  -> 100f
                count == 0 -> 1f
                else -> (count * 100) / 7f
            }
        }
        return 1f
    }

    private fun setDateStyle(container: DayViewContainer, day: CalendarDay) {
        container.date.background = null
        container.removeBadge()

        when {
            avm.existedMonthProgresses.value.contains(day.date.toString()) -> {
                if (day.date == pvm.selectedDate) {
                    container.date.setTextColor(ContextCompat.getColor(container.date.context, R.color.whiteText))
                    container.date.background = ResourcesCompat.getDrawable(resources, R.drawable.bckgnd_oval, null)
                    container.removeBadge()
                } else {
                    container.date.setTextColor(ContextCompat.getColor(container.date.context, R.color.thirdColor))
                    container.addBadge()
                }
            }
            day.date == pvm.selectedDate -> {
//                Log.v("현재날짜", "${day.date}, ${pvm.selectedDate}")
                container.date.setTextColor(ContextCompat.getColor(container.date.context, R.color.whiteText))
                container.date.background = ResourcesCompat.getDrawable(resources, R.drawable.bckgnd_oval, null)
            }
            day.date == LocalDate.now() -> {
                container.date.setTextColor(ContextCompat.getColor(container.date.context, R.color.subColor800))
                // 현재 날짜에 대한 특별한 배경이 필요하다면 여기에 추가
            }
            pvm.datesClassifiedByDay.contains(day.date) -> {
                container.date.setTextColor(ContextCompat.getColor(container.date.context, R.color.thirdColor))

            }
            day.position == DayPosition.MonthDate -> {
                container.date.setTextColor(ContextCompat.getColor(container.date.context, R.color.subColor500))
            }
            else -> {
                container.date.setTextColor(ContextCompat.getColor(container.date.context, R.color.subColor150))
            }
        }
    }

    private fun getCurrentMonthInKorean(month: YearMonth?): String {
        return month?.month?.getDisplayName(TextStyle.FULL, Locale("ko")) ?: "1월"
    }

    private fun getCurrentDayInKorean(date: LocalDate?): String {
        return "${date?.dayOfMonth ?: 1}일"
    }

    private fun setAdapter(progresses: List<ProgressHistoryVO>) {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvA.layoutManager = layoutManager

        // ------# 운동시청기록 + 운동 #------
        pvm.selectedDailyCount.value = progresses.size
        pvm.selectedDailyTime.value = progresses.sumOf { it.duration ?: 0 }
        val adapter = ProgressHistoryRVAdapter(this@AnalyzeFragment,  progresses)
        binding.rvA.adapter = adapter

        if (progresses.isEmpty()) {
            binding.clAEmpty.visibility = View.VISIBLE
        } else {
            binding.clAEmpty.visibility = View.GONE
        }
    }

    private fun sortTodayInWeek() : List<Int> {
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.value // 오늘 요일 (2, 3, 4, 5, 6, 7, 1)
        return (1..7).map { (dayOfWeek + it) % 7 }.map { if (it == 0) 7 else it } // 오늘요일을 7로 나눴을 때 0인 index가 오늘 요일임.
    }


    private fun sortIvInLayout() {
        val imageViews = listOf(
            binding.llAWeek.getChildAt(0),
            binding.llAWeek.getChildAt(1),
            binding.llAWeek.getChildAt(2),
            binding.llAWeek.getChildAt(3),
            binding.llAWeek.getChildAt(4),
            binding.llAWeek.getChildAt(5),
            binding.llAWeek.getChildAt(6)
        )

        binding.llAWeek.removeAllViews()
        for ( i in 0 until 7) {
            binding.llAWeek.addView(imageViews[todayInWeek[i] - 1])
        }
    }

    private fun setWeeklyDrawable(ivId: String, drawableName: String) {
        val resId = resources.getIdentifier(ivId, "id", requireContext().packageName)
        val imageView = view?.findViewById<ImageView>(resId)
        val drawableResId = resources.getIdentifier(drawableName, "drawable", requireContext().packageName)

        if (imageView != null) {
            imageView.setImageResource(drawableResId)
        } else {
            Log.e("MeasureDashBoard2Fragment", "ImageView with id $ivId not found")
        }
    }

    private fun updateMonthView() {
        binding.monthText.text = "${currentMonth.year}년 ${getCurrentMonthInKorean(currentMonth)}"
        binding.cvACalendar.scrollToMonth(currentMonth)
    }

    private fun initMonthData() {
        val oldDate = pvm.selectedDate
        pvm.selectedDate = null
        oldDate?.let { binding.cvACalendar.notifyDateChanged(it) }
        setAdapter(listOf())
        binding.tvADate.text = "날짜를 선택해주세요"
    }

    // 버튼으로 월이 바꼈을 때,
    private fun updateMonthProgress(date: String) {

        lifecycleScope.launch(Dispatchers.IO) {
            val dateList = getMonthProgress(requireActivity().getString(R.string.API_progress), date, requireContext())?.toList()
            if (dateList != null) {
                avm.updateMonthProgress(dateList)
            }
//            Log.v("VMProgresses", "${avm.existedMonthProgresses}")
        }
    }

    private fun setShimmer(isStart: Boolean) {
        when (isStart) {
            true -> {
                binding.tvAProgressGuide.visibility = View.VISIBLE
                binding.sflA.visibility = View.VISIBLE
                binding.cvAProgress.visibility = View.GONE
                binding.sflA.startShimmer()
            }
            false -> {
                binding.sflA.visibility = View.GONE
                binding.tvAProgressGuide.visibility = View.VISIBLE
                binding.cvAProgress.visibility = View.VISIBLE
                binding.sflA.stopShimmer()
            }
        }

    }

    private fun setGraph() {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val weeklySets = (6 downTo 0).map { i ->
            val currentDate = LocalDate.now().minusDays(i.toLong())
            val currentDateStr = currentDate.format(dateFormatter)

            // 해당 날짜가 MutableList에 있는지 확인
            val matchingPair = pvm.graphProgresses?.find { it.first == currentDateStr }
            val count = calculatePercent(matchingPair?.second)
            count
        }

        var finishSets = 0
        for (indices in weeklySets) {
            if (indices > 0f) finishSets += 1
        }

        // ------! bar chart 시작 !------
        val barChart: BarChart = binding.bcA
        barChart.renderer = BarChartRender(barChart, barChart.animator, barChart.viewPortHandler)
        val entries = ArrayList<BarEntry>()
//            Log.v("weeklySet가져오기", "$matchingPair")
        for (i in weeklySets.indices) {
            val entry = BarEntry(i.toFloat(), weeklySets[i])
            entries.add(entry)
        }
        val dataSet = BarDataSet(entries, "")
        dataSet.apply {
            color =  resources.getColor(R.color.thirdColor, null)
            setDrawValues(false)
        }
        // BarData 생성 및 차트에 설정
        val bcdata = BarData(dataSet)
        bcdata.apply {
            barWidth = 0.5f
        }
        barChart.data = bcdata
        // X축 설정
        barChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            setDrawAxisLine(false)
            labelRotationAngle = 2f
            setDrawLabels(false)
        }
        barChart.legend.apply {
            formSize = 0f
        }
        // 왼쪽 Y축 설정
        barChart.axisLeft.apply {
            axisMinimum = -1f // Y축 최소값
            axisMaximum = 100f
            setDrawAxisLine(false)
            setDrawGridLines(false)
            setLabelCount(0, false)
            setDrawLabels(false)
        }
        // 차트 스타일링 및 설정
        barChart.apply {
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            setDrawValueAboveBar(false)
            setDrawGridBackground(false)
            setFitBars(false)
            animateY(500)
            setScaleEnabled(false)
            setTouchEnabled(false)
            invalidate()
        }

        // ------# progress #------
        var progressCount = 0
        for (i in weeklySets.indices) {
            if (weeklySets[i] > 1f) {
                progressCount++
            }
        }

        binding.tvAWeekProgress.text = "진행일수 $progressCount/${weeklySets.size}"
        // ------# 월화수목금토일 데이터 존재할 시 변경할 구간 #------
        sortIvInLayout()
//        Log.v("weeklySets", "$weeklySets")
        for ((index, value) in weeklySets.withIndex()) {
            if (value > 1.0) {
                setWeeklyDrawable("ivA${todayInWeek[index]}", "icon_week_${todayInWeek[index]}_enabled")
            } else {
                setWeeklyDrawable("ivA${todayInWeek[index]}", "icon_week_${todayInWeek[index]}_disabled")
            }
            // 오늘 날짜를 지정할 index선택시 자동으로 정렬
            if (index == 6) {
                setWeeklyDrawable("ivA${todayInWeek[index]}", "icon_week_${todayInWeek[index]}_today")
            }
        }
    }
    private fun showDailyProgress(index: Int? = null) {
        if (index != null) {
            pvm.selectedDate = LocalDate.now().minusDays((6 - index).toLong())
        }
//        Log.v("막대그래프 클릭", "${pvm.selectedDate}")

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                pvm.selectedDate?.let { selectedDate ->
                    val date = selectedDate.format(formatter)
//                    Log.v("date", date)
                    val currentProgresses = getDailyProgress(getString(R.string.API_progress), date, requireContext())
                    withContext(Dispatchers.Main) {
                        if (!currentProgresses.isNullOrEmpty()) {
                            setAdapter(currentProgresses)
                        } else {
                            setAdapter(listOf())
                        }
                        binding.tvADate.text = "${selectedDate.year}년 ${getCurrentMonthInKorean(selectedDate.yearMonth)} ${getCurrentDayInKorean(selectedDate)} 운동 정보"
                    }
                } ?: run {
                    Log.e("DateSelection", "Selected date is null")
                }
            }
            withContext(Dispatchers.Main) {
                binding.cvACalendar.notifyCalendarChanged()
            }
        }
    }
    private fun scrollToView(view: View) {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        val viewTop = location[1]
        val scrollViewLocation = IntArray(2)

        binding.nsvA.getLocationInWindow(scrollViewLocation)
        val scrollViewTop = scrollViewLocation[1]
        val scrollY = binding.nsvA.scrollY
        val scrollTo = scrollY + viewTop - scrollViewTop
        binding.nsvA.smoothScrollTo(0, scrollTo)
    }

    private fun animateCardViewToPercentage(index: Int) {
        val params = binding.cvAEffect.layoutParams as ConstraintLayout.LayoutParams
        val startBias = params.horizontalBias
        val endBias = when (index) {
            6 -> 0.975f
            5 -> 0.815f
            4 -> 0.66f
            2 -> 0.34f
            1 -> 0.185f
            0 -> 0.025f
            else -> 0.5f
        } // 이동할 목표 위치

        val durations = when (index) {
            1,2, 4, 5 -> 650L
            else -> 500L
        }
        val animator = ValueAnimator.ofFloat(startBias, endBias).apply {
            duration = durations // 1초 동안 애니메이션
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Float
                params.horizontalBias = animatedValue
                binding.cvAEffect.layoutParams = params
            }
        }
        animator.start()
    }
}