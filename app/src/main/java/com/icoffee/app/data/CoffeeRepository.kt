package com.icoffee.app.data

import com.icoffee.app.R
import com.icoffee.app.data.model.Coffee
import com.icoffee.app.data.model.CoffeeCategory
import com.icoffee.app.data.model.CoffeeMood

object CoffeeRepository {

    val coffees: List<Coffee> = listOf(
        Coffee(
            id = "brazil_santos",
            titleRes = R.string.coffee_brazil_santos_title,
            subtitleRes = R.string.coffee_brazil_santos_subtitle,
            typeRes = R.string.coffee_brazil_santos_type,
            rating = 4.8,
            reviewCount = 1248,
            tags = listOf("brazil", "espresso", "nutty", "cocoa", "smooth"),
            notesRes = R.array.coffee_brazil_santos_notes,
            origin = "Brazil",
            roast = "Medium",
            altitude = "900-1200m",
            category = CoffeeCategory.ESPRESSO,
            mood = CoffeeMood.SMOOTH,
            imageRes = R.drawable.coffee_brazil_santos,
            descriptionRes = R.string.coffee_brazil_santos_description
        ),
        Coffee(
            id = "ethiopian_sidamo",
            titleRes = R.string.coffee_ethiopian_sidamo_title,
            subtitleRes = R.string.coffee_ethiopian_sidamo_subtitle,
            typeRes = R.string.coffee_ethiopian_sidamo_type,
            rating = 4.7,
            reviewCount = 982,
            tags = listOf("ethiopian", "sidamo", "pour over", "fruity", "floral"),
            notesRes = R.array.coffee_ethiopian_sidamo_notes,
            origin = "Ethiopia",
            roast = "Light",
            altitude = "1550-2200m",
            category = CoffeeCategory.HOT,
            mood = CoffeeMood.FRUITY,
            imageRes = R.drawable.coffee_ethiopian_sidamo,
            descriptionRes = R.string.coffee_ethiopian_sidamo_description
        ),
        Coffee(
            id = "colombia_supremo",
            titleRes = R.string.coffee_colombia_supremo_title,
            subtitleRes = R.string.coffee_colombia_supremo_subtitle,
            typeRes = R.string.coffee_colombia_supremo_type,
            rating = 4.9,
            reviewCount = 1640,
            tags = listOf("colombia", "supremo", "french press", "aromatic", "nutty"),
            notesRes = R.array.coffee_colombia_supremo_notes,
            origin = "Colombia",
            roast = "Medium-Dark",
            altitude = "1200-1800m",
            category = CoffeeCategory.HOT,
            mood = CoffeeMood.CHOCOLATEY,
            imageRes = R.drawable.coffee_colombia_supremo,
            descriptionRes = R.string.coffee_colombia_supremo_description
        ),
        Coffee(
            id = "vanilla_latte",
            titleRes = R.string.coffee_vanilla_latte_title,
            subtitleRes = R.string.coffee_vanilla_latte_subtitle,
            typeRes = R.string.coffee_vanilla_latte_type,
            rating = 4.6,
            reviewCount = 736,
            tags = listOf("latte", "vanilla", "milk", "creamy", "sweet"),
            notesRes = R.array.coffee_vanilla_latte_notes,
            origin = "Blend",
            roast = "Medium",
            altitude = "N/A",
            category = CoffeeCategory.MILK,
            mood = CoffeeMood.SMOOTH,
            imageRes = R.drawable.coffee_vanilla_latte,
            descriptionRes = R.string.coffee_vanilla_latte_description
        ),
        Coffee(
            id = "caramel_coffee",
            titleRes = R.string.coffee_caramel_coffee_title,
            subtitleRes = R.string.coffee_caramel_coffee_subtitle,
            typeRes = R.string.coffee_caramel_coffee_type,
            rating = 4.5,
            reviewCount = 592,
            tags = listOf("caramel", "cold", "cold brew", "toffee", "soft"),
            notesRes = R.array.coffee_caramel_coffee_notes,
            origin = "Blend",
            roast = "Medium",
            altitude = "N/A",
            category = CoffeeCategory.COLD,
            mood = CoffeeMood.CHOCOLATEY,
            imageRes = R.drawable.coffee_caramel_cloud,
            descriptionRes = R.string.coffee_caramel_coffee_description
        ),
        Coffee(
            id = "velvet_cappuccino",
            titleRes = R.string.coffee_velvet_cappuccino_title,
            subtitleRes = R.string.coffee_velvet_cappuccino_subtitle,
            typeRes = R.string.coffee_velvet_cappuccino_type,
            rating = 4.7,
            reviewCount = 861,
            tags = listOf("cappuccino", "espresso", "velvet", "cocoa", "milk"),
            notesRes = R.array.coffee_velvet_cappuccino_notes,
            origin = "Blend",
            roast = "Dark",
            altitude = "N/A",
            category = CoffeeCategory.MILK,
            mood = CoffeeMood.SMOOTH,
            imageRes = R.drawable.coffee_velvet_cappuccino,
            descriptionRes = R.string.coffee_velvet_cappuccino_description
        )
    )

    fun findCoffee(id: String): Coffee? = coffees.firstOrNull { it.id == id }

    fun featuredByMood(mood: CoffeeMood): Coffee {
        return coffees.firstOrNull { it.mood == mood } ?: coffees.first()
    }

    fun searchCoffees(query: String): List<Coffee> {
        if (query.isBlank()) return emptyList()
        val normalizedQuery = query.trim().lowercase()
        return coffees.filter { coffee ->
            coffee.origin.lowercase().contains(normalizedQuery) ||
                coffee.tags.any { it.lowercase().contains(normalizedQuery) }
        }
    }
}
