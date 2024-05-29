package com.tangoplus.tangoq.mediapipe

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
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

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {
    /** -----------------------------------! 스켈레톤 선 굵기 !------------------------------------ */
    companion object {
        const val LANDMARK_STROKE_WIDTH = 6F
    }


    private var results: PoseLandmarkerResult? = null
    private var pointPaint = Paint()
    private var linePaint = Paint()
    private var textPaint = Paint()
    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

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
        linePaint.color =
            ContextCompat.getColor(context!!, R.color.white)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE
        textPaint.color = ContextCompat.getColor(context!!, R.color.mainColor)
        textPaint.textSize = 32f

        // -----! 꼭짓점 색 !-----
        pointPaint.color = R.color.mainColor
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results?.let { poseLandmarkerResult ->
            for(landmark in poseLandmarkerResult.landmarks()) {
                for(normalizedLandmark in landmark) {
                    canvas.drawPoint(
                        normalizedLandmark.x() * imageWidth * scaleFactor,
                        normalizedLandmark.y() * imageHeight * scaleFactor,
                        pointPaint
                    )
                }
                // ------! 기울기 라인 적기 !------
                PoseLandmarker.POSE_LANDMARKS.forEach {
                    if (it!!.start() !in 1..10 && it.start() !in 17..22 && it.start() !in 29..32) {
                        val x1 = poseLandmarkerResult.landmarks().get(0).get(it.start()).x() * imageWidth * scaleFactor
                        val y1 = poseLandmarkerResult.landmarks().get(0).get(it.start()).y() * imageHeight * scaleFactor
                        val x2 = poseLandmarkerResult.landmarks().get(0).get(it.end()).x() * imageWidth * scaleFactor
                        val y2 = poseLandmarkerResult.landmarks().get(0).get(it.end()).y() * imageHeight * scaleFactor

                        val slope = (y2 - y1) / (x2 - x1)

                        canvas.drawText(
                            "%.2f".format(slope),
                            (x1 + x2) / 2,
                            (y1 + y2)  / 2,
                            textPaint)
                        canvas.drawLine(
                            x1,
                            y1,
                            x2,
                            y2,
                            linePaint)
                    }
                }
                val ankleXAxis = (poseLandmarkerResult.landmarks().get(0).get(27).x() - poseLandmarkerResult.landmarks().get(0).get(28).x()) / 2
                val bodyParts = listOf("leftshoulder", "rightshoulder", "leftelbow", "rightelbow", "leftwrist", "rightwrist", "lefthip", "righthip", "leftknee", "rightknee", "leftankle", "rightankle")
                val bodyPartIndices = listOf(11, 12, 13, 14, 15 , 16, 23, 24, 25, 26, 27, 28)
                bodyParts.zip(bodyPartIndices).forEach { (bodyPart, index) ->
                    val distance = abs(poseLandmarkerResult.landmarks().get(0).get(index).x() - ankleXAxis)
                    val y = poseLandmarkerResult.landmarks().get(0).get(index).y() * imageHeight * scaleFactor

                    // 거리를 화면에 표시
                    canvas.drawText(
                        "%.2f".format(distance),
                        poseLandmarkerResult.landmarks().get(0).get(index).x() * imageWidth * scaleFactor,
                        y,
                        textPaint
                    )
                }
            }
        }
    }

    fun setResults(
        poseLandmarkerResults: PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = poseLandmarkerResults

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }
            RunningMode.LIVE_STREAM -> {
                // PreviewView is in FILL_START mode. So we need to scale up the
                // landmarks to match with the size that the captured images will be
                // displayed.
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }
        invalidate()
    }

}