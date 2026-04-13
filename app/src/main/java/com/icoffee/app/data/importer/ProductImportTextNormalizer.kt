package com.icoffee.app.data.importer

import java.util.Locale

internal fun normalizeSlug(value: String): String {
    val normalized = value
        .trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
    return normalized.ifBlank { "product" }
}

internal fun normalizeTastingNotes(value: String): List<String> {
    return value
        .split(',', '\n')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase(Locale.ROOT) }
}
