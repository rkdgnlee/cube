package com.tangoplus.tangoq

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
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
import android.widget.LinearLayout
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
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.eventbus.Subscribe
import com.tangoplus.tangoq.viewmodel.PlayViewModel
import com.tangoplus.tangoq.databinding.ActivityPlayFullScreenBinding
import com.tangoplus.tangoq.api.NetworkProgress.patchProgress1Item
import com.tangoplus.tangoq.function.EventBusManager
import com.tangoplus.tangoq.service.ForcedTerminationService
import com.tangoplus.tangoq.service.TerminationEvent
import org.json.JSONObject
import kotlin.math.roundToInt


class PlayFullScreenActivity : AppCompatActivity() {
    lateinit var binding: ActivityPlayFullScreenBinding
    val pvm : PlayViewModel by viewModels()
    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var playbackPosition = 0L
    private lateinit var chronometer: Chronometer
    var currentExerciseId = ""
    private var isExitDialogVisible = false
    private lateinit var mediaSourceList: List<MediaSource>
    private var currentMediaSourceIndex = 0
    private var currentVideoDuration = 0L
    private var totalDuration = 0
    val baseUrls = ArrayList<String>()

    private var sns: MutableList<String>? = null
    private var uvpSns : MutableList<String>? = null

    private val playbackPositions = mutableMapOf<Int, Long>()
    private var exoPlay: ImageButton? = null
    private var exoPause: ImageButton? = null
    private var llSpeed: LinearLayout? = null
    private var btnSpeed: ImageButton? = null
    private var exo05: ImageButton? = null
    private var exo075: ImageButton? = null
    private var exo10: ImageButton? = null

    // ------! 카운트 다운  시작 !-------
    private  val mCountDown : CountDownTimer by lazy {
        object : CountDownTimer(3000, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                runOnUiThread {
                    binding.tvFullScreenGuide.visibility = View.VISIBLE
                    binding.tvFullScreenGuide.alpha = 1f
                    binding.tvFullScreenGuide.text = "다음 운동이 곧 시작합니다 !\n준비해 주세요\n\n${(millisUntilFinished.toFloat() / 1000.0f).roundToInt()}"
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
        startService(Intent(this, ForcedTerminationService::class.java))
        EventBusManager.eventBus.register(this)

        // ------# landscape로 방향 설정 & 재생시간 받아오기 #------
        val videoUrl = intent.getStringExtra("video_url")
        val videoUrls = intent.getStringArrayListExtra("video_urls")

        val exerciseId = intent.getStringArrayListExtra("exercise_id")
        val exerciseIds = intent.getStringArrayListExtra("exercise_ids")

        totalDuration = intent.getIntExtra("total_duration", 0)
        uvpSns = intent.getStringArrayListExtra("uvp_sns")
        pvm.isUnit = intent.getBooleanExtra("isUnit", false)
        Log.v("url들", "videoUrl: $exerciseIds, urls: $videoUrls, uvpSns: $uvpSns")

        // ------# 이걸로 재생 1개든 여러 개든 이곳에 담음 #------


        exoPlay = findViewById(R.id.btnPlay)
        exoPause = findViewById(R.id.btnPause)
        llSpeed = findViewById(R.id.llSpeed)
        btnSpeed = findViewById(R.id.btnSpeed)

        exo05 = findViewById(R.id.btn05)
        exo075 = findViewById(R.id.btn075)
        exo10 = findViewById(R.id.btn10)

        exoPlay?.visibility = View.GONE

        exoPause?.setOnClickListener {
            if (simpleExoPlayer?.isPlaying == true) {
                simpleExoPlayer?.pause()
                exoPause?.visibility = View.GONE
                exoPlay?.visibility = View.VISIBLE
            }
        }
        exoPlay?.setOnClickListener {
            if (simpleExoPlayer?.isPlaying == false) {
                simpleExoPlayer?.play()
                exoPause?.visibility = View.VISIBLE
                exoPlay?.visibility = View.GONE
            }
        }

        if (!videoUrls.isNullOrEmpty()) {
            baseUrls.addAll(videoUrls)
        } else if (videoUrl != null) {
            baseUrls.add(videoUrl)
        }

        // ------# exerciseId들도 하나로 일원화 #------
        sns = exerciseIds?.toMutableList()
        if (sns?.isEmpty() == true && exerciseId != null) {
            sns!!.add(exerciseId.toString())
        }

        currentExerciseId = sns?.get(0).toString()

        if (baseUrls.isNotEmpty()) {
            playbackPosition = intent.getLongExtra("current_position", 0L)
            Log.v("재생시점", "$playbackPosition")
            if (playbackPosition != 0L) {
                initPlayer(baseUrls, currentMediaSourceIndex, playbackPosition)
            } else {
                initPlayer(baseUrls, 0, 0)
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
                if (forwardPosition < (simpleExoPlayer?.duration?.minus(5000) ?: 0L)) {
                    simpleExoPlayer?.seekTo(forwardPosition)
                } else {
                    simpleExoPlayer?.pause()
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

        // ------# 중간 재생 시점부터 재생 #------

        simpleExoPlayer?.setMediaSources(mediaSourceList)
        simpleExoPlayer?.prepare()
        val positionMs = playbackPosition * 1000
        val savedPosition = playbackPositions[windowIndex] ?: positionMs
        simpleExoPlayer?.seekTo(windowIndex, savedPosition)
        simpleExoPlayer?.playWhenReady = true  // 준비되면 자동 재생



        simpleExoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                Log.v("PlaybackState", "State: $playbackState")
                when (playbackState) {
                    Player.STATE_IDLE -> Log.v("PlaybackState", "Player.STATE_IDLE")
                    Player.STATE_BUFFERING -> Log.v("PlaybackState", "Player.STATE_BUFFERING")
                    Player.STATE_READY -> {
                        Log.v("PlaybackState", "Player.STATE_READY, currentMediaSourceIndex: $currentMediaSourceIndex")
                        currentVideoDuration = simpleExoPlayer?.duration ?: 0
                        Log.e("currentVideoDuration임", "$currentVideoDuration")

                    }
                    Player.STATE_ENDED -> {
                        Log.v("PlaybackState", "Player.STATE_ENDED")

                        if (!pvm.isUnit) {
                            sendData(true) {
                                val elapsedMillis = SystemClock.elapsedRealtime() - chronometer.base
                                val elapsedSeconds = elapsedMillis / 1000
                                chronometer.stop()
                                Log.v("elapsedSeconds", "$elapsedSeconds")
                                pvm.exerciseLog = Triple(totalDuration, elapsedSeconds.toInt(), baseUrls.size )
                                // 이 곳에 총 크로노미터 + 도합 운동시간 + 운동 갯수 3개 보내야함.
                                val intent = Intent(this@PlayFullScreenActivity, MainActivity::class.java)
                                intent.putExtra("feedback_finish", pvm.exerciseLog)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                Log.v("feedback_finish", "VM_exercise_log: ${pvm.exerciseLog}")
                                startActivity(intent)
                                finish()
                            }

                        } else {
                            val intent = Intent(this@PlayFullScreenActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                val currentWindowIndex = simpleExoPlayer?.currentWindowIndex

                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    if (baseUrls.size > 1) {
                        sendData(true) {}
                        val currentPlaybackPosition = simpleExoPlayer?.currentPosition
                        pvm.currentWindowIndex.value = currentWindowIndex
                        pvm.currentPlaybackPosition.value = currentPlaybackPosition

                        if (currentWindowIndex != null) {
                            if (currentWindowIndex < mediaSourceList.size) {
                                pvm.totalProgressDuration += simpleExoPlayer?.duration?.toInt() ?: 0
                                currentMediaSourceIndex++
                                Log.v("윈도우인덱스", "$currentMediaSourceIndex")

                                currentExerciseId = sns?.get(currentWindowIndex) ?: ""
                                startNextVideoCountdown()
                                currentVideoDuration = simpleExoPlayer?.duration ?: 0
                                Log.e("currentVideoDuration임", "$currentVideoDuration")
                            }
                        }
                    }

                }
            }
        })
    }
    // ------# isFinish는 영상 길이 전부 시청했는지 여부 #------
    private fun sendData(isFinish: Boolean, callback: () -> Unit) {
        val player = simpleExoPlayer ?: return
        val currentPositionMs = player.currentPosition - 1
        // 현재 윈도우의 재생 위치 저장

        val currentPositionSeconds = (currentPositionMs / 1000.0).toInt()
        val totalDurationMs = (currentVideoDuration / 1000.0 ).toInt()
        val jo = JSONObject()
        if (isFinish && currentPositionSeconds >= 0) {
            jo.put("progress", totalDurationMs)
        } else {
            jo.put("progress", currentPositionSeconds)
        }
        Log.v("미디어소스인덱스", "${uvpSns?.get(currentMediaSourceIndex)?.toInt()}, jo: $jo, totalDuration: $totalDurationMs")
        val currentUvpSns = uvpSns?.get(currentMediaSourceIndex)?.toInt()
        if (uvpSns?.isNotEmpty() == true && currentUvpSns != null) {
            patchProgress1Item(getString(R.string.API_progress), currentUvpSns, jo, this@PlayFullScreenActivity) { }
            Log.v("progress완료업데이트", "${currentUvpSns}, currentPosition: $currentPositionMs")
            callback()
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

    // -----! 동영상 재생목록에 넣기 !-----
    private fun buildMediaSource(resourceList: ArrayList<String>) : List<MediaSource> {
        val mediaSources = mutableListOf<MediaSource>()
        val dataSourceFactory = DefaultDataSource.Factory(this)
        resourceList.forEach { url ->
            val mediaItem = MediaItem.fromUri(Uri.parse(url))
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
            mediaSources.add(mediaSource)
        }
        return mediaSources
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
                if (!pvm.isUnit) {
                    sendData(false) {
                        val intent = Intent(this@PlayFullScreenActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish()
                    }
                } else {
                    val intent = Intent(this@PlayFullScreenActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }

            }
            setNegativeButton("아니오") { dialog, _ ->
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

    @Subscribe
    fun onTerminationEvent(event: TerminationEvent) {
        Log.v("강제종료", "sendData: $currentVideoDuration")
        sendData(false) {
            EventBusManager.eventBus.unregister(this)
        }
    }
}