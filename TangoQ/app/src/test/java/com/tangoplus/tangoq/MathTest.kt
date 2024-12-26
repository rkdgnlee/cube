package com.tangoplus.tangoq

import com.tangoplus.tangoq.mediapipe.MathHelpers
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.Math.toDegrees
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

class MathTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

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


    @Test
    fun get3DotsAngle() {

        // 앉아 후면 어깨 코 삼각형의 기울기 만들기
        val result1 = calculateAngle(641f, 300f, 590f, 362f, 699f, 365f)
        val result2 = calculateAngle(590f, 362f, 699f, 365f, 641f, 300f)
        val result3 = calculateAngle(699f, 365f, 641f, 300f, 590f, 362f)

// 앉아 후면 어깨 코 삼각형의 기울기 만들기
        val result4 = calculateAngle(366f, 507f, 266f, 609f, 452f, 612f)
        val result5 = calculateAngle(266f, 609f, 452f, 612f, 366f, 507f)
        val result6 = calculateAngle(452f, 612f, 366f, 507f, 266f, 609f)

        println("왼어깨, 52.13: $result1")
        println("오른어깨, 46.68: $result2")
        println("코, 81.18 :$result3")
        println("")
        println("왼어깨, 30.55: $result4")
        println("오른어깨, 34.07: $result5")
        println("코, 115.36 :$result6")
        assertEquals(90, 40 + 40) // 예상 결과를 넣어줌 (74는 예시)
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

    fun calculateSlope(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val radians = atan2(y2 - y1, x2 - x1)
        val degrees = toDegrees(radians.toDouble())
        return degrees.toFloat()
    }


    @Test
    fun backSitTriangleHip() {
        val results1 = calculateAngle(699f, 365f, 590f, 362f, (612 + 668) / 2f, (527 + 525) /2f)
        val results2 = calculateAngle(590f, 362f, (612 + 668) / 2f, (527 + 525) /2f, 699f, 365f)
        val results3 = calculateAngle((612 + 668) / 2f, (527 + 525) / 2f,699f, 365f, 590f, 362f)
        println("1030 left: $results1")
        println("1030 hip: $results2")
        println("1030 right: $results3")

        val results4 = calculateAngle(452f, 612f, 266f, 609f, (300 + 413) / 2f, (844 + 845) /2f)
        val results5 = calculateAngle(266f, 609f, (300 + 413) / 2f, (844 + 845) /2f , 452f, 612f,)
        val results6 = calculateAngle((300 + 413) / 2f, (844 + 845) /2f,452f, 612f, 266f, 609f,)
        println("1205 left: $results4")
        println("1205 hip: $results5")
        println("1205 right: $results6")
    }

    @Test
    fun backSitTriangleHip2() {
        val results1 = calculateAngle(0.5978397f,  0.3127404f, 0.3711682f, 0.29385933f, 0.4426213f, 0.46341354f)
        val results2 = calculateAngle(0.3711682f, 0.29385933f, 0.4426213f, 0.46341354f, 0.5978397f,  0.3127404f,)
        val results3 = calculateAngle(0.4426213f, 0.46341354f, 0.5978397f,  0.3127404f, 0.3711682f, 0.29385933f,)
        println("1030 left: $results1")
        println("1030 hip: $results2")
        println("1030 right: $results3")

        val results4 = calculateAngle(452f, 612f, 266f, 609f, (300 + 413) / 2f, (844 + 845) /2f)
        val results5 = calculateAngle(266f, 609f, (300 + 413) / 2f, (844 + 845) /2f , 452f, 612f,)
        val results6 = calculateAngle((300 + 413) / 2f, (844 + 845) /2f,452f, 612f, 266f, 609f,)
        println("1205 left: $results4")
        println("1205 hip: $results5")
        println("1205 right: $results6")

    }

    @Test
    fun ShoulderElbowWrist() {
//        val results1 = calculateAngle(0.5978397f,  0.3127404f, 0.3711682f, 0.29385933f, 0.4426213f, 0.46341354f)
//        val results2 = calculateAngle(0.3711682f, 0.29385933f, 0.4426213f, 0.46341354f, 0.5978397f,  0.3127404f,)
//        val results3 = calculateAngle(0.4426213f, 0.46341354f, 0.5978397f,  0.3127404f, 0.3711682f, 0.29385933f,)
//        println("1030 left: $results1")
//        println("1030 hip: $results2")
//        println("1030 right: $results3")

        val results4 = calculateAngle(380f, 386f, 360f, 536f, 333f, 675f)
//        val results5 = calculateAngle(266f, 609f, (300 + 413) / 2f, (844 + 845) /2f , 452f, 612f,)
//        val results6 = calculateAngle((300 + 413) / 2f, (844 + 845) /2f,452f, 612f, 266f, 609f,)
        println("1205 left: $results4")
//        println("1205 hip: $results5")
//        println("1205 right: $results6")
    }

    @Test
    fun noseShoulderAngle() {
        val result1 = calculateSlope(0.3907014f, 0.32702205f, 0.48875982f, 0.26673737f)
//        val result2 = calculateSlope(0.48875982f, 0.26673737f,0.3907014f, 0.32702205f)
//        val result3 = calculateSlope(0.48815367f, 0.3326003f,0.39255288f, 0.27046937f)
        val result4 = calculateSlope(0.39255288f, 0.27046937f, 0.48815367f, 0.3326003f)

        println("왼쪽 정상 ${result1}")
//        println("왼쪽 인자 바꿈 ${result2}")
//        println("오른쪽 정상 ${result3}")
        println("오른쪽 인자 바꿈 ${result4}")
    }

    @Test
    fun distanceTest() {
        val leftAnkle = Pair(603f, 647f)
        val rightAnkle = Pair(691f, 647f)
        val middleAxis = Pair((leftAnkle.first + rightAnkle.first) / 2, (leftAnkle.second + rightAnkle.second) / 2 )
        val backLeftKnee = Pair(610f, 531f)
        val backRightKnee = Pair(687f, 527f)
        val result1 = getRealDistanceX(backLeftKnee, middleAxis)
        val result2 = getRealDistanceX(backRightKnee, middleAxis)
        println("중심축: ${middleAxis}")
        println("후면 좌우무릎거리 : ($result1, $result2)")
        val leftShoulder = Pair(697f, 233f)
        val rightShoulder = Pair(579f, 229f)

        val frontLeftKnee = Pair(674f, 528f)
        val frontRightKnee = Pair(608f, 529f)

        val result3 = getRealDistanceY(leftShoulder, rightShoulder)
        val result4 = getRealDistanceY(frontLeftKnee, frontRightKnee)

        println("정면 어깨 높이 차 : $result3")
        println("정면 무릎 차 : $result4")


    }
    @Test
    fun normalizeDistanceTest() {
        val leftAnkle = Pair(0.5524446f, 0.77244884f)
        val rightAnkle = Pair(0.6713135f, 0.7785636f)
        val axisX = Pair((leftAnkle.first + rightAnkle.first) / 2, (leftAnkle.second + rightAnkle.second) / 2 )

        val leftKnee = Pair(0.5334982f, 0.6657309f)
        val rightKnee = Pair(0.6614827f, 0.67373633f)

        val leftKneeResult = getRealDistanceX(leftKnee, axisX)
        val rightKneeResult = getRealDistanceX(rightKnee, axisX)
        val leftAnkleResult = getRealDistanceX(leftAnkle, axisX)
        val rightAnkleResult = getRealDistanceX(rightAnkle, axisX)
        println("양 무릎 거리 (왼, 오): ($leftKneeResult, $rightKneeResult)")
        println("양 발목 거리 (왼, 오): ($leftAnkleResult, $rightAnkleResult)")
    }

    @Test
    fun normalizeDistanceTest2() {
        val leftAnkle = Pair(0.5556974f, 0.79296726f)
        val rightAnkle = Pair(0.45331466f, 0.7848047f)
        val axisX = Pair((leftAnkle.first + rightAnkle.first) / 2, (leftAnkle.second + rightAnkle.second) / 2 )

        val leftKnee = Pair(0.56929255f, 0.6615991f)
        val rightKnee = Pair(0.43928188f, 0.66419697f)

        val leftEar = Pair(0.5089976f, 0.22771695f)
        val rightEar =Pair(0.41771114f, 0.23255113f)
        val leftShoulder = Pair(0.5959488f, 0.3162927f)
        val rightShoulder =Pair(0.35642296f, 0.3219477f)
        val leftKneeResult = getRealDistanceX(leftKnee, axisX)
        val rightKneeResult = getRealDistanceX(rightKnee, axisX)
        val leftAnkleResult = getRealDistanceX(leftAnkle, axisX)
        val rightAnkleResult = getRealDistanceX(rightAnkle, axisX)

        val leftEarYResult = getRealDistanceY(leftEar, rightEar)
        val leftShoulderYResult = getRealDistanceY(leftShoulder, rightShoulder)
        println("축: $axisX")
        println("양 무릎 거리 (왼, 오): ($leftKneeResult, $rightKneeResult)")
        println("양 발목 거리 (왼, 오): ($leftAnkleResult, $rightAnkleResult)")
        println("귀 높이 차 : $leftEarYResult")
        println("어깨 높이 차 : $leftShoulderYResult")

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