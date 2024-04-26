//package com.example.mhg.Room
//
//import com.example.mhg.VO.HomeRVBeginnerDataClass
//import org.json.JSONArray
//
//class ExerciseRepository(private val exerciseDao: ExerciseDao) {
//    suspend fun getExerciseDataByType(exerciseType: String): MutableList<HomeRVBeginnerDataClass> {
//        return exerciseDao.findByType(exerciseType).map {exercise ->
//            HomeRVBeginnerDataClass(
//                imgUrl = null,
//                exerciseName = exercise.exercise_name,
//                exerciseDescription = exercise.exercise_description,
//                relatedJoint = exercise.related_joint,
//                relatedMuscle = exercise.related_muscle,
//                relatedSymptom = exercise.related_symptom,
//                exerciseStage = exercise.exercise_stage,
//                exerciseFequency = exercise.exercise_frequency,
//                exerciseIntensity = exercise.exercise_intensity,
//                exerciseInitialPosture = exercise.exercise_initial_posture,
//                exerciseMethod = exercise.exercise_method,
//                exerciseCaution = exercise.exercise_caution,
//                videoAlternativeName = exercise.video_alternative_name,
//                videoFilepath = exercise.video_filepath,
//                videoTime = exercise.video_time,
//                exerciseTypeId = exercise.exercise_type_id,
//                exerciseTypeName = exercise.exercise_type_name
//            )
//        }.toMutableList()
//    }
//
//    suspend fun getHomeRVBeginnerData(): MutableList<HomeRVBeginnerDataClass> {
//        return exerciseDao.getAll().map { exercise ->
//            HomeRVBeginnerDataClass(
//                imgUrl = null,  // 이미지 URL은 데이터베이스에 없으므로 null 또는 기본 이미지 URL을 설정
//                exerciseName = exercise.exercise_name,
//                exerciseDescription = exercise.exercise_description,
//                relatedJoint = exercise.related_joint,
//                relatedMuscle = exercise.related_muscle,
//                relatedSymptom = exercise.related_symptom,
//                exerciseStage = exercise.exercise_stage,
//                exerciseFequency = exercise.exercise_frequency,
//                exerciseIntensity = exercise.exercise_intensity,
//                exerciseInitialPosture = exercise.exercise_initial_posture,
//                exerciseMethod = exercise.exercise_method,
//                exerciseCaution = exercise.exercise_caution,
//                videoAlternativeName = exercise.video_alternative_name,
//                videoFilepath = exercise.video_filepath,
//                videoTime = exercise.video_time,
//                exerciseTypeId = exercise.exercise_type_id,
//                exerciseTypeName = exercise.exercise_type_name
//            )
//        }.toMutableList()
//    }
//    suspend fun StoreExercises(jsonArr: JSONArray) {
//        for (i in 0 until jsonArr.length()) {
//            val jsonObj = jsonArr.getJSONObject(i)
//
//            // -----! 각 JSONObject의 이름 존재 체크 로직 시작 !-----
//            val name = jsonObj.getString("exercise_name")
//            val existExercise = exerciseDao.findByName(name)
//            // -----! 없을 때 insert !-----
//            if (existExercise == null) {
//                val exercise = Exercise(
//                    id = 0, // Room will auto-generate this
//                    exercise_name = jsonObj.getString("exercise_name"),
//                    exercise_description = jsonObj.getString("exercise_description"),
//                    related_joint = jsonObj.getString("related_joint"),
//                    related_muscle = jsonObj.getString("related_muscle"),
//                    related_symptom = jsonObj.getString("related_symptom"),
//                    exercise_stage = jsonObj.getString("exercise_stage"),
//                    exercise_frequency = jsonObj.getString("exercise_frequency"),
//                    exercise_intensity = jsonObj.getString("exercise_intensity"),
//                    exercise_initial_posture = jsonObj.getString("exercise_initial_posture"),
//                    exercise_method = jsonObj.getString("exercise_method"),
//                    exercise_caution = jsonObj.getString("exercise_caution"),
//                    video_alternative_name = jsonObj.getString("video_alternative_name"),
//                    video_filepath = jsonObj.getString("video_filepath"),
//                    video_time = jsonObj.getString("video_time"),
//                    exercise_type_id = jsonObj.getString("exercise_type_id"),
//                    exercise_type_name = jsonObj.getString("exercise_type_name"),
//                )
//                exerciseDao.insert(exercise)
//            }
//            // -----! 각 JSONObject의 이름 존재 체크 로직 끝 !-----
//        }
//    }
//}