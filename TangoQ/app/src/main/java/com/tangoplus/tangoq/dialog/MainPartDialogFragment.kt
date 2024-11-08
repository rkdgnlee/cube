package com.tangoplus.tangoq.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.adapter.MainPartRVAdapter
import com.tangoplus.tangoq.data.AnalysisUnitVO
import com.tangoplus.tangoq.data.AnalysisVO
import com.tangoplus.tangoq.data.AnalysisViewModel
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.data.UserViewModel
import com.tangoplus.tangoq.databinding.FragmentMainPartDialogBinding
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs


class MainPartDialogFragment : DialogFragment() {
    lateinit var binding : FragmentMainPartDialogBinding
    val avm : AnalysisViewModel by activityViewModels()
    val mvm : MeasureViewModel by activityViewModels()
    private lateinit var part: String
    private lateinit var measureResult : JSONArray

    companion object {
        private const val ARG_PART = "arg_part"
        fun newInstance(part: String): MainPartDialogFragment {
            val fragment = MainPartDialogFragment()
            val args = Bundle()
            args.putString(ARG_PART, part)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainPartDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        part = arguments?.getString(ARG_PART) ?: ""
        Log.v("파트", "part: $part")
        binding.tvMPD.text = "$part 부위 분석"
        measureResult = mvm.selectedMeasure?.measureResult ?: JSONArray()
        // ------# 이번 part에서 쓸 column값들 가져오기 #------
        // 초기화 후 담기
        avm.relatedAnalyzes = mutableListOf()
        avm.selectedPart = part
        for (i in matchedUris.get(part)!!) {

            // ------# 부위와 평균값 넣기 #------
            val analysisUnits = getAnalysisUnits(part, i)
            Log.v("analysisUnits", "i: $i, $analysisUnits")
            val normalUnits = analysisUnits.filter { it.state }
            val isNormal = if (normalUnits.size > (analysisUnits.size - normalUnits.size )) true else false
            Log.v("isNormal", "trueSize: ${analysisUnits.filter { it.state }.size} vs falseSize: ${analysisUnits.size} isNormal: ${isNormal}")
            val analysisVO = AnalysisVO(
                i,
                createSummary(part, i, analysisUnits),
                isNormal,
                analysisUnits,
                mvm.selectedMeasure?.fileUris!![i]
            )
            avm.relatedAnalyzes.add(analysisVO)
        }

        Log.v("relatedAnalyzes", "${avm.relatedAnalyzes.size}, ${avm.relatedAnalyzes.map { it.seq }}")
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        val adapter = MainPartRVAdapter(this@MainPartDialogFragment, avm.relatedAnalyzes)
        adapter.avm = avm
        binding.rvMPD.layoutManager = layoutManager
        binding.rvMPD.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    private val matchedUris = mapOf(
        "목관절" to listOf(0, 3, 4, 5, 6),
        "좌측 어깨" to listOf(0, 1, 3, 5, 6),
        "우측 어깨" to listOf(0, 1, 4, 5, 6),
        "좌측 팔꿉" to listOf(0, 2, 3, 4),
        "우측 팔꿉" to listOf(0, 2, 3, 4),
        "좌측 손목" to listOf(0, 2, 3),
        "우측 손목" to listOf(0, 2, 4),
        "좌측 골반" to listOf(0, 1, 3, 5, 6),
        "우측 골반" to listOf(0, 1, 4, 5, 6),
        "좌측 무릎" to listOf(0, 1, 5),
        "우측 무릎" to listOf(0, 1, 5),
        "좌측 발목" to listOf(0, 1, 5),
        "우측 발목" to listOf(0, 1, 5)
    )

    private val matchedIndexs = listOf(
        "목관절" , "좌측 어깨", "우측 어깨", "좌측 팔꿉", "우측 팔꿉", "좌측 손목" , "우측 손목" , "좌측 골반", "우측 골반" , "좌측 무릎" , "우측 무릎" , "좌측 발목", "우측 발목"
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


    val mainPartSeqs = listOf(
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
            3 to mapOf("side_right_vertical_angle_hip_knee" to "우측 골반과 무릎 기울기",
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


    // 정상 범위 판단 -> 좌측 우측 기울기 + 거리가 더 먼것도 판단해야함 / 손목 > 정면 측정 >
    // 현재는 부위 -> 그냥
    private fun createSummary(part: String, seq: Int, units: MutableList<AnalysisUnitVO>): String {
        val resultString = StringBuilder()
        var hasAbnormalAngle = false
        var hasAbnormalPosition = false
        var hasAbnormalOutAngle = false
        var hasAbnormalForwardAngle = false

        var heightDirection = ""
        var angleDirection = ""
        var forwardDirection = ""
        var angleValue = 0f
        var forwardAngleValue = 0f
        fun isInNormalRange(unit: AnalysisUnitVO): Boolean {
            return unit.rawData in unit.rawDataBound.first..unit.rawDataBound.second
        }
        fun getDirection(value: Float): String {
            return when {
                value > 0 -> "우측"
                value < 0 -> "좌측"
                else -> "중앙"
            }
        }
        fun getHeightDirection(value: Float): String {
            return when {
                value > 0 -> "위쪽"
                value < 0 -> "아래쪽"
                else -> "동일한 높이"
            }
        }
        fun getForwardDirection(value: Float): String {
            return when {
                value > 70 -> "정상"
                else -> "앞쪽"
            }
        }
        when (part) {
            "목관절" -> {
                when (seq) {
                    3 -> {
                        val frontNeckData = units.find { it.columnName == "side_left_vertical_angle_nose_shoulder"}
                        frontNeckData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalForwardAngle = true
                                forwardAngleValue = it.rawData
                            }
                        }
                    }
                    6 -> {
                        val frontNeckData1 = units.find { it.columnName == "back_sit_vertical_angle_right_shoulder_nose_left_shoulder"}
                        frontNeckData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalAngle = true
                                angleValue = it.rawData
                                angleDirection = getDirection(it.rawData)
                            }
                        }
                    }
                }
                if (hasAbnormalForwardAngle && hasAbnormalAngle) {
                    resultString.append("거북목 의심 - 목이 ${String.format("%.2f", abs(forwardAngleValue))}° 기울어져 있습니다. 또한,  ${angleDirection}방향으로 틀어져 있습니다. 바른 자세와 목 스트레칭을 추천드립니다.")
                } else if (hasAbnormalForwardAngle) {
                    resultString.append("거북목 의심 - 목이 ${String.format("%.2f", abs(forwardAngleValue))}° 기울어져 있습니다. 목 스트레칭을 권장드립니다.")
                } else if (hasAbnormalAngle) {
                    resultString.append("목이 ${angleDirection}방향으로 ${angleValue}° 틀어져있습니다. 바른 자세를 권장드립니다.")
                }
            }

            "좌측 어깨" -> {
                when (seq) {
                    0 -> {
                        val shoulderData = units.find { it.columnName == "front_horizontal_angle_shoulder"}
                        shoulderData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalAngle = true
                                angleValue = it.rawData
                            }
                        }
                    }
                    3 -> {
                        val shoulderData = units.find { it.columnName == "side_left_horizontal_distance_shoulder"}
                        shoulderData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalForwardAngle = true
                                forwardAngleValue = it.rawData
                                forwardDirection = getForwardDirection(it.rawData)
                            }
                        }
                    }
                    6 -> {
                        val shoulderData = units.find { it.columnName == "back_sit_vertical_angle_shoulder_center_hip"}
                        shoulderData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalOutAngle = true
                            }
                        }
                    }
                }
                if (hasAbnormalAngle && hasAbnormalForwardAngle && hasAbnormalOutAngle) {
                    resultString.append("어깨가 ${heightDirection} 방향으로 ${String.format("%.2f", abs(angleValue))}° 기울어져있고, 라운드 숄더가 의심됩니다. 등 스트레칭을 통해 날개뼈를 펼치고 틀어진 방향 반대쪽으로 스트레칭을 추천드립니다")
                } else if (hasAbnormalAngle && hasAbnormalForwardAngle) {
                    resultString.append("어깨가 ${heightDirection} 방향으로 ${String.format("%.2f", abs(angleValue))}° 기울어져있고, 라운드 숄더가 의심됩니다. 등 스트레칭을 통해 날개뼈를 펼치고 틀어진 방향 반대쪽으로 스트레칭을 추천드립니다")
                } else if (hasAbnormalForwardAngle) {
                    resultString.append("어깨는 기울어지지 않았지만 라운드 숄더가 의심됩니다. 등 스트레칭을 권장합니다.")
                }
            }
            "우측 어깨" -> {
                when (seq) {
                    0 -> {
                        val shoulderData = units.find { it.columnName == "front_horizontal_angle_shoulder"}
                        shoulderData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalAngle = true
                                angleValue = it.rawData
                            }
                        }
                    }
                    3 -> {
                        val shoulderData = units.find { it.columnName == "side_right_horizontal_distance_shoulder"}
                        shoulderData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalForwardAngle = true
                                forwardAngleValue = it.rawData
                                forwardDirection = getForwardDirection(it.rawData)
                            }
                        }
                    }
                    6 -> {
                        val shoulderData = units.find { it.columnName == "back_sit_vertical_angle_shoulder_center_hip"}
                        shoulderData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalOutAngle = true
                            }
                        }
                    }
                }
                if (hasAbnormalAngle && hasAbnormalForwardAngle && hasAbnormalOutAngle) {
                    resultString.append("어깨가 ${heightDirection} 방향으로 ${String.format("%.2f", abs(angleValue))}° 기울어져있고, 라운드 숄더가 의심됩니다. 등 스트레칭을 통해 날개뼈를 펼치고 틀어진 방향 반대쪽으로 스트레칭을 추천드립니다")
                } else if (hasAbnormalAngle && hasAbnormalForwardAngle) {
                    resultString.append("어깨가 ${heightDirection} 방향으로 ${String.format("%.2f", abs(angleValue))}° 기울어져있고, 라운드 숄더가 의심됩니다. 등 스트레칭을 통해 날개뼈를 펼치고 틀어진 방향 반대쪽으로 스트레칭을 추천드립니다")
                } else if (hasAbnormalForwardAngle) {
                    resultString.append("어깨는 기울어지지 않았지만 라운드 숄더가 의심됩니다. 등 스트레칭을 권장합니다.")
                }
            }
            "좌측 팔꿉"-> {

                when (seq) {
                    0 -> {
                        val frontElbowData1 = units.find { it.columnName == "front_horizontal_angle_elbow"}
                        val frontElbowData2 = units.find { it.columnName == "front_horizontal_distance_sub_elbow"}
                        val frontElbowData3 = units.find { it.columnName == "front_vertical_angle_shoulder_elbow_left"}
//                        val frontElbowData4 = units.find { it.columnName == "front_vertical_angle_elbow_wrist"}
                        frontElbowData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalAngle = true
                                angleValue = it.rawData
                                heightDirection = getHeightDirection(it.rawData)
                            }
                        }
                        frontElbowData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalPosition = true
                                angleDirection = getDirection(it.rawData)
                            }
                        }
                        frontElbowData3?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalOutAngle = true
                            }
                        }
//                        frontElbowData4?.let {
//                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
//                                hasAbnormalAngle = true
//                                angleValue = it.rawData
//                                heightDirection = getHeightDirection(it.rawData)
//                            }
//                        }
                    }
                    2 -> {
                        val frontElbowData1 = units.find { it.columnName == "front_elbow_align_angle_left_upper_elbow_elbow_wrist"}
//                        val frontElbowData2 = units.find { it.columnName == "front_elbow_align_angle_left_shoulder_elbow_wrist"}
                        frontElbowData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalAngle = true
                                angleValue = it.rawData
                                angleDirection = getDirection(it.rawData)
                            }
                        }
//                        frontElbowData2?.let {
//                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
//                                hasAbnormalPosition = true
//                            }
//                        }
                    }
                    3 -> {
//                        val sideElbowData1 = units.find { it.columnName == "side_left_vertical_angle_shoulder_elbow"}
//                        val sideElbowData2 = units.find { it.columnName == "side_left_vertical_angle_elbow_wrist"}
                        val sideElbowData3 = units.find { it.columnName == "side_left_vertical_angle_shoulder_elbow_wrist"}
                        sideElbowData3?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalAngle = true
                                angleValue = it.rawData
                                angleDirection = getDirection(it.rawData)
                            }
                        }
                    }
                }
                if (hasAbnormalAngle && hasAbnormalPosition && hasAbnormalOutAngle) {
                    resultString.append("좌측 팔꿉이 우측보다 ${heightDirection}에 위치해 있으며(${String.format("%.2f", abs(angleValue))}°), 팔이 바깥쪽으로 벌어져 있습니다. 주변 근육이 긴장된 상태입니다")
                } else if (hasAbnormalAngle && hasAbnormalPosition) {
                    resultString.append("좌측 팔꿉이 우측과 균형이 맞지않습니다. 어깨, 상완의 근육이나 자주 사용하는 부분의 긴장을 풀어주세요")
                } else if (hasAbnormalOutAngle) {
                    resultString.append("좌측 팔꿉이 우측보다 ${angleDirection}에 위치해 있습니다(${String.format("%.2f", abs(angleValue))}°). ")
                    resultString.append("평소 팔꿉 주변 근육의 긴장상태를 주의하여 관리하세요.")
                } else {
                    resultString.append("좌측 팔꿉의 위치와 각도가 정상 범위 내에 있습니다.")
                }
            }
            "우측 팔꿉" -> {
                when (seq) {
                    0 -> {
                        val frontElbowData1 = units.find { it.columnName == "front_horizontal_angle_elbow"}
                        val frontElbowData2 = units.find { it.columnName == "front_horizontal_distance_sub_elbow"}
                        val frontElbowData3 = units.find { it.columnName == "front_vertical_angle_shoulder_elbow_right"}
//                        val frontElbowData4 = units.find { it.columnName == "front_vertical_angle_elbow_wrist"}
                        frontElbowData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalAngle = true
                                angleValue = it.rawData
                                heightDirection = getHeightDirection(it.rawData)
                            }
                        }
                        frontElbowData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalPosition = true
                                angleDirection = getDirection(it.rawData)
                            }
                        }
                        frontElbowData3?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalOutAngle = true
                            }
                        }
//                        frontElbowData4?.let {
//                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
//                                hasAbnormalAngle = true
//                                angleValue = it.rawData
//                                heightDirection = getHeightDirection(it.rawData)
//                            }
//                        }
                    }
                    2 -> {
                        val frontElbowData1 = units.find { it.columnName == "front_elbow_align_angle_right_upper_elbow_elbow_wrist"}
//                        val frontElbowData2 = units.find { it.columnName == "front_elbow_align_angle_right_shoulder_elbow_wrist"}
                        frontElbowData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalAngle = true
                                angleValue = it.rawData
                                angleDirection = getDirection(it.rawData)
                            }
                        }
//                        frontElbowData2?.let {
//                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
//                                hasAbnormalPosition = true
//                            }
//                        }
                    }
                    4 -> {
//                        val sideElbowData1 = units.find { it.columnName == "side_right_vertical_angle_shoulder_elbow"}
//                        val sideElbowData2 = units.find { it.columnName == "side_right_vertical_angle_elbow_wrist"}
                        val sideElbowData3 = units.find { it.columnName == "side_right_vertical_angle_shoulder_elbow_wrist"}
                        sideElbowData3?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalAngle = true
                                angleValue = it.rawData
                                angleDirection = getDirection(it.rawData)
                            }
                        }
                    }
                }
                if (hasAbnormalAngle && hasAbnormalPosition && hasAbnormalOutAngle) {
                    resultString.append("좌측 팔꿉이 우측보다 ${heightDirection}에 위치해 있으며(${String.format("%.2f", abs(angleValue))}°), 팔이 바깥쪽으로 벌어져 있습니다. 주변 근육이 긴장된 상태입니다")
                    resultString.append("평소 팔꿉 주변 근육의 긴장상태를 주의하여 관리하세요.")
                } else if (hasAbnormalAngle && hasAbnormalPosition) {
                    resultString.append("좌측 팔꿉이 우측과 균형이 맞지않습니다. 어깨, 상완의 근육이나 자주 사용하는 부분의 긴장을 풀어주세요")
                } else if (hasAbnormalOutAngle) {
                    resultString.append("좌측 팔꿉이 우측보다 ${angleDirection}에 위치해 있습니다(${String.format("%.2f", abs(angleValue))}°)")
                    resultString.append("평소 팔꿉 주변 근육의 긴장상태를 주의하여 관리하세요.")
                } else {
                    resultString.append("좌측 팔꿉의 위치와 각도가 정상 범위 내에 있습니다.")
                }
            }
            "좌측 손목" -> {
                when (seq) {
                    0 -> {
                        val frontWristData1 = units.find { it.columnName == "front_vertical_angle_elbow_wrist_left"}
                        val frontWristData2 = units.find { it.columnName == "front_horizontal_angle_wrist"}
                        frontWristData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalAngle = true
                                angleValue = it.rawData

                            }
                        }
                        frontWristData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalAngle = true

                            }
                        }
                    }
                    2 -> {
                        val frontWristData1 = units.find { it.columnName == "front_elbow_align_angle_mid_index_wrist_elbow_left"}
                        val frontWristData2 = units.find { it.columnName == "front_elbow_align_distance_left_wrist_shoulder"}
                        frontWristData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalAngle = true
                                angleValue = it.rawData
                            }
                        }
                        frontWristData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalPosition = true
                                angleValue = it.rawData
                            }
                        }
                    }
                    4 -> {
                        val frontWristData1 = units.find { it.columnName == "side_left_horizontal_distance_wrist"}
                        frontWristData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalForwardAngle = true
                                forwardDirection = getForwardDirection(it.rawData)
                                forwardAngleValue = it.rawData
                            }
                        }
                    }
                }
                if (hasAbnormalAngle && hasAbnormalForwardAngle ) {
                    resultString.append("우측 손목이 좌측 손목보다 ${heightDirection}에 위치해 있으며, 반대 손목과 높이가 다릅니다")
                    resultString.append("평소 손목 주변 근육의 긴장상태를 주의하여 관리하세요.")
                } else if (hasAbnormalAngle) {
                    resultString.append("우측 손목의 각도가 ${String.format("%.2f", abs(angleValue))}°만큼 기울었습니다.")
                    resultString.append("평소 손목 주변 근육의 긴장상태를 주의하여 관리하세요.")
                } else if (hasAbnormalForwardAngle) {
                    resultString.append("우측 손목이 중심에서 ${forwardAngleValue} 만큼 벌어져 있습니다. 상완 근육과 어깨의 긴장을 주목해주세요")
                } else {
                    resultString.append("우측 손목의 위치와 각도가 정상 범위 내에 있습니다.")
                }

            }
            "우측 손목" -> {
                when (seq) {
                    0 -> {
                        val frontWristData1 = units.find { it.columnName == "front_vertical_angle_elbow_wrist_right"}
                        val frontWristData2 = units.find { it.columnName == "front_horizontal_angle_wrist"}
                        frontWristData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalAngle = true
                                angleValue = it.rawData

                            }
                        }
                        frontWristData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalAngle = true

                            }
                        }
                    }
                    2 -> {
                        val frontWristData1 = units.find { it.columnName == "front_elbow_align_angle_mid_index_wrist_elbow_right"}
                        val frontWristData2 = units.find { it.columnName == "front_elbow_align_distance_right_wrist_shoulder"}
                        frontWristData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalAngle = true
                                angleValue = it.rawData
                            }
                        }
                        frontWristData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalPosition = true
                                angleValue = it.rawData
                            }
                        }
                    }
                    4 -> {
                        val frontWristData1 = units.find { it.columnName == "side_right_horizontal_distance_wrist"}
                        frontWristData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalForwardAngle = true
                                forwardDirection = getForwardDirection(it.rawData)
                            }
                        }
                    }
                }
                if (hasAbnormalAngle && hasAbnormalForwardAngle ) {
                    resultString.append("우측 손목이 좌측 손목보다 ${heightDirection}에 위치해 있으며, 반대 손목과 높이가 다릅니다")
                    resultString.append("평소 손목 주변 근육의 긴장상태를 주의하여 관리하세요.")
                } else if (hasAbnormalAngle) {
                    resultString.append("우측 손목의 각도가 ${String.format("%.2f", abs(angleValue))}°만큼 기울었습니다.")
                    resultString.append("평소 손목 주변 근육의 긴장상태를 주의하여 관리하세요.")
                } else if (hasAbnormalForwardAngle) {
                    resultString.append("우측 손목이 중심에서 ${String.format("%.2f", abs(forwardAngleValue))}° 만큼 벌어져 있습니다. 상완 근육과 어깨의 긴장을 주목해주세요")
                } else {
                    resultString.append("우측 손목의 위치와 각도가 정상 범위 내에 있습니다.")
                }

            }
            "좌측 골반" -> {
                when (seq) {
                    3 -> {
                        val hipData = units.find { it.columnName == "side_left_horizontal_distance_hip"}
                        hipData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalForwardAngle = true
                                forwardAngleValue = it.rawData
                                forwardDirection = getHeightDirection(it.rawData)
                            }
                        }
                    }
                    5 -> {
                        val hipData = units.find { it.columnName == "back_horizontal_angle_hip"}
                        hipData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalAngle = true
                                angleValue = it.rawData
                                angleDirection = getDirection(it.rawData)
                            }
                        }
                    }
                    6 -> {
                        val hipData = units.find { it.columnName == "back_sit_vertical_angle_shoulder_center_hip"}
                        hipData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalAngle = true
                            }
                        }
                    }
                }
                if (hasAbnormalForwardAngle && hasAbnormalAngle) {
                    resultString.append("골반이 ${forwardAngleValue}cm 중심과 차이가 있습니다. 또한 골반이 ${angleDirection} 방향으로 ${angleValue}° 기울어져있습니다.")
                    resultString.append("엉덩이와 허벅지 뒤쪽을 스트레칭하고, 균형을 위한 골반 교정 스트레칭을 추천드립니다")
                } else if (hasAbnormalForwardAngle) {
                    resultString.append("골반이 ${forwardAngleValue}cm 중심과 차이가 있습니다.")
                    resultString.append("엉덩이와 허벅지 뒤쪽을 스트레칭하고, 코어 근육을 강화하는 운동을 해보세요.")
                }  else if (hasAbnormalAngle) {
                    resultString.append("골반이 ${angleDirection} 방향으로 ${angleValue}° 기울어져있습니다.")
                    resultString.append("허리와 엉덩이 근육을 균형 있게 풀어주는 스트레칭을 해보세요. ")
                }
            }
            "우측 골반" -> {
                when (seq) {
                    4 -> {
                        val hipData = units.find { it.columnName == "side_right_horizontal_distance_hip"}
                        hipData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalForwardAngle = true
                                forwardAngleValue = it.rawData
                                forwardDirection = getHeightDirection(it.rawData)
                            }
                        }
                    }
                    5 -> {
                        val hipData = units.find { it.columnName == "back_horizontal_angle_hip"}
                        hipData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalAngle = true
                                angleValue = it.rawData
                                angleDirection = getDirection(it.rawData)
                            }
                        }
                    }
                    6 -> {
                        val hipData = units.find { it.columnName == "back_sit_vertical_angle_shoulder_center_hip"}
                        hipData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalAngle = true
                            }
                        }
                    }
                }
                if (hasAbnormalForwardAngle && hasAbnormalAngle) {
                    resultString.append("골반이 ${forwardAngleValue}cm 중심과 차이가 있습니다. 또한 골반이 ${angleDirection} 방향으로 ${angleValue}° 기울어져있습니다.")
                    resultString.append("엉덩이와 허벅지 뒤쪽을 스트레칭하고, 균형을 위한 골반 교정 스트레칭을 추천드립니다")
                } else if (hasAbnormalForwardAngle) {
                    resultString.append("골반이 ${forwardAngleValue}cm 중심과 차이가 있습니다.")
                    resultString.append("엉덩이와 허벅지 뒤쪽을 스트레칭하고, 코어 근육을 강화하는 운동을 해보세요.")
                }  else if (hasAbnormalAngle) {
                    resultString.append("골반이 ${angleDirection} 방향으로 ${angleValue}° 기울어져있습니다.")
                    resultString.append("허리와 엉덩이 근육을 균형 있게 풀어주는 스트레칭을 해보세요. ")
                }
            }

            "좌측 무릎" -> {
                when (seq) {
                    0 -> {
                        val kneeData1 = units.find { it.columnName == "front_horizontal_angle_knee"}
                        val kneeData2 = units.find { it.columnName == "front_horizontal_distance_knee_left"}

                        kneeData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalAngle = true
                                angleValue = it.rawData
                                angleDirection = getDirection(it.rawData)
                            }
                        }
                        kneeData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalPosition = true
                                angleDirection = getDirection(it.rawData)
                            }
                        }
                    }
                }
                if (hasAbnormalAngle && hasAbnormalPosition) {
                    resultString.append("무릎이 ${angleDirection}방향으로 ${angleValue}° 기울어져 있습니다.")
                    resultString.append("허벅지와 엉덩이 근육을 강화하는 스트레칭을 해보세요.")
                } else if (hasAbnormalAngle) {
                    resultString.append("좌측 무릎이 ${forwardAngleValue}cm 중심과 차이가 있습니다.")
                    resultString.append("무릎 안정성을 높이는 운동을 추천합니다.")
                }  else if (hasAbnormalPosition) {
                    resultString.append("무릎이 ${angleDirection} 방향으로 ${angleValue}° 기울어져있습니다.")
                    resultString.append("허벅지와 엉덩이 근육을 강화하는 스트레칭을 해보세요.")
                }
            }
            "우측 무릎" -> {
                when (seq) {
                    0 -> {
                        val kneeData1 = units.find { it.columnName == "front_horizontal_angle_knee"}
                        val kneeData2 = units.find { it.columnName == "front_horizontal_distance_knee_left"}

                        kneeData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalAngle = true
                                angleValue = it.rawData
                                angleDirection = getDirection(it.rawData)
                            }
                        }
                        kneeData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalPosition = true
                                angleDirection = getDirection(it.rawData)
                            }
                        }
                    }
                }
                if (hasAbnormalAngle && hasAbnormalPosition) {
                    resultString.append("무릎이 ${angleDirection}방향으로 ${angleValue}° 기울어져 있습니다.")
                    resultString.append("허벅지와 엉덩이 근육을 강화하는 스트레칭을 해보세요.")
                } else if (hasAbnormalAngle) {
                    resultString.append("좌측 무릎이 ${forwardAngleValue}cm 중심과 차이가 있습니다.")
                    resultString.append("무릎 안정성을 높이는 운동을 추천합니다.")
                }  else if (hasAbnormalPosition) {
                    resultString.append("무릎이 ${angleDirection} 방향으로 ${angleValue}° 기울어져있습니다.")
                    resultString.append("허벅지와 엉덩이 근육을 강화하는 스트레칭을 해보세요.")
                }
            }

            "좌측 발목"-> {
                when (seq) {
                    0 -> {
                        val ankleData = units.find { it.columnName == "front_horizontal_angle_ankle"}
                        ankleData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalAngle = true
                                angleValue = it.rawData
                                angleDirection = getDirection(it.rawData)
                            }
                        }
                    }
                    5 -> {
                        val ankleData = units.find { it.columnName == "back_horizontal_distance_sub_ankle"}
                        ankleData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalPosition = true
                                angleDirection = getDirection(it.rawData)
                                forwardAngleValue = it.rawData
                            }
                        }
                    }

                }
                if (hasAbnormalAngle && hasAbnormalPosition) {
                    resultString.append("발목이 ${angleDirection} 방향으로 ${angleValue}° 기울고 ${forwardAngleValue}cm만큼 ${angleDirection}방향으로 틀어져있습니다.")
                    resultString.append("발목틀어짐을 위해 골반, 무릎, 서있는 자세를 종합적으로 관리하길 권장드립니다.")
                } else if (hasAbnormalPosition) {
                    resultString.append("골반이 ${forwardAngleValue}cm ${angleDirection}방향으로 틀어져있습니다.")
                    resultString.append("엉덩이와 허벅지 뒤쪽을 스트레칭하고, 코어 근육을 강화하는 운동을 해보세요.")
                }  else if (hasAbnormalAngle) {
                    resultString.append("골반이 ${angleDirection} 방향으로 ${angleValue}° 기울어져있습니다.")
                    resultString.append("허리와 엉덩이 근육을 균형 있게 풀어주는 스트레칭을 해보세요. ")
                }
            }
            "우측 발목" -> {
                when (seq) {
                    0 -> {
                        val ankleData = units.find { it.columnName == "front_horizontal_angle_ankle"}
                        ankleData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalAngle = true
                                angleValue = it.rawData
                                angleDirection = getDirection(it.rawData)
                            }
                        }
                    }
                    5 -> {
                        val ankleData = units.find { it.columnName == "back_horizontal_distance_sub_ankle"}
                        ankleData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                hasAbnormalPosition = true
                                angleDirection = getDirection(it.rawData)
                                forwardAngleValue = it.rawData
                            }
                        }
                    }

                }
                if (hasAbnormalAngle && hasAbnormalPosition) {
                    resultString.append("발목이 ${angleDirection} 방향으로 ${angleValue}° 기울고 ${forwardAngleValue}cm만큼 ${angleDirection}방향으로 틀어져있습니다.")
                    resultString.append("발목틀어짐을 위해 골반, 무릎, 서있는 자세를 종합적으로 관리하길 권장드립니다.")
                } else if (hasAbnormalPosition) {
                    resultString.append("골반이 ${forwardAngleValue}cm ${angleDirection}방향으로 틀어져있습니다.")
                    resultString.append("엉덩이와 허벅지 뒤쪽을 스트레칭하고, 코어 근육을 강화하는 운동을 해보세요.")
                }  else if (hasAbnormalAngle) {
                    resultString.append("골반이 ${angleDirection} 방향으로 ${angleValue}° 기울어져있습니다.")
                    resultString.append("허리와 엉덩이 근육을 균형 있게 풀어주는 스트레칭을 해보세요. ")
                }
            }
        }

        return if (resultString.isEmpty()) "정상 범위 내에 있습니다." else resultString.toString()
    }



    private fun getAnalysisUnits(part: String, currentKey: Int): MutableList<AnalysisUnitVO> {

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
}