package com.tangoplus.tangoq.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MeasureInfo::class, MeasureStatic::class, MeasureDynamic::class], version = 8)
abstract class MeasureDatabase : RoomDatabase() {
    abstract fun measureDao(): MeasureDao


    companion object {
        @Volatile
        private var INSTANCE: MeasureDatabase? = null

        fun getDatabase(context: Context): MeasureDatabase {
            Log.d("Database", "Initializing MeasureDatabase")
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MeasureDatabase::class.java,
                    "measure_database"
                ).build()
                INSTANCE = instance
                Log.d("Database", "MeasureDatabase created")
                instance
            }
        }
    }
}