package com.tangoplus.tangoq

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
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
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.shuhart.stepview.StepView
import com.tangoplus.tangoq.callback.CaptureCallback
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.data.SkeletonViewModel
import com.tangoplus.tangoq.databinding.ActivityMeasureSkeletonBinding
import com.tangoplus.tangoq.dialog.MeasureSkeletonDialogFragment
import com.tangoplus.tangoq.listener.OnSingleClickListener
import com.tangoplus.tangoq.mediapipe.OverlayView
import com.tangoplus.tangoq.mediapipe.PoseLandmarkAdapter
import com.tangoplus.tangoq.mediapipe.PoseLandmarkerHelper
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import com.tangoplus.tangoq.service.MediaProjectionService
import org.json.JSONObject
import java.math.BigDecimal
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


class MeasureSkeletonActivity : AppCompatActivity(), PoseLandmarkerHelper.LandmarkerListener, CaptureCallback, SensorEventListener {
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

    // 사진 캡처
    private lateinit var imageCapture: ImageCapture
    private var isCapture = true
    private var isRecording = false
    private var mediaProjectionService: MediaProjectionService? = null
    private var serviceConnection: ServiceConnection? = null
    private lateinit var mediaProjectionManager: MediaProjectionManager

    // ------! 싱글턴 패턴 객체 가져오기 !------
    private lateinit var singletonInstance: Singleton_t_measure

    var latestResult: PoseLandmarkerHelper.ResultBundle? = null
    private val mViewModel : MeasureViewModel by viewModels()

    private var repeatCount = MutableLiveData(BigDecimal("0.0"))
    private val maxRepeats = BigDecimal("6.0")
    private var progress = 12
    private var serviceBound = false

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
        object : CountDownTimer(5000, 1000) {
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
                if (isRecording) { // 동영상 촬영
                    binding.tvMeasureSkeletonCount.text = "스쿼트를 실시해주세요"

                    setAnimation(binding.tvMeasureSkeletonCount, 1000, 500, false) {
                        hideViews(3700)
                        Log.v("동영상녹화", "isRecording: ${isRecording}, isCapture: ${isCapture}")
                        // 녹화 종료 시점 아님
                        mediaProjectionService?.recordScreen {
                            Log.v("녹화시작시점", "step: $repeatCount, latestResult: $latestResult")
                            isRecording = false // 녹화 완료
                            // 녹화완료가 되면서 종료되는 곳
                        }
                        // ------! 영상일 경우 바로 0.3초 단위로 값 접근 !------
                        val runnableCode: Runnable = object : Runnable {
                            override fun run() {
                                if (repeatCount == BigDecimal("2.9")) {
                                    updateUI()
                                    return
                                }
                                Log.v("녹화도중시점", "step: $repeatCount, latestResult: $latestResult")

                                Handler(Looper.getMainLooper()).postDelayed(this, 300)
                                updateUI()
                            }
                        }
                        Handler(Looper.getMainLooper()).post(runnableCode)

                    }
                } else {
                    binding.tvMeasureSkeletonCount.text = "자세를 따라해주세요"
                    hideViews(600)
                    Log.v("사진service", "isCapture: ${isCapture}, isRecording: ${isRecording}")
                    // ------! 종료 후 다시 세팅 !------
                    mediaProjectionService?.captureScreen{
                        Log.v("캡쳐종료시점", "step: $repeatCount, latestResult: $latestResult")
                        latestResult?.let { resultBundleToJson(it, repeatCount.value!!) }
                        updateUI()
                        isCapture = false
                    }

                }
                binding.btnMeasureSkeletonStep.isEnabled = true
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
            unbindService(serviceConnection as ServiceConnection)
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
        Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
            mCountDown.cancel()
        if (serviceBound) {
//            unbindService(serviceConnection as ServiceConnection)
            serviceBound = false
        }
        val serviceIntent = Intent(this, MediaProjectionService::class.java)
        stopService(serviceIntent)

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

        // ------! foreground 서비스 연결 시작 !------

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as MediaProjectionService.LocalBinder
                mediaProjectionService = binder.getService()
                mediaProjectionService!!.setCallback(this@MeasureSkeletonActivity)
                serviceBound = true
                Log.w("serviceInit1", "$mediaProjectionService")
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                mediaProjectionService = null
                serviceBound = false
                Log.w("serviceInit Failed", "$mediaProjectionService")
            }
        }
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE)
        // Bind to the service
        val intent = Intent(this, MediaProjectionService::class.java)
        bindService(intent, serviceConnection as ServiceConnection, Context.BIND_AUTO_CREATE)
        // ------! foreground 서비스 연결 끝 !------

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
//        setAnimation(binding.tvMeasureSkeletonGuide!!, 2000, 2500, false ) { }
        // 옵저버 달아놓기

        /** 사진 및 동영상 촬영 순서
         * 1. isLooping true -> repeatCount확인 -> count에 맞게 isCapture및 isRecord 선택
         * 2. mCountdown.start() -> 카운트 다운이 종료될 때 isCapture, isRecording에 따라 service 함수 실행 */
        binding.ibtnMeasureSkeletonBack.setOnClickListener {
//            val intentBack = Intent(this, MainActivity::class.java)
//            intentBack.putExtra("finishMeasure", true)
//            startActivity(intentBack)

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
                    add("팔꿉")
                    add("오버헤드")
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
                // todo 통신 시간에 따라 alertDialog 띄우던가 해야 함 ( 전송 로딩 )
                finish()
            }
            if (repeatCount.value!! <= maxRepeats) {
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
            binding.btnMeasureSkeletonStepPrevious.visibility = if (count.compareTo(BigDecimal("0.0")) == 0) {
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

    private fun setPreviousStep() {
        when (repeatCount) {
            maxRepeats -> {
                repeatCount.value = repeatCount.value?.minus(BigDecimal("1.0"))
                binding.pvMeasureSkeleton.progress -= 16  // 마지막 남은 2까지 전부 빼기
                binding.tvMeasureSkeletonCount.text = "프레임에 맞춰 서주세요"
                binding.btnMeasureSkeletonStep.text = "측정하기"
                binding.svMeasureSkeleton.go(repeatCount.value!!.toInt(), true)
                }
            else -> {
                repeatCount.value = repeatCount.value?.minus(BigDecimal("1.0"))
                progress -= 14
                binding.pvMeasureSkeleton.progress = progress
                Log.v("녹화종료되나요?", "repeatCount: ${repeatCount.value}, progress: $progress")
                binding.svMeasureSkeleton.go(repeatCount.value!!.toInt(), true)
                val drawable = ContextCompat.getDrawable(this, resources.getIdentifier("drawable_measure_${repeatCount.value!!.toInt()}", "drawable", packageName))
                binding.ivMeasureSkeletonFrame.setImageDrawable(drawable)
            }

        }
        Log.v("updateUI", "progressbar: ${progress}, repeatCount: ${repeatCount.value}")
    }

    // ------! 촬영 시 view 가리고 보이기 !-----
    private fun hideViews(delay : Long) {
        binding.clMeasureSkeletonTop.visibility = View.INVISIBLE
        binding.ivMeasureSkeletonFrame.visibility = View.INVISIBLE
        binding.llMeasureSkeletonBottom.visibility = View.INVISIBLE
        if (repeatCount != BigDecimal("2.0")) startCameraShutterAnimation()

        setAnimation(binding.clMeasureSkeletonTop, 850, delay, true) {}
        setAnimation(binding.ivMeasureSkeletonFrame, 850, delay, true) {}
        setAnimation(binding.llMeasureSkeletonBottom, 850, delay, true) {}
        binding.btnMeasureSkeletonStep.visibility = View.VISIBLE
        binding.tvMeasureSkeletonCount.text = "프레임에 맞춰 서주세요"
    }

    // ------! 타이머 control 시작 !------
    private fun startTimer() {
        // 시작 버튼 후 시작
        binding.btnMeasureSkeletonStep.isEnabled = false

        when (repeatCount) {
            BigDecimal("2.0") -> {
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

    // ------! update UI 시작 !------
    private fun updateUI() {
        when (repeatCount.value!!) {
            in BigDecimal("2.0") .. BigDecimal("2.8") -> {
                binding.tvMeasureSkeletonCount.text = "스쿼트를 실시해주세요"
                repeatCount.value = repeatCount.value!!.plus(BigDecimal("0.1"))
            }
            maxRepeats -> {
                binding.pvMeasureSkeleton.progress = 100
                binding.tvMeasureSkeletonCount.text = "측정이 완료됐습니다 !"
                binding.btnMeasureSkeletonStep.text = "완료하기"
                mCountDown.cancel()
                Log.v("repeat", "Max repeats reached, stopping the loop")
            }
            BigDecimal("2.9") -> {
                repeatCount.value = repeatCount.value!!.plus(BigDecimal("0.1"))
                progress += 14
                binding.pvMeasureSkeleton.progress = progress

                binding.svMeasureSkeleton.go(repeatCount.value!!.toInt(), true)
                val drawable = ContextCompat.getDrawable(this, resources.getIdentifier("drawable_measure_${repeatCount.value!!.toInt()}", "drawable", packageName))
                binding.ivMeasureSkeletonFrame.setImageDrawable(drawable)
            }
            else -> {
                repeatCount.value = repeatCount.value!!.plus(BigDecimal("1.0"))
                progress += 14
                binding.pvMeasureSkeleton.progress = progress
                Log.v("녹화종료되나요?", "repeatCount: ${repeatCount.value}, progress: $progress")
                binding.svMeasureSkeleton.go(repeatCount.value!!.toInt(), true)
                binding.tvMeasureSkeletonCount.visibility = View.VISIBLE
                binding.tvMeasureSkeletonCount.text = "다음 동작을 준비해주세요"
                val drawable = ContextCompat.getDrawable(this, resources.getIdentifier("drawable_measure_${repeatCount.value!!.toInt()}", "drawable", packageName))
                binding.ivMeasureSkeletonFrame.setImageDrawable(drawable)
            }

        }
        Log.v("updateUI", "progressbar: ${progress}, repeatCount: ${repeatCount}")

    }
    // ------! update UI 끝 !------

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
        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9)
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
                        image.close()
                    }
                }
        // 이미지 캡처 설정
        imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .build()
//        videoCapture = VideoCapture.withOutput(Recorder.Builder()
//            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
//            .build()
//        )
        // ------! 카메라에 poselandmarker 그리기 !------
        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()
        try {
            // 여기에는 다양한 사용 사례가 전달될 수 있습니다.
            // 카메라는 CameraControl 및 CameraInfo에 대한 액세스를 제공합니다. // TODO 기능 추가할 경우 여기다가 선언한 기능 넣어야 함
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer, imageCapture
            )
            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectPose(imageProxy: ImageProxy) {
        if(this::poseLandmarkerHelper.isInitialized) {
            poseLandmarkerHelper.detectLiveStream(
                imageProxy = imageProxy,
                isFrontCamera = cameraFacing == CameraSelector.LENS_FACING_FRONT
            )
        }
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
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val intent = Intent(this, MediaProjectionService::class.java)
            intent.putExtra(MediaProjectionService.EXTRA_RESULT_CODE, resultCode)
            intent.putExtra(MediaProjectionService.EXTRA_PROJECTION_DATA, data)
            startService(intent)
        }
    }
    override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        runOnUiThread {
            if (_binding != null) {
                // Pass necessary information to OverlayView for drawing on the canvas
                val customResult = PoseLandmarkAdapter.toCustomPoseLandmarkResult(resultBundle.results.first())
                binding.overlay.setResults(
                    customResult,
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    OverlayView.RunningMode.LIVE_STREAM
                )

                // 여기에 resultbundle(poselandarkerhelper.resultbundle)이 들어감.

                binding.overlay.invalidate()
                latestResult = resultBundle
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

    private fun calculateBalanceScore(radian : Double) : Double {
        val angleRadian = radianToDegree(radian)
        return ( angleRadian / 180 ) * 100
    }



    // override 로 resultbundle이 계속 나오는데 해당 항목을 전역변수 latest
    fun resultBundleToJson(resultBundle: PoseLandmarkerHelper.ResultBundle, step: BigDecimal) {
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
            *
            * */


            // ------!   !------
            when (step) {
                BigDecimal("0.0") -> {


                    val earAngle : Double = calculateSlope(earData[0].first, earData[0].second, earData[1].first, earData[1].second)
                    val shoulderAngle : Double = calculateSlope(shoulderData[0].first, shoulderData[0].second, shoulderData[1].first, shoulderData[1].second)
                    val elbowAngle : Double = calculateSlope(elbowData[0].first, elbowData[0].second, elbowData[1].first, elbowData[1].second)
                    val wristAngle : Double = calculateSlope(wristData[0].first, wristData[0].second, wristData[1].first, wristData[1].second)
                    val hipAngle : Double = calculateSlope(hipData[0].first, hipData[0].second, hipData[1].first, hipData[1].second)
                    val kneeAngle : Double = calculateSlope(kneeData[0].first, kneeData[0].second, kneeData[1].first, kneeData[1].second)
                    val ankleAngle : Double = calculateSlope(ankleData[0].first, ankleData[0].second, ankleData[1].first, ankleData[1].second)
                    mViewModel.jo.put("front_horizontal_angleh_ear", earAngle)
                    mViewModel.jo.put("front_horizontal_angle_shoulder", shoulderAngle)
                    mViewModel.jo.put("front_horizontal_angle_elbow", elbowAngle)
                    mViewModel.jo.put("front_horizontal_angle_wrist", wristAngle)
                    mViewModel.jo.put("front_horizontal_angle_hip", hipAngle)
                    mViewModel.jo.put("front_horizontal_angle_knee", kneeAngle)
                    mViewModel.jo.put("front_horizontal_angle_ankle", ankleAngle)


                    // 부위 양 높이 차이
                    val earDistance : Double = abs(earData[0].second.minus(earData[1].second))
                    val shoulderDistance : Double = abs(shoulderData[0].second.minus(shoulderData[1].second))
                    val elbowDistance : Double = abs(elbowData[0].second.minus(elbowData[1].second))
                    val wristDistance : Double = abs(wristData[0].second.minus(wristData[1].second))
                    val hipDistance : Double = abs(hipData[0].second.minus(hipData[1].second))
                    val kneeDistance : Double = abs(kneeData[0].second.minus(kneeData[1].second))
                    val ankleDistance : Double = abs(ankleData[0].second.minus(ankleData[1].second))
                    mViewModel.jo.put("front_horizontal_distance_sub_ear", earDistance)
                    mViewModel.jo.put("front_horizontal_distance_sub_shoulder", shoulderDistance)
                    mViewModel.jo.put("front_horizontal_distance_sub_elbow", elbowDistance)
                    mViewModel.jo.put("front_horizontal_distance_sub_wrist", wristDistance)
                    mViewModel.jo.put("front_horizontal_distance_sub_hip", hipDistance)
                    mViewModel.jo.put("front_horizontal_distance_sub_knee", kneeDistance)
                    mViewModel.jo.put("front_horizontal_distance_sub_ankle", ankleDistance)

                    // 각 부위 x축 부터 거리 거리
                    val wristSubDistanceByX : Pair<Double, Double> = Pair(abs(wristData[0].first.minus(ankleXAxis)), abs(wristData[1].first.minus(ankleXAxis)))
                    val kneeSubDistanceByX : Pair<Double, Double> = Pair(abs(kneeData[0].first.minus(ankleXAxis)), abs(kneeData[1].first.minus(ankleXAxis)))
                    val ankleSubDistanceByX : Pair<Double, Double> = Pair(abs(ankleData[0].first.minus(ankleXAxis)), abs(ankleData[1].first.minus(ankleXAxis)))
                    val toeSubDistance : Pair<Double, Double> = Pair(abs(toeData[0].first.minus(ankleXAxis)), abs(toeData[1].first.minus(ankleXAxis)))
                    mViewModel.jo.put("front_horizontal_distance_wrist_left", wristSubDistanceByX.first)
                    mViewModel.jo.put("front_horizontal_distance_wrist_right", wristSubDistanceByX.second)
                    mViewModel.jo.put("front_horizontal_distance_knee_left", kneeSubDistanceByX.first)
                    mViewModel.jo.put("front_horizontal_distance_knee_right", kneeSubDistanceByX.second)
                    mViewModel.jo.put("front_horizontal_distance_ankle_left", ankleSubDistanceByX.first)
                    mViewModel.jo.put("front_horizontal_distance_ankle_right", ankleSubDistanceByX.second)
                    mViewModel.jo.put("front_horizontal_distance_toe_left", toeSubDistance.first)
                    mViewModel.jo.put("front_horizontal_distance_toe_right", toeSubDistance.second)


                    // 팔 각도
                    val shoulderElbowLean: Pair<Double, Double> = Pair(calculateSlope(shoulderData[0].first, shoulderData[0].second, elbowData[0].first, elbowData[0].second), calculateSlope(shoulderData[1].first, shoulderData[1].second, elbowData[1].first, elbowData[1].second))
                    val elbowWristLean: Pair<Double, Double> = Pair(calculateSlope(elbowData[0].first, elbowData[0].second, wristData[0].first, wristData[0].second), calculateSlope(elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second))
                    val wristThumbLean: Pair<Double, Double> = Pair(calculateSlope(wristData[0].first, wristData[0].second, thumbData[0].first, thumbData[0].second), calculateSlope(wristData[1].first, wristData[1].second, thumbData[1].first, thumbData[1].second))
                    val hipKneeLean : Pair<Double, Double> = Pair(calculateSlope(hipData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second), calculateSlope(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second))
                    val kneeAnkleLean : Pair<Double, Double> = Pair(calculateSlope(kneeData[0].first, kneeData[0].second, ankleData[0].first, ankleData[0].second), calculateSlope(kneeData[1].first, kneeData[1].second, ankleData[1].first, ankleData[1].second))
                    val ankleToeLean : Pair<Double, Double> = Pair(calculateSlope(ankleData[0].first, ankleData[0].second, toeData[0].first, toeData[0].second), calculateSlope(ankleData[1].first, ankleData[1].second, toeData[1].first, toeData[1].second))
                    mViewModel.jo.put("front_vertical_angle_shoulder_elbow_left", shoulderElbowLean.first)
                    mViewModel.jo.put("front_vertical_angle_shoulder_elbow_right", shoulderElbowLean.second)
                    mViewModel.jo.put("front_vertical_angle_elbow_wrist_left", elbowWristLean.first)
                    mViewModel.jo.put("front_vertical_angle_elbow_wrist_right", elbowWristLean.second)
                    mViewModel.jo.put("front_vertical_angle_wrist_thumb_left", wristThumbLean.first)
                    mViewModel.jo.put("front_vertical_angle_wrist_thumb_right", wristThumbLean.second)
                    mViewModel.jo.put("front_vertical_angle_hip_knee_left", hipKneeLean.first)
                    mViewModel.jo.put("front_vertical_angle_hip_knee_right", hipKneeLean.second)
                    mViewModel.jo.put("front_vertical_angle_knee_ankle_left", kneeAnkleLean.first)
                    mViewModel.jo.put("front_vertical_angle_knee_ankle_right", kneeAnkleLean.second)
                    mViewModel.jo.put("front_vertical_angle_ankle_toe_left", ankleToeLean.first)
                    mViewModel.jo.put("front_vertical_angle_ankle_toe_right", ankleToeLean.second)
                    saveJsonToSingleton(step, mViewModel.jo)

                    val shoulderElbowWristAngle : Pair<Double, Double> = Pair(calculateAngle(shoulderData[0].first, shoulderData[0].second, elbowData[0].first, elbowData[0].second, wristData[0].first, wristData[0].second),
                        calculateAngle(shoulderData[1].first, shoulderData[1].second, elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second))
                    val hipKneeAnkleAngle : Pair<Double, Double> = Pair(calculateAngle(hipData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second, ankleData[0].first, ankleData[0].second),
                        calculateAngle(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second, ankleData[1].first, ankleData[1].second))
                    mViewModel.jo.put("front_vertical_angle_shoulder_elbow_wrist_left", shoulderElbowWristAngle.first)
                    mViewModel.jo.put("front_vertical_angle_shoulder_elbow_wrist_right", shoulderElbowWristAngle.second)
                    mViewModel.jo.put("front_vertical_angle_hip_knee_ankle_left", hipKneeAnkleAngle.first)
                    mViewModel.jo.put("front_vertical_angle_hip_knee_ankle_right", hipKneeAnkleAngle.second)

                }
                BigDecimal("1.0") -> { // ------! 주먹 쥐고 !------
                    val wristShoulderDistance : Pair<Double, Double> = Pair(calculateDistanceByDots(wristData[0].first, wristData[0].second, shoulderData[0].first, shoulderData[0].second),
                        calculateDistanceByDots(wristData[1].first, wristData[1].second, shoulderData[1].first, shoulderData[1].second))
                    val wristsDistanceByY : Double = abs(wristData[0].second - wristData[1].second)
                    val indexWristElbowAngle : Pair<Double, Double> = Pair(calculateAngle(indexData[0].first, indexData[0].second, wristData[0].first, wristData[0].second, elbowData[0].first, elbowData[0].second),
                        calculateAngle(indexData[1].first, indexData[1].second, wristData[1].first, wristData[1].second, elbowData[1].first, elbowData[1].second))
                    val shoulderElbowWristAngle : Pair<Double, Double> = Pair(calculateAngle(shoulderData[0].first, shoulderData[0].second, elbowData[0].first, elbowData[0].second, wristData[0].first, wristData[0].second),
                        calculateAngle(shoulderData[1].first, shoulderData[1].second, elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second))
                    val indexDistanceByX: Pair<Double, Double> = Pair(abs(indexData[0].first.minus(ankleXAxis)), abs(indexData[1].first.minus(ankleXAxis)))
                    val wristDistanceByX: Pair<Double, Double> = Pair(abs(wristData[0].first.minus(ankleXAxis)), abs(wristData[1].first.minus(ankleXAxis)))

                    mViewModel.jo.put("front_elbow_align_distance_left_wrist_shoulder", wristShoulderDistance.first)
                    mViewModel.jo.put("front_elbow_align_distance_right_wrist_shoulder", wristShoulderDistance.second)
                    mViewModel.jo.put("front_elbow_align_distance_wrist_height", wristsDistanceByY)
                    mViewModel.jo.put("front_elbow_align_angle_mid_index_wrist_elbow_left", indexWristElbowAngle.first)
                    mViewModel.jo.put("front_elbow_align_angle_mid_index_wrist_elbow_rightt", indexWristElbowAngle.second)
                    mViewModel.jo.put("front_elbow_align_angle_left_shoulder_elbow_wrist", shoulderElbowWristAngle.first)
                    mViewModel.jo.put("front_elbow_align_angle_right_shoulder_elbow_wrist", shoulderElbowWristAngle.second)
                    mViewModel.jo.put("front_elbow_align_distance_center_mid_finger_left", indexDistanceByX.first)
                    mViewModel.jo.put("front_elbow_align_distance_center_mid_finger_right", indexDistanceByX.second)
                    mViewModel.jo.put("front_elbow_align_distance_center_wrist_left", wristDistanceByX.first)
                    mViewModel.jo.put("front_elbow_align_distance_center_wrist_right", wristDistanceByX.second)
                    saveJsonToSingleton(step, mViewModel.jo)
                }
                in BigDecimal("2.0") .. BigDecimal("2.9") -> { // 스쿼트

                    // ------# 각도 타임으로 넣기 #------
                    val elbowAngleDistance : Pair<Double, Double> = Pair(calculateSlope(elbowData[0].first, elbowData[0].second, elbowData[1].first, elbowData[1].second), abs(elbowData[0].second.minus(elbowData[1].second)))
                    val shoulderAngleDistance : Pair<Double, Double> = Pair(calculateSlope(shoulderData[0].first, shoulderData[0].second, shoulderData[1].first, shoulderData[1].second), abs(shoulderData[0].second.minus(shoulderData[1].second)))
                    val wristAngleDistance : Pair<Double, Double> = Pair(calculateSlope(wristData[0].first, wristData[0].second, wristData[1].first, wristData[1].second), abs(wristData[0].second.minus(wristData[1].second)))
                    val hipAngleDistance : Pair<Double, Double> = Pair(calculateSlope(hipData[0].first, hipData[0].second, hipData[1].first, hipData[1].second), abs(hipData[0].second.minus(hipData[1].second)))
                    val kneeAngleDistance : Pair<Double, Double> = Pair(calculateSlope(kneeData[0].first, kneeData[0].second, kneeData[1].first, kneeData[1].second), abs(kneeData[0].second.minus(hipData[1].second)))
                    val kneeSubDistanceByX : Pair<Double, Double> = Pair(abs(kneeData[0].first.minus(ankleXAxis)), abs(kneeData[1].first.minus(ankleXAxis)))
                    val wristElbowShoulderAngle : Pair<Double, Double> = Pair(calculateAngle(wristData[0].first, wristData[0].second, elbowData[0].first, elbowData[0].second, shoulderData[0].first, shoulderData[0].second),
                        calculateAngle(wristData[1].first, wristData[1].second, elbowData[1].first, elbowData[1].second, shoulderData[1].first, shoulderData[1].second))
                    val wristElbowAngle : Pair<Double, Double> = Pair(calculateSlope(wristData[0].first, wristData[0].second, elbowData[0].first, elbowData[0].second),
                        calculateSlope(wristData[1].first, wristData[1].second, elbowData[1].first, elbowData[1].second))
                    val elbowShoulderAngle : Pair<Double, Double> = Pair(calculateSlope(elbowData[0].first, elbowData[0].second, shoulderData[0].first, shoulderData[0].second),
                        calculateSlope(elbowData[1].first, elbowData[1].second, shoulderData[1].first, shoulderData[1].second))
                    val hipKneeAngle : Pair<Double, Double> = Pair(calculateSlope(hipData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second),
                        calculateSlope(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second))
                    val wristSubDistanceByX : Pair<Double, Double> = Pair(abs(wristData[0].first.minus(ankleXAxis)), abs(wristData[1].first.minus(ankleXAxis)))

                    mViewModel.dynamicJo?.put("ohs_front_horizontal_angle_elbow", elbowAngleDistance.first)
                    mViewModel.dynamicJo?.put("ohs_front_horizontal_distance_elbow", elbowAngleDistance.second)
                    mViewModel.dynamicJo?.put("ohs_front_horizontal_angle_shoulder", shoulderAngleDistance.first)
                    mViewModel.dynamicJo?.put("ohs_front_horizontal_distance_shoulder", shoulderAngleDistance.second)
                    mViewModel.dynamicJo?.put("ohs_front_horizontal_distance_wrist", wristAngleDistance.first)
                    mViewModel.dynamicJo?.put("ohs_front_horizontal_angle_mid_finger_tip", wristAngleDistance.second)
                    mViewModel.dynamicJo?.put("ohs_front_horizontal_angle_hip", hipAngleDistance.first)
                    mViewModel.dynamicJo?.put("ohs_front_horizontal_distance_hip", hipAngleDistance.second)
                    mViewModel.dynamicJo?.put("ohs_front_horizontal_angle_knee", kneeAngleDistance.first)
                    mViewModel.dynamicJo?.put("ohs_front_horizontal_distance_knee", kneeAngleDistance.second)
                    mViewModel.dynamicJo?.put("ohs_front_horizontal_distance_center_left_knee", kneeSubDistanceByX.first)
                    mViewModel.dynamicJo?.put("ohs_front_horizontal_distance_center_right_knee", kneeSubDistanceByX.second)
                    mViewModel.dynamicJo?.put("ohs_front_vertical_angle_wrist_elbow_shoulder_left", wristElbowShoulderAngle.first)
                    mViewModel.dynamicJo?.put("ohs_front_vertical_angle_wrist_elbow_shoulder_right", wristElbowShoulderAngle.second)
                    mViewModel.dynamicJo?.put("ohs_front_vertical_angle_wrist_elbow_left", wristElbowAngle.first)
                    mViewModel.dynamicJo?.put("ohs_front_vertical_angle_wrist_elbow_right", wristElbowAngle.second)
                    mViewModel.dynamicJo?.put("ohs_front_vertical_angle_elbow_shoulder_left", elbowShoulderAngle.first)
                    mViewModel.dynamicJo?.put("ohs_front_vertical_angle_elbow_shoulder_right", elbowShoulderAngle.second)
                    mViewModel.dynamicJo?.put("ohs_front_vertical_angle_hip_knee_left", hipKneeAngle.first)
                    mViewModel.dynamicJo?.put("ohs_front_vertical_angle_hip_knee_right", hipKneeAngle.second)
                    mViewModel.dynamicJo?.put("ohs_front_horizontal_distance_center_left_wrist", wristSubDistanceByX.first)
                    mViewModel.dynamicJo?.put("ohs_front_horizontal_distance_center_right_wrist", wristSubDistanceByX.second)

                    mViewModel.jo.put("$step",mViewModel.dynamicJo)
                    // step이 올라갈 때마다 넣고 총 10번이 됐을 때 save하고 나가짐.
                    if (step == BigDecimal("2.9")) {
                        saveJsonToSingleton(BigDecimal("$step"), mViewModel.jo)
                    }
                    mViewModel.dynamicJo = null
                }
                BigDecimal("3.0") -> { // 왼쪽보기 (오른쪽 팔)

                    // ------! 측면 거리  - 왼쪽 !------
                    val sideLeftShoulderDistance : Double = abs(shoulderData[0].first.minus(ankleXAxis))
                    val sideLeftWristDistance : Double = abs(wristData[0].first.minus(ankleXAxis))
                    val sideLeftPinkyDistance : Double = abs(pinkyData[0].first.minus(ankleXAxis))
                    val sideLeftHipDistance : Double = abs(hipData[0].first.minus(ankleXAxis))
                    mViewModel.jo.put("side_left_horizontal_distance_shoulder", sideLeftShoulderDistance)
                    mViewModel.jo.put("side_left_horizontal_distance_hip", sideLeftWristDistance)
                    mViewModel.jo.put("side_left_horizontal_distance_pinky", sideLeftPinkyDistance)
                    mViewModel.jo.put("side_left_horizontal_distance_wrist", sideLeftHipDistance)


                    val sideLeftShoulderElbowLean : Double = calculateSlope(shoulderData[0].first, shoulderData[0].second, kneeData[0].first, kneeData[0].second)
                    val sideLeftElbowWristLean: Double =calculateSlope(elbowData[0].first, elbowData[0].second, wristData[0].first, wristData[0].second)
                    val sideLeftHipKneeLean : Double = calculateSlope(hipData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second)
                    val sideLeftEarShoulderLean : Double = calculateSlope(earData[0].first, earData[0].second, shoulderData[0].first, shoulderData[0].second)
                    val sideLeftNoseShoulderLean : Double = calculateSlope(nose.first, nose.second, shoulderData[0].first, shoulderData[0].second)
                    val sideLeftShoulderElbowWristAngle : Double = calculateAngle(shoulderData[0].first, shoulderData[0].second, elbowData[0].first, elbowData[0].second, wristData[0].first, wristData[0].second)
                    val sideLeftHipKneeAnkleAngle : Double = calculateAngle(hipData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second, ankleData[0].first, ankleData[0].second)

                    mViewModel.jo.put("side_left_vertical_angle_shoulder_elbow", sideLeftShoulderElbowLean  % 180)
                    mViewModel.jo.put("side_left_vertical_angle_elbow_wrist", sideLeftElbowWristLean  % 180)
                    mViewModel.jo.put("side_left_vertical_angle_hip_knee", sideLeftHipKneeLean  % 180)
                    mViewModel.jo.put("side_left_vertical_angle_ear_shoulder", sideLeftEarShoulderLean  % 180)
                    mViewModel.jo.put("side_left_vertical_angle_nose_shoulder", sideLeftNoseShoulderLean  % 180)
                    mViewModel.jo.put("side_left_vertical_angle_shoulder_elbow_wrist", sideLeftShoulderElbowWristAngle  % 180)
                    mViewModel.jo.put("side_left_vertical_angle_hip_knee_ankle", sideLeftHipKneeAnkleAngle  % 180)

                    saveJsonToSingleton(step, mViewModel.jo)
                }
                BigDecimal("4.0") -> { // 오른쪽보기 (왼쪽 팔)
                    // ------! 측면 거리  - 오른쪽 !------

                    val sideRightShoulderDistance : Double = abs(shoulderData[1].first.minus(ankleXAxis))
                    val sideRightWristDistance : Double = abs(wristData[1].first.minus(ankleXAxis))
                    val sideRightPinkyDistance : Double = abs(pinkyData[1].first.minus(ankleXAxis))
                    val sideRightHipDistance : Double = abs(hipData[1].first.minus(ankleXAxis))
                    mViewModel.jo.put("side_right_horizontal_distance_shoulder", sideRightShoulderDistance)
                    mViewModel.jo.put("side_right_horizontal_distance_hip", sideRightWristDistance)
                    mViewModel.jo.put("side_right_horizontal_distance_pinky", sideRightPinkyDistance)
                    mViewModel.jo.put("side_right_horizontal_distance_wrist", sideRightHipDistance)

                    // ------! 측면 기울기  - 오른쪽 !------
                    val sideRightShoulderElbowLean : Double = calculateSlope(shoulderData[1].first, shoulderData[1].second, kneeData[1].first, kneeData[1].second)
                    val sideRightElbowWristLean: Double =calculateSlope(elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second)
                    val sideRightHipKneeLean : Double = calculateSlope(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second)
                    val sideRightEarShoulderLean : Double = calculateSlope(earData[1].first, earData[1].second, shoulderData[1].first, shoulderData[1].second)
                    val sideRightNoseShoulderLean : Double = calculateSlope(nose.first, nose.second, shoulderData[1].first, shoulderData[1].second)
                    val sideRightShoulderElbowWristAngle : Double = calculateAngle(shoulderData[1].first, shoulderData[1].second, elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second)
                    val sideRightHipKneeAnkleAngle : Double = calculateAngle(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second, ankleData[1].first, ankleData[1].second)

                    mViewModel.jo.put("side_right_vertical_angle_shoulder_elbow", sideRightShoulderElbowLean )
                    mViewModel.jo.put("side_right_vertical_angle_elbow_wrist", sideRightElbowWristLean )
                    mViewModel.jo.put("side_right_vertical_angle_hip_knee", sideRightHipKneeLean)
                    mViewModel.jo.put("side_right_vertical_angle_ear_shoulder", sideRightEarShoulderLean  % 180)
                    mViewModel.jo.put("side_right_vertical_angle_nose_shoulder", sideRightNoseShoulderLean  % 180)
                    mViewModel.jo.put("side_right_vertical_angle_shoulder_elbow_wrist", sideRightShoulderElbowWristAngle  % 180)
                    mViewModel.jo.put("side_right_vertical_angle_hip_knee_ankle", sideRightHipKneeAnkleAngle  % 180)
                    saveJsonToSingleton(step , mViewModel.jo)
                }
                BigDecimal("5.0") -> { // ------! 후면 서서 !-------
                    val backEarAngle : Double = calculateSlope(earData[0].first, earData[0].second, earData[1].first, earData[1].second)
                    val backShoulderAngle : Double = calculateSlope(shoulderData[0].first, shoulderData[0].second, shoulderData[1].first, shoulderData[1].second)
                    val backWristAngle : Double = calculateSlope(wristData[0].first, wristData[0].second, wristData[1].first, wristData[1].second)
                    val backElbowAngle : Double = calculateSlope(elbowData[0].first, elbowData[0].second, elbowData[1].first, elbowData[1].second)
                    val backHipAngle : Double = calculateSlope(hipData[0].first, hipData[0].second, hipData[1].first, hipData[1].second)
                    val backKneeAngle : Double = calculateSlope(kneeData[0].first, kneeData[0].second, kneeData[1].first, kneeData[1].second)
                    val backAnkleAngle : Double = calculateSlope(ankleData[0].first, ankleData[0].second, ankleData[1].first, ankleData[1].second)
                    mViewModel.jo.put("back_horizontal_angle_ear", backEarAngle % 180)
                    mViewModel.jo.put("back_horizontal_angle_shoulder", backShoulderAngle % 180)
                    mViewModel.jo.put("back_horizontal_angle_wrist", backWristAngle % 180)
                    mViewModel.jo.put("back_horizontal_angle_elbow", backElbowAngle % 180)
                    mViewModel.jo.put("back_horizontal_angle_hip", backHipAngle % 180)
                    mViewModel.jo.put("back_horizontal_angle_knee", backKneeAngle % 180)
                    mViewModel.jo.put("back_horizontal_angle_ankle", backAnkleAngle % 180)

                    // ------! 후면 거리 !------
                    val backEarDistance : Double = abs(earData[0].second.minus(earData[1].second))
                    val backShoulderDistance  : Double = abs(shoulderData[0].second.minus(shoulderData[1].second))
                    val backElbowDistance  : Double = abs(shoulderData[0].second.minus(shoulderData[1].second))
                    val backWristDistance  : Double = abs(shoulderData[0].second.minus(shoulderData[1].second))
                    val backHipDistance  : Double = abs(shoulderData[0].second.minus(shoulderData[1].second))
                    val backKneeDistance  : Double = abs(shoulderData[0].second.minus(shoulderData[1].second))
                    val backAnkleDistance  : Double = abs(ankleData[0].second.minus(ankleData[1].second))

                    mViewModel.jo.put("back_horizontal_distance_sub_ear", backEarDistance)
                    mViewModel.jo.put("back_horizontal_distance_sub_shoulder", backShoulderDistance)
                    mViewModel.jo.put("back_horizontal_distance_sub_elbow", backElbowDistance)
                    mViewModel.jo.put("back_horizontal_distance_sub_wrist", backWristDistance)
                    mViewModel.jo.put("back_horizontal_distance_sub_hip", backHipDistance)
                    mViewModel.jo.put("back_horizontal_distance_sub_knee", backKneeDistance)
                    mViewModel.jo.put("back_horizontal_distance_sub_ankle", backAnkleDistance)
                    val backKneeDistanceByX : Pair<Double,Double> = Pair(abs(kneeData[0].first.minus(ankleXAxis)), abs(kneeData[1].first.minus(ankleXAxis)))
                    val backHeelDistanceByX : Pair<Double, Double> = Pair(abs(heelData[0].first.minus(ankleXAxis)), abs(heelData[1].first.minus(ankleXAxis)))
                    mViewModel.jo.put("back_horizontal_distance_knee_left", backKneeDistanceByX.first)
                    mViewModel.jo.put("back_horizontal_distance_knee_right", backKneeDistanceByX.second)
                    mViewModel.jo.put("back_horizontal_distance_heel_left", backHeelDistanceByX.first)
                    mViewModel.jo.put("back_horizontal_distance_heel_right", backHeelDistanceByX.second)


                    val backNoseShoulderLean : Double = calculateSlope(nose.first, nose.second, middleShoulder.first, middleShoulder.second)
                    val backShoulderHipLean : Double = calculateSlope(middleShoulder.first, middleShoulder.second, abs(hipData[0].first - hipData[1].first), abs(hipData[0].second - hipData[1].second))
                    val backNoseHipLean : Double = calculateSlope(nose.first, nose.second, hipData[0].first, abs(hipData[0].second - hipData[1].second))
                    val backKneeHeelLean : Pair<Double, Double> = Pair(calculateSlope(heelData[0].first, heelData[0].second, kneeData[0].first, kneeData[0].second), calculateSlope(heelData[1].first, heelData[1].second, kneeData[1].first, kneeData[1].second))
                    val backWristDistanceByX : Pair<Double, Double> = Pair(abs(wristData[0].first.minus(ankleXAxis)), abs(wristData[1].first.minus(ankleXAxis)))

                    mViewModel.jo.put("back_vertical_angle_nose_center_shoulder", backNoseShoulderLean)
                    mViewModel.jo.put("back_vertical_angle_shoudler_center_hip", backShoulderHipLean)
                    mViewModel.jo.put("back_vertical_angle_nose_center_hip", backNoseHipLean)
                    mViewModel.jo.put("back_vertical_angle_knee_heel_left", backKneeHeelLean.first)
                    mViewModel.jo.put("back_vertical_angle_knee_heel_right", backKneeHeelLean.second)
                    mViewModel.jo.put("back_horizontal_distance_wrist_left", backWristDistanceByX.first)
                    mViewModel.jo.put("back_horizontal_distance_wrist_right", backWristDistanceByX.second)
                    saveJsonToSingleton(step, mViewModel.jo)


                    // 필요한 밸런스 점수 담기
                    val joScore = JSONObject()
                    joScore.put("result_balance_score_angle_ankle", calculateBalanceScore(backAnkleAngle))

                    saveJsonToSingleton(BigDecimal("10.0"), joScore)
                }
                BigDecimal("6.0") -> { // ------! 앉았을 때 !------


                    // ------! 의자 후면 거리, 양쪽 부위 높이 차이 - y값 차이 (절댓값)!------
                    val sitBackEarDistance : Double = abs(earData[0].second.minus(earData[1].second))
                    val sitBackShoulderDistance  : Double = abs(shoulderData[0].second.minus(shoulderData[1].second))
                    val sitBackHipDistance  : Double = abs(hipData[0].second.minus(hipData[1].second))
                    mViewModel.jo.put("back_sit_horizontal_distance_sub_ear", sitBackEarDistance)
                    mViewModel.jo.put("back_sit_horizontal_distance_sub_shoulder", sitBackShoulderDistance)
                    mViewModel.jo.put("back_sit_horizontal_distance_sub_hip", sitBackHipDistance)
                    // ------! 의자 후면 기울기 !------
                    val sitBackEarAngle = calculateSlope(earData[0].first, earData[0].second, earData[1].first, earData[1].second)
                    val sitBackShoulderAngle = calculateSlope(shoulderData[0].first, shoulderData[0].second, shoulderData[1].first, shoulderData[1].second)
                    val sitBackHipAngle = calculateSlope(hipData[0].first, hipData[0].second, hipData[1].first, hipData[1].second)
                    mViewModel.jo.put("back_sit_horizontal_angle_ear", sitBackEarAngle)
                    mViewModel.jo.put("back_sit_horizontal_angle_shoulder", sitBackShoulderAngle)
                    mViewModel.jo.put("back_sit_horizontal_angle_hip", sitBackHipAngle)

                    val shoulderNoseTriangleAngle : Triple<Double, Double, Double> = Triple(calculateAngle(nose.first, nose.second, shoulderData[0].first, shoulderData[0].second, shoulderData[1].first , shoulderData[1].second),
                        calculateAngle(shoulderData[0].first, shoulderData[0].second,  shoulderData[1].first , shoulderData[1].second, nose.first, nose.second),
                        calculateAngle(shoulderData[1].first , shoulderData[1].second, nose.first, nose.second, shoulderData[0].first, shoulderData[0].second))
                    val shoulderHipTriangleAngle : Triple<Double, Double, Double> = Triple(calculateAngle(shoulderData[0].first, shoulderData[0].second, middleHip.first ,middleHip.second, shoulderData[1].first , shoulderData[1].second),
                        calculateAngle(middleHip.first ,middleHip.second, shoulderData[1].first , shoulderData[1].second, shoulderData[0].first, shoulderData[0].second),
                        calculateAngle(shoulderData[1].first , shoulderData[1].second, shoulderData[0].first,shoulderData[0].second, middleHip.first ,middleHip.second ))
                    val shoulderHipRadian = calculateAngleByLine(shoulderData[0].first, shoulderData[0].second, shoulderData[1].first , shoulderData[1].second, middleHip.first, middleHip.second)

                    mViewModel.jo.put("back_sit_vertical_angle_nose_left_shoulder_right_shoulder", shoulderNoseTriangleAngle.first)
                    mViewModel.jo.put("back_sit_vertical_angle_left_shoulder_right_shoulder_nose", shoulderNoseTriangleAngle.second)
                    mViewModel.jo.put("back_sit_vertical_angle_right_shoulder_left_shoulder_nose", shoulderNoseTriangleAngle.third)
                    mViewModel.jo.put("back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder", shoulderHipTriangleAngle.first)
                    mViewModel.jo.put("back_sit_vertical_angle_center_hip_right_shoulder_left_shoulder", shoulderHipTriangleAngle.second)
                    mViewModel.jo.put("back_sit_vertical_angle_right_shoulder_left_shoulder_center_hip", shoulderHipTriangleAngle.third)
                    mViewModel.jo.put("back_sit_vertical_angle_shoulder_center_hip", shoulderHipRadian)

                    saveJsonToSingleton(step, mViewModel.jo)
                    Log.v("6단계", "${mViewModel.jo}")
                }
            }
        }
//
    }
    private fun saveJsonToSingleton(step: BigDecimal, jsonObj: JSONObject ) {
//        singletonInstance.jsonObject?.put(step.toString(), jsonObj)
//        Log.v("싱글턴", "${singletonInstance.jsonObject}")
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

    override fun onCaptureComplete(filePath: String) {
        // 싱글턴에 주소만 따로 저장.
        val jo = JSONObject()
        jo.put("fileName", filePath)
        saveJsonToSingleton(repeatCount.value!!, jo)
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
}