package com.example.mhg

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import com.example.mhg.VO.ExerciseViewModel
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.ActivityPlayFullScreenBinding
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory

class PlayFullScreenActivity : AppCompatActivity() {
        lateinit var binding: ActivityPlayFullScreenBinding
        lateinit var resultLauncher: ActivityResultLauncher<Intent>
        private var videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
        val viewModel: ExerciseViewModel by viewModels()
        private var simpleExoPlayer: SimpleExoPlayer? = null
        private var player : SimpleExoPlayer? = null
        private var playWhenReady = true
        private var currentWindow = 0
        private var playbackPosition = 0L
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            enableEdgeToEdge()
            binding = ActivityPlayFullScreenBinding.inflate(layoutInflater)
            setContentView(binding.root)

//            // -----! landscape로 방향 설정 & 재생시간 받아오기 !-----
//            videoUrl = intent.getStringExtra("video_url").toString()
//            playbackPosition = intent.getLongExtra("current_position", 0L)
//            initPlayer()

           // -----! 받아온 즐겨찾기 재생 목록 시작 !-----
            val resourceList = intent.getStringArrayListExtra("resourceList")
            if (resourceList != null) {
                simpleExoPlayer = SimpleExoPlayer.Builder(this).build()
                binding.pvFullScreen.player = simpleExoPlayer
                buildMediaSource(resourceList).let {
                    simpleExoPlayer?.prepare(it)
                    Log.w("resourcelist in fullscreen", "$resourceList")
                }
                simpleExoPlayer?.seekTo(playbackPosition)
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

                // -----! 영상 간 1초의 간격 !-----
                simpleExoPlayer!!.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        super.onPlaybackStateChanged(playbackState)
                        if (playbackState == Player.STATE_ENDED) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                simpleExoPlayer?.next()
                                // TODO : 여기에 History에 대한 걸 넣어야 함 (통신)
                            }, 1000)
                        }
                    }
                })
            }
            // -----! 받아온 즐겨찾기 재생 목록 끝 !-----
            val exitButton = binding.pvFullScreen.findViewById<ImageButton>(R.id.exo_exit)
            exitButton.setOnClickListener {
                onBackPressed()
            }

            // -----! 원래 화면으로 돌아감 !-----
//            val fullscreenButton = binding.pvFullScreen.findViewById<ImageButton>(com.google.android.exoplayer2.ui.R.id.exo_fullscreen)
//            fullscreenButton.setOnClickListener {
//                onBackPressed()
//            }

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
//        private fun initPlayer(){
//            simpleExoPlayer = SimpleExoPlayer.Builder(this).build()
//            binding.pvFullScreen.player = simpleExoPlayer
//            buildMediaSource().let {
//                simpleExoPlayer?.prepare(it)
//            }
//            simpleExoPlayer?.seekTo(playbackPosition)
//            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
//        }
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
    override fun onBackPressed() {

        val currentPosition = simpleExoPlayer?.currentPosition
        val video_url = videoUrl
        // TODO 시청기록에 대해 보내기 간단한 INSERT
//        val intent = Intent(this,PlayActivity::class.java)
//        intent.putExtra("current_position", currentPosition)
//        intent.putExtra("video_url", video_url)
//        setResult(Activity.RESULT_OK, intent)

        simpleExoPlayer?.seekTo(currentPosition ?: 0)
        super.onBackPressed()
    }


}