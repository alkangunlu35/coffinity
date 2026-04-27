package com.icoffee.app.domain.taste

import com.icoffee.app.data.model.UserTasteProfile
import com.icoffee.app.data.profile.FavoriteMenuPick
import com.icoffee.app.data.profile.FavoriteScanProduct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TasteEngineTest {

    @Test
    fun `bos input verildiginde state NOT_ENOUGH_DATA olmali`() {
        val input = TasteEngineInput(
            baseProfile = UserTasteProfile(),
            scanHistory = emptyList(),
            favoriteScans = emptyList(),
            favoriteMenuPicks = emptyList()
        )

        val result = TasteEngine.build(input)

        assertEquals(TasteDataState.NOT_ENOUGH_DATA, result.state)
        assertEquals(0, result.analyzedItemsCount)
        assertFalse(result.hasEnoughData)
    }

    @Test
    fun `tek item verildiginde hala NOT_ENOUGH_DATA olmali`() {
        val input = TasteEngineInput(
            baseProfile = UserTasteProfile(),
            scanHistory = listOf(makeScan("1")),
            favoriteScans = emptyList(),
            favoriteMenuPicks = emptyList()
        )

        val result = TasteEngine.build(input)

        assertEquals(TasteDataState.NOT_ENOUGH_DATA, result.state)
    }

    @Test
    fun `3 scan ile state LEARNING olmali`() {
        val input = TasteEngineInput(
            baseProfile = UserTasteProfile(),
            scanHistory = listOf(makeScan("1"), makeScan("2"), makeScan("3")),
            favoriteScans = emptyList(),
            favoriteMenuPicks = emptyList()
        )

        val result = TasteEngine.build(input)

        assertEquals(TasteDataState.LEARNING, result.state)
        assertTrue(result.hasEnoughData)
    }

    @Test
    fun `6 scan ile state READY olmali`() {
        val input = TasteEngineInput(
            baseProfile = UserTasteProfile(),
            scanHistory = (1..6).map { makeScan(it.toString()) },
            favoriteScans = emptyList(),
            favoriteMenuPicks = emptyList()
        )

        val result = TasteEngine.build(input)

        assertEquals(TasteDataState.READY, result.state)
    }

    @Test
    fun `favori scan agirligi normal scan'den daha fazla olmali`() {
        val onlyFavorites = TasteEngineInput(
            baseProfile = UserTasteProfile(),
            scanHistory = emptyList(),
            favoriteScans = listOf(makeScan("fav1")),
            favoriteMenuPicks = emptyList()
        )
        val onlyScans = TasteEngineInput(
            baseProfile = UserTasteProfile(),
            scanHistory = listOf(makeScan("scan1")),
            favoriteScans = emptyList(),
            favoriteMenuPicks = emptyList()
        )

        val favoriteResult = TasteEngine.build(onlyFavorites)
        val scanResult = TasteEngine.build(onlyScans)

        assertTrue(
            "Favori sinyal agirligi normal scan'den buyuk olmali",
            favoriteResult.totalSignalWeight > scanResult.totalSignalWeight
        )
    }

    private fun makeScan(id: String): FavoriteScanProduct = FavoriteScanProduct(
        barcode = "barcode_$id",
        name = "Test Coffee $id",
        brand = "Test Brand",
        origin = "Ethiopia",
        roast = "MEDIUM",
        imageUrl = null,
        savedAt = 0L
    )
}
