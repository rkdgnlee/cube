package com.tangoplus.tangoq.adapter

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvAnalysisItemBinding
import com.tangoplus.tangoq.function.BiometricManager
import com.tangoplus.tangoq.function.MeasurementManager.extractVideoCoordinates
import com.tangoplus.tangoq.function.MeasurementManager.getVideoDimensions
import com.tangoplus.tangoq.function.MeasurementManager.setImage
import com.tangoplus.tangoq.mediapipe.MathHelpers.isTablet
import com.tangoplus.tangoq.mediapipe.OverlayView
import com.tangoplus.tangoq.mediapipe.PoseLandmarkResult.Companion.fromCoordinates
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.viewmodel.PlayViewModel
import com.tangoplus.tangoq.vo.AnalysisUnitVO
import com.tangoplus.tangoq.vo.AnalysisVO
import com.tangoplus.tangoq.vo.MeasureVO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class AnalysisRVAdapter(private val fragment: Fragment,
                        private var analysises : List<AnalysisVO>,
                        private val mvm: MeasureViewModel,
                        private val pvm: PlayViewModel)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var updateUI = false
    // 영상재생
    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var videoUrl = ""
    private lateinit var jsonArray: JSONArray
    private lateinit var biometricManager : BiometricManager

    private var exoPlay: ImageButton? = null
    private var exoPause: ImageButton? = null
    private var llSpeed: LinearLayout? = null
    private var btnSpeed: ImageButton? = null
    private var exo05: ImageButton? = null
    private var exo075: ImageButton? = null
    private var exo10: ImageButton? = null
    private var cvLeft : CardView? = null
    private var cvRight : CardView? = null
    private lateinit var dynamicJa: JSONArray


    inner class AnalysisViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rvAI: RecyclerView = view.findViewById(R.id.rvAI)
        val ssivAI1 : SubsamplingScaleImageView = view.findViewById(R.id.ssivAI1)
        val ssivAI2 : SubsamplingScaleImageView = view.findViewById(R.id.ssivAI2)
        val tvAIPart1: TextView = view.findViewById(R.id.tvAIPart1)
        val tvAIPart2: TextView = view.findViewById(R.id.tvAIPart2)
        val flAI : FrameLayout = view.findViewById(R.id.flAI)
        val pvAI : PlayerView = view.findViewById(R.id.pvAI)
        val ovAI: OverlayView = view.findViewById(R.id.ovAI)
        val cv_exo_left : CardView = view.findViewById(R.id.cv_exo_left)
        val cv_exo_right : CardView = view.findViewById(R.id.cv_exo_right)
        val clAI: ConstraintLayout = view.findViewById(R.id.clAI)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvAnalysisItemBinding.inflate(inflater, parent, false)
        return AnalysisViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        Log.v("analysisSIze", "${analysises.size}")
        return analysises.size ?: 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        if (holder is AnalysisViewHolder) {
            Log.v("애널러시스", "$analysises")
            val currentItem = analysises.get(position)
            // 나머지 이미지와 seq & UI 연결
            // 하단 analysisUnitVO adapter 연결
            val layoutManager = LinearLayoutManager(fragment.requireContext(), LinearLayoutManager.VERTICAL, false)
            val adapter = MainPartAnalysisRVAdapter(fragment, currentItem.labels)
            holder.rvAI.layoutManager = layoutManager
            holder.rvAI.adapter = adapter
            fragment.lifecycleScope.launch(Dispatchers.IO) {
                when(currentItem.indexx) {
                    3 -> {

                    }
                    0 -> {

                    }
                    1 -> {

                    }
                    2-> {


                    }
                }
            }
        }
    }
    private fun setUI(holder: AnalysisViewHolder, isDynamic: Boolean) {
        if (isDynamic) {
            holder.ssivAI1.visibility = View.GONE
            holder.ssivAI2.visibility = View.GONE
            holder.flAI.visibility = View.VISIBLE
            holder.cv_exo_left.visibility = View.VISIBLE
            holder.cv_exo_right.visibility = View.VISIBLE
            holder.tvAIPart1.visibility = View.GONE
            holder.tvAIPart2.visibility = View.GONE
        } else {
            holder.ssivAI1.visibility = View.VISIBLE
            holder.ssivAI2.visibility = View.VISIBLE
            holder.flAI.visibility = View.GONE
            holder.cv_exo_left.visibility = View.GONE
            holder.cv_exo_right.visibility = View.GONE
            holder.tvAIPart1.visibility = View.VISIBLE
            holder.tvAIPart2.visibility = View.VISIBLE
        }
    }
    private fun setPlayer(holder: AnalysisViewHolder) {
        // 외부 코루틴 제거
        Log.v("동적측정json", "${mvm.selectedMeasure?.measureResult?.optJSONArray(1)}")
        dynamicJa = mvm.selectedMeasure?.measureResult?.optJSONArray(1) ?: JSONArray()
        Log.v("jsonDataLength", "${dynamicJa.length()}")
        initPlayer(holder)

        simpleExoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                when (playbackState) {
                    Player.STATE_READY -> {
                        val videoDuration = simpleExoPlayer?.duration ?: 0L
                        fragment.viewLifecycleOwner.lifecycleScope.launch {
                            while (simpleExoPlayer?.isPlaying == true) {
                                if (!updateUI) updateVideoUI(holder)
                                updateFrameData(videoDuration, dynamicJa.length(), holder)
                                delay(24)
                                Handler(Looper.getMainLooper()).postDelayed({ updateUI = true }, 1500)
                            }
                        }
                    }
                    Player.STATE_ENDED -> {
                        // binding을 사용하고 있으니, 여기서도 binding을 통해 접근
                        holder.pvAI.findViewById<ImageButton>(R.id.btnPlay)?.apply {
                            visibility = View.VISIBLE
                            bringToFront()
                        }
                        holder.pvAI.findViewById<ImageButton>(R.id.btnPause)?.apply {
                            visibility = View.GONE
                        }
                    }
                }
            }
        })
    }

    private fun updateVideoUI(holder: AnalysisViewHolder) {
        val (videoWidth, videoHeight) = getVideoDimensions(fragment.requireContext(), videoUrl.toUri())
        val displayMetrics = DisplayMetrics()
        fragment.requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels

        val aspectRatio = videoHeight.toFloat() / videoWidth.toFloat()
//        Log.v("비율", "aspectRatio: $aspectRatio, video: ($videoWidth, $videoHeight), playerView: (${binding.pvMPPD.width}, ${binding.pvMPPD.height}), overlay: (${binding.ovMPPD.width}, ${binding.ovMPPD.height})")
        val adjustedHeight = (screenWidth * aspectRatio).toInt()
        val resizingValue = if (isTablet(fragment.requireContext())) {
            if (aspectRatio > 1) {
                0.7f
            } else { // 가로 (키오스크 일 때 원본 유지 )
                1f
            }
        } else 1f
        // clMA의 크기 조절
        val params = holder.clAI.layoutParams
        params.width = (screenWidth  * resizingValue).toInt()
        params.height = (adjustedHeight * resizingValue).toInt()
        holder.clAI.layoutParams = params

        // llMARV를 clMA 아래에 위치시키기
        val constraintSet = ConstraintSet()
        constraintSet.clone(holder.clAI)
        constraintSet.connect(holder.rvAI.id, ConstraintSet.TOP, holder.clAI.id, ConstraintSet.BOTTOM)
        constraintSet.applyTo(holder.clAI)

        // PlayerView 크기 조절 (필요한 경우)
        val playerParams = holder.pvAI.layoutParams
        playerParams.width = (screenWidth  * resizingValue).toInt()
        playerParams.height = (adjustedHeight * resizingValue).toInt()
        holder.pvAI.layoutParams = playerParams

    }

    private fun initPlayer(holder: AnalysisViewHolder) {
        simpleExoPlayer = SimpleExoPlayer.Builder(fragment.requireContext()).build()
        holder.pvAI.player = simpleExoPlayer
        holder.pvAI.controllerShowTimeoutMs = 1100

        fragment.lifecycleScope.launch {
            // 저장된 URL이 있다면 사용, 없다면 새로운 URL 가져오기

            videoUrl = pvm.videoUrl ?: mvm.selectedMeasure?.fileUris?.get(1).toString()
            pvm.videoUrl = videoUrl  // URL 저장

            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
            val mediaSource = ProgressiveMediaSource.Factory(DefaultDataSourceFactory(fragment.requireContext()))
                .createMediaSource(mediaItem)

            mediaSource.let {
                simpleExoPlayer?.prepare(it)
                // 저장된 위치로 정확하게 이동
                simpleExoPlayer?.seekTo(pvm.getPlaybackPosition())
                simpleExoPlayer?.playWhenReady = pvm.getPlayWhenReady()
            }
        }
        holder.pvAI.findViewById<ImageButton>(R.id.exo_replay_5).visibility = View.GONE
        holder.pvAI.findViewById<ImageButton>(R.id.exo_exit).visibility = View.GONE
        holder.pvAI.findViewById<ImageButton>(R.id.exo_forward_5).visibility = View.GONE

    }

    private fun setClickListener() {
        exoPlay?.visibility = View.GONE
        exoPlay?.setOnClickListener {
            simpleExoPlayer?.seekTo(0)
            simpleExoPlayer?.play()
            exoPlay?.visibility = View.GONE
            exoPause?.visibility = View.VISIBLE
        }
        exoPause?.setOnClickListener{
            simpleExoPlayer?.pause()
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
                simpleExoPlayer?.playbackParameters = PlaybackParameters(0.5f)
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
                simpleExoPlayer?.playbackParameters = PlaybackParameters(0.75f)
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
                simpleExoPlayer?.playbackParameters = PlaybackParameters(1.0f)
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

    private fun updateFrameData(videoDuration: Long, totalFrames: Int, holder: AnalysisViewHolder) {
        val currentPosition = simpleExoPlayer?.currentPosition ?: 0L

        // 현재 재생 시간에 해당하는 프레임 인덱스 계산
        val frameIndex = ((currentPosition.toFloat() / videoDuration) * totalFrames).toInt()
        val coordinates = extractVideoCoordinates(dynamicJa)
        // 실제 mp4의 비디오 크기를 가져온다
        val (videoWidth, videoHeight) = getVideoDimensions(fragment.requireContext(), videoUrl.toUri())
//        Log.v("PlayerView,비디오 크기", "overlay: (${binding.ovMA.width}, ${binding.ovMA.height}), videoWidth,Height: (${videoWidth}, ${videoHeight}) width: ${binding.pvMA.width}, height: ${binding.pvMA.height}")
        val isPortrait = videoHeight > videoWidth
        if (frameIndex in 0 until totalFrames) {
            // 해당 인덱스의 데이터를 JSON에서 추출하여 변환
            val poseLandmarkResult = fromCoordinates(coordinates[frameIndex])
            // 변환된 데이터를 화면에 그리기
            fragment.requireActivity().runOnUiThread {
                holder.ovAI.scaleX = -1f
                holder.ovAI.setResults(
                    poseLandmarkResult,
                    videoWidth,
                    videoHeight,
                    OverlayView.RunningMode.VIDEO
                )
                holder.ovAI.invalidate()
            }
        }
    }
}