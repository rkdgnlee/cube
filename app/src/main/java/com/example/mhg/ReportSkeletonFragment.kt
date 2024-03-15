package com.example.mhg

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.HomeVerticalRecyclerViewAdapter
import com.example.mhg.VO.ChartVO
import com.example.mhg.VO.HomeRVBeginnerDataClass
import com.example.mhg.databinding.FragmentReportSkeletonBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.util.Calendar
import java.util.TimeZone

class ReportSkeletonFragment : Fragment() {
    lateinit var binding : FragmentReportSkeletonBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

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


        val datePickerDialog = DatePickerDialog(requireContext(), R.style.Theme_App, { view, year, month, day ->
            selectedDate = "$year. ${month + 1}. $day"
            binding.tvReportDateCurrent.text = selectedDate
            c.set(year, month, day)
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
        // ---- 달력 코드 끝 ----


        // ---- 그래프 코드 시작 ----
        val lineChart = binding.lcReport
        val xAxis = lineChart.xAxis
        val yAxisleft = lineChart.axisLeft
        val yAxisright = lineChart.axisRight
        val legend = lineChart.legend
        val datalist : List<ChartVO> = listOf(
            ChartVO("5월", 48),
            ChartVO("6월", 70),
            ChartVO("7월", 77),
            ChartVO("8월", 65),
            ChartVO("9월", 31),
            ChartVO("10월", 28),
            ChartVO("11월", 52),
            ChartVO("12월", 14),
            ChartVO("1월", 76),
            ChartVO("2월", 51),
            ChartVO("3월", 55),
            ChartVO("4월", 62),

        )
        val entries : MutableList<Entry> = mutableListOf()
        for (i in datalist.indices) {
            // entry는 y축에 넣는 데이터 형식을 말함. Entry의 1번째 인자는 x축의 데이터의 순서, 두 번째 인자는 y값
            entries.add(Entry(i.toFloat(), datalist[i].commitNum.toFloat()))
        }
        val lineDataSet = LineDataSet(entries, "")
        lineDataSet.apply {
            color = resources.getColor(R.color.orange, null)
            circleRadius = 3F
            lineWidth = 3F
            mode = LineDataSet.Mode.CUBIC_BEZIER
            valueTextSize = 0F
            setCircleColors(resources.getColor(R.color.orange))

        }
        xAxis.apply {
            textSize = 12f
            textColor = resources.getColor(R.color.textgrey)
            labelRotationAngle = 2F
            setDrawAxisLine(true)
            setDrawGridLines(false)
            xAxis.valueFormatter = (IndexAxisValueFormatter(datalist.map { it.date }))
            setLabelCount(12, true)
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM)
            axisLineWidth = 1.5f
        }
        yAxisleft.apply {
            setDrawGridLines(false)
            setDrawAxisLine(false)
            setDrawZeroLine(false)
            setLabelCount(0, false)
            setDrawLabels(false)
        }
        yAxisright.apply {
            setDrawGridLines(false)
            setDrawAxisLine(false)
            setLabelCount(0, false)
            setDrawLabels(false)
        }
        legend.apply {
            legend.formSize = 0f
        }
        lineChart.apply {
            data = LineData(lineDataSet)
            notifyDataSetChanged()
            description.text = ""
            setScaleEnabled(false)
            invalidate()
        }
        // ---- 그래프 코드 끝 ----

        // ---- 하단 완료 목록 코드 시작 ----
        // 완료 목록 데이터 리스트 가져와야 함
        val verticaldatalist = ArrayList<HomeRVBeginnerDataClass>()
        val adapter = HomeVerticalRecyclerViewAdapter(verticaldatalist)
        adapter.warmupList = verticaldatalist
        binding.rvSkeletonVertical.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvSkeletonVertical.layoutManager = linearLayoutManager

        // ---- 하단 완료 목록 코드 끝 ----


    }
}
