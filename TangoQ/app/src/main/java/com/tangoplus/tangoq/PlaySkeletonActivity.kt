package com.tangoplus.tangoq

import android.Manifest
import android.R
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
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
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.liveData
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.tangoplus.tangoq.data.SkeletonViewModel
import com.tangoplus.tangoq.databinding.ActivityPlaySkeletonBinding
import com.tangoplus.tangoq.mediapipe.PoseLandmarkerHelper
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt


typealias LumaListener = (luma: Double) -> Unit
private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)

class PlaySkeletonActivity : AppCompatActivity(), PoseLandmarkerHelper.LandmarkerListener {

//    private var simpleExoPlayer: SimpleExoPlayer? = null
//    private var player : SimpleExoPlayer? = null
//    private var playbackPosition = 0L
//    private lateinit var cameraExecutor: ExecutorService

    private lateinit var imageCapture: ImageCapture
    val detectbody = MediatorLiveData<Boolean>()
    var singleCapture = true
    var hasExecuted = false
    var isTimerRunning = false
    private lateinit var bitmap1 : Bitmap
    private lateinit var bitmap2 : Bitmap
    val bitmap1b  = MutableLiveData(false)
    val bitmap2b = MutableLiveData(false)
    private var repeatCount = 0
    private val maxRepeats = 2
    private lateinit var videoCapture : VideoCapture<Recorder>
    private var currentRecording: Recording? = null
    // ------! POSE LANDMARKER 설정 시작 !------
    companion object {
        private const val TAG = "Pose Landmarker"
        private const val REQUEST_CODE_PERMISSIONS = 1001
        private const val REQUEST_CODE_SCREEN_CAPTURE = 1001
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
    private var _fragmentCameraBinding: ActivityPlaySkeletonBinding? = null

    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!
    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private val viewModel: SkeletonViewModel by viewModels()
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_BACK
    private lateinit var backgroundExecutor: ExecutorService

    override fun onResume() {
        super.onResume()
        // TODO 그냥 Fragment로 연결해서 새로운 fragment를 fm~ 해서오전까지 해보고 안되는 걸로
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
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _fragmentCameraBinding = null
        // Shut down our background executor
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )
        Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
    }

    // ------! POSE LANDMARKER 설정 끝 !------

    @SuppressLint("Recycle")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _fragmentCameraBinding = ActivityPlaySkeletonBinding.inflate(layoutInflater)
        setContentView(_fragmentCameraBinding!!.root)

        // ------! 안내 문구 사라짐 시작 !------

        // -----! pose landmarker 시작 !-----
        // Initialize our background executor
        backgroundExecutor = Executors.newSingleThreadExecutor()
        // Wait for the views to be properly laid out
        fragmentCameraBinding.viewFinder.post {
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

        // ------! 명시적으로 루프 타기 !------
        setAnimation(fragmentCameraBinding.tvPSGuide, 2000, 1500) { }
        // ------! 카운트 다운 !-------
        val mCountDown : CountDownTimer by lazy {
            object : CountDownTimer(4000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    runOnUiThread{
                        fragmentCameraBinding.tvPSCount.visibility = View.VISIBLE
                        fragmentCameraBinding.tvPSCount.alpha = 1f
                        fragmentCameraBinding.tvPSCount.text = "${(millisUntilFinished.toFloat() / 1000.0f).roundToInt()}"
                        Log.v("count", "${fragmentCameraBinding.tvPSCount.text}")
                    }
                }
                override fun onFinish() {
                    if (singleCapture) {
                        fragmentCameraBinding.tvPSCount.text = "측정을 시작합니다 !"
                        setAnimation(fragmentCameraBinding.tvPSCount, 1000, 500) {
                            takePhoto()
                            captureScreen()
                            observeBitmaps()
                        }
                        singleCapture = false
                        isTimerRunning = false
                    }
                }
            }
        }
        val detectObserver = Observer<Boolean> {
            if (it && !hasExecuted) {
                if (!isTimerRunning) {
                    singleCapture = true
                    mCountDown.start()
                    isTimerRunning = true // Timer is now running
                }
                detectbody.postValue(false)
                hasExecuted = true
                Log.v("hasExecuted", "$hasExecuted")
            }
        }
        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
            override fun run() {
                if (repeatCount == maxRepeats) {
                    mCountDown.cancel()
                    detectbody.removeObserver(detectObserver)
                    Log.v("repeat", "Max repeats reached, stopping the loop")
                    return
                }
                detectbody.observe(this@PlaySkeletonActivity, detectObserver)
                Log.v("repeatCount", "$repeatCount")
                repeatCount++
                if (repeatCount <= maxRepeats) {
                    Handler(Looper.getMainLooper()).postDelayed(this, 8000)
                }
            }
        }, 5000)



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
        preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        // 미리보기. 4:3 비율만 사용합니다. 이것이 우리 모델에 가장 가깝기 때문입니다.
        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .build()

        // 이미지 분석. RGBA 8888을 사용하여 모델 작동 방식 일치
        imageAnalyzer =
            ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
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
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .build()
        videoCapture = VideoCapture.withOutput(Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        )



        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // 여기에는 다양한 사용 사례가 전달될 수 있습니다.
            // 카메라는 CameraControl 및 CameraInfo에 대한 액세스를 제공합니다.
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer, imageCapture
            )
            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
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
            fragmentCameraBinding.viewFinder.display.rotation
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
            if (_fragmentCameraBinding != null) {
                // Pass necessary information to OverlayView for drawing on the canvas
                fragmentCameraBinding.overlay.setResults(
                    resultBundle.results.first(),
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    RunningMode.LIVE_STREAM
                )

                // Force a redraw
                fragmentCameraBinding.overlay.invalidate()

            }
        }
        // --------------------------------! 포즈 값 받아오기 시작 !--------------------------------
//        val earData = mutableListOf<Pair<Float, Float>>()
//        val shoulderData = mutableListOf<Pair<Float, Float>>()
//        val elbowData = mutableListOf<Pair<Float, Float>>()
//        val wristData = mutableListOf<Pair<Float, Float>>()
//        val indexData = mutableListOf<Pair<Float, Float>>()
//        val hipData = mutableListOf<Pair<Float, Float>>()
//        val kneeData = mutableListOf<Pair<Float, Float>>()
//        val ankleData = mutableListOf<Pair<Float, Float>>()
//        val heelData = mutableListOf<Pair<Float, Float>>()

        if (resultBundle.results.first().landmarks().isNotEmpty()) {

            detectbody.postValue(true)




//            val poseLandmarkerResult = resultBundle.results.first().landmarks()[0]
//            Log.v("poseLandmarkerResult결과", "$poseLandmarkerResult")
//            for (i in 7 until  poseLandmarkerResult.size) {
//                when (i) {
//                    in 7 .. 8 -> earData.add(Pair(poseLandmarkerResult[i].x(), poseLandmarkerResult[i].y()))
//                    in 11 .. 12 -> shoulderData.add(Pair(poseLandmarkerResult[i].x(), poseLandmarkerResult[i].y()))
//                    in 13 .. 14 -> elbowData.add(Pair(poseLandmarkerResult[i].x(), poseLandmarkerResult[i].y()))
//                    in 15 .. 16 -> wristData.add(Pair(poseLandmarkerResult[i].x(), poseLandmarkerResult[i].y()))
//                    in 19 .. 20 -> indexData.add(Pair(poseLandmarkerResult[i].x(), poseLandmarkerResult[i].y()))
//                    in 23 .. 24 -> hipData.add(Pair(poseLandmarkerResult[i].x(), poseLandmarkerResult[i].y()))
//                    in 25 .. 26 -> kneeData.add(Pair(poseLandmarkerResult[i].x(), poseLandmarkerResult[i].y()))
//                    in 27 .. 28 -> ankleData.add(Pair(poseLandmarkerResult[i].x(), poseLandmarkerResult[i].y()))
//                    in 29 .. 30 -> heelData.add(Pair(poseLandmarkerResult[i].x(), poseLandmarkerResult[i].y()))
//                }
//            }
//            val nose : Pair<Float, Float> = Pair(poseLandmarkerResult[0].x(), poseLandmarkerResult[0].y())
////             -------! 왼쪽 그니까 홀수번이 높을 경우 true (left 가 true)
//            val shoulderAngle : Float = calculateSlope(shoulderData[0].first, shoulderData[0].second, shoulderData[1].first, shoulderData[1].second)
//            val elbowAngle : Float = calculateSlope(elbowData[0].first, elbowData[0].second, elbowData[1].first, elbowData[1].second)
//            val wristAngle : Float = calculateSlope(wristData[0].first, wristData[0].second, wristData[1].first, wristData[1].second)
//            val hipAngle : Float = calculateSlope(hipData[0].first, hipData[0].second, hipData[1].first, hipData[1].second)
//            val kneeAngle : Float = calculateSlope(kneeData[0].first, kneeData[0].second, kneeData[1].first, kneeData[1].second)
//            val ankleAngle : Float = calculateSlope(ankleData[0].first, ankleData[0].second, ankleData[1].first, ankleData[1].second)
//            Log.v("1 전면 기울기, 기울어짐", "어깨: $shoulderAngle, 팔꿈치: $elbowAngle, 손목: $wristAngle, 엉덩이: $hipAngle, 무릎: $kneeAngle, 발목: $ankleAngle")
//            val ankleXAxis = ankleData[0].first.minus(ankleData[1].first)  / 2
//
//            val shoulderDistance : Pair<Float, Float> = Pair(abs(shoulderData[0].first.minus(ankleXAxis)), shoulderData[1].first.minus(ankleXAxis))
//            val elbowDistance : Pair<Float, Float> = Pair(abs(elbowData[0].first.minus(ankleXAxis)), elbowData[1].first.minus(ankleXAxis))
//            val wristDistance : Pair<Float, Float> = Pair(abs(wristData[0].first.minus(ankleXAxis)), wristData[1].first.minus(ankleXAxis))
//            val hipDistance : Pair<Float, Float> = Pair(abs(hipData[0].first.minus(ankleXAxis)), hipData[1].first.minus(ankleXAxis))
//            val kneeDistance : Pair<Float, Float> = Pair(abs(kneeData[0].first.minus(ankleXAxis)), kneeData[1].first.minus(ankleXAxis))
//            val ankleDistance : Pair<Float, Float> = Pair(abs(ankleData[0].first.minus(ankleXAxis)), ankleData[1].first.minus(ankleXAxis))
//            Log.v("2 전면 거리", "어깨: $shoulderDistance, 팔꿈치: $elbowDistance, 손목: $wristDistance, 엉덩이: $hipDistance, 무릎: $kneeDistance, 발목: $ankleDistance")
//
//            // ------! 주먹쥐고 가슴에 붙이기 !------
//            val fistElbowLean : Pair<Float, Float> = Pair(calculateSlope(shoulderData[0].first, shoulderData[0].second, elbowData[0].first, elbowData[0].second), calculateSlope(shoulderData[1].first, shoulderData[1].second, elbowData[1].first, elbowData[1].second))
//            val fistWristLean : Pair<Float, Float> = Pair(calculateSlope(elbowData[0].first, elbowData[0].second, wristData[0].first, wristData[0].second), calculateSlope(elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second))
//
//            Log.v("3 주먹쥐고 팔 각도", "어깨-팔꿈치: $fistElbowLean, 팔꿈치-손목: $fistWristLean")
//
//            // ------! 전면 팔 기울기 !------
//            val bicepsLean: Pair<Float, Float> = Pair(calculateSlope(shoulderData[0].first, shoulderData[0].second, elbowData[0].first, elbowData[0].second), calculateSlope(shoulderData[1].first, shoulderData[1].second, elbowData[1].first, elbowData[1].second))
//            val forearmsLean: Pair<Float, Float> = Pair(calculateSlope(elbowData[0].first, elbowData[0].second, wristData[0].first, wristData[0].second), calculateSlope(elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second))
//            val thighsLean: Pair<Float, Float> = Pair(calculateSlope(hipData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second), calculateSlope(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second))
//            Log.v("4 팔 기울기", "이두: $bicepsLean, 전완: $forearmsLean, 허벅지: $thighsLean")
//
//            // ------! 측면 기울기 !------
//            val sideLeftBicepsLean : Pair<Float, Float> = Pair(calculateSlope(shoulderData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second), calculateSlope(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second))
//            val sideLeftForearmsLean: Pair<Float, Float> = Pair(calculateSlope(elbowData[0].first, elbowData[0].second, wristData[0].first, wristData[0].second), calculateSlope(elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second))
//            val sideLeftThighsLean : Pair<Float, Float> = Pair(calculateSlope(hipData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second), calculateSlope(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second))
//            val sideShoulderDistance: Pair<Float, Float> = Pair(abs(shoulderData[0].first.minus(ankleXAxis)), shoulderData[1].first.minus(ankleXAxis))
//            val sideHipDistance: Pair<Float, Float> = Pair(abs(hipData[0].first.minus(ankleXAxis)), hipData[1].first.minus(ankleXAxis))
//            Log.v("5 측면 기울기", "어깨: $sideShoulderDistance, 엉덩: $sideHipDistance 이두: $sideLeftBicepsLean, 전완: $sideLeftForearmsLean, 허벅지: $sideLeftThighsLean")
//
//            // ------! 후면 기울기 !------
//            val backWaistLean : Float = calculateSlope(nose.first, nose.second, ankleXAxis, hipData[0].second) // ankle의 x값, hip의 y값으로 기울기 계산
//            val backfootLean : Pair<Float, Float> = Pair(calculateSlope(kneeData[0].first, kneeData[0].second, heelData[0].first, heelData[0].second), calculateSlope(kneeData[1].first, kneeData[1].second, heelData[1].first, heelData[1].second))
//            Log.v("6 후면 기울기 ", "허리: $backWaistLean, 발: $backfootLean")
//
//            // ------! 후면 거리 !------
//            val backShoulderDistance : Pair<Float, Float> = Pair(abs(shoulderData[0].first.minus(ankleXAxis)), shoulderData[1].first.minus(ankleXAxis))
//            val backElbowDistance : Pair<Float, Float> = Pair(abs(elbowData[0].first.minus(ankleXAxis)), elbowData[1].first.minus(ankleXAxis))
//            val backWRistDistance : Pair<Float, Float> = Pair(abs(wristData[0].first.minus(ankleXAxis)), wristData[1].first.minus(ankleXAxis))
//            val backHipDistance : Pair<Float, Float> = Pair(abs(hipData[0].first.minus(ankleXAxis)), hipData[1].first.minus(ankleXAxis))
//            val backKneeDistance : Pair<Float, Float> = Pair(abs(kneeData[0].first.minus(ankleXAxis)), kneeData[1].first.minus(ankleXAxis))
//            val backAnkleDistance : Pair<Float, Float> = Pair(abs(ankleData[0].first.minus(ankleXAxis)), ankleData[1].first.minus(ankleXAxis))
//            Log.v("7 후면 거리", "어깨: $backShoulderDistance, 팔꿈치: $backElbowDistance, 손목: $backWRistDistance, 엉덩이: $backHipDistance, 무릎: $backKneeDistance, 발목: $backAnkleDistance")
//
//            // ------! 의자 후면 기울기 !------
//            val sitBackNeckDistance : Pair<Float, Float> = Pair(abs(earData[0].second.minus(shoulderData[0].second)), abs(earData[1].second.minus(shoulderData[1].second)))
//            val sitBackShoulderDistance  : Pair<Float, Float> = Pair(abs(shoulderData[0].first.minus(ankleXAxis)), shoulderData[1].first.minus(ankleXAxis))
//            val sitBackHipDistance : Pair<Float, Float> = Pair(abs(hipData[0].first.minus(ankleXAxis)), hipData[1].first.minus(ankleXAxis))
//            Log.v("8 의자 후면 기울기", "목: $sitBackNeckDistance, 어깨: $sitBackShoulderDistance, 엉덩: $sitBackHipDistance")
//
//            // ------! 스쿼트 자세 기울기 !------
//            val squatHandsLean : Float = calculateSlope(indexData[0].first, indexData[0].second, indexData[1].first, indexData[1].second)
//            val squatHipLean : Float = calculateSlope(hipData[0].first, hipData[0].second, hipData[1].first, hipData[1].second)
//            val squatKneeLean : Float = calculateSlope(kneeData[0].first, kneeData[0].second, kneeData[1].first, kneeData[1].second)
//            Log.v("9 스쿼트 자세 기울기", "손 끝: $squatHandsLean, 엉덩: $squatHipLean, 무릎: $squatKneeLean")
        }
    }
    // ------! 기울기 계산 !------
    fun calculateSlope(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return (y2 - y1) / (x2 - x1)
    }


    // 16:9로 사진찍기.
    private fun observeBitmaps() {
        // Removing existing observers to prevent multiple triggers
        bitmap1b.removeObservers(this)
        bitmap2b.removeObservers(this)

        bitmap1b.observe(this) { isBitmap1Ready ->
            if (isBitmap1Ready) {
                Log.v("observeBitmaps", "bitmap1 is ready")
                checkAndCombineBitmaps()
            }
        }

        bitmap2b.observe(this) { isBitmap2Ready ->
            if (isBitmap2Ready) {
                Log.v("observeBitmaps", "bitmap2 is ready")
                checkAndCombineBitmaps()
            }
        }
    }

    private fun checkAndCombineBitmaps() {
        if (bitmap1b.value == true && bitmap2b.value == true) {
            val combinedBitmap = combineBitmaps(bitmap1, bitmap2)
            saveBitmap(combinedBitmap)
            bitmap1b.value = false
            bitmap2b.value = false
        }
    }
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(this@PlaySkeletonActivity.getExternalFilesDir(null), "${System.currentTimeMillis()}.jpg")
        val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        try {
            imageCapture.takePicture(
                outputOption,
                ContextCompat.getMainExecutor(this@PlaySkeletonActivity),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val savedUri = Uri.fromFile(photoFile)
                        Log.v(TAG, "Photo Captured Succceded: $savedUri")
                        bitmap1 = BitmapFactory.decodeFile(photoFile.absolutePath)
                        bitmap1b.value = true
                        Log.v("bitmap", "bitmap1: ${bitmap1b.value}")
                    }
                    override fun onError(exception: ImageCaptureException) {}
                }
            )
        } catch (e: Exception) {
            Log.e("takePhoto", "${e.message}")
        }
    }
    // ------! 비디오 녹화 시작 !------

    private fun takeVideo() {
        val videoFile = File(
            externalMediaDirs.firstOrNull(),
            "${System.currentTimeMillis()}.mp4"
        )


        val outputOptions = FileOutputOptions.Builder(videoFile).build()
        currentRecording = videoCapture.output
            .prepareRecording(this, outputOptions)
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        Log.w("video", "Video Record Starrt")
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            Log.d(TAG, "Video recording succeeded: ${recordEvent.outputResults.outputUri}")
                            Toast.makeText(this, "Video saved: ${recordEvent.outputResults.outputUri}", Toast.LENGTH_SHORT).show()
                            // ------! 동영상 저장 !------
                            val contentValues = ContentValues().apply {
                                put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.name)
                                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
                            }

                            val contentResolver = contentResolver
                            val videoUri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

                            videoUri?.let {
                                contentResolver.openOutputStream(it)?.use { outputStream ->
                                    FileInputStream(videoFile).use { inputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                                }
                                Log.d(TAG, "Video saved to gallery: $videoUri")
                                Toast.makeText(this, "Video saved to gallery: $videoUri", Toast.LENGTH_SHORT).show()
                            }
                            // ------! 동영상 저장 !------
                        } else {
                            recordEvent.error.let {
                                Log.e(TAG, "Video recording error: ${it}")
                            }
                        }
                        currentRecording = null
                    }
                }
            }
        Handler(Looper.getMainLooper()).postDelayed({
            currentRecording?.stop()
        }, 3000) // 3초 후에 녹화를 중지합니다.
    } // ------! 비디오 녹화 끝 !------
        private fun captureScreen() {

            // 현재 화면의 비트맵을 가져옵니다.
            val rootView = window.decorView.rootView
            val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            rootView.draw(canvas)
            bitmap2 = bitmap
            bitmap2b.value = true
            Log.v("bitmap", "bitmap2: ${bitmap2b.value}")
    }
    private fun combineBitmaps(bitmap1: Bitmap, bitmap2: Bitmap): Bitmap {
        // bitmap2를 bitmap1의 크기에 맞게 조정 (중앙 부분 자르기)
        val matrix = Matrix().apply { postRotate(90f) }
        val rotatedBitmap1 = Bitmap.createBitmap(bitmap1, 0, 0, bitmap1.width, bitmap1.height, matrix, true)

        val width = rotatedBitmap1.width
        val height = rotatedBitmap1.height
        Log.v("결합", "${width}")

        val ratioBitmap2 = bitmap2.width.toFloat() / bitmap2.height.toFloat() 
        Log.v("결합", "${ratioBitmap2}")
        val scaledBitmap2: Bitmap = if (ratioBitmap2 > width.toFloat() / height.toFloat()) {
            // 너무 넓을 경우 상하로 잘라냄
            val scaledHeight = height
            val scaledWidth = (height * ratioBitmap2).roundToInt()
            Log.v("scaledWidth", "${scaledWidth}")
            Bitmap.createScaledBitmap(bitmap2, scaledWidth, scaledHeight, true)
        } else {
            // 너무 좁을 경우 좌우로 잘라냄
            val scaledWidth = width
            val scaledHeight = (width / ratioBitmap2).roundToInt()
            Log.v("scaledHeight", "${scaledHeight}")
            Bitmap.createScaledBitmap(bitmap2, scaledWidth, scaledHeight, true)
        }

        val xOffset = (scaledBitmap2.width - width) / 2
        val yOffset = (scaledBitmap2.height - height) / 2
        Log.v("결합", "${yOffset}")
        val adjustedBitmap2 = Bitmap.createBitmap(scaledBitmap2, xOffset, yOffset, width, height)

        // 결합할 비트맵 생성
        val combinedBitmap = Bitmap.createBitmap(rotatedBitmap1.width, rotatedBitmap1.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(combinedBitmap)
        canvas.drawBitmap(rotatedBitmap1 , Matrix(), null)
        canvas.drawBitmap(adjustedBitmap2, 0f, 0f, null)
        Log.v("결합", "${combinedBitmap.width}")
        return combinedBitmap
    }

    private fun saveBitmap(bitmap: Bitmap?) {
        var fos: FileOutputStream? = null
        try {
            val dir = File(Environment.getExternalStorageDirectory().absolutePath + "/Pictures")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            Log.v("저장", dir.absolutePath)
            val file = File(dir, "${System.currentTimeMillis()}.png")
            fos = FileOutputStream(file)
            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, fos)
            // ------! 종료 후 다시 세팅 !------
            hasExecuted = false



            Toast.makeText(this, "Screenshot saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Screenshot failed", Toast.LENGTH_LONG).show()
        } finally {
            fos?.close()
            bitmap?.recycle()
        }
    }
    // ------! 이미지 캡처 함수 !------
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