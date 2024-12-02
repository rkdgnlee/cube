package com.tangoplus.tangoq.function

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.tangoplus.tangoq.data.AnalysisUnitVO
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.db.MeasureInfo
import com.tangoplus.tangoq.mediapipe.ImageProcessingUtil
import com.tangoplus.tangoq.mediapipe.ImageProcessingUtil.cropToPortraitRatio
import com.tangoplus.tangoq.mediapipe.PoseLandmarkResult.Companion.fromCoordinates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import kotlin.coroutines.resume
import kotlin.math.abs

object MeasurementManager {
    val matchedUris = mapOf(
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
            0 to mapOf( "front_horizontal_angle_ear" to Pair(-0.5f, 0.4f)),
            3 to mapOf( "side_left_vertical_angle_nose_shoulder" to Pair(60f, 75f)),
            4 to mapOf( "side_right_vertical_angle_nose_shoulder" to Pair(60f, 75f)),
            5 to mapOf( "back_vertical_angle_nose_center_shoulder" to Pair(-5f, 5f)),
            6 to mapOf( "back_sit_horizontal_angle_ear" to Pair(-0.5f, 0.5f),
                "back_sit_vertical_angle_right_shoulder_nose_left_shoulder" to Pair(40f, 50f))
        ),
        // 어깨
        mapOf(
            0 to mapOf("front_horizontal_angle_shoulder" to Pair(-2f, 2f),
                "front_horizontal_distance_sub_shoulder" to Pair(-1.5f, 1.5f)),
            3 to mapOf("side_left_horizontal_distance_shoulder" to Pair(-2f, 2f)),
            5 to mapOf("back_vertical_angle_shoudler_center_hip" to Pair(88f, 92f)),
            6 to mapOf("back_sit_vertical_angle_shoulder_center_hip" to Pair(85f, 95f))
        ),
        mapOf(
            0 to mapOf("front_horizontal_angle_shoulder" to Pair(-2f, 2f),
                "front_horizontal_distance_sub_shoulder" to Pair(-1.5f, 1.5f)),
            4 to mapOf("side_right_horizontal_distance_shoulder" to Pair(-2f, 2f)),
            5 to mapOf("back_vertical_angle_shoudler_center_hip" to Pair(88f, 92f)),
            6 to mapOf("back_sit_vertical_angle_shoulder_center_hip" to Pair(85f, 95f))
        ),
        // 좌측 팔꿉
        mapOf(
            0 to mapOf("front_horizontal_angle_elbow" to Pair(-1f, 1f),
                "front_horizontal_distance_sub_elbow" to Pair(0.5f, 1.5f),
                "front_vertical_angle_shoulder_elbow_right" to Pair(-5f, 5f)),
            2 to mapOf("front_elbow_align_angle_left_upper_elbow_elbow_wrist" to Pair(25f, 35f),
                "front_elbow_align_angle_left_shoulder_elbow_wrist" to Pair(8f, 18f)),
            3 to mapOf("side_left_vertical_angle_shoulder_elbow" to Pair(0f, 5f),
                "side_left_vertical_angle_elbow_wrist" to Pair(85f, 105f),
                "side_left_vertical_angle_shoulder_elbow_wrist" to Pair(160f, 180f))
        ),
        mapOf(
            0 to mapOf("front_horizontal_angle_elbow" to Pair(40f, 50f),
                "front_horizontal_distance_sub_elbow" to Pair(40f, 50f),
                "front_vertical_angle_shoulder_elbow_left" to Pair(40f, 50f)),
            2 to mapOf("front_elbow_align_angle_right_upper_elbow_elbow_wrist" to Pair(25f, 35f),
                "front_elbow_align_angle_right_shoulder_elbow_wrist" to Pair(8f, 18f)),
            4 to mapOf("side_right_vertical_angle_shoulder_elbow" to Pair(0f, 5f),
                "side_right_vertical_angle_elbow_wrist" to Pair(85f, 105f),
                "side_right_vertical_angle_shoulder_elbow_wrist" to Pair(160f, 180f))
        ),
        // 좌측 손목
        mapOf(
            0 to mapOf("front_vertical_angle_elbow_wrist_left" to Pair(-7.5f, 7.5f),
                "front_horizontal_angle_wrist" to Pair(-0.5f, 0.5f)),
            2 to mapOf("front_elbow_align_angle_mid_index_wrist_elbow_left" to Pair(170f, 190f),
                "front_elbow_align_distance_left_wrist_shoulder" to Pair(-10f, 10f)),
            3 to mapOf("side_left_horizontal_distance_wrist" to Pair(-10f, 10f))
        ),
        mapOf(
            0 to mapOf("front_vertical_angle_elbow_wrist_right" to Pair(-7.5f, 7.5f),
                "front_horizontal_angle_wrist" to Pair(-0.5f, 0.5f)),
            2 to mapOf("front_elbow_align_angle_mid_index_wrist_elbow_right" to Pair(170f, 190f),
                "front_elbow_align_distance_right_wrist_shoulder" to Pair(-10f, 10f)),
            4 to mapOf("side_right_horizontal_distance_wrist" to  Pair(-10f, 10f))
        ),
        // 좌측 골반
        mapOf(
            0 to mapOf("front_vertical_angle_hip_knee_left" to Pair(85f, 95f),
                "front_vertical_angle_hip_knee_ankle_left" to Pair(175f, 185f),
                "front_horizontal_angle_hip" to Pair(-2.5f, 2.5f)),
            3 to mapOf("side_left_vertical_angle_hip_knee" to Pair(85f, 95f),
                "side_left_horizontal_distance_hip" to Pair(-0.5f, 0.5f)
            ),
            5 to mapOf("back_horizontal_angle_hip" to Pair(-1f, 1f)),
            6 to mapOf("back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder" to Pair(40f, 60f),
                "back_sit_vertical_angle_shoulder_center_hip" to Pair(80f, 100f))
        ),
        mapOf(
            0 to mapOf("front_vertical_angle_hip_knee_right" to Pair(85f, 95f),
                "front_vertical_angle_hip_knee_ankle_right" to Pair(175f, 185f),
                "front_horizontal_angle_hip" to Pair(-2.5f, 2.5f)),
            4 to mapOf("side_right_vertical_angle_hip_knee" to Pair(40f, 50f),
                "side_right_horizontal_distance_hip" to Pair(40f, 50f)),
            5 to mapOf("back_horizontal_angle_hip" to Pair(-1f, 1f)),
            6 to mapOf("back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder" to Pair(40f, 60f),
                "back_sit_vertical_angle_shoulder_center_hip" to Pair(80f, 100f))
        ),
        // 좌측 무릎
        mapOf(
            0 to mapOf("front_horizontal_angle_knee" to Pair(-0.5f, 0.5f),
                "front_horizontal_distance_knee_left" to Pair(10f, 30f),
                "front_vertical_angle_hip_knee_ankle_left" to Pair(170f, 190f)),
            5 to mapOf("back_horizontal_angle_knee" to Pair(-0.5f, 0.5f),
                "back_horizontal_distance_knee_left" to Pair(10f, 30f))
        ),
        mapOf(
            0 to mapOf("front_horizontal_angle_knee" to Pair(-0.5f, 0.5f),
                "front_horizontal_distance_knee_right" to Pair(10f, 30f),
                "front_vertical_angle_hip_knee_ankle_right" to Pair(170f, 190f)),
            5 to mapOf("back_horizontal_angle_knee" to Pair(-0.5f, 0.5f),
                "back_horizontal_distance_knee_right" to Pair(10f, 30f))
        ),
        // 좌측 발목
        mapOf(
            0 to mapOf("front_vertical_angle_knee_ankle_left" to Pair(85f, 95f),
                "front_horizontal_angle_ankle" to Pair(-0.5f, 0.5f),
                "front_horizontal_distance_ankle_left" to Pair(10f, 30f)),
            5 to mapOf("back_horizontal_distance_sub_ankle" to Pair(-0.5f, 0.5f),
                "back_horizontal_distance_heel_left" to Pair(5f, 15f))
        ),
        mapOf(
            0 to mapOf("front_vertical_angle_knee_ankle_right" to Pair(85f, 95f),
                "front_horizontal_angle_ankle" to Pair(-0.5f, 0.5f),
                "front_horizontal_distance_ankle_right" to Pair(10f, 30f)),
            5 to mapOf("back_horizontal_distance_sub_ankle" to Pair(-0.5f, 0.5f),
                "back_horizontal_distance_heel_right" to Pair(5f, 15f))
        )
    )

    private val mainPartSeqs = listOf(
        mapOf( // 6
            0 to mapOf( "front_horizontal_angle_ear" to "양 귀 기울기"),
            3 to mapOf("side_left_vertical_angle_nose_shoulder" to "코와 좌측 어깨 기울기"),
            4 to mapOf("side_right_vertical_angle_nose_shoulder" to "코와 우측 어깨 기울기"),
            5 to mapOf("back_vertical_angle_nose_center_shoulder" to "어깨중심과 코 기울기"),
            6 to mapOf( "back_sit_horizontal_angle_ear" to "양 귀 기울기",
                "back_sit_vertical_angle_right_shoulder_nose_left_shoulder" to "우측 어깨-코-좌측 어깨 기울기")
        ),
        mapOf( // 5
            0 to mapOf("front_horizontal_angle_shoulder" to "양 어깨 기울기",
                "front_horizontal_distance_sub_shoulder" to "양 어깨 높이 차"),
            3 to mapOf("side_left_horizontal_distance_shoulder" to "중심과 어깨 거리"),
            5 to mapOf("back_vertical_angle_shoudler_center_hip" to "골반중심과 어깨 기울기"),
            6 to mapOf("back_sit_vertical_angle_shoulder_center_hip" to "어깨와 골반중심 기울기")
        ),
        mapOf( // 5
            0 to mapOf("front_horizontal_angle_shoulder" to "양 어깨 기울기",
                "front_horizontal_distance_sub_shoulder" to "양 어깨 높이 차"),
            4 to mapOf("side_right_horizontal_distance_shoulder" to "중심과 어깨 거리"),
            5 to mapOf("back_vertical_angle_shoudler_center_hip" to "골반중심과 어깨 기울기"),
            6 to mapOf("back_sit_vertical_angle_shoulder_center_hip" to "어깨와 골반중심 기울기")
        ),
        // 좌측 팔꿉  // 8
        mapOf(
            0 to mapOf("front_horizontal_angle_elbow" to "양 팔꿉 기울기",
                "front_horizontal_distance_sub_elbow" to "양 팔꿉 높이 차",
                "front_vertical_angle_shoulder_elbow_left" to "좌측 어깨와 팔꿉 기울기"),
            2 to mapOf("front_elbow_align_angle_left_upper_elbow_elbow_wrist" to "좌측 상완-팔꿉-손목 기울기",
                "front_elbow_align_angle_left_shoulder_elbow_wrist" to "좌측 어깨-팔꿈치-손목 기울기"),
            3 to mapOf("side_left_vertical_angle_shoulder_elbow" to "어깨와 팔꿉 기울기",
                "side_left_vertical_angle_elbow_wrist" to "팔꿉와 손목 기울기",
                "side_left_vertical_angle_shoulder_elbow_wrist" to "어깨-팔꿉-손목 기울기")
        ),
        mapOf( // 8
            0 to mapOf("front_horizontal_angle_elbow" to "양 팔꿉 기울기",
                "front_horizontal_distance_sub_elbow" to "양 팔꿉 높이 차",
                "front_vertical_angle_shoulder_elbow_right" to "우측 어깨와 팔꿉 기울기"),
            2 to mapOf("front_elbow_align_angle_right_upper_elbow_elbow_wrist" to "좌측 상완-팔꿉-손목 기울기",
                "front_elbow_align_angle_right_shoulder_elbow_wrist" to "좌측 어깨-팔꿈치-손목 기울기"),
            4 to mapOf("side_right_vertical_angle_shoulder_elbow" to "어깨와 팔꿉 기울기",
                "side_right_vertical_angle_elbow_wrist" to "팔꿉와 손목 기울기",
                "side_right_vertical_angle_shoulder_elbow_wrist" to "어깨-팔꿉-손목 기울기")
        ),
        // 좌측 손목
        mapOf( // 5
            0 to mapOf("front_vertical_angle_elbow_wrist_left" to "좌측 팔꿉와 손목 기울기",
                "front_horizontal_angle_wrist" to "양 손목 기울기"),
            2 to mapOf("front_elbow_align_angle_mid_index_wrist_elbow_left" to "좌측 중지-손목-팔꿉 기울기",
                "front_elbow_align_distance_left_wrist_shoulder" to "좌측 손목과 어깨 기울기"),
            3 to mapOf("side_left_horizontal_distance_wrist" to "중심과 좌측 손목 거리")
        ),
        mapOf( // 5
            0 to mapOf("front_vertical_angle_elbow_wrist_right" to "우측 팔꿉와 손목 기울기",
                "front_horizontal_angle_wrist" to "양 손목 기울기"),
            2 to mapOf("front_elbow_align_angle_mid_index_wrist_elbow_right" to "우측 중지-손목-팔꿉 기울기",
                "front_elbow_align_distance_right_wrist_shoulder" to "우측 손목과 어깨 기울기"),
            4 to mapOf("side_right_horizontal_distance_wrist" to "중심과 우측 손목 거리")
        ),
        // 좌측 골반
        mapOf( // 7
            0 to mapOf("front_vertical_angle_hip_knee_left" to "좌측 골반과 무릎 기울기",
                "front_horizontal_angle_hip" to "양 골반 기울기"),
            3 to mapOf("side_left_vertical_angle_hip_knee" to "좌측 골반과 무릎 기울기",
                "side_left_horizontal_distance_hip" to "중심과 좌측 골반 거리"),
            5 to mapOf("back_horizontal_angle_hip" to "양 골반 기울기"),
            6 to mapOf("back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder" to "좌측 어깨-골반중심-우측 어깨 기울기",
                "back_sit_vertical_angle_shoulder_center_hip" to "어깨와 골반중심 기울기",)
        ),
        mapOf( // 7
            0 to mapOf("front_vertical_angle_hip_knee_right" to "우측 골반과 무릎 기울기",
                "front_horizontal_angle_hip" to "양 골반 기울기"),
            4 to mapOf("side_right_vertical_angle_hip_knee" to "우측 골반과 무릎 기울기",
                "side_right_horizontal_distance_hip" to "중심과 우측 골반 거리"),
            5 to mapOf("back_horizontal_angle_hip" to "양 골반 기울기"),
            6 to mapOf("back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder" to "좌측 어깨-골반중심-우측 어깨 기울기",
                "back_sit_vertical_angle_shoulder_center_hip" to "어깨와 골반중심 기울기",)
        ),
        // 좌측 무릎 + 스쿼트
        mapOf( // 5
            0 to mapOf("front_horizontal_angle_knee" to "양 무릎 기울기",
                "front_horizontal_distance_knee_left" to "중심에서 좌측 무릎 거리",
                "front_vertical_angle_hip_knee_ankle_left" to "좌측 골반-무릎-발목 기울기"),
            5 to mapOf("back_horizontal_angle_knee" to "양 무릎 기울기",
                "back_horizontal_distance_knee_left" to "중심에서 좌측 무릎 거리")
        ),
        mapOf( // 5
            0 to mapOf("front_horizontal_angle_knee" to "양 무릎 기울기",
                "front_horizontal_distance_knee_right" to "중심에서 우측 무릎 거리",
                "front_vertical_angle_hip_knee_ankle_right" to "우측 골반-무릎-발목 기울기"),
            5 to mapOf("back_horizontal_angle_knee" to "양 무릎 기울기",
                "back_horizontal_distance_knee_right" to "중심에서 우측 무릎 거리")
        ),
        mapOf( // 5
            0 to mapOf("front_vertical_angle_knee_ankle_left" to "좌측 무릎과 발목 기울기",
                "front_horizontal_angle_ankle" to "양 발목 기울기",
                "front_horizontal_distance_ankle_left" to "중심에서 좌측 발목 거리"),
            5 to mapOf("back_horizontal_distance_sub_ankle" to "양 발목 높이 차",
                "back_horizontal_distance_heel_left" to "중심에서 좌측 발목 거리")
        ),
        mapOf( // 5
            0 to mapOf("front_vertical_angle_knee_ankle_right" to "우측 무릎과 발목 기울기",
                "front_horizontal_angle_ankle" to "양 발목 기울기",
                "front_horizontal_distance_ankle_right" to "중심에서 우측 발목 거리"),
            5 to mapOf("back_horizontal_distance_sub_ankle" to "양 발목 높이 차",
                "back_horizontal_distance_heel_right" to "중심에서 우측 발목 거리")
        )
    ) // total 76개

    enum class status{
        DANGER, WARNING, NORMAL
    }
    val matchedIndexs = listOf(
        "목관절" , "좌측 어깨", "우측 어깨", "좌측 팔꿉", "우측 팔꿉", "좌측 손목" , "우측 손목" , "좌측 골반", "우측 골반" , "좌측 무릎" , "우측 무릎" , "좌측 발목", "우측 발목"
    )
    // 측정 완료 후 measure_info의 painpart만들기 - motherJa에는 dynamic포함된 값있어야함
    fun getPairParts(motherJa: JSONArray) : MutableList<Pair<String, status>> {
        val thresholdPercent = 1.15f

        val results = mutableListOf<Pair<String, status>>()
        matchedUris.forEach{ (part, seqList) ->
            val tempPart = mutableListOf<Pair<String, status>>()
            seqList.forEachIndexed { index, i ->
                val measureResult = motherJa.getJSONObject(i)
                val partIndex = matchedIndexs.indexOf(part)
                val mainSeq = mainPartSeqs[partIndex]
                val errorBound = errorBounds[partIndex]

                mainSeq[i]?.forEach{ (columnName, rawDataName) ->
                    val boundPair = errorBound[i]?.get(columnName)

                    if (boundPair != null) {
                        val boundary = abs(boundPair.first - boundPair.second)
                        val lowerBoundWithWeight = boundPair.first - (boundary * thresholdPercent)
                        val upperBoundWithWeight = boundPair.second + (boundary * thresholdPercent)
                        val data = measureResult.optDouble(columnName).toFloat()
                        when {
                            data < lowerBoundWithWeight || data > upperBoundWithWeight -> {
                                tempPart.add(Pair(rawDataName, status.DANGER))
                            }
                            data < boundPair.first || data > boundPair.second -> {
                                tempPart.add(Pair(rawDataName, status.WARNING))
                            }
                            else -> {
                                tempPart.add(Pair(rawDataName, status.NORMAL))
                            }
                        }
                    }
                }
            }
            val dangerCount = tempPart.count {it.second == status.DANGER}
            val warningCount = tempPart.count { it.second == status.WARNING }
            val totalCount = tempPart.count { it.second == status.NORMAL }
            Log.v("부위카운트", "$part: ($dangerCount, $warningCount, $totalCount)")
            if (warningCount + dangerCount > totalCount && dangerCount > warningCount) {
                results.add(Pair(part, status.DANGER))
            } else if (warningCount + dangerCount > dangerCount && dangerCount < warningCount) {
                results.add(Pair(part, status.WARNING))
            } else {
                results.add(Pair(part, status.NORMAL))
            }
        }
        return results
    }

    fun calculateOverall(parts: MutableList<Pair<String, status>>) : Int {
        val scores = mapOf(
            status.DANGER to 30,
            status.WARNING to 65,
            status.NORMAL to 100
        )
        val weightScore = 1.05
        val reverseWeightScore = 0.95
        var weightedScoreSum = 0.0
        var totalWeight = 0.0
        for (part in parts) {
            val (bodyPart, status) = part
            val weight = when {
                bodyPart.contains("어깨") -> weightScore
                bodyPart.contains("골반") -> weightScore
                bodyPart.contains("손목") -> reverseWeightScore
                bodyPart.contains("발목") -> reverseWeightScore
                else -> 1.0
            }
            weightedScoreSum += (scores[status] ?: 0) * weight
            totalWeight += weight
        }

        return if (totalWeight > 0) (weightedScoreSum / totalWeight).toInt() else 0
    }


    // mainPartAnalysis에서 unit 만들기
    fun getAnalysisUnits(part: String, currentKey: Int, measureResult: JSONArray): MutableList<AnalysisUnitVO> {
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
                            state = boundPair.first <= data && boundPair.second >= data
                        )
                    )
                }
            }
        }
        return result
    }


    fun getDangerParts(measureInfo: MeasureInfo) : MutableList<Pair<String, Float>> {
        val dangerParts = mutableListOf<Pair<String, Float>>()

        val neckRisk = measureInfo.risk_neck?.toFloat()
        if (neckRisk != null) {
            if (neckRisk > 0) {
                dangerParts.add(Pair("목관절", neckRisk))
            }
        }

        val shoulderLeftRisk = measureInfo.risk_shoulder_left?.toFloat()
        if (shoulderLeftRisk != null) {
            if (shoulderLeftRisk > 0) {
                dangerParts.add(Pair("우측 어깨", shoulderLeftRisk))
            }
        }

        val shoulderRightRisk = measureInfo.risk_shoulder_right?.toFloat()
        if (shoulderRightRisk != null) {
            if (shoulderRightRisk > 0) {
                dangerParts.add(Pair("좌측 어깨", shoulderRightRisk))
            }
        }

        val elbowLeftRisk = measureInfo.risk_elbow_left?.toFloat()
        if (elbowLeftRisk != null) {
            if (elbowLeftRisk > 0) {
                dangerParts.add(Pair("좌측 팔꿉", elbowLeftRisk))
            }
        }
        val elbowRightRisk =  measureInfo.risk_elbow_right?.toFloat()
        if (elbowRightRisk != null) {
            if (elbowRightRisk > 0) {
                dangerParts.add(Pair("우측 팔꿉", elbowRightRisk))
            }
        }

        val wristLeftRisk = measureInfo.risk_wrist_left?.toFloat()
        if (wristLeftRisk != null) {
            if (wristLeftRisk > 0) {
                dangerParts.add(Pair("좌측 손목", wristLeftRisk))
            }
        }
        val wristRightRisk = measureInfo.risk_wrist_right?.toFloat()
        if (wristRightRisk != null) {
            if (wristRightRisk > 0) {
                dangerParts.add(Pair("우측 손목", wristRightRisk))
            }
        }

        val hipLeftRisk = measureInfo.risk_hip_left?.toFloat()
        if (hipLeftRisk != null) {
            if (hipLeftRisk > 0) {
                dangerParts.add(Pair("좌측 골반", hipLeftRisk))
            }
        }
        val hipRightRisk = measureInfo.risk_hip_right?.toFloat()
        if (hipRightRisk != null) {
            if (hipRightRisk > 0) {
                dangerParts.add(Pair("우측 골반", hipRightRisk))
            }
        }

        val kneeLeftRisk = measureInfo.risk_knee_left?.toFloat()
        if (kneeLeftRisk != null) {
            if (kneeLeftRisk > 0) {
                dangerParts.add(Pair("좌측 무릎", kneeLeftRisk))
            }
        }
        val kneeRightRisk = measureInfo.risk_knee_right?.toFloat()
        if (kneeRightRisk != null) {
            if (kneeRightRisk > 0) {
                dangerParts.add(Pair("우측 무릎", kneeRightRisk))
            }
        }

        val ankleLeftRisk = measureInfo.risk_ankle_left?.toFloat()
        if (ankleLeftRisk != null) {
            if (ankleLeftRisk > 0) {
                dangerParts.add(Pair("좌측 발목", ankleLeftRisk))
            }
        }
        val ankleRightRisk = measureInfo.risk_ankle_right?.toFloat()
        if (ankleRightRisk != null) {
            if (ankleRightRisk > 0) {
                dangerParts.add(Pair("우측 발목", ankleRightRisk))
            }
        }
        return dangerParts
    }

    fun convertToJsonArrays(dangerParts: List<Pair<String, Float>>?): Pair<JSONArray, JSONArray> {
        val partIndices = mapOf(
            "목관절" to 1,
            "어깨" to 2,
            "팔꿉" to 3,
            "손목" to 4,
            "골반" to 8,
            "무릎" to 9,
            "발목" to 10
        )

        // 필터링된 결과를 저장할 맵
        val filteredParts = mutableMapOf<String, Float>()

        dangerParts?.forEach { (part, value) ->
            // 좌측/우측 구분 제거
            val basicPart = when {
                part.contains("목관절") -> "목관절"
                part.contains("어깨") -> "어깨"
                part.contains("팔꿉") -> "팔꿉"
                part.contains("손목") -> "손목"
                part.contains("골반") -> "골반"
                part.contains("무릎") -> "무릎"
                part.contains("발목") -> "발목"
                else -> ""
            }

            // 같은 부위의 값 중 큰 값을 유지
            if (basicPart.isNotEmpty()) {
                filteredParts[basicPart] = maxOf(filteredParts[basicPart] ?: Float.MIN_VALUE, value)
            }
        }

        val indices = mutableListOf<Int>()
        val values = mutableListOf<Float>()

        // 필터링된 부위를 JSON 배열로 변환
        filteredParts.forEach { (part, value) ->
            partIndices[part]?.let { index ->
                indices.add(index)
                values.add(value)
            }
        }
        return Pair(JSONArray(indices), JSONArray(values))
    }

    suspend fun setImage(fragment: Fragment, measureVO: MeasureVO?, seq: Int, ssiv: SubsamplingScaleImageView, case: String): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            val jsonData = measureVO?.measureResult?.optJSONObject(seq)
            val coordinates = extractImageCoordinates(jsonData)
            val imageUrls = measureVO?.fileUris?.get(seq)
            var isSet = false
            Log.v("시퀀스", "seq: ${seq}, view: (${ssiv.width}, ${ssiv.height}), imageSize: (${ssiv.sWidth}, ${ssiv.sHeight})")
            if (imageUrls != null) {
                val imageFile = File(imageUrls)
                val bitmap = BitmapFactory.decodeFile(imageUrls)
                fragment.lifecycleScope.launch(Dispatchers.Main) {
                    ssiv.setImage(ImageSource.uri(imageFile.toUri().toString()))
                    ssiv.setOnImageEventListener(object : SubsamplingScaleImageView.OnImageEventListener {
                        override fun onReady() {
                            if (!isSet) {
                                // ssiv 이미지뷰의 크기
//                                    val jsonObject = Gson().fromJson(jsonData.toString(), JsonObject::class.java)
//                                    val scaleFactorX = jsonObject.get("measure_overlay_scale_factor_x").asDouble
//                                    val scaleFactorY = jsonObject.get("measure_overlay_scale_factor_y").asDouble

                                val imageViewWidth = ssiv.width
                                val imageViewHeight = ssiv.height
                                // iv에 들어간 image의 크기 같음 screenWidth
                                val sWidth = ssiv.sWidth
                                val sHeight = ssiv.sHeight
                                // 스케일 비율 계산
                                val scaleFactorX = imageViewHeight / sHeight.toFloat()
                                val scaleFactorY =  imageViewHeight / sHeight.toFloat()
                                // 오프셋 계산 (뷰 크기 대비 이미지 크기의 여백)
                                val offsetX = (imageViewWidth - sWidth * scaleFactorX) / 2f
                                val offsetY = (imageViewHeight - sHeight * scaleFactorY) / 2f
                                val poseLandmarkResult = fromCoordinates(coordinates)
                                val combinedBitmap = ImageProcessingUtil.combineImageAndOverlay(
                                    bitmap,
                                    poseLandmarkResult,
                                    scaleFactorX,
                                    scaleFactorY,
                                    offsetX,
                                    offsetY,

                                    seq
                                )
                                isSet = true
                                if (case == "mainPart") {
                                    ssiv.setImage(ImageSource.bitmap(combinedBitmap))
                                } else if (case == "trend") {
                                    ssiv.setImage(ImageSource.bitmap(
                                        cropToPortraitRatio(combinedBitmap)
                                    ))
                                } else {
                                    when (seq) {
                                        0, 2, 3, 4 -> ssiv.setImage(
                                            ImageSource.bitmap(
                                                cropToPortraitRatio(combinedBitmap)
                                            ))
                                        else -> ssiv.setImage(ImageSource.bitmap(combinedBitmap))
                                    }
                                }

                                continuation.resume(true)
                            }
                        }
                        override fun onImageLoaded() {  }

                        override fun onPreviewLoadError(e: Exception?) { continuation.resume(false) }
                        override fun onImageLoadError(e: Exception?) { continuation.resume(false) }
                        override fun onTileLoadError(e: Exception?) { continuation.resume(false) }
                        override fun onPreviewReleased() { continuation.resume(false) }
                    })

                }
            } else { continuation.resume(false) }
        } catch (e: IndexOutOfBoundsException) {
            Log.e("Error", "$e")
        } catch (e: FileNotFoundException) {
            Log.e("scalingFileNotFound", "$e")
        } catch (e: IllegalStateException) {
            Log.e("scalingStateError", "$e" )
        }
    }
    fun getVideoDimensions(context : Context, videoUri: Uri) : Pair<Int, Int> {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        val videoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
        val videoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
        retriever.release()
        return Pair(videoWidth, videoHeight)
    }

    // -------# 측정 결과 MeasureVO로 변환 #------
    private fun extractImageCoordinates(jsonData: JSONObject?): List<Pair<Float, Float>>? {
        val poseData = jsonData?.optJSONArray("pose_landmark")
        return if (poseData != null) {
            List(poseData.length()) { i ->
                val landmark = poseData.getJSONObject(i)
                Pair(
                    landmark.getDouble("sx").toFloat(),
                    landmark.getDouble("sy").toFloat()
                )
            }
        } else null
    }

    fun extractVideoCoordinates(jsonData: JSONArray) : List<List<Pair<Float,Float>>> { // 200개의 33개의 x,y
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
}