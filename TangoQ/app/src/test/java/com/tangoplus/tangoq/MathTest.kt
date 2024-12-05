package com.tangoplus.tangoq

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.acos
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
}