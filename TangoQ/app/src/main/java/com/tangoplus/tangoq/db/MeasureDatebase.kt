package com.tangoplus.tangoq.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tangoplus.tangoq.function.SecurePreferencesManager.generateSecurePassphrase
import net.sqlcipher.database.SupportFactory

@Database(entities = [MeasureInfo::class, MeasureStatic::class, MeasureDynamic::class], version = 15)
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
                db.execSQL("ALTER TABLE t_measure_static ADD COLUMN mobile_info_sn INTEGER")
                db.execSQL("ALTER TABLE t_measure_dynamic ADD COLUMN mobile_info_sn INTEGER")
            }
        }
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE t_measure_info ADD COLUMN show_lines INTEGER")
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
                    .addMigrations(MIGRATION_13_14)
                    .addMigrations(MIGRATION_14_15)
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