package com.tangoplus.tangoq.mediapipe

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.util.Log
import androidx.core.graphics.scale

object ImageProcessingUtil {


    fun combineImageAndOverlay(
        originalBitmap: Bitmap,
        poseLandmarkResult: PoseLandmarkResult,
        scaleFactorX: Float,
        scaleFactorY: Float,
        offSetX: Float,
        offSetY: Float,
        sequence: Int,
    ): Bitmap {
        Log.v("스케일과오프셋", "scaleFactor: (${scaleFactorX}, ${scaleFactorY}), offset: ($offSetX, $offSetY)")


        val matrix = Matrix().apply {
            preScale(-1f, 1f)
        } // 전면카메라로 찍었을 경우 걍 원래대로 돌려야 함.
        val flippedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
        val resultBitmap = flippedBitmap .copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)
        val axisPaint = Paint().apply {
            color = Color.parseColor("#FF5449")
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        val axisSubPaint = Paint().apply {
            color = Color.parseColor("#2EE88B")
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }

        val paint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }

        val pointPaint = Paint().apply {
            color = Color.TRANSPARENT
            strokeWidth = 3f
            style = Paint.Style.FILL
        }
        // 랜드마크 및 연결선을 그리기
        poseLandmarkResult.landmarks.forEach { landmark ->
            val x = resultBitmap.width - (landmark.x)
            val y = landmark.y
            canvas.drawPoint(x, y, pointPaint)
        }


        val nose = poseLandmarkResult.landmarks.getOrNull(0)

        // ------# 수칙  & 수평 축 넣기 #------
        when (sequence) {
            0 -> {
                val leftFoot = poseLandmarkResult.landmarks.getOrNull(27) // 왼발 좌표
                val rightFoot = poseLandmarkResult.landmarks.getOrNull(28) // 오른발 좌표
                val leftShoulder = poseLandmarkResult.landmarks.getOrNull(11)
                val rightShoulder = poseLandmarkResult.landmarks.getOrNull(12)
                val leftElbow = poseLandmarkResult.landmarks.getOrNull(13)
                val rightElbow = poseLandmarkResult.landmarks.getOrNull(14)
                val leftWrist = poseLandmarkResult.landmarks.getOrNull(15)
                val rightWrist = poseLandmarkResult.landmarks.getOrNull(16)
                val leftKnee = poseLandmarkResult.landmarks.getOrNull(25)
                val rightKnee = poseLandmarkResult.landmarks.getOrNull(26)
                val leftAnkle = poseLandmarkResult.landmarks.getOrNull(27)
                val rightAnkle = poseLandmarkResult.landmarks.getOrNull(28)

                if ( nose != null
                    && leftFoot != null && rightFoot != null  && leftShoulder != null && rightShoulder != null
                    && leftElbow != null && rightElbow != null && leftWrist != null && rightWrist != null
                    && leftKnee != null && rightKnee != null && leftAnkle != null && rightAnkle != null ) {
                    val midFootX = (leftFoot.x + rightFoot.x) / 2
                    canvas.drawLine(midFootX, leftFoot.y + 100, midFootX, nose.y - 100, axisPaint)

                    canvas.drawLine(leftShoulder.x ,leftShoulder.y, rightShoulder.x, rightShoulder.y, axisSubPaint)
                    canvas.drawLine(leftElbow.x ,leftElbow.y, rightElbow.x, rightElbow.y, axisSubPaint)
                    canvas.drawLine(leftWrist.x ,leftWrist.y, rightWrist.x, rightWrist.y, axisSubPaint)
                    canvas.drawLine(leftKnee.x ,leftKnee.y, rightKnee.x, rightKnee.y, axisSubPaint)
                    canvas.drawLine(leftAnkle.x ,leftAnkle.y, rightAnkle.x, rightAnkle.y, axisSubPaint)
                }
            }
            2 -> {

            }
            3 -> {
                val leftFoot = poseLandmarkResult.landmarks.getOrNull(27) // 왼발 좌표
                val leftShoulder = poseLandmarkResult.landmarks.getOrNull(11)
                val leftWrist = poseLandmarkResult.landmarks.getOrNull(15)
                val leftHip = poseLandmarkResult.landmarks.getOrNull(23)
                val leftKnee = poseLandmarkResult.landmarks.getOrNull(25)
                if (leftFoot != null && nose != null  && leftShoulder != null && leftWrist != null && leftHip != null && leftKnee != null) {
                    canvas.drawLine(leftFoot.x, leftFoot.y + 50, leftFoot.x, nose.y - 200, axisPaint)
                    canvas.drawLine(nose.x, nose.y, leftShoulder.x, leftShoulder.y, axisSubPaint)
                    canvas.drawLine(nose.x - 100, nose.y, nose.x + 100, nose.y , axisPaint)
                    canvas.drawLine(leftShoulder.x - 100, leftShoulder.y , leftShoulder.x + 100, leftShoulder.y , axisPaint)
                    canvas.drawLine(leftWrist.x, leftWrist.y, leftFoot.x, leftWrist.y, axisSubPaint)
                    canvas.drawLine(leftFoot.x, leftHip.y, leftHip.x, leftHip.y, axisSubPaint)
                    canvas.drawLine(leftFoot.x, leftKnee.y, leftKnee.x, leftKnee.y, axisSubPaint)
                }
            }
            4 -> {
                val rightFoot = poseLandmarkResult.landmarks.getOrNull(28) // 오른발 좌표
                val rightShoulder = poseLandmarkResult.landmarks.getOrNull(12)
                val rightWrist = poseLandmarkResult.landmarks.getOrNull(16)
                val rightHip = poseLandmarkResult.landmarks.getOrNull(24)
                val rightKnee = poseLandmarkResult.landmarks.getOrNull(26)
                if (rightFoot != null && nose != null && rightShoulder != null && rightWrist != null && rightHip != null && rightKnee != null) {
                    canvas.drawLine(rightFoot.x, rightFoot.y + 50, rightFoot.x, nose.y - 200, axisPaint)
                    canvas.drawLine(nose.x - 100, nose.y, nose.x + 100, nose.y , axisPaint)
                    canvas.drawLine(nose.x, nose.y, rightShoulder.x, rightShoulder.y, axisSubPaint)
                    canvas.drawLine(rightShoulder.x - 100, rightShoulder.y , rightShoulder.x + 100, rightShoulder.y , axisPaint)
                    canvas.drawLine(rightFoot.x, rightHip.y, rightHip.x, rightHip.y, axisSubPaint)
                    canvas.drawLine(rightWrist.x, rightWrist.y, rightFoot.x, rightWrist.y, axisSubPaint)
                    canvas.drawLine(rightFoot.x, rightKnee.y, rightKnee.x, rightKnee.y, axisSubPaint)
                }
            }
            5 -> {
                val leftFoot = poseLandmarkResult.landmarks.getOrNull(27) // 왼발 좌표
                val rightFoot = poseLandmarkResult.landmarks.getOrNull(28) // 오른발 좌표
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

                if (leftFoot != null && rightFoot != null && nose != null && leftKnee != null && rightKnee != null
                    && leftShoulder != null && rightShoulder !=null && leftElbow != null && rightElbow != null
                    && leftWrist != null && rightWrist != null && leftAnkle != null && rightAnkle != null
                    && leftHip != null && rightHip != null) {
                    val midFootX = (leftFoot.x + rightFoot.x) / 2
                    canvas.drawLine(midFootX, leftFoot.y + 100, midFootX, nose.y - 100, axisPaint)
                    canvas.drawLine(leftShoulder.x - 100, leftShoulder.y, rightShoulder.x + 100, rightShoulder.y , axisSubPaint)
                    canvas.drawLine(leftHip.x - 100, leftHip.y, rightHip.x + 100, rightHip.y , axisSubPaint)
                    canvas.drawLine(leftShoulder.x - 100, leftShoulder.y, rightShoulder.x + 100, rightShoulder.y , axisSubPaint)
                    canvas.drawLine(leftKnee.x - 100, leftKnee.y, rightKnee.x + 100, rightKnee.y , axisSubPaint)

                    val midShoulderX = (leftShoulder.x + rightShoulder.x) / 2
                    val midShoulderY = (leftShoulder.y + rightShoulder.y) / 2
                    val midHipX = (leftHip.x + rightHip.x) / 2
                    val midHipY = (leftHip.y + rightHip.y) / 2
                    canvas.drawLine(midShoulderX, midShoulderY, midHipX, midHipY , axisSubPaint)
                    canvas.drawLine(midShoulderX, midShoulderY, nose.x, nose.y , axisSubPaint)
                }
            }
            6 -> {
                val leftFoot = poseLandmarkResult.landmarks.getOrNull(27) // 왼발 좌표
                val rightFoot = poseLandmarkResult.landmarks.getOrNull(28) // 오른발 좌표
                val leftShoulder = poseLandmarkResult.landmarks.getOrNull(11)
                val rightShoulder = poseLandmarkResult.landmarks.getOrNull(12)
                val leftHip = poseLandmarkResult.landmarks.getOrNull(23)
                val rightHip = poseLandmarkResult.landmarks.getOrNull(24)

                if (nose != null && leftShoulder != null && rightShoulder != null && leftHip != null && rightHip != null && leftFoot != null && rightFoot != null) {
                    val noseX = nose.x
                    val noseY = nose.y
                    val leftShoulderX = leftShoulder.x
                    val leftShoulderY = leftShoulder.y
                    val rightShoulderX = rightShoulder.x
                    val rightShoulderY = rightShoulder.y
                    canvas.drawLine(noseX, noseY, leftShoulderX, leftShoulderY, paint)
                    canvas.drawLine(noseX, noseY, rightShoulderX, rightShoulderY, paint)

                    val midShoulderX = (leftShoulder.x + rightShoulder.x) / 2
                    val midShoulderY = (leftShoulder.y + leftShoulder.y) / 2
                    canvas.drawLine(midShoulderX, midShoulderY, noseX, noseY, axisSubPaint)

                    val midHipX = ((leftHip.x + rightHip.x) / 2 )
                    val midHipY = ((leftHip.y + rightHip.y) / 2  )
                    canvas.drawLine(midHipX, midHipY, leftShoulderX, leftShoulderY, paint)
                    canvas.drawLine(midHipX, midHipY, rightShoulderX, rightShoulderY, paint)
                    canvas.drawLine(midHipX, midHipY, midShoulderX, midShoulderY, axisSubPaint)
                    // 골반 다이아 부터 머리 다이아까지
                    val midFootY = ((leftFoot.y + rightFoot.y) / 2  )
                    canvas.drawLine(midHipX, midFootY + 100, noseX, noseY - 100, axisPaint)
                }
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
        val connections = when (sequence) {
            0 -> listOf(
                Pair(7, 8), // Pair(11, 12), // 귀
                Pair(15, 17), Pair(15, 19), Pair(15, 21), // 왼팔
                Pair(16, 18), Pair(16, 20), Pair(16, 22), // 오른팔
                Pair(11, 13), Pair(13, 15), Pair(12, 14), Pair(14, 16), // 팔 연결
                Pair(23, 24), Pair(11, 23), Pair(23, 25), Pair(25, 27), // 왼쪽 다리
                Pair(12, 24), Pair(24, 26), Pair(26, 28),
//                Pair(27, 31), Pair(28, 32)
            )
            2 -> listOf(
                Pair(7, 8), Pair(11, 12), // 귀
                Pair(15, 17), Pair(15, 19), Pair(15, 21), // 왼팔
                Pair(16, 18), Pair(16, 20), Pair(16, 22), // 오른팔
                Pair(11, 13), Pair(13, 15), Pair(12, 14), Pair(14, 16), // 팔 연결
            )
            3 -> listOf(
                Pair(15, 17), Pair(15, 19), Pair(15, 21), Pair(11, 13), Pair(13, 15),
                Pair(11, 23), Pair(23, 25), Pair(25, 27),
                Pair(27, 31), Pair(27, 29)
            )
            4 -> listOf(
                Pair(16, 18), Pair(16, 20), Pair(16, 22), Pair(12, 14), Pair(14, 16),
                Pair(12, 24), Pair(24, 26), Pair(26, 28),
                Pair(28, 32), Pair(28, 30)
            )
            5 -> listOf(
                Pair(7, 8), Pair(11, 12), // 귀
                Pair(15, 17), Pair(15, 19), Pair(15, 21), // 왼팔
                Pair(16, 18), Pair(16, 20), Pair(16, 22), // 오른팔
                Pair(11, 13), Pair(13, 15), Pair(12, 14), Pair(14, 16), // 팔 연결
                Pair(23, 24), Pair(11, 23), Pair(23, 25), Pair(25, 27), // 왼쪽 다리
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
        resultBitmap.scale(-1, 1)
        return resultBitmap
    }

    fun decodeSampledBitmapFromFile(filePath: String, reqWidth: Int, reqHeight: Int) : Bitmap {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(filePath, options)

        options.inSampleSize = calcuateSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        var bitmap = BitmapFactory.decodeFile(filePath, options)

        bitmap = rotateImageIfRequired(filePath, bitmap)
        bitmap = Bitmap.createScaledBitmap(bitmap, reqWidth, reqHeight, true)
        return bitmap
    }

    fun calcuateSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val hlafWidth : Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && hlafWidth / inSampleSize >= reqHeight) {
                inSampleSize *=2

            }
        }
        return inSampleSize
    }

    private fun rotateImageIfRequired(filePath: String, bitmap: Bitmap): Bitmap {
        val ei = ExifInterface(filePath)
        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        return when {
            orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                    (bitmap.width < bitmap.height && orientation == ExifInterface.ORIENTATION_NORMAL) -> rotateImage(bitmap, 90f)

            else -> bitmap
        }
    }
    private fun rotateImage(bitmap: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
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

}