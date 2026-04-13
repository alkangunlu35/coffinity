package com.icoffee.app.data.normalization

import com.icoffee.app.data.model.AcidityLevel
import com.icoffee.app.data.model.CoffeeProfile
import com.icoffee.app.data.model.CoffeeType
import com.icoffee.app.data.model.OpenFoodFactsProduct
import com.icoffee.app.data.model.RoastLevel
import com.icoffee.app.data.model.StrengthLevel
import com.icoffee.app.data.model.TasteNote
import java.util.Locale

object CoffeeProfileNormalizer {

    fun normalize(product: OpenFoodFactsProduct): CoffeeProfile {
        val corpus = buildCorpus(product)

        val coffeeType = inferCoffeeType(corpus)
        val roastLevel = inferRoastLevel(corpus)
        val origin = inferOriginCountry(corpus)
        val tasteNotes = inferTasteNotes(corpus, roastLevel, origin)
        val strength = inferStrength(corpus, roastLevel, coffeeType)
        val acidity = inferAcidity(corpus, roastLevel, origin)
        val milkFriendly = inferMilkFriendly(corpus, roastLevel, tasteNotes, acidity)
        val confidence = inferConfidence(
            product = product,
            coffeeType = coffeeType,
            roastLevel = roastLevel,
            origin = origin,
            tasteNotes = tasteNotes,
            strength = strength,
            acidity = acidity
        )

        return CoffeeProfile(
            barcode = product.barcode,
            productName = product.name,
            brand = product.brand,
            imageUrl = product.imageUrl,
            coffeeType = coffeeType,
            roastLevel = roastLevel,
            originCountry = origin,
            tasteNotes = tasteNotes,
            strength = strength,
            acidity = acidity,
            milkFriendly = milkFriendly,
            confidenceScore = confidence
        )
    }

    private fun buildCorpus(product: OpenFoodFactsProduct): String {
        val raw = listOf(
            product.name,
            product.brand,
            product.countries,
            product.categories,
            product.genericName,
            product.quantity,
            product.packaging,
            product.ingredientsText,
            product.labels
        )
            .filterNotNull()
            .joinToString(" ")

        return raw.lowercase(Locale.ROOT)
    }

    private fun inferCoffeeType(corpus: String): CoffeeType = when {
        hasAny(corpus, "whole bean", "whole beans", "coffee beans", "beans") -> CoffeeType.WHOLE_BEAN
        hasAny(corpus, "ground coffee", "ground", "filter coffee", "grinded") -> CoffeeType.GROUND
        hasAny(corpus, "instant", "soluble", "3 in 1", "3in1") -> CoffeeType.INSTANT
        hasAny(corpus, "capsule", "capsules", "pod", "pods", "nespresso") -> CoffeeType.CAPSULE
        hasAny(corpus, "cold brew", "iced coffee", "ready to drink", "rtd", "ready-to-drink") -> CoffeeType.READY_TO_DRINK
        else -> CoffeeType.UNKNOWN
    }

    private fun inferRoastLevel(corpus: String): RoastLevel = when {
        hasAny(corpus, "light roast") -> RoastLevel.LIGHT
        hasAny(corpus, "medium roast", "medium-roast") -> RoastLevel.MEDIUM
        hasAny(corpus, "dark roast", "espresso roast", "intense", "extra dark") -> RoastLevel.DARK
        else -> RoastLevel.UNKNOWN
    }

    private fun inferOriginCountry(corpus: String): String? {
        val origins = listOf(
            "ethiopia" to "Ethiopia",
            "colombia" to "Colombia",
            "brazil" to "Brazil",
            "kenya" to "Kenya",
            "guatemala" to "Guatemala",
            "costa rica" to "Costa Rica",
            "peru" to "Peru",
            "honduras" to "Honduras",
            "sumatra" to "Sumatra",
            "rwanda" to "Rwanda",
            "panama" to "Panama",
            "indonesia" to "Indonesia",
            "nicaragua" to "Nicaragua"
        )
        return origins.firstOrNull { (needle, _) -> corpus.contains(needle) }?.second
    }

    private fun inferTasteNotes(
        corpus: String,
        roastLevel: RoastLevel,
        origin: String?
    ): List<TasteNote> {
        val notes = linkedSetOf<TasteNote>()

        when (origin) {
            "Ethiopia" -> notes += listOf(TasteNote.FLORAL, TasteNote.FRUITY, TasteNote.BRIGHT)
            "Colombia" -> notes += listOf(TasteNote.CHOCOLATE, TasteNote.NUTTY, TasteNote.SMOOTH)
            "Brazil" -> notes += listOf(TasteNote.NUTTY, TasteNote.CHOCOLATE, TasteNote.CARAMEL, TasteNote.SMOOTH)
            "Kenya" -> notes += listOf(TasteNote.BRIGHT, TasteNote.FRUITY)
        }

        when (roastLevel) {
            RoastLevel.LIGHT -> notes += listOf(TasteNote.BRIGHT, TasteNote.FLORAL, TasteNote.FRUITY)
            RoastLevel.MEDIUM -> notes += TasteNote.SMOOTH
            RoastLevel.DARK -> notes += listOf(TasteNote.BOLD, TasteNote.SMOKY)
            RoastLevel.UNKNOWN -> Unit
        }

        val keywordRules = mapOf(
            "chocolate" to TasteNote.CHOCOLATE,
            "nutty" to TasteNote.NUTTY,
            "fruity" to TasteNote.FRUITY,
            "floral" to TasteNote.FLORAL,
            "caramel" to TasteNote.CARAMEL,
            "smooth" to TasteNote.SMOOTH,
            "bold" to TasteNote.BOLD,
            "smoky" to TasteNote.SMOKY,
            "bright" to TasteNote.BRIGHT
        )
        keywordRules.forEach { (keyword, note) ->
            if (corpus.contains(keyword)) {
                notes += note
            }
        }

        return notes.take(6)
    }

    private fun inferStrength(
        corpus: String,
        roastLevel: RoastLevel,
        coffeeType: CoffeeType
    ): StrengthLevel {
        if (hasAny(corpus, "espresso", "intense", "strong")) return StrengthLevel.HIGH
        return when {
            roastLevel == RoastLevel.DARK -> StrengthLevel.HIGH
            roastLevel == RoastLevel.MEDIUM -> StrengthLevel.MEDIUM
            coffeeType == CoffeeType.READY_TO_DRINK && hasAny(corpus, "sweet", "latte", "mocha") -> StrengthLevel.LOW
            coffeeType == CoffeeType.INSTANT && hasAny(corpus, "latte", "mix", "3 in 1", "3in1") -> StrengthLevel.LOW
            roastLevel == RoastLevel.LIGHT -> StrengthLevel.MEDIUM
            else -> StrengthLevel.UNKNOWN
        }
    }

    private fun inferAcidity(
        corpus: String,
        roastLevel: RoastLevel,
        origin: String?
    ): AcidityLevel = when {
        origin in listOf("Ethiopia", "Kenya", "Rwanda") -> AcidityLevel.HIGH
        roastLevel == RoastLevel.LIGHT || hasAny(corpus, "bright", "citrus", "floral") -> AcidityLevel.HIGH
        origin == "Colombia" || roastLevel == RoastLevel.MEDIUM || hasAny(corpus, "balanced") -> AcidityLevel.MEDIUM
        origin == "Brazil" || roastLevel == RoastLevel.DARK || hasAny(corpus, "chocolate", "smooth", "low acidity") -> AcidityLevel.LOW
        else -> AcidityLevel.UNKNOWN
    }

    private fun inferMilkFriendly(
        corpus: String,
        roastLevel: RoastLevel,
        notes: List<TasteNote>,
        acidity: AcidityLevel
    ): Boolean {
        if (hasAny(corpus, "espresso blend", "latte", "cappuccino", "mocha")) return true
        if (roastLevel == RoastLevel.DARK) return true

        val creamyNotes = setOf(TasteNote.CHOCOLATE, TasteNote.NUTTY, TasteNote.CARAMEL, TasteNote.SMOOTH)
        if (notes.any { it in creamyNotes }) return true

        if (acidity == AcidityLevel.HIGH && notes.any { it == TasteNote.FLORAL || it == TasteNote.BRIGHT }) {
            return false
        }

        return false
    }

    private fun inferConfidence(
        product: OpenFoodFactsProduct,
        coffeeType: CoffeeType,
        roastLevel: RoastLevel,
        origin: String?,
        tasteNotes: List<TasteNote>,
        strength: StrengthLevel,
        acidity: AcidityLevel
    ): Int {
        var score = 20
        if (product.name.isNotBlank()) score += 10
        if (coffeeType != CoffeeType.UNKNOWN) score += 20
        if (roastLevel != RoastLevel.UNKNOWN) score += 15
        if (!origin.isNullOrBlank()) score += 15
        score += (tasteNotes.size.coerceAtMost(5) * 5)
        if (strength != StrengthLevel.UNKNOWN) score += 8
        if (acidity != AcidityLevel.UNKNOWN) score += 7

        return score.coerceIn(20, 98)
    }

    private fun hasAny(corpus: String, vararg needles: String): Boolean =
        needles.any { corpus.contains(it) }
}
