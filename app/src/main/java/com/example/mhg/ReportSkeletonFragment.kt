package com.example.mhg

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.HomeVerticalRecyclerViewAdapter
import com.example.mhg.VO.ChartVO
import com.example.mhg.VO.ExerciseVO
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentReportSkeletonBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random

class ReportSkeletonFragment : Fragment() {
    lateinit var binding : FragmentReportSkeletonBinding
    val viewModel: UserViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReportSkeletonBinding.inflate(inflater)

        // ---- web view 연결 코드 시작 ----
        val websettings : WebSettings = binding.wvReportSkeleton.settings
        websettings.javaScriptEnabled = true
//        websettings.builtInZoomControls = false  // 내장 확대 축소 비활성화
//        websettings.displayZoomControls = false  // 확대 축소 비활성화
        binding.wvReportSkeleton.webViewClient = WebViewClient()
//        binding.wvReportSkeleton.loadUrl("https://www.google.com")

        // ---- web view 연결 코드 끝 ----

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ---- 달력 코드 시작 ----
        val c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
        var year = c.get(Calendar.YEAR)
        var month = c.get(Calendar.MONTH)
        var day = c.get(Calendar.DAY_OF_MONTH)
        var selectedDate = "$year. ${month + 1}. $day"
        binding.tvReportDateCurrent.text = selectedDate


        val datePickerDialog = DatePickerDialog(requireContext(), R.style.Theme_App,
            { view, year, month, day ->

                selectedDate = "$year. ${month + 1}. $day"
                binding.tvReportDateCurrent.text = selectedDate
                c.set(year, month, day)

                // -----! 날짜 선택 후 서버 응답 후 처리 시작 !-----
                // TODO 1.날짜형식 알맞게 수정하기
                val fetchDate = "$year-${month + 1}-$day"
                // TODO 2. 매니저님과 조율하여 매개변수 넣어서 조회
//                fetchUserHistoryJson() {
//                TODO 3. 데이터 값 전부 뿌리기 - viewModel.UserHistory.value?.optString("")
//                }



            // -----! 날짜 선택 후 서버 응답 후 처리 끝 !-----
        }, year, month, day)

        binding.btnReportCalendar.setOnClickListener {
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        binding.btnReportDateLeft.setOnClickListener {
            c.add(Calendar.DAY_OF_MONTH, -1) // 하루를 빼기
            year = c.get(Calendar.YEAR)
            month = c.get(Calendar.MONTH)
            day = c.get(Calendar.DAY_OF_MONTH)
            selectedDate = "$year. ${month + 1}. $day"
            binding.tvReportDateCurrent.text = selectedDate
        }
        binding.btnReportDateRight.setOnClickListener {
            val today = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
            if (!c.after(today)) {
                // 하루 더하기
                year = c.get(Calendar.YEAR)
                month = c.get(Calendar.MONTH)
                day = c.get(Calendar.DAY_OF_MONTH)
                c.add(Calendar.DAY_OF_MONTH, + 1)
                selectedDate = "$year. ${month + 1}. $day"
                binding.tvReportDateCurrent.text = selectedDate
            }
        }
//        binding.btnReportDateRight.setOnClickListener {
//            if (binding.tvReportDateCurrent.text.substring(0, 3).toInt() == year && binding.tvReportDateCurrent.text.substring(6).toInt() + 1 == month && binding.tvReportDateCurrent.text.substring(9, 10).toInt() == day ) { }
//            else {
//                year = c.get(Calendar.YEAR)
//                month = c.get(Calendar.MONTH)+1
//                day = c.get(Calendar.DAY_OF_MONTH)
//                c.add(Calendar.DAY_OF_MONTH, + 1)
//                selectedDate = "$year. ${month + 1}. $day"
//                binding.tvReportDateCurrent.text = selectedDate
//            }
//        }
        // ---- 달력 코드 끝 ----

    // ---- 하단 완료 목록 코드 시작 ----
        // 완료 목록 데이터 리스트 가져와야 함
        val verticaldatalist = ArrayList<ExerciseVO>()
        val adapter = HomeVerticalRecyclerViewAdapter(verticaldatalist ,"home")
        adapter.verticalList = verticaldatalist
        binding.rvSkeletonVertical.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvSkeletonVertical.layoutManager = linearLayoutManager

        // ---- 하단 완료 목록 코드 끝 ----

        // ---- 꺾은선 그래프 코드 시작 ----
        val lineChart = binding.lcReport
        val lcXAxis = lineChart.xAxis
        val lcYAxisLeft = lineChart.axisLeft
        val lcYAxisRight = lineChart.axisRight
        val lcLegend = lineChart.legend

        val lcDataList : MutableList<ChartVO> = mutableListOf()
        val startMonth = (month + 8) % 12
            for (i in 0 until 12) {
                val currentMonth = (startMonth + i) % 12
                val monthLabel = if (currentMonth == 0) "12월" else "${currentMonth}월"
                lcDataList.add(ChartVO(monthLabel, Random.nextInt(99)))
            }
        val lcEntries : MutableList<Entry> = mutableListOf()
        for (i in lcDataList.indices) {
            // entry는 y축에 넣는 데이터 형식을 말함. Entry의 1번째 인자는 x축의 데이터의 순서, 두 번째 인자는 y값
            lcEntries.add(Entry(i.toFloat(), lcDataList[i].commitNum.toFloat()))
        }
        val lcLineDataSet = LineDataSet(lcEntries, "")
        lcLineDataSet.apply {
            color = resources.getColor(R.color.mainColor, null)
            circleRadius = 3F
            lineWidth = 3F
            mode = LineDataSet.Mode.CUBIC_BEZIER
            valueTextSize = 0F
            setCircleColors(resources.getColor(R.color.mainColor))

        }

        lcXAxis.apply {
            textSize = 12f
            textColor = resources.getColor(R.color.grey600)
            labelRotationAngle = 2F
            setDrawAxisLine(true)
            setDrawGridLines(false)
            lcXAxis.valueFormatter = (IndexAxisValueFormatter(lcDataList.map { it.date }))
            setLabelCount(12, true)
            lcXAxis.setPosition(XAxis.XAxisPosition.BOTTOM)
            axisLineWidth = 1.5f
        }
        lcYAxisLeft.apply {
            setDrawGridLines(false)
            setDrawAxisLine(true)
            setDrawZeroLine(false)
            setLabelCount(6, false)
            setDrawLabels(true)
            textColor = resources.getColor(R.color.grey600)
            axisLineWidth = 1.5f
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
            notifyDataSetChanged()
            description.text = ""
            setScaleEnabled(false)
            invalidate()
        }
        // ---- 꺾은선 그래프 코드 끝 ----

        // ---- 막대 그래프 코드 시작 ----
        val barChart = binding.bcReport
        val bcXAxis = barChart.xAxis
        val bcYAxisLeft = barChart.axisLeft
        val bcYAxisRight = barChart.axisRight
        val bcLegend = barChart.legend

        val bcDataList : MutableList<ChartVO> = mutableListOf()

        for (i in 0 until 12) {
            val currentMonth = (startMonth + i) % 12
            val monthLabel = if (currentMonth == 0) "12월" else "${currentMonth}월"
            bcDataList.add(ChartVO(monthLabel, Random.nextInt(99)))
        }
        val rcEntries : MutableList<BarEntry> = mutableListOf()
        for (i in bcDataList.indices) {
            // entry는 y축에 넣는 데이터 형식을 말함. Entry의 1번째 인자는 x축의 데이터의 순서, 두 번째 인자는 y값
            rcEntries.add(BarEntry(i.toFloat(), bcDataList[i].commitNum.toFloat()))
        }
        val bcLineDataSet = BarDataSet(rcEntries, "")
        bcLineDataSet.apply {
            color = resources.getColor(R.color.mainColor, null)
            valueTextSize = 0F
        }

        bcXAxis.apply {
            textSize = 12f
            textColor = resources.getColor(R.color.grey600)
            labelRotationAngle = 1.5F
            setDrawAxisLine(true)
            setDrawGridLines(false)
            bcXAxis.valueFormatter = (IndexAxisValueFormatter(bcDataList.map { it.date }))
            setLabelCount(12, false)
            bcXAxis.setPosition(XAxis.XAxisPosition.BOTTOM)
            axisLineWidth = 1.2f
        }
        bcYAxisLeft.apply {
            setDrawGridLines(false)
            setDrawAxisLine(true)
            setDrawZeroLine(false)
            setLabelCount(6, false)

            setDrawLabels(true)
            textColor = resources.getColor(R.color.grey600)
            axisLineWidth = 1.5f
        }
        bcYAxisRight.apply {
            setDrawGridLines(false)
            setDrawAxisLine(false)
            setLabelCount(0, false)
            setDrawLabels(false)
        }
        bcLegend.apply {
            bcLegend.formSize = 0f
        }
        barChart.apply {
            data = BarData(bcLineDataSet)
            notifyDataSetChanged()
            description.text = ""
            setScaleEnabled(false)
            invalidate()
        }
        // ---- 막대 그래프 코드 끝 ----


    }
}
