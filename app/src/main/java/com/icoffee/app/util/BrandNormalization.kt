package com.icoffee.app.util

import java.text.Normalizer

object BrandNormalization {

    private val turkishMap = mapOf(
        'ç' to "c",
        'Ç' to "c",
        'ğ' to "g",
        'Ğ' to "g",
        'ı' to "i",
        'İ' to "i",
        'ö' to "o",
        'Ö' to "o",
        'ş' to "s",
        'Ş' to "s",
        'ü' to "u",
        'Ü' to "u"
    )

    fun normalizeBrandName(input: String): String {
        if (input.isBlank()) return ""
        val replaced = buildString {
            input.trim().forEach { ch ->
                append(turkishMap[ch] ?: ch)
            }
        }
        val normalized = Normalizer.normalize(replaced, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()
            .replace("[^a-z0-9\\s-]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
        return normalized
    }

    fun slugify(input: String): String {
        return normalizeBrandName(input)
            .replace("\\s+".toRegex(), "-")
            .replace("-+".toRegex(), "-")
            .trim('-')
    }
}
