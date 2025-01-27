package com.tangoplus.tangoq.dialog


import android.content.res.ColorStateList
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
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.MainPartAnalysisRVAdapter
import com.tangoplus.tangoq.adapter.DataDynamicRVAdapter
import com.tangoplus.tangoq.vo.AnalysisUnitVO
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMainPartPoseDialogBinding
import com.tangoplus.tangoq.function.MeasurementManager.extractVideoCoordinates
import com.tangoplus.tangoq.function.MeasurementManager.getVideoDimensions
import com.tangoplus.tangoq.function.MeasurementManager.setImage
import com.tangoplus.tangoq.function.MeasurementManager.setLabels
import com.tangoplus.tangoq.mediapipe.MathHelpers.isTablet
import com.tangoplus.tangoq.mediapipe.OverlayView
import com.tangoplus.tangoq.mediapipe.PoseLandmarkResult.Companion.fromCoordinates
import com.tangoplus.tangoq.viewmodel.AnalysisViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray

class MainPartPoseDialogFragment : DialogFragment() {
    private lateinit var binding : FragmentMainPartPoseDialogBinding
    private val mvm : MeasureViewModel by activityViewModels()
    private val avm : AnalysisViewModel by activityViewModels()
    private var count = false
    private lateinit var dynamicJa: JSONArray
    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var videoUrl = ""
    private var updateUI = false
    private var measureResult: JSONArray? = null
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
        avm.currentAnalysis = avm.relatedAnalyzes.find { it.seq == avm.selectedSeq }
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
                        setImage(this@MainPartPoseDialogFragment, mvm.selectedMeasure, seq, binding.ssivMPPD, "mainPart")

                        binding.ssivMPPD.viewTreeObserver.addOnGlobalLayoutListener (object : ViewTreeObserver.OnGlobalLayoutListener{
                            override fun onGlobalLayout() {
                                binding.clMPPD.requestLayout()
                                binding.ssivMPPD.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            }
                        })
                    }
                }
            }
        }
        binding.tvMPPDTitle.text = "${avm.selectedPart} - ${avm.setSeqString(avm.currentAnalysis?.seq)}"
        binding.tvMPPDSummary.text = avm.currentAnalysis?.summary
        measureResult = mvm.selectedMeasure?.measureResult
        avm.currentAnalysis?.isNormal?.let { setState(it) }
        if (avm.currentAnalysis != null) {
            when (avm.currentAnalysis?.seq) {
                1 -> {
                    // -----# 동적 측정 setUI #------
                    binding.tvMPPDSummary.textSize = if (isTablet(requireContext())) 17f else 15f
                    val dynamicString = "스쿼트 1회 동작에서 좌우 부위의 궤적을 비교합니다.\n하단에 그려진 궤적이 대칭을 이룰 수록 정상범위입니다.\n\n이동 안정성의 불균형이 생겼을 때 손은 어깨, 골반은 엉덩이, 무릎은 허벅지로 각 관절주변에 연결된 근육을 풀어주어야 합니다."
                    binding.tvMPPDSummary.text = dynamicString
                    binding.tvMPPDSummary.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.secondContainerColor))
                    binding.tvMPPDSummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.thirdColor))
                    binding.ivMPPDIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.thirdColor))

                    val ja = measureResult?.optJSONArray(1)
                    if (ja != null) {
                        avm.dynamicJa = ja
                        lifecycleScope.launch {
                            val connections = listOf(
                                15, 16, 23, 24, 25, 26
                            )
                            val coordinates = extractVideoCoordinates(avm.dynamicJa)
                            val filteredCoordinates = mutableListOf<List<Pair<Float, Float>>>()

                            for (connection in connections) {
                                val filteredCoordinate = mutableListOf<Pair<Float, Float>>()
                                for (element in coordinates) {
                                    filteredCoordinate.add(element[connection])
                                }
                                filteredCoordinates.add(filteredCoordinate)
                            }
                            setVideoAdapter(filteredCoordinates)
                        }
                    }
                }
                else -> {
                    binding.tvMPPDSummary.textSize = if (isTablet(requireContext())) 18f else 15f
                    setAdapter()
                    val labels = avm.currentAnalysis?.labels
                    if (labels != null) {
                        for (uni in labels) {
                            uni.summary = setLabels(uni)
                        }
                    }
                }
            }
        }
    }
    private fun setVideoAdapter(data: List<List<Pair<Float, Float>>>) {
        val linearLayoutManager1 = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val dynamicAdapter = DataDynamicRVAdapter(data, avm.dynamicTitles, 1)
        binding.rvMPPD.layoutManager = linearLayoutManager1
        binding.rvMPPD.adapter = dynamicAdapter
    }

    private fun setAdapter() {
        Log.v("AnalysisLabel", "labels: ${avm.currentAnalysis?.labels}")
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        val adapter = MainPartAnalysisRVAdapter(this@MainPartPoseDialogFragment, avm.currentAnalysis?.labels)
        binding.rvMPPD.layoutManager = layoutManager
        binding.rvMPPD.adapter = adapter
    }

    private fun setState(isNormal: Int) {
        when (isNormal) {
            3 -> {
                binding.tvMPPDSummary.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.deleteContainerColor))
                binding.tvMPPDSummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.deleteColor))
                binding.ivMPPDIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.deleteColor))
            }

            1 -> {
                binding.tvMPPDSummary.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.secondBgContainerColor))
                binding.tvMPPDSummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.thirdColor))
                binding.ivMPPDIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.thirdColor))
            }
            2 -> {
                binding.tvMPPDSummary.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.cautionContainerColor))
                binding.tvMPPDSummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.cautionColor))
                binding.ivMPPDIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.cautionColor))
            }
        }
    }
    private fun setDynamicUI(isDynamic: Boolean) {
        if (isDynamic) {
            binding.ssivMPPD.visibility = View.GONE
            binding.flMPPD.visibility = View.VISIBLE
        } else {
            binding.ssivMPPD.visibility = View.VISIBLE
            binding.flMPPD.visibility = View.GONE
        }
    }



    private fun setPlayer() {
        lifecycleScope.launch {
            Log.v("동적측정json", "${mvm.selectedMeasure?.measureResult?.optJSONArray(1)}")
            dynamicJa = mvm.selectedMeasure?.measureResult?.optJSONArray(1) ?: JSONArray()
            Log.v("jsonDataLength", "${dynamicJa.length()}")
            initPlayer()

            simpleExoPlayer?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    if (playbackState == Player.STATE_READY) {

                        val videoDuration = simpleExoPlayer?.duration ?: 0L
                        lifecycleScope.launch {
                            while (simpleExoPlayer?.isPlaying == true) {
                                if (!updateUI) updateVideoUI()
//                                Log.v("업데이트video", "overlay: (${binding.ovMPPD.width}, ${binding.ovMPPD.height}) PlayerView: ( ${binding.pvMPPD.width}, ${binding.pvMPPD.height} )")
                                updateFrameData(videoDuration, dynamicJa.length())
                                delay(24)
                                Handler(Looper.getMainLooper()).postDelayed( { updateUI = true },1500)
                            }
                        }
                    } else if (playbackState == Player.STATE_ENDED) {
                        val exoPlay = binding.pvMPPD.findViewById<ImageButton>(R.id.btnPlay)
                        exoPlay.visibility = View.VISIBLE
                        exoPlay.bringToFront()
                    }
                }
            })
        }
    }
    private fun updateVideoUI() {
        val (videoWidth, videoHeight) = getVideoDimensions(requireContext(), videoUrl.toUri())
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels

        val aspectRatio = videoHeight.toFloat() / videoWidth.toFloat()
        Log.v("비율", "aspectRatio: $aspectRatio, video: ($videoWidth, $videoHeight), playerView: (${binding.pvMPPD.width}, ${binding.pvMPPD.height}), overlay: (${binding.ovMPPD.width}, ${binding.ovMPPD.height})")
        val adjustedHeight = (screenWidth * aspectRatio).toInt()

        // clMA의 크기 조절
        val params = binding.clMPPD.layoutParams
        params.width = screenWidth
        params.height = adjustedHeight
        binding.clMPPD.layoutParams = params

        // llMARV를 clMA 아래에 위치시키기
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.clMPPD)
        constraintSet.connect(binding.clMPPD2.id, ConstraintSet.TOP, binding.clMPPD.id, ConstraintSet.BOTTOM)
        constraintSet.applyTo(binding.clMPPD)

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

//        val exoPlay = requireActivity().findViewById<ImageButton>(R.id.btnPlay)
        val exoPlay = binding.pvMPPD.findViewById<ImageButton>(R.id.btnPlay)
        val exoPause = binding.pvMPPD.findViewById<ImageButton>(R.id.btnPause)
//        val exoPause = requireActivity().findViewById<ImageButton>(R.id.btnPause)
        exoPause.visibility = View.GONE
        exoPlay?.setOnClickListener {
            if (simpleExoPlayer?.isPlaying == false) {
                simpleExoPlayer?.seekTo(0)
                simpleExoPlayer?.play()
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
//        Log.v("PlayerView,비디오 크기", "overlay: (${binding.ovMA.width}, ${binding.ovMA.height}), videoWidth,Height: (${videoWidth}, ${videoHeight}) width: ${binding.pvMA.width}, height: ${binding.pvMA.height}")
        val isPortrait = videoHeight > videoWidth
        if (frameIndex in 0 until totalFrames) {
            // 해당 인덱스의 데이터를 JSON에서 추출하여 변환
            val poseLandmarkResult = fromCoordinates(coordinates[frameIndex])
            // 변환된 데이터를 화면에 그리기
            requireActivity().runOnUiThread {
                binding.ovMPPD.scaleX = -1f
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
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
}