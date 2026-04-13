package com.icoffee.app.data.menu

import java.text.Normalizer
import java.util.Locale

object MenuTextCleaner {

    private val pricePattern = Regex(
        pattern = """^[\p{Sc}]?\s*\d+(?:[.,]\d{1,2})?\s*(tl|try|usd|eur|€|\$|₺)?$""",
        option = RegexOption.IGNORE_CASE
    )
    private val trailingPricePattern = Regex(
        pattern = """\s+[\p{Sc}]?\s*\d+(?:[.,]\d{1,2})\s*(tl|try|usd|eur|€|\$|₺)?$""",
        option = RegexOption.IGNORE_CASE
    )

    fun cleanLines(rawText: String): List<String> {
        if (rawText.isBlank()) return emptyList()

        return rawText
            .replace("\r", "\n")
            .replace("•", "\n")
            .replace("·", "\n")
            .lineSequence()
            .map { normalizeLine(it) }
            .map { it.replace(trailingPricePattern, "").trim() }
            .filter { it.isNotBlank() }
            .filterNot { it.matches(pricePattern) }
            .filter { line ->
                val letters = line.count { it.isLetter() }
                letters >= 2
            }
            .distinct()
            .toList()
    }

    fun normalizeForHash(rawText: String): String =
        cleanLines(rawText)
            .joinToString(separator = "\n")

    private fun normalizeLine(input: String): String {
        val lowered = input.trim().lowercase(Locale.ROOT)
        val ascii = Normalizer
            .normalize(lowered, Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "")
        return ascii
            .replace(Regex("""[^a-z0-9çğıöşü\s/+&-]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
