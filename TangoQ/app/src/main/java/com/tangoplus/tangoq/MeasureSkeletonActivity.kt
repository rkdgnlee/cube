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
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.OptIn
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.shuhart.stepview.StepView
import com.tangoplus.tangoq.callback.CaptureCallback
import com.tangoplus.tangoq.data.HistoryVO
import com.tangoplus.tangoq.data.SkeletonViewModel
import com.tangoplus.tangoq.databinding.ActivityMeasureSkeletonBinding
import com.tangoplus.tangoq.dialog.MeasureSkeletonDialogFragment
import com.tangoplus.tangoq.mediapipe.PoseLandmarkerHelper
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import com.tangoplus.tangoq.service.MediaProjectionService
import org.json.JSONObject
import java.math.BigDecimal
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)


class MeasureSkeletonActivity : AppCompatActivity(), PoseLandmarkerHelper.LandmarkerListener, CaptureCallback{

//    private var simpleExoPlayer: SimpleExoPlayer? = null
//    private var player : SimpleExoPlayer? = null
//    private var playbackPosition = 0L
//    private lateinit var cameraExecutor: ExecutorService
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

    // 결과 분석
    // ------! 싱글턴 패턴 객체 가져오기 !------
    private lateinit var singletonInstance: Singleton_t_measure

    var latestResult: PoseLandmarkerHelper.ResultBundle? = null

    // 반복
//    val detectbody = MediatorLiveData<Boolean>()
//    var hasExecuted = false
//    var isTimerRunning = false
    private var repeatCount = BigDecimal("0.0")
    private val maxRepeats = BigDecimal("6.0")
//    private var isLooping = false
    private var progress = 12
    private var serviceBound = false

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
                    setAnimation(binding.tvMeasureSkeletonCount, 1000, 500, false) {
                        hideViews(600)
                        Log.v("사진service", "isCapture: ${isCapture}, isRecording: ${isRecording}")
                        // ------! 종료 후 다시 세팅 !------
                        mediaProjectionService?.captureScreen{
                            Log.v("캡쳐종료시점", "step: $repeatCount, latestResult: $latestResult")
                            resultBundleToJson(latestResult!!, repeatCount)
                            updateUI()
                            isCapture = false

//                            if (repeatCount == maxRepeats) {
//                                binding.btnMeasureSkeletonStep.text = "완료하기"
//                                binding.pvMeasureSkeleton.progress = 100
//
//                            }
                        }
                    }
                }
                binding.btnMeasureSkeletonStep.isEnabled = true
            }
        }
    } //  ------! 카운트 다운 끝 !-------
//    private val detectObserver = Observer<Boolean> {
//        if (it && !isLooping) { // 루프 안 되고 있음
//            if (!isTimerRunning) { // 타이머 안돌아가고 있음.
//                startTimer()
//            }
//            isLooping = true
//            Log.v("isLooping", "$isLooping")
//        }
//    }

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

        // Start the PoseLandmarkerHelper again when users come back
        // to the foreground.
        backgroundExecutor.execute {
            if(this::poseLandmarkerHelper.isInitialized) {
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

        // -----! pose landmarker 시작 !-----
        // Initialize our background executor
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
        setAnimation(binding.tvMeasureSkeletonGuide!!, 2000, 2500, false ) { }
        // 옵저버 달아놓기
//        detectbody.observe(this@PlaySkeletonActivity, detectObserver)

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

        // ------! 주의사항 키기 !------
        val dialog = MeasureSkeletonDialogFragment()
        dialog.show(supportFragmentManager, "MeasureSkeletonDialogFragment")



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

        // -----! 초기 시작 !-----
        binding.svMeasureSkeleton.go(0, true)
        binding.pvMeasureSkeleton.progress = 14

        // -----! 버튼 촬영 시작 !-----
        binding.btnMeasureSkeletonStep.setOnClickListener {

            if (binding.btnMeasureSkeletonStep.text == "완료하기") {
                // todo 통신 시간에 따라 alertDialog 띄우던가 해야 함
                finish()
            }
            if (repeatCount <= maxRepeats) {
                startTimer()
            }
        }
    }

    // ------! 촬영 시 view 가리고 보이기 !-----
    private fun hideViews(delay : Long) {
        binding.clMeasureSkeletonTop.visibility = View.INVISIBLE
        binding.ivMeasureSkeletonFrame.visibility = View.INVISIBLE
        binding.clMeasureSkeletonBottom.visibility = View.INVISIBLE
        if (repeatCount != BigDecimal("2.0")) startCameraShutterAnimation()


        setAnimation(binding.clMeasureSkeletonTop, 850, delay, true) {}
        setAnimation(binding.ivMeasureSkeletonFrame, 850, delay, true) {}
        setAnimation(binding.clMeasureSkeletonBottom, 850, delay, true) {}
        binding.btnMeasureSkeletonStep.visibility = View.VISIBLE
        binding.btnMeasureSkeletonStep.text = "프레임에 맞춰 서주세요"
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
        Log.v("repeatCount", "$repeatCount")
        mCountDown.start()
        // ------! 타이머 control 끝 !------
    }

    // ------! update UI 시작 !------
    private fun updateUI() {
        when (repeatCount) {
            in BigDecimal("2.0") .. BigDecimal("2.8") -> {
                repeatCount = repeatCount.plus(BigDecimal("0.1"))
            }
            maxRepeats -> {
                binding.pvMeasureSkeleton.progress = 100
                binding.tvMeasureSkeletonCount.text = "측정이 완료됐습니다 !"
                binding.btnMeasureSkeletonStep.text = "완료하기"
                mCountDown.cancel()
                Log.v("repeat", "Max repeats reached, stopping the loop")
            }
            BigDecimal("2.9") -> {
                repeatCount = repeatCount.plus(BigDecimal("0.1"))
                progress += 14
                binding.pvMeasureSkeleton.progress = progress
                binding.svMeasureSkeleton.go(repeatCount.toInt(), true)
                val drawable = ContextCompat.getDrawable(this, resources.getIdentifier("drawable_measure_${repeatCount.toInt()}", "drawable", packageName))
                binding.ivMeasureSkeletonFrame.setImageDrawable(drawable)
            }
            else -> {
                repeatCount = repeatCount.plus(BigDecimal("1.0"))
                progress += 14
                binding.pvMeasureSkeleton.progress = progress
                Log.v("녹화종료되나요?", "repeatCount: $repeatCount, progress: $progress")
                binding.svMeasureSkeleton.go(repeatCount.toInt(), true)
                val drawable = ContextCompat.getDrawable(this, resources.getIdentifier("drawable_measure_${repeatCount.toInt()}", "drawable", packageName))
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
                binding.overlay.setResults(
                    resultBundle.results.first(),
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    RunningMode.LIVE_STREAM
                )

                // 여기에 resultbundle(poselandarkerhelper.resultbundle)이 들어감.

                binding.overlay.invalidate()
                latestResult = resultBundle
            }
        }
//        if (resultBundle.results.first().landmarks().isNotEmpty()) {
//            detectbody.postValue(true)
//        } else {
//            detectbody.postValue(false)
//        }
    }
    // ------! 기울기 계산 !------
    fun calculateSlope(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        return (y2 - y1) / (x2 - x1)
    }
    // ------! 선과 점의 X 거리 !------
//    fun calculateXDifference(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Float {
//        val x4 = x1 + (y3 - y1) / (y2 - y1) * (x2 - x1)
//        return abs(x3 - x4)
//    }
    private fun radianToDegree(radian : Double) : Double {
        return radian * ( 180.0 / Math.PI )
    }

    private fun calculateBalanceScore(radian : Double) : Double {
        val angleRadian = radianToDegree(radian)
        return ( angleRadian / 180 ) * 100
    }

    private fun calculateElbowScore(elbowAngle: Double): Double {
        return elbowAngle / 90 * 100
    }

    // override 로 resultbundle이 계속 나오는데 해당 항목을 전역변수 latest
    fun resultBundleToJson(resultBundle: PoseLandmarkerHelper.ResultBundle, step: BigDecimal) {
        val earData = mutableListOf<Pair<Double, Double>>()
        val shoulderData = mutableListOf<Pair<Double, Double>>()
        val elbowData = mutableListOf<Pair<Double, Double>>()
        val wristData = mutableListOf<Pair<Double, Double>>()
        val indexData = mutableListOf<Pair<Double, Double>>()

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
            // ------! mutablelist 0 왼쪽 1 오른쪽 , 그리고 (x, y)  !------
            when (step) {
                BigDecimal("0.0") -> {
                    val jo = JSONObject()

                    val earAngle : Double = calculateSlope(earData[0].first, earData[0].second, earData[1].first, earData[1].second)
                    val shoulderAngle : Double = calculateSlope(shoulderData[0].first, shoulderData[0].second, shoulderData[1].first, shoulderData[1].second)
                    val elbowAngle : Double = calculateSlope(elbowData[0].first, elbowData[0].second, elbowData[1].first, elbowData[1].second)
                    val wristAngle : Double = calculateSlope(wristData[0].first, wristData[0].second, wristData[1].first, wristData[1].second)
                    val hipAngle : Double = calculateSlope(hipData[0].first, hipData[0].second, hipData[1].first, hipData[1].second)
                    val kneeAngle : Double = calculateSlope(kneeData[0].first, kneeData[0].second, kneeData[1].first, kneeData[1].second)
                    val ankleAngle : Double = calculateSlope(ankleData[0].first, ankleData[0].second, ankleData[1].first, ankleData[1].second)
                    jo.put("result_static_front_horizontal_angle_ear", earAngle)
                    jo.put("result_static_front_horizontal_angle_shoulder", shoulderAngle)
                    jo.put("result_static_front_horizontal_angle_elbow", elbowAngle)
                    jo.put("result_static_front_horizontal_angle_wrist", wristAngle)
                    jo.put("result_static_front_horizontal_angle_hip", hipAngle)
                    jo.put("result_static_front_horizontal_angle_knee", kneeAngle)
                    jo.put("result_static_front_horizontal_angle_ankle", ankleAngle)

                    // 필요한 밸런스 점수 담기
                    val joScore = JSONObject()
                    joScore.put("result_balance_score_angle_ear", calculateBalanceScore(earAngle))
                    joScore.put("result_balance_score_angle_shoulder", calculateBalanceScore(shoulderAngle))
                    joScore.put("result_balance_score_angle_wrist", calculateBalanceScore(wristAngle))
                    joScore.put("result_balance_score_angle_hip", calculateBalanceScore(hipAngle))
                    joScore.put("result_balance_score_angle_knee", calculateBalanceScore(kneeAngle))
                    saveJsonToSingleton(BigDecimal("10.0"), joScore)

                    // 각 부위 거리
                    val earSubDistance : Pair<Double, Double> = Pair(abs(earData[0].first.minus(ankleXAxis)), abs(earData[1].first.minus(ankleXAxis)))
                    val shoulderSubDistance : Pair<Double, Double> = Pair(abs(shoulderData[0].first.minus(ankleXAxis)), abs(shoulderData[1].first.minus(ankleXAxis)))
                    val elbowSubDistance : Pair<Double, Double> = Pair(abs(elbowData[0].first.minus(ankleXAxis)), abs(elbowData[1].first.minus(ankleXAxis)))
                    val wristSubDistance : Pair<Double, Double> = Pair(abs(wristData[0].first.minus(ankleXAxis)), abs(wristData[1].first.minus(ankleXAxis)))
//                val thumbSubDistance : Pair<Double, Double> = Pair(abs(thumbData[0].first.minus(ankleXAxis)), abs(thumbData[1].first.minus(ankleXAxis)))
                    val hipSubDistance : Pair<Double, Double> = Pair(abs(hipData[0].first.minus(ankleXAxis)), abs(hipData[1].first.minus(ankleXAxis)))
                    val kneeSubDistance : Pair<Double, Double> = Pair(abs(kneeData[0].first.minus(ankleXAxis)), abs(kneeData[1].first.minus(ankleXAxis)))
                    val ankleSubDistance : Pair<Double, Double> = Pair(abs(ankleData[0].first.minus(ankleXAxis)), abs(ankleData[1].first.minus(ankleXAxis)))
//                val toeSubDistance : Pair<Double, Double> = Pair(abs(toeData[0].first.minus(ankleXAxis)), abs(toeData[1].first.minus(ankleXAxis)))
                    jo.put("result_static_front_horizontal_distance_sub_ear", earSubDistance)
                    jo.put("result_static_front_horizontal_distance_sub_shoulder", shoulderSubDistance)
                    jo.put("result_static_front_horizontal_distance_sub_elbow", elbowSubDistance)
                    jo.put("result_static_front_horizontal_distance_sub_wrist", wristSubDistance)
                    jo.put("result_static_front_horizontal_distance_sub_hip", hipSubDistance)
                    jo.put("result_static_front_horizontal_distance_sub_knee", kneeSubDistance)
                    jo.put("result_static_front_horizontal_distance_sub_ankle", ankleSubDistance)
//                jo.put("result_static_front_horizontal_distance_thumb_left", thumbSubDistance.first)
//                jo.put("result_static_front_horizontal_distance_thumb_right", thumbSubDistance.second)
                    jo.put("result_static_front_horizontal_distance_knee_left", kneeSubDistance.first)
                    jo.put("result_static_front_horizontal_distance_knee_right", kneeSubDistance.second)
                    jo.put("result_static_front_horizontal_distance_ankle_left", ankleSubDistance.first)
                    jo.put("result_static_front_horizontal_distance_ankle_right", ankleSubDistance.second)
//                jo.put("result_static_front_horizontal_distance_toe_left", toeSubDistance.first)
//                jo.put("result_static_front_horizontal_distance_toe_right", toeSubDistance.second)

                    // 팔 각도
                    val bicepsLean: Pair<Double, Double> = Pair(calculateSlope(shoulderData[0].first, shoulderData[0].second, elbowData[0].first, elbowData[0].second), calculateSlope(shoulderData[1].first, shoulderData[1].second, elbowData[1].first, elbowData[1].second))
                    val forearmsLean: Pair<Double, Double> = Pair(calculateSlope(elbowData[0].first, elbowData[0].second, wristData[0].first, wristData[0].second), calculateSlope(elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second))
                    val thighsLean: Pair<Double, Double> = Pair(calculateSlope(hipData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second), calculateSlope(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second))
                    val calfLean : Pair<Double, Double> = Pair(calculateSlope(kneeData[0].first, kneeData[0].second, toeData[0].first, toeData[0].second), calculateSlope(kneeData[1].first, kneeData[1].second, toeData[1].first, toeData[1].second))
                    jo.put("result_static_front_vertical_angle_shoulder_elbow_left", bicepsLean.first)
                    jo.put("result_static_front_vertical_angle_shoulder_elbow_right", bicepsLean.second)
                    jo.put("result_static_front_vertical_angle_elbow_wrist_left", forearmsLean.first)
                    jo.put("result_static_front_vertical_angle_elbow_wrist_right", forearmsLean.second)
                    jo.put("result_static_front_vertical_angle_hip_knee_left", thighsLean.first)
                    jo.put("result_static_front_vertical_angle_hip_knee_right", thighsLean.second)
                    jo.put("result_static_front_vertical_angle_knee_ankle_left", calfLean.first)
                    jo.put("result_static_front_vertical_angle_knee_ankle_right", calfLean.second)
                    saveJsonToSingleton(step, jo)
                }
                BigDecimal("1.0") -> { // 주먹 쥐고

                    val jo = JSONObject()

                    // ------! 주먹 쥐고 !------
                    val fistBicepsLean : Pair<Double, Double> = Pair(calculateSlope(shoulderData[0].first, shoulderData[0].second, elbowData[0].first, elbowData[0].second), calculateSlope(shoulderData[1].first, shoulderData[1].second, elbowData[1].first, elbowData[1].second))
                    val fistForearmsLean : Pair<Double, Double> = Pair(calculateSlope(elbowData[0].first, elbowData[0].second, wristData[0].first, wristData[0].second), calculateSlope(elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second))
                    jo.put("result_static_front_vertical_angle_arm_shoulder_elbow_left", fistBicepsLean.first)
                    jo.put("result_static_front_vertical_angle_arm_shoulder_elbow_right", fistBicepsLean.second)
                    jo.put("result_static_front_vertical_angle_arm_elbow_wrist_left", fistForearmsLean.first)
                    jo.put("result_static_front_vertical_angle_arm_elbow_wrist_right", fistForearmsLean.second)
                    saveJsonToSingleton(step, jo)
                }
                in BigDecimal("2.0") .. BigDecimal("2.9") -> { // 스쿼트
                    val jo = JSONObject()
                    // ------! 스쿼트 자세 기울기 !------
                    val squatHandsLean : Double = calculateSlope(indexData[0].first, indexData[0].second, indexData[1].first, indexData[1].second)
                    val squatHipLean : Double = calculateSlope(hipData[0].first, hipData[0].second, hipData[1].first, hipData[1].second)
                    val squatKneeLean : Double = calculateSlope(kneeData[0].first, kneeData[0].second, kneeData[1].first, kneeData[1].second)
                    jo.put("res_ov_hd_sq_fnt_horiz_ang_wrist", squatHandsLean)
                    jo.put("res_ov_hd_sq_fnt_horiz_ang_hip", squatHipLean)
                    jo.put("res_ov_hd_sq_fnt_horiz_ang_knee", squatKneeLean)
                    saveJsonToSingleton(BigDecimal("$step"), jo)

                }
                BigDecimal("3.0") -> { // 왼쪽보기 (오른쪽 팔)
                    val jo = JSONObject()
                    // ------! 측면 거리  - 왼쪽 !------
                    val sideLeftShoulderDistance : Double = abs(shoulderData[1].first.minus(ankleXAxis))
                    val sideLeftHipDistance : Double = abs(hipData[1].first.minus(ankleXAxis))
                    jo.put("result_static_side_left_horizontal_distance_shoulder", sideLeftShoulderDistance)
                    jo.put("result_static_side_left_horizontal_distance_hip", sideLeftHipDistance)

                    val sideLeftBicepsLean : Double = calculateSlope(shoulderData[1].first, shoulderData[1].second, kneeData[1].first, kneeData[1].second)
                    val sideLeftForearmsLean: Double = calculateSlope(elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second)
                    val sideLeftThighsLean : Double = calculateSlope(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second)
                    val sideLeftNeckLean : Double = calculateSlope(earData[1].first, earData[1].second, shoulderData[1].first, shoulderData[1].second)
                    jo.put("result_static_side_left_vertical_angle_shoulder_elbow", sideLeftBicepsLean % 180)
                    jo.put("result_static_side_left_vertical_angle_elbow_wrist", sideLeftForearmsLean % 180)
                    jo.put("result_static_side_left_vertical_angle_hip_knee", sideLeftThighsLean % 180)
                    jo.put("result_static_side_left_vertical_angle_ear_shoulder", sideLeftNeckLean % 180)
                    saveJsonToSingleton(step, jo)

                    // 필요한 밸런스 점수 담기
                    val joScore = JSONObject()
                    joScore.put("result_balance_score_angle_left_arms", calculateElbowScore(sideLeftForearmsLean))

                    // 복부 밸런스 점수
                    val stomachAngle = calculateSlope(middleHip.first, middleHip.second, middleShoulder.first, middleShoulder.second)
                    // 목 밸런스 점수
                    val neckAngle = calculateSlope(middleShoulder.first, middleShoulder.second, nose.first, nose.second)

                    joScore.put("result_balance_score_angle_stomach", calculateBalanceScore(stomachAngle))
                    joScore.put("result_balance_score_angle_neck", calculateBalanceScore(neckAngle))
                    saveJsonToSingleton(BigDecimal("10.0"), joScore)

                }
                BigDecimal("4.0") -> { // 오른쪽보기 (왼쪽 팔)
                    // ------! 측면 거리  - 오른쪽 !------
                    val jo = JSONObject()
                    val sideRightShoulderDistance : Double = abs(shoulderData[1].first.minus(ankleXAxis))
                    val sideRightWristDistance : Double = abs(wristData[1].first.minus(ankleXAxis))
//                val sideRightIndexDistance : Double = abs(indexData[1].first.minus(ankleXAxis))
                    jo.put("result_static_side_right_horizontal_distance_shoulder", sideRightShoulderDistance)
                    jo.put("result_static_side_right_horizontal_distance_hip", sideRightWristDistance)

                    // ------! 측면 기울기  - 오른쪽 !------
                    val sideRightBicepsLean : Double = calculateSlope(shoulderData[1].first, shoulderData[1].second, kneeData[1].first, kneeData[1].second)
                    val sideRightForearmsLean: Double =calculateSlope(elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second)
                    val sideRightThighsLean : Double = calculateSlope(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second)
                    val sideRightNeckLean : Double = calculateSlope(earData[1].first, earData[1].second, shoulderData[1].first, shoulderData[1].second)
                    jo.put("result_static_side_right_vertical_angle_shoulder_elbow", sideRightBicepsLean  % 180)
                    jo.put("result_static_side_right_vertical_angle_elbow_wrist", sideRightForearmsLean  % 180)
                    jo.put("result_static_side_right_vertical_angle_hip_knee", sideRightThighsLean  % 180)
                    jo.put("result_static_side_right_vertical_angle_ear_shoulder", sideRightNeckLean  % 180)
                    saveJsonToSingleton(step , jo)


                    // 필요한 밸런스 점수 담기
                    val joScore = JSONObject()
                    joScore.put("result_balance_score_angle_right_arms", calculateElbowScore(sideRightForearmsLean))
                    saveJsonToSingleton(BigDecimal("10.0"), joScore)
                }
                BigDecimal("5.0") -> { // ------! 후면 서서 !-------
                    val jo = JSONObject()

                    val backEarAngle : Double = calculateSlope(earData[0].first, earData[0].second, earData[1].first, earData[1].second)
                    val backShoulderAngle : Double = calculateSlope(shoulderData[0].first, shoulderData[0].second, shoulderData[1].first, shoulderData[1].second)
                    val backElbowAngle : Double = calculateSlope(elbowData[0].first, elbowData[0].second, elbowData[1].first, elbowData[1].second)
                    val backHipAngle : Double = calculateSlope(hipData[0].first, hipData[0].second, hipData[1].first, hipData[1].second)
                    val backAnkleAngle : Double = calculateSlope(ankleData[0].first, ankleData[0].second, ankleData[1].first, ankleData[1].second)
                    jo.put("result_static_back_horizontal_angle_ear", backEarAngle % 180)
                    jo.put("result_static_back_horizontal_angle_shoulder", backShoulderAngle % 180)
                    jo.put("result_static_back_horizontal_angle_elbow", backElbowAngle % 180)
                    jo.put("result_static_back_horizontal_angle_hip", backHipAngle % 180)
                    jo.put("result_static_back_horizontal_angle_ankle", backAnkleAngle % 180)

                    // ------! 후면 거리 !------
                    val backWristDistance : Pair<Double, Double> = Pair(abs(wristData[0].first.minus(ankleXAxis)), abs(wristData[1].first.minus(ankleXAxis)))
                    val backKneeDistance : Pair<Double, Double> = Pair(abs(kneeData[0].first.minus(ankleXAxis)), abs(kneeData[1].first.minus(ankleXAxis)))
                    val backHeelDistance : Pair<Double, Double> = Pair(abs(heelData[0].first.minus(ankleXAxis)), abs(heelData[1].first.minus(ankleXAxis)))
                    jo.put("result_static_back_horizontal_distance_wrist_left", backWristDistance.first)
                    jo.put("result_static_back_horizontal_distance_wrist_right", backWristDistance.second)
                    jo.put("result_static_back_horizontal_distance_knee_left", backKneeDistance.first)
                    jo.put("result_static_back_horizontal_distance_knee_right", backKneeDistance.second)
                    jo.put("result_static_back_horizontal_distance_heel_left", backHeelDistance.first)
                    jo.put("result_static_back_horizontal_distance_heel_right", backHeelDistance.second)

                    val backSpineLean : Double = calculateSlope(nose.first, nose.second, hipData[0].first, abs(hipData[0].second - hipData[1].second))
                    val backCalfLean : Pair<Double, Double> = Pair(calculateSlope(heelData[0].first, heelData[0].second, kneeData[0].first, kneeData[0].second), calculateSlope(heelData[1].first, heelData[1].second, kneeData[1].first, kneeData[1].second))
                    jo.put("result_static_back_vertical_angle_nose_center_hip", backSpineLean)
                    jo.put("result_static_back_vertical_angle_knee_heel_left", backCalfLean.second)
                    jo.put("result_static_back_vertical_angle_knee_heel_right", backCalfLean.first)
                    saveJsonToSingleton(step, jo)


                    // 필요한 밸런스 점수 담기
                    val joScore = JSONObject()
                    joScore.put("result_balance_score_angle_ankle", calculateBalanceScore(backAnkleAngle))

                    saveJsonToSingleton(BigDecimal("10.0"), joScore)
                }
                BigDecimal("6.0") -> { // ------! 앉았을 때 !------
                    val jo = JSONObject()

                    // ------! 의자 후면 거리 !------
                    val sitBackEarDistance : Pair<Double, Double> = Pair(abs(earData[0].second.minus(shoulderData[0].second)), abs(earData[1].second.minus(shoulderData[1].second)))
                    val sitBackShoulderDistance  : Pair<Double, Double> = Pair(abs(shoulderData[0].first.minus(ankleXAxis)), abs(shoulderData[1].first.minus(ankleXAxis)))
                    val sitBackHipDistance : Pair<Double, Double> = Pair(abs(hipData[0].first.minus(ankleXAxis)), abs(hipData[1].first.minus(ankleXAxis)))
                    jo.put("result_static_back_sit_horizontal_distance_sub_ear", sitBackEarDistance)
                    jo.put("result_static_back_sit_horizontal_distance_sub_shoulder", sitBackShoulderDistance)
                    jo.put("result_static_back_sit_horizontal_distance_sub_hip", sitBackHipDistance)
                    // ------! 의자 후면 기울기 !------
                    val sitBackEarAngle = calculateSlope(earData[0].first, earData[0].second, earData[1].first, earData[1].second)
                    val sitBackShoulderAngle = calculateSlope(shoulderData[0].first, shoulderData[0].second, shoulderData[1].first, shoulderData[1].second)
                    val sitBackHipAngle = calculateSlope(hipData[0].first, hipData[0].second, hipData[1].first, hipData[1].second)

                    jo.put("result_static_back_sit_horizontal_angle_ear", sitBackEarAngle)
                    jo.put("result_static_back_sit_horizontal_angle_shoulder", sitBackShoulderAngle)
                    jo.put("result_static_back_sit_horizontal_angle_hip", sitBackHipAngle)

                    saveJsonToSingleton(step, jo)
                    Log.v("6단계", "${jo}")
                }
            }
        }
//
    }
    private fun saveJsonToSingleton(step: BigDecimal, jsonObj: JSONObject ) {
        singletonInstance .jsonObject?.put(step.toString(), jsonObj)
        Log.v("싱글턴", "${singletonInstance.jsonObject}")
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
        saveJsonToSingleton(repeatCount, jo)
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

            // 애니메이션 시작
            fadeIn.start()
            fadeIn.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    fadeOut.start()
                }
            })
        }, 150)

    }
}