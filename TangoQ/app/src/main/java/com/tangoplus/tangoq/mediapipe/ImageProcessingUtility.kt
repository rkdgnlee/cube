package com.tangoplus.tangoq.mediapipe

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
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
        scaleFactor: Float,
        offsetX: Float,
        offsetY: Float,
        context: android.content.Context,
        sequence: Int,
    ): Bitmap {



        // 원본 비트맵을 복사하고, 그 위에 오버레이(랜드마크)를 그린다
        val resultBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)
        val axisPaint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.deleteColor)
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
            val x = landmark.x  + offsetX
            val y = landmark.y  + offsetY
            canvas.drawPoint(x, y, pointPaint)
        }
        val nose = poseLandmarkResult.landmarks.getOrNull(0)

        if (sequence == 0 || sequence == 5 || sequence == 6) {
            val leftFoot = poseLandmarkResult.landmarks.getOrNull(27) // 왼발 좌표
            val rightFoot = poseLandmarkResult.landmarks.getOrNull(28) // 오른발 좌표
            if (leftFoot != null && rightFoot != null && nose != null) {
                val midFootX = (leftFoot.x + rightFoot.x) / 2 + offsetX
//                val canvasHeight = canvas.height.toFloat()
                canvas.drawLine(midFootX, leftFoot.y, midFootX, nose.y, axisPaint)
            }
        }
        when (sequence) {
            2 -> {

            }
            3 -> {
                val leftFoot = poseLandmarkResult.landmarks.getOrNull(27) // 왼발 좌표
                if (leftFoot != null && nose != null) {
                    val leftFootX = leftFoot.x + offsetX
                    val canvasHeight = canvas.height.toFloat()
                    canvas.drawLine(leftFootX, leftFoot.y, leftFootX, nose.y, axisPaint)
                }
            }
            4 -> {
                val rightFoot = poseLandmarkResult.landmarks.getOrNull(28) // 왼발 좌표
                if (rightFoot != null && nose != null) {
                    val rightFootX = rightFoot.x + offsetX
                    val canvasHeight = canvas.height.toFloat()
                    canvas.drawLine(rightFootX, rightFoot.y, rightFootX, nose.y, axisPaint)
                }
            }
            5 -> {

            }
            6 -> {
                val leftFoot = poseLandmarkResult.landmarks.getOrNull(27) // 왼발 좌표
                val rightFoot = poseLandmarkResult.landmarks.getOrNull(28) // 오른발 좌표
                val leftShoulder = poseLandmarkResult.landmarks.getOrNull(11)
                val rightShoulder = poseLandmarkResult.landmarks.getOrNull(12)
                val leftHip = poseLandmarkResult.landmarks.getOrNull(23)
                val rightHip = poseLandmarkResult.landmarks.getOrNull(24)
                if (nose != null && leftShoulder != null && rightShoulder != null && leftHip != null && rightHip != null && leftFoot != null && rightFoot != null) {
                    val noseX = nose.x + offsetX
                    val noseY = nose.y + offsetY
                    val leftShoulderX = leftShoulder.x + offsetX
                    val leftShoulderY = leftShoulder.y + offsetY
                    val rightShoulderX = rightShoulder.x + offsetX
                    val rightShoulderY = rightShoulder.y +offsetY
                    canvas.drawLine(noseX, noseY, leftShoulderX, leftShoulderY, paint)
                    canvas.drawLine(noseX, noseY, rightShoulderX, rightShoulderY, paint)

                    val midHipX = (leftHip.x + rightHip.x) / 2   + offsetX
                    val midHipY = (leftHip.y + rightHip.y) / 2   + offsetY
                    canvas.drawLine(midHipX, midHipY, leftShoulderX, leftShoulderY, paint)
                    canvas.drawLine(midHipX, midHipY, rightShoulderX, rightShoulderY, paint)


                    // 골반 다이아 부터 머리 다이아까지
                    val midFootY = (leftFoot.y + rightFoot.y) / 2 + offsetX
                    canvas.drawLine(midHipX, rightFoot.y, noseX, midFootY, axisPaint)
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
            6 -> listOf(Pair(7, 8), Pair(11, 12), // 귀
                Pair(23, 24),
            )
            else -> listOf()
        }

        connections.forEach { (start, end) ->
            val startLandmark = poseLandmarkResult.landmarks.getOrNull(start)
            val endLandmark = poseLandmarkResult.landmarks.getOrNull(end)
            if (startLandmark != null && endLandmark != null) {
                val startX = startLandmark.x  + offsetX
                val startY = startLandmark.y + offsetY
                val endX = endLandmark.x  + offsetX
                val endY = endLandmark.y   + offsetY
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

}