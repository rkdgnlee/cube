package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.gson.Gson
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.ReportRVAdapter
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.databinding.FragmentReportBinding
import com.tangoplus.tangoq.listener.OnReportClickListener
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import com.tangoplus.tangoq.view.DayViewContainer
import com.tangoplus.tangoq.view.MonthHeaderViewContainer
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

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

    @OptIn(ExperimentalBadgeUtils::class)
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        singletonInstance = Singleton_t_measure.getInstance(requireContext())

        setBadgeOnFlR()
        binding.tvRMeasureHistory.text = "최근 측정 기록 - ${selectedDate.year}년 ${getCurrentMonthInKorean(currentMonth)} ${selectedDate.dayOfMonth}일"

        binding.ibtnRBack.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
                replace(R.id.flMain, MeasureFragment())
                commit()
            }
        }
//        binding.ibtnRAlarm.setOnClickListener {
//            val intent = Intent(requireContext(), AlarmActivity::class.java)
//            startActivity(intent)
//        }
        binding.vRDiseasePredict.setOnClickListener{
            requireActivity().supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                replace(R.id.flMain, ReportDiseaseFragment())
                commit()
            }
        }

        // ------! calendar 시작 !------
        binding.monthText.text = "${YearMonth.now().year}월 ${getCurrentMonthInKorean(currentMonth)}"
        binding.cvRCalendar.setup(currentMonth.minusMonths(24), currentMonth.plusMonths(0), DayOfWeek.SUNDAY)
        binding.cvRCalendar.scrollToMonth(currentMonth)
        binding.cvRCalendar.monthScrollListener = { month ->
            currentMonth = month.yearMonth
            binding.monthText.text = "${currentMonth.year}년 ${getCurrentMonthInKorean(currentMonth)}"
        }
        binding.nextMonthButton.setOnClickListener {
            // 선택된 날짜 초기화
            val oldDate = selectedDate
            selectedDate = null
            if (oldDate != null) {
                binding.cvRCalendar.notifyDateChanged(oldDate)
            }
            if (currentMonth != YearMonth.now()) {
                currentMonth = currentMonth.plusMonths(1)
                binding.monthText.text = "${currentMonth.year}년 ${getCurrentMonthInKorean(currentMonth)}"
                binding.cvRCalendar.scrollToMonth(currentMonth)
            }
        }

        binding.previousMonthButton.setOnClickListener {
            // 선택된 날짜 초기화
            val oldDate = selectedDate
            selectedDate = null
            if (oldDate != null) {
                binding.cvRCalendar.notifyDateChanged(oldDate)
            }

            if (currentMonth == YearMonth.now().minusMonths(24)) {

            } else {
                currentMonth = currentMonth.minusMonths(1)
                binding.monthText.text = "${currentMonth.year}년 ${getCurrentMonthInKorean(currentMonth)}"
                binding.cvRCalendar.scrollToMonth(currentMonth)
            }
        }
        binding.cvRCalendar.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthHeaderViewContainer> {
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

        binding.cvRCalendar.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.date.text = day.date.dayOfMonth.toString()
                container.date.textSize = 20f
                container.date.setPadding(24)
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
                        container.date.setTextColor(ContextCompat.getColor(container.date.context, R.color.subColor150))
                    }
                }

                container.date.setOnClickListener {
                    // 선택된 날짜를 업데이트 + UI 갱신
                    if (day.date <= LocalDate.now()) {
                        val oldDate = selectedDate
                        selectedDate = day.date
                        if (oldDate != null) {
                            binding.cvRCalendar.notifyDateChanged(oldDate)
                        }
                        binding.cvRCalendar.notifyDateChanged(selectedDate)

                        // ------! 하드 코딩 - 하루 전에 측정 기록 있음 !------
                        if (selectedDate == LocalDate.now().minusDays(1)) {
                            if (binding.mcvRShowPosture.visibility == View.GONE) {
                                binding.mcvRShowPosture.visibility = View.VISIBLE
                                binding.mcvRShowPosture.alpha = 0f
                                binding.mcvRShowPosture.animate().apply {
                                    duration = 150
                                    alpha(1f)
                                    withEndAction {
                                        binding.mcvRShowPosture.rotation = 0f
                                    }
                                }
                            }
                        } else if (binding.mcvRShowPosture.visibility == View.VISIBLE) {
                            binding.mcvRShowPosture.animate().apply {
                                duration = 150
                                alpha(0f)
                                withEndAction {
                                    binding.mcvRShowPosture.visibility = View.GONE
                                }
                            }
                        }
                    }
                }
                // ------! 자세 보기 버튼 클릭 시작 !------
                binding.btnRShowPosture.setOnClickListener {
                    binding.mcvRPosture.visibility = View.VISIBLE
                    binding.mcvRPosture.animate().apply {
                        duration = 150
                        rotation(0f)
                    }
                    Handler(Looper.getMainLooper()).postDelayed({
                        scrollToView(binding.btnRShowPosture)
                    }, 170)

                    binding.tvRMeasureDate.text = "측정일 - $selectedDate"
                }
            } // ------! 자세 보기 버튼 클릭 끝 !------

            override fun create(view: View): DayViewContainer {
                return DayViewContainer(view)
            }

        } // ------! calendar 끝 !------


        val parts = mutableListOf<MeasureVO>() // TODO Triple<분류, 파일명, 데이터>

        parts.add(MeasureVO(
            partName = "정면 자세",
            anglesNDistances = singletonInstance.jsonObject?.optJSONObject("0")
        ))
        parts.add(MeasureVO(partName = "팔꿉 측정 자세", anglesNDistances = singletonInstance.jsonObject?.optJSONObject("1")))

        // ------! 측정 관련 7가지 json에서 가져오기 시작 !------
        val squartVideo = JSONArray()
        for (i in generateSequence(2.0) { it + 0.1 }.takeWhile { it < 3.0 }) {
            squartVideo.put(singletonInstance.jsonObject!!.optJSONObject("$i"))
        }

        val squatHandsLeanValues = mutableListOf<Double>()
        val squatHipLeanValues = mutableListOf<Double>()
        val squatKneeLeanValues = mutableListOf<Double>()


        for (i in 0 until squartVideo.length()) {
            val jsonObj = squartVideo.optJSONObject(i)
            if (jsonObj != null) {
                val handsLean = jsonObj.optDouble("res_ov_hd_sq_fnt_horiz_ang_wrist", Double.NaN)
                val hipLean = jsonObj.optDouble("res_ov_hd_sq_fnt_horiz_ang_hip", Double.NaN)
                val kneeLean = jsonObj.optDouble("res_ov_hd_sq_fnt_horiz_ang_knee", Double.NaN)

                if (!handsLean.isNaN()) squatHandsLeanValues.add(handsLean)
                if (!hipLean.isNaN()) squatHipLeanValues.add(hipLean)
                if (!kneeLean.isNaN()) squatKneeLeanValues.add(kneeLean)
            }
        }

        // 평균값 계산
        val avgSquatHandsLean = if (squatHandsLeanValues.isNotEmpty()) squatHandsLeanValues.average() else 0.0
        val avgSquatHipLean = if (squatHipLeanValues.isNotEmpty()) squatHipLeanValues.average() else 0.0
        val avgSquatKneeLean = if (squatKneeLeanValues.isNotEmpty()) squatKneeLeanValues.average() else 0.0

        // 평균값을 새로운 JSONObject에 넣기
        val averagedJsonObject = JSONObject()
        averagedJsonObject.put("res_ov_hd_sq_fnt_horiz_ang_wrist", avgSquatHandsLean)
        averagedJsonObject.put("res_ov_hd_sq_fnt_horiz_ang_hip", avgSquatHipLean)
        averagedJsonObject.put("res_ov_hd_sq_fnt_horiz_ang_knee", avgSquatKneeLean)

        // 최종적으로 하나의 JSON에 넣기
        val resultJsonObject = JSONObject()
        resultJsonObject.put("2", averagedJsonObject)

        parts.add(MeasureVO(partName = "오버헤드 스쿼트", anglesNDistances = resultJsonObject))
        parts.add(MeasureVO(partName = "왼쪽 측면 자세", anglesNDistances = singletonInstance.jsonObject?.optJSONObject("3")))
        parts.add(MeasureVO(partName = "오른쪽 측면 자세", anglesNDistances = singletonInstance.jsonObject!!.optJSONObject("4")))
        parts.add(MeasureVO(partName = "후면 자세", anglesNDistances = singletonInstance.jsonObject!!.optJSONObject("5")))
        parts.add(MeasureVO(partName = "의자 후면", anglesNDistances = singletonInstance.jsonObject!!.optJSONObject("6")))
        // ------! 측정 관련 7가지 json에서 가져오기 끝 !------

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

    private fun getCurrentMonthInKorean(month: YearMonth): String {
        return month.month.getDisplayName(TextStyle.FULL, Locale("ko"))
    }

    @OptIn(ExperimentalBadgeUtils::class)
    private fun setBadgeOnFlR() {
        // ------! 분석 뱃지 시작 !------
        val badgeDrawable = BadgeDrawable.create(requireContext()).apply {
            backgroundColor = ContextCompat.getColor(requireContext(), R.color.mainColor)
            badgeGravity = BadgeDrawable.TOP_START
            horizontalOffset = 12  // 원하는 가로 간격 (픽셀 단위)
            verticalOffset = 12  // 원하는 세로 간격 (픽셀 단위)h
        }

        val layoutParams = binding.tvRBadge.layoutParams as FrameLayout.LayoutParams
        layoutParams.marginStart = 20  // 오른쪽 마진
        layoutParams.topMargin = 20  // 위쪽 마진
        binding.tvRBadge.layoutParams = layoutParams

        // 뱃지를 View에 연결
        binding.flR.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            BadgeUtils.attachBadgeDrawable(badgeDrawable, binding.tvRBadge, binding.flR)
        } // ------! 분석 뱃지 끝 !------
    }
}