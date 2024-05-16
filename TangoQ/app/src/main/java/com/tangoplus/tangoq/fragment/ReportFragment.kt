package com.tangoplus.tangoq.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.ReportRVAdapter
import com.tangoplus.tangoq.databinding.FragmentReportBinding
import com.tangoplus.tangoq.view.DayViewContainer
import com.tangoplus.tangoq.view.MonthHeaderViewContainer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class ReportFragment : Fragment() {
    lateinit var binding : FragmentReportBinding
    var currentMonth = YearMonth.now()
    var selectedDate: LocalDate? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReportBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ------! calendar 시작 !------
        binding.monthText.text = "${YearMonth.now().year}월 ${YearMonth.now().month}"
        binding.cvR.setup(currentMonth.minusMonths(24), currentMonth.plusMonths(0), DayOfWeek.SUNDAY)
        binding.cvR.scrollToMonth(currentMonth)
        binding.cvR.monthScrollListener = { month ->
            currentMonth = month.yearMonth
            binding.monthText.text = "${currentMonth.year}년 ${currentMonth.month}"
        }
        binding.nextMonthButton.setOnClickListener {
            if (currentMonth != YearMonth.now()) {
                currentMonth = currentMonth.plusMonths(1)
                binding.monthText.text = "${currentMonth.year}년 ${currentMonth.month}"
                binding.cvR.scrollToMonth(currentMonth)
            }

        }
        binding.previousMonthButton.setOnClickListener {
            if (currentMonth == YearMonth.now().minusMonths(24)) {

            } else {
                currentMonth = currentMonth.minusMonths(1)
                binding.monthText.text = "${currentMonth.year}년 ${currentMonth.month}"
                binding.cvR.scrollToMonth(currentMonth)
            }

        }
        binding.cvR.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthHeaderViewContainer> {
            override fun create(view: View) = MonthHeaderViewContainer(view)
            override fun bind(container: MonthHeaderViewContainer, month: CalendarMonth) {
                // 여기에서 month 정보를 사용하여 header view를 업데이트 할 수 있습니다.
                // 예를 들어, TextView의 경우:
                container.tvSUN.text = "일"
                container.tvMON.text = "월"
                container.tvTUE.text = "화"
                container.tvWEN.text = "수"
                container.tvTUR.text = "목"
                container.tvFRI.text = "금"
                container.tvSAT.text = "토"

            }
        }

        binding.cvR.dayBinder = object : MonthDayBinder<DayViewContainer> {

            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.date.text = day.date.dayOfMonth.toString()
                container.date.textSize = 20f
                Log.v("container.date", "${container.date.text}")
                container.date.setTextColor(ContextCompat.getColor(container.date.context, R.color.mainColor))
//                if (day.date.month != currentMonth.month) {
//                    container.date.setTextColor(R.color.mainColor)
//
//                } else {
//                    container.date.setTextColor(R.color.mainblack)
//                }
//
//                container.date.setOnClickListener {
//                    container.date.setTextColor(R.color.mainColor)
//                }
                if (day.date == selectedDate) {
                    container.date.setTextColor(ContextCompat.getColor(container.date.context, R.color.mainColor))

                } else {
                    container.date.setTextColor(ContextCompat.getColor(container.date.context, R.color.subColor400))
                }
                container.date.setOnClickListener {
                    // 선택된 날짜를 업데이트하고 UI를 갱신합니다.
                    val oldDate = selectedDate
                    selectedDate = day.date
                    if (oldDate != null) {
                        binding.cvR.notifyDateChanged(oldDate)
                    }
                    binding.cvR.notifyDateChanged(day.date)
                }
            }
            override fun create(view: View): DayViewContainer {
                return DayViewContainer(view)
            }
        } // ------! calendar 끝 !------


        val parts = mutableListOf<String>()
        parts.add("정면 자세")
        parts.add("후면 자세")
        parts.add("오버헤드 스쿼트")
        parts.add("팔꿉 측정 자세")
        parts.add("의자 후면")
        parts.add("오른쪽 측면 자세")
        parts.add("왼쪽 측면 자세")
        val adapter = ReportRVAdapter(parts, this)
        binding.rvR.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvR.layoutManager = linearLayoutManager


    }
}