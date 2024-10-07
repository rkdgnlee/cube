package com.tangoplus.tangoq.mediapipe

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.tangoplus.tangoq.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToLong

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    companion object {
        const val LANDMARK_STROKE_WIDTH = 5F
    }

    private var results: PoseLandmarkResult? = null

    private var pointPaint = Paint()
    private var linePaint = Paint()
    private var textPaint = Paint()
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
        linePaint.color = 0xFFFFFFFF.toInt()
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        // -----! 꼭짓점 색 !-----
        pointPaint.color = R.color.mainColor
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL
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

        scaleFactorX = when (runningMode) {
            RunningMode.IMAGE, RunningMode.VIDEO -> {
                width * 1f / imageWidth
            }
            RunningMode.LIVE_STREAM -> {
                width * 1f / imageWidth
            }
        }
        scaleFactorY = when (runningMode) {
            RunningMode.IMAGE, RunningMode.VIDEO -> {
                height * 1f / imageHeight
            }
            RunningMode.LIVE_STREAM -> {
                height * 1f / imageHeight
            }
        }
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

            val offsetX = ((width - imageWidth * scaleFactorX) / 2 ) + 30
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

            // Draw lines between landmarks
            // Note: You'll need to define which landmarks should be connected
            // This is just an example and may need to be adjusted
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
            val offsetX = ((width - imageWidth * scaleFactorX) / 2 )
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

            // 코와 어깨 중간점 연결선 그리기 (모든 필요한 점이 있을 때만)
            if (nose != null && leftShoulder != null && rightShoulder != null) {
                val noseX = nose.x * scaleFactorX + offsetX
                val noseY = nose.y * scaleFactorY + offsetY
                val midShoulderX = (leftShoulder.x + rightShoulder.x) / 2 * scaleFactorX + offsetX
                val midShoulderY = (leftShoulder.y + rightShoulder.y) / 2 * scaleFactorY + offsetY

                canvas.drawLine(noseX, noseY, midShoulderX, midShoulderY, linePaint)

            }

            // Draw lines between landmarks
            // Note: You'll need to define which landmarks should be connected
            // This is just an example and may need to be adjusted
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