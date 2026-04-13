package com.icoffee.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VenueDao {

    @Query("SELECT * FROM venues WHERE menuFingerprint = :fingerprint LIMIT 1")
    suspend fun getByFingerprint(fingerprint: String): VenueEntity?

    @Query("SELECT * FROM venues ORDER BY lastSeenAt DESC LIMIT 50")
    suspend fun getRecent(): List<VenueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VenueEntity)

    @Query("UPDATE venues SET scanCount = scanCount + 1, lastSeenAt = :lastSeenAt WHERE id = :id")
    suspend fun incrementScanCount(id: String, lastSeenAt: Long)
}
