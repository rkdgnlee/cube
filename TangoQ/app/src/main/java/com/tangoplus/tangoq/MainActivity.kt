package com.tangoplus.tangoq

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tangoplus.tangoq.broadcastReceiver.AlarmReceiver
import com.tangoplus.tangoq.data.HistoryVO
import com.tangoplus.tangoq.data.ExerciseViewModel
import com.tangoplus.tangoq.data.HistoryUnitVO
import com.tangoplus.tangoq.data.HistoryViewModel
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.data.ProgramVO
import com.tangoplus.tangoq.data.UserViewModel
import com.tangoplus.tangoq.fragment.ExerciseFragment
import com.tangoplus.tangoq.fragment.MainFragment
import com.tangoplus.tangoq.fragment.ProfileFragment
import com.tangoplus.tangoq.databinding.ActivityMainBinding
import com.tangoplus.tangoq.dialog.FeedbackDialogFragment
import com.tangoplus.tangoq.fragment.MeasureDetailFragment
import com.tangoplus.tangoq.fragment.MeasureFragment
import com.tangoplus.tangoq.`object`.NetworkProgram.fetchProgramVOBySn
import com.tangoplus.tangoq.`object`.Singleton_t_history
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
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
    private val hViewModel : HistoryViewModel by viewModels()
    private val mViewModel : MeasureViewModel by viewModels()
    private var selectedTabId = R.id.main
    private lateinit var singletonMeasure : Singleton_t_measure
    private lateinit var singletonHistory : Singleton_t_history
    var historys =  mutableListOf<HistoryVO>()
    private lateinit var program : ProgramVO
    private val _dataLoaded = MutableLiveData<Boolean>()
    val dataLoaded: LiveData<Boolean> = _dataLoaded
    private lateinit var measureSkeletonLauncher: ActivityResultLauncher<Intent>

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
        // ------# 측정 완료 후 측정 디테일 화면으로 바로 가기 #------
        measureSkeletonLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val finishedMeasure = result.data?.getBooleanExtra("finishedMeasure", false) ?: false
                if (finishedMeasure) {
                    mViewModel.selectedMeasureDate.value = singletonMeasure.measures?.get(0)?.regDate
                    mViewModel.selectedMeasure = singletonMeasure.measures?.get(0)
                    val bnb : BottomNavigationView = findViewById(R.id.bnbMain)
                    bnb.selectedItemId = R.id.measure
                    supportFragmentManager.beginTransaction().apply {
                        replace(R.id.flMain, MeasureDetailFragment())
                        addToBackStack(null)
                        commit()
                    }
                }
            }
        }

        // ------# 현재 진행 프로그램 #------
        CoroutineScope(Dispatchers.Main).launch {

            // TODO 여기서 기존 유저 -> 선택된 프로그램 불러오기 or 신규 유저 -> 본인이 원하는 프로그램 선택하기 전까지 빈 program

            val mjo1 = JSONArray().apply {
                put(loadJsonData())
                put(loadJsonArray())
                put(loadJsonData())
                put(loadJsonData())
                put(loadJsonData())
                put(loadJsonData())
                put(loadJsonData())
            }
            val parts1 = mutableListOf<Pair<String, Int>>()
            parts1.add(Pair("어깨", 2))
            parts1.add(Pair("목", 2))
            parts1.add(Pair("손목", 1))
            parts1.add(Pair("무릎", 1))

            val measureVO1 = MeasureVO(
                "1",
                "2024-08-20 20:12:08",
                86,
                parts1,
                mjo1,
                // 총 7개
                mutableListOf(
                    getUrl("MT_STATIC_BACK_61_20240604143755.jpg", true),
                    getUrl("MT_DYNAMIC_OVERSQUAT_FRONT_1_1_20240606135241.mp4", false),
                    getUrl("MT_STATIC_BACK_61_20240604143755.jpg", true),
                    getUrl("MT_STATIC_BACK_61_20240604143755.jpg", true),
                    getUrl("MT_STATIC_BACK_61_20240604143755.jpg", true),
                    getUrl("MT_STATIC_BACK_61_20240604143755.jpg", true),
                    getUrl("MT_STATIC_BACK_61_20240604143755.jpg", true)
                ),
                false,
                mutableListOf(Pair("신체 능력 향상, 전신 강화 루틴",  10), Pair("거북목 스트레칭", 4), Pair("러닝 전 부상 방지 전신 스트레칭", 3))
            )
            Log.v("measureVO1", "${measureVO1.fileUris}")

            val parts2 = mutableListOf<Pair<String, Int>>()
            parts2.add(Pair("목", 2))
            parts2.add(Pair("어깨", 1))
            parts2.add(Pair("손목", 1))
            parts2.add(Pair("골반", 1))

            val measureVO2 = MeasureVO(
                "2",
                "2024-08-01 17:55:21",
                79,
                parts2,
                JSONArray(),
                mutableListOf(),
                false,
                mutableListOf()
            )

            // ------# 싱글턴 측정 결과 init #------
            singletonMeasure = Singleton_t_measure.getInstance(this@MainActivity)
            singletonMeasure.measures = mutableListOf()
            singletonMeasure.measures?.add(measureVO1)
            singletonMeasure.measures?.add(measureVO2)
            mViewModel.selectedMeasure = measureVO1
            mViewModel.selectedMeasureDate.value = singletonMeasure.measures?.get(0)?.regDate

            _dataLoaded.value = true
        }
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
        clearCache()
    }

    private fun clearCache() {
        val cacheDir = cacheDir // 앱의 캐시 디렉토리 가져오기
        cacheDir?.let {
            deleteDir(it)
        }
    }
    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            children?.forEach { child ->
                val success = deleteDir(File(dir, child))
                if (!success) {
                    return false
                }
            }
        }
        return dir?.delete() ?: false
    }
    // ------! 한 번 더 누르시면 앱이 종료됩니다. !------

    private fun isCurrentFragmentEmpty(): Boolean {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.flMain)
        return currentFragment == null || currentFragment.view == null || !currentFragment.isVisible
    }







    private suspend fun getUrl(fileName: String, isImage: Boolean) : String = withContext(Dispatchers.IO) {
        val fileExtension = if (isImage) ".jpg" else ".mp4"
        val tempFile = File.createTempFile(if (isImage) "temp_image" else "temp_video", fileExtension, cacheDir)
        tempFile.deleteOnExit()

        assets.open(fileName).use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        tempFile.absolutePath
    }

    // ------# 사진 영상 pose 가져오기 #------
    private suspend fun loadJsonData(): JSONObject = withContext(Dispatchers.IO) {
        val jsonString = assets.open("MT_STATIC_BACK_6_1_20240604143755.json").bufferedReader()
                .use { it.readText() }
        JSONObject(jsonString)
    }
    private suspend fun loadJsonArray() : JSONArray = withContext(Dispatchers.IO) {
        val jsonString = assets.open("MT_DYNAMIC_OVERHEADSQUAT_FRONT_1_1_20240606135241.json").bufferedReader().use { it.readText() }
        JSONArray(jsonString)
    }
    fun launchMeasureSkeletonActivity() {
        val intent = Intent(this, MeasureSkeletonActivity::class.java)
        measureSkeletonLauncher.launch(intent)
    }
}

