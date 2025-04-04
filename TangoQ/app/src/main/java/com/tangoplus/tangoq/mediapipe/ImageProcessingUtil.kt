package com.tangoplus.tangoq.mediapipe

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.core.graphics.scale
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.function.MeasurementManager.partIndexes
import com.tangoplus.tangoq.mediapipe.MathHelpers.isTablet

object ImageProcessingUtil {
    private val strokeWidths = 2.5f
    fun combineImageAndOverlay(
        originalBitmap: Bitmap,
        poseLandmarkResult: PoseLandmarkResult,
        sequence: Int,
        painParts: MutableList<Pair<String, Float>>,

    ) : Bitmap {

        val matrix = if (sequence in listOf(0, 2, 5, 6)) {
            Matrix().apply {
                preScale(1f, 1f)
            }
        } else {
            Matrix().apply {
                preScale(-1f, 1f)
            }
        }
        val plr = if (sequence in listOf(0, 2, 5, 6)) {
            reverseLeftRight(poseLandmarkResult.landmarks, originalBitmap.width.toFloat())
        } else {
            poseLandmarkResult.landmarks
        }
        val flippedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
        val resultBitmap = flippedBitmap .copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)
        val axisPaint = Paint().apply {
            color = Color.parseColor("#FF5449")
            strokeWidth = strokeWidths
            style = Paint.Style.STROKE
        }
        val axisSubPaint = Paint().apply {
            color = Color.parseColor("#FF981D")
            strokeWidth = strokeWidths
            style = Paint.Style.STROKE
        }

        val paint = Paint().apply {
            color = Color.parseColor("#2EE88B")
            strokeWidth = strokeWidths
            style = Paint.Style.STROKE
        }

        val borderPaint = Paint().apply {
            color = Color.parseColor("#2EE88B") // 테두리 색
            strokeWidth = strokeWidths
            style = Paint.Style.STROKE // 테두리만 그리기
            isAntiAlias = true
            setShadowLayer(10f, 0f, 0f, Color.parseColor("#1A2EE88B")) // 반지름, x-offset, y-offset, 그림자 색상
        }
        val fillPaint = Paint().apply {
            color = Color.parseColor("#FFFFFF") // 내부 색
            style = Paint.Style.FILL // 내부만 채우기
        }
        val textPaint = Paint().apply {
            color = Color.parseColor("#000000")
            textSize = 48f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER

        }
        val outerCirclePaint = Paint().apply {
            color = Color.parseColor("#41000000")
            style = Paint.Style.FILL
        }
        val innerCirclePaint = Paint().apply {
            color = Color.parseColor("#FFFFFF")
            style = Paint.Style.FILL
        }
        val nose = plr.getOrNull(0)
        val leftEar = plr.getOrNull(7)
        val rightEar = plr.getOrNull(8)
        val leftShoulder = plr.getOrNull(11)
        val rightShoulder = plr.getOrNull(12)
        val leftElbow = plr.getOrNull(13)
        val rightElbow = plr.getOrNull(14)
        val leftWrist = plr.getOrNull(15)
        val rightWrist = plr.getOrNull(16)
        val leftHip = plr.getOrNull(23)
        val rightHip = plr.getOrNull(24)
        val leftKnee = plr.getOrNull(25)
        val rightKnee = plr.getOrNull(26)
        val leftAnkle = plr.getOrNull(27)
        val rightAnkle = plr.getOrNull(28)
        val leftHeel = plr.getOrNull(29)
        val rightHeel = plr.getOrNull(30)
        val leftFoot = plr.getOrNull(31) // 왼발 좌표
        val rightFoot = plr.getOrNull(32) // 오른발 좌표

        val noseX = nose?.x
        val noseY = nose?.y
        val leftShoulderX = leftShoulder?.x
        val leftShoulderY = leftShoulder?.y
        val rightShoulderX = rightShoulder?.x
        val rightShoulderY = rightShoulder?.y
        val midShoulderX = (rightShoulder?.x?.let { leftShoulder?.x?.plus(it) })?.div(2)
        val midShoulderY = (rightShoulder?.y?.let { leftShoulder?.y?.plus(it) })?.div(2)
        val midHipX = (rightHip?.x?.let { leftHip?.x?.plus(it) })?.div(2)
        val midHipY = (rightHip?.y?.let { leftHip?.y?.plus(it) })?.div(2)
        // ------# 수칙  & 수평 보조 축 넣기 #------
        if ( nose != null && leftEar != null && rightEar != null && leftShoulder != null && rightShoulder != null
            && leftElbow != null && rightElbow != null && leftWrist != null && rightWrist != null && leftHip != null && rightHip != null
            && leftKnee != null && rightKnee != null && leftAnkle != null && rightAnkle != null && leftFoot != null && rightFoot != null
            && leftHeel != null && rightHeel != null && noseX != null && noseY != null && leftShoulderX != null && leftShoulderY != null
            && rightShoulderX != null && rightShoulderY != null && midShoulderX != null && midShoulderY != null && midHipX != null && midHipY != null
        ) {
            when (sequence) {
                0 -> {
                    val midAnkleX = (leftAnkle.x + rightAnkle.x) / 2
                    canvas.drawLine(leftEar.x, leftEar.y, rightEar.x, rightEar.y, axisSubPaint)
                    canvas.drawLine(midAnkleX, leftAnkle.y + 100, midAnkleX, nose.y - 100, axisPaint)
                    canvas.drawLine(leftShoulder.x ,leftShoulder.y, rightShoulder.x, rightShoulder.y, axisSubPaint)
                    canvas.drawLine(leftElbow.x ,leftElbow.y, rightElbow.x, rightElbow.y, axisSubPaint)
                    canvas.drawLine(leftWrist.x ,leftWrist.y, rightWrist.x, rightWrist.y, axisSubPaint)
                    canvas.drawLine(leftKnee.x ,leftKnee.y, rightKnee.x, rightKnee.y, axisSubPaint)
                    canvas.drawLine(leftAnkle.x ,leftAnkle.y, rightAnkle.x, rightAnkle.y, axisSubPaint)
                    canvas.drawLine(midShoulderX, midShoulderY, midHipX, midHipY , axisSubPaint)
                    canvas.drawLine(midShoulderX, midShoulderY, nose.x, nose.y , axisSubPaint)
                    canvas.drawLine(leftKnee.x, leftKnee.y - 50 ,  leftKnee.x, leftAnkle.y + 100, axisPaint)
                    canvas.drawLine(rightKnee.x, rightKnee.y - 50 , rightKnee.x, rightAnkle.y + 100, axisPaint)
                }
                3 -> {
                    canvas.drawLine(leftAnkle.x, leftAnkle.y + 50, leftAnkle.x, nose.y - 200, axisPaint)
                    canvas.drawLine(nose.x - 100, nose.y, nose.x + 100, nose.y , axisPaint)
                    canvas.drawLine(leftShoulder.x - 100, leftShoulder.y , leftShoulder.x + 100, leftShoulder.y , axisPaint)

                    canvas.drawLine(nose.x, nose.y, leftShoulder.x, leftShoulder.y, axisSubPaint)
                    canvas.drawLine(leftWrist.x, leftWrist.y, leftAnkle.x, leftWrist.y, axisSubPaint)
                    canvas.drawLine(leftAnkle.x, leftHip.y, leftHip.x, leftHip.y, axisSubPaint)
                    canvas.drawLine(leftAnkle.x, leftKnee.y, leftKnee.x, leftKnee.y, axisSubPaint)
                }
                4 -> {
                    canvas.drawLine(rightAnkle.x, rightAnkle.y + 50, rightAnkle.x, nose.y - 200, axisPaint)
                    canvas.drawLine(nose.x - 100, nose.y, nose.x + 100, nose.y , axisPaint)
                    canvas.drawLine(nose.x, nose.y, rightShoulder.x, rightShoulder.y, axisSubPaint)
                    canvas.drawLine(rightShoulder.x - 100, rightShoulder.y , rightShoulder.x + 100, rightShoulder.y , axisPaint)
                    canvas.drawLine(rightAnkle.x, rightHip.y, rightHip.x, rightHip.y, axisSubPaint)
                    canvas.drawLine(rightWrist.x, rightWrist.y, rightAnkle.x, rightWrist.y, axisSubPaint)
                    canvas.drawLine(rightAnkle.x, rightKnee.y, rightKnee.x, rightKnee.y, axisSubPaint)
                }
                5 -> {
                    val midFootX = (leftAnkle.x + rightAnkle.x) / 2
                    canvas.drawLine(midFootX, leftAnkle.y + 100, midFootX, nose.y - 100, axisPaint)
                    canvas.drawLine(leftShoulder.x, leftShoulder.y, rightShoulder.x, rightShoulder.y , axisSubPaint)
                    canvas.drawLine(leftHip.x, leftHip.y, rightHip.x, rightHip.y , axisSubPaint)
                    canvas.drawLine(leftShoulder.x, leftShoulder.y, rightShoulder.x, rightShoulder.y , axisSubPaint)
                    canvas.drawLine(leftKnee.x, leftKnee.y, rightKnee.x, rightKnee.y , axisSubPaint)
                    canvas.drawLine(leftAnkle.x, leftAnkle.y, rightAnkle.x, rightAnkle.y , axisSubPaint)

                    canvas.drawLine(midShoulderX, midShoulderY, midHipX, midHipY , axisSubPaint)
                    canvas.drawLine(midShoulderX, midShoulderY, nose.x, nose.y , axisSubPaint)
                }
                6 -> {
                    canvas.drawLine(noseX, noseY, leftShoulderX, leftShoulderY, paint)
                    canvas.drawLine(noseX, noseY, rightShoulderX, rightShoulderY, paint)
                    canvas.drawLine(midShoulderX, midShoulderY, noseX, noseY, axisSubPaint)
                    canvas.drawLine(midHipX, midHipY, leftShoulderX, leftShoulderY, paint)
                    canvas.drawLine(midHipX, midHipY, rightShoulderX, rightShoulderY, paint)
                    canvas.drawLine(midHipX, midHipY, midShoulderX, midShoulderY, axisSubPaint)
                    // 골반 다이아 부터 머리 다이아까지
                    val midFootY = ((leftHeel.y + rightHeel.y) / 2  )
                    canvas.drawLine(midHipX, midFootY + 100, noseX, noseY - 100, axisPaint)

                    val connections = listOf(Pair(7, 8), Pair(11, 12), Pair(23, 24))
                    connections.forEach { (start, end) ->
                        val startLandmark = plr.getOrNull(start)
                        val endLandmark = plr.getOrNull(end)
                        if (startLandmark != null && endLandmark != null) {
                            val startX = (startLandmark.x  )
                            val startY = (startLandmark.y )
                            val endX = (endLandmark.x )
                            val endY = (endLandmark.y )
                            canvas.drawLine(startX, startY, endX, endY, paint)
                        }
                    }
                }
            }
            // 관절 연결 선
            val connections = when (sequence) {
                0 -> listOf(
                    Pair(11, 13), Pair(13, 15), Pair(12, 14), Pair(14, 16), // 팔 연결
                    Pair(23, 24), Pair(11, 23), Pair(23, 25), Pair(25, 27), // 왼쪽 다리
                    Pair(12, 24), Pair(24, 26), Pair(26, 28),
                    Pair(27, 31), Pair(27, 29), Pair(29, 31),
                    Pair(28, 32), Pair(28, 30), Pair(30, 32),
                )
                2 -> listOf(
                    Pair(11, 12), // 귀
                    Pair(15, 21), // 왼팔
                    Pair(16, 22), // 오른팔
                    Pair(11, 13), Pair(13, 15), Pair(12, 14), Pair(14, 16), // 팔 연결
                )
                3 -> listOf(
                    Pair(15, 19), Pair(11, 13), Pair(13, 15),
                    Pair(11, 23), Pair(23, 25), Pair(25, 27),
                    Pair(27, 31), Pair(27, 29),
                )
                4 -> listOf(
                    Pair(16, 20), Pair(12, 14), Pair(14, 16),
                    Pair(12, 24), Pair(24, 26), Pair(26, 28),
                    Pair(28, 32), Pair(28, 30)
                )
                5 -> listOf(
                    Pair(11, 13), Pair(13, 15), Pair(12, 14), Pair(14, 16), // 팔 연결
                    Pair(11, 23), Pair(23, 25), Pair(25, 27), // 왼쪽 다리
                    Pair(12, 24), Pair(24, 26), Pair(26, 28),  // 오른쪽 다리
                    Pair(27, 31), Pair(28, 32), Pair(27, 31), Pair(28, 32)
                )
                else -> listOf()
            }

            connections.forEach { (start, end) ->
                val startLandmark = plr.getOrNull(start)
                val endLandmark = plr.getOrNull(end)
                if (startLandmark != null && endLandmark != null) {
                    val startX = startLandmark.x
                    val startY = startLandmark.y
                    val endX = endLandmark.x
                    val endY = endLandmark.y
                    canvas.drawLine(startX, startY, endX, endY, paint)
                }
            }
            if (sequence !in 3 .. 4) {
                val midX = (leftShoulderX + rightShoulderX) / 2
                val midY = (leftShoulderY + rightShoulderY) / 2
                canvas.drawLine(nose.x, nose.y, midX, midY, paint)
                canvas.drawCircle(midX, midY, 5f, fillPaint)
            }
            val pointAccentRange = when (sequence) {
                0 -> listOf(0, 11, 12, 23, 24, 25, 26)
                2 -> listOf(0, 11, 12)
                3 -> listOf(0, 11, 23, 25)
                4 -> listOf(0, 12, 24, 26)
                5 -> listOf(0, 11, 12, 23, 24, 25, 26)
                6 -> listOf(11, 12, 23, 24)
                else -> listOf()
            }
            val pointRange = when (sequence) {
                0 -> listOf(7, 8, 13, 14, 15, 16, 27, 28)
                2 -> listOf(13, 14, 15, 16)
                3 -> listOf(13, 15, 27)
                4 -> listOf(14, 16, 28)
                5 -> listOf(13, 14, 15, 16, 27, 28)
                6 -> listOf(0)
                else -> listOf()
            }
            // 부위의 더 큰 점
            pointAccentRange.forEach { index ->
                val x = plr.getOrNull(index)?.x
                val y = plr.getOrNull(index)?.y
                if (x != null && y != null) {
                    canvas.drawCircle(x, y, 7f, borderPaint)
                    canvas.drawCircle(x, y, 7f, fillPaint)
                }
            }
            pointRange.forEach { index ->
                val x = plr.getOrNull(index)?.x
                val y = plr.getOrNull(index)?.y
                if (x != null && y != null) {
                    canvas.drawCircle(x, y, 5f, borderPaint)
                    canvas.drawCircle(x, y, 5f, fillPaint)
                }
            }
            val allPartsValue = mapOf(
                0 to nose,
                1 to leftShoulder,
                2 to rightShoulder,
                3 to leftElbow,
                4 to rightElbow,
                5 to leftWrist,
                6 to rightWrist,
                7 to leftHip,
                8 to rightHip,
                9 to leftKnee,
                10 to rightKnee,
                11 to leftAnkle,
                12 to rightAnkle,
            )
            partIndexes.forEach { (index, string) ->
                val columnNames = painParts.map { it.first }
                if (columnNames.contains(string)) {
                    val selectParts = painParts.find { it.first == string }
                    when (index) {
                        0 -> setPartCircle(canvas, selectParts?.second?.toInt(), (noseX + midShoulderX) / 2, (noseY + midShoulderY) / 2)
                        else -> {
                            if (filterIndexAndSequence(index, sequence)) {
                                setPartCircle(canvas, selectParts?.second?.toInt(), allPartsValue[index]?.x, allPartsValue[index]?.y)
                            }
                        }
                    }
                }
            }
        }
//        resultBitmap.scale(-1, 1)

        // left right  표시 넣기
        val leftCircleX = 100f
        val leftCircleY = 100f
        val circleRadius = 48f
        val innerRadius = 10f
        if (sequence in listOf(5, 6)) {
            canvas.drawCircle(leftCircleX, leftCircleY, circleRadius, outerCirclePaint)
            canvas.drawCircle(leftCircleX, leftCircleY, circleRadius - innerRadius, innerCirclePaint)
            canvas.drawText("R", leftCircleX, leftCircleY + textPaint.textSize / 3 , textPaint)
            canvas.drawCircle(originalBitmap.width - leftCircleX, leftCircleY, circleRadius, outerCirclePaint)
            canvas.drawCircle(originalBitmap.width - leftCircleX, leftCircleY, circleRadius - innerRadius, innerCirclePaint)
            canvas.drawText("L", originalBitmap.width - leftCircleX, leftCircleY + textPaint.textSize / 3 , textPaint)
        } else if (sequence !in listOf(3, 4)) {
            canvas.drawCircle(leftCircleX, leftCircleY, circleRadius, outerCirclePaint)
            canvas.drawCircle(leftCircleX, leftCircleY, circleRadius - innerRadius, innerCirclePaint)
            canvas.drawText("L", leftCircleX, leftCircleY + textPaint.textSize / 3 , textPaint)
            canvas.drawCircle(originalBitmap.width - leftCircleX, leftCircleY, circleRadius, outerCirclePaint)
            canvas.drawCircle(originalBitmap.width - leftCircleX, leftCircleY, circleRadius - innerRadius, innerCirclePaint)
            canvas.drawText("R", originalBitmap.width - leftCircleX, leftCircleY + textPaint.textSize / 3 , textPaint)
        }

        return resultBitmap
    }

    private fun filterIndexAndSequence(index: Int, sequence: Int) : Boolean = when {
        index == 0 -> true
        sequence == 2 -> index < 7
        sequence == 3 -> index % 2 == 1
        sequence == 4 -> index % 2 == 0
        sequence == 6 -> index in listOf(1, 2, 7, 8)
        else -> true
    }

    fun rePaintDirection(originalBitmap: Bitmap, sequence: Int) : Bitmap {
        val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        // left right  표시 넣기
        val leftCircleX = 60f
        val leftCircleY = 60f
        val circleRadius = 28f
        val innerRadius = 6f
        val textPaint = Paint().apply {
            color = Color.parseColor("#000000")
            textSize = 26f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val outerCirclePaint = Paint().apply {
            color = Color.parseColor("#41000000")
            style = Paint.Style.FILL
        }
        val innerCirclePaint = Paint().apply {
            color = Color.parseColor("#FFFFFF")
            style = Paint.Style.FILL
        }

        if (sequence in listOf(5, 6)) {
            canvas.drawCircle(leftCircleX, leftCircleY, circleRadius, outerCirclePaint)
            canvas.drawCircle(leftCircleX, leftCircleY, circleRadius - innerRadius, innerCirclePaint)
            canvas.drawText("R", leftCircleX, leftCircleY + textPaint.textSize / 3 , textPaint)
            canvas.drawCircle(originalBitmap.width - leftCircleX, leftCircleY, circleRadius, outerCirclePaint)
            canvas.drawCircle(originalBitmap.width - leftCircleX, leftCircleY, circleRadius - innerRadius, innerCirclePaint)
            canvas.drawText("L", originalBitmap.width - leftCircleX, leftCircleY + textPaint.textSize / 3 , textPaint)
        } else if (sequence !in listOf(3, 4)) {
            canvas.drawCircle(leftCircleX, leftCircleY, circleRadius, outerCirclePaint)
            canvas.drawCircle(leftCircleX, leftCircleY, circleRadius - innerRadius, innerCirclePaint)
            canvas.drawText("L", leftCircleX, leftCircleY + textPaint.textSize / 3 , textPaint)
            canvas.drawCircle(originalBitmap.width - leftCircleX, leftCircleY, circleRadius, outerCirclePaint)
            canvas.drawCircle(originalBitmap.width - leftCircleX, leftCircleY, circleRadius - innerRadius, innerCirclePaint)
            canvas.drawText("R", originalBitmap.width - leftCircleX, leftCircleY + textPaint.textSize / 3 , textPaint)
        }
        return mutableBitmap
    }

    fun cropToPortraitRatio(original: Bitmap): Bitmap {
        val width = original.width
        val height = original.height

        // 9:16 비율 계산 ( 가로 비율 )
        val targetRatio = 9f / 16f
        val currentRatio = width.toFloat() / height.toFloat()

        val cropWidth: Int
        val cropHeight: Int
        val x: Int
        val y: Int

        if (currentRatio > targetRatio) {
            // 너비를 기준으로 자르기
            cropWidth = (height * targetRatio).toInt()
            cropHeight = height
            x = (width - cropWidth) / 2
            y = 0
        } else {
            // 높이를 기준으로 자르기
            cropWidth = width
            cropHeight = (width / targetRatio).toInt()
            x = 0
            y = (height - cropHeight) / 2
        }

        return Bitmap.createBitmap(original, x, y, cropWidth, cropHeight)
    }

    fun reverseLeftRight(landmarks: List<PoseLandmarkResult.PoseLandmark>, screenWidth: Float): List<PoseLandmarkResult.PoseLandmark> {
        println("Before swap - First 3 landmarks:")
        landmarks.take(3).forEachIndexed { index, landmark ->
        }

        val result = landmarks.map { landmark ->
            PoseLandmarkResult.PoseLandmark(
                x = screenWidth - landmark.x,  // x 좌표 반전
                y = landmark.y  // y 좌표는 그대로 유지
            )
        }
        result.take(3).forEachIndexed { index, landmark ->
            println("Position $index: X: ${landmark.x}, Y: ${landmark.y}")
        }

        return result
    }

    private fun setPartCircle(canvas: Canvas, degree: Int?, x: Float?, y: Float?) {
        val dangerPart = Paint().apply {
            isDither = true
            isAntiAlias = true
            style  = Paint.Style.FILL
        }
        when (degree) {
            1 -> {
                val radius = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    14f,
                    Resources.getSystem().displayMetrics
                )
                val colors = intArrayOf(
                    Color.argb(100, 255, 151, 29),
                    Color.argb(0, 255, 151, 29)
                )
                val positions = floatArrayOf(0.5f, 1f)
                if (x != null && y  != null) {
                    dangerPart.shader = RadialGradient(
                        x,  // 현재 그릴 x 좌표
                        y,  // 현재 그릴 y 좌표
                        radius,     // radius와 동일한 크기
                        colors,
                        positions,
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawCircle(x, y, radius, dangerPart)
                }

            }
            2 -> {
                val radius = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    14f,
                    Resources.getSystem().displayMetrics
                )
                val colors = intArrayOf(
                    Color.argb(100, 255, 84, 73),
                    Color.argb(0, 255, 84, 73)
                )
                val positions = floatArrayOf(0.5f, 1f)
                if (x != null && y  != null) {
                    dangerPart.shader = RadialGradient(
                        x,  // 현재 그릴 x 좌표
                        y,  // 현재 그릴 y 좌표
                        radius,     // radius와 동일한 크기
                        colors,
                        positions,
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawCircle(x, y, radius, dangerPart)
                }
            }
        }
    }
}