package com.icoffee.app.data.venue

import android.content.Context
import com.icoffee.app.data.db.MeetDatabase
import com.icoffee.app.data.db.VenueEntity
import com.icoffee.app.data.model.DetectedMenuItem
import com.icoffee.app.data.model.NormalizedCoffeeType
import com.icoffee.app.data.model.Venue
import com.icoffee.app.data.model.VenueMatchResult

object VenueRepository {

    private var database: MeetDatabase? = null

    fun initialize(context: Context) {
        database = MeetDatabase.getInstance(context)
    }

    private fun dao() = database?.venueDao()

    /**
     * Matches the current scan against known venues.
     * If no match found, registers as a new venue.
     * Returns [VenueMatchResult] with confidence 0 if menu has no detectable types.
     */
    suspend fun matchOrRegister(
        detectedItems: List<DetectedMenuItem>,
        venueHint: String?,
        latitude: Double = 0.0,
        longitude: Double = 0.0
    ): VenueMatchResult {
        val dao = dao() ?: return VenueMatchResult(null, isNew = false, confidence = 0)

        val signature = VenueFingerprinter.signature(detectedItems)
        if (signature.isBlank()) return VenueMatchResult(null, isNew = false, confidence = 0)

        val fingerprint = VenueFingerprinter.fingerprintFromSignature(signature)

        // Exact fingerprint match
        val exact = dao.getByFingerprint(fingerprint)
        if (exact != null) {
            dao.incrementScanCount(exact.id, System.currentTimeMillis())
            return VenueMatchResult(exact.toModel(), isNew = false, confidence = 100)
        }

        // Partial match from recent venues
        val candidates = dao.getRecent()
        val partialResult = VenueMatcher.match(fingerprint, signature, candidates)
        if (!partialResult.isNew && partialResult.venue != null) {
            dao.incrementScanCount(partialResult.venue.id, System.currentTimeMillis())
            return partialResult
        }

        // Register as new venue
        val newId = "venue-${fingerprint.take(12)}"
        val displayName = venueHint
            ?.trim()
            ?.replaceFirstChar { it.uppercaseChar() }
            ?.takeIf { it.isNotBlank() }
            ?: "Unnamed Café"
        val coverage = detectedItems.count { it.normalizedType != NormalizedCoffeeType.UNKNOWN }
        val now = System.currentTimeMillis()

        val entity = VenueEntity(
            id = newId,
            displayName = displayName,
            latitude = latitude,
            longitude = longitude,
            menuFingerprint = fingerprint,
            menuSignature = signature,
            coffeeCoverage = coverage,
            scanCount = 1,
            lastSeenAt = now,
            createdAt = now
        )
        dao.insert(entity)

        val newVenue = Venue(
            id = newId,
            displayName = displayName,
            latitude = latitude,
            longitude = longitude,
            menuFingerprint = fingerprint,
            menuSignature = signature,
            coffeeCoverage = coverage,
            scanCount = 1,
            lastSeenAt = now,
            createdAt = now
        )
        return VenueMatchResult(newVenue, isNew = true, confidence = 100)
    }
}

private fun VenueEntity.toModel(): Venue = Venue(
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
