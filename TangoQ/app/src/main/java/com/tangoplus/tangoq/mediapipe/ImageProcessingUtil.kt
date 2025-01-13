package com.tangoplus.tangoq.mediapipe

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
import androidx.core.graphics.scale
import com.tangoplus.tangoq.function.MeasurementManager.partIndexes

object ImageProcessingUtil {
    fun combineImageAndOverlay(
        originalBitmap: Bitmap,
        poseLandmarkResult: PoseLandmarkResult,
        scaleFactorX: Float,
        scaleFactorY: Float,
        offSetX: Float,
        offSetY: Float,
        sequence: Int,
        painParts: MutableList<Pair<String, Float>>,
    ) : Bitmap {
        Log.v("스케일과오프셋", "scaleFactor: (${scaleFactorX}, ${scaleFactorY}), offset: ($offSetX, $offSetY)")

        val matrix = Matrix().apply {
            preScale(-1f, 1f)
        } // 전면카메라로 찍었을 경우 걍 원래대로 돌려야 함.

        val flippedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
        val resultBitmap = flippedBitmap .copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)
        val axisPaint = Paint().apply {
            color = Color.parseColor("#FF5449")
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }
        val axisSubPaint = Paint().apply {
            color = Color.parseColor("#FF981D")
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }

        val paint = Paint().apply {
            color = Color.parseColor("#2EE88B")
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }

        val borderPaint = Paint().apply {
            color = Color.parseColor("#2EE88B") // 테두리 색
            strokeWidth = 4f
            style = Paint.Style.STROKE // 테두리만 그리기
            isAntiAlias = true
            setShadowLayer(10f, 0f, 0f, Color.parseColor("#1A2EE88B")) // 반지름, x-offset, y-offset, 그림자 색상
        }


        val fillPaint = Paint().apply {
            color = Color.parseColor("#FFFFFF") // 내부 색
            style = Paint.Style.FILL // 내부만 채우기
        }
        val nose = poseLandmarkResult.landmarks.getOrNull(0)
        val leftEar = poseLandmarkResult.landmarks.getOrNull(7)
        val rightEar = poseLandmarkResult.landmarks.getOrNull(8)
        val leftShoulder = poseLandmarkResult.landmarks.getOrNull(11)
        val rightShoulder = poseLandmarkResult.landmarks.getOrNull(12)
        val leftElbow = poseLandmarkResult.landmarks.getOrNull(13)
        val rightElbow = poseLandmarkResult.landmarks.getOrNull(14)
        val leftWrist = poseLandmarkResult.landmarks.getOrNull(15)
        val rightWrist = poseLandmarkResult.landmarks.getOrNull(16)
        val leftHip = poseLandmarkResult.landmarks.getOrNull(23)
        val rightHip = poseLandmarkResult.landmarks.getOrNull(24)
        val leftKnee = poseLandmarkResult.landmarks.getOrNull(25)
        val rightKnee = poseLandmarkResult.landmarks.getOrNull(26)
        val leftAnkle = poseLandmarkResult.landmarks.getOrNull(27)
        val rightAnkle = poseLandmarkResult.landmarks.getOrNull(28)
        val leftFoot = poseLandmarkResult.landmarks.getOrNull(27) // 왼발 좌표
        val rightFoot = poseLandmarkResult.landmarks.getOrNull(28) // 오른발 좌표

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
            && noseX != null && noseY != null && leftShoulderX != null && leftShoulderY != null && rightShoulderX != null && rightShoulderY != null
            && midShoulderX != null && midShoulderY != null && midHipX != null && midHipY != null
        ) {
            when (sequence) {
                0 -> {
                    val midFootX = (leftFoot.x + rightFoot.x) / 2
                    canvas.drawLine(leftEar.x, leftEar.y, rightEar.x, rightEar.y, axisSubPaint)
                    canvas.drawLine(midFootX, leftFoot.y + 100, midFootX, nose.y - 100, axisPaint)
                    canvas.drawLine(leftShoulder.x ,leftShoulder.y, rightShoulder.x, rightShoulder.y, axisSubPaint)
                    canvas.drawLine(leftElbow.x ,leftElbow.y, rightElbow.x, rightElbow.y, axisSubPaint)
                    canvas.drawLine(leftWrist.x ,leftWrist.y, rightWrist.x, rightWrist.y, axisSubPaint)
                    canvas.drawLine(leftKnee.x ,leftKnee.y, rightKnee.x, rightKnee.y, axisSubPaint)
                    canvas.drawLine(leftAnkle.x ,leftAnkle.y, rightAnkle.x, rightAnkle.y, axisSubPaint)
                    canvas.drawLine(midShoulderX, midShoulderY, midHipX, midHipY , axisSubPaint)
                    canvas.drawLine(midShoulderX, midShoulderY, nose.x, nose.y , axisSubPaint)
                }
                3 -> {
                    canvas.drawLine(leftFoot.x, leftFoot.y + 50, leftFoot.x, nose.y - 200, axisPaint)
                    canvas.drawLine(nose.x, nose.y, leftShoulder.x, leftShoulder.y, axisSubPaint)
                    canvas.drawLine(nose.x - 100, nose.y, nose.x + 100, nose.y , axisPaint)
                    canvas.drawLine(leftShoulder.x - 100, leftShoulder.y , leftShoulder.x + 100, leftShoulder.y , axisPaint)
                    canvas.drawLine(leftWrist.x, leftWrist.y, leftFoot.x, leftWrist.y, axisSubPaint)
                    canvas.drawLine(leftFoot.x, leftHip.y, leftHip.x, leftHip.y, axisSubPaint)
                    canvas.drawLine(leftFoot.x, leftKnee.y, leftKnee.x, leftKnee.y, axisSubPaint)
                }
                4 -> {
                    canvas.drawLine(rightFoot.x, rightFoot.y + 50, rightFoot.x, nose.y - 200, axisPaint)
                    canvas.drawLine(nose.x - 100, nose.y, nose.x + 100, nose.y , axisPaint)
                    canvas.drawLine(nose.x, nose.y, rightShoulder.x, rightShoulder.y, axisSubPaint)
                    canvas.drawLine(rightShoulder.x - 100, rightShoulder.y , rightShoulder.x + 100, rightShoulder.y , axisPaint)
                    canvas.drawLine(rightFoot.x, rightHip.y, rightHip.x, rightHip.y, axisSubPaint)
                    canvas.drawLine(rightWrist.x, rightWrist.y, rightFoot.x, rightWrist.y, axisSubPaint)
                    canvas.drawLine(rightFoot.x, rightKnee.y, rightKnee.x, rightKnee.y, axisSubPaint)
                }
                5 -> {
                    val midFootX = (leftFoot.x + rightFoot.x) / 2
                    canvas.drawLine(midFootX, leftFoot.y + 100, midFootX, nose.y - 100, axisPaint)
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
                    val midFootY = ((leftFoot.y + rightFoot.y) / 2  )
                    canvas.drawLine(midHipX, midFootY + 100, noseX, noseY - 100, axisPaint)

                    val connections = listOf(Pair(7, 8), Pair(11, 12), Pair(23, 24))
                    connections.forEach { (start, end) ->
                        val startLandmark = poseLandmarkResult.landmarks.getOrNull(start)
                        val endLandmark = poseLandmarkResult.landmarks.getOrNull(end)
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
                val startLandmark = poseLandmarkResult.landmarks.getOrNull(start)
                val endLandmark = poseLandmarkResult.landmarks.getOrNull(end)
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
                val x = poseLandmarkResult.landmarks.getOrNull(index)?.x
                val y = poseLandmarkResult.landmarks.getOrNull(index)?.y
                if (x != null && y != null) {
                    canvas.drawCircle(x, y, 7f, borderPaint)
                    canvas.drawCircle(x, y, 7f, fillPaint)
                }
            }
            pointRange.forEach { index ->
                val x = poseLandmarkResult.landmarks.getOrNull(index)?.x
                val y = poseLandmarkResult.landmarks.getOrNull(index)?.y
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

//            partIndexes.forEach { index, string ->
//                val columnNames = painParts.map { it.first }
//                if (columnNames.contains(string)) {
//                    val selectParts = painParts.find { it.first == string }
//                    when (index) {
//                        0 -> setCircleColor(canvas, selectParts?.second?.toInt(), (noseX + midShoulderX) / 2, (noseY + midShoulderY) / 2)
//                        else -> setCircleColor(canvas, selectParts?.second?.toInt(), allPartsValue[index]?.x, allPartsValue[index]?.y)
//                    }
//                }
//            }
        }
        resultBitmap.scale(-1, 1)
        return resultBitmap
    }

    fun cropToPortraitRatio(original: Bitmap): Bitmap {
        val width = original.width
        val height = original.height

        // 9:16 비율 계산
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

//    fun setCircleColor(canvas: Canvas, degree: Int?, x: Float?, y: Float?) {
//        val dangerPart = Paint().apply {
//            isDither = true
//            isAntiAlias = true
//            style  = Paint.Style.FILL
//        }
//        when (degree) {
//            1 -> {
//                val radius = TypedValue.applyDimension(
//                    TypedValue.COMPLEX_UNIT_DIP,
//                    16f,
//                    Resources.getSystem().displayMetrics
//                )
//                val colors = intArrayOf(
//                    Color.argb(100, 255, 151, 29),
//                    Color.argb(0, 255, 151, 29)
//                )
//                val positions = floatArrayOf(0.5f, 1f)
//                if (x != null && y  != null) {
//                    dangerPart.shader = RadialGradient(
//                        x,  // 현재 그릴 x 좌표
//                        y,  // 현재 그릴 y 좌표
//                        radius,     // radius와 동일한 크기
//                        colors,
//                        positions,
//                        Shader.TileMode.CLAMP
//                    )
//                    canvas.drawCircle(x, y, radius, dangerPart)
//                }
//
//            }
//            2 -> {
//                val radius = TypedValue.applyDimension(
//                    TypedValue.COMPLEX_UNIT_DIP,
//                    18f,
//                    Resources.getSystem().displayMetrics
//                )
//                val colors = intArrayOf(
//                    Color.argb(100, 255, 84, 73),
//                    Color.argb(0, 255, 84, 73)
//                )
//                val positions = floatArrayOf(0.5f, 1f)
//                if (x != null && y  != null) {
//                    dangerPart.shader = RadialGradient(
//                        x,  // 현재 그릴 x 좌표
//                        y,  // 현재 그릴 y 좌표
//                        radius,     // radius와 동일한 크기
//                        colors,
//                        positions,
//                        Shader.TileMode.CLAMP
//                    )
//                    canvas.drawCircle(x, y, radius, dangerPart)
//                }
//            }
//        }
//    }
}