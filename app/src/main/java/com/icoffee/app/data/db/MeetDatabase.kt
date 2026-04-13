package com.icoffee.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.icoffee.app.data.model.BusinessOffer
import com.icoffee.app.data.model.CoffeeMeet
import com.icoffee.app.data.model.EventHostType
import com.icoffee.app.data.model.MeetEventType
import com.icoffee.app.data.model.UserType
import org.json.JSONArray

@Database(entities = [MeetEntity::class, VenueEntity::class], version = 2, exportSchema = false)
abstract class MeetDatabase : RoomDatabase() {
    abstract fun meetDao(): MeetDao
    abstract fun venueDao(): VenueDao

    companion object {
        @Volatile private var instance: MeetDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS venues (
                        id TEXT NOT NULL PRIMARY KEY,
                        displayName TEXT NOT NULL,
                        latitude REAL NOT NULL DEFAULT 0.0,
                        longitude REAL NOT NULL DEFAULT 0.0,
                        menuFingerprint TEXT NOT NULL,
                        menuSignature TEXT NOT NULL,
                        coffeeCoverage INTEGER NOT NULL,
                        scanCount INTEGER NOT NULL DEFAULT 1,
                        lastSeenAt INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): MeetDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MeetDatabase::class.java,
                    "meet_events.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}

// ─── Serialization helpers ───────────────────────────────────────────────────

private val gson = Gson()

internal fun serializeParticipants(list: List<String>): String {
    val arr = JSONArray()
    list.forEach { arr.put(it) }
    return arr.toString()
}

internal fun parseParticipants(json: String): List<String> {
    if (json.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { arr.getString(it) }
    } catch (_: Exception) {
        emptyList()
    }
}

internal fun serializeBusinessOffer(offer: BusinessOffer): String = gson.toJson(offer)

internal fun parseBusinessOffer(json: String): BusinessOffer? {
    if (json.isBlank()) return null
    return try {
        gson.fromJson(json, BusinessOffer::class.java)
    } catch (_: Exception) {
        null
    }
}

// ─── Entity ↔ Domain mappers ─────────────────────────────────────────────────

fun CoffeeMeet.toEntity(): MeetEntity = MeetEntity(
    id = id,
    title = title,
    description = description,
    locationName = locationName,
    latitude = latitude,
    longitude = longitude,
    scheduledAt = scheduledAt,
    timeLabel = time,
    purpose = purpose,
    participantsJson = serializeParticipants(participants),
    maxParticipants = maxParticipants,
    hostId = hostId,
    hostUserType = hostUserType?.name,
    hostType = hostType.name,
    eventType = eventType.name,
    brewingType = brewingType,
    businessOfferJson = businessOffer?.let { serializeBusinessOffer(it) }
)

fun MeetEntity.toCoffeeMeet(currentUserId: String = ""): CoffeeMeet {
    val hostUserTypeEnum = hostUserType?.let { runCatching { UserType.valueOf(it) }.getOrNull() }
    val hostTypeEnum = runCatching { EventHostType.valueOf(hostType) }.getOrDefault(EventHostType.PERSONAL)
    val eventTypeEnum = runCatching { MeetEventType.valueOf(eventType) }.getOrDefault(MeetEventType.COMMUNITY)
    return CoffeeMeet(
        id = id,
        title = title,
        description = description,
        locationName = locationName,
        latitude = latitude,
        longitude = longitude,
        scheduledAt = scheduledAt,
        time = timeLabel,
        purpose = purpose,
        participants = parseParticipants(participantsJson),
        maxParticipants = maxParticipants,
        hostId = hostId,
        hostUserType = hostUserTypeEnum,
        hostType = hostTypeEnum,
        eventType = eventTypeEnum,
        brewingType = brewingType,
        businessOffer = businessOfferJson?.let { parseBusinessOffer(it) },
        isCreatedByUser = (hostId == currentUserId && currentUserId.isNotBlank())
    )
}
