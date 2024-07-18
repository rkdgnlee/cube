package com.tangoplus.tangoq

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tangoplus.tangoq.broadcastReceiver.AlarmReceiver
import com.tangoplus.tangoq.data.FavoriteViewModel
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.fragment.ExerciseFragment
import com.tangoplus.tangoq.fragment.FavoriteFragment
import com.tangoplus.tangoq.fragment.MainFragment
import com.tangoplus.tangoq.fragment.MeasureFragment
import com.tangoplus.tangoq.fragment.ProfileFragment
import com.tangoplus.tangoq.databinding.ActivityMainBinding
import com.tangoplus.tangoq.dialog.FeedbackDialogFragment
import com.tangoplus.tangoq.dialog.PlayThumbnailDialogFragment
import com.tangoplus.tangoq.`object`.DeepLinkUtil
import com.tangoplus.tangoq.`object`.NetworkHistory.fetchViewingHistory
import com.tangoplus.tangoq.`object`.Singleton_t_history
import com.tangoplus.tangoq.`object`.Singleton_t_user
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    val mViewModel : MeasureViewModel by viewModels()
    private val eViewModel : FavoriteViewModel by viewModels()

//    lateinit var requestPermissions : ActivityResultLauncher<Set<String>>
//    val backStack = Stack<Int>()
    private var selectedTabId = R.id.main
//    lateinit var  healthConnectClient : HealthConnectClient
//    val endTime = LocalDateTime.now()
//    val startTime = LocalDateTime.now().minusDays(1)
//
//    val PERMISSIONS =
//        setOf(
//            HealthPermission.getReadPermission(HeartRateRecord::class),
//            HealthPermission.getWritePermission(HeartRateRecord::class),
//            HealthPermission.getReadPermission(StepsRecord::class),
//            HealthPermission.getWritePermission(StepsRecord::class),
//            HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
//            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
//        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // ------! 다크모드 메뉴 이름 설정 시작 !------
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isNightMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES
//        binding.tvCurrentPage.text = (if (isNightMode) "내정보" else "내정보")
        // ------! 다크모드 메뉴 이름 설정 끝 !------



        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        val singletonTHistory = Singleton_t_history.getInstance(this@MainActivity)
        AlarmReceiver()

        // -----! 초기 화면 설정 !-----
        handlePendingDeepLink()
        val showMeasureFragment = intent.getBooleanExtra("showMeasureFragment", false)
        if (showMeasureFragment) {
            // MeasureFragment로 교체
            setCurrentFragment(R.id.measure);
        } else if (savedInstanceState == null) {
            // 초기 화면 설정
            setCurrentFragment(selectedTabId);
        }
        binding.bnbMain.itemIconTintList = null
        binding.bnbMain.isItemActiveIndicatorEnabled = false
        // -----! 초기 화면 설정 끝 !-----
        binding.bnbMain.setOnItemSelectedListener {
            if (selectedTabId != it.itemId) {
                selectedTabId = it.itemId
            }
            setCurrentFragment(selectedTabId)
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
        // TODO 시청기록 singleton으로 받아오기
//        lifecycleScope.launch {
//            singletonTHistory.viewingHistory = fetchViewingHistory(this@MainActivity, getString(R.string.IP_ADDRESS_t_viewing_history)).toMutableList()
//        }

//        binding.ibtnAlarm.setOnClickListener {
//            val intent = Intent(this@MainActivity, AlarmActivity::class.java)
//            startActivity(intent)
//        }

//         ------! 헬스 커넥트 연동 데이터 가져오기 시작 !------
//        val providerPackageName = "com.google.android.apps.healthdata"
//        val availabilityStatus = HealthConnectClient.getSdkStatus(this, providerPackageName )
//        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE) {
//            return // 실행 가능한 통합이 없기 때문에 조기 복귀
//        }
//        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
//             선택적으로 패키지 설치 프로그램으로 리디렉션하여 공급자를 찾습니다. 예:
//            val uriString = "market://details?id=$providerPackageName&url=healthconnect%3A%2F%2Fonboarding"
//            this@MainActivity.startActivity(
//                Intent(Intent.ACTION_VIEW).apply {
//                    setPackage("com.android.vending")
//                    data = Uri.parse(uriString)
//                    putExtra("overlay", true)
//                    putExtra("callerId", packageName)
//                }
//            )
//            return
//        }
//        healthConnectClient = HealthConnectClient.getOrCreate(this)
//        Log.v("현재 시간", "endTime: $endTime, startTime: $startTime")
//
//        healthConnectClient = HealthConnectClient.getOrCreate(this)
//        Log.v("현재 시간", "endTime: $endTime, startTime: $startTime")
//
//         Create the permissions launcher
//        val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()
//        requestPermissions = registerForActivityResult(requestPermissionActivityContract) { granted ->
//            lifecycleScope.launch {
//                if (granted.containsAll(PERMISSIONS)) {
//                    Log.v("권한o", "$healthConnectClient")
//                    aggregateData(healthConnectClient)
//                } else {
//                    Log.v("권한x", "$healthConnectClient")
//                    checkPermissionsAndRun(healthConnectClient)
//                }
//            }
//        }
//        lifecycleScope.launch {
//            checkPermissionsAndRun(healthConnectClient)
//        } // ------! 헬스 커넥트 연동 데이터 가져오기 끝 !------

    }

    private fun setCurrentFragment(itemId: Int) {
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
    // ------! 딥링크 처리 시작 !------
    private fun handlePendingDeepLink() {
        val prefs = getSharedPreferences("DeepLinkPrefs", Context.MODE_PRIVATE)
        val pendingDeepLink = prefs.getString("pending_deep_link", null)
        if (pendingDeepLink != null) {
            // 딥링크 처리
            handleDeepLink(Uri.parse(pendingDeepLink))
            // 처리 후 임시 저장된 딥링크 정보 삭제
            prefs.edit().remove("pending_deep_link").apply()
        }
    }

    fun handleDeepLink(uri: Uri) {
        val path = uri.path
        if (path?.startsWith("/content/") == true) {
            val encodedData = path.substringAfterLast("/")
            val exercise = DeepLinkUtil.decodeExercise(encodedData)
            if (exercise != null) {
                // exercise를 사용하여 필요한 작업 수행
                val dialogFragment = PlayThumbnailDialogFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable("ExerciseUnit", exercise)
                    }
                }
                dialogFragment.show(supportFragmentManager, "PlayThumbnailDialogFragment")

            } else {
                // 디코딩 실패 시 처리
                Log.e("ErrorDeepLink", "Failed to access deepLink")
            }
        }
    } // ------! 딥링크 처리 끝 !------

//    fun setFullLayout(frame: FrameLayout, const : ConstraintLayout) {
//        val constraintSet = ConstraintSet()
//        constraintSet.clone(const)
//        constraintSet.connect(frame.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
//        constraintSet.connect(frame.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0)
//        constraintSet.applyTo(const)
//        binding.cvCl.visibility = View.GONE
//        binding.bnbMain.visibility = View.GONE
//    }
//
//    fun setTopLayoutFull(frame: FrameLayout, const: ConstraintLayout) {
//        val constraintSet = ConstraintSet()
//        constraintSet.clone(const)
//        constraintSet.connect(frame.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0)
//        constraintSet.applyTo(const)
//        binding.cvCl.visibility = View.GONE
//
//    }
//    fun setOptiLayout(frame: FrameLayout, const: ConstraintLayout, cardView: CardView) {
//        val constraintSet = ConstraintSet()
//        constraintSet.clone(const)
//        constraintSet.connect(frame.id, ConstraintSet.TOP, cardView.id, ConstraintSet.BOTTOM, 0)
//        constraintSet.connect(frame.id, ConstraintSet.BOTTOM, binding.bnbMain.id, ConstraintSet.TOP, 0)
//        constraintSet.applyTo(const)
//        binding.cvCl.visibility = View.VISIBLE
//        binding.bnbMain.visibility = View.VISIBLE
//    }


//    private val onBackPressedCallback = object: OnBackPressedCallback(true) {
//        override fun handleOnBackPressed() {
//
//        }
//    }

//    private suspend fun checkPermissionsAndRun(healthConnectClient: HealthConnectClient) {
//        val granted = healthConnectClient.permissionController.getGrantedPermissions()
//        if (granted.containsAll(PERMISSIONS)) {
//            권한이 이미 부여되었습니다. 데이터 삽입 또는 읽기를 진행합니다.
//            aggregateData(healthConnectClient)
//        } else {
//            requestPermissions.launch(PERMISSIONS)
//        }
//    }
//    private suspend fun aggregateData(healthConnectClient: HealthConnectClient) {
//        val startTimeInstant = startTime.atZone(ZoneId.of("Asia/Seoul")).toInstant()
//        val endTimeInstant = endTime.atZone(ZoneId.of("Asia/Seoul")).toInstant()
//        val monthlyStart = startTime.minusDays(30).atZone(ZoneId.of("Asia/Seoul")).toInstant()
//
//        aggregateStepsInto3oMins(healthConnectClient, startTime, endTime)
//        readStepsByTimeRange(healthConnectClient, startTimeInstant, endTimeInstant)
//        readCaloryByTimeRange(healthConnectClient, startTimeInstant, endTimeInstant)
//
//    }
//    private suspend fun aggregateStepsInto3oMins(
//        healthConnectClient: HealthConnectClient,
//        startTime: LocalDateTime,
//        endTime: LocalDateTime
//    ) { try {
//        val response = healthConnectClient.aggregateGroupByDuration(
//            AggregateGroupByDurationRequest(
//                metrics = setOf(StepsRecord.COUNT_TOTAL),
//                timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
//                timeRangeSlicer = Duration.ofMinutes(30L)
//            )
//        )
//        val stepsList = mutableListOf<Long>()
//        var previousSteps : Long? = null
//        for (durationResult in response) {
//             The result may be null if no data is available in the time range
//            val totalSteps = durationResult.result[StepsRecord.COUNT_TOTAL]
//            if (totalSteps != null) {
//                if (previousSteps == null) {
//                    stepsList.add(totalSteps)
//                } else {
//                    stepsList.add(totalSteps - previousSteps)
//                }
//                previousSteps = totalSteps
//            } else {
//                stepsList[0]
//            }
//            Log.v("걸음 수 누적", "$totalSteps")
//        }
//        mViewModel.steps.value = stepsList
//        Log.v("걸음 리스트", "$stepsList")
//        Log.v("hour응답", "${response.size}")
//    } catch (e: Exception) {
//        Log.v("30분걸음오류", "$e")
//    }
//    }
//    private suspend fun readCaloryByTimeRange(
//        healthConnectClient: HealthConnectClient,
//        startTime: Instant,
//        endTime: Instant
//    ) {
//        try {
//            val response = healthConnectClient.readRecords(
//                ReadRecordsRequest(
//                    TotalCaloriesBurnedRecord::class,
//                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
//                )
//            )
//            val energy = response.records[0].energy.toString()
//            Log.v("총칼로리", energy)
//            mViewModel.calory.value = Math.round(energy.split(" ")[0].toDouble()).toString() + " Kcal"
//
//        } catch (e: Exception) {
//            Log.v("칼로리오류", "$e")
//        }
//    }
//    @SuppressLint("SetTextI18n")
//    private suspend fun readStepsByTimeRange(
//        healthConnectClient: HealthConnectClient,
//        startTime: Instant,
//        endTime: Instant
//    ) { try {
//        val response = healthConnectClient.readRecords(
//            ReadRecordsRequest(
//                StepsRecord::class,
//                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
//            )
//        )
//        mViewModel.totalSteps.value = response.records[0].count.toString()
//        binding.tvMsSteps.text = "${mViewModel.totalSteps.value} 보"
//        Log.v("총 걸음수", "${mViewModel.totalSteps.value}")
//    } catch (e: Exception) {
//        Log.v("총걸음오류", "$e")
//        binding.tvMsSteps.text = "0 보"
//    }
//    }

    override fun onResume() {
        super.onResume()
        // ------! 0일 때만 피드백 켜지게 !------
        val feedbackData = intent?.getSerializableExtra("feedback_finish") as? Triple<Int, String, Int>
        Log.v("intent>serializable", "$feedbackData")
        if (feedbackData != null) {
            if (eViewModel.isDialogShown.value == false) {
                eViewModel.exerciseLog.value = feedbackData

                // 이미 DialogFragment가 표시되어 있는지 확인
                val fragmentManager = supportFragmentManager
                val existingDialog = fragmentManager.findFragmentByTag("FeedbackDialogFragment")

                if (existingDialog == null) {
                    val dialog = FeedbackDialogFragment()
                    dialog.show(fragmentManager, "FeedbackDialogFragment")
                }
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
//    @Deprecated("Deprecated in Java")
//    override fun onBackPressed() {
//        if (backStack.size > 1) {
//            backStack.pop()
//            val itemId = backStack.peek()
//            binding.bnbMain.selectedItemId = itemId
//            selectedTabId = itemId
//            setCurrentFragment(itemId)
//        } else {
//            super.onBackPressed()
//        }
//    }

    // ------! 한 번 더 누르시면 앱이 종료됩니다. !------
    private var backPressedOnce = false
    private val backPressHandler = Handler(Looper.getMainLooper())
    private val backPressRunnable = Runnable { backPressedOnce = false }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (backPressedOnce) {
                // 뒤로 가기 버튼이 두 번 눌렸으므로, 콜백을 비활성화하고 super.onBackPressed()를 호출
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            } else {
                // 처음 눌렸을 때
                backPressedOnce = true
                Toast.makeText(this@MainActivity, "한 번 더 누르시면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show()
                backPressHandler.postDelayed(backPressRunnable, 1000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 핸들러의 콜백을 제거하여 메모리 누수를 방지
        backPressHandler.removeCallbacks(backPressRunnable)
    }
    // ------! 한 번 더 누르시면 앱이 종료됩니다. !------
}

