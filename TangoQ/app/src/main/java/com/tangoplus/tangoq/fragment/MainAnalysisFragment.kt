package com.tangoplus.tangoq.fragment

import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.DataDynamicRVAdapter
import com.tangoplus.tangoq.adapter.MainPartAnalysisRVAdapter
import com.tangoplus.tangoq.databinding.FragmentMainAnalysisBinding
import com.tangoplus.tangoq.dialog.bottomsheet.SequenceBSDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.function.MeasurementManager.createSummary
import com.tangoplus.tangoq.function.MeasurementManager.extractVideoCoordinates
import com.tangoplus.tangoq.function.MeasurementManager.getAnalysisUnits
import com.tangoplus.tangoq.function.MeasurementManager.getVideoDimensions
import com.tangoplus.tangoq.function.MeasurementManager.judgeFrontCameraByDynamic
import com.tangoplus.tangoq.function.MeasurementManager.matchedUris
import com.tangoplus.tangoq.function.MeasurementManager.setImage
import com.tangoplus.tangoq.vision.MathHelpers.isTablet
import com.tangoplus.tangoq.vision.OverlayView
import com.tangoplus.tangoq.vision.PoseLandmarkResult.Companion.fromCoordinates
import com.tangoplus.tangoq.viewmodel.AnalysisViewModel
import com.tangoplus.tangoq.viewmodel.PlayViewModel
import com.tangoplus.tangoq.vo.AnalysisUnitVO
import com.tangoplus.tangoq.vo.AnalysisVO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class MainAnalysisFragment : Fragment() {
    lateinit var binding : FragmentMainAnalysisBinding
    private val mvm : MeasureViewModel by activityViewModels()
    private val avm : AnalysisViewModel by activityViewModels()
    private val pvm : PlayViewModel by activityViewModels()
    private lateinit var mr : JSONArray

    private var updateUI = false
    // 영상재생
    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var videoUrl = ""
    private lateinit var jsonArray: JSONArray

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
    private lateinit var adapterAnalysises : List<AnalysisVO>
    private var updatedRv  = false
    companion object {
        private const val ARG_PART = "painParts"
        fun newInstance(painPart: String): MainAnalysisFragment {
            val fragment = MainAnalysisFragment()
            val args = Bundle()
            args.putString(ARG_PART, painPart)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainAnalysisBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ------# 데이터 필터링을 위한 사전 세팅 #------
        val part = arguments?.getString(ARG_PART) ?: ""
        avm.currentPart.value = part
        mr = mvm.selectedMeasure?.measureResult ?: JSONArray()
        avm.mdMeasureResult = JSONArray()


        // viewModel에 들어가있던 동적 자세 기록들 초기화

        pvm.videoUrl = null
        pvm.setPlaybackPosition(0)

        simpleExoPlayer?.let { pvm.savePlayerState(it, "") }

        avm.currentPart.observe(viewLifecycleOwner) { currentPart ->
            val painPart = avm.currentParts?.find { it == currentPart }
            val seqs = matchedUris[painPart]
            val groupedAnalyses = mutableMapOf<Int, MutableList<MutableList<AnalysisUnitVO>>>()

            seqs?.forEach { seq ->
                val analyses = getAnalysisUnits(requireContext(), painPart.toString(), seq, mr)
                val indexx = when (seq) {
                    0, 2 -> 0
                    3, 4 -> 1
                    5, 6 -> 2
                    1 -> 3
                    else -> 0
                }
                if (!groupedAnalyses.containsKey(indexx)) {
                    groupedAnalyses[indexx] = mutableListOf()
                }
                groupedAnalyses[indexx]?.add(analyses)

            }

            adapterAnalysises = groupedAnalyses.map { (indexx, analysesList) ->
                AnalysisVO(
                    indexx = indexx,
                    labels = analysesList.flatten().toMutableList()
                )
            }.sortedBy { it.indexx }

            binding.tvMATitle.text = "$painPart"
//            val myApplication = requireActivity().application as MyApplication
//            myApplication.setBiometricSuccess()

            // 초기 상태
            avm.currentIndex = 0
            // 버튼 클릭리스너와 사진 동영상 세팅
            updateButtonState()
            setMedia()
        }
        binding.tvMATitle.setOnSingleClickListener {
            val dialog = SequenceBSDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "SequenceBSDialogFragment")
        }
        binding.ivMAPartBS.setOnSingleClickListener {
            val dialog = SequenceBSDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "SequenceBSDialogFragment")
        }
    }
    // 버튼 UI
    private fun updateButtonState() {
        val buttons = listOf(binding.tvMA1, binding.tvMA2, binding.tvMA3, binding.tvMA4)
        val allIndexxes = listOf(0, 1, 2, 3)
        val missingIndexxes = allIndexxes.filterNot { index ->
            adapterAnalysises.any { it.indexx == index }
        }

        buttons.forEachIndexed { index, button ->
            val isActiveButton = !missingIndexxes.contains(index)

            button.isEnabled = isActiveButton
            button.backgroundTintList = when {
                avm.currentIndex == index -> ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
                isActiveButton -> ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor200))
                else -> ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor100))
            }

            if (isActiveButton) {
                button.setOnSingleClickListener {
                    avm.currentIndex = index
                    updateButtonState()  // 버튼 상태를 다시 업데이트
                    setMedia()
                }
            } else {
                button.setOnClickListener(null)
            }
        }
    }


    private fun setMedia() {
        if (avm.currentIndex != 3) {
            // 이미지
            if (simpleExoPlayer != null) {
                // clMA의 크기 조절
                val params = binding.clMA.layoutParams
                params.width = DisplayMetrics().widthPixels
                params.height = binding.ssivMA1.height
                binding.clMA.layoutParams = params
            }
            releasePlayer()
            binding.cvExoLeft.visibility = View.GONE
            binding.cvExoRight.visibility = View.GONE
            binding.flMA.visibility = View.GONE
            binding.ovMA.visibility = View.GONE
            binding.tvMAPart1.visibility = View.VISIBLE
            binding.tvMAPart2.visibility = View.VISIBLE
            binding.ssivMA1.visibility = View.VISIBLE
            binding.ssivMA2.visibility = View.VISIBLE
            // mainPartAnalysis 연결
            val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
//            Log.v("인덱스이외값전부", "${avm.currentIndex}, ${adapterAnalysises.find { it.indexx == avm.currentIndex }}")
            val adapter = MainPartAnalysisRVAdapter(this@MainAnalysisFragment, adapterAnalysises.find { it.indexx == avm.currentIndex }?.labels) // avm.currentIndex가 2인데 adapterAnalysises에는 0, 5밖에없어서 indexOutOfBoundException이 나옴.
            binding.rvMA.layoutManager = layoutManager
            binding.rvMA.adapter = adapter
            if (!updateUI) updateUI = false
        } else {
            // 영상
            binding.ssivMA1.visibility = View.GONE
            binding.ssivMA2.visibility = View.GONE
            binding.tvMAPart1.visibility = View.GONE
            binding.tvMAPart2.visibility = View.GONE
            binding.cvExoLeft.visibility = View.VISIBLE
            binding.cvExoRight.visibility = View.VISIBLE
            binding.flMA.visibility = View.VISIBLE
            binding.ovMA.visibility = View.VISIBLE
            setPlayer()
            // 0, 1, 2 (이미지 일 때)
            val params = binding.clMA.layoutParams
            params.width = binding.flMA.width
            params.height = binding.flMA.height
            binding.clMA.layoutParams = params
        }

        binding.tvMASummary.text =
            if (avm.currentIndex != 3) {
                createSummary(avm.currentPart.value, avm.currentIndex,
                    adapterAnalysises.find { it.indexx == avm.currentIndex }?.labels)
            } else {
                "스쿼트 1회 동작에서 좌우 부위의 궤적을 비교합니다.\n하단에 그려진 궤적이 대칭을 이룰 수록 정상범위입니다.\n\n이동 안정성의 불균형이 생겼을 때 손은 회전근개 주변 근육, 골반은 전반적인 하지, 무릎은 허벅지와 발목과 발의 정렬을 교정해야 합니다."
            }
        if (binding.tvMASummary.text.contains("부위가 정상 범위 내에 있습니다.") || avm.currentIndex == 3) {
            binding.tvMASummary.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.secondContainerColor))
            binding.tvMASummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.thirdColor))
            binding.ivMAIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.thirdColor))
        } else {
            binding.tvMASummary.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.deleteContainerColor))
            binding.tvMASummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.deleteColor))
            binding.ivMAIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.deleteColor))
        }
        lifecycleScope.launch(Dispatchers.IO) {
            when (avm.currentIndex) {
                0 -> {
                    setImage(this@MainAnalysisFragment, mvm.selectedMeasure, 0, binding.ssivMA1, "mainPart")
                    setImage(this@MainAnalysisFragment, mvm.selectedMeasure, 2, binding.ssivMA2, "mainPart")
                    withContext(Dispatchers.Main) {
                        binding.tvMAPart1.text = "정면 측정"
                        binding.tvMAPart2.text = "팔꿉 측정"
                        binding.clMA.requestLayout()
                    }
                }
                1 -> {

                    setImage(this@MainAnalysisFragment, mvm.selectedMeasure, 3, binding.ssivMA1, "mainPart")
                    setImage(this@MainAnalysisFragment, mvm.selectedMeasure, 4, binding.ssivMA2, "mainPart")
                    withContext(Dispatchers.Main) {
                        binding.tvMAPart1.text = "좌측 측정"
                        binding.tvMAPart2.text = "우측 측정"
                        binding.tvMAPart1.requestLayout()
                        binding.tvMAPart2.requestLayout()
                    }
                }
                2 -> {
                    setImage(this@MainAnalysisFragment, mvm.selectedMeasure, 5, binding.ssivMA1, "mainPart")
                    setImage(this@MainAnalysisFragment, mvm.selectedMeasure, 6, binding.ssivMA2, "mainPart")
                    withContext(Dispatchers.Main) {
                        binding.tvMAPart1.text = "후면 측정"
                        binding.tvMAPart2.text = "앉은 후면"
                        binding.tvMAPart1.requestLayout()
                        binding.tvMAPart2.requestLayout()
                    }
                }
                3 -> {

                    dynamicJa = mvm.selectedMeasure?.measureResult?.getJSONArray(1) ?: JSONArray()
                    withContext(Dispatchers.Main) {
                        pvm.setPlaybackPosition(0L)
                        pvm.setWindowIndex(0)

                        exoPlay = view?.findViewById(R.id.btnPlay)
                        exoPause = view?.findViewById(R.id.btnPause)
                        llSpeed = view?.findViewById(R.id.llSpeed)
                        btnSpeed = view?.findViewById(R.id.btnSpeed)

                        exo05 = view?.findViewById(R.id.btn05)
                        exo075 = view?.findViewById(R.id.btn075)
                        exo10 = view?.findViewById(R.id.btn10)

                        setClickListener()
                    }
                }
            }
        }
    }
    private fun releasePlayer() {
        simpleExoPlayer?.release()
        simpleExoPlayer = null

    }
    //---------------------------------------! VideoOverlay !---------------------------------------
    private fun setPlayer() {
        lifecycleScope.launch {
            jsonArray = mvm.selectedMeasure?.measureResult?.getJSONArray(1) ?: JSONArray()
            initPlayer()

            simpleExoPlayer?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    if (playbackState == Player.STATE_READY) {

                        val videoDuration = simpleExoPlayer?.duration ?: 0L
                        lifecycleScope.launch {
                            while (simpleExoPlayer?.isPlaying == true) {
                                updateVideoUI()
                                updateFrameData(videoDuration, jsonArray.length())
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

        simpleExoPlayer = SimpleExoPlayer.Builder(requireContext()).build()
        binding.pvMA.player = simpleExoPlayer
        binding.pvMA.controllerShowTimeoutMs = 1100
        lifecycleScope.launch {
            // 저장된 URL이 있다면 사용, 없다면 새로운 URL 가져오기
            videoUrl = pvm.videoUrl ?: mvm.selectedMeasure?.fileUris?.get(1).toString()
            pvm.videoUrl = videoUrl  // URL 저장

            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
            val mediaSource = ProgressiveMediaSource.Factory(DefaultDataSourceFactory(requireContext()))
                .createMediaSource(mediaItem)

            mediaSource.let {
                simpleExoPlayer?.prepare(it)
                // 저장된 위치로 정확하게 이동
                simpleExoPlayer?.seekTo(pvm.getPlaybackPosition())
                simpleExoPlayer?.playWhenReady = pvm.getPlayWhenReady()
            }
        }
        binding.pvMA.findViewById<ImageButton>(R.id.exo_replay_5).visibility = View.GONE
        binding.pvMA.findViewById<ImageButton>(R.id.exo_exit).visibility = View.GONE
        binding.pvMA.findViewById<ImageButton>(R.id.exo_forward_5).visibility = View.GONE
    }

    private fun setClickListener() {
        if (avm.currentIndex == 3) {
            exoPause?.visibility = View.VISIBLE
        }
        exoPlay?.visibility = View.GONE
        exoPlay?.setOnClickListener {
            simpleExoPlayer?.seekTo(pvm.getPlaybackPosition())
            simpleExoPlayer?.play()
            exoPlay?.visibility = View.GONE
            exoPause?.visibility = View.VISIBLE
        }
        exoPause?.setOnClickListener{
            simpleExoPlayer?.let { it1 -> pvm.savePlayerState(it1) }
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
    private fun updateFrameData(videoDuration: Long, totalFrames: Int) {
        val currentPosition = simpleExoPlayer?.currentPosition ?: 0L

        val frameIndex = ((currentPosition.toFloat() / videoDuration) * totalFrames).toInt()
        val coordinates = extractVideoCoordinates(jsonArray)

        val (videoWidth, videoHeight) = getVideoDimensions(requireContext(), videoUrl.toUri())
        if (frameIndex in 0 until totalFrames) {

            val poseLandmarkResult = fromCoordinates(coordinates[frameIndex])
            requireActivity().runOnUiThread {
                binding.ovMA.scaleX = -1f
                binding.ovMA.setResults(
                    poseLandmarkResult,
                    videoWidth,
                    videoHeight,
                    OverlayView.RunningMode.VIDEO
                )
                binding.ovMA.invalidate()
            }
        }
    }

    private fun updateVideoUI() {
        binding.ssivMA1.visibility = View.GONE
        binding.ssivMA2.visibility = View.GONE
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
        val params = binding.clMA.layoutParams
        params.width = (screenWidth  * resizingValue).toInt()
        params.height = (adjustedHeight * resizingValue).toInt()
        binding.clMA.layoutParams = params

        // 비디오 사이즈 6개넣어서 그대로 씀.
        if (!updatedRv) {
            val connections = listOf(
                15, 16, 23, 24, 25, 26 // 좌측 골반의 pose번호를 가져옴
            )

            val coordinates = extractVideoCoordinates(dynamicJa)
            val filteredCoordinates = mutableListOf<List<Pair<Float, Float>>>()
            for (connection in connections) {
                val filteredCoordinate = mutableListOf<Pair<Float, Float>>() // 부위 하나당 몇 십 프레임의 x,y 좌표임
                for (element in coordinates) {
                    filteredCoordinate.add(element[connection])
                }
                filteredCoordinates.add(filteredCoordinate)
            }
            setVideoAdapter(filteredCoordinates)
        }
    }

    private fun setVideoAdapter(data: List<List<Pair<Float, Float>>>) {
        val isFrontCamera = judgeFrontCameraByDynamic(data)
        Log.v("전면카메라인가요", "$isFrontCamera")
        val linearLayoutManager1 = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val dynamicAdapter = DataDynamicRVAdapter(data, avm.dynamicTitles, isFrontCamera)
        binding.rvMA.layoutManager = linearLayoutManager1
        binding.rvMA.adapter = dynamicAdapter

        // 어댑터가 한번만 선언되게끔 하기
        updatedRv = true
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
        simpleExoPlayer?.playWhenReady = true
    }
}