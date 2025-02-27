package com.tangoplus.tangoq.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tangoplus.tangoq.function.SecurePreferencesManager.generateSecurePassphrase
import net.sqlcipher.database.SupportFactory

@Database(entities = [MeasureInfo::class, MeasureStatic::class, MeasureDynamic::class], version = 13)
abstract class MeasureDatabase : RoomDatabase() {
    abstract fun measureDao(): MeasureDao

    companion object {
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE t_measure_info ADD COLUMN risk_result_ment TEXT DEFAULT ''")
            }
        }
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE t_measure_info ADD COLUMN kakao_send_count TEXT DEFAULT ''")
                db.execSQL("ALTER TABLE t_measure_info ADD COLUMN kakao_send_date TEXT DEFAULT ''")
            }
        }
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE t_measure_info ADD COLUMN kakao_send_count_temp INTEGER DEFAULT 0")
                db.execSQL("UPDATE t_measure_info SET kakao_send_count_temp = CASE WHEN kakao_send_count IS NULL OR kakao_send_count = '' THEN 0 ELSE CAST(kakao_send_count AS INTEGER) END")
                db.execSQL("ALTER TABLE t_measure_info DROP COLUMN kakao_send_count")
                db.execSQL("ALTER TABLE t_measure_info RENAME COLUMN kakao_send_count_temp TO kakao_send_count")

                // kakao_send_date의 기본값 변경
                db.execSQL("UPDATE t_measure_info SET kakao_send_date = '0000-00-00 00:00:00' WHERE kakao_send_date IS NULL OR kakao_send_date = ''")
            }
        }
        @Volatile
        private var INSTANCE: MeasureDatabase? = null
        fun getDatabase(context: Context): MeasureDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphrase = generateSecurePassphrase(context)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MeasureDatabase::class.java,
                    "measure_database"
                ).openHelperFactory(SupportFactory(passphrase))
                    .addMigrations(MIGRATION_11_12)
                    .addMigrations(MIGRATION_12_13)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }

    }
}