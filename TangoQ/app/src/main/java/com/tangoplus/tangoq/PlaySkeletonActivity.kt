package com.tangoplus.tangoq

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.tangoplus.tangoq.data.SkeletonViewModel
import com.tangoplus.tangoq.mediapipe.PoseLandmarkerHelper
import com.tangoplus.tangoq.databinding.ActivityPlaySkeletonBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs

typealias LumaListener = (luma: Double) -> Unit
private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)

class PlaySkeletonActivity : AppCompatActivity(), PoseLandmarkerHelper.LandmarkerListener {

    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var player : SimpleExoPlayer? = null
    private var playbackPosition = 0L
    private lateinit var cameraExecutor: ExecutorService

    // ------! POSE LANDMARKER 설정 시작 !------
    companion object {
        private const val TAG = "Pose Landmarker"
        private const val REQUEST_CODE_PERMISSIONS = 10
        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
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
        if (!hasPermissions(this)) {
//            Navigation.findNavController(
//                requireActivity(), R.id.fragment_container
//            ).navigate(R.id.action_camera_to_permissions)
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
    }

    // ------! POSE LANDMARKER 설정 끝 !------

    @SuppressLint("Recycle")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _fragmentCameraBinding = ActivityPlaySkeletonBinding.inflate(layoutInflater)
        setContentView(_fragmentCameraBinding!!.root)

        // ------! 안내 문구 사라짐 시작 !------
        val tvPSGuide = findViewById<TextView>(R.id.tvPSGuide)

        val animator = ObjectAnimator.ofFloat(tvPSGuide, "alpha", 1f, 0f)
        animator.duration = 2500
        animator.addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                tvPSGuide.visibility = View.GONE
            }
        })
        Handler(Looper.getMainLooper()).postDelayed({
            animator.start()
        }, 2000)



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
        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        // 미리보기. 4:3 비율만 사용합니다. 이것이 우리 모델에 가장 가깝기 때문입니다.
        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .build()

        // 이미지 분석. RGBA 8888을 사용하여 모델 작동 방식 일치
        imageAnalyzer =
            ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
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

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // 여기에는 다양한 사용 사례가 전달될 수 있습니다.
            // 카메라는 CameraControl 및 CameraInfo에 대한 액세스를 제공합니다.
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
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
//                Toast.makeText(this, "", Toast.LENGTH_SHORT).show()
                Log.v("스켈레톤 Init", "동작 성공")
            } else {
//                Toast.makeText(this, "Permission request denied", Toast.LENGTH_SHORT).show()
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
        val earData = mutableListOf<Pair<Float, Float>>()
        val shoulderData = mutableListOf<Pair<Float, Float>>()
        val elbowData = mutableListOf<Pair<Float, Float>>()
        val wristData = mutableListOf<Pair<Float, Float>>()
        val indexData = mutableListOf<Pair<Float, Float>>()
        val hipData = mutableListOf<Pair<Float, Float>>()
        val kneeData = mutableListOf<Pair<Float, Float>>()
        val ankleData = mutableListOf<Pair<Float, Float>>()
        val heelData = mutableListOf<Pair<Float, Float>>()

        if (resultBundle.results.first().landmarks().isNotEmpty()) {

            val poseLandmarkerResult = resultBundle.results.first().landmarks()[0]
            Log.v("poseLandmarkerResult결과", "$poseLandmarkerResult")
            for (i in 7 until  poseLandmarkerResult.size) {
                when (i) {
                    in 7 .. 8 -> earData.add(Pair(poseLandmarkerResult[i].x(), poseLandmarkerResult[i].y()))
                    in 11 .. 12 -> shoulderData.add(Pair(poseLandmarkerResult[i].x(), poseLandmarkerResult[i].y()))
                    in 13 .. 14 -> elbowData.add(Pair(poseLandmarkerResult[i].x(), poseLandmarkerResult[i].y()))
                    in 15 .. 16 -> wristData.add(Pair(poseLandmarkerResult[i].x(), poseLandmarkerResult[i].y()))
                    in 19 .. 20 -> indexData.add(Pair(poseLandmarkerResult[i].x(), poseLandmarkerResult[i].y()))
                    in 23 .. 24 -> hipData.add(Pair(poseLandmarkerResult[i].x(), poseLandmarkerResult[i].y()))
                    in 25 .. 26 -> kneeData.add(Pair(poseLandmarkerResult[i].x(), poseLandmarkerResult[i].y()))
                    in 27 .. 28 -> ankleData.add(Pair(poseLandmarkerResult[i].x(), poseLandmarkerResult[i].y()))
                    in 29 .. 30 -> heelData.add(Pair(poseLandmarkerResult[i].x(), poseLandmarkerResult[i].y()))
                }
            }
            val nose : Pair<Float, Float> = Pair(poseLandmarkerResult[0].x(), poseLandmarkerResult[0].y())
//             -------! 왼쪽 그니까 홀수번이 높을 경우 true (left 가 true)
            val shoulderAngle : Pair<Float, Boolean> = Pair(abs(shoulderData[0].second.minus(shoulderData[1].second))  , if (shoulderData[0].second > shoulderData[1].second) true else false)
            val elbowAngle : Pair<Float, Boolean> = Pair(abs(elbowData[0].second.minus(elbowData[1].second)) , if (elbowData[0].second > elbowData[1].second) true else false)
            val wristAngle : Pair<Float, Boolean> = Pair(abs(wristData[0].second.minus(wristData[1].second)) , if (wristData[0].second > wristData[1].second) true else false)
            val hipAngle : Pair<Float, Boolean> = Pair(abs(hipData[0].second.minus(hipData[1].second)) , if (hipData[0].second > hipData[1].second) true else false)
            val kneeAngle : Pair<Float, Boolean> = Pair(abs(kneeData[0].second.minus(kneeData[1].second)) , if (kneeData[0].second > kneeData[1].second) true else false)
            val ankleAngle : Pair<Float, Boolean> = Pair(abs(ankleData[0].second.minus(ankleData[1].second)) , if (ankleData[0].second > ankleData[1].second) true else false)
            Log.v("1 전면 기울기, 기울어짐", "어깨: $shoulderAngle, 팔꿈치: $elbowAngle, 손목: $wristAngle, 엉덩이: $hipAngle, 무릎: $kneeAngle, 발목: $ankleAngle")
            val ankleXAxis = ankleData[0].first.minus(ankleData[1].first)

            val shoulderDistance : Pair<Float, Float> = Pair(abs(shoulderData[0].first.minus(ankleXAxis)), shoulderData[1].first.minus(ankleXAxis))
            val elbowDistance : Pair<Float, Float> = Pair(abs(elbowData[0].first.minus(ankleXAxis)), elbowData[1].first.minus(ankleXAxis))
            val wristDistance : Pair<Float, Float> = Pair(abs(wristData[0].first.minus(ankleXAxis)), wristData[1].first.minus(ankleXAxis))
            val hipDistance : Pair<Float, Float> = Pair(abs(hipData[0].first.minus(ankleXAxis)), hipData[1].first.minus(ankleXAxis))
            val kneeDistance : Pair<Float, Float> = Pair(abs(kneeData[0].first.minus(ankleXAxis)), kneeData[1].first.minus(ankleXAxis))
            val ankleDistance : Pair<Float, Float> = Pair(abs(ankleData[0].first.minus(ankleXAxis)), ankleData[1].first.minus(ankleXAxis))
            Log.v("2 전면 거리", "어깨: $shoulderDistance, 팔꿈치: $elbowDistance, 손목: $wristDistance, 엉덩이: $hipDistance, 무릎: $kneeDistance, 발목: $ankleDistance")

            // ------! 전면 기울기 !------
            val bicepsLean: Pair<Float, Float> = Pair(calculateSlope(shoulderData[0].first, shoulderData[0].second, elbowData[0].first, elbowData[0].second), calculateSlope(shoulderData[1].first, shoulderData[1].second, elbowData[1].first, elbowData[1].second))
            val forearmsLean: Pair<Float, Float> = Pair(calculateSlope(elbowData[0].first, elbowData[0].second, wristData[0].first, wristData[0].second), calculateSlope(elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second))
            val thighsLean: Pair<Float, Float> = Pair(calculateSlope(hipData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second), calculateSlope(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second))
            Log.v("3 팔 기울기", "이두: $bicepsLean, 전완: $forearmsLean, 허벅지: $thighsLean")

            // ------! 측면 기울기 !------
            val sideLeftBicepsLean : Pair<Float, Float> = Pair(calculateSlope(shoulderData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second), calculateSlope(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second))
            val sideLeftForearmsLean: Pair<Float, Float> = Pair(calculateSlope(elbowData[0].first, elbowData[0].second, wristData[0].first, wristData[0].second), calculateSlope(elbowData[1].first, elbowData[1].second, wristData[1].first, wristData[1].second))
            val sideLeftThighsLean : Pair<Float, Float> = Pair(calculateSlope(hipData[0].first, hipData[0].second, kneeData[0].first, kneeData[0].second), calculateSlope(hipData[1].first, hipData[1].second, kneeData[1].first, kneeData[1].second))
            val sideShoulderDistance: Pair<Float, Float> = Pair(abs(shoulderData[0].first.minus(ankleXAxis)), shoulderData[1].first.minus(ankleXAxis))
            val sideHipDistance: Pair<Float, Float> = Pair(abs(hipData[0].first.minus(ankleXAxis)), hipData[1].first.minus(ankleXAxis))
            Log.v("4 측면 기울기", "어깨: $sideShoulderDistance, 엉덩: $sideHipDistance 이두: $sideLeftBicepsLean, 전완: $sideLeftForearmsLean, 허벅지: $sideLeftThighsLean")

            // ------! 후면 기울기 !------
            val backWaistLean : Float = calculateSlope(nose.first, nose.second, ankleXAxis, hipData[0].second) // ankle의 x값, hip의 y값으로 기울기 계산
            val backfootLean : Pair<Float, Float> = Pair(calculateSlope(kneeData[0].first, kneeData[0].second, heelData[0].first, heelData[0].second), calculateSlope(kneeData[1].first, kneeData[1].second, heelData[1].first, heelData[1].second))
            Log.v("5 후면 기울기 ", "허리: $backWaistLean, 발: $backfootLean")

            // ------! 후면 거리 !------
            val backShoulderDistance : Pair<Float, Float> = Pair(abs(shoulderData[0].first.minus(ankleXAxis)), shoulderData[1].first.minus(ankleXAxis))
            val backElbowDistance : Pair<Float, Float> = Pair(abs(elbowData[0].first.minus(ankleXAxis)), elbowData[1].first.minus(ankleXAxis))
            val backWRistDistance : Pair<Float, Float> = Pair(abs(wristData[0].first.minus(ankleXAxis)), wristData[1].first.minus(ankleXAxis))
            val backHipDistance : Pair<Float, Float> = Pair(abs(hipData[0].first.minus(ankleXAxis)), hipData[1].first.minus(ankleXAxis))
            val backKneeDistance : Pair<Float, Float> = Pair(abs(kneeData[0].first.minus(ankleXAxis)), kneeData[1].first.minus(ankleXAxis))
            val backAnkleDistance : Pair<Float, Float> = Pair(abs(ankleData[0].first.minus(ankleXAxis)), ankleData[1].first.minus(ankleXAxis))
            Log.v("6 후면 거리", "어깨: $backShoulderDistance, 팔꿈치: $backElbowDistance, 손목: $backWRistDistance, 엉덩이: $backHipDistance, 무릎: $backKneeDistance, 발목: $backAnkleDistance")

            // ------! 후면 기울기 !------
            val sitBackNeckDistance : Pair<Float, Float> = Pair(abs(earData[0].second.minus(shoulderData[0].second)), abs(earData[1].second.minus(shoulderData[1].second)))
            val sitBackShoulderDistance  : Pair<Float, Float> = Pair(abs(shoulderData[0].first.minus(ankleXAxis)), shoulderData[1].first.minus(ankleXAxis))
            val sitBackHipDistance : Pair<Float, Float> = Pair(abs(hipData[0].first.minus(ankleXAxis)), hipData[1].first.minus(ankleXAxis))
            Log.v("7 후면 기울기", "목: $sitBackNeckDistance, 어깨: $sitBackShoulderDistance, 엉덩: $sitBackHipDistance")

            // ------! 스쿼트 자세 기울기 !------
            val squatHandsLean : Float = calculateSlope(indexData[0].first, indexData[0].second, indexData[1].first, indexData[1].second)
            val squatHipLean : Float = calculateSlope(hipData[0].first, hipData[0].second, hipData[1].first, hipData[1].second)
            val squatKneeLean : Float = calculateSlope(kneeData[0].first, kneeData[0].second, kneeData[1].first, kneeData[1].second)
            Log.v("8 스쿼트 자세 기울기", "손 끝: $squatHandsLean, 엉덩: $squatHipLean, 무릎: $squatKneeLean")
        }

    }
    fun calculateSlope(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return (y2 - y1) / (x2 - x1)
    }

}