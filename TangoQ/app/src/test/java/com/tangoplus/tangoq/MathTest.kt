package com.tangoplus.tangoq

import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.Math.toDegrees
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
}