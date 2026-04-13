package com.icoffee.app.localization.formatter

import android.content.Context
import com.icoffee.app.R

object PluralFormatter {
    fun peopleCount(context: Context, count: Int): String =
        context.resources.getQuantityString(R.plurals.meet_people_count_plural, count, count)

    fun countriesCount(context: Context, count: Int): String =
        context.resources.getQuantityString(R.plurals.beans_country_count, count, count)
}
