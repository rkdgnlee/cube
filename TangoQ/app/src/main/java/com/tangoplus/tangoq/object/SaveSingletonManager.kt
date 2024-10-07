package com.tangoplus.tangoq.`object`

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.data.ProgressUnitVO
import com.tangoplus.tangoq.db.FileStorageUtil.getFile
import com.tangoplus.tangoq.db.FileStorageUtil.readJsonArrayFile
import com.tangoplus.tangoq.db.FileStorageUtil.readJsonFile
import com.tangoplus.tangoq.db.MeasureDatabase
import com.tangoplus.tangoq.db.MeasureInfo
import com.tangoplus.tangoq.`object`.NetworkMeasure.saveAllMeasureInfo
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

    fun getMeasures(userUUID: String, userInfoSn: Int ,userSn : Int, scope: CoroutineScope, callbacks: () -> Unit) {
        val md = MeasureDatabase.getDatabase(context)
        val mDao = md.measureDao()

        scope.launch {
            CoroutineScope(Dispatchers.IO).launch {

                saveAllMeasureInfo(context, context.getString(R.string.API_measure), userUUID, userSn)

                val allInfos = mDao.getAllInfo(userSn)
                val allStatics = mDao.getAllStatic(userSn)
                val allDynamics = mDao.getAllDynamic(userSn)
                Log.d("InsertDynamic", "allInfos: $allInfos, allStatics: $allStatics, allDynamics: $allDynamics")

                //  userSn에 맞는 모든 값들을 가져옴.
                val groupedInfos = allInfos.groupBy { entity -> entity.sn }
                val groupedStatics = allStatics.groupBy { entity -> entity.server_sn }
                val groupedDynamics = allDynamics.groupBy { entity -> entity.server_sn }
                val measures = mutableListOf<MeasureVO>()
                // ------# 모든 측정 기록을 전부 필터링하는 곳 #------

                for ((key, infoList) in groupedInfos) { // key가 sn, infoList가 각 infoList
                    val statics = groupedStatics[key]?.sortedBy { it.measure_seq } ?: emptyList()
                    val dynamic = groupedDynamics[key] ?: emptyList()

                    Log.v("스태틱다이나믹길이", "statics: ${statics.size}, dynamic: ${dynamic.size}")
                    val ja = JSONArray()
                    val uris = mutableListOf<String>()
                    val baseUrl = "https://gym.tangostar.co.kr/data/Results/"
                    for (i in 0 until 7) {
                        if (i == 0) {
                            val jsonFile = getFile(context, statics[0].measure_server_json_name.replace(baseUrl, ""))
                            val mediaFile = getFile(context, statics[0].measure_server_file_name.replace(baseUrl, ""))
                            Log.e("Files0", "jsonFile: $jsonFile, mediaFile: $mediaFile")
                            if (jsonFile != null && mediaFile != null) {
                                ja.put(readJsonFile(jsonFile))
                                uris.add(mediaFile.absolutePath)
                            } else {
                                Log.e("Files", "jsonFile: $jsonFile, mediaFile: $mediaFile")
                            }
                        } else if (i == 1) {
                            val jsonFile = getFile(context, dynamic[0].measure_server_json_name?.replace(baseUrl, "")!!)
                            val mediaFile = getFile(context, dynamic[0].measure_server_file_name?.replace(baseUrl, "")!!)
                            Log.e("File1", "jsonFile: $jsonFile, mediaFile: $mediaFile")
                            if (jsonFile != null && mediaFile != null) {
                                ja.put(readJsonArrayFile(jsonFile))
                                uris.add(mediaFile.absolutePath)
                            } else {
                                Log.e("FileError", "jsonFile: $jsonFile, mediaFile: $mediaFile")
                            }
                        } else {
                            val jsonFile = getFile(context, statics[i - 1].measure_server_json_name.replace(baseUrl, "")) // 0 1 2(static의 1) 3 4 5 6
                            val mediaFile = getFile(context, statics[i - 1].measure_server_file_name.replace(baseUrl, ""))
                            Log.e("File2+", "jsonFile: $jsonFile, mediaFile: $mediaFile")
                            if (jsonFile != null && mediaFile != null) {
                                ja.put(readJsonFile(jsonFile))
                                uris.add(mediaFile.absolutePath)
                            } else {
                                Log.e("FileError", "jsonFile: $jsonFile, mediaFile: $mediaFile")
                            }
                        }
                    }
                    Log.v("infoList", "${infoList}")
                    val dangerParts = if (infoList.isNotEmpty()) getDangerParts(infoList[0]) else emptyList()
                    Log.v("위험부위", "dangerParts: ${dangerParts} ,infoList:  ${infoList},infoList[0]:  ${infoList[0]}")
                    val measureVO = MeasureVO(
                        deviceSn = 0,
                        measureSn = infoList.firstOrNull()?.measure_sn!!,
                        regDate = infoList.firstOrNull()?.measure_date!!,
                        overall = infoList.firstOrNull()?.t_score ,
                        dangerParts = dangerParts.toMutableList(),
                        measureResult = ja,
                        fileUris = uris,
                        isMobile = infoList.firstOrNull()?.device_sn == 0,
                        recommendations = null
                    )
                    measures.add(measureVO)
                    Log.v("measureVO", "measureVO: $measureVO")
                }
                measures.reverse()
                singletonMeasure.measures = measures

                // ---------------------------# recommendation을 조회하는 함수 #-----------------------------

                val recommendations = getRecommendProgram(context.getString(R.string.API_recommendation), context)
                // 측정 별 추천 프로그램 받아오기
                val groupedRecs = recommendations.groupBy { it.measureSn }

                singletonMeasure.measures?.forEachIndexed { index, measure ->
                    val measureSn = measure.measureSn
                    if (measureSn in groupedRecs) {
                        // 이미 존재하는 추천 프로그램 사용
                        measure.recommendations = groupedRecs[measureSn] ?: emptyList()
                    } else {
                        // 새로운 추천 프로그램 생성
                        val recommendJson = JSONObject().apply {
                            put("user_sn", userInfoSn)
                            put("exercise_type_id", JSONArray().apply {
                                put(3)
                                put(1)
                                put(2)
                            })
                            put("exercise_stage", JSONArray().apply {
                                put(1)
                                put(2)
                                put(3)
                            })
                            put("measure_sn", measureSn)
                        }

                        createRecommendProgram(context.getString(R.string.API_recommendation), recommendJson.toString(), context) { newRecommendations ->
                            measure.recommendations = newRecommendations
                            singletonMeasure.measures!![index] = measure
                            Log.v("recommendCreated", "New recommendations for measureSn: $measureSn")
                        }
                    }
                    Log.v("recommendUpdate", "MeasureSn: $measureSn, Recommendations: ${measure.recommendations}")
                }

                callbacks()
            }
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

    // measureVO 따로빼논 api에서 한 번 더 쓰면 됨.
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



    fun getDangerParts(measureInfo: MeasureInfo) : MutableList<Pair<String, Int>> {
        val dangerParts = mutableListOf<Pair<String, Int>>()

        val neckRisk = measureInfo.risk_neck.toInt()
        if (neckRisk > 0) {
            dangerParts.add(Pair("목관절", neckRisk))
        }

        val shoulderRisk = maxOf(measureInfo.risk_shoulder_left.toInt(), measureInfo.risk_shoulder_right.toInt())
        if (shoulderRisk > 0) {
            dangerParts.add(Pair("어깨", shoulderRisk))
        }

        val elbowRisk = maxOf(measureInfo.risk_elbow_left.toInt(), measureInfo.risk_elbow_right.toInt())
        if (elbowRisk > 0) {
            dangerParts.add(Pair("팔꿉", elbowRisk))
        }

        val wristRisk = maxOf(measureInfo.risk_wrist_left.toInt(), measureInfo.risk_wrist_right.toInt())
        if (wristRisk > 0) {
            dangerParts.add(Pair("손목", wristRisk))
        }

        val hipRisk = maxOf(measureInfo.risk_hip_left.toInt(), measureInfo.risk_hip_right.toInt())
        if (hipRisk > 0) {
            dangerParts.add(Pair("골반", hipRisk))
        }

        val kneeRisk = maxOf(measureInfo.risk_knee_left.toInt(), measureInfo.risk_knee_right.toInt())
        if (kneeRisk > 0) {
            dangerParts.add(Pair("무릎", kneeRisk))
        }

        val ankleRisk = maxOf(measureInfo.risk_ankle_left.toInt(), measureInfo.risk_ankle_right.toInt())
        if (ankleRisk > 0) {
            dangerParts.add(Pair("발목", ankleRisk))
        }

        return dangerParts
    }
}