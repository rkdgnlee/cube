package com.tangoplus.tangoq.mediapipe

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.pow
import kotlin.math.sqrt

object MathHelpers {
    private const val SCALE_X = 16.67f // 클수록
    private const val SCALE_Y = 26.67f

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
    fun calculateAngleBySlope(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Float {
        val slope1 = (y2 - y1) / (x2 - x1) // 어깨 선의 기울기
        val slope2 = (y3 - y1) / (x3 - x1) // 골반 중심의 기울기

        val angleRad = atan((slope2 - slope1) / (1 + slope1 * slope2))
        return angleRad * (180f / PI.toFloat())
    }

    // 0~100점의 백분위로 점수 계산하기 (하나의 raw Data를)
    fun calculateBoundedScore(value: Float, range: Triple<Float, Float, Float>): Float {
        val midpoint = range.first
        val warningBoundary = range.second
        val criticalBoundary = range.third
        val warningMin = midpoint - warningBoundary
        val warningMax = midpoint + warningBoundary
        val criticalMin = midpoint - criticalBoundary
        val criticalMax = midpoint + criticalBoundary
        val boundaryMultiplier = 3f // 범위를 벗어난 3배 거리에서 0점

        return when {
            value in criticalMin..criticalMax -> {
                // 경고 경계 내 점수 계산
                if (value in warningMin..warningMax) {
                    // 주의 경계 내 점수 계산 (0~100)
                    val halfWarningRange = warningBoundary
                    val centeredValue = value - midpoint
                    val percentage = ((halfWarningRange - kotlin.math.abs(centeredValue)) / halfWarningRange) * 100f
                    percentage
                } else {
                    // 경고 범위 (주의 범위 밖)에서 점수 감소 (0~50)
                    val halfCriticalRange = criticalBoundary - warningBoundary
                    val diff = if (value < warningMin) warningMin - value else value - warningMax
                    maxOf(0f, 50f - (diff / halfCriticalRange) * 50f)
                }
            }
            value < criticalMin -> {
                // 경고 경계보다 작은 값에 대해 점수 감소
                val diff = criticalMin - value
                maxOf(0f, 50f - (diff / (criticalBoundary * boundaryMultiplier)) * 50f)
            }
            value > criticalMax -> {
                // 경고 경계보다 큰 값에 대해 점수 감소
                val diff = value - criticalMax
                maxOf(0f, 50f - (diff / (criticalBoundary * boundaryMultiplier)) * 50f)
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