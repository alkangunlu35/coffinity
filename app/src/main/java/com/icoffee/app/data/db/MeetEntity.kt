package com.icoffee.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meet_events")
data class MeetEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val scheduledAt: Long,          // epoch millis; 0 = unknown
    val timeLabel: String,          // formatted display label
    val purpose: String,
    val participantsJson: String,   // JSON array of user ID strings
    val maxParticipants: Int,
    val hostId: String,
    val hostUserType: String?,      // "NORMAL" | "BUSINESS" | null
    val hostType: String,           // "PERSONAL" | "BUSINESS"
    val eventType: String,          // "COMMUNITY" | "BUSINESS"
    val brewingType: String?,
    val businessOfferJson: String?  // JSON-encoded BusinessOffer or null
)
