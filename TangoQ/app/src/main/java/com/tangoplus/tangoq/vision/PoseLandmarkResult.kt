package com.tangoplus.tangoq.vision


data class PoseLandmarkResult(val landmarks : List<PoseLandmark>) {
    data class PoseLandmark(
        var x: Float,
        var y: Float,
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
