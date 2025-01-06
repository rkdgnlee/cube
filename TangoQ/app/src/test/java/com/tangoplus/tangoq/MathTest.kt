package com.tangoplus.tangoq

import com.tangoplus.tangoq.mediapipe.MathHelpers.normalizeToZero
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.Math.toDegrees
import kotlin.math.abs
import kotlin.math.atan2

class MathTest {

    @Test
    fun getBack0Angle() {
        val result1 = calculateSlope0(0.43276635f, 0.28003475f, 0.5174259f, 0.27613425f)
        print("귀: $result1")
        val result2 = calculateSlope0(0.37508744f, 0.3669743f, 0.6012284f, 0.3628323f)
        print("어깨: $result2")
    }



    @Test
    fun getAngle() {

        // 앉아 후면 어깨 코 삼각형의 기울기 만들기
        val result1 = calculateSlope( 316f, 317f, 380f, 386f,)
        val result2 = calculateSlope(616f, 217f, 634f, 265f)
        val result3 = calculateSlope( 297f, 332f, 349f, 416f,)
        println("1205, 52.13: $result1")
        println("1030, 69.44: $result2")
        println("1204, 69.44: $result3")
        assertEquals(90, 40 + 40) // 예상 결과를 넣어줌 (74는 예시)
    }

    // ------# 기울기 계산 #------
    fun calculateSlope(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val radians = atan2(y2 - y1, x2 - x1)
        val degrees = toDegrees(radians.toDouble())
        return degrees.toFloat() // 0도 기준으로 정규화
    }

    fun calculateSlope0(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val radians = atan2(y2 - y1, x2 - x1)
        val degrees = toDegrees(radians.toDouble())
        return normalizeToZero(degrees.toFloat()) // 0도 기준으로 정규화
    }
    // ------# 수평 초기화 기울기 계산 #------
    fun calculateSlope180(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val radians = atan2(y2 - y1, x2 - x1)
        val degrees = toDegrees(radians.toDouble())
        return normalizeTo180(degrees.toFloat()) // 180도 기준으로 정규화
    }

    private fun normalizeTo180(angle: Float): Float {
        var normalized = angle
        while (normalized < 0) normalized += 360
        while (normalized >= 360) normalized -= 360

        if (normalized > 180) {
            return normalized
        } else {
            return abs(180 - normalized)
        }
    }

    @Test
    fun getKneeDistance() {
//        val resultLeft =
//            getRealDistanceX(Pair(0.44432667f, 0.69172835f), Pair(0.50995743f, 0.8116318f))
//        val resultRight =
//            getRealDistanceX(Pair(0.5788526f, 0.696052f), Pair(0.50995743f, 0.8116318f))
//        print("left: $resultLeft, right: $resultRight")
//
//
//        val frontWristAngle = calculateSlope(0.4431353f, 0.24797726f, 0.5396624f, 0.2456446f)
//        val frontWristAngle0 = calculateSlope0(0.4431353f, 0.24797726f, 0.5396624f, 0.2456446f)
//        val frontWristAngle180 = calculateSlope180(0.4431353f, 0.24797726f, 0.5396624f, 0.2456446f)
//        print(" 원시: $frontWristAngle, 0: $frontWristAngle0, 180: ${frontWristAngle180 % 180}")
//
//        val seq2wristDistanceLeft = getRealDistanceX(Pair(0.34403014f, 0.33611208f), Pair(0.53042406f, 0.81937426f))
//        val seq2wristDistanceRight = getRealDistanceX( Pair(0.6469915f, 0.3353093f),Pair(0.53042406f, 0.81937426f))
//        println("왼쪽손목거리: $seq2wristDistanceLeft, 오른쪽손목거리: $seq2wristDistanceRight")

        val backShoulderAngle = calculateSlope(0.6290523f, 0.34049463f,0.37341383f, 0.34746212f )
        val backShoulderAngle0 = calculateSlope0(0.6290523f, 0.34049463f,0.37341383f, 0.34746212f )
        val backShoulderAngle180 = calculateSlope180(0.6290523f, 0.34049463f,0.37341383f, 0.34746212f )

        val backKneeAngle = calculateSlope(0.5895234f, 0.69777566f,0.4451991f, 0.70162326f )
        val backKneeAngle0 = calculateSlope0(0.5895234f, 0.69777566f,0.4451991f, 0.70162326f )
        val backKneeAngle180 = calculateSlope180(0.5895234f, 0.69777566f,0.4451991f, 0.70162326f )
        println("어깨 원시 ${backShoulderAngle}, 0: ${backShoulderAngle0}, 180: ${backShoulderAngle180}")
        println("무릎 원시 ${backKneeAngle}, 0: ${backKneeAngle0}, 180: ${backKneeAngle180}")
    }

    private val SCALE_X = 180f
    private val SCALE_Y = 160f
    fun getDistanceX(point1: Pair<Float, Float>, point2: Pair<Float, Float>): Float {
        return abs(point2.first - point1.first)
    }
    fun getDistanceY(point1: Pair<Float, Float>, point2: Pair<Float, Float>): Float {
        return abs(point2.second - point1.second)
    }

    fun getRealDistanceX(point1: Pair<Float, Float>, point2: Pair<Float, Float>) : Float {
        val normalizedDistance = getDistanceX(point1, point2)
        return normalizedToRealDistance(normalizedDistance, true)
    }
    fun getRealDistanceY(point1: Pair<Float, Float>, point2: Pair<Float, Float>) : Float {
        val normalizedDistance = getDistanceY(point1, point2)
        return normalizedToRealDistance(normalizedDistance, false)
    }
    // 정규화 좌표면 곱하기, 스케일링 좌표면 나누기
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

}