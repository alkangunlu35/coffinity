package com.icoffee.app.data.menu

import android.content.Context
import android.content.SharedPreferences
import com.icoffee.app.data.model.CachedDetectedMenuItem
import com.icoffee.app.data.model.CachedMenuEntry
import com.icoffee.app.data.model.NormalizedCoffeeType
import org.json.JSONArray
import org.json.JSONObject

object MenuScanCacheRepository {
    private const val PREFS_NAME = "coffinity_menu_scan_cache"
    private const val KEY_ENTRIES = "entries_json"
    private const val MAX_CACHE_ENTRIES = 120

    private lateinit var prefs: SharedPreferences
    private var cachedEntries: MutableList<CachedMenuEntry> = mutableListOf()

    fun initialize(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        cachedEntries = loadEntries().toMutableList()
    }

    fun getByScanId(scanId: String): CachedMenuEntry? =
        entries().firstOrNull { it.scanId == scanId }

    fun getByImageHash(imageHash: String): CachedMenuEntry? =
        entries().firstOrNull { it.imageHash == imageHash }

    fun getByTextHash(textHash: String): CachedMenuEntry? =
        entries().firstOrNull { it.textHash == textHash }

    fun getByVenueHint(venueHint: String): CachedMenuEntry? {
        val normalized = venueHint.trim().lowercase()
        if (normalized.isBlank()) return null
        return entries()
            .filter { it.venueHint?.lowercase() == normalized }
            .maxByOrNull { it.scannedAt }
    }

    fun save(entry: CachedMenuEntry) {
        val list = entries().toMutableList()
        list.removeAll { it.scanId == entry.scanId || it.imageHash == entry.imageHash || it.textHash == entry.textHash }
        list.add(0, entry)
        cachedEntries = list.take(MAX_CACHE_ENTRIES).toMutableList()
        persistEntries(cachedEntries)
    }

    private fun entries(): List<CachedMenuEntry> {
        ensureInitialized()
        return cachedEntries
    }

    private fun ensureInitialized() {
        check(::prefs.isInitialized) {
            "MenuScanCacheRepository.initialize(context) must be called before use."
        }
    }

    private fun loadEntries(): List<CachedMenuEntry> {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(item.toCachedMenuEntry())
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun persistEntries(entries: List<CachedMenuEntry>) {
        val array = JSONArray().apply {
            entries.forEach { put(it.toJson()) }
        }
        prefs.edit().putString(KEY_ENTRIES, array.toString()).apply()
    }

    private fun CachedMenuEntry.toJson(): JSONObject = JSONObject().apply {
        put("scanId", scanId)
        put("imageHash", imageHash)
        put("textHash", textHash)
        put("venueHint", venueHint)
        put("rawText", rawText)
        put("scannedAt", scannedAt)
        put("cleanedLines", JSONArray().apply { cleanedLines.forEach { put(it) } })
        put(
            "detectedItems",
            JSONArray().apply {
                detectedItems.forEach { item ->
                    put(
                        JSONObject().apply {
                            put("rawLine", item.rawLine)
                            put("normalizedType", item.normalizedType.name)
                            put("confidence", item.confidence)
                        }
                    )
                }
            }
        )
    }

    private fun JSONObject.toCachedMenuEntry(): CachedMenuEntry {
        val lineArray = optJSONArray("cleanedLines") ?: JSONArray()
        val lines = buildList {
            for (index in 0 until lineArray.length()) {
                val line = lineArray.optString(index).orEmpty()
                if (line.isNotBlank()) add(line)
            }
        }

        val itemArray = optJSONArray("detectedItems") ?: JSONArray()
        val detectedItems = buildList {
            for (index in 0 until itemArray.length()) {
                val item = itemArray.optJSONObject(index) ?: continue
                val type = runCatching {
                    NormalizedCoffeeType.valueOf(item.optString("normalizedType"))
                }.getOrElse { NormalizedCoffeeType.UNKNOWN }
                add(
                    CachedDetectedMenuItem(
                        rawLine = item.optString("rawLine").orEmpty(),
                        normalizedType = type,
                        confidence = item.optInt("confidence", 0)
                    )
                )
            }
        }

        return CachedMenuEntry(
            scanId = optString("scanId").orEmpty(),
            imageHash = optString("imageHash").orEmpty(),
            textHash = optString("textHash").orEmpty(),
            venueHint = optString("venueHint").takeIf { it.isNotBlank() },
            rawText = optString("rawText").orEmpty(),
            cleanedLines = lines,
            detectedItems = detectedItems,
            scannedAt = optLong("scannedAt", 0L)
        )
    }
}
