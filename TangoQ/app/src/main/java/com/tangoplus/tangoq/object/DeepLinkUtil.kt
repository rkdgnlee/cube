package com.tangoplus.tangoq.`object`

import android.util.Base64
import com.tangoplus.tangoq.data.ExerciseVO

object DeepLinkUtil {
    fun encodeExercise(exercise: ExerciseVO): String {
        val data = "${exercise.exerciseId},${exercise.exerciseName}"
        return Base64.encodeToString(data.toByteArray(), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun decodeExercise(encodedString: String): ExerciseVO? {
        return try {
            val decodedBytes = Base64.decode(encodedString, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            val decodedString = String(decodedBytes)
            val (id, name) = decodedString.split(",")
            ExerciseVO(id, name)
        } catch (e: Exception) {
            null
        }
    }
}