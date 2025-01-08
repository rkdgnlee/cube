package com.tangoplus.tangoq

import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.Math.toDegrees
import kotlin.math.abs
import kotlin.math.atan2

class MathTest {
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
        val radians = atan2(y2 -  y1, x2  - x1)
        val degrees = toDegrees(radians.toDouble()).toFloat()
        println("$degrees")
        return  if (degrees > 180) degrees % 180 else degrees
    }
    // ------# 수평 초기화 기울기 계산 #------
    fun calculateSlope180(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val radians = atan2(y2 - y1, x2 - x1)
        val degrees = toDegrees(radians.toDouble())
        return normalizeTo180(degrees.toFloat()) // 180도 기준으로 정규화
    }

    // 180도 기준으로 정규화하는 함수
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

        val prevFrontShoulder = calculateSlope(0.3800059f, 0.33592227f, 0.62224734f, 0.33485872f)
        val prevFrontShoulder180 = calculateSlope180(0.3800059f, 0.33592227f, 0.62224734f, 0.33485872f)
        println("어깨 원시: $prevFrontShoulder, 180: ${prevFrontShoulder180}")
        val prevFrontHip = calculateSlope(0.42858624f, 0.5420497f, 0.5741844f, 0.54563606f)
        val prevFrontHip180 = calculateSlope180(0.42858624f, 0.5420497f, 0.5741844f, 0.54563606f)
        println("골반 원시: $prevFrontHip, 180: $prevFrontHip180")
        val prevFrontKnee = calculateSlope(0.43309414f, 0.69748914f, 0.55954874f, 0.69440746f)
        val prevFrontKnee180 = calculateSlope180(0.43309414f, 0.69748914f, 0.55954874f, 0.69440746f)
        println("무릎 원시: $prevFrontKnee, 180: $prevFrontKnee180")
        val prevFrontAnkle = calculateSlope(0.44268543f, 0.8084885f, 0.53620124f, 0.80681765f)
        val prevFrontAnkle180 = calculateSlope180(0.44268543f, 0.8084885f, 0.53620124f, 0.80681765f)
        println("발목 원시: $prevFrontAnkle, 180: $prevFrontAnkle180")

        println("후면 데이터")
        val backShoulderAngle = calculateSlope( 0.62880296f, 0.33354384f, 0.37626427f, 0.3290543f)
        val backShoulderAngle180 = calculateSlope180( 0.62880296f, 0.33354384f, 0.37626427f, 0.3290543f)
        println("어깨 원시: $backShoulderAngle, 180: ${backShoulderAngle180}")
        val backWristAngle = calculateSlope( 0.6474066f, 0.53509456f, 0.3499326f, 0.53832406f)
        val backWristAngle180 = calculateSlope180( 0.5396624f, 0.2456446f, 0.4431353f, 0.24797726f)
        println("손목 원시: $backWristAngle, 180: ${backWristAngle180}")
        val backKneeAngle = calculateSlope( 0.5422297f, 0.69960886f, 0.41508266f, 0.7011234f)
        val backKneeAngle180 = calculateSlope180( 0.5422297f, 0.69960886f, 0.41508266f, 0.7011234f)
        println("무릎 원시: $backKneeAngle, 180: ${backKneeAngle180}")

    }

    @Test
    fun getScalingAngle() {

//        val FrontShoulder = calculateSlope(697f, 233f, 579f, 229f)
//        println("스케일링 어깨: $FrontShoulder")
//        val FrontHip = calculateSlope(671f, 408f, 605f, 407f)
//        println("스케일링 골반: $FrontHip")
//        val FrontKnee = calculateSlope(674f, 528f, 608f, 529f)
//        println("스케일링 무릎: $FrontKnee")
//        val FrontAnkle = calculateSlope(668f, 638f, 610f, 639f)
//        println("스케일링 발목: $FrontAnkle")

        println("최신 스케일링 전면")
        val frontShoulderScale = calculateSlope(452f, 430f, 278f, 432f)
        println("스케일링 어깨: $frontShoulderScale ")
        val frontHipScale = calculateSlope(416f, 694f, 315f, 702f)
        println("스케일링 골반: $frontHipScale ")
        val frontKneeScale = calculateSlope(418f, 889f, 326f, 889f)
        println("스케일링 무릎: $frontKneeScale ")
        val frontAnkleScale = calculateSlope(404f, 1029f, 335f, 1029f)
        println("스케일링 발목: $frontAnkleScale ")
        println("최신 전면")
        val frontShoulder = calculateSlope(444.7351f, 425.71603f, 269.59814f, 426.30432f)
        println("어깨 원시: $frontShoulder")
        val frontHip = calculateSlope(411.57208f, 702.4302f, 306.54706f, 703.8306f)
        println("골반 원시: $frontHip")
        val frontKnee = calculateSlope(395.34055f, 893.276f, 304.78033f, 885.38055f)
        println("무릎 원시: $frontKnee")
        val frontAnkle = calculateSlope(379.52997f, 1032.6945f, 321.5162f, 1040.9277f)
        println("발목 원시: $frontAnkle")

//        println("후면 데이터")
//        val backShoulderAngle = calculateSlope( 0.62880296f, 0.33354384f, 0.37626427f, 0.3290543f)
//        val backShoulderAngle180 = calculateSlope180( 0.62880296f, 0.33354384f, 0.37626427f, 0.3290543f)
//        println("어깨 원시: $backShoulderAngle, 180: ${backShoulderAngle180}")
//        val backWristAngle = calculateSlope( 0.6474066f, 0.53509456f, 0.3499326f, 0.53832406f)
//        val backWristAngle180 = calculateSlope180( 0.5396624f, 0.2456446f, 0.4431353f, 0.24797726f)
//        println("손목 원시: $backWristAngle, 180: ${backWristAngle180}")
//        val backKneeAngle = calculateSlope( 0.5422297f, 0.69960886f, 0.41508266f, 0.7011234f)
//        val backKneeAngle180 = calculateSlope180( 0.5422297f, 0.69960886f, 0.41508266f, 0.7011234f)
//        println("무릎 원시: $backKneeAngle, 180: ${backKneeAngle180}")

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

//    @Test
//    fun leftRightJudge() {
//        val leftFrontShoulder = judgeState(0, 178f, Triple(-180f, 0.64f, 2.34f))
//        val rightFrontShoulder = judgeState(0, 178f, Triple(180f, 0.64f, 2.34f))
//        val leftBackShoulder = judgeState(5, -3.32f, Triple(0f, 0.9f,1.8f))
//        val rightBackShoulder = judgeState(5, -3.32f, Triple(0f, -0.9f,-1.8f))
//
//
//        println("[front]left: $leftFrontShoulder, right: $rightFrontShoulder")
//        println("[back]left: $leftBackShoulder, right: $rightBackShoulder")
//    }
//
//    fun judgeState(i : Int, data: Float, boundTriple: Triple<Float, Float, Float>) : String {
//        val (center, warning, danger) = boundTriple
//        // 어느 쪽이 안좋은지 판단한 후, 주의 위험은 절대값으로 판단
//        return if (i == 5) {
//            val warningBound = center + warning
//            val dangerBound = center + danger
//            when {
//                warningBound > 0 -> {
//                    when {
//                        data > dangerBound -> {  "status.DANGER" }
//                        data > warningBound -> { "status.WARNING" }
//                        else -> { "status.NORMAL" }
//                    }
//                }
//                warningBound < 0 -> {
//                    when {
//                        data < dangerBound  -> { "status.DANGER" }
//                        data < warningBound -> { "status.WARNING" }
//                        else -> { "status.NORMAL" }
//                    }
//                }
//
//                else -> { ""}
//            }
//        } else {
//            val absCenter = abs(center)
//            val lowerWarning = absCenter - warning
//            val upperWarning = absCenter + warning
//            val lowerDanger = absCenter - danger
//            val upperDanger = absCenter + danger
//
//            when {
//                data < lowerDanger || data > upperDanger -> {
//                    // 위험
//                    "status.DANGER"
//                }
//                data < lowerWarning || data > upperWarning -> {
//                    // 주의
//                    "status.WARNING"
//                }
//                else -> {
//                    // 정상
//                    "status.NORMAL"
//                }
//            }
//        }
//    }

}