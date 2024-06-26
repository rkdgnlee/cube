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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.tangoplus.tangoq.PlayFullScreenActivity
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.`object`.NetworkExercise
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.FavoriteViewModel
import com.tangoplus.tangoq.databinding.FragmentPlayThumbnailDialogBinding
import com.tangoplus.tangoq.listener.OnMoreClickListener
import com.tangoplus.tangoq.`object`.Singleton_t_history
import kotlinx.coroutines.launch

class PlayThumbnailDialogFragment : DialogFragment(), OnMoreClickListener {
    lateinit var binding : FragmentPlayThumbnailDialogBinding
    private var videoUrl = "http://gym.tangostar.co.kr/data/contents/videos/걷기.mp4"
    val viewModel: FavoriteViewModel by activityViewModels()
    private var simpleExoPlayer: SimpleExoPlayer? = null
//    private var playWhenReady = true
//    private var currentWindow = 0
    private var playbackPosition = 0L
    private lateinit var singletonInstance: Singleton_t_history

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlayThumbnailDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        singletonInstance = Singleton_t_history.getInstance(requireContext())
        val bundle = arguments
        val exerciseData = bundle?.getParcelable<ExerciseVO>("ExerciseUnit")

// -----! 각 설명들 textView에 넣기 !-----
        videoUrl = exerciseData?.videoFilepath.toString()
        Log.w("동영상url", videoUrl)
        binding.tvPTDName.text = exerciseData?.exerciseName.toString()
        binding.tvPTDRelatedJoint.text = exerciseData?.relatedJoint.toString()

        binding.tvPTDTime.text = exerciseData?.videoDuration
        binding.tvPTDStage.text = exerciseData?.exerciseStage
        binding.tvPTDFrequency.text = exerciseData?.exerciseFrequency.toString()
        binding.tvPTDInitialPosture.text = exerciseData?.exerciseInitialPosture.toString()
        binding.tvPTDMethod.text = exerciseData?.exerciseMethod.toString()
        binding.tvPTDCaution.text = exerciseData?.exerciseCaution.toString()
        binding.tvPTDRelatedMuscle.text = exerciseData?.relatedMuscle.toString()


//        playbackPosition = intent.getLongExtra("current_position", 0L)
        initPlayer()
        // -----! 하단 운동 시작 버튼 시작 !-----
        binding.btnPTDPlay.setOnClickListener {
//            val intent = Intent(this, PlayFullScreenActivity::class.java)
//            intent.putExtra("video_url", videoUrl)
//            intent.putExtra("current_position", simpleExoPlayer?.currentPosition)
//            startActivityForResult(intent, 8080)
            val intent = Intent(requireContext(), PlayFullScreenActivity::class.java)
            intent.putExtra("exercise_id", exerciseData?.exerciseId)
            intent.putExtra("video_url", videoUrl)
            startActivityForResult(intent, 8080)

            // ------! 운동 하나 전부 다 보고 나서 feedback한개만 켜지게 !------
            viewModel.isDialogShown.value = false
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
        val exitButton = binding.pvPTD.findViewById<ImageButton>(R.id.exo_exit)
        exitButton.setOnClickListener {
            dismiss()
        }

        // ------! 앞으로 감기 뒤로 감기 시작 !------
        val replay5 = binding.pvPTD.findViewById<ImageButton>(R.id.exo_replay_5)
        val forward5 = binding.pvPTD.findViewById<ImageButton>(R.id.exo_forward_5)
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





        // ------! 관련 운동 횡 rv 시작 !------
        lifecycleScope.launch {
            val responseArrayList =
                NetworkExercise.fetchExerciseJson(getString(R.string.IP_ADDRESS_t_exercise_description))

//            val jointKeyword =
//                exerciseData?.relatedMuscle!!.contains("복근")
//                when (exerciseData?.relatedMuscle!!.contains(it)) {
//                    "복근", "척추기립근", "광배근" -> "척추"
//                    "둔근", "둔근 근육", "햄스트링", "대퇴사두근",-> "엉덩"
//                    "회전근개", "삼각근", "견갑거근" -> "어깨"
//                    "사각근", "승모근" -> "목"
//                    "내전근", "장딴지근", "가자미근" -> "무릎"
//                    "이두근", "삼두", "전완" -> "팔꿉"
//                else -> ""
//            }

            try {
                // 전체데이터에서 현재 데이터(exerciseData)와 비교함.
                val verticalDataList = responseArrayList.filter { it.relatedJoint!!.contains(
                    exerciseData?.relatedJoint!!.split(", ")[0])}.toMutableList()
                val adapter = ExerciseRVAdapter(this@PlayThumbnailDialogFragment, verticalDataList, singletonInstance.viewingHistory?.toList() ?: listOf(),
                    "recommend")
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
        binding.pvPTD.player = simpleExoPlayer
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