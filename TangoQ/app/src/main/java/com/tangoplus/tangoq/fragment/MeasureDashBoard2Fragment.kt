package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.gms.common.util.DeviceProperties.isTablet
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.yearMonth
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.ProgressHistoryRVAdapter
import com.tangoplus.tangoq.vo.ProgressHistoryVO
import com.tangoplus.tangoq.vo.ProgressUnitVO
import com.tangoplus.tangoq.viewmodel.ProgressViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureDashboard2Binding
import com.tangoplus.tangoq.api.NetworkProgress.getDailyProgress
import com.tangoplus.tangoq.api.NetworkProgress.getWeekProgress
import com.tangoplus.tangoq.db.Singleton_t_progress
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.view.BarChartRender
import com.tangoplus.tangoq.view.DayViewContainer
import com.tangoplus.tangoq.view.MonthHeaderViewContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale


class MeasureDashBoard2Fragment : Fragment() {
    lateinit var binding : FragmentMeasureDashboard2Binding
    var currentMonth = YearMonth.now()

    private val pvm : ProgressViewModel by activityViewModels()

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
        binding = FragmentMeasureDashboard2Binding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ------# 운동 기록 API 공간 #------
        todayInWeek = sortTodayInWeek()

        // ------# 그래프에 들어갈 가장 최근 일주일간 수치 넣기 #------
        var weeklySets = listOf<Float>()
        lifecycleScope.launch(Dispatchers.Main) {
            if (pvm.graphProgresses.isNullOrEmpty()) {
                pvm.graphProgresses = getWeekProgress(getString(R.string.API_progress), requireContext())
            }

            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            if (pvm.graphProgresses != null) {

                withContext(Dispatchers.Main) {
                    weeklySets = (6 downTo 0).map { i ->
                        val currentDate = LocalDate.now().minusDays(i.toLong())
                        val currentDateStr = currentDate.format(dateFormatter)

                        // 해당 날짜가 MutableList에 있는지 확인
                        val matchingPair = pvm.graphProgresses?.find { it.first == currentDateStr }
                        Log.v("weeklySet가져오기", "$matchingPair")
                        val count = calculatePercent(matchingPair?.second)
                        count
                    }


                    var finishSets = 0
                    for (indices in weeklySets) {
                        if (indices > 0f) finishSets += 1
                    }

                    // ------! bar chart 시작 !------
                    val barChart: BarChart = binding.bcMD2
                    barChart.renderer = BarChartRender(barChart, barChart.animator, barChart.viewPortHandler)
                    val entries = ArrayList<BarEntry>()

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

                    binding.tvMD2Progress.text = "최근 일주일 경과: $progressCount/${weeklySets.size}"
                    // ------# 월화수목금토일 데이터 존재할 시 변경할 구간 #------
                    sortIvInLayout()
                    Log.v("weeklySets", "${weeklySets}")
                    for ((index, value) in weeklySets.withIndex()) {
                        if (value > 1.0) {
                            setWeeklyDrawable("ivMD2${todayInWeek[index]}", "icon_week_${todayInWeek[index]}_enabled")
                        } else {
                            setWeeklyDrawable("ivMD2${todayInWeek[index]}", "icon_week_${todayInWeek[index]}_disabled")
                        }
                        // 오늘 날짜를 지정할 index선택시 자동으로 정렬
                        if (index == 6) {
                            setWeeklyDrawable("ivMD2${todayInWeek[index]}", "icon_week_${todayInWeek[index]}_today")
                        }
                    }
                }
            }
        }

        val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject
        Log.v("Singleton>Profile", "${userJson}")
        binding.tvMD2Title.text = "${userJson?.optString("user_name")}님의 기록"

        // ------! calendar 시작 !------
        binding.monthText.text = "${YearMonth.now().year}월 ${getCurrentMonthInKorean(currentMonth)}"

        // ------# 날짜와 운동 기록 보여주기 #------
        binding.tvMD2Date.text = "${pvm.selectedDate?.year}년 ${getCurrentMonthInKorean(pvm.selectedDate?.yearMonth)} ${getCurrentDayInKorean(pvm.selectedDate)} 운동 정보"

        pvm.selectedDailyTime.observe(viewLifecycleOwner) {
            binding.tvMD2DailyTime.text = "${it}초"
        }
        pvm.selectedDailyCount.observe(viewLifecycleOwner) {
            binding.tvMD2DailyCount.text = "${it}개"
        }
        binding.cvMD2Calendar.apply {
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
            val oldDate = pvm.selectedDate
            pvm.selectedDate = null
            oldDate?.let { binding.cvMD2Calendar.notifyDateChanged(it) }


            if (currentMonth != YearMonth.now()) {
                currentMonth = currentMonth.plusMonths(1)
                updateMonthView()
                updateMonthProgress()
            }
        }

        binding.previousMonthButton.setOnClickListener {
            // 선택된 날짜 초기화
            val oldDate = pvm.selectedDate
            pvm.selectedDate = null
            oldDate?.let { binding.cvMD2Calendar.notifyDateChanged(it) }

            if (currentMonth > YearMonth.now().minusMonths(24)) {
                currentMonth = currentMonth.minusMonths(1)
                updateMonthView()
                updateMonthProgress()
            }
        }

        binding.monthText.setOnClickListener {
            updateMonthProgress()
        }

        // ------# 운동 기록 날짜 받아오기 #------
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                pvm.selectedDate?.let { selectedDate ->
                    val date = selectedDate.format(formatter)
                    Log.v("date", date)
                    val currentProgresses = getDailyProgress(getString(R.string.API_progress), date, requireContext())
                    withContext(Dispatchers.Main) {
                        if (currentProgresses != null) {
                            setAdapter(currentProgresses)
                        }
                        binding.tvMD2Date.text = "${selectedDate.year}년 ${getCurrentMonthInKorean(selectedDate.yearMonth)} ${getCurrentDayInKorean(selectedDate)} 운동 정보"
                    }
                } ?: run {
                    Log.e("DateSelection", "Selected date is null")
                }
            }
        }

        binding.cvMD2Calendar.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthHeaderViewContainer> {
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

        binding.cvMD2Calendar.dayBinder = object : MonthDayBinder<DayViewContainer> {
            @SuppressLint("UseCompatLoadingForDrawables")
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.date.apply {
                    if (day.date.month == currentMonth.month) {
                        text = day.date.dayOfMonth.toString()
                        textSize = if (isTablet(requireContext())) 24f else 20f
                        when (day.date.dayOfMonth) {
                            in 2 .. 9 -> {
                                setPadding(28, 14, 28, 14)
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
                                setPadding(30, 22, 30, 22)
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
                                oldDate?.let { binding.cvMD2Calendar.notifyDateChanged(it) }
                                pvm.selectedDate?.let { binding.cvMD2Calendar.notifyDateChanged(it) }
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        Log.v("selectedDate", "${pvm.selectedDate}")
                                        pvm.selectedDate?.let { selectedDate ->
                                            val date = selectedDate.format(formatter)
                                            Log.v("date", date)
                                            val currentProgresses = getDailyProgress(getString(R.string.API_progress), date, requireContext())
                                            withContext(Dispatchers.Main) {
                                                if (currentProgresses != null) {
                                                    setAdapter(currentProgresses)
                                                }
                                                binding.tvMD2Date.text = "${selectedDate.year}년 ${getCurrentMonthInKorean(selectedDate.yearMonth)} ${getCurrentDayInKorean(selectedDate)} 운동 정보"
                                            }
                                        } ?: run {
                                            Log.e("DateSelection", "Selected date is null")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
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

        binding.btnMD2Exercise.setOnClickListener {
            val bnb : BottomNavigationView = requireActivity().findViewById(R.id.bnbMain)
            bnb.selectedItemId = R.id.exercise
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
        when {
            day.date == pvm.selectedDate -> {
                container.date.setTextColor(ContextCompat.getColor(container.date.context, R.color.white))
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
        binding.rvMD2.layoutManager = layoutManager

        // ------# 운동시청기록 + 운동 #------
        pvm.selectedDailyCount.value = progresses.size
        // TODO 여기서 item에 뭘 넣고, 합산 결과에 뭘 넣을지 결정해야함.
        pvm.selectedDailyTime.value = progresses.sumOf { it.sn }
        val adapter = ProgressHistoryRVAdapter(requireParentFragment(), progresses)
        binding.rvMD2.adapter = adapter

        if (progresses.isEmpty()) {
            binding.clMD2Empty.visibility = View.VISIBLE
        } else {
            binding.clMD2Empty.visibility = View.GONE
        }
    }

    private fun sortTodayInWeek() : List<Int> {
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.value // 오늘 요일 (2, 3, 4, 5, 6, 7, 1)
//        Log.v("오늘요일", "dayOfWeek: $dayOfWeek")
//        val days = (1..7).toList()
//        val dayIndex = days.indexOf(dayOfWeek)
//
//        val sortedIndex = (1..7).map {
//            val offset = (it - 4 + dayIndex) % 7
//            days[if (offset < 0) offset + 7 else offset]
//        }
//        Log.v("정렬된 요일", "sortedIndex: $sortedIndex")
//        return sortedIndex
        return (1..7).map { (dayOfWeek + it) % 7 }.map { if (it == 0) 7 else it } // 오늘요일을 7로 나눴을 때 0인 index가 오늘 요일임.
    }


    private fun sortIvInLayout() {
        val imageViews = listOf(
            binding.llMD2Week.getChildAt(0),
            binding.llMD2Week.getChildAt(1),
            binding.llMD2Week.getChildAt(2),
            binding.llMD2Week.getChildAt(3),
            binding.llMD2Week.getChildAt(4),
            binding.llMD2Week.getChildAt(5),
            binding.llMD2Week.getChildAt(6)
        )

        binding.llMD2Week.removeAllViews()
        for ( i in 0 until 7) {
            binding.llMD2Week.addView(imageViews[todayInWeek[i] - 1])
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
        binding.cvMD2Calendar.scrollToMonth(currentMonth)
    }

    private fun updateMonthProgress() {
//        val filteredExercises = pvm.allHistorys.filter { history ->
//            history.regDate?.let { regDateString ->
//                val historyDate = stringToLocalDate(regDateString)
//                YearMonth.from(historyDate) == currentMonth
//            } ?: false
//        }.toMutableList()
//        val historySummaries = filteredExercises.toHistorySummaries()
//        setAdapter(historySummaries)
    }

}