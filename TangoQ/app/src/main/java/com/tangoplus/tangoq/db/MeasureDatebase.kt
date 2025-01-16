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

@Database(entities = [MeasureInfo::class, MeasureStatic::class, MeasureDynamic::class], version = 12)
abstract class MeasureDatabase : RoomDatabase() {
    abstract fun measureDao(): MeasureDao

    companion object {
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE t_measure_info ADD COLUMN risk_result_ment TEXT DEFAULT ''")
            }
        }
        @Volatile
        private var INSTANCE: MeasureDatabase? = null
        fun getDatabase(context: Context): MeasureDatabase {
            Log.d("Database", "Initializing MeasureDatabase")
            return INSTANCE ?: synchronized(this) {
                val passphrase = generateSecurePassphrase(context)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MeasureDatabase::class.java,
                    "measure_database"
                ).openHelperFactory(SupportFactory(passphrase))
                    .addMigrations(MIGRATION_11_12)
                    .build()
                INSTANCE = instance
                Log.d("Database", "MeasureDatabase created")
                instance
            }
        }

        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }

    }
}