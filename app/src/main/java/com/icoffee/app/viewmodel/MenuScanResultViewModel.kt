package com.icoffee.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.icoffee.app.data.menu.MenuScanRepository
import com.icoffee.app.data.model.DetectedMenuItem
import com.icoffee.app.data.model.MenuScanResult
import com.icoffee.app.data.model.NormalizedCoffeeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class MenuScanResultUiState {
    data object Loading : MenuScanResultUiState()
    data class Found(
        val result: MenuScanResult,
        val manualSelection: DetectedMenuItem? = null
    ) : MenuScanResultUiState()
    data class NotFound(
        val result: MenuScanResult?,
        val manualSelection: DetectedMenuItem? = null
    ) : MenuScanResultUiState()
    data class Error(val message: String) : MenuScanResultUiState()
}

class MenuScanResultViewModel : ViewModel() {

    var uiState by mutableStateOf<MenuScanResultUiState>(MenuScanResultUiState.Loading)
        private set

    private var currentScanId: String? = null

    fun load(scanId: String, force: Boolean = false) {
        if (!force && currentScanId == scanId && uiState !is MenuScanResultUiState.Error) return
        currentScanId = scanId
        uiState = MenuScanResultUiState.Loading

        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                MenuScanRepository.getByScanId(scanId)
            }
            uiState = when {
                result == null -> MenuScanResultUiState.Error("menu_result_missing")
                result.detectedItems.isEmpty() -> MenuScanResultUiState.NotFound(result = result)
                else -> MenuScanResultUiState.Found(result = result)
            }
        }
    }

    fun chooseManualType(type: NormalizedCoffeeType) {
        val manual = MenuScanRepository.manualSelection(type, referenceScanId = currentScanId ?: "manual")
        uiState = when (val state = uiState) {
            is MenuScanResultUiState.Found -> state.copy(manualSelection = manual)
            is MenuScanResultUiState.NotFound -> state.copy(manualSelection = manual)
            else -> uiState
        }
    }

    fun retry() {
        currentScanId?.let { load(it, force = true) }
    }
}
