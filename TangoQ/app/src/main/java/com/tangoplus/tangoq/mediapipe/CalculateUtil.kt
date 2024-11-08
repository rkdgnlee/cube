package com.tangoplus.tangoq.mediapipe

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

object CalculateUtil {

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
    // ------# 점과 점사이의 거리 #------
    fun calculateDistanceByDots(x1: Float, y1: Float, x2: Float, y2: Float) : Float{
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }

    // ------! 선과 점의 X 거리 !------
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


    private fun isTablet(context: Context): Boolean {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        val widthDp = metrics.widthPixels / metrics.density
        return widthDp >= 600
    }
}