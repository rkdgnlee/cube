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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignBottom
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.DataDynamicRVAdapter
import com.tangoplus.tangoq.adapter.etc.SpinnerAdapter
import com.tangoplus.tangoq.adapter.TrendRVAdapter
import com.tangoplus.tangoq.vo.AnalysisVO
import com.tangoplus.tangoq.viewmodel.AnalysisViewModel
import com.tangoplus.tangoq.vo.MeasureVO
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureTrendDialogBinding
import com.tangoplus.tangoq.function.MeasurementManager.getAnalysisUnits
import com.tangoplus.tangoq.function.MeasurementManager.matchedUris
import com.tangoplus.tangoq.function.MeasurementManager.setImage
import com.tangoplus.tangoq.db.Singleton_t_measure
import com.tangoplus.tangoq.function.BiometricManager
import com.tangoplus.tangoq.function.MeasurementManager.extractVideoCoordinates
import com.tangoplus.tangoq.function.MeasurementManager.getVideoDimensions
import com.tangoplus.tangoq.function.MeasurementManager.matchedTripleIndexes
import com.tangoplus.tangoq.function.SaveSingletonManager
import com.tangoplus.tangoq.mediapipe.MathHelpers.isTablet
import com.tangoplus.tangoq.mediapipe.OverlayView
import com.tangoplus.tangoq.mediapipe.PoseLandmarkResult.Companion.fromCoordinates
import com.tangoplus.tangoq.viewmodel.PlayViewModel
import com.tangoplus.tangoq.vo.DateDisplay
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
    private lateinit var biometricManager : BiometricManager

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
                showBalloon()
                avm.leftMeasurement.value = null
                avm.leftAnalyzes = null
                avm.rightMeasurement.value = measures[0]

                measureResult = mvm.selectedMeasure?.measureResult ?: JSONArray()
                // ------# 비교할 모든 analysisVO 넣기 #------
                avm.rightAnalyzes =
                    transformAnalysis(avm.rightMeasurement.value?.measureResult ?: JSONArray())

                // ------# seq left spnr #------
                val seqs = listOf(
                    "정면 측정", "동적 측정", "팔꿉 측정", "좌측 측정", "우측 측정", "후면 측정", "앉아 후면"
                )

                // ------# 측정 내 seq 고르는 spinner left #------
                binding.spnrMTDSeqLeft.setPopupBackgroundDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.color.white
                    )
                )
                binding.spnrMTDSeqLeft.adapter =
                    SpinnerAdapter(requireContext(), R.layout.item_spinner, seqs, 2)
                binding.spnrMTDSeqLeft.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            CoroutineScope(Dispatchers.IO).launch {
                                if (avm.leftMeasurement.value != null) {
                                    if (position != 1) {
                                        withContext(Dispatchers.Main) {
                                            setVideoUI(false, false)
                                            Log.v("어디서?", "rightspnr else")
                                            setImage(this@MeasureTrendDialogFragment, avm.leftMeasurement.value, position, binding.ssivMTDLeft, "trend")
                                        }
                                    } else  {
                                        withContext(Dispatchers.Main) {
                                            setVideoUI(true, false)
                                            setClickListener(false)
                                            setPlayer(false)
                                        }
                                    }
                                }
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }

                // ------# 측정 내 seq 고르는 spinner right #------
                binding.spnrMTDSeqRight.setPopupBackgroundDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.color.white
                    )
                )
                binding.spnrMTDSeqRight.adapter =
                    SpinnerAdapter(requireContext(), R.layout.item_spinner, seqs, 2)
                binding.spnrMTDSeqRight.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            Log.v("시퀀스들", "position: ${position}, rightMeasure: ${avm.rightMeasurement.value}")
                            CoroutineScope(Dispatchers.IO).launch {
                                if (position != 1) {
                                    withContext(Dispatchers.Main) {
                                        setVideoUI(false, true)
                                        Log.v("어디서?", "rightspnr else")

                                        setImage(this@MeasureTrendDialogFragment, avm.rightMeasurement.value, position, binding.ssivMTDRight, "trend")
                                    }
                                } else  {
                                    withContext(Dispatchers.Main) {
                                        setVideoUI(true, true)
                                        setClickListener(true)
                                        setPlayer(true)
                                    }
                                }
                                // spinner 클릭시 adapter 갱신
                                withContext(Dispatchers.Main) {
                                    val parentIndexes =
                                        matchedTripleIndexes.mapIndexedNotNull { index, triples ->
                                            when {
                                                position == 0 -> if (triples.any { triple -> triple.first == 0 }) index else null
                                                else -> if (triples.any { triple -> triple.first == position + 1 }) index else null
                                            }
                                        }
                                    Log.v("parentIndexes", "$parentIndexes")
//                                val filteredAnalyzes = avm.leftAnalyzes?.filterIndexed { index, _ ->
//                                    index in parentIndexes
//                                }?.toMutableList()
                                    setAdapter(avm.leftAnalyzes, avm.rightAnalyzes, parentIndexes)


                                    val clMTDParams = binding.clMTD.layoutParams
                                    clMTDParams.height = binding.ssivMTDRight.height
                                    binding.clMTD.layoutParams = clMTDParams
                                }
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }


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
                            avm.leftAnalyzes = transformAnalysis(
                                avm.leftMeasurement.value?.measureResult ?: JSONArray()
                            )

                            // 다운로드한 jpg 파일 drawing
                            setImage(
                                this@MeasureTrendDialogFragment,
                                avm.leftMeasurement.value,
                                0,
                                binding.ssivMTDLeft,
                                "trend"
                            )
                            setAdapter(avm.leftAnalyzes, avm.rightAnalyzes)
                            binding.spnrMTDSeqLeft.setSelection(0)
                        }
                    }
                    binding.tvMTDAlert.visibility = View.GONE
                }

                // ------# 측정 고르는 ACTV right #------
                // 초기 오른쪽 데이터 셋팅
                binding.actvMTDRight.setText(avm.measureDisplayDates[0].displayDate)
                binding.actvMTDRight.setSelection(0)
                avm.rightAnalyzes = transformAnalysis(avm.rightMeasurement.value?.measureResult ?: JSONArray())
                setAdapter(avm.leftAnalyzes, avm.rightAnalyzes, listOf())

                // 오른쪽 actv adapter 연결
                val rightAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, avm.measureDisplayDates)
                binding.actvMTDRight.setAdapter(rightAdapter)
                binding.actvMTDRight.setOnItemClickListener { _, _, position, _ ->
                    // 날짜 선택
                    val selectedDate = binding.actvMTDRight.adapter.getItem(position) as DateDisplay


                    avm.rightMeasureDate.value = avm.measureDisplayDates.find { it.fullDateTime == selectedDate.fullDateTime }
                    binding.actvMTDRight.setText(avm.rightMeasureDate.value?.displayDate, false)
                    Log.v("selectedDateRight", "${avm.rightMeasureDate.value?.fullDateTime}, ${avm.measureDisplayDates.map { it.fullDateTime }}")
                    CoroutineScope(Dispatchers.IO).launch {
                        // 측정파일 없으면 다운로드
                        setMeasureFiles(avm.rightMeasureDate.value?.fullDateTime)
                        withContext(Dispatchers.Main) {
                            // TODO 날짜 확인해보기
                            avm.rightMeasurement.value = Singleton_t_measure.getInstance(requireContext()).measures?.find { it.regDate == avm.rightMeasureDate.value?.fullDateTime }

                            avm.rightAnalyzes = transformAnalysis(avm.rightMeasurement.value?.measureResult ?: JSONArray())
                            setImage(this@MeasureTrendDialogFragment, avm.rightMeasurement.value, 0, binding.ssivMTDRight, "trend")
                        }
                    }
                }

                CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.Main) {
                        // 초기 오른쪽 선택으로 렌더링 시작
                        binding.spnrMTDSeqRight.setSelection(0)
                    }
                }
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

    private fun setAdapter(analyzesLeft: MutableList<MutableList<AnalysisVO>>?,
                           analyzesRight : MutableList<MutableList<AnalysisVO>>?,
                           filteredIndexes: List<Int> = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)) {
        // 부위별 > 3개 > 2개의 float
        // ------# rv #------

//        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
//        val adapter = TrendRVAdapter(this@MeasureTrendDialogFragment, analyzesLeft, analyzesRight, filteredIndexes)
//        binding.rvMTD.layoutManager = layoutManager
//        binding.rvMTD.adapter = adapter
//        adapter.notifyDataSetChanged()
//
//        val rightHeight = binding.ssivMTDRight.height
//        val layoutParams = binding.ssivMTDLeft.layoutParams
//        layoutParams.height = rightHeight
//        binding.ssivMTDLeft.layoutParams = layoutParams
    }

    private fun showBalloon() {
        val balloon2 = Balloon.Builder(requireContext())
            .setWidth(BalloonSizeSpec.WRAP)
            .setHeight(BalloonSizeSpec.WRAP)
            .setText("비교 날짜를 선택해주세요")
            .setTextColorResource(R.color.white)
            .setTextSize(if (isTablet(requireContext())) 24f else 16f)
            .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            .setArrowSize(0)
            .setMargin(10)
            .setPadding(12)
            .setCornerRadius(8f)
            .setBackgroundColorResource(R.color.mainColor)
            .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        Handler(Looper.getMainLooper()).postDelayed({
            binding.tilMTDLeft.showAlignBottom(balloon2)
            balloon2.dismissWithDelay(5000L)
        }, 700)
//        if (isFirstRun("MeasureTrendDialogFragment_isFirstRun")) {
//
//        }
        binding.textView30.setOnClickListener { it.showAlignBottom(balloon2) }
    }

    // 13 > 3개의 AnalysisVO임. == 관절 1개의 3개가 들어간 raw 하나를 말하는 것.
    private fun transformAnalysis(ja : JSONArray) : MutableList<MutableList<AnalysisVO>> {
        val analyzes = mutableListOf<MutableList<AnalysisVO>>()
        matchedUris.forEach { (part, seqList) ->
            val relatedAnalyzes = mutableListOf<AnalysisVO>()

            seqList.forEachIndexed { index, i ->
                val analysisUnits = getAnalysisUnits(requireContext(), part, i, ja)
//                val normalUnits = analysisUnits.filter { it.state == 1 }.size
//                val warningUnits = analysisUnits.filter { it.state == 2 }.size
//                val dangerUnits = analysisUnits.filter { it.state == 3 }.size
//                val state = if (normalUnits >= warningUnits && normalUnits >= dangerUnits) 1
//                else if (warningUnits >= normalUnits && warningUnits >= dangerUnits) 2
//                else if (dangerUnits >= normalUnits && dangerUnits >= warningUnits) 3
//                else 0
                val analysisVO = AnalysisVO(
                    i,
                    analysisUnits,
                )
                relatedAnalyzes.add(analysisVO)
            }
            analyzes.add(relatedAnalyzes)
        }
        return analyzes
    }

    private suspend fun setMeasureFiles(inputRegDate: String?) {
        val dialog = LoadingDialogFragment.newInstance("측정파일")
        dialog.show(requireActivity().supportFragmentManager, "LoadingDialogFragment")

        // ------# 로딩을 통해 파일 가져오기 #------
        withContext(Dispatchers.IO) {
            try {
                val currentMeasure = singletonMeasure.measures?.find { it.regDate == inputRegDate }
                val uriTuples = currentMeasure?.sn?.let { it1 -> ssm.get1MeasureUrls(it1) }
                if (uriTuples != null) {
                    Log.v("리스너1", "$uriTuples")
                    ssm.downloadFiles(uriTuples)
                    val editedMeasure = ssm.insertUrlToMeasureVO(uriTuples, currentMeasure)
                    Log.v("리스너2", "$editedMeasure")

                    // singleton의 인덱스 찾아서 ja와 값 넣기
                    val singletonIndex = singletonMeasure.measures?.indexOfLast { it.regDate == inputRegDate }
                    if (singletonIndex != null && singletonIndex >= 0) {
                        singletonMeasure.measures?.set(singletonIndex, editedMeasure)
                        avm.leftMeasurement.value = editedMeasure
                        Log.v("수정완료", "index: $singletonIndex, VO: $editedMeasure")
                        withContext(Dispatchers.Main) {
                            dialog.dismiss()
                        }
                    } else {

                    }
                } else {

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
        Log.w("selectedMeasureDate", "selectedMeasure: ${mvm.selectedMeasureDate.value}, selectMeasure: ${mvm.selectMeasureDate.value}")
        Log.w("selectedMeasure", "${mvm.selectedMeasure}")
        dialog.dismiss()
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
                    Log.v("동적측정json", "${avm.rightMeasurement.value?.measureResult?.getJSONArray(1)}")
                    rightJa = avm.rightMeasurement.value?.measureResult?.getJSONArray(1) ?: JSONArray()
                    Log.v("rightJa길이", "${rightJa.length()}")
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
                            }
                        }
                    })

                }
            }
            false -> {
                lifecycleScope.launch {
                    Log.v("동적측정json", "${avm.leftMeasurement.value?.measureResult?.getJSONArray(1)}")
                    leftJa = avm.leftMeasurement.value?.measureResult?.getJSONArray(1) ?: JSONArray()
                    Log.v("leftJa길이", "${leftJa.length()}")
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
                    Log.v("VMTrendRight", "${avm.trendRightUri}")
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
                    simpleExoPlayer2?.seekTo(0)
                    simpleExoPlayer2?.play()
                    exoPlay2?.visibility = View.GONE
                    exoPause2?.visibility = View.VISIBLE
                }
                exoPause2?.setOnClickListener{
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
                    simpleExoPlayer1?.seekTo(0)
                    simpleExoPlayer1?.play()
                    exoPlay1?.visibility = View.GONE
                    exoPause1?.visibility = View.VISIBLE

                }
                exoPause1?.setOnClickListener{
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
                Log.v("aspectRatio", "$aspectRatio")
                val adjustedHeight = (screenWidth * aspectRatio).toInt()

                val resizingValue = if (isTablet(requireContext())) {
                    if (aspectRatio > 1) {
                        1f
                    } else { // 가로 (키오스크 일 때 원본 유지 )
                        1f
                    }
                } else 1f // 태블릿이 아닐 때는 상관없음.

                // clMA의 크기 조절
                val params = binding.clMTD.layoutParams
                params.width = (screenWidth  * resizingValue).toInt()
                params.height = (adjustedHeight * resizingValue).toInt()
                binding.clMTD.layoutParams = params

                // llMARV를 clMA 아래에 위치시키기
                val constraintSet = ConstraintSet()
                constraintSet.clone(binding.clMTD)
                constraintSet.connect(binding.rvMTD.id, ConstraintSet.TOP, binding.clMTD.id, ConstraintSet.BOTTOM)
                constraintSet.applyTo(binding.clMTD)

                val rightParams = binding.clMTDRight.layoutParams
                rightParams.height = binding.flMTDRight.height
                binding.clMTDRight.layoutParams = rightParams

                val clMTDParams = binding.clMTD.layoutParams
                clMTDParams.height = binding.clMTDRight.height
                binding.clMTD.layoutParams = clMTDParams
                // PlayerView 크기 조절 (필요한 경우)
//                val playerParams = binding.pvMTDRight.layoutParams
//                playerParams.width = (screenWidth  * resizingValue).toInt()
//                playerParams.height = (adjustedHeight * resizingValue).toInt()
//                binding.pvMTDRight.layoutParams = playerParams
            }
            false -> {
                val (videoWidth, videoHeight) = getVideoDimensions(requireContext(), avm.trendLeftUri?.toUri()?: "".toUri())
                val displayMetrics = DisplayMetrics()
                requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
                val screenWidth = displayMetrics.widthPixels

                val aspectRatio = videoHeight.toFloat() / videoWidth.toFloat()
                Log.v("aspectRatio", "$aspectRatio")
                val adjustedHeight = (screenWidth * aspectRatio).toInt()

                val resizingValue = if (isTablet(requireContext())) {
                    if (aspectRatio > 1) {
                        0.7f
                    } else { // 가로 (키오스크 일 때 원본 유지 )
                        1f
                    }
                } else 1f // 태블릿이 아닐 때는 상관없음.

                // clMA의 크기 조절
                val params = binding.clMTDLeft.layoutParams
                params.width = (screenWidth  * resizingValue).toInt()
                params.height = (adjustedHeight * resizingValue).toInt()
                binding.clMTDLeft.layoutParams = params

                // llMARV를 clMA 아래에 위치시키기
                val constraintSet = ConstraintSet()
                constraintSet.clone(binding.clMTD)
                constraintSet.connect(binding.rvMTD.id, ConstraintSet.TOP, binding.clMTD.id, ConstraintSet.BOTTOM)
                constraintSet.applyTo(binding.clMTD)

                binding.clMTDLeft.requestLayout()
                // PlayerView 크기 조절 (필요한 경우)
//                val playerParams = binding.pvMTDLeft.layoutParams
//                playerParams.width = (screenWidth  * resizingValue).toInt()
//                playerParams.height = (adjustedHeight * resizingValue).toInt()
//                binding.pvMTDLeft.layoutParams = playerParams
            }
        }
    }

    private fun setVideoAdapter(data: List<List<Pair<Float, Float>>>) {
        val linearLayoutManager1 = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val dynamicAdapter = DataDynamicRVAdapter(data, avm.dynamicTitles)
//        binding.rvMALeft.layoutManager = linearLayoutManager1
//        binding.rvMALeft.adapter = dynamicAdapter
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