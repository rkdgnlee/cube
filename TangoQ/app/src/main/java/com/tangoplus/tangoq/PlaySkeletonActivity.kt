package com.tangoplus.tangoq

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
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
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.tangoplus.tangoq.viewmodel.SkeletonViewModel
import com.tangoplus.tangoq.databinding.ActivityPlaySkeletonBinding
import com.tangoplus.tangoq.mediapipe.OverlayView
import com.tangoplus.tangoq.mediapipe.PoseLandmarkAdapter
import com.tangoplus.tangoq.mediapipe.PoseLandmarkerHelper
import com.tangoplus.tangoq.db.Singleton_t_measure
import com.tangoplus.tangoq.viewmodel.PlayViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs

private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)

class PlaySkeletonActivity : AppCompatActivity(), SensorEventListener, PoseLandmarkerHelper.LandmarkerListener {
    private lateinit var binding : ActivityPlaySkeletonBinding

    // ------! vertical sensor !------
    private var accelerometer: Sensor? = null
    private lateinit var sensorManager: SensorManager
    private var currentBias = 0f
    private var filteredAngle = 0f
    private val ALPHA = 0.1f
    private val INTERPOLATION_FACTOR = 0.1f
    private var hideIndicator = false
    var exerciseId = ""
    var totalTime = 0

    // ------! exoplayer2 !------
    val pvm: PlayViewModel by viewModels()
    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var playbackPosition = 0L
    private lateinit var chronometer: Chronometer

    // ------! mediapipe !------
    companion object {
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

    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private val viewModel: SkeletonViewModel by viewModels()
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT
    private lateinit var backgroundExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture
    private lateinit var singletonInstance: Singleton_t_measure
    var latestResult: PoseLandmarkerHelper.ResultBundle? = null

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaySkeletonBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        val videoUrl = intent.getStringExtra("video_url")
        exerciseId = intent.getStringExtra("exercise_id").toString()
        val urls = intent.getStringArrayListExtra("urls")
        totalTime = intent.getIntExtra("total_time", 0)
        Log.v("url들", "videoUrl: $videoUrl, urls: $urls")
        val url_list = ArrayList<String>()

        if (!urls.isNullOrEmpty()) {
            url_list.addAll(urls)
        } else if (videoUrl != null) {
            url_list.add(videoUrl)
        }

        if ( url_list.isNotEmpty() ) {
            playbackPosition = intent.getLongExtra("current_position", 0L)
            initPlayer(url_list)
            simpleExoPlayer?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    if (playbackState == Player.STATE_ENDED) {
                        Log.v("currentWindowIndex", "${simpleExoPlayer?.currentWindowIndex}")
                        // ------! 모든 영상 종료 시 자동 이동 !------
                        if (simpleExoPlayer?.currentWindowIndex == url_list.size - 1) {

                            val elapsedMills = SystemClock.elapsedRealtime() - chronometer.base
//                            Log.v("feedback_finish", "VM_exercise_log: ${eViewModel.exerciseLog.value}")
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
                if (forwardPosition < ((simpleExoPlayer?.duration?.minus(5000)) ?: 0)) {
                    simpleExoPlayer?.seekTo(forwardPosition)
                } else {
                    simpleExoPlayer?.pause()
                }
            }
        } // ------! 앞으로 감기 뒤로 감기 끝 !------

        singletonInstance = Singleton_t_measure.getInstance(this)
        // -----! pose landmarker 시작 !-----
        backgroundExecutor = Executors.newSingleThreadExecutor()
        binding.vfPS.post {
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
            requestPermissions(PERMISSIONS_REQUIRED, REQUEST_CODE_PERMISSIONS)
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

    @RequiresApi(Build.VERSION_CODES.R)
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

            backgroundExecutor.execute { poseLandmarkerHelper.clearPoseLandmarker() }
//            unbindService(serviceConnection as ServiceConnection)
            if (hideIndicator) {
                sensorManager.unregisterListener(this)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        simpleExoPlayer?.release()
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )
        sensorManager.unregisterListener(this)
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

    @RequiresApi(Build.VERSION_CODES.R)
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
    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        preview?.setSurfaceProvider(binding.vfPS.surfaceProvider)
        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        val rotation = when (display?.rotation) {
            Surface.ROTATION_90 -> Surface.ROTATION_90
            Surface.ROTATION_270 -> Surface.ROTATION_270
            else -> Surface.ROTATION_0
        }
        // 미리보기. 4:3 비율만 사용합니다. 이것이 우리 모델에 가장 가깝기 때문입니다.
        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(rotation)
            .build()

        // 이미지 분석. RGBA 8888을 사용하여 모델 작동 방식 일치
        imageAnalyzer =
            ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(rotation)
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
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(binding.vfPS.display.rotation)
            .build()

        cameraProvider.unbindAll()
        try {camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer, imageCapture
            )
            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(binding.vfPS.surfaceProvider)
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
    @RequiresApi(Build.VERSION_CODES.R)
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
            // Pass necessary information to OverlayView for drawing on the canvas
            val customResult = PoseLandmarkAdapter.toCustomPoseLandmarkResult(resultBundle.results.first())
            binding.olPS.setResults(
                customResult,
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth,
                OverlayView.RunningMode.LIVE_STREAM
            )

            // 여기에 resultbundle(poselandarkerhelper.resultbundle)이 들어감.

            binding.olPS.invalidate()
            latestResult = resultBundle
            if (!pvm.isDefaultPoseSet) {
                // 5초 후에 setDefaultPoseBundle 함수를 한 번만 실행
                Handler(Looper.getMainLooper()).postDelayed({
                    setDefaultPoseBundle(latestResult)
                    pvm.isDefaultPoseSet = true
                }, 2500)
            } else {
                // setDefaultPoseBundle이 실행된 후에는 매 프레임마다 countBundleToJson 실행
                countBundleToJson(latestResult, exerciseId)
            }
        }
    }

    // ------# 운동 시작 전 자세의 x, y값 저장하기 #------
    enum class horizontalPosition {
        CENTER, LEFT, RIGHT
    }

    enum class verticalPosition {
       CENTER, TOP, DOWN
    }

    private var leftThreshold = 0.00f  // 왼쪽으로 5% 이동
    private var rightThreshold = 0.00f // 오른쪽으로 5% 이동
    private var topThreshold = 0.2f
    private var downThreshold = 0.2f
    private val centerThreshold = 0.02f


    private fun setDefaultPoseBundle(resultBundle: PoseLandmarkerHelper.ResultBundle?) {
        if (resultBundle?.results?.first()?.landmarks()?.isNotEmpty() == true) {
            val plr = resultBundle.results.first().landmarks()[0]
            for (i in 0 until  plr.size) {
                when (i) {
                    0 -> pvm.normalNose = Pair(plr[0].x().toDouble(), plr[0].y().toDouble())
                    in 11 .. 12 -> pvm.normalShoulderData.add(Pair(plr[i].x().toDouble(), plr[i].y().toDouble()))
                    in 13 .. 14 -> pvm.normalElbowData.add(Pair(plr[i].x().toDouble(), plr[i].y().toDouble()))
                    in 15 .. 16 -> pvm.normalWristData.add(Pair(plr[i].x().toDouble(), plr[i].y().toDouble()))
                    in 23 .. 24 -> pvm.normalHipData.add(Pair(plr[i].x().toDouble(), plr[i].y().toDouble()))
                    in 25 .. 26 -> pvm.normalKneeData.add(Pair(plr[i].x().toDouble(), plr[i].y().toDouble()))
                    in 27 .. 28 -> pvm.normalAnkleData.add(Pair(plr[i].x().toDouble(), plr[i].y().toDouble()))
                }
            }
        }
    }

    private fun countBundleToJson(resultBundle: PoseLandmarkerHelper.ResultBundle?, case: String) {
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

        if (resultBundle?.results?.first()?.landmarks()?.isNotEmpty() == true) {
            val plr = resultBundle.results.first().landmarks()[0]
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
//            val ankleXAxis = ankleData[0].first.minus(ankleData[1].first)  / 2
//            val middleHip = Pair((hipData[0].first + hipData[1].first) / 2, (hipData[0].second + hipData[1].second) / 2)
//            val middleShoulder = Pair((shoulderData[0].first + shoulderData[1].first) / 2, (shoulderData[0].second + shoulderData[1].second) / 2)
            /** mutablelist 0 -> 왼쪽 1 -> 오른쪽
             *  그리고 first: x    second: y
             * */

            try {
                if (poseLandmarkerHelper.minPoseDetectionConfidence >= 0.7 || pvm.isCountingEnabled) {
                    // ------# 각 운동 별 count 기준 정하기 #------ TODO 각 부위별로 Threshold 수치를 정하는게 더 효율적..

                    // ------# 각 운동 별 count 경로 정하기 #------
                    when (case) {
                        "74" -> { // 완료
                            leftThreshold = 0.1f
                            rightThreshold = 0.1f
                            countBothDirectMovement("wrist", true, countHorizontalMovement(pvm.normalWristData[1], wristData[1])) // 오른손 기준
                        }
                        "133" -> {
                            leftThreshold = 0.1f
                            rightThreshold = 0.1f
                            countBothDirectMovement("knee", countHorizontalMovement(pvm.normalKneeData[0], kneeData[0]), true)
                        }
                        "134" -> { // 완료
                            leftThreshold = 0.06f
                            rightThreshold = 0.06f
                            countBothDirectMovement("nose", true, countHorizontalMovement(pvm.normalNose, nose))
                        }
                        "171" -> { // 스쿼트 https://gym.tangostar.co.kr/data/contents/videos/2024/98.mp4
                            topThreshold = 0.2f
                            downThreshold = 0.2f
                            countBothDirectMovement("hip",true, countVerticalMovement(pvm.normalHipData[0], hipData[0]))
                        }
                        "197" -> {
                            topThreshold = 0.05f
                            downThreshold = 0.05f
                            leftThreshold = 0.4f
                            rightThreshold = 0.4f
                            countBothDirectMovement("shoulder", countCrossHorizontalMovement(pvm.normalWristData, wristData), countVerticalMovement(pvm.normalShoulderData[0], shoulderData[0]))
                        }
                        "202" -> { // 고양이 자세 https://gym.tangostar.co.kr/data/contents/videos/2024/195.mp4
                            leftThreshold = 0.45f
                            rightThreshold = 0.45f
                            countBothDirectMovement("lieDown", countHorizontalMovement(pvm.normalWristData[1], wristData[1]), countVerticalMovement(pvm.normalShoulderData[0], shoulderData[0]))

                        }
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("PlaySkelIndex", "${e.message}")
            } catch (e: IllegalArgumentException) {
                Log.e("PlaySkelIllegal", "${e.message}")
            } catch (e: IllegalStateException) {
                Log.e("PlaySkelIllegal", "${e.message}")
            }catch (e: NullPointerException) {
                Log.e("PlaySkelNull", "${e.message}")
            } catch (e: java.lang.Exception) {
                Log.e("PlaySkelException", "${e.message}")
            }
        }
    }
    private fun countAndLimitTime(type: String) {
        if (pvm.isCountingEnabled) {
            pvm.count += 1
            pvm.isCountingEnabled = false
            pvm.lastCountTime = System.currentTimeMillis()

            CoroutineScope(Dispatchers.Main).launch {
                when (type) {
                    "neck" -> delay(pvm.countDown5)
                    "knee" -> delay(pvm.countDown8)
                    "lieDown", "shoulder" -> delay(pvm.countDown6)
                    else -> delay(pvm.countDown3_5)
                }
                pvm.isCountingEnabled = true
            }
            Log.e("partCount", "count: ${pvm.count}")
        }
    }

    private fun countBothDirectMovement(type: String,condition1 : Boolean, condition2 : Boolean) {
        Log.w("bothCondition", "out, condition1: $condition1, condition2: $condition2")
        if (condition1 && condition2) {
            CoroutineScope(Dispatchers.Main).launch {
                countAndLimitTime(type)
            }
        }
    }

    private fun countVerticalMovement(normalPart: Pair<Double, Double>, part: Pair<Double, Double>) : Boolean {
        val normalPartY = normalPart.second
        val currentPartY = part.second
        val difference = currentPartY - normalPartY

        val oldPosition = pvm.currentVerticalPosition
        var newPosition = oldPosition
        var counted = false

        when (oldPosition) {
            verticalPosition.CENTER -> {
                if (difference > topThreshold) {
                    newPosition = verticalPosition.TOP
                    pvm.hasMovedVertically = true
                } else if (difference < -downThreshold) { /// -0.3 < -0.2
                    newPosition = verticalPosition.DOWN
                    pvm.hasMovedVertically = true
                }
            }
            verticalPosition.TOP, verticalPosition.DOWN -> {
                if (abs(difference) <= centerThreshold) {
                    newPosition = verticalPosition.CENTER
                    if (pvm.hasMovedVertically) {
                        counted = true
                        pvm.hasMovedVertically = false
                    }
                }
            }
        }
        if (oldPosition != newPosition) {
            pvm.currentVerticalPosition = newPosition
        }

        Log.v("ComparePose", "counted: $counted currentPart: $currentPartY, normalPart: $normalPartY, difference: $difference, position: ${pvm.currentVerticalPosition}")
        return counted
    }
    private fun countHorizontalMovement(normalPart: Pair<Double, Double>, part: Pair<Double, Double>) : Boolean {
        val normalPartX = normalPart.first
        val currentPartX = part.first
        val difference = currentPartX - normalPartX

        val oldPosition = pvm.currentHorizontalPosition
        var newPosition = oldPosition
        var counted = false

        when (oldPosition) {
            horizontalPosition.CENTER -> {
                if (difference < -leftThreshold) {
                    newPosition = horizontalPosition.LEFT
                    pvm.hasMovedHorizontally = true
                } else if (difference > rightThreshold) {
                    newPosition = horizontalPosition.RIGHT
                    pvm.hasMovedHorizontally = true
                }
            }
            horizontalPosition.LEFT, horizontalPosition.RIGHT -> {
                if (abs(difference) <= centerThreshold) {
                    newPosition = horizontalPosition.CENTER
                    if (pvm.hasMovedHorizontally) {
                        counted = true
                        pvm.hasMovedHorizontally = false
                    }
                }
            }
        }
        if (oldPosition != newPosition) {
            pvm.currentHorizontalPosition = newPosition
        }
        Log.v("ComparePose", "currentPart: $currentPartX, normalPart: $normalPartX, difference: $difference, position: ${pvm.currentHorizontalPosition}")
        return counted
    }

    private fun countCrossHorizontalMovement(normalPart: MutableList<Pair<Double, Double>>, part: MutableList<Pair<Double, Double>>) : Boolean {
        val leftNormalPartX = normalPart[0].first
        val leftCurrentPartX = part[0].first
        val leftDifference = leftNormalPartX - leftCurrentPartX

        val rightNormalPartX = normalPart[1].first
        val rightCurrentPartX = part[1].first
        val rightDifference = rightNormalPartX - rightCurrentPartX

        val leftOldPosition = pvm.currentLeftHorizontalPosition
        val rightOldPosition = pvm.currentRightHorizontalPosition
        var leftNewPosition = leftOldPosition
        var rightNewPosition = rightOldPosition
        var counted = false

        when (leftOldPosition) {
            horizontalPosition.CENTER -> {
                if (leftDifference < -leftThreshold) {
                    leftNewPosition = horizontalPosition.LEFT
                    pvm.hasMovedLeftHorizontally = true
                } else if (leftDifference > rightThreshold) {
                    leftNewPosition = horizontalPosition.RIGHT
                    pvm.hasMovedLeftHorizontally = true
                }
            }
            horizontalPosition.LEFT, horizontalPosition.RIGHT -> {
                if (abs(leftDifference) <= centerThreshold) {
                    leftNewPosition = horizontalPosition.CENTER
                    if (pvm.hasMovedLeftHorizontally) {
                        pvm.hasMovedLeftHorizontally = false
                        counted = true
                    }
                }
            }
        }

        // 오른쪽 손목 처리
        when (rightOldPosition) {
            horizontalPosition.CENTER -> {
                if (rightDifference < -leftThreshold) {
                    rightNewPosition = horizontalPosition.LEFT
                    pvm.hasMovedRightHorizontally = true
                } else if (rightDifference > rightThreshold) {
                    rightNewPosition = horizontalPosition.RIGHT
                    pvm.hasMovedRightHorizontally = true
                }
            }
            horizontalPosition.LEFT, horizontalPosition.RIGHT -> {
                if (abs(rightDifference) <= centerThreshold) {
                    rightNewPosition = horizontalPosition.CENTER
                    if (pvm.hasMovedRightHorizontally) {
                        pvm.hasMovedRightHorizontally = false
                        counted = counted || true  // 둘 중 하나라도 카운트되면 true
                    }
                }
            }
        }

        if (leftOldPosition != leftNewPosition) {
            pvm.currentLeftHorizontalPosition = leftNewPosition
        }
        if (rightOldPosition != rightNewPosition) {
            pvm.currentRightHorizontalPosition = rightNewPosition
        }

        // 교차 움직임 확인
        val isCrossed = (leftNewPosition == horizontalPosition.RIGHT && rightNewPosition == horizontalPosition.LEFT) ||
                (leftNewPosition == horizontalPosition.LEFT && rightNewPosition == horizontalPosition.RIGHT)

        Log.v("ComparePose", "Left Wrist - current: $leftCurrentPartX, normal: $leftNormalPartX, difference: $leftDifference, position: $leftNewPosition")
        Log.v("ComparePose", "Right Wrist - current: $rightCurrentPartX, normal: $rightNormalPartX, difference: $rightDifference, position: $rightNewPosition")
        Log.v("ComparePose", "Crossed: $isCrossed, Counted: $counted")

        return counted
    }


}