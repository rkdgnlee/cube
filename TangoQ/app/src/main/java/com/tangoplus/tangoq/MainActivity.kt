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
import com.tangoplus.tangoq.data.ExerciseViewModel
import com.tangoplus.tangoq.data.ProgressViewModel
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
import com.tangoplus.tangoq.`object`.Singleton_t_progress
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private val eViewModel : ExerciseViewModel by viewModels()
    private val uViewModel : UserViewModel by viewModels()
    private val hViewModel : ProgressViewModel by viewModels()
    private val mViewModel : MeasureViewModel by viewModels()
    private var selectedTabId = R.id.main
    private lateinit var singletonMeasure : Singleton_t_measure
    private lateinit var program : ProgramVO

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

        // -----# 초기 화면 설정 #-----
//        handlePendingDeepLink()
        singletonMeasure = Singleton_t_measure.getInstance(this)
        setCurrentFragment(selectedTabId)
        binding.bnbMain.itemIconTintList = null
        binding.bnbMain.isItemActiveIndicatorEnabled = false



        mViewModel.selectedMeasure = singletonMeasure.measures?.get(0)!!
        mViewModel.selectedMeasureDate.value = singletonMeasure.measures?.get(0)?.regDate


        // -------! 버튼 시작 !------
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
        // -------! 버튼 끝 !------

        // ------# 측정 완료 후 측정 디테일 화면으로 바로 가기 #------
        measureSkeletonLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val finishedMeasure = result.data?.getBooleanExtra("finishedMeasure", false) ?: false
                if (finishedMeasure) {
                    mViewModel.selectedMeasureDate.value = singletonMeasure.measures?.get(0)?.regDate
                    mViewModel.selectedMeasure = singletonMeasure.measures?.get(0)
                    val bnb : BottomNavigationView = findViewById(R.id.bnbMain)
                    bnb.selectedItemId = R.id.measure
                    val bundle = Bundle()
                    bundle.putBoolean("showMeasure", true)
                    val measureDetailFragment = MeasureDetailFragment().apply {
                        arguments = bundle
                    }
                    supportFragmentManager.beginTransaction().apply {
                        replace(R.id.flMain, measureDetailFragment)
                        addToBackStack(null)
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

    fun launchMeasureSkeletonActivity() {
        val intent = Intent(this, MeasureSkeletonActivity::class.java)
        measureSkeletonLauncher.launch(intent)
    }
}

