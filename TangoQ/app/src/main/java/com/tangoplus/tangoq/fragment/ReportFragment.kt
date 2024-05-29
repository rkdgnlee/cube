package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import com.google.mediapipe.util.proto.RenderDataProto.RenderAnnotation.Line
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.ReportRVAdapter
import com.tangoplus.tangoq.databinding.FragmentReportBinding
import com.tangoplus.tangoq.listener.OnReportClickListener
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import com.tangoplus.tangoq.view.DayViewContainer
import com.tangoplus.tangoq.view.MonthHeaderViewContainer
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.exp

class ReportFragment : Fragment(), OnReportClickListener {
    lateinit var binding : FragmentReportBinding
    var currentMonth = YearMonth.now()
    var selectedDate = LocalDate.now()
    private lateinit var singletonInstance: Singleton_t_measure
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReportBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        singletonInstance = Singleton_t_measure.getInstance(requireContext())


        // ------! calendar 시작 !------
        binding.monthText.text = "${YearMonth.now().year}월 ${getCurrentMonthInKorean(currentMonth)}"
        binding.cvR.setup(currentMonth.minusMonths(24), currentMonth.plusMonths(0), DayOfWeek.SUNDAY)
        binding.cvR.scrollToMonth(currentMonth)
        binding.cvR.monthScrollListener = { month ->
            currentMonth = month.yearMonth
            binding.monthText.text = "${currentMonth.year}년 ${getCurrentMonthInKorean(currentMonth)}"
        }
        binding.nextMonthButton.setOnClickListener {
            if (currentMonth != YearMonth.now()) {
                currentMonth = currentMonth.plusMonths(1)
                binding.monthText.text = "${currentMonth.year}년 ${getCurrentMonthInKorean(currentMonth)}"
                binding.cvR.scrollToMonth(currentMonth)
            }
        }

        binding.previousMonthButton.setOnClickListener {
            if (currentMonth == YearMonth.now().minusMonths(24)) {

            } else {
                currentMonth = currentMonth.minusMonths(1)
                binding.monthText.text = "${currentMonth.year}년 ${getCurrentMonthInKorean(currentMonth)}"
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

                // ------! 선택 날짜 !------
                if (day.date == selectedDate) {
                    container.date.setTextColor(ContextCompat.getColor(container.date.context, R.color.mainColor))
                } else if (day.date == LocalDate.now()) {
                    if (day.date != selectedDate) {
                        container.date.setTextColor(ContextCompat.getColor(container.date.context, R.color.subColor700))
                    }
                    container.date.setTextColor(ContextCompat.getColor(container.date.context, R.color.black))
                } else {
                    // ------! 해당 월 이외 색상 처리 !------
                    if (day.position == DayPosition.MonthDate) {
                        container.date.setTextColor(ContextCompat.getColor(container.date.context, R.color.subColor700))
                    } else {
                        container.date.setTextColor(ContextCompat.getColor(container.date.context, R.color.subColor200))
                    }
                }
                binding.vRDiseasePredict.setOnClickListener{
                    requireActivity().supportFragmentManager.beginTransaction().apply {
                        setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                        replace(R.id.flMain, ReportDiseaseFragment())
                        commit()
                    }
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


        val parts = mutableListOf<Triple<String, String, JSONObject>>() // TODO Triple<분류, 파일명, 데이터>

        parts.add(Triple("정면 자세", singletonInstance.jsonObject?.optString("fileName"),singletonInstance.jsonObject!!.optJSONObject("0")) as Triple<String, String, JSONObject>)
        parts.add(Triple("팔꿉 측정 자세", singletonInstance.jsonObject?.optString("fileName"), singletonInstance.jsonObject!!.optJSONObject("1")) as Triple<String, String, JSONObject>)
        parts.add(Triple("오버헤드 스쿼트", singletonInstance.jsonObject?.optString("fileName"),singletonInstance.jsonObject!!.optJSONObject("2")) as Triple<String, String, JSONObject>)
        parts.add(Triple("왼쪽 측면 자세",singletonInstance.jsonObject?.optString("fileName") ,singletonInstance.jsonObject!!.optJSONObject("3")) as Triple<String, String, JSONObject>)
        parts.add(Triple("오른쪽 측면 자세", singletonInstance.jsonObject?.optString("fileName"),singletonInstance.jsonObject!!.optJSONObject("4")) as Triple<String, String, JSONObject>)
        parts.add(Triple("후면 자세",singletonInstance.jsonObject?.optString("fileName") ,singletonInstance.jsonObject!!.optJSONObject("5")) as Triple<String, String, JSONObject>)
        parts.add(Triple("의자 후면",singletonInstance.jsonObject?.optString("fileName") ,singletonInstance.jsonObject!!.optJSONObject("6")) as Triple<String, String, JSONObject>)

        val adapter = ReportRVAdapter(parts, this@ReportFragment, this@ReportFragment, binding.nsvR)
        binding.rvR.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvR.layoutManager = linearLayoutManager
        binding.rvR.smoothScrollToPosition(5)


    }
    private fun scrollToView(view: View) {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        val viewTop = location[1]

        val scrollViewLocation = IntArray(2)
        binding.nsvR.getLocationInWindow(scrollViewLocation)
        val scrollViewTop = scrollViewLocation[1]

        val scrollY = binding.nsvR.scrollY
        val scrollTo = scrollY + viewTop - scrollViewTop

        binding.nsvR.smoothScrollTo(0, scrollTo)
    }
    override fun onReportScroll(view: View) {
        scrollToView(view)
    }

    fun getCurrentMonthInKorean(month: YearMonth): String {
        return month.month.getDisplayName(TextStyle.FULL, Locale("ko"))
    }
}