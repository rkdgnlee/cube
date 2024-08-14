package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.os.Build
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.yearMonth
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.MD2RVAdpater
import com.tangoplus.tangoq.data.GraphVO
import com.tangoplus.tangoq.databinding.FragmentMeasureDashboard2Binding
import com.tangoplus.tangoq.view.BarChartRender
import com.tangoplus.tangoq.view.DayViewContainer
import com.tangoplus.tangoq.view.MonthHeaderViewContainer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.random.Random


class MeasureDashBoard2Fragment : Fragment() {
    lateinit var binding : FragmentMeasureDashboard2Binding
    var currentMonth = YearMonth.now()
    var selectedDate = LocalDate.now()
    val exerciseHistorys = mutableListOf<Triple<String, Int, Int>>()
    var exerciseHistoryDates = mutableListOf<LocalDate>()
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

        // ------! 운동 기록 API 공간 시작 !------

        // TODO 실제 운동 기록 값 넣기
        exerciseHistorys.add(Triple("2023-08-14 11:54:51", 9, 2))
        exerciseHistorys.add(Triple("2024-06-03 15:01:30", 16, 3))
        exerciseHistorys.add(Triple("2024-06-04 09:58:45", 26, 7))
        exerciseHistorys.add(Triple("2024-06-07 19:28:12", 13, 3))
        exerciseHistorys.add(Triple("2024-06-13 11:20:53", 22, 6))
        exerciseHistorys.add(Triple("2024-06-14 22:08:24", 46, 12))
        exerciseHistorys.add(Triple("2024-07-14 04:51:53", 30, 8))
        exerciseHistorys.add(Triple("2024-07-31 23:53:41", 29, 7))
        exerciseHistorys.add(Triple("2024-08-01 18:59:30", 46, 3))
        exerciseHistorys.add(Triple("2024-08-04 18:08:51", 46, 5))
        exerciseHistorys.add(Triple("2024-08-09 19:29:59", 46, 6))

        exerciseHistoryDates = exerciseHistorys.map { stringToLocalDate(it.first) }.toMutableList()


        // ------! 하단 rv 끝 !------


        // ------! 목표 그래프 시작 !------
        // ------! 일주일 간 운동 기록 들어올 곳 시작 !------
        val weeklySets = listOf(-1f, 38f, 60f, 12f, -1f, 0f, 0f)
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
            color =  resources.getColor(R.color.mainColor, null)
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

        // ------# 월화수목금토일 데이터 존재할 시 변경할 구간 #------
        for ((index, value) in weeklySets.withIndex()) {
            if (value > 0) {
                setWeeklyDrawable("ivMD2${index + 1}", "icon_week_${index + 1}_enabled")
            } else {
                setWeeklyDrawable("ivMD2${index + 1}", "icon_week_${index + 1}_disabled")
            }
        }
        // ------# progrees #------
        var progressCount = 0

        for (i in weeklySets.indices) {
            if (weeklySets[i] > 1f) {
                progressCount++
            }
        }
        binding.tvMD2Progress.text = "완료 $progressCount/${weeklySets.size}"
        // ---- 꺾은선 그래프 코드 끝 ----

        binding.tvMD2Goal.setOnClickListener {
        }

        // ------! calendar 시작 !------
        binding.monthText.text = "${YearMonth.now().year}월 ${getCurrentMonthInKorean(currentMonth)}"

        // ------# 날짜와 운동 기록 보여주기 #------
        binding.tvMD2Date.text = "${selectedDate.year}년 ${getCurrentMonthInKorean(selectedDate.yearMonth)} ${getCurrentDayInKorean(selectedDate)} 운동 정보"

        binding.cvMD2Calendar.apply {
            setup(currentMonth.minusMonths(24), currentMonth.plusMonths(0), DayOfWeek.SUNDAY)
            scrollToMonth(currentMonth)

            monthScrollListener = { month ->
                currentMonth = month.yearMonth
                binding.monthText.text = "${currentMonth.year}년 ${getCurrentMonthInKorean(currentMonth)}"
            }

            // ------# 월별로 운동 기록 필터링 #------
            val filteredExercises = exerciseHistorys.filter { history ->
                val historyDate = stringToLocalDate(history.first)
                historyDate.month == currentMonth?.month && historyDate.year == currentMonth?.year
            }.toMutableList()
            setAdapter(filteredExercises)
        }

        binding.nextMonthButton.setOnClickListener {
            // 선택된 날짜 초기화
            val oldDate = selectedDate
            selectedDate = null
            if (oldDate != null) {
                binding.cvMD2Calendar.notifyDateChanged(oldDate)
            }
            if (currentMonth != YearMonth.now()) {
                currentMonth = currentMonth.plusMonths(1)
                binding.monthText.text = "${currentMonth.year}년 ${getCurrentMonthInKorean(currentMonth)}"
                binding.cvMD2Calendar.scrollToMonth(currentMonth)

                val filteredExercises = exerciseHistorys.filter { history ->
                    val historyDate = stringToLocalDate(history.first)
                    historyDate.month == currentMonth?.month && historyDate.year == currentMonth?.year
                }.toMutableList()
                setAdapter(filteredExercises)
            }
        }


        binding.previousMonthButton.setOnClickListener {
            // 선택된 날짜 초기화
            val oldDate = selectedDate
            selectedDate = null
            if (oldDate != null) {
                binding.cvMD2Calendar.notifyDateChanged(oldDate)
            }

            if (currentMonth == YearMonth.now().minusMonths(24)) {

            } else {
                currentMonth = currentMonth.minusMonths(1)
                binding.monthText.text = "${currentMonth.year}년 ${getCurrentMonthInKorean(currentMonth)}"
                binding.cvMD2Calendar.scrollToMonth(currentMonth)

                val filteredExercises = exerciseHistorys.filter { history ->
                    val historyDate = stringToLocalDate(history.first)
                    historyDate.month == currentMonth?.month && historyDate.year == currentMonth?.year
                }.toMutableList()
                setAdapter(filteredExercises)
            }

        }

        // ------# 운동 기록 날짜 받아오기 #------
        // TODO 운동 기록 날짜) 변경 필요

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
                        textSize = 20f
                        when (day.date.dayOfMonth) {
                            in 0 .. 9 -> {
                                setPadding(34, 20, 34, 20)
                            }
                            else -> {
                                setPadding(24)
                            }
                        }
                        setDateStyle(container, day)

                        container.date.setOnClickListener {
                            // 선택된 날짜를 업데이트 + UI 갱신
                            if (day.date <= LocalDate.now() ) {
                                val oldDate = selectedDate

                                selectedDate = day.date
                                oldDate?.let { binding.cvMD2Calendar.notifyDateChanged(it) }
                                selectedDate?.let { binding.cvMD2Calendar.notifyDateChanged(it) }

                                binding.tvMD2Date.text = "${selectedDate?.year}월 ${getCurrentMonthInKorean(selectedDate?.yearMonth!!)} ${getCurrentDayInKorean(selectedDate)} 운동 정보"
                                Log.v("selectedDate", "$selectedDate")

                                // ------# 날짜 선택 시 #------
                                val filteredExercises = exerciseHistorys.filter { history ->
                                    val historyDate = stringToLocalDate(history.first)
                                    historyDate.dayOfMonth == selectedDate?.dayOfMonth && historyDate.month == selectedDate?.month && historyDate.year == selectedDate?.year
                                }.toMutableList()
                                setAdapter(filteredExercises)

                            }
                        }
                    } else {
                        visibility = View.INVISIBLE
                        setOnClickListener(null)
                    }

                }




            }
            override fun create(view: View): DayViewContainer {
                return DayViewContainer(view)
            }
        } // ------! calendar 끝 !------



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


    private fun setDateStyle(container: DayViewContainer, day: CalendarDay) {
        container.date.background = null
        when {
            day.date == selectedDate -> {
                container.date.setTextColor(ContextCompat.getColor(container.date.context, R.color.white))
                container.date.background = ResourcesCompat.getDrawable(resources, R.drawable.background_oval_main, null)
            }
            day.date == LocalDate.now() -> {
                container.date.setTextColor(ContextCompat.getColor(container.date.context, R.color.black))
                // 현재 날짜에 대한 특별한 배경이 필요하다면 여기에 추가
            }
            exerciseHistoryDates.contains(day.date) -> {
                container.date.setTextColor(ContextCompat.getColor(container.date.context, R.color.mainColor))
            }
            day.position == DayPosition.MonthDate -> {
                container.date.setTextColor(ContextCompat.getColor(container.date.context, R.color.subColor700))
            }
            else -> {
                container.date.setTextColor(ContextCompat.getColor(container.date.context, R.color.subColor150))
            }
        }
    }

    private fun getCurrentMonthInKorean(month: YearMonth): String {
        return month.month.getDisplayName(TextStyle.FULL, Locale("ko"))
    }

    private fun getCurrentDayInKorean(date: LocalDate): String {
        return "${date.dayOfMonth}일"
    }

    private fun stringToLocalDate(dateTimeString: String): LocalDate {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val localDateTime = LocalDateTime.parse(dateTimeString, formatter)
        return localDateTime.toLocalDate()
    }

    private fun setAdapter(exerciseHistorys: MutableList<Triple<String, Int, Int>>) {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvMD2.layoutManager = layoutManager
        val adapter = MD2RVAdpater(this@MeasureDashBoard2Fragment, exerciseHistorys)
        binding.rvMD2.adapter = adapter
    }
}