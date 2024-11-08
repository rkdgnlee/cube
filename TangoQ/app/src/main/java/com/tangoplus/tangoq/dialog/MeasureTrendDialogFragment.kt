package com.tangoplus.tangoq.dialog

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignBottom
import com.skydoves.balloon.showAlignEnd
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.SpinnerAdapter
import com.tangoplus.tangoq.adapter.TrendRVAdapter
import com.tangoplus.tangoq.data.AnalysisUnitVO
import com.tangoplus.tangoq.data.AnalysisVO
import com.tangoplus.tangoq.data.AnalysisViewModel
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureTrendDialogBinding
import com.tangoplus.tangoq.db.MeasurementManager.setImage
import com.tangoplus.tangoq.fragment.isFirstRun
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class MeasureTrendDialogFragment : DialogFragment() {
    lateinit var binding : FragmentMeasureTrendDialogBinding
    private lateinit var trend1 : String
    private lateinit var trend2 : String
    private val avm : AnalysisViewModel by activityViewModels()
    private val mvm : MeasureViewModel by activityViewModels()
    private lateinit var measures : MutableList<MeasureVO>
    private var count = false

    private lateinit var measureResult : JSONArray
    companion object{
        private const val ARG_COMPARE1 = "arg_compare1"
        private const val ARG_COMPARE2 = "arg_compare2"
        fun newInstance(trend1: String, trend2: String) : MeasureTrendDialogFragment {
            val fragment = MeasureTrendDialogFragment()
            val args = Bundle()
            args.putString(ARG_COMPARE1, trend1)
            args.putString(ARG_COMPARE2, trend2)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasureTrendDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        trend1 = arguments?.getString(ARG_COMPARE1) ?: ""
        trend2 = arguments?. getString(ARG_COMPARE2) ?: ""
        measures = Singleton_t_measure.getInstance(requireContext()).measures ?: mutableListOf()
        try {
            if (measures.isNotEmpty()) {
                showBalloon()
                avm.rightMeasurement.value = measures[0]
                measureResult = mvm.selectedMeasure?.measureResult ?: JSONArray()
                // ------# 비교할 모든 analysisVO 넣기 #------
                avm.rightAnalyzes = transformAnalysis(avm.rightMeasurement.value?.measureResult!!)
                CoroutineScope(Dispatchers.IO).launch {
                    trend2 = measures[0].regDate
                    val trendOne = measures.find { it.regDate == trend1 }
                    if (trendOne != null) {
                        avm.trends.add(trendOne)
                    }
                    val trendTwo = measures.find { it.regDate == trend2 }
                    if (trendTwo != null) {
                        avm.trends.add(trendTwo)
                    }
                    setImage(this@MeasureTrendDialogFragment, avm.rightMeasurement.value,0, binding.ssivMTDRight)

                    // ------#날짜 변경 감지 #------
                    withContext(Dispatchers.Main) {
                        setRightMeasurement()
                    }
                }
                // ------# left spinner 연결 #------
                val measureDates = measures.map { it.regDate.substring(0, 11) }.toMutableList()
                measureDates.add(0, "선택")

                binding.spnrMTDLeft.setPopupBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.color.white))
                binding.spnrMTDLeft.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, measureDates, true)
                binding.spnrMTDLeft.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            if (position != 0) {
                                setLeftMeasurement()
                                avm.rightMeasurement.value = measures[position]
                                avm.leftAnalyzes = transformAnalysis(avm.leftMeasurement.value?.measureResult!!)

                                setAdapter(avm.leftAnalyzes, avm.rightAnalyzes)
                            }
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }

                // ------# right spinner 연결 #------
                val measureDatesRight = measures.map { it.regDate.substring(0, 11) }.toMutableList()
                binding.spnrMTDRight.setPopupBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.color.white))

                binding.spnrMTDRight.adapter =
                    SpinnerAdapter(requireContext(), R.layout.item_spinner, measureDatesRight, true)
                binding.spnrMTDRight.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            avm.rightMeasurement.value = measures[position]
                            avm.rightAnalyzes = transformAnalysis(avm.rightMeasurement.value?.measureResult!!)
                            setAdapter(avm.leftAnalyzes, avm.rightAnalyzes)
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                // ------# trend #------
            }
            Log.v("AVM>Trend", "$avm")
        } catch (e: IllegalArgumentException) {
            Log.e("TrendError", "${e.printStackTrace()}")
        }

    }

    private fun setLeftMeasurement() {
        avm.leftMeasurement.observe(viewLifecycleOwner) {
            CoroutineScope(Dispatchers.IO).launch {
                setImage(this@MeasureTrendDialogFragment, avm.leftMeasurement.value,0, binding.ssivMTDLeft)
            }
        }
    }

    private fun setRightMeasurement() {
        avm.rightMeasurement.observe(viewLifecycleOwner) {
            CoroutineScope(Dispatchers.IO).launch {
                setImage(this@MeasureTrendDialogFragment, avm.rightMeasurement.value,0, binding.ssivMTDRight)
            }
        }
    }

    private fun setAdapter(analyzesLeft: MutableList<MutableList<AnalysisVO>>?, analyzesRight : MutableList<MutableList<AnalysisVO>>?) { // 부위별 > 3개 > 2개의 float
        // ------# rv #------

        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        val adapter = TrendRVAdapter(this@MeasureTrendDialogFragment, analyzesLeft, analyzesRight)
        binding.rvMTD.layoutManager = layoutManager
        binding.rvMTD.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    private fun showBalloon() {
        val balloon2 = Balloon.Builder(requireContext())
            .setWidthRatio(0.5f)
            .setHeight(BalloonSizeSpec.WRAP)
            .setText("비교 날짜를 선택해주세요")
            .setTextColorResource(R.color.white)
            .setTextSize(15f)
            .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            .setArrowSize(0)
            .setMargin(10)
            .setPadding(12)
            .setCornerRadius(8f)
            .setBackgroundColorResource(R.color.mainColor)
            .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        if (isFirstRun("MeasureTrendDialogFragment_isFirstRun")) {
            Handler(Looper.getMainLooper()).postDelayed({
                binding.spnrMTDLeft.showAlignEnd(balloon2)
                balloon2.dismissWithDelay(1800L)
            }, 700)
        }
        binding.textView30.setOnClickListener { it.showAlignBottom(balloon2) }
    }
    private val matchedUris = mapOf(
        "목관절" to listOf(0, 3, 4, 5, 6),
        "좌측 어깨" to listOf(0, 3, 5, 6),
        "우측 어깨" to listOf(0, 4, 5, 6),
        "좌측 팔꿉" to listOf(0, 2, 3),
        "우측 팔꿉" to listOf(0, 2, 4),
        "좌측 손목" to listOf(0, 2, 3),
        "우측 손목" to listOf(0, 2, 4),
        "좌측 골반" to listOf(0, 3, 5, 6),
        "우측 골반" to listOf(0, 4, 5, 6),
        "좌측 무릎" to listOf(0, 5),
        "우측 무릎" to listOf(0, 5),
        "좌측 발목" to listOf(0, 5),
        "우측 발목" to listOf(0, 5)
    )

    private val errorBounds = listOf(
        mapOf(
            0 to mapOf( "front_horizontal_angle_ear" to Pair(-0.5f, 0.5f)),
            3 to mapOf( "side_left_vertical_angle_nose_shoulder" to Pair(70f, 80f)),
            4 to mapOf( "side_right_vertical_angle_nose_shoulder" to Pair(70f, 80f)),
            5 to mapOf( "back_vertical_angle_nose_center_shoulder" to Pair(-5f, 5f)),
            6 to mapOf( "back_sit_horizontal_angle_ear" to Pair(-0.5f, 0.5f),
                "back_sit_vertical_angle_right_shoulder_nose_left_shoulder" to Pair(40f, 50f))
        ),
        // 어깨
        mapOf(
            0 to mapOf("front_horizontal_angle_shoulder" to Pair(-2f, 2f),
                "front_horizontal_distance_sub_shoulder" to Pair(0.5f, 1.5f)),
            3 to mapOf("side_left_horizontal_distance_shoulder" to Pair(-2f, 2f),
                "side_left_vertical_angle_shoulder_elbow_wrist" to Pair(-5f, 5f)),
            5 to mapOf("back_vertical_angle_shoudler_center_hip" to Pair(-2f, 2f)),
            6 to mapOf("back_sit_vertical_angle_shoulder_center_hip" to Pair(85f, 95f))
        ),
        mapOf(
            0 to mapOf("front_horizontal_angle_shoulder" to Pair(-2f, 2f),
                "front_horizontal_distance_sub_shoulder" to Pair(0.5f, 1.5f)),
            4 to mapOf("side_right_horizontal_distance_shoulder" to Pair(-2f, 2f),
                "side_right_vertical_angle_shoulder_elbow_wrist" to Pair(-5f, 5f)),
            5 to mapOf("back_vertical_angle_shoudler_center_hip" to Pair(-2f, 2f)),
            6 to mapOf("back_sit_vertical_angle_shoulder_center_hip" to Pair(85f, 95f))
        ),
        // 좌측 팔꿉
        mapOf(
            0 to mapOf("front_horizontal_angle_elbow" to Pair(-1f, 1f),
                "front_horizontal_distance_sub_elbow" to Pair(0.5f, 1.5f),
                "front_vertical_angle_shoulder_elbow_right" to Pair(-5f, 5f),
                "front_vertical_angle_elbow_wrist" to Pair(85f, 95f)),
            2 to mapOf("front_elbow_align_angle_left_upper_elbow_elbow_wrist" to Pair(25f, 35f),
                "front_elbow_align_angle_left_shoulder_elbow_wrist" to Pair(8f, 18f)),
            3 to mapOf("side_left_vertical_angle_shoulder_elbow" to Pair(0f, 5f),
                "side_left_vertical_angle_elbow_wrist" to Pair(85f, 105f),
                "side_left_vertical_angle_shoulder_elbow_wrist" to Pair(160f, 180f))
        ),
        mapOf(
            0 to mapOf("front_horizontal_angle_elbow" to Pair(40f, 50f),
                "front_horizontal_distance_sub_elbow" to Pair(40f, 50f),
                "front_vertical_angle_shoulder_elbow_left" to Pair(40f, 50f),
                "front_vertical_angle_elbow_wrist" to Pair(165f, 175f)),
            2 to mapOf("front_elbow_align_angle_right_upper_elbow_elbow_wrist" to Pair(25f, 35f),
                "front_elbow_align_angle_right_shoulder_elbow_wrist" to Pair(8f, 18f)),
            4 to mapOf("side_right_vertical_angle_shoulder_elbow" to Pair(0f, 5f),
                "side_right_vertical_angle_elbow_wrist" to Pair(85f, 105f),
                "side_right_vertical_angle_shoulder_elbow_wrist" to Pair(160f, 180f))
        ),
        // 좌측 손목
        mapOf(
            0 to mapOf("front_vertical_angle_elbow_wrist_left" to Pair(-5f, 5f),
                "front_horizontal_angle_wrist" to Pair(-0.5f, 0.5f)),
            2 to mapOf("front_elbow_align_angle_mid_index_wrist_elbow_left" to Pair(170f, 190f),
                "front_elbow_align_distance_left_wrist_shoulder" to Pair(-10f, 10f)),
            3 to mapOf("side_left_horizontal_distance_wrist" to Pair(-10f, 10f))
        ),
        mapOf(
            0 to mapOf("front_vertical_angle_elbow_wrist_right" to Pair(-5f, 5f),
                "front_horizontal_angle_wrist" to Pair(-0.5f, 0.5f)),
            2 to mapOf("front_elbow_align_angle_mid_index_wrist_elbow_right" to Pair(170f, 190f),
                "front_elbow_align_distance_right_wrist_shoulder" to Pair(-10f, 10f)),
            4 to mapOf("side_right_horizontal_distance_wrist" to Pair(80f, 100f))
        ),
        // 좌측 골반
        mapOf(
            0 to mapOf("front_vertical_angle_hip_knee_left" to Pair(85f, 95f),
                "front_vertical_angle_hip_knee_ankle" to Pair(175f, 185f),
                "front_horizontal_angle_hip" to Pair(-2.5f, 2.5f)),
            3 to mapOf("side_left_vertical_angle_hip_knee" to Pair(85f, 95f),
                "side_left_horizontal_distance_hip" to Pair(-0.5f, 0.5f),
                "side_left_vertical_angle_hip_knee_ankle" to Pair(175f, 185f)),
            5 to mapOf("back_horizontal_angle_hip" to Pair(-0.5f, 0.5f)),
            6 to mapOf("back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder" to Pair(40f, 60f),
                "back_sit_vertical_angle_shoulder_center_hip" to Pair(-1f, 1f))
        ),
        mapOf(
            0 to mapOf("front_vertical_angle_hip_knee_right" to Pair(85f, 95f),
                "front_vertical_angle_hip_knee_ankle" to Pair(175f, 185f),
                "front_horizontal_angle_hip" to Pair(-2.5f, 2.5f)),
            4 to mapOf("side_right_vertical_angle_hip_knee" to Pair(40f, 50f),
                "side_right_horizontal_distance_hip" to Pair(40f, 50f),
                "side_right_vertical_angle_hip_knee_ankle" to Pair(40f, 50f)),
            5 to mapOf("back_horizontal_angle_hip" to Pair(-0.5f, 0.5f)),
            6 to mapOf("back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder" to Pair(40f, 60f),
                "back_sit_vertical_angle_shoulder_center_hip" to Pair(-1f, 1f))
        ),
        // 좌측 무릎
        mapOf(
            0 to mapOf("front_horizontal_angle_knee" to Pair(-0.5f, 0.5f),
                "front_horizontal_distance_knee_left" to Pair(-0.5f, 0.5f),
                "front_vertical_angle_hip_knee_ankle_left" to Pair(175f, 185f)),
            5 to mapOf("back_horizontal_angle_knee" to Pair(-0.5f, 0.5f),
                "back_horizontal_distance_knee_left" to Pair(-0.5f, 0.5f))
        ),
        mapOf(
            0 to mapOf("front_horizontal_angle_knee" to Pair(-0.5f, 0.5f),
                "front_horizontal_distance_knee_right" to Pair(-0.5f, 0.5f),
                "front_vertical_angle_hip_knee_ankle_right" to Pair(175f, 185f)),
            5 to mapOf("back_horizontal_angle_knee" to Pair(-0.5f, 0.5f),
                "back_horizontal_distance_knee_right" to Pair(-0.5f, 0.5f))
        ),
        // 좌측 발목
        mapOf(
            0 to mapOf("front_vertical_angle_knee_ankle_left" to Pair(85f, 95f),
                "front_horizontal_angle_ankle" to Pair(-0.5f, 0.5f),
                "front_horizontal_distance_ankle_left" to Pair(-0.5f, 0.5f)),
            5 to mapOf("back_horizontal_distance_sub_ankle" to Pair(5f, 15f),
                "back_horizontal_distance_ankle_left" to Pair(-0.5f, 0.5f))
        ),
        mapOf(
            0 to mapOf("front_vertical_angle_knee_ankle_right" to Pair(85f, 95f),
                "front_horizontal_angle_ankle" to Pair(-0.5f, 0.5f),
                "front_horizontal_distance_ankle_right" to Pair(-0.5f, 0.5f)),
            5 to mapOf("back_horizontal_distance_sub_ankle" to Pair(5f, 15f),
                "back_horizontal_distance_ankle_right" to Pair(-0.5f, 0.5f))
        )

    )


    private val mainPartSeqs = listOf(
        mapOf(
            0 to mapOf( "front_horizontal_angle_ear" to "양 귀 기울기"),
            3 to mapOf("side_left_vertical_angle_nose_shoulder" to "코와 좌측 어깨 기울기"),
            4 to mapOf("side_right_vertical_angle_nose_shoulder" to "코와 우측 어깨 기울기"),
            5 to mapOf("back_vertical_angle_nose_center_shoulder" to "어깨중심과 코 기울기"),
            6 to mapOf( "back_sit_horizontal_angle_ear" to "양 귀 기울기",
                "back_sit_vertical_angle_right_shoulder_nose_left_shoulder" to "우측 어깨-코-좌측 어깨 기울기")
        ),
        mapOf(
            0 to mapOf("front_horizontal_angle_shoulder" to "양 어깨 기울기",
                "front_horizontal_distance_sub_shoulder" to "양 어깨 높이 차"),
            3 to mapOf("side_left_horizontal_distance_shoulder" to "중심과 어깨 거리",
                "side_left_vertical_angle_shoulder_elbow_wrist" to "어깨-팔꿉-손목 기울기"),
            5 to mapOf("back_vertical_angle_shoudler_center_hip" to "골반중심과 어깨 기울기"),
            6 to mapOf("back_sit_vertical_angle_shoulder_center_hip" to "어깨와 골반중심 기울기")
        ),
        mapOf(
            0 to mapOf("front_horizontal_angle_shoulder" to "양 어깨 기울기",
                "front_horizontal_distance_sub_shoulder" to "양 어깨 높이 차"),
            4 to mapOf("side_right_horizontal_distance_shoulder" to "중심과 어깨 거리",
                "side_right_vertical_angle_shoulder_elbow_wrist" to "어깨-팔꿉-손목 기울기"),
            5 to mapOf("back_vertical_angle_shoudler_center_hip" to "골반중심과 어깨 기울기"),
            6 to mapOf("back_sit_vertical_angle_shoulder_center_hip" to "어깨와 골반중심 기울기")
        ),
        // 좌측 팔꿉
        mapOf(
            0 to mapOf("front_horizontal_angle_elbow" to "양 팔꿉 기울기",
                "front_horizontal_distance_sub_elbow" to "양 팔꿉 높이 차",
                "front_vertical_angle_shoulder_elbow_left" to "좌측 어깨와 팔꿉 기울기",
                "front_vertical_angle_elbow_wrist" to "팔꿉와 손목 기울기"),
            2 to mapOf("front_elbow_align_angle_left_upper_elbow_elbow_wrist" to "좌측 상완-팔꿉-손목 기울기",
                "front_elbow_align_angle_left_shoulder_elbow_wrist" to "좌측 어깨-팔꿈치-손목 기울기"),
            3 to mapOf("side_left_vertical_angle_shoulder_elbow" to "어깨와 팔꿉 기울기",
                "side_left_vertical_angle_elbow_wrist" to "팔꿉와 손목 기울기",
                "side_left_vertical_angle_shoulder_elbow_wrist" to "어깨-팔꿉-손목 기울기")
        ),
        mapOf(
            0 to mapOf("front_horizontal_angle_elbow" to "양 팔꿉 기울기",
                "front_horizontal_distance_sub_elbow" to "양 팔꿉 높이 차",
                "front_vertical_angle_shoulder_elbow_right" to "우측 어깨와 팔꿉 기울기",
                "front_vertical_angle_elbow_wrist" to "팔꿉와 손목 기울기"),
            2 to mapOf("front_elbow_align_angle_right_upper_elbow_elbow_wrist" to "좌측 상완-팔꿉-손목 기울기",
                "front_elbow_align_angle_right_shoulder_elbow_wrist" to "좌측 어깨-팔꿈치-손목 기울기"),
            4 to mapOf("side_right_vertical_angle_shoulder_elbow" to "어깨와 팔꿉 기울기",
                "side_right_vertical_angle_elbow_wrist" to "팔꿉와 손목 기울기",
                "side_right_vertical_angle_shoulder_elbow_wrist" to "어깨-팔꿉-손목 기울기")
        ),
        // 좌측 손목
        mapOf(
            0 to mapOf("front_vertical_angle_elbow_wrist_left" to "좌측 팔꿉와 손목 기울기",
                "front_horizontal_angle_wrist" to "양 손목 기울기"),
            2 to mapOf("front_elbow_align_angle_mid_index_wrist_elbow_left" to "좌측 중지-손목-팔꿉 기울기",
                "front_elbow_align_distance_left_wrist_shoulder" to "좌측 손목과 어깨 기울기"),
            3 to mapOf("side_left_horizontal_distance_wrist" to "중심과 좌측 손목 거리")
        ),
        mapOf(
            0 to mapOf("front_vertical_angle_elbow_wrist_right" to "우측 팔꿉와 손목 기울기",
                "front_horizontal_angle_wrist" to "양 손목 기울기"),
            2 to mapOf("front_elbow_align_angle_mid_index_wrist_elbow_right" to "우측 중지-손목-팔꿉 기울기",
                "front_elbow_align_distance_right_wrist_shoulder" to "우측 손목과 어깨 기울기"),
            4 to mapOf("side_right_horizontal_distance_wrist" to "중심과 우측 손목 거리")
        ),
        // 좌측 골반
        mapOf(
            0 to mapOf("front_vertical_angle_hip_knee_left" to "좌측 골반과 무릎 기울기",
                "front_vertical_angle_hip_knee_ankle" to "골반-무릎-발목 기울기",
                "front_horizontal_angle_hip" to "양 골반 기울기"),
            3 to mapOf("side_left_vertical_angle_hip_knee" to "좌측 골반과 무릎 기울기",
                "side_left_horizontal_distance_hip" to "중심과 좌측 골반 거리",
                "side_left_vertical_angle_hip_knee_ankle" to "좌측 골반-무릎-발목 기울기"),
            5 to mapOf("back_horizontal_angle_hip" to "양 골반 기울기"),
            6 to mapOf("back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder" to "좌측 어깨-골반중심-우측 어깨 기울기",
                "back_sit_vertical_angle_shoulder_center_hip" to "어깨와 골반중심 기울기",)
        ),
        mapOf(
            0 to mapOf("front_vertical_angle_hip_knee_right" to "우측 골반과 무릎 기울기",
                "front_vertical_angle_hip_knee_ankle" to "골반-무릎-발목 기울기",
                "front_horizontal_angle_hip" to "양 골반 기울기"),
            4 to mapOf("side_right_vertical_angle_hip_knee" to "우측 골반과 무릎 기울기",
                "side_right_horizontal_distance_hip" to "중심과 우측 골반 거리",
                "side_right_vertical_angle_hip_knee_ankle" to "우측 골반-무릎-발목 기울기"),
            5 to mapOf("back_horizontal_angle_hip" to "양 골반 기울기"),
            6 to mapOf("back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder" to "좌측 어깨-골반중심-우측 어깨 기울기",
                "back_sit_vertical_angle_shoulder_center_hip" to "어깨와 골반중심 기울기",)
        ),
        // 좌측 무릎 + 스쿼트
        mapOf(
            0 to mapOf("front_horizontal_angle_knee" to "양 무릎 기울기",
                "front_horizontal_distance_knee_left" to "중심에서 좌측 무릎 거리",
                "front_vertical_angle_hip_knee_ankle_left" to "좌측 골반-무릎-발목 기울기"),
            5 to mapOf("back_horizontal_angle_knee" to "양 무릎 기울기",
                "back_horizontal_distance_knee_left" to "중심에서 좌측 무릎 거리")
        ),
        mapOf(
            0 to mapOf("front_horizontal_angle_knee" to "양 무릎 기울기",
                "front_horizontal_distance_knee_right" to "중심에서 우측 무릎 거리",
                "front_vertical_angle_hip_knee_ankle_right" to "우측 골반-무릎-발목 기울기"),
            5 to mapOf("back_horizontal_angle_knee" to "양 무릎 기울기",
                "back_horizontal_distance_knee_right" to "중심에서 우측 무릎 거리")
        ),
        mapOf(
            0 to mapOf("front_vertical_angle_knee_ankle_left" to "좌측 무릎과 발목 기울기",
                "front_horizontal_angle_ankle" to "양 발목 기울기",
                "front_horizontal_distance_ankle_left" to "중심에서 좌측 발목 거리"),
            5 to mapOf("back_horizontal_distance_sub_ankle" to "양 발목 높이 차",
                "back_horizontal_distance_ankle_left" to "중심에서 좌측 발목 거리")
        ),
        mapOf(
            0 to mapOf("front_vertical_angle_knee_ankle_right" to "우측 무릎과 발목 기울기",
                "front_horizontal_angle_ankle" to "양 발목 기울기",
                "front_horizontal_distance_ankle_right" to "중심에서 우측 발목 거리"),
            5 to mapOf("back_horizontal_distance_sub_ankle" to "양 발목 높이 차",
                "back_horizontal_distance_ankle_right" to "중심에서 우측 발목 거리")
        )
    )

    private val matchedIndexs = listOf(
        "목관절" , "좌측 어깨", "우측 어깨", "좌측 팔꿉", "우측 팔꿉", "좌측 손목" , "우측 손목" , "좌측 골반", "우측 골반" , "좌측 무릎" , "우측 무릎" , "좌측 발목", "우측 발목"
    )
    private fun getAnalysisUnits(part: String, currentKey: Int, measureResult: JSONArray): MutableList<AnalysisUnitVO> {

        val result = mutableListOf<AnalysisUnitVO>()
        val partIndex = matchedIndexs.indexOf(part)

        // partIndex에 해당하는 mainPartSeqs와 errorBounds 가져오기
        val mainSeq = mainPartSeqs[partIndex]
        val errorBound = errorBounds[partIndex]
        if (currentKey != 1) {
            val jo = measureResult.getJSONObject(currentKey) // 지금 1이 껴있어서 (dynamic이 있어서 오류가 나옴)

            // 현재 key에 해당하는 데이터만 처리
            mainSeq[currentKey]?.forEach { (columnName, rawDataName) ->
                // errorBounds에서 해당하는 Pair 값 찾기
                val boundPair = errorBound[currentKey]?.get(columnName)

                if (boundPair != null) {
                    val data = jo.optDouble(columnName).toFloat()
                    result.add(
                        AnalysisUnitVO(
                            columnName = columnName,
                            rawDataName = rawDataName,
                            rawData = data,
                            rawDataBound = boundPair,
                            summary = "",
                            state = if (boundPair.first <= data && boundPair.second >= data) true else false
                        )
                    )
                }
            }
        } else {
//            val ja = measureResult.getJSONArray(currentKey) // 지금 1이 껴있어서 (dynamic이 있어서 오류가 나옴)
//            avm.dynamicJa = ja

            // 현재 key에 해당하는 데이터만 처리
//            mainSeq[currentKey]?.forEach { (columnName, rawDataName) ->
//                // errorBounds에서 해당하는 Pair 값 찾기
//                val boundPair = errorBound[currentKey]?.get(columnName)
//
//                if (boundPair != null) {
//                    val data = ja.opt(columnName).toFloat()
//                    result.add(
//                        AnalysisUnitVO(
//                            columnName = columnName,
//                            rawDataName = rawDataName,
//                            rawData = data,
//                            rawDataBound = boundPair,
//                            comment = "",
//                            state = if (boundPair.first <= data && boundPair.second >= data) true else false
//                        )
//                    )
//                }
//            }
        }
        return result
    }
    private fun transformAnalysis(ja : JSONArray) : MutableList<MutableList<AnalysisVO>> {
        val analyzes = mutableListOf<MutableList<AnalysisVO>>()
        matchedUris.forEach { (part, seqList) ->
            val relatedAnalyzes = mutableListOf<AnalysisVO>()

            seqList.forEachIndexed { index, i ->
                val analysisUnits = getAnalysisUnits(part, i, ja)
                val normalUnits = analysisUnits.filter { it.state }
                val isNormal = if (normalUnits.size > (analysisUnits.size - normalUnits.size )) true else false
                val analysisVO = AnalysisVO(
                    i,
                    "",
                    isNormal,
                    analysisUnits,
                    mvm.selectedMeasure?.fileUris!![i]
                )
                relatedAnalyzes.add(analysisVO)
            }
            analyzes.add(relatedAnalyzes)
        }
        return analyzes
    }
}