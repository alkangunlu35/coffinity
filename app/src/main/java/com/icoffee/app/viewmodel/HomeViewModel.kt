package com.icoffee.app.viewmodel

import androidx.lifecycle.ViewModel
import com.icoffee.app.data.CoffeeRepository
import com.icoffee.app.data.model.Coffee
import com.icoffee.app.data.model.CoffeeCategory

class HomeViewModel : ViewModel() {
    val categories: List<CoffeeCategory> = CoffeeCategory.entries
    val recommended: List<Coffee> = CoffeeRepository.coffees
}
