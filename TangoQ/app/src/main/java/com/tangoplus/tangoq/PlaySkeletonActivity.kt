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
import android.widget.TextView
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
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.tangoplus.tangoq.data.SkeletonViewModel
import com.tangoplus.tangoq.databinding.ActivityPlaySkeletonBinding
import com.tangoplus.tangoq.mediapipe.PoseLandmarkerHelper
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import com.tangoplus.tangoq.service.MediaProjectionService
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)


class PlaySkeletonActivity : AppCompatActivity(), PoseLandmarkerHelper.LandmarkerListener{

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
    private var _binding: ActivityPlaySkeletonBinding? = null
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

    // 타이머 반복
    val detectbody = MediatorLiveData<Boolean>()
    var hasExecuted = false
    var isTimerRunning = false
    private var repeatCount = 0
    private val maxRepeats = 6
    private var isLooping = false
    private val delay = if (isRecording) 100000L else 65000L //10,6.5초

    // ------! 카운트 다운  시작 !-------
    private  val mCountDown : CountDownTimer by lazy {
        object : CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                runOnUiThread{
                    binding.tvPSCount.visibility = View.VISIBLE
                    binding.tvPSCount.alpha = 1f
                    binding.tvPSCount.text = "${(millisUntilFinished.toFloat() / 1000.0f).roundToInt()}"
                    Log.v("count", "${binding.tvPSCount.text}")
                }
            }
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onFinish() {
                if (isRecording) { // 동영상 촬영
                    binding.tvPSCount.text = "스쿼트를 실시해주세요"
                    setAnimation(binding.tvPSCount, 1000, 500) {
                        Log.v("동영상녹화", "hasExecuted: ${hasExecuted}, isCapture: ${isCapture}")
                        mediaProjectionService?.recordScreen {
                            Log.v("녹화종료시점", "step: $repeatCount, latestResult: $latestResult")
                            resultBundleToJson(latestResult!!, repeatCount)
                            isLooping = false
                            isRecording = false // 녹화 완료
                            repeatCount++
                        }

                    }
                } else {

                    binding.tvPSCount.text = "촬영을 시작합니다"
                    setAnimation(binding.tvPSCount, 1000, 500) {
                        Log.v("사진service", "isCapture: ${isCapture}, hasExecuted: ${hasExecuted}")
                        // ------! 종료 후 다시 세팅 !------
                        mediaProjectionService?.captureScreen{
                            Log.v("캡쳐종료시점", "step: $repeatCount, latestResult: $latestResult")
                            resultBundleToJson(latestResult!!, repeatCount)
                            isLooping = false
                            isCapture = false
                            repeatCount++

                            if (repeatCount == 6) {
                                binding.btnPSStep.text = "완료하기"
                            }
                        }
                    }
                }
                isTimerRunning = false

            }
        }
    } //  ------! 카운트 다운 끝 !-------
    private val detectObserver = Observer<Boolean> {
        if (it && !isLooping) { // 루프 안 되고 있음
            if (!isTimerRunning) { // 타이머 안돌아가고 있음.
                startTimer()
            }
            isLooping = true
            Log.v("isLooping", "$isLooping")
        }
    }

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
        unbindService(serviceConnection as ServiceConnection)
        val serviceIntent = Intent(this, MediaProjectionService::class.java)
        stopService(serviceIntent)
    }

    // ------! POSE LANDMARKER 설정 끝 !------

    @SuppressLint("Recycle")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityPlaySkeletonBinding.inflate(layoutInflater)
        setContentView(_binding!!.root)
        singletonInstance = Singleton_t_measure.getInstance(this)

        // ------! foreground 서비스 연결 시작 !------
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as MediaProjectionService.LocalBinder
                mediaProjectionService = binder.getService()
                Log.w("serviceInit1", "$mediaProjectionService")
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                mediaProjectionService = null
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
        if (!hasPermissions(this)) {
            requestPermissions(PERMISSIONS_REQUIRED, REQUEST_CODE_PERMISSIONS)
            setUpCamera()
        }


        // ------! 안내 문구 사라짐 시작 !------
        setAnimation(binding.tvPSGuide, 2000, 3000) { }
        // 옵저버 달아놓기
        detectbody.observe(this@PlaySkeletonActivity, detectObserver)

        /** 사진 및 동영상 촬영 순서
         * 1. isLooping true -> repeatCount확인 -> count에 맞게 isCapture및 isRecord 선택
         * 2. mCountdown.start() -> 카운트 다운이 종료될 때 isCapture, isRecording에 따라 service 함수 실행 */
        binding.ibtnPSBack.setOnClickListener {
            val intentBack = Intent(this, MainActivity::class.java)
            intentBack.putExtra("finishMeasure", true)
            startActivity(intentBack)
            finish()
        }
    }

    // ------! 타이머 control 시작 !------
    private fun startTimer() {
        when (repeatCount) {
            1 -> {
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

        // -----! 버튼 촬영 시작 !-----
        binding.btnPSStep.setOnClickListener {
            // TODO return 으로 빠져나가기
            if (binding.btnPSStep.text == "완료하기") {
                // todo 통신 시간에 따라 alertDialog 띄우던가 해야 함
                mCountDown.cancel()
                detectbody.removeObserver(detectObserver)
                Log.v("repeat", "Max repeats reached, stopping the loop")
                finish()
            }
            if (repeatCount <= maxRepeats) {
                detectbody.postValue(true)
            }
        }
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

                // Force a redraw
                binding.overlay.invalidate()
                latestResult = resultBundle
            }
        }
        if (resultBundle.results.first().landmarks().isNotEmpty()) {
            detectbody.postValue(true)
        } else {
            detectbody.postValue(false)
        }
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
    // --------------------------------! 포즈 값 받아오기 시작 !--------------------------------
    private fun resultBundleToJson(resultBundle: PoseLandmarkerHelper.ResultBundle, step: Int) {
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

        // ------! 왼쪽 true (left 가 true) !------
        when (step) {
            0 -> {
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
                saveJsonToSingleton(jo, step)
            }
            1 -> {  // 스쿼트
                val jo = JSONObject()
                // ------! 스쿼트 자세 기울기 !------
                val squatHandsLean : Double = calculateSlope(indexData[0].first, indexData[0].second, indexData[1].first, indexData[1].second)
                val squatHipLean : Double = calculateSlope(hipData[0].first, hipData[0].second, hipData[1].first, hipData[1].second)
                val squatKneeLean : Double = calculateSlope(kneeData[0].first, kneeData[0].second, kneeData[1].first, kneeData[1].second)

            }
            2 -> { // 주먹 쥐고
                val jo = JSONObject()

                // ------! 주먹 쥐고 !------
                val fistBicepsLean : Pair<Double, Double> = Pair(calculateSlope(shoulderData[0].first, shoulderData[0].second, elbowData[0].first, elbowData[0].second), calculateSlope(shoulderData[1].first, shoulderData[1].second, elbowData[1].first, elbowData[1].second))
                val fistForearmsLean : Pair<Double, Double> = Pair(calculateSlope(elbowData[0].first, elbowData[0].second, wristData[0].first, wristData[0].second), calculateSlope(elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second))

            }
            3 -> { // 왼쪽보기 (오른쪽 팔)
                val jo = JSONObject()
                // ------! 측면 거리  - 왼쪽 !------
                val sideLeftShoulderDistance : Double = abs(shoulderData[1].first.minus(ankleXAxis))
                val sideLeftHipDistance : Double = abs(hipData[1].first.minus(ankleXAxis))
                jo.put("result_static_side_left_horizontal_distance_shoulder", sideLeftShoulderDistance)
                jo.put("result_static_side_left_horizontal_distance_hip", sideLeftHipDistance)

                val sideLeftBicepsLean : Double = calculateSlope(shoulderData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second)
                val sideLeftForearmsLean: Double = calculateSlope(elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second)
                val sideLeftThighsLean : Double = calculateSlope(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second)
                val sideLeftNeckLean : Double = calculateSlope(earData[1].first, earData[1].second, shoulderData[1].first, shoulderData[1].second)
                jo.put("result_static_side_left_vertical_angle_shoulder_elbow", sideLeftBicepsLean)
                jo.put("result_static_side_left_vertical_angle_elbow_wrist", sideLeftForearmsLean)
                jo.put("result_static_side_left_vertical_angle_hip_knee", sideLeftThighsLean)
                jo.put("result_static_side_left_vertical_angle_ear_shoulder", sideLeftNeckLean)
                saveJsonToSingleton(jo, step)
            }
            4 -> { // 오른쪽보기 (왼쪽 팔)
                // ------! 측면 거리  - 오른쪽 !------
                val jo = JSONObject()
                val sideRightShoulderDistance : Double = abs(shoulderData[1].first.minus(ankleXAxis))
                val sideRightWristDistance : Double = abs(wristData[1].first.minus(ankleXAxis))
//                val sideRightIndexDistance : Double = abs(indexData[1].first.minus(ankleXAxis))
                jo.put("result_static_side_left_horizontal_distance_shoulder", sideRightShoulderDistance)
                jo.put("result_static_side_left_horizontal_distance_hip", sideRightWristDistance)

                // ------! 측면 기울기  - 오른쪽 !------
                val sideRightBicepsLean : Double = calculateSlope(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second)
                val sideRightForearmsLean: Double =calculateSlope(elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second)
                val sideRightThighsLean : Double = calculateSlope(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second)
                val sideRightNeckLean : Double = calculateSlope(earData[1].first, earData[1].second, shoulderData[1].first, shoulderData[1].second)
                jo.put("result_static_side_left_vertical_angle_shoulder_elbow", sideRightBicepsLean)
                jo.put("result_static_side_left_vertical_angle_elbow_wrist", sideRightForearmsLean)
                jo.put("result_static_side_left_vertical_angle_hip_knee", sideRightThighsLean)
                jo.put("result_static_side_left_vertical_angle_ear_shoulder", sideRightNeckLean)
                saveJsonToSingleton(jo, step)
            }
            5 -> { // ------! 후면 서서 !-------
                val jo = JSONObject()

                val backEarAngle : Double = calculateSlope(earData[0].first, earData[0].second, earData[1].first, earData[1].second)
                val backShoulderAngle : Double = calculateSlope(shoulderData[0].first, shoulderData[0].second, shoulderData[1].first, shoulderData[1].second)
                val backElbowAngle : Double = calculateSlope(elbowData[0].first, elbowData[0].second, elbowData[1].first, elbowData[1].second)
                val backHipAngle : Double = calculateSlope(hipData[0].first, hipData[0].second, hipData[1].first, hipData[1].second)
                val backAnkleAngle : Double = calculateSlope(ankleData[0].first, ankleData[0].second, ankleData[1].first, ankleData[1].second)
                jo.put("result_static_back_horizontal_angle_ear", backEarAngle)
                jo.put("result_static_back_horizontal_angle_shoulder", backShoulderAngle)
                jo.put("result_static_back_horizontal_angle_elbow", backElbowAngle)
                jo.put("result_static_back_horizontal_angle_hip", backHipAngle)
                jo.put("result_static_back_horizontal_angle_ankle", backAnkleAngle)

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
                saveJsonToSingleton(jo, step)
            }
            6 -> { // ------! 앉았을 때 !------
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
                saveJsonToSingleton(jo, step)
            }
        }

//

    }
    private fun saveJsonToSingleton(jsonObj: JSONObject, step: Int) {
        singletonInstance .jsonObject?.put(step.toString(), jsonObj)
        Log.v("싱글턴", "${singletonInstance.jsonObject}")
    }

    // ------! 애니메이션 !------
    private fun setAnimation(tv: TextView, duration : Long, delay: Long, callback: () -> Unit) {
        val animator = ObjectAnimator.ofFloat(tv, "alpha", 1f, 0f)
        animator.duration = duration
        animator.addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                tv.visibility = View.INVISIBLE
                callback()
            }
        })
        Handler(Looper.getMainLooper()).postDelayed({
            animator.start()
        }, delay)
    }

}