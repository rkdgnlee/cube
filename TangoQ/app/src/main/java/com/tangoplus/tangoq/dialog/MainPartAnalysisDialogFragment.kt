package com.tangoplus.tangoq.dialog

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.AnalysisRVAdapter
import com.tangoplus.tangoq.adapter.AnalysisRVAdapter.AnalysisViewHolder
import com.tangoplus.tangoq.adapter.DataDynamicRVAdapter
import com.tangoplus.tangoq.data.AnalysisUnitVO
import com.tangoplus.tangoq.data.AnalysisVO
import com.tangoplus.tangoq.data.AnalysisViewModel
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMainPartAnalysisDialogBinding
import com.tangoplus.tangoq.db.MeasurementManager.extractVideoCoordinates
import com.tangoplus.tangoq.dialog.AgreementDetailDialogFragment.Companion.ARG_AGREEMENT_TYPE
import kotlinx.coroutines.launch
import org.json.JSONArray

class MainPartAnalysisDialogFragment : DialogFragment() {
    lateinit var binding : FragmentMainPartAnalysisDialogBinding
    val mvm : MeasureViewModel by activityViewModels()
    val avm : AnalysisViewModel by activityViewModels()
    private var currentAnalysis : AnalysisVO? = null
    private var measureResult: JSONArray? = null
    override fun onResume() {
        super.onResume()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog?.window?.setDimAmount(0.6f) // 원하는 만큼의 어둠 설정
        dialogFragmentResize()
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainPartAnalysisDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 선택한 자세의 raw Data들이 들어간 변수 하나 가져오기
        currentAnalysis = avm.relatedAnalyzes.find { it.seq == avm.selectedSeq }

        binding.ibtnMPADExit.setOnClickListener { dismiss() }
        binding.tvMPADTitle.text = "${avm.selectedPart} - ${setSeqString(currentAnalysis?.seq)}"
        binding.tvMPADSummary.text = currentAnalysis?.summary
        measureResult = mvm.selectedMeasure?.measureResult
        setState(currentAnalysis?.isNormal)
        if (currentAnalysis != null) {
            when (currentAnalysis?.seq) {
                1 -> {
                    val ja = measureResult // 지금 1이 껴있어서 (dynamic이 있어서 오류가 나옴)
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
                    setAdapter()
                    for (uni in currentAnalysis?.labels!!) {
                        uni.summary = setLabels(uni)
                    }
                }
            }
        }
    }
    private fun setVideoAdapter(data: List<List<Pair<Float, Float>>>) {

        val titles = listOf("좌측 손", "우측 손", "좌측 골반", "우측 골반", "좌측 무릎", "우측 무릎") // 0 , 1 , 2

        val linearLayoutManager1 = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val leftData = data.filterIndexed { index, _ -> index % 2 == 0 }.toMutableList()
        val leftTitle = titles.filterIndexed{ index, _ -> index % 2 == 0}
        val leftadapter = DataDynamicRVAdapter(leftData, leftTitle)
        binding.rvMPADLeft.layoutManager = linearLayoutManager1
        binding.rvMPADLeft.adapter = leftadapter

        val linearLayoutManager2 = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val rightData = data.filterIndexed { index, _ -> index % 2 == 1 }.toMutableList()
        val rightTitle = titles.filterIndexed{ index, _ -> index % 2 == 1}
        val rightadapter = DataDynamicRVAdapter(rightData, rightTitle)
        binding.rvMPADRight.layoutManager = linearLayoutManager2
        binding.rvMPADRight.adapter = rightadapter

        binding.rvMPADRight.visibility = View.VISIBLE
        val params1 = binding.rvMPADLeft.layoutParams as LayoutParams
        params1.width = 0
        params1.constrainedWidth  = true
        params1.matchConstraintPercentWidth = 0.425f
        val params2 = binding.rvMPADRight.layoutParams as LayoutParams
        params2.width = 0
        params2.constrainedWidth  = true
        params2.matchConstraintPercentWidth = 0.425f
        binding.tvMPAIDynamic.visibility = View.VISIBLE
    }

    private fun setAdapter() {
        binding.rvMPADRight.visibility = View.GONE
        val params = binding.rvMPADLeft.layoutParams as LayoutParams
        params.width = 0
        params.constrainedWidth  = true
        params.matchConstraintPercentWidth = 1.0f
        params.marginStart = 0
        params.marginEnd = 0
        binding.tvMPAIDynamic.visibility = View.GONE
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        val adapter = AnalysisRVAdapter(this@MainPartAnalysisDialogFragment, currentAnalysis?.labels)
        binding.rvMPADLeft.layoutManager = layoutManager
        binding.rvMPADLeft.adapter = adapter
    }

    // 평균과 설명을 넣어주는 곳
    private fun setLabels(unit : AnalysisUnitVO ) : String {
        return when (unit.columnName) {

            // 목관절
            "front_horizontal_angle_ear" -> "양 귀의 위치를 비교한 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다."
            "side_left_vertical_angle_nose_shoulder" -> "측면에서 코와 어꺠의 위치의 기울기를 의미합니다. 기울기 값 70° 미만이면 거북목이 의심됩니다."
            "side_right_vertical_angle_nose_shoulder" -> "측면에서 코와 어꺠의 위치의 기울기를 의미합니다. 기울기 값 70° 미만이면 거북목이 의심됩니다."
            "back_vertical_angle_nose_center_shoulder" -> "양 어깨의 중심과 코의 기울기를 의미합니다.  기울기 값 0°를 기준으로 5° 오차를 넘어가면 목관절 틀어짐이 의심됩니다."
            "back_sit_horizontal_angle_ear" -> "양 귀의 위치를 비교한 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다."
            "back_sit_vertical_angle_right_shoulder_nose_left_shoulder" -> "앉은 자세에서 코를 기준으로 양 어깨의 각도를 의미합니다. 값이 클수록 심한 거북목으로 예측할 수 있습니다."
            // 좌측 어깨
            "front_horizontal_angle_shoulder" -> "양 어깨의 위치를 비교한 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다."
            "front_horizontal_distance_sub_shoulder" -> "양 어깨의 높낮이를 의미합니다. 값 0cm를 기준으로 1cm 오차 이내가 표준 어깨 높이 차이 입니다."
            "side_left_horizontal_distance_shoulder" -> "측면에서 몸의 무게중심에서 어깨까지의 거리를 의미합니다. 멀어질 수록 라운드 숄더가 의심됩니다."
            "side_left_vertical_angle_shoulder_elbow_wrist" -> "측면에서 " // TODO 이거 빼도 됨
            "back_vertical_angle_shoulder_center_hip" -> "양 골반 중심에서 어깨의 기울기를 의미합니다. 값 "
            "back_sit_vertical_angle_shoulder_center_hip" -> "코와 양 어깨 중 좌측 어깨의 각이 더 넓습니다. 좌측 승모근 긴장이 의심됩니다."
            // 우측 어깨
//            "front_horizontal_angle_shoulder" -> "양 어깨의 위치를 비교한 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다."
//            "front_horizontal_distance_sub_shoulder" -> "양 어깨의 높낮이를 의미합니다. 값 0cm를 기준으로 1cm 오차 이내가 표준 어깨 높이 차이 입니다."
            "side_right_horizontal_distance_shoulder" -> "측면에서 몸의 무게중심에서 어깨까지의 거리를 의미합니다. 멀어질 수록 라운드 숄더가 의심됩니다."
            "side_right_vertical_angle_shoulder_elbow_wrist" -> "측면에서 " // TODO 이거 빼도 됨
//            "back_vertical_angle_shoulder_center_hip" -> "양 골반 중심에서 어깨의 기울기를 의미합니다. 값 "
//            "back_sit_vertical_angle_shoulder_center_hip" -> "코와 양 어깨 중 좌측 어깨의 각이 더 넓습니다. 좌측 승모근 긴장이 의심됩니다."
            // 좌측 팔꿉
            "front_horizontal_angle_elbow" -> "양 팔꿉의 높낮이를 의미합니다. 값 0cm를 기준으로 1cm 오차 이내가 표준 어깨 높이 차이 입니다"
            "front_horizontal_distance_sub_elbow" -> "양 팔꿉의 위치를 비교한 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다"
            "front_vertical_angle_shoulder_elbow_left" -> "어깨와 팔꿉의 각도를 의미합니다. 기울기 값 일수록 어깨와 상완 근육의 긴장이 의심됩니다" // TODO 각도 추가
            "front_vertical_angle_elbow_wrist" -> "팔꿉과 손목의 각도를 의미합니다. 기울기 값 일수록 상완 근육의 긴장이 의심됩니다" // TODO 각도 추가
            "front_elbow_align_angle_left_upper_elbow_elbow_wrist" -> "팔꿉 측정에서 상완-팔꿉-손목의 각도를 의미합니다. 값이 할수록 이두 근육의 긴장이 의심됩니다"
            "front_elbow_align_angle_left_shoulder_elbow_wrist" -> "어깨-팔꿉-손목의 각도를 의미합니다. 값이 할수록 이두 근육의 긴장이 의심됩니다"
            "side_left_vertical_angle_shoulder_elbow" -> "측면에서 어깨와 팔꿉의 각도입니다. 값이 할수록 어깨 주변 근육의 긴장이 의심됩니다"
            "side_left_vertical_angle_elbow_wrist" -> "측면에서 어깨와 팔꿉의 각도입니다. 값이 할수록 어깨 주변 근육의 긴장이 의심됩니다"
            "side_left_vertical_angle_shoulder_elbow_wrist" -> "측면에서 어깨-팔꿉-손목의 각도입니다. 값이 할수록 이두 근육의 긴장이 의심됩니다"
            // 우측 팔꿉
            "front_horizontal_angle_elbow" -> "양 팔꿉의 높낮이를 의미합니다. 값 0cm를 기준으로 1cm 오차 이내가 표준 어깨 높이 차이 입니다"
            "front_horizontal_distance_sub_elbow" -> "양 팔꿉의 위치를 비교한 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다"
            "front_vertical_angle_shoulder_elbow_right" -> "어깨와 팔꿉의 각도를 의미합니다. 기울기 값 일수록 어깨와 상완 근육의 긴장이 의심됩니다"
            "front_vertical_angle_elbow_wrist" -> "팔꿉과 손목의 각도를 의미합니다. 기울기 값 일수록 상완 근육의 긴장이 의심됩니다"
            "front_elbow_align_angle_right_upper_elbow_elbow_wrist" -> "팔꿉 측정에서 상완-팔꿉-손목의 각도를 의미합니다. 값이 할수록 이두 근육의 긴장이 의심됩니다"
            "front_elbow_align_angle_right_shoulder_elbow_wrist" -> "어깨-팔꿉-손목의 각도를 의미합니다. 값이 할수록 이두 근육의 긴장이 의심됩니다"
            "side_right_vertical_angle_shoulder_elbow" -> "측면에서 어깨와 팔꿉의 각도입니다. 값이 할수록 어깨 주변 근육의 긴장이 의심됩니다"
            "side_right_vertical_angle_elbow_wrist" -> "측면에서 어깨와 팔꿉의 각도입니다. 값이 할수록 어깨 주변 근육의 긴장이 의심됩니다"
            "side_right_vertical_angle_shoulder_elbow_wrist" -> "측면에서 어깨-팔꿉-손목의 각도입니다. 값이 할수록 이두 근육의 긴장이 의심됩니다"
            // 좌측 손목
            "front_vertical_angle_elbow_wrist_left" -> "팔꿉-손목의 기울기를 의미합니다. 각도가 수직과 멀어질수록 전완, 상완 근육의 긴장이 의심됩니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다"
            "front_horizontal_angle_wrist" -> "양 손목의 높이 차를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다"
            "front_elbow_align_angle_mid_index_wrist_elbow_left" -> "중지-손목-팔꿉의 기울기를 의미합니다. 몸쪽으로 가파를 수록 손목의 과부하가 의심됩니다. 짧은 손바닥근과 새끼벌림근 등을 마사지 해주세요 "
            "front_elbow_align_distance_left_wrist_shoulder" -> "팔꿉자세에서 손목과 어깨 위치의 거리를 나타냅니다. 값이 0cm에 가까울수록 정상입니다. 멀어질수록 주관증후군이 의심되니 테니스 엘보 주변을 마사지해주세요"
            "side_left_horizontal_distance_wrist" -> "측면에서 몸의 무게중심에서 손목까지의 거리를 의미합니다. 멀어질 수록 이두근 긴장, 전완근의 긴장 등이 의심됩니다."
            // 우측 손목
            "front_vertical_angle_elbow_wrist_right" -> "팔꿉-손목의 기울기를 의미합니다. 각도가 수직과 멀어질수록 전완, 상완 근육의 긴장이 의심됩니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다"
//            "front_horizontal_angle_wrist" -> ""
            "front_elbow_align_angle_mid_index_wrist_elbow_right" -> "중지-손목-팔꿉의 기울기를 의미합니다. 몸쪽으로 가파를 수록 손목의 과부하가 의심됩니다. 짧은 손바닥근과 새끼벌림근 등을 마사지 해주세요 "
            "front_elbow_align_distance_right_wrist_shoulder" -> "팔꿉자세에서 손목과 어깨 위치의 거리를 나타냅니다. 값이 0cm에 가까울수록 정상입니다. 멀어질수록 주관증후군이 의심되니 테니스 엘보 주변을 마사지해주세요"
            "side_right_horizontal_distance_wrist" -> "측면에서 몸의 무게중심에서 손목까지의 거리를 의미합니다. 멀어질 수록 이두근 긴장, 전완근의 긴장 등이 의심됩니다."
            // 좌측 골반
            "front_vertical_angle_hip_knee_left" -> "골반-무릎의 기울기를 의미합니다. 기울기 값 90° 기준으로 5° 이내의 범위를 표준적인 기울기입니다. 벗어날 경우, 서있는 자세를 교정해보세요"
            "front_vertical_angle_hip_knee_ankle" -> "골반-무릎-발목의 기울기를 의미합니다. 기울기 값 180° 기준으로 10° 이내의 범위를 표준적인 기울기입니다. 벗어날 경우, 무릎위치 및 걷는 자세를 주목해보세요."
            "front_horizontal_angle_hip" -> "양 골반의 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다"
            "side_left_vertical_angle_hip_knee" -> "측면에서 골반-무릎의 기울기를 의미합니다. 기울기 값 90° 기준으로 5° 이내의 범위를 표준적인 기울기입니다. 벗어날 경우, 햄스트링, 종아리근육의 긴장이 있을 수 있으니 스트레칭을 추천드립니다"
            "side_left_horizontal_distance_hip" -> "측면에서 몸의 무게중심에서 골반까지의 거리를 의미합니다. 멀어질 수록 몸의 무게중심 쏠림 등이 의심됩니다."
            "side_left_vertical_angle_hip_knee_ankle" -> "측면에서 골반-무릎-발목의 기울기를 의미합니다. 기울기 값 180° 기준으로 10° 이내의 범위를 표준적인 기울기입니다. 벗어날 경우, 햄스트링, 종아리근육의 긴장이 있을 수 있으니 스트레칭을 추천드립니다"
            "back_horizontal_angle_hip" -> "양 골반의 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다"
            "back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder" -> "앉은 자세에서 양 어깨와 골반 중심의 각도를 의미합니다. 각도 값 50° 기준으로 10° 이내의 범위를 표준적인 각도입니다."
            "back_sit_vertical_angle_shoulder_center_hip" -> "앉은 자세에서 어깨와 골반 중심의 기울기를 의미합니다. 각도 값 90° 기준으로 1° 이내의 범위를 표준적인 각도입니다."
            // 우측 골반
            "front_vertical_angle_hip_knee_right" -> "골반-무릎의 기울기를 의미합니다. 기울기 값 90° 기준으로 5° 이내의 범위를 표준적인 기울기입니다. 벗어날 경우, 서있는 자세를 교정해보세요"
//            "front_vertical_angle_hip_knee_ankle" -> "골반-무릎-발목의 기울기를 의미합니다. 기울기 값 180° 기준으로 10° 이내의 범위를 표준적인 기울기입니다. 벗어날 경우, 무릎위치 및 걷는 자세를 주목해보세요."
//            "front_horizontal_angle_hip" -> "양 골반의 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다"
            "side_right_vertical_angle_hip_knee" -> "측면에서 골반-무릎의 기울기를 의미합니다. 기울기 값 90° 기준으로 5° 이내의 범위를 표준적인 기울기입니다. 벗어날 경우, 햄스트링, 종아리근육의 긴장이 있을 수 있으니 스트레칭을 추천드립니다"
            "side_right_horizontal_distance_hip" -> "측면에서 몸의 무게중심에서 골반까지의 거리를 의미합니다. 멀어질 수록 몸의 무게중심 쏠림 등이 의심됩니다."
            "side_right_vertical_angle_hip_knee_ankle" -> "측면에서 골반-무릎-발목의 기울기를 의미합니다. 기울기 값 180° 기준으로 10° 이내의 범위를 표준적인 기울기입니다. 벗어날 경우, 햄스트링, 종아리근육의 긴장이 있을 수 있으니 스트레칭을 추천드립니다"
            "back_horizontal_angle_hip" -> "양 골반의 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다"
//            "back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder" -> "앉은 자세에서 양 어깨와 골반 중심의 각도를 의미합니다. 각도 값 50° 기준으로 10° 이내의 범위를 표준적인 각도입니다."
//            "back_sit_vertical_angle_shoulder_center_hip" -> ""
            // 좌측 무릎
            "front_horizontal_angle_knee" -> "양 무릎의 위치를 비교한 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다."
            "front_horizontal_distance_knee_left" -> "몸의 중심에서 우측 무릎의 거리를 의미합니다. 값 0cm를 기준으로 1cm 오차 이내가 표준 어깨 높이 차이 입니다"
            "front_vertical_angle_hip_knee_ankle_left" -> "골반-무릎-발목의 수직각도를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다. 벗어날 경우 햄스트링과 오금의 근육을 이완하는 스트레칭을 추천드립니다."
            "back_horizontal_angle_knee" -> "양 무릎의 기울기를 의미합니다. 기울기 값 0° 기준으로 0.5° 오차 이내가 표준적인 기울기 입니다"
            "back_horizontal_distance_knee_left" -> "몸의 중심에서 좌측 무릎까지의 거리를 의미합니다. 기울기 값 0cm 기준으로 0.5cm 오차 이내가 표준적인 거리 입니다. 벗어날 경우 좌측 다리에 무게를 더 실어 서는 습관이 의심됩니다. 골반 교정 운동을 추천드립니다"
            // 우측 무릎
            "front_horizontal_angle_knee" -> "양 무릎의 위치를 비교한 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다."
            "front_horizontal_distance_knee_right" -> "몸의 중심에서 우측 무릎의 거리를 의미합니다. 값 0cm를 기준으로 1cm 오차 이내가 표준 어깨 높이 차이 입니다"
            "front_vertical_angle_hip_knee_ankle_right" -> "골반-무릎-발목의 수직각도를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다. 벗어날 경우 햄스트링과 오금의 근육을 이완하는 스트레칭을 추천드립니다."
            "back_horizontal_angle_knee" -> "양 무릎의 기울기를 의미합니다. 기울기 값 0° 기준으로 0.5° 오차 이내가 표준적인 기울기 입니다"
            "back_horizontal_distance_knee_right" -> "몸의 중심에서 좌측 무릎까지의 거리를 의미합니다. 기울기 값 0cm 기준으로 0.5cm 오차 이내가 표준적인 거리 입니다. 벗어날 경우 좌측 다리에 무게를 더 실어 서는 습관이 의심됩니다. 골반 교정 운동을 추천드립니다"
            // 좌측 발목
            "front_vertical_angle_knee_ankle_left" -> "무릎-발목의 수직각도를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다. 벗어날 경우 평소 발목과 아킬레스건의 긴장, 가자미근을 스트레칭하길 추천드립니다"
            "front_horizontal_angle_ankle" -> "양 발목의 위치를 비교한 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다."
            "front_horizontal_distance_ankle_left" ->  "몸의 중심에서 좌측 발목의 거리를 의미합니다. 값 0cm를 기준으로 1cm 오차 이내가 표준 어깨 높이 차이 입니다"
            "back_horizontal_distance_sub_ankle" -> "양 발목의 위치를 비교한 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다"
            "back_horizontal_distance_ankle_left" -> "몸의 중심에서 좌측 발목의 거리를 의미합니다. 값 0cm를 기준으로 1cm 오차 이내가 표준 어깨 높이 차이 입니다"
            // 우측 발목
            "front_vertical_angle_knee_ankle_right" -> "무릎-발목의 수직각도를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다. 벗어날 경우 평소 발목과 아킬레스건의 긴장, 가자미근을 스트레칭하길 추천드립니다"
            "front_horizontal_angle_ankle" -> "양 발목의 위치를 비교한 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다."
            "front_horizontal_distance_ankle_right" -> "몸의 중심에서 우측 발목의 거리를 의미합니다. 값 0cm를 기준으로 1cm 오차 이내가 표준 어깨 높이 차이 입니다"
            "back_horizontal_distance_sub_ankle" -> ""
            "back_horizontal_distance_ankle_right" -> "몸의 중심에서 우측 발목의 거리를 의미합니다. 값 0cm를 기준으로 1cm 오차 이내가 표준 어깨 높이 차이 입니다"
            else -> ""
        }
    }



    private fun dialogFragmentResize() {
        val windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (Build.VERSION.SDK_INT < 30) {
            val display = windowManager.defaultDisplay
            val size = Point()

            display.getSize(size)

            val window = dialog?.window

            val x = (size.x * 0.85f).toInt()
            val y = (size.y * 0.8f).toInt()
            window?.setLayout(x, y)
        } else {
            val rect = windowManager.currentWindowMetrics.bounds

            val window = dialog?.window

            val x = (rect.width() * 0.85f).toInt()
            val y = (rect.height() * 0.8f).toInt()

            window?.setLayout(x, y)
        }
    }
    private fun setSeqString(seq: Int?) :String {
        return when (seq) {
            0 -> "정면 분석"
            1 -> "동적 분석"
            2 -> "팔꿉 분석"
            3 -> "왼쪽 측면"
            4 -> "오른쪽 측면"
            5 -> "후면 분석"
            6 -> "앉아 후면"
            else -> ""
        }
    }

    private fun setState(isNormal: Boolean?) {
        when (isNormal) {
            false -> {
                binding.tvMPADSummary.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.deleteContainerColor))
                binding.tvMPADSummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.deleteColor))
                binding.ivMPADIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.deleteColor))
            }

            true -> {
                binding.tvMPADSummary.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.secondBgContainerColor))
                binding.tvMPADSummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.thirdColor))
                binding.ivMPADIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.thirdColor))
            }
            null -> {
                binding.tvMPADSummary.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.deleteContainerColor))
                binding.tvMPADSummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.deleteColor))
                binding.ivMPADIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.deleteColor))
            }
        }
    }
}