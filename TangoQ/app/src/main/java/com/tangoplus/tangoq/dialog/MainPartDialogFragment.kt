package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
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
import com.tangoplus.tangoq.viewmodel.AnalysisViewModel
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMainPartDialogBinding
import com.tangoplus.tangoq.function.MeasurementManager.getAnalysisUnits
import org.json.JSONArray
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
            val analysisUnits = getAnalysisUnits(part, i, measureResult)
            Log.v("analysisUnits", "i: $i, $analysisUnits")
            val normalUnits = analysisUnits.filter { it.state }
            val isNormal = if (i == 1) { true } else if (normalUnits.size > (analysisUnits.size - normalUnits.size )) true else false

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
        "좌측 팔꿉" to listOf(0, 2, 3),
        "우측 팔꿉" to listOf(0, 2, 4),
        "좌측 손목" to listOf(0, 2, 3),
        "우측 손목" to listOf(0, 2, 4),
        "좌측 골반" to listOf(0, 1, 3, 5, 6),
        "우측 골반" to listOf(0, 1, 4, 5, 6),
        "좌측 무릎" to listOf(0, 1, 5),
        "우측 무릎" to listOf(0, 1, 5),
        "좌측 발목" to listOf(0, 1, 5),
        "우측 발목" to listOf(0, 1, 5)
    )

    // 정상 범위 판단 -> 좌측 우측 기울기 + 거리가 더 먼것도 판단해야함 / 손목 > 정면 측정 >
    // 현재는 부위 -> 그냥
    @SuppressLint("DefaultLocale")
    private fun createSummary(part: String, seq: Int, units: MutableList<AnalysisUnitVO>): String {
        val resultString = StringBuilder()

        fun getDirection(value: Float): String {
            return when {
                value > 0 -> "우측"
                value < 0 -> "좌측"
                else -> "중앙"
            }
        }

        fun getForwardDirection(value: Float): String {
            return when {
                value > 70 -> "정상"
                else -> "앞쪽"
            }
        }
        if (seq == 1) {
            resultString.append("스쿼트 정보를 확인하세요")
        }
        when (part) {
            "목관절" -> {
                when (seq) {
                    0 -> {
                        val frontNeckData = units.find { it.columnName == "front_horizontal_angle_ear"}
                        var angleData = 0f
                        var angleDirection = ""
                        frontNeckData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData = it.rawData
                                angleDirection = getDirection(it.rawData)
                                resultString.append("머리가 $angleDirection 방향으로 ${String.format("%.2f", abs(angleData))}° 치우쳐 있습니다. 바른 자세와 목근육 긴장을 수시로 이완해주세요. ")
                            }
                        }
                    }
                    3 -> {
                        val frontNeckData = units.find { it.columnName == "side_left_vertical_angle_nose_shoulder"}
                        var angleData = 0f
                        var angleDirection = ""
                        frontNeckData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData = it.rawData
                                angleDirection = getDirection(it.rawData)
                                resultString.append("거북목 의심 - 목이 ${String.format("%.2f", abs(angleData))}° ${angleDirection}방향으로 틀어져 있습니다. 바른 자세와 목 스트레칭을 추천드립니다. ")
                            }
                        }
                    }
                    4 -> {
                        val frontNeckData = units.find { it.columnName == "side_right_vertical_angle_nose_shoulder"}
                        var angleData = 0f
                        var angleDirection = ""
                        frontNeckData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData = it.rawData
                                angleDirection = getDirection(it.rawData)
                                resultString.append("거북목 의심 - 목이 ${String.format("%.2f", abs(angleData))}° ${angleDirection}방향으로 틀어져 있습니다. 바른 자세와 목 스트레칭을 추천드립니다. ")
                            }
                        }
                    }
                    6 -> {

                        val frontNeckData = units.find { it.columnName == "back_sit_vertical_angle_right_shoulder_nose_left_shoulder"}
                        var angleData = 0f
                        var angleDirection = ""
                        frontNeckData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData = it.rawData
                                angleDirection = getDirection(it.rawData)
                                resultString.append("선자세와 비교했을 때, 앉았을 때 목이 약 ${String.format("%.2f", abs(angleData))}° ${angleDirection}방향으로 틀어져 있습니다. ")
                            }
                        }
                    }
                }
            }
            "좌측 어깨" -> {
                when (seq) {
                    0 -> {
                        var angleData = 0f
                        var angleDirection = ""
                        var subDistanceData = 0f
                        var distanceDirection = ""
                        val shoulderData1 = units.find { it.columnName == "front_horizontal_angle_shoulder"}
                        shoulderData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData = it.rawData
                                angleDirection = getDirection(it.rawData)
                                resultString.append("어깨는 ${String.format("%.2f", abs(angleData))}° ${angleDirection}방향으로 틀어져 있습니다. 골반 교정운동을 추천드립니다. ")
                            }
                        }
                        val shoulderData2 = units.find { it.columnName == "front_horizontal_angle_shoulder"}
                        shoulderData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                subDistanceData = it.rawData
                                distanceDirection = getDirection(it.rawData)
                                resultString.append("또한 양 어깨가 ${String.format("%.2f", abs(subDistanceData))}cm ${distanceDirection}방향으로 높이 차이가 있습니다. ")
                            }
                        }
                    }
                    3 -> {
                        val shoulderData = units.find { it.columnName == "side_left_horizontal_distance_shoulder"}
                        var subDistanceData = 0f
                        var distanceDirection = ""
                        shoulderData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                subDistanceData = it.rawData
                                distanceDirection = getForwardDirection(it.rawData)
                                resultString.append("중심에서 좌측 어깨까지 ${String.format("%.2f", abs(subDistanceData))}cm 차이가 있습니다. 중심보다 앞에 있을 경우 라운드 숄더가 의심됩니다. ")
                            }
                        }
                    }
                    6 -> {
                        val shoulderData = units.find { it.columnName == "back_sit_vertical_angle_shoulder_center_hip"}
                        var angleData = 0f
                        var angleDirection = ""
                        shoulderData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData = it.rawData
                                angleDirection = getDirection(it.rawData)
                                resultString.append("양 어깨와 허리 중심을 이었을 때 ${String.format("%.2f", abs(angleData))}° ${angleDirection}방향으로 틀어져 있습니다. ")
                            }
                        }
                    }
                }
            }
            "우측 어깨" -> {
                when (seq) {
                    0 -> {
                        var angleData = 0f
                        var angleDirection = ""
                        var subDistanceData = 0f
                        var distanceDirection = ""
                        val shoulderData1 = units.find { it.columnName == "front_horizontal_angle_shoulder"}
                        shoulderData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData = it.rawData
                                angleDirection = getDirection(it.rawData)
                                resultString.append("어깨는 ${String.format("%.2f", abs(angleData))}° ${angleDirection}방향으로 틀어져 있습니다. 골반 교정운동을 추천드립니다. ")

                            }
                        }
                        val shoulderData2 = units.find { it.columnName == "front_horizontal_angle_shoulder"}
                        shoulderData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                subDistanceData = it.rawData
                                distanceDirection = getDirection(it.rawData)
                                resultString.append("또한 양 어깨가 ${String.format("%.2f", abs(subDistanceData))}cm ${distanceDirection}방향으로 높이 차이가 있습니다. ")
                            }
                        }

                    }
                    4 -> {
                        val shoulderData = units.find { it.columnName == "side_right_horizontal_distance_shoulder"}
                        var subDistanceData = 0f
                        var distanceDirection = ""
                        shoulderData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                subDistanceData = it.rawData
                                distanceDirection = getForwardDirection(it.rawData)
                                resultString.append("중심에서 우측 어깨까지 ${String.format("%.2f", abs(subDistanceData))}cm 차이가 있습니다. 라운드 숄더를 조심하세요. ")
                            }
                        }
                    }
                    6 -> {
                        val shoulderData = units.find { it.columnName == "back_sit_vertical_angle_shoulder_center_hip"}
                        var angleData = 0f
                        var angleDirection = ""
                        shoulderData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData = it.rawData
                                angleDirection = getDirection(it.rawData)
                                resultString.append("양 어깨와 허리 중심을 이었을 때 ${String.format("%.2f", abs(angleData))}° ${angleDirection}방향으로 틀어져 있습니다. ")
                            }
                        }
                    }
                }
            }
            "좌측 팔꿉"-> {
                when (seq) {
                    0 -> {
                        var angleData1 = 0f
                        var angleDirection1 = ""
                        var subDistanceData = 0f
                        var distanceDirection = ""
                        var angleData2 = 0f
                        var angleDirection2 = ""
                        val frontElbowData1 = units.find { it.columnName == "front_horizontal_angle_elbow"}
                        frontElbowData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData
                                angleDirection1 = getDirection(it.rawData)
                                resultString.append("어깨는 ${String.format("%.2f", abs(angleData1))}° ${angleDirection1}방향으로 틀어져 있습니다. ")
                            }
                        }
                        val frontElbowData2 = units.find { it.columnName == "front_horizontal_distance_sub_elbow"}
                        frontElbowData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                subDistanceData = it.rawData
                                distanceDirection = getDirection(it.rawData)
                                resultString.append("${String.format("%.2f", abs(subDistanceData))}cm ${distanceDirection}방향으로 높이 차이가 있습니다. ")
                            }
                        }
                        val frontElbowData3 = units.find { it.columnName == "front_vertical_angle_shoulder_elbow_left"}
                        frontElbowData3?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData2 = it.rawData
                                angleDirection2 = getDirection(it.rawData)
                                resultString.append("어깨-팔꿉의 각도가 ${String.format("%.2f", abs(angleData2))}° ${angleDirection2}방향으로 높이 차이가 있습니다. ")
                            }
                        }
                    }
                    2 -> {
                        var angleData1 = 0f
                        var angleDirection1 = ""
                        var angleData2 = 0f
                        var angleDirection2 = ""
                        val elbowData1 = units.find { it.columnName == "front_elbow_align_angle_left_upper_elbow_elbow_wrist"}
                        elbowData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData
                                angleDirection1 = getDirection(it.rawData)
                                resultString.append("상완과 하완을 겹쳤을 때 ${String.format("%.2f", abs(angleData1))}° 로 정상범위보다 팔이 겹치지 않습니다. ")

                            }
                        }
                        val elbowData2 = units.find { it.columnName == "front_elbow_align_angle_left_shoulder_elbow_wrist"}
                        elbowData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData2 = it.rawData
                                angleDirection2 = getDirection(it.rawData)
                                resultString.append("${String.format("%.2f", abs(angleData2))}° 로 정상범위보다 팔이 겹치지 않습니다. ")
                            }
                        }
                    }
                    3 -> {
                        var angleData1 = 0f
                        var angleDirection1 = ""
                        var subDistanceData = 0f
                        var distanceDirection = ""
                        var angleData2 = 0f
                        val sideElbowData1 = units.find { it.columnName == "side_left_vertical_angle_shoulder_elbow"}
                        sideElbowData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData
                                angleDirection1 = getDirection(it.rawData)
                                resultString.append("중심에서 ${String.format("%.2f", abs(angleData1))}° ${angleDirection1}방향으로 틀어져 있습니다. ")
                            }
                        }
                        val sideElbowData2 = units.find { it.columnName == "side_left_vertical_angle_elbow_wrist"}
                        sideElbowData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                subDistanceData = it.rawData
                                distanceDirection = getDirection(it.rawData)
                                resultString.append("${String.format("%.2f", abs(subDistanceData))}cm ${distanceDirection}방향으로 높이 차이가 있습니다. ")
                            }
                        }
                        val sideElbowData3 = units.find { it.columnName == "side_left_vertical_angle_shoulder_elbow_wrist"}
                        sideElbowData3?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData2 = it.rawData
                                resultString.append("좌측 어깨-팔꿉 각도는 ${String.format("%.2f", abs(angleData2))}°로 팔이 바깥쪽으로 벌어져 주변 근육 긴장이 의심됩니다. ")
                            }
                        }
                    }
                }
            }
            "우측 팔꿉" -> {
                when (seq) {
                    0 -> {
                        var angleData1 = 0f
                        var angleDirection1 = ""
                        var subDistanceData = 0f
                        var distanceDirection = ""
                        var angleData2 = 0f
                        var angleDirection2 = ""
                        val frontElbowData1 = units.find { it.columnName == "front_horizontal_angle_elbow"}
                        frontElbowData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData
                                angleDirection1 = getDirection(it.rawData)
                                resultString.append("어깨는 ${String.format("%.2f", abs(angleData1))}° ${angleDirection1}방향으로 틀어져 있습니다. ")
                            }
                        }
                        val frontElbowData2 = units.find { it.columnName == "front_horizontal_distance_sub_elbow"}
                        frontElbowData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                subDistanceData = it.rawData
                                distanceDirection = getDirection(it.rawData)
                                resultString.append("${String.format("%.2f", abs(subDistanceData))}cm ${distanceDirection}방향으로 높이 차이가 있습니다. ")
                            }
                        }
                        val frontElbowData3 = units.find { it.columnName == "front_vertical_angle_shoulder_elbow_right"}
                        frontElbowData3?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData2 = it.rawData
                                angleDirection2 = getDirection(it.rawData)
                                resultString.append("어깨-팔꿉의 각도가 ${String.format("%.2f", abs(angleData2))}° ${angleDirection2}방향으로 높이 차이가 있습니다. ")
                            }
                        }
                    }
                    2 -> {
                        var angleData1 = 0f
                        var angleDirection1 = ""
                        var angleData2 = 0f
                        var angleDirection2 = ""
                        val elbowData1 = units.find { it.columnName == "front_elbow_align_angle_right_upper_elbow_elbow_wrist"}
                        elbowData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData
                                angleDirection1 = getDirection(it.rawData)
                                resultString.append("상완과 하완을 겹쳤을 때 ${String.format("%.2f", abs(angleData1))}° 로 정상범위보다 팔이 겹쳐지지 않습니다. ")

                            }
                        }
                        val elbowData2 = units.find { it.columnName == "front_elbow_align_angle_right_shoulder_elbow_wrist"}
                        elbowData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData2 = it.rawData
                                angleDirection2 = getDirection(it.rawData)
                                resultString.append("${String.format("%.2f", abs(angleData2))}° 로 정상범위보다 팔이 겹쳐지지 않습니다. ")
                            }
                        }
                    }
                    3 -> {
                        var angleData1 = 0f
                        var angleDirection1 = ""
                        var subDistanceData = 0f
                        var distanceDirection = ""
                        var angleData2 = 0f
                        val sideElbowData1 = units.find { it.columnName == "side_right_vertical_angle_shoulder_elbow"}
                        sideElbowData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData
                                angleDirection1 = getDirection(it.rawData)
                                resultString.append("어깨-팔꿉이 ${String.format("%.2f", abs(angleData1))}° ${angleDirection1}방향으로 틀어져 있습니다. ")
                            }
                        }
                        val sideElbowData2 = units.find { it.columnName == "side_right_vertical_angle_elbow_wrist"}
                        sideElbowData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                subDistanceData = it.rawData
                                distanceDirection = getDirection(it.rawData)
                                resultString.append("${String.format("%.2f", abs(subDistanceData))}cm ${distanceDirection}방향으로 높이 차이가 있습니다. ")
                            }
                        }
                        val sideElbowData3 = units.find { it.columnName == "side_right_vertical_angle_shoulder_elbow_wrist"}
                        sideElbowData3?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData2 = it.rawData
                                resultString.append("좌측 어깨-팔꿉 각도는 ${String.format("%.2f", abs(angleData2))}°로 팔이 바깥쪽으로 벌어져 주변 근육 긴장이 의심됩니다. ")
                            }
                        }
                    }
                }
            }
            "좌측 손목" -> {
                when (seq) {
                    0 -> {
                        var angleData1 = 0f
                        var angleData2 = 0f
                        var angleDirection2 = ""
                        val WristData1 = units.find { it.columnName == "front_vertical_angle_elbow_wrist_left"}
                        WristData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData

                                resultString.append("손목의 기울기가 ${String.format("%.2f", abs(angleData1))}° 로 정상범위에서 벗어나 주변 근육이 긴장돼 있는 상태입니다. ")

                            }
                        }
                        val WristData2 = units.find { it.columnName == "front_horizontal_angle_wrist"}
                        WristData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData2 = it.rawData
                                angleDirection2 = getDirection(it.rawData)
                                resultString.append("${String.format("%.2f", abs(angleData2))}° 로 ${angleDirection2}방향으로 높이 차이가 있습니다. ")
                            }
                        }
                    }
                    2 -> {
                        var angleData1 = 0f
                        var angleData2 = 0f
                        var angleDirection2 = ""
                        val wristData1 = units.find { it.columnName == "front_elbow_align_angle_mid_index_wrist_elbow_left"}
                        wristData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData

                                resultString.append("손-손목의 기울기가 ${String.format("%.2f", abs(angleData1))}° 로 정상범위에서 벗어났습니다. 과부하된 손목의 근육을 확인하세요. ")

                            }
                        }
                        val wristData2 = units.find { it.columnName == "front_elbow_align_distance_left_wrist_shoulder"}
                        wristData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData2 = it.rawData
                                angleDirection2 = getDirection(it.rawData)
                                resultString.append("${String.format("%.2f", abs(angleData2))}° 로 ${angleDirection2}방향으로 수직상태에서 벗어났습니다. ")
                            }
                        }
                    }
                    3 -> {
                        var subDistanceData = 0f
                        var distanceDirection = ""
                        val wristData1 = units.find { it.columnName == "side_left_horizontal_distance_wrist"}
                        wristData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                subDistanceData = it.rawData
                                distanceDirection = getForwardDirection(it.rawData)
                                resultString.append("중심에서 ${String.format("%.2f", abs(subDistanceData))}cm로 벗어나있습니다. 이두근, 손목 긴장을 체크하세요. ")

                            }
                        }
                    }
                }
            }
            "우측 손목" -> {
                when (seq) {
                    0 -> {
                        var angleData1 = 0f
                        var angleData2 = 0f
                        var angleDirection2 = ""
                        val wristData1 = units.find { it.columnName == "front_vertical_angle_elbow_wrist_right"}
                        wristData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData
                                resultString.append("손목의 기울기가 ${String.format("%.2f", abs(angleData1))}° 로 정상 범위에서 벗어나 주변 근육이 긴장돼 있는 상태입니다. ")

                            }
                        }
                        val wristData2 = units.find { it.columnName == "front_horizontal_angle_wrist"}
                        wristData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData2 = it.rawData
                                angleDirection2 = getDirection(it.rawData)
                                resultString.append("${String.format("%.2f", abs(angleData2))}° 로 ${angleDirection2}방향으로 높이 차이가 있습니다. ")
                            }
                        }
                    }
                    2 -> {
                        var angleData1 = 0f
                        var angleData2 = 0f
                        var angleDirection2 = ""
                        val wristData1 = units.find { it.columnName == "front_elbow_align_angle_mid_index_wrist_elbow_right"}
                        wristData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData

                                resultString.append("손-손목의 기울기가 ${String.format("%.2f", abs(angleData1))}° 로 정상범위에서 벗어났습니다. 과부하된 손목의 근육을 확인하세요. ")

                            }
                        }
                        val wristData2 = units.find { it.columnName == "front_elbow_align_distance_right_wrist_shoulder"}
                        wristData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData2 = it.rawData
                                angleDirection2 = getDirection(it.rawData)
                                resultString.append("${String.format("%.2f", abs(angleData2))}° 로 ${angleDirection2}방향으로 수직상태에서 벗어났습니다. ")
                            }
                        }
                    }
                    4 -> {
                        var subDistanceData = 0f
                        var distanceDirection = ""
                        val wristData1 = units.find { it.columnName == "side_right_horizontal_distance_wrist"}
                        wristData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                subDistanceData = it.rawData
                                distanceDirection = getForwardDirection(it.rawData)
                                resultString.append("중심에서 ${String.format("%.2f", abs(subDistanceData))}cm로 벗어나있습니다. 이두근, 손목 긴장을 체크하세요. ")

                            }
                        }
                    }
                }
            }
            "좌측 골반" -> {
                when (seq) {
                    0 -> {
                        var angleData1 = 0f
                        var angleDirection1 = ""
                        var angleData2 = 0f
                        var angleDirection2 = ""
                        val hipData1 = units.find { it.columnName == "front_vertical_angle_hip_knee_left"}
                        hipData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData
                                angleDirection1 = getDirection(it.rawData)
                                resultString.append("무릎과의 기울기 ${String.format("%.2f", abs(angleData1))}° 정상범위에서 벗어났습니다. 고관절 스트레칭을 추천합니다. ")
                            }
                        }
                        val hipData2 = units.find { it.columnName == "front_horizontal_angle_hip"}
                        hipData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData2 = it.rawData
                                angleDirection2 = getDirection(it.rawData)
                                resultString.append("양 골반의 기울기가 ${String.format("%.2f", abs(angleData2))}°로 ${angleDirection2}방향으로 치우쳐져 있습니다. ")
                            }
                        }
                    }
                    3 -> {
                        var angleData1 = 0f
                        var angleDirection1 = ""
                        var subDistanceData = 0f
                        var distanceDirection = ""
                        val hipData1 = units.find { it.columnName == "side_left_vertical_angle_hip_knee"}
                        hipData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData
                                angleDirection1 = getDirection(it.rawData)
                                resultString.append("골반-무릎간 측면 기울기가 ${String.format("%.2f", abs(angleData1))}°로 정상범위에서 벗어났습니다. 고관절 스트레칭을 추천합니다. ")
                            }
                        }
                        val hipData2 = units.find { it.columnName == "side_left_horizontal_distance_hip"}
                        hipData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {

                                subDistanceData = it.rawData
                                distanceDirection = getForwardDirection(it.rawData)
                                resultString.append("중심에서 ${String.format("%.2f", abs(subDistanceData))}cm ${distanceDirection}방향으로 나와있습니다. ")
                            }
                        }
                    }
                    5 -> {
                        var angleData1 = 0f
                        var angleDirection1 = ""
                        val hipData = units.find { it.columnName == "back_horizontal_angle_hip"}
                        hipData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData
                                angleDirection1 = getDirection(it.rawData)
                                resultString.append("후면 측정 시 ${String.format("%.2f", abs(angleData1))}°로 ${angleDirection1}방향으로 틀어져 있습니다. 골반 틀어짐 교정 운동을 추천드립니다. ")
                            }
                        }
                    }
                    6 -> {
                        var angleData1 = 0f
                        var angleDirection1 = ""
                        var angleData2 = 0f
                        var angleDirection2 = ""
                        val hipData1 = units.find { it.columnName == "back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder"}
                        hipData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData
                                angleDirection1 = getDirection(it.rawData)
                                resultString.append("양 어깨와 골반 중심의 기울기가 ${String.format("%.2f", abs(angleData1))}°로 정상범위에서 벗어났습니다. 굽은 등 및 거북목 스트레칭을 추천합니다. ")
                            }
                        }
                        val hipData2 = units.find { it.columnName == "back_sit_vertical_angle_shoulder_center_hip"}
                        hipData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData2 = it.rawData
                                angleDirection2 = getDirection(it.rawData)
//                                resultString.append("양 어깨와 골반 중심의 기울기가 ${String.format("%.2f", abs(angleData1))}°로 정상범위에서 벗어났습니다. 굽은 등 및 거북목 스트레칭을 추천합니다.")
                            }
                        }
                    }
                }
            }
            "우측 골반" -> {
                when (seq) {
                    0 -> {
                        var angleData1 = 0f
                        var angleDirection1 = ""
                        var angleData2 = 0f
                        var angleDirection2 = ""
                        val hipData1 = units.find { it.columnName == "front_vertical_angle_hip_knee_right"}
                        hipData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData
                                angleDirection1 = getDirection(it.rawData)
                                resultString.append("무릎과의 기울기 ${String.format("%.2f", abs(angleData1))}° 정상범위에서 벗어났습니다. 고관절 스트레칭을 추천합니다. ")
                            }
                        }
                        val hipData2 = units.find { it.columnName == "front_horizontal_angle_hip"}
                        hipData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData2 = it.rawData
                                angleDirection2 = getDirection(it.rawData)
                                resultString.append("양 골반의 기울기가 ${String.format("%.2f", abs(angleData2))}°로 ${angleDirection2}방향으로 치우쳐져 있습니다. ")
                            }
                        }
                    }
                    4 -> {
                        var angleData1 = 0f
                        var angleDirection1 = ""
                        var subDistanceData = 0f
                        var distanceDirection = ""
                        val hipData1 = units.find { it.columnName == "side_right_vertical_angle_hip_knee"}
                        hipData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData
                                angleDirection1 = getDirection(it.rawData)
                                resultString.append("골반-무릎간 측면 기울기가 ${String.format("%.2f", abs(angleData1))}°로 정상범위에서 벗어났습니다. 고관절 스트레칭을 추천합니다. ")
                            }
                        }
                        val hipData2 = units.find { it.columnName == "side_left_horizontal_distance_hip"}
                        hipData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {

                                subDistanceData = it.rawData
                                distanceDirection = getForwardDirection(it.rawData)
                                resultString.append("중심에서 ${String.format("%.2f", abs(subDistanceData))}cm ${distanceDirection}방향으로 나와있습니다. ")
                            }
                        }
                    }
                    5 -> {
                        var angleData1 = 0f
                        var angleDirection1 = ""
                        val hipData = units.find { it.columnName == "back_horizontal_angle_hip"}
                        hipData?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData
                                angleDirection1 = getDirection(it.rawData)
                                resultString.append("후면 측정 시 ${String.format("%.2f", abs(angleData1))}°로 ${angleDirection1}방향으로 틀어져 있습니다. 골반 틀어짐 교정 운동을 추천드립니다. ")
                            }
                        }
                    }
                    6 -> {
                        var angleData1 = 0f

                        val hipData1 = units.find { it.columnName == "back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder"}
                        hipData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData
                                resultString.append("양 어깨와 골반 중심의 기울기가 ${String.format("%.2f", abs(angleData1))}°로 정상범위에서 벗어났습니다. 굽은 등 및 거북목 스트레칭을 추천합니다. ")
                            }
                        }
                    }
                }
            }

            "좌측 무릎" -> {
                when (seq) {
                    0 -> {
                        var angleData1 = 0f
                        var angleDirection1 = ""
                        var subDistanceData = 0f
                        var distanceDirection = ""
                        var angleData2 = 0f
                        val kneeData1 = units.find { it.columnName == "front_horizontal_angle_knee"}
                        kneeData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData
                                angleDirection1 = getDirection(it.rawData)
                                resultString.append("중심에서 ${String.format("%.2f", abs(angleData1))}° ${angleDirection1}방향으로 틀어져 있습니다. ")
                            }
                        }
                        val kneeData2 = units.find { it.columnName == "front_horizontal_distance_knee_left"}
                        kneeData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                subDistanceData = it.rawData
                                distanceDirection = getDirection(it.rawData)
                                resultString.append("${String.format("%.2f", abs(subDistanceData))}cm ${distanceDirection}방향으로 높이 차이가 있습니다. ")
                            }
                        }
                        val kneeData3 = units.find { it.columnName == "front_vertical_angle_hip_knee_ankle_left"}
                        kneeData3?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData2 = it.rawData
                                resultString.append("좌측 골반-무릎-팔꿉 각도는 ${String.format("%.2f", abs(angleData2))}°로  팔이 바깥쪽으로 벌어져 있습니다. 주변 근육이 긴장된 상태입니다. ")
                            }
                        }
                    }
                    5 -> {
                        var angleData1 = 0f
                        var angleDirection1 = ""
                        var angleData2 = 0f
                        var angleDirection2 = ""
                        val kneeData1 = units.find { it.columnName == "back_horizontal_angle_knee"}
                        kneeData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData
                                angleDirection1 = getDirection(it.rawData)
                                resultString.append("양 무릎의 기울기가 ${String.format("%.2f", abs(angleData1))}° 로 정상범위에서 벗어났습니다. ")

                            }
                        }
                        val kneeData2 = units.find { it.columnName == "back_horizontal_distance_knee_left"}
                        kneeData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData2 = it.rawData
                                angleDirection2 = getDirection(it.rawData)
                                resultString.append("${String.format("%.2f", abs(angleData2))}cm 로 중심에서 더 멉니다. 골반 틀어짐이나 허벅지 근육 긴장을 확인하세요. ")
                            }
                        }
                    }
                }
            }
            "우측 무릎" -> {
                when (seq) {
                    0 -> {
                        var angleData1 = 0f
                        var angleDirection1 = ""
                        var subDistanceData = 0f
                        var distanceDirection = ""
                        var angleData2 = 0f
                        val kneeData1 = units.find { it.columnName == "front_horizontal_angle_knee"}
                        kneeData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData
                                angleDirection1 = getDirection(it.rawData)
                                resultString.append("중심에서 ${String.format("%.2f", abs(angleData1))}° ${angleDirection1}방향으로 틀어져 있습니다. ")
                            }
                        }
                        val kneeData2 = units.find { it.columnName == "front_horizontal_distance_knee_right"}
                        kneeData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                subDistanceData = it.rawData
                                distanceDirection = getDirection(it.rawData)
                                resultString.append("${String.format("%.2f", abs(subDistanceData))}cm ${distanceDirection}방향으로 높이 차이가 있습니다. ")
                            }
                        }
                        val kneeData3 = units.find { it.columnName == "front_vertical_angle_hip_knee_ankle_right"}
                        kneeData3?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData2 = it.rawData
                                resultString.append("좌측 골반-무릎-팔꿉 각도는 ${String.format("%.2f", abs(angleData2))}°로  팔이 바깥쪽으로 벌어져 있습니다. 주변 근육이 긴장된 상태입니다. ")
                            }
                        }
                    }
                    5 -> {
                        var angleData1 = 0f
                        var angleDirection1 = ""
                        var angleData2 = 0f
                        var angleDirection2 = ""
                        val kneeData1 = units.find { it.columnName == "back_horizontal_angle_knee"}
                        kneeData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData
                                angleDirection1 = getDirection(it.rawData)
                                resultString.append("양 무릎의 기울기가 ${String.format("%.2f", abs(angleData1))}° 로 정상범위에서 벗어났습니다. ")

                            }
                        }
                        val kneeData2 = units.find { it.columnName == "back_horizontal_distance_knee_right"}
                        kneeData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData2 = it.rawData
                                angleDirection2 = getDirection(it.rawData)
                                resultString.append("${String.format("%.2f", abs(angleData2))}cm 로 중심에서 더 멉니다. 골반 틀어짐이나 허벅지 근육 긴장을 확인하세요. ")
                            }
                        }
                    }
                }
            }

            "좌측 발목"-> {
                when (seq) {
                    0 -> {
                        var angleData1 = 0f
                        var angleDirection1 = ""
                        var subDistanceData = 0f
                        var distanceDirection = ""
                        var angleData2 = 0f
                        var angleDirection2 = ""
                        val ankleData1 = units.find { it.columnName == "front_vertical_angle_knee_ankle_left"}
                        ankleData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData
                                angleDirection1 = getDirection(it.rawData)
                                resultString.append("무릎과의 기울기가 ${String.format("%.2f", abs(angleData1))}° 로 정상 범위에서 벗어나 있습니다. ")
                            }
                        }
                        val ankleData2 = units.find { it.columnName == "front_horizontal_angle_ankle"}
                        ankleData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData2 = it.rawData
                                angleDirection2 = getDirection(it.rawData)
                                resultString.append("양 발목간 기울기가 ${String.format("%.2f", abs(angleData2))}°로 ${angleDirection2}방향으로 기울어져 있습니다. ")
                            }
                        }
                        val ankleData3 = units.find { it.columnName == "front_horizontal_distance_ankle_left"}
                        ankleData3?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                subDistanceData = it.rawData
                                distanceDirection = getDirection(it.rawData)
                                resultString.append("중심에서 ${String.format("%.2f", abs(angleData2))}cm로 발목이 바깥쪽으로 벌어져 있습니다. 골반부터 서있는 자세를 확인하세요. ")
                            }
                        }
                    }
                    5 -> {
                        var angleData1 = 0f
                        var angleDirection1 = ""
                        var angleData2 = 0f
                        var angleDirection2 = ""
                        val ankleData1 = units.find { it.columnName == "back_horizontal_distance_sub_ankle"}
                        ankleData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData
                                angleDirection1 = getDirection(it.rawData)
                                resultString.append("양 발목의 높이차가 ${String.format("%.2f", abs(angleData1))}cm 로 ${angleDirection1}방향으로 높이가 다릅니다. ")

                            }
                        }
                        val ankleData2 = units.find { it.columnName == "back_horizontal_distance_ankle_left"}
                        ankleData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData2 = it.rawData

                                resultString.append("중심에서 ${String.format("%.2f", abs(angleData1))}cm 로 거리가 정상범위에서 벗어나 있습니다. ")
                            }
                        }
                    }
                }
            }
            "우측 발목" -> {
                when (seq) {
                    0 -> {
                        var angleData1 = 0f
                        var angleDirection1 = ""
                        var subDistanceData = 0f
                        var distanceDirection = ""
                        var angleData2 = 0f
                        var angleDirection2 = ""
                        val ankleData1 = units.find { it.columnName == "front_vertical_angle_knee_ankle_right"}
                        ankleData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData
                                angleDirection1 = getDirection(it.rawData)
                                resultString.append("무릎과의 기울기가 ${String.format("%.2f", abs(angleData1))}° 로 정상 범위에서 벗어나 있습니다. ")
                            }
                        }
                        val ankleData2 = units.find { it.columnName == "front_horizontal_angle_ankle"}
                        ankleData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData2 = it.rawData
                                angleDirection2 = getDirection(it.rawData)
                                resultString.append("양 발목간 기울기가 ${String.format("%.2f", abs(angleData2))}°로 ${angleDirection2}방향으로 기울어져 있습니다. ")
                            }
                        }
                        val ankleData3 = units.find { it.columnName == "front_horizontal_distance_ankle_right"}
                        ankleData3?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                subDistanceData = it.rawData
                                distanceDirection = getDirection(it.rawData)
                                resultString.append("중심에서 ${String.format("%.2f", abs(angleData2))}cm로 발목이 바깥쪽으로 벌어져 있습니다. 골반부터 서있는 자세를 확인하세요. ")
                            }
                        }
                    }
                    5 -> {
                        var angleData1 = 0f
                        var angleDirection1 = ""
                        var angleData2 = 0f
                        var angleDirection2 = ""
                        val ankleData1 = units.find { it.columnName == "back_horizontal_distance_sub_ankle"}
                        ankleData1?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData1 = it.rawData
                                angleDirection1 = getDirection(it.rawData)
                                resultString.append("양 발목의 높이차가 ${String.format("%.2f", abs(angleData1))}cm 로 ${angleDirection1}방향으로 높이가 다릅니다. ")

                            }
                        }
                        val ankleData2 = units.find { it.columnName == "back_horizontal_distance_ankle_right"}
                        ankleData2?.let {
                            if (it.rawData !in it.rawDataBound.first..it.rawDataBound.second) {
                                angleData2 = it.rawData

                                resultString.append("중심에서 ${String.format("%.2f", abs(angleData1))}cm 로 거리가 정상범위에서 벗어나 있습니다.")
                            }
                        }
                    }
                }
            }
        }

        return if (resultString.isEmpty()) "정상 범위 내에 있습니다." else resultString.toString()
    }
}