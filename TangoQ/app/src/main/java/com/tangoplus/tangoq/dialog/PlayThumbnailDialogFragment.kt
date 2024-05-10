package com.tangoplus.tangoq.dialog

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.`object`.NetworkExerciseService
import com.tangoplus.tangoq.PlaySkeletonActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.databinding.FragmentPlayThumbnailDialogBinding
import kotlinx.coroutines.launch

class PlayThumbnailDialogFragment : DialogFragment() {
    lateinit var binding : FragmentPlayThumbnailDialogBinding
    private var videoUrl = "http://gym.tangostar.co.kr/data/contents/videos/걷기.mp4"

    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var player : SimpleExoPlayer? = null
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition = 0L
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlayThumbnailDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bundle = arguments
        val exerciseData = bundle?.getParcelable<ExerciseVO>("ExerciseUnit")

// -----! 각 설명들 textView에 넣기 !-----
        videoUrl = exerciseData?.videoFilepath.toString()
        Log.w("동영상url", videoUrl)
        binding.tvPlayExerciseName.text = exerciseData?.exerciseName.toString()
        binding.tvPlayExerciseRelateJoint.text = exerciseData?.relatedJoint.toString()

        binding.tvPlayExerciseTime.text = exerciseData?.videoTime.toString()
        binding.tvPlayExerciseStage.text = when (exerciseData?.exerciseStage) {
            "초기" -> "초급자"
            "향상" -> "상급자"
            "유지" -> "중급자"
            else -> "기타"
        }
        binding.tvPlayExerciseFrequency.text = exerciseData?.exerciseFequency.toString()
        binding.tvPlayExerciseInitialPosture.text = exerciseData?.exerciseInitialPosture.toString()
        binding.tvPlayExerciseMethod.text = exerciseData?.exerciseMethod.toString()
        binding.tvPlayExerciseCaution.text = exerciseData?.exerciseCaution.toString()


//        playbackPosition = intent.getLongExtra("current_position", 0L)
        initPlayer()
        // -----! 하단 운동 시작 버튼 시작 !-----
        binding.btnExercisePlay.setOnClickListener {
//            val intent = Intent(this, PlayFullScreenActivity::class.java)
//            intent.putExtra("video_url", videoUrl)
//            intent.putExtra("current_position", simpleExoPlayer?.currentPosition)
//            startActivityForResult(intent, 8080)
            val intent = Intent(requireContext(), PlaySkeletonActivity::class.java)
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
        val exitButton = binding.pvPT.findViewById<ImageButton>(R.id.exo_exit)
        exitButton.setOnClickListener {
            dismiss()
        }


        binding.tvPlayExerciseRelateMuscle.setOnClickListener {
            val dialog = FeedbackDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "FeedbackDialogFragment")
        }


        // ------! 관련 운동 횡 rv 시작 !------
        lifecycleScope.launch {
            val responseArrayList =
                NetworkExerciseService.fetchExerciseJson(getString(R.string.IP_ADDRESS_t_Exercise_Description))
            try {
                val verticalDataList = responseArrayList.filter { it.exerciseName!!.contains(
                    exerciseData!!.exerciseName!!.substring(0, 1))}.toMutableList()
                val adapter = ExerciseRVAdapter(this@PlayThumbnailDialogFragment, verticalDataList, "recommend")
                binding.rvPTn.adapter = adapter
                val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                binding.rvPTn.layoutManager = linearLayoutManager
            } catch (e: Exception) {
                Log.e(ContentValues.TAG, "Error storing exercises", e)
            }
        }

        // ------! 관련 운동 횡 rv 끝 !------

    }

    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        simpleExoPlayer?.playWhenReady = true
    }
    private fun initPlayer(){
        simpleExoPlayer = SimpleExoPlayer.Builder(requireContext()).build()
        binding.pvPT.player = simpleExoPlayer
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
            val VideoUrl = data?.getStringExtra("video_url")

            videoUrl = VideoUrl.toString()
            playbackPosition = currentPosition!!
            initPlayer()
        }
    }
}