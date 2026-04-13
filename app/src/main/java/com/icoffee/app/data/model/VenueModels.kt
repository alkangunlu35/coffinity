package com.icoffee.app.data.model

data class Venue(
    val id: String,
    val displayName: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val menuFingerprint: String,    // SHA-256 of sorted NormalizedCoffeeType names
    val menuSignature: String,      // "AMERICANO,CAPPUCCINO,ESPRESSO,LATTE" (sorted, comma-sep)
    val coffeeCoverage: Int,        // number of distinct coffee types detected
    val scanCount: Int = 1,
    val lastSeenAt: Long,
    val createdAt: Long
)

data class VenueMatchResult(
    val venue: Venue?,
    val isNew: Boolean,
    val confidence: Int             // 0–100; 100 = exact fingerprint match
)

data class VenueAwareMenuScanResult(
    val scanResult: MenuScanResult,
    val venue: Venue?,
    val linkedEvents: List<CoffeeMeet>
)
