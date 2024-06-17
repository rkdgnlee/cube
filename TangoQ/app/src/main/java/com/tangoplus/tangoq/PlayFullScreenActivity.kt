package com.tangoplus.tangoq

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Chronometer
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangoplus.tangoq.data.FavoriteViewModel
import com.tangoplus.tangoq.databinding.ActivityPlayFullScreenBinding


class PlayFullScreenActivity : AppCompatActivity() {
    lateinit var binding: ActivityPlayFullScreenBinding
    lateinit var resultLauncher: ActivityResultLauncher<Intent>
    val viewModel: FavoriteViewModel by viewModels()
    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var player : SimpleExoPlayer? = null
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition = 0L
    private lateinit var chronometer: Chronometer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayFullScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ------! 재생시간 타이머 시작 !------
        chronometer = findViewById(R.id.chronometer)
        chronometer.base = SystemClock.elapsedRealtime()
        chronometer.start()
        // ------! 재생시간 타이머 끝 !------

        // ------! landscape로 방향 설정 & 재생시간 받아오기 !------
        val videoUrl = intent.getStringExtra("video_url")
        val urls = intent.getStringArrayListExtra("urls")
        val url_list = ArrayList<String>()

        if (urls != null && urls.isNotEmpty()) {
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
                            viewModel.exerciseLog.value = Triple((elapsedMills / 1000).toInt(), "${(simpleExoPlayer?.currentWindowIndex)!! + 1}",56)
                            val intent = Intent(this@PlayFullScreenActivity, MainActivity::class.java)
                            intent.putExtra("feedback_finish", viewModel.exerciseLog.value)
                            Log.v("feedback_finish", "VM_exercise_log: ${viewModel.exerciseLog.value}")
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
                simpleExoPlayer?.seekTo((if (replayPosition < 0) 0 else replayPosition)!!)
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
        MaterialAlertDialogBuilder(this@PlayFullScreenActivity, R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setTitle("알림")
            setMessage("운동을 종료하시겠습니까 ?")
            setPositiveButton("예") { dialog, _ ->
                // 소요 시간
                val intent = Intent(this@PlayFullScreenActivity, MainActivity::class.java)
                startActivity(intent)
                finishAffinity()

            }
            setNegativeButton("아니오") { dialog, _ ->
                chronometer.start()
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