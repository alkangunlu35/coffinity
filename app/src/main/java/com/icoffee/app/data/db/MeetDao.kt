package com.icoffee.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetDao {

    @Query("SELECT * FROM meet_events ORDER BY scheduledAt ASC")
    fun getAllFlow(): Flow<List<MeetEntity>>

    @Query("SELECT * FROM meet_events ORDER BY scheduledAt ASC")
    suspend fun getAllSync(): List<MeetEntity>

    @Query("SELECT * FROM meet_events WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MeetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MeetEntity)

    @Update
    suspend fun update(entity: MeetEntity)

    @Query("UPDATE meet_events SET participantsJson = :participantsJson WHERE id = :id")
    suspend fun updateParticipants(id: String, participantsJson: String)

    @Query("DELETE FROM meet_events WHERE id = :id")
    suspend fun deleteById(id: String)
}
