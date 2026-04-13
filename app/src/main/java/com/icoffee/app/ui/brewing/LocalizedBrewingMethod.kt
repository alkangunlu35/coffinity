package com.icoffee.app.ui.brewing

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.icoffee.app.R
import com.icoffee.app.data.PhaseOneRepository
import com.icoffee.app.data.model.BrewingMethod

@Composable
fun localizedBrewingMethod(method: BrewingMethod): BrewingMethod {
    val title = stringResource(brewingMethodTitleRes(method.id))
    val subtitle = stringResource(brewCategorySubtitleRes(method.category))
    val summary = stringResource(brewCategorySummaryRes(method.category), title)
    val howItWorks = stringResource(brewCategoryHowItWorksRes(method.category), title)

    val tasteProfile = brewCategoryTasteRes(method.category).map { stringResource(it) }
    val brewCharacteristics = brewCategoryCharacteristicRes(method.category).map { stringResource(it) }
    val howToBrew = listOf(
        stringResource(R.string.brew_step_prepare, title),
        stringResource(brewCategoryCoreStepRes(method.category), title),
        stringResource(R.string.brew_step_finish, title)
    )

    val bestFor = brewCategoryBestForRes(method.category).map { stringResource(it) }
    val localizedBrewTime = localizeBrewTime(method.brewTime)

    return method.copy(
        title = title,
        cardSubtitle = subtitle,
        summary = summary,
        howItWorks = howItWorks,
        brewTime = localizedBrewTime,
        tasteProfile = tasteProfile,
        brewCharacteristics = brewCharacteristics,
        howToBrew = howToBrew,
        homeMachineTips = emptyList(),
        homeMachineNote = stringResource(R.string.brew_home_note_generic, title),
        bestFor = bestFor
    )
}

@Composable
private fun localizeBrewTime(raw: String): String {
    val sec = stringResource(R.string.brew_unit_sec)
    val min = stringResource(R.string.brew_unit_min)
    val hour = stringResource(R.string.brew_unit_hour)

    return raw
        .replace("sec", sec, ignoreCase = true)
        .replace("min", min, ignoreCase = true)
        .replace(" h", " $hour", ignoreCase = true)
}

@StringRes
private fun brewCategorySubtitleRes(category: String): Int = when (category) {
    PhaseOneRepository.CATEGORY_ESPRESSO_BASED -> R.string.brew_card_subtitle_espresso
    PhaseOneRepository.CATEGORY_FILTER_MANUAL -> R.string.brew_card_subtitle_filter
    PhaseOneRepository.CATEGORY_TRADITIONAL_AUTHENTIC -> R.string.brew_card_subtitle_traditional
    PhaseOneRepository.CATEGORY_COLD_BREWING -> R.string.brew_card_subtitle_cold
    PhaseOneRepository.CATEGORY_PRACTICAL_MODERN -> R.string.brew_card_subtitle_practical
    else -> R.string.brew_card_subtitle_filter
}

@StringRes
private fun brewCategorySummaryRes(category: String): Int = when (category) {
    PhaseOneRepository.CATEGORY_ESPRESSO_BASED -> R.string.brew_summary_espresso
    PhaseOneRepository.CATEGORY_FILTER_MANUAL -> R.string.brew_summary_filter
    PhaseOneRepository.CATEGORY_TRADITIONAL_AUTHENTIC -> R.string.brew_summary_traditional
    PhaseOneRepository.CATEGORY_COLD_BREWING -> R.string.brew_summary_cold
    PhaseOneRepository.CATEGORY_PRACTICAL_MODERN -> R.string.brew_summary_practical
    else -> R.string.brew_summary_filter
}

@StringRes
private fun brewCategoryHowItWorksRes(category: String): Int = when (category) {
    PhaseOneRepository.CATEGORY_ESPRESSO_BASED -> R.string.brew_how_espresso
    PhaseOneRepository.CATEGORY_FILTER_MANUAL -> R.string.brew_how_filter
    PhaseOneRepository.CATEGORY_TRADITIONAL_AUTHENTIC -> R.string.brew_how_traditional
    PhaseOneRepository.CATEGORY_COLD_BREWING -> R.string.brew_how_cold
    PhaseOneRepository.CATEGORY_PRACTICAL_MODERN -> R.string.brew_how_practical
    else -> R.string.brew_how_filter
}

@StringRes
private fun brewCategoryCoreStepRes(category: String): Int = when (category) {
    PhaseOneRepository.CATEGORY_ESPRESSO_BASED -> R.string.brew_step_core_espresso
    PhaseOneRepository.CATEGORY_FILTER_MANUAL -> R.string.brew_step_core_filter
    PhaseOneRepository.CATEGORY_TRADITIONAL_AUTHENTIC -> R.string.brew_step_core_traditional
    PhaseOneRepository.CATEGORY_COLD_BREWING -> R.string.brew_step_core_cold
    PhaseOneRepository.CATEGORY_PRACTICAL_MODERN -> R.string.brew_step_core_practical
    else -> R.string.brew_step_core_filter
}

private fun brewCategoryTasteRes(category: String): List<Int> = when (category) {
    PhaseOneRepository.CATEGORY_ESPRESSO_BASED -> listOf(
        R.string.brew_taste_espresso_a,
        R.string.brew_taste_espresso_b
    )

    PhaseOneRepository.CATEGORY_FILTER_MANUAL -> listOf(
        R.string.brew_taste_filter_a,
        R.string.brew_taste_filter_b
    )

    PhaseOneRepository.CATEGORY_TRADITIONAL_AUTHENTIC -> listOf(
        R.string.brew_taste_traditional_a,
        R.string.brew_taste_traditional_b
    )

    PhaseOneRepository.CATEGORY_COLD_BREWING -> listOf(
        R.string.brew_taste_cold_a,
        R.string.brew_taste_cold_b
    )

    PhaseOneRepository.CATEGORY_PRACTICAL_MODERN -> listOf(
        R.string.brew_taste_practical_a,
        R.string.brew_taste_practical_b
    )

    else -> listOf(R.string.brew_taste_filter_a, R.string.brew_taste_filter_b)
}

private fun brewCategoryCharacteristicRes(category: String): List<Int> = when (category) {
    PhaseOneRepository.CATEGORY_ESPRESSO_BASED -> listOf(
        R.string.brew_characteristic_espresso_1,
        R.string.brew_characteristic_espresso_2,
        R.string.brew_characteristic_espresso_3
    )

    PhaseOneRepository.CATEGORY_FILTER_MANUAL -> listOf(
        R.string.brew_characteristic_filter_1,
        R.string.brew_characteristic_filter_2,
        R.string.brew_characteristic_filter_3
    )

    PhaseOneRepository.CATEGORY_TRADITIONAL_AUTHENTIC -> listOf(
        R.string.brew_characteristic_traditional_1,
        R.string.brew_characteristic_traditional_2,
        R.string.brew_characteristic_traditional_3
    )

    PhaseOneRepository.CATEGORY_COLD_BREWING -> listOf(
        R.string.brew_characteristic_cold_1,
        R.string.brew_characteristic_cold_2,
        R.string.brew_characteristic_cold_3
    )

    PhaseOneRepository.CATEGORY_PRACTICAL_MODERN -> listOf(
        R.string.brew_characteristic_practical_1,
        R.string.brew_characteristic_practical_2,
        R.string.brew_characteristic_practical_3
    )

    else -> listOf(
        R.string.brew_characteristic_filter_1,
        R.string.brew_characteristic_filter_2,
        R.string.brew_characteristic_filter_3
    )
}

private fun brewCategoryBestForRes(category: String): List<Int> = when (category) {
    PhaseOneRepository.CATEGORY_ESPRESSO_BASED -> listOf(
        R.string.brew_best_for_espresso_1,
        R.string.brew_best_for_espresso_2
    )

    PhaseOneRepository.CATEGORY_FILTER_MANUAL -> listOf(
        R.string.brew_best_for_filter_1,
        R.string.brew_best_for_filter_2
    )

    PhaseOneRepository.CATEGORY_TRADITIONAL_AUTHENTIC -> listOf(
        R.string.brew_best_for_traditional_1,
        R.string.brew_best_for_traditional_2
    )

    PhaseOneRepository.CATEGORY_COLD_BREWING -> listOf(
        R.string.brew_best_for_cold_1,
        R.string.brew_best_for_cold_2
    )

    PhaseOneRepository.CATEGORY_PRACTICAL_MODERN -> listOf(
        R.string.brew_best_for_practical_1,
        R.string.brew_best_for_practical_2
    )

    else -> listOf(
        R.string.brew_best_for_filter_1,
        R.string.brew_best_for_filter_2
    )
}

@StringRes
fun brewingMethodTitleRes(methodId: String): Int = when (methodId) {
    "espresso" -> R.string.brew_method_espresso_title
    "doppio" -> R.string.brew_method_doppio_title
    "ristretto" -> R.string.brew_method_ristretto_title
    "lungo" -> R.string.brew_method_lungo_title
    "americano" -> R.string.brew_method_americano_title
    "cappuccino" -> R.string.brew_method_cappuccino_title
    "latte" -> R.string.brew_method_latte_title
    "flat_white" -> R.string.brew_method_flat_white_title
    "macchiato" -> R.string.brew_method_macchiato_title
    "cortado" -> R.string.brew_method_cortado_title
    "mocha" -> R.string.brew_method_mocha_title
    "long_black" -> R.string.brew_method_long_black_title
    "v60" -> R.string.brew_method_v60_title
    "pour_over" -> R.string.brew_method_pour_over_title
    "chemex" -> R.string.brew_method_chemex_title
    "kalita_wave" -> R.string.brew_method_kalita_wave_title
    "aeropress" -> R.string.brew_method_aeropress_title
    "french_press" -> R.string.brew_method_french_press_title
    "syphon" -> R.string.brew_method_syphon_title
    "clever_dripper" -> R.string.brew_method_clever_dripper_title
    "turkish_coffee" -> R.string.brew_method_turkish_coffee_title
    "greek_coffee" -> R.string.brew_method_greek_coffee_title
    "ibriq" -> R.string.brew_method_arabic_coffee_title
    "moka_pot" -> R.string.brew_method_moka_pot_title
    "vietnamese_phin" -> R.string.brew_method_vietnamese_phin_title
    "ethiopian_coffee_ceremony" -> R.string.brew_method_ethiopian_ceremony_title
    "cold_brew" -> R.string.brew_method_cold_brew_title
    "cold_drip" -> R.string.brew_method_cold_drip_title
    "japanese_iced_coffee" -> R.string.brew_method_iced_coffee_title
    "capsule_coffee" -> R.string.brew_method_capsule_coffee_title
    "coffee_machines" -> R.string.brew_method_coffee_machines_title
    "bean_to_cup_machines" -> R.string.brew_method_bean_to_cup_title
    "instant_coffee" -> R.string.brew_method_instant_coffee_title
    "drip_bag" -> R.string.brew_method_drip_bag_title
    else -> R.string.home_brewing_methods
}
