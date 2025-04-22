package com.tangoplus.tangoq.dialog

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.arthenica.ffmpegkit.FFmpegKit
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.TrendRVAdapter
import com.tangoplus.tangoq.viewmodel.AnalysisViewModel
import com.tangoplus.tangoq.vo.MeasureVO
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureTrendDialogBinding
import com.tangoplus.tangoq.function.MeasurementManager.getAnalysisUnits
import com.tangoplus.tangoq.function.MeasurementManager.setImage
import com.tangoplus.tangoq.db.Singleton_t_measure
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.function.MeasurementManager.extractVideoCoordinates
import com.tangoplus.tangoq.function.MeasurementManager.getVideoDimensions
import com.tangoplus.tangoq.function.MeasurementManager.matchedIndexs
import com.tangoplus.tangoq.function.MeasurementManager.matchedUris
import com.tangoplus.tangoq.function.SaveSingletonManager
import com.tangoplus.tangoq.vision.MathHelpers.isTablet
import com.tangoplus.tangoq.vision.OverlayView
import com.tangoplus.tangoq.vision.PoseLandmarkResult
import com.tangoplus.tangoq.vision.PoseLandmarkResult.Companion.fromCoordinates
import com.tangoplus.tangoq.viewmodel.PlayViewModel
import com.tangoplus.tangoq.vo.AnalysisUnitVO
import com.tangoplus.tangoq.vo.DateDisplay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import kotlin.math.max
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.doOnLayout
import androidx.media3.common.util.UnstableApi

class MeasureTrendDialogFragment : DialogFragment() {
    lateinit var binding : FragmentMeasureTrendDialogBinding
    private val avm : AnalysisViewModel by activityViewModels()
    private val mvm : MeasureViewModel by activityViewModels()
    private val pvm : PlayViewModel by activityViewModels()
    private var isLoadingShown = false
    private lateinit var measures : MutableList<MeasureVO>
    private lateinit var singletonMeasure : Singleton_t_measure
    private lateinit var ssm : SaveSingletonManager
    private lateinit var measureResult : JSONArray

    // 영상재생
    private var simpleExoPlayer1: SimpleExoPlayer? = null
    private var simpleExoPlayer2: SimpleExoPlayer? = null
    private lateinit var leftJa: JSONArray
    private lateinit var rightJa: JSONArray

    private var exoPlay1: ImageButton? = null
    private var exoPause1: ImageButton? = null
    private var exoReplay1: ImageButton? = null
    private var exoForward1: ImageButton? = null
    private var llSpeed1: LinearLayout? = null
    private var btnSpeed1: ImageButton? = null
    private var exoPosition1 : TextView? = null
    private var exo051: ImageButton? = null
    private var exo0751: ImageButton? = null
    private var exo101: ImageButton? = null
    private var exoExit1 : ImageButton? = null

    private var exoPlay2: ImageButton? = null
    private var exoPause2: ImageButton? = null
    private var exoReplay2: ImageButton? = null
    private var exoForward2: ImageButton? = null
    private var llSpeed2: LinearLayout? = null
    private var btnSpeed2: ImageButton? = null
    private var exoPosition2 : TextView? = null
    private var exo052: ImageButton? = null
    private var exo0752: ImageButton? = null
    private var exo102: ImageButton? = null
    private var exoExit2 : ImageButton? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_DialogFragment)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMeasureTrendDialogBinding.inflate(inflater)
        return binding.root
    }

    @UnstableApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // api35이상 화면 크기 조절
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 상태 표시줄 높이만큼 상단 패딩 적용
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        measures = Singleton_t_measure.getInstance(requireContext()).measures ?: mutableListOf()
        ssm = SaveSingletonManager(requireContext(), requireActivity())
        singletonMeasure = Singleton_t_measure.getInstance(requireContext())

        try {
            if (measures.isNotEmpty()) {
//                showBalloon()
                // 모든 이전 선택들 초기화 하기
                avm.leftMeasurement.value = null
                avm.leftAnalysises = null
                avm.rightMeasurement.value = measures[0]
                avm.currentIndex = 0
                measureResult = mvm.selectedMeasure?.measureResult ?: JSONArray()
                avm.resetMeasureDates()
//                Log.v("초기measureDates들", "lef t: ${avm.leftMeasureDate.value}, right: ${avm.rightMeasureDate.value}")

                binding.ibtnMTDBack.setOnSingleClickListener { dismiss() }
                pvm.setLeftPlaybackPosition(0)
                pvm.setRightPlaybackPosition(0)
                // ------# 비교할 모든 analysisVO 넣기 #------
                avm.rightAnalysises = transformAnalysisUnit(avm.rightMeasurement.value?.measureResult ?: JSONArray())

                // -------# 측정 날짜 고르는 ACTV left #-------
                avm.measureDisplayDates = avm.createDateDisplayList(measures)

                val leftAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, avm.measureDisplayDates.filterIndexed { index, dateDisplay -> index != 0 })
                binding.actvMTDLeft.setAdapter(leftAdapter)

                // 왼쪽 actv 눌렀을 때
                binding.actvMTDLeft.setOnItemClickListener { _, _, position, _ ->
                    // 날짜 선택
                    val selectedDate = binding.actvMTDLeft.adapter.getItem(position) as DateDisplay

                    avm.leftMeasureDate.value = avm.measureDisplayDates.find { it.fullDateTime == selectedDate.fullDateTime }
                    binding.actvMTDLeft.setText(avm.leftMeasureDate.value?.displayDate, false)
                    Log.v("selectedDateLeft", "${avm.leftMeasureDate.value?.fullDateTime}, ${avm.measureDisplayDates.map { it.fullDateTime }}")
                    lifecycleScope.launch(Dispatchers.IO) {
                        // 측정파일 없으면 다운로드
                        setMeasureFiles(avm.leftMeasureDate.value?.fullDateTime, false)
                        withContext(Dispatchers.Main) {
                            avm.leftMeasurement.value = Singleton_t_measure.getInstance(requireContext()).measures?.find { it.regDate == avm.leftMeasureDate.value?.fullDateTime }
                            avm.leftAnalysises = transformAnalysisUnit(avm.leftMeasurement.value?.measureResult ?: JSONArray())
                            val transIndex = if (avm.currentIndex == 6) {
                                1
                            } else if (avm.currentIndex > 0){
                                avm.currentIndex + 1
                            } else {
                                0
                            }
                            // 다운로드한 jpg 파일 drawing
                            if (avm.currentIndex != 6) {
                                setImage(this@MeasureTrendDialogFragment, avm.leftMeasurement.value, transIndex, binding.ssivMTDLeft, "trend")
                            } else {
                                setVideoUI(true, false)
                                setClickListener(false)
                                setPlayer(false)
                            }
                            setAdapter(avm.leftAnalysises, avm.rightAnalysises, transIndex)
                        }
                    }
                    binding.tvMTDAlert.visibility = View.GONE
                }

                // ------# 측정 고르는 ACTV right #------
                // 오른쪽 actv adapter 연결
                val rightAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, avm.measureDisplayDates)
                binding.actvMTDRight.setAdapter(rightAdapter)
                binding.actvMTDRight.setOnItemClickListener { _, _, position, _ ->
                    setACTVClickListener(position)
                }
                binding.actvMTDRight.setText(binding.actvMTDRight.adapter.getItem(0).toString())

                // 어댑터 연결 후 초기 세팅
                setACTVClickListener(0)
                updateButtonState()
                avm.currentIndex = 0
                setAdapter(avm.leftAnalysises, avm.rightAnalysises)

                val combinedObserver = Observer<Any> {
                    val leftSelected = avm.leftMeasureDate.value
                    val rightSelected = avm.rightMeasureDate.value
                    if (leftSelected == null && rightSelected == null) return@Observer // null이면 바로 리턴
                    // 오른쪽은 왼쪽 선택값 제외, 왼쪽은 오른쪽 선택값 제외
                    val filteredLeftDates = avm.getFilteredDates(listOfNotNull(rightSelected))
                    val filteredRightDates = avm.getFilteredDates(listOfNotNull(leftSelected))

                    // Left adapter 갱신
                    binding.actvMTDLeft.setAdapter(
                        ArrayAdapter(requireContext(), R.layout.dropdown_item, filteredLeftDates)
                    )

                    // Right adapter 갱신
                    binding.actvMTDRight.setAdapter(
                        ArrayAdapter(requireContext(), R.layout.dropdown_item, filteredRightDates)
                    )
                }
                avm.leftMeasureDate.observe(viewLifecycleOwner, combinedObserver)
                avm.rightMeasureDate.observe(viewLifecycleOwner, combinedObserver)
            }
        } catch (e: NullPointerException) {
            Log.e("TrendError", "NullPointer: ${e.message}")
        } catch (e: IllegalStateException) {
            Log.e("TrendError", "IllegalState: ${e.message}")
        } catch (e: ClassNotFoundException) {
            Log.e("TrendError", "ClassNotFound: ${e.message}")
        }  catch (e: IllegalArgumentException) {
            Log.e("TrendError", "IllegalArgumentException: ${e.message}")
        } catch (e: Exception) {
            Log.e("TrendError", "Exception: ${e.message}")
        }

    }

    // 오른쪽 actv에 대한 listener
    @UnstableApi
    private fun setACTVClickListener(position: Int) {
        // 날짜 선택
        val selectedDate = binding.actvMTDRight.adapter.getItem(position) as DateDisplay
        avm.rightMeasureDate.value = avm.measureDisplayDates.find { it.fullDateTime == selectedDate.fullDateTime }
        binding.actvMTDRight.setText(avm.rightMeasureDate.value?.displayDate, false)

//        Log.v("selectedDateRight", "${avm.rightMeasureDate.value?.fullDateTime}, ${avm.measureDisplayDates.map { it.fullDateTime }}")
        CoroutineScope(Dispatchers.IO).launch {
            // 데이터 셋팅 확인
            setMeasureFiles(avm.rightMeasureDate.value?.fullDateTime, true)

            // 값 셋팅
            withContext(Dispatchers.Main) {
                avm.rightMeasurement.value = Singleton_t_measure.getInstance(requireContext()).measures?.find { it.regDate == avm.rightMeasureDate.value?.fullDateTime }
                avm.rightAnalysises = transformAnalysisUnit(avm.rightMeasurement.value?.measureResult ?: JSONArray())
                val transIndex = if (avm.currentIndex == 6) {
                    1
                } else if (avm.currentIndex > 0){
                    avm.currentIndex + 1
                } else {
                    0
                }
                if (avm.currentIndex != 6) {
                    setImage(this@MeasureTrendDialogFragment, avm.rightMeasurement.value, transIndex, binding.ssivMTDRight, "trend")
                } else {
                    setVideoUI(true, true)
                    setClickListener(true)
                    setPlayer(true)
                }
                setAdapter(avm.leftAnalysises, avm.rightAnalysises, transIndex)
            }
        }
    }

    // 버튼 UI
    @UnstableApi
    @OptIn(UnstableApi::class)
    private fun updateButtonState() {
        val buttons = listOf(binding.tvMTD1, binding.tvMTD2, binding.tvMTD3, binding.tvMTD4, binding.tvMTD5, binding.tvMTD6, binding.tvMTD7)

        buttons.forEachIndexed { index, button ->
            val isSelected = avm.currentIndex == index
            button.backgroundTintList = when {
                avm.currentIndex == index -> ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
                else -> ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor200))
            }
            button.isClickable = !isSelected // 선택된 버튼은 클릭 불가능하게 설정
            button.isEnabled = !isSelected  // 선택된 버튼은 비활성화

            button.setOnSingleClickListener {
                avm.currentIndex = index
                updateButtonState()  // 버튼 상태를 다시 업데이트

                lifecycleScope.launch(Dispatchers.IO) {
                    val transIndex = if (avm.currentIndex == 6) {
                        1
                    } else if (avm.currentIndex > 0){
                        avm.currentIndex + 1
                    } else {
                        0
                    }

                    // 왼쪽이 없을 때는 오른쪽만 갱신
                    withContext(Dispatchers.Main) {
                        Log.v("avm.leftMeasurement", "${avm.leftMeasurement.value}")
                        if (avm.leftMeasurement.value == null) {
                            // 이미지 0~6
                            if (avm.currentIndex != 6) {
                                setVideoUI(false, true)
                                setImage(this@MeasureTrendDialogFragment, avm.rightMeasurement.value, transIndex, binding.ssivMTDRight, "trend")
                                pvm.rightUpdateUI = false
                            } else {
                                simpleExoPlayer2?.stop()
                                simpleExoPlayer2?.release()
                                simpleExoPlayer2 = null
                                setVideoUI(true, true)
                                setClickListener(true)
                                setPlayer(true)
                            }

                        } else {
                            // 이미지 0~6
                            if (avm.currentIndex != 6) {
//                                avm.bothStartChecking.removeObservers(viewLifecycleOwner)
                                simpleExoPlayer1?.stop()
                                simpleExoPlayer1?.release()
                                simpleExoPlayer1 = null

                                simpleExoPlayer2?.stop()
                                simpleExoPlayer2?.release()
                                simpleExoPlayer2 = null

                                setVideoUI(isVideo = false, isRight = false)
                                setVideoUI(isVideo = false, isRight = true)
                                setImage(this@MeasureTrendDialogFragment, avm.leftMeasurement.value, transIndex, binding.ssivMTDLeft, "trend")
                                setImage(this@MeasureTrendDialogFragment, avm.rightMeasurement.value, transIndex, binding.ssivMTDRight, "trend")
                                pvm.rightUpdateUI = false
                                pvm.leftUpdateUI = false
                            } else  {
                                setVideoUI(true, false)
                                setClickListener(false)
                                setPlayer(false)

                                // 오른쪽 재생
                                setVideoUI(true, true)
                                setClickListener(true)
                                setPlayer(true)
                            }
                        }
                        setAdapter(avm.leftAnalysises, avm.rightAnalysises, transIndex)
                    }
                }
            }
        }
    }

    private fun setVideoUI(isVideo: Boolean, isRight: Boolean) {
        when (isVideo) {
            true -> {
                when (isRight) {
                    true -> {
                        // 비디오이고 오른쪽임
                        binding.ssivMTDRight.visibility = View.GONE
                        binding.clMTDRight.visibility = View.VISIBLE

                        exoPlay2 = binding.pvMTDRight.findViewById(R.id.btnPlay)
                        exoPause2 = binding.pvMTDRight.findViewById(R.id.btnPause)
                        exoReplay2 = binding.pvMTDRight.findViewById(R.id.exo_replay_5)
                        exoForward2 = binding.pvMTDRight.findViewById(R.id.exo_forward_5)
                        llSpeed2 = binding.pvMTDRight.findViewById(R.id.llSpeed)
                        btnSpeed2 = binding.pvMTDRight.findViewById(R.id.btnSpeed)
                        exo052 = binding.pvMTDRight.findViewById(R.id.btn05)
                        exo0752 = binding.pvMTDRight.findViewById(R.id.btn075)
                        exo102 = binding.pvMTDRight.findViewById(R.id.btn10)
                        exoExit2 = binding.pvMTDRight.findViewById(R.id.exo_exit)
                    }
                    false -> {
                        // 비디오이고 왼쪽
                        binding.ssivMTDLeft.visibility = View.GONE
                        binding.clMTDLeft.visibility = View.VISIBLE
                        exoPlay1 = binding.pvMTDLeft.findViewById(R.id.btnPlay)
                        exoPause1 = binding.pvMTDLeft.findViewById(R.id.btnPause)
                        exoReplay1 = binding.pvMTDLeft.findViewById(R.id.exo_replay_5)
                        exoForward1 = binding.pvMTDLeft.findViewById(R.id.exo_forward_5)
                        llSpeed1 = binding.pvMTDLeft.findViewById(R.id.llSpeed)
                        btnSpeed1 = binding.pvMTDLeft.findViewById(R.id.btnSpeed)
                        exo051 = binding.pvMTDLeft.findViewById(R.id.btn05)
                        exo0751 = binding.pvMTDLeft.findViewById(R.id.btn075)
                        exo101 = binding.pvMTDLeft.findViewById(R.id.btn10)
                        exoExit1 = binding.pvMTDLeft.findViewById(R.id.exo_exit)
                    }
                }
            }
            false -> {
                when (isRight) {
                    true -> {
                        // 사진이고 오른쪽
                        binding.ssivMTDRight.visibility = View.VISIBLE
                        binding.clMTDRight.visibility = View.GONE
                    }
                    false -> {
                        // 사진이고 왼쪽
                        binding.ssivMTDLeft.visibility = View.VISIBLE
                        binding.clMTDLeft.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun setAdapter(
        analyzesLeft: MutableList<MutableList<AnalysisUnitVO>>?,
        analyzesRight: MutableList<MutableList<AnalysisUnitVO>>?,
        currentSeq: Int = 0,
    ) {

        val filteredLeftAnalysises = if (currentSeq == 1) {
            // 동적 측정일 때
            analyzesLeft?.filterIndexed{ index, _ -> index in listOf(1, 2, 7, 8, 9, 10 ) }?.toMutableList()
        } else {
            // 정적 측정일 떄
            analyzesLeft?.mapNotNull { analysisList ->
                if (analysisList.any { it.seq == currentSeq }) {
                    analysisList
                } else {
                    null
                }
            }?.toMutableList()
        }
        val filteredRightAnalysises = if (currentSeq == 1) {
            // 동적 측정일 때
            analyzesRight?.filterIndexed{ index, _ -> index in listOf(1, 2, 7, 8, 9, 10) }?.toMutableList()
        } else {

            // 정적 측정일 떄
            analyzesRight?.mapNotNull { analysisList ->
                if (analysisList.any { it.seq == currentSeq }) {
                    analysisList
                } else {
                    null
                }
            }?.toMutableList()

        }
        Log.v("오른쪽 필터링", "$filteredRightAnalysises")

        val filteredIndexes = if (currentSeq == 1 ) {
            listOf(1, 2, 7, 8, 9, 10)
        } else {
            analyzesRight?.mapIndexedNotNull { index, analysisList ->
                if (analysisList.any { it.seq == currentSeq }) index else null
            }
        }
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        val adapter = TrendRVAdapter(this@MeasureTrendDialogFragment, filteredLeftAnalysises, filteredRightAnalysises, filteredIndexes?.map { matchedIndexs[it] }, avm.currentIndex)
        binding.rvMTD.layoutManager = layoutManager
        binding.rvMTD.adapter = adapter
    }


    // 13 > 3개의 AnalysisVO임. == 관절 1개의 3개가 들어간 raw 하나를 말하는 것. Trend에서 사용.
    private fun transformAnalysisUnit(ja : JSONArray) : MutableList<MutableList<AnalysisUnitVO>> {
        val result = mutableListOf<MutableList<AnalysisUnitVO>>()

        matchedUris.forEach { (part, seqList) -> // 13개 관절 각각에 대해
            val analysisUnitsForPart = mutableListOf<AnalysisUnitVO>()

            // 해당 부위의 모든 시퀀스에서 분석 유닛을 가져와서 하나의 리스트로 합치기
            seqList.forEach { seq ->
                val unitsForSeq = getAnalysisUnits(requireContext(), part, seq, ja)
                analysisUnitsForPart.addAll(unitsForSeq)
            }

            // 각 부위별로 모은 분석 유닛들을 결과 리스트에 추가
            result.add(analysisUnitsForPart)
        }

        Log.v("모든분석유닛", "${result.size}, ${result.map { it.size }}, ${result.map { it.map { it.columnName } }}")
        return result
    }

    private suspend fun setMeasureFiles(inputRegDate: String?, isRight: Boolean) {

        val loadingDialog = LoadingDialogFragment.newInstance("측정파일")
        // ------# 로딩을 통해 파일 가져오기 #------
        withContext(Dispatchers.IO) {
            try {
                val currentMeasure = singletonMeasure.measures?.find { it.regDate == inputRegDate }
                val uriTuples = currentMeasure?.sn?.let { it1 -> ssm.get1MeasureUrls(it1) }
                if (uriTuples != null && currentMeasure.fileUris.isNullOrEmpty() && currentMeasure.measureResult == null) {
                    withContext(Dispatchers.Main) {
                        loadingDialog.show(requireActivity().supportFragmentManager, "LoadingDialogFragment")
                        ssm.downloadFiles(uriTuples)
                        val editedMeasure = ssm.insertUrlToMeasureVO(uriTuples, currentMeasure)

                        // singleton의 인덱스 찾아서 ja와 값 넣기
                        val singletonIndex = singletonMeasure.measures?.indexOfLast { it.regDate == inputRegDate }
                        if (singletonIndex != null && singletonIndex >= 0) {
                            singletonMeasure.measures?.set(singletonIndex, editedMeasure)
                            if (isRight) {
                                avm.rightMeasurement.value = editedMeasure
                            } else {
                                avm.leftMeasurement.value = editedMeasure
                            }
                            withContext(Dispatchers.Main) {
                                loadingDialog.dismiss()
                            }
                        }
                    }

                } else {
                    return@withContext
                }
            } catch (e: IllegalStateException) {
                Log.e("trendError", "MeasureBSIllegalState: ${e.message}")
            } catch (e: IllegalArgumentException) {
                Log.e("trendError", "MeasureBSIllegalArgument: ${e.message}")
            } catch (e: NullPointerException) {
                Log.e("trendError", "MeasureBSNullPointer: ${e.message}")
            } catch (e: InterruptedException) {
                Log.e("trendError", "MeasureBSInterrupted: ${e.message}")
            } catch (e: IndexOutOfBoundsException) {
                Log.e("trendError", "MeasureBSIndexOutOfBounds: ${e.message}")
            } catch (e: Exception) {
                Log.e("trendError", "MeasureBS: ${e.message}")
            }
        }
//        Log.w("selectedMeasureDate", "selectedMeasure: ${mvm.selectedMeasureDate.value}, selectMeasure: ${mvm.selectMeasureDate.value}")
//        Log.w("selectedMeasure", "${mvm.selectedMeasure}")
        if (loadingDialog.dialog?.isShowing == true) {
            loadingDialog.dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    private fun setPlayer(isRight: Boolean) {
        when (isRight) {
            true -> {
                lifecycleScope.launch {
                    rightJa = avm.rightMeasurement.value?.measureResult?.getJSONArray(1) ?: JSONArray()
                    initPlayer(true)
                    simpleExoPlayer2?.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            super.onPlaybackStateChanged(playbackState)
                            if (playbackState == Player.STATE_READY) {

                                val videoDuration = simpleExoPlayer2?.duration ?: 0L
                                lifecycleScope.launch {
                                    while (simpleExoPlayer2?.isPlaying == true) {
                                        if (!pvm.rightUpdateUI) updateVideoUI(true)
//                                        updateVideoUI(isRight)
//                                        Log.e("비디오레이아웃", "right: ${pvm.leftUpdateUI}")
                                        updateFrameData(true, videoDuration, rightJa.length())
                                        delay(24)
                                        pvm.rightUpdateUI = true
                                    }
                                }
                            } else if (playbackState == Player.STATE_ENDED) {
                                exoPlay2?.visibility = View.VISIBLE
                                exoPlay2?.bringToFront()
                                exoPause2?.visibility = View.GONE
                                pvm.setRightPlaybackPosition(0)
                            }
                        }
                    })

                }
            }
            false -> {
                lifecycleScope.launch {
//                    Log.v("동적측정json", "${avm.leftMeasurement.value?.measureResult?.getJSONArray(1)}")
                    leftJa = avm.leftMeasurement.value?.measureResult?.getJSONArray(1) ?: JSONArray()
//                    Log.v("leftJa길이", "${leftJa.length()}")
                    initPlayer(false)

                    simpleExoPlayer1?.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            super.onPlaybackStateChanged(playbackState)
                            if (playbackState == Player.STATE_READY) {

                                val videoDuration = simpleExoPlayer1?.duration ?: 0L
                                lifecycleScope.launch {
                                    while (simpleExoPlayer1?.isPlaying == true) {
//                                        Log.e("비디오레이아웃", "left: ${pvm.leftUpdateUI}")
                                        if (!pvm.leftUpdateUI) updateVideoUI(false)
                                        updateFrameData(false, videoDuration, leftJa.length())
                                        delay(24)
                                        pvm.leftUpdateUI = true
                                    }
                                }
                            } else if (playbackState == Player.STATE_ENDED) {
                                pvm.setLeftPlaybackPosition(0)
                                exoPlay1?.visibility = View.VISIBLE
                                exoPlay1?.bringToFront()
                                exoPause1?.visibility = View.GONE
                            }
                        }
                    })
                }
            }
        }
    }


    private fun initPlayer(isRight: Boolean) {
// 동영상 처리 로딩
        val loadingDialog = LoadingDialogFragment.newInstance("동영상")
        when (isRight) {
            true -> {
                Log.e("오른쪽플레이어init", "initPlayer isRight: true")

                setClickListener(true)
                simpleExoPlayer2 = SimpleExoPlayer.Builder(requireContext()).build()
                binding.pvMTDRight.player = simpleExoPlayer2
                binding.pvMTDRight.controllerShowTimeoutMs = 1100

                avm.trendRightUri = avm.rightMeasurement.value?.fileUris?.get(1).toString()
                val (videoWidth, videoHeight) = getVideoDimensions(requireContext(), avm.trendRightUri?.toUri() ?: "".toUri())
                val aspectRatio = videoHeight.toFloat() / videoWidth.toFloat()
                if (aspectRatio < 1) {
                    val inputPath = avm.trendRightUri.toString() // 기존 파 일 경로
                    val tempOutputPath = "${context?.cacheDir}/right_temp_video.mp4" // 임시 파일

                    // 이미 처리한 영상 파일이 남아있을 때
                    if (avm.rightEditedFile != null && avm.rightEditedFile!!.exists()) {
                        Log.v("rightFileExisted", "${avm.rightEditedFile}")
                        setPlayerByCroppedVideo(true, videoWidth.toFloat(), videoHeight.toFloat())
                    }
                    // 로딩창 키기
                    if (!isLoadingShown) {
                        loadingDialog.show(requireActivity().supportFragmentManager, "LoadingDialogFragment")
                        isLoadingShown = true
                    }

                    val targetWidth = (videoHeight * 9) / 16
                    val cropX = max(0, (videoWidth - targetWidth) / 2)
                    val command = "-i $inputPath -vf crop=$targetWidth:$videoHeight:$cropX:0 -c:v libx264 -preset fast -crf 23 -c:a aac -strict experimental -y $tempOutputPath"

                    lifecycleScope.launch {
                        FFmpegKit.executeAsync(command) { session ->
                            if (session.returnCode.isSuccess) {
                                Log.d("FFmpeg", "✅ 9:16 크롭 성공!")

                                avm.rightEditedFile = File(tempOutputPath)

                                // ⚠️ 변환된 파일이 정상적으로 생성되었는지 확인
                                if (avm.rightEditedFile != null && avm.rightEditedFile!!.exists() && avm.rightEditedFile!!.length() > 0) {
                                    requireActivity().runOnUiThread {
                                        setPlayerByCroppedVideo(true, videoWidth.toFloat(), videoHeight.toFloat())
                                        avm.onPlayer2Ready.value = true
                                    }
                                    Log.v("로딩창state", "right: ${loadingDialog.isAdded}, ${loadingDialog.isVisible}")
                                    if (loadingDialog.isAdded) {
                                        loadingDialog.dismiss()
                                        Log.v("다이얼로그켜져있는지", "${loadingDialog.isAdded}")
                                        simpleExoPlayer2?.pause()
                                    }
                                    isLoadingShown = false
                                } else {
                                    Log.e("FFmpeg", "❌ 변환된 파일이 존재하지 않음!")
                                }
                            } else {
                                Log.e("FFmpeg", "❌ 크롭 실패! ${session.returnCode}")
                            }
                        }
                    }
                } else {
                    lifecycleScope.launch {
//                    Log.v("VMTrendRight", "${avm.trendRightUri}")
                        val mediaItem = MediaItem.fromUri(Uri.parse(avm.trendRightUri))
                        val mediaSource = ProgressiveMediaSource.Factory(DefaultDataSourceFactory(requireContext()))
                            .createMediaSource(mediaItem)

                        mediaSource.let {
                            simpleExoPlayer2?.prepare(it)
                            // 저장된 위치로 정확하게 이동
                            simpleExoPlayer2?.seekTo(0)
                            avm.onPlayer2Ready.value = true
                            simpleExoPlayer2?.playWhenReady = true
                            if (loadingDialog.isAdded) {
                                Log.v("다이얼로그켜져있는지", "${loadingDialog.isAdded}")
                                simpleExoPlayer2?.pause()
                            }
                        }
                        Log.e("오른쪽notCrop", "${simpleExoPlayer2?.playWhenReady}")
                    }
                }
            }
            false -> {
                Log.e("왼쪽플레이어init", "initPlayer isRight: false")

                setClickListener(false)
                simpleExoPlayer1 = SimpleExoPlayer.Builder(requireContext()).build()
                binding.pvMTDLeft.player = simpleExoPlayer1
                binding.pvMTDLeft.controllerShowTimeoutMs = 1100

                avm.trendLeftUri = avm.leftMeasurement.value?.fileUris?.get(1).toString()
                val (videoWidth, videoHeight) = getVideoDimensions(requireContext(), avm.trendLeftUri?.toUri() ?: "".toUri())
                val aspectRatio = videoHeight.toFloat() / videoWidth.toFloat()
                if (aspectRatio < 1) {
                    val inputPath = avm.trendLeftUri.toString() // 기존 파일 경로
                    val tempOutputPath = "${context?.cacheDir}/left_temp_video.mp4" // 임시 파일

                    if (avm.leftEditedFile != null && avm.leftEditedFile!!.exists()) {
                        Log.v("rightFileExisted", "${avm.leftEditedFile}")
                        setPlayerByCroppedVideo(false, videoWidth.toFloat(), videoHeight.toFloat())
                    }
                    if (!isLoadingShown) {
                        loadingDialog.show(requireActivity().supportFragmentManager, "LoadingDialogFragment")
                        isLoadingShown = true
                    }
                    val targetWidth = (videoHeight * 9) / 16
                    val cropX = max(0, (videoWidth - targetWidth) / 2)
                    val command = "-i $inputPath -vf crop=$targetWidth:$videoHeight:$cropX:0 -c:v libx264 -preset fast -crf 23 -c:a aac -strict experimental -y $tempOutputPath"

                    lifecycleScope.launch {
                        FFmpegKit.executeAsync(command) { session ->
                            if (session.returnCode.isSuccess) {
                                Log.d("FFmpeg", "✅ 9:16 크롭 성공! output: $tempOutputPath")

                                avm.leftEditedFile = File(tempOutputPath)

                                // ⚠️ 변환된 파일이 정상적으로 생성되었는지 확인
                                if (avm.leftEditedFile != null && avm.leftEditedFile!!.exists() && avm.leftEditedFile!!.length() > 0) {
                                    Log.d("FFmpeg", "✅ 변환된 파일 존재 확인")
                                    requireActivity().runOnUiThread {
                                        // UI 스레드에서 실행 (ExoPlayer는 UI 스레드에서 실행해야 함)
                                        setPlayerByCroppedVideo(false, videoWidth.toFloat(), videoHeight.toFloat())
                                        avm.onPlayer1Ready.value = true

                                        // 로딩창 종료
                                        Log.v("로딩창state", "left: ${loadingDialog.isAdded}, ${loadingDialog.isVisible}")
                                        if (loadingDialog.isAdded) {
                                            Log.v("다이얼로그켜져있는지", "${loadingDialog.isAdded}")
                                            simpleExoPlayer1?.pause()
                                            loadingDialog.dismiss()
                                            isLoadingShown = false
                                        }

                                    }
                                } else {
                                    Log.e("FFmpeg", "❌ 변환된 파일이 존재하지 않음!")
                                }
                            } else {
                                Log.e("FFmpeg", "❌ 크롭 실패! ${session.failStackTrace}")
                            }
                        }
                    }
                } else {
                    lifecycleScope.launch {
                        val mediaItem = MediaItem.fromUri(Uri.parse(avm.trendLeftUri))
                        val mediaSource = ProgressiveMediaSource.Factory(DefaultDataSourceFactory(requireContext()))
                            .createMediaSource(mediaItem)
                        mediaSource.let {
                            simpleExoPlayer1?.prepare(it)
                            // 저장된 위치로 정확하게 이동
                            simpleExoPlayer1?.seekTo(0)
                            avm.onPlayer1Ready.value = true
                            simpleExoPlayer1?.playWhenReady = true
                            if (loadingDialog.isAdded) {
                                simpleExoPlayer1?.pause()
                            }
                        }
                        Log.e("왼쪽notCrop", "${simpleExoPlayer1?.playWhenReady}")
                    }
                }
            }
        }
    }
    private fun setClickListener(isRight: Boolean) {
        when (isRight) {
            true -> {
                exoForward2?.visibility = View.GONE
                exoReplay2?.visibility = View.GONE
                exoPlay2?.visibility = View.GONE
                exoExit2?.visibility = View.GONE
                llSpeed2?.visibility = View.VISIBLE
                exoPause2?.visibility = View.VISIBLE
                exoPlay2?.setOnClickListener {
                    simpleExoPlayer2?.seekTo(pvm.getRightPlaybackPosition())
                    simpleExoPlayer2?.play()
                    exoPlay2?.visibility = View.GONE
                    exoPause2?.visibility = View.VISIBLE
                }
                exoPause2?.setOnClickListener{
                    simpleExoPlayer2?.let { it1 -> pvm.setRightPlaybackPosition(it1.currentPosition) }
                    simpleExoPlayer2?.pause()
                    exoPlay2?.visibility = View.VISIBLE
                    exoPause2?.visibility = View.GONE
                }
                var isShownSpeed2 = false
                var forChanged2 = false
                btnSpeed2?.setOnClickListener{
                    if (!isShownSpeed2) {
                        exo052?.visibility = View.VISIBLE
                        exo0752?.visibility = View.VISIBLE
                        isShownSpeed2 = true
                    } else {
                        exo052?.visibility = View.GONE
                        exo0752?.visibility = View.GONE
                        isShownSpeed2 = false
                    }
                }

                exo052?.setOnClickListener {
                    if (forChanged2) {
                        exo052?.visibility = View.VISIBLE
                        exo0752?.visibility = View.VISIBLE
                        exo102?.visibility = View.VISIBLE
                        forChanged2 = false
                    } else {
                        simpleExoPlayer2?.playbackParameters = PlaybackParameters(0.5f)
                        exo052?.visibility = View.VISIBLE
                        exo0752?.visibility = View.GONE
                        exo102?.visibility = View.GONE
                        btnSpeed2?.visibility = View.GONE
                        isShownSpeed2 = false
                        forChanged2 = true
                    }

                }
                exo0752?.setOnClickListener {
                    if (forChanged2) {
                        exo052?.visibility = View.VISIBLE
                        exo0752?.visibility = View.VISIBLE
                        exo102?.visibility = View.VISIBLE
                        forChanged2 = false
                    } else {
                        simpleExoPlayer2?.playbackParameters = PlaybackParameters(0.75f)
                        exo052?.visibility = View.GONE
                        exo0752?.visibility = View.VISIBLE
                        exo102?.visibility = View.GONE
                        btnSpeed2?.visibility = View.GONE
                        isShownSpeed2 = false
                        forChanged2 = true
                    }
                }
                exo102?.setOnClickListener {
                    if (forChanged2) {
                        exo052?.visibility = View.VISIBLE
                        exo0752?.visibility = View.VISIBLE
                        exo102?.visibility = View.VISIBLE
                        forChanged2 = false
                    } else {
                        simpleExoPlayer2?.playbackParameters = PlaybackParameters(1.0f)
                        exo052?.visibility = View.GONE
                        exo0752?.visibility = View.GONE
                        exo102?.visibility = View.VISIBLE
                        btnSpeed2?.visibility = View.GONE
                        isShownSpeed2 = false
                        forChanged2 = true
                    }
                }

            }
            false -> {
                exoForward1?.visibility = View.GONE
                exoReplay1?.visibility = View.GONE
                exoPlay1?.visibility = View.GONE
                exoExit1?.visibility = View.GONE
                llSpeed1?.visibility = View.VISIBLE
                exoPause1?.visibility = View.VISIBLE
                exoPlay1?.setOnClickListener {
                    simpleExoPlayer1?.seekTo(pvm.getLeftPlaybackPosition())
                    simpleExoPlayer1?.play()
                    exoPlay1?.visibility = View.GONE
                    exoPause1?.visibility = View.VISIBLE

                }
                exoPause1?.setOnClickListener{
                    simpleExoPlayer1?.let { it1 -> pvm.setLeftPlaybackPosition(it1.currentPosition) }
                    simpleExoPlayer1?.pause()
                    exoPlay1?.visibility = View.VISIBLE
                    exoPause1?.visibility = View.GONE
                }
                var isShownSpeed1 = false
                var forChanged1 = false
                btnSpeed1?.setOnClickListener{
                    if (!isShownSpeed1) {
                        exo051?.visibility = View.VISIBLE
                        exo0751?.visibility = View.VISIBLE
                        isShownSpeed1 = true
                    } else {
                        exo051?.visibility = View.GONE
                        exo0751?.visibility = View.GONE
                        isShownSpeed1 = false
                    }
                }

                exo051?.setOnClickListener {
                    if (forChanged1) {
                        exo051?.visibility = View.VISIBLE
                        exo0751?.visibility = View.VISIBLE
                        exo101?.visibility = View.VISIBLE
                        forChanged1 = false
                    } else {
                        simpleExoPlayer1?.playbackParameters = PlaybackParameters(0.5f)
                        exo051?.visibility = View.VISIBLE
                        exo0751?.visibility = View.GONE
                        exo101?.visibility = View.GONE
                        btnSpeed1?.visibility = View.GONE
                        isShownSpeed1 = false
                        forChanged1 = true
                    }

                }
                exo0751?.setOnClickListener {
                    if (forChanged1) {
                        exo051?.visibility = View.VISIBLE
                        exo0751?.visibility = View.VISIBLE
                        exo101?.visibility = View.VISIBLE
                        forChanged1 = false
                    } else {
                        simpleExoPlayer1?.playbackParameters = PlaybackParameters(0.75f)
                        exo051?.visibility = View.GONE
                        exo0751?.visibility = View.VISIBLE
                        exo101?.visibility = View.GONE
                        btnSpeed1?.visibility = View.GONE
                        isShownSpeed1 = false
                        forChanged1 = true
                    }
                }
                exo101?.setOnClickListener {
                    if (forChanged1) {
                        exo051?.visibility = View.VISIBLE
                        exo0751?.visibility = View.VISIBLE
                        exo101?.visibility = View.VISIBLE
                        forChanged1 = false
                    } else {
                        simpleExoPlayer1?.playbackParameters = PlaybackParameters(1.0f)
                        exo051?.visibility = View.GONE
                        exo0751?.visibility = View.GONE
                        exo101?.visibility = View.VISIBLE
                        btnSpeed1?.visibility = View.GONE
                        isShownSpeed1 = false
                        forChanged1 = true
                    }
                }
            }
        }
    }
    private fun updateFrameData(isRight: Boolean, videoDuration: Long, totalFrames: Int) {
        when (isRight) {
            true -> {
                val currentPosition = simpleExoPlayer2?.currentPosition ?: 0L
                val frameIndex = ((currentPosition.toFloat() / videoDuration) * totalFrames).toInt()
                val coordinates = extractVideoCoordinates(rightJa)

                val (videoWidth, videoHeight) = getVideoDimensions(requireContext(), avm.trendRightUri?.toUri())
                val aspectRatio = videoHeight.toFloat() / videoWidth.toFloat()

                // 가로비율의 영상일 때
                if (aspectRatio < 1) {
                    val (editedWidth, editedHeight) = getVideoDimensions(requireContext(), Uri.fromFile(avm.rightEditedFile))
                    if (frameIndex in 0 until totalFrames) {
                        // overlay의 좌표를 crop된 영상에 맞게 수정하는 함수
                        val poseLandmarkResult = fromCoordinates(coordinates[frameIndex])
                        val transformedResult = transformCoordinates(
                            poseLandmarkResult,
                            videoWidth, videoHeight,  // 원본 크기
                            editedWidth, editedHeight  // 편집된 크기
                        )

                        requireActivity().runOnUiThread {
                            binding.ovMTDRight.scaleX = -1f
                            binding.ovMTDRight.setResults(
                                transformedResult, // if (isTablet(requireContext())) poseLandmarkResult else
                                videoWidth,
                                videoHeight,
                                OverlayView.RunningMode.VIDEO
                            )
                            binding.ovMTDRight.invalidate()
                        }
                    }
                } else {
                    if (frameIndex in 0 until totalFrames) {

                        val poseLandmarkResult = fromCoordinates(coordinates[frameIndex])
                        requireActivity().runOnUiThread {
                            binding.ovMTDRight.scaleX = -1f
                            binding.ovMTDRight.setResults(
                                poseLandmarkResult,
                                videoWidth,
                                videoHeight,
                                OverlayView.RunningMode.VIDEO
                            )
                            binding.ovMTDRight.invalidate()
                        }
                    }
                }
            }
            false -> {
                val currentPosition = simpleExoPlayer1?.currentPosition ?: 0L

                val frameIndex = ((currentPosition.toFloat() / videoDuration) * totalFrames).toInt()
                val coordinates = extractVideoCoordinates(leftJa)

                val (videoWidth, videoHeight) = getVideoDimensions(requireContext(), avm.trendLeftUri?.toUri())
                val aspectRatio = videoHeight.toFloat() / videoWidth.toFloat()
                if (aspectRatio < 1) {
                    val (editedWidth, editedHeight) = getVideoDimensions(requireContext(), Uri.fromFile(avm.leftEditedFile))
                    if (frameIndex in 0 until totalFrames) {
                        // overlay의 좌표를 crop된 영상에 맞게 수정하는 함수
                        val poseLandmarkResult = fromCoordinates(coordinates[frameIndex])
                        val transformedResult = transformCoordinates(
                            poseLandmarkResult,
                            videoWidth, videoHeight,  // 원본 크기
                            editedWidth, editedHeight  // 편집된 크기
                        )

                        requireActivity().runOnUiThread {
                            binding.ovMTDLeft.scaleX = -1f
                            binding.ovMTDLeft.setResults(
                                transformedResult, // if (isTablet(requireContext())) poseLandmarkResult else
                                videoWidth,
                                videoHeight,
                                OverlayView.RunningMode.VIDEO
                            )
                            binding.ovMTDLeft.invalidate()
                        }
                    }
                } else {
                    if (frameIndex in 0 until totalFrames) {

                        val poseLandmarkResult = fromCoordinates(coordinates[frameIndex])
                        requireActivity().runOnUiThread {
                            binding.ovMTDLeft.scaleX = -1f
                            binding.ovMTDLeft.setResults(
                                poseLandmarkResult,
                                videoWidth,
                                videoHeight,
                                OverlayView.RunningMode.VIDEO
                            )
                            binding.ovMTDLeft.invalidate()
                        }
                    }
                }
            }
        }
    }
    private fun transformCoordinates(
        result: PoseLandmarkResult,
        originalWidth: Int, originalHeight: Int,
        targetWidth: Int, targetHeight: Int,
    ): PoseLandmarkResult {

        // 원본 영상에서 크롭된 부분 계산 (FFmpeg 코드와 동일하게)
        val cropWidth = (originalHeight * 9) / 16
        val cropX = maxOf(0, (originalWidth - cropWidth) / 2)
        // 디버깅을 위한 로그
//        Log.d("크롭 계산", "원본 크기: ${originalWidth}x${originalHeight}, 크롭 너비: $cropWidth, 크롭 시작점: $cropX")

        val transformedLandmarks = result.landmarks.map { landmark ->
            if (isTablet(requireContext())) {
                // 태블릿의 경우 scaleY값만 맞추기

                val xScale = originalWidth.toFloat() / targetWidth.toFloat()  // X 축 스케일 증가 (더 넓게)
                val yScale = originalHeight.toFloat() / targetHeight.toFloat()
                val maxScale = max(xScale, yScale)

                val yOffset = (targetHeight.toFloat() - originalHeight.toFloat() * maxScale) / 2
                val normalizedY = landmark.y / originalHeight
                val newY = normalizedY * targetHeight * maxScale + yOffset

                PoseLandmarkResult.PoseLandmark(landmark.x, newY)
            } else {
                // 모바일 일 때 (scaleX만 맞추면 됨)

                // 원본 좌표에서 크롭 시작점 빼기
                val adjustedX = landmark.x - cropX
                // 크롭 영역 밖의 좌표 처리 (경계 안으로 제한)
                val clampedX = adjustedX.coerceIn(0f, cropWidth.toFloat())

                // 크롭된 영역에서의 비율 계산
                val normalizedX = clampedX / cropWidth
                val xScale = originalWidth.toFloat() / targetWidth.toFloat()  // X 축 스케일 증가 (더 넓게)
                val newX = normalizedX * targetWidth * xScale
                val newY = landmark.y * targetHeight / originalHeight  // Y는 잘 맞으므로 간단히 비율만 적용

                PoseLandmarkResult.PoseLandmark(newX, newY)
            }
        }
        return PoseLandmarkResult(transformedLandmarks)
    }

    private fun updateVideoUI(isRight: Boolean) {
        when (isRight) {
            true -> {
                val (videoWidth, videoHeight) = getVideoDimensions(requireContext(), avm.trendRightUri?.toUri()?: "".toUri())
                val aspectRatio = videoWidth.toFloat() / videoHeight.toFloat()
                Log.v("aspectRatio", "오른쪽: $aspectRatio")

                if (aspectRatio > 1) {
                    setPlayerByCroppedVideo(true, videoWidth.toFloat() , videoHeight.toFloat())
                } else {
                    val resizingValue = 0.5f
                    Log.v("오른쪽ratio", "$aspectRatio, $resizingValue")

                    // clMA의 크기 조절
                    val displayMetrics = DisplayMetrics()
                    requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
                    val screenWidth = displayMetrics.widthPixels
                    val adjustedHeight = (screenWidth / aspectRatio).toInt()

                    val params = binding.clMTDRight.layoutParams
                    params.width = (screenWidth  * resizingValue).toInt()
                    params.height = (adjustedHeight * resizingValue).toInt()
                    binding.clMTDRight.layoutParams = params
                    Log.v("오른쪽Params", "${params.width}, ${params.height}")

                    binding.clMTDRight.doOnLayout {
                        val clMTDParams = binding.clMTD.layoutParams
                        clMTDParams.height = binding.clMTDRight.height
                        binding.clMTD.layoutParams = clMTDParams
                        Log.v("오른쪽뷰높이체크", "in right:: height: ${binding.clMTDRight.height}, visibility: ${binding.clMTDRight.visibility}")
                        val constraintSet = ConstraintSet()
                        constraintSet.clone(binding.clMTD)
                        constraintSet.connect(binding.tvMTDGuide.id, ConstraintSet.TOP, binding.clMTD.id, ConstraintSet.BOTTOM)
                        constraintSet.applyTo(binding.clMTD)
                    }
                }
            }
            false -> {
                val (videoWidth, videoHeight) = getVideoDimensions(requireContext(), avm.trendLeftUri?.toUri()?: "".toUri())
                val aspectRatio = videoWidth.toFloat() / videoHeight.toFloat() // 9: 16
                Log.v("aspectRatio", "왼쪽: $aspectRatio")
                if (aspectRatio > 1) {
                    setPlayerByCroppedVideo(false, videoWidth.toFloat() , videoHeight.toFloat())
                    Log.v("플레이어 틀어지나요", "setPlayerByCroppedVideo, false")
                } else {
                    val resizingValue = 0.5f
                    Log.v("왼쪽ratio", "$aspectRatio, $resizingValue")

                    // clMA의 크기 조절
                    val displayMetrics = DisplayMetrics()
                    requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
                    val screenWidth = displayMetrics.widthPixels
                    val adjustedHeight = (screenWidth / aspectRatio).toInt()

                    val params = binding.clMTDLeft.layoutParams
                    params.width = (screenWidth * resizingValue).toInt()
                    params.height = (adjustedHeight * resizingValue).toInt()
                    binding.clMTDLeft.layoutParams = params
                    binding.clMTDLeft.doOnLayout {
                        val clMTDParams = binding.clMTD.layoutParams
                        clMTDParams.height = binding.clMTDRight.height
                        binding.clMTD.layoutParams = clMTDParams
                        Log.v("오른쪽뷰높이체크", "in left:: height: ${binding.clMTDRight.height}, visibility: ${binding.clMTDRight.visibility}")
                        val constraintSet = ConstraintSet()
                        constraintSet.clone(binding.clMTD)
                        constraintSet.connect(binding.rvMTD.id, ConstraintSet.TOP, binding.clMTD.id, ConstraintSet.BOTTOM)
                        constraintSet.applyTo(binding.clMTD)
                    }
                }
            }
        }
    }
    
    private fun setPlayerByCroppedVideo(isRight: Boolean, videoWidth: Float, videoHeight: Float) {
       when (isRight) {
           true -> {
               Handler(Looper.getMainLooper()).post {
                   Log.v("파일들", "${avm.leftEditedFile}, ${avm.leftEditedFile?.exists()} / ${avm.rightEditedFile}, ${avm.rightEditedFile?.exists()}")
                   val mediaItem = MediaItem.fromUri(Uri.fromFile(avm.rightEditedFile))
                   val mediaSource = ProgressiveMediaSource.Factory(DefaultDataSourceFactory(requireContext()))
                       .createMediaSource(mediaItem)

                   simpleExoPlayer2?.apply {
                       setMediaSource(mediaSource)
                       prepare()
                       seekTo(0)
                       playWhenReady = true
                   }
                   val displayMetrics = DisplayMetrics()
                   requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
                   val screenWidth = displayMetrics.widthPixels
                   val aspectRatio2 = videoWidth.toFloat() / videoHeight.toFloat()
                   val adjustedHeight = ((screenWidth / 2) * aspectRatio2).toInt()

                   // clMA의 크기 조절
                   val params = binding.clMTDRight.layoutParams
                   params.width = screenWidth / 2
                   params.height = adjustedHeight
                   binding.clMTDRight.layoutParams = params

                   // llMARV를 clMA 아래에 위치시키기
                   val constraintSet = ConstraintSet()
                   constraintSet.clone(binding.clMTD)
                   constraintSet.connect(binding.rvMTD.id, ConstraintSet.TOP, binding.clMTD.id, ConstraintSet.BOTTOM)
                   constraintSet.applyTo(binding.clMTD)
               }
           }
           false -> {
               Handler(Looper.getMainLooper()).post {
                   val mediaItem = MediaItem.fromUri(Uri.fromFile(avm.leftEditedFile))
                   val mediaSource = ProgressiveMediaSource.Factory(DefaultDataSourceFactory(requireContext()))
                       .createMediaSource(mediaItem)

                   simpleExoPlayer1?.apply {
                       setMediaSource(mediaSource)
                       prepare()
                       seekTo(0)
                       playWhenReady = true
                   }
                   val displayMetrics = DisplayMetrics()
                   requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
                   val screenWidth = displayMetrics.widthPixels
                   val aspectRatio2 = videoWidth.toFloat() / videoHeight.toFloat()
                   val adjustedHeight = ((screenWidth / 2) * aspectRatio2).toInt()

                   // clMA의 크기 조절
                   val params = binding.clMTDLeft.layoutParams
                   params.width = screenWidth / 2
                   params.height = adjustedHeight
                   binding.clMTDLeft.layoutParams = params
                   Log.v("왼쪽croppedVideo", "$aspectRatio2, ${screenWidth / 2 }, $adjustedHeight")
                   // llMARV를 clMA 아래에 위치시키기
                   val constraintSet = ConstraintSet()
                   constraintSet.clone(binding.clMTD)
                   constraintSet.connect(binding.rvMTD.id, ConstraintSet.TOP, binding.clMTD.id, ConstraintSet.BOTTOM)
                   constraintSet.applyTo(binding.clMTD)
               }
           }
       }
    }

    override fun onPause() {
        super.onPause()
        simpleExoPlayer1?.let { player ->
            player.stop()
            player.playWhenReady = false
        }
        simpleExoPlayer2?.let { player ->
            player.stop()
            player.playWhenReady = false
        }
    }

    override fun onStop() {
        super.onStop()
        simpleExoPlayer1?.let { player ->
            player.stop()
            player.playWhenReady = false
        }
        simpleExoPlayer2?.let { player ->
            player.stop()
            player.playWhenReady = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        simpleExoPlayer1?.release()
        simpleExoPlayer2?.release()
    }
}