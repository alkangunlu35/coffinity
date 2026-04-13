package com.icoffee.app.data

import com.icoffee.app.data.model.BeanRepository
import com.icoffee.app.data.model.beans.BeanVariety
import com.icoffee.app.data.model.beans.CountryBeans
import com.icoffee.app.data.model.beans.CountryGroup

object CountryBeansRepository {

    private val countries: List<CountryBeans> = BeanRepository.countries.map { legacyCountry ->
        CountryBeans(
            id = legacyCountry.id,
            country = legacyCountry.name,
            continent = legacyCountry.continent.label,
            flagEmoji = legacyCountry.flag,
            varieties = legacyCountry.varieties.map { legacyVariety ->
                BeanVariety(
                    name = legacyVariety.name,
                    description = legacyVariety.description,
                    flavorNotes = legacyVariety.flavors,
                    processing = legacyVariety.processing,
                    altitude = legacyVariety.altitude,
                    roast = legacyVariety.roast,
                    species = legacyVariety.species,
                    recommendedBrewing = legacyVariety.brewingMethods
                )
            }
        )
    }

    private val orderedContinents: List<String> = BeanRepository.countries
        .map { it.continent.label }
        .distinct()

    fun getAllCountries(): List<CountryBeans> = countries

    fun getContinents(): List<String> = orderedContinents

    fun getCountriesByContinent(continent: String): List<CountryBeans> =
        countries.filter { it.continent.equals(continent, ignoreCase = true) }

    fun searchCountries(query: String): List<CountryBeans> {
        if (query.isBlank()) return countries
        val normalized = query.trim().lowercase()
        return countries.filter { country ->
            country.country.lowercase().contains(normalized) ||
                country.continent.lowercase().contains(normalized) ||
                country.varieties.any { variety ->
                    variety.name.lowercase().contains(normalized) ||
                        variety.description.lowercase().contains(normalized) ||
                        variety.flavorNotes.any { it.lowercase().contains(normalized) }
                }
        }
    }

    fun getCountry(countryName: String): CountryBeans? {
        val normalized = countryName.trim()
        return countries.firstOrNull { item ->
            item.id.equals(normalized, ignoreCase = true) ||
                item.country.equals(normalized, ignoreCase = true)
        }
    }

    fun getGroupedCountries(
        continentFilter: String?,
        query: String
    ): List<CountryGroup> {
        val filteredByQuery = if (query.isBlank()) countries else searchCountries(query)
        val filtered = if (continentFilter.isNullOrBlank() || continentFilter == "All") {
            filteredByQuery
        } else {
            filteredByQuery.filter { it.continent.equals(continentFilter, ignoreCase = true) }
        }
        return getContinents().mapNotNull { continent ->
            val items = filtered.filter { it.continent == continent }
            if (items.isEmpty()) null else CountryGroup(continent = continent, countries = items)
        }
    }
}
