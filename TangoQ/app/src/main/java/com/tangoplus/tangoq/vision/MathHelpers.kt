package com.tangoplus.tangoq.vision

import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import java.lang.Math.toDegrees
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

object MathHelpers {
    private const val SCALE_X = 0.25f
    private const val SCALE_Y = 0.3f

    // ------# 기울기 계산 #------
    fun calculateSlope(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val radians = atan2(y1 - y2, x1 - x2)
        val degrees = toDegrees(radians.toDouble()).toFloat()
        return  if (degrees > 180) degrees % 180 else degrees
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
        return toDegrees(angleRadians.toDouble()).toFloat()
    }

    // 전면-후면 카메라 판단을 위한 3점의 각도 판단
    fun determineDirection(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Boolean {
        // 벡터1: 첫 번째 점에서 두 번째 점으로의 벡터(어깨에서 팔꿈치)
        val v1x = x2 - x1
        val v1y = y2 - y1

        // 벡터2: 두 번째 점에서 세 번째 점으로의 벡터(팔꿈치에서 손목)
        val v2x = x3 - x2
        val v2y = y3 - y2

        // 2D 외적 계산 (z 성분만 계산)
        // 외적은 v1 × v2 = v1x * v2y - v1y * v2x
        val crossProduct = v1x * v2y - v1y * v2x

        // 외적 결과 해석
        // 양수: 두 번째 벡터는 첫 번째 벡터의 왼쪽에 위치 (왼쪽으로 펼쳐짐)
        // 음수: 두 번째 벡터는 첫 번째 벡터의 오른쪽에 위치 (오른쪽으로 펼쳐짐)
        return if (crossProduct > 0) true else false
    }


    // ------! 선과 점의 X 각도 !------
    fun calculateAngleBySlope(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Float {
        val x4 = (x1 + x2) / 2
        val y4 = (y1 + y2) / 2
        // 벡터1: (x1, y1) -> (x4, y4)
        val dx1 = x4 - x1
        val dy1 = y4 - y1
        // 벡터2: (x4, y4) -> (x3, y3)
        val dx2 = x3 - x4
        val dy2 = y3 - y4
        // 내적 계산
        val dotProduct = dx1 * dx2 + dy1 * dy2
        // 벡터 크기 계산
        val magnitude1 = sqrt(dx1.pow(2) + dy1.pow(2))
        val magnitude2 = sqrt(dx2.pow(2) + dy2.pow(2))
        // 코사인 값 계산
        val cosTheta = dotProduct / (magnitude1 * magnitude2)
        // 라디안 -> 도 변환
        val angleRadians = acos(cosTheta)
        return toDegrees(angleRadians.toDouble()).toFloat()
    }

    // 0~100점의 백분위로 점수 계산하기 (하나의 raw Data를)
    fun calculateBoundedScore(value: Float, range: Triple<Float, Float, Float>, columnName: String): Float {
        val midpoint = range.first
        val warningBoundary = abs(range.second)
        val criticalBoundary = abs(range.third)
        val boundaryMultiplier = 3f

        val warningMin = midpoint - warningBoundary
        val warningMax = midpoint + warningBoundary
        val criticalMin = midpoint - criticalBoundary
        val criticalMax = midpoint + criticalBoundary
        val result = when {
            value in warningMin..warningMax -> {
                val diff = abs(value - midpoint)
                // 73 ~ 100점
                73f + ((warningBoundary - diff) / warningBoundary) * 27f
            }
            value in criticalMin .. warningMin || value in warningMax..criticalMax -> {
                val diff = if (value < warningMin) warningMin - value else value - warningMax
                val maxDiff = criticalBoundary - warningBoundary
                // 51 ~ 73점
                51f + ((maxDiff - diff) / maxDiff) * 22f
            }
            value < criticalMin -> {
                val diff = criticalMin - value
                val maxDiff = criticalBoundary * boundaryMultiplier
                // 30 ~ 51점
                maxOf(20f, 51f - (diff / maxDiff) * 21f)
            }
            value > criticalMax -> {
                val diff = value - criticalMax
                val maxDiff = criticalBoundary * boundaryMultiplier
                // 30 ~ 51점
                maxOf(20f, 51f - (diff / maxDiff) * 21f)
            }
            else -> 20f
        }
        return if ("horizontal_distance" in columnName) {
            result * 0.90f // 비중을 85%로 줄임
        } else {
            result
        }
    }

    fun normalizeAngle90(angle: Float): Float {
        val absAngle = abs(angle % 180)
        return if (absAngle > 90) 180 - absAngle else absAngle
    }
    private fun getDistanceX(point1: Pair<Float, Float>, point2: Pair<Float, Float>): Float {
        return abs(point2.first - point1.first)
    }

    // Y축 거리 계산
    private fun getDistanceY(point1: Pair<Float, Float>, point2: Pair<Float, Float>): Float {
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