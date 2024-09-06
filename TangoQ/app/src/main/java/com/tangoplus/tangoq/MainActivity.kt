package com.tangoplus.tangoq

import android.app.UiModeManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.tangoplus.tangoq.broadcastReceiver.AlarmReceiver
import com.tangoplus.tangoq.data.AnalysisVO
import com.tangoplus.tangoq.data.EpisodeVO
import com.tangoplus.tangoq.data.ExerciseViewModel
import com.tangoplus.tangoq.data.HistoryUnitVO
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.data.ProgramVO
import com.tangoplus.tangoq.data.UserViewModel
import com.tangoplus.tangoq.fragment.ExerciseFragment
import com.tangoplus.tangoq.fragment.MainFragment
import com.tangoplus.tangoq.fragment.ProfileFragment
import com.tangoplus.tangoq.databinding.ActivityMainBinding
import com.tangoplus.tangoq.dialog.CustomExerciseDialogFragment
import com.tangoplus.tangoq.dialog.FeedbackDialogFragment
import com.tangoplus.tangoq.fragment.MeasureFragment
import com.tangoplus.tangoq.`object`.NetworkProgram.fetchProgramVOBySn
import com.tangoplus.tangoq.`object`.Singleton_t_history
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private val eViewModel : ExerciseViewModel by viewModels()
    private val uViewModel : UserViewModel by viewModels()
//    private var pendingAction: (() -> Unit)? = null
//    lateinit var requestPermissions : ActivityResultLauncher<Set<String>>
//    val backStack = Stack<Int>()
    private var selectedTabId = R.id.main
    private lateinit var singletonMeasure : Singleton_t_measure
    private lateinit var singletonHistory : Singleton_t_history
    val historys =  mutableListOf<MutableList<EpisodeVO>>()
    private lateinit var program : ProgramVO
    private val _dataLoaded = MutableLiveData<Boolean>()
    val dataLoaded: LiveData<Boolean> = _dataLoaded
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
        selectedTabId = savedInstanceState?.getInt("selectedTabId") ?: R.id.main
        // ------! 다크모드 메뉴 이름 설정 끝 !------

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        AlarmReceiver()

        // -----! 초기 화면 설정 !-----
//        handlePendingDeepLink()

        setCurrentFragment(selectedTabId)
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
                // ------# fragment 경로 지정 #------
                R.id.main -> {}
                R.id.exercise -> {}
                R.id.measure -> {}
                R.id.profile -> {}
            }
        }

        // ------# 현재 진행 프로그램 #------
        lifecycleScope.launch {

            // TODO 여기서 기존 유저 -> 선택된 프로그램 불러오기 or 신규 유저 -> 본인이 원하는 프로그램 선택하기 전까지 빈 program

            if (uViewModel.User.value?.optString("current_program_id") == null) {
                program = ProgramVO(-1, "", "", "", 0, 0, 0,mutableListOf(), mutableListOf())
            } else {
                program = fetchProgramVOBySn(getString(R.string.IP_ADDRESS_t_exercise_programs), 10.toString())
            }
            eViewModel.currentProgram = program

            val mjo1 = JSONObject().apply {
                put("front_horizontal_angle_ear", -178.315)
                put("front_horizontal_distance_sub_ear", 0.17671)
                put("front_horizontal_angle_shoulder", -177.563)
                put("front_horizontal_distance_sub_shoulder", 1.6494)
                put("front_horizontal_angle_elbow", -179.518)
                put("front_horizontal_distance_sub_elbow", 0.454279)
                put("front_horizontal_angle_wrist", 179.518)
                put("front_horizontal_distance_sub_wrist", -0.57936)
                put("front_horizontal_angle_hip", -178.958)
                put("front_horizontal_distance_sub_hip", 0.405051)
                put("front_horizontal_angle_knee", -172.875)
                put("front_horizontal_distance_sub_knee", 1.86277)
                put("front_horizontal_angle_ankle", 0)
                put("front_horizontal_distance_sub_ankle", 0)

                put("front_horizontal_distance_wrist_left", 24.513)
                put("front_horizontal_distance_wrist_right", 20.9849)
                put("front_horizontal_distance_knee_left", 10.797)
                put("front_horizontal_distance_knee_right", 7.42002)
                put("front_horizontal_distance_ankle_left", 10.0549)
                put("front_horizontal_distance_ankle_right", 10.0549)

                put("front_vertical_angle_shoulder_elbow_left", 82.7757)
                put("front_vertical_angle_shoulder_elbow_right", 77.7995)
                put("front_vertical_angle_elbow_wrist_left", 91.9415)
                put("front_vertical_angle_elbow_wrist_right", 88.1221)
                put("front_vertical_angle_hip_knee_left", 93.2397)
                put("front_vertical_angle_hip_knee_right", 90.5673)
                put("front_vertical_angle_knee_ankle_left", 91.7899)
                put("front_vertical_angle_knee_ankle_right", 86.4604)
                put("front_vertical_angle_shoulder_elbow_wrist_left", 170.834)
                put("front_vertical_angle_shoulder_elbow_wrist_right", 169.677)
                put("front_vertical_angle_hip_knee_ankle_left", 178.55)
                put("front_vertical_angle_hip_knee_ankle_right", 175.893)

                // 팔꿉
                put("front_horizontal_angle_thumb", 0.0)
                put("front_horizontal_distance_sub_thumb", 0.0)
                put("front_horizontal_distance_thumb_left", 20.8453)
                put("front_horizontal_distance_thumb_right", 0.0)

                put("front_hand_angle_thumb_cmc_tip_left", 99.4623)
                put("front_hand_angle_thumb_cmc_tip_right", 0.0)
                put("front_hand_distance_index_pinky_mcp_left", 2.62518)
                put("front_hand_distance_index_pinky_mcp_right", 0.0)
                put("front_hand_angle_elbow_wrist_mid_finger_mcp_left", 176.108)
                put("front_hand_angle_elbow_wrist_mid_finger_mcp_right", 0.0)

                put("front_elbow_align_angle_left_upper_elbow_elbow_wrist", 30.9638)
                put("front_elbow_align_angle_right_upper_elbow_elbow_wrist", 36.6561)
                put("front_elbow_align_distance_left_wrist_shoulder", 7.08502)
                put("front_elbow_align_distance_right_wrist_shoulder", 5.98912)
                put("front_elbow_align_distance_wrist_height", -1.8121)

                put("front_elbow_align_distance_mid_index_height", 0.0)
                put("front_elbow_align_distance_shoulder_mid_index_left", 0.0)
                put("front_elbow_align_distance_shoulder_mid_index_right", 0.0)

                put("front_elbow_align_angle_mid_index_wrist_elbow_left", 0)
                put("front_elbow_align_angle_mid_index_wrist_elbow_right", 179.31)
                put("front_elbow_align_angle_left_shoulder_elbow_wrist", 21)
                put("front_elbow_align_angle_right_shoulder_elbow_wrist", 23.7181)

                put("front_elbow_align_distance_center_mid_finger_left", 0)
                put("front_elbow_align_distance_center_mid_finger_right", 5.31334)
                put("front_elbow_align_distance_center_wrist_left", 12.2275)
                put("front_elbow_align_distance_center_wrist_right", 9.36387)

                // 2. 측면
                put("side_left_horizontal_distance_shoulder", 1.48447)
                put("side_left_horizontal_distance_hip", 1.8291)
                put("side_left_horizontal_distance_pinky", 8.3724)
                put("side_left_horizontal_distance_wrist", 9.15559)

                put("side_left_vertical_angle_shoulder_elbow", 96.5819)
                put("side_left_vertical_angle_elbow_wrist", 100.376)
                put("side_left_vertical_angle_hip_knee", 91.7184)
                put("side_left_vertical_angle_ear_shoulder", 82.5686)
                put("side_left_vertical_angle_nose_shoulder", 55.9807)
                put("side_left_vertical_angle_shoulder_elbow_wrist", 176.206)
                put("side_left_vertical_angle_hip_knee_ankle", 173.925)


                put("side_right_horizontal_distance_shoulder", 4.0316)
                put("side_right_horizontal_distance_hip", 9.56569)
                put("side_right_horizontal_distance_pinky", 5.03904)
                put("side_right_horizontal_distance_wrist", 7.88262)

                put("side_right_vertical_angle_shoulder_elbow", 84.7376)
                put("side_right_vertical_angle_elbow_wrist", 104.421)
                put("side_right_vertical_angle_hip_knee", 86.0741)
                put("side_right_vertical_angle_ear_shoulder", 74.1975)
                put("side_right_vertical_angle_nose_shoulder", 51.8924)
                put("side_right_vertical_angle_shoulder_elbow_wrist", 160.317)
                put("side_right_vertical_angle_hip_knee_ankle", 173.142)

                // 3. 후면
                put("back_horizontal_angle_ear", -1.84761)
                put("back_horizontal_distance_sub_ear", 0.219524)
                put("back_horizontal_angle_shoulder", -1.71836)
                put("back_horizontal_distance_sub_shoulder", 0.329086)
                put("back_horizontal_angle_elbow", 1.49433)
                put("back_horizontal_distance_sub_elbow", -1.50296)
                put("back_horizontal_angle_wrist", 0.458356)
                put("back_horizontal_distance_sub_wrist", 0.186689)
                put("back_horizontal_angle_hip", 1.14576)
                put("back_horizontal_distance_sub_hip", -0.107886)
                put("back_horizontal_angle_knee", 12.5288)
                put("back_horizontal_distance_sub_knee", -2.45158)
                put("back_horizontal_angle_ankle", 9.61973)
                put("back_horizontal_distance_sub_ankle", -2.12846)

                put("back_horizontal_distance_wrist_left", 19.7794)
                put("back_horizontal_distance_wrist_right", 28.3849)
                put("back_horizontal_distance_knee_left", 8.36942)
                put("back_horizontal_distance_knee_right", 11.6458)

                put("back_vertical_angle_shoudler_center_hip", 88.1759)
                put("back_vertical_angle_nose_center_hip", 90)
                put("back_vertical_angle_nose_center_shoulder", 96.0725)
                put("back_vertical_angle_knee_heel_left", 95.2812)
                put("back_vertical_angle_knee_heel_right", 87.594)


                // 6. 앉아
                put("back_sit_horizontal_angle_ear", -2.86241)
                put("back_sit_horizontal_distance_sub_ear", 0.719041)
                put("back_sit_horizontal_angle_shoulder", -0.902221)
                put("back_sit_horizontal_distance_sub_shoulder", 0.627342)
                put("back_sit_horizontal_angle_hip", -1.59114)
                put("back_sit_horizontal_distance_sub_hip", 0.432825)

                // 이거 measureSkeleton에ㅐ 추가해야 함 
                put("back_sit_vertical_angle_nose_left_shoulder_right_shoulder", 48.6883) // 양 어깨와 코 삼각형의 왼 어깨 각도
                put("back_sit_vertical_angle_left_shoulder_right_shoulder_nose", 49.2687) // 양 어깨와 코 삼각형의 오른 어깨 각도
                put("back_sit_vertical_angle_right_shoulder_left_shoulder_nose", 82.043) // 양 어깨와 코 삼각형의 오른 어깨 각도
                // 삼각형의 각각의 각도
                put("back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder", 41.1265) // 양 어깨 - 골반 중앙 삼각형의 중앙 각도
                put("back_sit_vertical_angle_center_hip_right_shoulder_left_shoulder", 71.1699) // 양 어깨 - 골반 중앙 삼각형의 오른 어깨 각도
                put("back_sit_vertical_angle_right_shoulder_left_shoulder_center_hip", 67.7036) // 양 어깨 - 골반 중앙 삼각형의 왼 어깨 각도
                put("back_sit_vertical_angle_shoulder_center_hip", 87.1376) // 앉아서 어깨와 골반  중앙 각도



                // 4. 스쿼트
//                put("ohs_front_horizontal_angle_mid_finger_tip",)
//                put("ohs_front_horizontal_angle_hip",)
//                put("ohs_front_horizontal_angle_knee",)

            }

            convertJsonToAnalysisList(mjo1)
            val parts1 = mutableListOf<Pair<String, Int>>()
            parts1.add(Pair("어깨", 2))
            parts1.add(Pair("목", 2))
            parts1.add(Pair("손목", 1))
            parts1.add(Pair("무릎", 1))

            val measureVO1 = MeasureVO(
                "1",
                "2024.08.20",
                86,
                parts1,
                convertJsonToAnalysisList(mjo1)
            )


            val parts2 = mutableListOf<Pair<String, Int>>()
            parts2.add(Pair("목", 2))
            parts2.add(Pair("어깨", 1))
            parts2.add(Pair("손목", 1))
            parts2.add(Pair("골반", 1))
            val measureVO2 = MeasureVO(
                "2",
                "2024.08.01",
                79,
                parts2,
                mutableListOf()
            )



            // ------# singleton init #------
            singletonMeasure = Singleton_t_measure.getInstance(this@MainActivity)
            singletonMeasure.measures = mutableListOf()
            singletonMeasure.measures?.add(measureVO1)
            singletonMeasure.measures?.add(measureVO2)

            Log.v("singletonMeasure", "${singletonMeasure.measures}")

            // 기존 데이터 초기화
            for (weekIndex in 0 until (eViewModel.currentProgram?.programWeek!!)) {
                val weekEpisodes = mutableListOf<EpisodeVO>()
                for (episodeIndex in 0 until eViewModel.currentProgram!!.programEpisode) {
                    val historys = mutableListOf<HistoryUnitVO>()
                    var isAllFinished = true

                    when (weekIndex) { // 주차
                        0 -> {
                            when (episodeIndex) { // 회차
                                0 -> {
                                    for (k in 0 until (eViewModel.currentProgram?.exercises?.get(episodeIndex)?.size ?: 0)) {
                                        val historyUnit = HistoryUnitVO(
                                            eViewModel.currentProgram?.exercises?.get(episodeIndex)?.get(k)?.exerciseId,
                                            1,
                                            0,
                                            "2024-08-26 16:00:24" // TODO 실제 데이터는 각각의 regDate가 다름
                                        )
                                        historys.add(historyUnit)
                                    }

                                    isAllFinished = true
                                }
                                1 -> {
                                    for (k in 0 until (eViewModel.currentProgram?.exercises?.get(weekIndex)?.size ?: 0)) {
                                        val historyUnit = HistoryUnitVO(
                                            eViewModel.currentProgram?.exercises?.get(weekIndex)?.get(k)?.exerciseId,
                                            1,
                                            0,
                                            "2024-08-27 17:51:24"
                                        )
                                        historys.add(historyUnit)
                                    }
                                    isAllFinished = true
                                }
                                2 -> {
                                    for (k in 0 until (eViewModel.currentProgram?.exercises?.get(weekIndex)?.size ?: 0)) {
                                        val historyUnit = HistoryUnitVO(
                                            eViewModel.currentProgram?.exercises?.get(weekIndex)?.get(k)?.exerciseId,
                                            1,
                                            0,
                                            "2024-08-29 18:26:24"
                                        )
                                        historys.add(historyUnit)
                                    }
                                    isAllFinished = true

                                }
                                else -> {
                                    for (k in 0 until 4) {
                                        val historyUnit = HistoryUnitVO(
                                            eViewModel.currentProgram?.exercises?.get(weekIndex)?.get(k)?.exerciseId,
                                            1,
                                            0,
                                            "2024-08-31 20:11:57"
                                        )
                                        historys.add(historyUnit)
                                    }
                                    val historyUnit = HistoryUnitVO(
                                        eViewModel.currentProgram?.exercises?.get(weekIndex)?.get(4)?.exerciseId,
                                        0,
                                        Random.nextInt(0, eViewModel.currentProgram?.exercises?.get(weekIndex)?.get(4)?.videoDuration!!.toInt()),
                                        "2024-08-31 20:13:57"
                                    )
                                    historys.add(historyUnit)
                                    for (k in 5 until 7) {
                                        val historyUnit = HistoryUnitVO(
                                            eViewModel.currentProgram?.exercises?.get(weekIndex)?.get(k)?.exerciseId,
                                            0,
                                            0,
                                            null
                                        )
                                        historys.add(historyUnit)
                                    }
                                    isAllFinished = false
                                }
                            }

                        }
                        1 -> { // 2주차
                            when (episodeIndex) { // 회차
                                0 -> {
                                    for (k in 0 until (eViewModel.currentProgram?.exercises?.get(episodeIndex)?.size!! - 1)) {
                                        val historyUnit = HistoryUnitVO(
                                            eViewModel.currentProgram?.exercises?.get(weekIndex)?.get(k)?.exerciseId,
                                            1,
                                            0,
                                            "2024-09-02 22:54:12"
                                        )
                                        historys.add(historyUnit)
                                    }
                                    isAllFinished = false
                                    val historyUnit = HistoryUnitVO(
                                        eViewModel.currentProgram?.exercises?.get(weekIndex)?.get(6)?.exerciseId,
                                        0,
                                        0,
                                        null
                                        )
                                        historys.add(historyUnit)

                                }
                                1 -> {
                                    for (k in 0 until (eViewModel.currentProgram?.exercises?.get(weekIndex)?.size ?: 0)) {
                                        val historyUnit = HistoryUnitVO(
                                            eViewModel.currentProgram?.exercises?.get(weekIndex)?.get(k)?.exerciseId,
                                            1,
                                            0,
                                            "2024-09-03 20:39:44"
                                        )
                                        historys.add(historyUnit)
                                    }
                                    isAllFinished = true
                                }
                                2 -> {
                                    for (k in 0 until 3) {
                                        val historyUnit = HistoryUnitVO(
                                            eViewModel.currentProgram?.exercises?.get(weekIndex)?.get(k)?.exerciseId,
                                            1,
                                            0,
                                            "2024-09-04 17:54:24"
                                        )
                                        historys.add(historyUnit)
                                    }
                                    val historyUnit = HistoryUnitVO(
                                        eViewModel.currentProgram?.exercises?.get(weekIndex)?.get(3)?.exerciseId,
                                        0,
                                        Random.nextInt(0, eViewModel.currentProgram?.exercises?.get(weekIndex)?.get(3)?.videoDuration!!.toInt()),
                                        "2024-09-04 19:24:11")
                                    historys.add(historyUnit)
                                    for (k in 3 until 7) {
                                        val historyUnit = HistoryUnitVO(
                                            eViewModel.currentProgram?.exercises?.get(weekIndex)?.get(k)?.exerciseId,
                                            0,
                                            0,
                                            null
                                        )
                                        historys.add(historyUnit)
                                    }
                                    isAllFinished = false

                                }
                                else -> {
                                    for (k in 0 until (eViewModel.currentProgram?.exercises?.get(weekIndex)?.size ?: 0)) {
                                        val historyUnit = HistoryUnitVO(
                                            eViewModel.currentProgram?.exercises?.get(weekIndex)?.get(k)?.exerciseId,
                                            0,
                                            0,
                                            null
                                        )
                                        historys.add(historyUnit)
                                    }
                                    isAllFinished = false
                                }
                            }
                        }
                        else -> { // 나머지 주차 들
                            for (k in 0 until (eViewModel.currentProgram?.exercises?.get(episodeIndex)?.size ?: 0)) {
                                val historyUnit = HistoryUnitVO(
                                    eViewModel.currentProgram?.exercises?.get(episodeIndex)?.get(k)?.exerciseId,
                                    0,
                                    0,

                                    null
                                    )
                                historys.add(historyUnit)
                            }
                            isAllFinished = false
                        }
                    }

                    val episodeVO = EpisodeVO(eViewModel.currentProgram?.programSn.toString(), isFinish = isAllFinished, historys)
                    weekEpisodes.add(episodeVO)
                }
                historys.add(weekEpisodes)
                // episdoes는 episode가 모인 회차 1개를 모인 거임. 그럼?

            }
            singletonHistory = Singleton_t_history.getInstance(this@MainActivity)
            singletonHistory.historys = historys


            for (i in 0 until historys.size) {
                for (j in 0 until historys[i].size) {
                    // 하나의 하루간 운동량 만들고 넣고 초기화
                    var regDate = ""
                    var progressTime = 0
                    var finishedExercise = 0

                    for (k in 0 until historys[i][j].doingExercises.size) {
                        val historyUnit = historys[i][j].doingExercises
                        // 리스트가 일자별로 나눠져 있음
                        regDate = historyUnit[0].regDate.toString()
                        // 하루 간 진행 시간 더하기
                        if (historyUnit[k].lastPosition!! > 0) {
                            progressTime += historyUnit[k].lastPosition!!

                        } else if (historyUnit[k].lastPosition == 0 && historyUnit[k].viewCount!! > 0) {
                            progressTime += getTime(historyUnit[k].exerciseId.toString())
                            finishedExercise += 1
                        }


                        // ------# 전체 운동 담는 공간 #------
                        eViewModel.allHistorys.add(historyUnit[k])

                    }
                    eViewModel.classifiedByDay.add(Triple(regDate, progressTime, finishedExercise))
                }
            }
            // 전체 반복문 빠져나옴

            // ------# 그래프에 들어갈 가장 최근 일주일간 데이터 가져오기 #------
            eViewModel.weeklyHistorys = getWeeklyExerciseHistory(eViewModel.classifiedByDay)

            // ------# dates만 넣기(달력에 들어갈 것들) #------
            val historysUntilToday = eViewModel.classifiedByDay.filter { it.first != "null" }
            for (i in 0 until historysUntilToday.size) {
                eViewModel.datesClassifiedByDay.add(stringToLocalDate(historysUntilToday[i].first))
            }
            Log.v("VM>datesClassifiedByDay", "${eViewModel.classifiedByDay}")
            _dataLoaded.value = true
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


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("selectedTabId", selectedTabId)
    }

    fun setCurrentFragment(itemId: Int) {
        val fragment = when(itemId) {
            R.id.main -> MainFragment()
            R.id.exercise -> ExerciseFragment()
            R.id.measure -> MeasureFragment()
            R.id.profile -> ProfileFragment()

            else -> throw IllegalArgumentException("Invalid tab ID")
        }
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flMain, fragment)
            addToBackStack(null)
            commit()
        }

//        supportFragmentManager.executePendingTransactions()
//        pendingAction?.invoke()
//        pendingAction = null
//        if (itemId == R.id.measure) {
//            Handler(Looper.getMainLooper()).post {
//                (supportFragmentManager.findFragmentById(R.id.flMain) as? MeasureFragment)?.selectSecondTab()
//            }
//        }
    }
//    // ------! 딥링크 처리 시작 !------
//    private fun handlePendingDeepLink() {
//        val prefs = getSharedPreferences("DeepLinkPrefs", Context.MODE_PRIVATE)
//        val pendingDeepLink = prefs.getString("pending_deep_link", null)
//        if (pendingDeepLink != null) {
//            // 딥링크 처리
//            handleDeepLink(Uri.parse(pendingDeepLink))
//            // 처리 후 임시 저장된 딥링크 정보 삭제
//            prefs.edit().remove("pending_deep_link").apply()
//        }
//    }
//
//    fun handleDeepLink(uri: Uri) {
//        val path = uri.path
//        if (path?.startsWith("/content/") == true) {
//            val encodedData = path.substringAfterLast("/")
//            val exercise = DeepLinkUtil.decodeExercise(encodedData)
//            if (exercise != null) {
//                // exercise를 사용하여 필요한 작업 수행
//                val dialogFragment = PlayThumbnailDialogFragment().apply {
//                    arguments = Bundle().apply {
//                        putParcelable("ExerciseUnit", exercise)
//                    }
//                }
//                dialogFragment.show(supportFragmentManager, "PlayThumbnailDialogFragment")
//
//            } else {
//                // 디코딩 실패 시 처리
//                Log.e("ErrorDeepLink", "Failed to access deepLink")
//            }
//        }
//    } // ------! 딥링크 처리 끝 !------

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
            val fragmentManager = supportFragmentManager

            if (fragmentManager.backStackEntryCount <= 1 || isCurrentFragmentEmpty()) {
                // 백 스택에 entry가 1개 이하이거나 현재 프래그먼트가 비어있으면 앱 종료 로직 실행
                if (backPressedOnce) {
                    isEnabled = false
                    finishAffinity() // 앱을 완전히 종료
                } else {
                    backPressedOnce = true
                    Toast.makeText(this@MainActivity, "한 번 더 누르시면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show()
                    backPressHandler.postDelayed(backPressRunnable, 1000)
                }
            } else {
                // 그 외의 경우 이전 프래그먼트로 돌아갑니다.
                fragmentManager.popBackStack()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 핸들러의 콜백을 제거하여 메모리 누수를 방지
        backPressHandler.removeCallbacks(backPressRunnable)
    }
    // ------! 한 번 더 누르시면 앱이 종료됩니다. !------

    private fun isCurrentFragmentEmpty(): Boolean {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.flMain)
        return currentFragment == null || currentFragment.view == null || !currentFragment.isVisible
    }

    private fun getTime(id: String) : Int {
        return eViewModel.currentProgram?.exerciseTimes?.find { it.first == id }?.second!!
    }

    private fun getWeeklyExerciseHistory(data: MutableList<Triple<String, Int, Int>>): MutableList<Triple<String, Int, Int>> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val currentDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val sevenDaysAgo = currentDateTime.minusDays(6).truncatedTo(ChronoUnit.DAYS)

        val filteredData = data.filter {
            it.first != "null" &&
                    LocalDateTime.parse(it.first, formatter).isAfter(sevenDaysAgo.minusSeconds(1)) &&
                    LocalDateTime.parse(it.first, formatter).isBefore(currentDateTime.plusDays(1).truncatedTo(
                        ChronoUnit.DAYS))
        }.toMutableList()


        val completeData = mutableListOf<Triple<String, Int, Int>>()
        for (i in 0..6) {
            val date = sevenDaysAgo.plusDays(i.toLong())
            val nextDate = date.plusDays(1)

            val entry = filteredData.find {
                val entryDateTime = LocalDateTime.parse(it.first, formatter)
                entryDateTime.isAfter(date.minusSeconds(1)) && entryDateTime.isBefore(nextDate)
            }

            if (entry != null) {
                completeData.add(entry)
            } else {
                // 빈 데이터의 경우 해당 날짜의 자정(00:00:00)으로 설정
                val dateString = date.format(formatter)
                completeData.add(Triple(dateString, 0, 0))
            }
        }

        Log.v("completedData", "$completeData")
        return completeData
    }

    private fun stringToLocalDate(dateTimeString: String): LocalDate {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val localDateTime = LocalDateTime.parse(dateTimeString, formatter)
        return localDateTime.toLocalDate()
    }

    private fun convertJsonToAnalysisList(jsonObject: JSONObject) : MutableList<AnalysisVO> {
        val resultList = mutableListOf<AnalysisVO>()

        val partMap = mapOf(
            "ear" to 0, "shoulder" to 1, "elbow" to 2, "wrist" to 3, "hip" to 4, "knee" to 5, "ankle" to 6
        )
        fun mapJsonKeyToAnalysisVO(key:String, value: Double) : AnalysisVO {
            val keyParts = key.split("_")
            val type = (keyParts.contains("vertical"))

            val part = keyParts.find { partMap.containsKey(it) }?.let { partMap[it] } ?: 0

            val sequence = when {
                key.startsWith("front_vertical") || key.startsWith("front_horizontal") -> 0
                key.startsWith("front_elbow") -> 1
                key.startsWith("side_left") -> 3
                key.startsWith("side_right") -> 4
                key.startsWith("back_vertical") || key.startsWith("back_horizontal") -> 5
                key.startsWith("back_sit") -> 6
                else -> 2 // 기본값
            }

            return AnalysisVO(sequence, type, key, value)
        }

        jsonObject.keys().forEach { key ->
            val value = jsonObject.getDouble(key)
            resultList.add(mapJsonKeyToAnalysisVO(key, value))
        }
        return resultList
    }
}

