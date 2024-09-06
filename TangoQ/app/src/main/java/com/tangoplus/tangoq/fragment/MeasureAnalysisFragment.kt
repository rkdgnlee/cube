package com.tangoplus.tangoq.fragment

import android.animation.Animator
import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.MeasureDataRVAdapter
import com.tangoplus.tangoq.data.AnalysisVO
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureAnalysisBinding

class MeasureAnalysisFragment : Fragment() {
    lateinit var binding : FragmentMeasureAnalysisBinding
    private val viewModel : MeasureViewModel by activityViewModels()
    private lateinit var measureResult: MutableList<AnalysisVO>
    private var allData = mutableListOf<Triple<String, Double, Double>>()
    private var index = -1
    private var currentSequence = -1
    companion object {
        private const val ARG_INDEX = "index_analysis"
        fun newInstance(indexx: Int): MeasureAnalysisFragment {
            val fragment = MeasureAnalysisFragment()
            val args = Bundle()
            args.putInt(ARG_INDEX, indexx)
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

        index = arguments?.getInt(ARG_INDEX)!!
        measureResult = viewModel.selectedMeasure!!.measureResult


        // ------# 1차 필터링 (균형별) #------
        when (index) {
            0 -> { // 정면 균형
                measureResult = measureResult.filter { it.sequence == 0 || it.sequence == 1 }.toMutableList()
                setDynamicUI(false)
                binding.tlMP.getTabAt(0)?.text = "정면 측정"
                binding.tlMP.getTabAt(1)?.text = "팔꿉 측정"
                currentSequence = 0
                switchScreenData(measureResult, currentSequence, true)

            }
            1 -> { // 측면 균형
                measureResult = measureResult.filter { it.sequence == 3 || it.sequence == 4 }.toMutableList()
                setDynamicUI(false)
                binding.tlMP.getTabAt(0)?.text = "우측 측정"
                binding.tlMP.getTabAt(1)?.text = "좌측 측정"
                currentSequence = 3
                switchScreenData(measureResult, currentSequence, true)

            }
            2 -> { // 후면 균형
                measureResult = measureResult.filter { it.sequence == 5 || it.sequence == 6 }.toMutableList()
                setDynamicUI(false)
                binding.tlMP.getTabAt(0)?.text = "후면 측정"
                binding.tlMP.getTabAt(1)?.text = "앉은 후면"
                currentSequence = 5
                switchScreenData(measureResult, 5, true)

            }
            3 -> { // 동적 균형
                measureResult = measureResult.filter { it.sequence == 2 }.toMutableList()
                setDynamicUI(true)
                binding.tlMP.getTabAt(0)?.text = "동적 측정"
                binding.tlMP.getTabAt(1)?.text = ""
                binding.tlMP.setSelectedTabIndicatorColor(ContextCompat.getColor(requireContext(), R.color.subColor400))
                binding.tlMP.isEnabled = false
                binding.tlMP.background = null
                currentSequence = 2
                switchScreenData(measureResult, currentSequence, true)

            }
            else -> {}
        }

        // ------# 1-1차 필터링(tabLayout의 첫번째) #------
        binding.tlMP.addOnTabSelectedListener(object: OnTabSelectedListener{
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        // 데이터, 시퀀스(0~6단계), toVerti
                        currentSequence = getSequence(index).first
                        switchScreenData(measureResult, currentSequence, true)
                        animateIndicator(true)
                    }
                    1 -> {
                        currentSequence = getSequence(index).second
                        switchScreenData(measureResult, currentSequence, true)
                        animateIndicator(true)
                    }
                }
            }

        })

        // ------! 하단 버튼토글 그룹 시작 !------
        binding.btgMP.check(R.id.btnMP1)
        binding.btgMP.addOnButtonCheckedListener{ _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnMP1 -> {
                        animateIndicator(true)
                        switchScreenData(measureResult, currentSequence, true)
                    }
                    R.id.btnMP2 -> {
                        animateIndicator(false)
                        switchScreenData(measureResult, currentSequence, false)
                    }
                }

            }
        }
        binding.btnMP1.setOnClickListener { binding.btgMP.check(R.id.btnMP1) }
        binding.btnMP2.setOnClickListener { binding.btgMP.check(R.id.btnMP2) }

        binding.btgMP.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener{
            override fun onGlobalLayout() {
                initToggleIndicator()
                binding.btgMP.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        // ------! 하단 버튼토글 그룹 끝 !------
    }
    // 고정적으로
    // big은 자세 , small은 수직 수평 분석임
    private fun switchScreenData(measureResult: MutableList<AnalysisVO>, sequence: Int, toVerti: Boolean) {
//        binding.ssivMP.setImage()
        // 정면은 필터링 됨. 수직부터 필터링
        var minResultData = mutableListOf<Triple<String, String, String?>>()
        when (sequence) {
            0 -> {
                when (toVerti) {
                    true -> {
                        val keyPairs = mapOf(
                            "front_vertical_angle_shoulder_elbow" to "어깨와 팔꿈치 기울기",
                            "front_vertical_angle_elbow_wrist" to "팔꿈치와 손목 기울기",
                            "front_vertical_angle_hip_knee" to "엉덩이와 무릎 기울기",
                            "front_vertical_angle_knee_ankle" to "무릎과 발목 기울기",
                            "front_vertical_angle_shoulder_elbow_wrist" to "어깨-팔꿈치-손목 기울기",
                            "front_vertical_angle_hip_knee_ankle" to "골반-무릎-발목 기울기"
                        )
                        minResultData = keyPairs.mapNotNull {(key, description) ->
                            val leftValue = measureResult.find { it.key == "${key}_left" }?.value
                            val rightValue = measureResult.find { it.key == "${key}_right" }?.value

                            if (leftValue != null && rightValue != null) {
                                Triple(description, "왼쪽: ${String.format("%.1f", leftValue)}°", "오른쪽: ${String.format("%.1f", rightValue)}°")
                            } else null
                        }.toMutableList()
                        setBothAdapter(minResultData)
                    }
                    false -> {
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
                            val angleValue = measureResult.find { it.key == angleKey }?.value
                            val distanceValue = measureResult.find { it.key == distanceKey }?.value
                            if (angleValue != null && distanceValue != null) {
                                minResultData.add(Triple(description, "기울기: ${String.format("%.1f", angleValue)}°", "높이 차: ${String.format("%.1f", distanceValue)}cm"))
                            }
                        }
                        distancePairs.forEach{ (descrption, leftKey, rightKey) ->
                            val leftValue = measureResult.find { it.key == leftKey }?.value
                            val rightValue = measureResult.find { it.key == rightKey }?.value
                            if (leftValue != null && rightValue != null) {
                                minResultData.add(Triple(descrption, "왼쪽: ${String.format("%.1f", leftValue)}cm", "오른쪽: ${String.format("%.1f", rightValue)}cm"))
                            }
                        }
                        setBothAdapter(minResultData)
                    }
                }

            }
            1 -> {
                when (toVerti) {
                    true -> {
                        val valuePairs = listOf(
                            Triple("엄지와 엄지관절 기울기", "front_hand_angle_thumb_cmc_tip_left", "front_hand_angle_thumb_cmc_tip_right"),
                            Triple("팔꿉-손목-중지관절 기울기", "front_hand_angle_elbow_wrist_mid_finger_mcp_left", "front_hand_angle_elbow_wrist_mid_finger_mcp_right"),
                            Triple("상완-팔꿈치-손목 기울기", "front_elbow_align_angle_left_upper_elbow_elbow_wrist", "front_elbow_align_angle_right_upper_elbow_elbow_wrist"),
                            Triple("양 손목과 어깨 기울기", "front_elbow_align_distance_left_wrist_shoulder", "front_elbow_align_distance_right_wrist_shoulder"),
                            Triple("중지-손목-팔꿈치 기울기", "front_elbow_align_angle_mid_index_wrist_elbow_left", "front_elbow_align_angle_mid_index_wrist_elbow_right"),
                            Triple("어깨-팔꿈치-손목 기울기", "front_elbow_align_angle_left_shoulder_elbow_wrist", "front_elbow_align_angle_right_shoulder_elbow_wrist")
                        )
                        valuePairs.forEach{ (descrption, leftKey, rightKey) ->
                            val leftValue = measureResult.find { it.key == leftKey }?.value
                            val rightValue = measureResult.find { it.key == rightKey }?.value
                            if (leftValue != null && rightValue != null) {
                                minResultData.add(Triple(descrption, "왼쪽: ${String.format("%.1f", leftValue)}°", "오른쪽: ${String.format("%.1f", rightValue)}°"))
                            }
                        }
                        val distancePairs = listOf(
                            Triple("검지관절과 새끼관절 거리", "front_hand_distance_index_pinky_mcp_left", "front_hand_distance_index_pinky_mcp_right"),
                            Triple("어깨와 중지 거리", "front_elbow_align_distance_shoulder_mid_index_left", "front_elbow_align_distance_shoulder_mid_index_right"),
                            Triple("중심과 중지 거리", "front_elbow_align_distance_center_mid_finger_left", "front_elbow_align_distance_center_mid_finger_right"),
                            Triple("중심과 손목 거리", "front_elbow_align_distance_center_wrist_left", "front_elbow_align_distance_center_wrist_right"))


                        distancePairs.forEach{ (descrption, leftKey, rightKey) ->
                            val leftValue = measureResult.find { it.key == leftKey }?.value
                            val rightValue = measureResult.find { it.key == rightKey }?.value
                            if (leftValue != null && rightValue != null) {
                                minResultData.add(Triple(descrption,"왼쪽: ${String.format("%.1f", leftValue)}cm", "오른쪽: ${String.format("%.1f", rightValue)}cm"))
                            }
                        }
                        setBothAdapter(minResultData)
                    }
                    false -> {
                        val anglePairs = listOf(
                            "thumb" to "엄지"
                        ).map { (part, description) ->
                            Triple(
                                "양 $description 기울기 높이 차",
                                "front_horizontal_angle_$part",
                                "front_horizontal_distance_sub_$part"
                            )
                        }
                        val distancePairs = listOf(
                            Triple("중심에서 양 엄지 거리", "front_horizontal_distance_thumb_left", "front_horizontal_distance_thumb_right"),
                            )

                        anglePairs.forEach { (description, angleKey, distanceKey) ->
                            val angleValue = measureResult.find { it.key == angleKey }?.value
                            val distanceValue = measureResult.find { it.key == distanceKey }?.value
                            if (angleValue != null && distanceValue != null) {
                                minResultData.add(Triple(description, "기울기: ${String.format("%.1f", angleValue)}°", "높이 차: ${String.format("%.1f", distanceValue)}cm"))
                            }
                        }
                        distancePairs.forEach{ (descrption, leftKey, rightKey) ->
                            val leftValue = measureResult.find { it.key == leftKey }?.value
                            val rightValue = measureResult.find { it.key == rightKey }?.value
                            if (leftValue != null && rightValue != null) {
                                minResultData.add(Triple(descrption, "왼쪽: ${String.format("%.1f", leftValue)}cm", "오른쪽: ${String.format("%.1f", rightValue)}cm"))
                            }
                        }
                        setBothAdapter(minResultData)
                    }
                }
            }
            2 -> {

            }
            3 -> {
                when (toVerti) {
                    true -> {
                        val keyPairs = mapOf(
                            "side_left_vertical_angle_shoulder_elbow" to "어깨와 팔꿈치 기울기",
                            "side_left_vertical_angle_elbow_wrist" to "팔꿈치와 손목 기울기",
                            "side_left_vertical_angle_hip_knee" to "골반과 무릎 기울기",
                            "side_left_vertical_angle_ear_shoulder" to "귀와 어깨 기울기",
                            "side_left_vertical_angle_nose_shoulder" to "코와 어깨 기울기",
                            "side_left_vertical_angle_shoulder_elbow_wrist" to "어깨-팔꿉-손목 기울기",
                            "side_left_vertical_angle_hip_knee_ankle" to "엉덩이-무릎-발목 기울기"
                        )
                        keyPairs.forEach { (key, description) ->
                            val angleValue = measureResult.find { it.key == key }?.value

                            if (angleValue != null ) {
                                minResultData.add(Triple(description, "기울기: ${String.format("%.1f", angleValue)}°", null))
                            }
                        }
                        setBothAdapter(minResultData)
                    }
                    false -> {
                        val keyPairs = mapOf(
                            "side_left_horizontal_distance_shoulder" to "중심과 어깨 거리",
                            "side_left_horizontal_distance_hip" to "중심과 골반 거리",
                            "side_left_horizontal_distance_pinky" to "중심과 새끼 거리",
                            "side_left_horizontal_distance_wrist" to "중심과 손목 거리",

                        )
                        keyPairs.forEach { (key, description) ->
                            val angleValue = measureResult.find { it.key == key }?.value

                            if (angleValue != null ) {
                                minResultData.add(Triple(description, "거리: ${String.format("%.1f", angleValue)}cm", null))
                            }
                        }
                        setBothAdapter(minResultData)
                    }
                }
            }
            4 -> {
                when (toVerti) {
                    true -> {
                        val keyPairs = mapOf(
                            "side_right_vertical_angle_shoulder_elbow" to "어깨와 팔꿈치 기울기",
                            "side_right_vertical_angle_elbow_wrist" to "팔꿈치와 손목 기울기",
                            "side_right_vertical_angle_hip_knee" to "골반과 무릎 기울기",
                            "side_right_vertical_angle_ear_shoulder" to "귀와 어깨 기울기",
                            "side_right_vertical_angle_nose_shoulder" to "코와 어깨 기울기",
                            "side_right_vertical_angle_shoulder_elbow_wrist" to "어깨-팔꿉-손목 기울기",
                            "side_right_vertical_angle_hip_knee_ankle" to "엉덩이-무릎-발목 기울기"
                        )
                        keyPairs.forEach { (key, description) ->
                            val angleValue = measureResult.find { it.key == key }?.value

                            if (angleValue != null ) {
                                minResultData.add(Triple(description, "기울기: ${String.format("%.1f", angleValue)}°", null))
                            }
                        }
                        setBothAdapter(minResultData)
                    }
                    false -> {
                        val keyPairs = mapOf(
                            "side_right_horizontal_distance_shoulder" to "중심과 어깨 거리",
                            "side_right_horizontal_distance_hip" to "중심과 골반 거리",
                            "side_right_horizontal_distance_pinky" to "중심과 새끼 거리",
                            "side_right_horizontal_distance_wrist" to "중심과 손목 거리",

                            )
                        keyPairs.forEach { (key, description) ->
                            val angleValue = measureResult.find { it.key == key }?.value

                            if (angleValue != null ) {
                                minResultData.add(Triple(description, "기울기: ${String.format("%.1f", angleValue)}°", null))
                            }
                        }
                        setBothAdapter(minResultData)
                    }
                }
            }
            5 -> {
                when (toVerti) {
                    true -> {
                        val keyPairs = mapOf(
                            "back_vertical_angle_shoudler_center_hip" to "골반중심과 어깨 기울기",
                            "back_vertical_angle_nose_center_hip" to "골반중심과 코 기울기",
                            "back_vertical_angle_nose_center_shoulder" to "어깨중심과 코 기울기",
                            )
                        val distancePairs = listOf(
                            Triple("중심에서 양 손목 거리", "back_vertical_angle_knee_heel_left", "back_vertical_angle_knee_heel_right"),
                        )
                        keyPairs.forEach { (key, description) ->
                            val angleValue = measureResult.find { it.key == key }?.value

                            if (angleValue != null ) {
                                minResultData.add(Triple(description, "기울기: ${String.format("%.1f", angleValue)}°", null))
                            }
                        }
                        distancePairs.forEach{ (descrption, leftKey, rightKey) ->
                            val leftValue = measureResult.find { it.key == leftKey }?.value
                            val rightValue = measureResult.find { it.key == rightKey }?.value
                            if (leftValue != null && rightValue != null) {
                                minResultData.add(Triple(descrption, "왼쪽: ${String.format("%.1f", leftValue)}cm", "오른쪽: ${String.format("%.1f", rightValue)}cm"))
                            }
                        }
                        setBothAdapter(minResultData)
                    }
                    false -> {
                        val anglePairs = listOf(
                            "ear" to "귀", "shoulder" to "어깨", "elbow" to "팔꿉", "wrist" to "손목", "hip" to "골반", "knee" to "무릎", "ankle" to "발목"
                        ).map { (part, description) ->
                            Triple(
                                "양 $description 기울기 높이 차",
                                "back_horizontal_angle_$part",
                                "back_horizontal_distance_sub_$part"
                            )
                        }
                        val distancePairs = listOf(
                            Triple("중심에서 양 손목 거리", "back_horizontal_distance_wrist_left", "back_horizontal_distance_wrist_right"),
                            Triple("중심에서 양 무릎 거리", "back_horizontal_distance_knee_left", "back_horizontal_distance_knee_right"),
                            Triple("중심에서 양 발목 거리", "front_horizontal_distance_ankle_left", "front_horizontal_distance_ankle_right")
                        )

                        anglePairs.forEach { (description, angleKey, distanceKey) ->
                            val angleValue = measureResult.find { it.key == angleKey }?.value
                            val distanceValue = measureResult.find { it.key == distanceKey }?.value
                            if (angleValue != null && distanceValue != null) {
                                minResultData.add(Triple(description, "기울기: ${String.format("%.1f", angleValue)}°", "높이 차: ${String.format("%.1f", distanceValue)}cm"))
                            }
                        }
                        distancePairs.forEach{ (descrption, leftKey, rightKey) ->
                            val leftValue = measureResult.find { it.key == leftKey }?.value
                            val rightValue = measureResult.find { it.key == rightKey }?.value
                            if (leftValue != null && rightValue != null) {
                                minResultData.add(Triple(descrption,"왼쪽: ${String.format("%.1f", leftValue)}cm", "오른쪽: ${String.format("%.1f", rightValue)}cm"))
                            }
                        }
                        setBothAdapter(minResultData)
                    }
                }
            }
            6 -> {
                when (toVerti) {
                    true -> {
                        val keyPairs = mapOf(
                            "back_sit_vertical_angle_nose_left_shoulder_right_shoulder" to "코-왼쪽어깨-오른쪽어깨 기울기",
                            "back_sit_vertical_angle_left_shoulder_right_shoulder_nose" to "왼쪽어깨-오른쪽어깨-코 기울기",
                            "back_sit_vertical_angle_right_shoulder_left_shoulder_nose" to "오른쪽어깨-코-왼쪽어깨 기울기",
                            "back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder" to "왼쪽어깨-골반중심-오른쪽어깨 기울기",
                            "back_sit_vertical_angle_center_hip_right_shoulder_left_shoulder" to "골반중심-오른쪽어깨-왼쪽어깨 기울기",
                            "back_sit_vertical_angle_right_shoulder_left_shoulder_center_hip" to "오른쪽어깨-왼쪽어깨-골반중심 기울기",
                            "back_sit_vertical_angle_shoulder_center_hip" to "어깨와 골반중심 기울기",
                        )
                        keyPairs.forEach { (key, description) ->
                            val angleValue = measureResult.find { it.key == key }?.value

                            if (angleValue != null ) {
                                minResultData.add(Triple(description, "기울기: ${String.format("%.1f", angleValue)}°", null))
                            }
                        }
                        setBothAdapter(minResultData)
                    }
                    false -> {
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
                            val angleValue = measureResult.find { it.key == angleKey }?.value
                            val distanceValue = measureResult.find { it.key == distanceKey }?.value
                            if (angleValue != null && distanceValue != null) {
                                minResultData.add(Triple(description, "기울기: ${String.format("%.1f", angleValue)}°", "높이 차: ${String.format("%.1f", distanceValue)}cm"))
                            }
                        }

                        setBothAdapter(minResultData)
                    }
                }
            }

        }

//        setBothAdapter()
        // 정면의 수직분석 --> 전부 angle임 그럼 높이 차라는 건 없음. 왼쪽 오른쪽 기울기만 있을 뿐
        // 정면의 수평분석 --> 수평이되야 양 어꺠 기울기, 높이 차 가능.
        // 측면의 수직분석 --> 각도들
        // 측면의 수평분석 -> x축부터 거리
        // 후면의 수평분석 -> 기울기, 높이차 있음
        // 후면의
        // 앉아 후면의 수평 -> 각도, 거리
        // 앉아 후면의 수직 -> 각도가
    }

    private fun setDynamicUI(isDynamic: Boolean) {
        if (isDynamic) {
            binding.llMPVertical.visibility = View.GONE
            binding.llMPHorizontal.visibility = View.GONE
            binding.flMP.visibility = View.GONE
        } else {
            binding.llMPVertical.visibility = View.VISIBLE
            binding.llMPHorizontal.visibility = View.VISIBLE
            binding.flMP.visibility = View.VISIBLE
        }
    }

    private fun setBothAdapter(allData: MutableList<Triple<String,String, String?>>) {
        val linearLayoutManager1 = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        val leftData = allData.filterIndexed { index, _ -> index % 2 == 0 }.toMutableList()
        val leftadapter = MeasureDataRVAdapter(this@MeasureAnalysisFragment, leftData)
        binding.rvMPLeft.layoutManager = linearLayoutManager1
        binding.rvMPLeft.adapter = leftadapter

        val linearLayoutManager2 = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val rightData = allData.filterIndexed { index, _ -> index % 2 == 1 }.toMutableList()
        val rightadapter = MeasureDataRVAdapter(this@MeasureAnalysisFragment, rightData)
        binding.rvMPRight.layoutManager = linearLayoutManager2
        binding.rvMPRight.adapter = rightadapter


    }

    private fun initToggleIndicator() {
        val buttonWidth = binding.btgMP.width / 2
        val params = binding.toggleIndicator.layoutParams
        params.width = buttonWidth - 36
        params.height = binding.btgMP.height - 36
        binding.toggleIndicator.layoutParams = params

        // 초기 위치 설정
        binding.toggleIndicator.x = 24f
    }

    private fun animateIndicator(toLeft: Boolean) {
        val animator = ValueAnimator.ofFloat(
            binding.toggleIndicator.x,
            if (toLeft) 24f else (binding.btgMP.width - binding.toggleIndicator.width - 24f)
        )

        animator.addUpdateListener { animation ->
            binding.toggleIndicator.x = animation.animatedValue as Float
        }

        animator.duration = 250
        animator.start()

        animator.addListener(object: Animator.AnimatorListener{
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                when (toLeft) {
                    true -> {
                        setToggleText(R.color.subColor800, R.color.subColor400)
                    }
                    false -> {
                        setToggleText(R.color.subColor400, R.color.subColor800)
                    }
                }
            }
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
    }

    private fun setToggleText(color1 : Int, color2: Int) {
        val enabledDrawable1 = ContextCompat.getDrawable(requireContext(), R.drawable.icon_vertical)
        enabledDrawable1?.let {
            val wrappedDrawable = DrawableCompat.wrap(it).mutate()
            DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(requireContext(), color1))
            binding.ivMPVertical.setImageDrawable(wrappedDrawable)

        }
        binding.tvMPVertical.setTextColor(ContextCompat.getColor(requireContext(), color1))

        val enabledDrawable2 = ContextCompat.getDrawable(requireContext(), R.drawable.icon_horizontal)
        enabledDrawable2?.let {
            val wrappedDrawable = DrawableCompat.wrap(it).mutate()
            DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(requireContext(), color2))
            binding.ivMPHorizontal.setImageDrawable(wrappedDrawable)
        }
        binding.tvMPHorizontal.setTextColor(ContextCompat.getColor(requireContext(), color2))
    }

    private fun getSequence(index: Int) : Pair<Int, Int> {
        return when (index) {
            0 -> Pair(0, 1)
            1 -> Pair(3, 4)
            2 -> Pair(5, 6)
            else -> Pair(2, -1)
        }
    }
}