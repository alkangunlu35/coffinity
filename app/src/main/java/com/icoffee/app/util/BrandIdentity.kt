package com.icoffee.app.util

private val FORBIDDEN_BRAND_ID_PREFIXES = listOf(
    "brand_suggestion_",
    "suggestion_action_",
    "wrapper_",
    "relation_",
    "temp_",
    "import_row_",
    "ui_",
    "cache_"
)

object BrandIdentity {

    fun normalize(rawBrandId: String?): String = rawBrandId.orEmpty().trim()

    fun isValidBrandId(rawBrandId: String?): Boolean {
        val normalized = normalize(rawBrandId)
        if (normalized.isBlank()) return false
        if (normalized.contains('/')) return false
        val lowered = normalized.lowercase()
        return FORBIDDEN_BRAND_ID_PREFIXES.none { lowered.startsWith(it) }
    }
}

