package com.tangoplus.tangoq.fragment

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
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.DataDynamicRVAdapter
import com.tangoplus.tangoq.adapter.DataStaticRVAdapter
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureAnalysisBinding
import com.tangoplus.tangoq.function.BiometricManager
import com.tangoplus.tangoq.function.MeasurementManager.extractVideoCoordinates
import com.tangoplus.tangoq.function.MeasurementManager.getVideoDimensions
import com.tangoplus.tangoq.function.MeasurementManager.setImage
import com.tangoplus.tangoq.mediapipe.OverlayView
import com.tangoplus.tangoq.mediapipe.PoseLandmarkResult.Companion.fromCoordinates
import com.tangoplus.tangoq.viewmodel.AnalysisViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray

class MeasureAnalysisFragment : Fragment() {
    lateinit var binding : FragmentMeasureAnalysisBinding
    private val viewModel : MeasureViewModel by activityViewModels()
    private val avm : AnalysisViewModel by activityViewModels()
    private lateinit var mr : JSONArray
    private var index = -1

    private var updateUI = false
    private var count = false
    // 영상재생
    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var videoUrl = "http://gym.tangostar.co.kr/data/contents/videos/걷기.mp4"
    private lateinit var jsonArray: JSONArray
    private lateinit var biometricManager : BiometricManager

    companion object {
        private const val ARG_INDEX = "index_analysis"
        private const val ARG_SCORE = "index_score"

        fun newInstance(indexx: Int, balance: Int): MeasureAnalysisFragment {
            val fragment = MeasureAnalysisFragment()
            val args = Bundle()
            args.putInt(ARG_INDEX, indexx)
            args.putInt(ARG_SCORE, balance)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasureAnalysisBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ------# 데이터 필터링을 위한 사전 세팅 #------

        biometricManager = BiometricManager(this)
        biometricManager.authenticate(
            onSuccess = {
                index = arguments?.getInt(ARG_INDEX) ?: -1
                mr = viewModel.selectedMeasure?.measureResult ?: JSONArray()
                avm.mafMeasureResult = JSONArray()
                Log.v("현재측정", viewModel.selectedMeasure?.regDate.toString())

                // ------# 1차 필터링 (균형별) #------
                // 0, 1, 2, 3으로 3가지.
                when (index) {
                    0 -> { // 정면 균형
                        binding.tvMPTitle.text = "정면 균형"
                        binding.tvMAName.text = "정면 균형"
                        binding.tvMAPart1.text = "정면 측정"
                        binding.tvMAPart2.text = "팔꿉 측정"
                        avm.mafMeasureResult.put(mr.optJSONObject(0))
                        avm.mafMeasureResult.put(mr.optJSONObject(2))
                        setDynamicUI(false)
                        setStaticSplitUi(true)
                        binding.flMA.visibility = View.GONE
                        lifecycleScope.launch {
                            setImage(this@MeasureAnalysisFragment, viewModel.selectedMeasure, 0, binding.ssivMA1, "")
                            setImage(this@MeasureAnalysisFragment, viewModel.selectedMeasure, 2, binding.ssivMA2, "")
                        }
                        setScreenRawData(avm.mafMeasureResult, 0)
                        binding.ivMA.setImageResource(R.drawable.drawable_front)
                    }
                    1 -> { // 측면 균형
                        binding.tvMPTitle.text = "측면 균형"
                        binding.tvMAName.text = "측면 균형"
                        binding.tvMAPart1.text = "좌측 측정"
                        binding.tvMAPart2.text = "우측 측정"
                        avm.mafMeasureResult.put(mr.optJSONObject(3))
                        avm.mafMeasureResult.put(mr.optJSONObject(4))
                        setDynamicUI(false)
                        setStaticSplitUi(true)
                        lifecycleScope.launch {
                            setImage(this@MeasureAnalysisFragment, viewModel.selectedMeasure, 3, binding.ssivMA1, "")
                            setImage(this@MeasureAnalysisFragment, viewModel.selectedMeasure, 4, binding.ssivMA2, "")
                         }
                        setScreenRawData(avm.mafMeasureResult,  3)
                        binding.ivMA.setImageResource(R.drawable.drawable_side)
                    }
                    2 -> { // 후면 균형
                        binding.tvMPTitle.text = "후면 균형"
                        binding.tvMAName.text = "후면 균형"
                        binding.tvMAPart1.text = "후면 측정"
                        avm.mafMeasureResult.put(mr.optJSONObject(5))
                        setDynamicUI(false)
                        setStaticSplitUi(false)
                        lifecycleScope.launch { setImage(this@MeasureAnalysisFragment, viewModel.selectedMeasure, 5, binding.ssivMA1, "") }
                        binding.flMA.visibility = View.GONE
                        setScreenRawData(avm.mafMeasureResult,  5)
                        binding.ivMA.setImageResource(R.drawable.drawable_back)
                    }
                    3 -> {
                        binding.tvMPTitle.text = "앉은 후면"
                        binding.tvMAName.text = "앉은 후면"
                        binding.tvMAPart1.text = "앉은 후면"
                        avm.mafMeasureResult.put(mr.optJSONObject(6))
                        setDynamicUI(false)
                        setStaticSplitUi(false)
                        binding.tvMPTitle.visibility = View.VISIBLE
                        binding.flMA.visibility = View.GONE
                        lifecycleScope.launch { setImage(this@MeasureAnalysisFragment, viewModel.selectedMeasure, 6, binding.ssivMA1, "") }
                        binding.ssivMA2.visibility = View.GONE
                        setScreenRawData(avm.mafMeasureResult,  6)
                        binding.ivMA.setImageResource(R.drawable.drawable_back)
                    }
                    4 -> { // 동적 균형
                        binding.tvMPTitle.text = "동적 측정"
                        binding.tvMAName.text = "동적 균형"
                        avm.mafMeasureResult.put(mr.optJSONArray(1))
                        setDynamicUI(true)
                        setPlayer()
                    }
                }
            },
            onError = {
                Toast.makeText(requireContext(),"인증에 실패했습니다. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                requireActivity().supportFragmentManager.beginTransaction().apply {
                    replace(R.id.flMain, MainFragment())
                    addToBackStack(null)
                    commit()
                }
            }
        )
    }

    // 고정적으로
    // big은 자세 , small은 수직 수평 분석임
    private fun setScreenRawData(measureResult: JSONArray, seq: Int) {
        // 정면은 필터링 됨. 수직부터 필터링
        var minResultData = mutableListOf<Triple<String, String, String?>>()
        when (seq) {
            0 -> {
                // ------# 수직 값 #------
                val keyPairs = mapOf(
                    "front_vertical_angle_shoulder_elbow" to "어깨와 팔꿉 기울기",
                    "front_vertical_angle_elbow_wrist" to "팔꿉와 손목 기울기",
                    "front_vertical_angle_hip_knee" to "골반과 무릎 기울기",
                    "front_vertical_angle_knee_ankle" to "무릎과 발목 기울기",
                    "front_vertical_angle_shoulder_elbow_wrist" to "어깨-팔꿉-손목 기울기",
                    "front_vertical_angle_hip_knee_ankle" to "골반-무릎-발목 기울기"
                )
                minResultData = keyPairs.mapNotNull {(key, description) ->
                    val leftValue = measureResult.optJSONObject(0).optDouble("${key}_left")
                    val rightValue = measureResult.optJSONObject(0).optDouble("${key}_right")

                    if (!leftValue.isNaN() && !rightValue.isNaN()) {
                        Triple(description, "왼쪽: ${String.format("%.2f", leftValue)}°", "오른쪽: ${String.format("%.2f", rightValue)}°")
                    } else null
                }.toMutableList()

                // ------# 수평 값 #------
                val anglePairs = listOf(
                    "ear" to "귀", "shoulder" to "어깨", "elbow" to "팔꿉", "wrist" to "손목", "hip" to "골반", "knee" to "무릎", "ankle" to "발목"
                ).map { (part, description) ->
                    Triple(
                        "양 $description 기울기 높이 차",
                        "front_horizontal_angle_$part",
                        "front_horizontal_distance_sub_$part"
                    )
                }
                val distancePairs = listOf(
                    Triple("중심에서 양 손목 거리", "front_horizontal_distance_wrist_left", "front_horizontal_distance_wrist_right"),
                    Triple("중심에서 양 무릎 거리", "front_horizontal_distance_knee_left", "front_horizontal_distance_knee_right"),
                    Triple("중심에서 양 발목 거리", "front_horizontal_distance_ankle_left", "front_horizontal_distance_ankle_right")
                )
                anglePairs.forEach { (description, angleKey, distanceKey) ->
                    val angleValue = measureResult.optJSONObject(0).optDouble(angleKey)
                    val distanceValue = measureResult.optJSONObject(0).optDouble(distanceKey)
                    if (!angleValue.isNaN() && !distanceValue.isNaN()) {

                        minResultData.add(Triple(description, "기울기: ${String.format("%.2f", angleValue)}°", "높이 차: ${String.format("%.2f", distanceValue)}cm"))
                    }
                }
                distancePairs.forEach{ (descrption, leftKey, rightKey) ->
                    val leftValue = measureResult.optJSONObject(0).optDouble(leftKey)
                    val rightValue = measureResult.optJSONObject(0).optDouble(rightKey)
                    if (!leftValue.isNaN() && !rightValue.isNaN()) {
                        minResultData.add(Triple(descrption, "왼쪽: ${String.format("%.2f", leftValue)}cm", "오른쪽: ${String.format("%.2f", rightValue)}cm"))
                    }
                }

                // ------# 팔꿉 #------
                val valueAlignPairs = listOf(
                    Triple("엄지와 엄지관절 기울기", "front_hand_angle_thumb_cmc_tip_left", "front_hand_angle_thumb_cmc_tip_right"),
                    Triple("팔꿉-손목-중지관절 기울기", "front_hand_angle_elbow_wrist_mid_finger_mcp_left", "front_hand_angle_elbow_wrist_mid_finger_mcp_right"),
                    Triple("상완-팔꿉-손목 기울기", "front_elbow_align_angle_left_upper_elbow_elbow_wrist", "front_elbow_align_angle_right_upper_elbow_elbow_wrist"),
                    Triple("양 손목과 어깨 기울기", "front_elbow_align_distance_left_wrist_shoulder", "front_elbow_align_distance_right_wrist_shoulder"),
                    Triple("중지-손목-팔꿉 기울기", "front_elbow_align_angle_mid_index_wrist_elbow_left", "front_elbow_align_angle_mid_index_wrist_elbow_right"),
                    Triple("어깨-팔꿉-손목 기울기", "front_elbow_align_angle_left_shoulder_elbow_wrist", "front_elbow_align_angle_right_shoulder_elbow_wrist")
                )
                valueAlignPairs.forEach{ (descrption, leftKey, rightKey) ->
                    val leftValue = measureResult.optJSONObject(1).optDouble(leftKey)
                    val rightValue = measureResult.optJSONObject(1).optDouble(rightKey)
                    if (!leftValue.isNaN() && !rightValue.isNaN()) {
                        minResultData.add(Triple(descrption, "왼쪽: ${String.format("%.2f", leftValue)}°", "오른쪽: ${String.format("%.2f", rightValue)}°"))
                    }
                }
                val distanceAlignPairs = listOf(
                    Triple("중심과 중지 거리", "front_elbow_align_distance_center_mid_finger_left", "front_elbow_align_distance_center_mid_finger_right"),
                    Triple("중심과 손목 거리", "front_elbow_align_distance_center_wrist_left", "front_elbow_align_distance_center_wrist_right"),
                )
                distanceAlignPairs.forEach{ (descrption, leftKey, rightKey) ->
                    val leftValue = measureResult.optJSONObject(1).optDouble(leftKey)
                    val rightValue = measureResult.optJSONObject(1).optDouble(rightKey)
                    if (!leftValue.isNaN() && !rightValue.isNaN()) {
                        minResultData.add(Triple(descrption, "왼쪽: ${String.format("%.2f", leftValue)}cm", "오른쪽: ${String.format("%.2f", rightValue)}cm"))
                    }
                }
                set012Adapter(minResultData)
            }
            1 -> {
                lifecycleScope.launch {
                    val connections = listOf(
                        15, 16, 23, 24, 25, 26
                    )
                    val coordinates = extractVideoCoordinates(avm.mafMeasureResult.getJSONArray(0))
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
            3 -> {
                val keyAnglePairs = mapOf(
                    "vertical_angle_shoulder_elbow" to "어깨와 팔꿉 기울기",
                    "vertical_angle_elbow_wrist" to "팔꿉와 손목 기울기",
                    "vertical_angle_hip_knee" to "골반과 무릎 기울기",
                    "vertical_angle_ear_shoulder" to "귀와 어깨 기울기",
                    "vertical_angle_nose_shoulder" to "코와 어깨 기울기",
                    "vertical_angle_shoulder_elbow_wrist" to "어깨-팔꿉-손목 기울기",
                    "vertical_angle_hip_knee_ankle" to "골반-무릎-발목 기울기"
                )
                keyAnglePairs.forEach { (key, description) ->
                    val leftAngleValue = measureResult.optJSONObject(0).optDouble("side_left_$key")
                    val rightAngleValue = measureResult.optJSONObject(1).optDouble("side_right_$key")

                    if (!leftAngleValue.isNaN() && !rightAngleValue.isNaN() ) {
                        minResultData.add(Triple(description, "왼쪽 기울기: ${String.format("%.2f", leftAngleValue)}°", "오른쪽 기울기: ${String.format("%.2f", rightAngleValue)}°"))
                    }
                }

                val keyDistancePairs = mapOf(
                    "horizontal_distance_shoulder" to "중심에서 어깨 거리",
                    "horizontal_distance_hip" to "중심에서 골반 거리",
                    "horizontal_distance_pinky" to "중심에서 새끼 거리",
                    "horizontal_distance_wrist" to "중심에서 손목 거리"
                )
                keyDistancePairs.forEach { (key, description) ->
                    val leftDistanceValue = measureResult.optJSONObject(0).optDouble("side_left_$key")
                    val rightDistanceValue = measureResult.optJSONObject(1).optDouble("side_right_$key")

                    if (!leftDistanceValue.isNaN() && !rightDistanceValue.isNaN()) {
                        Log.v("angle들", "side_left_$key: $leftDistanceValue, side_right_$key: $rightDistanceValue")
                        minResultData.add(Triple(description, "왼쪽 거리: ${String.format("%.2f", leftDistanceValue)}cm", "오른쪽 거리: ${String.format("%.2f", rightDistanceValue)}cm"))
                    }
                }
                set012Adapter(minResultData)
//                // ------# 우측 #------
//                val keyRightAnglePairs = mapOf(
//                    "side_right_vertical_angle_shoulder_elbow" to "오른쪽 어깨와 팔꿉 기울기",
//                    "side_right_vertical_angle_elbow_wrist" to "오른쪽 팔꿉와 손목 기울기",
//                    "side_right_vertical_angle_hip_knee" to "오른쪽 골반과 무릎 기울기",
//                    "side_right_vertical_angle_ear_shoulder" to "오른쪽 귀와 어깨 기울기",
//                    "side_right_vertical_angle_nose_shoulder" to "오른쪽 코와 어깨 기울기",
//                    "side_right_vertical_angle_shoulder_elbow_wrist" to "오른쪽 어깨-팔꿉-손목 기울기",
//                    "side_right_vertical_angle_hip_knee_ankle" to "오른쪽 골반-무릎-발목 기울기"
//                )
//                keyRightAnglePairs.forEach { (key, description) ->
//                    val angleValue = measureResult.optJSONObject(1).optDouble(key)
//                    if (!angleValue.isNaN()) {
//                        minResultData.add(Triple(description, "기울기: ${String.format("%.2f", angleValue)}°", null))
//                    }
//                }
//                val keyRightDistancePairs = mapOf(
//                    "side_right_horizontal_distance_shoulder" to "중심과 오른쪽 어깨 거리",
//                    "side_right_horizontal_distance_hip" to "중심과 오른쪽 골반 거리",
//                    "side_right_horizontal_distance_pinky" to "중심과 오른쪽 새끼 거리",
//                    "side_right_horizontal_distance_wrist" to "중심과 오른쪽 손목 거리",
//
//                    )
//                keyRightDistancePairs.forEach { (key, description) ->
//                    val angleValue = measureResult.optJSONObject(1).optDouble(key)
//
//                    if (!angleValue.isNaN() ) {
//                        minResultData.add(Triple(description, "거리: ${String.format("%.2f", angleValue)}cm", null))
//                    }
//                }
//                set012Adapter(minResultData)
            }
            5 -> {
                // ------# 수직 #------
                val verticalKeyPairs = mapOf(
                    "back_vertical_angle_shoudler_center_hip" to "골반중심과 어깨 기울기",
                    "back_vertical_angle_nose_center_hip" to "골반중심과 코 기울기",
                    "back_vertical_angle_nose_center_shoulder" to "어깨중심과 코 기울기",
                )
                val verticalAnglePairs = listOf(
                    Triple("무릎과 발목 기울기", "back_vertical_angle_knee_heel_left", "back_vertical_angle_knee_heel_right"),
                )
                verticalKeyPairs.forEach { (key, description) ->
                    val angleValue = measureResult.optJSONObject(0).optDouble(key)
                    if (!angleValue.isNaN() ) {
                        minResultData.add(Triple(description, "기울기: ${String.format("%.2f", angleValue)}°", null))
                    }
                }
                verticalAnglePairs.forEach{ (descrption, leftKey, rightKey) ->
                    val leftValue = measureResult.optJSONObject(0).optDouble(leftKey)
                    val rightValue = measureResult.optJSONObject(0).optDouble(rightKey)
                    if (!leftValue.isNaN() && !rightValue.isNaN()) {
                        minResultData.add(Triple(descrption, "왼쪽: ${String.format("%.2f", leftValue)}°", "오른쪽: ${String.format("%.2f", rightValue)}°"))
                    }
                }

                // ------# 수평 #------
                val horizontalAnglePairs = listOf(
                    "ear" to "귀", "shoulder" to "어깨", "elbow" to "팔꿉", "wrist" to "손목", "hip" to "골반", "knee" to "무릎", "ankle" to "발목"
                ).map { (part, description) ->
                    Triple(
                        "양 $description 기울기 높이 차",
                        "back_horizontal_angle_$part",
                        "back_horizontal_distance_sub_$part"
                    )
                }
                val horizontalDistancePairs = listOf(
                    Triple("중심에서 양 손목 거리", "back_horizontal_distance_wrist_left", "back_horizontal_distance_wrist_right"),
                    Triple("중심에서 양 무릎 거리", "back_horizontal_distance_knee_left", "back_horizontal_distance_knee_right"),
                    Triple("중심에서 양 발뒷꿈치 거리", "back_horizontal_distance_heel_left", "back_horizontal_distance_heel_right")
                )

                horizontalAnglePairs.forEach { (description, angleKey, distanceKey) ->
                    val angleValue = measureResult.optJSONObject(0).optDouble(angleKey)
                    val distanceValue = measureResult.optJSONObject(0).optDouble(distanceKey)
                    if (!angleValue.isNaN() && !distanceValue.isNaN()) {
                        minResultData.add(Triple(description, "기울기: ${String.format("%.2f", angleValue)}°", "높이 차: ${String.format("%.2f", distanceValue)}cm"))
                    }
                }
                horizontalDistancePairs.forEach{ (descrption, leftKey, rightKey) ->
                    val leftValue = measureResult.optJSONObject(0).optDouble(leftKey)
                    val rightValue = measureResult.optJSONObject(0).optDouble(rightKey)
                    if (!leftValue.isNaN() && !rightValue.isNaN() ) {
                        minResultData.add(Triple(descrption,"왼쪽: ${String.format("%.2f", leftValue)}cm", "오른쪽: ${String.format("%.2f", rightValue)}cm"))
                    }
                }
                set012Adapter(minResultData)
            }

            6 -> {
                val keyPairs = mapOf(
                    "back_sit_vertical_angle_nose_left_shoulder_right_shoulder" to "코-좌측 어깨-우측 어깨 기울기",
                    "back_sit_vertical_angle_left_shoulder_right_shoulder_nose" to "좌측 어깨-우측 어깨-코 기울기",
                    "back_sit_vertical_angle_right_shoulder_nose_left_shoulder" to "우측 어깨-코-좌측 어깨 기울기",
                    "back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder" to "좌측 어깨-골반중심-우측 어깨 기울기",
                    "back_sit_vertical_angle_center_hip_right_shoulder_left_shoulder" to "골반중심-우측 어깨-좌측 어깨 기울기",
                    "back_sit_vertical_angle_right_shoulder_left_shoulder_center_hip" to "우측어깨-좌측 어깨-골반중심 기울기",
                    "back_sit_vertical_angle_shoulder_center_hip" to "어깨와 골반중심 기울기",
                )
                keyPairs.forEach { (key, description) ->
                    val angleValue = measureResult.optJSONObject(0).optDouble(key)

                    if (!angleValue.isNaN() ) {
                        minResultData.add(Triple(description, "기울기: ${String.format("%.3f", angleValue)}°", null))
                    }
                }
                val anglePairs = listOf(
                    "ear" to "귀", "shoulder" to "어깨", "hip" to "골반",
                ).map { (part, description) ->
                    Triple(
                        "양 $description 기울기 높이 차",
                        "back_sit_horizontal_angle_$part",
                        "back_sit_horizontal_distance_sub_$part"
                    )
                }
                anglePairs.forEach { (description, angleKey, distanceKey) ->
                    val angleValue = measureResult.optJSONObject(0).optDouble(angleKey)
                    val distanceValue = measureResult.optJSONObject(0).optDouble(distanceKey)
                    if (!angleValue.isNaN() && !distanceValue.isNaN()) {
                        minResultData.add(Triple(description, "기울기: ${String.format("%.2f", angleValue)}°", "높이 차: ${String.format("%.2f", distanceValue)}cm"))
                    }
                }
                set012Adapter(minResultData)
            }
        }
    }

    private fun setDynamicUI(isDynamic: Boolean) {
        if (isDynamic) {
            binding.ssivMA1.visibility = View.GONE
            binding.ssivMA2.visibility = View.GONE
            binding.flMA.visibility = View.VISIBLE
        } else {
            binding.ssivMA1.visibility = View.VISIBLE
            binding.ssivMA2.visibility = View.VISIBLE
            binding.flMA.visibility = View.GONE
        }
    }

    private fun setStaticSplitUi(isSplit: Boolean) {
        val params = binding.ssivMA1.layoutParams as LayoutParams

        if (isSplit) {
            binding.ssivMA2.visibility = View.VISIBLE
            binding.tvMAPart2.visibility = View.VISIBLE
            params.width = 0 // MATCH_CONSTRAINT
            params.constrainedWidth = true
            params.matchConstraintPercentWidth = 0.5f
        } else {
            binding.ssivMA2.visibility = View.GONE
            binding.tvMAPart2.visibility = View.GONE
            params.width = LayoutParams.MATCH_PARENT
            params.constrainedWidth = false
            params.matchConstraintPercentWidth = 1.0f
        }

        binding.ssivMA1.layoutParams = params
        (binding.ssivMA1.parent as ConstraintLayout).requestLayout()
    }

    private fun set012Adapter(allData: MutableList<Triple<String,String, String?>>) {
        val dangerParts = viewModel.selectedMeasure?.dangerParts?.map { it.first }?.map { it.replace("좌측 ", "").replace("우측 ", "") }
        val filteredData = allData.filter { dangerParts?.any { part -> it.first.contains(part) } == true }
        val linearLayoutManager1 = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val topAdapter = DataStaticRVAdapter(requireContext(), filteredData, true)
        binding.rvMALeft.layoutManager = linearLayoutManager1
        binding.rvMALeft.adapter = topAdapter

        val notFilteredData = allData.filterNot { dangerParts?.any { part -> it.first.contains(part) } == true }
        val linearLayoutManager3 = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        val bottomAdapter = DataStaticRVAdapter(requireContext(), notFilteredData, false)
        binding.rvMALeft2.layoutManager = linearLayoutManager3
        binding.rvMALeft2.adapter = bottomAdapter

    }
    //---------------------------------------! VideoOverlay !---------------------------------------
    private fun setPlayer() {
        lifecycleScope.launch {
            Log.v("동적측정json", "${viewModel.selectedMeasure?.measureResult?.getJSONArray(1)}")
            jsonArray = viewModel.selectedMeasure?.measureResult?.getJSONArray(1) ?: JSONArray()
            Log.v("jsonDataLength", "${jsonArray.length()}")
            initPlayer()

            simpleExoPlayer?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    if (playbackState == Player.STATE_READY) {

                        val videoDuration = simpleExoPlayer?.duration ?: 0L
                        lifecycleScope.launch {
                            while (simpleExoPlayer?.isPlaying == true) {
                                if (!updateUI) updateVideoUI()
//                                Log.v("업데이트video", "overlay: (${binding.ovMA.width}, ${binding.ovMA.height}) PlayerView: ( ${binding.pvMA.width}, ${binding.pvMA.height} )")
                                updateFrameData(videoDuration, jsonArray.length())
                                delay(24)
                                Handler(Looper.getMainLooper()).postDelayed( {updateUI = true},1500)
                            }
                        }
                    }
                }
            })
        }
    }

    private fun initPlayer() {
        simpleExoPlayer = SimpleExoPlayer.Builder(requireContext()).build()
        binding.pvMA.player = simpleExoPlayer
        binding.pvMA.controllerShowTimeoutMs = 1100
        lifecycleScope.launch {
            videoUrl = viewModel.selectedMeasure?.fileUris?.get(1).toString()
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
            val mediaSource = ProgressiveMediaSource.Factory(DefaultDataSourceFactory(requireContext()))
                .createMediaSource(mediaItem)
            mediaSource.let {
                simpleExoPlayer?.prepare(it)
                simpleExoPlayer?.seekTo(0)
                simpleExoPlayer?.playWhenReady = true
            }
        }
        binding.pvMA.findViewById<ImageButton>(R.id.exo_replay_5).visibility = View.GONE
        binding.pvMA.findViewById<ImageButton>(R.id.exo_exit).visibility = View.GONE
        binding.pvMA.findViewById<ImageButton>(R.id.exo_forward_5).visibility = View.GONE

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
        val coordinates = extractVideoCoordinates(jsonArray)
        // 실제 mp4의 비디오 크기를 가져온다
        val (videoWidth, videoHeight) = getVideoDimensions(requireContext(), videoUrl.toUri())
//        Log.v("PlayerView,비디오 크기", "overlay: (${binding.ovMA.width}, ${binding.ovMA.height}), videoWidth,Height: (${videoWidth}, ${videoHeight}) width: ${binding.pvMA.width}, height: ${binding.pvMA.height}")

        if (frameIndex in 0 until totalFrames) {
            // 해당 인덱스의 데이터를 JSON에서 추출하여 변환
            val poseLandmarkResult = fromCoordinates(coordinates[frameIndex])
            // 변환된 데이터를 화면에 그리기
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
        Log.v("업데이트", "UI")


        binding.tvMPTitle.text = "동적 측정"
        binding.ssivMA1.visibility = View.GONE
        binding.ssivMA2.visibility = View.GONE
        val (videoWidth, videoHeight) = getVideoDimensions(requireContext(), videoUrl.toUri())
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels

        val aspectRatio = videoHeight.toFloat() / videoWidth.toFloat()
//        Log.v("비율", "aspectRatio: $aspectRatio, video: ($videoWidth, $videoHeight), playerView: (${binding.pvMPPD.width}, ${binding.pvMPPD.height}), overlay: (${binding.ovMPPD.width}, ${binding.ovMPPD.height})")
        val adjustedHeight = (screenWidth * aspectRatio).toInt()
        // clMA의 크기 조절
        val params = binding.clMA.layoutParams
        params.width = screenWidth
        params.height = adjustedHeight
        binding.clMA.layoutParams = params

        // llMARV를 clMA 아래에 위치시키기
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.clMA)
        constraintSet.connect(binding.llMARV.id, ConstraintSet.TOP, binding.clMA.id, ConstraintSet.BOTTOM)
        constraintSet.applyTo(binding.clMA)

        // PlayerView 크기 조절 (필요한 경우)
        val playerParams = binding.pvMA.layoutParams
        playerParams.width = screenWidth
        playerParams.height = adjustedHeight
        binding.pvMA.layoutParams = playerParams

        setScreenRawData(avm.mafMeasureResult, 1)
        binding.ivMA.setImageResource(R.drawable.drawable_dynamic)

    }

    private fun setVideoAdapter(data: List<List<Pair<Float, Float>>>) {
        val linearLayoutManager1 = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val dynamicAdapter = DataDynamicRVAdapter(data, avm.dynamicTitles, 0)
        binding.rvMALeft.layoutManager = linearLayoutManager1
        binding.rvMALeft.adapter = dynamicAdapter
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
        simpleExoPlayer?.playWhenReady = true
    }
}