package com.icoffee.app.data.menu

import com.icoffee.app.data.model.CachedDetectedMenuItem
import com.icoffee.app.data.model.NormalizedCoffeeType
import kotlin.math.roundToInt

object MenuCoffeeNormalizer {

    private data class CoffeeKeywordGroup(
        val type: NormalizedCoffeeType,
        val phrases: List<String>,
        val keywords: Set<String>
    )

    private data class LineDetection(
        val rawLine: String,
        val type: NormalizedCoffeeType,
        val confidence: Int
    )

    private val foodNoiseKeywords = setOf(
        "tea", "chai", "cay", "çay", "sandwich", "cake", "cookie", "muffin", "brownie",
        "croissant", "bagel", "salad", "soup", "juice", "smoothie", "water", "cola",
        "pasta", "pizza", "burger", "dessert", "tiramisu", "cheesecake"
    )

    private val groups: List<CoffeeKeywordGroup> = listOf(
        CoffeeKeywordGroup(
            type = NormalizedCoffeeType.ESPRESSO,
            phrases = listOf("espresso", "expresso", "espresso shot"),
            keywords = setOf("espresso", "expresso", "ristretto")
        ),
        CoffeeKeywordGroup(
            type = NormalizedCoffeeType.AMERICANO,
            phrases = listOf("americano", "caffe americano", "cafe americano"),
            keywords = setOf("americano", "americanoo")
        ),
        CoffeeKeywordGroup(
            type = NormalizedCoffeeType.CAPPUCCINO,
            phrases = listOf("cappuccino", "capuccino", "cappucino", "kapuccino", "kapucino"),
            keywords = setOf("cappuccino", "capuccino", "cappucino", "kapuccino", "kapucino")
        ),
        CoffeeKeywordGroup(
            type = NormalizedCoffeeType.LATTE,
            phrases = listOf("latte", "caffe latte", "cafe latte", "café latte", "latte macchiato"),
            keywords = setOf("latte", "lattee")
        ),
        CoffeeKeywordGroup(
            type = NormalizedCoffeeType.FLAT_WHITE,
            phrases = listOf("flat white", "flatwhite"),
            keywords = setOf("flat", "white", "flatwhite")
        ),
        CoffeeKeywordGroup(
            type = NormalizedCoffeeType.MACCHIATO,
            phrases = listOf("macchiato", "machiato", "caffe macchiato", "cafe macchiato"),
            keywords = setOf("macchiato", "machiato")
        ),
        CoffeeKeywordGroup(
            type = NormalizedCoffeeType.MOCHA,
            phrases = listOf("mocha", "cafe mocha", "caffe mocha", "mocca", "moka"),
            keywords = setOf("mocha", "mocca", "moka")
        ),
        CoffeeKeywordGroup(
            type = NormalizedCoffeeType.CORTADO,
            phrases = listOf("cortado", "kortado"),
            keywords = setOf("cortado", "kortado")
        ),
        CoffeeKeywordGroup(
            type = NormalizedCoffeeType.RISTRETTO,
            phrases = listOf("ristretto", "ristreto"),
            keywords = setOf("ristretto", "ristreto")
        ),
        CoffeeKeywordGroup(
            type = NormalizedCoffeeType.LUNGO,
            phrases = listOf("lungo", "caffe lungo", "cafe lungo"),
            keywords = setOf("lungo")
        ),
        CoffeeKeywordGroup(
            type = NormalizedCoffeeType.FILTER_V60,
            phrases = listOf("v60", "hario v60", "v 60"),
            keywords = setOf("v60", "hario")
        ),
        CoffeeKeywordGroup(
            type = NormalizedCoffeeType.POUR_OVER,
            phrases = listOf("pour over", "pour-over", "pourover", "drip coffee"),
            keywords = setOf("pour", "over", "pourover", "drip")
        ),
        CoffeeKeywordGroup(
            type = NormalizedCoffeeType.CHEMEX,
            phrases = listOf("chemex", "kemeks"),
            keywords = setOf("chemex", "kemeks")
        ),
        CoffeeKeywordGroup(
            type = NormalizedCoffeeType.AEROPRESS,
            phrases = listOf("aeropress", "aero press", "aeropres"),
            keywords = setOf("aeropress", "aeropres")
        ),
        CoffeeKeywordGroup(
            type = NormalizedCoffeeType.COLD_BREW,
            phrases = listOf("cold brew", "cold-brew", "coldbrew", "nitro cold brew"),
            keywords = setOf("cold", "brew", "coldbrew", "nitro")
        ),
        CoffeeKeywordGroup(
            type = NormalizedCoffeeType.ICED_COFFEE,
            phrases = listOf("iced coffee", "ice coffee", "iced latte", "iced americano"),
            keywords = setOf("iced", "ice", "coffee")
        ),
        CoffeeKeywordGroup(
            type = NormalizedCoffeeType.TURKISH_COFFEE,
            phrases = listOf("turkish coffee", "turk kahvesi", "turk kahve", "türk kahvesi"),
            keywords = setOf("turkish", "turk", "kahvesi", "kahve")
        ),
        CoffeeKeywordGroup(
            type = NormalizedCoffeeType.FILTER_COFFEE,
            phrases = listOf("filter coffee", "filtre kahve", "filter kaffee", "cafe filtre", "café filtre"),
            keywords = setOf("filter", "filtre", "kaffee", "coffee", "kahve")
        )
    )

    fun detectMenuItems(cleanedLines: List<String>): List<CachedDetectedMenuItem> {
        if (cleanedLines.isEmpty()) return emptyList()

        val lineDetections = cleanedLines
            .asSequence()
            .filterNot { isNoiseLine(it) }
            .mapNotNull { detectLine(it) }
            .toList()

        if (lineDetections.isEmpty()) return emptyList()

        return lineDetections
            .groupBy { it.type }
            .map { (type, detections) ->
                val top = detections.maxByOrNull { it.confidence } ?: return@map null
                val duplicateBonus = ((detections.size - 1) * 6).coerceAtMost(12)
                CachedDetectedMenuItem(
                    rawLine = top.rawLine,
                    normalizedType = type,
                    confidence = (top.confidence + duplicateBonus).coerceIn(38, 99)
                )
            }
            .filterNotNull()
            .sortedByDescending { it.confidence }
    }

    private fun detectLine(line: String): LineDetection? {
        val best = groups
            .map { group -> group to scoreLine(line, group) }
            .maxByOrNull { (_, score) -> score }
            ?: return null

        val score = best.second
        if (score < 0.42) return null

        val confidence = calculateConfidence(line = line, matchScore = score)
        return LineDetection(
            rawLine = line,
            type = best.first.type,
            confidence = confidence
        )
    }

    private fun scoreLine(line: String, group: CoffeeKeywordGroup): Double {
        val lowerLine = line.lowercase()
        val tokens = lowerLine.split(" ").filter { it.isNotBlank() }

        val phraseHits = group.phrases.count { lowerLine.contains(it) }
        val exactTokenHits = group.keywords.count { keyword -> tokens.any { it == keyword } }
        val fuzzyTokenHits = group.keywords.count { keyword ->
            tokens.any { token -> token.length >= 5 && isFuzzyMatch(token, keyword) }
        } - exactTokenHits

        var score = 0.0
        if (phraseHits > 0) {
            score += 0.55 + (phraseHits - 1) * 0.08
        }
        if (exactTokenHits > 0) {
            score += (exactTokenHits * 0.16).coerceAtMost(0.32)
        }
        if (fuzzyTokenHits > 0) {
            score += (fuzzyTokenHits * 0.08).coerceAtMost(0.16)
        }
        if (lowerLine.contains("/") && phraseHits > 0) {
            score += 0.04
        }
        return score.coerceAtMost(1.0)
    }

    private fun calculateConfidence(line: String, matchScore: Double): Int {
        val letters = line.count { it.isLetter() }
        val digits = line.count { it.isDigit() }
        val clarityRatio = if (line.isNotEmpty()) letters.toDouble() / line.length.toDouble() else 0.0
        val clarityBoost = (clarityRatio * 18.0).roundToInt()
        val digitPenalty = if (digits >= 4) 8 else if (digits >= 2) 4 else 0
        val lengthPenalty = if (line.length > 36) 6 else 0
        val base = (matchScore * 72.0).roundToInt() + 24
        return (base + clarityBoost - digitPenalty - lengthPenalty).coerceIn(0, 100)
    }

    private fun isNoiseLine(line: String): Boolean {
        val hasNoiseFood = foodNoiseKeywords.any { keyword -> line.contains(keyword) }
        val hasCoffeeSignal = groups.any { group -> group.phrases.any { line.contains(it) } }
        return hasNoiseFood && !hasCoffeeSignal
    }

    private fun isFuzzyMatch(token: String, keyword: String): Boolean {
        if (token == keyword) return true
        val distance = levenshtein(token, keyword)
        return when {
            keyword.length <= 5 -> distance <= 1
            keyword.length <= 8 -> distance <= 2
            else -> distance <= 2
        }
    }

    private fun levenshtein(lhs: String, rhs: String): Int {
        if (lhs == rhs) return 0
        if (lhs.isEmpty()) return rhs.length
        if (rhs.isEmpty()) return lhs.length

        val costs = IntArray(rhs.length + 1) { it }
        for (i in lhs.indices) {
            var previous = i
            costs[0] = i + 1
            for (j in rhs.indices) {
                val current = costs[j + 1]
                val substitutionCost = if (lhs[i] == rhs[j]) 0 else 1
                costs[j + 1] = minOf(
                    costs[j + 1] + 1,
                    costs[j] + 1,
                    previous + substitutionCost
                )
                previous = current
            }
        }
        return costs[rhs.length]
    }
}
