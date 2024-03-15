package com.example.mhg

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mhg.VO.HomeRVBeginnerDataClass
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
    private var videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"

    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var player : SimpleExoPlayer? = null
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition = 0L
    
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val exerciseData: HomeRVBeginnerDataClass? = intent.getParcelableExtra("ExerciseData", HomeRVBeginnerDataClass::class.java)
        Log.w(TAG, "$exerciseData")
//        videoUrl = exerciseData?.videoFilepath.toString()
//        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        // -----! 각 설명들 textView에 넣기 !-----
        binding.tvPlayExerciseStage.text = exerciseData?.exerciseStage.toString()
        binding.tvPlayExerciseFrequency.text = exerciseData?.exerciseFequency.toString()
        binding.tvPlayExerciseIntensity.text = exerciseData?.exerciseIntensity.toString()
        binding.tvPlayExerciseInitialPosture.text = exerciseData?.exerciseInitialPosture.toString()
        binding.tvPlayExerciseMethod.text = exerciseData?.exerciseMethod.toString()
        binding.tvPlayExerciseCaution.text = exerciseData?.exerciseCaution.toString()




        // 인텐트로부터 재생 시간 가져오기
        playbackPosition = intent.getLongExtra("current_position", 0L)
        initPlayer()

        // -----! 전체화면 구현 로직 시작 !-----
        val fullscreenButton = binding.pvPlay.findViewById<ImageButton>(com.google.android.exoplayer2.ui.R.id.exo_fullscreen)

        fullscreenButton.setOnClickListener {
            val intent = Intent(this, FullScreenActivity::class.java)
            intent.putExtra("current_position", simpleExoPlayer?.currentPosition)
            startActivity(intent)
        }

    }

    private fun initPlayer(){
        simpleExoPlayer = SimpleExoPlayer.Builder(this).build()
        binding.pvPlay.player = simpleExoPlayer
        buildMediaSource()?.let {
            simpleExoPlayer?.prepare(it)
        }
        simpleExoPlayer?.seekTo(playbackPosition)
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
//        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("playbackPosition", simpleExoPlayer?.currentPosition ?: 0L)
        outState.putInt("currentWindow", simpleExoPlayer?.currentWindowIndex ?: 0)
        outState.putBoolean("playWhenReady", simpleExoPlayer?.playWhenReady ?: true)
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



}