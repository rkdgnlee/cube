package com.tangoplus.tangoq.mediapipe


data class PoseLandmarkResult(
    val landmarks : List<PoseLandmark>,

) {
    data class PoseLandmark(
        val x: Float,
        val y: Float,
    )

    companion object {
        fun fromCoordinates(coordinates: List<Pair<Float, Float>>?): PoseLandmarkResult {
            val landmarks = coordinates?.map { (x, y) ->
                PoseLandmark(x, y)
            }
            return if ( landmarks != null ) PoseLandmarkResult(landmarks) else PoseLandmarkResult(listOf())
        }
    }
}
