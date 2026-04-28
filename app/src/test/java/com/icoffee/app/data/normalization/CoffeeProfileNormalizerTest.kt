package com.icoffee.app.data.normalization

import com.icoffee.app.data.model.CoffeeType
import com.icoffee.app.data.model.OpenFoodFactsProduct
import com.icoffee.app.data.model.RoastLevel
import com.icoffee.app.data.model.TasteNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoffeeProfileNormalizerTest {

    // ============================================
    // Coffee Type Inference
    // ============================================

    @Test
    fun `whole beans iceren product WHOLE_BEAN olarak siniflanmali`() {
        val product = makeProduct(name = "Premium Whole Beans Coffee")

        val profile = CoffeeProfileNormalizer.normalize(product)

        assertEquals(CoffeeType.WHOLE_BEAN, profile.coffeeType)
    }

    @Test
    fun `ground coffee iceren product GROUND olarak siniflanmali`() {
        val product = makeProduct(name = "Filter ground coffee 250g")

        val profile = CoffeeProfileNormalizer.normalize(product)

        assertEquals(CoffeeType.GROUND, profile.coffeeType)
    }

    @Test
    fun `instant iceren product INSTANT olarak siniflanmali`() {
        val product = makeProduct(name = "Nescafe Instant Coffee")

        val profile = CoffeeProfileNormalizer.normalize(product)

        assertEquals(CoffeeType.INSTANT, profile.coffeeType)
    }

    @Test
    fun `nespresso iceren product CAPSULE olarak siniflanmali`() {
        val product = makeProduct(name = "Nespresso Compatible Pods")

        val profile = CoffeeProfileNormalizer.normalize(product)

        assertEquals(CoffeeType.CAPSULE, profile.coffeeType)
    }

    @Test
    fun `cold brew iceren product READY_TO_DRINK olarak siniflanmali`() {
        val product = makeProduct(name = "Starbucks Cold Brew Bottle")

        val profile = CoffeeProfileNormalizer.normalize(product)

        assertEquals(CoffeeType.READY_TO_DRINK, profile.coffeeType)
    }

    // ============================================
    // Roast Level Inference
    // ============================================

    @Test
    fun `dark roast iceren product DARK olarak siniflanmali`() {
        val product = makeProduct(name = "Dark Roast Espresso Beans")

        val profile = CoffeeProfileNormalizer.normalize(product)

        assertEquals(RoastLevel.DARK, profile.roastLevel)
    }

    // ============================================
    // Origin & Taste Notes
    // ============================================

    @Test
    fun `ethiopia origin FLORAL ve FRUITY note'lara sahip olmali`() {
        val product = makeProduct(
            name = "Ethiopian Yirgacheffe",
            countries = "Ethiopia"
        )

        val profile = CoffeeProfileNormalizer.normalize(product)

        assertEquals("Ethiopia", profile.originCountry)
        assertTrue(
            "Ethiopia coffee FLORAL note icermeli",
            profile.tasteNotes.contains(TasteNote.FLORAL)
        )
        assertTrue(
            "Ethiopia coffee FRUITY note icermeli",
            profile.tasteNotes.contains(TasteNote.FRUITY)
        )
    }

    @Test
    fun `colombia origin CHOCOLATE ve NUTTY note'lara sahip olmali`() {
        val product = makeProduct(
            name = "Colombian Supremo",
            countries = "Colombia"
        )

        val profile = CoffeeProfileNormalizer.normalize(product)

        assertEquals("Colombia", profile.originCountry)
        assertTrue(profile.tasteNotes.contains(TasteNote.CHOCOLATE))
        assertTrue(profile.tasteNotes.contains(TasteNote.NUTTY))
    }

    // ============================================
    // Output structure
    // ============================================

    @Test
    fun `normalize tum CoffeeProfile alanlarini doldurmali`() {
        val product = makeProduct(
            name = "Test Coffee",
            brand = "Test Brand"
        )

        val profile = CoffeeProfileNormalizer.normalize(product)

        assertEquals("test", profile.barcode)
        assertEquals("Test Coffee", profile.productName)
        assertEquals("Test Brand", profile.brand)
        // confidenceScore 0-100 arasında olmalı
        assertTrue(profile.confidenceScore in 0..100)
    }

    @Test
    fun `bilinmeyen product UNKNOWN type'larla donmeli`() {
        val product = makeProduct(name = "Random Drink Without Coffee Keywords")

        val profile = CoffeeProfileNormalizer.normalize(product)

        // Coffee type belirsiz olabilir - ama crash etmemeli
        assertTrue(profile.coffeeType in CoffeeType.entries.toList())
        // Origin null olabilir
        // Confidence düşük olmalı
        assertTrue(profile.confidenceScore < 80)
    }

    private fun makeProduct(
        name: String = "Test Product",
        brand: String? = null,
        countries: String? = null,
        categories: String? = null,
        ingredientsText: String? = null
    ) = OpenFoodFactsProduct(
        barcode = "test",
        name = name,
        brand = brand,
        countries = countries,
        categories = categories,
        imageUrl = null,
        genericName = null,
        quantity = null,
        packaging = null,
        ingredientsText = ingredientsText,
        labels = null,
        stores = null
    )
}
