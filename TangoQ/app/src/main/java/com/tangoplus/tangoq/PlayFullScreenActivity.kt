package com.tangoplus.tangoq

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.activity.OnBackPressedCallback
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
import com.tangoplus.tangoq.api.NetworkExercise.patchExerciseHistory
import com.tangoplus.tangoq.viewmodel.PlayViewModel
import com.tangoplus.tangoq.databinding.ActivityPlayFullScreenBinding
import com.tangoplus.tangoq.api.NetworkProgress.patchProgress1Item
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.roundToInt


class PlayFullScreenActivity : AppCompatActivity() {
    lateinit var binding: ActivityPlayFullScreenBinding
    val pvm : PlayViewModel by viewModels()
    private var playbackPosition = 0L
    private lateinit var chronometer: Chronometer
    var currentExerciseId = ""
    private var isExitDialogVisible = false
    private lateinit var mediaSourceList: List<MediaSource>
//    private var pvm.simpleExoPlayer : pvm.simpleExoPlayer? = null
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
                pvm.simpleExoPlayer?.play()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayFullScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, R.anim.slide_in_left, R.anim.none)
        } else {
            overridePendingTransition(R.anim.none, R.anim.slide_in_left)
        }

        // ------# 재생시간 타이머 시작하기 #------
        chronometer = findViewById(R.id.chronometer)
        chronometer.base = SystemClock.elapsedRealtime()
        chronometer.start()

        // ------# landscape로 방향 설정 & 재생시간 받아오기 #------
        val videoUrl = intent.getStringExtra("video_url")
        val videoUrls = intent.getStringArrayListExtra("video_urls")

        val exerciseId = intent.getStringExtra("exercise_id")
        val exerciseIds = intent.getStringArrayListExtra("exercise_ids")

        val currentWeek = intent.getIntExtra("currentWeek", 0)
        val currentSeq = intent.getIntExtra("currentSeq", 0)

        val isProgram = intent.getBooleanExtra("isProgram", false)
        val uvpSn = intent.getIntExtra("uvpSn", 0)
        pvm.uvpSns = intent.getStringArrayListExtra("uvp_sns")
        pvm.isEVP = intent.getBooleanExtra("isEVP", false)
        pvm.weekNumber = currentWeek
        pvm.cycle = currentSeq
        pvm.isProgram = isProgram
        pvm.uvpSn = uvpSn

        Log.v("url들", "isEVP: ${pvm.isEVP}, urls: $videoUrls, uvpSns: ${pvm.uvpSns}, isProgram: $isProgram, uvpSn: $uvpSn, cycle: ${pvm.cycle}, weekNumber: ${pvm.weekNumber}")

        // ------# 이걸로 재생 1개든 여러 개든 이곳에 담음 #------
        if (!videoUrls.isNullOrEmpty()) {
            pvm.baseUrls.addAll(videoUrls)
        } else if (videoUrl != null) {
            pvm.baseUrls.add(videoUrl)
        }

        // ------# exerciseId들도 하나로 일원화 #------
        pvm.sns = exerciseIds?.toMutableList()
        if (pvm.sns?.isEmpty() == true && exerciseId != null) {
            pvm.sns!!.add(exerciseId.toString())
        }
        if (exerciseId != null) {
            currentExerciseId = exerciseId
        }
        if (pvm.baseUrls.isNotEmpty()) {
            playbackPosition = intent.getLongExtra("current_position", 0L)
            Log.v("재생시점원시", "$playbackPosition")
            pvm.isResume = false
//            setPlayer()

        }

        exoPlay = findViewById(R.id.btnPlay)
        exoPause = findViewById(R.id.btnPause)
        llSpeed = findViewById(R.id.llSpeed)
        btnSpeed = findViewById(R.id.btnSpeed)

        exo05 = findViewById(R.id.btn05)
        exo075 = findViewById(R.id.btn075)
        exo10 = findViewById(R.id.btn10)

        exoPlay?.visibility = View.GONE

        exoPause?.setOnClickListener {
            if (pvm.simpleExoPlayer?.isPlaying == true) {
                pvm.simpleExoPlayer?.pause()
                exoPause?.visibility = View.GONE
                exoPlay?.visibility = View.VISIBLE
            }
        }
        exoPlay?.setOnClickListener {
            if (pvm.simpleExoPlayer?.isPlaying == false) {
                pvm.simpleExoPlayer?.play()
                exoPause?.visibility = View.VISIBLE
                exoPlay?.visibility = View.GONE
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
            val replayPosition = pvm.simpleExoPlayer?.currentPosition?.minus(5000)
            if (replayPosition != null) {
                pvm.simpleExoPlayer?.seekTo((if (replayPosition < 0) 0 else replayPosition))
            }
        }

        forward5.setOnClickListener {
            val forwardPosition = pvm.simpleExoPlayer?.currentPosition?.plus(5000)
            if (forwardPosition != null) {
                if (forwardPosition < (pvm.simpleExoPlayer?.duration?.minus(5000) ?: 0L)) {
                    pvm.simpleExoPlayer?.seekTo(forwardPosition)
                } else {
                    pvm.simpleExoPlayer?.pause()
                }
            }
        } // ------! 앞으로 감기 뒤로 감기 끝 !------


        // 뒤로가기시 바로 보내기
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.v("뒤로가기누름", "뒤로가기 버튼 눌림.")
                if (!pvm.isEVP) {
                    sendUVP(false) {
                        val intent = Intent(this@PlayFullScreenActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish()
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        sendEVP(false) {
                            val intent = Intent(this@PlayFullScreenActivity, MainActivity::class.java)
                            intent.putExtra("evp_finish", currentExerciseId)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }
        })
    }

    private fun setPlayer() {
        if (pvm.getPlaybackPosition() == 0L) {
            pvm.setPlaybackPosition(playbackPosition)
        } else {
            pvm.setPlaybackPosition(pvm.getPlaybackPosition() / 1000)
        }
        Log.v("setPlayer", "position: ${pvm.getPlaybackPosition()}, windowIndex: ${pvm.getWindowIndex()}")
        initPlayer(pvm.baseUrls, pvm.getWindowIndex(), pvm.getPlaybackPosition())
        pvm.simpleExoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                Log.v("PlaybackState", "State: $playbackState")
                when (playbackState) {
                    Player.STATE_IDLE -> Log.v("PlaybackState", "Player.STATE_IDLE")
                    Player.STATE_BUFFERING -> Log.v("PlaybackState", "Player.STATE_BUFFERING")
                    Player.STATE_READY -> {
                        Log.v("PlaybackState", "Player.STATE_READY, currentMediaSourceIndex: ${pvm.currentMediaSourceIndex}")
                        pvm.currentVideoDuration = pvm.simpleExoPlayer?.duration ?: 0
                        Log.e("currentVideoDuration임", "${pvm.currentVideoDuration}")

                    }
                    Player.STATE_ENDED -> {
                        Log.v("PlaybackState", "Player.STATE_ENDED")

                        if (!pvm.isEVP) {
                            sendUVP(true) {
                                val elapsedMillis = SystemClock.elapsedRealtime() - chronometer.base
                                val elapsedSeconds = elapsedMillis / 1000
                                chronometer.stop()
                                Log.v("elapsedSeconds", "$elapsedSeconds")
//                                pvm.exerciseLog = Triple(totalDuration, elapsedSeconds.toInt(), baseUrls.size )
                                // 이 곳에 총 크로노미터 + 도합 운동시간 + 운동 갯수 3개 보내야함.
                                val intent = Intent(this@PlayFullScreenActivity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                startActivity(intent)
                                finish()
                            }

                        } else {
                            CoroutineScope(Dispatchers.Main).launch {
                                sendEVP(true) {
                                    val intent = Intent(this@PlayFullScreenActivity, MainActivity::class.java)
                                    intent.putExtra("evp_finish", currentExerciseId)
                                    startActivity(intent)
                                    finish()
                                }
                            }
                        }
                    }

                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                val currentWindowIndex = pvm.simpleExoPlayer?.currentWindowIndex

                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    if (pvm.baseUrls.size > 1) {
                        sendUVP(true) {}
//                        val currentPlaybackPosition = pvm.simpleExoPlayer?.currentPosition
//                        pvm.currentWindowIndex.value = currentWindowIndex
//                        pvm.currentPlaybackPosition.value = currentPlaybackPosition
                        if (currentWindowIndex != null) {
                            Log.v("현재윈도index", "$currentWindowIndex")
                            pvm.setWindowIndex(currentWindowIndex)
                        }
                        pvm.simpleExoPlayer?.let { player ->
                            pvm.savePlayerState(player, pvm.baseUrls[pvm.getWindowIndex()])
                        }
                        if (currentWindowIndex != null) {
                            if (currentWindowIndex < mediaSourceList.size) {
                                pvm.totalProgressDuration += pvm.simpleExoPlayer?.duration?.toInt() ?: 0
                                pvm.currentMediaSourceIndex++
                                Log.v("윈도우인덱스", "${pvm.currentMediaSourceIndex}")

                                currentExerciseId = pvm.sns?.get(currentWindowIndex) ?: ""
                                startNextVideoCountdown()
                                pvm.currentVideoDuration = pvm.simpleExoPlayer?.duration ?: 0
                                Log.e("currentVideoDuration임", "${pvm.currentVideoDuration}")
                            }
                        }
                    }
                }
            }
        })
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

        pvm.simpleExoPlayer = SimpleExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .build()
        binding.pvFullScreen.player = pvm.simpleExoPlayer
        mediaSourceList = buildMediaSource(resourceList)
        pvm.currentMediaSourceIndex = windowIndex

        // ------# 중간 재생 시점부터 재생 #------
        pvm.setPlaybackPosition(playbackPosition * 1000)
        pvm.simpleExoPlayer?.setMediaSources(mediaSourceList)
        pvm.simpleExoPlayer?.prepare()
        pvm.simpleExoPlayer?.seekTo(pvm.getWindowIndex(), pvm.getPlaybackPosition())
        Log.v("재생시점", "${pvm.getPlaybackPosition()} / ${pvm.simpleExoPlayer?.duration}")
//        val positionMs = playbackPosition * 1000
//        val savedPosition = playbackPositions[windowIndex] ?: positionMs
//        pvm.simpleExoPlayer?.seekTo(windowIndex, savedPosition)
        pvm.simpleExoPlayer?.playWhenReady = true  // 준비되면 자동 재생
    }

    // ------# isFinish는 영상 길이 전부 시청했는지 여부 #------
    private fun sendUVP(isFinish: Boolean, callback: () -> Unit) {
        val player = pvm.simpleExoPlayer ?: return
        val currentPositionMs = player.currentPosition - 1
        // 현재 윈도우의 재생 위치 저장

        val currentPositionSeconds = (currentPositionMs / 1000.0).toInt()
        val totalDurationMs = (pvm.currentVideoDuration / 1000.0 ).toInt()
        val jo = JSONObject()
        if (isFinish && currentPositionSeconds >= 0) {
            jo.put("progress", totalDurationMs)

        } else {
            jo.put("progress", currentPositionSeconds)
        }

        jo.put("week_number", pvm.weekNumber)
        jo.put("cycle", pvm.cycle)

        Log.v("미디어소스인덱스", "${pvm.uvpSns?.get(pvm.currentMediaSourceIndex)?.toInt()}, jo: $jo, totalDuration: $totalDurationMs")
        val currentUvpSns = pvm.uvpSns?.get(pvm.currentMediaSourceIndex)?.toInt()
        if (pvm.uvpSns?.isNotEmpty() == true && currentUvpSns != null) {
            patchProgress1Item(getString(R.string.API_progress), currentUvpSns, jo, this@PlayFullScreenActivity) { }
            Log.v("progresses완료업데이트", "${currentUvpSns}, currentPosition: $currentPositionMs")
            callback()
        } else if (pvm.uvpSn != 0) {
            patchProgress1Item(getString(R.string.API_progress), pvm.uvpSn, jo, this@PlayFullScreenActivity) { }
            Log.v("progress완료업데이트", "week: ${pvm.weekNumber} cycle: ${pvm.cycle}, currentPosition: $currentPositionMs")
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
        pvm.simpleExoPlayer?.pause()
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
                if (!pvm.isEVP) {
                    sendUVP(false) {
                        val intent = Intent(this@PlayFullScreenActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish()
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        sendEVP(false) {
                            val intent = Intent(this@PlayFullScreenActivity, MainActivity::class.java)
                            intent.putExtra("evp_finish", currentExerciseId)
                            startActivity(intent)
                            finish()
                        }
                    }
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

    // ------# isFinish는 영상 길이 전부 시청했는지 여부 #------
    private suspend fun sendEVP(isFinish: Boolean, callback: () -> Unit) {
        val player = pvm.simpleExoPlayer ?: return
        val currentPositionMs = withContext(Dispatchers.Main) {
            player.currentPosition - 1
        }
        // 현재 윈도우의 재생 위치 저장

        val currentPositionSeconds = (currentPositionMs / 1000.0).toInt()
        val totalDurationMs = (pvm.currentVideoDuration / 1000.0 ).toInt()
        val jo = JSONObject()
        if (isFinish && currentPositionSeconds >= 0) {
            jo.put("progress", totalDurationMs)
        } else {
            jo.put("progress", currentPositionSeconds)
        }

        patchExerciseHistory(this@PlayFullScreenActivity, getString(R.string.API_exercise), currentExerciseId, jo.toString())
        callback()
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

//    override fun onResume() {
//        super.onResume()
//        if (pvm.isResume) {
//            setPlayer()
//            pvm.isResume = false
//        }
//    }
    override fun onStart() {
        super.onStart()
        setPlayer()
    }

    override fun onStop() {
        super.onStop()
        pvm.simpleExoPlayer?.let { player ->
            pvm.savePlayerState(player, pvm.baseUrls[pvm.getWindowIndex()])
            player.stop()
            player.release()
            player.playWhenReady = false
            playbackPosition = player.currentPosition
        }
        pvm.isResume = true
        CoroutineScope(Dispatchers.Main).launch {
            if (!pvm.isEVP) {
                sendUVP(false) {
                    Log.v("sendUVP", "success to send UVP")
                }
            } else {
                sendEVP(false) {
                    Log.v("sendEVP", "success to send EVP")
                }
            }
        }
        Log.v("exoOnStop", "pvm.simpleExoPlayer released and null")

    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, R.anim.none, R.anim.slide_in_right)
        } else {
            overridePendingTransition(R.anim.none, R.anim.slide_in_right)
        }
        pvm.simpleExoPlayer?.let { player ->
            pvm.savePlayerState(player, pvm.baseUrls[pvm.getWindowIndex()])
            player.stop()
            player.playWhenReady = false
            player.release()
            player.playWhenReady = false
            playbackPosition = player.currentPosition

        }
        pvm.isResume = true
        Log.v("exoOnPause", "pvm.simpleExoPlayer released and null")

    }

    override fun onDestroy() {
        super.onDestroy()
        if (pvm.simpleExoPlayer != null) {
            pvm.simpleExoPlayer?.let { player ->
                pvm.savePlayerState(player, pvm.baseUrls[pvm.getWindowIndex()])
                player.stop()
                player.release()
                player.playWhenReady = false
                playbackPosition = player.currentPosition
                Log.v("exoOnDestroy", "Player released in onDestroy()")
            }
            pvm.simpleExoPlayer?.release()
            pvm.simpleExoPlayer = null

            Log.v("exoOnDestroy", "pvm.simpleExoPlayer released and null")
        }
    }

    override fun finish() {
        super.finish()
        if (pvm.simpleExoPlayer != null) {
            pvm.simpleExoPlayer?.release()
            pvm.simpleExoPlayer = null
            Log.v("exoplayerExit", "pvm.simpleExoPlayer released and null, finish")
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("playbackPosition", pvm.simpleExoPlayer?.currentPosition ?: 0L)
        outState.putInt("currentWindow", pvm.simpleExoPlayer?.currentWindowIndex ?: 0)
        outState.putBoolean("playWhenReady", pvm.simpleExoPlayer?.playWhenReady ?: true)
    }


}