package com.tangoplus.tangoq.dialog


import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.ImageButton
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMainPartPoseDialogBinding
import com.tangoplus.tangoq.db.MeasurementManager.extractVideoCoordinates
import com.tangoplus.tangoq.db.MeasurementManager.getVideoDimensions
import com.tangoplus.tangoq.db.MeasurementManager.setImage
import com.tangoplus.tangoq.mediapipe.OverlayView
import com.tangoplus.tangoq.mediapipe.PoseLandmarkResult.Companion.fromCoordinates
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray

class MainPartPoseDialogFragment : DialogFragment() {
    private lateinit var binding : FragmentMainPartPoseDialogBinding
    private val mvm : MeasureViewModel by activityViewModels()
    private var count = false
    private lateinit var dynamicJa: JSONArray
    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var videoUrl = "http://gym.tangostar.co.kr/data/contents/videos/걷기.mp4"
    private var updateUI = false

    companion object {
        private const val ARG_SEQ = "arg_seq"
        fun newInstance(seq: Int): MainPartPoseDialogFragment {
            val fragment = MainPartPoseDialogFragment()
            val args = Bundle()
            args.putInt(ARG_SEQ, seq)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainPartPoseDialogBinding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.DialogTheme)
        val seq = arguments?.getInt(ARG_SEQ)
        if (seq != null) {
            lifecycleScope.launch {

                when (seq) {
                    1 -> {
                        setDynamicUI(true)
                        setPlayer()
                    }
                    else -> {
                        setDynamicUI(false)
                        setImage(this@MainPartPoseDialogFragment, mvm.selectedMeasure, seq, binding.ssivMPPD)
                        binding.ssivMPPD.viewTreeObserver.addOnGlobalLayoutListener (object : ViewTreeObserver.OnGlobalLayoutListener{
                            override fun onGlobalLayout() {
                                binding.ssivMPPD.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            }
                        })
                    }
                }
            }
        }
    }

    private fun setDynamicUI(isDynamic: Boolean) {
        if (isDynamic) {
            binding.ssivMPPD.visibility = View.GONE
            binding.ovMPPD.visibility = View.VISIBLE
            binding.flMPPD.visibility = View.VISIBLE
            binding.pvMPPD.visibility = View.VISIBLE
        } else {
            binding.ssivMPPD.visibility = View.VISIBLE
            binding.ovMPPD.visibility = View.GONE
            binding.flMPPD.visibility = View.GONE
            binding.pvMPPD.visibility = View.GONE
        }
    }



    private fun setPlayer() {
        lifecycleScope.launch {
            Log.v("동적측정json", "${mvm.selectedMeasure?.measureResult!!.getJSONArray(1)}")
            dynamicJa = mvm.selectedMeasure?.measureResult!!.getJSONArray(1)
            Log.v("jsonDataLength", "${dynamicJa.length()}")
            initPlayer()

            simpleExoPlayer?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    if (playbackState == Player.STATE_READY) {

                        val videoDuration = simpleExoPlayer?.duration ?: 0L
                        lifecycleScope.launch {
                            while (simpleExoPlayer?.isPlaying == true) {
                                if (!updateUI) updateVideoUI(mvm.selectedMeasure?.isMobile!!)
                                updateFrameData(videoDuration, dynamicJa.length())
                                delay(24)
                                Handler(Looper.getMainLooper()).postDelayed( {updateUI = true},1000)
                            }
                        }
                    }
                }
            })
        }
    }
    private fun updateVideoUI(isMobile: Boolean) {
        Log.v("업데이트", "UI")

        val (videoWidth, videoHeight) = getVideoDimensions(requireContext(), videoUrl.toUri())
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels

        // 비디오 높이를 화면 너비에 맞게 조절
        val aspectRatio = if (isMobile) videoWidth.toFloat() / videoHeight.toFloat() else videoHeight.toFloat() / videoWidth.toFloat()
        val adjustedHeight = (screenWidth * aspectRatio).toInt()

        // clMA의 크기 조절
        val params = binding.clMPPD.layoutParams
        params.width = screenWidth
        params.height = adjustedHeight
        binding.clMPPD.layoutParams = params

        // PlayerView 크기 조절 (필요한 경우)
        val playerParams = binding.pvMPPD.layoutParams
        playerParams.width = screenWidth
        playerParams.height = adjustedHeight
        binding.pvMPPD.layoutParams = playerParams

    }

    private fun initPlayer() {
        simpleExoPlayer = SimpleExoPlayer.Builder(requireContext()).build()
        binding.pvMPPD.player = simpleExoPlayer
        binding.pvMPPD.controllerShowTimeoutMs = 1100
        lifecycleScope.launch {
            videoUrl = mvm.selectedMeasure?.fileUris?.get(1).toString()
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
            val mediaSource = ProgressiveMediaSource.Factory(DefaultDataSourceFactory(requireContext()))
                .createMediaSource(mediaItem)
            mediaSource.let {
                simpleExoPlayer?.prepare(it)
                simpleExoPlayer?.seekTo(0)
                simpleExoPlayer?.playWhenReady = true
            }
        }
        binding.pvMPPD.findViewById<ImageButton>(R.id.exo_replay_5).visibility = View.GONE
        binding.pvMPPD.findViewById<ImageButton>(R.id.exo_exit).visibility = View.GONE
        binding.pvMPPD.findViewById<ImageButton>(R.id.exo_forward_5).visibility = View.GONE

        val exoPlay = requireActivity().findViewById<ImageButton>(R.id.btnPlay)
        val exoPause = requireActivity().findViewById<ImageButton>(R.id.btnPause)
        exoPause?.setOnClickListener {
            if (simpleExoPlayer?.isPlaying == true) {
                simpleExoPlayer?.pause()
                exoPause.visibility = View.GONE
                exoPlay.visibility = View.VISIBLE
            }
        }
        exoPlay?.setOnClickListener {
            if (simpleExoPlayer?.isPlaying == false) {
                simpleExoPlayer?.play()
                exoPause.visibility = View.VISIBLE
                exoPlay.visibility = View.GONE
            }
        }
    }

    private fun updateFrameData(videoDuration: Long, totalFrames: Int) {
        val currentPosition = simpleExoPlayer?.currentPosition ?: 0L

        // 현재 재생 시간에 해당하는 프레임 인덱스 계산
        val frameIndex = ((currentPosition.toFloat() / videoDuration) * totalFrames).toInt()
        val coordinates = extractVideoCoordinates(dynamicJa)
        // 실제 mp4의 비디오 크기를 가져온다
        val (videoWidth, videoHeight) = getVideoDimensions(requireContext(), videoUrl.toUri())
        if (frameIndex in 0 until totalFrames) {
            // 해당 인덱스의 데이터를 JSON에서 추출하여 변환
            val poseLandmarkResult = fromCoordinates(coordinates[frameIndex])
            // 변환된 데이터를 화면에 그리기
            requireActivity().runOnUiThread {
                binding.ovMPPD.setResults(
                    poseLandmarkResult,
                    videoWidth,
                    videoHeight,
                    OverlayView.RunningMode.VIDEO
                )
                binding.ovMPPD.invalidate()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        simpleExoPlayer?.stop()
        simpleExoPlayer?.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        simpleExoPlayer?.release()
    }
    override fun onResume() {
        super.onResume()
//        isCancelable = false
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog?.window?.setDimAmount(0.6f) // 원하는 만큼의 어둠 설정
        simpleExoPlayer?.playWhenReady = true
    }
}