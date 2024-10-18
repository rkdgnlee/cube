package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.yearMonth
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.adapter.MD2RVAdpater
import com.tangoplus.tangoq.data.HistorySummaryVO
import com.tangoplus.tangoq.data.ProgressUnitVO
import com.tangoplus.tangoq.data.ProgressViewModel
import com.tangoplus.tangoq.data.UserViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureDashboard2Binding
import com.tangoplus.tangoq.`object`.NetworkExercise.fetchExerciseById
import com.tangoplus.tangoq.`object`.NetworkProgress.getDailyProgress
import com.tangoplus.tangoq.`object`.Singleton_t_progress
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.view.BarChartRender
import com.tangoplus.tangoq.view.DayViewContainer
import com.tangoplus.tangoq.view.MonthHeaderViewContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale


class MeasureDashBoard2Fragment : Fragment() {
    lateinit var binding : FragmentMeasureDashboard2Binding
    var currentMonth = YearMonth.now()
    var selectedDate = LocalDate.now()

    private var graphProgresses : MutableList<MutableList<ProgressUnitVO>>? = null
    private lateinit var progresses: MutableList<HistorySummaryVO> // 운동 기록 전체에서 어떤 프로그램의 운동이었는지를 보여줘야함.
    private val pvm : ProgressViewModel by activityViewModels()
    private val uvm : UserViewModel by activityViewModels()

    private lateinit var  todayInWeek : List<Int>



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


        // ------# 일별 운동 담기 #------
        graphProgresses = Singleton_t_progress.getInstance(requireContext()).graphProgresses
        // TODO 여기서 이틀했을 수도 있고, 삼일 했을 수도 있고, 전혀 없을 수도 있음.

        // ------# 그래프에 들어갈 가장 최근 일주일간 수치 넣기 #------
        val weeklySets = mutableListOf<Float>()
        if (graphProgresses != null) {
            for (i in 0 until 7) {
                if (i < graphProgresses!!.size && graphProgresses!![i].count() > 0) {
                    weeklySets.add(graphProgresses!![i].count() * 100 / 7.toFloat())
                } else {
                    weeklySets.add(1f)
                }
            }
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

        // ------# 월화수목금토일 데이터 존재할 시 변경할 구간 #------
        sortIvInLayout()
        Log.v("weeklySets", "${weeklySets}")
        for ((index, value) in weeklySets.withIndex()) {
            if (value > 1.0) {
                setWeeklyDrawable("ivMD2${todayInWeek[index]}", "icon_week_${todayInWeek[index]}_enabled")
            } else {
                setWeeklyDrawable("ivMD2${todayInWeek[index]}", "icon_week_${todayInWeek[index]}_disabled")
            }
            if (index == 3) {
                setWeeklyDrawable("ivMD2${todayInWeek[index]}", "icon_week_${todayInWeek[index]}_today")
            }
        }

        // ------# progress #------
        var progressCount = 0
        for (i in weeklySets.indices) {
            if (weeklySets[i] > 1f) {
                progressCount++
            }
        }

        binding.tvMD2Progress.text = "완료 $progressCount/${weeklySets.size}"
        // ---- 꺾은선 그래프 코드 끝 ----
        val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject

        Log.v("Singleton>Profile", "${userJson}")

        binding.tvMD2Title.text = "${userJson?.optString("user_name")}님의 기록"

        // ------! calendar 시작 !------
        binding.monthText.text = "${YearMonth.now().year}월 ${getCurrentMonthInKorean(currentMonth)}"

        // ------# 날짜와 운동 기록 보여주기 #------
        binding.tvMD2Date.text = "${selectedDate.year}년 ${getCurrentMonthInKorean(selectedDate.yearMonth)} ${getCurrentDayInKorean(selectedDate)} 운동 정보"

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
//                val historyDate = stringToLocalDate(history.regDate!!)
//                historyDate.month == currentMonth?.month && historyDate.year == currentMonth?.year
//            }.toMutableList()
//            setAdapter(filteredExercises)
        }

        binding.nextMonthButton.setOnClickListener {
            // 선택된 날짜 초기화
            val oldDate = selectedDate
            selectedDate = null
            oldDate?.let { binding.cvMD2Calendar.notifyDateChanged(it) }


            if (currentMonth != YearMonth.now()) {
                currentMonth = currentMonth.plusMonths(1)
                updateMonthView()
                updateExerciseList()
            }
        }


        binding.previousMonthButton.setOnClickListener {
            // 선택된 날짜 초기화
            val oldDate = selectedDate
            selectedDate = null
            oldDate?.let { binding.cvMD2Calendar.notifyDateChanged(it) }

            if (currentMonth > YearMonth.now().minusMonths(24)) {
                currentMonth = currentMonth.minusMonths(1)
                updateMonthView()
                updateExerciseList()
            }
        }

        binding.monthText.setOnClickListener {
            updateExerciseList()
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
                        textSize = if (isTablet(requireContext())) 24f else 20f
                        when (day.date.dayOfMonth) {
                            in 0 .. 9 -> {
                                setPadding(32, 20, 32, 20)
                            }
                            in 10 .. 19 -> {
                                setPadding(26, 24, 26, 24)
                            }
                            else -> {
                                setPadding(24)
                            }
                        }
                        setDateStyle(container, day)
                        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
                        container.date.setOnClickListener {
                            // 선택된 날짜를 업데이트 + UI 갱신
                            if (day.date <= LocalDate.now() ) {
                                val oldDate = selectedDate

                                selectedDate = day.date
                                oldDate?.let { binding.cvMD2Calendar.notifyDateChanged(it) }
                                selectedDate?.let { binding.cvMD2Calendar.notifyDateChanged(it) }
                                CoroutineScope(Dispatchers.IO).launch {
                                    selectedDate?.let { selectedDate ->
                                        val date = selectedDate.format(formatter)
                                        val currentProgresses = getDailyProgress(getString(R.string.API_progress), date, requireContext())
                                        withContext(Dispatchers.Main) {
                                            setAdapter(currentProgresses)
                                        }
//                                    pvm.currentProgressItem =
//                                    val filteredExercises = pvm.allHistorys.filter { history ->
//                                        history.regDate?.let { regDateString ->
//                                            val historyDate = stringToLocalDate(regDateString)
//                                            historyDate.isEqual(selectedDate)
//                                        } ?: false
//                                    }.toHistorySummaries()
//
//                                    setAdapter(filteredExercises)
//                                    binding.tvMD2Date.text = "${selectedDate.year}년 ${getCurrentMonthInKorean(selectedDate.yearMonth)} ${getCurrentDayInKorean(selectedDate)} 운동 정보"
                                    } ?: run {
                                        Log.e("DateSelection", "Selected date is null")
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

    private fun setDateStyle(container: DayViewContainer, day: CalendarDay) {
        container.date.background = null
        when {
            day.date == selectedDate -> {
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

    private fun setAdapter(progresses: List<ProgressUnitVO>) {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvMD2.layoutManager = layoutManager

        // ------# 운동시청기록 + 운동 #------
        // 같이 들어와야 하는게 좋을 듯 함. 그래서 그걸 exerciseUnit으로 만들기.

        pvm.selectedDailyCount.value = progresses.size
        pvm.selectedDailyTime.value = progresses.sumOf { it.videoDuration }
        Log.v("프로그레스", "${progresses}")
        val adapter = ExerciseRVAdapter(this@MeasureDashBoard2Fragment, mutableListOf(), progresses.toMutableList(), Pair(0,0), "main")
//        val adapter = MD2RVAdpater(this@MeasureDashBoard2Fragment, progresses)
        binding.rvMD2.adapter = adapter


        if (progresses.isEmpty()) {
            binding.clMD2Empty.visibility = View.VISIBLE
        } else {
            binding.clMD2Empty.visibility = View.GONE
        }
    }

    private fun getTime(id: String) : Int {
        return pvm.currentProgram?.exerciseTimes?.find { it.first == id }?.second!!
    }


    private fun sortTodayInWeek() : List<Int> {
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.value // 오늘 요일 (2, 3, 4, 5, 6, 7, 1)
        Log.v("오늘요일", "dayOfWeek: $dayOfWeek")
        val days = (1..7).toList()
        val dayIndex = days.indexOf(dayOfWeek)

        val sortedIndex = (1..7).map {
            val offset = (it - 4 + dayIndex) % 7
            days[if (offset < 0) offset + 7 else offset]
        }
        Log.v("정렬된 요일", "sortedIndex: $sortedIndex")
        return sortedIndex
//        return (1..7).map { (dayOfWeek + it) % 7 }.map { if (it == 0) 7 else it } // 오늘요일을 7로 나눴을 때 0인 index가 오늘 요일임.
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

    private fun isTablet(context: Context): Boolean {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        val widthDp = metrics.widthPixels / metrics.density
        return widthDp >= 600
    }

    private fun updateMonthView() {
        binding.monthText.text = "${currentMonth.year}년 ${getCurrentMonthInKorean(currentMonth)}"
        binding.cvMD2Calendar.scrollToMonth(currentMonth)
    }

    private fun updateExerciseList() {
//        val filteredExercises = pvm.allHistorys.filter { history ->
//            history.regDate?.let { regDateString ->
//                val historyDate = stringToLocalDate(regDateString)
//                YearMonth.from(historyDate) == currentMonth
//            } ?: false
//        }.toMutableList()
//        val historySummaries = filteredExercises.toHistorySummaries()
//        setAdapter(historySummaries)
    }

//    private fun List<ProgressUnitVO>.toHistorySummaries() : List<HistorySummaryVO> {
//        return this.groupBy {it.exerciseId}
//            .map { (exerciseId, histories) ->
//            HistorySummaryVO(
//                exerciseId = exerciseId.toString(),
//                viewCount = histories.size,
//                lastViewDate = histories.maxByOrNull { it.regDate ?: "" }?.regDate
//                    )
//            }
//    }
}