package com.tangoplus.tangoq

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
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
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import android.view.View
import android.widget.Chronometer
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
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
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.tangoplus.tangoq.data.ExerciseViewModel
import com.tangoplus.tangoq.data.SkeletonViewModel
import com.tangoplus.tangoq.databinding.ActivityPlaySkeletonBinding
import com.tangoplus.tangoq.mediapipe.PoseLandmarkerHelper
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import com.tangoplus.tangoq.service.MediaProjectionService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)

class PlaySkeletonActivity : AppCompatActivity(), SensorEventListener, PoseLandmarkerHelper.LandmarkerListener {
    private var _binding : ActivityPlaySkeletonBinding? = null
    private val binding get() = _binding!!
    // ------! vertical sensor !------
    private var accelerometer: Sensor? = null
    private lateinit var sensorManager: SensorManager
    private var currentBias = 0f
    private var filteredAngle = 0f
    private val ALPHA = 0.1f
    private val INTERPOLATION_FACTOR = 0.1f
    private var hideIndicator = false

    // ------! exoplayer2 !------
    val eViewModel: ExerciseViewModel by viewModels()
    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var playbackPosition = 0L
    private lateinit var chronometer: Chronometer

    // ------! mediapipe !------
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
    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private val viewModel: SkeletonViewModel by viewModels()
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_BACK
    private lateinit var backgroundExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture
    private lateinit var singletonInstance: Singleton_t_measure
    var latestResult: PoseLandmarkerHelper.ResultBundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityPlaySkeletonBinding.inflate(layoutInflater)
        setContentView(_binding!!.root)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // ------! 수직 감도 사라지기 보이기 시작 !------
        Handler(Looper.getMainLooper()).postDelayed({
            binding.clPSVerticalAngle.animate().alpha(0f).setDuration(300).start()
            hideIndicator = true
        }, 5000)
        binding.clPSVerticalAngle.setOnClickListener {
            if (!hideIndicator) {
                binding.clPSVerticalAngle.animate().alpha(0f).setDuration(300).start()
                hideIndicator = true
            } else {
                binding.clPSVerticalAngle.animate().alpha(1f).setDuration(300).start()
                hideIndicator = false
            }
        }
        // ------! 수직 감도 사라지기 보이기 끝 !------

        // ------! 재생시간 타이머 시작 !------
        chronometer = findViewById(R.id.chronometer)
        chronometer.base = SystemClock.elapsedRealtime()
        chronometer.start()
        // ------! 재생시간 타이머 끝 !------

        // ------! landscape로 방향 설정 & 재생시간 받아오기 !------
//        val videoUrl = intent.getStringExtra("video_url")
        val videoUrl = "https://gym.tangostar.co.kr/data/contents/videos/0601/73-s.mp4"
        val urls = intent.getStringArrayListExtra("urls")
        val url_list = ArrayList<String>()

        if (!urls.isNullOrEmpty()) {
            url_list.addAll(urls)
        } else if (videoUrl != null) {
            url_list.add(videoUrl)
        }

        if ( url_list.isNotEmpty() ) {
            playbackPosition = intent.getLongExtra("current_position", 0L)
            initPlayer(url_list)
            simpleExoPlayer!!.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    if (playbackState == Player.STATE_ENDED) {
                        Log.v("currentWindowIndex", "${simpleExoPlayer!!.currentWindowIndex}")
                        // ------! 모든 영상 종료 시 자동 이동 !------
                        if (simpleExoPlayer!!.currentWindowIndex == url_list.size - 1) {

                            val elapsedMills = SystemClock.elapsedRealtime() - chronometer.base
                            eViewModel.exerciseLog.value = Triple((elapsedMills / 1000).toInt(), "${(simpleExoPlayer?.currentWindowIndex)!! + 1}",56)
                            val intent = Intent(this@PlaySkeletonActivity, MainActivity::class.java)
                            intent.putExtra("feedback_finish", eViewModel.exerciseLog.value)
                            Log.v("feedback_finish", "VM_exercise_log: ${eViewModel.exerciseLog.value}")
                            startActivity(intent)
                            finish()
                        }else {
                            Handler(Looper.getMainLooper()).postDelayed({
                                simpleExoPlayer?.next()
                                // TODO : 여기에 History에 대한 걸 넣어야 함 (통신)
                            }, 1000)
                        }
                    }
                }
            })
        }
        // -----! 받아온 즐겨찾기 재생 목록 끝 !-----
        val exitButton = binding.pvFullScreen.findViewById<ImageButton>(R.id.exo_exit)
        exitButton.setOnClickListener {
            chronometer.stop()
            showExitDialog()
        }

        // ------! 앞으로 감기 뒤로 감기 시작 !------
        val replay5 = binding.pvFullScreen.findViewById<ImageButton>(R.id.exo_replay_5)
        val forward5 = binding.pvFullScreen.findViewById<ImageButton>(R.id.exo_forward_5)
        replay5.setOnClickListener {
            val replayPosition = simpleExoPlayer?.currentPosition?.minus(5000)
            if (replayPosition != null) {
                simpleExoPlayer?.seekTo((if (replayPosition < 0) 0 else replayPosition))
            }
        }
        forward5.setOnClickListener {
            val forwardPosition = simpleExoPlayer?.currentPosition?.plus(5000)
            if (forwardPosition != null) {
                if (forwardPosition < simpleExoPlayer?.duration?.minus(5000)!!) {
                    simpleExoPlayer?.seekTo(forwardPosition)
                } else {
                    simpleExoPlayer!!.pause()
                }
            }
        } // ------! 앞으로 감기 뒤로 감기 끝 !------

        singletonInstance = Singleton_t_measure.getInstance(this)
        // -----! pose landmarker 시작 !-----
        // Initialize our background executor
        backgroundExecutor = Executors.newSingleThreadExecutor()
        // Wait for the views to be properly laid out
        binding.vfPS.post {
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
        }
        if (!hasPermissions(this)) {
            requestPermissions(PERMISSIONS_REQUIRED, REQUEST_CODE_PERMISSIONS
            )
            setUpCamera()
        }
    }

    // ------! exoplayer 함수 시작 !------
    private fun initPlayer(resourceList: ArrayList<String>) {
        simpleExoPlayer = SimpleExoPlayer.Builder(this).build()
        binding.pvFullScreen.player = simpleExoPlayer

        // raw에 있는 것 가져오기
        buildMediaSource(resourceList).let {
            simpleExoPlayer?.prepare(it)
            Log.w("resourcelist in fullscreen", "$resourceList")
        }
        simpleExoPlayer?.seekTo(playbackPosition)
//        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
    private fun fullScreen(fullScreenOption : Int) {
        window.decorView.systemUiVisibility = (
                fullScreenOption
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN )
    }
    override fun onWindowFocusChanged(hasFocus : Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(hasFocus) fullScreen(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }
    // -----! 동영상 재생목록에 넣기 !-----
    private fun buildMediaSource(resourceList: ArrayList<String>) : MediaSource {
        val dataSourceFactory = DefaultDataSourceFactory(this, "MHG")
        val concatenatingMediaSource = ConcatenatingMediaSource()
        resourceList.forEach { url ->
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(url))
            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        return concatenatingMediaSource
    }
    private fun showExitDialog() {
        MaterialAlertDialogBuilder(this@PlaySkeletonActivity, R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setTitle("알림")
            setMessage("운동을 종료하시겠습니까 ?")
            setPositiveButton("예") { dialog, _ ->
                // 소요 시간
                val intent = Intent(this@PlaySkeletonActivity, MainActivity::class.java)
                startActivity(intent)
                finishAffinity()

            }
            setNegativeButton("아니오") { dialog, _ ->
                chronometer.start()
            }
            create()
        }.show()
    }
    // ------! exoplayer 함수 끝 !------

    override fun onResume() {
        super.onResume()
        if (!hideIndicator) {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }
        simpleExoPlayer?.playWhenReady = true

        if (!hasPermissions(this)) {
            ActivityCompat.requestPermissions(this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        } else {
            // 권한이 이미 부여된 경우 카메라 설정을 진행합니다.
            setUpCamera()
        }

        backgroundExecutor.execute {
            if(this::poseLandmarkerHelper.isInitialized) {
                if (poseLandmarkerHelper.isClose()) {
                    poseLandmarkerHelper.setupPoseLandmarker()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        simpleExoPlayer?.stop()
        simpleExoPlayer?.playWhenReady = false
    }

    override fun onPause() {
        super.onPause()
        if (hideIndicator) {
            sensorManager.unregisterListener(this)
        }
        if(this::poseLandmarkerHelper.isInitialized) {
            viewModel.setMinPoseDetectionConfidence(poseLandmarkerHelper.minPoseDetectionConfidence)
            viewModel.setMinPoseTrackingConfidence(poseLandmarkerHelper.minPoseTrackingConfidence)
            viewModel.setMinPosePresenceConfidence(poseLandmarkerHelper.minPosePresenceConfidence)
            viewModel.setDelegate(poseLandmarkerHelper.currentDelegate)

            // Close the PoseLandmarkerHelper and release resources
            backgroundExecutor.execute { poseLandmarkerHelper.clearPoseLandmarker() }
//            unbindService(serviceConnection as ServiceConnection)

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        simpleExoPlayer?.release()
        _binding = null
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("playbackPosition", simpleExoPlayer?.currentPosition ?: 0L)
        outState.putInt("currentWindow", simpleExoPlayer?.currentWindowIndex ?: 0)
        outState.putBoolean("playWhenReady", simpleExoPlayer?.playWhenReady ?: true)
    }

    // ------! 센서 시작!------
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
            (binding.cvPSIndicator.layoutParams as ConstraintLayout.LayoutParams).verticalBias = currentBias
            binding.cvPSIndicator.requestLayout()

            if (normalizedAngle in 88f..92f) {
                binding.cvPSIndicator.setCardBackgroundColor(ContextCompat.getColor(this, R.color.mainColor))
            } else {
                binding.cvPSIndicator.setCardBackgroundColor(ContextCompat.getColor(this, R.color.subColor100))
            }
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    // ------! 센서 끝 !------

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
        preview?.setSurfaceProvider(binding.vfPS.surfaceProvider)
        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        // 미리보기. 4:3 비율만 사용합니다. 이것이 우리 모델에 가장 가깝기 때문입니다.
        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
//            .setTargetRotation(binding.viewFinder.display.rotation)
            .setTargetRotation(Surface.ROTATION_90)
            .build()

        // 이미지 분석. RGBA 8888을 사용하여 모델 작동 방식 일치
        imageAnalyzer =
            ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_DEFAULT)
//                .setTargetRotation(binding.viewFinder.display.rotation)
                .setTargetRotation(Surface.ROTATION_0)
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
            .setTargetRotation(binding.vfPS.display.rotation)
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
            preview?.setSurfaceProvider(binding.vfPS.surfaceProvider)
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
            binding.vfPS.display.rotation
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
                binding.olPS.setResults(
                    resultBundle.results.first(),
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    RunningMode.LIVE_STREAM
                )

                // 여기에 resultbundle(poselandarkerhelper.resultbundle)이 들어감.

                binding.olPS.invalidate()
                latestResult = resultBundle
            }
        }
    }

    private fun recognizeNCountExercise(resultBundle: PoseLandmarkerHelper.ResultBundle, exerciseId: Int) {
        when (exerciseId) {
            in 47 .. 74 -> {

            }
        }

    }
}