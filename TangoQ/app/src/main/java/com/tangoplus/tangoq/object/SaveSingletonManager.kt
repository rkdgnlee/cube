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
import com.tangoplus.tangoq.db.MeasureStatic
import com.tangoplus.tangoq.db.MeasurementManager.convertToJsonArrays
import com.tangoplus.tangoq.db.MeasurementManager.getDangerParts
import com.tangoplus.tangoq.db.SecurePreferencesManager.getServerUUID
import com.tangoplus.tangoq.db.SecurePreferencesManager.saveServerUUID
import com.tangoplus.tangoq.dialog.LoadingDialogFragment
import com.tangoplus.tangoq.`object`.DeviceService.getDeviceUUID
import com.tangoplus.tangoq.`object`.DeviceService.getSSAID
import com.tangoplus.tangoq.`object`.NetworkMeasure.saveAllMeasureInfo
import com.tangoplus.tangoq.`object`.NetworkProgress.postProgressInCurrentProgram
import com.tangoplus.tangoq.`object`.NetworkRecommendation.createRecommendProgram
import com.tangoplus.tangoq.`object`.NetworkRecommendation.getRecommendProgram
import com.tangoplus.tangoq.`object`.NetworkRecommendation.getRecommendationInOneMeasure
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

    fun getMeasures(userUUID: String, userInfoSn: Int, scope: CoroutineScope, callbacks: () -> Unit) {
        scope.launch {

            // 1. device_info를 넣고 저장함
            if (getServerUUID(context) == null) {
                val ssaid = getSSAID(activity)
                Log.v("ssaidExist", "mobile Num is not existed")
                val jo = JSONObject().put("serial_number", ssaid)
                getDeviceUUID(context.getString(R.string.API_user), context, jo) { mobileDeviceUuid ->
//                    Log.v("mobileDeviceUuid", mobileDeviceUuid)
                    saveServerUUID(context, mobileDeviceUuid)
                }
            } else {
                Log.v("ssaidExist", "mobile Num is already existed.")
            }

            // 2. saveAllMeasureInfo 부분 실행
            saveMeasureInfo(userUUID, userInfoSn) { existed2 ->
                if (existed2) {
                    CoroutineScope(Dispatchers.IO).launch {
                        fetchAndFilterMeasureInfo(userUUID)
                        // 2. Measure 정보 필터링 및 저장
                        Log.v("싱글턴measures", "${singletonMeasure.measures?.size}")
                        // 3. 추천 프로그램 추가
                        addRecommendations()
                        callbacks()
                    }
                } else {
                    callbacks()
                }
            }
        }
    }

    private suspend fun saveMeasureInfo(userUUID: String, userInfoSn: Int, callbacks: (Boolean) -> Unit) {
        withContext(Dispatchers.Main) {
            val dialog = LoadingDialogFragment.newInstance("측정이력")
            dialog.show(activity.supportFragmentManager, "LoadingDialogFragment")

            withContext(Dispatchers.IO) {
                saveAllMeasureInfo(context, context.getString(R.string.API_measure), userUUID) { existed ->
                    callbacks(existed)
                }
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                }
            }
        }
    }

    // static과 dynamic에서는 column을 쓰지를 않음. 그냥 json파일에 있는 값들을 내가 쓰지,
    private suspend fun fetchAndFilterMeasureInfo(userUUID: String) {
        withContext(Dispatchers.Main) {
            val dialog = LoadingDialogFragment.newInstance("측정파일")
            dialog.show(activity.supportFragmentManager, "LoadingDialogFragment")

            withContext(Dispatchers.IO) {

                val md = MeasureDatabase.getDatabase(context)
                val mDao = md.measureDao()

                val allInfos = mDao.getAllInfo(userUUID)
                val allStatics = mDao.getAllStatic(userUUID)
                val allDynamics = mDao.getAllDynamic(userUUID)

//                val groupedInfos = allInfos.groupBy { entity -> entity.sn }
                val groupedStatics = allStatics.groupBy { entity -> entity.server_sn }
                val groupedDynamics = allDynamics.groupBy { entity -> entity.server_sn }

                val measures = mutableListOf<MeasureVO>()

                for (info in allInfos) {
                    Log.v("인포리스트", "$allInfos")



                    val currentInfoSn = info.sn
                    Log.v("현재인포sn", "$currentInfoSn")


                    val statics = groupedStatics[info.sn]?.sortedBy { it.measure_seq } ?: emptyList()
                    val dynamic = groupedDynamics[info.sn] ?: emptyList()

                    val ja = JSONArray()
                    val uris = mutableListOf<String>()

                    for (i in 0 until 7) {
                        val jsonFile: File?
                        val mediaFile: File?

                        when (i) {
                            0 -> {
                                jsonFile = getFile(context, statics[0].measure_server_json_name)
                                mediaFile = getFile(context, statics[0].measure_server_file_name)
                            }
                            1 -> {
                                jsonFile = getFile(context, dynamic[0].measure_server_json_name!!)
                                mediaFile = getFile(context, dynamic[0].measure_server_file_name!!)
                            }
                            else -> {
                                jsonFile = getFile(context, statics[i - 1].measure_server_json_name)
                                mediaFile = getFile(context, statics[i - 1].measure_server_file_name)
                            }
                        }

                        if (jsonFile != null && mediaFile != null) {
                            if (i == 1) ja.put(readJsonArrayFile(jsonFile)) else ja.put(readJsonFile(jsonFile))
                            uris.add(mediaFile.absolutePath)
                        }
                    }
                    Log.v("인포목록들", "${info}, sn: ${info.sn}")
                    val dangerParts = getDangerParts(info)
                    val measureVO = MeasureVO(
                        deviceSn = 0,
                        sn = currentInfoSn!!,
                        regDate = info.measure_date,
                        overall = info.t_score,
                        dangerParts = dangerParts.toMutableList(),
                        measureResult = ja,
                        fileUris = uris,
                        isMobile = info.device_sn == 0,
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
    // ------# measure 1개 기존 싱글턴에 추가 #-------
    suspend fun addMeasurementInSingleton(mobileInfoSn: Int, mobileStaticSns : MutableList<Int>, mobileDynamicSn: Int) {

        withContext(Dispatchers.IO) {

            val md = MeasureDatabase.getDatabase(context)
            val mDao = md.measureDao()

            val info = mDao.getInfoByMobileSn(mobileInfoSn)
            val statics = mutableListOf<MeasureStatic>()
            for (i in 0 until mobileStaticSns.size) {
                statics.add(mDao.getStaticByMobileSn(mobileStaticSns[i]))
            }
            val dynamic = mDao.getDynamicByMobileSn(mobileDynamicSn)

            val ja = JSONArray()
            val uris = mutableListOf<String>()

            for (i in 0 until 7) {
                val jsonFile: File?
                val mediaFile: File?

                when (i) {
                    0 -> {
                        jsonFile = getFile(context, statics[0].measure_server_json_name)
                        mediaFile = getFile(context, statics[0].measure_server_file_name)
                    }
                    1 -> {
                        jsonFile = getFile(context, dynamic.measure_server_json_name!!)
                        mediaFile = getFile(context, dynamic.measure_server_file_name!!)
                    }
                    else -> {
                        jsonFile = getFile(context, statics[i - 1].measure_server_json_name)
                        mediaFile = getFile(context, statics[i - 1].measure_server_file_name)
                    }
                }

                if (jsonFile != null && mediaFile != null) {
                    if (i == 1) ja.put(readJsonArrayFile(jsonFile)) else ja.put(readJsonFile(jsonFile))
                    uris.add(mediaFile.absolutePath)
                }
            }


            val dangerParts =  getDangerParts(info)
            val measureVO = MeasureVO(
                deviceSn = 0,
                sn = info.sn!!,
                regDate = info.measure_date,
                overall = info.t_score,
                dangerParts = dangerParts.toMutableList(),
                measureResult = ja,
                fileUris = uris,
                isMobile = info.device_sn == 0,
                recommendations = null
            )
            singletonMeasure.measures?.add(0, measureVO)
            Log.v("싱글턴Measure1Item", "${singletonMeasure.measures?.size}")
            // 3. 추천 프로그램 추가
            mergeRecommendationInOneMeasure(measureVO.sn)
        }
    }



    /* 1. measure -> 있는지 확인 안함. 가져오기만
    *  2. recommendation -> 있는지 확인 후 없으면 추가하고 있으면 넣기.
    *  3. progress -> 있는지 없는지 확인 여기서 안하고, 자동으로 반환 (없으면 생성 후 반환, 있으면 반환)
    * */

    private suspend fun addRecommendations() {
        withContext(Dispatchers.Main) {
            val dialog = LoadingDialogFragment.newInstance("추천")
            dialog.show(activity.supportFragmentManager, "LoadingDialogFragment")
            withContext(Dispatchers.IO) {
                val recommendations = getRecommendProgram(context.getString(R.string.API_recommendation), context)
                val groupedRecs = recommendations.groupBy { it.serverSn }

                // eachIndexed로 각각의 measureSn에 접근. 현재 근데 여기서 measureSn이라는게 좀 애매함. info에 있는 sn을 활용해야하는 것 같기도.
                singletonMeasure.measures?.forEachIndexed { index, measure ->
                    val measureSn = measure.sn
                    if (measureSn in groupedRecs) {
                        measure.recommendations = groupedRecs[measureSn] ?: emptyList()
                    } else {
                        val (types, stages) = convertToJsonArrays(measure.dangerParts)
                        Log.v("types와stages", "types: $types, stages: $stages")
                        val recommendJson = JSONObject().apply {
                            put("exercise_type_id", types)
                            put("exercise_stage", stages)
                            put("server_sn", measureSn)
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

    // -------# measure 1개에 1번의 recommendation 넣기 (조회 후 없으면 생성) #------
    suspend fun mergeRecommendationInOneMeasure(measureInfoSn: Int) {
        withContext(Dispatchers.Main) {
            val dialog = LoadingDialogFragment.newInstance("추천")
            dialog.show(activity.supportFragmentManager, "LoadingDialogFragment")
            withContext(Dispatchers.IO) {
                val measure = singletonMeasure.measures?.find { it.sn == measureInfoSn }
                val recommendations = getRecommendationInOneMeasure(context.getString(R.string.API_recommendation), context, measureInfoSn)
                if (recommendations.isEmpty()) {
                    val (types, stages) = convertToJsonArrays(measure?.dangerParts!!)
                    Log.v("types와stages", "types: $types, stages: $stages")
                    val recommendJson = JSONObject().apply {
                        put("exercise_type_id", types)
                        put("exercise_stage", stages)
                        put("server_sn", measure.sn)
                    }
                    createRecommendProgram(context.getString(R.string.API_recommendation), recommendJson.toString(), context) { newRecommendations ->
                        measure.recommendations = newRecommendations
                        val currentMeasureIndex = singletonMeasure.measures!!.indexOfFirst { it.sn == measureInfoSn }
                        singletonMeasure.measures!![currentMeasureIndex] = measure
                        Log.v("recommendCreated", "New recommendations for measureInfoSn: $measureInfoSn")
                    }
                } else {
                    measure?.recommendations = recommendations
                    val currentMeasureIndex = singletonMeasure.measures!!.indexOfFirst { it.sn == measureInfoSn }
                    if (measure != null) {
                        singletonMeasure.measures!![currentMeasureIndex] = measure
                        Log.v("recommendCreated", "existed recommendations for measureInfoSn: $measureInfoSn")
                    }
                }
            }
        }
    }



    suspend fun getOrInsertProgress(jo: JSONObject) : Unit = suspendCoroutine  { continuation ->

        postProgressInCurrentProgram(context.getString(R.string.API_progress), jo, context) { progressUnits -> // MutableList<ProgressUnitVO>
            // (자동) 있으면 가져오고 없으면 추가되서 가져오고

            if (progressUnits.isNotEmpty()) {
                Log.v("프로그레스유닛들", "${progressUnits.size}")
                val weeks = 1..progressUnits.maxOf { it.currentWeek } // 4

                val organizedUnits = mutableListOf<MutableList<ProgressUnitVO>>() // 1이 속한 12개의 seq, 21개의 progressUnits
                for (week in weeks) { // 1, 2, 3, 4
                    val weekUnits = progressUnits.filter { it.currentWeek == week }.sortedBy { it.uvpSn }// 일단 주차별로 나눔. 1주차 2주차 3주차 4주차
                    organizedUnits.add(weekUnits.toMutableList())
                }
                // 결론적으로 4 * 21의 값만 들어와짐.
                singletonProgress.programProgresses = organizedUnits
                Log.v("singletonProgress", "${singletonProgress.programProgresses!!.size}")
                for (i in 0 until singletonProgress.programProgresses?.size!!) {
                    Log.v("singletonProgress2", "${singletonProgress.programProgresses!![i].size}")
                }

                continuation.resume(Unit) // continuation이라는 Coroutine함수를 통해 보내기
            } else {
                // ------# 측정 기록이 없음 #------
                singletonProgress.programProgresses?.add(mutableListOf())
                continuation.resume(Unit)
            }
        }
    }


}