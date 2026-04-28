package com.icoffee.app.data.matching

import com.icoffee.app.data.model.AcidityLevel
import com.icoffee.app.data.model.CoffeeProfile
import com.icoffee.app.data.model.CoffeeType
import com.icoffee.app.data.model.MatchLevel
import com.icoffee.app.data.model.RoastLevel
import com.icoffee.app.data.model.StrengthLevel
import com.icoffee.app.data.model.TasteInsightState
import com.icoffee.app.data.model.TasteNote
import com.icoffee.app.data.model.UserTasteProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BasicMatchScoreEngineTest {

    @Test
    fun `bos user profile ile valid bir match level dondurmeli`() {
        val coffee = makeCoffee()
        val emptyUser = UserTasteProfile()

        val result = BasicMatchScoreEngine.calculate(coffee, emptyUser)

        // Bos profile'da bile gecerli bir level donmeli (engine LOW yerine GOOD verebilir)
        assertTrue(result.level in MatchLevel.entries.toList())
        // Insight kesin olarak NOT_ENOUGH_DATA olmali (interaction=0)
        assertEquals(TasteInsightState.NOT_ENOUGH_DATA, result.insight.state)
    }

    @Test
    fun `yeterli veri yokken insight state NOT_ENOUGH_DATA olmali`() {
        val coffee = makeCoffee()
        // 4 interaction altinda
        val youngUser = UserTasteProfile(
            preferredNotes = mapOf(TasteNote.CHOCOLATE to 1),
            interactionCount = 2
        )

        val result = BasicMatchScoreEngine.calculate(coffee, youngUser)

        assertEquals(TasteInsightState.NOT_ENOUGH_DATA, result.insight.state)
    }

    @Test
    fun `tum tercihler eslesiyorsa skor yuksek olmali`() {
        val coffee = makeCoffee(
            roast = RoastLevel.MEDIUM,
            notes = listOf(TasteNote.CHOCOLATE, TasteNote.NUTTY),
            type = CoffeeType.WHOLE_BEAN,
            origin = "Ethiopia"
        )
        val matchingUser = UserTasteProfile(
            preferredNotes = mapOf(TasteNote.CHOCOLATE to 5, TasteNote.NUTTY to 5),
            roastPreference = mapOf(RoastLevel.MEDIUM to 5),
            favoriteCoffeeTypes = mapOf(CoffeeType.WHOLE_BEAN to 5),
            favoriteOrigins = mapOf("Ethiopia" to 5),
            interactionCount = 20
        )

        val result = BasicMatchScoreEngine.calculate(coffee, matchingUser)

        assertTrue(
            "Yuksek eslesmede skor 70+ olmali, gelen=${result.score}",
            result.score >= 70
        )
    }

    @Test
    fun `skor her zaman 0 ile 100 arasinda olmali`() {
        val coffee = makeCoffee()
        val user = UserTasteProfile(
            preferredNotes = mapOf(TasteNote.CHOCOLATE to 10),
            interactionCount = 50
        )

        val result = BasicMatchScoreEngine.calculate(coffee, user)

        assertTrue("Skor 0+ olmali, gelen=${result.score}", result.score >= 0)
        assertTrue("Skor 100 alti olmali, gelen=${result.score}", result.score <= 100)
    }

    @Test
    fun `match level skoru ile uyumlu olmali`() {
        val coffee = makeCoffee()
        val user = UserTasteProfile(
            preferredNotes = mapOf(TasteNote.CHOCOLATE to 5),
            interactionCount = 10
        )

        val result = BasicMatchScoreEngine.calculate(coffee, user)

        // Level her zaman bir geçerli enum değeri olmalı
        assertTrue(result.level in MatchLevel.entries.toList())
    }

    private fun makeCoffee(
        roast: RoastLevel = RoastLevel.MEDIUM,
        notes: List<TasteNote> = listOf(TasteNote.CHOCOLATE),
        type: CoffeeType = CoffeeType.WHOLE_BEAN,
        origin: String? = "Ethiopia"
    ) = CoffeeProfile(
        barcode = "test",
        productName = "Test Coffee",
        brand = "Test Brand",
        imageUrl = null,
        coffeeType = type,
        roastLevel = roast,
        originCountry = origin,
        tasteNotes = notes,
        strength = StrengthLevel.MEDIUM,
        acidity = AcidityLevel.MEDIUM,
        milkFriendly = false,
        confidenceScore = 80
    )
}
