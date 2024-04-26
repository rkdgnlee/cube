//package com.example.mhg.Room
//
//import android.content.Context
//import androidx.room.Database
//import androidx.room.Room
//import androidx.room.RoomDatabase
//@Database(entities = [Exercise::class], version = 1)
//abstract class ExerciseDatabase: RoomDatabase() {
//    abstract fun ExerciseDao(): ExerciseDao
//    companion object{
//        @Volatile
//        private var INSTANCE : ExerciseDatabase? = null
//
//        private fun buildDatabase(context: Context) : ExerciseDatabase =
//            Room.databaseBuilder(
//                context.applicationContext,
//                ExerciseDatabase::class.java,
//                "t_exercise_description"
//            ).build()
//
//        fun getInstance(context: Context) : ExerciseDatabase =
//            INSTANCE ?: synchronized(this) {
//                INSTANCE ?: buildDatabase(context).also {  INSTANCE = it }
//            }
//    }
//}