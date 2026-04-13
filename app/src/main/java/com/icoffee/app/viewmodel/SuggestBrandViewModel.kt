package com.icoffee.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.icoffee.app.data.auth.FirebaseAuthRepository
import com.icoffee.app.data.firebase.model.FirestoreBrandSuggestion
import com.icoffee.app.data.model.SuggestBrandInput
import com.icoffee.app.data.suggestion.SubmitBrandSuggestionResult
import com.icoffee.app.data.suggestion.SuggestionRepository
import com.icoffee.app.data.suggestion.SuggestionSubmitFailureReason
import kotlinx.coroutines.launch

class SuggestBrandViewModel : ViewModel() {

    var isSubmitting by mutableStateOf(false)
        private set

    var submitFeedbackMessage by mutableStateOf<String?>(null)
        private set

    var mySuggestions by mutableStateOf<List<FirestoreBrandSuggestion>>(emptyList())
        private set

    var isLoadingMySuggestions by mutableStateOf(false)
        private set

    private val currentUserId: String
        get() = FirebaseAuthRepository.currentUser?.uid.orEmpty()

    val isSignedIn: Boolean
        get() = currentUserId.isNotBlank()

    fun clearFeedback() {
        submitFeedbackMessage = null
    }

    fun submitSuggestion(
        input: SuggestBrandInput,
        onResult: (SubmitBrandSuggestionResult) -> Unit = {}
    ) {
        val actorId = currentUserId
        if (actorId.isBlank()) {
            val result = SubmitBrandSuggestionResult.Failure(SuggestionSubmitFailureReason.UNAUTHORIZED)
            onResult(result)
            return
        }
        viewModelScope.launch {
            isSubmitting = true
            val result = SuggestionRepository.submitBrandSuggestion(
                actorUserId = actorId,
                input = input
            )
            submitFeedbackMessage = when (result) {
                is SubmitBrandSuggestionResult.Success -> null
                is SubmitBrandSuggestionResult.Failure -> result.reason.name
            }
            isSubmitting = false
            onResult(result)
        }
    }

    fun loadMySuggestions() {
        val actorId = currentUserId
        if (actorId.isBlank()) {
            mySuggestions = emptyList()
            return
        }
        viewModelScope.launch {
            isLoadingMySuggestions = true
            mySuggestions = SuggestionRepository.loadUserSuggestions(actorId)
            isLoadingMySuggestions = false
        }
    }
}
