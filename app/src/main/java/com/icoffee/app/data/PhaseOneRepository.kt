package com.icoffee.app.data

import com.icoffee.app.R
import com.icoffee.app.data.model.BrewingMethod
import com.icoffee.app.data.model.HomeMachineTip
import com.icoffee.app.data.model.MoodType
import com.icoffee.app.data.model.RecommendationItem
import com.icoffee.app.data.model.RecommendationTrait
import com.icoffee.app.data.model.TimeBucket

object PhaseOneRepository {

    val moods: List<MoodType> = MoodType.entries

    const val CATEGORY_ESPRESSO_BASED = "Espresso-Based Coffees"
    const val CATEGORY_FILTER_MANUAL = "Filter & Manual Brewing"
    const val CATEGORY_TRADITIONAL_AUTHENTIC = "Traditional & Authentic Methods"
    const val CATEGORY_COLD_BREWING = "Cold Brewing Methods"
    const val CATEGORY_PRACTICAL_MODERN = "Practical / Modern Methods"

    val brewingCategoryOrder: List<String> = listOf(
        CATEGORY_ESPRESSO_BASED,
        CATEGORY_FILTER_MANUAL,
        CATEGORY_TRADITIONAL_AUTHENTIC,
        CATEGORY_COLD_BREWING,
        CATEGORY_PRACTICAL_MODERN
    )

    private val recommendationItems: List<RecommendationItem> = listOf(
        RecommendationItem(
            id = "relaxed_1",
            mood = MoodType.RELAXED,
            coffeeId = "colombia_supremo",
            coffeeNameRes = R.string.rec_relaxed_1_name,
            brewingStyleRes = R.string.rec_relaxed_1_style,
            reasonRes = R.string.rec_relaxed_1_reason,
            imageRes = R.drawable.french_press,
            tagsRes = R.array.rec_relaxed_1_tags,
            methodId = "french_press",
            rating = 4.8,
            preferredTimes = setOf(TimeBucket.AFTERNOON, TimeBucket.EVENING, TimeBucket.NIGHT),
            moodAffinity = 5,
            traits = setOf(RecommendationTrait.FILTER, RecommendationTrait.NUTTY, RecommendationTrait.CLEAN)
        ),
        RecommendationItem(
            id = "relaxed_2",
            mood = MoodType.RELAXED,
            coffeeId = "vanilla_latte",
            coffeeNameRes = R.string.rec_relaxed_2_name,
            brewingStyleRes = R.string.rec_relaxed_2_style,
            reasonRes = R.string.rec_relaxed_2_reason,
            imageRes = R.drawable.latte,
            tagsRes = R.array.rec_relaxed_2_tags,
            methodId = "espresso",
            rating = 4.7,
            preferredTimes = setOf(TimeBucket.AFTERNOON, TimeBucket.EVENING, TimeBucket.NIGHT),
            moodAffinity = 4,
            traits = setOf(RecommendationTrait.MILK, RecommendationTrait.SWEET)
        ),
        RecommendationItem(
            id = "relaxed_3",
            mood = MoodType.RELAXED,
            coffeeId = "velvet_cappuccino",
            coffeeNameRes = R.string.rec_relaxed_3_name,
            brewingStyleRes = R.string.rec_relaxed_3_style,
            reasonRes = R.string.rec_relaxed_3_reason,
            imageRes = R.drawable.cappuccino,
            tagsRes = R.array.rec_relaxed_3_tags,
            methodId = "espresso",
            rating = 4.9,
            preferredTimes = setOf(TimeBucket.AFTERNOON, TimeBucket.EVENING, TimeBucket.NIGHT),
            moodAffinity = 4,
            traits = setOf(RecommendationTrait.MILK, RecommendationTrait.SWEET)
        ),
        RecommendationItem(
            id = "energetic_1",
            mood = MoodType.ENERGETIC,
            coffeeId = "brazil_santos",
            coffeeNameRes = R.string.rec_energetic_1_name,
            brewingStyleRes = R.string.rec_energetic_1_style,
            reasonRes = R.string.rec_energetic_1_reason,
            imageRes = R.drawable.espresso,
            tagsRes = R.array.rec_energetic_1_tags,
            methodId = "espresso",
            rating = 4.8,
            preferredTimes = setOf(TimeBucket.MORNING, TimeBucket.AFTERNOON),
            moodAffinity = 5,
            traits = setOf(RecommendationTrait.BOLD, RecommendationTrait.NUTTY)
        ),
        RecommendationItem(
            id = "energetic_2",
            mood = MoodType.ENERGETIC,
            coffeeId = "colombia_supremo",
            coffeeNameRes = R.string.rec_energetic_2_name,
            brewingStyleRes = R.string.rec_energetic_2_style,
            reasonRes = R.string.rec_energetic_2_reason,
            imageRes = R.drawable.americano,
            tagsRes = R.array.rec_energetic_2_tags,
            methodId = "espresso",
            rating = 4.6,
            preferredTimes = setOf(TimeBucket.MORNING, TimeBucket.AFTERNOON),
            moodAffinity = 4,
            traits = setOf(RecommendationTrait.BOLD, RecommendationTrait.CLEAN)
        ),
        RecommendationItem(
            id = "energetic_3",
            mood = MoodType.ENERGETIC,
            coffeeId = "brazil_santos",
            coffeeNameRes = R.string.rec_energetic_3_name,
            brewingStyleRes = R.string.rec_energetic_3_style,
            reasonRes = R.string.rec_energetic_3_reason,
            imageRes = R.drawable.aeropress,
            tagsRes = R.array.rec_energetic_3_tags,
            methodId = "aeropress",
            rating = 4.7,
            preferredTimes = setOf(TimeBucket.MORNING, TimeBucket.AFTERNOON),
            moodAffinity = 4,
            traits = setOf(RecommendationTrait.BOLD, RecommendationTrait.CLEAN)
        ),
        RecommendationItem(
            id = "focused_1",
            mood = MoodType.FOCUSED,
            coffeeId = "ethiopian_sidamo",
            coffeeNameRes = R.string.rec_focused_1_name,
            brewingStyleRes = R.string.rec_focused_1_style,
            reasonRes = R.string.rec_focused_1_reason,
            imageRes = R.drawable.v60,
            tagsRes = R.array.rec_focused_1_tags,
            methodId = "v60",
            rating = 4.9,
            preferredTimes = setOf(TimeBucket.MORNING, TimeBucket.AFTERNOON),
            moodAffinity = 5,
            traits = setOf(RecommendationTrait.FILTER, RecommendationTrait.CLEAN, RecommendationTrait.FRUITY)
        ),
        RecommendationItem(
            id = "focused_2",
            mood = MoodType.FOCUSED,
            coffeeId = "brazil_santos",
            coffeeNameRes = R.string.rec_focused_2_name,
            brewingStyleRes = R.string.rec_focused_2_style,
            reasonRes = R.string.rec_focused_2_reason,
            imageRes = R.drawable.kalita_wave,
            tagsRes = R.array.rec_focused_2_tags,
            methodId = "kalita_wave",
            rating = 4.5,
            preferredTimes = setOf(TimeBucket.MORNING, TimeBucket.AFTERNOON),
            moodAffinity = 4,
            traits = setOf(RecommendationTrait.FILTER, RecommendationTrait.CLEAN, RecommendationTrait.NUTTY)
        ),
        RecommendationItem(
            id = "focused_3",
            mood = MoodType.FOCUSED,
            coffeeId = "brazil_santos",
            coffeeNameRes = R.string.rec_focused_3_name,
            brewingStyleRes = R.string.rec_focused_3_style,
            reasonRes = R.string.rec_focused_3_reason,
            imageRes = R.drawable.ristretto,
            tagsRes = R.array.rec_focused_3_tags,
            methodId = "espresso",
            rating = 4.8,
            preferredTimes = setOf(TimeBucket.MORNING, TimeBucket.AFTERNOON),
            moodAffinity = 4,
            traits = setOf(RecommendationTrait.BOLD, RecommendationTrait.CLEAN)
        ),
        RecommendationItem(
            id = "social_1",
            mood = MoodType.SOCIAL,
            coffeeId = "vanilla_latte",
            coffeeNameRes = R.string.rec_social_1_name,
            brewingStyleRes = R.string.rec_social_1_style,
            reasonRes = R.string.rec_social_1_reason,
            imageRes = R.drawable.latte,
            tagsRes = R.array.rec_social_1_tags,
            methodId = "espresso",
            rating = 4.7,
            preferredTimes = setOf(TimeBucket.AFTERNOON, TimeBucket.EVENING),
            moodAffinity = 5,
            traits = setOf(RecommendationTrait.MILK, RecommendationTrait.SWEET)
        ),
        RecommendationItem(
            id = "social_2",
            mood = MoodType.SOCIAL,
            coffeeId = "velvet_cappuccino",
            coffeeNameRes = R.string.rec_social_2_name,
            brewingStyleRes = R.string.rec_social_2_style,
            reasonRes = R.string.rec_social_2_reason,
            imageRes = R.drawable.cappuccino,
            tagsRes = R.array.rec_social_2_tags,
            methodId = "espresso",
            rating = 4.9,
            preferredTimes = setOf(TimeBucket.AFTERNOON, TimeBucket.EVENING),
            moodAffinity = 5,
            traits = setOf(RecommendationTrait.MILK, RecommendationTrait.NUTTY, RecommendationTrait.SWEET)
        ),
        RecommendationItem(
            id = "social_3",
            mood = MoodType.SOCIAL,
            coffeeId = "caramel_coffee",
            coffeeNameRes = R.string.rec_social_3_name,
            brewingStyleRes = R.string.rec_social_3_style,
            reasonRes = R.string.rec_social_3_reason,
            imageRes = R.drawable.cold_brew,
            tagsRes = R.array.rec_social_3_tags,
            methodId = "cold_brew",
            rating = 4.6,
            preferredTimes = setOf(TimeBucket.AFTERNOON, TimeBucket.EVENING, TimeBucket.NIGHT),
            moodAffinity = 4,
            traits = setOf(RecommendationTrait.SWEET, RecommendationTrait.COLD, RecommendationTrait.MILK)
        ),
        RecommendationItem(
            id = "curious_1",
            mood = MoodType.CURIOUS,
            coffeeId = "brazil_santos",
            coffeeNameRes = R.string.rec_curious_1_name,
            brewingStyleRes = R.string.rec_curious_1_style,
            reasonRes = R.string.rec_curious_1_reason,
            imageRes = R.drawable.aeropress,
            tagsRes = R.array.rec_curious_1_tags,
            methodId = "aeropress",
            rating = 4.8,
            preferredTimes = setOf(TimeBucket.AFTERNOON, TimeBucket.EVENING),
            moodAffinity = 4,
            traits = setOf(RecommendationTrait.FILTER, RecommendationTrait.CLEAN)
        ),
        RecommendationItem(
            id = "curious_2",
            mood = MoodType.CURIOUS,
            coffeeId = "ethiopian_sidamo",
            coffeeNameRes = R.string.rec_curious_2_name,
            brewingStyleRes = R.string.rec_curious_2_style,
            reasonRes = R.string.rec_curious_2_reason,
            imageRes = R.drawable.chemex,
            tagsRes = R.array.rec_curious_2_tags,
            methodId = "chemex",
            rating = 4.9,
            preferredTimes = setOf(TimeBucket.MORNING, TimeBucket.AFTERNOON, TimeBucket.EVENING),
            moodAffinity = 5,
            traits = setOf(RecommendationTrait.FILTER, RecommendationTrait.FRUITY, RecommendationTrait.CLEAN)
        ),
        RecommendationItem(
            id = "curious_3",
            mood = MoodType.CURIOUS,
            coffeeId = "colombia_supremo",
            coffeeNameRes = R.string.rec_curious_3_name,
            brewingStyleRes = R.string.rec_curious_3_style,
            reasonRes = R.string.rec_curious_3_reason,
            imageRes = R.drawable.french_press,
            tagsRes = R.array.rec_curious_3_tags,
            methodId = "french_press",
            rating = 4.5,
            preferredTimes = setOf(TimeBucket.AFTERNOON, TimeBucket.EVENING),
            moodAffinity = 4,
            traits = setOf(RecommendationTrait.NUTTY, RecommendationTrait.FILTER)
        ),
        RecommendationItem(
            id = "calm_1",
            mood = MoodType.CALM,
            coffeeId = "colombia_supremo",
            coffeeNameRes = R.string.rec_calm_1_name,
            brewingStyleRes = R.string.rec_calm_1_style,
            reasonRes = R.string.rec_calm_1_reason,
            imageRes = R.drawable.v60,
            tagsRes = R.array.rec_calm_1_tags,
            methodId = "v60",
            rating = 4.8,
            preferredTimes = setOf(TimeBucket.MORNING, TimeBucket.AFTERNOON),
            moodAffinity = 5,
            traits = setOf(RecommendationTrait.FILTER, RecommendationTrait.CLEAN)
        ),
        RecommendationItem(
            id = "calm_2",
            mood = MoodType.CALM,
            coffeeId = "vanilla_latte",
            coffeeNameRes = R.string.rec_calm_2_name,
            brewingStyleRes = R.string.rec_calm_2_style,
            reasonRes = R.string.rec_calm_2_reason,
            imageRes = R.drawable.latte,
            tagsRes = R.array.rec_calm_2_tags,
            methodId = "espresso",
            rating = 4.7,
            preferredTimes = setOf(TimeBucket.AFTERNOON, TimeBucket.EVENING),
            moodAffinity = 4,
            traits = setOf(RecommendationTrait.MILK, RecommendationTrait.SWEET)
        ),
        RecommendationItem(
            id = "adventurous_1",
            mood = MoodType.ADVENTUROUS,
            coffeeId = "ethiopian_sidamo",
            coffeeNameRes = R.string.rec_adventurous_1_name,
            brewingStyleRes = R.string.rec_adventurous_1_style,
            reasonRes = R.string.rec_adventurous_1_reason,
            imageRes = R.drawable.chemex,
            tagsRes = R.array.rec_adventurous_1_tags,
            methodId = "chemex",
            rating = 4.9,
            preferredTimes = setOf(TimeBucket.MORNING, TimeBucket.AFTERNOON),
            moodAffinity = 5,
            traits = setOf(RecommendationTrait.FRUITY, RecommendationTrait.FILTER, RecommendationTrait.CLEAN)
        ),
        RecommendationItem(
            id = "adventurous_2",
            mood = MoodType.ADVENTUROUS,
            coffeeId = "brazil_santos",
            coffeeNameRes = R.string.rec_adventurous_2_name,
            brewingStyleRes = R.string.rec_adventurous_2_style,
            reasonRes = R.string.rec_adventurous_2_reason,
            imageRes = R.drawable.cold_brew,
            tagsRes = R.array.rec_adventurous_2_tags,
            methodId = "cold_brew",
            rating = 4.7,
            preferredTimes = setOf(TimeBucket.AFTERNOON, TimeBucket.EVENING),
            moodAffinity = 4,
            traits = setOf(RecommendationTrait.COLD, RecommendationTrait.BOLD)
        ),
        RecommendationItem(
            id = "cozy_1",
            mood = MoodType.COZY,
            coffeeId = "velvet_cappuccino",
            coffeeNameRes = R.string.rec_cozy_1_name,
            brewingStyleRes = R.string.rec_cozy_1_style,
            reasonRes = R.string.rec_cozy_1_reason,
            imageRes = R.drawable.cappuccino,
            tagsRes = R.array.rec_cozy_1_tags,
            methodId = "espresso",
            rating = 4.9,
            preferredTimes = setOf(TimeBucket.EVENING, TimeBucket.NIGHT),
            moodAffinity = 5,
            traits = setOf(RecommendationTrait.MILK, RecommendationTrait.SWEET)
        ),
        RecommendationItem(
            id = "cozy_2",
            mood = MoodType.COZY,
            coffeeId = "colombia_supremo",
            coffeeNameRes = R.string.rec_cozy_2_name,
            brewingStyleRes = R.string.rec_cozy_2_style,
            reasonRes = R.string.rec_cozy_2_reason,
            imageRes = R.drawable.french_press,
            tagsRes = R.array.rec_cozy_2_tags,
            methodId = "french_press",
            rating = 4.8,
            preferredTimes = setOf(TimeBucket.EVENING, TimeBucket.NIGHT),
            moodAffinity = 4,
            traits = setOf(RecommendationTrait.FILTER, RecommendationTrait.NUTTY)
        )
    )

    private val recommendationsByMood: Map<MoodType, List<RecommendationItem>> =
        recommendationItems.groupBy { it.mood }

    private val baseBrewingMethods: List<BrewingMethod> = listOf(
        BrewingMethod(
            id = "espresso",
            category = CATEGORY_ESPRESSO_BASED,
            title = "Espresso",
            cardSubtitle = "Bold and concentrated",
            summary = "A concentrated 30 ml shot with crema and deep intensity.",
            howItWorks = "Pressurized hot water passes through finely ground coffee for a fast, concentrated extraction.",
            brewTime = "25-30 sec",
            tasteProfile = listOf("Bold", "Creamy", "Chocolatey"),
            brewCharacteristics = listOf("High concentration", "Thick mouthfeel", "Crema layer"),
            imageRes = R.drawable.espresso,
            howToBrew = listOf(
                "Use 18g fine coffee in a preheated portafilter.",
                "Tamp level and extract 34-38g in 25-30 seconds.",
                "Adjust grind finer if thin, coarser if bitter."
            ),
            homeMachineTips = listOf(
                HomeMachineTip("Philips Automatic", listOf("Strength: High", "Grind: 3/5", "Cup: Small"), "Balanced espresso with steady crema"),
                HomeMachineTip("De'Longhi Bean-to-Cup", listOf("Aroma: 4/5", "Temp: Medium-High", "Cup: 30-40 ml"), "Full-bodied morning shot")
            ),
            bestFor = listOf("Strong coffee lovers", "Morning routines"),
        ),
        BrewingMethod(
            id = "ristretto",
            category = CATEGORY_ESPRESSO_BASED,
            title = "Ristretto",
            cardSubtitle = "Shorter and sweeter",
            summary = "A shorter espresso shot with dense body and perceived sweetness.",
            howItWorks = "Uses the same dose as espresso but a shorter yield, emphasizing early extraction compounds.",
            brewTime = "18-22 sec",
            tasteProfile = listOf("Dense", "Sweet", "Intense"),
            brewCharacteristics = listOf("Short yield", "Lower dilution", "Punchy finish"),
            imageRes = R.drawable.ristretto,
            howToBrew = listOf(
                "Dose 18g coffee and tamp evenly.",
                "Stop extraction at 20-24g output.",
                "Taste and fine-tune by grind size."
            ),
            homeMachineTips = listOf(
                HomeMachineTip("Automatic Espresso Mode", listOf("Cup size: Extra small", "Strength: High"), "Compact, syrupy shot")
            ),
            bestFor = listOf("Sweet-concentrated preference", "Short coffee breaks"),
        ),
        BrewingMethod(
            id = "lungo",
            category = CATEGORY_ESPRESSO_BASED,
            title = "Lungo",
            cardSubtitle = "Longer and lighter",
            summary = "A longer espresso extraction with a lighter, more diluted profile.",
            howItWorks = "Extends water contact time beyond espresso to increase cup volume and lighter body.",
            brewTime = "35-45 sec",
            tasteProfile = listOf("Long", "Light Body", "Slightly Bitter"),
            brewCharacteristics = listOf("Extended extraction", "Larger volume", "More aromatic top notes"),
            imageRes = R.drawable.lungo,
            howToBrew = listOf(
                "Start from espresso grind.",
                "Extract to 50-60 ml total cup.",
                "Lower temperature slightly if bitterness rises."
            ),
            homeMachineTips = listOf(
                HomeMachineTip("Bean-to-Cup", listOf("Drink: Lungo", "Strength: Medium", "Cup: Medium"), "Balanced long cup")
            ),
            bestFor = listOf("Long sipping", "Less concentrated espresso"),
        ),
        BrewingMethod(
            id = "cappuccino",
            category = CATEGORY_ESPRESSO_BASED,
            title = "Cappuccino",
            cardSubtitle = "Espresso with dense foam",
            summary = "Espresso, steamed milk, and dense foam in balanced layers.",
            howItWorks = "Combines a concentrated espresso base with textured milk for contrast and sweetness.",
            brewTime = "3-4 min",
            tasteProfile = listOf("Creamy", "Cocoa", "Balanced"),
            brewCharacteristics = listOf("Milk foam crown", "Soft sweetness", "Rounded body"),
            imageRes = R.drawable.cappuccino,
            howToBrew = listOf(
                "Pull one espresso shot.",
                "Steam milk to silky foam with volume.",
                "Pour in thirds: espresso, milk, foam."
            ),
            homeMachineTips = listOf(
                HomeMachineTip("Automatic Milk System", listOf("Milk foam: High", "Strength: Medium-High", "Cup: 180 ml"), "Classic café-style cappuccino")
            ),
            bestFor = listOf("Milk coffee lovers", "Balanced intensity"),
        ),
        BrewingMethod(
            id = "latte",
            category = CATEGORY_ESPRESSO_BASED,
            title = "Latte",
            cardSubtitle = "Smooth and milky",
            summary = "A milk-forward espresso drink with soft texture and gentle intensity.",
            howItWorks = "Uses a smaller espresso base with a larger amount of steamed milk.",
            brewTime = "3-4 min",
            tasteProfile = listOf("Milky", "Soft", "Caramel"),
            brewCharacteristics = listOf("High milk ratio", "Low sharpness", "Silky texture"),
            imageRes = R.drawable.latte,
            howToBrew = listOf(
                "Pull one espresso shot.",
                "Steam milk to fine microfoam.",
                "Pour milk gently to keep a smooth body."
            ),
            homeMachineTips = listOf(
                HomeMachineTip("Philips LatteGo", listOf("Milk foam: Medium", "Strength: Medium", "Cup: 240 ml"), "Soft everyday latte")
            ),
            bestFor = listOf("Milder coffee preference", "Evening cups"),
        ),
        BrewingMethod(
            id = "flat_white",
            category = CATEGORY_ESPRESSO_BASED,
            title = "Flat White",
            cardSubtitle = "Stronger than latte",
            summary = "Microfoam milk coffee with stronger espresso presence.",
            howItWorks = "Pairs a ristretto-style base with thin microfoam for higher coffee intensity.",
            brewTime = "3-4 min",
            tasteProfile = listOf("Velvety", "Rich", "Sweet"),
            brewCharacteristics = listOf("Microfoam", "Compact volume", "Pronounced espresso"),
            imageRes = R.drawable.flat_white,
            howToBrew = listOf(
                "Pull a short double shot.",
                "Steam milk with very fine microfoam.",
                "Pour to integrate, not layer heavily."
            ),
            homeMachineTips = listOf(
                HomeMachineTip("Home Espresso + Steam Wand", listOf("Shot: Double", "Milk foam: Low", "Cup: 160-180 ml"), "Coffee-forward milk drink")
            ),
            bestFor = listOf("Espresso taste with milk comfort"),
        ),
        BrewingMethod(
            id = "cortado",
            category = CATEGORY_ESPRESSO_BASED,
            title = "Cortado",
            cardSubtitle = "1:1 coffee and milk",
            summary = "Equal parts espresso and warm milk with low foam.",
            howItWorks = "A small milk addition cuts acidity while preserving espresso texture.",
            brewTime = "2-3 min",
            tasteProfile = listOf("Balanced", "Soft Acidity", "Dense"),
            brewCharacteristics = listOf("Small volume", "Low foam", "Textured finish"),
            imageRes = R.drawable.cortado,
            howToBrew = listOf(
                "Pull one espresso shot.",
                "Steam a small amount of milk with minimal foam.",
                "Combine roughly 1:1 by volume."
            ),
            homeMachineTips = listOf(
                HomeMachineTip("Bean-to-Cup + Milk Frother", listOf("Espresso: Standard", "Milk: 40-50 ml"), "Balanced cortado profile")
            ),
            bestFor = listOf("Compact milk coffee", "Lower acidity"),
        ),
        BrewingMethod(
            id = "macchiato",
            category = CATEGORY_ESPRESSO_BASED,
            title = "Macchiato",
            cardSubtitle = "Espresso marked with foam",
            summary = "Espresso topped with a small spoon of milk foam.",
            howItWorks = "Keeps espresso dominant while adding a slight creamy touch.",
            brewTime = "2-3 min",
            tasteProfile = listOf("Intense", "Creamy Top", "Short"),
            brewCharacteristics = listOf("Minimal milk", "Espresso-forward", "Aromatic"),
            imageRes = R.drawable.macchiato,
            howToBrew = listOf(
                "Pull one espresso shot.",
                "Steam milk and collect dense foam.",
                "Top espresso with a small spoon of foam."
            ),
            homeMachineTips = listOf(
                HomeMachineTip("Automatic Espresso + Milk", listOf("Espresso: Strong", "Foam: Small portion"), "Classic macchiato mark")
            ),
            bestFor = listOf("Espresso fans who want slight softness"),
        ),
        BrewingMethod(
            id = "americano",
            category = CATEGORY_ESPRESSO_BASED,
            title = "Americano",
            cardSubtitle = "Espresso + hot water",
            summary = "Espresso diluted with hot water for a longer black cup.",
            howItWorks = "Maintains espresso flavor while reducing intensity through controlled dilution.",
            brewTime = "2-3 min",
            tasteProfile = listOf("Clean", "Long", "Balanced"),
            brewCharacteristics = listOf("Diluted espresso", "Lower intensity", "Easy sipping"),
            imageRes = R.drawable.americano,
            howToBrew = listOf(
                "Pull one or two espresso shots.",
                "Add 90-150 ml hot water.",
                "Adjust ratio by roast intensity."
            ),
            homeMachineTips = listOf(
                HomeMachineTip("Automatic Machine", listOf("Americano mode", "Strength: Medium", "Cup: Medium-Large"), "Daily balanced black coffee")
            ),
            bestFor = listOf("Long black coffee drinkers"),
        ),
        BrewingMethod(
            id = "long_black",
            category = CATEGORY_ESPRESSO_BASED,
            title = "Long Black",
            cardSubtitle = "Espresso over water",
            summary = "Espresso poured over hot water to preserve crema.",
            howItWorks = "Water goes first, then espresso, keeping a richer top layer and aroma.",
            brewTime = "2-3 min",
            tasteProfile = listOf("Aromatic", "Layered", "Crema"),
            brewCharacteristics = listOf("Crema retention", "Water-first build", "Cleaner finish"),
            imageRes = R.drawable.long_black,
            howToBrew = listOf(
                "Prepare 100-120 ml hot water in cup.",
                "Extract espresso directly on top.",
                "Do not stir immediately to keep crema."
            ),
            homeMachineTips = listOf(
                HomeMachineTip("Home Espresso Machine", listOf("Double shot", "Hot water first", "Cup: 160 ml"), "Preserved crema style")
            ),
            bestFor = listOf("Crema lovers", "Long yet intense cups"),
        ),
        BrewingMethod(
            id = "v60",
            category = CATEGORY_FILTER_MANUAL,
            title = "V60",
            cardSubtitle = "Bright and clean",
            summary = "Conical filter brewing known for clarity and lively acidity.",
            howItWorks = "Fast flow through spiral-rib dripper highlights origin clarity and aromatics.",
            brewTime = "3-4 min",
            tasteProfile = listOf("Bright", "Clean", "Tea-like"),
            brewCharacteristics = listOf("Fast flow", "High clarity", "Precision pouring"),
            imageRes = R.drawable.v60,
            howToBrew = listOf(
                "Use 18g medium-fine coffee and 300g water.",
                "Bloom with 50g water for 30-40 seconds.",
                "Continue in controlled pulses until total drawdown."
            ),
            homeMachineTips = listOf(
                HomeMachineTip("Home Drip Machine", listOf("Grind: Medium", "Ratio: 1:16"), "Closest practical filter-like cup")
            ),
            bestFor = listOf("Floral and fruit-forward beans", "Precision brewers"),
        ),
        BrewingMethod(
            id = "chemex",
            category = CATEGORY_FILTER_MANUAL,
            title = "Chemex",
            cardSubtitle = "Crisp and wine-like",
            summary = "Thicker filter brew with very clean body and elegant aromatics.",
            howItWorks = "Heavy paper filter removes more oils, creating a crisp and transparent cup.",
            brewTime = "4-5 min",
            tasteProfile = listOf("Crisp", "Clean", "Delicate"),
            brewCharacteristics = listOf("Thick filter", "Low sediment", "Elegant finish"),
            imageRes = R.drawable.chemex,
            howToBrew = listOf(
                "Rinse filter thoroughly and add medium coffee grind.",
                "Bloom, then pour in circular stages.",
                "Finish around 4:30 to 5:00 total time."
            ),
            homeMachineTips = emptyList(),
            homeMachineNote = "Best made manually to keep Chemex clarity and texture.",
            bestFor = listOf("Clean cup preference", "Single-origin tasting"),
        ),
        BrewingMethod(
            id = "kalita_wave",
            category = CATEGORY_FILTER_MANUAL,
            title = "Kalita Wave",
            cardSubtitle = "Balanced and fuller",
            summary = "Flat-bottom filter method with stable extraction and balanced body.",
            howItWorks = "Three-hole flat bed slows and evens water flow for consistency.",
            brewTime = "3:30-4:30",
            tasteProfile = listOf("Balanced", "Sweet", "Fuller"),
            brewCharacteristics = listOf("Flat bed", "Stable flow", "Consistent extraction"),
            imageRes = R.drawable.kalita_wave,
            howToBrew = listOf(
                "Use medium grind and rinse filter.",
                "Bloom 30 seconds, then pulse pour steadily.",
                "Keep bed level for even extraction."
            ),
            homeMachineTips = emptyList(),
            homeMachineNote = "Primarily a manual method; automatic machines cannot mimic its flat-bed flow exactly.",
            bestFor = listOf("Balanced flavor seekers", "Consistent manual brewing"),
        ),
        BrewingMethod(
            id = "french_press",
            category = CATEGORY_FILTER_MANUAL,
            title = "French Press",
            cardSubtitle = "Full-bodied and rich",
            summary = "Immersion brew with metal filter and heavy body.",
            howItWorks = "Coffee steeps fully in water, preserving oils and texture before pressing.",
            brewTime = "5-6 min",
            tasteProfile = listOf("Rich", "Heavy", "Nutty"),
            brewCharacteristics = listOf("Metal filter oils", "Immersion", "Thick mouthfeel"),
            imageRes = R.drawable.french_press,
            howToBrew = listOf(
                "Use coarse grind at 1:15 ratio.",
                "Steep 4 minutes, break crust, rest 1 minute.",
                "Plunge slowly and serve immediately."
            ),
            homeMachineTips = listOf(
                HomeMachineTip("Bean-to-Cup", listOf("Use Americano profile", "Strength: Medium", "Cup: Large"), "Closest body-focused alternative")
            ),
            bestFor = listOf("Body lovers", "Low-acid preference"),
        ),
        BrewingMethod(
            id = "aeropress",
            category = CATEGORY_FILTER_MANUAL,
            title = "AeroPress",
            cardSubtitle = "Versatile and modern",
            summary = "Hybrid immersion-pressure brew that can be intense or smooth.",
            howItWorks = "Short steep and manual pressure produce a clean but expressive cup.",
            brewTime = "2-3 min",
            tasteProfile = listOf("Smooth", "Versatile", "Clean"),
            brewCharacteristics = listOf("Pressure-assisted", "Recipe-flexible", "Quick prep"),
            imageRes = R.drawable.aeropress,
            howToBrew = listOf(
                "Use medium-fine grind with 1:15 ratio.",
                "Steep 90 seconds and stir gently.",
                "Press in 25-30 seconds."
            ),
            homeMachineTips = listOf(
                HomeMachineTip("Home Espresso Machine", listOf("Use short shot", "Dilute after extraction"), "Aeropress-like clean long cup")
            ),
            bestFor = listOf("Experimenters", "Fast specialty brewing"),
        ),
        BrewingMethod(
            id = "clever_dripper",
            category = CATEGORY_FILTER_MANUAL,
            title = "Clever Dripper",
            cardSubtitle = "Immersion with clean finish",
            summary = "Immersion brew that drains like filter coffee for clarity and body balance.",
            howItWorks = "Steeps like French press, then releases through paper for a cleaner cup.",
            brewTime = "3-4 min",
            tasteProfile = listOf("Balanced", "Clean", "Sweet"),
            brewCharacteristics = listOf("Immersion + filter", "Easy control", "Low bitterness"),
            imageRes = R.drawable.clever_dripper,
            howToBrew = listOf(
                "Add medium grind coffee and hot water.",
                "Steep 2-3 minutes with lid.",
                "Place on cup to trigger drawdown."
            ),
            homeMachineTips = emptyList(),
            homeMachineNote = "Best made manually to preserve the immersion-to-filter transition.",
            bestFor = listOf("Low-effort manual brewing", "Balanced everyday cups"),
        ),
        BrewingMethod(
            id = "turkish_coffee",
            category = CATEGORY_TRADITIONAL_AUTHENTIC,
            title = "Turkish Coffee",
            cardSubtitle = "Intense and traditional",
            summary = "Ultra-fine coffee simmered in cezve and served unfiltered.",
            howItWorks = "Very fine coffee is heated slowly with water to build foam and dense texture.",
            brewTime = "4-6 min",
            tasteProfile = listOf("Intense", "Thick", "Traditional"),
            brewCharacteristics = listOf("Ultra-fine grind", "Foamy top", "Grounds in cup"),
            imageRes = R.drawable.turkish_coffee,
            howToBrew = listOf(
                "Mix cold water and ultra-fine coffee in cezve.",
                "Heat slowly and lift before boiling over.",
                "Repeat foam rise once, then serve gently."
            ),
            homeMachineTips = emptyList(),
            homeMachineNote = "Best made manually in cezve; machine shortcuts lose foam character.",
            bestFor = listOf("Traditional coffee rituals", "Dense texture lovers"),
        ),
        BrewingMethod(
            id = "moka_pot",
            category = CATEGORY_TRADITIONAL_AUTHENTIC,
            title = "Moka Pot",
            cardSubtitle = "Stovetop and robust",
            summary = "Stovetop pressure brew delivering strong, dense coffee.",
            howItWorks = "Steam pressure pushes hot water through coffee grounds into the top chamber.",
            brewTime = "4-6 min",
            tasteProfile = listOf("Strong", "Dense", "Roasty"),
            brewCharacteristics = listOf("Stovetop pressure", "Intense body", "No paper filter"),
            imageRes = R.drawable.moka_pot,
            howToBrew = listOf(
                "Fill base with hot water below valve.",
                "Add medium-fine grounds without tamping.",
                "Brew on medium-low heat and stop when gurgling starts."
            ),
            homeMachineTips = emptyList(),
            homeMachineNote = "This method is best on stovetop; automatic machines cannot replicate moka pressure profile.",
            bestFor = listOf("Strong home coffee", "Budget espresso-style cups"),
        ),
        BrewingMethod(
            id = "syphon",
            category = CATEGORY_TRADITIONAL_AUTHENTIC,
            title = "Syphon",
            cardSubtitle = "Theatrical and precise",
            summary = "Vacuum coffee brewing known for clarity, aroma, and visual ritual.",
            howItWorks = "Vapor pressure lifts water upward; vacuum draws brewed coffee back through a filter.",
            brewTime = "6-8 min",
            tasteProfile = listOf("Aromatic", "Clean", "Refined"),
            brewCharacteristics = listOf("Vacuum extraction", "High clarity", "Visual brewing"),
            imageRes = R.drawable.syphon,
            howToBrew = listOf(
                "Heat water in lower globe.",
                "Add coffee when water rises to upper chamber.",
                "Stir gently, brew briefly, then remove heat for drawdown."
            ),
            homeMachineTips = emptyList(),
            homeMachineNote = "Best made manually with syphon setup; this method is not suitable for automatic machines.",
            bestFor = listOf("Special occasions", "Aroma-focused cups"),
        ),
        BrewingMethod(
            id = "ibriq",
            category = CATEGORY_TRADITIONAL_AUTHENTIC,
            title = "Ibriq",
            cardSubtitle = "Regional cezve style",
            summary = "Traditional cezve-style preparation with regional spice variations.",
            howItWorks = "Fine coffee is simmered in a small pot, often with sugar or spices depending on region.",
            brewTime = "4-6 min",
            tasteProfile = listOf("Spiced", "Dense", "Traditional"),
            brewCharacteristics = listOf("Low heat", "Foam build", "Custom sweetness"),
            imageRes = R.drawable.ibriq,
            howToBrew = listOf(
                "Combine water, coffee, and optional sugar/spice.",
                "Heat slowly and watch foam rise.",
                "Serve without filtering."
            ),
            homeMachineTips = emptyList(),
            homeMachineNote = "Manual preparation is recommended to preserve authentic texture and spice control.",
            bestFor = listOf("Traditional flavor rituals", "Spice-friendly cups"),
        ),
        BrewingMethod(
            id = "cold_brew",
            category = CATEGORY_COLD_BREWING,
            title = "Cold Brew",
            cardSubtitle = "Smooth and low-acid",
            summary = "Coarse coffee steeped in cold water for 12-24 hours.",
            howItWorks = "Long cold immersion extracts sweetness and body while keeping acidity low.",
            brewTime = "12-24 h",
            tasteProfile = listOf("Smooth", "Sweet", "Low Acid"),
            brewCharacteristics = listOf("Long steep", "Cold extraction", "Concentrate-friendly"),
            imageRes = R.drawable.cold_brew,
            howToBrew = listOf(
                "Mix coarse coffee with cold water at 1:10.",
                "Steep in fridge for 12-24 hours.",
                "Filter and dilute to taste."
            ),
            homeMachineTips = listOf(
                HomeMachineTip("Bean-to-Cup Quick Alternative", listOf("Brew double espresso", "Cool over ice", "Add cold water"), "Fast cold-style cup when steeping is not possible")
            ),
            bestFor = listOf("Summer coffee", "Low-acidity preference"),
        ),
        BrewingMethod(
            id = "cold_drip",
            category = CATEGORY_COLD_BREWING,
            title = "Cold Drip",
            cardSubtitle = "Slow drip tower style",
            summary = "Slow, drop-by-drop cold extraction for bright and layered iced coffee.",
            howItWorks = "Cold water drips gradually through grounds, creating controlled extraction over hours.",
            brewTime = "4-8 h",
            tasteProfile = listOf("Bright", "Layered", "Silky"),
            brewCharacteristics = listOf("Drop control", "High clarity", "Elegant iced cup"),
            imageRes = R.drawable.cold_drip,
            howToBrew = listOf(
                "Set drip rate around one drop per second.",
                "Use medium-coarse grind in middle chamber.",
                "Collect brewed concentrate and chill."
            ),
            homeMachineTips = emptyList(),
            homeMachineNote = "Cold drip is best done with dedicated manual towers or drippers.",
            bestFor = listOf("Specialty iced coffee", "Aromatic cold cups"),
        ),
        BrewingMethod(
            id = "japanese_iced_coffee",
            category = CATEGORY_COLD_BREWING,
            title = "Japanese Iced Coffee",
            cardSubtitle = "Hot brewed over ice",
            summary = "Hot filter brew directly over ice to preserve aromatics and brightness.",
            howItWorks = "Brews hot for aroma extraction, then flash-chills on ice for clarity.",
            brewTime = "3-4 min",
            tasteProfile = listOf("Aromatic", "Crisp", "Refreshing"),
            brewCharacteristics = listOf("Flash chilled", "Preserved aromatics", "Clean finish"),
            imageRes = R.drawable.japanese_iced_coffee,
            howToBrew = listOf(
                "Place ice in server as part of total brew water.",
                "Brew hot using V60-style pour.",
                "Stir and serve immediately over fresh ice."
            ),
            homeMachineTips = listOf(
                HomeMachineTip("Home Drip Machine", listOf("Brew stronger batch", "Pour over ice immediately"), "Quick Japanese iced-style cup")
            ),
            bestFor = listOf("Iced coffee with lively aroma", "Warm weather"),
        ),
        BrewingMethod(
            id = "instant_coffee",
            category = CATEGORY_PRACTICAL_MODERN,
            title = "Instant Coffee",
            cardSubtitle = "Fast and practical",
            summary = "Brewed coffee dried into soluble granules for instant preparation.",
            howItWorks = "Granules dissolve directly in hot water, creating coffee in seconds.",
            brewTime = "1 min",
            tasteProfile = listOf("Quick", "Light Body", "Convenient"),
            brewCharacteristics = listOf("No equipment", "Portable", "Consistent"),
            imageRes = R.drawable.instant_coffee,
            howToBrew = listOf(
                "Add instant granules to cup.",
                "Pour hot water and stir fully.",
                "Adjust strength by teaspoon amount."
            ),
            homeMachineTips = listOf(
                HomeMachineTip("Home Convenience", listOf("Water temp: 85-90C", "Start with 1-2 tsp"), "Cleaner instant cup without harshness")
            ),
            bestFor = listOf("Travel", "Office", "Very quick cups"),
        ),
        BrewingMethod(
            id = "capsule_coffee",
            category = CATEGORY_PRACTICAL_MODERN,
            title = "Capsule Coffee",
            cardSubtitle = "Portioned and consistent",
            summary = "Single-serve capsule system for quick and repeatable coffee.",
            howItWorks = "Machine punctures capsule and pushes pressurized water through pre-dosed coffee.",
            brewTime = "1-2 min",
            tasteProfile = listOf("Consistent", "Convenient", "Balanced"),
            brewCharacteristics = listOf("Pre-dosed", "Low effort", "Fast cleanup"),
            imageRes = R.drawable.capsule_coffee,
            howToBrew = listOf(
                "Insert capsule and preheat machine.",
                "Choose cup volume for espresso or lungo style.",
                "Run clean water cycle after use when needed."
            ),
            homeMachineTips = listOf(
                HomeMachineTip("Capsule Machine", listOf("Use small cup for stronger profile", "Descale regularly"), "Stable flavor across capsules")
            ),
            bestFor = listOf("Speed and consistency", "Low-prep mornings"),
        ),
        BrewingMethod(
            id = "drip_bag",
            category = CATEGORY_PRACTICAL_MODERN,
            title = "Drip Bag",
            cardSubtitle = "Single-use hanging filter",
            summary = "Portable single-use filter bag that brews directly on the cup.",
            howItWorks = "A prefilled paper filter hangs on cup edges while you pour hot water in stages.",
            brewTime = "2-3 min",
            tasteProfile = listOf("Clean", "Easy", "Portable"),
            brewCharacteristics = listOf("Single-use format", "No gear", "Travel friendly"),
            imageRes = R.drawable.drip_bag,
            howToBrew = listOf(
                "Open drip bag and attach hooks to cup rim.",
                "Bloom with a small pour for 30 seconds.",
                "Complete pouring in 2-3 small stages."
            ),
            homeMachineTips = emptyList(),
            homeMachineNote = "Drip bags are manual and portable by design, no machine required.",
            bestFor = listOf("Travel brewing", "Quick clean cups"),
        )
    )

    private val patchedBaseBrewingMethods: List<BrewingMethod> = baseBrewingMethods.map { method ->
        when (method.id) {
            "syphon" -> method.copy(category = CATEGORY_FILTER_MANUAL)
            "ibriq" -> method.copy(
                title = "Arabic Coffee",
                cardSubtitle = "Spiced and aromatic",
                summary = "Traditional Arabic-style coffee preparation with gentle spice character.",
                howItWorks = "Fine coffee is simmered slowly, often with cardamom, for a fragrant cup.",
                tasteProfile = listOf("Aromatic", "Spiced", "Traditional")
            )
            "japanese_iced_coffee" -> method.copy(
                title = "Iced Coffee",
                cardSubtitle = "Hot brewed over ice"
            )
            else -> method
        }
    }

    private val taxonomyMethodAdditions: List<BrewingMethod> = listOf(
        BrewingMethod(
            id = "doppio",
            category = CATEGORY_ESPRESSO_BASED,
            title = "Doppio",
            cardSubtitle = "Double espresso shot",
            summary = "A double espresso built for fuller body and stronger intensity.",
            howItWorks = "Uses a double dose and yield to deliver richer espresso concentration.",
            brewTime = "25-30 sec",
            tasteProfile = listOf("Bold", "Dense", "Chocolatey"),
            brewCharacteristics = listOf("Double shot", "Thicker crema", "Longer finish"),
            imageRes = R.drawable.espresso,
            howToBrew = listOf(
                "Dose 18-20g finely ground coffee.",
                "Extract around 36-40g in 25-30 seconds.",
                "Adjust grind for balanced sweetness and body."
            ),
            homeMachineTips = listOf(
                HomeMachineTip("Bean-to-Cup", listOf("Shot: Double", "Strength: High"), "Full-bodied double shot")
            ),
            bestFor = listOf("Strong espresso drinkers", "Morning focus"),
        ),
        BrewingMethod(
            id = "mocha",
            category = CATEGORY_ESPRESSO_BASED,
            title = "Mocha",
            cardSubtitle = "Chocolate espresso milk drink",
            summary = "Espresso, milk, and chocolate in a richer dessert-style cup.",
            howItWorks = "Combines espresso intensity with cocoa sweetness and steamed milk texture.",
            brewTime = "4-5 min",
            tasteProfile = listOf("Chocolatey", "Creamy", "Sweet"),
            brewCharacteristics = listOf("Cocoa integration", "Milk-forward", "Rounded finish"),
            imageRes = R.drawable.latte,
            howToBrew = listOf(
                "Pull one espresso shot.",
                "Mix with chocolate sauce or cocoa base.",
                "Add steamed milk and finish with light foam."
            ),
            homeMachineTips = listOf(
                HomeMachineTip("Automatic Milk System", listOf("Espresso: Standard", "Milk: Medium", "Add cocoa"), "Balanced café-style mocha")
            ),
            bestFor = listOf("Sweet coffee lovers", "Comfort drinks"),
        ),
        BrewingMethod(
            id = "pour_over",
            category = CATEGORY_FILTER_MANUAL,
            title = "Pour Over",
            cardSubtitle = "Hand-poured filter classic",
            summary = "General hand-pour technique focused on clarity and controlled extraction.",
            howItWorks = "Hot water is poured in stages through ground coffee and paper filter.",
            brewTime = "3-4 min",
            tasteProfile = listOf("Clean", "Balanced", "Aromatic"),
            brewCharacteristics = listOf("Manual control", "Pulse pouring", "Clean cup"),
            imageRes = R.drawable.v60,
            howToBrew = listOf(
                "Rinse filter and add medium grind coffee.",
                "Bloom for 30-40 seconds.",
                "Pour in pulses until desired volume is reached."
            ),
            homeMachineTips = emptyList(),
            homeMachineNote = "For authentic results, prepare manually with a gooseneck kettle.",
            bestFor = listOf("Hands-on brewers", "Clean everyday cups"),
        ),
        BrewingMethod(
            id = "greek_coffee",
            category = CATEGORY_TRADITIONAL_AUTHENTIC,
            title = "Greek Coffee",
            cardSubtitle = "Traditional briki method",
            summary = "Fine-ground coffee simmered in a briki with rich foam and dense body.",
            howItWorks = "Coffee and water are heated slowly for controlled foam and layered flavor.",
            brewTime = "4-6 min",
            tasteProfile = listOf("Dense", "Traditional", "Aromatic"),
            brewCharacteristics = listOf("Fine grind", "Foam rise", "Unfiltered cup"),
            imageRes = R.drawable.turkish_coffee,
            howToBrew = listOf(
                "Add water and fine coffee to a briki.",
                "Heat slowly until foam rises.",
                "Serve gently without filtering."
            ),
            homeMachineTips = emptyList(),
            homeMachineNote = "Best prepared manually to preserve authentic ritual and texture.",
            bestFor = listOf("Traditional rituals", "Dense texture lovers"),
        ),
        BrewingMethod(
            id = "vietnamese_phin",
            category = CATEGORY_TRADITIONAL_AUTHENTIC,
            title = "Vietnamese Phin",
            cardSubtitle = "Slow metal drip",
            summary = "A slow-drip metal filter method with concentrated and sweet potential.",
            howItWorks = "Hot water drips through a compact metal filter directly into the cup.",
            brewTime = "5-7 min",
            tasteProfile = listOf("Bold", "Sweet", "Rich"),
            brewCharacteristics = listOf("Metal filter", "Slow drip", "Concentrated brew"),
            imageRes = R.drawable.clever_dripper,
            howToBrew = listOf(
                "Add medium-fine coffee to the phin chamber.",
                "Bloom briefly, then fill with hot water.",
                "Let it drip slowly into cup and serve."
            ),
            homeMachineTips = emptyList(),
            homeMachineNote = "Manual phin setup is essential for authentic extraction speed and body.",
            bestFor = listOf("Strong traditional cups", "Sweetened iced coffee styles"),
        ),
        BrewingMethod(
            id = "ethiopian_coffee_ceremony",
            category = CATEGORY_TRADITIONAL_AUTHENTIC,
            title = "Ethiopian Coffee Ceremony",
            cardSubtitle = "Ceremonial jebena brew",
            summary = "A ceremonial brewing ritual centered on aroma, patience, and community.",
            howItWorks = "Freshly roasted coffee is ground and brewed in a jebena over gentle heat.",
            brewTime = "15-25 min",
            tasteProfile = listOf("Aromatic", "Layered", "Traditional"),
            brewCharacteristics = listOf("Ritual preparation", "Fresh roast aroma", "Shared service"),
            imageRes = R.drawable.syphon,
            howToBrew = listOf(
                "Roast and grind beans freshly when possible.",
                "Brew in jebena with controlled heat.",
                "Serve in rounds to share evolving flavor."
            ),
            homeMachineTips = emptyList(),
            homeMachineNote = "This heritage method is ritual-based and best preserved through manual preparation.",
            bestFor = listOf("Cultural coffee rituals", "Aroma-rich shared experiences"),
        ),
        BrewingMethod(
            id = "coffee_machines",
            category = CATEGORY_PRACTICAL_MODERN,
            title = "Coffee Machines",
            cardSubtitle = "Everyday convenience brewing",
            summary = "Automatic or semi-automatic machines for quick, consistent daily coffee.",
            howItWorks = "Pre-configured brewing controls simplify extraction and improve repeatability.",
            brewTime = "2-4 min",
            tasteProfile = listOf("Consistent", "Convenient", "Balanced"),
            brewCharacteristics = listOf("Programmed control", "Low effort", "Repeatable cups"),
            imageRes = R.drawable.capsule_coffee,
            howToBrew = listOf(
                "Preheat machine and cup.",
                "Use fresh beans or quality grounds.",
                "Clean brew path regularly for stable flavor."
            ),
            homeMachineTips = listOf(
                HomeMachineTip("Automatic Drip/Combo", listOf("Use filtered water", "Clean weekly"), "Reliable daily cup quality")
            ),
            bestFor = listOf("Busy mornings", "Consistent routine brewing"),
        ),
        BrewingMethod(
            id = "bean_to_cup_machines",
            category = CATEGORY_PRACTICAL_MODERN,
            title = "Bean-to-Cup Machines",
            cardSubtitle = "Grind and brew in one flow",
            summary = "Integrated grinder systems that automate fresh grinding and extraction.",
            howItWorks = "Beans are ground per cup and brewed immediately through built-in recipes.",
            brewTime = "1-3 min",
            tasteProfile = listOf("Fresh", "Consistent", "Smooth"),
            brewCharacteristics = listOf("Integrated grinder", "One-touch brewing", "Fresh extraction"),
            imageRes = R.drawable.espresso,
            howToBrew = listOf(
                "Use fresh beans and adjust grind setting.",
                "Choose drink profile and strength.",
                "Run cleaning cycles to preserve taste."
            ),
            homeMachineTips = listOf(
                HomeMachineTip("Bean-to-Cup", listOf("Grind: Medium", "Strength: Medium-High"), "Balanced fresh espresso-style cup")
            ),
            bestFor = listOf("Fresh daily coffee", "Low-friction espresso drinks"),
        )
    )

    private val curatedBrewingMethodOrder: List<String> = listOf(
        "espresso",
        "doppio",
        "ristretto",
        "lungo",
        "americano",
        "cappuccino",
        "latte",
        "flat_white",
        "macchiato",
        "cortado",
        "mocha",
        "long_black",
        "v60",
        "pour_over",
        "chemex",
        "kalita_wave",
        "aeropress",
        "french_press",
        "syphon",
        "clever_dripper",
        "turkish_coffee",
        "greek_coffee",
        "ibriq",
        "moka_pot",
        "vietnamese_phin",
        "ethiopian_coffee_ceremony",
        "cold_brew",
        "cold_drip",
        "japanese_iced_coffee",
        "capsule_coffee",
        "coffee_machines",
        "bean_to_cup_machines",
        "instant_coffee",
        "drip_bag"
    )

    val brewingMethods: List<BrewingMethod> = run {
        val methodMap = (patchedBaseBrewingMethods + taxonomyMethodAdditions).associateBy { it.id }
        val primary = curatedBrewingMethodOrder.mapNotNull(methodMap::get)
        val extras = methodMap.values
            .filterNot { method -> curatedBrewingMethodOrder.contains(method.id) }
            .sortedBy { it.title }
        (primary + extras).map(::resourceDrivenBrewingSkeleton)
    }

    fun brewingMethodsByCategory(methods: List<BrewingMethod> = brewingMethods): List<Pair<String, List<BrewingMethod>>> {
        return brewingCategoryOrder.mapNotNull { category ->
            val categoryMethods = methods.filter { it.category == category }
            if (categoryMethods.isEmpty()) null else category to categoryMethods
        }
    }

    private fun resourceDrivenBrewingSkeleton(method: BrewingMethod): BrewingMethod {
        return method.copy(
            title = method.id,
            cardSubtitle = "",
            summary = "",
            howItWorks = "",
            tasteProfile = emptyList(),
            brewCharacteristics = emptyList(),
            howToBrew = emptyList(),
            homeMachineTips = emptyList(),
            homeMachineNote = null,
            bestFor = emptyList(),
        )
    }

    fun methodById(id: String): BrewingMethod =
        brewingMethods.firstOrNull { it.id == id } ?: brewingMethods.first()

    fun recommendationPool(mood: MoodType): List<RecommendationItem> =
        recommendationsByMood[mood].orEmpty()

    fun methodTraits(methodId: String): Set<RecommendationTrait> = when (methodId) {
        "espresso", "doppio", "ristretto", "lungo", "americano", "long_black", "moka_pot", "turkish_coffee", "greek_coffee", "ibriq", "vietnamese_phin" ->
            setOf(RecommendationTrait.BOLD)
        "latte", "cappuccino", "flat_white", "macchiato", "cortado", "mocha" ->
            setOf(RecommendationTrait.MILK, RecommendationTrait.SWEET)
        "v60", "pour_over", "chemex", "kalita_wave", "clever_dripper", "french_press", "aeropress", "syphon", "ethiopian_coffee_ceremony" ->
            setOf(RecommendationTrait.FILTER, RecommendationTrait.CLEAN)
        "cold_brew", "cold_drip", "japanese_iced_coffee" ->
            setOf(RecommendationTrait.COLD, RecommendationTrait.CLEAN)
        "capsule_coffee", "coffee_machines", "bean_to_cup_machines", "instant_coffee" ->
            setOf(RecommendationTrait.CLEAN)
        else -> emptySet()
    }
}
