package com.tangoplus.tangoq.`object`

import android.content.Context
import android.util.Log
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.data.ProgressUnitVO
import com.tangoplus.tangoq.`object`.NetworkProgress.getProgressInCurrentProgram
import com.tangoplus.tangoq.`object`.NetworkProgress.postProgressInCurrentProgram
import com.tangoplus.tangoq.`object`.NetworkRecommendation.createRecommendProgram
import com.tangoplus.tangoq.`object`.NetworkRecommendation.getRecommendProgram
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SaveSingletonManager(private val context: Context) {
    private val singletonMeasure = Singleton_t_measure.getInstance(context)
    private val singletonProgress = Singleton_t_progress.getInstance(context)

    fun getMeasures(userSn: String, scope: CoroutineScope, callbacks: () -> Unit) {
        scope.launch {
            /* TODO 현재는 더미. 그런데 여기서 기존에 recommendations를 불러오면서 measures를 불러오기 .그러면서 measures에 recommendations를 담아놓고 사용하는거지.
            *  TODO 그러면 측정이 완료됐을 때도, 그렇게 넣어서 만들어. 그 다음에
            *
            * */

//            fetchMeasureResults7(getString(R.string.API_measure)) // 이 측정결과 가져오는 것도 콜백보다 return이 좋을거 같은디..
            CoroutineScope(Dispatchers.IO).launch {
                val mjo1 = JSONArray().apply {
                    put(loadJsonData())
                    put(loadJsonArray())
                    put(loadJsonData())
                    put(loadJsonData())
                    put(loadJsonData())
                    put(loadJsonData())
                    put(loadJsonData())
                }
                val parts1 = mutableListOf<Pair<String, Int>>()
                parts1.add(Pair("어깨", 2))
                parts1.add(Pair("목관절", 2))
                parts1.add(Pair("손목", 1))
                parts1.add(Pair("무릎", 1))

                val measureVO1 = MeasureVO(
                    "1",
                    "2024-08-20 20:12:08",
                    86,
                    parts1,
                    mjo1,
                    // 총 7개
                    mutableListOf(
                        getUrl("MT_STATIC_BACK_61_20240604143755.jpg", true),
                        getUrl("MT_DYNAMIC_OVERSQUAT_FRONT_1_1_20240606135241.mp4", false),
                        getUrl("MT_STATIC_BACK_61_20240604143755.jpg", true),
                        getUrl("MT_STATIC_BACK_61_20240604143755.jpg", true),
                        getUrl("MT_STATIC_BACK_61_20240604143755.jpg", true),
                        getUrl("MT_STATIC_BACK_61_20240604143755.jpg", true),
                        getUrl("MT_STATIC_BACK_61_20240604143755.jpg", true)
                    ),
                    false,
                    mutableListOf()
                )
                Log.v("measureVO1", "${measureVO1.fileUris}")

                val parts2 = mutableListOf<Pair<String, Int>>()
                parts2.add(Pair("목관절", 2))
                parts2.add(Pair("어깨", 1))
                parts2.add(Pair("손목", 1))
                parts2.add(Pair("골반", 1))

                val measureVO2 = MeasureVO(
                    "2",
                    "2024-08-01 17:55:21",
                    79,
                    parts2,
                    JSONArray(),
                    mutableListOf(),
                    false,
                    mutableListOf()
                )
                val measureVO3 = MeasureVO(
                    "3",
                    "2024-08-06 17:55:21",
                    79,
                    parts2,
                    JSONArray(),
                    mutableListOf(),
                    false,
                    mutableListOf()
                )

                // ------# 싱글턴 측정 결과 init #------
                singletonMeasure.measures = mutableListOf()
                singletonMeasure.measures?.add(measureVO1)
                singletonMeasure.measures?.add(measureVO2)
//                singletonMeasure.measures?.add(measureVO3)

                // ------# 측정 1개 -> 추천 여러개 가져오기 #------

                val recommendations = getRecommendProgram(context.getString(R.string.API_recommendation), measureVO1.measureId.toInt(), context)
                if (recommendations.isEmpty()) {
                    val recommendJson = JSONObject()

                    // TODO 더미임. 이곳에는 recommendation을 하기 위한 종합 점수 산출이 필요함.
                    val ja1 = JSONArray()
                    ja1.apply {
                        put(3)
                        put(1)
                        put(2)
                    }
                    val ja2 = JSONArray()
                    ja2.apply {
                        put(0)
                        put(1)
                        put(2)
                    }
                    recommendJson.put("user_sn", userSn.toInt())
                    recommendJson.put("exercise_type_id", ja1)
                    recommendJson.put("exercise_stage", ja2)
                    recommendJson.put("measure_sn", measureVO1.measureId)

                    createRecommendProgram(context.getString(R.string.API_recommendation), recommendJson.toString(), context) { recommendations ->
                        val measure = singletonMeasure.measures?.get(0)
                        measure?.recommendations = recommendations
                        if (measure != null) {
                            singletonMeasure.measures!![0] = measure
                        }
                        Log.v("recommendations", "${singletonMeasure.measures?.get(0)?.recommendations}")
                        callbacks()

                    }
                } else {
                    val measure = singletonMeasure.measures?.get(0)
                    measure?.recommendations = recommendations
                    if (measure != null) {
                        singletonMeasure.measures!![0] = measure
                    }
                    Log.v("recommendations", "${singletonMeasure.measures?.get(0)?.recommendations}")
                    callbacks()
                }



            }
            // ------# 더미 측정 결과 #------
        }
    }

    suspend fun getOrInsertProgress(jo: JSONObject) : Unit = suspendCoroutine  { continuation ->
        postProgressInCurrentProgram(context.getString(R.string.API_progress), jo, context) { progressUnits -> // MutableList<ProgressUnitVO>
            if (progressUnits.isNotEmpty()) {
                Log.v("프로그레스유닛들", "${progressUnits}")
                val weeks = 1..progressUnits.maxOf { it.currentWeek } // 4
                val requiredSequences = 1..progressUnits[0].requiredSequence // 3
                val organizedUnits = mutableListOf<MutableList<ProgressUnitVO>>() // 1이 속한 12개의 seq, 21개의 progressUnits

                for (week in weeks) { // 1, 2, 3, 4
                    val weekUnits = progressUnits.filter { it.currentWeek == week }
                    val groupedByUvpSn = weekUnits.groupBy { it.uvpSn } // 21개임
                    val maxCurrentSequence = weekUnits.maxOfOrNull { it.currentSequence } ?: 0
                    for (seq in requiredSequences) { // 1, 2, 3
                        val orgUnit = mutableListOf<ProgressUnitVO>() // 시퀀스별로 새로운 리스트 생성
                        for ((_, units) in groupedByUvpSn) {
                            val unit = units.firstOrNull() ?: continue
                            val currentProgress = when {
                                seq - 1 < maxCurrentSequence -> unit.lastProgress

                                else -> 0  // 미래 시퀀스
                            }

                            orgUnit.add(unit.copy(
                                currentSequence = (seq - 1),
                                lastProgress = currentProgress
                            ))
                        }
                        organizedUnits.add(orgUnit) // 각 시퀀스마다 리스트 추가
                    }
                }

                singletonProgress.progresses = organizedUnits
                Log.v("singletonProgress", "${singletonProgress.progresses!![0].map { it.lastProgress }}, ${singletonProgress.progresses!![1].map { it.lastProgress }}, ${singletonProgress.progresses!![2].map { it.lastProgress }}, ${singletonProgress.progresses!![3].map { it.lastProgress }}, ")
                continuation.resume(Unit)
            } else {
                singletonProgress.progresses?.add(mutableListOf())
                continuation.resume(Unit)
            }
        }
    }


    // ------# 사진 영상 pose 가져오기 #------
    private suspend fun loadJsonData(): JSONObject = withContext(Dispatchers.IO) {
        val jsonString = context.assets.open("MT_STATIC_BACK_6_1_20240604143755.json").bufferedReader()
            .use { it.readText() }
        JSONObject(jsonString)
    }

    private suspend fun loadJsonArray() : JSONArray = withContext(Dispatchers.IO) {
        val jsonString = context.assets.open("MT_DYNAMIC_OVERHEADSQUAT_FRONT_1_1_20240606135241.json").bufferedReader().use { it.readText() }
        JSONArray(jsonString)
    }
    private suspend fun getUrl(fileName: String, isImage: Boolean) : String = withContext(
        Dispatchers.IO) {
        val fileExtension = if (isImage) ".jpg" else ".mp4"
        val tempFile = File.createTempFile(if (isImage) "temp_image" else "temp_video", fileExtension, context.cacheDir)
        tempFile.deleteOnExit()

        context.assets.open(fileName).use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        tempFile.absolutePath
    }
}