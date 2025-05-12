package com.tangoplus.tangobody.dialog

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.video.VideoSize
import com.tangoplus.tangobody.R
import com.tangoplus.tangobody.databinding.FragmentPoseDialogBinding
import com.tangoplus.tangobody.function.MeasurementManager.extractVideoCoordinates
import com.tangoplus.tangobody.function.MeasurementManager.getVideoDimensions
import com.tangoplus.tangobody.function.MeasurementManager.setImage
import com.tangoplus.tangobody.vision.MathHelpers.isTablet
import com.tangoplus.tangobody.vision.OverlayView
import com.tangoplus.tangobody.vision.PoseLandmarkResult.Companion.fromCoordinates
import com.tangoplus.tangobody.viewmodel.MeasureViewModel
import com.tangoplus.tangobody.viewmodel.PlayViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class PoseDialogFragment : DialogFragment() {
    private lateinit var binding : FragmentPoseDialogBinding
    private val mvm :  MeasureViewModel by activityViewModels()
    private val pvm : PlayViewModel by activityViewModels()
    private var seq = 0
    private var updateUI = false
    private var videoUrl = ""

    private var exoPlay: ImageButton? = null
    private var exoPause: ImageButton? = null
    private var llSpeed: LinearLayout? = null
    private var btnSpeed: ImageButton? = null
    private var exo05: ImageButton? = null
    private var exo075: ImageButton? = null
    private var exo10: ImageButton? = null
    private var cvLeft : CardView? = null
    private var cvRight : CardView? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPoseDialogBinding.inflate(inflater)
        return binding.root
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_FlexableDialogFragment)
    }
    companion object {
        const val ARG_SEQ_INDEX = "pose_seq_index"
        fun newInstance(seq: Int) : PoseDialogFragment {
            val fragment = PoseDialogFragment()
            val args = Bundle()
            args.putInt(ARG_SEQ_INDEX, seq)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        seq = arguments?.getInt(ARG_SEQ_INDEX) ?: 0
        // api35이상 화면 크기 조절
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 상태 표시줄 높이만큼 상단 패딩 적용
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        when (seq) {
            1 -> {
// 영상
                binding.ssivPD.visibility = View.GONE
                binding.cvExoLeft.visibility = View.VISIBLE
                binding.cvExoRight.visibility = View.VISIBLE
                binding.flPD.visibility = View.VISIBLE
                binding.ovPD.visibility = View.VISIBLE
                setPlayer()
                // 0, 1, 2 (이미지 일 때)
                val params = binding.clPD.layoutParams
                params.width = binding.flPD.width
                params.height = binding.flPD.height
                binding.clPD.layoutParams = params

                pvm.setPlaybackPosition(0L)
                pvm.setWindowIndex(0)
                videoUrl = mvm.selectedMeasure?.fileUris?.get(1).toString()
                exoPlay = view.findViewById(R.id.btnPlay)
                exoPause = view.findViewById(R.id.btnPause)
                llSpeed = view.findViewById(R.id.llSpeed)
                btnSpeed = view.findViewById(R.id.btnSpeed)

                exo05 = view.findViewById(R.id.btn05)
                exo075 = view.findViewById(R.id.btn075)
                exo10 = view.findViewById(R.id.btn10)

                setClickListener()
            }
            else -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    setImage(this@PoseDialogFragment, mvm.selectedMeasure, seq, binding.ssivPD, "solo")

                    withContext(Dispatchers.Main) {
                        binding.ssivPD.viewTreeObserver.addOnGlobalLayoutListener(object :
                            ViewTreeObserver.OnGlobalLayoutListener {
                            override fun onGlobalLayout() {
                                binding.ssivPD.viewTreeObserver.removeOnGlobalLayoutListener(this)

                                val measuredHeight = binding.ssivPD.measuredHeight
                                val aspectRatio = binding.ssivPD.sWidth.toFloat() / binding.ssivPD.sHeight.toFloat()
                                val calculatedWidth = (measuredHeight * aspectRatio).toInt()

                                dialog?.window?.setLayout(calculatedWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
                            }
                        })
                        binding.clPD.requestLayout()
                    }
                }

                releasePlayer()
                binding.cvExoLeft.visibility = View.GONE
                binding.cvExoRight.visibility = View.GONE
                binding.ssivPD.visibility = View.VISIBLE
                binding.flPD.visibility = View.GONE
                if (!updateUI) updateUI = false
            }
        }
    }

    //---------------------------------------! VideoOverlay !---------------------------------------
    private fun setPlayer() {
        lifecycleScope.launch {
            pvm.dynamicJa = mvm.selectedMeasure?.measureResult?.getJSONArray(1) ?: JSONArray()
            initPlayer()

            pvm.simpleExoPlayer?.addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    super.onVideoSizeChanged(videoSize)
                    val videoWidth = videoSize.width
                    val videoHeight = videoSize.height
                    val aspectRatio = videoWidth.toFloat() / videoHeight.toFloat()

                    // 현재 PlayerView의 높이를 기준으로 가로 크기 설정
                    binding.pvPD.post {
                        val playerViewHeight = binding.pvPD.measuredHeight
                        val calculatedWidth = (playerViewHeight * aspectRatio).toInt()


                        dialog?.window?.setLayout(calculatedWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
//                        binding.flPD.background = resources.getDrawable(R.drawable.bckgnd_rectangle_20, null)
//                        dialog?.window?.setBackgroundDrawable(resources.getDrawable(R.drawable.bckgnd_rectangle_20, null))
                    }
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    if (playbackState == Player.STATE_READY) {

                        val videoDuration = pvm.simpleExoPlayer?.duration ?: 0L
                        lifecycleScope.launch {
                            while (pvm.simpleExoPlayer?.isPlaying == true) {
                                updateVideoUI()
                                updateFrameData(videoDuration, pvm.dynamicJa.length())
                                delay(24)
                                Handler(Looper.getMainLooper()).postDelayed( { updateUI = true },1500)
                            }
                        }
                    } else if (playbackState == Player.STATE_ENDED) {
                        exoPlay?.visibility = View.VISIBLE
                        exoPlay?.bringToFront()
                        exoPause?.visibility = View.GONE
                        pvm.setPlaybackPosition(0)
                    }
                }
            })
        }
    }

    private fun initPlayer() {
        // viewModel의 이전 영상 보존값들 초기화

        pvm.simpleExoPlayer = SimpleExoPlayer.Builder(requireContext()).build()
        binding.pvPD.player = pvm.simpleExoPlayer
        binding.pvPD.controllerShowTimeoutMs = 1100
        lifecycleScope.launch {
            // 저장된 URL이 있다면 사용, 없다면 새로운 URL 가져오기
            videoUrl = mvm.selectedMeasure?.fileUris?.get(1).toString()

            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
            val mediaSource = ProgressiveMediaSource.Factory(DefaultDataSourceFactory(requireContext()))
                .createMediaSource(mediaItem)

            mediaSource.let {
                pvm.simpleExoPlayer?.prepare(it)
                // 저장된 위치로 정확하게 이동
                pvm.simpleExoPlayer?.seekTo(pvm.getPlaybackPosition())
                pvm.simpleExoPlayer?.playWhenReady = pvm.getPlayWhenReady()
            }
        }
        binding.pvPD.findViewById<ImageButton>(R.id.exo_replay_5).visibility = View.GONE
        binding.pvPD.findViewById<ImageButton>(R.id.exo_exit).visibility = View.GONE
        binding.pvPD.findViewById<ImageButton>(R.id.exo_forward_5).visibility = View.GONE
    }

    private fun setClickListener() {
//        if (avm.currentIndex == 3) {
//            exoPause?.visibility = View.VISIBLE
//        }
        exoPlay?.visibility = View.GONE
        exoPlay?.setOnClickListener {
            pvm.simpleExoPlayer?.seekTo(pvm.getPlaybackPosition())
            pvm.simpleExoPlayer?.play()
            exoPlay?.visibility = View.GONE
            exoPause?.visibility = View.VISIBLE
        }
        exoPause?.setOnClickListener{
            pvm.simpleExoPlayer?.let { it1 -> pvm.savePlayerState(it1) }
            pvm.simpleExoPlayer?.pause()
            exoPlay?.visibility = View.VISIBLE
            exoPause?.visibility = View.GONE
        }
        var isShownSpeed = false
        var forChanged = false

        llSpeed?.visibility = View.VISIBLE

        btnSpeed?.setOnClickListener{
            if (!isShownSpeed) {
                exo05?.visibility = View.VISIBLE
                exo075?.visibility = View.VISIBLE
                isShownSpeed = true
            } else {
                exo05?.visibility = View.GONE
                exo075?.visibility = View.GONE
                isShownSpeed = false
            }
        }

        exo05?.setOnClickListener {
            if (forChanged) {
                exo05?.visibility = View.VISIBLE
                exo075?.visibility = View.VISIBLE
                exo10?.visibility = View.VISIBLE
                forChanged = false
            } else {
                pvm.simpleExoPlayer?.playbackParameters = PlaybackParameters(0.5f)
                exo05?.visibility = View.VISIBLE
                exo075?.visibility = View.GONE
                exo10?.visibility = View.GONE
                btnSpeed?.visibility = View.GONE
                isShownSpeed = false
                forChanged = true
            }

        }
        exo075?.setOnClickListener {
            if (forChanged) {
                exo05?.visibility = View.VISIBLE
                exo075?.visibility = View.VISIBLE
                exo10?.visibility = View.VISIBLE
                forChanged = false
            } else {
                pvm.simpleExoPlayer?.playbackParameters = PlaybackParameters(0.75f)
                exo05?.visibility = View.GONE
                exo075?.visibility = View.VISIBLE
                exo10?.visibility = View.GONE
                btnSpeed?.visibility = View.GONE
                isShownSpeed = false
                forChanged = true
            }
        }
        exo10?.setOnClickListener {
            if (forChanged) {
                exo05?.visibility = View.VISIBLE
                exo075?.visibility = View.VISIBLE
                exo10?.visibility = View.VISIBLE
                forChanged = false
            } else {
                pvm.simpleExoPlayer?.playbackParameters = PlaybackParameters(1.0f)
                exo05?.visibility = View.GONE
                exo075?.visibility = View.GONE
                exo10?.visibility = View.VISIBLE
                btnSpeed?.visibility = View.GONE
                isShownSpeed = false
                forChanged = true
            }
        }

        cvLeft?.visibility = View.VISIBLE
        cvRight?.visibility = View.VISIBLE
    }
    private fun updateFrameData(videoDuration: Long, totalFrames: Int) {
        val currentPosition = pvm.simpleExoPlayer?.currentPosition ?: 0L

        val frameIndex = ((currentPosition.toFloat() / videoDuration) * totalFrames).toInt()
        val coordinates = extractVideoCoordinates(pvm.dynamicJa)

        val (videoWidth, videoHeight) = getVideoDimensions(requireContext(), videoUrl?.toUri())
        if (frameIndex in 0 until totalFrames) {

            val poseLandmarkResult = fromCoordinates(coordinates[frameIndex])
            requireActivity().runOnUiThread {
                binding.ovPD.scaleX = -1f
                binding.ovPD.setResults(
                    poseLandmarkResult,
                    videoWidth,
                    videoHeight,
                    OverlayView.RunningMode.VIDEO
                )
                binding.ovPD.invalidate()
            }
        }
    }

    private fun updateVideoUI() {
        binding.ssivPD.visibility = View.GONE

        val (videoWidth, videoHeight) = getVideoDimensions(requireContext(), videoUrl.toUri())
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels

        val aspectRatio = videoHeight.toFloat() / videoWidth.toFloat()
        val adjustedHeight = (screenWidth * aspectRatio).toInt()

        val resizingValue = if (isTablet(requireContext())) {
            if (aspectRatio > 1) {
                0.55f
            } else { // 가로 (키오스크 일 때 원본 유지 )
                1f
            }
        } else {
            if (aspectRatio > 1) {
                0.65f
            } else { // 가로 (키오스크 일 때 원본 유지 )
                1f
            }
        }

        // clMA의 크기 조절
        val params = binding.clPD.layoutParams
        params.width = (screenWidth  * resizingValue).toInt()
        params.height = (adjustedHeight * resizingValue).toInt()
        binding.clPD.layoutParams = params

        val connections = listOf(
            15, 16, 23, 25, 26 // 좌측 골반의 pose번호를 가져옴
        )
        val coordinates = extractVideoCoordinates(pvm.dynamicJa)
        val filteredCoordinates = mutableListOf<List<Pair<Float, Float>>>()
        for (connection in connections) {
            val filteredCoordinate = mutableListOf<Pair<Float, Float>>() // 부위 하나당 몇 십 프레임의 x,y 좌표임
            for (element in coordinates) {
                if (connection == 23) {
                    val a = element[connection]
                    val b = element[connection]
                    val midHip = Pair((a.first + b.first) / 2, (a.second + b.second) / 2)
                    filteredCoordinate.add(midHip)
                } else {
                    filteredCoordinate.add(element[connection]) // element의 23, 24가 각각 담김
                }
            }
            filteredCoordinates.add(filteredCoordinate)
        }
        // 이제 data 는 size가 6개가 아니라 5개임.
//        setVideoAdapter(filteredCoordinates)
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }
    override fun onStop() {
        super.onStop()
        releasePlayer()
    }
    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    override fun onResume() {
        super.onResume()
        pvm.simpleExoPlayer?.playWhenReady = true
    }

    private fun releasePlayer() {
        pvm.simpleExoPlayer?.release()
        pvm.simpleExoPlayer = null
        binding.pvPD.player = null
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        dialog?.window?.setBackgroundDrawable(resources.getDrawable(R.drawable.bckgnd_rectangle_20, null))

        val background = dialog?.window?.decorView?.background
        if (background != null) {
            val wrappedDrawable = DrawableCompat.wrap(background).mutate()
            DrawableCompat.setTint(wrappedDrawable, Color.parseColor("#00FFFFFF"))
            dialog?.window?.setBackgroundDrawable(wrappedDrawable)
        }
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

}