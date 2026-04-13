package com.icoffee.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.icoffee.app.data.auth.FirebaseAuthRepository
import com.icoffee.app.data.firebase.model.FirestoreBrandSuggestion
import com.icoffee.app.data.firebase.model.FirestoreSuggestionActionLog
import com.icoffee.app.data.model.BrandLifecycleStatus
import com.icoffee.app.data.suggestion.SuggestionAdminActionResult
import com.icoffee.app.data.suggestion.SuggestionApprovalDraft
import com.icoffee.app.data.suggestion.SuggestionRepository
import kotlinx.coroutines.launch

class SuggestionAdminViewModel : ViewModel() {

    var suggestions by mutableStateOf<List<FirestoreBrandSuggestion>>(emptyList())
        private set

    var actionLogs by mutableStateOf<List<FirestoreSuggestionActionLog>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isActing by mutableStateOf(false)
        private set

    private val currentUserId: String
        get() = FirebaseAuthRepository.currentUser?.uid.orEmpty()

    fun refreshSuggestions(
        statusFilter: String?,
        query: String
    ) {
        val actorId = currentUserId
        if (actorId.isBlank()) {
            suggestions = emptyList()
            return
        }
        viewModelScope.launch {
            isLoading = true
            suggestions = SuggestionRepository.loadAdminSuggestions(
                actorUserId = actorId,
                statusFilter = statusFilter,
                query = query
            )
            isLoading = false
        }
    }

    fun loadLogs(suggestionId: String) {
        val actorId = currentUserId
        if (actorId.isBlank()) {
            actionLogs = emptyList()
            return
        }
        viewModelScope.launch {
            actionLogs = SuggestionRepository.loadActionLogs(
                actorUserId = actorId,
                suggestionId = suggestionId
            )
        }
    }

    fun markUnderReview(
        suggestionId: String,
        notes: String?,
        onResult: (SuggestionAdminActionResult) -> Unit
    ) {
        val actorId = currentUserId
        if (actorId.isBlank()) {
            onResult(SuggestionAdminActionResult.Failure(com.icoffee.app.data.suggestion.SuggestionAdminFailureReason.UNAUTHORIZED))
            return
        }
        viewModelScope.launch {
            isActing = true
            val result = SuggestionRepository.markUnderReview(actorId, suggestionId, notes)
            isActing = false
            onResult(result)
        }
    }

    fun approveAsNewBrand(
        suggestion: FirestoreBrandSuggestion,
        brandName: String,
        description: String,
        websiteUrl: String,
        instagramUrl: String,
        country: String,
        city: String,
        publishAsActive: Boolean,
        notes: String?,
        onResult: (SuggestionAdminActionResult) -> Unit
    ) {
        val actorId = currentUserId
        if (actorId.isBlank()) {
            onResult(SuggestionAdminActionResult.Failure(com.icoffee.app.data.suggestion.SuggestionAdminFailureReason.UNAUTHORIZED))
            return
        }
        viewModelScope.launch {
            isActing = true
            val result = SuggestionRepository.approveAsNewBrand(
                actorUserId = actorId,
                suggestionId = suggestion.id,
                draft = SuggestionApprovalDraft(
                    brandName = brandName,
                    description = description,
                    websiteUrl = websiteUrl,
                    instagramUrl = instagramUrl,
                    country = country,
                    city = city,
                    status = if (publishAsActive) {
                        BrandLifecycleStatus.ACTIVE.storageValue
                    } else {
                        BrandLifecycleStatus.DRAFT.storageValue
                    }
                ),
                notes = notes
            )
            isActing = false
            onResult(result)
        }
    }

    fun mergeIntoExistingBrand(
        suggestionId: String,
        targetBrandId: String,
        notes: String?,
        onResult: (SuggestionAdminActionResult) -> Unit
    ) {
        val actorId = currentUserId
        if (actorId.isBlank()) {
            onResult(SuggestionAdminActionResult.Failure(com.icoffee.app.data.suggestion.SuggestionAdminFailureReason.UNAUTHORIZED))
            return
        }
        viewModelScope.launch {
            isActing = true
            val result = SuggestionRepository.mergeIntoExistingBrand(
                actorUserId = actorId,
                suggestionId = suggestionId,
                targetBrandId = targetBrandId,
                notes = notes
            )
            isActing = false
            onResult(result)
        }
    }

    fun reject(
        suggestionId: String,
        rejectionReason: String,
        notes: String?,
        onResult: (SuggestionAdminActionResult) -> Unit
    ) {
        val actorId = currentUserId
        if (actorId.isBlank()) {
            onResult(SuggestionAdminActionResult.Failure(com.icoffee.app.data.suggestion.SuggestionAdminFailureReason.UNAUTHORIZED))
            return
        }
        viewModelScope.launch {
            isActing = true
            val result = SuggestionRepository.rejectSuggestion(
                actorUserId = actorId,
                suggestionId = suggestionId,
                rejectionReason = rejectionReason,
                notes = notes
            )
            isActing = false
            onResult(result)
        }
    }
}
