package com.tangoplus.tangoq.Room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageDao {
    @Insert
    suspend fun insert(message: Message)

    @Query("select * from Message order by timestamp desc")
    suspend fun getAllMessages() : List<Message>

    @Query("DELETE FROM MESSAGE WHERE id = :messageId")
    suspend fun deleteMessage(messageId: Long)
}