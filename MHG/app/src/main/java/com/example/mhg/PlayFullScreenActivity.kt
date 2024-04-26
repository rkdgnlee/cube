package com.example.mhg

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PlayFullScreenActivity : AppCompatActivity() {
        lateinit var binding: ActivityPlayFullScreenBinding
        lateinit var resultLauncher: ActivityResultLauncher<Intent>
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

            val videoUrl = intent.getStringExtra("video_url")
            if (videoUrl != null) {
                val url_list = ArrayList<String>()
                url_list.add(videoUrl)
                playbackPosition = intent.getLongExtra("current_position", 0L)
                initPlayer(url_list)
            }

           // -----! 받아온 즐겨찾기 재생 목록 시작 !-----
            val resourceList = intent.getStringArrayListExtra("resourceList")
            if (resourceList != null) {
                initPlayer(resourceList)

                // -----! 영상 간 1초의 간격 !-----
                simpleExoPlayer!!.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        super.onPlaybackStateChanged(playbackState)
                        if (playbackState == Player.STATE_ENDED) {
                            // -----! 모든 영상 종료 시 자동 이동 !-----
                            if (simpleExoPlayer!!.currentWindowIndex == resourceList.size -1) {
                                val intent = Intent(this@PlayFullScreenActivity, FeedbackActivity::class.java)
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
                showExitDialog()
            }
        }
        private fun initPlayer(resourceList: ArrayList<String>) {
            simpleExoPlayer = SimpleExoPlayer.Builder(this).build()
            binding.pvFullScreen.player = simpleExoPlayer

            // raw에 있는 것 가져오기
            buildMediaSource(resourceList).let {
                simpleExoPlayer?.prepare(it)
                Log.w("resourcelist in fullscreen", "$resourceList")
            }
            simpleExoPlayer?.seekTo(playbackPosition)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
//private fun initPlayer() {
//    simpleExoPlayer = SimpleExoPlayer.Builder(this).build()
//    binding.pvFullScreen.player = simpleExoPlayer
//
//    // raw mp4 URI 가져오기
//    val rawUri = "android.resource://$packageName/" + R.raw.kakaotalk_20240408_102448280
//
//    // URI MediaItem변환
//    val mediaItem = MediaItem.fromUri(rawUri)
//    val mediaSource = ProgressiveMediaSource.Factory(DefaultDataSourceFactory(this, "ExoPlayer")).createMediaSource(mediaItem)
//    simpleExoPlayer?.setMediaSource(mediaSource)
//    simpleExoPlayer?.prepare()
//
//    simpleExoPlayer?.seekTo(playbackPosition)
//    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
//}

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
        MaterialAlertDialogBuilder(this@PlayFullScreenActivity, R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setTitle("알림")
            setMessage("운동을 종료하시겠습니까 ?")
            setPositiveButton("예") { dialog, _ ->
//                // TODO feedback activity에서 운동 기록 데이터 백그라운드 작업 전송 필요
//                val intent= Intent(this@PlayFullScreenActivity, FeedbackActivity::class.java)
//                startActivity(intent)
                finish()
            }
            setNegativeButton("아니오") { dialog, _ ->
            }
            create()
        }.show()
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
        super.onBackPressed()

    }


}