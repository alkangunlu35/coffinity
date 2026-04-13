package com.icoffee.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.icoffee.app.data.CoffeeRepository
import com.icoffee.app.data.model.Coffee
import com.icoffee.app.data.model.CoffeeMood

class DiscoverViewModel : ViewModel() {
    var selectedMood by mutableStateOf(CoffeeMood.SMOOTH)
        private set

    val moods: List<CoffeeMood> = CoffeeMood.entries

    val featuredCoffee: Coffee
        get() = CoffeeRepository.featuredByMood(selectedMood)

    fun selectMood(mood: CoffeeMood) {
        selectedMood = mood
    }
}
