package com.tangoplus.tangoq

import com.tangoplus.tangoq.function.MeasurementManager
import com.tangoplus.tangoq.function.MeasurementManager.Status
import org.junit.Test
import org.junit.Assert.*
import java.lang.Math.toDegrees
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

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
        val radians = atan2(y1 - y2, x1 - x2)
        val degrees = toDegrees(radians.toDouble()).toFloat()
        return  if (degrees > 180) degrees % 180 else degrees
    }

    @Test
    fun testCalculateOverall() {
        // Arrange: 테스트 입력 값 준비
        val parts = mutableListOf(
            "목관절" to Status.DANGER,
            "좌측 어깨" to Status.NORMAL,
            "우측 어깨" to Status.DANGER,

            "좌측 팔꿉" to Status.NORMAL,
            "우측 팔꿉" to Status.NORMAL,

            "좌측 손목" to Status.NORMAL,
            "우측 손목" to Status.DANGER,

            "좌측 골반" to Status.WARNING,
            "우측 골반" to Status.WARNING,
            "좌측 무릎" to Status.WARNING,
            "우측 무릎" to Status.NORMAL,
            "우측 발목" to Status.DANGER,
            "우측 발목" to Status.WARNING
        )

        // Act: 함수 호출
        val result = calculateOverall(parts)
        println("77 - $result")

        val parts2 = mutableListOf(
            "목관절" to Status.WARNING,
            "좌측 어깨" to Status.WARNING,
            "우측 어깨" to Status.WARNING,

            "좌측 팔꿉" to Status.NORMAL,
            "우측 팔꿉" to Status.DANGER,

            "좌측 손목" to Status.WARNING,
            "우측 손목" to Status.DANGER,

            "좌측 골반" to Status.WARNING,
            "우측 골반" to Status.WARNING,
            "좌측 무릎" to Status.NORMAL,
            "우측 무릎" to Status.NORMAL,
            "우측 발목" to Status.NORMAL,
            "우측 발목" to Status.NORMAL
        )

        // Act: 함수 호출
        val result2 = calculateOverall(parts2)
        println("77 - $result2")
    }

    fun calculateOverall(parts: MutableList<Pair<String, Status>>) : Int {
        val scores = mapOf(
            Status.DANGER to 39,
            Status.WARNING to 64,
            Status.NORMAL to 95
        )
        val weightScore = 1.65
        val reverseWeightScore = 0.7
        var weightedScoreSum = 0.0
        var totalWeight = 0.0
        for (part in parts) {
            val (bodyPart, status) = part
            val weight = when {
                bodyPart.contains("팔꿉") -> reverseWeightScore
                bodyPart.contains("손목") -> reverseWeightScore
                bodyPart.contains("무릎") -> weightScore
                else -> 1.0
            }
            weightedScoreSum += (scores[status] ?: 0) * weight
            totalWeight += weight
        }
        return if (totalWeight > 0) (weightedScoreSum / totalWeight).toInt() else 0
    }

    fun determineStatus(dangerCount: Int, warningCount: Int, normalCount: Int): Status {
        val total = dangerCount + warningCount + normalCount

        // Case 1: 어떤 상태가 과반수 이상인 경우
        if (dangerCount > total / 2) return Status.DANGER
        if (warningCount > total / 2) return Status.WARNING
        if (normalCount > total / 2) return Status.NORMAL

        // Case 2: danger가 warning과 normal의 합보다 큰 경우
        if (dangerCount > warningCount + normalCount) return Status.DANGER

        // Case 3: 동률 처리 (수정된 규칙)
        when {
            // danger와 warning이 같은 경우 -> DANGER
            dangerCount == warningCount && dangerCount > normalCount -> return Status.DANGER

            // warning과 normal이 같은 경우 -> NORMAL
            warningCount == normalCount && warningCount > dangerCount -> return Status.NORMAL

            // danger와 normal이 같고 warning보다 많은 경우
            dangerCount == normalCount && dangerCount > warningCount -> return Status.WARNING
        }

        // Case 4: warning + danger가 normal보다 많은 경우
        if (warningCount + dangerCount > normalCount) {
            return if (dangerCount > warningCount) Status.DANGER else Status.WARNING
        }

        // 나머지 경우는 NORMAL
        return Status.NORMAL
    }

    @Test
    fun calculatePart() {
        val ear = determineStatus(2, 2 ,2)
        val leftShoulder = determineStatus(2, 1 ,4)
        val rightShoulder = determineStatus(3, 1 ,3)
        val leftElbow = determineStatus(3, 0,4)
        val rightElbow = determineStatus(2, 1 ,4)
        val leftWrist = determineStatus(2, 1 ,3)
        val rightWrist = determineStatus(4, 2 ,1)
        val rightHip = determineStatus(2, 1 ,2)
        val leftKnee = determineStatus(3, 2 ,3)

        print("ear: $ear, shoulder: ($leftShoulder, $rightShoulder), elbow: ($leftElbow, $rightElbow), wrist: ($leftWrist, $rightWrist), rightHip: $rightHip, leftKnee: $leftKnee")
    }


    @Test
    fun noseShoulderTest() {
        val nose1 = Pair(348f, 308f)
        val ear1 = Pair(394f, 294f)
        val shoulder1 = Pair(430f, 382f)

        val ear2 = Pair(406f, 297f)
        val nose2 = Pair(447f, 306f)
        val shoulder2 = Pair(388f, 386f)

        val result1 = 180 + calculateSlope(ear1.first, ear1.second, shoulder1.first, shoulder1.second) % 180
        val result2 = 180 + calculateSlope(nose1.first, nose1.second, shoulder1.first, shoulder1.second) % 180
        val result3 = abs(calculateSlope(ear2.first, ear2.second, shoulder2.first, shoulder2.second) % 180)
        val result4 = abs(calculateSlope(nose2.first, nose2.second, shoulder2.first, shoulder2.second) % 180)
        println(result1)
        println(result2)
        println(result3)
        println(result4)

//        val hip1 = Pair(380f, 698f)
//        val knee1 = Pair(359f, 887f)
//
//        val hip2 = Pair(373f, 698f)
//        val knee2 = Pair(373f, 872f)
//        val result3 = 180 + calculateSlope(hip1.first, hip1.second, knee1.first, knee1.second) % 180
//        val result4 = 180 + calculateSlope(hip2.first, hip2.second, knee2.first, knee2.second) % 180
//        println(result3)
//        println(result4)

    }
}