package com.tangoplus.tangoq

import android.util.Log
import com.tangoplus.tangoq.function.MeasurementManager
import com.tangoplus.tangoq.mediapipe.MathHelpers.calculateAngle
import com.tangoplus.tangoq.mediapipe.MathHelpers.determineDirection
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.Math.toDegrees
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.atan
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
        val leftShoulder1 = Pair(474f, 445f)
        val rightShoulder1 = Pair(305f, 438f)
        val leftShoulder2 = Pair(285f, 500f)
        val rightShoulder2 = Pair(459f, 476f)

        val leftWrist1 = Pair(517f, 671f)
        val rightWrist1 = Pair(276f, 664f)
        val leftWrist2 = Pair(240f, 716f)
        val rightWrist2 = Pair(500f, 713f)

        val leftKnee1 = Pair(395f, 885f)
        val rightKnee1 = Pair(310f, 888f)
        val leftKnee2 = Pair(395f, 885f)
        val rightKnee2 = Pair(310f, 888f)

        //
        val frontShoulder = getRealDistanceY(leftShoulder1, rightShoulder1)
        val backShoulder = getRealDistanceY(leftShoulder2, rightShoulder2)
        val frontWrist = getRealDistanceY(leftWrist1, rightWrist1)
        val backWrist = getRealDistanceY(leftWrist2, rightWrist2)
        val frontKnee = getRealDistanceY(leftKnee1, rightKnee1)
        val backKnee = getRealDistanceY(leftKnee2, rightKnee2)
        println("정카 어깨: $frontShoulder, 후카 어깨 $backShoulder")
        println("정카 팔꿉: $frontWrist, 후카 팔꿉 $backWrist")
        println("정카 무릎: $frontKnee, 후카 무릎 $backKnee")
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
    private val SCALE_Y = 0.225f
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

    @Test
    fun calculateWeekSeq() {
        val result = listOf(listOf(3, 2, 3, 2), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0))
        val adjustedWeek = 1 - 1
        val adjustedSeq = 3 - 1
//        Log.v("포스트getProgress", "callback: ($adjustedWeek, $adjustedSeq, $week, $seq,) result: $result")
        val countSets = result[adjustedWeek]
        val isMaxSeq = countSets.map { it == 3 }.all { it }
        val isMinSeq = countSets.min()
        val isSeqFinish = countSets.distinct().size == 1
        val setWeek : Int
        val setSeq : Int
//                    val serverLastItem = result[adjustedWeek].last()
//                    Log.v("포스트getLast팡", "$serverLastItem $adjustedSeq, $week, $seq")
        if (isMaxSeq && isSeqFinish) {
            // 모든 회차가 끝남 ( 주차가 넘어가야하는 상황 )
            setWeek = adjustedWeek + 1
            setSeq = 0
            // 회차 진행중
            println("0")
        } else if (!isMaxSeq && isSeqFinish && isMinSeq > 0) { // 3회차가 아니고, 전부 끝났고, 현재 회차가 1이상임 -> 여기서 그럼 전부 안끝냈을 때의 조건이 있어야 함.
            setWeek = adjustedWeek
            setSeq = adjustedSeq + 1
            // 회차가 끝남
            println("1")

        } else if (isMinSeq > 0) { // 현재 회차가 1 이상임 그리고
            setWeek = adjustedWeek
            setSeq = adjustedSeq
            // 회차가 끝남
            println("2")

        } else {
            setWeek = adjustedWeek
            setSeq = adjustedSeq
            println("3")
        }
        print("week: $setWeek, seq: $setSeq")
    }

    @Test
    fun seqText() {
        val indexes = mutableListOf<Int>()
        for (i in 0 .. 32) {
            indexes.add(i + 10)
        }
        indexes.forEachIndexed { index, i ->
            val swapIndex = if (index >= 7 && index % 2 == 0) index - 1 // 짝수인 경우 뒤의 홀수 인덱스로 교체
            else if (index >= 7 && index % 2 == 1) index + 1 // 홀수인 경우 앞의 짝수 인덱스로 교체
            else index // 7 미만인 경우 그대로 사용
            println(swapIndex)
        }

    }

//    // 4월 10일자 전면 카메라
//    @Test
//    fun calculateShoulderElbow1() {
//        val leftShoulder = Pair(477f, 407f)
//        val rightShoulder = Pair(287f, 401f)
//        val leftElbow = Pair(498f, 556f)
//        val rightElbow = Pair(260f, 546f)
//        val result1 =  calculateSlope(leftShoulder.first, leftShoulder.second, leftElbow.first, leftElbow.second)
//        val result2 =  calculateSlope(rightShoulder.first, rightShoulder.second, rightElbow.first, rightElbow.second)
//        println("왼쪽: ${180 + result1 % 180}, 오른쪽: ${abs(result2) % 180}")
//    }
//    // 4월 11일자 후면 카메라
//    @Test
//    fun calculateShoulderElbow2() {
//        val leftShoulder = Pair(477f, 407f)
//        val rightShoulder = Pair(287f, 401f)
//        val leftElbow = Pair(498f, 556f)
//        val rightElbow = Pair(260f, 546f)
//        val result1 =  calculateSlope(leftShoulder.first, leftShoulder.second, leftElbow.first, leftElbow.second)
//        val result2 =  calculateSlope(rightShoulder.first, rightShoulder.second, rightElbow.first, rightElbow.second)
//        println("왼쪽: ${180 + result1 % 180}, 오른쪽: ${abs(result2) % 180}")
//    }
//
//    // 4월 1일자 키오스크
//    @Test
//    fun calculateShoulderElbow3() {
//        val leftShoulder = Pair(690f, 245f)
//        val rightShoulder = Pair(586f, 238f)
//        val leftElbow = Pair(704f, 327f)
//        val rightElbow = Pair(569f, 320f)
//        val result1 =  calculateSlope(leftShoulder.first, leftShoulder.second, leftElbow.first, leftElbow.second)
//        val result2 =  calculateSlope(rightShoulder.first, rightShoulder.second, rightElbow.first, rightElbow.second)
//        println("왼쪽: ${180 + result1 % 180}, 오른쪽: ${abs(result2) % 180}")
//    }
    fun normalizeAngle90(angle: Float): Float {
        val absAngle = abs(angle % 180)
        return if (absAngle > 90) 180 - absAngle else absAngle
    }
    // 4월 10일자 후면 카메라
    @Test
    fun calculateTopSlope1() {
        val nose = Pair(448f, 377f)
        val leftEar = Pair(402f, 370f)
        val rightEar = Pair(414f, 369f)

        val leftShoulder = Pair(359f, 472f)
        val rightShoulder = Pair(405f, 476f)
        val result1 =  calculateSlope(nose.first, nose.second, leftShoulder.first, leftShoulder.second)
        val result2 =  calculateSlope(nose.first, nose.second, rightShoulder.first, rightShoulder.second)
        val result3 =  calculateSlope(leftEar.first, leftEar.second, leftShoulder.first, leftShoulder.second)
        val result4 =  calculateSlope(rightEar.first, rightEar.second, rightShoulder.first, rightShoulder.second)

        val rangeResult1 = normalizeAngle90(result1)
        val rangeResult2 = normalizeAngle90(result2)
        val rangeResult3 = normalizeAngle90(result3)
        val rangeResult4 = normalizeAngle90(result4)
        println("왼쪽 코-어깨: $result1 -> $rangeResult1, 오른쪽 코-어깨: $result2 -> $rangeResult2, 왼쪽귀-어깨: $result3 -> $rangeResult3, 오른쪽귀-어깨: $result4 -> $rangeResult4")
    }
    @Test
    fun calculateTopSlope2() {
        val nose = Pair(307f, 338f)
        val leftEar = Pair(351f, 329f)
        val rightEar = Pair(343f, 328f)

        val leftShoulder = Pair(389f, 433f)
        val rightShoulder = Pair(359f, 435f)
        val result1 =  calculateSlope(nose.first, nose.second, leftShoulder.first, leftShoulder.second)
        val result2 =  calculateSlope(nose.first, nose.second, rightShoulder.first, rightShoulder.second)
        val result3 =  calculateSlope(leftEar.first, leftEar.second, leftShoulder.first, leftShoulder.second)
        val result4 =  calculateSlope(rightEar.first, rightEar.second, rightShoulder.first, rightShoulder.second)

        val rangeResult1 = normalizeAngle90(result1)
        val rangeResult2 = normalizeAngle90(result2)
        val rangeResult3 = normalizeAngle90(result3)
        val rangeResult4 = normalizeAngle90(result4)
        println("왼쪽 코-어깨: $result1 -> $rangeResult1, 오른쪽 코-어깨: $result2 -> $rangeResult2, 왼쪽귀-어깨: $result3 -> $rangeResult3, 오른쪽귀-어깨: $result4 -> $rangeResult4")
    }

    @Test
    fun calculateTopSlope3() {

        val leftKnee = Pair(339f, 928f)
        val rightKnee = Pair(444f, 920f)

        val leftAnkle = Pair(347f, 1080f)
        val rightAnkle = Pair(451f, 1070f)
        val result1 =  calculateSlope(leftKnee.first, leftKnee.second, leftAnkle.first, leftAnkle.second)
        val result2 =  calculateSlope(rightKnee.first, rightKnee.second, rightAnkle.first, rightAnkle.second)

        val rangeResult1 = normalizeAngle90(result1)
        val rangeResult2 = normalizeAngle90(result2)

        println("왼쪽 코-어깨: $result1 -> $rangeResult1, 오른쪽 코-어깨: $result2 -> $rangeResult2")

        val leftKnee2 = Pair(441f, 884f)
        val rightKnee2 = Pair(319f, 884f)

        val leftAnkle2 = Pair(432f, 1020f)
        val rightAnkle2 = Pair(306f, 1044f)
        val result3 =  calculateSlope(leftKnee2.first, leftKnee2.second, leftAnkle2.first, leftAnkle2.second)
        val result4 =  calculateSlope(rightKnee2.first, rightKnee2.second, rightAnkle2.first, rightAnkle2.second)

        val rangeResult3 = normalizeAngle90(result3)
        val rangeResult4 = normalizeAngle90(result4)

        println("왼쪽 코-어깨: $result3 -> $rangeResult3, 오른쪽 코-어깨: $result4 -> $rangeResult4")

    }


//    @Test
//    fun calculateShoulderElbow1() {
//        val leftShoulder = Pair(285f, 500f)
//        val rightShoulder = Pair(459f, 476f)
//        val leftElbow = Pair(267f, 626f)
//        val rightElbow = Pair(491f, 604f)
//        val result1 =  calculateSlope(leftShoulder.first, leftShoulder.second, leftElbow.first, leftElbow.second)
//        val result2 =  calculateSlope(rightShoulder.first, rightShoulder.second, rightElbow.first, rightElbow.second)
//
//        val rangeResult1 = normalizeAngle90(result1)
//        val rangeResult2 = normalizeAngle90(result2)
//        println("왼쪽: $result1 -> $rangeResult1, 오른쪽: $result2 -> $rangeResult2")
//    }
//    @Test
//    fun calculateShoulderElbow2() {
//        val leftShoulder = Pair(477f, 407f)
//        val rightShoulder = Pair(287f, 401f)
//        val leftElbow = Pair(498f, 556f)
//        val rightElbow = Pair(260f, 546f)
//        val result1 =  calculateSlope(leftShoulder.first, leftShoulder.second, leftElbow.first, leftElbow.second)
//        val result2 =  calculateSlope(rightShoulder.first, rightShoulder.second, rightElbow.first, rightElbow.second)
//        println("왼쪽: $result1 -> ${180 + result1 % 180}, 오른쪽: $result2 -> ${abs(result2) % 180}")
//    }
//
//    // 4월 1일자 키오스크
//    @Test
//    fun calculateShoulderElbow3() {
//        val leftShoulder = Pair(690f, 245f)
//        val rightShoulder = Pair(586f, 238f)
//        val leftElbow = Pair(704f, 327f)
//        val rightElbow = Pair(569f, 320f)
//        val result1 =  calculateSlope(leftShoulder.first, leftShoulder.second, leftElbow.first, leftElbow.second)
//        val result2 =  calculateSlope(rightShoulder.first, rightShoulder.second, rightElbow.first, rightElbow.second)
//        val rangeResult1 = normalizeAngle90(result1)
//        val rangeResult2 = normalizeAngle90(result2)
//        println("왼쪽: $result1 -> $rangeResult1, 오른쪽: $result2 -> $rangeResult2")
//    }


    @Test
    fun angle() {

        // 좌측
        val leftShoulder1 = Pair(637f,233f)
        val leftElbow1 = Pair(632f, 325f)
        val leftWrist1 = Pair(604f, 397f)

        val leftShoulder2 = Pair(418f, 402f)
        val leftElbow2 = Pair(418f, 556f)
        val leftWrist2 = Pair(355f, 673f)

        val leftShoulder3 = Pair(333f, 548f)
        val leftElbow3 = Pair(342f, 650f)
        val leftWrist3 = Pair(373f, 738f)

        val result1 = determineDirection(leftShoulder1.first, leftShoulder1.second, leftElbow1.first, leftElbow1.second, leftWrist1.first, leftWrist1.second)
        val result2 = determineDirection(leftShoulder2.first, leftShoulder2.second, leftElbow2.first, leftElbow2.second, leftWrist2.first, leftWrist3.second)
        val result3 = determineDirection(leftShoulder3.first, leftShoulder3.second, leftElbow3.first, leftElbow3.second, leftWrist2.first, leftWrist3.second)
        println("좌측면 seq: 키오스크: $result1, 앱 정면: $result2 앱 후면: $result3")

        // 우측
        val leftShoulder4 = Pair(625f,228f)
        val leftElbow4 = Pair(635f, 323f)
        val leftWrist4 = Pair(646f, 390f)

        val leftShoulder5 = Pair(361f, 390f)
        val leftElbow5 = Pair(361f, 538f)
        val leftWrist5 = Pair(404f, 635f)

        val leftShoulder6 = Pair(360f, 565f)
        val leftElbow6 = Pair(360f, 664f)
        val leftWrist6 = Pair(337f, 740f)

        val result4 = determineDirection(leftShoulder4.first, leftShoulder4.second, leftElbow4.first, leftElbow4.second, leftWrist4.first, leftWrist4.second)
        val result5 = determineDirection(leftShoulder5.first, leftShoulder5.second, leftElbow5.first, leftElbow5.second, leftWrist5.first, leftWrist5.second)
        val result6 = determineDirection(leftShoulder6.first, leftShoulder6.second, leftElbow6.first, leftElbow6.second, leftWrist6.first, leftWrist6.second)
        println("우측면 seq: 키오스크: $result4, 앱 정면: $result5 앱 후면: $result6")
    }
}