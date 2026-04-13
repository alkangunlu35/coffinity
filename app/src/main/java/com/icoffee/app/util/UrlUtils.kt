package com.icoffee.app.util

import java.net.URI

object UrlUtils {

    fun normalizeHttpUrlOrNull(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return null
        val candidate = if (value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)
        ) {
            value
        } else {
            "https://$value"
        }
        val uri = runCatching { URI(candidate) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase().orEmpty()
        val host = uri.host?.lowercase().orEmpty()
        if (scheme !in setOf("http", "https")) return null
        if (host.isBlank()) return null
        return uri.toString()
    }

    fun normalizeInstagramHandleOrNull(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return null
        val compact = value.lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .removePrefix("instagram.com/")
            .removePrefix("@")
            .trim()
            .trim('/')
        return compact.takeIf { it.isNotBlank() }?.let { "@$it" }
    }

    fun extractWebsiteHost(rawUrl: String?): String? {
        val normalized = normalizeHttpUrlOrNull(rawUrl) ?: return null
        val host = runCatching { URI(normalized).host }.getOrNull()?.trim()?.lowercase().orEmpty()
        if (host.isBlank()) return null
        return host.removePrefix("www.")
    }
}
