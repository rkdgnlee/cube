package com.tangoplus.tangoq

import com.tangoplus.tangoq.function.MeasurementManager.status
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class CalculateOverallTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }


    @Test
    fun testCalculateOverall() {
        // Arrange: 테스트 입력 값 준비
        val parts = mutableListOf(
            "목관절" to status.DANGER,
            "좌측 어깨" to status.NORMAL,
            "우측 어깨" to status.NORMAL,

            "좌측 팔꿉" to status.DANGER,
            "우측 팔꿉" to status.DANGER,

            "좌측 손목" to status.WARNING,
            "우측 손목" to status.DANGER,

            "좌측 골반" to status.DANGER,
            "우측 골반" to status.DANGER,
            "좌측 무릎" to status.DANGER,
            "우측 무릎" to status.NORMAL,
            "우측 발목" to status.NORMAL,
            "우측 발목" to status.NORMAL
        )

        // Act: 함수 호출
        val result = calculateOverall(parts)

        // Assert: 예상 결과와 비교
        assertEquals(74, result) // 예상 결과를 넣어줌 (74는 예시)
    }

    fun calculateOverall(parts: MutableList<Pair<String, status>>) : Int {
        val scores = mapOf(
            status.DANGER to 30,
            status.WARNING to 65,
            status.NORMAL to 100
        )
        val weightScore = 1.05
        val reverseWeightScore = 0.95
        var weightedScoreSum = 0.0
        var totalWeight = 0.0
        for (part in parts) {
            val (bodyPart, status) = part
            val weight = when {
                bodyPart.contains("어깨") -> weightScore
                bodyPart.contains("골반") -> weightScore
                bodyPart.contains("손목") -> reverseWeightScore
                bodyPart.contains("발목") -> reverseWeightScore
                else -> 1.0
            }
            weightedScoreSum += (scores[status] ?: 0) * weight
            totalWeight += weight
        }
        return if (totalWeight > 0) (weightedScoreSum / totalWeight).toInt() else 0
    }
}