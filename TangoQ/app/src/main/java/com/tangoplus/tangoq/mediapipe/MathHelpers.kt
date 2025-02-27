package com.tangoplus.tangoq.mediapipe

import android.content.Context
import android.util.DisplayMetrics
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
    fun calculateBoundedScore(columnName: String, value: Float, range: Triple<Float, Float, Float>): Float {
        val midpoint = range.first
        val warningBoundary = abs(range.second) // 항상 절대값으로 처리
        val criticalBoundary = abs(range.third) // 항상 절대값으로 처리
        val boundaryMultiplier = 3f // 범위를 벗어난 3배 거리에서 0점

        val isBackHorizontalAngle = columnName.contains("back_horizontal_angle")

        val warningMin = midpoint - warningBoundary
        val warningMax = midpoint + warningBoundary
        val criticalMin = midpoint - criticalBoundary
        val criticalMax = midpoint + criticalBoundary

        return when {
            isBackHorizontalAngle -> {
                if (range.second < 0 || range.third < 0) {
                    // 음수 방향만 검사
                    if (value >= midpoint) 100f // 양수 값은 무조건 100점
                    else calculateNegativeScore(value, midpoint, warningBoundary, criticalBoundary, boundaryMultiplier)
                } else {
                    // 양수 방향만 검사
                    if (value <= midpoint) 100f // 음수 값은 무조건 100점
                    else calculatePositiveScore(value, midpoint, warningBoundary, criticalBoundary, boundaryMultiplier)
                }
            }
            else -> {
                // 절대값으로 양쪽 검사
                when {
                    value in warningMin..warningMax -> {
                        val diff = abs(value - midpoint)
                        66f + ((warningBoundary - diff) / warningBoundary) * 34f
                    }
                    value in criticalMin .. warningMin || value in warningMax..criticalMax -> {
                        val diff = if (value < warningMin) warningMin - value else value - warningMax
                        val maxDiff = criticalBoundary - warningBoundary
                        33f + ((maxDiff - diff) / maxDiff) * 33f
                    }
                    value < criticalMin -> {
                        val diff = criticalMin - value
                        val maxDiff = criticalBoundary * boundaryMultiplier
                        maxOf(0f, 33f - (diff / maxDiff) * 33f)
                    }
                    value > criticalMax -> {
                        val diff = value - criticalMax
                        val maxDiff = criticalBoundary * boundaryMultiplier
                        maxOf(0f, 33f - (diff / maxDiff) * 33f)
                    }
                    else -> 0f
                }
            }
        }
    }

    fun calculateNegativeScore(value: Float, midpoint: Float, warningBoundary: Float, criticalBoundary: Float, boundaryMultiplier: Float): Float {
        val warningMin = midpoint - warningBoundary
        val criticalMin = midpoint - criticalBoundary

        return when {
            value in criticalMin..warningMin -> {
                val diff = warningMin - value
                val maxDiff = criticalBoundary - warningBoundary
                33f + ((maxDiff - diff) / maxDiff) * 33f
            }
            value < criticalMin -> {
                val diff = criticalMin - value
                val maxDiff = criticalBoundary * boundaryMultiplier
                maxOf(0f, 33f - (diff / maxDiff) * 33f)
            }
            else -> 66f // warningMin 이상이면 66점
        }
    }

    fun calculatePositiveScore(value: Float, midpoint: Float, warningBoundary: Float, criticalBoundary: Float, boundaryMultiplier: Float): Float {
        val warningMax = midpoint + warningBoundary
        val criticalMax = midpoint + criticalBoundary

        return when {
            value in warningMax..criticalMax -> {
                val diff = value - warningMax
                val maxDiff = criticalBoundary - warningBoundary
                33f + ((maxDiff - diff) / maxDiff) * 33f
            }
            value > criticalMax -> {
                val diff = value - criticalMax
                val maxDiff = criticalBoundary * boundaryMultiplier
                maxOf(0f, 33f - (diff / maxDiff) * 33f)
            }
            else -> 66f // warningMax 이하이면 66점
        }
    }
    // ------# 점과 점사이의 거리 #------


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

    fun phoneNumber82(msg: String) : String {
        val firstNumber: String = msg.substring(0,3)

        var phoneEdit = msg.substring(3)
        when (firstNumber) {
            "010" -> phoneEdit = "+82 10$phoneEdit"
            "011" -> phoneEdit = "+82 11$phoneEdit"
            "016" -> phoneEdit = "+82 16$phoneEdit"
            "017" -> phoneEdit = "+82 17$phoneEdit"
            "018" -> phoneEdit = "+82 18$phoneEdit"
            "019" -> phoneEdit = "+82 19$phoneEdit"
        }
        return phoneEdit
    }
}