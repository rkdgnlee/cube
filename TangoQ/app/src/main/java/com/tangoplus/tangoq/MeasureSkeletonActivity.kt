package com.tangoplus.tangoq

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.ExifInterface
import android.net.Uri
import android.net.http.NetworkException
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.NetworkOnMainThreadException
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.google.android.gms.common.util.DeviceProperties.isTablet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.shuhart.stepview.StepView
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.viewmodel.SkeletonViewModel
import com.tangoplus.tangoq.databinding.ActivityMeasureSkeletonBinding
import com.tangoplus.tangoq.db.FileStorageUtil.saveJa
import com.tangoplus.tangoq.db.FileStorageUtil.saveJo
import com.tangoplus.tangoq.db.MeasureDao
import com.tangoplus.tangoq.db.MeasureDatabase
import com.tangoplus.tangoq.db.MeasureDynamic
import com.tangoplus.tangoq.db.MeasureInfo
import com.tangoplus.tangoq.db.MeasureStatic
import com.tangoplus.tangoq.function.MeasurementManager.extractVideoCoordinates
import com.tangoplus.tangoq.function.SecurePreferencesManager.getServerUUID
import com.tangoplus.tangoq.dialog.GuideDialogFragment.Companion.getRequiredPermissions
import com.tangoplus.tangoq.dialog.LoadingDialogFragment
import com.tangoplus.tangoq.dialog.MeasureSkeletonDialogFragment
import com.tangoplus.tangoq.function.MeasurementManager
import com.tangoplus.tangoq.function.MeasurementManager.calculateOverall
import com.tangoplus.tangoq.function.MeasurementManager.getPairParts
import com.tangoplus.tangoq.listener.OnSingleClickListener
import com.tangoplus.tangoq.mediapipe.MathHelpers.calculateAngle
import com.tangoplus.tangoq.mediapipe.MathHelpers.calculateSlope
import com.tangoplus.tangoq.mediapipe.OverlayView
import com.tangoplus.tangoq.mediapipe.PoseLandmarkAdapter
import com.tangoplus.tangoq.mediapipe.PoseLandmarkerHelper
import com.tangoplus.tangoq.api.NetworkMeasure.resendMeasureFile
import com.tangoplus.tangoq.api.NetworkMeasure.sendMeasureData
import com.tangoplus.tangoq.db.FileStorageUtil.getPathFromUri
import com.tangoplus.tangoq.function.SaveSingletonManager
import com.tangoplus.tangoq.mediapipe.MathHelpers.calculateAngleBySlope
import com.tangoplus.tangoq.mediapipe.MathHelpers.getRealDistanceX
import com.tangoplus.tangoq.mediapipe.MathHelpers.getRealDistanceY
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.dialog.MeasureSetupDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.roundToInt

class MeasureSkeletonActivity : AppCompatActivity(), PoseLandmarkerHelper.LandmarkerListener, SensorEventListener {
    /*  1. button 클릭
    *   2. 카운트 다운 시작
    *   3. 카운트 다운 종료
    *   4. 촬영/녹화
    *   5. updateUI
    *
    *   그 외 previousStep
    * */
    // ------! POSE LANDMARKER 설정 시작 !------
    companion object {
        private const val TAG = "Pose Landmarker"
        private const val REQUEST_CODE_PERMISSIONS = 1001
        // 버전별 필요 권한 정의
        fun hasPermissions(context: Context): Boolean {
            Log.d("PermissionCheck", "Context type: ${context::class.java.name}")
            return getRequiredPermissions().all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
    private var permissionDialog: AlertDialog? = null
    private lateinit var binding : ActivityMeasureSkeletonBinding
    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private val viewModel: SkeletonViewModel by viewModels()
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT
    private lateinit var backgroundExecutor: ExecutorService
    private var scaleFactorX : Float? = null
    private var scaleFactorY : Float? = null

    // 사진 캡처
    private lateinit var imageCapture: ImageCapture
    private var isCapture = true
    private var isRecording = false
    private var startRecording = false

    // 영상 캡처
    private var recordingJob: Job? = null
    private lateinit var videoCapture : VideoCapture<Recorder>
    private var recording: Recording? = null
    private var videoFileName = ""
    private var measureVideoName = ""
    private var isNameInit = false
    private var dynamicStartTime = ""
    private var dynamicEndTime = ""

    // ------! 싱글턴 패턴 객체 가져오기 !------
    private lateinit var decryptedUUID : String
    private var poseLandmarks = JSONArray()
    private lateinit var singletonUser : Singleton_t_user
    private lateinit var md : MeasureDatabase
    private lateinit var mDao : MeasureDao
    private var measureInfoSn = 0

    var latestResult: PoseLandmarkerHelper.ResultBundle? = null
    private val mvm : MeasureViewModel by viewModels()

    private var seqStep = MutableLiveData(0)
    private val maxSeq = 6
    private var progress = 12
    private var timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
    private val startTime = LocalDateTime.now()
    private var endTime : LocalDateTime? = null

    // ------# 수직 감지기 #------
    private lateinit var hideIndicatorHandler: Handler
    private lateinit var hideIndicatorRunnable: Runnable
    private var accelerometer: Sensor? = null
    private lateinit var sensorManager: SensorManager
    private var currentBiasZ = 0f
    private var currentBiasX = 0f
    private var filteredAngleZ = 0f
    private var filteredAngleX = 0f
    private val ALPHA = 0.1f
    private val INTERPOLATION_FACTOR = 0.1f
    private var hideIndicator = false

    // ------! 카운트 다운  시작 !-------
    private  val mCountDown : CountDownTimer by lazy {
        object : CountDownTimer((if (seqStep.value == 5) 7000 else 5000), 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                runOnUiThread{
                    binding.btnMeasureSkeletonStepPrevious.isEnabled = false
                    binding.clMeasureSkeletonCount.visibility = View.VISIBLE
                    binding.clMeasureSkeletonCount.alpha = 1f
                    binding.tvMeasureSkeletonCount.text = "${(millisUntilFinished / 1000.0f).roundToInt()}"
                    binding.tvMeasureSkeletonCount.textSize = if (isTablet(this@MeasureSkeletonActivity)) 150f else 110f
                    Log.v("count", "${binding.tvMeasureSkeletonCount.text}")
                }
            }
            @SuppressLint("SetTextI18n", "DefaultLocale")
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onFinish() {
//                binding.tvMeasureSkeletonCount.textSize = if (isTablet(this@MeasureSkeletonActivity)) 32f else 28f
                if (latestResult?.results?.first()?.landmarks()?.isNotEmpty() == true) {
                    // ------# resultBundleToJson이 동작하는 시간으로 통일 #------
                    timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    if (isRecording) { // 동영상 촬영
                        binding.tvMeasureSkeletonGuide.text = "스쿼트를 실시해주세요"
                        setAnimation(binding.clMeasureSkeletonCount, 1000, 500, false) {
                            hideViews(6000)
                            // 녹화 종료 시점 아님
                            dynamicStartTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            startVideoRecording {
                                // 녹화 종료 시점
                                dynamicEndTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                updateUI()
                                Log.v("dynamicJa총길이", "${mvm.dynamicJa.length()}")
                                // ------# dynamic의 프레임들에서 db에 넣을 값을 찾는 곳 #------
                                lifecycleScope.launch(Dispatchers.IO) {
                                    try {
                                        // JSONArray 복사본 생성 후 작업
                                        val jsonArrayCopy = synchronized(mvm.dynamicJa) {
                                            val tempArray = JSONArray()
                                            for (i in 0 until mvm.dynamicJa.length()) {
                                                tempArray.put(mvm.dynamicJa.getJSONObject(i))
                                            }
                                            tempArray
                                        }
                                        // 첫 번째 object에 times 넣기
                                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                        val dynamicStart = LocalDateTime.parse(dynamicStartTime, formatter)
                                        val dynamicEnd = LocalDateTime.parse(dynamicEndTime, formatter)
                                        val duration = Duration.between(dynamicStart, dynamicEnd)
                                        val diffInSeconds = String.format("%.7f",
                                            duration.seconds.toFloat() + duration.nano.toFloat() / 1_000_000_000
                                        ).toFloat()
                                        jsonArrayCopy.getJSONObject(0).put("time", diffInSeconds)

                                        // 복사본을 파일로 저장
                                        val saveResult = withContext(Dispatchers.IO) {
                                            saveJa(this@MeasureSkeletonActivity, "${videoFileName}.json", jsonArrayCopy, mvm)
                                        }

                                        if (saveResult) {
                                            withContext(Dispatchers.Main) {
                                                val noseDynamic = extractVideoCoordinates(jsonArrayCopy).map { it[0] }
                                                Log.v("noseDynamic", "$noseDynamic")
                                                val decreasingFrameIndex = findLowestYFrame(noseDynamic)
                                                val saveDynamic = jsonArrayCopy.optJSONObject(decreasingFrameIndex)
                                                Log.v("인덱스와변형전dynamicJson추출", "decreasingFrameIndex: ${decreasingFrameIndex}, saveDynamic: $saveDynamic")

                                                saveDynamic?.let { jsonObject ->
                                                    val modifiedObject = JSONObject(jsonObject.toString()) // 객체 복사
                                                    modifiedObject.put("measure_start_time", dynamicStartTime)
                                                    modifiedObject.put("measure_end_time", dynamicEndTime)
                                                    modifiedObject.remove("pose_landmark")

                                                    synchronized(mvm) {
                                                        mvm.dynamic = mvm.convertToMeasureDynamic(modifiedObject)
                                                        mvm.toSendDynamicJo = modifiedObject
                                                        Log.v("넣을dynamicJo", "${mvm.toSendDynamicJo}")
                                                    }
                                                }
                                            }
                                        }
                                    } catch (e: IndexOutOfBoundsException) {
                                        Log.e("MSIndex", "${e.message}")
                                    } catch (e: IllegalArgumentException) {
                                        Log.e("MSIllegal", "${e.message}")
                                    } catch (e: IllegalStateException) {
                                        Log.e("MSIllegal", "${e.message}")
                                    } catch (e: NullPointerException) {
                                        Log.e("MSNull", "${e.message}")
                                    } catch (e: java.lang.Exception) {
                                        Log.e("MSException", "${e.printStackTrace()}")
                                    }
                                }
                            }
                        }
                    } else {

                        binding.tvMeasureSkeletonGuide.text = "자세를 따라해주세요"
                        hideViews(600)
                        Log.v("사진service", "isCapture: ${isCapture}, isRecording: $isRecording")
                        // ------! 종료 후 다시 세팅 !------
                        latestResult?.let { resultBundleToJson(it, seqStep.value ?: -1) }
                        if (seqStep.value != null) {
                            captureImage(seqStep.value ?: -1)
                        }
                        Log.v("캡쳐종료시점", "step: ${seqStep.value}")
                        updateUI()
                        isCapture = false
                    }
                    binding.btnMeasureSkeletonStep.isEnabled = true
                } else {
                    binding.tvMeasureSkeletonGuide.text = "자세를 다시 따라해주세요"
                    binding.btnMeasureSkeletonStep.isEnabled = true
                    Toast.makeText(this@MeasureSkeletonActivity, "측정에 실패했습니다\n다시 화면에 적절히 포즈가 나오도록 움직여주세요", Toast.LENGTH_LONG).show()
                    binding.clMeasureSkeletonCount.visibility = View.INVISIBLE
                }
                binding.btnMeasureSkeletonStepPrevious.isEnabled = true
            }
        }
    } //  ------! 카운트 다운 끝 !-------


    override fun onResume() {
        super.onResume()
        if (!hasPermissions(this)) {
            ActivityCompat.requestPermissions(this, getRequiredPermissions(), REQUEST_CODE_PERMISSIONS)
        } else {
            setUpCamera()
        }
        // ------# 수직 감지기 #------
        if (!hideIndicator) {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }
        backgroundExecutor.execute {
            if (this::poseLandmarkerHelper.isInitialized) {
                if (poseLandmarkerHelper.isClose()) {
                    poseLandmarkerHelper.setupPoseLandmarker()
                }
            }
        }
    }
    override fun onPause() {
        super.onPause()
        if(this::poseLandmarkerHelper.isInitialized) {
            viewModel.setMinPoseDetectionConfidence(poseLandmarkerHelper.minPoseDetectionConfidence)
            viewModel.setMinPoseTrackingConfidence(poseLandmarkerHelper.minPoseTrackingConfidence)
            viewModel.setMinPosePresenceConfidence(poseLandmarkerHelper.minPosePresenceConfidence)
            viewModel.setDelegate(poseLandmarkerHelper.currentDelegate)
            backgroundExecutor.execute { poseLandmarkerHelper.clearPoseLandmarker() }

            if (hideIndicator) {
                sensorManager.unregisterListener(this)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )
        sensorManager.unregisterListener(this)
        hideIndicatorHandler.removeCallbacks(hideIndicatorRunnable)
    }
    // ------! POSE LANDMARKER 설정 끝 !------

    @SuppressLint("Recycle")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeasureSkeletonBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // ------# room(DB) & singleton & uuid init #------
        md = MeasureDatabase.getDatabase(this)
        mDao = md.measureDao()
        singletonUser = Singleton_t_user.getInstance(this@MeasureSkeletonActivity)
        CoroutineScope(Dispatchers.IO).launch {
            measureInfoSn = mDao.getMaxMobileInfoSn(singletonUser.jsonObject?.optInt("user_sn") ?: -1) + 1
            Log.v("이제들어갈measureSn", "$measureInfoSn")
        }
        decryptedUUID = getServerUUID(this@MeasureSkeletonActivity).toString()

        // ------# sensor 연결 #------
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // -----# pose landmark helper & camera init #-----
        backgroundExecutor = Executors.newSingleThreadExecutor()
        binding.viewFinder.post {
            setUpCamera()
        }
        backgroundExecutor.execute {
            try {
                poseLandmarkerHelper = PoseLandmarkerHelper(
                    context = this,
                    runningMode = RunningMode.LIVE_STREAM,
                    minPoseDetectionConfidence = viewModel.currentMinPoseDetectionConfidence,
                    minPoseTrackingConfidence = viewModel.currentMinPoseTrackingConfidence,
                    minPosePresenceConfidence = viewModel.currentMinPosePresenceConfidence,
                    currentDelegate = viewModel.currentDelegate,
                    poseLandmarkerHelperListener = this
                )
            }
            catch (e: UnsatisfiedLinkError) {
                Log.e("PoseLandmarkerHelper", "Failed to load libmediapipe_tasks_vision_jni.so", e)
            }
            if (!hasPermissions(this)) {
                ActivityCompat.requestPermissions(
                    this,
                    getRequiredPermissions(),
                    REQUEST_CODE_PERMISSIONS
                )
            } else {
                setUpCamera()
            }
        }
        // ------! 안내 문구 사라짐 시작 !------

        /** 사진 및 동영상 촬영 순서
         * 1. isLooping true -> seqStep확인 -> count에 맞게 isCapture및 isRecord 선택
         * 2. mCountdown.start() -> 카운트 다운이 종료될 때 isCapture, isRecording에 따라 service 함수 실행 */
        binding.ibtnMeasureSkeletonBack.setOnClickListener {
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                setTitle("알림")
                setMessage("측정을 종료하시겠습니까 ?")
                setPositiveButton("예") { _, _ ->
                    Log.v("mvm이름잘", "name: ${mvm.setupName}")
                    val activityIntent = Intent(this@MeasureSkeletonActivity, MainActivity::class.java)
                    intent.putExtra("showMeasureFragment", true)
                    startActivity(activityIntent)
                    finish()
                }
                setNegativeButton("아니오") { dialog, _ ->
                    dialog.dismiss()
                }
            }.show()
        }

        // ------! STEP CIRCLE !------
        binding.svMeasureSkeleton.state.animationType(StepView.ANIMATION_CIRCLE)
            .steps(object : ArrayList<String?>() {
                init {
                    add("정면")
                    add("오버헤드")
                    add("팔꿉")
                    add("왼쪽")
                    add("오른쪽")
                    add("후면")
                    add("앉아후면")
                }
            })
            .stepsNumber(7)
            .animationDuration(getResources().getInteger(android.R.integer.config_shortAnimTime))
            .commit()

        // ------# 초기 시작 #------
        binding.svMeasureSkeleton.go(0, true)
        binding.pvMeasureSkeleton.progress = 14f

        // ------# 버튼 촬영 #------
        binding.btnMeasureSkeletonStep.setOnSingleClickListener {
            binding.tvMeasureSkeletonDefault.visibility = View.GONE
            if (binding.btnMeasureSkeletonStep.text == "완료하기") {
                endTime = LocalDateTime.now()

                // ------# 리소스 정리 #------
                if(this::poseLandmarkerHelper.isInitialized) {
                    viewModel.setMinPoseDetectionConfidence(poseLandmarkerHelper.minPoseDetectionConfidence)
                    viewModel.setMinPoseTrackingConfidence(poseLandmarkerHelper.minPoseTrackingConfidence)
                    viewModel.setMinPosePresenceConfidence(poseLandmarkerHelper.minPosePresenceConfidence)
                    viewModel.setDelegate(poseLandmarkerHelper.currentDelegate)

                    backgroundExecutor.execute { poseLandmarkerHelper.clearPoseLandmarker() }
                    if (hideIndicator) {
                        sensorManager.unregisterListener(this)
                    }
                }

                // ------# 업로드 시작 #------
                val dialog = LoadingDialogFragment.newInstance("업로드")
                dialog.show(supportFragmentManager, "LoadingDialogFragment")

                CoroutineScope(Dispatchers.IO).launch {
                    val motherJo = JSONObject()

                    val userJson = singletonUser.jsonObject
                    val userUUID = userJson?.getString("user_uuid") ?: ""

                    val inputFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val date: Date = inputFormat.parse(timestamp) as Date
                    val measureDate = outputFormat.format(date)
                    val seconds = Duration.between(startTime, endTime).toMillis() / 1000.0
                    val elapsedTime = "%.3f".format(seconds)

                    // ------# info 넣기 #------
                    for (i in 0 until  mvm.statics.size) {
                        if (i == 1) { mvm.infoResultJa.put(mvm.toSendDynamicJo) }
                        mvm.infoResultJa.put(JSONObject(mvm.statics[i].toJson()))
                    }
                    Log.v("infoResultJa", "${mvm.infoResultJa.length()}")

                    val parts = getPairParts(this@MeasureSkeletonActivity, mvm.infoResultJa)
                    Log.v("parts결과", "$parts")

                    val measureInfo = MeasureInfo(
                        user_uuid = userUUID,
                        mobile_device_uuid = decryptedUUID,
                        user_sn = userJson?.optString("user_sn")?.toInt() ?: -1,
                        user_name = mvm.setupName, // userJson?.optString("user_name"),
                        measure_date = measureDate,
                        elapsed_time = elapsedTime,
                        t_score = calculateOverall(parts).toString(),
                        measure_seq = 7,
                    )
                    val riskMapping = mapOf(
                        "목관절" to { value: String -> measureInfo.risk_neck = value },
                        "좌측 어깨" to { value: String -> measureInfo.risk_shoulder_left = value },
                        "우측 어깨" to { value: String -> measureInfo.risk_shoulder_right = value },
                        "좌측 팔꿉" to { value: String -> measureInfo.risk_elbow_left = value },
                        "우측 팔꿉" to { value: String -> measureInfo.risk_elbow_right = value },
                        "좌측 손목" to { value: String -> measureInfo.risk_wrist_left = value },
                        "우측 손목" to { value: String -> measureInfo.risk_wrist_right = value },
                        "좌측 골반" to { value: String -> measureInfo.risk_hip_left = value },
                        "우측 골반" to { value: String -> measureInfo.risk_hip_right = value },
                        "좌측 무릎" to { value: String -> measureInfo.risk_knee_left = value },
                        "우측 무릎" to { value: String -> measureInfo.risk_knee_right = value },
                        "좌측 발목" to { value: String -> measureInfo.risk_ankle_left = value },
                        "우측 발목" to { value: String -> measureInfo.risk_ankle_right = value }
                    )
                    val statusMapping = mapOf(
                        MeasurementManager.Status.DANGER to "2",
                        MeasurementManager.Status.WARNING to "1",
                        MeasurementManager.Status.NORMAL to "0"
                    )
                    for (part in parts) {
                        val statusValue = statusMapping[part.second]
                        if (statusValue != null) {
                            riskMapping[part.first]?.invoke(statusValue)
                            Log.v("파트status받기", "${part}, $statusValue")
                        }
                    }
                    Log.v("measure빈칼럼여부", "$measureInfo")

                    val mobileInfoSn = mDao.insertInfo(measureInfo).toInt()
                    var mobileDynamicSn = 0
                    val mobileStaticSns = mutableListOf<Int>()
                    for (i in 0 until mvm.statics.size) {
                        val staticUnit = mvm.statics[i]
                        val staticId = mDao.insertByStatic(staticUnit).toInt()
                        mobileStaticSns.add(staticId)
                    }
                    val dynamic = mvm.dynamic
                    if (dynamic != null) {
                        val dynamicId = mDao.insertByDynamic(dynamic).toInt()
                        mobileDynamicSn = dynamicId
                    }
                    Log.v("들어간데이터SN들", "mobileInfoSn: $mobileInfoSn, mobileDynamicSn: $mobileDynamicSn, mobileStaticSns: $mobileStaticSns")

//                    // ------# 업로드 준비 #------
                    val infoJson = JSONObject(measureInfo.toJson())
                    motherJo.put("measure_info", infoJson)
                    Log.v("viewModelStatic", "multipartBody로 넣기 전 statics의 size: ${mvm.statics.size}")

                    for (i in 0 until mvm.statics.size) {
                        val staticUnit = mvm.statics[i].toJson()
                        val joStaticUnit = JSONObject(staticUnit)
                        Log.v("스태틱변환", "$joStaticUnit")
                        motherJo.put("static_${i+1}", joStaticUnit)
                    }

                    val dynamicJo = JSONObject(mvm.dynamic?.toJson().toString())
                    motherJo.put("dynamic", dynamicJo)
                    Log.v("motherJo1", "${motherJo.optJSONObject("measure_info")}")
                    Log.v("dynamic", "${motherJo.getJSONObject("dynamic").keys().asSequence().toList().filter { !it.startsWith("ohs") && !it.startsWith("ols")}}")

                    // ------# 멀티파트 init 하면서 data 넣기 #------
                    val requestBodyBuilder = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("json", motherJo.toString())
                    Log.v("멀티파트바디빌드", "전체 데이터 - motherJo키값들 ${motherJo.keys().asSequence().toList()}")

                    // static jpg파일들
                    for (i in mvm.staticFiles.indices) {
                        val file = mvm.staticFiles[i]
                        Log.v("파일정보", "Static File: 이름=${file.name}, 크기=${file.length()} bytes")
                        requestBodyBuilder.addFormDataPart(
                            "static_file_${i+1}",
                            file.name,
                            file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        )
                    }
                    // static json파일
                    for (i in mvm.staticJsonFiles.indices) {
                        val file = mvm.staticJsonFiles[i]
                        Log.v("파일정보", "Static JSON: 이름=${file.name}, 크기=${file.length()} bytes")
                        requestBodyBuilder.addFormDataPart(
                            "static_json_${i+1}",
                            file.name,
                            file.asRequestBody("application/json".toMediaTypeOrNull())
                        )
                    }
                    // Dynamic json파일
                    mvm.dynamicJsonFile?.let { file ->
                        Log.v("파일정보", "Dynamic JSON: 이름=${file.name}, 크기=${file.length()} bytes")
                        requestBodyBuilder.addFormDataPart(
                            "dynamic_json",
                            file.name,
                            file.asRequestBody("application/json".toMediaTypeOrNull())
                        )
                    }
                    // Dynamic mp4 파일
                    mvm.dynamicFile?.let { file ->
                        Log.v("파일정보", "Dynamic MP4: 이름=${file.name}, 크기=${file.length()} bytes")
                        requestBodyBuilder.addFormDataPart(
                            "dynamic_file",
                            file.name,
                            file.asRequestBody("video/mp4".toMediaTypeOrNull())
                        )
                    }
                    val joKeys = motherJo.keys()
                    for (key in joKeys) {
                        Log.v("파일제외바디", "motherJo: $key")
                    }

                    var requestBody = requestBodyBuilder.build()
                    val partCount = requestBodyBuilder.build().parts.size
                    Log.v("파트개수", "총 파트 개수: $partCount / 15")
                    Log.v("sn들집계", "info: ${mobileInfoSn}, static: ${mobileStaticSns},dynamic: $mobileDynamicSn")
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            sendMeasureData(this@MeasureSkeletonActivity, getString(R.string.API_results), requestBody, mobileInfoSn, mobileStaticSns, mobileDynamicSn) { jo ->

                                val reMotherJo = JSONObject()
                                if (jo != JSONObject()) {
                                    val staticUploadResults = mutableListOf<Triple<Boolean, Boolean,Boolean>>()
                                    val staticUploadSns = mutableListOf<Int>()
                                    for (i in 1..6) {
                                        val staticResult = jo.optJSONObject("static_$i")
                                        if (staticResult != null) {
                                            val sn = staticResult.optInt("sn")
                                            val jsonSuccess = staticResult.optString("uploaded") == "1"
                                            val jsonFileSuccess = staticResult.optString("uploaded_json") == "1"
                                            val mediaFileSuccess = staticResult.optString("uploaded_file") == "1"
                                            staticUploadSns.add(sn)
                                            staticUploadResults.add(Triple(jsonSuccess, jsonFileSuccess, mediaFileSuccess))
                                        } else {
                                            staticUploadResults.add(Triple(false, false, false)) // 실패로 간주
                                        }
                                    }

                                    val dynamicResult = jo.optJSONObject("dynamic")
                                    var dynamicUploadResults = Triple(false, false, false) // 실패로 간주
                                    var dynamicUploadSn = 0
                                    if (dynamicResult != null) {
                                        dynamicUploadSn = dynamicResult.optInt("sn")
                                        val jsonSuccess = dynamicResult.optString("uploaded") == "1"
                                        val jsonFileSuccess = dynamicResult.optString("uploaded_json") == "1"
                                        val mediaFileSuccess = dynamicResult.optString("uploaded_file") == "1"
                                        dynamicUploadResults = Triple(jsonSuccess, jsonFileSuccess, mediaFileSuccess)
                                    }
                                    // 모든 업로드 항목이 성공했는지 확인
                                    val allStaticsUploaded = staticUploadResults.all { it.first && it.second && it.third }
                                    val allDynamicUploaded = dynamicUploadResults.first && dynamicUploadResults.second && dynamicUploadResults.third

                                    if (allStaticsUploaded && allDynamicUploaded) {
                                        Log.d("Upload", "모든 파일이 성공적으로 업로드되었습니다.")
                                        CoroutineScope(Dispatchers.Main).launch {
                                            dialog.dismiss()
                                            // ------# 측정 완료 && 업로드 후 싱글턴 저장 #------
                                            finishMeasure(mobileInfoSn, mobileStaticSns, mobileDynamicSn)
                                        }
                                        return@sendMeasureData // 여기서 최초 1회 전송 완료 코루틴 밖으로 나감.
                                    }

                                    val uploadJobs = mutableListOf<Deferred<Result<Pair<String, String>>>>()
                                    var allStaticSuccess = false
                                    var retryDynamicSuccess = false
                                    CoroutineScope(Dispatchers.IO).launch {
                                        staticUploadResults.forEachIndexed { index, result ->

                                            val staticTargetJo = JSONObject().put("target", "0")
                                            val retryStaticRequestBodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                                                .addFormDataPart("json", staticTargetJo.toString())
                                            if (!result.second) {
                                                val file = mvm.staticJsonFiles[index]
                                                Log.v("파일정보", "Static JSON: 이름=${file.name}, 크기=${file.length()} bytes")
                                                retryStaticRequestBodyBuilder.addFormDataPart(
                                                    "json",
                                                    file.name,
                                                    file.asRequestBody("application/json".toMediaTypeOrNull())
                                                )
                                            }
                                            if (!result.third) {
                                                val file = mvm.staticFiles[index]
                                                Log.v("파일 재전송", "Static File 재전송: 이름=${file.name}, 크기=${file.length()} bytes")
                                                retryStaticRequestBodyBuilder.addFormDataPart(
                                                    "file",
                                                    file.name,
                                                    file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                                )
                                            }
                                            requestBody = retryStaticRequestBodyBuilder.build()
                                            Log.v("sn들집계2", "info: ${mobileInfoSn},static:  ${mobileStaticSns},dynamic: $mobileDynamicSn")
                                            val uploadJob = async {
                                                resendMeasureFileWithRetry(
                                                    context = this@MeasureSkeletonActivity,
                                                    apiUrl = getString(R.string.API_results),
                                                    requestBody = requestBody,
                                                    isStatic = true,
                                                    uploadSn = staticUploadSns[index],
                                                    mobileSn = mobileStaticSns[index]
                                                )
                                            }
                                            uploadJobs.add(uploadJob)

                                            val allResults = uploadJobs.awaitAll()
                                            allStaticSuccess = allResults.all { uploadResult ->
                                                uploadResult.getOrNull()?.let { (jsonSuccess, fileSuccess) ->
                                                    jsonSuccess == "1" && fileSuccess == "1"
                                                } ?: false
                                            }
                                        }
                                        // dynamic 파일들 재전송
                                        val dynamicTargetJo = JSONObject().put("target", "1")
                                        val retryDynamicRequestBodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                                            .addFormDataPart("json", dynamicTargetJo.toString())

                                        if (!dynamicUploadResults.second) {
                                            mvm.dynamicJsonFile?.let { file ->
                                                Log.v("파일정보", "Dynamic JSON: 이름=${file.name}, 크기=${file.length()} bytes")
                                                retryDynamicRequestBodyBuilder.addFormDataPart(
                                                    "json",
                                                    file.name,
                                                    file.asRequestBody("application/json".toMediaTypeOrNull())
                                                )
                                            }
                                        }
                                        if (!dynamicUploadResults.third) {
                                            mvm.dynamicFile?.let { file ->
                                                Log.v("파일 재전송", "Dynamic MP4 재전송: 이름=${file.name}, 크기=${file.length()} bytes")
                                                retryDynamicRequestBodyBuilder.addFormDataPart(
                                                    "file",
                                                    file.name,
                                                    file.asRequestBody("video/mp4".toMediaTypeOrNull())
                                                )
                                            }
                                        }

                                        retryDynamicRequestBodyBuilder.addFormDataPart("json", reMotherJo.toString())

                                        // 새로운 requestBody로 재전송 시도

                                        requestBody = retryDynamicRequestBodyBuilder.build()
                                        val uploadResult = resendMeasureFileWithRetry(
                                            context = this@MeasureSkeletonActivity,
                                            apiUrl = getString(R.string.API_results),
                                            requestBody = requestBody,
                                            isStatic = false,
                                            uploadSn = dynamicUploadSn,
                                            mobileSn = mobileDynamicSn
                                        )
                                        uploadResult.onSuccess { (jsonSuccess, fileSuccess) ->
                                            Log.d("Upload", "Dynamic file upload completed: json=$jsonSuccess, file=$fileSuccess")
                                            retryDynamicSuccess = true
                                        }.onFailure { error ->
                                            Log.e("Upload", "Dynamic file upload failed after all retries", error)
                                        }

                                        if (allStaticSuccess && retryDynamicSuccess) {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                dialog.dismiss()
                                                // ------# 측정 완료 && 업로드 후 싱글턴 저장 #------
                                                finishMeasure(mobileInfoSn, mobileStaticSns, mobileDynamicSn)
                                                return@launch
                                            }
                                        } else {
                                            dialog.dismiss()
                                            Toast.makeText(this@MeasureSkeletonActivity, "전송에 실패한 항목이 있습니다. 다음에 다시 시도해주세요", Toast.LENGTH_LONG).show()
                                            val intent = Intent()
                                            intent.putExtra("finishedMeasure", false)
                                            setResult(Activity.RESULT_OK, intent)
                                            finish()
                                        }
                                    }
                                }
                            }
                        } catch (e: IndexOutOfBoundsException) {
                            Log.e("MSUploadIndex", "${e.message}")
                        } catch (e: IllegalArgumentException) {
                            Log.e("MSUploadIllegal", "${e.message}")
                        } catch (e: IllegalStateException) {
                            Log.e("MSUploadIllegal", "${e.message}")
                        }catch (e: NullPointerException) {
                            Log.e("MSUploadNull", "${e.message}")
                        } catch (e: java.lang.Exception) {
                            Log.e("MSUploadException", "${e.message}")
                        }
                    }
                }
            } else {
                startTimer()
            }
        }

        // ------# 주의사항 키기 #------
        val dialog1 = MeasureSkeletonDialogFragment.newInstance(true, 0)
        dialog1.show(supportFragmentManager, "MeasureSkeletonDialogFragment")
        val dialog2 = MeasureSkeletonDialogFragment.newInstance(false)
        dialog2.show(supportFragmentManager, "MeasureSkeletonDialogFragment")

        binding.ibtnMeasureSkeletonInfo.setOnSingleClickListener {
            dialog1.show(supportFragmentManager, "MeasureSkeletonDialogFragment")
        }
        binding.fabtnMeasureSkeleton.setOnSingleClickListener {
            val dialog3 = MeasureSkeletonDialogFragment.newInstance(true, seqStep.value?.toInt() ?: -1)
            dialog3.show(supportFragmentManager, "MeasureSkeletonDialogFragment")
        }
        binding.ibtnMeasureSkeletonSetup.setOnSingleClickListener {
            val dialog4 = MeasureSetupDialogFragment.newInstance(1)
            dialog4.show(supportFragmentManager, "MeasureSkeletonSetup")
        }
        // ------! 다시 찍기 관리 시작 !------
        seqStep.observe(this@MeasureSkeletonActivity) { count ->
            binding.btnMeasureSkeletonStepPrevious.visibility = if (count.compareTo(0) == 0) {
                View.GONE
            } else {
                View.VISIBLE
            }
            Log.v("visible", "seqStep: ${seqStep.value}")
        }

        binding.btnMeasureSkeletonStepPrevious.setOnSingleClickListener {
            setPreviousStep()
        }
        // ------! 다시 찍기 관리 끝 !------

        // ------! 수직 감도 사라지기 보이기 시작 !------
        hideIndicatorHandler = Handler(Looper.getMainLooper())
        hideIndicatorRunnable = Runnable {
            binding.clCautionAngleVerti.animate().alpha(0f).setDuration(300).start()
            binding.clCautionAngleHorizon.animate().alpha(0f).setDuration(300).start()
            hideIndicator = true
        }
        hideIndicatorHandler.postDelayed(hideIndicatorRunnable, 30000)

        binding.clCautionAngleVerti.setOnClickListener {
            if (!hideIndicator) {
                binding.clCautionAngleVerti.animate().alpha(0f).setDuration(300).start()
                binding.clCautionAngleHorizon.animate().alpha(0f).setDuration(300).start()
                hideIndicator = true
                hideIndicatorHandler.removeCallbacks(hideIndicatorRunnable)
            } else {
                binding.clCautionAngleVerti.animate().alpha(1f).setDuration(300).start()
                binding.clCautionAngleHorizon.animate().alpha(1f).setDuration(300).start()
                hideIndicator = false
                // 다시 타이머 시작
                hideIndicatorHandler.postDelayed(hideIndicatorRunnable, 5000)
            }
        }
        // ------! 수직 감도 사라지기 보이기 끝 !------
    }
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val z = event.values[2]
            val clampedZ = z.coerceIn(-SensorManager.GRAVITY_EARTH, SensorManager.GRAVITY_EARTH)
            val angleZ = Math.toDegrees(asin((clampedZ / SensorManager.GRAVITY_EARTH).toDouble()))
            filteredAngleZ = lowPassFilterZ(angleZ.toFloat())
            val normalizedAngleZ = (filteredAngleZ + 90).coerceIn(0f, 180f)

            val targetBiasZ = 1 - (normalizedAngleZ / 180f)
            currentBiasZ = interpolateZ(targetBiasZ)

            // cvCautionVerti의 layoutParams를 독립적으로 가져와 수정
            val layoutParamsVerti = (binding.cvCautionVerti.layoutParams as ConstraintLayout.LayoutParams)
            layoutParamsVerti.verticalBias = currentBiasZ
            binding.cvCautionVerti.layoutParams = layoutParamsVerti
            binding.cvCautionVerti.requestLayout()
            if (normalizedAngleZ in 88f..92f) {
                binding.cvCautionVerti.setCardBackgroundColor(ContextCompat.getColor(this, R.color.mainColor))
            } else {
                binding.cvCautionVerti.setCardBackgroundColor(ContextCompat.getColor(this, R.color.subColor100))
            }


            // 수평 감지
            val x = event.values[0]
            val clampedX = x.coerceIn(-SensorManager.GRAVITY_EARTH, SensorManager.GRAVITY_EARTH)
            val angleX = Math.toDegrees(asin((clampedX / SensorManager.GRAVITY_EARTH).toDouble()))
            filteredAngleX = lowPassFilterX(angleX.toFloat())
            val normalizedAngleX = (filteredAngleX + 90).coerceIn(0f, 180f)

            val targetBiasX = normalizedAngleX / 180f
            currentBiasX = interpolateX(targetBiasX)

            // cvCautionHorizon의 layoutParams를 독립적으로 가져와 수정
            val layoutParamsHoriz = (binding.cvCautionHorizon.layoutParams as ConstraintLayout.LayoutParams)
            layoutParamsHoriz.horizontalBias = currentBiasX
            binding.cvCautionHorizon.layoutParams = layoutParamsHoriz
            binding.cvCautionHorizon.requestLayout()
            if (normalizedAngleX in 89f..91f) {
                binding.cvCautionHorizon.setCardBackgroundColor(ContextCompat.getColor(this, R.color.mainColor))
            } else {
                binding.cvCautionHorizon.setCardBackgroundColor(ContextCompat.getColor(this, R.color.subColor100))
            }
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    // ------! 센서 시작 !------
    private fun lowPassFilterZ(input: Float): Float {
        filteredAngleZ += ALPHA * (input - filteredAngleZ)
        return filteredAngleZ
    }

    private fun lowPassFilterX(input: Float): Float {
        filteredAngleX += ALPHA * (input - filteredAngleX)
        return filteredAngleX
    }

    private fun interpolateZ(target: Float): Float {
        return currentBiasZ + (target - currentBiasZ) * INTERPOLATION_FACTOR
    }

    private fun interpolateX(target: Float): Float {
        return currentBiasX + (target - currentBiasX) * INTERPOLATION_FACTOR
    }
    // ------# 측정 seq가 종료될 때 실행되는 함수 #------
    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        when (seqStep.value) {
            maxSeq -> {
                binding.pvMeasureSkeleton.progress = 100f
                binding.clMeasureSkeletonCount.visibility = View.VISIBLE

                binding.tvMeasureSkeletonGuide.text = "측정이 완료됐습니다 !"
                binding.tvMeasureSkeletonCount.visibility = View.GONE
                binding.tvMeasureSkeletonGuide.textSize = 36f
                binding.btnMeasureSkeletonStep.text = "완료하기"
                mCountDown.cancel()
                Log.v("몇단계?", "Max repeats reached, stopping the loop")
            }
            else -> {
                Log.v("dynamic잘들어갔", "${mvm.dynamic}, ${mvm.dynamicJa.length()}")
                binding.tvMeasureSkeletonGuide.text = "다음 동작을 준비해주세요"
                seqStep.value = seqStep.value?.plus(1)
                progress += 14
                binding.pvMeasureSkeleton.progress = progress.toFloat()
                Log.v("몇단계?", "seqStep: ${seqStep.value}, progress: $progress")
                binding.clMeasureSkeletonCount.visibility = View.INVISIBLE
                binding.svMeasureSkeleton.go(seqStep.value?.toInt() ?: 0, true)

                Handler(Looper.getMainLooper()).postDelayed({ val dialog = MeasureSkeletonDialogFragment.newInstance(true, seqStep.value?.toInt() ?: -1)
                    dialog.show(supportFragmentManager, "MeasureSkeletonDialogFragment") }, 1000)

                val drawable = ContextCompat.getDrawable(this, resources.getIdentifier("drawable_measure_${seqStep.value!!.toInt()}", "drawable", packageName))
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.ivMeasureSkeletonFrame.setImageDrawable(drawable)
                }, 1100)
            }
        }
        Log.v("updateUI", "progressbar: ${progress}, seqStep: ${seqStep.value}")
    }


    @SuppressLint("SetTextI18n")
    private fun setPreviousStep() {

        /*  이전 버튼 시 동작해야할 사항
        *   1. seqStep.value
        *   2. media file -> jpg, mp4
        *   3. json file -> json
        *   4. comment와 progress들
        * */

        when (seqStep.value) {
            maxSeq -> {
                mvm.statics.removeAt(seqStep.value?.minus(1)  ?: 0)
                mvm.staticFiles.removeAt(seqStep.value ?: 0)
                mvm.staticJsonFiles.removeAt(seqStep.value ?: 0)
                binding.pvMeasureSkeleton.progress -= 16  // 마지막 남은 2까지 전부 빼기
                binding.tvMeasureSkeletonGuide.text = "프레임에 맞춰 서주세요"
                binding.btnMeasureSkeletonStep.text = "측정하기"
                binding.svMeasureSkeleton.go(seqStep.value?.toInt() ?: 0, true)
                binding.tvMeasureSkeletonCount.visibility = View.VISIBLE
                binding.clMeasureSkeletonCount.visibility = View.INVISIBLE
                // 1 3 4 5 6 7
            }
            // 0번 스텝이 완료됐을 때
            1 -> {
                seqStep.value = seqStep.value?.minus(1)
                progress -= 14
                binding.pvMeasureSkeleton.progress = progress.toFloat()
                Log.v("녹화종료되나요?", "seqStep: ${seqStep.value} / 6")
                binding.svMeasureSkeleton.go(seqStep.value?.toInt() ?: 0, true)
                mvm.statics.removeAt(seqStep.value ?: 0) // static을
                mvm.staticFiles.removeAt(seqStep.value ?: 0)
                mvm.staticJsonFiles.removeAt(seqStep.value ?: 0)
                val mirroredVideoFile = File(cacheDir, "mirrored_video.mp4")
                if (mirroredVideoFile.exists()) {
                    // 파일 삭제
                    val isDeleted = mirroredVideoFile.delete()
                    if (isDeleted) {
                        Log.v("MPEGLog","mirrored_video.mp4 파일이 성공적으로 삭제되었습니다.")
                    } else {
                        Log.v("MPEGLog","mirrored_video.mp4 파일 삭제에 실패했습니다.")
                    }
                } else {
                    Log.v("MPEGLog","mirrored_video.mp4 파일이 존재하지 않습니다.")
                }
            }
            // 1번 이상 (dynamic, static 들)
            2, 3, 4, 5, 6 -> {
                seqStep.value = seqStep.value?.minus(1)
                progress -= 14
                binding.pvMeasureSkeleton.progress = progress.toFloat()
                Log.v("촬영중단되나요?", "seqStep: ${seqStep.value} / 6")
                binding.svMeasureSkeleton.go(seqStep.value?.toInt() ?: 0, true)
                // 이곳이 1인 이유는 minus를 한 값기준으로 세기 때문임 when 절의 2, 3, 4, 5, 6 은 이제 할 seqSteps임.
                if (seqStep.value != 1) {
                    mvm.statics.removeAt(seqStep.value?.minus(1) ?: 0) // static을
                    mvm.staticFiles.removeAt(seqStep.value?.minus(1)?: 0)
                    mvm.staticJsonFiles.removeAt(seqStep.value?.minus(1) ?: 0)
                } else {
                    mvm.dynamic = null
                    mvm.dynamicJa = JSONArray()
                    mvm.dynamicFile = null
                    mvm.dynamicJsonFile = null
                }
            }
        }
        val drawable = ContextCompat.getDrawable(this, resources.getIdentifier("drawable_measure_${seqStep.value!!.toInt()}", "drawable", packageName))
        binding.ivMeasureSkeletonFrame.setImageDrawable(drawable)
        Log.v("updateUI", "progressbar: ${progress}, seqStep: ${seqStep.value}, staticsSize: ${mvm.statics.size} / 6 ")
    }

    // ------! 촬영 시 view 즉시 가리고 -> 서서히 보이기 !-----
    private fun hideViews(delay : Long) {
        binding.clMeasureSkeletonTop.visibility = View.INVISIBLE
        binding.fabtnMeasureSkeleton.visibility = View.INVISIBLE
        binding.llMeasureSkeletonBottom.visibility = View.INVISIBLE
        binding.clMeasureSkeletonCount.visibility = View.INVISIBLE
        if (seqStep.value != 1) startCameraShutterAnimation()

        setAnimation(binding.clMeasureSkeletonTop, 850, delay, true) {}
        setAnimation(binding.fabtnMeasureSkeleton, 850, delay, true) {}
        setAnimation(binding.llMeasureSkeletonBottom, 850, delay, true) {}
        binding.btnMeasureSkeletonStep.visibility = View.VISIBLE
        binding.tvMeasureSkeletonGuide.text = "프레임에 맞춰 서주세요"
    }
    // ------! 타이머 control 시작 !------
    private fun startTimer() {
        // 시작 버튼 후 시작
        binding.btnMeasureSkeletonStep.isEnabled = false
        when (seqStep.value) {
            1 -> {
                isCapture = false
                isRecording = true
            }
            else -> {
                isCapture = true
                isRecording = false
            }
        }
        Log.v("seqStep", "${seqStep.value} / 6")
        mCountDown.start()
        // ------! 타이머 control 끝 !------
    }

    private fun setUpCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()
                // Build and bind the camera use cases
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(this)
        )
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        // 회전 정보 가져오기
        val rotation = binding.viewFinder.display?.rotation ?: Surface.ROTATION_0
        Log.d("RotationDebug", "Display Rotation: $rotation")

        // 미리보기 설정
        preview = Preview.Builder()
            .setTargetResolution(Size(720, 1280))
            .setTargetRotation(rotation)
            .build()

        // 중요: 이 시점에서 surfaceProvider 설정
        preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)

        // 이미지 분석 설정
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(720, 1280)) // aspectRatio 대신 resolution 사용
            .setTargetRotation(rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(backgroundExecutor) { image ->
                    detectPose(image)
                }
            }

        // 이미지 캡처 설정
        imageCapture = ImageCapture.Builder()
            .setTargetResolution(Size(720, 1280))
            .setTargetRotation(rotation)
            .build()

        videoCapture = VideoCapture.withOutput(
            Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
        )

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, imageAnalyzer, imageCapture, videoCapture, preview
            )
        } catch (e: IndexOutOfBoundsException) {
            Log.e("MSCameraIndex", "${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.e("MSCameraIllegal", "${e.message}")
        } catch (e: IllegalStateException) {
            Log.e("MSCameraIllegal", "${e.message}")
        }catch (e: NullPointerException) {
            Log.e("MSCameraNull", "${e.message}")
        } catch (e: java.lang.Exception) {
            Log.e("MSCameraException", "${e.message}")
        }
    }

    private fun detectPose(imageProxy: ImageProxy) {
        if (this::poseLandmarkerHelper.isInitialized) {
            poseLandmarkerHelper.detectLiveStream(
                imageProxy = imageProxy,
                isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
            )
            if (startRecording && isRecording && latestResult != null) {
                resultBundleToJson(latestResult, seqStep.value?: -1)
            }
        }
        imageProxy.close()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            binding.viewFinder.display.rotation
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != REQUEST_CODE_PERMISSIONS) return  // 잘못된 요청 코드 방지

        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            setUpCamera()
            Log.v("스켈레톤 Init", "모든 권한 승인 완료")
        } else {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] == PackageManager.PERMISSION_DENIED
            }
            Log.v("스켈레톤 Init", "거부된 권한: ${deniedPermissions.joinToString()}")

            // "다시 묻지 않음"을 체크한 경우 -> 앱 종료
            if (deniedPermissions.all { !shouldShowRequestPermissionRationale(it) }) {
                finish()
                Toast.makeText(this, "권한을 모두 허용한 후 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            } else {
                // 한 번 거부한 경우 -> 설명 다이얼로그 표시
                showPermissionExplanationDialog()
            }
        }
    }


    private fun showPermissionExplanationDialog() {
        if (permissionDialog?.isShowing == true) return  // 이미 다이얼로그가 떠 있으면 return

        permissionDialog = AlertDialog.Builder(this)
            .setTitle("권한 필요")
            .setMessage("측정을 위해서는 사진 및 갤러리 권한을 모두 허용해야 합니다.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                })
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("권한 설정 필요")
            .setMessage("설정에서 권한을 허용해주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                })
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
    }

    override fun onError(error: String, errorCode: Int) {
        runOnUiThread {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        runOnUiThread {
            // Pass necessary information to OverlayView for drawing on the canvas
            val customResult = PoseLandmarkAdapter.toCustomPoseLandmarkResult(resultBundle.results.first())

            // ------# scaleFactor 초기화 #------
            binding.overlay.setResults(
                customResult,
                resultBundle.inputImageWidth,
                resultBundle.inputImageHeight,
                OverlayView.RunningMode.LIVE_STREAM
            )
            latestResult = resultBundle
            binding.overlay.invalidate()
        }
    }

    // override 로 resultbundle이 계속 나오는데 해당 항목을 전역변수 latest
    fun resultBundleToJson(resultBundle: PoseLandmarkerHelper.ResultBundle?, step: Int) {
        if (scaleFactorX == null && scaleFactorY == null) {
            val inputWidth = latestResult?.inputImageWidth
            val inputHeight = latestResult?.inputImageHeight
            if (inputWidth != null && inputWidth != 0 && inputHeight != null && inputHeight != 0) {
                scaleFactorX = (binding.overlay.width / inputWidth).toFloat()
                scaleFactorY = (binding.overlay.height / inputHeight).toFloat()
            } else {
                scaleFactorX = 1f
                scaleFactorY = 1f
            }
            Log.v("ScreenSettings", "scaleFactor(x, y): ($scaleFactorX, $scaleFactorY), imageSize(width, height): (${latestResult?.inputImageWidth}, ${latestResult?.inputImageHeight})")
        }

        val frameStartTime = System.nanoTime()
        if (resultBundle?.results?.first()?.landmarks()?.isNotEmpty() == true) {
            val plr = resultBundle.results.first().landmarks()[0]
            val tempLandmarks = mutableMapOf<Int, Triple<Float, Float, Float>>()
            // 원시데이터 pose 33개
            plr.forEachIndexed { index, poseLandmark ->
                tempLandmarks[index] = Triple(
                    poseLandmark.x(),
                    poseLandmark.y(),
                    poseLandmark.z()
                )
            }
            // 스케일링 pose 33개
            plr.forEachIndexed { index, poseLandmark ->
                val jo = JSONObject().apply {
                    put("index", index)
                    put("isActive", true)
//                    if (index == 0) { // 코는 아주 유명한 몸에서 1개만 있는 부위임
                    put("sx", calculateScreenX(poseLandmark.x()).roundToInt())
                    put("sy", calculateScreenY(poseLandmark.y()).roundToInt())
                    put("wx", poseLandmark.x())
                    put("wy", poseLandmark.y())
                    put("wz", poseLandmark.z())
                }
                poseLandmarks.put(jo)
            }
            // 좌우반전과 똑같이 데이터 좌우 반전으로 넣기 ( 각도와 거리 등을 계산하기 위해 )
            mvm.apply {
                noseData = Pair(calculateScreenX(plr[0].x()), calculateScreenY(plr[0].y()))
                earData = listOf(
                    Pair(calculateScreenX(plr[7].x()), calculateScreenY(plr[7].y())),
                    Pair(calculateScreenX(plr[8].x()), calculateScreenY(plr[8].y()))

                )
                shoulderData = listOf(
                    Pair(calculateScreenX(plr[11].x()), calculateScreenY(plr[11].y())),
                    Pair(calculateScreenX(plr[12].x()), calculateScreenY(plr[12].y()))
                )
                elbowData = listOf(
                    Pair(calculateScreenX(plr[13].x()), calculateScreenY(plr[13].y())),
                    Pair(calculateScreenX(plr[14].x()), calculateScreenY(plr[14].y()))
                )
                wristData = listOf(
                    Pair(calculateScreenX(plr[15].x()), calculateScreenY(plr[15].y())),
                    Pair(calculateScreenX(plr[16].x()), calculateScreenY(plr[16].y()))
                )
                hipData = listOf(
                    Pair(calculateScreenX(plr[23].x()), calculateScreenY(plr[23].y())),
                    Pair(calculateScreenX(plr[24].x()), calculateScreenY(plr[24].y()))
                )
                kneeData = listOf(
                    Pair(calculateScreenX(plr[25].x()), calculateScreenY(plr[25].y())),
                    Pair(calculateScreenX(plr[26].x()), calculateScreenY(plr[26].y()))
                )
                ankleData = listOf(
                    Pair(calculateScreenX(plr[27].x()), calculateScreenY(plr[27].y())),
                    Pair(calculateScreenX(plr[28].x()), calculateScreenY(plr[28].y()))

                )
                heelData = listOf(
                    Pair(calculateScreenX(plr[29].x()), calculateScreenY(plr[29].y())),
                    Pair(calculateScreenX(plr[30].x()), calculateScreenY(plr[30].y()))
                )
                toeData = listOf(
                    Pair(calculateScreenX(plr[31].x()), calculateScreenY(plr[31].y())),
                    Pair(calculateScreenX(plr[32].x()), calculateScreenY(plr[32].y()))

                )
            }
            val ankleAxis : Pair<Float ,Float> = Pair( (mvm.ankleData[1].first + mvm.ankleData[0].first) / 2, (mvm.ankleData[1].second + mvm.ankleData[0].second) / 2 )
            var middleHip = Pair((mvm.hipData[0].first + mvm.hipData[1].first) / 2, (mvm.hipData[0].second + mvm.hipData[1].second) / 2)
            val middleShoulder = Pair((mvm.shoulderData[0].first + mvm.shoulderData[1].first) / 2, (mvm.shoulderData[0].second + mvm.shoulderData[1].second) / 2)
            when (step) {
                0 -> {
                    val earAngle : Float = calculateSlope(mvm.earData[0].first, mvm.earData[0].second, mvm.earData[1].first, mvm.earData[1].second) % 180
                    val shoulderAngle : Float = calculateSlope(mvm.shoulderData[0].first, mvm.shoulderData[0].second, mvm.shoulderData[1].first, mvm.shoulderData[1].second) % 180
                    val elbowAngle : Float = calculateSlope(mvm.elbowData[0].first, mvm.elbowData[0].second, mvm.elbowData[1].first, mvm.elbowData[1].second) % 180
                    val wristAngle : Float = calculateSlope(mvm.wristData[0].first, mvm.wristData[0].second, mvm.wristData[1].first, mvm.wristData[1].second) % 180
                    val hipAngle : Float = calculateSlope(mvm.hipData[0].first, mvm.hipData[0].second, mvm.hipData[1].first, mvm.hipData[1].second) % 180
                    val kneeAngle : Float = calculateSlope(mvm.kneeData[0].first, mvm.kneeData[0].second, mvm.kneeData[1].first, mvm.kneeData[1].second) % 180
                    val ankleAngle : Float = calculateSlope(mvm.ankleData[0].first, mvm.ankleData[0].second, mvm.ankleData[1].first, mvm.ankleData[1].second) % 180
                    // 부위 양 높이 차이
                    val earSubDistance : Float = getRealDistanceY(mvm.earData[0], mvm.earData[1])
                    val shoulderSubDistance : Float = getRealDistanceY(mvm.shoulderData[0], mvm.shoulderData[1])
                    val elbowSubDistance : Float = getRealDistanceY(mvm.elbowData[0], mvm.elbowData[1])
                    val wristSubDistance : Float = getRealDistanceY(mvm.wristData[0], mvm.wristData[1])
                    val hipSubDistance : Float = getRealDistanceY(mvm.hipData[0], mvm.hipData[1])
                    val kneeSubDistance : Float = getRealDistanceY(mvm.kneeData[0], mvm.kneeData[1])
                    val ankleSubDistance : Float = getRealDistanceY(mvm.ankleData[0], mvm.ankleData[1])

                    // 각 부위 x축 부터 거리 거리
                    val wristSubDistanceByX : Pair<Float, Float> = Pair(getRealDistanceX(mvm.wristData[0], ankleAxis), getRealDistanceX(mvm.wristData[1], ankleAxis))
                    val kneeSubDistanceByX : Pair<Float, Float> = Pair(getRealDistanceX(mvm.kneeData[0], ankleAxis), getRealDistanceX(mvm.kneeData[1], ankleAxis))
                    val ankleSubDistanceByX : Pair<Float, Float> = Pair(getRealDistanceX(mvm.ankleData[0], ankleAxis), getRealDistanceX(mvm.ankleData[1], ankleAxis))
                    val toeSubDistanceByX : Pair<Float, Float> = Pair(getRealDistanceX(mvm.toeData[0], ankleAxis), getRealDistanceX(mvm.toeData[1], ankleAxis))

                    val shoulderElbowLean: Pair<Float, Float> = Pair(abs(calculateSlope(mvm.shoulderData[0].first, mvm.shoulderData[0].second, mvm.elbowData[0].first, mvm.elbowData[0].second)) % 180 ,
                        abs(calculateSlope(mvm.shoulderData[1].first, mvm.shoulderData[1].second, mvm.elbowData[1].first, mvm.elbowData[1].second)) % 180 )

                    val elbowWristLean: Pair<Float, Float> = Pair(abs(calculateSlope(mvm.elbowData[0].first, mvm.elbowData[0].second, mvm.wristData[0].first, mvm.wristData[0].second)) % 180 ,
                        abs(calculateSlope(mvm.elbowData[1].first, mvm.elbowData[1].second, mvm.wristData[1].first, mvm.wristData[1].second)) % 180 )

                    val hipKneeLean : Pair<Float, Float> = Pair(abs(calculateSlope(mvm.hipData[0].first, mvm.hipData[0].second, mvm.kneeData[0].first, mvm.kneeData[0].second)) % 180 ,
                        abs(calculateSlope(mvm.hipData[1].first, mvm.hipData[1].second, mvm.kneeData[1].first, mvm.kneeData[1].second)) % 180 )

                    val kneeAnkleLean : Pair<Float, Float> = Pair(abs(calculateSlope(mvm.kneeData[0].first, mvm.kneeData[0].second, mvm.ankleData[0].first, mvm.ankleData[0].second)) % 180 ,
                        abs(calculateSlope(mvm.kneeData[1].first, mvm.kneeData[1].second, mvm.ankleData[1].first, mvm.ankleData[1].second)) % 180 )
                    val ankleToeLean : Pair<Float, Float> = Pair(abs(calculateSlope(mvm.ankleData[0].first, mvm.ankleData[0].second, mvm.toeData[0].first, mvm.toeData[0].second)) % 180 ,
                        abs(calculateSlope(mvm.ankleData[1].first, mvm.ankleData[1].second, mvm.toeData[1].first, mvm.toeData[1].second)) % 180 )

                    val shoulderElbowWristAngle : Pair<Float, Float> = Pair(calculateAngle(mvm.shoulderData[0].first, mvm.shoulderData[0].second, mvm.elbowData[0].first, mvm.elbowData[0].second, mvm.wristData[0].first, mvm.wristData[0].second) % 180 ,
                        calculateAngle(mvm.shoulderData[1].first, mvm.shoulderData[1].second, mvm.elbowData[1].first, mvm.elbowData[1].second, mvm.wristData[1].first, mvm.wristData[1].second) % 180 )

                    val hipKneeAnkleAngle : Pair<Float, Float> = Pair(calculateAngle(mvm.hipData[0].first, mvm.hipData[0].second, mvm.kneeData[0].first, mvm.kneeData[0].second, mvm.ankleData[0].first, mvm.ankleData[0].second) % 180 ,
                        calculateAngle(mvm.hipData[1].first, mvm.hipData[1].second, mvm.kneeData[1].first, mvm.kneeData[1].second, mvm.ankleData[1].first, mvm.ankleData[1].second) % 180 )


                    mvm.staticjo.apply {
                        put("front_horizontal_angle_ear", safePut(earAngle))
                        put("front_horizontal_angle_shoulder", safePut(shoulderAngle))
                        put("front_horizontal_angle_elbow", safePut(elbowAngle))
                        put("front_horizontal_angle_wrist", safePut(wristAngle))
                        put("front_horizontal_angle_hip", safePut(hipAngle))
                        put("front_horizontal_angle_knee", safePut(kneeAngle))
                        put("front_horizontal_angle_ankle", safePut(ankleAngle))

                        put("front_horizontal_distance_sub_ear", earSubDistance)
                        put("front_horizontal_distance_sub_shoulder", shoulderSubDistance)
                        put("front_horizontal_distance_sub_elbow", elbowSubDistance)
                        put("front_horizontal_distance_sub_wrist", wristSubDistance)
                        put("front_horizontal_distance_sub_hip", hipSubDistance)
                        put("front_horizontal_distance_sub_knee", kneeSubDistance)
                        put("front_horizontal_distance_sub_ankle", ankleSubDistance)

                        put("front_horizontal_distance_wrist_left", wristSubDistanceByX.first)
                        put("front_horizontal_distance_wrist_right", wristSubDistanceByX.second)
                        put("front_horizontal_distance_knee_left", kneeSubDistanceByX.first)
                        put("front_horizontal_distance_knee_right", kneeSubDistanceByX.second)
                        put("front_horizontal_distance_ankle_left", ankleSubDistanceByX.first)
                        put("front_horizontal_distance_ankle_right", ankleSubDistanceByX.second)
                        put("front_horizontal_distance_toe_left", toeSubDistanceByX.first)
                        put("front_horizontal_distance_toe_right", toeSubDistanceByX.second)

                        put("front_vertical_angle_shoulder_elbow_left", safePut(shoulderElbowLean.first))
                        put("front_vertical_angle_shoulder_elbow_right", safePut(shoulderElbowLean.second))
                        put("front_vertical_angle_elbow_wrist_left", safePut(elbowWristLean.first))
                        put("front_vertical_angle_elbow_wrist_right",safePut(elbowWristLean.second))
                        put("front_vertical_angle_hip_knee_left",safePut(hipKneeLean.first))
                        put("front_vertical_angle_hip_knee_right",safePut(hipKneeLean.second))
                        put("front_vertical_angle_knee_ankle_left", safePut(kneeAnkleLean.first))
                        put("front_vertical_angle_knee_ankle_right", safePut(kneeAnkleLean.second))
                        put("front_vertical_angle_ankle_toe_left", safePut(ankleToeLean.first))
                        put("front_vertical_angle_ankle_toe_right", safePut( ankleToeLean.second))

                        put("front_vertical_angle_shoulder_elbow_wrist_left", safePut(shoulderElbowWristAngle.first))
                        put("front_vertical_angle_shoulder_elbow_wrist_right", safePut(shoulderElbowWristAngle.second))
                        put("front_vertical_angle_hip_knee_ankle_left", safePut(hipKneeAnkleAngle.first))
                        put("front_vertical_angle_hip_knee_ankle_right", safePut(hipKneeAnkleAngle.second))
                    }
                    Log.v("전면데이터", "ear: ${mvm.earData}, shoulder: ${mvm.shoulderData}, elbow: ${mvm.elbowData}, wrist: ${mvm.wristData}, hip: ${mvm.hipData}, knee: ${mvm.kneeData}, ankle: ${mvm.ankleData}")
                    Log.v("전면각도들", "손목거리: $wristSubDistanceByX, 무릎거리: $kneeSubDistanceByX, 어깨팔꿉: $shoulderElbowLean, 팔꿉손목: $elbowWristLean, 골반무릎: $hipKneeLean, 무릎발목: $kneeAnkleLean, ")
                    saveJson(mvm.staticjo, step)
                }
                1 -> {  // 스쿼트
                    // 현재 프레임 당 계속 넣을 공간임
                    // ------# 각도 타임으로 넣기 #------
                    val wristAngle : Float = calculateSlope(mvm.wristData[0].first, mvm.wristData[0].second, mvm.wristData[1].first, mvm.wristData[1].second)
                    val wristDistance : Float = getRealDistanceX(mvm.wristData[0], mvm.wristData[1])

                    val wristDistanceByCenter : Pair<Float, Float> = Pair(getRealDistanceX(mvm.wristData[0], ankleAxis),
                        getRealDistanceX(mvm.wristData[1], ankleAxis))

                    val elbowAngle : Float = calculateSlope(mvm.elbowData[0].first, mvm.elbowData[0].second, mvm.elbowData[1].first, mvm.elbowData[1].second)
                    val elbowDistance : Float = getRealDistanceX(mvm.wristData[0], mvm.wristData[1])

                    val shoulderAngle : Float = calculateSlope(mvm.shoulderData[0].first, mvm.shoulderData[0].second, mvm.shoulderData[1].first, mvm.shoulderData[1].second)
                    val shoulderDistance : Float = getRealDistanceX(mvm.wristData[0], mvm.wristData[1])

                    val hipAngle : Float = calculateSlope(mvm.hipData[0].first, mvm.hipData[0].second, mvm.hipData[1].first, mvm.hipData[1].second)
                    val hipDistance : Float = getRealDistanceX(mvm.hipData[0], mvm.hipData[1])
                    val hipDistanceByCenter : Pair<Float, Float> = Pair(getRealDistanceX(mvm.hipData[0], ankleAxis),
                        getRealDistanceX(mvm.hipData[1], ankleAxis))

                    val kneeAngle : Float = calculateSlope(mvm.kneeData[0].first, mvm.kneeData[0].second, mvm.kneeData[1].first, mvm.kneeData[1].second)
                    val kneeDistance : Float = getRealDistanceX(mvm.kneeData[0], mvm.kneeData[1])
                    val kneeDistanceByCenter : Pair<Float, Float> = Pair(getRealDistanceX(mvm.kneeData[0], ankleAxis), getRealDistanceX(mvm.kneeData[1], ankleAxis))

                    val toeDistanceByCenter : Pair<Float, Float> = Pair(getRealDistanceX(mvm.toeData[0], ankleAxis),
                        getRealDistanceX(mvm.toeData[1], ankleAxis))

                    val wristElbowShoulderAngle : Pair<Float, Float> = Pair(calculateAngle(mvm.wristData[0].first, mvm.wristData[0].second, mvm.elbowData[0].first, mvm.elbowData[0].second, mvm.shoulderData[0].first, mvm.shoulderData[0].second) % 180 ,
                        calculateAngle(mvm.wristData[1].first, mvm.wristData[1].second, mvm.elbowData[1].first, mvm.elbowData[1].second, mvm.shoulderData[1].first, mvm.shoulderData[1].second) % 180 )

                    val wristElbowAngle : Pair<Float, Float> = Pair(calculateSlope(mvm.wristData[0].first, mvm.wristData[0].second, mvm.elbowData[0].first, mvm.elbowData[0].second)  % 180 ,
                        calculateSlope(mvm.wristData[1].first, mvm.wristData[1].second, mvm.elbowData[1].first, mvm.elbowData[1].second) % 180 )

                    val elbowShoulderAngle : Pair<Float, Float> = Pair(calculateSlope(mvm.elbowData[0].first, mvm.elbowData[0].second, mvm.shoulderData[0].first, mvm.shoulderData[0].second) % 180 ,
                        calculateSlope(mvm.elbowData[1].first, mvm.elbowData[1].second, mvm.shoulderData[1].first, mvm.shoulderData[1].second) % 180 )

                    val hipKneeToeAngle : Pair<Float, Float> = Pair(calculateAngle(mvm.hipData[0].first, mvm.hipData[0].second, mvm.kneeData[0].first, mvm.kneeData[0].second, mvm.toeData[0].first, mvm.toeData[0].second) % 180 ,
                        calculateAngle(mvm.hipData[1].first, mvm.hipData[1].second, mvm.kneeData[1].first, mvm.kneeData[1].second, mvm.toeData[1].first, mvm.toeData[1].second) % 180 )

                    val hipKneeAngle : Pair<Float, Float> = Pair(calculateSlope(mvm.hipData[0].first, mvm.hipData[0].second, mvm.kneeData[0].first, mvm.kneeData[0].second) % 180 ,
                        calculateSlope(mvm.hipData[1].first, mvm.hipData[1].second, mvm.kneeData[1].first, mvm.kneeData[1].second) % 180 )

                    val kneeToeAngle : Pair<Float, Float> = Pair(calculateSlope(mvm.kneeData[0].first, mvm.kneeData[0].second, mvm.toeData[0].first, mvm.toeData[0].second) % 180 ,
                        calculateSlope(mvm.kneeData[1].first, mvm.kneeData[1].second, mvm.toeData[1].first, mvm.toeData[1].second) % 180 )

                    val ankleToeAngle : Pair<Float, Float> = Pair(calculateSlope(mvm.ankleData[0].first, mvm.ankleData[0].second, mvm.toeData[0].first, mvm.toeData[0].second) % 180 ,
                        calculateSlope(mvm.ankleData[1].first, mvm.ankleData[1].second, mvm.toeData[1].first, mvm.toeData[1].second) % 180 )

                    val kneeAnkleToeAngle : Pair<Float, Float> = Pair(calculateAngle(mvm.kneeData[0].first, mvm.kneeData[0].second, mvm.ankleData[0].first, mvm.ankleData[0].second, mvm.toeData[0].first, mvm.toeData[0].second) % 180 ,
                        calculateAngle(mvm.kneeData[1].first, mvm.kneeData[1].second, mvm.ankleData[1].first, mvm.ankleData[1].second, mvm.toeData[1].first, mvm.toeData[1].second) % 180 )

                    if (!isNameInit) {
                        videoFileName = "0_${measureInfoSn}_2_7_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}"
                        measureVideoName = "MT_DYNAMIC_OVERHEADSQUAT_FRONT_0_0_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}"
                        isNameInit = true
                    }

                    // 1프레임당 dynamic 측정 모두 들어감
                    mvm.dynamicJoUnit.apply {
                        put("device_sn", 0)
                        put("user_uuid", singletonUser.jsonObject?.getString("user_uuid"))
                        put("mobile_device_uuid", decryptedUUID)
                        put("user_name", singletonUser.jsonObject?.getString("user_name") ?: "" )
                        put("user_sn", singletonUser.jsonObject?.getInt("user_sn") ?: -1)
                        put("measure_seq", 2)
                        put("measure_type", 7)
                        put("measure_start_time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        put("measure_end_time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        put("measure_overlay_width", binding.overlay.width)
                        put("measure_overlay_height", binding.overlay.height)
                        put("measure_overlay_scale_factor_x", scaleFactorX)
                        put("measure_overlay_scale_factor_y", scaleFactorY)
                        put("measure_photo_file_name", measureVideoName)
                        put("measure_server_json_name", "$videoFileName.json")
                        put("measure_server_file_name", "$videoFileName.mp4")

                        put("front_horizontal_angle_wrist", wristAngle)
                        put("front_horizontal_distance_wrist", wristDistance)
                        put("front_horizontal_distance_center_left_wrist", wristDistanceByCenter.first)
                        put("front_horizontal_distance_center_right_wrist", wristDistanceByCenter.second)
                        put("front_horizontal_angle_elbow", elbowAngle)
                        put("front_horizontal_distance_elbow", elbowDistance)
                        put("front_horizontal_angle_shoulder", shoulderAngle)
                        put("front_horizontal_distance_shoulder", shoulderDistance)
                        put("front_horizontal_angle_hip", hipAngle)
                        put("front_horizontal_distance_hip", hipDistance)
                        put("front_horizontal_distance_center_left_hip", hipDistanceByCenter.first)
                        put("front_horizontal_distance_center_right_hip", hipDistanceByCenter.second)
                        put("front_horizontal_angle_knee", kneeAngle)
                        put("front_horizontal_distance_knee", kneeDistance)
                        put("front_horizontal_distance_center_left_knee", kneeDistanceByCenter.first)
                        put("front_horizontal_distance_center_right_knee", kneeDistanceByCenter.second)
                        put("front_horizontal_distance_center_left_toe", toeDistanceByCenter.first)
                        put("front_horizontal_distance_center_right_toe", toeDistanceByCenter.second)
                        put("front_vertical_angle_wrist_elbow_shoulder_left", safePut(wristElbowShoulderAngle.first))
                        put("front_vertical_angle_wrist_elbow_shoulder_right", safePut(wristElbowShoulderAngle.second))
                        put("front_vertical_angle_wrist_elbow_left", safePut(wristElbowAngle.first))
                        put("front_vertical_angle_wrist_elbow_right", safePut(wristElbowAngle.second))
                        put("front_vertical_angle_elbow_shoulder_left", safePut(elbowShoulderAngle.first))
                        put("front_vertical_angle_elbow_shoulder_right", safePut(elbowShoulderAngle.second))
                        put("front_vertical_angle_hip_knee_toe_left", safePut(hipKneeToeAngle.first))
                        put("front_vertical_angle_hip_knee_toe_right", safePut(hipKneeToeAngle.second))
                        put("front_vertical_angle_hip_knee_left", safePut(hipKneeAngle.first))
                        put("front_vertical_angle_hip_knee_right", safePut(hipKneeAngle.second))
                        put("front_vertical_angle_knee_toe_left", safePut(kneeToeAngle.first))
                        put("front_vertical_angle_knee_toe_right", safePut(kneeToeAngle.second))
                        put("front_vertical_angle_ankle_toe_left", safePut(ankleToeAngle.first))
                        put("front_vertical_angle_ankle_toe_right", safePut(ankleToeAngle.second))
                        put("front_vertical_angle_knee_ankle_toe_left", safePut(kneeAnkleToeAngle.first))
                        put("front_vertical_angle_knee_ankle_toe_right", safePut(kneeAnkleToeAngle.second))
                        put("times",String.format("%.7f", (System.nanoTime() - frameStartTime) / 1_000_000_000f).toFloat())
                        put("pose_landmark", poseLandmarks)
                    }
                    // 1프레임당 dynamic 측정 모두 들어감
                    mvm.dynamicJa.put(mvm.dynamicJoUnit)
                    mvm.dynamicJoUnit = JSONObject()
                }
                2 -> { // ------! 주먹 쥐고 !------

                    val wristShoulderDistance : Pair<Float, Float> = Pair(getRealDistanceX(mvm.wristData[0], mvm.shoulderData[0]),
                        getRealDistanceX(mvm.wristData[1], mvm.shoulderData[1]))

                    val wristsDistanceByY : Float = getRealDistanceY(mvm.wristData[1], ankleAxis)
                    val shoulderElbowWristAngle : Pair<Float, Float> = Pair(calculateAngle(mvm.shoulderData[0].first, mvm.shoulderData[0].second, mvm.elbowData[0].first, mvm.elbowData[0].second, mvm.wristData[0].first, mvm.wristData[0].second) % 180 ,
                        calculateAngle(mvm.shoulderData[1].first, mvm.shoulderData[1].second, mvm.elbowData[1].first, mvm.elbowData[1].second, mvm.wristData[1].first, mvm.wristData[1].second) % 180 )
                    val wristDistanceByX: Pair<Float, Float> = Pair(getRealDistanceX(mvm.wristData[0], middleShoulder), getRealDistanceX(mvm.wristData[1], middleShoulder))

                    mvm.staticjo.apply {
                        put("front_elbow_align_distance_left_wrist_shoulder", wristShoulderDistance.first)
                        put("front_elbow_align_distance_right_wrist_shoulder", wristShoulderDistance.second)
                        put("front_elbow_align_distance_wrist_height", wristsDistanceByY)
                        put("front_elbow_align_angle_left_shoulder_elbow_wrist", safePut(shoulderElbowWristAngle.first))
                        put("front_elbow_align_angle_right_shoulder_elbow_wrist", safePut(shoulderElbowWristAngle.second))
                        put("front_elbow_align_distance_center_wrist_left", wristDistanceByX.first)
                        put("front_elbow_align_distance_center_wrist_right", wristDistanceByX.second)
                    }
                    Log.v("팔꿉", "ear: ${mvm.earData} shoulder: ${mvm.shoulderData}, elbow: ${mvm.elbowData}, wrist: ${mvm.wristData}")
                    Log.v("팔꿉각도들", "어깨팔꿉손목각: $shoulderElbowWristAngle")

                    saveJson(mvm.staticjo, step)
                }
                3 -> { // 왼쪽보기 (오른쪽 팔)

                    // ------! 측면 거리  - 왼쪽 !------
                    val sideLeftShoulderDistance : Float = getRealDistanceX(mvm.shoulderData[0], ankleAxis)
                    val sideLeftWristDistance : Float = getRealDistanceX(mvm.wristData[0], ankleAxis)
                    val sideLeftHipDistance : Float = getRealDistanceX(mvm.hipData[0], ankleAxis)

                    val sideLeftShoulderElbowLean : Float = 180 + (calculateSlope(mvm.shoulderData[0].first, mvm.shoulderData[0].second, mvm.kneeData[0].first, mvm.kneeData[0].second) % 180)
                    val sideLeftElbowWristLean: Float = 180 + (calculateSlope(mvm.elbowData[0].first, mvm.elbowData[0].second, mvm.wristData[0].first, mvm.wristData[0].second) % 180)
                    val sideLeftHipKneeLean : Float = 180 + (calculateSlope(mvm.hipData[0].first, mvm.hipData[0].second, mvm.kneeData[0].first, mvm.kneeData[0].second) % 180)
                    val sideLeftEarShoulderLean : Float = 180 + (calculateSlope(mvm.earData[0].first, mvm.earData[0].second, mvm.shoulderData[0].first, mvm.shoulderData[0].second) % 180)

                    val sideLeftNoseShoulderLean : Float = 180 + (calculateSlope(mvm.noseData.first, mvm.noseData.second, mvm.shoulderData[0].first, mvm.shoulderData[0].second))

                    val sideLeftShoulderElbowWristAngle : Float = calculateAngle(mvm.shoulderData[0].first, mvm.shoulderData[0].second, mvm.elbowData[0].first, mvm.elbowData[0].second, mvm.wristData[0].first, mvm.wristData[0].second) % 180
                    val sideLeftHipKneeAnkleAngle : Float = calculateAngle(mvm.hipData[0].first, mvm.hipData[0].second, mvm.kneeData[0].first, mvm.kneeData[0].second, mvm.ankleData[0].first, mvm.ankleData[0].second) % 180

                    mvm.staticjo.apply {
                        put("side_left_horizontal_distance_shoulder", sideLeftShoulderDistance)
                        put("side_left_horizontal_distance_hip", sideLeftHipDistance)
                        put("side_left_horizontal_distance_wrist", sideLeftWristDistance)

                        put("side_left_vertical_angle_shoulder_elbow", safePut( sideLeftShoulderElbowLean ))
                        put("side_left_vertical_angle_elbow_wrist", safePut( sideLeftElbowWristLean ))
                        put("side_left_vertical_angle_hip_knee", safePut( sideLeftHipKneeLean ))
                        put("side_left_vertical_angle_ear_shoulder", safePut( sideLeftEarShoulderLean ))
                        put("side_left_vertical_angle_nose_shoulder", abs(safePut( sideLeftNoseShoulderLean ) % 90))
                        put("side_left_vertical_angle_shoulder_elbow_wrist", safePut( sideLeftShoulderElbowWristAngle ))
                        put("side_left_vertical_angle_hip_knee_ankle", safePut( sideLeftHipKneeAnkleAngle ))
                    }

                    Log.v("좌측 데이터", "코: ${mvm.noseData}, 어깨: ${mvm.shoulderData[1]}, 팔꿉: ${mvm.elbowData[1]}, 손목: ${mvm.wristData[1]}, 골반: ${mvm.hipData[1]}, 무릎: ${mvm.kneeData[1]}, 발목: ${mvm.ankleData[1]}")
                    Log.v("좌측각도들", "어깨팔꿉: $sideLeftShoulderElbowLean, 팔꿉손목: $sideLeftElbowWristLean, 골반무릎: $sideLeftHipKneeLean, 귀어깨: $sideLeftEarShoulderLean ")

                    saveJson(mvm.staticjo, step)
                }
                4 -> { // 오른쪽보기 (왼쪽 팔)
                    // ------! 측면 거리  - 오른쪽 !------

                    val sideRightShoulderDistance : Float = getRealDistanceX(mvm.shoulderData[1], ankleAxis)
                    val sideRightWristDistance : Float = getRealDistanceX(mvm.wristData[1], ankleAxis)
                    val sideRightHipDistance : Float = getRealDistanceX(mvm.hipData[1], ankleAxis)
                    // ------! 측면 기울기  - 오른쪽 !------
                    val sideRightShoulderElbowLean : Float = abs(calculateSlope(mvm.shoulderData[1].first, mvm.shoulderData[1].second, mvm.kneeData[1].first, mvm.kneeData[1].second)) % 180
                    val sideRightElbowWristLean: Float = abs(calculateSlope(mvm.elbowData[1].first, mvm.elbowData[1].second, mvm.wristData[1].first, mvm.wristData[1].second)) % 180
                    val sideRightHipKneeLean : Float = abs(calculateSlope(mvm.hipData[1].first, mvm.hipData[1].second, mvm.kneeData[1].first, mvm.kneeData[1].second)) % 180
                    val sideRightEarShoulderLean : Float = abs(calculateSlope(mvm.earData[1].first, mvm.earData[1].second, mvm.shoulderData[1].first, mvm.shoulderData[1].second)) % 180

                    val sideRightNoseShoulderLean : Float = abs(calculateSlope(mvm.shoulderData[1].first, mvm.shoulderData[1].second, mvm.noseData.first, mvm.noseData.second ))

                    val sideRightShoulderElbowWristAngle : Float = calculateAngle(mvm.shoulderData[1].first, mvm.shoulderData[1].second, mvm.elbowData[1].first, mvm.elbowData[1].second, mvm.wristData[1].first, mvm.wristData[1].second) % 180
                    val sideRightHipKneeAnkleAngle : Float = calculateAngle(mvm.hipData[1].first, mvm.hipData[1].second, mvm.kneeData[1].first, mvm.kneeData[1].second, mvm.ankleData[1].first, mvm.ankleData[1].second) % 180
                    mvm.staticjo.apply {
                        put("side_right_horizontal_distance_shoulder", sideRightShoulderDistance)
                        put("side_right_horizontal_distance_wrist", sideRightWristDistance)
                        put("side_right_horizontal_distance_hip", sideRightHipDistance)

                        put("side_right_vertical_angle_shoulder_elbow", safePut(sideRightShoulderElbowLean))
                        put("side_right_vertical_angle_elbow_wrist", safePut(sideRightElbowWristLean))
                        put("side_right_vertical_angle_hip_knee", safePut(sideRightHipKneeLean))
                        put("side_right_vertical_angle_ear_shoulder", safePut(sideRightEarShoulderLean)) // 거북목 거의 없을 때 90언저리까지 나옴
                        put("side_right_vertical_angle_nose_shoulder", abs(safePut(sideRightNoseShoulderLean) % 90))
                        put("side_right_vertical_angle_shoulder_elbow_wrist", safePut(sideRightShoulderElbowWristAngle))
                        put("side_right_vertical_angle_hip_knee_ankle", safePut(sideRightHipKneeAnkleAngle))
                    }

                    Log.v("우측 데이터", "코: ${mvm.noseData}, 어깨: ${mvm.shoulderData[0]}, 팔꿉: ${mvm.elbowData[0]}, 손목: ${mvm.wristData[0]}, 골반: ${mvm.hipData[0]}, 무릎: ${mvm.kneeData[0]}, 발목: ${mvm.ankleData[0]}")
                    Log.v("우측각도들", "어깨팔꿉: $sideRightShoulderElbowLean, 팔꿉손목: $sideRightElbowWristLean, 골반무릎: $sideRightHipKneeLean, 귀어깨: $sideRightEarShoulderLean ")

                    saveJson(mvm.staticjo, step)
                }
                5 -> { // ------! 후면 서서 !-------
                    val backEarAngle : Float = calculateSlope(mvm.earData[0].first, mvm.earData[0].second, mvm.earData[1].first, mvm.earData[1].second) % 180
                    val backShoulderAngle : Float = calculateSlope(mvm.shoulderData[0].first, mvm.shoulderData[0].second, mvm.shoulderData[1].first, mvm.shoulderData[1].second) % 180
                    val backElbowAngle : Float = calculateSlope(mvm.elbowData[0].first, mvm.elbowData[0].second, mvm.elbowData[1].first, mvm.elbowData[1].second) % 180
                    val backWristAngle : Float = calculateSlope(mvm.wristData[0].first, mvm.wristData[0].second, mvm.wristData[1].first, mvm.wristData[1].second) % 180
                    val backHipAngle : Float = calculateSlope(mvm.hipData[0].first, mvm.hipData[0].second, mvm.hipData[1].first, mvm.hipData[1].second) % 180
                    val backKneeAngle : Float = calculateSlope(mvm.kneeData[0].first, mvm.kneeData[0].second, mvm.kneeData[1].first, mvm.kneeData[1].second) % 180
                    val backAnkleAngle : Float = calculateSlope(mvm.ankleData[0].first, mvm.ankleData[0].second, mvm.ankleData[1].first, mvm.ankleData[1].second) % 180

                    // ------! 후면 거리 !------
                    val backEarSubDistance : Float = getRealDistanceY(mvm.earData[0], mvm.earData[1])
                    val backShoulderSubDistance  : Float = getRealDistanceY(mvm.shoulderData[0], mvm.shoulderData[1])
                    val backElbowSubDistance  : Float = getRealDistanceY(mvm.elbowData[0], mvm.elbowData[1])
                    val backWristSubDistance  : Float = getRealDistanceY(mvm.wristData[0], mvm.wristData[1])
                    val backHipSubDistance  : Float = getRealDistanceY(mvm.hipData[0], mvm.hipData[1])
                    val backKneeSubDistance  : Float = getRealDistanceY(mvm.kneeData[0], mvm.kneeData[1])
                    val backAnkleSubDistance  : Float = getRealDistanceY(mvm.ankleData[0], mvm.ankleData[1])
                    val backKneeDistanceByX : Pair<Float,Float> = Pair(getRealDistanceX(mvm.kneeData[0], ankleAxis), getRealDistanceX(mvm.kneeData[1], ankleAxis))
                    val backHeelDistanceByX : Pair<Float, Float> = Pair(getRealDistanceX(mvm.heelData[0], ankleAxis), getRealDistanceX(mvm.heelData[1], ankleAxis))

                    val backNoseShoulderLean : Float = calculateAngleBySlope(mvm.shoulderData[0].first, mvm.shoulderData[0].second, mvm.shoulderData[1].first, mvm.shoulderData[1].second, mvm.noseData.first, mvm.noseData.second) % 180
                    val backShoulderHipLean : Float = calculateAngleBySlope(mvm.shoulderData[0].first, mvm.shoulderData[0].second, mvm.shoulderData[1].first , mvm.shoulderData[1].second, middleHip.first, middleHip.second) % 180
                    val backNoseHipLean : Float = calculateSlope(mvm.noseData.first, mvm.noseData.second, mvm.hipData[0].first, abs(mvm.hipData[0].second - mvm.hipData[1].second)) % 180
                    val backKneeHeelLean : Pair<Float, Float> = Pair(calculateSlope(mvm.heelData[0].first, mvm.heelData[0].second, mvm.kneeData[0].first, mvm.kneeData[0].second) % 180 ,
                        calculateSlope(mvm.heelData[1].first, mvm.heelData[1].second, mvm.kneeData[1].first, mvm.kneeData[1].second) % 180 )

                    val backWristDistanceByX : Pair<Float, Float> = Pair(getRealDistanceX(mvm.wristData[0], ankleAxis),
                        getRealDistanceX(mvm.wristData[1], ankleAxis))

                    mvm.staticjo.apply {
                        put("back_horizontal_angle_ear", safePut(backEarAngle))
                        put("back_horizontal_angle_shoulder", safePut(backShoulderAngle))
                        put("back_horizontal_angle_wrist", safePut(backWristAngle))
                        put("back_horizontal_angle_elbow", safePut(backElbowAngle))
                        put("back_horizontal_angle_hip", safePut(backHipAngle))
                        put("back_horizontal_angle_knee", safePut(backKneeAngle))
                        put("back_horizontal_angle_ankle", safePut(backAnkleAngle))

                        put("back_horizontal_distance_sub_ear", backEarSubDistance)
                        put("back_horizontal_distance_sub_shoulder", backShoulderSubDistance)
                        put("back_horizontal_distance_sub_elbow", backElbowSubDistance)
                        put("back_horizontal_distance_sub_wrist", backWristSubDistance)
                        put("back_horizontal_distance_sub_hip", backHipSubDistance)
                        put("back_horizontal_distance_sub_knee", backKneeSubDistance)
                        put("back_horizontal_distance_sub_ankle", backAnkleSubDistance)

                        put("back_horizontal_distance_knee_left", backKneeDistanceByX.first)
                        put("back_horizontal_distance_knee_right", backKneeDistanceByX.second)
                        put("back_horizontal_distance_heel_left", backHeelDistanceByX.first)
                        put("back_horizontal_distance_heel_right", backHeelDistanceByX.second)

                        put("back_vertical_angle_nose_center_shoulder", safePut(backNoseShoulderLean))
                        put("back_vertical_angle_shoudler_center_hip", safePut(backShoulderHipLean))
                        put("back_vertical_angle_nose_center_hip", safePut(backNoseHipLean))
                        put("back_vertical_angle_knee_heel_left", safePut(backKneeHeelLean.first))
                        put("back_vertical_angle_knee_heel_right", safePut(backKneeHeelLean.second))
                        put("back_horizontal_distance_wrist_left", backWristDistanceByX.first)
                        put("back_horizontal_distance_wrist_right", backWristDistanceByX.second)
                    }
                    Log.v("후면데이터", "ear: ${mvm.earData}, shoulder: ${mvm.shoulderData}, elbow: ${mvm.elbowData}, wrist: ${mvm.wristData}, hip: ${mvm.hipData}, knee: ${mvm.kneeData}, ankle: ${mvm.ankleData}")
                    Log.v("후면각도들", "어깨: $backShoulderAngle, 팔꿉: $backElbowAngle, 손목: $backWristAngle, 골반: $backHipAngle, 무릎: $backKneeAngle, 발목: $backAnkleAngle 팔꿉손목: $backShoulderHipLean, 코골반: $backNoseHipLean, 무릎뒷꿈치: $backKneeHeelLean,")

                    saveJson(mvm.staticjo, step)
                }
                6 -> { // ------! 앉았을 때 !------

                    // ------! 의자 후면 거리, 양쪽 부위 높이 차이 - y값 차이 (절댓값)!------
                    val sitBackEarDistance : Float = getRealDistanceY(mvm.earData[0], mvm.earData[1])
                    val sitBackShoulderDistance  : Float = getRealDistanceY(mvm.shoulderData[0], mvm.shoulderData[1])
                    val sitBackHipDistance  : Float = getRealDistanceY(mvm.hipData[0], mvm.hipData[1])

                    val sitBackEarAngle = calculateSlope(mvm.earData[0].first, mvm.earData[0].second, mvm.earData[1].first, mvm.earData[1].second) % 180
                    val sitBackShoulderAngle = calculateSlope(mvm.shoulderData[0].first, mvm.shoulderData[0].second, mvm.shoulderData[1].first, mvm.shoulderData[1].second) % 180
                    val sitBackHipAngle = calculateSlope(mvm.hipData[0].first, mvm.hipData[0].second, mvm.hipData[1].first, mvm.hipData[1].second) % 180
                    // -------# 삼각형 계산을 위한 scale 적용 #------
                    mvm.apply {
                        mvm.noseData = Pair(calculateScreenX(mvm.noseData.first), calculateScreenY(mvm.noseData.second))
                        mvm.shoulderData = listOf(
                            Pair(calculateScreenX(mvm.shoulderData[0].first), calculateScreenY(mvm.shoulderData[0].second)),
                            Pair(calculateScreenX(mvm.shoulderData[1].first), calculateScreenY(mvm.shoulderData[1].second))
                        )
                        middleHip = Pair(calculateScreenX((mvm.hipData[0].first + mvm.hipData[1].first) / 2), calculateScreenY((mvm.hipData[0].second + mvm.hipData[1].second) / 2))
                    }

                    val shoulderNoseTriangleAngle : Triple<Float, Float, Float> = Triple(
                        calculateAngle(mvm.noseData.first, mvm.noseData.second, mvm.shoulderData[0].first, mvm.shoulderData[0].second, mvm.shoulderData[1].first , mvm.shoulderData[1].second),
                        calculateAngle(mvm.shoulderData[0].first, mvm.shoulderData[0].second,  mvm.shoulderData[1].first , mvm.shoulderData[1].second, mvm.noseData.first, mvm.noseData.second),
                        calculateAngle(mvm.shoulderData[1].first , mvm.shoulderData[1].second, mvm.noseData.first, mvm.noseData.second, mvm.shoulderData[0].first, mvm.shoulderData[0].second ))

                    val shoulderHipTriangleAngle : Triple<Float, Float, Float> = Triple(
                        calculateAngle(mvm.shoulderData[0].first, mvm.shoulderData[0].second, middleHip.first ,middleHip.second, mvm.shoulderData[1].first , mvm.shoulderData[1].second),
                        calculateAngle(middleHip.first ,middleHip.second, mvm.shoulderData[1].first , mvm.shoulderData[1].second, mvm.shoulderData[0].first, mvm.shoulderData[0].second),
                        calculateAngle(mvm.shoulderData[1].first , mvm.shoulderData[1].second, mvm.shoulderData[0].first,mvm.shoulderData[0].second, middleHip.first ,middleHip.second ))

                    val shoulderHipRadian = calculateAngleBySlope(mvm.shoulderData[0].first, mvm.shoulderData[0].second, mvm.shoulderData[1].first , mvm.shoulderData[1].second, middleHip.first, middleHip.second)
                    mvm.staticjo.apply {
                        put("back_sit_horizontal_distance_sub_ear", sitBackEarDistance)
                        put("back_sit_horizontal_distance_sub_shoulder", sitBackShoulderDistance)
                        put("back_sit_horizontal_distance_sub_hip", sitBackHipDistance)

                        put("back_sit_horizontal_angle_ear", safePut(sitBackEarAngle))
                        put("back_sit_horizontal_angle_shoulder", safePut(sitBackShoulderAngle))
                        put("back_sit_horizontal_angle_hip", safePut(sitBackHipAngle))

                        put("back_sit_vertical_angle_nose_left_shoulder_right_shoulder", shoulderNoseTriangleAngle.first)
                        put("back_sit_vertical_angle_left_shoulder_right_shoulder_nose", shoulderNoseTriangleAngle.second)
                        put("back_sit_vertical_angle_right_shoulder_nose_left_shoulder", shoulderNoseTriangleAngle.third)

                        put("back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder", shoulderHipTriangleAngle.first)
                        put("back_sit_vertical_angle_center_hip_right_shoulder_left_shoulder", shoulderHipTriangleAngle.second)
                        put("back_sit_vertical_angle_right_shoulder_left_shoulder_center_hip", shoulderHipTriangleAngle.third)
                        put("back_sit_vertical_angle_shoulder_center_hip", shoulderHipRadian)
                    }
                    Log.v("앉아후면데이터", "ear: ${mvm.earData}, shoulder: ${mvm.shoulderData}, elbow: ${mvm.elbowData}, wrist: ${mvm.wristData}, hip: ${mvm.hipData}, knee: ${mvm.kneeData}, ankle: ${mvm.ankleData}")
                    Log.v("앉아후면각도들", "귀각도: $sitBackEarAngle, 어깨: $sitBackShoulderAngle, 골반: $sitBackHipAngle, 어깨코삼각형: $shoulderNoseTriangleAngle, 골반어깨 삼각형: $shoulderHipTriangleAngle")
                    saveJson(mvm.staticjo, step)
                }
            }
            poseLandmarks = JSONArray()
        }
    }

    // 공통 인적사항ㅅ
    private fun saveJson(jsonObj: JSONObject, step: Int) {
        jsonObj.apply {
            put("measure_seq", step + 1)
            put("measure_type", when (step) {
                0 -> 1
                2 -> 6
                3 -> 2
                4 -> 3
                5 -> 4
                6 -> 5
                else -> -1
            })
            put("device_sn", 0)
            put("mobile_device_uuid", decryptedUUID)
            put("user_uuid", singletonUser.jsonObject?.getString("user_uuid"))
            put("user_name", mvm.setupName) // singletonUser.jsonObject?.getString("user_name") ?: ""
            put("user_sn", singletonUser.jsonObject?.getInt("user_sn") ?: -1)
            put("measure_start_time", timestamp)
            put("measure_end_time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            put("measure_overlay_width", binding.overlay.width)
            put("measure_overlay_height", binding.overlay.height)
            put("measure_overlay_scale_factor_x", scaleFactorX)
            put("measure_overlay_scale_factor_y", scaleFactorY)
            put("measure_photo_file_name", getMediaName(step))
            put("measure_server_json_name", "${getFileName(step)}.json")
            put("measure_server_file_name", "${getFileName(step)}.jpg")
            put("pose_landmark", poseLandmarks)
        }
//        Log.v("${step}_JsonStatic변환 scaleFactor", "${jsonObj.getString("measure_overlay_scale_factor_x")}, ${jsonObj.getString("measure_overlay_scale_factor_y")}")

        val measureStaticUnit = jsonObj.toMeasureStatic()
        mvm.statics.add(measureStaticUnit)
        Log.v("뷰모델스태틱_$step", "$measureStaticUnit")

        val addPoseLandmarkJo = JSONObject(measureStaticUnit.toJson()).apply {
            put("pose_landmark", poseLandmarks)
        }
        saveJo(this@MeasureSkeletonActivity, "${getFileName(step)}.json", addPoseLandmarkJo, mvm)

        mvm.staticjo = JSONObject()
    }

    private fun getMediaName(step: Int): String {
        return when (step) {
            0 -> "MT_STATIC_FRONT_1_3_$timestamp"
            2 -> "MT_STATIC_ELBOW_ALIGN_3_3_$timestamp"
            3 -> "MT_STATIC_SIDE_LEFT_4_3_$timestamp"
            4 -> "MT_STATIC_SIDE_RIGHT_5_3_$timestamp"
            5 -> "MT_STATIC_BACK_6_3_$timestamp"
            6 -> "MT_STATIC_BACK_SIT_7_3_$timestamp"
            else -> "UNKNOWN_$timestamp"
        }
    }

    private fun getFileName(step: Int) : String {
        return when (step) {
            0 -> "0-${measureInfoSn}-1-1-$timestamp"
            2 -> "0-${measureInfoSn}-3-6-$timestamp"
            3 -> "0-${measureInfoSn}-4-2-$timestamp"
            4 -> "0-${measureInfoSn}-5-3-$timestamp"
            5 -> "0-${measureInfoSn}-6-4-$timestamp"
            6 -> "0-${measureInfoSn}-7-5-$timestamp"
            else -> "UNKNOWN_$timestamp"
        }
    }
    private fun captureImage(step: Int) {
        val imageCapture = imageCapture

        val name = getFileName(step)
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TangoQ")
            }
        }
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback{
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri
                    if (savedUri != null) {
                        saveMediaToCache(this@MeasureSkeletonActivity, savedUri, name, true)
                    } else {
                        Log.e("SavedUri", "Saved URI is null")
                    }
                }
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                }
            }
        )
    }

    private fun startVideoRecording(callback: () -> Unit){
        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            val videoCapture = this@MeasureSkeletonActivity.videoCapture
            val curRecording = recording
            if (curRecording != null) {
                curRecording.stop()
                recording = null
                return@launch
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, videoFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/TangoQ")
                }
            }

            val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
                contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            ).setContentValues(contentValues)
                .build()

            // Start recording video with audio
            recording = videoCapture.output.prepareRecording(this@MeasureSkeletonActivity, mediaStoreOutputOptions)
//                .apply {
//                    if (PermissionChecker.checkSelfPermission(
//                            this@MeasureSkeletonActivity, Manifest.permission.RECORD_AUDIO
//                        ) == PermissionChecker.PERMISSION_GRANTED) {
//                        withAudioEnabled()
//                    }
//                }
                .start(ContextCompat.getMainExecutor(this@MeasureSkeletonActivity)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        CoroutineScope(Dispatchers.Main).launch {
                            startRecording = true
                            startRecordingTimer()
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            Log.v("녹화종료시점", "isRecording: $isRecording, isCapture: $isCapture")
                            isRecording = false
                            startRecording = false
                            val savedUri = recordEvent.outputResults.outputUri
                            val inputPath = getPathFromUri(this@MeasureSkeletonActivity, savedUri) // URI를 파일 경로로 변환
                            val outputPath = File(this@MeasureSkeletonActivity.cacheDir, "mirrored_video.mp4").absolutePath

                            if (inputPath != null) {
                                flipVideoHorizontally(inputPath, outputPath) { success ->
                                    if (success) {
                                        saveMediaToCache(this@MeasureSkeletonActivity, Uri.fromFile(File(outputPath)), videoFileName, false)
                                        callback()
                                    } else {
                                        Log.e(TAG, "Failed to apply mirror effect to video.")
                                    }
                                }
                            }
                        } else {
                            CoroutineScope(Dispatchers.Main).launch  {
                                isRecording = false
                                startRecording = false
                                recording?.close()
                                recording = null
                                Log.e(TAG, "Video capture ends with error: ${recordEvent.error}")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startRecordingTimer() {
        CoroutineScope(Dispatchers.Main).launch {
            delay(5000) // 5 seconds delay
            stopVideoRecording()
        }
    }

    private fun stopVideoRecording() {
        recordingJob?.cancel() // Cancel the coroutine if needed
        val curRecording = recording
        if (curRecording != null) {
            curRecording.stop()
            recording = null
        }
    }

    fun saveMediaToCache(context: Context, uri: Uri, fileName: String, isImage: Boolean) {
        try {
            val extension = if (isImage) ".jpg" else ".mp4"
            val file = File(context.cacheDir, "$fileName$extension")

            if (isImage) {
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = File.createTempFile("tempImage", null, context.cacheDir)
                inputStream?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // EXIF 데이터 읽기
                val exif = ExifInterface(tempFile.absolutePath)
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                Log.d("ExifDebug", "Exif Orientation: $orientation")
                // 비트맵 디코딩
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(tempFile.absolutePath, options)

                val sourceWidth = options.outWidth
                val sourceHeight = options.outHeight

                val targetWidth = 720
                val targetHeight = 1280

                // 이미지 스케일 계산
                val widthRatio = sourceWidth.toFloat() / targetWidth
                val heightRatio = sourceHeight.toFloat() / targetHeight
                val scale = maxOf(widthRatio, heightRatio)

                options.inJustDecodeBounds = false
                options.inSampleSize = scale.toInt()

                var bitmap = BitmapFactory.decodeFile(tempFile.absolutePath, options)
                val matrix = Matrix()

                // 회전 적용
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)   // 시계 방향 90도
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f) // 시계 방향 180도
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f) // 시계 방향 270도
                }

                // ★★★ 좌우반전 ★★★
                matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)

                // 모든 변환을 한번에 적용
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                // 스케일 조정
                bitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)

                // 리사이즈된 이미지를 캐시에 저장
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.flush()
                outputStream.close()
                // 임시 파일과 비트맵 정리
                tempFile.delete()
                bitmap.recycle()
                mvm.staticFiles.add(file)
            }  else {
                // 비디오일 경우 그대로 캐시에 저장
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                        mvm.dynamicFile = file
                    }
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            Log.e("SaveMediaIndex", "${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.e("SaveMediaIllegal", "${e.message}")
        } catch (e: IllegalStateException) {
            Log.e("SaveMediaIllegal", "${e.message}")
        } catch (e: NullPointerException) {
            Log.e("SaveMediaNull", "${e.message}")
        } catch (e: java.lang.Exception) {
            Log.e("SaveMediaException", "${e.message}")
        }
    }

    // ------! 애니메이션 !------
    // duration 서서히 떠오르는 시간 / delay 딜레이시간
    private fun setAnimation(tv: View, duration : Long, delay: Long, fade: Boolean, callback: () -> Unit) {
        val animator = ObjectAnimator.ofFloat(tv, "alpha", if (fade) 0f else 1f, if (fade) 1f else 0f)
        animator.duration = duration
        animator.addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                tv.visibility = if (fade) View.VISIBLE else View.INVISIBLE
                callback()
            }
        })
        Handler(Looper.getMainLooper()).postDelayed({
            animator.start()
        }, delay)
    }

    private fun startCameraShutterAnimation() {
        // 첫 번째 애니메이션: VISIBLE로 만들고 alpha를 0에서 1로
        Handler(Looper.getMainLooper()).postDelayed({
            binding.flMeasureSkeleton.visibility = View.VISIBLE
            val fadeIn = ObjectAnimator.ofFloat(binding.flMeasureSkeleton, "alpha", 0f, 1f).apply {
                duration = 50 // 0.1초
                interpolator = AccelerateDecelerateInterpolator()
            }

            // 두 번째 애니메이션: alpha를 1에서 0으로 만들고, 끝난 후 INVISIBLE로 설정
            val fadeOut = ObjectAnimator.ofFloat(binding.flMeasureSkeleton, "alpha", 1f, 0f).apply {
                duration = 50 // 0.1초
                interpolator = AccelerateDecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        binding.flMeasureSkeleton.visibility = View.INVISIBLE
                    }
                })
            }

            fadeIn.start()
            fadeIn.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    fadeOut.start()
                }
            })
        }, 150)
    }

    private fun View.setOnSingleClickListener(action: (v: View) -> Unit) {
        val listener = View.OnClickListener { action(it) }
        setOnClickListener(OnSingleClickListener(listener))
    }

    private fun calculateScreenX(xx: Float): Float {
        val scaleFactor = binding.overlay.width * 1f / 720
        val offsetX = ((binding.overlay.width - 720 * scaleFactor) / 2 )
        val x = (1 - xx) * binding.overlay.width / scaleFactor + offsetX // ★★ 반전이 된 이미지라 screen계산에도 역치가 필요함
        return x
    }

    private fun calculateScreenY(yy: Float): Float {
        val scaleFactor = binding.overlay.height * 1f / 1280
        val offsetY = (binding.overlay.height - 1280 * scaleFactor) / 2
        val y = yy * binding.overlay.height / scaleFactor + offsetY
        return y
    }

    suspend fun findLowestYFrame(coordinates: List<Pair<Float, Float>>): Int = withContext(Dispatchers.Default) {
        val yValues = coordinates.map { it.second }
        val highestYIndex = yValues.withIndex().maxByOrNull { it.value }?.index ?: 0
        maxOf(0, highestYIndex - 3)
    }

    // Activity 내부에 추가할 함수
    private suspend fun resendMeasureFileWithRetry(
        context: Context,
        apiUrl: String,
        requestBody: RequestBody,
        isStatic: Boolean,
        uploadSn: Int,
        mobileSn: Int,
        maxRetries: Int = 5,
        delayMillis: Long = 1000
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        var attempts = 0
        var lastException: Exception? = null

        while (attempts < maxRetries) {
            try {
                return@withContext suspendCancellableCoroutine { continuation ->
                    CoroutineScope(Dispatchers.IO).launch {
                        resendMeasureFile(
                            context,
                            apiUrl,
                            requestBody,
                            isStatic,
                            uploadSn,
                            mobileSn
                        ) { result ->
                            result?.let { (jsonSuccess, fileSuccess) ->
                                // 둘 다 성공한 경우에만 성공으로 처리
                                if (jsonSuccess == "1" && fileSuccess == "1") {
                                    continuation.resume(Result.success(Pair(jsonSuccess, fileSuccess)))
                                } else {
                                    attempts++
                                    if (attempts >= maxRetries) {
                                        continuation.resume(Result.failure(Exception("Failed after $maxRetries attempts. Last result: json=$jsonSuccess, file=$fileSuccess")))
                                    } else {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            delay(delayMillis)
                                            Log.d("RetryUpload", "Retrying... Attempt $attempts of $maxRetries. Last result: json=$jsonSuccess, file=$fileSuccess")
                                            // 재시도를 위해 실패로 처리
                                            continuation.resume(Result.failure(Exception("Retrying... Attempt $attempts of $maxRetries")))
                                        }
                                    }
                                }
                            } ?: run {
                                // result가 null인 경우
                                attempts++
                                if (attempts >= maxRetries) {
                                    continuation.resume(Result.failure(Exception("Failed after $maxRetries attempts. Response was null")))
                                } else {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        delay(delayMillis)
                                        Log.d("RetryUpload", "Retrying... Attempt $attempts of $maxRetries. Response was null")
                                        continuation.resume(Result.failure(Exception("Retrying... Attempt $attempts of $maxRetries")))
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                lastException = e
                delay(delayMillis)
                attempts++
                Log.e("SaveMediaIndex", "${e.message}")
            } catch (e: IllegalArgumentException) {
                lastException = e
                delay(delayMillis)
                attempts++
                Log.e("SaveMediaIllegal", "${e.message}")
            } catch (e: IllegalStateException) {
                lastException = e
                delay(delayMillis)
                attempts++
                Log.e("SaveMediaIllegal", "${e.message}")
            }catch (e: NullPointerException) {
                lastException = e
                delay(delayMillis)
                attempts++

                Log.e("SaveMediaNull", "${e.message}")
            } catch (e: java.lang.Exception) {
                lastException = e
                delay(delayMillis)
                attempts++
                Log.e("SaveMediaException", "${e.message}")
            }
        }
        Result.failure(lastException ?: Exception("Failed after $maxRetries attempts"))
    }

    private suspend fun finishMeasure(mobileInfoSn: Int, mobileStaticSns: MutableList<Int>, mobileDynamicSn: Int) {
        // ------# 측정 완료 && 업로드 후 싱글턴 저장 #------
        try {
            val ssm = SaveSingletonManager(this@MeasureSkeletonActivity, this@MeasureSkeletonActivity)
            withContext(Dispatchers.IO) {
                ssm.addMeasurementInSingleton(mobileInfoSn, mobileStaticSns, mobileDynamicSn)
            }

            val intent = Intent()
            intent.putExtra("finishedMeasure", true)
            setResult(Activity.RESULT_OK, intent)
            finish()
        } catch (e: IllegalStateException) {
            Log.e("MSError", "${e.message}")
            Toast.makeText(this@MeasureSkeletonActivity, "데이터 처리 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
            val intent = Intent().apply { putExtra("finishedMeasure", false) }
            setResult(Activity.RESULT_CANCELED, intent)
            finish()
        }  catch (e: IndexOutOfBoundsException) {
            Log.e("MSError", "${e.message}")
            Toast.makeText(this@MeasureSkeletonActivity, "데이터 처리 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
            val intent = Intent().apply { putExtra("finishedMeasure", false) }
            setResult(Activity.RESULT_CANCELED, intent)
            finish()
        }  catch (e: NullPointerException) {
            Log.e("MSError", "${e.message}")
            Toast.makeText(this@MeasureSkeletonActivity, "데이터 처리 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
            val intent = Intent().apply { putExtra("finishedMeasure", false) }
            setResult(Activity.RESULT_CANCELED, intent)
            finish()
        } catch (e: SocketTimeoutException) {
            Log.e("MSError", "${e.message}")
            Toast.makeText(this@MeasureSkeletonActivity, "데이터 처리 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
            val intent = Intent().apply { putExtra("finishedMeasure", false) }
            setResult(Activity.RESULT_CANCELED, intent)
            finish()
        } catch (e: Exception) {
            Log.e("MeasureSkeletonActivity", "Error processing measure data", e)
            // 에러 발생 시 사용자에게 알림
            Toast.makeText(this@MeasureSkeletonActivity, "데이터 처리 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
            val intent = Intent().apply { putExtra("finishedMeasure", false) }
            setResult(Activity.RESULT_CANCELED, intent)
            finish()
        }
    }
    private fun JSONObject?.toMeasureStatic(): MeasureStatic {
        return Gson().fromJson(this.toString(), MeasureStatic::class.java)
    }
    private fun MeasureStatic?.toJson(): String {
        return Gson().toJson(this)
    }
    private fun MeasureDynamic?.toJson(): String {
        return Gson().toJson(this)
    }
    private fun MeasureInfo?.toJson(): String {
        return Gson().toJson(this)
    }
    private fun safePut(value: Float) : Float {
        return if (value.isNaN()) 0f else value
    }

    private fun flipVideoHorizontally(inputPath: String, outputPath: String, callback: (Boolean) -> Unit) {
        val command = "-i $inputPath -vf hflip -y -c:v libx264 -crf 18 -preset veryfast -c:a copy $outputPath"
        lifecycleScope.launch(Dispatchers.Main) {
            val dialog = LoadingDialogFragment.newInstance("동영상").apply {
                show(supportFragmentManager, "LoadingDialogFragment")
            }

            withContext(Dispatchers.IO) {
                // FFmpeg 명령 실행
                FFmpegKit.executeAsync(command) { session ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        val returnCode = session.returnCode
                        if (!isFinishing && !isDestroyed) {
                            dialog.dismiss()
                        }
                        if (ReturnCode.isSuccess(returnCode)) {
                            Log.d("FFMPEGAlert", "Video successfully mirrored and saved to: $outputPath")
                            callback(true)
                        } else {
                            Log.e("FFMPEGAlert", "Error occurred while mirroring video: ${session.getLogsAsString()}")
                            callback(false)
                        }
                    }
                }
            }
        }

    }
}