package com.tangoplus.tangoq

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tangoplus.tangoq.data.ExerciseViewModel
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.fragment.ExerciseFragment
import com.tangoplus.tangoq.fragment.FavoriteFragment
import com.tangoplus.tangoq.fragment.MainFragment
import com.tangoplus.tangoq.fragment.MeasureFragment
import com.tangoplus.tangoq.fragment.ProfileFragment
import com.tangoplus.tangoq.databinding.ActivityMainBinding
import com.tangoplus.tangoq.dialog.FeedbackDialogFragment
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Stack

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    val mViewModel : MeasureViewModel by viewModels()
    val eViewModel : ExerciseViewModel by viewModels()
    lateinit var requestPermissions : ActivityResultLauncher<Set<String>>
    val backStack = Stack<Int>()
    var selectedTabId = R.id.main
    lateinit var  healthConnectClient : HealthConnectClient
    val endTime = LocalDateTime.now()
    val startTime = LocalDateTime.now().minusDays(1)

    val PERMISSIONS =
        setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getWritePermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getWritePermission(StepsRecord::class),
            HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ------! 다크모드 메뉴 이름 설정 시작 !------
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isNightMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES
        binding.tvCurrentPage.text = (if (isNightMode) "내정보" else "내정보")
        // ------! 다크모드 메뉴 이름 설정 끝 !------

        // -----! 초기 화면 설정 !-----
        if (savedInstanceState == null) {
            backStack.push(selectedTabId)
            setCurrentFragment(selectedTabId)
        }
        binding.bnbMain.itemIconTintList = null
        binding.bnbMain.isItemActiveIndicatorEnabled = false

        binding.bnbMain.setOnItemSelectedListener {
            if (selectedTabId != it.itemId) {
                selectedTabId = it.itemId
                if (backStack.isEmpty() || backStack.peek() != it.itemId) {
                    backStack.push(it.itemId)
                }
            }

            setTopLayoutFull(binding.flMain, binding.clMain)
            setCurrentFragment(selectedTabId)
            binding.tvCurrentPage.text = when (it.itemId) {
                // ---- fragment 경로 지정 시작 ----
                R.id.main -> "메인"
                R.id.exercise -> "운동"
                R.id.measure -> "측정"
                R.id.favorite -> ""
                R.id.profile -> "내정보"
                else -> ""
            }
            if (it.itemId == R.id.favorite || it.itemId == R.id.exercise) {
                setTopLayoutFull(binding.flMain, binding.clMain)
            } else {
                setOptiLayout(binding.flMain,  binding.clMain ,binding.cvCl)
            }
            true

        }


        binding.bnbMain.setOnItemReselectedListener {
            when(it.itemId) {
                // ---- fragment 경로 지정 시작 ----
                R.id.main -> {}
                R.id.exercise -> {}
                R.id.measure -> {}
                R.id.favorite -> {}
                R.id.profile -> {}
            }
        }

        binding.ibtnAlarm.setOnClickListener {
            val intent = Intent(this@MainActivity, AlarmActivity::class.java)
            startActivity(intent)
        }

        // ------! 헬스 커넥트 연동 데이터 가져오기 시작 !------
        val providerPackageName = "com.google.android.apps.healthdata"
        val availabilityStatus = HealthConnectClient.getSdkStatus(this, providerPackageName )
        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE) {
            return // 실행 가능한 통합이 없기 때문에 조기 복귀
        }
        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            // 선택적으로 패키지 설치 프로그램으로 리디렉션하여 공급자를 찾습니다. 예:
            val uriString = "market://details?id=$providerPackageName&url=healthconnect%3A%2F%2Fonboarding"
            this@MainActivity.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setPackage("com.android.vending")
                    data = Uri.parse(uriString)
                    putExtra("overlay", true)
                    putExtra("callerId", packageName)
                }
            )
            return
        }
        healthConnectClient = HealthConnectClient.getOrCreate(this)
        Log.v("현재 시간", "endTime: $endTime, startTime: $startTime")

        healthConnectClient = HealthConnectClient.getOrCreate(this)
        Log.v("현재 시간", "endTime: $endTime, startTime: $startTime")

        // Create the permissions launcher
        val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()
        requestPermissions = registerForActivityResult(requestPermissionActivityContract) { granted ->
            lifecycleScope.launch {
                if (granted.containsAll(PERMISSIONS)) {
                    Log.v("권한o", "$healthConnectClient")
                    aggregateData(healthConnectClient)
                } else {
                    Log.v("권한x", "$healthConnectClient")
                    checkPermissionsAndRun(healthConnectClient)
                }
            }
        }
        lifecycleScope.launch {
            checkPermissionsAndRun(healthConnectClient)
        } // ------! 헬스 커넥트 연동 데이터 가져오기 끝 !------

    }

    fun setCurrentFragment(itemId: Int) {
        val fragment = when(itemId) {
            R.id.main -> MainFragment()
            R.id.exercise -> ExerciseFragment()
            R.id.measure -> MeasureFragment()
            R.id.favorite -> FavoriteFragment()
            R.id.profile -> ProfileFragment()

            else -> throw IllegalArgumentException("Invalid tab ID")
        }
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flMain, fragment)
//            addToBackStack(null)
            commit()
        }
    }
    fun setFullLayout(frame: FrameLayout, const : ConstraintLayout) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(const)
        constraintSet.connect(frame.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        constraintSet.connect(frame.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
        constraintSet.applyTo(const)
        binding.cvCl.visibility = View.GONE
        binding.bnbMain.visibility = View.GONE
    }

    fun setTopLayoutFull(frame: FrameLayout, const: ConstraintLayout) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(const)
        constraintSet.connect(frame.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
        constraintSet.applyTo(const)
        binding.cvCl.visibility = View.GONE

    }
    fun setOptiLayout(frame: FrameLayout, const: ConstraintLayout, cardView: CardView) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(const)
        constraintSet.connect(frame.id, ConstraintSet.TOP, cardView.id, ConstraintSet.BOTTOM, 0)
        constraintSet.connect(frame.id, ConstraintSet.BOTTOM, binding.bnbMain.id, ConstraintSet.TOP, 0)
        constraintSet.applyTo(const)
        binding.cvCl.visibility = View.VISIBLE
        binding.bnbMain.visibility = View.VISIBLE
    }


//    private val onBackPressedCallback = object: OnBackPressedCallback(true) {
//        override fun handleOnBackPressed() {
//
//        }
//    }

    private suspend fun checkPermissionsAndRun(healthConnectClient: HealthConnectClient) {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        if (granted.containsAll(PERMISSIONS)) {
            //권한이 이미 부여되었습니다. 데이터 삽입 또는 읽기를 진행합니다.
            aggregateData(healthConnectClient)
        } else {
            requestPermissions.launch(PERMISSIONS)
        }
    }
    private suspend fun aggregateData(healthConnectClient: HealthConnectClient) {
        val startTimeInstant = startTime.atZone(ZoneId.of("Asia/Seoul")).toInstant()
        val endTimeInstant = endTime.atZone(ZoneId.of("Asia/Seoul")).toInstant()
        val monthlyStart = startTime.minusDays(30).atZone(ZoneId.of("Asia/Seoul")).toInstant()

        aggregateStepsInto3oMins(healthConnectClient, startTime, endTime)
        readStepsByTimeRange(healthConnectClient, startTimeInstant, endTimeInstant)
        readCaloryByTimeRange(healthConnectClient, startTimeInstant, endTimeInstant)

    }
    private suspend fun aggregateStepsInto3oMins(
        healthConnectClient: HealthConnectClient,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ) { try {
        val response = healthConnectClient.aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                timeRangeSlicer = Duration.ofMinutes(30L)
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
        mViewModel.steps.value = stepsList
        Log.v("걸음 리스트", "$stepsList")
        Log.v("hour응답", "${response.size}")
    } catch (e: Exception) {
        Log.v("30분걸음오류", "$e")
    }
    }
    private suspend fun readCaloryByTimeRange(
        healthConnectClient: HealthConnectClient,
        startTime: Instant,
        endTime: Instant
    ) {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    TotalCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            val energy = response.records[0].energy.toString()
            Log.v("총칼로리", energy)
            mViewModel.calory.value = Math.round(energy.split(" ")[0].toDouble()).toString() + " Kcal"

        } catch (e: Exception) {
            Log.v("칼로리오류", "$e")
        }
    }
    @SuppressLint("SetTextI18n")
    private suspend fun readStepsByTimeRange(
        healthConnectClient: HealthConnectClient,
        startTime: Instant,
        endTime: Instant
    ) { try {
        val response = healthConnectClient.readRecords(
            ReadRecordsRequest(
                StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
        )
        mViewModel.totalSteps.value = response.records[0].count.toString()
//        binding.tvMsSteps.text = "${mViewModel.totalSteps.value} 보"
        Log.v("총 걸음수", "${mViewModel.totalSteps.value}")
    } catch (e: Exception) {
        Log.v("총걸음오류", "$e")
//        binding.tvMsSteps.text = "0 보"
    }
    }

    override fun onResume() {
        super.onResume()
        // ------! 0일 때만 피드백 켜지게 !------

        var feedbackData = intent?.getSerializableExtra("feedback_finish")
        if (feedbackData != null ) {
            if (eViewModel.isDialogShown.value == false) {
                eViewModel.exerciseLog.value = feedbackData as Triple<Int, String, Int>
                val dialog = FeedbackDialogFragment()
                dialog.show(supportFragmentManager, "FeedbackDialogFragment")
            } else {
                eViewModel.isDialogShown.value = true
            }
        }
    }
//    fun formattedUpsertionChange(change: UpsertionChange) {
//        when (change.record) {
//            is ExerciseSessionRecord -> {
//                val activity = change.record as ExerciseSessionRecord
//                FormattedChangeRow(
//                    startTime = dateTimeWithOffsetOrDefault(
//                        activity.startTime,
//                        activity.startZoneOffset
//                    ),
//                    recordType = "Exercise session",
//                    dataSource = change.record.metadata.dataOrigin.packageName
//                )
//            }
//        }
//    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (backStack.size > 1) {
            backStack.pop()
            val itemId = backStack.peek()
            binding.bnbMain.selectedItemId = itemId
            selectedTabId = itemId
            setCurrentFragment(itemId)
        } else {
            super.onBackPressed()
        }
    }
}

