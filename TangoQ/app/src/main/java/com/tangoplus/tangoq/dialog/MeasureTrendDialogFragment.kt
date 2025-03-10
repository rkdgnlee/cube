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
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.MediaItem
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
import com.tangoplus.tangoq.function.MeasurementManager.extractVideoCoordinates
import com.tangoplus.tangoq.function.MeasurementManager.getVideoDimensions
import com.tangoplus.tangoq.function.MeasurementManager.matchedIndexs
import com.tangoplus.tangoq.function.MeasurementManager.matchedTripleIndexes
import com.tangoplus.tangoq.function.MeasurementManager.matchedUris
import com.tangoplus.tangoq.function.SaveSingletonManager
import com.tangoplus.tangoq.listener.OnSingleClickListener
import com.tangoplus.tangoq.mediapipe.MathHelpers.isTablet
import com.tangoplus.tangoq.mediapipe.OverlayView
import com.tangoplus.tangoq.mediapipe.PoseLandmarkResult.Companion.fromCoordinates
import com.tangoplus.tangoq.viewmodel.PlayViewModel
import com.tangoplus.tangoq.vo.AnalysisUnitVO
import com.tangoplus.tangoq.vo.DateDisplay
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class MeasureTrendDialogFragment : DialogFragment() {
    lateinit var binding : FragmentMeasureTrendDialogBinding
    private val avm : AnalysisViewModel by activityViewModels()
    private val mvm : MeasureViewModel by activityViewModels()
    private val pvm : PlayViewModel by activityViewModels()

    private lateinit var measures : MutableList<MeasureVO>
    private lateinit var singletonMeasure : Singleton_t_measure
    private lateinit var ssm : SaveSingletonManager
    private lateinit var measureResult : JSONArray
    private var updateUI = false
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasureTrendDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        measures = Singleton_t_measure.getInstance(requireContext()).measures ?: mutableListOf()
        ssm = SaveSingletonManager(requireContext(), requireActivity())
        singletonMeasure = Singleton_t_measure.getInstance(requireContext())
        try {
            if (measures.isNotEmpty()) {
//                showBalloon()
                avm.leftMeasurement.value = null
                avm.leftAnalysises = null
                avm.rightMeasurement.value = measures[0]
                avm.currentIndex = 0
                measureResult = mvm.selectedMeasure?.measureResult ?: JSONArray()

                binding.ibtnMTDBack.setOnClickListener { dismiss() }
                pvm.setLeftPlaybackPosition(0)
                pvm.setRightPlaybackPosition(0)
                // ------# 비교할 모든 analysisVO 넣기 #------
                avm.rightAnalysises = transformAnalysisUnit(avm.rightMeasurement.value?.measureResult ?: JSONArray())

                // -------# 측정 날짜 고르는 ACTV left #-------
                avm.measureDisplayDates = avm.createDateDisplayList(measures)

                val leftAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, avm.measureDisplayDates.filterIndexed { index, dateDisplay -> index != 0 })
                binding.actvMTDLeft.setAdapter(leftAdapter)
                binding.actvMTDLeft.setOnItemClickListener { _, _, position, _ ->
                    // 날짜 선택
                    val selectedDate = binding.actvMTDLeft.adapter.getItem(position) as DateDisplay

                    avm.leftMeasureDate.value = avm.measureDisplayDates.find { it.fullDateTime == selectedDate.fullDateTime }
                    binding.actvMTDLeft.setText(avm.leftMeasureDate.value?.displayDate, false)
                    Log.v("selectedDateLeft", "${avm.leftMeasureDate.value?.fullDateTime}, ${avm.measureDisplayDates.map { it.fullDateTime }}")
                    CoroutineScope(Dispatchers.IO).launch {
                        // 측정파일 없으면 다운로드
                        setMeasureFiles(avm.leftMeasureDate.value?.fullDateTime)
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
    private fun setACTVClickListener(position: Int) {
        // 날짜 선택
        val selectedDate = binding.actvMTDRight.adapter.getItem(position) as DateDisplay
        avm.rightMeasureDate.value = avm.measureDisplayDates.find { it.fullDateTime == selectedDate.fullDateTime }
        binding.actvMTDRight.setText(avm.rightMeasureDate.value?.displayDate, false)

//        Log.v("selectedDateRight", "${avm.rightMeasureDate.value?.fullDateTime}, ${avm.measureDisplayDates.map { it.fullDateTime }}")
        CoroutineScope(Dispatchers.IO).launch {
            // 데이터 셋팅 확인
            setMeasureFiles(avm.rightMeasureDate.value?.fullDateTime)

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
    private fun updateButtonState() {
        val buttons = listOf(binding.tvMTD1, binding.tvMTD2, binding.tvMTD3, binding.tvMTD4, binding.tvMTD5, binding.tvMTD6, binding.tvMTD7)

        buttons.forEachIndexed { index, button ->
            button.backgroundTintList = when {
                avm.currentIndex == index -> ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
                else -> ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor200))
            }
            button.setOnSingleClickListener {
                avm.currentIndex = index
                updateButtonState()  // 버튼 상태를 다시 업데이트

                CoroutineScope(Dispatchers.IO).launch {
                    val transIndex = if (avm.currentIndex == 6) {
                        1
                    } else if (avm.currentIndex > 0){
                        avm.currentIndex + 1
                    } else {
                        0
                    }

                    // 왼쪽이 없을 때는 오른쪽만 갱신
                    withContext(Dispatchers.Main) {
                        if (avm.leftMeasurement.value == null) {
                            if (avm.currentIndex != 6) {
                                setVideoUI(false, true)
                                setImage(this@MeasureTrendDialogFragment, avm.rightMeasurement.value, transIndex, binding.ssivMTDRight, "trend")
                            } else {
                                setVideoUI(true, true)
                                setClickListener(true)
                                setPlayer(true)
                            }
                        } else {

                            if (avm.currentIndex != 6) {
                                setVideoUI(false, false)
                                setVideoUI(false, true)
                                setImage(this@MeasureTrendDialogFragment, avm.leftMeasurement.value, transIndex, binding.ssivMTDLeft, "trend")
                                setImage(this@MeasureTrendDialogFragment, avm.rightMeasurement.value, transIndex, binding.ssivMTDRight, "trend")
                            } else  {
                                setVideoUI(true, false)
                                setClickListener(false)
                                setPlayer(false)
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

    private fun setAdapter(analyzesLeft: MutableList<MutableList<AnalysisUnitVO>>?,
                           analyzesRight : MutableList<MutableList<AnalysisUnitVO>>?,
                           currentSeq : Int = 0) {
        val filteredLeftAnalysises = if (currentSeq == 1) {
            analyzesLeft?.filterIndexed{ index, _ -> index in listOf(1, 2, 7,8,9,10) }?.toMutableList()
        } else {
            analyzesLeft?.mapNotNull { analysisList ->
                if (analysisList.any { it.seq == currentSeq }) {
                    analysisList
                } else {
                    null
                }
            }?.toMutableList()
        }
        val filteredRightAnalysises = if (currentSeq == 1) {
            analyzesRight?.filterIndexed{ index, _ -> index in listOf(1, 2, 7,8,9,10) }?.toMutableList()
        } else {
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
        val adapter = TrendRVAdapter(this@MeasureTrendDialogFragment, filteredLeftAnalysises, filteredRightAnalysises, filteredIndexes?.map { matchedIndexs[it] })
        binding.rvMTD.layoutManager = layoutManager
        binding.rvMTD.adapter = adapter
    }


    // 13 > 3개의 AnalysisVO임. == 관절 1개의 3개가 들어간 raw 하나를 말하는 것.
    private fun transformAnalysisUnit(ja : JSONArray) : MutableList<MutableList<AnalysisUnitVO>> {
        val analyzes1 = mutableListOf<MutableList<MutableList<AnalysisUnitVO>>>()
        matchedUris.forEach { (part, seqList) -> // 13가지 관절 전부다
            val analyzes2 = mutableListOf<MutableList<AnalysisUnitVO>>()
            seqList.forEachIndexed { index, i -> // 각 시퀀스별로
                val analyzes3 = getAnalysisUnits(requireContext(), part, i, ja) // 지금 여기서 1이 들어가서 문제가 생긴듯?
                analyzes2.add(analyzes3)
            }
            analyzes1.add(analyzes2)
        }
        val analysis1 = mutableListOf<MutableList<AnalysisUnitVO>>()
        // 지금 모든 값들이 다 들어가있는데 matchedTripleIndexes를 통해서 13개의 각각의
        Log.v("analyse1", "${analyzes1.size}, ${analyzes1.map { it.size }}")
        matchedTripleIndexes.mapIndexed { indexx, item3 ->

            val analysis2 = item3.map { (seq, matchedIndex, index1) ->
                analyzes1[indexx][matchedIndex][index1] // 해당 index에서 가져오기
            }.toMutableList()
            analysis1.add(analysis2)
        }
        Log.v("유닛3개로", "${analysis1.size}, ${analysis1.map { it.map { it.columnName } }}")
        return analysis1
    }

    private suspend fun setMeasureFiles(inputRegDate: String?) {

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
                            avm.leftMeasurement.value = editedMeasure
//                            Log.v("수정완료", "index: $singletonIndex, VO: $editedMeasure")
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
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
    private fun setPlayer(isRight: Boolean) {
        when (isRight) {
            true -> {
                lifecycleScope.launch {
                    rightJa = avm.rightMeasurement.value?.measureResult?.getJSONArray(1) ?: JSONArray()
                    initPlayer(isRight)
                    simpleExoPlayer2?.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            super.onPlaybackStateChanged(playbackState)
                            if (playbackState == Player.STATE_READY) {

                                val videoDuration = simpleExoPlayer2?.duration ?: 0L
                                lifecycleScope.launch {
                                    while (simpleExoPlayer2?.isPlaying == true) {
                                        if (!updateUI) updateVideoUI(isRight)

                                        updateFrameData(true, videoDuration, rightJa.length())
                                        delay(24)
                                        Handler(Looper.getMainLooper()).postDelayed( { updateUI = true },1500)
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
                    initPlayer(isRight)

                    simpleExoPlayer1?.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            super.onPlaybackStateChanged(playbackState)
                            if (playbackState == Player.STATE_READY) {

                                val videoDuration = simpleExoPlayer1?.duration ?: 0L
                                lifecycleScope.launch {
                                    while (simpleExoPlayer1?.isPlaying == true) {
                                        if (!updateUI) updateVideoUI(isRight)
                                        updateFrameData(isRight, videoDuration, leftJa.length())
                                        delay(24)
                                        Handler(Looper.getMainLooper()).postDelayed( { updateUI = true },1500)
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
        when (isRight) {
            true -> {
                // viewModel의 이전 영상 보존값들 초기화
                simpleExoPlayer2 = SimpleExoPlayer.Builder(requireContext()).build()
                binding.pvMTDRight.player = simpleExoPlayer2
                binding.pvMTDRight.controllerShowTimeoutMs = 1100
                lifecycleScope.launch {
                    // 저장된 URL이 있다면 사용, 없다면 새로운 URL 가져오기
                    avm.trendRightUri = avm.rightMeasurement.value?.fileUris?.get(1).toString()
//                    Log.v("VMTrendRight", "${avm.trendRightUri}")
                    val mediaItem = MediaItem.fromUri(Uri.parse(avm.trendRightUri))
                    val mediaSource = ProgressiveMediaSource.Factory(DefaultDataSourceFactory(requireContext()))
                        .createMediaSource(mediaItem)

                    mediaSource.let {
                        simpleExoPlayer2?.prepare(it)
                        // 저장된 위치로 정확하게 이동
                        simpleExoPlayer2?.seekTo(0)
                        simpleExoPlayer2?.playWhenReady = pvm.getPlayWhenReady()
                    }
                }
            }
            false -> {
                simpleExoPlayer1 = SimpleExoPlayer.Builder(requireContext()).build()
                binding.pvMTDLeft.player = simpleExoPlayer1
                binding.pvMTDLeft.controllerShowTimeoutMs = 1100
                lifecycleScope.launch {
                    // 저장된 URL이 있다면 사용, 없다면 새로운 URL 가져오기
                    avm.trendLeftUri = avm.leftMeasurement.value?.fileUris?.get(1).toString()
                    val mediaItem = MediaItem.fromUri(Uri.parse(avm.trendLeftUri))
                    val mediaSource = ProgressiveMediaSource.Factory(DefaultDataSourceFactory(requireContext()))
                        .createMediaSource(mediaItem)

                    mediaSource.let {
                        simpleExoPlayer1?.prepare(it)
                        // 저장된 위치로 정확하게 이동
                        simpleExoPlayer1?.seekTo(0)
                        simpleExoPlayer1?.playWhenReady = pvm.getPlayWhenReady()
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
                llSpeed2?.visibility = View.GONE
                exoExit2?.visibility = View.GONE
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
            }
            false -> {
                exoForward1?.visibility = View.GONE
                exoReplay1?.visibility = View.GONE
                exoPlay1?.visibility = View.GONE
                llSpeed1?.visibility = View.GONE
                exoExit1?.visibility = View.GONE
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
            false -> {
                val currentPosition = simpleExoPlayer1?.currentPosition ?: 0L

                val frameIndex = ((currentPosition.toFloat() / videoDuration) * totalFrames).toInt()
                val coordinates = extractVideoCoordinates(leftJa)

                val (videoWidth, videoHeight) = getVideoDimensions(requireContext(), avm.trendLeftUri?.toUri())
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

    private fun updateVideoUI(isRight: Boolean) {
        when (isRight) {
            true -> {
                val (videoWidth, videoHeight) = getVideoDimensions(requireContext(), avm.trendRightUri?.toUri()?: "".toUri())
                val displayMetrics = DisplayMetrics()
                requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
                val screenWidth = displayMetrics.widthPixels

                val aspectRatio = videoHeight.toFloat() / videoWidth.toFloat()
//                Log.v("aspectRatio", "$aspectRatio")
                val adjustedHeight = (screenWidth * aspectRatio).toInt()

                val resizingValue = if (isTablet(requireContext())) {
                    if (aspectRatio > 1) {
                        0.5f
                    } else { // 가로 (키오스크 일 때 원본 유지 )
                        1f
                    }
                } else 0.5f // 태블릿이 아닐 때는 상관없음.

                // clMA의 크기 조절
                val params = binding.clMTDRight.layoutParams
                params.width = (screenWidth  * resizingValue).toInt()
                params.height = (adjustedHeight * resizingValue).toInt()
                binding.clMTDRight.layoutParams = params

                // llMARV를 clMA 아래에 위치시키기
                val constraintSet = ConstraintSet()
                constraintSet.clone(binding.clMTD)
                constraintSet.connect(binding.rvMTD.id, ConstraintSet.TOP, binding.clMTD.id, ConstraintSet.BOTTOM)
                constraintSet.applyTo(binding.clMTD)

                // PlayerView 크기 조절 (필요한 경우)
            }
            false -> {
                val (videoWidth, videoHeight) = getVideoDimensions(requireContext(), avm.trendLeftUri?.toUri()?: "".toUri())
                val displayMetrics = DisplayMetrics()
                requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
                val screenWidth = displayMetrics.widthPixels

                val aspectRatio = videoHeight.toFloat() / videoWidth.toFloat()
                val adjustedHeight = (screenWidth * aspectRatio).toInt()

                val resizingValue = if (isTablet(requireContext())) {
                    if (aspectRatio > 1) {
                        0.5f
                    } else { // 가로 (키오스크 일 때 원본 유지 )
                        1f
                    }
                } else 0.5f // 태블릿이 아닐 때는 상관없음.

                // clMA의 크기 조절
                val params = binding.clMTDLeft.layoutParams
                params.width = (screenWidth  * resizingValue).toInt()
                params.height = (adjustedHeight * resizingValue).toInt()
                binding.clMTDLeft.layoutParams = params

                val constraintSet = ConstraintSet()
                constraintSet.clone(binding.clMTD)
                constraintSet.connect(binding.rvMTD.id, ConstraintSet.TOP, binding.clMTD.id, ConstraintSet.BOTTOM)
                constraintSet.applyTo(binding.clMTD)

//                binding.clMTDLeft.requestLayout()
            }
        }
        val clMTDParams = binding.clMTD.layoutParams
        clMTDParams.height = binding.clMTDRight.height
        binding.clMTD.layoutParams = clMTDParams
    }

//    private fun setVideoAdapter(data: List<List<Pair<Float, Float>>>) {
//        val linearLayoutManager1 = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
//        val dynamicAdapter = DataDynamicRVAdapter(data, avm.dynamicTitles)
//        binding.rvMALeft.layoutManager = linearLayoutManager1
//        binding.rvMALeft.adapter = dynamicAdapter
//    }

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
    private fun View.setOnSingleClickListener(action: (v: View) -> Unit) {
        val listener = View.OnClickListener { action(it) }
        setOnClickListener(OnSingleClickListener(listener))
    }
}