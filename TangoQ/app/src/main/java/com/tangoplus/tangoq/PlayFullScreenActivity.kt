package com.tangoplus.tangoq

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Chronometer
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangoplus.tangoq.data.FavoriteViewModel
import com.tangoplus.tangoq.data.HistoryVO
import com.tangoplus.tangoq.data.PlayViewModel
import com.tangoplus.tangoq.databinding.ActivityPlayFullScreenBinding
import com.tangoplus.tangoq.`object`.NetworkHistory.insertViewingHistory
import com.tangoplus.tangoq.`object`.Singleton_t_user
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


class PlayFullScreenActivity : AppCompatActivity() {
    lateinit var binding: ActivityPlayFullScreenBinding
//    lateinit var resultLauncher: ActivityResultLauncher<Intent>
    val viewModel: FavoriteViewModel by viewModels()
    val pViewModel : PlayViewModel by viewModels()
    private var simpleExoPlayer: SimpleExoPlayer? = null
//    private var player : SimpleExoPlayer? = null
//    private var playWhenReady = true
//    private var currentWindow = 0
    private var playbackPosition = 0L
    private lateinit var chronometer: Chronometer
    var exerciseId = ""
    private var isExitDialogVisible = false


    // ------! 카운트 다운  시작 !-------
    private  val mCountDown : CountDownTimer by lazy {
        object : CountDownTimer(2500, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                runOnUiThread {
                    binding.tvFullScreenGuide.visibility = View.VISIBLE
                    binding.tvFullScreenGuide.alpha = 1f
                    binding.tvFullScreenGuide.text = "다음 운동이 곧 시작합니다 !\n준비해 주세요\n\n${(millisUntilFinished.toFloat() / 1000.0f).roundToInt()}"
                    Log.v("count", "${binding.tvFullScreenGuide.text}")
                }
            }

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onFinish() {
                setAnimation(binding.tvFullScreenGuide, 500, 0, false ) { }
                simpleExoPlayer?.play()
            }
        }
    }
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
        exerciseId = intent.getStringExtra("exercise_id").toString()
        val urls = intent.getStringArrayListExtra("urls")
        val totalTime = intent.getIntExtra("total_time", 0)
        Log.v("totalTime", "${totalTime}")
        val url_list = ArrayList<String>()

        if (!urls.isNullOrEmpty()) {
            url_list.addAll(urls)
        } else if (videoUrl != null) {
            url_list.add(videoUrl)
        }
        if (url_list.isNotEmpty()) {
            playbackPosition = intent.getLongExtra("current_position", 0L)
            if (pViewModel.currentPlaybackPosition.value != null && pViewModel.currentWindowIndex.value != null) {
                playbackPosition = pViewModel.currentPlaybackPosition.value!!
                val windowIndex = pViewModel.currentWindowIndex.value!!
                initPlayer(url_list, windowIndex, playbackPosition)
            } else {
                initPlayer(url_list)
            }

//                initPlayer(url_list)
            simpleExoPlayer!!.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    Log.v("PlaybackState", "State: $playbackState")
                    when (playbackState) {
                        Player.STATE_IDLE -> Log.v("PlaybackState", "Player.STATE_IDLE")
                        Player.STATE_BUFFERING -> Log.v("PlaybackState", "Player.STATE_BUFFERING")
                        Player.STATE_READY -> Log.v("PlaybackState", "Player.STATE_READY")
                        Player.STATE_ENDED -> {
                            Log.v("PlaybackState", "Player.STATE_ENDED")
                            val currentWindowIndex = simpleExoPlayer!!.currentWindowIndex
                            // ------! 모든 재생 목록 종료 !------
                            if (currentWindowIndex == url_list.size - 1) {
                                val elapsedMills = SystemClock.elapsedRealtime() - chronometer.base
                                viewModel.exerciseLog.value = Triple((elapsedMills / 1000).toInt(), "${currentWindowIndex + 1}", totalTime) // third 총 시간
                                val intent = Intent(this@PlayFullScreenActivity, MainActivity::class.java)
                                intent.putExtra("feedback_finish", viewModel.exerciseLog.value)
                                Log.v("feedback_finish", "VM_exercise_log: ${viewModel.exerciseLog.value}")
                                startActivity(intent)
                                finish()
                            } else {
                                simpleExoPlayer?.pause()
                                binding.tvFullScreenGuide.visibility = View.VISIBLE
                                mCountDown.start()

                                Log.v("simpleExoPlayer", "Moved to next video")
                            }
                        }
                    }
                }

                override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                    super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                    Log.v("PositionDiscontinuity", "Reason: $reason")

                    // ------! 영상 한 개가 끝났을 때 !------
                    if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {

                        // -----! 현재 재생 시점 저장 !------
                        val currentWindowIndex = simpleExoPlayer!!.currentWindowIndex
                        Log.v("currentWindowIndex", "$currentWindowIndex")
                        val currentPlaybackPosition = simpleExoPlayer!!.currentPosition
                        pViewModel.currentWindowIndex.value = currentWindowIndex
                        pViewModel.currentPlaybackPosition.value = currentPlaybackPosition


                    }
                        // ------! 모든 영상 종료 시 자동 이동 !------
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
                if (forwardPosition < simpleExoPlayer?.duration?.minus(5000)!!) {
                    simpleExoPlayer?.seekTo(forwardPosition)
                } else {
                    simpleExoPlayer!!.pause()
                }
            }
        } // ------! 앞으로 감기 뒤로 감기 끝 !------
    }

    private fun initPlayer(resourceList: ArrayList<String>, windowIndex: Int = 0, playbackPosition: Long = 0L) {
        val loadControl: LoadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .build()
        val trackSelector: TrackSelector = DefaultTrackSelector(this)

        simpleExoPlayer = SimpleExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .build()
        binding.pvFullScreen.player = simpleExoPlayer

        // raw에 있는 것 가져오기
        buildMediaSource(resourceList).let {
            simpleExoPlayer?.prepare(it)
            Log.w("resourcelist in fullscreen", "$resourceList")
        }
        simpleExoPlayer?.seekTo(windowIndex, playbackPosition)
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

        if (isExitDialogVisible) return
        isExitDialogVisible = true
        MaterialAlertDialogBuilder(this@PlayFullScreenActivity, R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setTitle("알림")
            setMessage("운동을 종료하시겠습니까 ?")
            setPositiveButton("예") { dialog, _ ->
                // 소요 시간

                // ------! 시청 기록 넣기 시작 !------
                val historyVO = HistoryVO(
                    exerciseId = exerciseId,
                    timestamp = chronometer.length()
                )
//                lifecycleScope.launch {
//                    insertViewingHistory(this@PlayFullScreenActivity,
//                        getString(R.string.IP_ADDRESS_t_viewing_history),
//                        Singleton_t_user.getInstance(this@PlayFullScreenActivity).jsonObject?.getJSONObject("data")?.optString("user_email").toString(),
//                        historyVO)
//                }
                // ------! 시청 기록 넣기 끝 !------



//                val intent = Intent(this@PlayFullScreenActivity, MainActivity::class.java)
//                startActivity(intent)
                finish()

            }
            setNegativeButton("아니오") { dialog, _ ->
                chronometer.start()
                dialog.dismiss()
            }
            setOnDismissListener {
                isExitDialogVisible = false
            }
            create()
            create()
        }.show()
    }

    private fun setAnimation(tv: View, duration : Long, delay: Long, fade: Boolean, callback: () -> Unit) {

        val animator = ObjectAnimator.ofFloat(tv, "alpha", if (fade) 0f else 1f, if (fade) 1f else 0f)
        animator.duration = duration
        animator.addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                tv.visibility = if (fade) View.VISIBLE else View.INVISIBLE
                callback()
            }
        })
        Handler(Looper.getMainLooper()).postDelayed({
            animator.start()
        }, delay)
    }

    override fun onPause() {
        super.onPause()
//        pViewModel.currentPlaybackPosition.value = simpleExoPlayer!!.currentPosition
//        pViewModel.currentWindowIndex.value = simpleExoPlayer!!.currentWindowIndex
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
        if (simpleExoPlayer != null) {
            simpleExoPlayer?.release()
            simpleExoPlayer = null
            Log.v("exoplayerExit", "SimpleExoPlayer released and null")
        }
//        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun finish() {
        super.finish()
        if (simpleExoPlayer != null) {
            simpleExoPlayer?.release()
            simpleExoPlayer = null
            Log.v("exoplayerExit", "SimpleExoPlayer released and null")
        }
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