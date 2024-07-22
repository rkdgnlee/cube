package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.tangoplus.tangoq.PlayFullScreenActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.ProgramVO
import com.tangoplus.tangoq.databinding.FragmentPlayProgramThumbnailDialogBinding


class PlayProgramThumbnailDialogFragment : DialogFragment() {
    lateinit var binding : FragmentPlayProgramThumbnailDialogBinding
    lateinit var program : ProgramVO
    private var videoUrl = "http://gym.tangostar.co.kr/data/contents/videos/걷기.mp4"
    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var playbackPosition = 0L
    private lateinit var behavior: BottomSheetBehavior<ConstraintLayout>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlayProgramThumbnailDialogBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("Range")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        binding.nsvPPTD.isNestedScrollingEnabled = true
//        binding.rvPPTD.overScrollMode = View.OVER_SCROLL_NEVER
//        binding.rvPPTD.isNestedScrollingEnabled = false


        val bundle = arguments
        program = bundle?.getParcelable("Program")!!

        binding.tvPPTDName.text = program.programName
//        binding.tvPPTDExplain.text=  program.programDescription
        binding.tvPPTDTime.text = (if (program.programTime <= 60) {
            "${program.programTime}초"
        } else {
            "${program.programTime / 60}분 ${program.programTime % 60}초"
        }).toString()
        binding.tvPPTDCount.text = "${program.programCount} 개"
        videoUrl = program.exercises?.get(0)?.videoFilepath.toString()
        initPlayer()

        // ------! behavior 조작 시작 !------
        val isTablet = resources.configuration.screenWidthDp >= 600
        behavior = BottomSheetBehavior.from(binding.clPPTD)
        val screenHeight = resources.displayMetrics.heightPixels
        val topSpaceHeight = resources.getDimensionPixelSize(R.dimen.top_space_height_dialog)
        val peekHeight = screenHeight - topSpaceHeight
        behavior.apply {
            this.peekHeight = peekHeight
            isFitToContents = false
            expandedOffset = 0
            state = BottomSheetBehavior.STATE_COLLAPSED
            skipCollapsed = false
            // 기기 유형에 따라 halfExpandedRatio 설정
            halfExpandedRatio = if (isTablet) {
                0.65f
            } else {
                0.99f
            }
        }
        // ------! behavior 조작 끝 !------

        val adapter = ExerciseRVAdapter(this@PlayProgramThumbnailDialogFragment, program.exercises!!, listOf(), "main")
        binding.rvPPTD.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(requireContext() ,LinearLayoutManager.VERTICAL, false)
        binding.rvPPTD.layoutManager = linearLayoutManager


        binding.btnPPTDPlay.setOnClickListener {
            val urls = storePickUrl(program.exercises!!)
            val intent = Intent(requireContext(), PlayFullScreenActivity::class.java)
            intent.putStringArrayListExtra("urls", ArrayList(urls))
            intent.putExtra("total_time", program.programTime)
            requireContext().startActivity(intent)
            startActivityForResult(intent, 8080)
        }

        // ------! 전체화면 구현 로직 시작 !------
        val exitButton = binding.pvPPTD.findViewById<ImageButton>(R.id.exo_exit)
        exitButton.setOnClickListener {
            dismiss()
        }

        // ------! 앞으로 감기 뒤로 감기 시작 !------
        val replay5 = binding.pvPPTD.findViewById<ImageButton>(R.id.exo_replay_5)
        val forward5 = binding.pvPPTD.findViewById<ImageButton>(R.id.exo_forward_5)
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
    private fun adjustDialogHeight(orientation: Int) {
        val params = binding.root.layoutParams
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            params.height = resources.displayMetrics.heightPixels
        } else {
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        binding.root.layoutParams = params
    }

    private fun storePickUrl(currentItem : MutableList<ExerciseVO>) : MutableList<String> {
        val urls = mutableListOf<String>()
        for (i in currentItem.indices) {
            val exercise = currentItem[i]
            urls.add(exercise.videoFilepath.toString())
        }
        Log.v("urls", "${urls}")
        return urls
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        adjustDialogHeight(newConfig.orientation)
    }

    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    private fun initPlayer(){
        simpleExoPlayer = SimpleExoPlayer.Builder(requireContext()).build()
        binding.pvPPTD.player = simpleExoPlayer
        buildMediaSource().let {
            simpleExoPlayer?.prepare(it)
        }
        simpleExoPlayer?.seekTo(playbackPosition)
    }
    private fun buildMediaSource() : MediaSource {
        val dataSourceFactory = DefaultDataSourceFactory(requireContext(), "sample")
        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoUrl))
    }
    // 일시중지
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
            videoUrl = data?.getStringExtra("video_url").toString()
            playbackPosition = currentPosition!!
            initPlayer()
        }
    }
}