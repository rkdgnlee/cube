package com.tangoplus.tangoq.mediapipe

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.mediapipe.MathHelpers.isTablet
import kotlin.math.max
import kotlin.math.min

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    companion object {
        const val LANDMARK_STROKE_WIDTH = 5F
    }

    private var results: PoseLandmarkResult? = null

    private var pointPaint = Paint()
    private var linePaint = Paint()
    private var axisPaint = Paint()
    private var scaleFactorX: Float = 1f
    private var scaleFactorY : Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1
    private var currentRunningMode: RunningMode = RunningMode.IMAGE
    init {
        initPaints()
    }
    fun clear() {
        results = null
        pointPaint.reset()
        linePaint.reset()
        invalidate()
        initPaints()
    }

    @SuppressLint("ResourceAsColor")
    private fun initPaints() {
        // -----! 연결선 색 !-----
        linePaint.apply {
            color = 0xFFFFFFFF.toInt()
            strokeWidth = LANDMARK_STROKE_WIDTH
            style = Paint.Style.STROKE
        }

        // -----! 꼭짓점 색 !-----
        pointPaint.apply {
            color = R.color.mainColor
            strokeWidth = LANDMARK_STROKE_WIDTH
            style = Paint.Style.FILL
        }
        axisPaint.apply {
            color = Color.parseColor("#FF5449")
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }
    }



    fun setResults(
        poseLandmarkResult: PoseLandmarkResult,
        imageWidth: Int,
        imageHeight: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = poseLandmarkResult
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth
        currentRunningMode = runningMode

//        scaleFactorX = min(width * 1f / imageWidth, height * 1f / imageHeight)
//        scaleFactorY = max(width * 1f / imageWidth, height * 1f / imageHeight)
        scaleFactorX = if (isTablet(context)) max(width * 1f / imageWidth, height * 1f / imageHeight) else min(width * 1f / imageWidth, height * 1f / imageHeight)
        scaleFactorY = if (isTablet(context)) min(width * 1f / imageWidth, height * 1f / imageHeight) else max(width * 1f / imageWidth, height * 1f / imageHeight)
        invalidate()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results?.let { poseLandmarkResult ->
            drawLandmarks(canvas, poseLandmarkResult.landmarks)
        }

    }
    enum class RunningMode {
        IMAGE, VIDEO, LIVE_STREAM
    }

    private fun drawLandmarks(canvas: Canvas, landmarks: List<PoseLandmarkResult.PoseLandmark>) {

        if (landmarks.isEmpty()) {
            return
        }

        if (currentRunningMode == RunningMode.LIVE_STREAM) {

//            Log.v("스케일", "scale: (${width * 1f / imageWidth}, ${height * 1f / imageHeight})")
            val offsetX = (width - imageWidth * scaleFactorX) / 2
            val offsetY = (height - imageHeight * scaleFactorY) / 2
            landmarks.forEach { landmark ->
                canvas.drawPoint(
                    landmark.x * imageWidth * scaleFactorX + offsetX,
                    landmark.y * imageHeight * scaleFactorY + offsetY,
                    pointPaint
                )
            }

            // 안전하게 특정 랜드마크 접근
            val nose = landmarks.getOrNull(0)
            val leftShoulder = landmarks.getOrNull(11)
            val rightShoulder = landmarks.getOrNull(12)

            // 코와 어깨 중간점 연결선 그리기 (모든 필요한 점이 있을 때만)
            if (nose != null && leftShoulder != null && rightShoulder != null) {
                val noseX = nose.x * imageWidth * scaleFactorX + offsetX
                val noseY = nose.y * imageHeight * scaleFactorY + offsetY
                val midShoulderX = (leftShoulder.x + rightShoulder.x) / 2 * imageWidth * scaleFactorX + offsetX
                val midShoulderY = (leftShoulder.y + rightShoulder.y) / 2 * imageHeight * scaleFactorY + offsetY

                canvas.drawLine(noseX, noseY, midShoulderX, midShoulderY, linePaint)

            }

            val connections = listOf(
                // 얼굴
                Pair(0, 1), Pair(0, 4), Pair(1, 2), Pair(2, 3), Pair(3, 7), Pair(4, 5), Pair(5, 6), Pair(6, 8),
                // 손
                Pair(16, 18), Pair(16, 20), Pair(16, 22), Pair(15, 17), Pair(15, 19), Pair(15, 21),
                // 몸통 + 팔
                Pair(11, 12), Pair(11, 13), Pair(12, 14), Pair(13, 15), Pair(14, 16),
                // Legs
                Pair(11, 23), Pair(12, 24), Pair(23, 25), Pair(23, 24), Pair(24, 26), Pair(25, 27), Pair(26, 28),
                // 다리
                Pair(27, 31), Pair(28, 32), Pair(27, 29), Pair(28, 30),
            )

            connections.forEach { (start, end) ->
                if (start < landmarks.size && end < landmarks.size) {
                    canvas.drawLine(
                        landmarks[start].x * imageWidth * scaleFactorX + offsetX,
                        landmarks[start].y * imageHeight * scaleFactorY + offsetY,
                        landmarks[end].x * imageWidth * scaleFactorX + offsetX,
                        landmarks[end].y * imageHeight * scaleFactorY + offsetY,
                        linePaint
                    )
                }
            }
        }

        else { // video 일 때
//            this.scaleX = -1f
            val offsetX = (width - imageWidth * scaleFactorX) / 2
            val offsetY = (height - imageHeight * scaleFactorY) / 2

            landmarks.forEach { landmark ->
                canvas.drawPoint(
                    landmark.x * scaleFactorX + offsetX,
                    landmark.y * scaleFactorY + offsetY,
                    pointPaint
                )
            }

            // 안전하게 특정 랜드마크 접근
            val nose = landmarks.getOrNull(0)
            val leftShoulder = landmarks.getOrNull(11)
            val rightShoulder = landmarks.getOrNull(12)
            val leftIndex = landmarks.getOrNull(19)
            val rightIndex = landmarks.getOrNull(20)

            val leftHip = landmarks.getOrNull(23)
            val rightHip = landmarks.getOrNull(24)

            val leftKnee = landmarks.getOrNull(25)
            val rightKnee = landmarks.getOrNull(26)
            // 코와 어깨 중간점 연결선 그리기 (모든 필요한 점이 있을 때만)
            if (nose != null && leftShoulder != null && rightShoulder != null && leftIndex != null && rightIndex != null && leftHip != null && rightHip != null && leftKnee != null && rightKnee != null) {
                val noseX = nose.x * scaleFactorX + offsetX
                val noseY = nose.y * scaleFactorY + offsetY
                val midShoulderX = (leftShoulder.x + rightShoulder.x) / 2 * scaleFactorX + offsetX
                val midShoulderY = (leftShoulder.y + rightShoulder.y) / 2 * scaleFactorY + offsetY

                val leftIndexX = leftIndex.x * scaleFactorX + offsetX
                val leftIndexY = leftIndex.y * scaleFactorY + offsetY
                val rightIndexX = rightIndex.x * scaleFactorX + offsetX
                val rightIndexY = rightIndex.y * scaleFactorY + offsetY

                val leftHipX = leftHip.x * scaleFactorX + offsetX
                val leftHipY = leftHip.y * scaleFactorY + offsetY
                val rightHipX = rightHip.x * scaleFactorX + offsetX
                val rightHipY = rightHip.y * scaleFactorY + offsetY

                val leftKneeX = leftKnee.x * scaleFactorX + offsetX
                val leftKneeY = leftKnee.y * scaleFactorY + offsetY
                val rightKneeX = rightKnee.x * scaleFactorX + offsetX
                val rightKneeY = rightKnee.y * scaleFactorY + offsetY

                canvas.drawLine(noseX, noseY, midShoulderX, midShoulderY, linePaint)
                canvas.drawLine(leftIndexX + 100, leftIndexY, rightIndexX - 100, rightIndexY, axisPaint)
                canvas.drawLine(leftHipX + 100, leftHipY, rightHipX - 100, rightHipY, axisPaint)
                canvas.drawLine(leftKneeX + 100, leftKneeY, rightKneeX - 100, rightKneeY, axisPaint)
            }

            val connections = listOf(
                // 얼굴
                Pair(0, 1), Pair(0, 4), Pair(1, 2), Pair(2, 3), Pair(3, 7), Pair(4, 5), Pair(5, 6), Pair(6, 8),
                // 손
                Pair(16, 18), Pair(16, 20), Pair(16, 22), Pair(15, 17), Pair(15, 19), Pair(15, 21),
                // 몸통 + 팔
                Pair(11, 12), Pair(11, 13), Pair(12, 14), Pair(13, 15), Pair(14, 16),
                // Legs
                Pair(11, 23), Pair(12, 24), Pair(23, 25), Pair(23, 24), Pair(24, 26), Pair(25, 27), Pair(26, 28),
                // 다리
                Pair(27, 31), Pair(28, 32), Pair(27, 29), Pair(28, 30),
            )

            connections.forEach { (start, end) ->
                if (start < landmarks.size && end < landmarks.size) {
                    canvas.drawLine(
                        landmarks[start].x * scaleFactorX + offsetX,
                        landmarks[start].y  * scaleFactorY + offsetY,
                        landmarks[end].x * scaleFactorX + offsetX,
                        landmarks[end].y * scaleFactorY + offsetY,
                        linePaint
                    )

                }
            }
        }
    }
}