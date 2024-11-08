package com.tangoplus.tangoq.db

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.mediapipe.ImageProcessingUtil
import com.tangoplus.tangoq.mediapipe.ImageProcessingUtil.cropToPortraitRatio
import com.tangoplus.tangoq.mediapipe.PoseLandmarkResult.Companion.fromCoordinates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume

object MeasurementManager {


    fun getDangerParts(measureInfo: MeasureInfo) : MutableList<Pair<String, Float>> {
        val dangerParts = mutableListOf<Pair<String, Float>>()

        val neckRisk = measureInfo.risk_neck.toFloat()
        if (neckRisk > 0) {
            dangerParts.add(Pair("목관절", neckRisk))
        }

        val shoulderLeftRisk = measureInfo.risk_shoulder_left.toFloat()
        if (shoulderLeftRisk > 0) {
            dangerParts.add(Pair("우측 어깨", shoulderLeftRisk))
        }

        val shoulderRightRisk = measureInfo.risk_shoulder_right.toFloat()
        if (shoulderRightRisk > 0) {
            dangerParts.add(Pair("좌측 어깨", shoulderRightRisk))
        }

        val elbowLeftRisk = measureInfo.risk_elbow_left.toFloat()
        if (elbowLeftRisk > 0) {
            dangerParts.add(Pair("좌측 팔꿉", elbowLeftRisk))
        }
        val elbowRightRisk =  measureInfo.risk_elbow_right.toFloat()
        if (elbowRightRisk > 0) {
            dangerParts.add(Pair("우측 팔꿉", elbowRightRisk))
        }

        val wristLeftRisk = measureInfo.risk_wrist_left.toFloat()
        if (wristLeftRisk > 0) {
            dangerParts.add(Pair("좌측 손목", wristLeftRisk))
        }
        val wristRightRisk = measureInfo.risk_wrist_right.toFloat()
        if (wristRightRisk > 0) {
            dangerParts.add(Pair("우측 손목", wristRightRisk))
        }

        val hipLeftRisk = measureInfo.risk_hip_left.toFloat()
        if (hipLeftRisk > 0) {
            dangerParts.add(Pair("좌측 골반", hipLeftRisk))
        }
        val hipRightRisk = measureInfo.risk_hip_right.toFloat()
        if (hipRightRisk > 0) {
            dangerParts.add(Pair("우측 골반", hipRightRisk))
        }

        val kneeLeftRisk = measureInfo.risk_knee_left.toFloat()
        if (kneeLeftRisk > 0) {
            dangerParts.add(Pair("좌측 무릎", kneeLeftRisk))
        }
        val kneeRightRisk = measureInfo.risk_knee_right.toFloat()
        if (kneeRightRisk > 0) {
            dangerParts.add(Pair("우측 무릎", kneeRightRisk))
        }

        val ankleLeftRisk = measureInfo.risk_ankle_left.toFloat()
        if (ankleLeftRisk > 0) {
            dangerParts.add(Pair("좌측 발목", ankleLeftRisk))
        }
        val ankleRightRisk = measureInfo.risk_ankle_right.toFloat()
        if (ankleRightRisk > 0) {
            dangerParts.add(Pair("우측 발목", ankleRightRisk))
        }

        return dangerParts
    }
    fun convertToJsonArrays(dangerParts: List<Pair<String, Float>>): Pair<JSONArray, JSONArray> {
        val partIndices = mapOf(
            "목관절" to 1,
            "어깨" to 2,
            "팔꿉" to 3,
            "손목" to 4,
            "골반" to 5,
            "무릎" to 6,
            "발목" to 7
        )

        val indices = mutableListOf<Int>()
        val values = mutableListOf<Float>()

        dangerParts.forEach { (part, value) ->
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

            // 매핑된 인덱스가 있으면 추가
            partIndices[basicPart]?.let { index ->
                indices.add(index)
                values.add(value)
            }
        }

        return Pair(JSONArray(indices), JSONArray(values))
    }

    suspend fun setImage(fragment: Fragment, measureVO: MeasureVO?, seq: Int, ssiv: SubsamplingScaleImageView): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            val jsonData = measureVO?.measureResult?.optJSONObject(seq)
            val coordinates = extractImageCoordinates(jsonData!!)
            val imageUrls = measureVO.fileUris[seq]
            var isSet = false
            Log.v("시퀀스", "seq: ${seq}, view: (${ssiv.width}, ${ssiv.height}), imageSize: (${ssiv.sWidth}, ${ssiv.sHeight})")
            if (imageUrls != null) {
                val imageFile = imageUrls.let { File(it) }
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
                                val poseLandmarkResult = fromCoordinates(coordinates!!)
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
                                when (seq) {
                                    0, 2, 3, 4 -> ssiv.setImage(
                                        ImageSource.bitmap(
                                            cropToPortraitRatio(combinedBitmap)
                                        ))
                                    else -> ssiv.setImage(ImageSource.bitmap(combinedBitmap))
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
                    Log.v("ImageLoading", "Image loaded successfully. Width: ${bitmap.width}, Height: ${bitmap.height}")
                }
            } else { continuation.resume(false) }
        } catch (e: IndexOutOfBoundsException) {
            Log.e("Error", "${e}")
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
    private fun extractImageCoordinates(jsonData: JSONObject): List<Pair<Float, Float>>? {
        val poseData = jsonData.optJSONArray("pose_landmark")
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