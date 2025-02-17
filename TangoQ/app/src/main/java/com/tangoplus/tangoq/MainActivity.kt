package com.tangoplus.tangoq

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tangoplus.tangoq.api.HttpClientProvider.scheduleTokenCheck
import com.tangoplus.tangoq.broadcastReceiver.AlarmReceiver
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.fragment.ExerciseFragment
import com.tangoplus.tangoq.fragment.MainFragment
import com.tangoplus.tangoq.fragment.ProfileFragment
import com.tangoplus.tangoq.databinding.ActivityMainBinding
import com.tangoplus.tangoq.function.DeepLinkManager
import com.tangoplus.tangoq.db.MeasureDatabase
import com.tangoplus.tangoq.dialog.FeedbackDialogFragment
import com.tangoplus.tangoq.dialog.PlayThumbnailDialogFragment
import com.tangoplus.tangoq.fragment.MeasureDetailFragment
import com.tangoplus.tangoq.fragment.MeasureFragment
import com.tangoplus.tangoq.function.SecurePreferencesManager.logout
import com.tangoplus.tangoq.function.WifiManager
import com.tangoplus.tangoq.api.NetworkExercise.fetchExerciseById
import com.tangoplus.tangoq.db.Singleton_t_measure
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.dialog.AlertDialogFragment
import com.tangoplus.tangoq.fragment.ExerciseDetailFragment
import com.tangoplus.tangoq.fragment.AnalyzeFragment
import com.tangoplus.tangoq.fragment.MeasureHistoryFragment
import com.tangoplus.tangoq.fragment.ProgramSelectFragment
import com.tangoplus.tangoq.fragment.WithdrawalFragment
import com.tangoplus.tangoq.viewmodel.MainViewModel
import com.tangoplus.tangoq.viewmodel.PlayViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private val pvm : PlayViewModel by viewModels()
    private val mvm : MeasureViewModel by viewModels()
    private val viewModel: MainViewModel by viewModels()
    private var selectedTabId = R.id.main
    private lateinit var singletonMeasure : Singleton_t_measure
    private lateinit var measureSkeletonLauncher: ActivityResultLauncher<Intent>
    private lateinit var wifiManager: WifiManager
    private lateinit var myApplication: MyApplication
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 새로운 인텐트가 들어왔을 때 딥링크 처리
        handleIntent(intent)
    }

    @SuppressLint("ScheduleExactAlarm")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (Singleton_t_user.getInstance(this).jsonObject?.optString("user_name").isNullOrEmpty()) {
            Toast.makeText(this, "올바르지 않은 접근입니다.\n다시 로그인을 진행해주세요", Toast.LENGTH_LONG).show()
            logout(this@MainActivity, 0)
        }
        // ------! activity 사전 설정 시작 !------
        // 로그인 완료 시 2분마다 토큰 갱신
        myApplication = application as MyApplication

        val workManager = WorkManager.getInstance(this)
        scheduleTokenCheck(this)
        workManager.getWorkInfosForUniqueWorkLiveData("TokenCheckWork").observe(this) { workInfos ->
            if (workInfos.isNullOrEmpty()) return@observe
            val workInfo = workInfos[0]
            Log.v("TokenCheckWorker", "$workInfos")
            if (workInfo.state == WorkInfo.State.FAILED) {

                // 로그아웃 처리 / 성공 처리에 대한 토큰 저장은 이미 api 함수에서 실행 중
                val dialog = AlertDialogFragment.newInstance("logout")
                dialog.show(supportFragmentManager, "AlertDialogFragment")
            }
        }
        viewModel.showLogoutDialog.observe(this) { shouldShow ->
            if (shouldShow) {
                Log.v("로그아웃갑시다", "중복 로그인 - 현재 기기 로그아웃 처리")
                // 로그아웃 처리 / 성공 처리에 대한 토큰 저장은 이미 api 함수에서 실행 중
                val dialog = AlertDialogFragment.newInstance("logout")
                dialog.show(supportFragmentManager, "AlertDialogFragment")
                viewModel.resetLogoutDialog()
            }
        }


        wifiManager = WifiManager(this)
        // ------! activity 사전 설정 끝 !------

        // ------# 접근 방지 #------
        when (val securityType = wifiManager.checkWifiSecurity()) {
            "OPEN","WEP" -> {
                Log.v("securityNotice", "securityType: $securityType")
                Toast.makeText(this, "취약한 보안 환경에서 접근했습니다($securityType)\n3분뒤 자동 로그아웃 됩니다.", Toast.LENGTH_LONG).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    logout(this@MainActivity, 0)
                },   3 * 60000) //
            }
        }

        // ------# 접근 방지 #------

        selectedTabId = savedInstanceState?.getInt("selectedTabId") ?: R.id.main
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        // bottomnavigation도 같이 backstack 반응하기
        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.flMain)
            when (currentFragment) {
                is MainFragment, is ProgramSelectFragment -> binding.bnbMain.selectedItemId = R.id.main
                is AnalyzeFragment -> binding.bnbMain.selectedItemId = R.id.analyze
                is MeasureFragment, is MeasureHistoryFragment, is MeasureDetailFragment -> binding.bnbMain.selectedItemId = R.id.measure
                is ExerciseFragment, is ExerciseDetailFragment -> binding.bnbMain.selectedItemId = R.id.exercise
                is ProfileFragment, is WithdrawalFragment -> binding.bnbMain.selectedItemId = R.id.profile
            }
        }

        AlarmReceiver()
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (hasExactAlarmPermission(this@MainActivity)) {

            val time = Triple(13, 5, 0)
            val intent = Intent(this@MainActivity, AlarmReceiver::class.java).apply {
                putExtra("title", title)
                putExtra("text", "장시간 앉아있었다면 무리한 목과 허리를 위해 스트레칭을 추천드려요")
            }
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, time.first)
            calendar.set(Calendar.MINUTE, time.second)
            calendar.set(Calendar.SECOND, time.third)

            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            val pendingIntent = PendingIntent.getBroadcast(this@MainActivity, 8080, intent, PendingIntent.FLAG_IMMUTABLE)
            Log.v("setAlarm", "Success to Alarm $title, $time")
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)

            val time2 = Triple(19, 30, 0)
            val intent2 = Intent(this@MainActivity, AlarmReceiver::class.java).apply {
                putExtra("title", title)
                putExtra("text", "더 나은 내일을 위해 운동을 시작할 때 입니다")
            }
            val calendar2 = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, time2.first)
            calendar.set(Calendar.MINUTE, time2.second)
            calendar.set(Calendar.SECOND, time2.third)

            if (calendar2.timeInMillis <= System.currentTimeMillis()) {
                calendar2.add(Calendar.DAY_OF_MONTH, 1)
            }
            val pendingIntent2 = PendingIntent.getBroadcast(this@MainActivity, 8080, intent2, PendingIntent.FLAG_IMMUTABLE)
            Log.v("setAlarm", "Success to Alarm $title, $time2")
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent2)
        }

        // -----# 초기 화면 설정 #-----

        singletonMeasure = Singleton_t_measure.getInstance(this)
        setCurrentFragment(selectedTabId)

        binding.bnbMain.itemIconTintList = null
        binding.bnbMain.isItemActiveIndicatorEnabled = false

        if (!singletonMeasure.measures.isNullOrEmpty()) { // 값이 하나라도 있을 때만 가져오기.
            mvm.selectedMeasure = singletonMeasure.measures?.get(0)
            mvm.selectedMeasureDate.value = singletonMeasure.measures?.get(0)?.regDate
        }
        handleIntent(intent)

        // -------# 버튼 시작 #------
        binding.bnbMain.setOnItemSelectedListener {
            setCurrentFragment(it.itemId)
            true
        }
        binding.bnbMain.setOnItemReselectedListener { setCurrentFragment(it.itemId) }

        // ------# 측정 완료 후 측정 디테일 화면으로 바로 가기 #------
        measureSkeletonLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val finishedMeasure = result.data?.getBooleanExtra("finishedMeasure", false) ?: false
                if (finishedMeasure) {
                    mvm.selectedMeasureDate.value = singletonMeasure.measures?.get(0)?.regDate
                    mvm.selectedMeasure = singletonMeasure.measures?.get(0)
                    val bnb : BottomNavigationView = findViewById(R.id.bnbMain)
                    bnb.selectedItemId = R.id.measure
                    val bundle = Bundle()
                    bundle.putBoolean("showMeasure", true)
                    val measureDetailFragment = MeasureDetailFragment().apply {
                        arguments = bundle
                    }
                    supportFragmentManager.beginTransaction().apply {
                        replace(R.id.flMain, measureDetailFragment)
                        commit()
                    }

                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("selectedTabId", selectedTabId)
    }

    private fun setCurrentFragment(itemId: Int) {
        // 새로운 프래그먼트 생성
        val fragment = when (itemId) {
            R.id.main -> MainFragment()
            R.id.analyze -> AnalyzeFragment()
            R.id.exercise -> ExerciseFragment()
            R.id.measure -> MeasureFragment()
            R.id.profile -> ProfileFragment()
            else -> MainFragment()
        }
        // 프래그먼트 변경
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flMain, fragment)
            commit()
        }
    }

    override fun onResume() {
        super.onResume()

        // ------! 0일 때만 피드백 켜지게 !------
        val feedbackData = intent?.getSerializableExtra("feedback_finish") as? Triple<Int, Int, Int>
        Log.v("intent>feedback", "$feedbackData")
        if (feedbackData != null) {
            if (pvm.isDialogShown.value == false) {
                pvm.exerciseLog = feedbackData

                // 이미 DialogFragment가 표시되어 있는지 확인
                val fragmentManager = supportFragmentManager
                val existingDialog = fragmentManager.findFragmentByTag("FeedbackDialogFragment")

                if (existingDialog == null) {
                    val dialog = FeedbackDialogFragment()
                    dialog.show(fragmentManager, "FeedbackDialogFragment")
                }
            } else {
                pvm.isDialogShown.value = true
            }
        }
    }

    // ------# 한 번 더 누르시면 앱이 종료됩니다. #------
    private var backPressedOnce = false
    private val backPressHandler = Handler(Looper.getMainLooper())
    private val backPressRunnable = Runnable { backPressedOnce = false }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val fragmentManager = supportFragmentManager

            if (fragmentManager.fragments.lastOrNull() is MainFragment) {
                MeasureDatabase.closeDatabase()
                backPressedOnce = true
                Toast.makeText(this@MainActivity, "한 번 더 누르시면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show()
                backPressHandler.postDelayed(backPressRunnable, 1000)
                myApplication.clearLastActivity()
                finishAffinity()
            } else {
                myApplication.setLastActivity()
                binding.bnbMain.selectedItemId = R.id.main
                setCurrentFragment(0)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 핸들러의 콜백을 제거하여 메모리 누수를 방지
        backPressHandler.removeCallbacks(backPressRunnable)
        WorkManager.getInstance(this).cancelUniqueWork("TokenCheckWork")
    }

    private fun handleIntent(intent: Intent) {
        val deepLinkPath = intent.getStringExtra(DeepLinkManager.DEEP_LINK_PATH_KEY)
        val exerciseId = intent.getStringExtra(DeepLinkManager.EXERCISE_ID_KEY)
        val finishEVP =  intent.getStringExtra("evp_finish")
        Log.v("finishEVP", "$finishEVP")
        if (deepLinkPath != null) {
            if (exerciseId != null) {
                navigateToFragment(deepLinkPath, exerciseId)
            }
        } else if (finishEVP != null) {
            navigateToFragment("PlayThumbnail", finishEVP)
        }
    }

    private fun navigateToFragment(path: String, exerciseId : String?) {
        Log.v("Main>NavigateDeeplink", "path : ${path}, exerciseId: $exerciseId")
        when (path) {
            "PT" -> {
                if (exerciseId != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val exerciseUnit = fetchExerciseById(getString(R.string.API_exercise), exerciseId)
                        withContext(Dispatchers.Main) {
                            val dialog = PlayThumbnailDialogFragment().apply {
                                arguments = Bundle().apply {
                                    putParcelable("ExerciseUnit", exerciseUnit)
                                }
                            }
                            dialog.show(supportFragmentManager, "PlayThumbnailDialogFragment")
                        }
                    }
                }
            }
            "MD1" -> {
                supportFragmentManager.beginTransaction().apply {
                    replace(R.id.flMain, MeasureFragment())
                    commit()
                }
            }
            "MD" -> {
                supportFragmentManager.beginTransaction().apply {
                    replace(R.id.flMain, MeasureDetailFragment())
                    commit()
                }
            }

            "PlayThumbnail" -> {
                if (exerciseId != null) {
//                    val typeIds = when (exerciseId.toInt()) {
//                        in 1.. 27 -> arrayListOf(1, 2)
//                        in 28 .. 72 -> arrayListOf(3, 4, 5)
//                        in 73 .. 133 -> arrayListOf(6, 7, 8, 9)
//                        else -> arrayListOf(10, 11)
//                    }
                    binding.bnbMain.selectedItemId = R.id.exercise
                    supportFragmentManager.beginTransaction().apply {
                        replace(R.id.flMain, ExerciseFragment())
                        commit()
                    }
                }

            }
            else -> MainFragment()
        }
    }


    fun launchMeasureSkeletonActivity() {
        val intent = Intent(this, MeasureSkeletonActivity::class.java)
        measureSkeletonLauncher.launch(intent)
    }

    private fun hasExactAlarmPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}

