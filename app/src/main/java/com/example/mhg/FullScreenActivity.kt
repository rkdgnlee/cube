package com.example.mhg

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mhg.databinding.ActivityFullScreenBinding
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory

class FullScreenActivity : AppCompatActivity() {
        lateinit var binding: ActivityFullScreenBinding
        private var videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"

        private var simpleExoPlayer: SimpleExoPlayer? = null
        private var player : SimpleExoPlayer? = null
        private var playWhenReady = true
        private var currentWindow = 0
        private var playbackPosition = 0L
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            enableEdgeToEdge()
            binding = ActivityFullScreenBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // -----! landscape로 방향 설정 & 재생시간 받아오기 !-----

            playbackPosition = intent.getLongExtra("current_position", 0L)
            initPlayer()


            // -----! 원래 화면으로 돌아감 !-----
            val fullscreenButton = binding.pvFullScreen.findViewById<ImageButton>(com.google.android.exoplayer2.ui.R.id.exo_fullscreen)
            fullscreenButton.setOnClickListener {
                val intent = Intent(this, PlayActivity::class.java)
                intent.putExtra("current_position", simpleExoPlayer?.currentPosition)
                startActivity(intent)
                finish()
            }

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
        private fun initPlayer(){
            simpleExoPlayer = SimpleExoPlayer.Builder(this).build()
            binding.pvFullScreen.player = simpleExoPlayer
            buildMediaSource()?.let {
                simpleExoPlayer?.prepare(it)
            }
            simpleExoPlayer?.seekTo(playbackPosition)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
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

    override fun onBackPressed() {
        super.onBackPressed()
    }
}