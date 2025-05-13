package com.tangoplus.tangoq.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.tangoplus.tangoq.R

class TrendCurveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        strokeWidth = 4.5f
        style = Paint.Style.STROKE
    }

    private var points = listOf<Pair<Float, Float>>()
    private var resampledPoints = listOf<Pair<Float, Float>>()
    private var margin = 10f // 기본 마진 값
    private var boundingBox = RectF()
    private val resampleSize = 35
    private var isFrontCamera = false // 좌우 반전 여부

    fun setPoints(newPoints: List<Pair<Float, Float>>, newMargin: Float = 15f) {
        points = newPoints
        margin = newMargin
        calculateBoundingBox()
        resamplePoints()
        invalidate()
    }
    fun setMirrored(mirrored: Boolean) {
        isFrontCamera = mirrored
        invalidate()
    }
    private fun calculateBoundingBox() {
        val minX = points.minOf { it.first }
        val maxX = points.maxOf { it.first }
        val minY = points.minOf { it.second }
        val maxY = points.maxOf { it.second }
        boundingBox.set(minX, minY, maxX, maxY)
    }

    private fun resamplePoints() {
        val chunkedPoints = points.chunked(points.size / resampleSize)
        resampledPoints = chunkedPoints.map { chunk ->
            val avgX = chunk.map { it.first }.average().toFloat()
            val avgY = chunk.map { it.second }.average().toFloat()
            Pair(avgX, avgY)
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (points.isEmpty()) return

        // 전면 카메라일경우 그대로, 후면카메라일 경우 반대로
        if (isFrontCamera) {
            canvas.scale(1f, 1f, width / 2f, height / 2f)

        } else {
            canvas.scale(-1f, 1f, width / 2f, height / 2f)
        }
        val scaledPoints = scalePoints(points, flipHorizontal = true, flipVertical = true)

        // 경로 길이 계산
        val segmentLengths = mutableListOf<Float>()
        var totalLength = 0f
        for (i in 1 until scaledPoints.size) {
            val dx = scaledPoints[i].first - scaledPoints[i - 1].first
            val dy = scaledPoints[i].second - scaledPoints[i - 1].second
            val segmentLength = kotlin.math.sqrt(dx * dx + dy * dy)
            segmentLengths.add(segmentLength)
            totalLength += segmentLength
        }

        // 각 선분의 상대 위치 계산
        var accumulatedLength = 0f
        for (i in 1 until scaledPoints.size) {
            val startX = scaledPoints[i - 1].first
            val startY = scaledPoints[i - 1].second
            val endX = scaledPoints[i].first
            val endY = scaledPoints[i].second

            // 시작과 끝의 색상 비율 계산
            val startRatio = accumulatedLength / totalLength
            val endRatio = (accumulatedLength + segmentLengths[i - 1]) / totalLength

            // 색상 계산 (그라데이션 범위 )
            val startColor = blendColorsWithThreshold(
                ContextCompat.getColor(context, R.color.thirdColor),
                ContextCompat.getColor(context, R.color.deleteColor),
                startRatio,
                0.3f,
                0.45f
            )
            val endColor = blendColorsWithThreshold(
                ContextCompat.getColor(context, R.color.thirdColor),
                ContextCompat.getColor(context, R.color.deleteColor),
                endRatio,
                0.3f,
                0.45f
            )

            // LinearGradient를 선분에 적용
            val shader = LinearGradient(
                startX, startY, endX, endY,
                startColor, endColor,
                Shader.TileMode.CLAMP
            )

            paint.shader = shader
            canvas.drawLine(startX, startY, endX, endY, paint)

            accumulatedLength += segmentLengths[i - 1]
        }
    }
    private fun blendColorsWithThreshold(
        color1: Int,
        color2: Int,
        ratio: Float,
        startThreshold: Float,
        endThreshold: Float
    ): Int {
        return when {
            ratio <= startThreshold -> color1 // startThreshold 이전은 첫 번째 색상
            ratio >= endThreshold -> color2  // endThreshold 이후는 두 번째 색상
            else -> { // startThreshold ~ endThreshold 사이에서 색상 혼합
                val adjustedRatio = (ratio - startThreshold) / (endThreshold - startThreshold)
                blendColors(color1, color2, adjustedRatio)
            }
        }
    }
    private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val inverseRatio = 1 - ratio
        val a = (Color.alpha(color1) * inverseRatio + Color.alpha(color2) * ratio).toInt()
        val r = (Color.red(color1) * inverseRatio + Color.red(color2) * ratio).toInt()
        val g = (Color.green(color1) * inverseRatio + Color.green(color2) * ratio).toInt()
        val b = (Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio).toInt()
        return Color.argb(a, r, g, b)
    }

    private fun scalePoints(
        points: List<Pair<Float, Float>>,
        flipHorizontal: Boolean = false, // 좌우 반전 여부
        flipVertical: Boolean = false    // 상하 반전 여부
    ): List<Pair<Float, Float>> {
        val availableWidth = width - 2 * margin
        val availableHeight = height - 2 * margin

        val boxWidth = boundingBox.width()
        val boxHeight = boundingBox.height()

        val scale = minOf(availableWidth / boxWidth, availableHeight / boxHeight)

        val offsetX = (width - boxWidth * scale) / 2
        val offsetY = (height - boxHeight * scale) / 2

        return points.map { (x, y) ->
            val scaledX = offsetX + (x - boundingBox.left) * scale
            val scaledY = height - (offsetY + (y - boundingBox.top) * scale)

            // 좌우 반전 처리
            val flippedX = if (flipHorizontal) width - scaledX else scaledX
            // 상하 반전 처리
            val flippedY = if (flipVertical) height - scaledY else scaledY

            Pair(flippedX, flippedY)
        }
    }
}