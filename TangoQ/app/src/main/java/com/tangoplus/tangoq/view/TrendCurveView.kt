package com.tangoplus.tangoq.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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
        color = ContextCompat.getColor(context, R.color.thirdColor)
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }
    private val path = Path()

    private var points = listOf<Pair<Float, Float>>()
    private var resampledPoints = listOf<Pair<Float, Float>>()
    private var margin = 20f // 기본 마진 값
    private var boundingBox = RectF()
    private val resampleSize = 20

    fun setPoints(newPoints: List<Pair<Float, Float>>, newMargin: Float = 15f) {
        points = newPoints
        margin = newMargin
        calculateBoundingBox()
        resamplePoints()
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

        if (resampledPoints.isEmpty()) return

        path.reset()

        val scaledPoints = scalePoints(resampledPoints)

        path.moveTo(scaledPoints[0].first, scaledPoints[0].second)

        for (i in 1 until scaledPoints.size) {
            val p0 = if (i > 1) scaledPoints[i - 2] else scaledPoints[0]
            val p1 = scaledPoints[i - 1]
            val p2 = scaledPoints[i]
            val p3 = if (i < scaledPoints.size - 1) scaledPoints[i + 1] else p2

            // 제어점 계산
            val controlPoint1 = Pair(
                p1.first + (p2.first - p0.first) / 4,
                p1.second + (p2.second - p0.second) / 4
            )
            val controlPoint2 = Pair(
                p2.first - (p3.first - p1.first) / 4,
                p2.second - (p3.second - p1.second) / 4
            )

            path.cubicTo(
                controlPoint1.first, controlPoint1.second,
                controlPoint2.first, controlPoint2.second,
                p2.first, p2.second
            )
        }

        canvas.drawPath(path, paint)
    }

    private fun scalePoints(points: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        val availableWidth = width - 2 * margin
        val availableHeight = height - 2 * margin

        val boxWidth = boundingBox.width()
        val boxHeight = boundingBox.height()

        val scale = minOf(availableWidth / boxWidth, availableHeight / boxHeight)

        val offsetX = (width - boxWidth * scale) / 2
        val offsetY = (height - boxHeight * scale) / 2

        return points.map { (x, y) ->
            Pair(
                offsetX + (x - boundingBox.left) * scale,
                height - (offsetY + (y - boundingBox.top) * scale)
            )
        }
    }
}