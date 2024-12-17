package com.tangoplus.tangoq

import com.tangoplus.tangoq.function.MeasurementManager.status
import org.junit.Test

import org.junit.Assert.*
import java.lang.Math.toDegrees
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class CalculateOverallTest {


    @Test
    fun testSlope() {
        val result = calculateAngleBySlope(259f, 567f, 405f, 557f, (303+390)/ 2f, (756+752)/2f)
        assertEquals(90, result) // 예상 결과를 넣어줌 (74는 예시)
    }

    @Test
    fun testSlope2() {
        val result = calculateAngleBySlope(590f, 362f, 699f, 365f, (612+668)/ 2f, (527+525)/2f)
        assertEquals(90, result) // 예상 결과를 넣어줌 (74는 예시)
    }

    @Test
    fun noseCenterShoulderSlope() {
        val result = calculateAngleBySlope(262f, 395f, 460f, 402f, 372f, 284f)
        println(result)
        assertEquals(90, result) // 예상 결과를 넣어줌 (74는 예시)
    }



fun calculateAngleBySlope(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Float {
    // 중간값 계산
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
    return Math.toDegrees(angleRadians.toDouble()).toFloat()
}

    @Test
    fun testSlope3() {
        val result = calculateSlope(689f, 278f, 592f, 276f)
        assertEquals(-178, result) // 예상 결과를 넣어줌 (74는 예시)
    }

    // 더미 데이터 왼어깨-코 각도 (거북목)
    @Test
    fun testSlope4() {
        val result = calculateAngleBySlope(285f, 428f, 461f, 427f, (329f + 421f)/2, (690f+689f)/2)
        assertEquals(52.519786, abs(result) ) // 예상 결과를 넣어줌 (74는 예시)
    }
    // 현재 146도 가 나옴 90을 빼면 약 56도 정도가 나오는데

//    // 실제 이진영씨 왼어깨-코 각도 (거북목)
//    @Test
//    fun testSlope5() {
//        val result = calculateAngleBySlope(616f, 217f, 634f, 265f)
//        assertEquals(69.4439545, result) // 예상 결과를 넣어줌 (74는 예시)
//    }


    // 더미 데이터 back_vertical_angle_nose_center_shoulder 코-어깨 중심 각도 (거북목)
    @Test
    fun testSlope6() {
        val result = calculateSlope(315f, 304f, (233f + 390f) / 2, (392f + 390f) / 2)
        assertEquals(92.30376, abs(result) ) // 예상 결과를 넣어줌 (74는 예시)
    }
    // 현재 146도 가 나옴 90을 빼면 약 56도 정도가 나오는데

    // 실제 이진영씨 back_vertical_angle_nose_center_shoulder 코-어깨 중심 각도 (거북목)
    @Test
    fun testSlope7() {
        val result = calculateSlope(648f, 212f, (597f + 696f) / 2, (268f + 270f) / 2)
        assertEquals(91.48146, result) // 예상 결과를 넣어줌 (74는 예시)
    }

    fun calculateSlope(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val radians = atan2(y2 - y1, x2 - x1)
        val degrees = toDegrees(radians.toDouble())
        return degrees.toFloat()
    }

    @Test
    fun testCalculateOverall() {
        // Arrange: 테스트 입력 값 준비
        val parts = mutableListOf(
            "목관절" to status.WARNING,
            "좌측 어깨" to status.WARNING,
            "우측 어깨" to status.WARNING,

            "좌측 팔꿉" to status.NORMAL,
            "우측 팔꿉" to status.DANGER,

            "좌측 손목" to status.NORMAL,
            "우측 손목" to status.NORMAL,

            "좌측 골반" to status.WARNING,
            "우측 골반" to status.NORMAL,
            "좌측 무릎" to status.NORMAL,
            "우측 무릎" to status.WARNING,
            "우측 발목" to status.NORMAL,
            "우측 발목" to status.NORMAL
        )
        val parts2 = mutableListOf(
            "목관절" to status.WARNING,
            "좌측 어깨" to status.WARNING,
            "우측 어깨" to status.NORMAL,

            "좌측 팔꿉" to status.NORMAL,
            "우측 팔꿉" to status.NORMAL,

            "좌측 손목" to status.NORMAL,
            "우측 손목" to status.NORMAL,

            "좌측 골반" to status.WARNING,
            "우측 골반" to status.WARNING,
            "좌측 무릎" to status.NORMAL,
            "우측 무릎" to status.WARNING,
            "우측 발목" to status.NORMAL,
            "우측 발목" to status.NORMAL
        )
        val parts3= mutableListOf(
            "목관절" to status.WARNING,
            "좌측 어깨" to status.DANGER,
            "우측 어깨" to status.NORMAL,

            "좌측 팔꿉" to status.NORMAL,
            "우측 팔꿉" to status.NORMAL,

            "좌측 손목" to status.NORMAL,
            "우측 손목" to status.NORMAL,

            "좌측 골반" to status.WARNING,
            "우측 골반" to status.WARNING,
            "좌측 무릎" to status.WARNING,
            "우측 무릎" to status.WARNING,
            "우측 발목" to status.NORMAL,
            "우측 발목" to status.NORMAL
        )
        val parts4= mutableListOf(
            "목관절" to status.WARNING,
            "좌측 어깨" to status.WARNING,
            "우측 어깨" to status.WARNING,

            "좌측 팔꿉" to status.WARNING,
            "우측 팔꿉" to status.NORMAL,

            "좌측 손목" to status.NORMAL,
            "우측 손목" to status.NORMAL,

            "좌측 골반" to status.WARNING,
            "우측 골반" to status.WARNING,
            "좌측 무릎" to status.NORMAL,
            "우측 무릎" to status.DANGER,
            "우측 발목" to status.NORMAL,
            "우측 발목" to status.NORMAL
        )
        val parts5= mutableListOf(
            "목관절" to status.WARNING,
            "좌측 어깨" to status.WARNING,
            "우측 어깨" to status.DANGER,

            "좌측 팔꿉" to status.NORMAL,
            "우측 팔꿉" to status.NORMAL,

            "좌측 손목" to status.NORMAL,
            "우측 손목" to status.NORMAL,

            "좌측 골반" to status.DANGER,
            "우측 골반" to status.WARNING,
            "좌측 무릎" to status.WARNING,
            "우측 무릎" to status.WARNING,
            "우측 발목" to status.NORMAL,
            "우측 발목" to status.WARNING
        )

        // Act: 함수 호출
        val result = calculateOverall(parts)
        val result2 = calculateOverall(parts2)
        val result3 = calculateOverall(parts3)
        val result4 = calculateOverall(parts4)
        val result5 = calculateOverall(parts5)
        println("82 - $result")
        println("83 - $result2")
        println("76 - $result3")
        println("69 - $result4")
        println("65 - $result5")
        // Assert: 예상 결과와 비교
        assertEquals(82, result)
    }

    fun calculateOverall(parts: MutableList<Pair<String, status>>) : Int {
        val scores = mapOf(
            status.DANGER to 25,
            status.WARNING to 65,
            status.NORMAL to 90
        )
        val weightScore = 2.6
        val reverseWeightScore = 0.8
        var weightedScoreSum = 0.0
        var totalWeight = 0.0
        for (part in parts) {
            val (bodyPart, status) = part
            val weight = when {
                bodyPart.contains("목관절") -> weightScore
//                bodyPart.contains("어깨") -> weightScore
                bodyPart.contains("골반") -> weightScore
                bodyPart.contains("팔꿉") -> reverseWeightScore
                bodyPart.contains("손목") -> weightScore
                bodyPart.contains("무릎") -> weightScore
//                bodyPart.contains("발목") -> reverseWeightScore
                else -> 1.0
            }
            weightedScoreSum += (scores[status] ?: 0) * weight
            totalWeight += weight
        }
        return if (totalWeight > 0) (weightedScoreSum / totalWeight).toInt() else 0
    }
}