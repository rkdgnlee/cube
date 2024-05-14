package com.tangoplus.tangoq.Room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val message: String,
    val timestamp : Long = System.currentTimeMillis(),
    val route : String
)
