package com.tangoplus.tangoq.function

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.google.gson.Gson
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.vo.MeasureVO
import com.tangoplus.tangoq.vo.ProgressUnitVO
import com.tangoplus.tangoq.vo.UrlTuple
import com.tangoplus.tangoq.db.FileStorageUtil
import com.tangoplus.tangoq.db.FileStorageUtil.getFile
import com.tangoplus.tangoq.db.FileStorageUtil.readJsonArrayFile
import com.tangoplus.tangoq.db.FileStorageUtil.readJsonFile
import com.tangoplus.tangoq.db.FileStorageUtil.saveFileFromUrl
import com.tangoplus.tangoq.db.MeasureDatabase
import com.tangoplus.tangoq.db.MeasureStatic
import com.tangoplus.tangoq.function.MeasurementManager.convertToJsonArrays
import com.tangoplus.tangoq.function.MeasurementManager.getDangerParts
import com.tangoplus.tangoq.function.SecurePreferencesManager.getServerUUID
import com.tangoplus.tangoq.function.SecurePreferencesManager.saveServerUUID
import com.tangoplus.tangoq.dialog.LoadingDialogFragment
import com.tangoplus.tangoq.function.SecurePreferencesManager.getEncryptedAccessJwt
import com.tangoplus.tangoq.api.DeviceService.getDeviceUUID
import com.tangoplus.tangoq.api.DeviceService.getSSAID
import com.tangoplus.tangoq.api.NetworkMeasure.saveAllMeasureInfo
import com.tangoplus.tangoq.api.NetworkRecommendation.createRecommendProgram
import com.tangoplus.tangoq.api.NetworkRecommendation.getRecommendProgram
import com.tangoplus.tangoq.api.NetworkRecommendation.getRecommendationInOneMeasure
import com.tangoplus.tangoq.db.FileStorageUtil.deleteCorruptedFile
import com.tangoplus.tangoq.db.MeasureDynamic
import com.tangoplus.tangoq.db.Singleton_t_measure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class SaveSingletonManager(private val context: Context, private val activity: FragmentActivity) {
    private val singletonMeasure = Singleton_t_measure.getInstance(context)
    private val md = MeasureDatabase.getDatabase(context)
    private val mDao = md.measureDao()

    fun getMeasures(userUUID: String, userInfoSn: Int, scope: CoroutineScope, callbacks: () -> Unit) {
        scope.launch {

            // 1. device_info를 넣고 저장함
            if (getServerUUID(context) == null) {
                val ssaid = getSSAID(activity)
                Log.v("ssaidExist", "mobile Num is not existed")
                val jo = JSONObject().put("serial_number", ssaid)
                getDeviceUUID(context.getString(R.string.API_user), context, jo) { mobileDeviceUuid ->
                    saveServerUUID(context, mobileDeviceUuid)
                }
            } else {
                Log.v("ssaidExist", "mobile Num is already existed.")
            }

            // 2. saveAllMeasureInfo 부분 실행
            if (userUUID != "" || userInfoSn != -1) {
                saveMeasureInfo(userUUID) { existed2 ->
                    if (existed2) {
                        CoroutineScope(Dispatchers.IO).launch {

                            // 3. Room에 저장된 것들 꺼내서 MeasureVO로 변환.
                            fetchAndFilterMeasureInfo(userUUID)

                            Log.v("싱글턴measures", "${singletonMeasure.measures?.size}")
                            addRecommendations()
                            callbacks()
                        }
                    } else {
                        callbacks()
                    }
                }
            }
        }
    }

    private suspend fun saveMeasureInfo(userUUID: String, callbacks: (Boolean) -> Unit) {
        val currentActivity = activity
        val dialog = withContext(Dispatchers.Main) {
            if (!currentActivity.isFinishing && !currentActivity.isDestroyed && !activity.supportFragmentManager.isStateSaved) {
                LoadingDialogFragment.newInstance("측정이력").apply {
                    show(currentActivity.supportFragmentManager, "LoadingDialogFragment")
                }
            } else null
        }
        Log.v("getEncryptedJwt", "save Access Token: ${getEncryptedAccessJwt(context) != ""}")
        withContext(Dispatchers.IO) {
            saveAllMeasureInfo(context, context.getString(R.string.API_measure), userUUID) { existed ->
                callbacks(existed)
            }
            withContext(Dispatchers.Main) {
                if (dialog != null) {
                    if (dialog.isAdded && dialog.isVisible) {
                        dialog.dismissAllowingStateLoss()
                    }
                }
            }
        }
    }

    // static과 dynamic에서는 column을 쓰지를 않음. 그냥 json파일에 있는 값들을 내가 쓰지,


    private suspend fun fetchAndFilterMeasureInfo(userUUID: String) {
        val currentActivity = activity
        val dialog = withContext(Dispatchers.Main) {
            if (!currentActivity.isFinishing && !currentActivity.isDestroyed && !activity.supportFragmentManager.isStateSaved) {
                LoadingDialogFragment.newInstance("측정파일").apply {
                    show(currentActivity.supportFragmentManager, "LoadingDialogFragment")
                }
            } else null
        }
        try {

            withContext(Dispatchers.IO) {
                val allInfos = mDao.getAllInfo(userUUID)
                val allStatics = mDao.getAllStatic(userUUID)
                val allDynamics = mDao.getAllDynamic(userUUID)

//                val groupedInfos = allInfos.groupBy { entity -> entity.sn }
                val groupedStatics = allStatics.groupBy { entity -> entity.server_sn }
                val groupedDynamics = allDynamics.groupBy { entity -> entity.server_sn }

                val measures = mutableListOf<MeasureVO>()

                allInfos.mapIndexed { index, info ->
                    async {
                        val currentInfoSn = info.sn

                        val statics = groupedStatics[currentInfoSn]?.sortedBy { it.measure_seq } ?: emptyList()
                        val dynamic = groupedDynamics[currentInfoSn] ?: emptyList()
//                        Log.v("groupedStatics", "${statics.size}")

                        if (statics.size < 6 || dynamic.isEmpty()) {
//                            Log.v("건너뜀", "현재 measure: $currentInfoSn, statics size: ${statics.size}, dynamic size: ${dynamic.size}")
                            return@async // 현재 info 처리 건너뜀
                        }
//                        Log.v("인포목록들", "${info}, sn: ${info.sn}")
                        val dangerParts = getDangerParts(info)

                        // 마지막 index만 지금 즉시 uris와 함꼐 넣기
                        if (currentInfoSn != null ) {
//                            Log.v("현재index", "$index")
                            if (index == allInfos.size - 1) {

                                val measureVO = MeasureVO(
                                    deviceSn = 0,
                                    sn = currentInfoSn,
                                    userName = info.user_name.toString(),
                                    regDate = info.measure_date.toString(),
                                    overall = info.t_score,
                                    dangerParts = dangerParts.toMutableList(),
                                    measureResult = null,
                                    fileUris = null,
                                    isMobile = info.device_sn == 0,
                                    recommendations = null
                                )
                                val serverSn = info.sn
                                val uriTuples = get1MeasureUrls(serverSn)
                                downloadFiles(uriTuples)
                                val editedMeasure = insertUrlToMeasureVO(uriTuples, measureVO)

                                // singleton의 인덱스 찾아서 ja와 값 넣기
                                measures.add(editedMeasure)
                            } else {
                                val measureVO = MeasureVO(
                                    deviceSn = 0,
                                    sn = currentInfoSn,
                                    userName = info.user_name.toString(),
                                    regDate = info.measure_date.toString(),
                                    overall = info.t_score,
                                    dangerParts = dangerParts.toMutableList(),
                                    measureResult = null,
                                    fileUris = null,
                                    isMobile = info.device_sn == 0,
                                    recommendations = null
                                )
                                measures.add(measureVO)
                            }

                        }
                        measures.sortByDescending { it.regDate }
                        singletonMeasure.measures = measures.toMutableList()
                    }.await()
                }
            }
        } catch (e: IllegalStateException) {
            Log.e("measureError", "fetchAndFilterIllegalState: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.e("measureError", "fetchAndFilterIllegalArgument: ${e.message}")
        } catch (e: NullPointerException) {
            Log.e("measureError", "fetchAndFilterNullPointer: ${e.message}")
        } catch (e: InterruptedException) {
            Log.e("measureError", "fetchAndFilterInterrupted: ${e.message}")
        } catch (e: IndexOutOfBoundsException) {
            Log.e("measureError", "fetchAndFilterIndexOutOfBounds: ${e.message}")
        } catch (e: Exception) {
            Log.e("measureError", "fetchAndFilter: ${e.message}")
        } finally {
            withContext(Dispatchers.Main) {
                if (dialog?.isAdded == true && dialog.isVisible) {
                    dialog.dismissAllowingStateLoss()
                }
            }
        }
    }


    // ------# 파일 다운로드 전 url들을 가져오기 #------
    fun get1MeasureUrls(serverSn: Int): List<UrlTuple> {
        val statics = mDao.getStaticUrl(serverSn).sortedBy { it.measure_seq }.toMutableList()
        val dynamic = mDao.getDynamicUrl(serverSn)
        // dynamic의 첫 번째 요소를 1번 인덱스에 삽입
        if (dynamic.isNotEmpty()) {
            statics.add(1, dynamic[0])
        }
//        Log.v("url가져오기", "$statics")
        return statics
    }
    // ------# 저장된 measure에다가 파일 다운로드 및 암호화 하기 (1개의 measure만) #-------
    suspend fun downloadFiles(urlTuples: List<UrlTuple>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val saveJobs = mutableListOf<Deferred<Boolean>>()

            try {
                for (index in urlTuples.indices) {
                    if (index == 1) {
                        val fileName2 = urlTuples[index].measure_server_file_name
                        val jsonName2 = urlTuples[index].measure_server_json_name
//                        Log.v("urlTuples", "mp4: ${fileName2}, json: $jsonName2")
                        saveJobs.add(async {
                            try {
                                saveFileFromUrl(context, fileName2, FileStorageUtil.FileType.VIDEO)
                            } catch (e: Exception) {
                                Log.e("downloadFiles", "Error saving video file", e)
                                false
                            }
                        })
                        saveJobs.add(async {
                            try {
                                saveFileFromUrl(context, jsonName2, FileStorageUtil.FileType.JSON)
                            } catch (e: Exception) {
                                Log.e("downloadFiles", "Error saving JSON file", e)
                                false
                            }
                        })
                    } else {
                        val fileName = urlTuples[index].measure_server_file_name
                        val jsonName = urlTuples[index].measure_server_json_name
//                        Log.v("urlTuples", "jpg: ${fileName}, json: $jsonName")
                        saveJobs.add(async {
                            try {
                                saveFileFromUrl(context, fileName, FileStorageUtil.FileType.IMAGE)
                            } catch (e: Exception) {
                                Log.e("downloadFiles", "Error saving image file", e)
                                false
                            }
                        })

                        saveJobs.add(async {
                            try {
                                saveFileFromUrl(context, jsonName, FileStorageUtil.FileType.JSON)
                            } catch (e: Exception) {
                                Log.e("downloadFiles", "Error saving JSON file", e)
                                false
                            }
                        })
                    }
                }

                // 작업 개수 로그 확인
                Log.v("downloadFiles", "Total save jobs")

                // 모든 작업 완료 대기
                saveJobs.awaitAll()

                Log.v("파일다운로드", "finish saveJobs")
                Result.success(Unit)
            } catch (e: ClassNotFoundException) {
                Log.e("downloadFiles", "Unexpected 'ClassNotFound' error: ${e.message}")
                Result.failure(e)
            } catch (e: NullPointerException) {
                Log.e("downloadFiles", "Unexpected 'NullPointer' error: ${e.message}")
                Result.failure(e)
            } catch (e: IllegalAccessException) {
                Log.e("downloadFiles", "Unexpected 'IllegalAccess' error: ${e.message}")
                Result.failure(e)
            } catch (e: IllegalStateException) {
                Log.e("downloadFiles", "Unexpected 'IllegalState' error: ${e.message}")
                Result.failure(e)
            } catch (e: Exception) {
                Log.e("downloadFiles", "Unexpected error: ${e.message}")
                Result.failure(e)
            }
        }
    }

    // ------# 다운로드 파일을 암호화 MeasureVO에 넣기 #------
    //  이 함수에서는 파일 복화하면서 가져오면서 jsonArray를 가져와서 매개변수 measureVO에 넣기 해야 함.
    suspend fun insertUrlToMeasureVO(uriTuples: List<UrlTuple>, measureVO: MeasureVO): MeasureVO {
        val ja = JSONArray()
        val uris = mutableListOf<String>()
        // 1. 복호화 부터
        val fileResults = (0 until 7).map { i ->
            var jsonFile = getFile(context, uriTuples[i].measure_server_json_name)
            var mediaFile = getFile(context, uriTuples[i].measure_server_file_name)

            val jsonJudge = if (jsonFile != null) {
                if (i == 1)
                    readJsonArrayFile(jsonFile)
                else readJsonFile(jsonFile)
            } else null
            val mediaJudge = if (jsonFile != null) {
                getFile(context, uriTuples[i].measure_server_file_name)
            } else null
            // 파일 중 손상된 경우 삭제 후 재 다운로드 로직
            if (jsonJudge == null || mediaJudge == null || mediaFile == null || !mediaFile.canRead()) {
                // 손상된 파일 삭제
//                Log.e("FileDebug", "json: $jsonJudge, mediaFile: $mediaFile, mediaFile.canRead: ${mediaFile?.canRead()}")
                if (jsonFile != null) deleteCorruptedFile(context, uriTuples[i].measure_server_json_name)
                if (mediaFile != null) deleteCorruptedFile(context, uriTuples[i].measure_server_file_name)
                // 파일 재다운로드
                val jsonDownloaded = saveFileFromUrl(
                    context,
                    uriTuples[i].measure_server_json_name,
                    FileStorageUtil.FileType.JSON
                )
                val mediaDownloaded = saveFileFromUrl(
                    context,
                    uriTuples[i].measure_server_file_name,
                    if (i == 1) FileStorageUtil.FileType.VIDEO else FileStorageUtil.FileType.IMAGE
                )

                // 새로 다운로드한 파일 가져오기
                jsonFile = if (jsonDownloaded) getFile(context, uriTuples[i].measure_server_json_name) else null
                mediaFile = if (mediaDownloaded) getFile(context, uriTuples[i].measure_server_file_name) else null
            }

            // 둘다 완성된 값일 경우.
            if (jsonFile != null && mediaFile != null) {
                Triple(
                    i,
                    if (i == 1) readJsonArrayFile(jsonFile) else readJsonFile(jsonFile),
                    mediaFile.absolutePath
                )
            } else null
        }
        // 2. file을 읽어와서 jsonArray에 담음
        fileResults.filterNotNull().sortedBy { it.first }.forEach { (_, json, uri) ->
            ja.put(json)
            uris.add(uri)
        }
        measureVO.measureResult = ja
        measureVO.fileUris = uris
        // 3. 전부 가져왔으면 measureVO에 넣고
        return measureVO
    }

    // ------# measure 1개 기존 싱글턴에 추가 #-------
    suspend fun addMeasurementInSingleton(mobileInfoSn: Int, mobileStaticSns : MutableList<Int>, mobileDynamicSn: Int) {
        withContext(Dispatchers.IO) {
            val md = MeasureDatabase.getDatabase(context)
            val mDao = md.measureDao()

            val info = mDao.getInfoByMobileSn(mobileInfoSn)
//            Log.v("1itemInfo", "$info")
            val statics = mutableListOf<MeasureStatic>()
            for (i in 0 until mobileStaticSns.size) {
                statics.add(mDao.getStaticByMobileSn(mobileStaticSns[i]))
//                Log.v("1itemStatic$i", "${statics[i]}")
            }
            val dynamic = mDao.getDynamicByMobileSn(mobileDynamicSn)
//            Log.v("1itemDynamic", "$dynamic")
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
                        jsonFile = getFile(context, dynamic.measure_server_json_name.toString())
                        mediaFile = getFile(context, dynamic.measure_server_file_name.toString())
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
//            Log.v("dangerParts", "$dangerParts")
            if (info.sn != null) {
//                Log.v("싱글턴Measure넣기전", "info.sn : ${info.sn}")
                val measureVO = MeasureVO(
                    deviceSn = 0,
                    sn = info.sn,
                    userName = info.user_name.toString(),
                    regDate = info.measure_date.toString(),
                    overall = info.t_score,
                    dangerParts = dangerParts.toMutableList(),
                    measureResult = ja,
                    fileUris = uris,
                    isMobile = info.device_sn == 0,
                    recommendations = null
                )
//                Log.v("싱글턴Measure넣기전 다른 값", "t_score: ${info.t_score},ja:  ${ja},uris: ${uris}, ${info.device_sn}")
                // ------# 초기 측정 상태일 때 null 예외 처리 #------
                if (singletonMeasure.measures == null) {
                    singletonMeasure.measures = mutableListOf()
                }
                singletonMeasure.measures?.add(0, measureVO)
//                Log.v("싱글턴Measure넣기전", "${singletonMeasure.measures}")
//                Log.v("싱글턴Measure1Item", "${singletonMeasure.measures?.size}")
                // 3. 추천 프로그램 추가
                mergeRecommendationInOneMeasure(measureVO.sn)
            }
        }
    }

    /* 1. measure -> 있는지 확인 안함. 가져오기만
    *  2. recommendation -> 있는지 확인 후 없으면 추가하고 있으면 넣기.
    *  3. progress -> 있는지 없는지 확인 여기서 안하고, 자동으로 반환 (없으면 생성 후 반환, 있으면 반환)
    * */
    private suspend fun addRecommendations() {
        withContext(Dispatchers.Main) {
            val dialog = LoadingDialogFragment.newInstance("추천")
            if (!activity.isFinishing && !activity.isDestroyed && !activity.supportFragmentManager.isStateSaved) {
               dialog.show(activity.supportFragmentManager, "LoadingDialogFragment")
            }
            withContext(Dispatchers.IO) {
                val recommendations = getRecommendProgram(context.getString(R.string.API_recommendation), context)
                val groupedRecs = recommendations.groupBy { it.serverSn }

                // eachIndexed로 각각의 measureSn에 접근. 현재 근데 여기서 measureSn이라는게 좀 애매함. info에 있는 sn을 활용해야하는 것 같기도.
                singletonMeasure.measures?.mapIndexed { index, measure ->
                    async {
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
                                singletonMeasure.measures?.set(index, measure)
                                Log.v("recommendCreated", "New recommendations")
                            }
                        }
                    }.await()
                    // 모든 async 작업이 완료될 때까지 대기
                    withContext(Dispatchers.Main) {
                        if (dialog.isAdded && dialog.isVisible) {
                            dialog.dismissAllowingStateLoss()
                        }
                    }
                }
            }
        }
    }

    // -------# measure 1개에 1번의 recommendation 넣기 (조회 후 없으면 생성) #------
    private suspend fun mergeRecommendationInOneMeasure(measureInfoSn: Int) {
        withContext(Dispatchers.Main) {
            val dialog = LoadingDialogFragment.newInstance("추천")
            dialog.show(activity.supportFragmentManager, "LoadingDialogFragment")
            withContext(Dispatchers.IO) {
                val measure = singletonMeasure.measures?.find { it.sn == measureInfoSn }
                val recommendations = getRecommendationInOneMeasure(context.getString(R.string.API_recommendation), context, measureInfoSn)
                if (recommendations.isEmpty()) {
                    val (types, stages) = convertToJsonArrays(measure?.dangerParts)
                    val recommendJson = JSONObject().apply {
                        put("exercise_type_id", types)
                        put("exercise_stage", stages)
                        put("server_sn", measure?.sn)
                    }
                    createRecommendProgram(context.getString(R.string.API_recommendation), recommendJson.toString(), context) { newRecommendations ->
                        measure?.recommendations = newRecommendations
                        val currentMeasureIndex = singletonMeasure.measures?.indexOfFirst { it.sn == measureInfoSn }
                        if (currentMeasureIndex != null && measure != null) {
                            singletonMeasure.measures?.set(currentMeasureIndex, measure)
                        }
                        Log.v("recommendCreated", "New recommendations")
                    }
                } else {
                    measure?.recommendations = recommendations
                    val currentMeasureIndex = singletonMeasure.measures?.indexOfFirst { it.sn == measureInfoSn }
                    if (measure != null && currentMeasureIndex != null) {
                        singletonMeasure.measures?.set(currentMeasureIndex, measure)
                        Log.v("recommendCreated", "existed recommendations")
                    }
                }
            }
        }
    }


    // 최근 measureResult를 Room에서 꺼내서 만들기 - 없는 값만 확인후 추가.
    fun MeasureStatic.toJson(): String {
        return Gson().toJson(this)
    }
    fun MeasureDynamic.toJson(): String {
        return Gson().toJson(this)
    }
}