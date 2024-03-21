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
import androidx.activity.viewModels
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.HomeVerticalRecyclerViewAdapter
import com.example.mhg.VO.ChartVO
import com.example.mhg.VO.HomeRVBeginnerDataClass
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentReportSkeletonBinding
import com.github.mikephil.charting.components.XAxis
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
import java.util.Calendar
import java.util.TimeZone
import kotlin.random.Random

class ReportSkeletonFragment : Fragment() {
    lateinit var binding : FragmentReportSkeletonBinding
    val viewModel: UserViewModel by viewModels()
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
            month = c.get(Calendar.MONTH)+1
            day = c.get(Calendar.DAY_OF_MONTH)
            selectedDate = "$year. ${month + 1}. $day"
            binding.tvReportDateCurrent.text = selectedDate
        }

        binding.btnReportDateRight.setOnClickListener {
            val today = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
            if (!c.after(today)) {
                 // 하루 더하기
                year = c.get(Calendar.YEAR)
                month = c.get(Calendar.MONTH)+1
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

        val datalist : MutableList<ChartVO> = mutableListOf()
        for (i in 0 until 12) {
            val currentMonth = (month +i) % 12
            if (currentMonth == 0) {
                datalist.add(ChartVO("12월", Random.nextInt(99)))
            } else {
                datalist.add(ChartVO("${currentMonth}월", Random.nextInt(99)))
            }
        }
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
        val adapter = HomeVerticalRecyclerViewAdapter(verticaldatalist, "home")
        adapter.verticalList = verticaldatalist
        binding.rvSkeletonVertical.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvSkeletonVertical.layoutManager = linearLayoutManager

        // ---- 하단 완료 목록 코드 끝 ----


    }
    fun fetchUserHistoryJson(myUrl : String, user_mobile:String, callback: () -> Unit) {
        val client = OkHttpClient()
//        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("${myUrl}users/read.php?user_mobile=$user_mobile")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OKHTTP3", "Failed to execute request!")
            }
            override fun onResponse(call: Call, response: Response)  {
                val responseBody = response.body?.string()
                Log.e("OKHTTP3", "Success to execute request!: $responseBody")
                val jsonObj__ = responseBody?.let { JSONObject(it) }
//              TODO  히스토리에 맞게 정보 받아와서 VIEWMODEL에 담아넣기
                val jsonObj = jsonObj__?.optJSONObject("data")
                viewModel.UserHistory.value = jsonObj
//                val t_userInstance = context?.let { Singleton_t_user.getInstance(requireContext()) }
//                t_userInstance?.jsonObject = jsonObj
//                Log.e("OKHTTP3>싱글톤", "${t_userInstance?.jsonObject}")
                callback()
            }
        })
    }

}
