package com.tangoplus.tangoq.mediapipe

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.util.Log
import androidx.core.content.ContextCompat
import com.bumptech.glide.load.resource.bitmap.TransformationUtils.rotateImage
import com.tangoplus.tangoq.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ImageProcessingUtility {


    fun combineImageAndOverlay(
        originalBitmap: Bitmap,
        poseLandmarkResult: PoseLandmarkResult,
        scaleFactorX: Float,
        scaleFactorY: Float,
        offSetX: Float,
        offSetY: Float,
        context: android.content.Context,
        sequence: Int,
    ): Bitmap {
        Log.v("스케일과오프셋", "scaleFactor: (${scaleFactorX}, ${scaleFactorY}), offset: ($offSetX, $offSetY)")

        val matrix = Matrix().apply {
            preScale(-1f, 1f)
        }
        val flippedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
        val resultBitmap = flippedBitmap .copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)
        val axisPaint = Paint().apply {
            color = Color.parseColor("#FF5449")
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }
        val paint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }

        val pointPaint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.subColor400)
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

        // ------# 축 넣기 #------
        when (sequence) {
            0 -> {
                val leftFoot = poseLandmarkResult.landmarks.getOrNull(27) // 왼발 좌표
                val rightFoot = poseLandmarkResult.landmarks.getOrNull(28) // 오른발 좌표
                if (leftFoot != null && rightFoot != null && nose != null) {
                    val midFootX = (leftFoot.x + rightFoot.x) / 2
                    canvas.drawLine(midFootX, leftFoot.y, midFootX, nose.y, axisPaint)
                }
            }
            2 -> {

            }
            3 -> {
                val leftFoot = poseLandmarkResult.landmarks.getOrNull(27) // 왼발 좌표
                if (leftFoot != null && nose != null) {
                    val leftFootX = leftFoot.x
                    canvas.drawLine(leftFootX, leftFoot.y, leftFootX, nose.y, axisPaint)
                }
            }
            4 -> {
                val rightFoot = poseLandmarkResult.landmarks.getOrNull(28) // 오른발 좌표
                if (rightFoot != null && nose != null) {
                    val rightFootX = rightFoot.x
                    canvas.drawLine(rightFootX, rightFoot.y, rightFootX, nose.y, axisPaint)
                }
            }
            5 -> {
                val leftFoot = poseLandmarkResult.landmarks.getOrNull(27) // 왼발 좌표
                val rightFoot = poseLandmarkResult.landmarks.getOrNull(28) // 오른발 좌표
                if (leftFoot != null && rightFoot != null && nose != null) {
                    val midFootX = (leftFoot.x + rightFoot.x) / 2
                    canvas.drawLine(midFootX, leftFoot.y, midFootX, nose.y, axisPaint)
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
                    val leftShoulderX = (leftShoulder.x )
                    val leftShoulderY = (leftShoulder.y)
                    val rightShoulderX = (rightShoulder.x  )
                    val rightShoulderY = (rightShoulder.y)
                    canvas.drawLine(noseX, noseY, leftShoulderX, leftShoulderY, paint)
                    canvas.drawLine(noseX, noseY, rightShoulderX, rightShoulderY, paint)

                    val midHipX = ((leftHip.x + rightHip.x) / 2 )
                    val midHipY = ((leftHip.y + rightHip.y) / 2  )
                    canvas.drawLine(midHipX, midHipY, leftShoulderX, leftShoulderY, paint)
                    canvas.drawLine(midHipX, midHipY, rightShoulderX, rightShoulderY, paint)

                    // 골반 다이아 부터 머리 다이아까지
                    val midFootY = ((leftFoot.y + rightFoot.y) / 2  )
                    canvas.drawLine(midHipX, rightFoot.y, noseX, midFootY, axisPaint)
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
                Pair(7, 8), Pair(11, 12), // 귀
                Pair(15, 17), Pair(15, 19), Pair(15, 21), // 왼팔
                Pair(16, 18), Pair(16, 20), Pair(16, 22), // 오른팔
                Pair(11, 13), Pair(13, 15), Pair(12, 14), Pair(14, 16), // 팔 연결
                Pair(23, 24), Pair(11, 23), Pair(23, 25), Pair(25, 27), // 왼쪽 다리
                Pair(12, 24), Pair(24, 26), Pair(26, 28),  // 오른쪽 다리
                Pair(27, 31), Pair(28, 32), Pair(27, 31), Pair(28, 32)
            )
            2 -> listOf(
                Pair(7, 8), Pair(11, 12), // 귀
                Pair(15, 17), Pair(15, 19), Pair(15, 21), // 왼팔
                Pair(16, 18), Pair(16, 20), Pair(16, 22), // 오른팔
                Pair(11, 13), Pair(13, 15), Pair(12, 14), Pair(14, 16), // 팔 연결
            )
            3 ->  listOf(
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