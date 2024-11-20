package com.tangoplus.tangoq.mediapipe

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

object MathHelpers {
    private const val SCALE_X = 50f
    private const val SCALE_Y = 80f

    // ------# 기울기 계산 #------
    fun calculateSlope(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return (y2 - y1) / (x2 - x1)
    }
    // ------# 점 3개의 각도 계산 #------
    fun calculateAngle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3:Float): Float {
        val v1x = x1 - x2
        val v1y = y1 - y2
        val v2x = x3 - x2
        val v2y = y3 - y2
        val dotProduct = v1x * v2x + v1y * v2y
        val magnitude1 = sqrt(v1x * v1x + v1y * v1y)
        val magnitude2 = sqrt(v2x * v2x + v2y * v2y)

        val cosTheta = dotProduct / (magnitude1 * magnitude2)
        val angleRadians = acos(cosTheta)
        return Math.toDegrees(angleRadians.toDouble()).toFloat()
    }

    // ------! 선과 점의 X 각도 !------
    fun calculateAngleByLine(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) : Float {
        val vector1X = x2 - x1
        val vector1Y = y2 - y1
        val vector2X = x3 - x1
        val vector2Y = y3 - y1
        val dotProduct = vector1X * vector2X + vector1Y * vector2Y
        val magnitude1 = sqrt(vector1X.pow(2) + vector1Y.pow(2))
        val magnitude2 = sqrt(vector2X.pow(2) + vector2Y.pow(2))

        val cosTheta = dotProduct / (magnitude1 * magnitude2)
        val angleRad = acos(cosTheta.coerceIn(-1.0F, 1.0F))

        return angleRad * (180f / PI.toFloat())
    }

    fun calculateBoundedScore(value: Float, range: Pair<Float, Float>): Float {
        val min = range.first
        val max = range.second
        val midpoint = (min + max) / 2
        val halfRange = (max - min) / 2
        val boundaryMultiplier = 3f // 범위를 벗어난 3배 거리에서 0점

        return when {
            value in min..max -> {
                // 범위 내 점수 계산 (0~100)
                val centeredValue = value - midpoint
                val percentage = ((halfRange - kotlin.math.abs(centeredValue)) / halfRange) * 100f
                percentage
            }
            value < min -> {
                // 범위보다 작은 값에 대해 점수 감소
                val diff = min - value
                maxOf(0f, 50f - (diff / (halfRange * boundaryMultiplier)) * 50f)
            }
            value > max -> {
                // 범위보다 큰 값에 대해 점수 감소
                val diff = value - max
                maxOf(0f, 50f - (diff / (halfRange * boundaryMultiplier)) * 50f)
            }
            else -> 0f
        }
    }
    // ------# 점과 점사이의 거리 #------


    fun getDistanceX(point1: Pair<Float, Float>, point2: Pair<Float, Float>): Float {
        return abs(point2.first - point1.first)
    }

    // Y축 거리 계산
    fun getDistanceY(point1: Pair<Float, Float>, point2: Pair<Float, Float>): Float {
        return abs(point2.second - point1.second)
    }

    fun getRealDistanceX(point1: Pair<Float, Float>, point2: Pair<Float, Float>) : Float {
        val normalizedDistance = getDistanceX(point1, point2)
        return normalizedToRealDistance(normalizedDistance,true)
    }

    fun getRealDistanceY(point1: Pair<Float, Float>, point2: Pair<Float, Float>) : Float {
        val normalizedDistance = getDistanceY(point1, point2)
        return normalizedToRealDistance(normalizedDistance,  false)
    }

    private fun normalizedToRealDistance(
        normalizedDistance: Float,
        isXAxis: Boolean = true
    ): Float {
        return if (isXAxis) {
            normalizedDistance * SCALE_X
        } else {
            normalizedDistance * SCALE_Y
        }
    }

    fun isTablet(context: Context): Boolean {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        val widthDp = metrics.widthPixels / metrics.density
        return widthDp >= 600
    }
}