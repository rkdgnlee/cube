package com.tangoplus.tangoq

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
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
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangoplus.tangoq.data.ExerciseViewModel

import com.tangoplus.tangoq.data.HistoryUnitVO
import com.tangoplus.tangoq.data.PlayViewModel
import com.tangoplus.tangoq.databinding.ActivityPlayFullScreenBinding
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt


class PlayFullScreenActivity : AppCompatActivity() {
    lateinit var binding: ActivityPlayFullScreenBinding
    val viewModel: ExerciseViewModel by viewModels()
    val pViewModel : PlayViewModel by viewModels()
    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var playbackPosition = 0L
    private lateinit var chronometer: Chronometer
    var currentExerciseId = ""
    private var isExitDialogVisible = false
    private lateinit var mediaSourceList: List<MediaSource>
    private var currentMediaSourceIndex = 0
    private var sns: MutableList<String>? = null
    var totalTime = 0
    // ------! 카운트 다운  시작 !-------
    private  val mCountDown : CountDownTimer by lazy {
        object : CountDownTimer(2500, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                runOnUiThread {
                    binding.tvFullScreenGuide.visibility = View.VISIBLE
                    binding.tvFullScreenGuide.alpha = 1f
                    binding.tvFullScreenGuide.text = "다음 운동이 곧 시작합니다 !\n준비해 주세요\n\n${(millisUntilFinished.toFloat() / 1000.0f).roundToInt()}"
                    Log.v("chronometerTime", "${SystemClock.elapsedRealtime() - chronometer.base}")
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

        // ------# 재생시간 타이머 시작하기 #------
        chronometer = findViewById(R.id.chronometer)
        chronometer.base = SystemClock.elapsedRealtime()
        chronometer.start()


        // ------# landscape로 방향 설정 & 재생시간 받아오기 #------
        val videoUrl = intent.getStringExtra("video_url")
        val urls = intent.getStringArrayListExtra("video_urls")
        val exerciseIds = intent.getStringArrayListExtra("exercise_ids")
        totalTime = intent.getIntExtra("total_time", 0)
        Log.v("url들", "videoUrl: $videoUrl, urls: $urls")

        // ------# 이걸로 재생 1개든 여러 개든 이곳에 담음 #------
        val url_list = ArrayList<String>()

        if (!urls.isNullOrEmpty()) {
            url_list.addAll(urls)
        } else if (videoUrl != null) {
            url_list.add(videoUrl)
        }
        sns = exerciseIds?.toMutableList()
        currentExerciseId = sns?.get(0).toString()

        if (url_list.isNotEmpty()) {
            playbackPosition = intent.getLongExtra("current_position", 0L)
            if (pViewModel.currentPlaybackPosition.value != null && pViewModel.currentWindowIndex.value != null) {
                playbackPosition = pViewModel.currentPlaybackPosition.value!!
                val windowIndex = pViewModel.currentWindowIndex.value!!
                initPlayer(url_list, windowIndex, playbackPosition)
            } else {
                initPlayer(url_list)
            }
        }

        // -----! 받아온 즐겨찾기 재생 목록 끝 !-----
        val exitButton = binding.pvFullScreen.findViewById<ImageButton>(R.id.exo_exit)
        exitButton.setOnClickListener {

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

        mediaSourceList = buildMediaSource(resourceList)
        currentMediaSourceIndex = windowIndex

        simpleExoPlayer?.setMediaSources(mediaSourceList)
        simpleExoPlayer?.prepare()
        simpleExoPlayer?.seekTo(windowIndex, playbackPosition)


        simpleExoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                Log.v("PlaybackState", "State: $playbackState")
                when (playbackState) {
                    Player.STATE_IDLE -> Log.v("PlaybackState", "Player.STATE_IDLE")
                    Player.STATE_BUFFERING -> Log.v("PlaybackState", "Player.STATE_BUFFERING")
                    Player.STATE_READY -> Log.v("PlaybackState", "Player.STATE_READY")
                    Player.STATE_ENDED -> {
                        Log.v("PlaybackState", "Player.STATE_ENDED")
                        // 모든 영상이 끝났을 때의 처리


                        val elapsedMills = SystemClock.elapsedRealtime() - chronometer.base
                        viewModel.exerciseLog.value = Triple((elapsedMills / 1000).toInt(), "${mediaSourceList.size}", totalTime)
                        val intent = Intent(this@PlayFullScreenActivity, MainActivity::class.java)
                        intent.putExtra("feedback_finish", viewModel.exerciseLog.value)
                        Log.v("feedback_finish", "VM_exercise_log: ${viewModel.exerciseLog.value}")
                        startActivity(intent)
                        finish()
                    }
                }
            }

            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                Log.v("PositionDiscontinuity", "Reason: $reason")
                if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {

                    val currentWindowIndex = simpleExoPlayer!!.currentWindowIndex
                    Log.v("currentWindowIndex", "currentWindowIndex2: $currentWindowIndex") // 이미 재생목록이 변하고 나서 말하는 거임.
                    val currentPlaybackPosition = simpleExoPlayer!!.currentPosition
                    pViewModel.currentWindowIndex.value = currentWindowIndex
                    pViewModel.currentPlaybackPosition.value = currentPlaybackPosition

                    // ------# 다음 동영상으로 넘어갈 때 구간 #------
                    if (currentWindowIndex < mediaSourceList.size - 1) {

                        // ------# 카운트다운 후 검색 기록 담기 #------
                        val historyUnitVO = HistoryUnitVO(
                            sns!![currentWindowIndex - 1],
                            0,
                            getCurrentDateTimeAsString()
                        )
                        // ------# 타이머 초기화, 현재 재생중인 sn 수정, 검색기록 VM추가, 카운트다운 시작
                        chronometer.base = SystemClock.elapsedRealtime()
                        viewModel.finishHistorys.add(historyUnitVO)
                        currentExerciseId = sns!![currentWindowIndex]
                        startNextVideoCountdown()
                    }
                }
            }
        })
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
    private fun buildMediaSource(resourceList: ArrayList<String>) : List<MediaSource> {
        val dataSourceFactory = DefaultDataSourceFactory(this, "TangoQ")
        return resourceList.map { url ->
            ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(url))
        }
    }

    private fun startNextVideoCountdown() {
        simpleExoPlayer?.pause()
        binding.tvFullScreenGuide.visibility = View.VISIBLE
        binding.tvFullScreenGuide.alpha = 1f
        mCountDown.start()
    }

    private fun showExitDialog() {
        if (isExitDialogVisible) return
        isExitDialogVisible = true
        MaterialAlertDialogBuilder(this@PlayFullScreenActivity, R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setTitle("알림")
            setMessage("운동을 종료하시겠습니까 ?")
            setPositiveButton("예") { dialog, _ ->

                // 타이머가 경과한 시간
                val elapsedMillis = SystemClock.elapsedRealtime() - chronometer.base

                // 경과 시간을 초로 변환
                val elapsedSeconds = (elapsedMillis / 1000).toInt()
                Log.v("elapsedSeconds", "$elapsedSeconds")
                chronometer.base = SystemClock.elapsedRealtime()

                // ------# 시청 기록 일시 중단 항목 #------
                val historyUnitVO = HistoryUnitVO(
                    currentExerciseId,
                    elapsedSeconds,
                    getCurrentDateTimeAsString()
                )
                Log.v("sendingHistory>VM", "${viewModel.finishHistorys}")
                Log.v("sendingHistory", "$historyUnitVO")
                // TODO historyVO를 이제 보내주기.




                val intent = Intent(this@PlayFullScreenActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
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

    private fun historyToJson(exerciseHistorys: MutableList<HistoryUnitVO>) : JSONObject {
        val jo = JSONObject()
        for (i in exerciseHistorys.indices) {
            // TODO 보내는 형식 정해야함 jsonArray > jsonObject 여러개 해서 보낼건지
        }


        return jo

    }

    // 특정 동작이 완료되었을 때 호출되는 함수
    private fun getCurrentDateTimeAsString(): String {
        // 현재 날짜와 시간을 가져옴
        val currentDateTime = LocalDateTime.now()

        // 원하는 형식으로 날짜와 시간을 포맷
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return currentDateTime.format(formatter)
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
            Log.v("exoplayerExit", "SimpleExoPlayer released and null, finish")
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("playbackPosition", simpleExoPlayer?.currentPosition ?: 0L)
        outState.putInt("currentWindow", simpleExoPlayer?.currentWindowIndex ?: 0)
        outState.putBoolean("playWhenReady", simpleExoPlayer?.playWhenReady ?: true)
    }
}