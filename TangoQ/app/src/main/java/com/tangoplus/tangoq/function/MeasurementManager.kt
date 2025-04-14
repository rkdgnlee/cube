package com.tangoplus.tangoq.function

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.tangoplus.tangoq.vo.AnalysisUnitVO
import com.tangoplus.tangoq.vo.MeasureVO
import com.tangoplus.tangoq.db.MeasureInfo
import com.tangoplus.tangoq.mediapipe.ImageProcessingUtil
import com.tangoplus.tangoq.mediapipe.ImageProcessingUtil.cropToPortraitRatio
import com.tangoplus.tangoq.mediapipe.PoseLandmarkResult.Companion.fromCoordinates
import com.tangoplus.tangoq.db.Singleton_t_user
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import kotlin.coroutines.resume
import kotlin.math.abs
import androidx.core.graphics.scale
import com.tangoplus.tangoq.mediapipe.ImageProcessingUtil.drawDirectionUIOnBitmap
import com.tangoplus.tangoq.mediapipe.ImageProcessingUtil.rePaintDirection
import com.tangoplus.tangoq.mediapipe.MathHelpers.determineDirection
import com.tangoplus.tangoq.mediapipe.PoseLandmarkResult

object MeasurementManager {
    val partIndexes = mapOf(
        0 to "목관절",
        1 to "좌측 어깨",
        2 to "우측 어깨",
        3 to "좌측 팔꿉",
        4 to "우측 팔꿉",
        5 to "좌측 손목",
        6 to "우측 손목",
        7 to "좌측 골반",
        8 to "우측 골반",
        9 to "좌측 무릎",
        10 to "우측 무릎",
        11 to "좌측 발목",
        12 to "우측 발목",
    )

    val seqs = listOf("정면 측정", "동적 측정", "팔꿉 측정", "좌측 측정", "우측 측정", "후면 측정", "앉은 후면")
    val matchedUris = mapOf(
        "목관절" to listOf(0, 3, 4, 5, 6),
        "좌측 어깨" to listOf(0, 1, 3, 5, 6),
        "우측 어깨" to listOf(0, 1, 4, 5, 6),
        "좌측 팔꿉" to listOf(0, 2, 3),
        "우측 팔꿉" to listOf(0, 2, 4),
        "좌측 손목" to listOf(0, 2, 3),
        "우측 손목" to listOf(0, 2, 4),
        "좌측 골반" to listOf(0, 1, 3, 5, 6),
        "우측 골반" to listOf(0, 1, 4, 5, 6),
        "좌측 무릎" to listOf(0, 1, 3, 5),
        "우측 무릎" to listOf(0, 1, 4, 5),
        "좌측 발목" to listOf(0, 5),
        "우측 발목" to listOf(0, 5)
    )

    val matchedIndexs = listOf(
        "목관절" , "좌측 어깨", "우측 어깨", "좌측 팔꿉", "우측 팔꿉", "좌측 손목" , "우측 손목" , "좌측 골반", "우측 골반" , "좌측 무릎" , "우측 무릎" , "좌측 발목", "우측 발목"
    )

    // first: seq / second: matchedUris 내부의 index / third: 가장 작은 index
    fun transSeqToColumnName(seq: Int) : String {
        return when (seq) {
            0 -> "front_"
            1 -> "front_elbow"
            2 -> "side_left"
            3 -> "side_right"
            4 -> "back"
            5 -> "back_sit"
            else -> ""
        }
    }

    private val femaleErrorBounds = listOf(
        mapOf(
            0 to mapOf( "front_horizontal_angle_ear" to Triple(180f, 1.9f, 3.8f),
                "front_horizontal_distance_sub_ear" to Triple(0f, 1.9f, 3.8f)), //*&*
            3 to mapOf( "side_left_vertical_angle_ear_shoulder" to Triple(87f,9.74f, 13.21f)), //*&*
            4 to mapOf( "side_right_vertical_angle_ear_shoulder" to Triple(87f,9.74f, 13.21f)), //*&*
            5 to mapOf( "back_horizontal_angle_ear" to Triple(0f,1.6f, 3.8f),
            "back_vertical_angle_nose_center_shoulder" to Triple(90f,3f, 9f)),
            6 to mapOf( "back_sit_horizontal_angle_ear" to Triple(0f,1.09f, 3.29f), //*&*
                "back_sit_vertical_angle_right_shoulder_nose_left_shoulder" to Triple(84.23f,3.07f, 9.19f)) //*&*
        ),
        // 어깨
        mapOf(
            0 to mapOf("front_horizontal_angle_shoulder" to Triple(-180f, 3.64f, 5.93f),
                "front_horizontal_distance_sub_shoulder" to Triple(0f, 2.6f, 3.6f)), //*&*
            3 to mapOf("side_left_horizontal_distance_shoulder" to Triple(1.9f, 6.1f, 8.9f)), //*&*
            5 to mapOf("back_vertical_angle_shoudler_center_hip" to Triple(90f, 3f, 5f),
                "back_horizontal_angle_shoulder" to Triple(-0.1f, 0.9f,1.8f)),
            6 to mapOf("back_sit_vertical_angle_shoulder_center_hip" to Triple(90f, 6f, 10f),
                "back_sit_vertical_angle_right_shoulder_left_shoulder_center_hip" to Triple(70f, 5f, 10f)
            )
        ),
        mapOf( // 179 -> 양수 -> 오른쪽이 안좋은 거 // -0.76
            0 to mapOf("front_horizontal_angle_shoulder" to Triple(180f, 3.64f, 5.93f),
                "front_horizontal_distance_sub_shoulder" to Triple(0f, 2.6f, 3.6f)), //*&*
            4 to mapOf("side_right_horizontal_distance_shoulder" to Triple(1.9f, 6.1f, 8.9f)), //*&*
            5 to mapOf("back_vertical_angle_shoudler_center_hip" to Triple(90f, 3f, 5f),
                "back_horizontal_angle_shoulder" to Triple(0.1f, 0.9f,1.8f)),
            6 to mapOf("back_sit_vertical_angle_shoulder_center_hip" to Triple(90f, 6f, 10f),
                "back_sit_vertical_angle_center_hip_right_shoulder_left_shoulder" to Triple(70f, 5f, 10f)
            )
        ),
        // 좌측 팔꿉
        mapOf(
            0 to mapOf("front_horizontal_angle_elbow" to Triple(-180f, 2.4f, 3.6f),
                "front_horizontal_distance_sub_elbow" to Triple(0f, 3.1f, 4.2f),
                "front_vertical_angle_shoulder_elbow_left" to Triple(79f, 5.2f, 9.8f)),
            2 to mapOf("front_elbow_align_angle_left_shoulder_elbow_wrist" to Triple(5f,9f, 13f)),
            3 to mapOf("side_left_vertical_angle_shoulder_elbow" to Triple(90f,4.7f, 10.1f),
                "side_left_vertical_angle_elbow_wrist" to Triple(95f,5.67f, 11.27f),
                "side_left_vertical_angle_shoulder_elbow_wrist" to Triple(170f, 8f, 12f))
        ),
        mapOf(
            0 to mapOf("front_horizontal_angle_elbow" to Triple(180f, 2.4f, 3.6f),
                "front_horizontal_distance_sub_elbow" to Triple(0f, 3.1f, 4.2f),
                "front_vertical_angle_shoulder_elbow_right" to Triple(103f, 5.2f, 9.8f)),
            2 to mapOf("front_elbow_align_angle_right_shoulder_elbow_wrist" to Triple(5f,9f, 13f)),
            4 to mapOf("side_right_vertical_angle_shoulder_elbow" to Triple(90f,4.7f, 10.1f),
                "side_right_vertical_angle_elbow_wrist" to Triple(85f,5.67f, 11.27f),
                "side_right_vertical_angle_shoulder_elbow_wrist" to Triple(170f, 8f, 12f))
        ),
        // 좌측 손목
        mapOf(
            0 to mapOf("front_vertical_angle_elbow_wrist_left" to Triple(85f, 6f, 8f),
                "front_horizontal_angle_wrist" to Triple(-180f, 2.8f, 4.2f),
                "front_horizontal_distance_wrist_left" to Triple(22f, 4f, 6f)),
            2 to mapOf("front_elbow_align_distance_left_wrist_shoulder" to Triple(4f, 3.7f, 4.9f),
                "front_elbow_align_distance_center_wrist_left" to Triple(22f, 7f, 10f)),
            3 to mapOf("side_left_horizontal_distance_wrist" to Triple(12f, 6.5f, 8.4f))
        ),
        mapOf(
            0 to mapOf("front_vertical_angle_elbow_wrist_right" to Triple(85f, 6f, 8f),
                "front_horizontal_angle_wrist" to Triple(180f, 2.8f, 4.2f),
                "front_horizontal_distance_wrist_right" to Triple(22f, 4f, 6f)),
            2 to mapOf("front_elbow_align_distance_right_wrist_shoulder" to Triple(4f, 3.7f, 4.9f),
                "front_elbow_align_distance_center_wrist_right" to Triple(22f, 7f, 10f)),
            4 to mapOf("side_right_horizontal_distance_wrist" to Triple(12f, 4.5f, 6.4f))
        ),
        // 좌측 골반
        mapOf(
            0 to mapOf("front_horizontal_angle_hip" to Triple(-180f, 1.8f, 2.9f),
                "front_horizontal_distance_sub_hip" to Triple(0f,1.9f, 2.8f),
                "front_vertical_angle_hip_knee_left" to Triple(90f,1.9f, 5.8f)),
            3 to mapOf("side_left_horizontal_distance_hip" to Triple(2.5f, 3.55f, 5.25f)),
            5 to mapOf("back_horizontal_angle_hip" to Triple(-0.1f, 2.6f, 4.1f)),
            6 to mapOf("back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder" to Triple(35f, 9f, 15f)
            )
        ),
        mapOf(
            0 to mapOf("front_horizontal_angle_hip" to Triple(180f, 1.8f, 2.9f),
                "front_horizontal_distance_sub_hip" to Triple(0f,1.9f, 2.8f),
                "front_vertical_angle_hip_knee_right" to Triple(90f,1.9f, 5.8f),
                ),
            4 to mapOf("side_right_horizontal_distance_hip" to Triple(2.5f, 3.55f, 5.25f)),
            5 to mapOf("back_horizontal_angle_hip" to Triple(0.1f, 2.6f, 4.1f)),
            6 to mapOf("back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder" to Triple(35f, 9f, 15f)
            )
        ),
        // 좌측 무릎
        mapOf(
            0 to mapOf("front_horizontal_angle_knee" to Triple(-180f, 2.1f, 4.25f),
                "front_horizontal_distance_knee_left" to Triple(13f, 2.3f, 3.9f),
                "front_vertical_angle_hip_knee_ankle_left" to Triple(175f,2.5f, 5f)),
            3 to mapOf("side_left_vertical_angle_hip_knee" to Triple(91f, 6.4f, 9.4f),
                "side_left_vertical_angle_hip_knee_ankle" to Triple(175f, 8.9f, 13.9f)),
            5 to mapOf("back_horizontal_angle_knee" to Triple(-0.1f, 1.85f, 3.05f),
                "back_horizontal_distance_knee_left" to Triple(12f, 5.6f, 8.9f))
        ),
        mapOf(
            0 to mapOf("front_horizontal_angle_knee" to Triple(180f, 2.1f, 4.25f),
                "front_horizontal_distance_knee_right" to Triple(13f, 2.3f, 3.9f),
                "front_vertical_angle_hip_knee_ankle_right" to Triple(175f,2.5f, 5f)),
            4 to mapOf("side_right_vertical_angle_hip_knee" to Triple(89f, 6.4f, 9.4f),
                "side_right_vertical_angle_hip_knee_ankle" to Triple(175f, 8.9f, 13.9f)),
            5 to mapOf("back_horizontal_angle_knee" to Triple(0.1f, 1.85f, 3.05f),
                "back_horizontal_distance_knee_right" to Triple(12f, 5.6f, 8.9f))
        ),
        // 좌측 발목
        mapOf(
            0 to mapOf("front_vertical_angle_knee_ankle_left" to Triple(88f,4.25f, 7.8f),
                "front_horizontal_angle_ankle" to Triple(-180f, 2.4f, 5.1f),
                "front_horizontal_distance_ankle_left" to Triple(10f,5.2f, 8.2f)),
            5 to mapOf("back_horizontal_angle_ankle" to Triple(-0.1f, 2.3f, 6.63f),
                "back_horizontal_distance_sub_ankle" to  Triple(0f, 0.7f, 1.9f),
                "back_horizontal_distance_heel_left" to Triple(11f, 7f, 12f))
        ),
        mapOf(
            0 to mapOf("front_vertical_angle_knee_ankle_right" to Triple(88f,3.85f, 6.8f),
                "front_horizontal_angle_ankle" to Triple(180f, 2.4f, 5.1f),
                "front_horizontal_distance_ankle_right" to Triple(10f,5.2f, 8.2f)),
            5 to mapOf("back_horizontal_angle_ankle" to Triple(0.1f, 2.3f, 6.63f),
                "back_horizontal_distance_sub_ankle" to Triple(0f, -0.7f, -1.9f),
                "back_horizontal_distance_heel_right" to Triple(11f, 7f, 12f))
        )
    )
    // ------# 남자 점수 bound #------
    // 값이 올라갔을 경우
    // 정면 정상 부위: +179 오른쪽이 정상 // -179 -> 왼쪽이 정상
    // 후면 안좋은 부위: -179 오른쪽이 정상  // +179 -> 왼쪽이 정상
    // 0108 수정불필요: //*&* 수정 필요: //*^*
    // 좌측 무릎 정상은 정면 좌측이 음수 우측이 양수 // 후면은 좌측이 양수 우측이 음수
    private val maleErrorBounds = listOf(
        mapOf(
            0 to mapOf( "front_horizontal_angle_ear" to Triple(180f, 1.6f, 2.9f),
                "front_horizontal_distance_sub_ear" to Triple(0f, 1.6f, 2.9f)), //*&*
            3 to mapOf( "side_left_vertical_angle_ear_shoulder" to Triple(88f,7.74f, 12.21f)), //*&*
            4 to mapOf( "side_right_vertical_angle_ear_shoulder" to Triple(88f,7.74f, 12.21f)), //*&*
            5 to mapOf( "back_horizontal_angle_ear" to Triple(0f,1.2f, 3.5f),
            "back_vertical_angle_nose_center_shoulder" to Triple(90f,3f, 8f)), //*&*
            6 to mapOf( "back_sit_horizontal_angle_ear" to Triple(0f,1.09f, 3.29f), //*&*
                "back_sit_vertical_angle_right_shoulder_nose_left_shoulder" to Triple(74.23f,13.07f, 29.19f)) //*&*
        ),
        // 어깨
        mapOf(
            0 to mapOf("front_horizontal_angle_shoulder" to Triple(-180f, 2.1f, 3.9f),
                "front_horizontal_distance_sub_shoulder" to Triple(0f, 1.9f, 3.2f)), //*&*
            3 to mapOf("side_left_horizontal_distance_shoulder" to Triple(1.3f, 5.3f, 7.1f)), //*&*
            5 to mapOf("back_vertical_angle_shoudler_center_hip" to Triple(90f, 2.5f, 4f),
                "back_horizontal_angle_shoulder" to Triple(-0.1f, 0.6f,1.8f)),
            6 to mapOf("back_sit_vertical_angle_shoulder_center_hip" to Triple(90f, 6f, 9.2f),
                "back_sit_vertical_angle_right_shoulder_left_shoulder_center_hip" to Triple(70f, 4.2f, 8f)
            )
        ),
        mapOf( // 179 -> 양수 -> 오른쪽이 안좋은 거 // -0.76
            0 to mapOf("front_horizontal_angle_shoulder" to Triple(180f, 2.1f, 3.9f),
                "front_horizontal_distance_sub_shoulder" to Triple(0f, 1.9f, 3.2f)), //*&*
            4 to mapOf("side_right_horizontal_distance_shoulder" to Triple(1.3f, 5.3f, 7.1f)), //*&*
            5 to mapOf("back_vertical_angle_shoudler_center_hip" to Triple(90f, 2.5f, 4f),
                "back_horizontal_angle_shoulder" to Triple(0.1f, 0.6f,1.8f)),
            6 to mapOf("back_sit_vertical_angle_shoulder_center_hip" to Triple(90f, 6f, 9.2f),
                "back_sit_vertical_angle_center_hip_right_shoulder_left_shoulder" to Triple(70f, 4.2f, 8f)
            )
        ),
        // 좌측 팔꿉
        mapOf(
            0 to mapOf("front_horizontal_angle_elbow" to Triple(-180f, 1.9f, 3.5f),
                "front_horizontal_distance_sub_elbow" to Triple(0f, 1.31f, 2.72f),
                "front_vertical_angle_shoulder_elbow_left" to Triple(79f, 5.2f, 9.8f)),
            2 to mapOf("front_elbow_align_angle_left_shoulder_elbow_wrist" to Triple(4f,8f, 12f)),
            3 to mapOf("side_left_vertical_angle_shoulder_elbow" to Triple(90f, 3.3f, 8.6f),
                "side_left_vertical_angle_elbow_wrist" to Triple(85f,5.67f, 11.27f),
                "side_left_vertical_angle_shoulder_elbow_wrist" to Triple(170f, 5f, 9f))
        ),
        mapOf(
            0 to mapOf("front_horizontal_angle_elbow" to Triple(180f, 1.9f, 3.5f),
                "front_horizontal_distance_sub_elbow" to Triple(0f, 1.31f, 2.72f),
                "front_vertical_angle_shoulder_elbow_right" to Triple(79f, 5.2f, 9.8f)),
            2 to mapOf("front_elbow_align_angle_right_shoulder_elbow_wrist" to Triple(4f,8f, 12f)),
            4 to mapOf("side_right_vertical_angle_shoulder_elbow" to Triple(90f,3.3f, 8.6f),
                "side_right_vertical_angle_elbow_wrist" to Triple(85f,5.67f, 11.27f),
                "side_right_vertical_angle_shoulder_elbow_wrist" to Triple(170f, 5f, 9f))
        ),
        // 좌측 손목
        mapOf(
            0 to mapOf("front_vertical_angle_elbow_wrist_left" to Triple(85f, 6f, 8f),
                "front_horizontal_angle_wrist" to Triple(-180f, 2.8f, 4.2f),
                "front_horizontal_distance_wrist_left" to Triple(22f, 8f, 12f)),
            2 to mapOf("front_elbow_align_distance_left_wrist_shoulder" to Triple(3f, 3.1f, 4.9f),
                "front_elbow_align_distance_center_wrist_left" to Triple(22f, 8f, 12f)),
            3 to mapOf("side_left_horizontal_distance_wrist" to Triple(13f, 5.5f, 6.7f))
        ),
        mapOf(
            0 to mapOf("front_vertical_angle_elbow_wrist_right" to Triple(85f, 6f, 8f),
                "front_horizontal_angle_wrist" to Triple(180f, 2.8f, 4.2f),
                "front_horizontal_distance_wrist_right" to Triple(22f, 8f, 12f)),
            2 to mapOf("front_elbow_align_distance_right_wrist_shoulder" to Triple(3f, 3.1f, 4.9f),
                "front_elbow_align_distance_center_wrist_right" to Triple(22f, 8f, 12f)),
            4 to mapOf("side_right_horizontal_distance_wrist" to Triple(13f, 5.5f, 6.7f))
        ),
        // 좌측 골반
        mapOf(
            0 to mapOf("front_horizontal_angle_hip" to Triple(-180f, 1.7f, 2.89f),
                "front_horizontal_distance_sub_hip" to Triple(0f,1.3f, 2.3f),
                "front_vertical_angle_hip_knee_left" to Triple(90f,2.1f, 3.8f),),
            3 to mapOf("side_left_horizontal_distance_hip" to Triple(2.5f, 3.55f, 5.25f)),
            5 to mapOf("back_horizontal_angle_hip" to Triple(-0.1f, 1.9f, 3.3f)),
            6 to mapOf("back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder" to Triple(35f, 8f, 14f)
            )
        ),
        mapOf(
            0 to mapOf( "front_horizontal_angle_hip" to Triple(180f, 1.7f, 2.89f),
                "front_horizontal_distance_sub_hip" to Triple(0f,1.3f, 2.3f),
                "front_vertical_angle_hip_knee_right" to Triple(90f,2.1f, 3.8f),
            ),
            4 to mapOf("side_right_horizontal_distance_hip" to Triple(2.5f, 3.55f, 5.75f)),
            5 to mapOf("back_horizontal_angle_hip" to Triple(0.1f, 1.9f, 3.3f)),
            6 to mapOf("back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder" to Triple(35f, 8f, 14f)
            )
        ),

        // 좌측 무릎, 정상은 정면 좌측이 음수 우측이 양수 // 후면은 좌측이 양수 우측이 음수
        mapOf(
            0 to mapOf("front_horizontal_angle_knee" to Triple(-180f, 1.85f, 3.45f),
                "front_horizontal_distance_knee_left" to Triple(13f, 2.15f, 3.75f),
                "front_vertical_angle_hip_knee_ankle_left" to Triple(175f,2.5f, 5f)),
            3 to mapOf("side_left_vertical_angle_hip_knee" to Triple(91f, 6.4f, 9.4f),
                "side_left_vertical_angle_hip_knee_ankle" to Triple(175f, 6.9f, 10.9f)),
            5 to mapOf("back_horizontal_angle_knee" to Triple(-0.1f, 1.85f, 2.45f),
                "back_horizontal_distance_knee_left" to Triple(12f, 5.6f, 8.9f))
        ),
        mapOf(
            0 to mapOf("front_horizontal_angle_knee" to Triple(180f, 1.85f, 3.45f),
                "front_horizontal_distance_knee_right" to Triple(13f, 2.15f, 3.75f),
                "front_vertical_angle_hip_knee_ankle_right" to Triple(175f,2.5f, 5f)),
            4 to mapOf("side_right_vertical_angle_hip_knee" to Triple(89f, 6.4f, 9.4f),
                "side_right_vertical_angle_hip_knee_ankle" to Triple(175f, 6.9f, 10.9f)),
            5 to mapOf("back_horizontal_angle_knee" to Triple(0.1f, 1.85f, 2.45f),
                "back_horizontal_distance_knee_right" to Triple(12f, 5.6f, 8.9f))
        ),
        // 좌측 발목
        mapOf(
            0 to mapOf("front_vertical_angle_knee_ankle_left" to Triple(88f,3.85f, 5.8f),
                "front_horizontal_angle_ankle" to Triple(-180f, 1.5f, 2.2f),
                "front_horizontal_distance_ankle_left" to Triple(10f,5.2f, 8.2f)),
            5 to mapOf("back_horizontal_angle_ankle" to Triple(-0.1f, 2.3f, 4.63f),
                "back_horizontal_distance_sub_ankle" to  Triple(0f, 0.3f, 1.1f),
                "back_horizontal_distance_heel_left" to Triple(11f, 6f, 11f))
        ),
        mapOf(
            0 to mapOf("front_vertical_angle_knee_ankle_right" to Triple(88f,3.85f, 5.8f),
                "front_horizontal_angle_ankle" to Triple(180f, 1.5f, 2.2f),
                "front_horizontal_distance_ankle_right" to Triple(10f,5.2f, 8.2f)),
            5 to mapOf("back_horizontal_angle_ankle" to Triple(0.1f, 2.5f, 4.63f),
                "back_horizontal_distance_sub_ankle" to Triple(0f, 0.3f, 1.1f),
                "back_horizontal_distance_heel_right" to Triple(11f, 6f, 11f))
        )
    )

    private val mainPartSeqs = listOf(
        mapOf( // 6
            0 to mapOf( "front_horizontal_angle_ear" to "정면 - 양 귀 기울기",
                "front_horizontal_distance_sub_ear" to "정면 - 양 귀 기울기 높이 차"),
            3 to mapOf("side_left_vertical_angle_ear_shoulder" to "왼쪽 귀와 좌측 어깨 기울기"),
            4 to mapOf("side_right_vertical_angle_ear_shoulder" to "오른쪽 귀와 우측 어깨 기울기"),
            5 to mapOf("back_horizontal_angle_ear" to "후면 - 양 귀 기울기",
                "back_vertical_angle_nose_center_shoulder" to "후면 - 어깨중심과 코 기울기"),
            6 to mapOf( "back_sit_horizontal_angle_ear" to "앉은 후면 - 양 귀 기울기",
                "back_sit_vertical_angle_right_shoulder_nose_left_shoulder" to "앉은 후면 - 양 어깨와 코를 이은 삼각형에서 코 기울기")
        ),
        mapOf( // 5
            0 to mapOf("front_horizontal_angle_shoulder" to "정면 - 양 어깨 기울기",
                "front_horizontal_distance_sub_shoulder" to "정면 - 양 어깨 높이 차"),
            3 to mapOf("side_left_horizontal_distance_shoulder" to "왼쪽 중심과 어깨 거리"),
            5 to mapOf("back_vertical_angle_shoudler_center_hip" to "후면 - 골반중심과 어깨 기울기",
                "back_horizontal_angle_shoulder" to "후면 - 양 어깨 기울기"),
            6 to mapOf("back_sit_vertical_angle_shoulder_center_hip" to "앉은 후면 - 양 어깨에서 골반중심과의 기울기",
                "back_sit_vertical_angle_right_shoulder_left_shoulder_center_hip" to "앉은 후면 - 양 어깨-골반 삼각형에서 좌측 어깨 기울기")
        ),
        mapOf( // 5
            0 to mapOf("front_horizontal_angle_shoulder" to "정면 - 양 어깨 기울기",
                "front_horizontal_distance_sub_shoulder" to "정면 - 양 어깨 높이 차"),
            4 to mapOf("side_right_horizontal_distance_shoulder" to "오른쪽 중심과 어깨 거리"),
            5 to mapOf("back_vertical_angle_shoudler_center_hip" to "후면 - 골반중심과 어깨 기울기",
                "back_horizontal_angle_shoulder" to "후면 - 양 어깨 기울기"),
            6 to mapOf("back_sit_vertical_angle_shoulder_center_hip" to "앉은 후면 - 양 어깨에서 골반중심과의 기울기",
                "back_sit_vertical_angle_center_hip_right_shoulder_left_shoulder" to  "앉은 후면 - 양 어깨-골반 삼각형에서 우측 어깨 기울기")
        ),
        // 좌측 팔꿉  // 8
        mapOf(
            0 to mapOf("front_horizontal_angle_elbow" to "정면 - 양 팔꿉 기울기",
                "front_horizontal_distance_sub_elbow" to "정면 - 양 팔꿉 높이 차",
                "front_vertical_angle_shoulder_elbow_left" to "왼쪽 어깨와 팔꿉 기울기"),
            2 to mapOf("front_elbow_align_angle_left_shoulder_elbow_wrist" to "왼쪽 어깨-팔꿈치-손목 기울기"),
            3 to mapOf("side_left_vertical_angle_shoulder_elbow" to "어깨와 팔꿉 기울기",
                "side_left_vertical_angle_elbow_wrist" to "팔꿉와 손목 기울기",
                "side_left_vertical_angle_shoulder_elbow_wrist" to "어깨-팔꿉-손목 기울기")
        ),
        mapOf( // 8
            0 to mapOf("front_horizontal_angle_elbow" to "정면 - 양 팔꿉 기울기",
                "front_horizontal_distance_sub_elbow" to "정면 - 양 팔꿉 높이 차",
                "front_vertical_angle_shoulder_elbow_right" to "오른쪽 어깨와 팔꿉 기울기"),
            2 to mapOf("front_elbow_align_angle_right_shoulder_elbow_wrist" to "오른쪽 어깨-팔꿈치-손목 기울기"),
            4 to mapOf("side_right_vertical_angle_shoulder_elbow" to "오른쪽 어깨와 팔꿉 기울기",
                "side_right_vertical_angle_elbow_wrist" to "오른쪽 팔꿉와 손목 기울기",
                "side_right_vertical_angle_shoulder_elbow_wrist" to "오른쪽 어깨-팔꿉-손목 기울기")
        ),
        // 좌측 손목
        mapOf( // 5
            0 to mapOf("front_vertical_angle_elbow_wrist_left" to "정면 - 왼쪽 팔꿉과 손목 기울기",
                "front_horizontal_angle_wrist" to "정면 - 양 손목 기울기",
                "front_horizontal_distance_wrist_left" to "정면 - 몸의 중심에서 왼쪽 손목 거리"),
            2 to mapOf("front_elbow_align_distance_left_wrist_shoulder" to "팔꿉 - 왼쪽 손목-어깨 거리",
                "front_elbow_align_distance_center_wrist_left" to "팔꿉 - 중심과 왼쪽 손목 거리"),
            3 to mapOf("side_left_horizontal_distance_wrist" to "중심에서 왼쪽 손목 거리")
        ),
        mapOf( // 5
            0 to mapOf("front_vertical_angle_elbow_wrist_right" to "정면 - 오른쪽 팔꿉과 손목 기울기",
                "front_horizontal_angle_wrist" to "정면 - 양 손목 기울기",
                "front_horizontal_distance_wrist_right" to "정면 - 중심에서 오른쪽 손목 거리"),
            2 to mapOf("front_elbow_align_distance_right_wrist_shoulder" to "팔꿉 - 오른쪽 손목-어깨 거리",
                "front_elbow_align_distance_center_wrist_right" to "팔꿉 - 중심에서 오른쪽 손목 거리"),
            4 to mapOf("side_right_horizontal_distance_wrist" to "중심에서 오른쪽 손목 거리")
        ),
        // 좌측 골반
        mapOf( // 7
            0 to mapOf("front_vertical_angle_hip_knee_left" to "정면 - 왼쪽 골반과 무릎 기울기",
                "front_horizontal_angle_hip" to "정면 - 양 골반 기울기",
                "front_horizontal_distance_sub_hip" to "정면 - 양 골반 높이 차"),
            3 to mapOf("side_left_vertical_angle_hip_knee" to "왼쪽 골반과 무릎 기울기",
                "side_left_horizontal_distance_hip" to "중심과 왼쪽 골반 거리"),
            5 to mapOf("back_horizontal_angle_hip" to "후면 - 양 골반 기울기"),
            6 to mapOf(
                "back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder" to "양 어깨와 골반을 이은 삼각형에서 골반 기울기",
//                "back_sit_vertical_angle_shoulder_center_hip" to "어깨와 골반중심 기울기",
            )
        ),
        mapOf( // 7
            0 to mapOf("front_vertical_angle_hip_knee_right" to "정면 - 오른쪽 골반과 무릎 기울기",
                "front_horizontal_angle_hip" to "정면 - 양 골반 기울기",
                "front_horizontal_distance_sub_hip" to "정면 - 양 골반 높이 차"),
            4 to mapOf("side_right_vertical_angle_hip_knee" to "오른쪽 골반과 무릎 기울기",
                "side_right_horizontal_distance_hip" to "중심과 오른쪽 골반 거리"),
            5 to mapOf("back_horizontal_angle_hip" to "후면 - 양 골반 기울기"),
            6 to mapOf(
                "back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder" to "양 어깨와 골반을 이은 삼각형에서 골반 기울기",
//                "back_sit_vertical_angle_shoulder_center_hip" to "어깨와 골반중심 기울기",
            )
        ),
        // 좌측 무릎 + 스쿼트
        mapOf( // 5
            0 to mapOf("front_horizontal_angle_knee" to "정면 - 양 무릎 기울기",
                "front_horizontal_distance_knee_left" to "정면 - 중심에서 왼쪽 무릎 거리",
                "front_vertical_angle_hip_knee_ankle_left" to "정면 - 왼쪽 골반-무릎-발목 기울기"),
            3 to mapOf("side_left_vertical_angle_hip_knee" to "측면 왼쪽 골반-무릎 기울기",
                "side_left_vertical_angle_hip_knee_ankle" to "측면 왼쪽 하지(골반-무릎-발목) 기울기"),
            5 to mapOf("back_horizontal_angle_knee" to "후면 - 양 무릎 기울기",
                "back_horizontal_distance_knee_left" to "후면 - 중심에서 좌측 무릎 거리")
        ),
        mapOf( // 5
            0 to mapOf("front_horizontal_angle_knee" to "정면 - 양 무릎 기울기",
                "front_horizontal_distance_knee_right" to "정면 - 중심에서 오른쪽 무릎 거리",
                "front_vertical_angle_hip_knee_ankle_right" to "정면 - 오른쪽 골반-무릎-발목 기울기"),
            4 to mapOf("side_right_vertical_angle_hip_knee" to "오른쪽 측면 - 골반-무릎 기울기",
                "side_right_vertical_angle_hip_knee_ankle" to "오른쪽 측면 - 하지(골반-무릎-발목) 기울기"),
            5 to mapOf("back_horizontal_angle_knee" to "후면 - 양 무릎 기울기",
                "back_horizontal_distance_knee_right" to "후면 - 중심에서 오른쪽 무릎 거리")
        ),
        mapOf( // 5
            0 to mapOf("front_vertical_angle_knee_ankle_left" to "정면 - 왼쪽 무릎과 발목 기울기",
                "front_horizontal_angle_ankle" to "정면 - 양 발목 기울기",
                "front_horizontal_distance_ankle_left" to "정면 - 중심에서 왼쪽 발목 거리"),
            5 to mapOf("back_horizontal_angle_ankle" to "후면 - 양 발목 기울기",
                "back_horizontal_distance_sub_ankle" to "후면 - 양 발목 높이 차",
                "back_horizontal_distance_heel_left" to "후면 - 중심에서 왼쪽 발목 거리")
        ),
        mapOf( // 5
            0 to mapOf("front_vertical_angle_knee_ankle_right" to "정면 - 오른쪽 무릎과 발목 기울기",
                "front_horizontal_angle_ankle" to "정면 - 양 발목 기울기",
                "front_horizontal_distance_ankle_right" to "정면 - 중심에서 오른쪽 발목 거리"),
            5 to mapOf("back_horizontal_angle_ankle" to "후면 - 양 발목 기울기",
                "back_horizontal_distance_sub_ankle" to "후면 - 양 발목 높이 차",
                "back_horizontal_distance_heel_right" to "후면 - 중심에서 오른쪽 발목 거리")
        )
    ) // total 76개

    enum class Status{
        DANGER, WARNING, NORMAL
    }

    // 측정 완료 후 measure_info의 painpart만들기 - motherJa에는 dynamic포함된 값있어야함
    fun getPairParts(context: Context, motherJa: JSONArray) : MutableList<Pair<String, Status>> {
        val results = mutableListOf<Pair<String, Status>>()
        matchedUris.forEach{ (part, seqList) ->
            val tempPart = mutableListOf<Pair<String, Status>>()
            seqList.forEachIndexed { index, i ->
                val measureResult = motherJa.getJSONObject(i)
                val partIndex = matchedIndexs.indexOf(part)
                val mainSeq = mainPartSeqs[partIndex]

                val errorBound = if (Singleton_t_user.getInstance(context).jsonObject?.optInt("gender") == 1)
                    femaleErrorBounds[partIndex] else maleErrorBounds[partIndex]

                mainSeq[i]?.forEach{ (columnName, rawDataName) ->
                    val boundTriple = errorBound[i]?.get(columnName) // Triple<Float, Float, Float> 사용
                    if (boundTriple != null) {
                        val (center, warning, danger) = boundTriple
                            val lowerWarning = center - warning
                            val upperWarning = center + warning
                            val lowerDanger = center - danger
                            val upperDanger = center + danger


                            val data = measureResult.optDouble(columnName).toFloat()
                            when {
                                data < lowerDanger || data > upperDanger -> {
                                    // 위험
                                    tempPart.add(Pair(rawDataName, Status.DANGER))
                                }
                                data < lowerWarning || data > upperWarning -> {
                                    // 주의
                                    tempPart.add(Pair(rawDataName, Status.WARNING))
                                }
                                else -> {
                                    // 정상
                                    tempPart.add(Pair(rawDataName, Status.NORMAL))
                                }
                            }
//                        }
                    }
                }
            }
            val dangerCount = tempPart.count { it.second == Status.DANGER }
            val warningCount = tempPart.count { it.second == Status.WARNING }
            val normalCount = tempPart.count { it.second == Status.NORMAL }
//            Log.v("부위카운트", "$part: ($dangerCount, $warningCount, $normalCount)")
            val total = dangerCount + warningCount + normalCount
            if (dangerCount > total / 2) results.add(Pair(part, Status.DANGER))
            if (warningCount > total / 2) results.add(Pair(part, Status.WARNING))
            if (normalCount > total / 2) results.add(Pair(part, Status.NORMAL))

            // Case 2: danger가 warning과 normal의 합보다 큰 경우
            if (dangerCount > warningCount + normalCount) results.add(Pair(part, Status.DANGER))

            // Case 3: 동률 처리 (수정된 규칙)
            when {
                // danger와 warning이 같은 경우 -> DANGER
                dangerCount == warningCount && dangerCount > normalCount -> results.add(Pair(part, Status.DANGER))

                // warning과 normal이 같은 경우 -> NORMAL
                warningCount == normalCount && warningCount > dangerCount -> results.add(Pair(part, Status.NORMAL))

                // danger와 normal이 같고 warning보다 많은 경우
                dangerCount == normalCount && dangerCount > warningCount -> results.add(Pair(part, Status.WARNING))
            }
            // Case 4: warning + danger가 normal보다 많은 경우
            if (warningCount + dangerCount > normalCount) {
                if (dangerCount > warningCount) results.add(Pair(part, Status.DANGER)) else results.add(Pair(part, Status.WARNING))
            }
        }
        return results
    }

    fun calculateOverall(parts: MutableList<Pair<String, Status>>) : Int {
        val scores = mapOf(
            Status.DANGER to 39,
            Status.WARNING to 64,
            Status.NORMAL to 95
        )
        val weightScore = 1.65
        val reverseWeightScore = 0.7
        var weightedScoreSum = 0.0
        var totalWeight = 0.0
        for (part in parts) {
            val (bodyPart, status) = part
            val weight = when {
                bodyPart.contains("팔꿉") -> reverseWeightScore
                bodyPart.contains("손목") -> reverseWeightScore
                bodyPart.contains("무릎") -> weightScore
                else -> 1.0
            }
            weightedScoreSum += (scores[status] ?: 0) * weight
            totalWeight += weight
        }
        return if (totalWeight > 0) (weightedScoreSum / totalWeight).toInt() else 0
    }
    fun normalizeAngle(angle: Float): Float {
        return if (angle < 0) angle + 360f else angle
    }

    // mainPartAnalysis에서 unit 만들기
    fun getAnalysisUnits(context: Context, part: String, currentKey: Int, measureResult: JSONArray): MutableList<AnalysisUnitVO> {
        val result = mutableListOf<AnalysisUnitVO>()
        val partIndex = matchedIndexs.indexOf(part) // list(0, 3, 4, 5, 6)

        // partIndex에 해당하는 mainPartSeqs와 errorBounds 가져오기
        val mainSeq = mainPartSeqs[partIndex]
        val errorBound = if (Singleton_t_user.getInstance(context).jsonObject?.optInt("gender") == 1) femaleErrorBounds[partIndex] else maleErrorBounds[partIndex]

        if (currentKey != 1) { // measureResult에서 dynamic .length() = 7
            val jo = measureResult.getJSONObject(currentKey)
            // 현재 key에 해당하는 데이터만 처리
            mainSeq[currentKey]?.forEach { (columnName, rawDataName) ->
                // errorBounds에서 해당하는 Pair 값 찾기
                val boundPair = errorBound[currentKey]?.get(columnName)

                if (boundPair != null) {
                    val rawData = jo.optDouble(columnName).toFloat()
                    // 180을 기준으로 하는 정면만 정규화를 통해서 0에 맞춰서 값을 게산.
                    // 값이 180이 기준일 때만 0으로 정규화
                    val normalizedRaw = when (columnName.contains("angle") && currentKey in listOf(0)) {
                        true -> {
//                            if (rawData < 0) -(normalizeAngle(rawData) % 180) else normalizeAngle(rawData) % 180
//                            if (rawData < 0) (rawData + 360) % 360 else rawData % 360
                            (normalizeAngle(rawData) + 360) % 360
                        }
                        false -> {
                            rawData
                        }
                    }
                    val boundCenter = when (columnName.contains("angle") && currentKey in listOf(0, 5)) {
                        true -> {
                            when {
                                (boundPair.first < -90f) -> { // -180일 때
                                    -(normalizeAngle(boundPair.first) % 360)
                                }
                                else -> {
                                    boundPair.first
                                }
                            }
                        }
                        false -> {
                            boundPair.first
                        }
                    }

                    val state  = when {
                        abs(abs(boundCenter) - abs(rawData)) <= 1f -> 1 // 오차가 거의 없으면 걍 1
                        normalizedRaw < (boundCenter - boundPair.third) || normalizedRaw > (boundCenter + boundPair.third) -> 3
                        normalizedRaw < (boundCenter - boundPair.second) ||  normalizedRaw > (boundCenter + boundPair.second) -> 2
//                        abs(rawData) > (abs(boundPair.first) - 1) &&  normalizedRaw < (abs(boundPair.first) + 1) -> 1 // 정상범위에서 1의 오차내에 있으면 그냥 1
                        else -> 1
                    }
//                    Log.v("값들", "${rawDataName}, rawData: $rawData, 범위: ${(abs(boundPair.first) - 0.5)} ${(abs(boundPair.first) + 0.5)}")
                    Log.v("값들", "${rawDataName}, state: $state 값:  $rawData -> $normalizedRaw boundCenter: ${boundCenter}, 위험: ${(boundCenter - boundPair.third)}, ${(boundCenter + boundPair.third)}, 주의: ${(boundCenter - boundPair.second)}, ${(boundCenter + boundPair.second)}")
                    result.add(
                        AnalysisUnitVO(
                            columnName = columnName,
                            rawDataName = rawDataName,
                            rawData = rawData,
                            rawDataBound = boundPair,
                            summary = setLabels(columnName),
                            state = state,
                            seq = currentKey
                        )
                    )
                }
            }
        }
        return result
    }

    // poselandmark에서 3번 seq의 값을 가져와서 정면카메라인지, 후면카메라인지 판단
    fun judgeFrontCamera(seq: Int, plr: List<PoseLandmarkResult.PoseLandmark>) :Boolean {
        // 좌측면 측정 knee-ankle-toe의 각도가 좌/우측 별러져있는 값에 따라 정면, 후면 카메라 판단
        // 우측면 측정 반대
        // 그 이외 nose-leftShoulder-rightShoulder 의 각도 보기 정면, 후면에서
        val isFrontLens = when (seq) {
            3 -> determineDirection(plr[25].x, plr[25].y, plr[27].x, plr[27].y, plr[31].x, plr[31].y) // 좌측
            4 -> !determineDirection(plr[25].x, plr[25].y, plr[27].x, plr[27].y, plr[31].x, plr[31].y) // 우측
            0, 2 -> determineDirection(plr[0].x, plr[0].y, plr[11].x, plr[11].y, plr[13].x, plr[13].y) // 정면
//            1 -> determineDirection(plr[0].x, plr[0].y, plr[11].x, plr[11].y, plr[13].x, plr[13].y) // 정면
            else -> !determineDirection(plr[0].x, plr[0].y, plr[11].x, plr[11].y, plr[13].x, plr[13].y) // 후면
        }
        return isFrontLens
    }

    // dynamic에서
    fun judgeFrontCameraByDynamic(jaFrame1: List<List<Pair<Float, Float>>>) : Boolean {
        Log.v("값", "${jaFrame1[0][0].first}, ${jaFrame1[1][0].first}")
        // 첫번쨰 index는 부위임 0 : 왼쪽 어깨 1: 오른쪽 어깨  / 2번째 인덱스가 frame
        return if (jaFrame1[0][0].first > jaFrame1[1][0].first) {
            true
        } else {
            false
        }
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
            if (imageUrls != null && imageUrls != "") {
                val imageFile = File(imageUrls)
//                Log.v("setImages", imageFile.toUri().toString())
                val bitmap = BitmapFactory.decodeFile(imageUrls)
                fragment.lifecycleScope.launch(Dispatchers.Main) {
                    ssiv.setImage(ImageSource.uri(imageFile.toUri().toString()))
                    ssiv.setOnImageEventListener(object : SubsamplingScaleImageView.OnImageEventListener {
                        override fun onReady() {
                            if (!isSet) {

                                val imageViewHeight = ssiv.height
                                // iv에 들어간 image의 크기 같음 screenWidth

                                val sHeight = ssiv.sHeight
                                // 스케일 비율 계산
                                val scaleFactorX = imageViewHeight / sHeight.toFloat()

                                val poseLandmarkResult = fromCoordinates(coordinates)
//                                Log.v("댄저파트", "${measureVO.dangerParts}")
                                val combinedBitmap = ImageProcessingUtil.combineImageAndOverlay(
                                    bitmap,
                                    poseLandmarkResult,
                                    seq,
                                    measureVO.dangerParts,
                                )
                                isSet = true
                                // ------# MeasureDetail의 Solo #------
                                // 가로비율은 2배로 확대 세로 비율은 그대로 보여주기
                                if (case in listOf("solo") ) {
                                    val targetRatio = combinedBitmap.height.toFloat() / combinedBitmap.width.toFloat()
                                    Log.v("targetRatio", "$targetRatio, ${combinedBitmap.height}, ${combinedBitmap.width}")
                                    val scaleFactor = if (targetRatio > 1 ) 1f else 2f // 원하는 확대 비율 (가로만 확대하기 )
                                    val scaledBitmap = combinedBitmap.scale(
                                        (combinedBitmap.width * scaleFactor).toInt(),
                                        (combinedBitmap.height * scaleFactor).toInt()
                                    )
                                    // 가로 비율일 경우
                                    if (targetRatio < 1) {
                                        val rePaintedBitmap = rePaintDirection(cropToPortraitRatio(scaledBitmap), seq)
                                        ssiv.setImage(ImageSource.bitmap(
                                            rePaintedBitmap
                                        ))
                                    } else {
                                        val croppedBitmap = ImageSource.bitmap(
                                            cropToPortraitRatio(scaledBitmap)
                                        )
                                        drawDirectionUIOnBitmap(cropToPortraitRatio(scaledBitmap), seq)
                                        ssiv.setImage(croppedBitmap)
                                    }

                                    // 이미지 크기 맞추기
                                    // ------# trend 비교 일 때 #------
                                } else if (case in listOf("trend", "mainPart") ) {
                                    val ratio = bitmap.height.toFloat() / bitmap.width.toFloat()
                                    if (ratio < 1) {
                                        val croppedBitmap = cropToPortraitRatio(combinedBitmap)
                                        // 가로 비율일 경우 LR을 다시 그림
                                        val directedBitmap = rePaintDirection(croppedBitmap, seq)
                                        ssiv.setImage(ImageSource.bitmap(
                                            directedBitmap
                                        ))
                                    } else {
                                        ssiv.setImage(ImageSource.bitmap(
                                            cropToPortraitRatio(combinedBitmap)
                                        ))
                                    }
                                } else {
                                    // ------# analysisFragment #------
                                    when (seq) {
                                        0, 2, 3, 4 -> ssiv.setImage(
                                            ImageSource.bitmap(
                                                cropToPortraitRatio(combinedBitmap)
                                            ))
                                        else -> ssiv.setImage(ImageSource.bitmap(combinedBitmap))
                                    }
                                }
                                ssiv.maxScale = 3.5f
                                ssiv.minScale = 1f
                                if (case in listOf("mainPart", "")) {
                                    ssiv.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP)
                                    ssiv.setScaleAndCenter(scaleFactorX, PointF(ssiv.sWidth / 2f, ssiv.sHeight / 2f))
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
            Log.e("scalingError", "IndexOutOfBound: ${e.message}")
        } catch (e: FileNotFoundException) {
            Log.e("scalingError", "FileNotFound: ${e.message}")
        } catch (e: IllegalStateException) {
            Log.e("scalingError", "IllegalState: ${e.message}" )
        } catch (e: ClassNotFoundException) {
            Log.e("scalingError", "Class Not Found: ${e.message}" )
        } catch (e: Exception) {
            Log.e("scalingError", "Exception: ${e.message}" )
        }
    }
    fun getVideoDimensions(context : Context, videoUri: Uri?) : Pair<Int, Int> {
        if (videoUri == null) {
            Log.e("videoUri", "$videoUri")
            return Pair(0, 0) // 기본값 반환
        }
//        Log.e("videoUri", "$videoUri")
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        val videoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
        val videoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
        retriever.release()
        return Pair(videoWidth, videoHeight)
    }

//    // media3를 통한 동영상 크롭
//    @OptIn(androidx.media3.common.util.UnstableApi::class)
//    @UnstableApi
//    private suspend fun cropVideoTo916Ratio(
//        context: Context,
//        inputPath: String,
//        outputPath: String,
//        videoWidth: Int,
//        videoHeight: Int,
//
//    ) : Result<File> =
//        suspendCoroutine { continuation ->
//            // 입력 비디오 URI 생성
//            val inputUri = Uri.fromFile(File(inputPath))
//            val mediaItem = MediaItem.fromUri(inputUri)
//
//            val targetAspectRatio = 9f / 16f // 세로 비율 (0.5625)
//
//            val sourceAspectRatio = videoWidth / videoHeight
//
//            val cropLeft: Float
//            val cropRight: Float
//            val cropTop: Float
//            val cropBottom: Float
//
//            if (sourceAspectRatio > targetAspectRatio) {
//                // 좌우 크롭 (16:9 → 9:16으로 변환 시 이 경우가 적용됨)
//                val targetWidth = videoHeight * targetAspectRatio
//                val cropRatio = targetWidth / videoWidth
//                Log.v("크롭기준수치", "$targetWidth,  $cropRatio")
//                // Media3의 Crop은 left < right 조건이 필요합니다
//                cropLeft = -(cropRatio)   // 왼쪽에서 잘라낼 비율
//                cropRight = cropRatio   // 오른쪽에서 잘라낼 비율
//                cropTop = -1f                // 상단은 잘라내지 않음
//                cropBottom = 1f              // 하단은 잘라내지 않음
//            } else {
//                // 상하 크롭 (이미 세로가 긴 경우)
//                val targetHeight = videoWidth / targetAspectRatio
//                val cropAmount = (videoHeight - targetHeight) / 2f
//                val cropRatio = cropAmount / videoHeight
//
//                cropLeft = -1f               // 왼쪽은 잘라내지 않음
//                cropRight = 1f               // 오른쪽은 잘라내지 않음
//                cropTop = -1f + cropRatio    // 상단에서 잘라낼 비율
//                cropBottom = 1f - cropRatio  // 하단에서 잘라낼 비율
//            }
//
//            Log.d("CropDebug", "left: $cropLeft, right: $cropRight, top: $cropTop, bottom: $cropBottom")
//
//            val cropEffect = Crop(cropLeft, cropRight, cropTop, cropBottom)
//            val effects = Effects(
//                listOf(ChannelMixingAudioProcessor()),
//                listOf(cropEffect)
//            )
//
//            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
//                .setEffects(effects)
//                .build()
//
//            val transformer = Transformer.Builder(context)
//                .setVideoMimeType(MimeTypes.VIDEO_H264)
//                .addListener(object : Transformer.Listener {
//                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
//                        super.onCompleted(composition, exportResult)
//                        continuation.resume(Result.success(File(outputPath)))
//                    }
//
//                    override fun onError(
//                        composition: Composition,
//                        exportResult: ExportResult,
//                        exportException: ExportException,
//                    ) {
//                        super.onError(composition, exportResult, exportException)
//
//                        continuation.resume(Result.failure(exportException))
//                    }
//                }).build()
//            transformer.start(editedMediaItem, outputPath)
//        }
//
//
//    // 사용 예시 (코루틴 내에서)
//    @UnstableApi
//    fun performVideoCrop(context: Context, inputFile: File, outputPath: String, onComplete: (Result<File>) -> Unit) {
//        CoroutineScope(Dispatchers.Main).launch {
//            val (videoWidth, videoHeight) = getVideoDimensions(context, inputFile.toUri())
//            Log.v("영상크기", "$videoWidth, $videoHeight")
//            val result = cropVideoTo916Ratio(
//                context,
//                inputPath = inputFile.absolutePath,
//                outputPath = outputPath,
//                videoWidth = videoWidth,
//                videoHeight = videoHeight,
//            )
//            onComplete(result)
//        }
//    }
//
//    @UnstableApi
//    private suspend fun flipVideo(
//        context: Context,
//        inputPath: String,
//        outputPath: String
//    ): Result<File> = suspendCoroutine { cont ->
//        val inputUri = Uri.fromFile(File(inputPath))
//        val mediaItem = MediaItem.fromUri(inputUri)
//
//        val flipEffect = ScaleAndRotateTransformation.Builder()
//            .setScale(-1f, 1f)
//            .build()
//        val effects = Effects(
//            listOf(ChannelMixingAudioProcessor()),
//            listOf(flipEffect)
//        )
//        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
//            .setEffects(effects)
//            .build()
//
//        val outputFile = File(outputPath)
//        val parentDir = outputFile.parentFile
//        if (parentDir != null && !parentDir.exists()) {
//            Log.e("mkdirs알림", "$parentDir is mkdirs.")
//            parentDir.mkdirs()
//        }
//        Log.e("mkdirs이후알림", "${File(outputPath)}")
//        if (File(outputPath).exists()) {
//            File(outputPath).delete()
//        }
//
//        val transformer = Transformer.Builder(context)
//            .setVideoMimeType(MimeTypes.VIDEO_H264)
//            .addListener(object : Transformer.Listener {
//                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
//                    cont.resume(Result.success(File(outputPath)))
//                }
//
//                override fun onError(
//                    composition: Composition,
//                    exportResult: ExportResult,
//                    exportException: ExportException
//                ) {
//                    cont.resume(Result.failure(exportException))
//                }
//            }).build()
//
//        transformer.start(editedMediaItem, outputPath)
//    }
//
//    @UnstableApi
//    fun performVideoFlip(
//        context: Context,
//        inputPath: String,
//        outputPath: String,
//        onComplete: (Result<File>) -> Unit
//    ) {
//        CoroutineScope(Dispatchers.Main).launch {
//            val result = flipVideo(context, inputPath, outputPath)
//            onComplete(result)
//        }
//    }

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

    // 평균과 설명을 넣어주는 곳
    private fun setLabels(columnName : String) : String {
        return when (columnName) {
            // 목관절
            "front_horizontal_angle_ear" -> "기울기 값 180° 기준으로 1° 오차 이내가 표준적인 기울기 입니다. 한쪽으로 기울었을 경우, 기울어진 반대편의 목빗근의 스트레칭을 권장드립니다."
            "front_horizontal_distance_sub_ear" -> "양 귀의 높이 차이를 의미합니다. 값 0cm를 기준으로 1cm 오차 이내가 표준 어깨 높이 차이 입니다."
            "side_left_vertical_angle_ear_shoulder" -> "측면에서는 귀와 어깨가 일직선 상에 있어야 가장 이상적입니다. 목이 앞으로 나와있을 수록 수직에서 멀어지며, 굽은 등, 허리 교정을 추천드립니다."
            "side_right_vertical_angle_ear_shoulder" -> "측면에서는 귀와 어깨가 일직선 상에 있어야 가장 이상적입니다. 목이 앞으로 나와있을 수록 수직에서 멀어지며, 굽은 등, 허리 교정을 추천드립니다."
            "back_horizontal_angle_ear" -> "기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다. 한쪽으로 기울었을 경우, 기울어진 반대편의 목빗근과 지지하는 후면 어깨 강화를 추천드립니다."
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
            "front_horizontal_distance_sub_hip" -> "좌우 양 골반의 높이는 수평에서 3cm 이내가 정상입니다. 골반 기울기와 비례하기 때문에 기울어진 쪽 무릎 슬개골의 폄을 확인해 골반 수평 정렬을 목표로 운동하세요"
            "side_left_horizontal_distance_hip" -> "발뒷꿈치에서 시작되는 중심선에서 골반까지의 거리를 의미합니다. 우측과 비교해서 몸의 쏠림, 골반 전방 경사를 판단할 수 있습니다."
            "back_horizontal_angle_hip" -> "양 골반의 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다"
            "back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder" -> "앉은 자세에서 골반 중심-양 어깨를 이은 삼각형의 왼쪽 어깨 각도입니다. 각도 값이 높을 수록 굽은 등을 교정해주세요"
            // 우측 골반
            "front_vertical_angle_hip_knee_right" -> "골반-무릎 간의 기울기는 90° 기준으로 약 2° 이내가 정상입니다. 측면의 골반-무릎-발목 기울기와 함께 비교해서 평소 무릎이 조금 굽어진 자세로 서있는지 확인해보세요."
//            "front_horizontal_angle_hip" -> "양 골반의 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다"
            "side_right_horizontal_distance_hip" -> "발뒷꿈치에서 시작되는 h중심선에서 골반까지의 거리를 의미합니다. 좌측과 비교해서 몸의 쏠림, 골반 전방 경사를 판단할 수 있습니다."
//            "back_horizontal_angle_hip" -> "양 골반의 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다"
//            "back_sit_vertical_angle_left_shoulder_center_hip_right_shoulder" -> "앉은 자세에서 양 어깨와 골반 중심의 각도를 의미합니다. 각도 값 50° 기준으로 10° 이내의 범위를 표준적인 각도입니다."
//            "back_sit_vertical_angle_right_shoulder_left_shoulder_center_hip" -> "앉은 자세에서 골반 중심-양 어깨를 이은 삼각형의 오른쪽 어깨 각도입니다. 각도 값이 높을 수록 굽은 등을 교정해주세요"
            // 좌측 무릎
            "front_horizontal_angle_knee" -> "양 골반의 기울기는 180° 기준으로 약 2° 이내가 정상입니다. 측면의 어깨 각도, 후면의 발뒷꿈치 위치를 비교해서, 평소 서있는 자세를 교정해보세요"
            "front_horizontal_distance_knee_left" -> "몸의 중심에서 우측 무릎의 거리는 값 13cm를 기준으로 약 2cm 이내가 정상입니다. 우측 무릎과 비교해서 거리가 유난히 멀다면, 발의 정렬이 잘못돼 정강이, 대퇴부의 긴장을 풀어주세요."
            "front_vertical_angle_hip_knee_ankle_left" -> "골반-무릎-발목 간의 기울기는 175° 기준으로 약 3° 오차 이내가 표준적인 기울기 입니다. 벗어날 경우 슬개골 왕복운동을 추천드립니다."
            "side_left_vertical_angle_hip_knee" -> "측면에서 골반-무릎의 기울기를 의미합니다. 기울기 값 90° 기준으로 5° 이내의 범위를 표준적인 기울기입니다. 벗어날 경우, 햄스트링, 종아리근육의 긴장이 있을 수 있으니 스트레칭을 추천드립니다"
            "side_left_vertical_angle_hip_knee_ankle" -> "측면에서 골반-무릎-발목의 기울기는 90° 기준으로 5° 이내의 범위를 표준적인 기울기입니다. 정면과 비교해서 평소 서있는 자세에서 다리가 조금 굽힘이 있는지 확인하고 교정 해보세요"
            "back_horizontal_angle_knee" -> "몸 뒷편의 무릎 기울기는  0°를 기준으로 약 2° 오차 이내가 정상입니다. 이를 벗어나면 발의 정렬 문제, 주변 근육인 햄스트링과 종아리 근육의 긴장을 풀어보세요"
            "back_horizontal_distance_knee_left" -> "몸의 중심에서 좌측 무릎까지의 거리를 의미합니다. 거리 10cm 기준으로 5cm 오차 이내가 표준적인 거리 입니다. 벗어날 경우 좌측 다리에 무게를 더 실어 서는 습관이 의심됩니다. 골반 교정 운동을 추천드립니다"
            // 우측 무릎
//            "front_horizontal_angle_knee" -> "양 무릎의 위치를 비교한 기울기를 의미합니다. 기울기 값 0° 기준으로 1° 오차 이내가 표준적인 기울기 입니다."
            "front_horizontal_distance_knee_right" -> "몸의 중심에서 우측 무릎의 거리는 값 13cm를 기준으로 약 2cm 이내가 정상입니다. 우측 무릎과 비교해서 거리가 유난히 멀다면, 발의 정렬이 잘못돼 정강이, 대퇴부의 긴장을 풀어주세요."
            "front_vertical_angle_hip_knee_ankle_right" -> "골반-무릎-발목 간의 기울기는 175° 기준으로 약 3° 오차 이내가 표준적인 기울기 입니다. 벗어날 경우 슬개골 왕복운동을 추천드립니다."
            "side_right_vertical_angle_hip_knee" -> "측면에서 골반-무릎의 기울기를 의미합니다. 기울기 값 90° 기준으로 5° 이내의 범위를 표준적인 기울기입니다. 벗어날 경우, 햄스트링, 종아리근육의 긴장이 있을 수 있으니 스트레칭을 추천드립니다"
            "side_right_vertical_angle_hip_knee_ankle" -> "측면에서 골반-무릎-발목의 기울기는 90° 기준으로 5° 이내의 범위를 표준적인 기울기입니다. 정면과 비교해서 평소 서있는 자세에서 다리가 조금 굽힘이 있는지 확인하고 교정 해보세요"
//            "back_horizontal_angle_knee" -> "양 무릎의 기울기를 의미합니다. 기울기 값 0° 기준으로 0.5° 오차 이내가 표준적인 기울기 입니다"
            "back_horizontal_distance_knee_right" -> "몸의 중심에서 좌측 무릎까지의 거리를 의미합니다. 기울기 값 10cm 기준으로 5cm 오차 이내가 표준적인 거리 입니다. 벗어날 경우 좌측 다리에 무게를 더 실어 서는 습관이 의심됩니다. 골반 교정 운동을 추천드립니다"
            // 좌측 발목
            "front_vertical_angle_knee_ankle_left" -> "무릎-발목간 수직각도는 값 88° 기준으로 약 4° 오차 이내가 정상입니다. 벗어날 경우 평소 발목과 아킬레스건의 긴장, 가자미근을 스트레칭하길 추천드립니다"
            "front_horizontal_angle_ankle" -> "양 발목의 위치를 비교한 기울기를 의미합니다. 기울기 값 180° 기준으로 약 4° 오차 이내가 표준적인 기울기 입니다."
            "front_horizontal_distance_ankle_left" ->  "몸의 중심-발목 간 거리는 약 10cm를 기준으로 5cm 이내가 정상입니다. 우측 발목과 비교해 값의 차이가 많이 날 경우, 발의 정렬로 하지 전체적인 근육을 풀어야 합니다."
            "back_horizontal_angle_ankle" -> "후면에서 양발목의 기울기는 0도를 기준으로 약 2도 이내가 정상입니다. 이를 벗어날 경우, 평소 선 자세의 발의 위치가 정렬돼 있지 않아 무릎과 허리에 과도한 긴장을 줄 수 있습니다."
            "back_horizontal_distance_sub_ankle" -> "후면에서 양 발목의 높이 차는 값 0cm 기준으로 1cm 오차 이내가 정상입니다. 이를 벗어날 경우, 전면과 비교해 발의 정렬을 교정해주세요"
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

    fun createSeqGuideComment(seq: Int) : String {
        return when (seq) {
            0 -> "정면 선자세를 먼저 측정해 전체적인 체형의 이상 상태를 확인합니다."
            1 -> "오버헤드 스쿼트를 통해 어깨, 무릎, 골반의 궤적을 통해 흔들림을 확인합니다."
            2 -> "팔의 정렬을 통해 손목, 팔꿉, 어깨 후면의 긴장 상태를 확인합니다."
            3 -> "발뒷꿈치 중심선을 기준으로 좌측 각 관절의 위치를 확인합니다."
            4 -> "발뒷꿈치 중심선을 기준으로 우측 각 관절의 위치를 확인합니다."
            5 -> "정면과 비교해 어깨, 무릎, 발의 정렬 상태를 확인합니다."
            6 -> "후면 선자세와 비교해, 골반의 틀어짐과 발의 정렬 상태를 비교합니다."
            else -> ""
        }
    }

    fun createMeasureComment(dangerParts : MutableList<Pair<String, Float>>?) : List<String> {
        val result = mutableListOf<String>()
        val painParts = dangerParts?.map { it.first }
        val keywordToCommentMap = mapOf(
            listOf("목관절", "좌측 어깨", "우측 어깨") to "어깨 불균형과 거북목을 조심하세요.",
            // 목 -> 하체 -> 작은 범위의 상체, -> 작은 범위의 어깨
            listOf("좌측 골반", "우측 골반") to "골반 균형을 확인하세요",
            listOf("좌측 어깨", "좌측 팔꿉", "좌측 손목") to "좌측 상체의 긴장을 의심해야 합니다.",
            listOf("좌측 골반", "우측 무릎") to "우측 쏠림을 의심해보세요",
            listOf("우측 어깨", "우측 팔꿉", "우측 손목") to "우측 상체의 긴장을 의심해야 합니다.",
            listOf("우측 어깨", "좌측 골반") to "상체 좌측 쏠림으로 인한 긴장을 의심해야 합니다.",
            listOf("우측 골반", "좌측 무릎") to "좌측 쏠림을 의심해보세요",
            listOf("좌측 팔꿉", "좌측 손목") to "좌측 팔 근육과 주변 어깨 근육을 확인하세요",
            listOf("좌측 어깨", "우측 골반") to "상체 우측 쏠림으로 인한 긴장을 의심해야 합니다.",
            listOf("좌측 무릎", "좌측 발목") to "좌측 다리의 정렬을 확인하세요",
            listOf("우측 팔꿉", "우측 손목") to "우측 팔 근육과 주변 어깨 근육을 확인하세요",
            listOf("우측 무릎", "우측 발목") to "우측 다리의 정렬을 확인하세요",
            listOf("목관절", "좌측 어깨") to "우측으로 쏠린 상체를 교정해보세요",
            listOf("목관절", "우측 어깨") to "좌측으로 쏠린 상체를 교정해보세요",
            listOf("우측 어깨", "우측 손목", "좌측 골반") to "우측 상체의 긴장을 의심해야 합니다.",
            listOf("좌측 어깨", "좌측 손목", "우측 골반") to "좌측 상체의 긴장을 의심해야 합니다.",
            listOf("좌측 어깨", "우측 어깨") to "라운드 숄더나 자세 틀어짐을 확인하세요",
            listOf("좌측 팔꿉", "우측 팔꿉") to "상완근, 회전근개의 긴장을 의심해야합니다.",
            listOf("좌측 무릎", "우측 무릎") to "무릎 주변의 근육의 수축과 이완을 확인하세요",
            listOf("목관절") to "거북목과 머리쏠림을 확인하세요",
        )
        for ((keywords, comments) in keywordToCommentMap) {
            if (result.size < 3) {
                if (painParts != null && painParts.containsAll(keywords)) {
                    result.add(comments)
                }
            }
        }
        return result
    }

    // 그냥 여기다가 seq 를 0 1, 2, 3으로 받는 형식으로 변형
    fun createSummary(part: String?, seq: Int, units: MutableList<AnalysisUnitVO>?): String {
        val resultString = StringBuilder()
        fun countWarning() : Boolean {
            val totalUnits = units?.size
            val warningUnits = units?.count { it.state > 1 }

            val percent = totalUnits?.toFloat()?.let { warningUnits?.div(it) }
            // 갯수를 셌는데 과반수면 해당관절 + 해당 관절
            if (percent != null) {
                return percent >= 0.5f
            } else {
                return true
            }
        }

        if (seq == 3) {
            resultString.append("스쿼트 정보를 확인하세요")
        }
        when (part) {
            "목관절" -> {
                when (seq) {
                    0 -> if (countWarning()) resultString.append("중심선을 기준으로 더 먼 곳에 있는 귀 아래의 목빗근을 이완시켜주시고, 정확한 자세를 위해 측면과 비교해주세요. ")
                    1 -> if (countWarning()) resultString.append("어깨와 코를 이은 선을 반대편과 비교하세요. 한 쪽의 길이가 더 길다면, 목이 짧은 방향으로 틀어져 있어 짧은 방향의 목빗근을 이완시켜야 합니다. 장시간 긴장될 경우 두통의 원인이 될 수 있습니다.")
                    2 -> if (countWarning()) resultString.append("정면과 귀 기울기를 비교하세요. 목의 틀어짐을 더 정확하게 판단할 수 있습니다. ")
                }

            }
            "좌측 어깨","우측 어깨" -> {
                when (seq) {
                    0 -> if (countWarning()) resultString.append("기울어진 어깨 방향과 반대 방향의 골반이 불편하다면 척추 측만증을 의심할 수 있습니다. 후면과 이어서 비교해보세요. ")
                    1 -> if (countWarning()) resultString.append("중심선과 어깨가 멀어질 수록 라운드숄더 또는 허리 굽어짐을 의심할 수 있습니다. ")
                    2 -> if (countWarning()) resultString.append("정면과 어깨의 틀어짐을 비교해서 정확도를 높여보세요. ")
                }
                resultString.append("활동 전, 노머니 스트레칭과 회전근개 동적 스트레칭을 수시로 해주세요")
            }

            "좌측 팔꿉", "우측 팔꿉"-> {
                when (seq) {
                    0 -> if (countWarning()) resultString.append("팔꿉 측정의 상완과 하완이 일직선으로 굽혀지지 않는다면, 팔꿉과 이어진 견갑하근을 강화해서 저항을 길러야합니다. ")
                    1 -> if (countWarning()) resultString.append("팔의 각도가 굽혀져 있다면 이두근 긴장과 가동범위에 대해 스트레칭 해야합니다. ")
                }
            }
            "좌측 손목", "우측 손목" -> {
                when (seq) {
                    0 -> if (countWarning()) resultString.append("손목이 좌우가 다른 방향으로 꺾여있다면, 골프 엘보의 회전근을 이완시켜야 합니다. ")
                    1 -> if (countWarning()) resultString.append("손목의 거리가 멀수록 팔의 긴장을 의심해볼 수 있습니다. 팔꿉 자세와 비교해보세요. ")
                }
                resultString.append("적당한 압력으로 손바닥을 당기고 손등을 내리는 스트레칭을 권장합니다.")
            }
            "좌측 골반", "우측 골반" -> {
                when (seq) {
                    0 -> if (countWarning()) resultString.append("더 높이 위치한 골반이 긴장돼 있는 상태입니다. 이완된 장골근을 스트레칭 해 허리의 가동범위를 넓혀주세요")
                    1 -> if (countWarning()) resultString.append("중심선에서 골반이 멀어질 수록, 몸의 쏠림을 방지하고 밸런스를 키우고자 중둔근을 강화시켜주세요.")
                    2 -> if (countWarning()) resultString.append("앉은 자세와 비교해 골반 자체의 틀어짐인지 발의 정렬 문제로 인한 위치 문제인지 확인하세요. 발의 정렬로 인해 외복사근에 통증이 올 수 있습니다.")
                }
            }
            "좌측 무릎", "우측 무릎" -> {
                when (seq) {
                    0 -> if (countWarning()) resultString.append("골반 위치와 무릎의 위치가 일직선인지 확인하세요. 일직선에서 틀어질 수록 무릎의 연골이 닳아져 문제가 생길 수 있습니다.")
                    1 -> if (countWarning()) resultString.append("측면에서 다리 각도를 확인하세요. 굽혀져 있을 수록, 무릎 슬개골 굳어짐으로 문제가 생길 수 있습니다.")
                    2 -> if (countWarning()) resultString.append("양 발의 방향을 비교해서 몸의 중심에서 더 벌어지고 모아졌는지 확인하세요. 해당 발로 인해 골반과 무릎에 통증이 수반될 수 있습니다.")
                }
//                resultString.append("인대나 관절에 통증이 있을 경우, 발의 정렬이 맞지 않거나, 골반 주변 근육에 잘못된 힘이 전달되고 있을 수 있습니다. ")
            }

            "좌측 발목", "우측 발목"-> {
                when (seq) {
                    0 -> if (countWarning()) resultString.append("발목의 정렬을 골반, 무릎과 비교해보세요. 틀어짐이 심할 수록, 발의 지지력에 문제가 생겨, 허리, 무릎에 통증이 올 수 있습니다.")
                    2 -> if (countWarning()) resultString.append("양 발의 방향을 비교해서 몸의 중심에서 더 벌어지고 모아졌는지 확인하세요. 해당 발로 인해 골반과 무릎에 통증이 수반될 수 있습니다.")
                }
            }
        }
        return if (resultString.isEmpty()) "${part} 부위가 정상 범위 내에 있습니다." else resultString.toString()
    }
}