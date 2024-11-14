package com.tangoplus.tangoq.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.google.gson.Gson
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Dao
interface MeasureDao {
    // ------------------------------# measure Info #------------------------------------------
    @Insert
    fun insertInfo(info: MeasureInfo)  : Long

    @Query("SELECT sn FROM t_measure_info WHERE user_uuid = :userUUID")
    fun getAllSns(userUUID: String): List<Int>

    @Query("SELECT * FROM t_measure_info WHERE user_uuid = :userUUID")
    fun getAllInfo(userUUID: String): List<MeasureInfo>

    @Query("SELECT mobile_info_sn FROM t_measure_info WHERE sn = :userSn") // mobile_info_sn이란 실제 db의 sn이 아님. room의 sn
    fun getMaxMobileInfoSn(userSn: Int) : Int


    @Query("UPDATE t_measure_info SET uploaded = :uploaded WHERE mobile_info_sn = :mobileInfoSn")
    suspend fun updateUploaded(mobileInfoSn: Int, uploaded: String)

    @Query("UPDATE t_measure_info SET upload_date = :uploadDate WHERE mobile_info_sn = :mobileInfoSn")
    suspend fun updateUploadDate(mobileInfoSn: Int, uploadDate: String)

    @Query("UPDATE t_measure_info SET sn = :sn WHERE mobile_info_sn = :mobileInfoSn")
    suspend fun updateSn(mobileInfoSn: Int, sn: Int)

    @Transaction
    suspend fun updateAndGetInfo(
        mobileInfoSn: Int,
        sn: Int? = null,
        uploaded: String? = null,
        uploadDate: String? = null
    ): MeasureInfo {
        sn?.let { updateSn(mobileInfoSn, it) }
        uploaded?.let { updateUploaded(mobileInfoSn, it) }
        uploadDate?.let { updateUploadDate(mobileInfoSn, it) }

        return getInfoByMobileSn(mobileInfoSn)
    }
    @Query("SELECT * FROM t_measure_info WHERE mobile_info_sn = :mobileInfoSn")
    fun getInfoByMobileSn(mobileInfoSn: Int): MeasureInfo

    @Query("SELECT * FROM t_measure_info WHERE user_uuid = :userUUID AND uploaded = '0'")
    fun getNotUploadedInfo(userUUID: String) : List<MeasureInfo>

    // -------------------------------# MeasureStatic #-------------------------------
    @Insert
    suspend fun insertByStatic(entity: MeasureStatic) : Long

    // TODO 현재 user_sn인데 user_uuid로 바꿔야함.
    @Query("SELECT * FROM t_measure_static WHERE user_uuid = :userUUID")
    fun getAllStatic(userUUID: String): List<MeasureStatic>

    // ------# server_sn과 uploaded를 update 하는 쿼리문 #------
    // Static 부분
    @Query("UPDATE t_measure_static SET server_sn = :serverSn WHERE mobile_sn = :mobileSn")
    suspend fun updateStaticServerSn(mobileSn: Int, serverSn: Int)

    @Query("UPDATE t_measure_static SET upload_date = :uploadDate WHERE mobile_sn = :mobileSn")
    suspend fun updateStaticUploadDate(mobileSn: Int, uploadDate: String)

    @Query("UPDATE t_measure_static SET uploaded_json = :uploadedJson WHERE mobile_sn = :mobileSn")
    suspend fun updateStaticUploadedJson(mobileSn: Int, uploadedJson: String)

    @Query("UPDATE t_measure_static SET uploaded_file = :uploadedFile WHERE mobile_sn = :mobileSn")
    suspend fun updateStaticUploadedFile(mobileSn: Int, uploadedFile: String)

    @Query("UPDATE t_measure_static SET uploaded = :uploaded WHERE mobile_sn = :mobileSn")
    suspend fun updateStaticUploaded(mobileSn: Int, uploaded: String)

    @Query("UPDATE t_measure_static SET measure_server_json_name = :serverJsonName WHERE mobile_sn = :mobileSn")
    suspend fun updateStaticServerJsonName(mobileSn: Int, serverJsonName: String)

    @Query("UPDATE t_measure_static SET measure_server_file_name = :serverFileName WHERE mobile_sn = :mobileSn")
    suspend fun updateStaticServerFIleName(mobileSn: Int, serverFileName: String)

    @Query("SELECT * FROM t_measure_static WHERE uploaded_json = '0' OR uploaded_file = '0'")
    suspend fun getFailedUploadedStatics() : List<MeasureStatic>

    @Transaction
    suspend fun updateAndGetStatic(
        mobileSn: Int,
        serverSn: Int? = null,
        uploaded: String? = null,
        uploadDate: String? = null,
        uploadedJson: String? = null,
        uploadedFile: String? = null,
        serverJsonName: String? = null,
        serverFileName: String? = null
    ): MeasureStatic {
        serverSn?.let { updateStaticServerSn(mobileSn, it) }
        uploaded?.let { updateStaticUploaded(mobileSn, it) }
        uploadDate?.let { updateStaticUploadDate(mobileSn, it) }
        uploadedJson?.let { updateStaticUploadedJson(mobileSn, it) }
        uploadedFile?.let { updateStaticUploadedFile(mobileSn, it) }
        serverJsonName?.let { updateStaticServerJsonName(mobileSn, it) }
        serverFileName?.let { updateStaticServerFIleName(mobileSn, it) }
        return getStaticByMobileSn(mobileSn)
    }

    @Query("SELECT * FROM t_measure_static WHERE user_uuid = :userUUID AND uploaded = '0'")
    fun getNotUploadedStatic(userUUID: String) : List<MeasureStatic>

    @Query("SELECT * FROM t_measure_static WHERE mobile_sn = :mobileSn")
    fun getStaticByMobileSn(mobileSn: Int): MeasureStatic


    // -------------------------------# MeasureDynamic #-------------------------------
    @Insert
    suspend fun insertByDynamic(entity: MeasureDynamic)  : Long

    @Query("SELECT * FROM t_measure_dynamic WHERE user_uuid = :userUUID")
    fun getAllDynamic(userUUID: String): List<MeasureDynamic>

    // ------# server_sn과 uploaded를 update 하는 쿼리문 #------
    @Query("UPDATE t_measure_dynamic SET server_sn = :serverSn WHERE mobile_sn = :mobileSn")
    suspend fun updateDynamicServerSn(mobileSn: Int, serverSn: Int)

    @Query("UPDATE t_measure_dynamic SET upload_date = :uploadDate WHERE mobile_sn = :mobileSn")
    suspend fun updateDynamicUploadDate(mobileSn: Int, uploadDate: String)

    @Query("UPDATE t_measure_dynamic SET uploaded_json = :uploadedJson WHERE mobile_sn = :mobileSn")
    suspend fun updateDynamicUploadedJson(mobileSn: Int, uploadedJson: String)

    @Query("UPDATE t_measure_dynamic SET uploaded_file = :uploadedFile WHERE mobile_sn = :mobileSn")
    suspend fun updateDynamicUploadedFile(mobileSn: Int, uploadedFile: String)

    @Query("UPDATE t_measure_dynamic SET uploaded = :uploaded WHERE mobile_sn = :mobileSn")
    suspend fun updateDynamicUploaded(mobileSn: Int, uploaded: String)

    @Query("UPDATE t_measure_dynamic SET measure_server_json_name = :serverJsonName WHERE mobile_sn = :mobileSn")
    suspend fun updateDynamicServerJsonName(mobileSn: Int, serverJsonName: String)

    @Query("UPDATE t_measure_dynamic SET measure_server_file_name = :serverFileName WHERE mobile_sn = :mobileSn")
    suspend fun updateDynamicServerFileName(mobileSn: Int, serverFileName: String)

    @Query("SELECT * FROM t_measure_dynamic WHERE uploaded_json = '0' OR uploaded_file = '0'")
    suspend fun getFailedUploadedDynamic() : List<MeasureDynamic>

    @Transaction
    suspend fun updateAndGetDynamic(
        mobileSn: Int,
        serverSn: Int? = null,
        uploaded: String? = null,
        uploadDate: String? = null,
        uploadedJson: String? = null,
        uploadedFile: String? = null,
        serverJsonName: String? = null,
        serverFileName: String? = null
    ): MeasureDynamic {
        serverSn?.let { updateDynamicServerSn(mobileSn, it) }
        uploaded?.let { updateDynamicUploaded(mobileSn, it) }
        uploadDate?.let { updateDynamicUploadDate(mobileSn, it) }
        uploadedJson?.let { updateDynamicUploadedJson(mobileSn, it) }
        uploadedFile?.let { updateDynamicUploadedFile(mobileSn, it) }
        serverJsonName?.let { updateDynamicServerJsonName(mobileSn, it) }
        serverFileName?.let { updateDynamicServerFileName(mobileSn, it) }

        return getDynamicByMobileSn(mobileSn)
    }

    @Query("SELECT * FROM t_measure_dynamic WHERE user_uuid = :userUUID AND uploaded = '0'")
    fun getNotUploadedDynamic(userUUID: String) : List<MeasureDynamic>

    @Query("SELECT * FROM t_measure_dynamic WHERE mobile_sn = :mobileSn")
    fun getDynamicByMobileSn(mobileSn: Int): MeasureDynamic
    // -----------------------------------------# sn 관리 #------------------------------------------

    fun MeasureStatic.toJson(): String {
        return Gson().toJson(this)
    }
    fun String.toMeasureStatic(): MeasureStatic {
        return Gson().fromJson(this, MeasureStatic::class.java)
    }
    fun MeasureDynamic.toJson(): String {
        return Gson().toJson(this)
    }
    fun String.toMeasureDynamic(): MeasureDynamic {
        return Gson().fromJson(this, MeasureDynamic::class.java)
    }
    fun MeasureInfo.toJson(): String {
        return Gson().toJson(this)
    }
    fun String.toMeasureInfo(): MeasureInfo {
        return Gson().fromJson(this, MeasureInfo::class.java)
    }

    fun JSONObject.toMeasureStatic(): MeasureStatic {
        return Gson().fromJson(this.toString(), MeasureStatic::class.java)
    }
    fun JSONObject.toMeasureDynamic(): MeasureDynamic {
        return Gson().fromJson(this.toString(), MeasureDynamic::class.java)
    }
    fun JSONObject.toMeasureInfo(): MeasureInfo {
        return Gson().fromJson(this.toString(), MeasureInfo::class.java)
    }



}