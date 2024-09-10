package com.tangoplus.tangoq.mediapipe

import android.util.Log

data class PoseLandmarkResult(
    val landmarks : List<PoseLandmark>,
    val videolandmarks : List<List<PoseLandmark>> = emptyList()
) {
    data class PoseLandmark(
        val x: Float,
        val y: Float,
    )

    companion object {
        fun fromImageCoordinates(coordinates: List<Pair<Float, Float>>): PoseLandmarkResult {
            val landmarks = coordinates.map { (x, y) ->
                PoseLandmark(x, y)
            }
            return PoseLandmarkResult(landmarks)
        }

        fun fromVideoCoordinates(coordinates: List<List<Pair<Float, Float>>>): PoseLandmarkResult {
            val landmarks = coordinates.map { coordinate ->
                coordinate.map { (x, y) ->
                    PoseLandmark(x, y)
                }
            }
            Log.v("videoLandmarks", "videoLandmarks: ${landmarks}")
            return PoseLandmarkResult(emptyList(), landmarks)
        }
    }

}
