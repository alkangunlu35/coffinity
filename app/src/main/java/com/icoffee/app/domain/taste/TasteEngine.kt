package com.icoffee.app.domain.taste

import com.icoffee.app.data.model.AcidityLevel
import com.icoffee.app.data.model.CoffeeType
import com.icoffee.app.data.model.RoastLevel
import com.icoffee.app.data.model.StrengthLevel
import com.icoffee.app.data.model.TasteNote
import com.icoffee.app.data.model.UserTasteProfile
import com.icoffee.app.data.profile.FavoriteMenuPick
import com.icoffee.app.data.profile.FavoriteScanProduct
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

data class TasteEngineInput(
    val baseProfile: UserTasteProfile,
    val scanHistory: List<FavoriteScanProduct>,
    val favoriteScans: List<FavoriteScanProduct>,
    val favoriteMenuPicks: List<FavoriteMenuPick>
)

object TasteEngine {
    private const val SCAN_WEIGHT = 1
    private const val FAVORITE_WEIGHT = 4
    private const val MENU_PICK_WEIGHT = 1

    private const val NOT_ENOUGH_DATA_THRESHOLD = 3
    private const val READY_DATA_THRESHOLD = 6

    fun build(input: TasteEngineInput): TasteProfile {
        val noteScores = mutableMapOf<TasteNote, Int>()
        val roastScores = mutableMapOf<RoastLevel, Int>()
        val acidityScores = mutableMapOf<AcidityLevel, Int>()
        val bodyScores = mutableMapOf<StrengthLevel, Int>()
        val originScores = mutableMapOf<String, Int>()
        val coffeeTypeScores = mutableMapOf<CoffeeType, Int>()
        val brewStyleScores = mutableMapOf<BrewStylePreference, Int>()
        val signals = mutableListOf<TasteSignal>()

        var milkScore = 0
        var totalSignalWeight = 0

        fun registerSignal(
            dimension: TasteSignalDimension,
            key: String,
            weight: Int,
            source: TasteSignalSource
        ) {
            if (weight == 0 || key.isBlank()) return
            signals += TasteSignal(
                dimension = dimension,
                key = key,
                weight = weight,
                source = source
            )
            totalSignalWeight += abs(weight)
        }

        // Base profile contains normalized metadata from real scan/favorite interactions.
        input.baseProfile.preferredNotes.forEach { (note, value) ->
            val weight = profileWeight(value)
            noteScores.bump(note, weight)
            registerSignal(TasteSignalDimension.NOTE, note.name, weight, TasteSignalSource.PROFILE_METADATA)
        }
        input.baseProfile.roastPreference.forEach { (roast, value) ->
            val weight = profileWeight(value)
            roastScores.bump(roast, weight)
            registerSignal(TasteSignalDimension.ROAST, roast.name, weight, TasteSignalSource.PROFILE_METADATA)
            bodyFromRoast(roast)?.let { bodyScores.bump(it, max(1, weight / 2)) }
        }
        input.baseProfile.acidityPreference.forEach { (acidity, value) ->
            val weight = profileWeight(value)
            acidityScores.bump(acidity, weight)
            registerSignal(TasteSignalDimension.ACIDITY, acidity.name, weight, TasteSignalSource.PROFILE_METADATA)
        }
        input.baseProfile.strengthPreference.forEach { (strength, value) ->
            val weight = profileWeight(value)
            bodyScores.bump(strength, weight)
            registerSignal(TasteSignalDimension.BODY, strength.name, weight, TasteSignalSource.PROFILE_METADATA)
        }
        input.baseProfile.favoriteOrigins.forEach { (origin, value) ->
            val weight = profileWeight(value)
            originScores.bump(origin.trim(), weight)
            registerSignal(TasteSignalDimension.ORIGIN, origin.trim(), weight, TasteSignalSource.PROFILE_METADATA)
        }
        input.baseProfile.favoriteCoffeeTypes.forEach { (type, value) ->
            val weight = profileWeight(value)
            coffeeTypeScores.bump(type, weight)
            registerSignal(TasteSignalDimension.COFFEE_TYPE, type.name, weight, TasteSignalSource.PROFILE_METADATA)
            brewStyleFromCoffeeType(type)?.let { brewStyleScores.bump(it, max(1, weight / 2)) }
        }
        milkScore += input.baseProfile.milkFriendlyPreferenceScore / 2
        registerSignal(
            dimension = TasteSignalDimension.MILK_TENDENCY,
            key = if (input.baseProfile.milkFriendlyPreferenceScore >= 0) "milk_friendly" else "black_coffee",
            weight = input.baseProfile.milkFriendlyPreferenceScore / 2,
            source = TasteSignalSource.PROFILE_METADATA
        )

        input.scanHistory.forEach { scan ->
            applyScanSignal(
                scan = scan,
                weight = SCAN_WEIGHT,
                source = TasteSignalSource.SCAN,
                noteScores = noteScores,
                roastScores = roastScores,
                bodyScores = bodyScores,
                originScores = originScores,
                brewStyleScores = brewStyleScores,
                registerSignal = ::registerSignal
            ).also { delta -> milkScore += delta }
        }

        input.favoriteScans.forEach { scan ->
            applyScanSignal(
                scan = scan,
                weight = FAVORITE_WEIGHT,
                source = TasteSignalSource.FAVORITE,
                noteScores = noteScores,
                roastScores = roastScores,
                bodyScores = bodyScores,
                originScores = originScores,
                brewStyleScores = brewStyleScores,
                registerSignal = ::registerSignal
            ).also { delta -> milkScore += delta }
        }

        input.favoriteMenuPicks.forEach { menuPick ->
            val corpus = "${menuPick.title} ${menuPick.subtitle}".normalizeTasteText()
            val inferredStyle = detectBrewStyles(corpus)
            inferredStyle.forEach { style ->
                brewStyleScores.bump(style, MENU_PICK_WEIGHT)
                registerSignal(
                    TasteSignalDimension.BREW_STYLE,
                    style.name,
                    MENU_PICK_WEIGHT,
                    TasteSignalSource.MENU_PICK
                )
            }
            val milkDelta = inferMilkDelta(corpus, MENU_PICK_WEIGHT)
            milkScore += milkDelta
            registerSignal(
                TasteSignalDimension.MILK_TENDENCY,
                if (milkDelta >= 0) "milk_friendly" else "black_coffee",
                milkDelta,
                TasteSignalSource.MENU_PICK
            )
            detectNotes(corpus).forEach { note ->
                noteScores.bump(note, MENU_PICK_WEIGHT)
                registerSignal(TasteSignalDimension.NOTE, note.name, MENU_PICK_WEIGHT, TasteSignalSource.MENU_PICK)
            }
        }

        val uniqueScannedProducts = (input.scanHistory + input.favoriteScans)
            .map { it.barcode.trim() }
            .filter { it.isNotBlank() }
            .toSet()
            .size
        val analyzedItemsCount = uniqueScannedProducts + input.favoriteMenuPicks.size

        val state = when {
            analyzedItemsCount < NOT_ENOUGH_DATA_THRESHOLD -> TasteDataState.NOT_ENOUGH_DATA
            analyzedItemsCount >= READY_DATA_THRESHOLD -> TasteDataState.READY
            else -> TasteDataState.LEARNING
        }

        return TasteProfile(
            state = state,
            analyzedItemsCount = analyzedItemsCount,
            totalSignalWeight = totalSignalWeight,
            strongestNotes = noteScores.topKeys(limit = 3),
            roastPreference = roastScores.topKey(),
            acidityTendency = acidityScores.topKey(),
            bodyTendency = bodyScores.topKey(),
            milkTendency = when {
                milkScore >= 4 -> true
                milkScore <= -4 -> false
                else -> null
            },
            topCoffeeTypes = coffeeTypeScores.topKeys(limit = 2),
            topOrigins = originScores.topKeys(limit = 3),
            topBrewStyles = brewStyleScores.topKeys(limit = 2),
            signals = signals
        )
    }

    private fun applyScanSignal(
        scan: FavoriteScanProduct,
        weight: Int,
        source: TasteSignalSource,
        noteScores: MutableMap<TasteNote, Int>,
        roastScores: MutableMap<RoastLevel, Int>,
        bodyScores: MutableMap<StrengthLevel, Int>,
        originScores: MutableMap<String, Int>,
        brewStyleScores: MutableMap<BrewStylePreference, Int>,
        registerSignal: (TasteSignalDimension, String, Int, TasteSignalSource) -> Unit
    ): Int {
        var milkDelta = 0
        val corpus = "${scan.name} ${scan.brand.orEmpty()} ${scan.origin.orEmpty()}".normalizeTasteText()

        parseRoast(scan.roast)?.let { roast ->
            roastScores.bump(roast, weight)
            registerSignal(TasteSignalDimension.ROAST, roast.name, weight, source)
            bodyFromRoast(roast)?.let { body ->
                val bodyWeight = max(1, weight / 2)
                bodyScores.bump(body, bodyWeight)
                registerSignal(TasteSignalDimension.BODY, body.name, bodyWeight, source)
            }
        }

        scan.origin?.trim()?.takeIf { it.isNotBlank() }?.let { origin ->
            originScores.bump(origin, weight)
            registerSignal(TasteSignalDimension.ORIGIN, origin, weight, source)
        }

        detectNotes(corpus).forEach { note ->
            noteScores.bump(note, weight)
            registerSignal(TasteSignalDimension.NOTE, note.name, weight, source)
        }

        detectBrewStyles(corpus).forEach { style ->
            brewStyleScores.bump(style, weight)
            registerSignal(TasteSignalDimension.BREW_STYLE, style.name, weight, source)
        }

        milkDelta += inferMilkDelta(corpus, weight)
        registerSignal(
            TasteSignalDimension.MILK_TENDENCY,
            if (milkDelta >= 0) "milk_friendly" else "black_coffee",
            milkDelta,
            source
        )

        return milkDelta
    }

    private fun profileWeight(value: Int): Int = (value / 3).coerceAtLeast(1)

    private fun parseRoast(rawRoast: String?): RoastLevel? {
        val value = rawRoast?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return when {
            value.startsWith("light") -> RoastLevel.LIGHT
            value.startsWith("medium") -> RoastLevel.MEDIUM
            value.startsWith("dark") -> RoastLevel.DARK
            else -> null
        }
    }

    private fun bodyFromRoast(roast: RoastLevel): StrengthLevel? = when (roast) {
        RoastLevel.LIGHT -> StrengthLevel.LOW
        RoastLevel.MEDIUM -> StrengthLevel.MEDIUM
        RoastLevel.DARK -> StrengthLevel.HIGH
        RoastLevel.UNKNOWN -> null
    }

    private fun brewStyleFromCoffeeType(type: CoffeeType): BrewStylePreference? = when (type) {
        CoffeeType.CAPSULE -> BrewStylePreference.ESPRESSO_BASED
        CoffeeType.INSTANT -> BrewStylePreference.INSTANT
        else -> null
    }

    private fun detectNotes(corpus: String): Set<TasteNote> {
        val notes = linkedSetOf<TasteNote>()
        if (containsAny(corpus, "chocolate", "cacao", "kakao")) notes += TasteNote.CHOCOLATE
        if (containsAny(corpus, "nut", "nutty", "fındık", "hazelnut")) notes += TasteNote.NUTTY
        if (containsAny(corpus, "fruit", "fruity", "berry", "citrus", "meyve")) {
            notes += TasteNote.FRUITY
            notes += TasteNote.BRIGHT
        }
        if (containsAny(corpus, "floral", "flower", "çiçek")) notes += TasteNote.FLORAL
        if (containsAny(corpus, "caramel", "karamel", "toffee")) notes += TasteNote.CARAMEL
        if (containsAny(corpus, "bold", "strong", "intense", "yoğun")) notes += TasteNote.BOLD
        if (containsAny(corpus, "smooth", "soft", "yumuşak")) notes += TasteNote.SMOOTH
        if (containsAny(corpus, "smoky", "smoke", "isli", "roasty")) notes += TasteNote.SMOKY
        return notes
    }

    private fun detectBrewStyles(corpus: String): Set<BrewStylePreference> {
        val styles = linkedSetOf<BrewStylePreference>()
        if (containsAny(corpus, "espresso", "americano", "ristretto")) {
            styles += BrewStylePreference.ESPRESSO_BASED
        }
        if (containsAny(corpus, "filter", "v60", "pour over", "chemex", "aeropress")) {
            styles += BrewStylePreference.FILTER
        }
        if (containsAny(corpus, "cold brew", "cold", "iced", "nitro")) {
            styles += BrewStylePreference.COLD_BREW
        }
        if (containsAny(corpus, "turkish", "türk kahvesi", "cezve")) {
            styles += BrewStylePreference.TURKISH
        }
        if (containsAny(corpus, "latte", "cappuccino", "flat white", "macchiato", "mocha")) {
            styles += BrewStylePreference.MILK_BASED
        }
        if (containsAny(corpus, "instant", "3in1", "3 in 1")) {
            styles += BrewStylePreference.INSTANT
        }
        return styles
    }

    private fun inferMilkDelta(corpus: String, weight: Int): Int {
        return when {
            containsAny(corpus, "latte", "cappuccino", "flat white", "macchiato", "mocha", "milk", "süt") ->
                weight

            containsAny(corpus, "americano", "espresso", "filter", "black", "sade") ->
                -weight

            else -> 0
        }
    }

    private fun String.normalizeTasteText(): String = lowercase(Locale.ROOT)

    private fun containsAny(corpus: String, vararg tokens: String): Boolean {
        return tokens.any { corpus.contains(it) }
    }

    private fun <K> MutableMap<K, Int>.bump(key: K, delta: Int) {
        if (delta == 0) return
        this[key] = (this[key] ?: 0) + delta
    }

    private fun <K> Map<K, Int>.topKey(): K? =
        entries
            .filter { it.value > 0 }
            .maxByOrNull { it.value }
            ?.key

    private fun <K> Map<K, Int>.topKeys(limit: Int): List<K> =
        entries
            .asSequence()
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .map { it.key }
            .take(limit)
            .toList()
}
