package com.example.mhg

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import androidx.lifecycle.lifecycleScope
import com.example.mhg.databinding.FragmentHomeIntermediateBinding
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.TimeZone


class HomeIntermediateFragment : Fragment() {
    lateinit var binding: FragmentHomeIntermediateBinding
    lateinit var requestPermissions : ActivityResultLauncher<Set<String>>
    lateinit var  healthConnectClient : HealthConnectClient


    val endTime = LocalDateTime.now()
    val startTime = endTime.minusDays(10)
    val PERMISSIONS =
        setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getWritePermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getWritePermission(StepsRecord::class),

        )
//    val permissionController = HealthConnectClient.getOrCreate(requireContext()).permissionController

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeIntermediateBinding.inflate(inflater)
        return binding.root
    }

//    @RequiresApi(Build.VERSION_CODES.O)
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
                    aggregateStepsIntoHours(healthConnectClient, startTime, endTime)
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
    }
    private suspend fun checkPermissionsAndRun(healthConnectClient: HealthConnectClient) {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        if (granted.containsAll(PERMISSIONS)) {
            //권한이 이미 부여되었습니다. 데이터 삽입 또는 읽기를 진행합니다.
            aggregateStepsIntoHours(healthConnectClient, startTime, endTime)

            val startTimeInstant = startTime.atZone(ZoneId.of("Asia/Seoul")).toInstant()
            val endTimeInstant = endTime.atZone(ZoneId.of("Asia/Seoul")).toInstant()
            readStepsByTimeRange(healthConnectClient, startTimeInstant, endTimeInstant)
        } else {
            requestPermissions.launch(PERMISSIONS)
        }
    }

    private suspend fun aggregateStepsIntoHours(
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
                        timeRangeSlicer = Duration.ofHours(1)
                    )
                )
            Log.v("hour응답", "${response.size}")
            for (durationResult in response) { // 값이 없어서 오류가 나오는 중
                // The result may be null if no data is available in the time range
                val totalSteps = durationResult.result[StepsRecord.COUNT_TOTAL]
                Log.v("총 걸음수", "$totalSteps")
            }
            Log.v("기록", "$response")
        } catch (e: Exception) {
            Log.v("오류", "$e")
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
                        timeRangeFilter = TimeRangeFilter.before(endTime)
                    )
                )
            Log.v("원시데이터", "${response.records}")
            for (stepRecord in response.records) {
            // Process each step record
            }
            Log.v("원시데이터 기록", "${response.records.get(0)}, ${response.pageToken}")
        } catch (e: Exception) {
            Log.v("원시오류", "$e")
        }
    }


}