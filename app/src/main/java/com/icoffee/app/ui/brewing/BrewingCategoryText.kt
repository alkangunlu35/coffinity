package com.icoffee.app.ui.brewing

import androidx.annotation.StringRes
import com.icoffee.app.R
import com.icoffee.app.data.PhaseOneRepository

@StringRes
fun brewingCategoryTitleRes(category: String): Int = when (category) {
    PhaseOneRepository.CATEGORY_ESPRESSO_BASED -> R.string.brew_category_espresso_based_title
    PhaseOneRepository.CATEGORY_FILTER_MANUAL -> R.string.brew_category_filter_manual_title
    PhaseOneRepository.CATEGORY_TRADITIONAL_AUTHENTIC -> R.string.brew_category_traditional_authentic_title
    PhaseOneRepository.CATEGORY_COLD_BREWING -> R.string.brew_category_cold_methods_title
    PhaseOneRepository.CATEGORY_PRACTICAL_MODERN -> R.string.brew_category_practical_modern_title
    else -> R.string.home_brewing_methods
}

@StringRes
fun brewingCategorySubtitleRes(category: String): Int = when (category) {
    PhaseOneRepository.CATEGORY_ESPRESSO_BASED -> R.string.brew_category_espresso_based_subtitle
    PhaseOneRepository.CATEGORY_FILTER_MANUAL -> R.string.brew_category_filter_manual_subtitle
    PhaseOneRepository.CATEGORY_TRADITIONAL_AUTHENTIC -> R.string.brew_category_traditional_authentic_subtitle
    PhaseOneRepository.CATEGORY_COLD_BREWING -> R.string.brew_category_cold_methods_subtitle
    PhaseOneRepository.CATEGORY_PRACTICAL_MODERN -> R.string.brew_category_practical_modern_subtitle
    else -> R.string.home_brewing_methods
}
