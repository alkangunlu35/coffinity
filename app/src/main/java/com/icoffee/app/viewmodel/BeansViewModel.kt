package com.icoffee.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.icoffee.app.data.CountryBeansRepository
import com.icoffee.app.data.model.beans.CountryBeans
import com.icoffee.app.data.model.beans.CountryGroup

class BeansViewModel : ViewModel() {

    var selectedContinent by mutableStateOf("All")
        private set

    var searchQuery by mutableStateOf("")
        private set

    private val selectedVarietyIndexByCountry = mutableStateMapOf<String, Int>()

    val allCountries: List<CountryBeans>
        get() = CountryBeansRepository.getAllCountries()

    val continents: List<String>
        get() = listOf("All") + CountryBeansRepository.getContinents()

    val groupedCountries: List<CountryGroup>
        get() = CountryBeansRepository.getGroupedCountries(
            continentFilter = selectedContinent,
            query = searchQuery
        )

    fun selectContinent(continent: String) {
        selectedContinent = continent
    }

    fun updateQuery(query: String) {
        searchQuery = query
    }

    fun getCountry(countryIdOrName: String): CountryBeans? =
        CountryBeansRepository.getCountry(countryIdOrName)

    fun selectedVarietyIndex(countryId: String): Int =
        selectedVarietyIndexByCountry[countryId] ?: 0

    fun selectVariety(countryId: String, index: Int) {
        selectedVarietyIndexByCountry[countryId] = index
    }
}
