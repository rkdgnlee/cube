package com.tangoplus.tangoq

import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.Math.toDegrees
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.atan2

class MathTest {
    @Test
    fun getAngle() {
        // 앉아 후면 어깨 코 삼각형의 기울기 만들기
//        val result1 = calculateSlope( 450f, 488f, 280f, 482f)
//        val result2 = calculateSlope(291f, 473f, 466f, 472f)
//
//        println("front, $result1")
//        println("back, $result2")
        val result1 = calculateSlope( 287.02985f, 481.2249f, 457.20618f, 480.0976f)
        val result2 = calculateSlope(434.58414f, 466.1927f, 266.95728f, 470.476f)

        println("front, $result1")
        println("후면")
        println("back어깨, $result2")

        val result3 = calculateSlope( 459.60547f, 608.5799f, 235.98128f, 613.73126f)
        val result4 = calculateSlope(452.9229f, 710.3057f, 225.02202f, 720.5872f)
        val result5 = calculateSlope( 392.82767f, 730.6516f, 302.51285f, 733.3984f)
        val result6 = calculateSlope(391.3203f, 921.23047f, 306.8647f, 921.87427f)
        println("back팔꿉, $result3")
        println("back손목, $result4")
        println("back골반, $result5")
        println("back무릎, $result6")

//        assertEquals(90, 40 + 40) // 예상 결과를 넣어줌 (74는 예시)
    }

    // ------# 기울기 계산 #------
    fun calculateSlope(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val radians =  atan2(y1 - y2, x1 - x2) // atan2(y2 -  y1, x2  - x1)
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
        println("어깨 전면: $frontShoulder")
        val frontHip = calculateSlope(411.57208f, 702.4302f, 306.54706f, 703.8306f)
        println("골반 전면: $frontHip")
        val frontKnee = calculateSlope(395.34055f, 893.276f, 304.78033f, 885.38055f)
        println("무릎 전면: $frontKnee")
        val frontAnkle = calculateSlope(379.52997f, 1032.6945f, 321.5162f, 1040.9277f)
        println("발목 전면: $frontAnkle")


        val backShoulder = calculateSlope(269.59814f, 426.30432f,444.7351f, 425.71603f)
        println("어깨 후면: $backShoulder")
        val backHip = calculateSlope(306.54706f, 703.8306f,411.57208f, 702.4302f, )
        println("골반 후면: $backHip")
        val backKnee = calculateSlope( 304.78033f, 885.38055f,395.34055f, 893.276f)
        println("무릎 후면: $backKnee")
        val backAnkle = calculateSlope(321.5162f, 1040.9277f, 379.52997f, 1032.6945f)
        println("발목 후면: $backAnkle")

    }

    @Test
    fun noseAngle() {
        val seq3Nose = calculateSlope(394f, 435f,  317f, 344f)
        val seq4Nose = calculateSlope(354f, 422f, 406f, 328f)
        println("왼쪽코기울기: ${seq3Nose} 오른쪽 코기울기: ${seq4Nose}")
    }

    @Test
    fun distanceXTest() {
        val halfAxis = Pair((390f + 327f) / 2, (1035f + 1043f) / 2)

        val leftShoulder = Pair(439f, 447f)
        val rightShoulder = Pair(269f, 447f)
        val leftWrist = Pair(489f, 676f)
        val rightWrist = Pair(239f, 684f)
        val leftKnee = Pair(395f, 885f)
        val rightKnee = Pair(310f, 888f)

        val shoulderLeftDis = getRealDistanceX(leftShoulder, halfAxis)
        val shoulderRightDis = getRealDistanceX(rightShoulder, halfAxis)
        val wristLeftDis = getRealDistanceX(leftWrist, halfAxis)
        val wristRightDis = getRealDistanceX(rightWrist, halfAxis)
        val kneeLeftDis = getRealDistanceX(leftKnee, halfAxis)
        val kneeRightDis = getRealDistanceX(rightKnee, halfAxis)
        println("어깨 좌우: (${shoulderLeftDis}, ${shoulderRightDis})")
        println("손목 좌우: (${wristLeftDis}, ${wristRightDis})")
        println("무릎 좌우: (${kneeLeftDis}, ${kneeRightDis})")
    }

    @Test
    fun distanceYTest() {
//        val halfAxis = Pair((390f + 327f) / 2, (1035f + 1043f) / 2)
        val leftShoulder = Pair(439f, 447f)
        val rightShoulder = Pair(269f, 447f)
        val leftWrist = Pair(489f, 676f)
        val rightWrist = Pair(239f, 684f)
        val leftKnee = Pair(395f, 885f)
        val rightKnee = Pair(310f, 888f)

        val shoulderLeftDis = getRealDistanceY(rightShoulder, leftShoulder)

        val wristLeftDis = getRealDistanceY(rightWrist, leftWrist)

        val kneeLeftDis = getRealDistanceY(rightKnee, leftKnee)

        println("어깨: ${shoulderLeftDis}")
        println("손목: ${wristLeftDis}")
        println("무릎: ${kneeLeftDis}")
    }

    @Test
    fun seq2Wrist() {
        val halfAxis = Pair((453f + 283f) / 2, (432f + 432f) / 2)
        val leftShoulder = Pair(453f, 432f)
        val rightShoulder = Pair(283f, 432f)
        val leftWrist = Pair(477f, 440f)
        val rightWrist = Pair(251f, 426f)
        val shoulderLeftDis = getRealDistanceX(leftShoulder, halfAxis)
        val shoulderRightDis = getRealDistanceX(rightShoulder, halfAxis)
        val wristLeftDis = getRealDistanceX(leftWrist, halfAxis)
        val wristRightDis = getRealDistanceX(rightWrist, halfAxis)
        println("어깨 양 거리: ($shoulderLeftDis, $shoulderRightDis)")
        println("손목 양 거리: ($wristLeftDis, $wristRightDis)")

        val halfAxis2 = Pair((687f + 582f) / 2, (224f + 227f) / 2)
        val leftShoulder2 = Pair(687f, 224f)
        val rightShoulder2 = Pair(582f, 227f)
        val leftWrist2 = Pair(695f, 234f)
        val rightWrist2 = Pair(564f, 242f)
        val shoulderLeftDis2 = getRealDistanceX(leftShoulder2, halfAxis2)
        val shoulderRightDis2 = getRealDistanceX(rightShoulder2, halfAxis2)
        val wristLeftDis2 = getRealDistanceX(leftWrist2, halfAxis2)
        val wristRightDis2 = getRealDistanceX(rightWrist2, halfAxis2)
        println("어깨 양 거리: ($shoulderLeftDis2, $shoulderRightDis2)")
        println("손목 양 거리: ($wristLeftDis2, $wristRightDis2)")
    }

    @Test
    fun seq34angle() {
        val leftNoseShoulder = calculateSlope(306.4f, 335.7f, 371.5f, 429f)
        println("목기울기: $leftNoseShoulder")
    }


    private val SCALE_X = 0.25f
    private val SCALE_Y = 0.3f
    fun getDistanceX(point1: Pair<Float, Float>, point2: Pair<Float, Float>): Float {
        return abs(point2.first - point1.first)
    }
    fun getDistanceY(point1: Pair<Float, Float>, point2: Pair<Float, Float>): Float {
        return point2.second - point1.second
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

    @Test
    fun getTrim() {
        val list1 = listOf(1, 2, 3)
        val list2  = listOf(6, 7, 8, 9)
        println("list1: $list1, list2: $list2")

        val transList1 = list1.toString().replace(" ", "").replace("[", "").replace("]", "")
        val transList2= list2.toString().replace(" ", "").replace("[", "").replace("]", "")
        println("list1: $transList1, list2: $transList2")

    }
    @Test
    fun current() {
        val list1 = LocalDate.now()
        println("list1: $list1")

    }

}