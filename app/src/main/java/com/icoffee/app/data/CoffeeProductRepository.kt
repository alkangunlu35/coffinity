package com.icoffee.app.data

import com.icoffee.app.R
import com.icoffee.app.data.model.CoffeeProduct

object CoffeeProductRepository {

    private val products: List<CoffeeProduct> = listOf(
        CoffeeProduct(
            barcode = "4006381333931",
            name = "Colombia Supremo",
            brand = "Coffinity Select",
            origin = "Colombia",
            roast = "Medium",
            type = "Ground Coffee",
            notes = listOf("Chocolate", "Nutty", "Smooth"),
            recommendation = "Perfect for your taste — smooth, balanced and chocolate-forward.",
            strength = "Balanced 6/10",
            profile = "Balanced & Chocolate-forward",
            matchScore = 87,
            imageRes = R.drawable.coffee_colombia_supremo
        ),
        CoffeeProduct(
            barcode = "5901234123457",
            name = "Ethiopian Sidamo",
            brand = "Coffinity Origins",
            origin = "Ethiopia",
            roast = "Light",
            type = "Whole Bean",
            notes = listOf("Fruity", "Floral", "Bright"),
            recommendation = "A vibrant choice — great if you enjoy fruity and aromatic coffees.",
            strength = "Bright 5/10",
            profile = "Bright & Aromatic",
            matchScore = 91,
            imageRes = R.drawable.coffee_ethiopian_sidamo
        ),
        CoffeeProduct(
            barcode = "5449000000996",
            name = "Colombia Medium Roast",
            brand = "CoffeeX",
            origin = "Colombia",
            roast = "Medium",
            type = "Ground Coffee",
            notes = listOf("Chocolate", "Nutty", "Smooth", "Caramel"),
            recommendation = "Great match for your taste if you enjoy smooth, chocolate-forward cups in the morning.",
            strength = "Balanced 6/10",
            profile = "Smooth & Caramel-rich",
            matchScore = 86,
            imageRes = R.drawable.coffee_colombia_supremo
        ),
        CoffeeProduct(
            barcode = "737628064502",
            name = "Ethiopia Sidamo Light",
            brand = "Bean Atelier",
            origin = "Ethiopia",
            roast = "Light",
            type = "Whole Bean",
            notes = listOf("Fruity", "Floral", "Bright", "Tea-like"),
            recommendation = "Perfect for pour-over lovers who want a lively cup with clear acidity and floral aroma.",
            strength = "Bright 5/10",
            profile = "Floral & Lively",
            matchScore = 90,
            imageRes = R.drawable.coffee_ethiopian_sidamo
        ),
        CoffeeProduct(
            barcode = "4902777000430",
            name = "Brazil Santos Classic",
            brand = "Roast House",
            origin = "Brazil",
            roast = "Medium-Dark",
            type = "Whole Bean",
            notes = listOf("Cocoa", "Hazelnut", "Sweet", "Round Body"),
            recommendation = "A reliable daily pick for espresso or moka pot when you want low-acidity comfort and body.",
            strength = "Rich 7/10",
            profile = "Rich & Comforting",
            matchScore = 84,
            imageRes = R.drawable.coffee_brazil_santos
        ),
        CoffeeProduct(
            barcode = "8711000421032",
            name = "Velvet Cappuccino Blend",
            brand = "Coffinity Select",
            origin = "Blend",
            roast = "Dark",
            type = "Espresso Blend",
            notes = listOf("Creamy", "Dark Chocolate", "Silky", "Toasted"),
            recommendation = "Good for focused mornings and milk drinks when you want a stronger, creamier base.",
            strength = "Bold 8/10",
            profile = "Bold & Creamy",
            matchScore = 82,
            imageRes = R.drawable.coffee_velvet_cappuccino
        )
    )

    private val aliases: Map<String, String> = mapOf(
        "4006381333931" to "4006381333931",
        "5901234123457" to "5901234123457",
        "5449000000996" to "5449000000996",
        "544900000099" to "5449000000996",
        "737628064502" to "737628064502",
        "4902777000430" to "4902777000430",
        "8711000421032" to "8711000421032"
    )

    fun findByBarcode(rawCode: String): CoffeeProduct? {
        val normalized = normalize(rawCode)
        if (normalized.isEmpty()) return null

        val canonical = aliases[normalized] ?: normalized
        return products.firstOrNull { normalize(it.barcode) == canonical }
    }

    private fun normalize(code: String): String = code.filter(Char::isDigit)
}
