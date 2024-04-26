//package com.example.mhg.Room
//
//import androidx.room.Dao
//import androidx.room.Delete
//import androidx.room.Insert
//import androidx.room.Query
//import androidx.room.Update
//
//@Dao
//interface ExerciseDao{
//    @Query("SELECT * FROM exercise")
//    suspend fun getAll(): List<Exercise>
//    @Query("SELECT * FROM exercise WHERE exercise_name = :name")
//    suspend fun findByName(name:String) : Exercise?
//
//    @Query("SELECT * FROM exercise WHERE exercise_type_name = :type")
//    suspend fun findByType(type:String) : List<Exercise>
//
//    @Insert
//    suspend fun insert(exercise: Exercise)
//
//    @Update
//    suspend fun update(exercise: Exercise)
//
//    @Delete
//    suspend fun delete(exercise: Exercise)
//}