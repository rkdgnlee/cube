package com.tangoplus.tangoq.api

import android.content.Context
import android.util.Log
import com.tangoplus.tangoq.vo.ExerciseVO
import com.tangoplus.tangoq.vo.ProgramVO
import com.tangoplus.tangoq.function.SecurePreferencesManager.getEncryptedAccessJwt
import com.tangoplus.tangoq.api.HttpClientProvider.getClient
import com.tangoplus.tangoq.api.NetworkExercise.jsonToExerciseVO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.Request
import org.json.JSONObject

object NetworkProgram {
    suspend fun fetchProgram(myUrl: String, context: Context, sn: String): ProgramVO? {
        val client = getClient(context)
        val request = Request.Builder()
            .url("$myUrl$sn")
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.v("responseBody", "$responseBody")
                    val jsonInfo = responseBody?.let { JSONObject(it) }
                    val exercises = mutableListOf<ExerciseVO>()
                    var exerciseTimes = 0
                    val exerciseSize = jsonInfo?.optString("exercise_ids")?.split(",")?.count()
                    if (exerciseSize != null) {
                        for (i in 0 until exerciseSize) {
                            val exerciseUnit = jsonToExerciseVO((responseBody.let { JSONObject(it) }.getJSONObject("$i")))
                            exercises.add(exerciseUnit)
                            exerciseTimes += exerciseUnit.duration?.toInt() ?: 0
                        }
//                    Log.v("프로그램Exercises", "exerciseSize: $exerciseSize, exerciseTime: ${exerciseTimes}")
                        val programVO = ProgramVO(
                            programSn = sn.toInt(),
                            programName = jsonInfo.optString("exercise_program_title"),
                            programTime = exerciseTimes,
                            programStage = jsonInfo.optInt("exercise_stage").toString(),
                            programCount = exerciseSize.toString(),
                            programFrequency = jsonInfo.optInt("exercise_frequency"),
                            programWeek = jsonInfo.optInt("required_week"),
//                        exerciseTime = exerciseTimes,
                            exercises = exercises
                        )
                        return@use programVO
                    }
                    return@use null
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("ProgramError", "IndexOutOfBounds: ${e.message}")
                null
            } catch (e: IllegalArgumentException) {
                Log.e("ProgramError", "IllegalArgument: ${e.message}")
                null
            } catch (e: IllegalStateException) {
                Log.e("ProgramError", "IllegalState: ${e.message}")
                null
            }catch (e: NullPointerException) {
                Log.e("ProgramError", "NullPointer: ${e.message}")
                null
            } catch (e: java.lang.Exception) {
                Log.e("ProgramError", "Exception: ${e}")
                null
            }
        }
    }


}