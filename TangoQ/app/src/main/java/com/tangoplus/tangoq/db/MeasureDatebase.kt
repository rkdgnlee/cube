package com.tangoplus.tangoq.db

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Base64
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tangoplus.tangoq.db.SecurePreferencesManager.generateSecurePassphrase
import net.sqlcipher.database.SupportFactory
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.SecureRandom

@Database(entities = [MeasureInfo::class, MeasureStatic::class, MeasureDynamic::class], version = 8)
abstract class MeasureDatabase : RoomDatabase() {
    abstract fun measureDao(): MeasureDao


    companion object {

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
                    .build()
                INSTANCE = instance
                Log.d("Database", "MeasureDatabase created")
                instance
            }
        }

        private fun createMigration(fromVersion: Int, toVersion: Int) = object : Migration(fromVersion, toVersion) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 각 버전별 마이그레이션 로직
                when (fromVersion) {
                    6 -> {
                        // 6 -> 7 마이그레이션 로직
                    }
                    7 -> {
                        // 7 -> 8 마이그레이션 로직
                    }
                }
            }
        }

        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }

    }
}