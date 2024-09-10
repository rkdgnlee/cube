package com.tangoplus.tangoq.mediapipe

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import androidx.core.content.ContextCompat
import com.tangoplus.tangoq.R

object ImageProcessingUtility {

    fun combineImageAndOverlay(
        originalBitmap: Bitmap,
        poseLandmarkResult: PoseLandmarkResult,
        scaleFactor: Float,
        offsetX: Float,
        offsetY: Float,
        context: android.content.Context
    ): Bitmap {
        // 원본 비트맵을 복사하고, 그 위에 오버레이(랜드마크)를 그린다
        val resultBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)

        val paint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.white)
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
            Log.v("resultt2", "$landmark")
        }

        val connections = listOf(
            Pair(7, 8), Pair(11, 12), // 귀
            Pair(15, 17), Pair(15, 19), Pair(15, 21), // 왼팔
            Pair(16, 18), Pair(16, 20), Pair(16, 22), // 오른팔
            Pair(11, 13), Pair(13, 15), Pair(12, 14), Pair(14, 16), // 팔 연결
            Pair(23, 24), Pair(11, 23), Pair(23, 25), Pair(25, 27), // 왼쪽 다리
            Pair(12, 24), Pair(24, 26), Pair(26, 28)  // 오른쪽 다리
        )

        // 안전하게 특정 랜드마크 접근
        val nose = poseLandmarkResult.landmarks.getOrNull(0)
        val leftShoulder = poseLandmarkResult.landmarks.getOrNull(11)
        val rightShoulder = poseLandmarkResult.landmarks.getOrNull(12)

        // 코와 어깨 중간점 연결선 그리기 (모든 필요한 점이 있을 때만)
        if (nose != null && leftShoulder != null && rightShoulder != null) {
            val noseX = nose.x + offsetX
            val noseY = nose.y + offsetY
            val midShoulderX = (leftShoulder.x + rightShoulder.x) / 2   + offsetX
            val midShoulderY = (leftShoulder.y + rightShoulder.y) / 2   + offsetY

            canvas.drawLine(noseX, noseY, midShoulderX, midShoulderY, paint)
            Log.v("canvasResultLandmarks3", "${noseX} $noseY, $midShoulderX, $midShoulderY")
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
            Log.v("resultt3", "startLandmark: $startLandmark endLandmark: $endLandmark")
        }

        return resultBitmap
    }
}