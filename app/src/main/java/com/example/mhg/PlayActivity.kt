package com.example.mhg

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mhg.databinding.ActivityPlayBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.RawResourceDataSource
import com.google.android.exoplayer2.util.Util
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PlayActivity : AppCompatActivity() {
    lateinit var binding : ActivityPlayBinding
    private var videoUrl = "http://techslides.com/demos/sample-videos/small.mp4"
//    private lateinit var cameraExecutor: ExecutorService

    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var player : SimpleExoPlayer? = null
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        if (allPermissionsGranted()) {
//            startCamera()
//        } else {
//            ActivityCompat.requestPermissions(
//                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
//            )
//        }
//        cameraExecutor = Executors.newSingleThreadExecutor()
//        binding.pcvPlay.showTimeoutMs = 0
        initPlayer()
    }

    private fun initPlayer(){
        simpleExoPlayer = SimpleExoPlayer.Builder(this).build()
        binding.pvPlay.player = simpleExoPlayer
        buildMediaSource()?.let {
            simpleExoPlayer?.prepare(it)
        }
    }
    private fun buildMediaSource() : MediaSource {
        val dataSourceFactory = DefaultDataSourceFactory(this, "sample")
        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoUrl))

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

    override fun onDestroy() {
        super.onDestroy()
        simpleExoPlayer?.release()
    }
    // -----  오디오 플레이어 코드 시작  -----
//    override fun onStart() {
//        super.onStart()
//        if (Util.SDK_INT >= 24) {
//            initializePlayer()
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
////        hideSystemUi()
//        if ((Util.SDK_INT < 24 || player == null)) {
//            initializePlayer()
//        }
//    }
//    override fun onPause() {
//        super.onPause()
//        if (Util.SDK_INT < 24) {
//            releasePlayer()
//        }
//    }
//
//    override fun onStop() {
//        super.onStop()
//        if (Util.SDK_INT >= 24) {
//            releasePlayer()
//        }
//    }
//    private fun releasePlayer() {
//        player?.run {
//            playbackPosition = this.currentPosition
//            currentWindow = this.currentWindowIndex
//            playWhenReady = this.playWhenReady
//            release()
//        }
//        player = null
//    }
//    private fun initializePlayer() {
//        player = SimpleExoPlayer.Builder(this)
//            .build()
//            .also { exoPlayer ->
//                binding.pcvPlay.player = exoPlayer
//
//                // -----! 노래 (미디어) 항목 만들기 ! ------
//                // uri에서 가져오기
////                val mediaItem = com.google.android.exoplayer2.MediaItem.fromUri(getString(R.string.media_url_mp3))
//                val mediaItem = com.google.android.exoplayer2.MediaItem.fromUri(RawResourceDataSource.buildRawResourceUri(R.raw.winner_winner_funky_chicken_dinner))
//
//                exoPlayer.setMediaItem(mediaItem)
//                exoPlayer.playWhenReady = playWhenReady
//                exoPlayer.seekTo(currentWindow, playbackPosition)
//                exoPlayer.prepare()
//            }
//    }  // -----  오디오 플레이어 코드 끝  -----


//    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//        cameraProviderFuture.addListener({
//            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
//
//            val preview = Preview.Builder()
//                .build()
//                .also {
//                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
//                }
//            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
//            try {
//                cameraProvider.unbindAll()
//                cameraProvider.bindToLifecycle(
//                    this, cameraSelector, preview
//                )
//            } catch (exc: Exception) {
//                Log.e("실패", "USE CASE binding failed", exc)
//            }
//        }, ContextCompat.getMainExecutor(this))
//
//    }
//
//    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
//        ContextCompat.checkSelfPermission(
//            baseContext, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
//    }
//
//    companion object {
//        private const val TAG = "CameraXApp"
//        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
//        private const val REQUEST_CODE_PERMISSIONS = 10
//        private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == REQUEST_CODE_PERMISSIONS) {
//            if (allPermissionsGranted()) {
//                startCamera()
//            } else {
//                Toast.makeText(this, "접근 권한이 허용되지 않아 카메라를 실행할 수 없습니다. 설정에서 접근 권한을 허용해주세요", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        cameraExecutor.shutdown()
//    }
}