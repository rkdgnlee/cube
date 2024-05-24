package com.tangoplus.tangoq

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.Handler
import android.os.IBinder
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
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.tangoplus.tangoq.data.SkeletonViewModel
import com.tangoplus.tangoq.databinding.ActivityPlaySkeletonBinding
import com.tangoplus.tangoq.mediapipe.PoseLandmarkerHelper
import com.tangoplus.tangoq.service.MediaProjectionService
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
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
    // 타이머 반복
    val detectbody = MediatorLiveData<Boolean>()
    var hasExecuted = false
    var isTimerRunning = false
    private var repeatCount = 0
    private val maxRepeats = 5
    private var isLooping = false
    private val delay = if (isRecording) 110000L else 80000L //11,8초
    // 동영상 녹화
    private lateinit var videoCapture : VideoCapture<Recorder>
    private var currentRecording: Recording? = null
    private var recordingState:VideoRecordEvent? = null

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
            override fun onFinish() {
                binding.tvPSCount.text = "측정을 시작합니다 !"
                setAnimation(binding.tvPSCount, 1000, 500) {
                    if (isRecording) {
                        Log.v("동영상녹화", "hasExecuted: ${hasExecuted}, isCapture: ${isCapture}")
                        if (recordingState == null || recordingState is VideoRecordEvent.Finalize) {
                            mediaProjectionService?.recordScreen {
                                isLooping = false
                                isRecording = false // 녹화 완료
                            }
                        }
                    } else {
                        Log.v("사진service", "isCapture: ${isCapture}, hasExecuted: ${hasExecuted}")
                        // ------! 종료 후 다시 세팅 !------
                        mediaProjectionService?.captureScreen{
                            isLooping = false
                            isCapture = false
                        }
                    }
                }
                isTimerRunning = false
            }
        }
    } //  ------! 카운트 다운 끝 !-------
    private val detectObserver = Observer<Boolean> {
        Handler(Looper.getMainLooper()).postDelayed({
            if (it && !isLooping) { // 루프 안 되고 있음
                if (!isTimerRunning) { // 타이머 안돌아가고 있음.
                    startTimer()

                }
                isLooping = true
                Log.v("isLooping", "$isLooping")
            }

        }, 1000) // 탐지가 되면 4초 뒤에 시작
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
    }

    // ------! POSE LANDMARKER 설정 끝 !------

    @SuppressLint("Recycle")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityPlaySkeletonBinding.inflate(layoutInflater)
        setContentView(_binding!!.root)

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as MediaProjectionService.LocalBinder
                mediaProjectionService = binder.getService()
                Log.w("serviceInit1", "$mediaProjectionService")
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                mediaProjectionService = null
                Log.w("serviceInit2", "$mediaProjectionService")
            }
        }
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE)
        // Bind to the service
        val intent = Intent(this, MediaProjectionService::class.java)
        bindService(intent, serviceConnection as ServiceConnection, Context.BIND_AUTO_CREATE)


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
        setAnimation(binding.tvPSGuide, 2000, 1500) { }
        // 옵저버 달아놓기
        detectbody.observe(this@PlaySkeletonActivity, detectObserver)

        /** */
    }

    private fun startTimer() {

       Handler(Looper.getMainLooper()).postDelayed(object: Runnable {
           override fun run() {
               if (repeatCount == maxRepeats) {
                   mCountDown.cancel()
                   detectbody.removeObserver(detectObserver)
                   Log.v("repeat", "Max repeats reached, stopping the loop")
                   return
               }
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
               if (repeatCount <= maxRepeats) {
                   Handler(Looper.getMainLooper()).postDelayed(this, delay)
               }
               repeatCount++
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
        videoCapture = VideoCapture.withOutput(Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        )

        // ------! 카메라에 poselandmarker 그리기 !------



        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()
        try {
            // 여기에는 다양한 사용 사례가 전달될 수 있습니다.
            // 카메라는 CameraControl 및 CameraInfo에 대한 액세스를 제공합니다. // TODO 기능 추가할 경우 여기다가 선언한 기능 넣어야 함
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
        } else {
            detectbody.postValue(false)
        }
    }
    // ------! 기울기 계산 !------
    fun calculateSlope(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return (y2 - y1) / (x2 - x1)
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