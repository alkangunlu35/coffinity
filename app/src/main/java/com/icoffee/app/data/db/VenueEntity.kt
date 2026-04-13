package com.icoffee.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.icoffee.app.data.model.Venue

@Entity(tableName = "venues")
data class VenueEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val menuFingerprint: String,
    val menuSignature: String,
    val coffeeCoverage: Int,
    val scanCount: Int = 1,
    val lastSeenAt: Long,
    val createdAt: Long
)

fun VenueEntity.toVenue(): Venue = Venue(
    id = id,
    displayName = displayName,
    latitude = latitude,
    longitude = longitude,
    menuFingerprint = menuFingerprint,
    menuSignature = menuSignature,
    coffeeCoverage = coffeeCoverage,
    scanCount = scanCount,
    lastSeenAt = lastSeenAt,
    createdAt = createdAt
)
