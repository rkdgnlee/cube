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
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import android.webkit.MimeTypeMap
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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.shuhart.stepview.StepView
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.data.SkeletonViewModel
import com.tangoplus.tangoq.databinding.ActivityMeasureSkeletonBinding
import com.tangoplus.tangoq.dialog.MeasureSkeletonDialogFragment
import com.tangoplus.tangoq.listener.OnSingleClickListener
import com.tangoplus.tangoq.mediapipe.ImageProcessingUtility.decodeSampledBitmapFromFile
import com.tangoplus.tangoq.mediapipe.OverlayView
import com.tangoplus.tangoq.mediapipe.PoseLandmarkAdapter
import com.tangoplus.tangoq.mediapipe.PoseLandmarkerHelper
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)


class MeasureSkeletonActivity : AppCompatActivity(), PoseLandmarkerHelper.LandmarkerListener, SensorEventListener {
// ------! POSE LANDMARKER 설정 시작 !------
companion object {
    private const val TAG = "Pose Landmarker"
    private const val REQUEST_CODE_PERMISSIONS = 1001
    private const val REQUEST_CODE = 1000
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    fun hasPermissions(context: Context) = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            context,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }
}
    private var _binding: ActivityMeasureSkeletonBinding? = null
    private val binding get() = _binding!!
    private val jo = JSONObject()
    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private val viewModel: SkeletonViewModel by viewModels()
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_BACK
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

    // ------! 싱글턴 패턴 객체 가져오기 !------
    private lateinit var singletonInstance: Singleton_t_measure

    var latestResult: PoseLandmarkerHelper.ResultBundle? = null
    private val mViewModel : MeasureViewModel by viewModels()

    private var repeatCount = MutableLiveData(0)
    private val maxRepeats = 6
    private var progress = 12

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

    // ------# step과 파일이름 매칭 #------
    private val fileNames = mapOf(
        0 to "MT_STATIC_FRONT_1_3_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}",
        2 to "MT_STATIC_ELBOW_ALIGN_3_3_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}",
        3 to "MT_STATIC_SIDE_LEFT_4_3_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}",
        4 to "MT_STATIC_SIDE_RIGHT_5_3_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}",
        5 to "MT_STATIC_BACK_6_3_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}",
        6 to "MT_STATIC_BACK_SIT_7_3_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}"
    )
    // ------! 카운트 다운  시작 !-------
    private  val mCountDown : CountDownTimer by lazy {
        object : CountDownTimer(1000, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                runOnUiThread{
                    binding.tvMeasureSkeletonCount.visibility = View.VISIBLE
                    binding.tvMeasureSkeletonCount.alpha = 1f
                    binding.tvMeasureSkeletonCount.text = "${(millisUntilFinished.toFloat() / 1000.0f).roundToInt()}"
                    Log.v("count", "${binding.tvMeasureSkeletonCount.text}")
                }
            }
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onFinish() {
                if (latestResult != null) {
                    if (isRecording) { // 동영상 촬영
                        binding.tvMeasureSkeletonCount.text = "스쿼트를 실시해주세요"
                        startRecording = true
                        setAnimation(binding.tvMeasureSkeletonCount, 1000, 500, false) {
                            hideViews(5700)
                            // 녹화 종료 시점 아님
                            startVideoRecording {
                                Log.v("녹화종료시점", "isRecording: $isRecording, isCapture: $isCapture, latestResult: $latestResult")
                                isRecording = false // 녹화 완료
                                startRecording = false
                                updateUI()
                                Log.v("videoFrame", "videoFrame Count: ${mViewModel.dynamicJa.length()}")
                                mViewModel.measurejo.put(mViewModel.dynamicJa) // 종료됐을 때 약 220개 size의 dynamicJa를 meausrejo에 넣는다.
                                Log.v("jsonCastErrorVideo", "measureJo길이: ${mViewModel.measurejo.length()}")

                                binding.tvMeasureSkeletonCount.text = "다음 동작을 준비해주세요"
                            }
                        }
                    } else {
                        binding.tvMeasureSkeletonCount.text = "자세를 따라해주세요"
                        hideViews(600)
                        Log.v("사진service", "isCapture: ${isCapture}, isRecording: ${isRecording}")
                        // ------! 종료 후 다시 세팅 !------

                        captureImage(repeatCount.value!!)
                        latestResult?.let { resultBundleToJson(it, repeatCount.value!!) }
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
        singletonInstance = Singleton_t_measure.getInstance(this)

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
                // TODO 통신 시간에 따라 alertDialog 띄우던가 해야 함 ( 전송 로딩 )
                Log.v("측정완료VM", "${mViewModel.measurejo.length()}, ${mViewModel.measurejo}")
                singletonInstance.measures?.add(0, mViewModel.convertJsonToMeasureVO(mViewModel.measurejo))

                val intent = Intent()
                intent.putExtra("finishedMeasure", true)
                setResult(Activity.RESULT_OK, intent)
                finish()

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
            val angle = Math.toDegrees(Math.asin((clampedZ / SensorManager.GRAVITY_EARTH).toDouble())).toFloat()
            filteredAngle = lowPassFilter(angle)
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

    private fun setPreviousStep() {
        when (repeatCount.value) {
            maxRepeats -> {
                repeatCount.value = repeatCount.value?.minus(1)
                binding.pvMeasureSkeleton.progress -= 16  // 마지막 남은 2까지 전부 빼기
                binding.tvMeasureSkeletonCount.text = "프레임에 맞춰 서주세요"
                binding.btnMeasureSkeletonStep.text = "측정하기"
                binding.svMeasureSkeleton.go(repeatCount.value!!.toInt(), true)
                mViewModel.measurejo.remove(6)
                }
            else -> {
                repeatCount.value = repeatCount.value?.minus(1)
                progress -= 14
                binding.pvMeasureSkeleton.progress = progress
                Log.v("녹화종료되나요?", "repeatCount: ${repeatCount.value}, progress: $progress")
                binding.svMeasureSkeleton.go(repeatCount.value!!.toInt(), true)
                val drawable = ContextCompat.getDrawable(this, resources.getIdentifier("drawable_measure_${repeatCount.value!!.toInt()}", "drawable", packageName))
                binding.ivMeasureSkeletonFrame.setImageDrawable(drawable)
                mViewModel.measurejo.remove(repeatCount.value!!)
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
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
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
                resultBundleToJson(latestResult!!, repeatCount.value!!)
                Log.v("detectLiveStream", "videoCount: ${repeatCount.value}")
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

                binding.overlay.invalidate()
                latestResult = resultBundle
                if (scaleFactorX != null && scaleFactorY != null) {
                    scaleFactorX = binding.overlay.width * 1f / latestResult!!.inputImageWidth
                    scaleFactorY = binding.overlay.height * 1f / latestResult!!.inputImageHeight
                }
            }
        }
    }
    // ------# 기울기 계산 #------
    fun calculateSlope(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        return (y2 - y1) / (x2 - x1)
    }
    // ------# 점 3개의 각도 계산 #------
    private fun calculateAngle(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3:Double): Double {
        val v1x = x1 - x2
        val v1y = y1 - y2
        val v2x = x3 - x2
        val v2y = y3 - y2
        val dotProduct = v1x * v2x + v1y * v2y
        val magnitude1 = Math.sqrt(v1x * v1x + v1y * v1y)
        val magnitude2 = Math.sqrt(v2x * v2x + v2y * v2y)

        val cosTheta = dotProduct / (magnitude1 * magnitude2)
        val angleRadians = Math.acos(cosTheta)
        return Math.toDegrees(angleRadians)
    }
    // ------# 점과 점사이의 거리 #------
    private fun calculateDistanceByDots(x1: Double, y1: Double, x2: Double, y2: Double) : Double{
        val dx = x2 - x1
        val dy = y2 - y1
        return Math.sqrt(dx * dx + dy * dy)
    }

    // ------! 선과 점의 X 거리 !------
//    fun calculateXDifference(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Float {
//        val x4 = x1 + (y3 - y1) / (y2 - y1) * (x2 - x1)
//        return abs(x3 - x4)
//    }
    private fun radianToDegree(radian : Double) : Double {
        return radian * ( 180.0 / Math.PI )
    }

    private fun calculateAngleByLine(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double) : Double {
        val vector1X = x2 - x1
        val vector1Y = y2 - y1
        val vector2X = x3 - x1
        val vector2Y = y3 - y1
        val dotProduct = vector1X * vector2X + vector1Y * vector2Y
        val magnitude1 = sqrt(vector1X.pow(2) + vector1Y.pow(2))
        val magnitude2 = sqrt(vector2X.pow(2) + vector2Y.pow(2))

        val cosTheta = dotProduct / (magnitude1 * magnitude2)
        val angleRad = acos(cosTheta.coerceIn(-1.0, 1.0))

        return angleRad * (180f / PI.toFloat())
    }

    // override 로 resultbundle이 계속 나오는데 해당 항목을 전역변수 latest
    fun resultBundleToJson(resultBundle: PoseLandmarkerHelper.ResultBundle, step: Int) {
        val earData = mutableListOf<Pair<Double, Double>>() // index 0 왼 index 1 오른
        val shoulderData = mutableListOf<Pair<Double, Double>>()
        val elbowData = mutableListOf<Pair<Double, Double>>()
        val wristData = mutableListOf<Pair<Double, Double>>()
        val indexData = mutableListOf<Pair<Double, Double>>()
        val pinkyData = mutableListOf<Pair<Double, Double>>()

        val thumbData = mutableListOf<Pair<Double, Double>>()
        val hipData = mutableListOf<Pair<Double, Double>>()
        val kneeData = mutableListOf<Pair<Double, Double>>()
        val ankleData = mutableListOf<Pair<Double, Double>>()
        val heelData = mutableListOf<Pair<Double, Double>>()
        val toeData = mutableListOf<Pair<Double, Double>>()
        if (resultBundle.results.first().landmarks().isNotEmpty()) {

            val plr = resultBundle.results.first().landmarks()[0]!!
            val poseLandmarks = JSONArray()

            Log.v("scales", "latestResult!!.inputImage: (${latestResult!!.inputImageWidth}, ${latestResult!!.inputImageHeight})")
            plr.forEachIndexed { index, poseLandmark ->
                val jo = JSONObject()
                jo.put("index", index)
                jo.put("isActive", true)
                jo.put("sx", calculateScreenX(poseLandmark.x() ))
                jo.put("sy", calculateScreenY(poseLandmark.y()  ))
                jo.put("wx", poseLandmark.x())
                jo.put("wy", poseLandmark.y())
                jo.put("wz", poseLandmark.z())
                poseLandmarks.put(jo)
            }

            for (i in 7 until  plr.size) {
                when (i) {
                    in 7 .. 8 -> earData.add(Pair(plr[i].x().toDouble(), plr[i].y().toDouble()))
                    in 11 .. 12 -> shoulderData.add(Pair(plr[i].x().toDouble(), plr[i].y().toDouble()))
                    in 13 .. 14 -> elbowData.add(Pair(plr[i].x().toDouble(), plr[i].y().toDouble()))
                    in 15 .. 16 -> wristData.add(Pair(plr[i].x().toDouble(), plr[i].y().toDouble()))
                    in 17 .. 18 -> pinkyData.add(Pair(plr[i].x().toDouble(), plr[i].y().toDouble()))
                    in 19 .. 20 -> indexData.add(Pair(plr[i].x().toDouble(), plr[i].y().toDouble()))
                    in 21 .. 22 -> thumbData.add(Pair(plr[i].x().toDouble(), plr[i].y().toDouble()))
                    in 23 .. 24 -> hipData.add(Pair(plr[i].x().toDouble(), plr[i].y().toDouble()))
                    in 25 .. 26 -> kneeData.add(Pair(plr[i].x().toDouble(), plr[i].y().toDouble()))
                    in 27 .. 28 -> ankleData.add(Pair(plr[i].x().toDouble(), plr[i].y().toDouble()))
                    in 29 .. 30 -> heelData.add(Pair(plr[i].x().toDouble(), plr[i].y().toDouble()))
                    in 31 .. 32 -> toeData.add(Pair(plr[i].x().toDouble(), plr[i].y().toDouble()))
                }
            }
            val nose : Pair<Double, Double> = Pair(plr[0].x().toDouble(), plr[0].y().toDouble())
            val ankleXAxis = ankleData[0].first.minus(ankleData[1].first)  / 2
            val middleHip = Pair((hipData[0].first + hipData[1].first) / 2, (hipData[0].second + hipData[1].second) / 2)
            val middleShoulder = Pair((shoulderData[0].first + shoulderData[1].first) / 2, (shoulderData[0].second + shoulderData[1].second) / 2)
            /** mutablelist 0 왼쪽 1 오른쪽
            *  , 그리고 first: x    second: y
            * */
            when (step) {
                0 -> {
                    val earAngle : Double = calculateSlope(earData[0].first, earData[0].second, earData[1].first, earData[1].second)
                    val shoulderAngle : Double = calculateSlope(shoulderData[0].first, shoulderData[0].second, shoulderData[1].first, shoulderData[1].second)
                    val elbowAngle : Double = calculateSlope(elbowData[0].first, elbowData[0].second, elbowData[1].first, elbowData[1].second)
                    val wristAngle : Double = calculateSlope(wristData[0].first, wristData[0].second, wristData[1].first, wristData[1].second)
                    val hipAngle : Double = calculateSlope(hipData[0].first, hipData[0].second, hipData[1].first, hipData[1].second)
                    val kneeAngle : Double = calculateSlope(kneeData[0].first, kneeData[0].second, kneeData[1].first, kneeData[1].second)
                    val ankleAngle : Double = calculateSlope(ankleData[0].first, ankleData[0].second, ankleData[1].first, ankleData[1].second)
                                        // 부위 양 높이 차이
                    val earDistance : Double = abs(earData[0].second.minus(earData[1].second))
                    val shoulderDistance : Double = abs(shoulderData[0].second.minus(shoulderData[1].second))
                    val elbowDistance : Double = abs(elbowData[0].second.minus(elbowData[1].second))
                    val wristDistance : Double = abs(wristData[0].second.minus(wristData[1].second))
                    val hipDistance : Double = abs(hipData[0].second.minus(hipData[1].second))
                    val kneeDistance : Double = abs(kneeData[0].second.minus(kneeData[1].second))
                    val ankleDistance : Double = abs(ankleData[0].second.minus(ankleData[1].second))

                    // 각 부위 x축 부터 거리 거리
                    val wristSubDistanceByX : Pair<Double, Double> = Pair(abs(wristData[0].first.minus(ankleXAxis)), abs(wristData[1].first.minus(ankleXAxis)))
                    val kneeSubDistanceByX : Pair<Double, Double> = Pair(abs(kneeData[0].first.minus(ankleXAxis)), abs(kneeData[1].first.minus(ankleXAxis)))
                    val ankleSubDistanceByX : Pair<Double, Double> = Pair(abs(ankleData[0].first.minus(ankleXAxis)), abs(ankleData[1].first.minus(ankleXAxis)))
                    val toeSubDistance : Pair<Double, Double> = Pair(abs(toeData[0].first.minus(ankleXAxis)), abs(toeData[1].first.minus(ankleXAxis)))

                    // 팔 각도
                    val shoulderElbowLean: Pair<Double, Double> = Pair(calculateSlope(shoulderData[0].first, shoulderData[0].second, elbowData[0].first, elbowData[0].second), calculateSlope(shoulderData[1].first, shoulderData[1].second, elbowData[1].first, elbowData[1].second))
                    val elbowWristLean: Pair<Double, Double> = Pair(calculateSlope(elbowData[0].first, elbowData[0].second, wristData[0].first, wristData[0].second), calculateSlope(elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second))
                    val wristThumbLean: Pair<Double, Double> = Pair(calculateSlope(wristData[0].first, wristData[0].second, thumbData[0].first, thumbData[0].second), calculateSlope(wristData[1].first, wristData[1].second, thumbData[1].first, thumbData[1].second))
                    val hipKneeLean : Pair<Double, Double> = Pair(calculateSlope(hipData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second), calculateSlope(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second))
                    val kneeAnkleLean : Pair<Double, Double> = Pair(calculateSlope(kneeData[0].first, kneeData[0].second, ankleData[0].first, ankleData[0].second), calculateSlope(kneeData[1].first, kneeData[1].second, ankleData[1].first, ankleData[1].second))
                    val ankleToeLean : Pair<Double, Double> = Pair(calculateSlope(ankleData[0].first, ankleData[0].second, toeData[0].first, toeData[0].second), calculateSlope(ankleData[1].first, ankleData[1].second, toeData[1].first, toeData[1].second))

                    val shoulderElbowWristAngle : Pair<Double, Double> = Pair(calculateAngle(shoulderData[0].first, shoulderData[0].second, elbowData[0].first, elbowData[0].second, wristData[0].first, wristData[0].second),
                        calculateAngle(shoulderData[1].first, shoulderData[1].second, elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second))
                    val hipKneeAnkleAngle : Pair<Double, Double> = Pair(calculateAngle(hipData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second, ankleData[0].first, ankleData[0].second),
                        calculateAngle(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second, ankleData[1].first, ankleData[1].second))

                    mViewModel.staticjo.apply {
                        put("front_horizontal_angleh_ear", earAngle)
                        put("front_horizontal_angle_shoulder", shoulderAngle)
                        put("front_horizontal_angle_elbow", elbowAngle)
                        put("front_horizontal_angle_wrist", wristAngle)
                        put("front_horizontal_angle_hip", hipAngle)
                        put("front_horizontal_angle_knee", kneeAngle)
                        put("front_horizontal_angle_ankle", ankleAngle)

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

                        put("front_vertical_angle_shoulder_elbow_left", shoulderElbowLean.first)
                        put("front_vertical_angle_shoulder_elbow_right", shoulderElbowLean.second)
                        put("front_vertical_angle_elbow_wrist_left", elbowWristLean.first)
                        put("front_vertical_angle_elbow_wrist_right", elbowWristLean.second)
                        put("front_vertical_angle_wrist_thumb_left", wristThumbLean.first)
                        put("front_vertical_angle_wrist_thumb_right", wristThumbLean.second)
                        put("front_vertical_angle_hip_knee_left", hipKneeLean.first)
                        put("front_vertical_angle_hip_knee_right", hipKneeLean.second)
                        put("front_vertical_angle_knee_ankle_left", kneeAnkleLean.first)
                        put("front_vertical_angle_knee_ankle_right", kneeAnkleLean.second)
                        put("front_vertical_angle_ankle_toe_left", ankleToeLean.first)
                        put("front_vertical_angle_ankle_toe_right", ankleToeLean.second)

                        put("front_vertical_angle_shoulder_elbow_wrist_left", shoulderElbowWristAngle.first)
                        put("front_vertical_angle_shoulder_elbow_wrist_right", shoulderElbowWristAngle.second)
                        put("front_vertical_angle_hip_knee_ankle_left", hipKneeAnkleAngle.first)
                        put("front_vertical_angle_hip_knee_ankle_right", hipKneeAnkleAngle.second)

                        put("measure_sequence", step)
                        put("measure_type", 1)
                        put("pose_landmark", poseLandmarks)
                    }

                    savejson(mViewModel.staticjo, step)


                }
                1 -> {  // 스쿼트
                    // 현재 프레임 당 계속 넣을 공간임
                    // ------# 각도 타임으로 넣기 #------
                    val wristAngle : Double = calculateSlope(wristData[0].first, wristData[0].second, wristData[1].first, wristData[1].second)
                    val wristDistance : Double = abs(wristData[0].second - wristData[1].second)
                    val wristDistanceByCenter : Pair<Double, Double> = Pair(abs(wristData[0].first.minus(ankleXAxis)), abs(wristData[1].first.minus(ankleXAxis)))
                    val elbowAngle : Double = calculateSlope(elbowData[0].first, elbowData[0].second, elbowData[1].first, elbowData[1].second)
                    val elbowDistance : Double = abs(elbowData[0].second - elbowData[1].second)
                    val shoulderAngle : Double = calculateSlope(shoulderData[0].first, shoulderData[0].second, shoulderData[1].first, shoulderData[1].second)
                    val shoulderDistance : Double = abs(shoulderData[0].second - shoulderData[1].second)
                    val hipAngle : Double = calculateSlope(hipData[0].first, hipData[0].second, hipData[1].first, hipData[1].second)
                    val hipDistance : Double = abs(hipData[0].second - hipData[1].second)
                    val hipDistanceByCenter : Pair<Double, Double> = Pair(abs(hipData[0].first.minus(ankleXAxis)), abs(hipData[1].first.minus(ankleXAxis)))
                    val kneeAngle : Double = calculateSlope(kneeData[0].first, kneeData[0].second, kneeData[1].first, kneeData[1].second)
                    val kneeDistance : Double = abs(kneeData[0].second - kneeData[1].second)
                    val kneeDistanceByCenter : Pair<Double, Double> = Pair(abs(kneeData[0].first.minus(ankleXAxis)), abs(kneeData[1].first.minus(ankleXAxis)))
                    val toeDistanceByCenter : Pair<Double, Double> = Pair(abs(toeData[0].first.minus(ankleXAxis)), abs(toeData[1].first.minus(ankleXAxis)))
                    val wristElbowShoulderAngle : Pair<Double, Double> = Pair(calculateAngle(wristData[0].first, wristData[0].second, elbowData[0].first, elbowData[0].second, shoulderData[0].first, shoulderData[0].second),
                        calculateAngle(wristData[1].first, wristData[1].second, elbowData[1].first, elbowData[1].second, shoulderData[1].first, shoulderData[1].second))
                    val wristElbowAngle : Pair<Double, Double> = Pair(calculateSlope(wristData[0].first, wristData[0].second, elbowData[0].first, elbowData[0].second),
                        calculateSlope(wristData[1].first, wristData[1].second, elbowData[1].first, elbowData[1].second))
                    val elbowShoulderAngle : Pair<Double, Double> = Pair(calculateSlope(elbowData[0].first, elbowData[0].second, shoulderData[0].first, shoulderData[0].second),
                        calculateSlope(elbowData[1].first, elbowData[1].second, shoulderData[1].first, shoulderData[1].second))
                    val hipKneeToeAngle : Pair<Double, Double> = Pair(calculateAngle(hipData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second, toeData[0].first, toeData[0].second),
                        calculateAngle(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second, toeData[1].first, toeData[1].second))
                    val hipKneeAngle : Pair<Double, Double> = Pair(calculateSlope(hipData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second),
                        calculateSlope(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second))
                    val kneeToeAngle : Pair<Double, Double> = Pair(calculateSlope(kneeData[0].first, kneeData[0].second, toeData[0].first, toeData[0].second),
                        calculateSlope(kneeData[1].first, kneeData[1].second, toeData[1].first, toeData[1].second))
                    val ankleToeAngle : Pair<Double, Double> = Pair(calculateSlope(ankleData[0].first, ankleData[0].second, toeData[0].first, toeData[0].second),
                        calculateSlope(ankleData[1].first, ankleData[1].second, toeData[1].first, toeData[1].second))
                    val kneeAnkleToeAngle : Pair<Double, Double> = Pair(calculateAngle(kneeData[0].first, kneeData[0].second, ankleData[0].first, ankleData[0].second, toeData[0].first, toeData[0].second),
                        calculateAngle(kneeData[1].first, kneeData[1].second, ankleData[1].first, ankleData[1].second, toeData[1].first, toeData[1].second))
                    val indexDistanceByCenter : Pair<Double, Double> = Pair(abs(indexData[0].first.minus(ankleXAxis)), abs(indexData[1].first.minus(ankleXAxis)))
                    val indexAngle : Double = calculateSlope(indexData[0].first, indexData[0].second, indexData[1].first, indexData[1].second)
                    val indexDistance : Double = abs(indexData[0].second - indexData[1].second)

                    mViewModel.dynamicJoUnit.apply {
                        put("horizontal_angle_wrist", wristAngle)
                        put("horizontal_distance_wrist", wristDistance)

                        put("horizontal_distance_center_left_wrist", wristDistanceByCenter.first)
                        put("horizontal_distance_center_right_wrist", wristDistanceByCenter.second)
                        put("horizontal_angle_elbow", elbowAngle)
                        put("horizontal_distance_elbow", elbowDistance)
                        put("horizontal_angle_shoulder", shoulderAngle)
                        put("horizontal_distance_shoulder", shoulderDistance)
                        put("horizontal_angle_hip", hipAngle)
                        put("horizontal_distance_hip", hipDistance)
                        put("horizontal_distance_center_left_hip", hipDistanceByCenter.first)
                        put("horizontal_distance_center_right_hip", hipDistanceByCenter.second)
                        put("horizontal_angle_knee", kneeAngle)
                        put("horizontal_distance_knee", kneeDistance)
                        put("horizontal_distance_center_left_knee", kneeDistanceByCenter.first)
                        put("horizontal_distance_center_right_knee", kneeDistanceByCenter.second)
                        put("horizontal_distance_center_left_toe", toeDistanceByCenter.first)
                        put("horizontal_distance_center_right_toe", toeDistanceByCenter.second)
                        put("vertical_angle_wrist_elbow_shoulder_left", wristElbowShoulderAngle.first)
                        put("vertical_angle_wrist_elbow_shoulder_right", wristElbowShoulderAngle.second)
                        put("vertical_angle_wrist_elbow_left", wristElbowAngle.first)
                        put("vertical_angle_wrist_elbow_right", wristElbowAngle.second)
                        put("vertical_angle_elbow_shoulder_left", elbowShoulderAngle.first)
                        put("vertical_angle_elbow_shoulder_right", elbowShoulderAngle.second)
                        put("vertical_angle_hip_knee_toe_left", hipKneeToeAngle.first)
                        put("vertical_angle_hip_knee_toe_right", hipKneeToeAngle.second)
                        put("vertical_angle_hip_knee_left", hipKneeAngle.first)
                        put("vertical_angle_hip_knee_right", hipKneeAngle.second)
                        put("vertical_angle_knee_toe_left", kneeToeAngle.first)
                        put("vertical_angle_knee_toe_right", kneeToeAngle.second)
                        put("vertical_angle_ankle_toe_left", ankleToeAngle.first)
                        put("vertical_angle_ankle_toe_right", ankleToeAngle.second)
                        put("vertical_angle_knee_ankle_toe_left", kneeAnkleToeAngle.first)
                        put("vertical_angle_knee_ankle_toe_right", kneeAnkleToeAngle.second)
                        put("horizontal_angle_mid_finger_tip", indexAngle)
                        put("horizontal_distance_mid_finger_tip", indexDistance)
                        put("horizontal_distance_center_mid_finger_tip_left", indexDistanceByCenter.first)
                        put("vertical_angle_knee_toe_right", indexDistanceByCenter.second)
                        put("pose_landmark", poseLandmarks)
                        put("measure_seq", 2)
                        put("measure_type", 7)
                        put("measure_start_time", "${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}")
                        put("measure_photo_file_name", "MT_DYNAMIC_OVERHEADSQUAT_FRONT_1_3_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}")
                    }
                    // 1프레임당 dynamic 측정 모두 들어감
                    mViewModel.dynamicJa.put(mViewModel.dynamicJoUnit)
                    mViewModel.dynamicJoUnit = JSONObject()

                }
                2 -> { // ------! 주먹 쥐고 !------

                    val wristShoulderDistance : Pair<Double, Double> = Pair(calculateDistanceByDots(wristData[0].first, wristData[0].second, shoulderData[0].first, shoulderData[0].second),
                        calculateDistanceByDots(wristData[1].first, wristData[1].second, shoulderData[1].first, shoulderData[1].second))
                    val wristsDistanceByY : Double = abs(wristData[0].second - wristData[1].second)
                    val indexWristElbowAngle : Pair<Double, Double> = Pair(calculateAngle(indexData[0].first, indexData[0].second, wristData[0].first, wristData[0].second, elbowData[0].first, elbowData[0].second),
                        calculateAngle(indexData[1].first, indexData[1].second, wristData[1].first, wristData[1].second, elbowData[1].first, elbowData[1].second))
                    val shoulderElbowWristAngle : Pair<Double, Double> = Pair(calculateAngle(shoulderData[0].first, shoulderData[0].second, elbowData[0].first, elbowData[0].second, wristData[0].first, wristData[0].second),
                        calculateAngle(shoulderData[1].first, shoulderData[1].second, elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second))
                    val indexDistanceByX: Pair<Double, Double> = Pair(abs(indexData[0].first.minus(ankleXAxis)), abs(indexData[1].first.minus(ankleXAxis)))
                    val wristDistanceByX: Pair<Double, Double> = Pair(abs(wristData[0].first.minus(ankleXAxis)), abs(wristData[1].first.minus(ankleXAxis)))

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

                        mViewModel.staticjo.put("pose_landmark", poseLandmarks)
                    }

                    savejson(mViewModel.staticjo, step)

                }
                3 -> { // 왼쪽보기 (오른쪽 팔)

                    // ------! 측면 거리  - 왼쪽 !------
                    val sideLeftShoulderDistance : Double = abs(shoulderData[0].first.minus(ankleXAxis))
                    val sideLeftWristDistance : Double = abs(wristData[0].first.minus(ankleXAxis))
                    val sideLeftPinkyDistance : Double = abs(pinkyData[0].first.minus(ankleXAxis))
                    val sideLeftHipDistance : Double = abs(hipData[0].first.minus(ankleXAxis))

                    val sideLeftShoulderElbowLean : Double = calculateSlope(shoulderData[0].first, shoulderData[0].second, kneeData[0].first, kneeData[0].second)
                    val sideLeftElbowWristLean: Double =calculateSlope(elbowData[0].first, elbowData[0].second, wristData[0].first, wristData[0].second)
                    val sideLeftHipKneeLean : Double = calculateSlope(hipData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second)
                    val sideLeftEarShoulderLean : Double = calculateSlope(earData[0].first, earData[0].second, shoulderData[0].first, shoulderData[0].second)
                    val sideLeftNoseShoulderLean : Double = calculateSlope(nose.first, nose.second, shoulderData[0].first, shoulderData[0].second)
                    val sideLeftShoulderElbowWristAngle : Double = calculateAngle(shoulderData[0].first, shoulderData[0].second, elbowData[0].first, elbowData[0].second, wristData[0].first, wristData[0].second)
                    val sideLeftHipKneeAnkleAngle : Double = calculateAngle(hipData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second, ankleData[0].first, ankleData[0].second)

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

                        put("pose_landmark", poseLandmarks)
                    }
                    Log.v("mViewModelStatic", "${mViewModel.staticjo.optJSONArray("pose_landmark")}")
                    savejson(mViewModel.staticjo, step)
                }
                4 -> { // 오른쪽보기 (왼쪽 팔)
                    // ------! 측면 거리  - 오른쪽 !------

                    val sideRightShoulderDistance : Double = abs(shoulderData[1].first.minus(ankleXAxis))
                    val sideRightWristDistance : Double = abs(wristData[1].first.minus(ankleXAxis))
                    val sideRightPinkyDistance : Double = abs(pinkyData[1].first.minus(ankleXAxis))
                    val sideRightHipDistance : Double = abs(hipData[1].first.minus(ankleXAxis))
                                        // ------! 측면 기울기  - 오른쪽 !------
                    val sideRightShoulderElbowLean : Double = calculateSlope(shoulderData[1].first, shoulderData[1].second, kneeData[1].first, kneeData[1].second)
                    val sideRightElbowWristLean: Double =calculateSlope(elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second)
                    val sideRightHipKneeLean : Double = calculateSlope(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second)
                    val sideRightEarShoulderLean : Double = calculateSlope(earData[1].first, earData[1].second, shoulderData[1].first, shoulderData[1].second)
                    val sideRightNoseShoulderLean : Double = calculateSlope(nose.first, nose.second, shoulderData[1].first, shoulderData[1].second)
                    val sideRightShoulderElbowWristAngle : Double = calculateAngle(shoulderData[1].first, shoulderData[1].second, elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second)
                    val sideRightHipKneeAnkleAngle : Double = calculateAngle(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second, ankleData[1].first, ankleData[1].second)

                    mViewModel.staticjo.apply {
                        put("side_right_horizontal_distance_shoulder", sideRightShoulderDistance)
                        put("side_right_horizontal_distance_hip", sideRightWristDistance)
                        put("side_right_horizontal_distance_pinky", sideRightPinkyDistance)
                        put("side_right_horizontal_distance_wrist", sideRightHipDistance)

                        put("side_right_vertical_angle_shoulder_elbow", sideRightShoulderElbowLean )
                        put("side_right_vertical_angle_elbow_wrist", sideRightElbowWristLean )
                        put("side_right_vertical_angle_hip_knee", sideRightHipKneeLean)
                        put("side_right_vertical_angle_ear_shoulder", sideRightEarShoulderLean  % 180)
                        put("side_right_vertical_angle_nose_shoulder", sideRightNoseShoulderLean  % 180)
                        put("side_right_vertical_angle_shoulder_elbow_wrist", sideRightShoulderElbowWristAngle  % 180)
                        put("side_right_vertical_angle_hip_knee_ankle", sideRightHipKneeAnkleAngle  % 180)

                        put("pose_landmark", poseLandmarks)
                    }

                    savejson( mViewModel.staticjo, step)
                }
                5 -> { // ------! 후면 서서 !-------
                    val backEarAngle : Double = calculateSlope(earData[0].first, earData[0].second, earData[1].first, earData[1].second)
                    val backShoulderAngle : Double = calculateSlope(shoulderData[0].first, shoulderData[0].second, shoulderData[1].first, shoulderData[1].second)
                    val backWristAngle : Double = calculateSlope(wristData[0].first, wristData[0].second, wristData[1].first, wristData[1].second)
                    val backElbowAngle : Double = calculateSlope(elbowData[0].first, elbowData[0].second, elbowData[1].first, elbowData[1].second)
                    val backHipAngle : Double = calculateSlope(hipData[0].first, hipData[0].second, hipData[1].first, hipData[1].second)
                    val backKneeAngle : Double = calculateSlope(kneeData[0].first, kneeData[0].second, kneeData[1].first, kneeData[1].second)
                    val backAnkleAngle : Double = calculateSlope(ankleData[0].first, ankleData[0].second, ankleData[1].first, ankleData[1].second)
                                        // ------! 후면 거리 !------
                    val backEarDistance : Double = abs(earData[0].second.minus(earData[1].second))
                    val backShoulderDistance  : Double = abs(shoulderData[0].second.minus(shoulderData[1].second))
                    val backElbowDistance  : Double = abs(shoulderData[0].second.minus(shoulderData[1].second))
                    val backWristDistance  : Double = abs(shoulderData[0].second.minus(shoulderData[1].second))
                    val backHipDistance  : Double = abs(shoulderData[0].second.minus(shoulderData[1].second))
                    val backKneeDistance  : Double = abs(shoulderData[0].second.minus(shoulderData[1].second))
                    val backAnkleDistance  : Double = abs(ankleData[0].second.minus(ankleData[1].second))

                    val backKneeDistanceByX : Pair<Double,Double> = Pair(abs(kneeData[0].first.minus(ankleXAxis)), abs(kneeData[1].first.minus(ankleXAxis)))
                    val backHeelDistanceByX : Pair<Double, Double> = Pair(abs(heelData[0].first.minus(ankleXAxis)), abs(heelData[1].first.minus(ankleXAxis)))

                    val backNoseShoulderLean : Double = calculateSlope(nose.first, nose.second, middleShoulder.first, middleShoulder.second)
                    val backShoulderHipLean : Double = calculateSlope(middleShoulder.first, middleShoulder.second, abs(hipData[0].first - hipData[1].first), abs(hipData[0].second - hipData[1].second))
                    val backNoseHipLean : Double = calculateSlope(nose.first, nose.second, hipData[0].first, abs(hipData[0].second - hipData[1].second))
                    val backKneeHeelLean : Pair<Double, Double> = Pair(calculateSlope(heelData[0].first, heelData[0].second, kneeData[0].first, kneeData[0].second), calculateSlope(heelData[1].first, heelData[1].second, kneeData[1].first, kneeData[1].second))
                    val backWristDistanceByX : Pair<Double, Double> = Pair(abs(wristData[0].first.minus(ankleXAxis)), abs(wristData[1].first.minus(ankleXAxis)))

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

                        put("back_vertical_angle_nose_center_shoulder", backNoseShoulderLean)
                        put("back_vertical_angle_shoudler_center_hip", backShoulderHipLean)
                        put("back_vertical_angle_nose_center_hip", backNoseHipLean)
                        put("back_vertical_angle_knee_heel_left", backKneeHeelLean.first)
                        put("back_vertical_angle_knee_heel_right", backKneeHeelLean.second)
                        put("back_horizontal_distance_wrist_left", backWristDistanceByX.first)
                        put("back_horizontal_distance_wrist_right", backWristDistanceByX.second)

                        put("pose_landmark", poseLandmarks)
                    }
                    savejson( mViewModel.staticjo, step)

                }
                6 -> { // ------! 앉았을 때 !------

                    // ------! 의자 후면 거리, 양쪽 부위 높이 차이 - y값 차이 (절댓값)!------
                    val sitBackEarDistance : Double = abs(earData[0].second.minus(earData[1].second))
                    val sitBackShoulderDistance  : Double = abs(shoulderData[0].second.minus(shoulderData[1].second))
                    val sitBackHipDistance  : Double = abs(hipData[0].second.minus(hipData[1].second))

                    // ------! 의자 후면 기울기 !------
                    val sitBackEarAngle = calculateSlope(earData[0].first, earData[0].second, earData[1].first, earData[1].second)
                    val sitBackShoulderAngle = calculateSlope(shoulderData[0].first, shoulderData[0].second, shoulderData[1].first, shoulderData[1].second)
                    val sitBackHipAngle = calculateSlope(hipData[0].first, hipData[0].second, hipData[1].first, hipData[1].second)

                    val shoulderNoseTriangleAngle : Triple<Double, Double, Double> = Triple(calculateAngle(nose.first, nose.second, shoulderData[0].first, shoulderData[0].second, shoulderData[1].first , shoulderData[1].second),
                        calculateAngle(shoulderData[0].first, shoulderData[0].second,  shoulderData[1].first , shoulderData[1].second, nose.first, nose.second),
                        calculateAngle(shoulderData[1].first , shoulderData[1].second, nose.first, nose.second, shoulderData[0].first, shoulderData[0].second))
                    val shoulderHipTriangleAngle : Triple<Double, Double, Double> = Triple(calculateAngle(shoulderData[0].first, shoulderData[0].second, middleHip.first ,middleHip.second, shoulderData[1].first , shoulderData[1].second),
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
                        put("pose_landmark", poseLandmarks)
                    }
                    savejson(mViewModel.staticjo, step)

                }
            }
        }
//
    }
    private fun savejson(jsonObj: JSONObject, step: Int) {
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
            put("measure_photo_file_name", getFileName(step))
            put("measure_start_time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            put("measure_end_time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            put("measure_overlay_scale_factor_x", scaleFactorX)
            put("measure_overlay_scale_factor_y", scaleFactorY)
        }


        mViewModel.measurejo.put(jsonObj)
        Log.v("mViewModelStatic", mViewModel.measurejo.optJSONObject(0).optString("measure_photo_file_name"))
        Log.v("jsonCast", "measureJo길이: ${mViewModel.measurejo.length()} scaleFactor: (${mViewModel.measurejo.optJSONObject(step).optLong("measure_overlay_scale_factor_x")}, ${mViewModel.measurejo.optJSONObject(step).optLong("measure_overlay_scale_factor_y")})")
        mViewModel.staticjo = JSONObject()
        // 초기화
    }

    private fun getFileName(step: Int): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
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
                        saveMediaToCache(this@MeasureSkeletonActivity, savedUri, name.toString(), true)
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

            // Create file name and content values
            val name = "MT_DYNAMIC_OVERHEADSQUAT_FRONT_1_3_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
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
                            startRecordingTimer()
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val savedUri = recordEvent.outputResults.outputUri
                            saveMediaToCache(this@MeasureSkeletonActivity, savedUri, name.toString(), false)
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

    private fun calculateScreenX(xx: Float): Int {

        val scaleFactor = max(binding.overlay.width * 1f / 720, binding.overlay.height * 1f / 1280)
        val offsetX = ((binding.overlay.width - 720 * scaleFactor) / 2) + 62

        val x = xx * binding.overlay.width / scaleFactor + offsetX
        return x.roundToInt()
    }

    private fun calculateScreenY(yy: Float): Int {
        val scaleFactor = max(binding.overlay.width * 1f / 720, binding.overlay.height * 1f / 1280)
        val offsetY = (binding.overlay.height - 1280 * scaleFactor) / 2
        val y = yy * binding.overlay.height / scaleFactor + offsetY
        return  y.roundToInt()
    }

    fun saveMediaToCache(context: Context, uri: Uri, fileName: String, isImage: Boolean) {
        try {
            val extension = if (isImage) ".jpg" else ".mp4"
            val file = File(context.cacheDir, "$fileName$extension")
            if (isImage) {
                // 이미지일 경우 720x1280 크기로 리스케일
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = File.createTempFile("tempImage", null, context.cacheDir)
                inputStream?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // 이미지를 720x1280으로 리스케일
                val scaledBitmap = decodeSampledBitmapFromFile(tempFile.absolutePath, 720, 1280)

                // 리스케일된 이미지를 캐시에 저장
                val outputStream = FileOutputStream(file)
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.flush()
                outputStream.close()

                // 임시 파일 삭제
                tempFile.delete()
            } else {
                // 비디오일 경우 그대로 캐시에 저장
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("MeasureViewModel", "Error saving image to cache: ${e.message}")
        }
    }


}