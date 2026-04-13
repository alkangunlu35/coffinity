package com.icoffee.app.data.menu

import android.content.Context
import com.icoffee.app.data.matching.BasicMatchScoreEngine
import com.icoffee.app.data.model.CachedMenuEntry
import com.icoffee.app.data.model.DetectedMenuItem
import com.icoffee.app.data.model.MenuScanResult
import com.icoffee.app.data.model.NormalizedCoffeeType
import com.icoffee.app.data.profile.UserTasteProfileRepository
import java.security.MessageDigest
import java.util.Locale

object MenuScanRepository {

    fun initialize(context: Context) {
        MenuScanCacheRepository.initialize(context)
    }

    fun getCachedByImageHash(imageHash: String): MenuScanResult? {
        val cached = MenuScanCacheRepository.getByImageHash(imageHash) ?: return null
        return buildResult(cached, fromCache = true, trackTasteSignal = false)
    }

    fun getByScanId(scanId: String): MenuScanResult? {
        val cached = MenuScanCacheRepository.getByScanId(scanId) ?: return null
        return buildResult(cached, fromCache = true, trackTasteSignal = false)
    }

    fun processRawMenuText(
        rawText: String,
        imageHash: String,
        venueHint: String? = null
    ): MenuScanResult {
        val cleanedLines = MenuTextCleaner.cleanLines(rawText)
        val canonicalText = MenuTextCleaner.normalizeForHash(rawText)
        val textHash = hashSha256(canonicalText.ifBlank { "image:$imageHash" })
        val normalizedVenue = venueHint?.normalizeVenueHint() ?: extractVenueHint(cleanedLines)

        MenuScanCacheRepository.getByTextHash(textHash)?.let { cached ->
            return buildResult(cached, fromCache = true, trackTasteSignal = false)
        }

        if (!normalizedVenue.isNullOrBlank()) {
            val venueCached = MenuScanCacheRepository.getByVenueHint(normalizedVenue)
            if (venueCached != null && hasSimilarMenuLines(cleanedLines, venueCached.cleanedLines)) {
                val reused = venueCached.copy(
                    scanId = "menu-${textHash.take(12)}",
                    imageHash = imageHash,
                    textHash = textHash,
                    venueHint = normalizedVenue,
                    rawText = rawText,
                    cleanedLines = cleanedLines,
                    scannedAt = System.currentTimeMillis()
                )
                MenuScanCacheRepository.save(reused)
                return buildResult(reused, fromCache = true, trackTasteSignal = false)
            }
        }

        val detectedItems = MenuCoffeeNormalizer.detectMenuItems(cleanedLines)
        val entry = CachedMenuEntry(
            scanId = "menu-${textHash.take(12)}",
            imageHash = imageHash,
            textHash = textHash,
            venueHint = normalizedVenue,
            rawText = rawText,
            cleanedLines = cleanedLines,
            detectedItems = detectedItems,
            scannedAt = System.currentTimeMillis()
        )
        MenuScanCacheRepository.save(entry)
        return buildResult(entry, fromCache = false, trackTasteSignal = true)
    }

    fun manualSelection(
        type: NormalizedCoffeeType,
        referenceScanId: String = "manual"
    ): DetectedMenuItem {
        val profile = MenuCoffeeProfileFactory.createProfile(
            type = type,
            confidence = 62,
            scanId = referenceScanId,
            venueHint = null
        )
        val userProfile = UserTasteProfileRepository.currentProfile()
        val match = BasicMatchScoreEngine.calculate(profile, userProfile)
        UserTasteProfileRepository.onMenuItemViewed(profile)
        return DetectedMenuItem(
            rawLine = type.name.lowercase(Locale.ROOT),
            normalizedType = type,
            confidence = 62,
            profile = profile,
            matchResult = match
        )
    }

    private fun buildResult(
        entry: CachedMenuEntry,
        fromCache: Boolean,
        trackTasteSignal: Boolean
    ): MenuScanResult {
        val userProfile = UserTasteProfileRepository.currentProfile()
        val detectedItems = entry.detectedItems
            .map { cachedItem ->
                val profile = MenuCoffeeProfileFactory.createProfile(
                    type = cachedItem.normalizedType,
                    confidence = cachedItem.confidence,
                    scanId = entry.scanId,
                    venueHint = entry.venueHint
                )
                val match = BasicMatchScoreEngine.calculate(profile, userProfile)
                DetectedMenuItem(
                    rawLine = cachedItem.rawLine,
                    normalizedType = cachedItem.normalizedType,
                    confidence = cachedItem.confidence,
                    profile = profile,
                    matchResult = match
                )
            }
            .sortedWith(
                compareByDescending<DetectedMenuItem> { it.matchResult.score }
                    .thenByDescending { it.confidence }
            )

        val best = detectedItems.firstOrNull()
        if (trackTasteSignal && best != null) {
            UserTasteProfileRepository.onMenuItemViewed(best.profile)
        }

        return MenuScanResult(
            scanId = entry.scanId,
            imageHash = entry.imageHash,
            textHash = entry.textHash,
            venueHint = entry.venueHint,
            rawText = entry.rawText,
            cleanedLines = entry.cleanedLines,
            detectedItems = detectedItems,
            bestMatch = best,
            alternatives = detectedItems.drop(1),
            scannedAt = entry.scannedAt,
            fromCache = fromCache
        )
    }

    private fun extractVenueHint(cleanedLines: List<String>): String? {
        return cleanedLines
            .take(4)
            .firstOrNull { line ->
                line.length in 3..42 &&
                    line.count { it.isLetter() } >= 4 &&
                    line.count { it.isDigit() } <= 2 &&
                    MenuCoffeeNormalizer.detectMenuItems(listOf(line)).isEmpty()
            }
            ?.normalizeVenueHint()
    }

    private fun String.normalizeVenueHint(): String =
        trim().replace(Regex("""\s+"""), " ").lowercase(Locale.ROOT)

    private fun hasSimilarMenuLines(first: List<String>, second: List<String>): Boolean {
        if (first.isEmpty() || second.isEmpty()) return false
        val a = first.take(25).toSet()
        val b = second.take(25).toSet()
        val intersection = a.intersect(b).size.toDouble()
        val union = a.union(b).size.toDouble().coerceAtLeast(1.0)
        val score = intersection / union
        return score >= 0.52
    }

    private fun hashSha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
