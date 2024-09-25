package com.tangoplus.tangoq.fragment

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.DataDynamicRVAdapter
import com.tangoplus.tangoq.adapter.DataStaticRVAdapter
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureAnalysisBinding
import com.tangoplus.tangoq.mediapipe.ImageProcessingUtility
import com.tangoplus.tangoq.mediapipe.OverlayView
import com.tangoplus.tangoq.mediapipe.PoseLandmarkResult.Companion.fromCoordinates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.min

class MeasureAnalysisFragment : Fragment() {
    lateinit var binding : FragmentMeasureAnalysisBinding
    private val viewModel : MeasureViewModel by activityViewModels()
    private lateinit var measureResult: JSONArray
    private lateinit var mr : JSONArray
    private var allData = mutableListOf<Triple<String, Double, Double>>()
    private var index = -1
    private var currentSequence = -1
    private lateinit var combinedBitmap : Bitmap
    private var scaleFactorX =  0f
    private var scaleFactorY = 0f

    private var count = false

    // 영상재생
    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var videoUrl = "http://gym.tangostar.co.kr/data/contents/videos/걷기.mp4"
    private lateinit var jsonArray: JSONArray
    private var hideNotice = false

    companion object {
        private const val ARG_INDEX = "index_analysis"
        private const val ARG_SCORE = "index_score"
        private const val ARG_COMMENT = "index_comment"
        fun newInstance(indexx: Int, balance: Pair<Int, String>): MeasureAnalysisFragment {
            val fragment = MeasureAnalysisFragment()
            val args = Bundle()
            args.putInt(ARG_INDEX, indexx)
            args.putInt(ARG_SCORE, balance.first)
            args.putString(ARG_COMMENT, balance.second)

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
        measureResult = JSONArray()
        index = arguments?.getInt(ARG_INDEX)!!
        mr = viewModel.selectedMeasure!!.measureResult

        Log.v("현재측정", "${viewModel.selectedMeasure!!.regDate}")
        val noticeScore = arguments?.getInt(ARG_SCORE)
        val noticeComment = arguments?.getString(ARG_COMMENT)
        setColor(noticeScore!!)
        binding.tvMAPredict.text = noticeComment.toString()
        // ------# 상단 snackbar 형식 가져오기 #------


        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val slideDownAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_down)
                binding.clMANotice.visibility = View.VISIBLE
                binding.clMANotice.startAnimation(slideDownAnimation)
                hideNotice = false
            } catch (e: Exception) {
                Log.e("not Attached Exception", "${e.message}")
            }
        }, 600)
        binding.clMANotice.setOnClickListener {
            if (!hideNotice) {
                // 숨기기
                val slideUpAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
                slideUpAnimation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}
                    override fun onAnimationRepeat(animation: Animation?) {}
                    override fun onAnimationEnd(animation: Animation?) {
                        binding.clMANotice.visibility = View.INVISIBLE
                    }
                })
                binding.clMANotice.startAnimation(slideUpAnimation)
                hideNotice = true
            } else {
                // 보이기
                val slideDownAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_down)
                binding.clMANotice.visibility = View.VISIBLE
                binding.clMANotice.startAnimation(slideDownAnimation)
                hideNotice = false
            }
        }

        // ------# 1차 필터링 (균형별) #------
        // 0, 1, 2, 3으로 3가지.
        when (index) {

            0 -> { // 정면 균형
                measureResult.put(mr.optJSONObject(0))
                measureResult.put(mr.optJSONObject(2))
                setDynamicUI(false)
                binding.tvMPTitle.visibility = View.GONE
                binding.tlMP.getTabAt(0)?.text = "정면 측정"
                binding.tlMP.getTabAt(1)?.text = "팔꿉 측정"
                binding.clMA.visibility = View.VISIBLE
                binding.flMA.visibility = View.GONE
                currentSequence = 0
                setImage(currentSequence)
                switchScreenData(measureResult, currentSequence,true)
                binding.ivMA.setImageResource(R.drawable.drawable_front)
                binding.tvMAName.text = "정면 균형"
            }
            1 -> { // 측면 균형
                measureResult.put(mr.optJSONObject(3))
                measureResult.put(mr.optJSONObject(4))
                setDynamicUI(false)
                binding.tlMP.visibility = View.INVISIBLE
                binding.tvMPTitle.visibility = View.VISIBLE
                binding.tvMPTitle.text = "측면 측정"
                binding.clMA.visibility = View.VISIBLE
                binding.flMA.visibility = View.GONE
                currentSequence = 3
                setImage(currentSequence)
                switchScreenData(measureResult,  currentSequence,true)
                binding.ivMA.setImageResource(R.drawable.drawable_side)
                binding.tvMAName.text = "측면 균형"
            }
            2 -> { // 후면 균형
                measureResult.put(mr.optJSONObject(5))
                setDynamicUI(false)
                binding.tlMP.visibility = View.INVISIBLE
                binding.tvMPTitle.visibility = View.VISIBLE
                binding.tvMPTitle.text = "후면 측정"
                binding.clMA.visibility = View.VISIBLE
                binding.flMA.visibility = View.GONE
                currentSequence = 5
                setImage(currentSequence)
                switchScreenData(measureResult,  currentSequence,true)
                binding.ivMA.setImageResource(R.drawable.drawable_back)
                binding.tvMAName.text = "후면 균형"

            }
            3 -> {
                measureResult.put(mr.optJSONObject(6))
                setDynamicUI(false)
                binding.tlMP.visibility = View.INVISIBLE
                binding.tvMPTitle.visibility = View.VISIBLE
                binding.tvMPTitle.text = "앉은 후면"
                binding.clMA.visibility = View.VISIBLE
                binding.flMA.visibility = View.GONE
                currentSequence = 6
                setImage(currentSequence)
                switchScreenData(measureResult,  currentSequence,true)
                binding.ivMA.setImageResource(R.drawable.drawable_back)

                binding.tvMAName.text = "앉은 후면 "
            }
            4 -> { // 동적 균형
                measureResult.put(mr.optJSONArray(1))
                setDynamicUI(true)
                binding.tlMP.getTabAt(0)?.text = "동적 측정"
                binding.tlMP.getTabAt(1)?.text = ""
                binding.tlMP.setSelectedTabIndicatorColor(ContextCompat.getColor(requireContext(), R.color.subColor400))
                binding.tlMP.background = null
                binding.clMA.visibility = View.GONE
                binding.flMA.visibility = View.VISIBLE
                binding.flMA2.visibility = View.GONE
                currentSequence = 1

                setPlayer()
                switchScreenData(measureResult, currentSequence,true)
                binding.ivMA.setImageResource(R.drawable.drawable_dynamic)
            }

        }

        // ------# 1-1차 필터링(tabLayout의 첫번째) #------
        binding.tlMP.addOnTabSelectedListener(object: OnTabSelectedListener{
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
            override fun onTabSelected(tab: TabLayout.Tab?) {
                count = false
                if (index != 3) {
                    when (tab?.position) {
                        0 -> {
                            // 데이터, 시퀀스(0~6단계), toVerti
                            currentSequence = getSequence(index).first
                            switchScreenData(measureResult, currentSequence,true)
                            animateIndicator(true)
                            setImage(currentSequence)
                        }
                        1 -> {

                            currentSequence = getSequence(index).second
                            switchScreenData(measureResult, currentSequence,true)
                            animateIndicator(true)
                            setImage(currentSequence)

                        }
                    }
                }

            }

        })

        // ------! 하단 버튼토글 그룹 시작 !------
        binding.btgMP.check(R.id.btnMA1)
        binding.btgMP.addOnButtonCheckedListener{ _, checkedId, isChecked ->

            if (isChecked) {
                when (checkedId) {
                    R.id.btnMA1 -> {
                        animateIndicator(true)
                        switchScreenData(measureResult, currentSequence,true)
                    }
                    R.id.btnMA2 -> {
                        animateIndicator(false)
                        switchScreenData(measureResult, currentSequence,false)
                    }
                }

            }
        }
        binding.btnMA1.setOnClickListener { binding.btgMP.check(R.id.btnMA1) }
        binding.btnMA2.setOnClickListener { binding.btgMP.check(R.id.btnMA2) }

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
    private fun switchScreenData(measureResult: JSONArray, sequence: Int, toVerti: Boolean) {

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
                            val leftValue = measureResult.optJSONObject(0).optDouble("${key}_left")
                            val rightValue = measureResult.optJSONObject(0).optDouble("${key}_right")

                            if (!leftValue.isNaN() && !rightValue.isNaN()) {
                                Triple(description, "왼쪽: ${String.format("%.1f", leftValue)}°", "오른쪽: ${String.format("%.1f", rightValue)}°")
                            } else null
                        }.toMutableList()
                        set012Adapter(minResultData)

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
                            val angleValue = measureResult.optJSONObject(0).optDouble(angleKey)
                            val distanceValue = measureResult.optJSONObject(0).optDouble(distanceKey)
                            if (!angleValue.isNaN() && !distanceValue.isNaN()) {
                                minResultData.add(Triple(description, "기울기: ${String.format("%.1f", angleValue)}°", "높이 차: ${String.format("%.1f", distanceValue)}cm"))
                            }
                        }
                        distancePairs.forEach{ (descrption, leftKey, rightKey) ->
                            val leftValue = measureResult.optJSONObject(0).optDouble(leftKey)
                            val rightValue = measureResult.optJSONObject(0).optDouble(rightKey)
                            if (!leftValue.isNaN() && !rightValue.isNaN()) {
                                minResultData.add(Triple(descrption, "왼쪽: ${String.format("%.1f", leftValue)}cm", "오른쪽: ${String.format("%.1f", rightValue)}cm"))
                            }
                        }
                        set012Adapter(minResultData)
                    }
                }

            }
            1 -> {
                lifecycleScope.launch {
                    val connections = listOf(
                        15, 16, 23, 24, 25, 26
                    )
                    val coordinates = extractVideoCoordinates(measureResult.optJSONArray(0))
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
            2 -> {
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
                            val leftValue = measureResult.optJSONObject(1).optDouble(leftKey)
                            val rightValue = measureResult.optJSONObject(1).optDouble(rightKey)
                            if (!leftValue.isNaN() && !rightValue.isNaN()) {
                                minResultData.add(Triple(descrption, "왼쪽: ${String.format("%.1f", leftValue)}°", "오른쪽: ${String.format("%.1f", rightValue)}°"))
                            }
                        }
                        val distancePairs = listOf(
                            Triple("검지관절과 새끼관절 거리", "front_hand_distance_index_pinky_mcp_left", "front_hand_distance_index_pinky_mcp_right"),
                            Triple("어깨와 중지 거리", "front_elbow_align_distance_shoulder_mid_index_left", "front_elbow_align_distance_shoulder_mid_index_right"),
                            Triple("중심과 중지 거리", "front_elbow_align_distance_center_mid_finger_left", "front_elbow_align_distance_center_mid_finger_right"),
                            Triple("중심과 손목 거리", "front_elbow_align_distance_center_wrist_left", "front_elbow_align_distance_center_wrist_right"))


                        distancePairs.forEach{ (descrption, leftKey, rightKey) ->
                            val leftValue = measureResult.optJSONObject(1).optDouble(leftKey)
                            val rightValue = measureResult.optJSONObject(1).optDouble(rightKey)
                            if (!leftValue.isNaN() && !rightValue.isNaN()) {
                                minResultData.add(Triple(descrption,"왼쪽: ${String.format("%.1f", leftValue)}cm", "오른쪽: ${String.format("%.1f", rightValue)}cm"))
                            }
                        }
                        set012Adapter(minResultData)
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
                            val angleValue = measureResult.optJSONObject(1).optDouble(angleKey)
                            val distanceValue = measureResult.optJSONObject(1).optDouble(distanceKey)
                            if (!angleValue.isNaN() && !distanceValue.isNaN()) {
                                minResultData.add(Triple(description, "기울기: ${String.format("%.1f", angleValue)}°", "높이 차: ${String.format("%.1f", distanceValue)}cm"))
                            }
                        }
                        distancePairs.forEach{ (descrption, leftKey, rightKey) ->
                            val leftValue = measureResult.optJSONObject(1).optDouble(leftKey)
                            val rightValue = measureResult.optJSONObject(1).optDouble(rightKey)
                            if (!leftValue.isNaN() && !rightValue.isNaN()) {
                                minResultData.add(Triple(descrption, "왼쪽: ${String.format("%.1f", leftValue)}cm", "오른쪽: ${String.format("%.1f", rightValue)}cm"))
                            }
                        }
                        set012Adapter(minResultData)
                    }
                }


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
                            val angleValue = measureResult.optJSONObject(0).optDouble(key)

                            if (!angleValue.isNaN() ) {
                                minResultData.add(Triple(description, "기울기: ${String.format("%.1f", angleValue)}°", null))
                            }
                        }
                        set012Adapter(minResultData)
                    }
                    false -> {
                        val keyPairs = mapOf(
                            "side_left_horizontal_distance_shoulder" to "중심과 어깨 거리",
                            "side_left_horizontal_distance_hip" to "중심과 골반 거리",
                            "side_left_horizontal_distance_pinky" to "중심과 새끼 거리",
                            "side_left_horizontal_distance_wrist" to "중심과 손목 거리",

                        )
                        keyPairs.forEach { (key, description) ->
                            val angleValue = measureResult.optJSONObject(0).optDouble(key)

                            if (!angleValue.isNaN()) {
                                minResultData.add(Triple(description, "거리: ${String.format("%.1f", angleValue)}cm", null))
                            }
                        }
                        set012Adapter(minResultData)
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
                            val angleValue = measureResult.optJSONObject(1).optDouble(key)

                            if (!angleValue.isNaN()) {
                                minResultData.add(Triple(description, "기울기: ${String.format("%.1f", angleValue)}°", null))
                            }
                        }
                        set012Adapter(minResultData)
                    }
                    false -> {
                        val keyPairs = mapOf(
                            "side_right_horizontal_distance_shoulder" to "중심과 어깨 거리",
                            "side_right_horizontal_distance_hip" to "중심과 골반 거리",
                            "side_right_horizontal_distance_pinky" to "중심과 새끼 거리",
                            "side_right_horizontal_distance_wrist" to "중심과 손목 거리",

                            )
                        keyPairs.forEach { (key, description) ->
                            val angleValue = measureResult.optJSONObject(1).optDouble(key)

                            if (!angleValue.isNaN() ) {
                                minResultData.add(Triple(description, "기울기: ${String.format("%.1f", angleValue)}°", null))
                            }
                        }
                        set012Adapter(minResultData)
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
                            val angleValue = measureResult.optJSONObject(0).optDouble(key)

                            if (!angleValue.isNaN() ) {
                                minResultData.add(Triple(description, "기울기: ${String.format("%.1f", angleValue)}°", null))
                            }
                        }
                        distancePairs.forEach{ (descrption, leftKey, rightKey) ->
                            val leftValue = measureResult.optJSONObject(0).optDouble(leftKey)
                            val rightValue = measureResult.optJSONObject(0).optDouble(rightKey)
                            if (!leftValue.isNaN() && !rightValue.isNaN()) {
                                minResultData.add(Triple(descrption, "왼쪽: ${String.format("%.1f", leftValue)}cm", "오른쪽: ${String.format("%.1f", rightValue)}cm"))
                            }
                        }
                        set012Adapter(minResultData)
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
                            val angleValue = measureResult.optJSONObject(0).optDouble(angleKey)
                            val distanceValue = measureResult.optJSONObject(0).optDouble(distanceKey)
                            if (!angleValue.isNaN() && !distanceValue.isNaN()) {
                                minResultData.add(Triple(description, "기울기: ${String.format("%.1f", angleValue)}°", "높이 차: ${String.format("%.1f", distanceValue)}cm"))
                            }
                        }
                        distancePairs.forEach{ (descrption, leftKey, rightKey) ->
                            val leftValue = measureResult.optJSONObject(0).optDouble(leftKey)
                            val rightValue = measureResult.optJSONObject(0).optDouble(rightKey)
                            if (!leftValue.isNaN() && !rightValue.isNaN() ) {
                                minResultData.add(Triple(descrption,"왼쪽: ${String.format("%.1f", leftValue)}cm", "오른쪽: ${String.format("%.1f", rightValue)}cm"))
                            }
                        }
                        set012Adapter(minResultData)
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
                            val angleValue = measureResult.optJSONObject(0).optDouble(key)

                            if (!angleValue.isNaN() ) {
                                minResultData.add(Triple(description, "기울기: ${String.format("%.1f", angleValue)}°", null))
                            }
                        }
                        set012Adapter(minResultData)
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
                            val angleValue = measureResult.optJSONObject(0).optDouble(angleKey)
                            val distanceValue = measureResult.optJSONObject(0).optDouble(distanceKey)
                            if (!angleValue.isNaN() && !distanceValue.isNaN()) {
                                minResultData.add(Triple(description, "기울기: ${String.format("%.1f", angleValue)}°", "높이 차: ${String.format("%.1f", distanceValue)}cm"))
                            }
                        }

                        set012Adapter(minResultData)
                    }
                }
            }

        }

//        set012Adapter()
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
            binding.llMAVertical.visibility = View.GONE
            binding.llMAHorizontal.visibility = View.GONE
            binding.flMA.visibility = View.GONE
        } else {
            binding.llMAVertical.visibility = View.VISIBLE
            binding.llMAHorizontal.visibility = View.VISIBLE
            binding.flMA.visibility = View.VISIBLE
        }
    }

    private fun set012Adapter(allData: MutableList<Triple<String,String, String?>>) {
        val linearLayoutManager1 = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        val leftData = allData.filterIndexed { index, _ -> index % 2 == 0 }.toMutableList()
        val leftadapter = DataStaticRVAdapter(leftData)
        binding.rvMALeft.layoutManager = linearLayoutManager1
        binding.rvMALeft.adapter = leftadapter

        val linearLayoutManager2 = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val rightData = allData.filterIndexed { index, _ -> index % 2 == 1 }.toMutableList()
        val rightadapter = DataStaticRVAdapter(rightData)
        binding.rvMARight.layoutManager = linearLayoutManager2
        binding.rvMARight.adapter = rightadapter
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
            binding.ivMAVertical.setImageDrawable(wrappedDrawable)

        }
        binding.tvMAVertical.setTextColor(ContextCompat.getColor(requireContext(), color1))

        val enabledDrawable2 = ContextCompat.getDrawable(requireContext(), R.drawable.icon_horizontal)
        enabledDrawable2?.let {
            val wrappedDrawable = DrawableCompat.wrap(it).mutate()
            DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(requireContext(), color2))
            binding.ivMAHorizontal.setImageDrawable(wrappedDrawable)

        }
        binding.tvMAHorizontal.setTextColor(ContextCompat.getColor(requireContext(), color2))
    }

    private fun getSequence(index: Int) : Pair<Int, Int> {
        return when (index) {
            0 -> Pair(0, 2)
            1 -> Pair(3, 4)
            2 -> Pair(5, 6)
            else -> Pair(1, -1)
        }
    }

    //---------------------------------------! imageOverlay !---------------------------------------
    private fun extractImageCoordinates(jsonData: JSONObject): List<Pair<Float, Float>> {
        val poseData = jsonData.optJSONArray("pose_landmark")
        return List(poseData.length()) { i ->
            val landmark = poseData.getJSONObject(i)
            Pair(
                landmark.getDouble("sx").toFloat(),
                landmark.getDouble("sy").toFloat()
            )
        }
    }
    /* TODO 현재 File 형식으로 받아오는중 (assets폴더에서 가져오기 때문)
    *   추후 API에서는 그냥 URL일 수 있음. 이부분이 생략되고 ,setImage(url), initPlayer(videoUrl)일 수 있음. */
    private fun setImage(seq: Int) {
        lifecycleScope.launch {
            try {
                val jsonData = viewModel.selectedMeasure?.measureResult?.optJSONObject(seq)
                Log.v("measureResult", "${jsonData}")
                val coordinates = extractImageCoordinates(jsonData!!)
                Log.v("모바일POSELANDMARK", "jsonData: ${jsonData.optJSONArray("pose_landmark")}, coordinates: ${coordinates}")
                val imageUrls = viewModel.selectedMeasure?.fileUris?.get(seq)
                if (imageUrls != null) {
                    lifecycleScope.launch {
                        try {
                            val imageFile = imageUrls.let { File(it) }
                            val bitmap = BitmapFactory.decodeFile(imageUrls)

                            withContext(Dispatchers.Main) {
                                binding.ssivMA.setImage(ImageSource.uri(imageFile.toUri().toString()))
                                binding.ssivMA.setOnImageEventListener(object : SubsamplingScaleImageView.OnImageEventListener {
                                    override fun onReady() {

                                        if (!count) {
                                            // ssiv 이미지뷰의 크기
                                            val imageViewWidth = binding.ssivMA.width
                                            val imageViewHeight = binding.ssivMA.height

                                            // iv에 들어간 image의 크기 같음 screenWidth
                                            val sWidth = binding.ssivMA.sWidth
                                            val sHeight = binding.ssivMA.sHeight

                                            // 스케일 비율 계산
                                            val scaleFactor = min(imageViewWidth / sWidth.toFloat(), imageViewHeight / sHeight.toFloat())

                                            // 오프셋 계산 (뷰 크기 대비 이미지 크기의 여백)
                                            val offsetX = (imageViewWidth - sWidth * scaleFactor) / 2f
                                            val offsetY = (imageViewHeight - sHeight * scaleFactor) / 2f

                                            val poseLandmarkResult = fromCoordinates(coordinates)

                                            val combinedBitmap = ImageProcessingUtility.combineImageAndOverlay(
                                                bitmap,
                                                poseLandmarkResult,
                                                scaleFactor,
                                                offsetX,
                                                offsetY,
                                                requireContext(),
                                                seq
                                            )
                                            count = true
                                            binding.ssivMA.setImage(ImageSource.bitmap(combinedBitmap))

                                            // count를 true로 설정하여 다시 호출되지 않도록 제어
                                        }
                                    }

                                    override fun onImageLoaded() {}
                                    override fun onPreviewLoadError(e: Exception?) {}
                                    override fun onImageLoadError(e: Exception?) {}
                                    override fun onTileLoadError(e: Exception?) {}
                                    override fun onPreviewReleased() {}
                                })
                            }
                            Log.v("ImageLoading", "Image loaded successfully. Width: ${bitmap.width}, Height: ${bitmap.height}")
                        } catch (e: Exception) {
                            Log.e("ImageLoading", "Error loading image: ${e.message}", e)
                        }
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("Error", "${e}")
            }
        }
    }
    private fun getRealPathFromUri(context: Context, uri: Uri) : String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursur = context.contentResolver.query(uri, projection, null, null, null)
        cursur?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                return it.getString(columnIndex)
            }
        }
        return null
    }
    //---------------------------------------! VideoOverlay !---------------------------------------
    private fun extractVideoCoordinates(jsonData: JSONArray) : List<List<Pair<Float,Float>>> {

        return List(jsonData.length()) { i ->
            val landmarks = jsonData.getJSONObject(i).getJSONArray("pose_landmark")
            List(landmarks.length()) { j ->
                val landmark = landmarks.getJSONObject(j)
                Pair(
                    landmark.getDouble("sx").toFloat(),
                    landmark.getDouble("sy").toFloat()
                )
            }
        }
    }

    private fun setPlayer() {
        lifecycleScope.launch {

            jsonArray = viewModel.selectedMeasure?.measureResult!!.getJSONArray(1)
            Log.v("jsonDataLength", "${jsonArray.length()}")
            initPlayer()

            simpleExoPlayer?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    if (playbackState == Player.STATE_READY) {
                        val videoDuration = simpleExoPlayer?.duration ?: 0L
                        lifecycleScope.launch {
                            while (simpleExoPlayer?.isPlaying == true) {
                                updateFrameData(videoDuration, jsonArray.length())
                                delay(24)

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

    }

    private fun updateFrameData(videoDuration: Long, totalFrames: Int) {
        val currentPosition = simpleExoPlayer?.currentPosition ?: 0L

        // 현재 재생 시간에 해당하는 프레임 인덱스 계산
        val frameIndex = ((currentPosition.toFloat() / videoDuration) * totalFrames).toInt()
        val coordinates = extractVideoCoordinates(jsonArray)


        // 실제 mp4의 비디오 크기를 가져온다
        val (videoWidth, videoHeight) = getVideoDimensions(videoUrl.toUri())
//        Log.v("widhtHeight","(${binding.tlMP.width},${binding.tlMP.height}) , (${binding.ovMA.width}, ${binding.ovMA.height})")
        if (frameIndex in 0 until totalFrames) {
            // 해당 인덱스의 데이터를 JSON에서 추출하여 변환
            val poseLandmarkResult = fromCoordinates(coordinates[frameIndex])

            // 변환된 데이터를 화면에 그리기
            requireActivity().runOnUiThread {

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

    private fun getVideoDimensions(videoUri: Uri) : Pair<Int, Int> {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(requireContext(), videoUri)
        val videoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
        val videoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
        retriever.release()
        return Pair(videoWidth, videoHeight)
    }

    private fun setVideoAdapter(data: List<List<Pair<Float, Float>>>) {
        val titles = listOf("좌측 손", "우측 손", "좌측 골반", "우측 골반", "좌측 무릎", "우측 무릎") // 0 , 1 , 2

        val linearLayoutManager1 = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val leftData = data.filterIndexed { index, _ -> index % 2 == 0 }.toMutableList()
        val leftTitle = titles.filterIndexed{ index, _ -> index % 2 == 0}
        val leftadapter = DataDynamicRVAdapter(leftData, leftTitle)
        binding.rvMALeft.layoutManager = linearLayoutManager1
        binding.rvMALeft.adapter = leftadapter

        val linearLayoutManager2 = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val rightData = data.filterIndexed { index, _ -> index % 2 == 1 }.toMutableList()
        val rightTitle = titles.filterIndexed{ index, _ -> index % 2 == 1}
        val rightadapter = DataDynamicRVAdapter(rightData, rightTitle)
        binding.rvMARight.layoutManager = linearLayoutManager2
        binding.rvMARight.adapter = rightadapter



    }

    override fun onStop() {
        super.onStop()
        simpleExoPlayer?.stop()
        simpleExoPlayer?.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        simpleExoPlayer?.release()
//        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    override fun onResume() {
        super.onResume()
        simpleExoPlayer?.playWhenReady = true
    }

    private fun setColor(index: Int) {
        when (index) {
            2 -> {
                binding.tvMAPredict.setTextColor(ContextCompat.getColor(requireContext(), R.color.deleteColor))
                binding.ivMAicon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_caution))
            }
            1 -> {
                binding.tvMAPredict.setTextColor(ContextCompat.getColor(requireContext(), R.color.cautionColor))
                binding.ivMAicon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_warning))
            }
            0 -> {
                binding.tvMAPredict.setTextColor(ContextCompat.getColor(requireContext(), R.color.subColor400))
                binding.ivMAicon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_ball))

            }
        }
    }


}