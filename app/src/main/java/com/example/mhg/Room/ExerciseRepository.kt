package com.example.mhg.Room

import com.example.mhg.R
import com.example.mhg.`object`.NetworkService
import org.json.JSONArray
import org.json.JSONObject

class ExerciseRepository(private val exerciseDao: ExerciseDao, private val networkService: NetworkService) {
    suspend fun getExercises(): List<Exercise> {
        return exerciseDao.getAll()
    }

    suspend fun StoreExercises(jsonArr: JSONArray) {
        for (i in 0 until jsonArr.length()) {
            val jsonObj = jsonArr.getJSONObject(i)

            // -----! 각 JSONObject의 이름 존재 체크 로직 시작 !-----
            val name = jsonObj.getString("exercise_name")
            val existExercise = exerciseDao.findByName(name)
            // -----! 없을 때 insert !-----
            if (existExercise == null) {
                val exercise = Exercise(
                    id = 0, // Room will auto-generate this
                    exercise_name = jsonObj.getString("exercise_name"),
                    exercise_description = jsonObj.getString("exercise_description"),
                    related_joint = jsonObj.getString("related_joint"),
                    related_muscle = jsonObj.getString("related_muscle"),
                    related_symptom = jsonObj.getString("related_symptom"),
                    exercise_stage = jsonObj.getString("exercise_stage"),
                    exercise_frequency = jsonObj.getString("exercise_frequency"),
                    exercise_intensity = jsonObj.getString("exercise_intensity"),
                    exercise_initial_posture = jsonObj.getString("exercise_initial_posture"),
                    exercise_method = jsonObj.getString("exercise_method"),
                    exercise_caution = jsonObj.getString("exercise_caution"),
                    video_alternative_name = jsonObj.getString("video_alternative_name"),
                    video_filepath = jsonObj.getString("video_filepath"),
                    video_time = jsonObj.getString("video_time")
                )
                exerciseDao.insert(exercise)
            }
            // -----! 각 JSONObject의 이름 존재 체크 로직 끝 !-----
        }

    }
}