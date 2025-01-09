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
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.MainPartAnalysisRVAdapter
import com.tangoplus.tangoq.adapter.DataDynamicRVAdapter
import com.tangoplus.tangoq.vo.AnalysisUnitVO
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMainPartPoseDialogBinding
import com.tangoplus.tangoq.function.MeasurementManager.extractVideoCoordinates
import com.tangoplus.tangoq.function.MeasurementManager.getVideoDimensions
import com.tangoplus.tangoq.function.MeasurementManager.setImage
import com.tangoplus.tangoq.mediapipe.MathHelpers.isTablet
import com.tangoplus.tangoq.mediapipe.OverlayView
import com.tangoplus.tangoq.mediapipe.PoseLandmarkResult.Companion.fromCoordinates
import com.tangoplus.tangoq.viewmodel.AnalysisViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray

class MainPartPoseDialogFragment : DialogFragment() {
    private lateinit var binding : FragmentMainPartPoseDialogBinding
    private val mvm : MeasureViewModel by activityViewModels()
    private val avm : AnalysisViewModel by activityViewModels()
    private var count = false
    private lateinit var dynamicJa: JSONArray
    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var videoUrl = ""
    private var updateUI = false
    private var measureResult: JSONArray? = null
    companion object {
        private const val ARG_SEQ = "arg_seq"
        fun newInstance(seq: Int): MainPartPoseDialogFragment {
            val fragment = MainPartPoseDialogFragment()
            val args = Bundle()
            args.putInt(ARG_SEQ, seq)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainPartPoseDialogBinding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        avm.currentAnalysis = avm.relatedAnalyzes.find { it.seq == avm.selectedSeq }
        val seq = arguments?.getInt(ARG_SEQ)
        if (seq != null) {
            lifecycleScope.launch {

                when (seq) {
                    1 -> {
                        setDynamicUI(true)
                        setPlayer()
                    }
                    else -> {

                        setDynamicUI(false)
                        setImage(this@MainPartPoseDialogFragment, mvm.selectedMeasure, seq, binding.ssivMPPD, "mainPart")

                        binding.ssivMPPD.viewTreeObserver.addOnGlobalLayoutListener (object : ViewTreeObserver.OnGlobalLayoutListener{
                            override fun onGlobalLayout() {
                                binding.clMPPD.requestLayout()
                                binding.ssivMPPD.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            }
                        })
                    }
                }
            }
        }
        binding.tvMPPDTitle.text = "${avm.selectedPart} - ${avm.setSeqString(avm.currentAnalysis?.seq)}"
        binding.tvMPPDSummary.text = avm.currentAnalysis?.summary
        measureResult = mvm.selectedMeasure?.measureResult
        avm.currentAnalysis?.isNormal?.let { setState(it) }
        if (avm.currentAnalysis != null) {
            when (avm.currentAnalysis?.seq) {
                1 -> {
                    // -----# 동적 측정 setUI #------
                    binding.tvMPPDSummary.textSize = if (isTablet(requireContext())) 17f else 15f
                    val dynamicString = "스쿼트 1회 동작에서 좌우 부위의 궤적을 비교합니다.\n하단에 그려진 궤적이 대칭을 이룰 수록 정상범위입니다.\n\n이동 안정성의 불균형이 생겼을 때 손은 어깨, 골반은 엉덩이, 무릎은 허벅지로 각 관절주변에ㅅ 연결된 근육을 풀어주어야 합니다."
                    binding.tvMPPDSummary.text = dynamicString
                    binding.tvMPPDSummary.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.secondContainerColor))
                    binding.tvMPPDSummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.thirdColor))
                    binding.ivMPPDIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.thirdColor))

                    val ja = measureResult?.optJSONArray(1)
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
                    binding.tvMPPDSummary.textSize = if (isTablet(requireContext())) 18f else 15f
                    setAdapter()
                    val labels = avm.currentAnalysis?.labels
                    if (labels != null) {
                        for (uni in labels) {
                            uni.summary = setLabels(uni)
                        }
                    }
                }
            }
        }
    }
    private fun setVideoAdapter(data: List<List<Pair<Float, Float>>>) {
        val linearLayoutManager1 = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val dynamicAdapter = DataDynamicRVAdapter(data, avm.dynamicTitles, 1)
        binding.rvMPPD.layoutManager = linearLayoutManager1
        binding.rvMPPD.adapter = dynamicAdapter
    }

    private fun setAdapter() {
        Log.v("AnalysisLabel", "labels: ${avm.currentAnalysis?.labels}")
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        val adapter = MainPartAnalysisRVAdapter(this@MainPartPoseDialogFragment, avm.currentAnalysis?.labels)
        binding.rvMPPD.layoutManager = layoutManager
        binding.rvMPPD.adapter = adapter
    }

    // 평균과 설명을 넣어주는 곳
    private fun setLabels(unit : AnalysisUnitVO) : String {
        return when (unit.columnName) {

            // 목관절
            "front_horizontal_angle_ear" -> "기울기 값 180° 기준으로 1° 오차 이내가 표준적인 기울기 입니다. 한쪽으로 기울었을 경우, 기울어진 반대편의 목빗근의 스트레칭을 권장드립니다."
            "side_left_vertical_angle_ear_shoulder" -> "측면에서는 귀와 어깨가 일직선 상에 있어야 가장 이상적입니다. 목이 앞으로 나와있을 수록 수직에서 멀어지며, 굽은 등, 허리 교정을 추천드립니다."
            "side_right_vertical_angle_ear_shoulder" -> "측면에서는 귀와 어깨가 일직선 상에 있어야 가장 이상적입니다. 목이 앞으로 나와있을 수록 수직에서 멀어지며, 굽은 등, 허리 교정을 추천드립니다."
            "back_vertical_angle_nose_center_shoulder" -> "양 어깨의 중심과 코의 기울기를 의미합니다.  기울기 값 90°를 기준으로 5° 오차를 넘어가면 목관절 틀어짐이 의심됩니다."
            "back_sit_horizontal_angle_ear" -> "기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다. 기울어진 부위의 반대편의 목빗근 스트레칭을 추천드립니다."
            "back_sit_vertical_angle_right_shoulder_nose_left_shoulder" -> "앉은 자세에서 코를 기준으로 양 어깨의 각도를 의미합니다. 값이 클수록 목이 신체 정면으로 나오고 내려와있기 때문에 심한 거북목으로 예측할 수 있습니다."
            // 좌측 어깨
            "front_horizontal_angle_shoulder" -> "양 어깨의 기울기가 값 180° 기준으로 1° 오차 이내가 표준적인 기울기 입니다. 음의 기울기일 경우, 좌측 어깨가 더 긴장된 상태입니다."
            "front_horizontal_distance_sub_shoulder" -> "양 어깨의 높이 차이를 의미합니다. 값 0cm를 기준으로 1cm 오차 이내가 표준 어깨 높이 차이 입니다."
            "side_left_horizontal_distance_shoulder" -> "발뒷꿈치에서 시작되는 중심선에서 어깨까지의 거리를 의미합니다. 우측과 비교해서 몸의 쏠림, 라운드 숄더를 판단할 수 있습니다."
            "back_vertical_angle_shoudler_center_hip" -> "골반 중심에서 어깨의 기울기를 의미합니다. 기울기 값 90° 기준으로 2° 오차를 벗어나면, 상체 쏠림을 의심하고, 쏠리는 반대 편의 근육의 긴장을 풀어줘야 합니다."
            "back_horizontal_angle_shoulder" -> "후면에서의 어깨 기울기가 0°에서 멀어질 수록 몸의 측면 틀어짐이 의심됩니다."
            "back_sit_vertical_angle_shoulder_center_hip" -> "어깨선과 골반 중심의 각도입니다. 정상범위에서 벗어날 경우 골반의 평행과 어깨 틀어짐을 교정하는 운동을 추천드립니다"
            "back_sit_vertical_angle_right_shoulder_left_shoulder_center_hip" -> "양 어깨와 골반 중심의 각도에서 왼쪽 어깨의 각도를 의미합니다. 앉은 자세에서 몸의 쏠림을 다른 부위의 각도와 함께 분석해보세요."
            // 우측 어깨
//            "front_horizontal_angle_shoulder" -> "양 어깨의 위치를 비교한 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다."
//            "front_horizontal_distance_sub_shoulder" -> "양 어깨의 높낮이를 의미합니다. 값 0cm를 기준으로 1cm 오차 이내가 표준 어깨 높이 차이 입니다."
            "side_right_horizontal_distance_shoulder" -> "발뒷꿈치에서 시작되는 중심선에서 어깨까지의 거리를 의미합니다. 좌측과 비교해서 몸의 쏠림, 라운드 숄더를 판단할 수 있습니다."
//            "back_vertical_angle_shoudler_center_hip" -> "양 골반 중심에서 어깨의 기울기를 의미합니다. 값 "
//            "back_sit_vertical_angle_shoulder_center_hip" -> "코와 양 어깨 중 좌측 어깨의 각이 더 넓습니다. 좌측 승모근 긴장이 의심됩니다."
            "back_sit_vertical_angle_center_hip_right_shoulder_left_shoulder" -> "양 어깨와 골반 중심의 각도에서 왼쪽 어깨의 각도를 의미합니다. 앉은 자세에서 몸의 쏠림을 다른 부위의 각도와 함께 분석해보세요."
            // 좌측 팔꿉
            "front_horizontal_angle_elbow" -> "팔꿈치는 기울기 값 180° 기준으로 2° 오차 이내가 표준적인 기울기 입니다. 이를 벗어날 경우 어깨 후면, 상완의 긴장을 확인해야 합니다."
            "front_horizontal_distance_sub_elbow" -> "양 팔꿉의 높이 차이는 값 0cm를 기준으로 2cm 오차 이내를 이를 벗어날 경우, 측면 측정의 손목 위치와 함께, 상완과 근육의 긴장을 주목해야 합니다."
            "front_vertical_angle_shoulder_elbow_left" -> "어깨와 팔꿉을 이었을 때 기울기 79° 기준으로 몸의 바깥방향으로 위치할 수록 어깨와 상완 근육의 긴장이 의심됩니다"
            "front_elbow_align_angle_left_shoulder_elbow_wrist" -> "상완과 하완의 붙는 면적을 넓게 해서 붙였을 때 12°를 기준으로 넓어질 수록, 전면 어깨, 회전 근개 등 어깨 가동범위와 관련된 근육을 풀어야합니다."
            "side_left_vertical_angle_shoulder_elbow" -> "측면에서 어깨와 팔꿉을 이었을 때 기울기 90° 기준으로 값이 커질수록, 후면 어깨의 긴장을 의심해야 합니다."
            "side_left_vertical_angle_elbow_wrist" -> "측면에서 팔꿉과 손목을 이었을 때 기울기 95° 기준으로 값이 커질수록, 상완 근육의 긴장을 의심해야 합니다."
            "side_left_vertical_angle_shoulder_elbow_wrist" -> "좌측 상완-하완의 각도를 의미합니다. 170° 를 기준으로 값이 작아질 수록 상완 근육의 긴장을 의심해야 합니다."
            // 우측 팔꿉
//            "front_horizontal_angle_elbow" -> "양 팔꿉의 높낮이를 의미합니다. 값 0cm를 기준으로 1cm 오차 이내가 표준 어깨 높이 차이 입니다"
//            "front_horizontal_distance_sub_elbow" -> "양 팔꿉의 위치를 비교한 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다"
            "front_vertical_angle_shoulder_elbow_right" -> "어깨에서 팔꿉을 이었을 때 기울기 79° 기준으로 몸의 바깥방향으로 위치할 수록 어깨와 상완 근육의 긴장이 의심됩니다"
            "front_elbow_align_angle_right_shoulder_elbow_wrist" -> "상완과 하완의 붙는 면적을 넓게 해서 붙였을 때 12° 기준으로 넓어질 수록 전면 어깨, 회전 근개 등 어깨 가동범위와 관련된 근육을 풀어야합니다."
            "side_right_vertical_angle_shoulder_elbow" -> "측면에서 어깨와 팔꿉을 이었을 때 기울기 90° 기준으로 값이 커질수록, 후면 어깨의 긴장을 의심해야 합니다."
            "side_right_vertical_angle_elbow_wrist" -> "측면에서 팔꿉과 손목을 이었을 때 기울기 95° 기준으로 값이 커질수록, 상완 근육의 긴장을 의심해야 합니다."
            "side_right_vertical_angle_shoulder_elbow_wrist" -> "좌측 상완-하완의 각도를 의미합니다. 170° 를 기준으로 값이 작아질 수록 상완 근육의 긴장을 의심해야 합니다."
            // 좌측 손목
            "front_vertical_angle_elbow_wrist_left" -> "팔꿉-손목의 기울기를 의미합니다. 각도가 수직과 멀어질수록 전완, 상완 근육의 긴장이 의심됩니다. 기울기 값 85° 기준으로 5° 오차 이내가 표준적인 기울기 입니다"
            "front_horizontal_angle_wrist" -> "양 손목은 기울기 값 180° 기준으로 약 3° 오차 이내를 벗어날 경우, 더 높은 곳에 위치한 손목의 삼두 근육의 긴장이나, 어깨긴장을 의심해야 합니다. "
            "front_horizontal_distance_wrist_left" -> "중심에서 좌측 손목까지의 거리입니다. 반대편과 비교해서 값 차이가 많이 날 수록 삼두 근육, 팔꿈치 긴장을 의심해야 합니다."
            "front_elbow_align_distance_left_wrist_shoulder" -> "팔꿉 자세에서 어깨와 손목의 거리는 3cm 기준으로 멀어질 수록, 전면 어깨, 회전 근개 등 어깨 가동범위와 관련된 근육을 풀어야합니다. "
            "front_elbow_align_distance_center_wrist_left" -> "팔꿉 자세에서 몸의 중심에서 손목의 거리는 값이 20cm에 가까울수록 정상입니다. 멀어질수록 전면 어깨, 회전 근개 등 어깨 가동범위와 관련된 근육을 풀어야합니다. "
            "side_left_horizontal_distance_wrist" -> "발뒷꿈치에서 시작되는 중심선에서 손목까지의 거리를 의미합니다. 우측과 비교해서 삼두 근육, 팔꿉의 긴장을 판단할 수 있습니다."
            // 우측 손목
            "front_vertical_angle_elbow_wrist_right" -> "팔꿉-손목의 기울기를 의미합니다. 각도가 수직과 멀어질수록 전완, 상완 근육의 긴장이 의심됩니다. 기울기 값 85° 기준으로 5° 오차 이내가 표준적인 기울기 입니다"
//            "front_horizontal_angle_wrist" -> ""
            "front_horizontal_distance_wrist_right" -> "중심에서 좌측 손목까지의 거리입니다. 반대편과 비교해서 값 차이가 많이 날 수록 삼두 근육, 팔꿈치 긴장을 의심해야 합니다."
            "front_elbow_align_distance_right_wrist_shoulder" -> "팔꿉 자세에서 어깨와 손목의 거리는 3cm 기준으로 멀어질 수록, 전면 어깨, 회전 근개 등 어깨 가동범위와 관련된 근육을 풀어야합니다. "
            "front_elbow_align_distance_center_wrist_right" ->"팔꿉 자세에서 몸의 중심에서 손목의 거리는 값이 20cm에 가까울수록 정상입니다. 멀어질수록 전면 어깨, 회전 근개 등 어깨 가동범위와 관련된 근육을 풀어야합니다. "
            "side_right_horizontal_distance_wrist" -> "발뒷꿈치에서 시작되는 중심선에서 손목까지의 거리를 의미합니다. 좌측과 비교해서 삼두 근육, 팔꿉의 긴장을 판단할 수 있습니다."
            // 좌측 골반
            "front_vertical_angle_hip_knee_left" -> "골반-무릎 간의 기울기는 90° 기준으로 약 2° 이내가 정상입니다. 측면의 골반-무릎-발목 기울기와 함께 비교해서 평소 무릎이 조금 굽어진 자세로 서있는지 확인해보세요."
            "front_horizontal_angle_hip" -> "양 골반의 기울기는 값 180° 기준으로 2° 오차 이내가 정상입니다. 앉아 후면 자세의 골반 기울기와 비교해서 골반이 틀어진건지, 하체가 불균형한건지 비교해보세요"
            "side_left_horizontal_distance_hip" -> "발뒷꿈치에서 시작되는 중심선에서 골반까지의 거리를 의미합니다. 우측과 비교해서 몸의 쏠림, 골반 전방 경사를 판단할 수 있습니다."
            "back_horizontal_angle_hip" -> "양 골반의 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다"
            "back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder" -> "앉은 자세에서 골반 중심-양 어깨를 이은 삼각형의 왼쪽 어깨 각도입니다. 각도 값이 높을 수록 굽은 등을 교정해주세요"
            // 우측 골반
            "front_vertical_angle_hip_knee_right" -> "골반-무릎 간의 기울기는 90° 기준으로 약 2° 이내가 정상입니다. 측면의 골반-무릎-발목 기울기와 함께 비교해서 평소 무릎이 조금 굽어진 자세로 서있는지 확인해보세요."
//            "front_horizontal_angle_hip" -> "양 골반의 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다"
            "side_right_horizontal_distance_hip" -> "발뒷꿈치에서 시작되는 중심선에서 골반까지의 거리를 의미합니다. 좌측과 비교해서 몸의 쏠림, 골반 전방 경사를 판단할 수 있습니다."
//            "back_horizontal_angle_hip" -> "양 골반의 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다"
//            "back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder" -> "앉은 자세에서 양 어깨와 골반 중심의 각도를 의미합니다. 각도 값 50° 기준으로 10° 이내의 범위를 표준적인 각도입니다."
//            "back_sit_vertical_angle_right_shoulder_left_shoulder_center_hip" -> "앉은 자세에서 골반 중심-양 어깨를 이은 삼각형의 오른쪽 어깨 각도입니다. 각도 값이 높을 수록 굽은 등을 교정해주세요"
            // 좌측 무릎
            "front_horizontal_angle_knee" -> "양 골반의 기울기는 180° 기준으로 약 2° 이내가 정상입니다. 측면의 어깨 각도, 후면의 발뒷꿈치 위치를 비교해서, 평소 서있는 자세를 교정해보세요"
            "front_horizontal_distance_knee_left" -> "몸의 중심에서 우측 무릎의 거리는 값 13cm를 기준으로 약 2cm 이내가 정상입니다. 우측 무릎과 비교해서 거리가 유난히 멀다면, 발의 정렬이 잘못돼 정강이, 대퇴부의 긴장을 풀어주세요."
            "front_vertical_angle_hip_knee_ankle_left" -> "골반-무릎-발목 간의 기울기는 175° 기준으로 약 3° 오차 이내가 표준적인 기울기 입니다. 벗어날 경우 햄스트링과 오금의 근육을 이완하는 스트레칭을 추천드립니다."
            "side_left_vertical_angle_hip_knee" -> "측면에서 골반-무릎의 기울기를 의미합니다. 기울기 값 90° 기준으로 5° 이내의 범위를 표준적인 기울기입니다. 벗어날 경우, 햄스트링, 종아리근육의 긴장이 있을 수 있으니 스트레칭을 추천드립니다"
            "side_left_vertical_angle_hip_knee_ankle" -> "측면에서 골반-무릎-발목의 기울기는 90° 기준으로 5° 이내의 범위를 표준적인 기울기입니다. 정면과 비교해서 평소 서있는 자세에서 다리가 조금 굽힘이 있는지 확인하고 교정 해보세요"
            "back_horizontal_angle_knee" -> "몸 뒷편의 무릎 기울기는  0°를 기준으로 약 2° 오차 이내가 정상입니다. 이를 벗어나면 발의 정렬 문제, 주변 근육인 햄스트링과 종아리 근육의 긴장을 풀어보세요"
            "back_horizontal_distance_knee_left" -> "몸의 중심에서 좌측 무릎까지의 거리를 의미합니다. 거리 10cm 기준으로 5cm 오차 이내가 표준적인 거리 입니다. 벗어날 경우 좌측 다리에 무게를 더 실어 서는 습관이 의심됩니다. 골반 교정 운동을 추천드립니다"
            // 우측 무릎
//            "front_horizontal_angle_knee" -> "양 무릎의 위치를 비교한 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다."
            "front_horizontal_distance_knee_right" -> "몸의 중심에서 우측 무릎의 거리는 값 13cm를 기준으로 약 2cm 이내가 정상입니다. 우측 무릎과 비교해서 거리가 유난히 멀다면, 발의 정렬이 잘못돼 정강이, 대퇴부의 긴장을 풀어주세요."
            "front_vertical_angle_hip_knee_ankle_right" -> "골반-무릎-발목 간의 기울기는 175° 기준으로 약 3° 오차 이내가 표준적인 기울기 입니다. 벗어날 경우 햄스트링과 오금의 근육을 이완하는 스트레칭을 추천드립니다."
            "side_right_vertical_angle_hip_knee" -> "측면에서 골반-무릎의 기울기를 의미합니다. 기울기 값 90° 기준으로 5° 이내의 범위를 표준적인 기울기입니다. 벗어날 경우, 햄스트링, 종아리근육의 긴장이 있을 수 있으니 스트레칭을 추천드립니다"
            "side_right_vertical_angle_hip_knee_ankle" -> "측면에서 골반-무릎-발목의 기울기는 90° 기준으로 5° 이내의 범위를 표준적인 기울기입니다. 정면과 비교해서 평소 서있는 자세에서 다리가 조금 굽힘이 있는지 확인하고 교정 해보세요"
//            "back_horizontal_angle_knee" -> "양 무릎의 기울기를 의미합니다. 기울기 값 0° 기준으로 0.5° 오차 이내가 표준적인 기울기 입니다"
            "back_horizontal_distance_knee_right" -> "몸의 중심에서 좌측 무릎까지의 거리를 의미합니다. 기울기 값 10cm 기준으로 5cm 오차 이내가 표준적인 거리 입니다. 벗어날 경우 좌측 다리에 무게를 더 실어 서는 습관이 의심됩니다. 골반 교정 운동을 추천드립니다"
            // 좌측 발목
            "front_vertical_angle_knee_ankle_left" -> "무릎-발목간 수직각도는 값 88° 기준으로 약 4° 오차 이내가 정상입니다. 벗어날 경우 평소 발목과 아킬레스건의 긴장, 가자미근을 스트레칭하길 추천드립니다"
            "front_horizontal_angle_ankle" -> "양 발목의 위치를 비교한 기울기를 의미합니다. 기울기 값 180° 기준으로 약 4° 오차 이내가 표준적인 기울기 입니다."
            "front_horizontal_distance_ankle_left" ->  "몸의 중심-발목 간 거리는 약 10cm를 기준으로 5cm 이내가 정상입니다. 우측 발목과 비교해 값의 차이가 많이 날 경우, 발의 정렬로 하지 전체적인 근육을 풀어야 합니다."
            "back_horizontal_distance_sub_ankle" -> "후면에서 양 발목의 기울기는 값 0° 기준으로 1° 오차 이내가 정상입니다. 이를 벗어날 경우, 전면과 비교해 발의 정렬을 교정해주세요"
            "back_horizontal_distance_heel_left" -> "후면에서 발뒷꿈치의 기울기는 값 11cm 기준으로 7cm 오차 이내가 정상입니다. 이를 벗어날 경우, 전면과 비교해 발의 정렬을 교정해주세요"
            // 우측 발목
            "front_vertical_angle_knee_ankle_right" -> "무릎-발목간 수직각도는 값 88° 기준으로 약 4° 오차 이내가 정상입니다. 벗어날 경우 평소 발목과 아킬레스건의 긴장, 가자미근을 스트레칭하길 추천드립니다"
//            "front_horizontal_angle_ankle" -> "양 발목의 위치를 비교한 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다."
            "front_horizontal_distance_ankle_right" -> "몸의 중심-발목 간 거리는 약 10cm를 기준으로 5cm 이내가 정상입니다. 우측 발목과 비교해 값의 차이가 많이 날 경우, 발의 정렬로 하지 전체적인 근육을 풀어야 합니다."
//            "back_horizontal_distance_sub_ankle" -> ""
            "back_horizontal_distance_heel_right" -> "후면에서 발뒷꿈치의 기울기는 값 11cm 기준으로 7cm 오차 이내가 정상입니다. 이를 벗어날 경우, 전면과 비교해 발의 정렬을 교정해주세요"
            else -> "측정 사진을 확인하세요"
        }
    }

    private fun setState(isNormal: Int) {
        when (isNormal) {
            3 -> {
                binding.tvMPPDSummary.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.deleteContainerColor))
                binding.tvMPPDSummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.deleteColor))
                binding.ivMPPDIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.deleteColor))
            }

            1 -> {
                binding.tvMPPDSummary.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.secondBgContainerColor))
                binding.tvMPPDSummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.thirdColor))
                binding.ivMPPDIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.thirdColor))
            }
            2 -> {
                binding.tvMPPDSummary.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.cautionContainerColor))
                binding.tvMPPDSummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.cautionColor))
                binding.ivMPPDIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.cautionColor))
            }
        }
    }
    private fun setDynamicUI(isDynamic: Boolean) {
        if (isDynamic) {
            binding.ssivMPPD.visibility = View.GONE
            binding.flMPPD.visibility = View.VISIBLE
        } else {
            binding.ssivMPPD.visibility = View.VISIBLE
            binding.flMPPD.visibility = View.GONE
        }
    }



    private fun setPlayer() {
        lifecycleScope.launch {
            Log.v("동적측정json", "${mvm.selectedMeasure?.measureResult?.optJSONArray(1)}")
            dynamicJa = mvm.selectedMeasure?.measureResult?.optJSONArray(1) ?: JSONArray()
            Log.v("jsonDataLength", "${dynamicJa.length()}")
            initPlayer()

            simpleExoPlayer?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    if (playbackState == Player.STATE_READY) {

                        val videoDuration = simpleExoPlayer?.duration ?: 0L
                        lifecycleScope.launch {
                            while (simpleExoPlayer?.isPlaying == true) {
                                if (!updateUI) updateVideoUI()
//                                Log.v("업데이트video", "overlay: (${binding.ovMPPD.width}, ${binding.ovMPPD.height}) PlayerView: ( ${binding.pvMPPD.width}, ${binding.pvMPPD.height} )")
                                updateFrameData(videoDuration, dynamicJa.length())
                                delay(24)
                                Handler(Looper.getMainLooper()).postDelayed( {updateUI = true},1500)
                            }
                        }
                    }
                }
            })
        }
    }
    private fun updateVideoUI() {
        val (videoWidth, videoHeight) = getVideoDimensions(requireContext(), videoUrl.toUri())
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels

        val aspectRatio = videoHeight.toFloat() / videoWidth.toFloat()
        Log.v("비율", "aspectRatio: $aspectRatio, video: ($videoWidth, $videoHeight), playerView: (${binding.pvMPPD.width}, ${binding.pvMPPD.height}), overlay: (${binding.ovMPPD.width}, ${binding.ovMPPD.height})")
        val adjustedHeight = (screenWidth * aspectRatio).toInt()

        // clMA의 크기 조절
        val params = binding.clMPPD.layoutParams
        params.width = screenWidth
        params.height = adjustedHeight
        binding.clMPPD.layoutParams = params

        // llMARV를 clMA 아래에 위치시키기
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.clMPPD)
        constraintSet.connect(binding.clMPPD2.id, ConstraintSet.TOP, binding.clMPPD.id, ConstraintSet.BOTTOM)
        constraintSet.applyTo(binding.clMPPD)

        // PlayerView 크기 조절 (필요한 경우)
        val playerParams = binding.pvMPPD.layoutParams
        playerParams.width = screenWidth
        playerParams.height = adjustedHeight
        binding.pvMPPD.layoutParams = playerParams

    }

    private fun initPlayer() {
        simpleExoPlayer = SimpleExoPlayer.Builder(requireContext()).build()
        binding.pvMPPD.player = simpleExoPlayer
        binding.pvMPPD.controllerShowTimeoutMs = 1100
        lifecycleScope.launch {
            videoUrl = mvm.selectedMeasure?.fileUris?.get(1).toString()
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
            val mediaSource = ProgressiveMediaSource.Factory(DefaultDataSourceFactory(requireContext()))
                .createMediaSource(mediaItem)
            mediaSource.let {
                simpleExoPlayer?.prepare(it)
                simpleExoPlayer?.seekTo(0)
                simpleExoPlayer?.playWhenReady = true
            }
        }
        binding.pvMPPD.findViewById<ImageButton>(R.id.exo_replay_5).visibility = View.GONE
        binding.pvMPPD.findViewById<ImageButton>(R.id.exo_exit).visibility = View.GONE
        binding.pvMPPD.findViewById<ImageButton>(R.id.exo_forward_5).visibility = View.GONE

        val exoPlay = requireActivity().findViewById<ImageButton>(R.id.btnPlay)
        val exoPause = requireActivity().findViewById<ImageButton>(R.id.btnPause)
        exoPause?.setOnClickListener {
            if (simpleExoPlayer?.isPlaying == true) {
                simpleExoPlayer?.pause()
                exoPause.visibility = View.GONE
                exoPlay.visibility = View.VISIBLE
            }
        }
        exoPlay?.setOnClickListener {
            if (simpleExoPlayer?.isPlaying == false) {
                simpleExoPlayer?.play()
                exoPause.visibility = View.VISIBLE
                exoPlay.visibility = View.GONE
            }
        }
    }


    private fun updateFrameData(videoDuration: Long, totalFrames: Int) {
        val currentPosition = simpleExoPlayer?.currentPosition ?: 0L

        // 현재 재생 시간에 해당하는 프레임 인덱스 계산
        val frameIndex = ((currentPosition.toFloat() / videoDuration) * totalFrames).toInt()
        val coordinates = extractVideoCoordinates(dynamicJa)
        // 실제 mp4의 비디오 크기를 가져온다
        val (videoWidth, videoHeight) = getVideoDimensions(requireContext(), videoUrl.toUri())
//        Log.v("PlayerView,비디오 크기", "overlay: (${binding.ovMA.width}, ${binding.ovMA.height}), videoWidth,Height: (${videoWidth}, ${videoHeight}) width: ${binding.pvMA.width}, height: ${binding.pvMA.height}")
        val isPortrait = videoHeight > videoWidth
        if (frameIndex in 0 until totalFrames) {
            // 해당 인덱스의 데이터를 JSON에서 추출하여 변환
            val poseLandmarkResult = fromCoordinates(coordinates[frameIndex])
            // 변환된 데이터를 화면에 그리기
            requireActivity().runOnUiThread {
                binding.ovMPPD.scaleX = -1f
                binding.ovMPPD.setResults(
                    poseLandmarkResult,
                    videoWidth,
                    videoHeight,
                    OverlayView.RunningMode.VIDEO
                )
                binding.ovMPPD.invalidate()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        simpleExoPlayer?.stop()
        simpleExoPlayer?.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        simpleExoPlayer?.release()
    }
    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
}