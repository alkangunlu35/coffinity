package com.icoffee.app.data.model

data class DetectedMenuItem(
    val rawLine: String,
    val normalizedType: NormalizedCoffeeType,
    val confidence: Int,
    val profile: CoffeeProfile,
    val matchResult: CoffeeMatchResult
)

enum class NormalizedCoffeeType {
    ESPRESSO,
    AMERICANO,
    CAPPUCCINO,
    LATTE,
    FLAT_WHITE,
    MACCHIATO,
    MOCHA,
    CORTADO,
    RISTRETTO,
    LUNGO,
    FILTER_V60,
    POUR_OVER,
    CHEMEX,
    AEROPRESS,
    COLD_BREW,
    ICED_COFFEE,
    TURKISH_COFFEE,
    FILTER_COFFEE,
    UNKNOWN
}

data class MenuScanResult(
    val scanId: String,
    val imageHash: String,
    val textHash: String,
    val venueHint: String?,
    val rawText: String,
    val cleanedLines: List<String>,
    val detectedItems: List<DetectedMenuItem>,
    val bestMatch: DetectedMenuItem?,
    val alternatives: List<DetectedMenuItem>,
    val scannedAt: Long,
    val fromCache: Boolean
)

data class CachedDetectedMenuItem(
    val rawLine: String,
    val normalizedType: NormalizedCoffeeType,
    val confidence: Int
)

data class CachedMenuEntry(
    val scanId: String,
    val imageHash: String,
    val textHash: String,
    val venueHint: String?,
    val rawText: String,
    val cleanedLines: List<String>,
    val detectedItems: List<CachedDetectedMenuItem>,
    val scannedAt: Long
)
