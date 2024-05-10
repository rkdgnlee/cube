package com.example.mhg

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class ReportSkeletonFragment : Fragment() {
    lateinit var binding : FragmentReportSkeletonBinding
    val viewModel: UserViewModel by activityViewModels()
    lateinit var requestPermissions : ActivityResultLauncher<Set<String>>
    lateinit var  healthConnectClient : HealthConnectClient


    val endTime = LocalDateTime.now()
    val startTime = endTime.minusDays(1)
    val PERMISSIONS =
        setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getWritePermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getWritePermission(StepsRecord::class),
            HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)

            )
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
        Log.v("시간 설정", "startTime: $startTime, endTime: $endTime")

        val providerPackageName = "com.google.android.apps.healthdata"
        val availabilityStatus = HealthConnectClient.getSdkStatus(requireContext(), providerPackageName )
        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE) {
            return // 실행 가능한 통합이 없기 때문에 조기 복귀
        }
        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            // 선택적으로 패키지 설치 프로그램으로 리디렉션하여 공급자를 찾습니다. 예:
            val uriString = "market://details?id=$providerPackageName&url=healthconnect%3A%2F%2Fonboarding"
            requireContext().startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setPackage("com.android.vending")
                    data = Uri.parse(uriString)
                    putExtra("overlay", true)
                    putExtra("callerId", requireContext().packageName)
                }
            )
            return
        }
        healthConnectClient = HealthConnectClient.getOrCreate(requireContext())
        Log.v("헬커인스턴스", "$healthConnectClient")

        // Create the permissions launcher
        val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()
        requestPermissions = registerForActivityResult(requestPermissionActivityContract) { granted ->
            lifecycleScope.launch {
                if (granted.containsAll(PERMISSIONS)) {
                    Log.v("권한o", "$healthConnectClient")
                } else {
                    checkPermissionsAndRun(healthConnectClient)
                    Log.v("권한x", "$healthConnectClient")
                }
            }
        }
        lifecycleScope.launch {
            checkPermissionsAndRun(healthConnectClient)
        }
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
    private suspend fun checkPermissionsAndRun(healthConnectClient: HealthConnectClient) {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        if (granted.containsAll(PERMISSIONS)) {
            //권한이 이미 부여되었습니다. 데이터 삽입 또는 읽기를 진행합니다.

            aggregateStepsInto3oMins(healthConnectClient, startTime, endTime)

            val startTimeInstant = startTime.atZone(ZoneId.of("Asia/Seoul")).minusDays(1).toInstant()
            val endTimeInstant = endTime.atZone(ZoneId.of("Asia/Seoul")).toInstant()
            readStepsByTimeRange(healthConnectClient, startTimeInstant, endTimeInstant)
            readCaloryByTimeRange(healthConnectClient, startTimeInstant, endTimeInstant)
        } else {
            requestPermissions.launch(PERMISSIONS)
        }
    }

    private suspend fun aggregateStepsInto3oMins(
        healthConnectClient: HealthConnectClient,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ) {
        try {
            val response =
                healthConnectClient.aggregateGroupByDuration(
                    AggregateGroupByDurationRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                        timeRangeSlicer = Duration.ofHours(30)
                    )
                )
            val stepsList = mutableListOf<Long>()
            var previousSteps : Long? = null
            for (durationResult in response) {
                // The result may be null if no data is available in the time range
                val totalSteps = durationResult.result[StepsRecord.COUNT_TOTAL]

                if (totalSteps != null) {
                    if (previousSteps == null) {
                        stepsList.add(totalSteps)
                    } else {
                        stepsList.add(totalSteps - previousSteps)
                    }
                    previousSteps = totalSteps

                } else {
                    stepsList[0]
                }
                Log.v("걸음 수 누적", "$totalSteps")
            }
            Log.v("걸음 리스트", "$stepsList")
            Log.v("hour응답", "${response.size}")

        } catch (e: Exception) {
            Log.v("오류", "$e")
        }
    }

//    fun readSteps(startTime: Long, endTime: Long) {
//        val ESTIMATED_STEP_DELTAS = DataSource.Builder()
//            .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
//            .setType(DataSource.TYPE_DERIVED)
//            .setStreamName("estimated_steps")
//            .setAppPackageName("com.google.android.gms")
//            .build()
//        val readRequest = DataReadRequest.Builder()
//            .aggregate(ESTIMATED_STEP_DELTAS, DataType.AGGREGATE_STEP_COUNT_DELTA)
//            .bucketByTime(1, TimeUnit.DAYS)
//            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
//            .build()
//
//        Fitness.getHistoryClient(requireActivity(),
//            GoogleSignIn.getLastSignedInAccount(requireActivity())!!
//        )
//            .readData(readRequest)
//            .addOnSuccessListener { response ->
//                for (bucket in response.buckets) {
//                    val dataSets = bucket.dataSets
//                    for (dataSet in dataSets) {
//                        for (dp in dataSet.dataPoints) {
//                            for (field in dp.dataType.fields) {
//                                val steps = dp.getValue(field).asInt()
//                                    // 이제 steps 변수에는 startTime과 endTime 사이의 걸음 수가 저장되어 있습니다.
//                                Log.v("30분 걸음", "$steps")
//                            }
//                        }
//                    }
//                }
//            }
//            .addOnFailureListener { e ->
//                // 요청이 실패한 경우에 대한 처리를 여기에 작성합니다.
//            }
//    }

                // 요청이 실패한 경우에 대한 처리를 여기에 작
    private suspend fun readCaloryByTimeRange(
        healthConnectClient: HealthConnectClient,
        startTime: Instant,
        endTime: Instant
    ) {
        try {
            val response =
                healthConnectClient.readRecords(
                    ReadRecordsRequest(
                        TotalCaloriesBurnedRecord::class,
                        timeRangeFilter = TimeRangeFilter.before(endTime)
                    )
                )
            val energy = response.records[0].energy
            Log.v("ca", "${response.records.size}")


        } catch (e: Exception) {
            Log.v("원시오류", "$e")
        }
    }
    private suspend fun readStepsByTimeRange(
        healthConnectClient: HealthConnectClient,
        startTime: Instant,
        endTime: Instant
    ) {
        try {
            val response =
                healthConnectClient.readRecords(
                    ReadRecordsRequest(
                        StepsRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                    )
                )

            for (stepRecord in response.records) {
                // Process each step record
                Log.v("걸음 개수", "${response.records.size}")
                Log.v("총 걸음count", "${stepRecord.count}")
                Log.v("총 걸음meta", "${stepRecord.metadata}")
            }
            Log.v("원시데이터 기록", "${response.records.get(0)}, ${response.pageToken}")
        } catch (e: Exception) {
            Log.v("원시오류", "$e")
        }
    }
}
