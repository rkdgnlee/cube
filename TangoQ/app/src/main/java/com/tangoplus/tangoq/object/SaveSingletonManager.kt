package com.tangoplus.tangoq.`object`

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.data.ProgressUnitVO
import com.tangoplus.tangoq.db.FileStorageUtil.getFile
import com.tangoplus.tangoq.db.FileStorageUtil.readJsonArrayFile
import com.tangoplus.tangoq.db.FileStorageUtil.readJsonFile
import com.tangoplus.tangoq.db.MeasureDatabase
import com.tangoplus.tangoq.db.MeasureInfo
import com.tangoplus.tangoq.dialog.LoadingDialogFragment
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

class SaveSingletonManager(private val context: Context, private val activity: FragmentActivity) {
    private val singletonMeasure = Singleton_t_measure.getInstance(context)
    private val singletonProgress = Singleton_t_progress.getInstance(context)

    fun getMeasures(userUUID: String, userInfoSn: Int, userSn: Int, scope: CoroutineScope, callbacks: () -> Unit) {
        scope.launch {
            // 1. saveAllMeasureInfo 부분 실행

            // 측정기록이 있는 user이다? 그러면 true문 없으면 바로 mainactivity로 감.
            saveMeasureInfo(userUUID, userSn) { existed2 ->
                if (existed2) {
                    CoroutineScope(Dispatchers.IO).launch {
                        fetchAndFilterMeasureInfo(userSn)
                        // 2. Measure 정보 필터링 및 저장
                        Log.v("싱글턴measures", "${singletonMeasure.measures?.size}")
                        // 3. 추천 프로그램 추가
                        addRecommendations(userInfoSn)
                        callbacks()
                    }
                } else {
                    callbacks()
                }
            }
        }
    }

    private suspend fun saveMeasureInfo(userUUID: String, userSn: Int, callbacks: (Boolean) -> Unit) {
        withContext(Dispatchers.Main) {
            val dialog = LoadingDialogFragment.newInstance("측정이력")
            dialog.show(activity.supportFragmentManager, "LoadingDialogFragment")

            withContext(Dispatchers.IO) {
                saveAllMeasureInfo(context, context.getString(R.string.API_measure), userUUID, userSn) { existed ->
                    callbacks(existed)
                }
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                }
            }
        }
    }

    private suspend fun fetchAndFilterMeasureInfo(userSn: Int) {
        withContext(Dispatchers.Main) {
            val dialog = LoadingDialogFragment.newInstance("측정파일")
            dialog.show(activity.supportFragmentManager, "LoadingDialogFragment")

            withContext(Dispatchers.IO) {

                val md = MeasureDatabase.getDatabase(context)
                val mDao = md.measureDao()

                val allInfos = mDao.getAllInfo(userSn)
                val allStatics = mDao.getAllStatic(userSn)
                val allDynamics = mDao.getAllDynamic(userSn)


                val groupedInfos = allInfos.groupBy { entity -> entity.sn }
                val groupedStatics = allStatics.groupBy { entity -> entity.server_sn }
                val groupedDynamics = allDynamics.groupBy { entity -> entity.server_sn }

                val measures = mutableListOf<MeasureVO>()

                for ((key, infoList) in groupedInfos) {
                    val statics = groupedStatics[key]?.sortedBy { it.measure_seq } ?: emptyList()
                    val dynamic = groupedDynamics[key] ?: emptyList()

                    val ja = JSONArray()
                    val uris = mutableListOf<String>()
                    val baseUrl = "https://gym.tangostar.co.kr/data/Results/"

                    for (i in 0 until 7) {
                        val jsonFile: File?
                        val mediaFile: File?

                        if (i == 0) {
                            jsonFile = getFile(context, statics[0].measure_server_json_name.replace(baseUrl, ""))
                            mediaFile = getFile(context, statics[0].measure_server_file_name.replace(baseUrl, ""))
                        } else if (i == 1) {
                            jsonFile = getFile(context, dynamic[0].measure_server_json_name?.replace(baseUrl, "")!!)
                            mediaFile = getFile(context, dynamic[0].measure_server_file_name?.replace(baseUrl, "")!!)
                        } else {
                            jsonFile = getFile(context, statics[i - 1].measure_server_json_name.replace(baseUrl, ""))
                            mediaFile = getFile(context, statics[i - 1].measure_server_file_name.replace(baseUrl, ""))
                        }

                        if (jsonFile != null && mediaFile != null) {
                            if (i == 1) ja.put(readJsonArrayFile(jsonFile)) else ja.put(readJsonFile(jsonFile))
                            uris.add(mediaFile.absolutePath)
                        }
                    }

                    val dangerParts = if (infoList.isNotEmpty()) getDangerParts(infoList[0]) else emptyList()
                    val measureVO = MeasureVO(
                        deviceSn = 0,
                        measureSn = infoList.firstOrNull()?.measure_sn!!,
                        regDate = infoList.firstOrNull()?.measure_date!!,
                        overall = infoList.firstOrNull()?.t_score,
                        dangerParts = dangerParts.toMutableList(),
                        measureResult = ja,
                        fileUris = uris,
                        isMobile = infoList.firstOrNull()?.device_sn == 0,
                        recommendations = null
                    )
                    measures.add(measureVO)
                }
                measures.reverse()
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                }
                singletonMeasure.measures = measures.toMutableList()

            }
        }

    }

    suspend fun addRecommendations(userInfoSn: Int) {
        withContext(Dispatchers.Main) {
            val dialog = LoadingDialogFragment.newInstance("추천")
            dialog.show(activity.supportFragmentManager, "LoadingDialogFragment")
            withContext(Dispatchers.IO) {
                val recommendations = getRecommendProgram(context.getString(R.string.API_recommendation), context)
                val groupedRecs = recommendations.groupBy { it.infoSn }

                singletonMeasure.measures?.forEachIndexed { index, measure ->

                    // eachIndexed로 각각의 measureSn에 접근. 현재 근데 여기서 measureSn이라는게 좀 애매함. info에 있는 sn을 활용해야하는 것 같기도.
                    val measureSn = measure.measureSn
                    if (measureSn in groupedRecs) {
                        measure.recommendations = groupedRecs[measureSn] ?: emptyList()
                    } else {
                        val recommendJson = JSONObject().apply {
                            put("user_sn", userInfoSn)
                            put("exercise_type_id", JSONArray().apply { put(3); put(1); put(2) })
                            put("exercise_stage", JSONArray().apply { put(1); put(2); put(3) })
                            put("measure_sn", measureSn)
                        }

                        createRecommendProgram(context.getString(R.string.API_recommendation), recommendJson.toString(), context) { newRecommendations ->
                            measure.recommendations = newRecommendations
                            singletonMeasure.measures!![index] = measure
                            Log.v("recommendCreated", "New recommendations for measureSn: $measureSn")
                        }
                    }
                    Log.v("recommendUpdate", "MeasureSn: $measureSn, Recommendations: ${measure.recommendations}")
                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                    }
                }
            }

        }

    }

    suspend fun getOrInsertProgress(jo: JSONObject) : Unit = suspendCoroutine  { continuation ->

        postProgressInCurrentProgram(context.getString(R.string.API_progress), jo, context) { progressUnits -> // MutableList<ProgressUnitVO>
            // (자동) 있으면 가져오고 없으면 추가되서 가져오고

            if (progressUnits.isNotEmpty()) {
                Log.v("프로그레스유닛들", "$progressUnits")
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

                singletonProgress.programProgresses = organizedUnits
                Log.v("singletonProgress", "${singletonProgress.programProgresses!![0].map { it.lastProgress }}, ${singletonProgress.programProgresses!![1].map { it.lastProgress }}, ${singletonProgress.programProgresses!![2].map { it.lastProgress }}, ${singletonProgress.programProgresses!![3].map { it.lastProgress }}, ")
                continuation.resume(Unit)
            } else {
                // ------# 측정 기록이 없음 #------
                singletonProgress.programProgresses?.add(mutableListOf())
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



    fun getDangerParts(measureInfo: MeasureInfo) : MutableList<Pair<String, Float>> {
        val dangerParts = mutableListOf<Pair<String, Float>>()

        val neckRisk = measureInfo.risk_neck.toFloat()
        if (neckRisk > 0) {
            dangerParts.add(Pair("목관절", neckRisk))
        }

        val shoulderLeftRisk = measureInfo.risk_shoulder_left.toFloat()
        if (shoulderLeftRisk > 0) {
            dangerParts.add(Pair("우측 어깨", shoulderLeftRisk))
        }

        val shoulderRightRisk = measureInfo.risk_shoulder_right.toFloat()
        if (shoulderRightRisk > 0) {
            dangerParts.add(Pair("좌측 어깨", shoulderRightRisk))
        }

        val elbowLeftRisk = measureInfo.risk_elbow_left.toFloat()
        if (elbowLeftRisk > 0) {
            dangerParts.add(Pair("좌측 팔꿉", elbowLeftRisk))
        }
        val elbowRightRisk =  measureInfo.risk_elbow_right.toFloat()
        if (elbowRightRisk > 0) {
            dangerParts.add(Pair("우측 팔꿉", elbowRightRisk))
        }

        val wristLeftRisk = measureInfo.risk_wrist_left.toFloat()
        if (wristLeftRisk > 0) {
            dangerParts.add(Pair("좌측 손목", wristLeftRisk))
        }
        val wristRightRisk = measureInfo.risk_wrist_right.toFloat()
        if (wristRightRisk > 0) {
            dangerParts.add(Pair("우측 손목", wristRightRisk))
        }

        val hipLeftRisk = measureInfo.risk_hip_left.toFloat()
        if (hipLeftRisk > 0) {
            dangerParts.add(Pair("좌측 골반", hipLeftRisk))
        }
        val hipRightRisk = measureInfo.risk_hip_right.toFloat()
        if (hipRightRisk > 0) {
            dangerParts.add(Pair("우측 골반", hipRightRisk))
        }

        val kneeLeftRisk = measureInfo.risk_knee_left.toFloat()
        if (kneeLeftRisk > 0) {
            dangerParts.add(Pair("좌측 무릎", kneeLeftRisk))
        }
        val kneeRightRisk = measureInfo.risk_knee_right.toFloat()
        if (kneeRightRisk > 0) {
            dangerParts.add(Pair("우측 무릎", kneeRightRisk))
        }

        val ankleLeftRisk = measureInfo.risk_ankle_left.toFloat()
        if (ankleLeftRisk > 0) {
            dangerParts.add(Pair("좌측 발목", ankleLeftRisk))
        }
        val ankleRightRisk = measureInfo.risk_ankle_right.toFloat()
        if (ankleRightRisk > 0) {
            dangerParts.add(Pair("우측 발목", ankleRightRisk))
        }

        return dangerParts
    }
}