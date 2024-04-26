package com.example.mhg

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.annotation.RequiresApi
import com.example.mhg.VO.ExerciseVO
import com.example.mhg.databinding.ActivityPlayBinding
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory

class PlayActivity : AppCompatActivity() {
    lateinit var binding : ActivityPlayBinding
    private var videoUrl = "http://gym.tangostar.co.kr/data/contents/videos/걷기.mp4"

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

        val exerciseData: ExerciseVO? = intent.getParcelableExtra("exercise")
        Log.w(TAG, "$exerciseData")

//        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // -----! 각 설명들 textView에 넣기 !-----
        videoUrl = exerciseData?.videoFilepath.toString()
        Log.w("동영상url", videoUrl)
        binding.tvPlayExerciseName.text = exerciseData?.exerciseName.toString()
        binding.tvPlayExerciseDscript.text = exerciseData?.exerciseDescription.toString()
        binding.tvPlayExerciseRelateSymptom.text = exerciseData?.relatedSymptom.toString()
        binding.tvPlayExerciseRelateJoint.text = exerciseData?.relatedJoint.toString()
        binding.tvPlayExerciseRelateSymptom.text = exerciseData?. relatedSymptom.toString()

        binding.tvPlayExerciseStage.text = exerciseData?.exerciseStage.toString()
        binding.tvPlayExerciseFrequency.text = exerciseData?.exerciseFequency.toString()
        binding.tvPlayExerciseIntensity.text = exerciseData?.exerciseIntensity.toString()
        binding.tvPlayExerciseInitialPosture.text = exerciseData?.exerciseInitialPosture.toString()
        binding.tvPlayExerciseMethod.text = exerciseData?.exerciseMethod.toString()
        binding.tvPlayExerciseCaution.text = exerciseData?.exerciseCaution.toString()


        playbackPosition = intent.getLongExtra("current_position", 0L)
        initPlayer()
        // -----! 하단 운동 시작 버튼 시작 !-----
        binding.btnExercisePlay.setOnClickListener {
//            val intent = Intent(this, PlayFullScreenActivity::class.java)
//            intent.putExtra("video_url", videoUrl)
//            intent.putExtra("current_position", simpleExoPlayer?.currentPosition)
//            startActivityForResult(intent, 8080)
            val intent = Intent(this, PlaySkeletonActivity::class.java)
            intent.putExtra("video_url", videoUrl)
            startActivityForResult(intent, 8080)
        } // -----! 하단 운동 시작 버튼 끝 !-----
//        // -----! 전체화면 구현 로직 시작 !-----
//        val fullscreenButton = binding.pvPlay.findViewById<ImageButton>(com.google.android.exoplayer2.ui.R.id.exo_fullscreen)
//
//        fullscreenButton.setOnClickListener {
//            val intent = Intent(this, PlayFullScreenActivity::class.java)
//            intent.putExtra("video_url", videoUrl)
//            intent.putExtra("current_position", simpleExoPlayer?.currentPosition)
//
//            startActivityForResult(intent, 8080)
//        }
        val exitButton = binding.pvPlay.findViewById<ImageButton>(R.id.exo_exit)
        exitButton.setOnClickListener {
            finish()
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

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 8080 && resultCode == Activity.RESULT_OK) {
            val currentPosition = data?.getLongExtra("current_position", 0)
            val VideoUrl = data?.getStringExtra("video_url")

            videoUrl = VideoUrl.toString()
            playbackPosition = currentPosition!!
            initPlayer()
        }
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