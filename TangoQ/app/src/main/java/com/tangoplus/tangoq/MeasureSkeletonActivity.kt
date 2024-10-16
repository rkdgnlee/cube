package com.tangoplus.tangoq

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
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
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.View
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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.MutableLiveData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.shuhart.stepview.StepView
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.data.SkeletonViewModel
import com.tangoplus.tangoq.databinding.ActivityMeasureSkeletonBinding
import com.tangoplus.tangoq.db.FileStorageUtil.saveJa
import com.tangoplus.tangoq.db.FileStorageUtil.saveJo
import com.tangoplus.tangoq.db.MeasureDao
import com.tangoplus.tangoq.db.MeasureDatabase
import com.tangoplus.tangoq.db.MeasureDynamic
import com.tangoplus.tangoq.db.MeasureInfo
import com.tangoplus.tangoq.db.MeasureStatic
import com.tangoplus.tangoq.dialog.MeasureSkeletonDialogFragment
import com.tangoplus.tangoq.listener.OnSingleClickListener
import com.tangoplus.tangoq.mediapipe.OverlayView
import com.tangoplus.tangoq.mediapipe.PoseLandmarkAdapter
import com.tangoplus.tangoq.mediapipe.PoseLandmarkerHelper
import com.tangoplus.tangoq.`object`.NetworkRecommendation.createRecommendProgram
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import com.tangoplus.tangoq.`object`.Singleton_t_user
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)


class MeasureSkeletonActivity : AppCompatActivity(), PoseLandmarkerHelper.LandmarkerListener, SensorEventListener {
// ------! POSE LANDMARKER 설정 시작 !------
    companion object {
        private const val TAG = "Pose Landmarker"
        private const val REQUEST_CODE_PERMISSIONS = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        fun hasPermissions(context: Context) = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                context,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    private var _binding: ActivityMeasureSkeletonBinding? = null
    private val binding get() = _binding!!
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

    // ------! 싱글턴 패턴 객체 가져오기 !------
    var poseLandmarks = JSONArray()
    private lateinit var singletonUser : Singleton_t_user
    private lateinit var singletonMeasure : Singleton_t_measure
    private lateinit var md : MeasureDatabase
    private lateinit var mDao : MeasureDao
    private var measureInfoSn = 0

    var latestResult: PoseLandmarkerHelper.ResultBundle? = null
    private val mViewModel : MeasureViewModel by viewModels()

    private var repeatCount = MutableLiveData(0)
    private val maxRepeats = 6
    private var progress = 12
    private var timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
    private val startTime = LocalDateTime.now()
    private var endTime : LocalDateTime? = null
    // ------# 수직 감지기 #------
    private lateinit var hideIndicatorHandler: Handler
    private lateinit var hideIndicatorRunnable: Runnable
    private var accelerometer: Sensor? = null
    private lateinit var sensorManager: SensorManager
    private var currentBias = 0f
    private var filteredAngle = 0f
    private val ALPHA = 0.1f
    private val INTERPOLATION_FACTOR = 0.1f
    private var hideIndicator = false

    // ------! 카운트 다운  시작 !-------
    private  val mCountDown : CountDownTimer by lazy {
        object : CountDownTimer(1000, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                runOnUiThread{
                    binding.tvMeasureSkeletonCount.visibility = View.VISIBLE
                    binding.tvMeasureSkeletonCount.alpha = 1f
                    binding.tvMeasureSkeletonCount.text = "${(millisUntilFinished / 1000.0f).roundToInt()}"
                    Log.v("count", "${binding.tvMeasureSkeletonCount.text}")
                }
            }
            @SuppressLint("SetTextI18n")
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onFinish() {
                if (latestResult != null) {
                    // ------# resultBundleToJson이 동작하는 시간으로 통일 #------
                    timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))

                    if (isRecording) { // 동영상 촬영
                        binding.tvMeasureSkeletonCount.text = "스쿼트를 실시해주세요"

                        setAnimation(binding.tvMeasureSkeletonCount, 1000, 500, false) {
                            hideDynamicViews(5700)
                            setDrawableSequence()
                            // 녹화 종료 시점 아님
                            startVideoRecording {
                                Log.v("녹화종료시점", "isRecording: $isRecording, isCapture: $isCapture")
                                isRecording = false // 녹화 완료
                                startRecording = false
                                updateUI()
                                binding.ivMeasureSkeletonFrame.visibility = View.VISIBLE
                                binding.tvMeasureSkeletonCount.text = "다음 동작을 준비해주세요"

                                // ------# 약 200프레임에서 db에 넣을 값을 찾는 곳 #------
                                CoroutineScope(Dispatchers.IO).launch {
                                    saveJa(this@MeasureSkeletonActivity, "${videoFileName}.json", mViewModel.dynamicJa, mViewModel)
                                    val noseDynamic = mViewModel.extractVideoCoordinates(mViewModel.dynamicJa).map { it[0] } // x, y좌표를 가져와서, nose(0)만 추출함.
                                    Log.v("noseDynamic", "${noseDynamic}")
                                    val decreasingFrameIndex = findLowestYFrame(noseDynamic) // 전체 코의 y궤적에서 감소되는 구간의 index 추출
                                    val saveDynamic = mViewModel.dynamicJa.optJSONObject(decreasingFrameIndex)
                                    Log.v("인덱스와변형전dynamicJson추출", "decreasingFrameIndex: ${decreasingFrameIndex}, saveDynamic: ${saveDynamic}")
                                    //
                                    saveDynamic?.let { jsonObject ->
                                        jsonObject.remove("pose_landmark")
                                        mViewModel.dynamic = mViewModel.convertToMeasureDynamic(jsonObject)
                                        // 정제된 dynamic을 viewModel을 담음.
                                        Log.v("mViewModelDynamic", "${mViewModel.dynamic}")
                                    }

                                }

                            }
                        }
                    } else {
                        binding.tvMeasureSkeletonCount.text = "자세를 따라해주세요"
                        hideViews(600)
                        Log.v("사진service", "isCapture: ${isCapture}, isRecording: $isRecording")
                        // ------! 종료 후 다시 세팅 !------
                        latestResult?.let { resultBundleToJson(it, repeatCount.value!!) }
                        captureImage(repeatCount.value!!)

                        Log.v("캡쳐종료시점", "step: ${repeatCount.value}")
                        updateUI()
                        isCapture = false

                    }
                    binding.btnMeasureSkeletonStep.isEnabled = true
                } else {
                    binding.tvMeasureSkeletonCount.text = "인식 후 다시 시도해주세요"
                    binding.btnMeasureSkeletonStep.isEnabled = true
                }
            }
        }
    } //  ------! 카운트 다운 끝 !-------


    override fun onResume() {
        super.onResume()

        // 모든 권한이 여전히 존재하는지 확인하세요.
        // 앱이 일시 중지된 상태에서 사용자가 해당 항목을 제거했을 수 있습니다.
        // 권한 확인 및 요청
        if (!hasPermissions(this)) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)

        } else {
            // 권한이 이미 부여된 경우 카메라 설정을 진행합니다.
            setUpCamera()
        }
        // ------# 수직 감지기 #------
        if (!hideIndicator) {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }

        // Start the PoseLandmarkerHelper again when users come back
        // to the foreground.
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

            // Close the PoseLandmarkerHelper and release resources
            backgroundExecutor.execute { poseLandmarkerHelper.clearPoseLandmarker() }

            if (hideIndicator) {
                sensorManager.unregisterListener(this)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        // Shut down our background executor
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
        _binding = ActivityMeasureSkeletonBinding.inflate(layoutInflater)
        setContentView(_binding!!.root)
        md = MeasureDatabase.getDatabase(this)
        mDao = md.measureDao()
        singletonUser = Singleton_t_user.getInstance(this@MeasureSkeletonActivity)
        CoroutineScope(Dispatchers.IO).launch {
            measureInfoSn = mDao.getMaxMobileInfoSn(singletonUser.jsonObject?.optInt("user_sn")!!) + 1
            Log.v("이제들어갈measureSn", "$measureInfoSn")
        }

        // ------# sensor 연결 #------
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // -----! pose landmarker 시작 !-----
        // our background executor 초기화
        backgroundExecutor = Executors.newSingleThreadExecutor()
        // Wait for the views to be properly laid out
        binding.viewFinder.post {
            // Set up the camera and its use cases
            setUpCamera()
        }
        // Create the PoseLandmarkerHelper that will handle the inference
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
                requestPermissions(PERMISSIONS_REQUIRED, REQUEST_CODE_PERMISSIONS)
                setUpCamera()
            }
        }
        // ------! 안내 문구 사라짐 시작 !------

        /** 사진 및 동영상 촬영 순서
         * 1. isLooping true -> repeatCount확인 -> count에 맞게 isCapture및 isRecord 선택
         * 2. mCountdown.start() -> 카운트 다운이 종료될 때 isCapture, isRecording에 따라 service 함수 실행 */
        binding.ibtnMeasureSkeletonBack.setOnClickListener {

            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                setTitle("알림")
                setMessage("측정을 종료하시겠습니까 ?")
                setPositiveButton("예") { dialog, _ ->
                    val activityIntent = Intent(this@MeasureSkeletonActivity, MainActivity::class.java)
                    intent.putExtra("showMeasureFragment", true);
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
                    add("전면")
                    add("오버헤드")
                    add("팔꿉")
                    add("왼쪽")
                    add("오른쪽")
                    add("후면")
                    add("앉아 후면")
                }
            })
            .stepsNumber(7)
            .animationDuration(getResources().getInteger(android.R.integer.config_shortAnimTime))
            .commit()

        // ------# 초기 시작 #------
        binding.svMeasureSkeleton.go(0, true)
        binding.pvMeasureSkeleton.progress = 14

        // ------# 버튼 촬영 #------
        binding.btnMeasureSkeletonStep.setOnClickListener {
            if (binding.btnMeasureSkeletonStep.text == "완료하기") {
                endTime = LocalDateTime.now()
                CoroutineScope(Dispatchers.IO).launch {
                    val motherJo = JSONObject()

                    val userJson = singletonUser.jsonObject
                    val userUUID = userJson?.getString("user_uuid")!!

                    val inputFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val date: Date = inputFormat.parse(timestamp)!!
                    val measureDate = outputFormat.format(date)
                    val seconds = Duration.between(startTime, endTime).toMillis() / 1000.0
                    val elapsedTime = "%.3f".format(seconds)
                    // ------# info 넣기 #------
                    val measureInfo = MeasureInfo(
                        user_uuid = userUUID,
                        user_sn = userJson.optString("user_sn").toInt(),
                        user_name = userJson.optString("user_name"),
                        measure_date = measureDate,
                        elapsed_time = elapsedTime,
                        t_score = 90.toString(),
                        measure_seq = 7,
                        risk_neck = "1",
                        risk_elbow_left = "2",
                        risk_hip_left = "1"
                    )
                    Log.v("measure빈칼럼여부", "${measureInfo}")
                    val infoJson = JSONObject(measureInfo.toJson())
                    motherJo.put("measure_info", infoJson)

                    Log.v("뷰모델스태틱", "${mViewModel.statics.size}")

                    for (i in 0 until mViewModel.statics.size) {
                        val staticUnit = mViewModel.statics[i].toJson()
                        val joStaticUnit = JSONObject(staticUnit)
                        Log.v("스태틱변환", "$joStaticUnit")
                        motherJo.put("static_${i+1}", joStaticUnit)
                        Log.v("motherJo2", "${motherJo.getJSONObject("static_${i+1}")}")
                    }
                    val dynamicJo = JSONObject(mViewModel.dynamic!!.toJson())
                    motherJo.put("dynamic", dynamicJo)
                    Log.v("motherJo1", "${motherJo.optJSONObject("measure_info")}")
                    Log.v("motherJo3", "${motherJo.optJSONObject("dynamic")}")
                    // 일단 json 값들.

                    val requestBodyBuilder = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("json", motherJo.toString())

                    // static jpg파일들
                    for (i in mViewModel.staticFiles.indices) {
                        val file = mViewModel.staticFiles[i]
                        Log.v("파일정보", "Static File: 이름=${file.name}, 크기=${file.length()} bytes")
                        requestBodyBuilder.addFormDataPart(
                            "static_file_${i+1}",
                            file.name,
                            file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        )

                    }
                    // static json파일
                    for (i in mViewModel.staticJsonFiles.indices) {
                        val file = mViewModel.staticJsonFiles[i]
                        Log.v("파일정보", "Static JSON: 이름=${file.name}, 크기=${file.length()} bytes")
                        requestBodyBuilder.addFormDataPart(
                            "static_json_${i+1}",
                            file.name,
                            file.asRequestBody("application/json".toMediaTypeOrNull())
                        )
                    }
                    // Dynamic json파일
                    mViewModel.dynamicJsonFile?.let { file ->
                        Log.v("파일정보", "Dynamic JSON: 이름=${file.name}, 크기=${file.length()} bytes")
                        requestBodyBuilder.addFormDataPart(
                            "dynamic_json",
                            file.name,
                            file.asRequestBody("application/json".toMediaTypeOrNull())
                        )
                    }

                    // Dynamic mp4 파일
                    mViewModel.dynamicFile?.let { file ->
                        Log.v("파일정보", "Dynamic MP4: 이름=${file.name}, 크기=${file.length()} bytes")
                        requestBodyBuilder.addFormDataPart(
                            "dynamic_file",
                            file.name,
                            file.asRequestBody("video/mp4".toMediaTypeOrNull())
                        )
                    }

                    val requestBody = requestBodyBuilder.build()
                    val partCount = requestBodyBuilder.build().parts.size
                    Log.v("파트개수", "총 파트 개수: $partCount")
//                    sendMeasureData(this@MeasureSkeletonActivity, getString(R.string.API_results), requestBody)



                    // TODO 측정 후 값을 임시로 보고 싶을 때.
                    // ------# userJSonBody로 보내기 #------
//                    sendMeasureData(this@MeasureSkeletonActivity, getString(R.string.API_measure), requestBody) // getMeasureResult처럼 다 넣게됨.
//
//                    val infoSn = Singleton_t_user.getInstance(this@MeasureSkeletonActivity).jsonObject?.optString("sn")?.toInt()!!
//                    val userSn = Singleton_t_user.getInstance(this@MeasureSkeletonActivity).jsonObject?.optString("user_sn")?.toInt()!!
//                    getMeasureResult(this@MeasureSkeletonActivity, getString(R.string.API_measure), infoSn, userSn , userUUID)
//                    SaveSingletonManager(this@MeasureSkeletonActivity, this@MeasureSkeletonActivity).fetchAndFilterMeasureInfo(userSn)
//
//                    SaveSingletonManager(this@MeasureSkeletonActivity, this@MeasureSkeletonActivity).addRecommendations(infoSn)
//                    withContext(Dispatchers.Main) {
//                        val intent = Intent()
//                        intent.putExtra("finishedMeasure", true)
//                        setResult(Activity.RESULT_OK, intent)
//                        finish()
//                    }
//                    val userJson = singletonUser.jsonObject
//                    val userUUID = userJson?.getString("user_uuid")!!
//
//                    val inputFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
//                    val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
//                    val date: Date = inputFormat.parse(timestamp)!!
//                    val measureDate = outputFormat.format(date)
//
//                    // ------# info 넣기 #------
//                    val measureInfo = MeasureInfo(
//                        user_uuid = userUUID,
//                        user_sn = userJson.optString("user_sn").toInt(),
//                        user_name = userJson.optString("user_name"),
//                        measure_date = measureDate,
//                        elapsed_time = "",
//                        t_score = 90.toString(),
//                        measure_seq = 7,
//                        risk_neck = "1",
//                        risk_elbow_left = "2",
//                        risk_hip_left = "1"
//                    )
                    mDao.insertInfo(measureInfo)
                    Log.v("measureInfo넣기", "방금넣은info: ${measureInfo}, allInfo: ${mDao.getAllInfo(userJson.optString("user_sn").toInt())}")

                    // ------# static 넣기 #------
                    for (i in 0 until mViewModel.statics.size) {
                        mDao.insertWithAutoIncrementStatic(mViewModel.statics[i], userUUID, measureInfoSn)
                    }

                    // ------# dynamic 넣기 #------
                    Log.v("다이나믹널찾기", "dynamic: ${mViewModel.dynamic}, userUUID: ${userUUID}, dynamicJa: ${mViewModel.dynamicJa.length()}, measureSn: ${measureInfoSn}")
                    mDao.insertWithAutoIncrementDynamic(mViewModel.dynamic!!, userUUID, mViewModel.dynamicJa.length(), measureInfoSn)

                    val convertedMeasureVO = mViewModel.convertJsonToMeasureVO(measureInfo, mViewModel.statics, mViewModel.dynamic!!)
                    Log.v("변환된measureVO", "${convertedMeasureVO}")
                    // 1. static 넣기 2. dynamic 넣기 3. 파일 매칭 되게 수정
                    withContext(Dispatchers.Main) {
                        val ja1 = JSONArray()
                        ja1.apply {
                            put(2)
                            put(10)
                            put(6)
                        }
                        val ja2 = JSONArray()
                        ja2.apply {
                            put(1)
                            put(2)
                            put(1)
                        }
                        val recJo = JSONObject().apply {
                            put("user_sn", userJson.getString("user_sn").toInt())
                            put("exercise_type_id", ja1)
                            put("exercise_stage", ja2)
                            put("measure_sn", measureInfoSn)
                        }

                        createRecommendProgram(getString(R.string.API_recommendation), recJo.toString(), this@MeasureSkeletonActivity) { recommendation ->
                            singletonMeasure = Singleton_t_measure.getInstance(this@MeasureSkeletonActivity)
                            convertedMeasureVO.recommendations = recommendation
                            singletonMeasure.measures?.add(0, convertedMeasureVO)
                            Log.v("측정싱글턴크기", "${singletonMeasure.measures?.size}")
                            val intent = Intent()
                            intent.putExtra("finishedMeasure", true)
                            setResult(Activity.RESULT_OK, intent)
                            finish()
                        }

                    }
                    val intent = Intent()
                    intent.putExtra("finishedMeasure", true)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            } else {
                startTimer()
            }
        }

        // ------# 주의사항 키기 #------
        val dialog = MeasureSkeletonDialogFragment()
        dialog.show(supportFragmentManager, "MeasureSkeletonDialogFragment")

        binding.ibtnMeasureSkeletonInfo.setOnClickListener {
            dialog.show(supportFragmentManager, "MeasureSkeletonDialogFraagment")
        }

        // ------! 다시 찍기 관리 시작 !------
        repeatCount.observe(this@MeasureSkeletonActivity) { count ->
            binding.btnMeasureSkeletonStepPrevious.visibility = if (count.compareTo(0) == 0) {
                View.GONE
            } else {
                View.VISIBLE
            }
            Log.v("visible", "repeatCount: ${repeatCount.value}")
        }

        binding.btnMeasureSkeletonStepPrevious.setOnSingleClickListener {
            setPreviousStep()
        }
        // ------! 다시 찍기 관리 끝 !------

        // ------! 수직 감도 사라지기 보이기 시작 !------
        hideIndicatorHandler = Handler(Looper.getMainLooper())
        hideIndicatorRunnable = Runnable {
            binding.clMeasureSkeletonAngle.animate().alpha(0f).setDuration(300).start()
            hideIndicator = true
        }
        hideIndicatorHandler.postDelayed(hideIndicatorRunnable, 30000)

        binding.clMeasureSkeletonAngle.setOnClickListener {
            if (!hideIndicator) {

                binding.clMeasureSkeletonAngle.animate().alpha(0f).setDuration(300).start()
                hideIndicator = true

                hideIndicatorHandler.removeCallbacks(hideIndicatorRunnable)
            } else {
                binding.clMeasureSkeletonAngle.animate().alpha(1f).setDuration(300).start()
                hideIndicator = false
                // 다시 타이머 시작
                hideIndicatorHandler.postDelayed(hideIndicatorRunnable, 5000)
            }
        }
        // ------! 수직 감도 사라지기 보이기 끝 !------
    }


    // ------! 센서 시작 !------
    private fun lowPassFilter(input: Float): Float {
        filteredAngle += ALPHA * (input - filteredAngle)
        return filteredAngle
    }

    private fun interpolate(target: Float): Float {
        return currentBias + (target - currentBias) * INTERPOLATION_FACTOR
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val z = event.values[2]
            val clampedZ = z.coerceIn(-SensorManager.GRAVITY_EARTH, SensorManager.GRAVITY_EARTH)
            val angle = Math.toDegrees(Math.asin(((clampedZ / SensorManager.GRAVITY_EARTH).toDouble())))
            filteredAngle = lowPassFilter(angle.toFloat())
            // 각도를 0~180 범위로 정규화
            val normalizedAngle = (filteredAngle + 90).coerceIn(0f, 180f)

            // verticalBias 계산 (0~1 범위)
            val targetBias = 1 - (normalizedAngle / 180f)
            currentBias = interpolate(targetBias)

            // CardView의 verticalBias 설정
            (binding.cvMeasureSkeletonIndicator.layoutParams as ConstraintLayout.LayoutParams).verticalBias = currentBias
            binding.cvMeasureSkeletonIndicator.requestLayout()

            if (normalizedAngle in 88f..92f) {
                binding.cvMeasureSkeletonIndicator.setCardBackgroundColor(ContextCompat.getColor(this, R.color.mainColor))
            } else {
                binding.cvMeasureSkeletonIndicator.setCardBackgroundColor(ContextCompat.getColor(this, R.color.subColor100))
            }
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    // ------! 센서 끝 !------


    // ------! update UI 시작 !------
    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        when (repeatCount.value!!) {
            maxRepeats -> {
                binding.pvMeasureSkeleton.progress = 100
                binding.tvMeasureSkeletonCount.text = "측정이 완료됐습니다 !"
                binding.btnMeasureSkeletonStep.text = "완료하기"
                mCountDown.cancel()
                Log.v("repeat", "Max repeats reached, stopping the loop")
            }
            else -> {
                if (repeatCount.value!! == 1) {
                    binding.tvMeasureSkeletonCount.text = "스쿼트를 실시해주세요"
                } else {
                    binding.tvMeasureSkeletonCount.visibility = View.VISIBLE
                    binding.tvMeasureSkeletonCount.text = "다음 동작을 준비해주세요"
                }
                repeatCount.value = repeatCount.value!!.plus(1)
                progress += 14
                binding.pvMeasureSkeleton.progress = progress
                Log.v("몇단계?", "repeatCount: ${repeatCount.value}, progress: $progress")
                binding.svMeasureSkeleton.go(repeatCount.value!!.toInt(), true)
                binding.tvMeasureSkeletonCount.visibility = View.VISIBLE
                val drawable = ContextCompat.getDrawable(this, resources.getIdentifier("drawable_measure_${repeatCount.value!!.toInt()}", "drawable", packageName))
                binding.ivMeasureSkeletonFrame.setImageDrawable(drawable)

            }

        }
        Log.v("updateUI", "progressbar: ${progress}, repeatCount: ${repeatCount.value}")
    } // ------! update UI 끝 !------


    fun setDrawableSequence(index: Int = 0) {
        val ids = listOf("1", "1_1", "1_2", "1_1", "1")
        if (index >= ids.size) {
            Log.v("DynamicDrawable", "Finished setDrawable in Frame.")
            binding.ivMeasureSkeletonFrame.visibility = View.INVISIBLE
            return
        }

        val drawableId = resources.getIdentifier("drawable_measure_${ids[index]}", "drawable", packageName)
        val drawable = ContextCompat.getDrawable(this@MeasureSkeletonActivity, drawableId)
        binding.ivMeasureSkeletonFrame.setImageDrawable(drawable)

        val delay = if (ids[index] == "1_2") 1400L else 1000L

        Handler(Looper.getMainLooper()).postDelayed({
            setDrawableSequence(index + 1)
        }, delay)
    }

    @SuppressLint("SetTextI18n")
    private fun setPreviousStep() {
        when (repeatCount.value) {
            maxRepeats -> {
                repeatCount.value = repeatCount.value?.minus(1)
                binding.pvMeasureSkeleton.progress -= 16  // 마지막 남은 2까지 전부 빼기
                binding.tvMeasureSkeletonCount.text = "프레임에 맞춰 서주세요"
                binding.btnMeasureSkeletonStep.text = "측정하기"
                binding.svMeasureSkeleton.go(repeatCount.value!!.toInt(), true)

                // 1 3 4 5 6 7
                }
            else -> {
                repeatCount.value = repeatCount.value?.minus(1)
                progress -= 14
                binding.pvMeasureSkeleton.progress = progress
                Log.v("녹화종료되나요?", "repeatCount: ${repeatCount.value}, progress: $progress")
                binding.svMeasureSkeleton.go(repeatCount.value!!.toInt(), true)
                val drawable = ContextCompat.getDrawable(this, resources.getIdentifier("drawable_measure_${repeatCount.value!!.toInt()}", "drawable", packageName))
                binding.ivMeasureSkeletonFrame.setImageDrawable(drawable)
                mViewModel.statics.removeAt(repeatCount.value!!)
            }

        }
        Log.v("updateUI", "progressbar: ${progress}, repeatCount: ${repeatCount.value}")
    }

    // ------! 촬영 시 view 가리고 보이기 !-----
    private fun hideViews(delay : Long) {
        binding.clMeasureSkeletonTop.visibility = View.INVISIBLE
        binding.ivMeasureSkeletonFrame.visibility = View.INVISIBLE
        binding.llMeasureSkeletonBottom.visibility = View.INVISIBLE
        if (repeatCount.value != 1) startCameraShutterAnimation()

        setAnimation(binding.clMeasureSkeletonTop, 850, delay, true) {}
        setAnimation(binding.ivMeasureSkeletonFrame, 850, delay, true) {}
        setAnimation(binding.llMeasureSkeletonBottom, 850, delay, true) {}
        setAnimation(binding.tvMeasureSkeletonCount, 850, delay ,true) {}
        binding.btnMeasureSkeletonStep.visibility = View.VISIBLE
        binding.tvMeasureSkeletonCount.text = "프레임에 맞춰 서주세요"
    }

    private fun hideDynamicViews(delay: Long) {
        binding.clMeasureSkeletonTop.visibility = View.INVISIBLE
        binding.llMeasureSkeletonBottom.visibility = View.INVISIBLE
        setAnimation(binding.clMeasureSkeletonTop, 850, delay, true) {}
        setAnimation(binding.llMeasureSkeletonBottom, 850, delay, true) {}
        setAnimation(binding.tvMeasureSkeletonCount, 850, delay ,true) {}
        binding.btnMeasureSkeletonStep.visibility = View.VISIBLE
        binding.tvMeasureSkeletonCount.text = "프레임에 맞춰 서주세요"
    }


    // ------! 타이머 control 시작 !------
    private fun startTimer() {
        // 시작 버튼 후 시작
        binding.btnMeasureSkeletonStep.isEnabled = false

        when (repeatCount.value) {
            1 -> {
                isCapture = false
                isRecording = true
            }
            else -> {
                isCapture = true
                isRecording = false
            }
        }
        Log.v("repeatCount", "${repeatCount.value}")
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

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        // 미리보기. 4:3 비율만 사용합니다. 이것이 우리 모델에 가장 가깝기 때문입니다.
        preview = Preview.Builder()
//            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setTargetResolution(Size(720, 1280))
            .setTargetRotation(binding.viewFinder.display.rotation)
            .build()

        // 이미지 분석. RGBA 8888을 사용하여 모델 작동 방식 일치
        imageAnalyzer =
            ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setTargetRotation(binding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(backgroundExecutor) { image ->
                        detectPose(image)
                    }
                }
        // 이미지 캡처 설정
        imageCapture = ImageCapture.Builder()
//            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setTargetResolution(Size(720, 1280))
            .setTargetRotation(android.view.Surface.ROTATION_0)
            .build()

        videoCapture = VideoCapture.withOutput(
            Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()
        )

        // ------! 카메라에 poselandmarker 그리기 !------
        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // 여기에는 다양한 사용 사례가 전달될 수 있습니다.
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer, imageCapture, videoCapture
            )
            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectPose(imageProxy: ImageProxy) {
        if (this::poseLandmarkerHelper.isInitialized) {
            poseLandmarkerHelper.detectLiveStream(
                imageProxy = imageProxy,
                isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
            )
            if (startRecording && isRecording && latestResult != null) {
                // 여기서 넣는데 영상이 종료되고,
                resultBundleToJson(latestResult!!, repeatCount.value!!)
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
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (PackageManager.PERMISSION_GRANTED == grantResults.firstOrNull()) {
                setUpCamera()
                Log.v("스켈레톤 Init", "동작 성공")
            } else {
                Log.v("스켈레톤 Init", "동작 실패")
            }
        }
    }
    override fun onError(error: String, errorCode: Int) {
        runOnUiThread {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        runOnUiThread {
            if (_binding != null) {
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
                if (scaleFactorX != null && scaleFactorY != null) {
                    scaleFactorX = binding.overlay.width * 1f / latestResult!!.inputImageWidth
                    scaleFactorY = binding.overlay.height * 1f / latestResult!!.inputImageHeight
                }
            }
        }
    }
    // ------# 기울기 계산 #------
    fun calculateSlope(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return (y2 - y1) / (x2 - x1)
    }
    // ------# 점 3개의 각도 계산 #------
    private fun calculateAngle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3:Float): Float {
        val v1x = x1 - x2
        val v1y = y1 - y2
        val v2x = x3 - x2
        val v2y = y3 - y2
        val dotProduct = v1x * v2x + v1y * v2y
        val magnitude1 = sqrt(v1x * v1x + v1y * v1y)
        val magnitude2 = sqrt(v2x * v2x + v2y * v2y)

        val cosTheta = dotProduct / (magnitude1 * magnitude2)
        val angleRadians = acos(cosTheta)
        return Math.toDegrees(angleRadians.toDouble()).toFloat()
    }
    // ------# 점과 점사이의 거리 #------
    private fun calculateDistanceByDots(x1: Float, y1: Float, x2: Float, y2: Float) : Float{
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }

    // ------! 선과 점의 X 거리 !------
    private fun calculateAngleByLine(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) : Float {
        val vector1X = x2 - x1
        val vector1Y = y2 - y1
        val vector2X = x3 - x1
        val vector2Y = y3 - y1
        val dotProduct = vector1X * vector2X + vector1Y * vector2Y
        val magnitude1 = sqrt(vector1X.pow(2) + vector1Y.pow(2))
        val magnitude2 = sqrt(vector2X.pow(2) + vector2Y.pow(2))

        val cosTheta = dotProduct / (magnitude1 * magnitude2)
        val angleRad = acos(cosTheta.coerceIn(-1.0F, 1.0F))

        return angleRad * (180f / PI.toFloat())
    }

    // override 로 resultbundle이 계속 나오는데 해당 항목을 전역변수 latest
    fun resultBundleToJson(resultBundle: PoseLandmarkerHelper.ResultBundle, step: Int) {
        val earData = mutableListOf<Pair<Float, Float>>() // index 0 왼 index 1 오른
        val shoulderData = mutableListOf<Pair<Float, Float>>()
        val elbowData = mutableListOf<Pair<Float, Float>>()
        val wristData = mutableListOf<Pair<Float, Float>>()
        val indexData = mutableListOf<Pair<Float, Float>>()
        val pinkyData = mutableListOf<Pair<Float, Float>>()

        val thumbData = mutableListOf<Pair<Float, Float>>()
        val hipData = mutableListOf<Pair<Float, Float>>()
        val kneeData = mutableListOf<Pair<Float, Float>>()
        val ankleData = mutableListOf<Pair<Float, Float>>()
        val heelData = mutableListOf<Pair<Float, Float>>()
        val toeData = mutableListOf<Pair<Float, Float>>()
        if (resultBundle.results.first().landmarks().isNotEmpty()) {

            val plr = resultBundle.results.first().landmarks()[0]!!

            plr.forEachIndexed { index, poseLandmark ->
                val jo = JSONObject().apply {
                    put("index", index)
                    put("isActive", true)
                    if (step == 1) {
                        put("sx", calculateScreenX(poseLandmark.x(), false ))
                        put("sy", calculateScreenY(poseLandmark.y(), false ))
                    } else {
                        put("sx", calculateScreenX(poseLandmark.x(), true ))
                        put("sy", calculateScreenY(poseLandmark.y(), true ))
                    }
                    put("wx", poseLandmark.x())
                    put("wy", poseLandmark.y())
                    put("wz", poseLandmark.z())
                }
                poseLandmarks.put(jo)
            }
            for (i in 7 until  plr.size) {
                when (i) {
                    in 7 .. 8 -> earData.add(Pair(plr[i].x(), plr[i].y()))
                    in 11 .. 12 -> shoulderData.add(Pair(plr[i].x(), plr[i].y()))
                    in 13 .. 14 -> elbowData.add(Pair(plr[i].x(), plr[i].y()))
                    in 15 .. 16 -> wristData.add(Pair(plr[i].x(), plr[i].y()))
                    in 17 .. 18 -> pinkyData.add(Pair(plr[i].x(), plr[i].y()))
                    in 19 .. 20 -> indexData.add(Pair(plr[i].x(), plr[i].y()))
                    in 21 .. 22 -> thumbData.add(Pair(plr[i].x(), plr[i].y()))
                    in 23 .. 24 -> hipData.add(Pair(plr[i].x(), plr[i].y()))
                    in 25 .. 26 -> kneeData.add(Pair(plr[i].x(), plr[i].y()))
                    in 27 .. 28 -> ankleData.add(Pair(plr[i].x(), plr[i].y()))
                    in 29 .. 30 -> heelData.add(Pair(plr[i].x(), plr[i].y()))
                    in 31 .. 32 -> toeData.add(Pair(plr[i].x(), plr[i].y()))
                }
            }


            val nose : Pair<Float, Float> = Pair(plr[0].x(), plr[0].y())
            val ankleXAxis = ankleData[0].first.minus(ankleData[1].first)  / 2
            val middleHip = Pair((hipData[0].first + hipData[1].first) / 2, (hipData[0].second + hipData[1].second) / 2)
            val middleShoulder = Pair((shoulderData[0].first + shoulderData[1].first) / 2, (shoulderData[0].second + shoulderData[1].second) / 2)
            /** mutablelist 0 왼쪽 1 오른쪽
            *  , 그리고 first: x    second: y
            * */



            when (step) {
                0 -> {
                    val earAngle : Float = calculateSlope(earData[0].first, earData[0].second, earData[1].first, earData[1].second)
                    val shoulderAngle : Float = calculateSlope(shoulderData[0].first, shoulderData[0].second, shoulderData[1].first, shoulderData[1].second)
                    val elbowAngle : Float = calculateSlope(elbowData[0].first, elbowData[0].second, elbowData[1].first, elbowData[1].second)
                    val wristAngle : Float = calculateSlope(wristData[0].first, wristData[0].second, wristData[1].first, wristData[1].second)
                    val hipAngle : Float = calculateSlope(hipData[0].first, hipData[0].second, hipData[1].first, hipData[1].second)
                    val kneeAngle : Float = calculateSlope(kneeData[0].first, kneeData[0].second, kneeData[1].first, kneeData[1].second)
                    val ankleAngle : Float = calculateSlope(ankleData[0].first, ankleData[0].second, ankleData[1].first, ankleData[1].second)
                    // 부위 양 높이 차이
                    val earDistance : Float = abs(earData[0].second.minus(earData[1].second))
                    val shoulderDistance : Float = abs(shoulderData[0].second.minus(shoulderData[1].second))
                    val elbowDistance : Float = abs(elbowData[0].second.minus(elbowData[1].second))
                    val wristDistance : Float = abs(wristData[0].second.minus(wristData[1].second))
                    val hipDistance : Float = abs(hipData[0].second.minus(hipData[1].second))
                    val kneeDistance : Float = abs(kneeData[0].second.minus(kneeData[1].second))
                    val ankleDistance : Float = abs(ankleData[0].second.minus(ankleData[1].second))

                    // 각 부위 x축 부터 거리 거리
                    val wristSubDistanceByX : Pair<Float, Float> = Pair(abs(wristData[0].first.minus(ankleXAxis)), abs(wristData[1].first.minus(ankleXAxis)))
                    val kneeSubDistanceByX : Pair<Float, Float> = Pair(abs(kneeData[0].first.minus(ankleXAxis)), abs(kneeData[1].first.minus(ankleXAxis)))
                    val ankleSubDistanceByX : Pair<Float, Float> = Pair(abs(ankleData[0].first.minus(ankleXAxis)), abs(ankleData[1].first.minus(ankleXAxis)))
                    val toeSubDistance : Pair<Float, Float> = Pair(abs(toeData[0].first.minus(ankleXAxis)), abs(toeData[1].first.minus(ankleXAxis)))

                    // 팔 각도
                    val shoulderElbowLean: Pair<Float, Float> = Pair(calculateSlope(shoulderData[0].first, shoulderData[0].second, elbowData[0].first, elbowData[0].second), calculateSlope(shoulderData[1].first, shoulderData[1].second, elbowData[1].first, elbowData[1].second))
                    val elbowWristLean: Pair<Float, Float> = Pair(calculateSlope(elbowData[0].first, elbowData[0].second, wristData[0].first, wristData[0].second), calculateSlope(elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second))
                    val wristThumbLean: Pair<Float, Float> = Pair(calculateSlope(wristData[0].first, wristData[0].second, thumbData[0].first, thumbData[0].second), calculateSlope(wristData[1].first, wristData[1].second, thumbData[1].first, thumbData[1].second))
                    val hipKneeLean : Pair<Float, Float> = Pair(calculateSlope(hipData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second), calculateSlope(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second))
                    val kneeAnkleLean : Pair<Float, Float> = Pair(calculateSlope(kneeData[0].first, kneeData[0].second, ankleData[0].first, ankleData[0].second), calculateSlope(kneeData[1].first, kneeData[1].second, ankleData[1].first, ankleData[1].second))
                    val ankleToeLean : Pair<Float, Float> = Pair(calculateSlope(ankleData[0].first, ankleData[0].second, toeData[0].first, toeData[0].second), calculateSlope(ankleData[1].first, ankleData[1].second, toeData[1].first, toeData[1].second))

                    val shoulderElbowWristAngle : Pair<Float, Float> = Pair(calculateAngle(shoulderData[0].first, shoulderData[0].second, elbowData[0].first, elbowData[0].second, wristData[0].first, wristData[0].second),
                        calculateAngle(shoulderData[1].first, shoulderData[1].second, elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second))
                    val hipKneeAnkleAngle : Pair<Float, Float> = Pair(calculateAngle(hipData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second, ankleData[0].first, ankleData[0].second),
                        calculateAngle(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second, ankleData[1].first, ankleData[1].second))


                    mViewModel.staticjo.apply {
                        put("front_horizontal_angle_ear", (earAngle % 180) )
                        put("front_horizontal_angle_shoulder", (shoulderAngle % 180) )
                        put("front_horizontal_angle_elbow", (elbowAngle % 180) )
                        put("front_horizontal_angle_wrist", (wristAngle % 180) )
                        put("front_horizontal_angle_hip", (hipAngle % 180 ))
                        put("front_horizontal_angle_knee", (kneeAngle % 180) )
                        put("front_horizontal_angle_ankle", (ankleAngle % 180) )

                        put("front_horizontal_distance_sub_ear", earDistance)
                        put("front_horizontal_distance_sub_shoulder", shoulderDistance)
                        put("front_horizontal_distance_sub_elbow", elbowDistance)
                        put("front_horizontal_distance_sub_wrist", wristDistance)
                        put("front_horizontal_distance_sub_hip", hipDistance)
                        put("front_horizontal_distance_sub_knee", kneeDistance)
                        put("front_horizontal_distance_sub_ankle", ankleDistance)

                        put("front_horizontal_distance_wrist_left", wristSubDistanceByX.first)
                        put("front_horizontal_distance_wrist_right", wristSubDistanceByX.second)
                        put("front_horizontal_distance_knee_left", kneeSubDistanceByX.first)
                        put("front_horizontal_distance_knee_right", kneeSubDistanceByX.second)
                        put("front_horizontal_distance_ankle_left", ankleSubDistanceByX.first)
                        put("front_horizontal_distance_ankle_right", ankleSubDistanceByX.second)
                        put("front_horizontal_distance_toe_left", toeSubDistance.first)
                        put("front_horizontal_distance_toe_right", toeSubDistance.second)

                        put("front_vertical_angle_shoulder_elbow_left", (shoulderElbowLean.first % 180))
                        put("front_vertical_angle_shoulder_elbow_right", (shoulderElbowLean.second % 180))
                        put("front_vertical_angle_elbow_wrist_left", (elbowWristLean.first % 180))
                        put("front_vertical_angle_elbow_wrist_right", (elbowWristLean.second % 180))
                        put("front_vertical_angle_wrist_thumb_left",( wristThumbLean.first % 180))
                        put("front_vertical_angle_wrist_thumb_right",( wristThumbLean.second % 180))
                        put("front_vertical_angle_hip_knee_left",( hipKneeLean.first % 180))
                        put("front_vertical_angle_hip_knee_right",( hipKneeLean.second % 180))
                        put("front_vertical_angle_knee_ankle_left", (kneeAnkleLean.first % 180))
                        put("front_vertical_angle_knee_ankle_right",( kneeAnkleLean.second % 180))
                        put("front_vertical_angle_ankle_toe_left", (ankleToeLean.first % 180))
                        put("front_vertical_angle_ankle_toe_right",( ankleToeLean.second % 180))

                        put("front_vertical_angle_shoulder_elbow_wrist_left", shoulderElbowWristAngle.first)
                        put("front_vertical_angle_shoulder_elbow_wrist_right", shoulderElbowWristAngle.second)
                        put("front_vertical_angle_hip_knee_ankle_left", hipKneeAnkleAngle.first)
                        put("front_vertical_angle_hip_knee_ankle_right", hipKneeAnkleAngle.second)
                    }
                    saveJson(mViewModel.staticjo, step)
                }
                1 -> {  // 스쿼트
                    // 현재 프레임 당 계속 넣을 공간임
                    // ------# 각도 타임으로 넣기 #------
                    val wristAngle : Float = calculateSlope(wristData[0].first, wristData[0].second, wristData[1].first, wristData[1].second)
                    val wristDistance : Float = abs(wristData[0].second - wristData[1].second)
                    val wristDistanceByCenter : Pair<Float, Float> = Pair(abs(wristData[0].first.minus(ankleXAxis)), abs(wristData[1].first.minus(ankleXAxis)))
                    val elbowAngle : Float = calculateSlope(elbowData[0].first, elbowData[0].second, elbowData[1].first, elbowData[1].second)
                    val elbowDistance : Float = abs(elbowData[0].second - elbowData[1].second)
                    val shoulderAngle : Float = calculateSlope(shoulderData[0].first, shoulderData[0].second, shoulderData[1].first, shoulderData[1].second)
                    val shoulderDistance : Float = abs(shoulderData[0].second - shoulderData[1].second)
                    val hipAngle : Float = calculateSlope(hipData[0].first, hipData[0].second, hipData[1].first, hipData[1].second)
                    val hipDistance : Float = abs(hipData[0].second - hipData[1].second)
                    val hipDistanceByCenter : Pair<Float, Float> = Pair(abs(hipData[0].first.minus(ankleXAxis)), abs(hipData[1].first.minus(ankleXAxis)))
                    val kneeAngle : Float = calculateSlope(kneeData[0].first, kneeData[0].second, kneeData[1].first, kneeData[1].second)
                    val kneeDistance : Float = abs(kneeData[0].second - kneeData[1].second)
                    val kneeDistanceByCenter : Pair<Float, Float> = Pair(abs(kneeData[0].first.minus(ankleXAxis)), abs(kneeData[1].first.minus(ankleXAxis)))
                    val toeDistanceByCenter : Pair<Float, Float> = Pair(abs(toeData[0].first.minus(ankleXAxis)), abs(toeData[1].first.minus(ankleXAxis)))
                    val wristElbowShoulderAngle : Pair<Float, Float> = Pair(calculateAngle(wristData[0].first, wristData[0].second, elbowData[0].first, elbowData[0].second, shoulderData[0].first, shoulderData[0].second),
                        calculateAngle(wristData[1].first, wristData[1].second, elbowData[1].first, elbowData[1].second, shoulderData[1].first, shoulderData[1].second))
                    val wristElbowAngle : Pair<Float, Float> = Pair(calculateSlope(wristData[0].first, wristData[0].second, elbowData[0].first, elbowData[0].second),
                        calculateSlope(wristData[1].first, wristData[1].second, elbowData[1].first, elbowData[1].second))
                    val elbowShoulderAngle : Pair<Float, Float> = Pair(calculateSlope(elbowData[0].first, elbowData[0].second, shoulderData[0].first, shoulderData[0].second),
                        calculateSlope(elbowData[1].first, elbowData[1].second, shoulderData[1].first, shoulderData[1].second))
                    val hipKneeToeAngle : Pair<Float, Float> = Pair(calculateAngle(hipData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second, toeData[0].first, toeData[0].second),
                        calculateAngle(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second, toeData[1].first, toeData[1].second))
                    val hipKneeAngle : Pair<Float, Float> = Pair(calculateSlope(hipData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second),
                        calculateSlope(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second))
                    val kneeToeAngle : Pair<Float, Float> = Pair(calculateSlope(kneeData[0].first, kneeData[0].second, toeData[0].first, toeData[0].second),
                        calculateSlope(kneeData[1].first, kneeData[1].second, toeData[1].first, toeData[1].second))
                    val ankleToeAngle : Pair<Float, Float> = Pair(calculateSlope(ankleData[0].first, ankleData[0].second, toeData[0].first, toeData[0].second),
                        calculateSlope(ankleData[1].first, ankleData[1].second, toeData[1].first, toeData[1].second))
                    val kneeAnkleToeAngle : Pair<Float, Float> = Pair(calculateAngle(kneeData[0].first, kneeData[0].second, ankleData[0].first, ankleData[0].second, toeData[0].first, toeData[0].second),
                        calculateAngle(kneeData[1].first, kneeData[1].second, ankleData[1].first, ankleData[1].second, toeData[1].first, toeData[1].second))
                    val indexDistanceByCenter : Pair<Float, Float> = Pair(abs(indexData[0].first.minus(ankleXAxis)), abs(indexData[1].first.minus(ankleXAxis)))
                    val indexAngle : Float = calculateSlope(indexData[0].first, indexData[0].second, indexData[1].first, indexData[1].second)
                    val indexDistance : Float = abs(indexData[0].second - indexData[1].second)

                    if (!isNameInit) {
                        videoFileName = "0_${measureInfoSn}_2_7_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}"
                        measureVideoName = "MT_DYNAMIC_OVERHEADSQUAT_FRONT_0_0_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}"
                        isNameInit = true
                    }

                    // 1프레임당 dynamic 측정 모두 들어감
                    mViewModel.dynamicJoUnit.apply {
                        put("device_sn", 0)
                        put("user_uuid", singletonUser.jsonObject?.getString("user_uuid"))
                        put("user_name", singletonUser.jsonObject?.getString("user_name")!! )
                        put("user_sn", singletonUser.jsonObject?.getInt("user_sn")!!)
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
                        put("front_vertical_angle_wrist_elbow_shoulder_left", wristElbowShoulderAngle.first)
                        put("front_vertical_angle_wrist_elbow_shoulder_right", wristElbowShoulderAngle.second)
                        put("front_vertical_angle_wrist_elbow_left", wristElbowAngle.first)
                        put("front_vertical_angle_wrist_elbow_right", wristElbowAngle.second)
                        put("front_vertical_angle_elbow_shoulder_left", elbowShoulderAngle.first)
                        put("front_vertical_angle_elbow_shoulder_right", elbowShoulderAngle.second)
                        put("front_vertical_angle_hip_knee_toe_left", hipKneeToeAngle.first)
                        put("front_vertical_angle_hip_knee_toe_right", hipKneeToeAngle.second)
                        put("front_vertical_angle_hip_knee_left", hipKneeAngle.first)
                        put("front_vertical_angle_hip_knee_toe_right", hipKneeAngle.second)
                        put("front_vertical_angle_hip_knee_left", kneeToeAngle.first)
                        put("front_vertical_angle_hip_knee_right", kneeToeAngle.second)
                        put("front_vertical_angle_ankle_toe_left", ankleToeAngle.first)
                        put("front_vertical_angle_ankle_toe_right", ankleToeAngle.second)
                        put("front_vertical_angle_knee_ankle_toe_left", kneeAnkleToeAngle.first)
                        put("front_vertical_angle_knee_ankle_toe_right", kneeAnkleToeAngle.second)
                        put("front_horizontal_distance_center_mid_finger_tip_left", indexDistanceByCenter.first)
                        put("front_horizontal_distance_center_mid_finger_tip_right", indexDistanceByCenter.second)
                        put("front_horizontal_angle_mid_finger_tip", indexAngle)
                        put("front_horizontal_distance_mid_finger_tip", indexDistance)
                        put("pose_landmark", poseLandmarks)
                    }

                    // 1프레임당 dynamic 측정 모두 들어감
                    Log.v("dynamicJa에들어감", "${mViewModel.dynamicJa.length()}")
                    mViewModel.dynamicJa.put(mViewModel.dynamicJoUnit)
                    mViewModel.dynamicJoUnit = JSONObject()
                }
                2 -> { // ------! 주먹 쥐고 !------

                    val wristShoulderDistance : Pair<Float, Float> = Pair(calculateDistanceByDots(wristData[0].first, wristData[0].second, shoulderData[0].first, shoulderData[0].second),
                        calculateDistanceByDots(wristData[1].first, wristData[1].second, shoulderData[1].first, shoulderData[1].second))
                    val wristsDistanceByY : Float = abs(wristData[0].second - wristData[1].second)
                    val indexWristElbowAngle : Pair<Float, Float> = Pair(calculateAngle(indexData[0].first, indexData[0].second, wristData[0].first, wristData[0].second, elbowData[0].first, elbowData[0].second),
                        calculateAngle(indexData[1].first, indexData[1].second, wristData[1].first, wristData[1].second, elbowData[1].first, elbowData[1].second))
                    val shoulderElbowWristAngle : Pair<Float, Float> = Pair(calculateAngle(shoulderData[0].first, shoulderData[0].second, elbowData[0].first, elbowData[0].second, wristData[0].first, wristData[0].second),
                        calculateAngle(shoulderData[1].first, shoulderData[1].second, elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second))
                    val indexDistanceByX: Pair<Float, Float> = Pair(abs(indexData[0].first.minus(ankleXAxis)), abs(indexData[1].first.minus(ankleXAxis)))
                    val wristDistanceByX: Pair<Float, Float> = Pair(abs(wristData[0].first.minus(ankleXAxis)), abs(wristData[1].first.minus(ankleXAxis)))

                    mViewModel.staticjo.apply {
                        put("front_elbow_align_distance_left_wrist_shoulder", wristShoulderDistance.first)
                        put("front_elbow_align_distance_right_wrist_shoulder", wristShoulderDistance.second)
                        put("front_elbow_align_distance_wrist_height", wristsDistanceByY)
                        put("front_elbow_align_angle_mid_index_wrist_elbow_left", indexWristElbowAngle.first)
                        put("front_elbow_align_angle_mid_index_wrist_elbow_rightt", indexWristElbowAngle.second)
                        put("front_elbow_align_angle_left_shoulder_elbow_wrist", shoulderElbowWristAngle.first)
                        put("front_elbow_align_angle_right_shoulder_elbow_wrist", shoulderElbowWristAngle.second)
                        put("front_elbow_align_distance_center_mid_finger_left", indexDistanceByX.first)
                        put("front_elbow_align_distance_center_mid_finger_right", indexDistanceByX.second)
                        put("front_elbow_align_distance_center_wrist_left", wristDistanceByX.first)
                        put("front_elbow_align_distance_center_wrist_right", wristDistanceByX.second)
                    }
                    saveJson(mViewModel.staticjo, step)
                }
                3 -> { // 왼쪽보기 (오른쪽 팔)

                    // ------! 측면 거리  - 왼쪽 !------
                    val sideLeftShoulderDistance : Float = abs(shoulderData[0].first.minus(ankleXAxis))
                    val sideLeftWristDistance : Float = abs(wristData[0].first.minus(ankleXAxis))
                    val sideLeftPinkyDistance : Float = abs(pinkyData[0].first.minus(ankleXAxis))
                    val sideLeftHipDistance : Float = abs(hipData[0].first.minus(ankleXAxis))

                    val sideLeftShoulderElbowLean : Float = calculateSlope(shoulderData[0].first, shoulderData[0].second, kneeData[0].first, kneeData[0].second)
                    val sideLeftElbowWristLean: Float =calculateSlope(elbowData[0].first, elbowData[0].second, wristData[0].first, wristData[0].second)
                    val sideLeftHipKneeLean : Float = calculateSlope(hipData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second)
                    val sideLeftEarShoulderLean : Float = calculateSlope(earData[0].first, earData[0].second, shoulderData[0].first, shoulderData[0].second)
                    val sideLeftNoseShoulderLean : Float = calculateSlope(nose.first, nose.second, shoulderData[0].first, shoulderData[0].second)
                    val sideLeftShoulderElbowWristAngle : Float = calculateAngle(shoulderData[0].first, shoulderData[0].second, elbowData[0].first, elbowData[0].second, wristData[0].first, wristData[0].second)
                    val sideLeftHipKneeAnkleAngle : Float = calculateAngle(hipData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second, ankleData[0].first, ankleData[0].second)

                    mViewModel.staticjo.apply {
                        put("side_left_horizontal_distance_shoulder", sideLeftShoulderDistance)
                        put("side_left_horizontal_distance_hip", sideLeftWristDistance)
                        put("side_left_horizontal_distance_pinky", sideLeftPinkyDistance)
                        put("side_left_horizontal_distance_wrist", sideLeftHipDistance)

                        put("side_left_vertical_angle_shoulder_elbow", sideLeftShoulderElbowLean  % 180)
                        put("side_left_vertical_angle_elbow_wrist", sideLeftElbowWristLean  % 180)
                        put("side_left_vertical_angle_hip_knee", sideLeftHipKneeLean  % 180)
                        put("side_left_vertical_angle_ear_shoulder", sideLeftEarShoulderLean  % 180)
                        put("side_left_vertical_angle_nose_shoulder", sideLeftNoseShoulderLean  % 180)
                        put("side_left_vertical_angle_shoulder_elbow_wrist", sideLeftShoulderElbowWristAngle  % 180)
                        put("side_left_vertical_angle_hip_knee_ankle", sideLeftHipKneeAnkleAngle  % 180)
                    }
                    saveJson(mViewModel.staticjo, step)
                }
                4 -> { // 오른쪽보기 (왼쪽 팔)
                    // ------! 측면 거리  - 오른쪽 !------

                    val sideRightShoulderDistance : Float = abs(shoulderData[1].first.minus(ankleXAxis))
                    val sideRightWristDistance : Float = abs(wristData[1].first.minus(ankleXAxis))
                    val sideRightPinkyDistance : Float = abs(pinkyData[1].first.minus(ankleXAxis))
                    val sideRightHipDistance : Float = abs(hipData[1].first.minus(ankleXAxis))
                                        // ------! 측면 기울기  - 오른쪽 !------
                    val sideRightShoulderElbowLean : Float = calculateSlope(shoulderData[1].first, shoulderData[1].second, kneeData[1].first, kneeData[1].second)
                    val sideRightElbowWristLean: Float =calculateSlope(elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second)
                    val sideRightHipKneeLean : Float = calculateSlope(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second)
                    val sideRightEarShoulderLean : Float = calculateSlope(earData[1].first, earData[1].second, shoulderData[1].first, shoulderData[1].second)
                    val sideRightNoseShoulderLean : Float = calculateSlope(nose.first, nose.second, shoulderData[1].first, shoulderData[1].second)
                    val sideRightShoulderElbowWristAngle : Float = calculateAngle(shoulderData[1].first, shoulderData[1].second, elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second)
                    val sideRightHipKneeAnkleAngle : Float = calculateAngle(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second, ankleData[1].first, ankleData[1].second)
                    mViewModel.staticjo.apply {
                        put("side_right_horizontal_distance_shoulder", sideRightShoulderDistance)
                        put("side_right_horizontal_distance_hip", sideRightWristDistance)
                        put("side_right_horizontal_distance_pinky", sideRightPinkyDistance)
                        put("side_right_horizontal_distance_wrist", sideRightHipDistance)

                        put("side_right_vertical_angle_shoulder_elbow", sideRightShoulderElbowLean% 180 )
                        put("side_right_vertical_angle_elbow_wrist", sideRightElbowWristLean % 180)
                        put("side_right_vertical_angle_hip_knee", sideRightHipKneeLean% 180)
                        put("side_right_vertical_angle_ear_shoulder", sideRightEarShoulderLean  % 180)
                        put("side_right_vertical_angle_nose_shoulder", sideRightNoseShoulderLean  % 180)
                        put("side_right_vertical_angle_shoulder_elbow_wrist", sideRightShoulderElbowWristAngle  % 180)
                        put("side_right_vertical_angle_hip_knee_ankle", sideRightHipKneeAnkleAngle  % 180)
                    }
                    saveJson(mViewModel.staticjo, step)
                }
                5 -> { // ------! 후면 서서 !-------
                    val backEarAngle : Float = calculateSlope(earData[0].first, earData[0].second, earData[1].first, earData[1].second)
                    val backShoulderAngle : Float = calculateSlope(shoulderData[0].first, shoulderData[0].second, shoulderData[1].first, shoulderData[1].second)
                    val backWristAngle : Float = calculateSlope(wristData[0].first, wristData[0].second, wristData[1].first, wristData[1].second)
                    val backElbowAngle : Float = calculateSlope(elbowData[0].first, elbowData[0].second, elbowData[1].first, elbowData[1].second)
                    val backHipAngle : Float = calculateSlope(hipData[0].first, hipData[0].second, hipData[1].first, hipData[1].second)
                    val backKneeAngle : Float = calculateSlope(kneeData[0].first, kneeData[0].second, kneeData[1].first, kneeData[1].second)
                    val backAnkleAngle : Float = calculateSlope(ankleData[0].first, ankleData[0].second, ankleData[1].first, ankleData[1].second)
                                        // ------! 후면 거리 !------
                    val backEarDistance : Float = abs(earData[0].second.minus(earData[1].second))
                    val backShoulderDistance  : Float = abs(shoulderData[0].second.minus(shoulderData[1].second))
                    val backElbowDistance  : Float = abs(shoulderData[0].second.minus(shoulderData[1].second))
                    val backWristDistance  : Float = abs(shoulderData[0].second.minus(shoulderData[1].second))
                    val backHipDistance  : Float = abs(shoulderData[0].second.minus(shoulderData[1].second))
                    val backKneeDistance  : Float = abs(shoulderData[0].second.minus(shoulderData[1].second))
                    val backAnkleDistance  : Float = abs(ankleData[0].second.minus(ankleData[1].second))
                    val backKneeDistanceByX : Pair<Float,Float> = Pair(abs(kneeData[0].first.minus(ankleXAxis)), abs(kneeData[1].first.minus(ankleXAxis)))
                    val backHeelDistanceByX : Pair<Float, Float> = Pair(abs(heelData[0].first.minus(ankleXAxis)), abs(heelData[1].first.minus(ankleXAxis)))

                    val backNoseShoulderLean : Float = calculateSlope(nose.first, nose.second, middleShoulder.first, middleShoulder.second)
                    val backShoulderHipLean : Float = calculateSlope(middleShoulder.first, middleShoulder.second, abs(hipData[0].first - hipData[1].first), abs(hipData[0].second - hipData[1].second))
                    val backNoseHipLean : Float = calculateSlope(nose.first, nose.second, hipData[0].first, abs(hipData[0].second - hipData[1].second))
                    val backKneeHeelLean : Pair<Float, Float> = Pair(calculateSlope(heelData[0].first, heelData[0].second, kneeData[0].first, kneeData[0].second), calculateSlope(heelData[1].first, heelData[1].second, kneeData[1].first, kneeData[1].second))
                    val backWristDistanceByX : Pair<Float, Float> = Pair(abs(wristData[0].first.minus(ankleXAxis)), abs(wristData[1].first.minus(ankleXAxis)))

                    mViewModel.staticjo.apply {
                        put("back_horizontal_angle_ear", backEarAngle % 180)
                        put("back_horizontal_angle_shoulder", backShoulderAngle % 180)
                        put("back_horizontal_angle_wrist", backWristAngle % 180)
                        put("back_horizontal_angle_elbow", backElbowAngle % 180)
                        put("back_horizontal_angle_hip", backHipAngle % 180)
                        put("back_horizontal_angle_knee", backKneeAngle % 180)
                        put("back_horizontal_angle_ankle", backAnkleAngle % 180)

                        put("back_horizontal_distance_sub_ear", backEarDistance)
                        put("back_horizontal_distance_sub_shoulder", backShoulderDistance)
                        put("back_horizontal_distance_sub_elbow", backElbowDistance)
                        put("back_horizontal_distance_sub_wrist", backWristDistance)
                        put("back_horizontal_distance_sub_hip", backHipDistance)
                        put("back_horizontal_distance_sub_knee", backKneeDistance)
                        put("back_horizontal_distance_sub_ankle", backAnkleDistance)

                        put("back_horizontal_distance_knee_left", backKneeDistanceByX.first)
                        put("back_horizontal_distance_knee_right", backKneeDistanceByX.second)
                        put("back_horizontal_distance_heel_left", backHeelDistanceByX.first)
                        put("back_horizontal_distance_heel_right", backHeelDistanceByX.second)

                        put("back_vertical_angle_nose_center_shoulder", backNoseShoulderLean % 180)
                        put("back_vertical_angle_shoudler_center_hip", backShoulderHipLean % 180)
                        put("back_vertical_angle_nose_center_hip", backNoseHipLean % 180)
                        put("back_vertical_angle_knee_heel_left", backKneeHeelLean.first% 180)
                        put("back_vertical_angle_knee_heel_right", backKneeHeelLean.second% 180)
                        put("back_horizontal_distance_wrist_left", backWristDistanceByX.first)
                        put("back_horizontal_distance_wrist_right", backWristDistanceByX.second)
                    }
                    saveJson(mViewModel.staticjo, step)
                }
                6 -> { // ------! 앉았을 때 !------

                    // ------! 의자 후면 거리, 양쪽 부위 높이 차이 - y값 차이 (절댓값)!------
                    val sitBackEarDistance : Float = abs(earData[0].second.minus(earData[1].second))
                    val sitBackShoulderDistance  : Float = abs(shoulderData[0].second.minus(shoulderData[1].second))
                    val sitBackHipDistance  : Float = abs(hipData[0].second.minus(hipData[1].second))

                    // ------! 의자 후면 기울기 !------
                    val sitBackEarAngle = calculateSlope(earData[0].first, earData[0].second, earData[1].first, earData[1].second)
                    val sitBackShoulderAngle = calculateSlope(shoulderData[0].first, shoulderData[0].second, shoulderData[1].first, shoulderData[1].second)
                    val sitBackHipAngle = calculateSlope(hipData[0].first, hipData[0].second, hipData[1].first, hipData[1].second)

                    val shoulderNoseTriangleAngle : Triple<Float, Float, Float> = Triple(calculateAngle(nose.first, nose.second, shoulderData[0].first, shoulderData[0].second, shoulderData[1].first , shoulderData[1].second),
                        calculateAngle(shoulderData[0].first, shoulderData[0].second,  shoulderData[1].first , shoulderData[1].second, nose.first, nose.second),
                        calculateAngle(shoulderData[1].first , shoulderData[1].second, nose.first, nose.second, shoulderData[0].first, shoulderData[0].second))
                    val shoulderHipTriangleAngle : Triple<Float, Float, Float> = Triple(calculateAngle(shoulderData[0].first, shoulderData[0].second, middleHip.first ,middleHip.second, shoulderData[1].first , shoulderData[1].second),
                        calculateAngle(middleHip.first ,middleHip.second, shoulderData[1].first , shoulderData[1].second, shoulderData[0].first, shoulderData[0].second),
                        calculateAngle(shoulderData[1].first , shoulderData[1].second, shoulderData[0].first,shoulderData[0].second, middleHip.first ,middleHip.second ))
                    val shoulderHipRadian = calculateAngleByLine(shoulderData[0].first, shoulderData[0].second, shoulderData[1].first , shoulderData[1].second, middleHip.first, middleHip.second)

                    mViewModel.staticjo.apply {
                        put("back_sit_horizontal_distance_sub_ear", sitBackEarDistance)
                        put("back_sit_horizontal_distance_sub_shoulder", sitBackShoulderDistance)
                        put("back_sit_horizontal_distance_sub_hip", sitBackHipDistance)

                        put("back_sit_horizontal_angle_ear", sitBackEarAngle)
                        put("back_sit_horizontal_angle_shoulder", sitBackShoulderAngle)
                        put("back_sit_horizontal_angle_hip", sitBackHipAngle)

                        put("back_sit_vertical_angle_nose_left_shoulder_right_shoulder", shoulderNoseTriangleAngle.first)
                        put("back_sit_vertical_angle_left_shoulder_right_shoulder_nose", shoulderNoseTriangleAngle.second)
                        put("back_sit_vertical_angle_right_shoulder_left_shoulder_nose", shoulderNoseTriangleAngle.third)
                        put("back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder", shoulderHipTriangleAngle.first)
                        put("back_sit_vertical_angle_center_hip_right_shoulder_left_shoulder", shoulderHipTriangleAngle.second)
                        put("back_sit_vertical_angle_right_shoulder_left_shoulder_center_hip", shoulderHipTriangleAngle.third)
                        put("back_sit_vertical_angle_shoulder_center_hip", shoulderHipRadian)
                    }
                    saveJson(mViewModel.staticjo, step)
                }
            }
            poseLandmarks = JSONArray()
        }
    }

    private fun saveJson(jsonObj: JSONObject, step: Int) {
        jsonObj.apply {
            put("measure_seq", step)
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
            put("user_uuid", singletonUser.jsonObject?.getString("user_uuid"))
            put("user_name", singletonUser.jsonObject?.getString("user_name")!!)
            put("user_sn", singletonUser.jsonObject?.getInt("user_sn")!!)
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
        Log.v("${step}번JSON>Static변환", "${jsonObj.getString("measure_server_json_name")}, ${jsonObj.getString("measure_server_file_name")}")
        saveJo(this@MeasureSkeletonActivity, "${getFileName(step)}.json", jsonObj, mViewModel)
        val measureStaticUnit = mViewModel.staticjo.toMeasureStatic()
        mViewModel.statics.add(measureStaticUnit)
        Log.v("뷰모델스태틱", "${mViewModel.statics}")
        mViewModel.staticjo = JSONObject()
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
        val imageCapture = imageCapture ?: return

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
            val videoCapture = this@MeasureSkeletonActivity.videoCapture ?: return@launch
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

            // Set up MediaStore output options
            val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
                contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            ).setContentValues(contentValues)
                .build()

            // Start recording video with audio
            recording = videoCapture.output.prepareRecording(this@MeasureSkeletonActivity, mediaStoreOutputOptions).apply {
                if (PermissionChecker.checkSelfPermission(
                        this@MeasureSkeletonActivity, Manifest.permission.RECORD_AUDIO
                    ) == PermissionChecker.PERMISSION_GRANTED) {
                    withAudioEnabled()
                }
            }.start(ContextCompat.getMainExecutor(this@MeasureSkeletonActivity)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        CoroutineScope(Dispatchers.Main).launch {
                            startRecording = true
                            startRecordingTimer()
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val savedUri = recordEvent.outputResults.outputUri
                            saveMediaToCache(this@MeasureSkeletonActivity, savedUri, videoFileName, false)
                            callback()
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

//    fun saveMediaToCache(context: Context, uri: Uri, fileName: String, isImage: Boolean) {
//        try {
//            val extension = if (isImage) ".jpg" else ".mp4"
//            val file = File(context.cacheDir, "$fileName$extension")
//            if (isImage) {
//                // 이미지일 경우 720x1280 크기로 리스케일
//
////                val inputStream = context.contentResolver.openInputStream(uri)
////                inputStream?.use { input ->
////                    file.outputStream().use { output ->
////                        input.copyTo(output)
////                    }
////                }
//                val inputStream = context.contentResolver.openInputStream(uri)
//                val tempFile = File.createTempFile("tempImage", null, context.cacheDir)
//                inputStream?.use { input ->
//                    tempFile.outputStream().use { output ->
//                        input.copyTo(output)
//                    }
//                }
//
//                // 이미지를 720x1280으로 리스케일
//                val scaledBitmap = decodeSampledBitmapFromFile(tempFile.absolutePath, 720, 1280)
//
//                // 리스케일된 이미지를 캐시에 저장
//                val outputStream = FileOutputStream(file)
//                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
//                outputStream.flush()
//                outputStream.close()
//
//                // 임시 파일 삭제
//                tempFile.delete()
//                mViewModel.staticFiles.add(file)
//            } else {
//                // 비디오일 경우 그대로 캐시에 저장
//                context.contentResolver.openInputStream(uri)?.use { input ->
//                    file.outputStream().use { output ->
//                        input.copyTo(output)
//                        mViewModel.dynamicFile = file
//                    }
//
//                }
//            }
//
//        } catch (e: IOException) {
//            Log.e("MeasureViewModel", "Error saving image to cache: ${e.message}")
//        }
//    }

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
                val matrix = Matrix().apply {
                    postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
                }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                // 필요한 경우 이미지 회전

                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }

                if (matrix.isIdentity.not()) {
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                }

                // 정확한 크기로 리사이즈
                bitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)

                // 리사이즈된 이미지를 캐시에 저장
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.flush()
                outputStream.close()

                // 임시 파일과 비트맵 정리
                tempFile.delete()
                bitmap.recycle()

                mViewModel.staticFiles.add(file)
            } else {
                // 비디오일 경우 그대로 캐시에 저장
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                        mViewModel.dynamicFile = file
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("MeasureViewModel", "Error saving media to cache: ${e.message}")
        }
    }
//    fun rotateImageIfRequired(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
//        val inputStream = context.contentResolver.openInputStream(uri)
//        val exif = inputStream?.let { ExifInterface(it) }
//        val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
//
//        return when (orientation) {
//            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
//            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
//            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
//            else -> bitmap
//        }
//    }
//
//    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
//        val matrix = Matrix()
//        matrix.postRotate(degrees)
//        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
//    }

    // ------! 애니메이션 !------
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

    private fun calculateScreenX(xx: Float, isImage: Boolean): Int {
        return if (isImage) {
            val scaleFactor = binding.overlay.width * 1f / 720
            val offsetX = (binding.overlay.width - 720 * scaleFactor) / 2
            val x = xx * binding.overlay.width / scaleFactor + offsetX // TODO 후면 카메라 기준임 전면일 경우  1 - 제거
            x.roundToInt()
        } else {
            val scaleFactor = binding.overlay.width * 1f / 1280
            val offsetX = ((binding.overlay.width - 1280 * scaleFactor) / 2 )  // TODO 영상의 liveStream에는 이상하게 offset이 필요함
            val x = xx * binding.overlay.width / scaleFactor + offsetX
            x.roundToInt()
        }
    }

    private fun calculateScreenY(yy: Float, isImage: Boolean): Int {
        return if (isImage) {
            val scaleFactor = binding.overlay.height * 1f / 1280
            val offsetY = (binding.overlay.height - 1280 * scaleFactor) / 2
            val y = yy * binding.overlay.height / scaleFactor + offsetY
            y.roundToInt()
        } else {
            val scaleFactor = binding.overlay.height * 1f / 720
            val offsetY = (binding.overlay.height - 720 * scaleFactor) / 2
            val y = yy * binding.overlay.height / scaleFactor + offsetY
            y.roundToInt()
        }
    }



    suspend fun findLowestYFrame(coordinates: List<Pair<Float, Float>>): Int = withContext(Dispatchers.Default) {
        val yValues = coordinates.map { it.second }
        val highestYIndex = yValues.withIndex().maxByOrNull { it.value }?.index ?: 0

        // 3프레임 전의 인덱스를 반환하되, 리스트의 범위를 벗어나지 않도록 합니다.
        maxOf(0, highestYIndex - 3)
    }
    private fun JSONObject.toMeasureStatic(): MeasureStatic {
        return Gson().fromJson(this.toString(), MeasureStatic::class.java)
    }

    private fun JSONObject.toMeasureDynamic(): MeasureDynamic {
        return Gson().fromJson(this.toString(), MeasureDynamic::class.java)
    }

    fun MeasureStatic.toJson(): String {
        return Gson().toJson(this)
    }
    fun MeasureDynamic.toJson(): String {
        return Gson().toJson(this)
    }
    fun MeasureInfo.toJson(): String {
        return Gson().toJson(this)
    }
}