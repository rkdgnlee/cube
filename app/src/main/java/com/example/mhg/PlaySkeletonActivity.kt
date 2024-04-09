package com.example.mhg

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mhg.databinding.ActivityPlaySkeletonBinding
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias LumaListener = (luma: Double) -> Unit

class PlaySkeletonActivity : AppCompatActivity() {
    lateinit var binding : ActivityPlaySkeletonBinding
    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var player : SimpleExoPlayer? = null

    private var playbackPosition = 0L
    private lateinit var cameraExecutor: ExecutorService
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPlaySkeletonBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val videoUrl = intent.getStringExtra("video_url")
        if (videoUrl != null) {
            playbackPosition = intent.getLongExtra("current_position", 0L)
            initPlayer(videoUrl)
        }
        val exitButton = binding.pvPlaySkeleton.findViewById<ImageButton>(R.id.exo_exit)
        exitButton.setOnClickListener {
            showExitDialog()
        }
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun initPlayer(videoUrl : String) {
        simpleExoPlayer = SimpleExoPlayer.Builder(this).build()
        binding.pvPlaySkeleton.player = simpleExoPlayer

        // raw에 있는 것 가져오기
//        buildMediaSource(resourceList).let {
//            simpleExoPlayer?.prepare(it)
//            Log.w("resourcelist in fullscreen", "$resourceList")
//        }
        val dataSourceFactory = DefaultDataSourceFactory(this, "MHG")
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoUrl))
        simpleExoPlayer!!.prepare(mediaSource)
        simpleExoPlayer?.seekTo(playbackPosition)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
    // -----! 동영상 재생목록에 넣기 !-----
//    private fun buildMediaSource(resourceList: ArrayList<String>) : MediaSource {
//        val dataSourceFactory = DefaultDataSourceFactory(this, "MHG")
//        val concatenatingMediaSource = ConcatenatingMediaSource()
//        resourceList.forEach { url ->
//            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
//                .createMediaSource(MediaItem.fromUri(url))
//            concatenatingMediaSource.addMediaSource(mediaSource)
//        }
//        return concatenatingMediaSource
//    }
    private fun showExitDialog() {
        MaterialAlertDialogBuilder(this@PlaySkeletonActivity, R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setTitle("알림")
            setMessage("운동을 종료하시겠습니까 ?")
            setPositiveButton("예") { dialog, _ ->
//                // TODO feedback activity에서 운동 기록 데이터 백그라운드 작업 전송 필요
//                val intent= Intent(this@PlaySkeletonActivity, FeedbackActivity::class.java)
//                startActivity(intent)
                finish()
            }
            setNegativeButton("아니오") { dialog, _ ->
            }
            create()
        }.show()
    }
    private fun startCamera() {
        var cameraController =  LifecycleCameraController(baseContext)
        val preview : PreviewView = binding.previewView
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()
        cameraController.cameraSelector = cameraSelector
//        val options = FaceDetectorOptions.Builder()
//            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
//            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
//            .build()
//
//        faceDetector = FaceDetection.getClient(options)
//        cameraController.setImageAnalysisAnalyzer(
//            ContextCompat.getMainExecutor(this),
//            MlKitAnalyzer()
//        )
        cameraController.bindToLifecycle(this)
        binding.previewView.controller = cameraController

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
//        private const val TAG = "CameraXApp"
//        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "접근 권한이 허용되지 않아 카메라를 실행할 수 없습니다. 설정에서 접근 권한을 허용해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // 일시중지
    override fun onResume() {
        super.onResume()
        simpleExoPlayer?.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        simpleExoPlayer?.stop()
        simpleExoPlayer?.playWhenReady = false
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("playbackPosition", simpleExoPlayer?.currentPosition ?: 0L)
        outState.putInt("currentWindow", simpleExoPlayer?.currentWindowIndex ?: 0)
        outState.putBoolean("playWhenReady", simpleExoPlayer?.playWhenReady ?: true)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()

    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        simpleExoPlayer?.release()
    }
}
