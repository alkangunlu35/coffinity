package com.icoffee.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.icoffee.app.data.OpenFoodFactsRepository
import com.icoffee.app.data.ProductLookupResult
import com.icoffee.app.data.matching.BasicMatchScoreEngine
import com.icoffee.app.data.model.CoffeeMatchResult
import com.icoffee.app.data.model.CoffeeProfile
import com.icoffee.app.data.model.OpenFoodFactsProduct
import com.icoffee.app.data.model.UserTasteSummary
import com.icoffee.app.data.model.toSummary
import com.icoffee.app.data.normalization.CoffeeProfileNormalizer
import com.icoffee.app.data.profile.TasteReaction
import com.icoffee.app.data.profile.UserTasteProfileRepository
import kotlinx.coroutines.launch

sealed class ScanResultUiState {
    data object Loading : ScanResultUiState()
    data class Found(
        val product: OpenFoodFactsProduct,
        val profile: CoffeeProfile,
        val matchResult: CoffeeMatchResult,
        val profileSummary: UserTasteSummary,
        val isFavorited: Boolean = false
    ) : ScanResultUiState()
    data object NotFound : ScanResultUiState()
    data class Error(val message: String) : ScanResultUiState()
}

class ScanResultViewModel(
    private val repository: OpenFoodFactsRepository = OpenFoodFactsRepository()
) : ViewModel() {

    var uiState by mutableStateOf<ScanResultUiState>(ScanResultUiState.Loading)
        private set

    private var lastBarcode: String? = null

    fun lookup(barcode: String, force: Boolean = false) {
        val normalized = barcode.filter(Char::isDigit)
        if (normalized.isBlank()) {
            uiState = ScanResultUiState.NotFound
            return
        }

        if (!force && lastBarcode == normalized && uiState !is ScanResultUiState.Error) {
            return
        }

        lastBarcode = normalized
        uiState = ScanResultUiState.Loading

        viewModelScope.launch {
            uiState = when (val result = repository.lookupByBarcode(normalized)) {
                is ProductLookupResult.Found -> {
                    val profile = CoffeeProfileNormalizer.normalize(result.product)
                    val updatedProfile = UserTasteProfileRepository.onProductScanned(profile)
                    val matchResult = BasicMatchScoreEngine.calculate(
                        coffeeProfile = profile,
                        userProfile = updatedProfile
                    )
                    ScanResultUiState.Found(
                        product = result.product,
                        profile = profile,
                        matchResult = matchResult,
                        profileSummary = updatedProfile.toSummary(),
                        isFavorited = UserTasteProfileRepository.isFavoriteScan(profile.barcode)
                    )
                }
                ProductLookupResult.NotFound -> ScanResultUiState.NotFound
                is ProductLookupResult.Error -> ScanResultUiState.Error(result.message)
            }
        }
    }

    fun retry() {
        lastBarcode?.let { lookup(it, force = true) }
    }

    fun toggleFavorite() {
        val state = uiState as? ScanResultUiState.Found ?: return
        if (state.isFavorited) {
            UserTasteProfileRepository.onProductUnfavorited(state.profile)
        } else {
            UserTasteProfileRepository.onProductFavorited(state.profile)
        }
        val updatedProfile = UserTasteProfileRepository.currentProfile()
        uiState = state.copy(
            isFavorited = !state.isFavorited,
            profileSummary = updatedProfile.toSummary(),
            matchResult = BasicMatchScoreEngine.calculate(
                coffeeProfile = state.profile,
                userProfile = updatedProfile
            )
        )
    }

    fun submitQuickReaction(reaction: TasteReaction) {
        val state = uiState as? ScanResultUiState.Found ?: return
        val updatedProfile = UserTasteProfileRepository.onQuickReaction(state.profile, reaction)
        uiState = state.copy(
            profileSummary = updatedProfile.toSummary(),
            matchResult = BasicMatchScoreEngine.calculate(
                coffeeProfile = state.profile,
                userProfile = updatedProfile
            )
        )
    }
}
