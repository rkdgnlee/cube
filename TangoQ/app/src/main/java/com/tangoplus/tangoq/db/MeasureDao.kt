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
    fun insertInfo(info: MeasureInfo)

    @Query("SELECT sn FROM t_measure_info WHERE user_sn = :userSn")
    fun getAllSns(userSn: Int): List<Int>

    @Query("SELECT * FROM t_measure_info WHERE user_sn = :userSn")
    fun getAllInfo(userSn: Int): List<MeasureInfo>

    @Query("SELECT mobile_info_sn FROM t_measure_info WHERE user_sn = :userSn") // mobile_info_sn이란 실제 db의 sn이 아님. room의 sn
    fun getMaxMobileInfoSn(userSn: Int) : Int

    // -------------------------------# MeasureStatic sn 증가 #-------------------------------
    @Transaction
    suspend fun insertWithAutoIncrementStatic(entity: MeasureStatic, userUUID: String ,measureSn : Int) {
        val newEntity = entity.copy(
            mobile_sn = 0,
            user_uuid = userUUID,
            measure_sn = measureSn,
            reg_date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            uploaded = "0",
            upload_date = "0",
            uploaded_json = "0",
            uploaded_file = "0",
            used = "0"
        )
        insertByStatic(newEntity)
    }
    @Insert
    suspend fun insertByStatic(entity: MeasureStatic)

    // TODO 현재 user_sn인데 user_uuid로 바꿔야함.
    @Query("SELECT * FROM t_measure_static WHERE user_sn = :userSn")
    fun getAllStatic(userSn: Int): List<MeasureStatic>


    // -------------------------------# MeasureDynamic sn 증가 #-------------------------------
    @Transaction
    suspend fun insertWithAutoIncrementDynamic(entity: MeasureDynamic, userUUID: String , indexes: Int, measureSn : Int) {
        val newEntity = entity.copy(
            mobile_sn = 0,
            user_uuid = userUUID,
            measure_sn = measureSn,
            reg_date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            result_index = indexes,
            uploaded = "0",
            upload_date = "0",
            uploaded_json = "0",
            uploaded_file = "0",
            uploaded_json_fail = "0",
            used = "0"
        )
        insertByDynamic(newEntity)
    }
    @Insert
    suspend fun insertByDynamic(entity: MeasureDynamic)

    @Query("SELECT * FROM t_measure_dynamic WHERE user_sn = :userSn")
    fun getAllDynamic(userSn: Int): List<MeasureDynamic>

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
    // JSONObject를 사용하는 경우 (org.json 라이브러리 필요)
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