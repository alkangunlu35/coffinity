package com.icoffee.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.icoffee.app.data.admin.BrandEditDraft
import com.icoffee.app.data.admin.BrandManagementRepository
import com.icoffee.app.data.admin.BrandManagementResult
import com.icoffee.app.data.admin.BrandOwnershipResult
import com.icoffee.app.data.admin.BrandCreateDraft
import com.icoffee.app.data.admin.BrandManagementSession
import com.icoffee.app.data.admin.ProductEditDraft
import com.icoffee.app.data.auth.FirebaseAuthRepository
import com.icoffee.app.data.firebase.model.FirestoreBrand
import com.icoffee.app.data.firebase.model.FirestoreBrandSuggestion
import com.icoffee.app.data.firebase.model.FirestoreProduct
import com.icoffee.app.data.firebase.repository.FirestoreBrandsRepository
import com.icoffee.app.data.importer.BrandUrlImportRepository
import com.icoffee.app.data.model.importer.BrandImportPreviewResult
import com.icoffee.app.data.model.AppUserRole
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BrandAdminViewModel : ViewModel() {

    var refreshVersion by mutableIntStateOf(0)
        private set

    private val currentUserId: String
        get() = FirebaseAuthRepository.currentUser?.uid.orEmpty()

    private val brandUrlImportRepository = BrandUrlImportRepository()
    private var brandStreamJob: Job? = null

    private val _sessionState = MutableStateFlow<BrandManagementSession?>(null)
    val sessionState: StateFlow<BrandManagementSession?> = _sessionState.asStateFlow()

    private val _manageableBrandsState = MutableStateFlow<List<FirestoreBrand>>(emptyList())
    val manageableBrandsState: StateFlow<List<FirestoreBrand>> = _manageableBrandsState.asStateFlow()

    private val _isBrandStreamLoading = MutableStateFlow(true)
    val isBrandStreamLoading: StateFlow<Boolean> = _isBrandStreamLoading.asStateFlow()

    init {
        startRealtimeBrandSync()
    }

    private fun startRealtimeBrandSync() {
        brandStreamJob?.cancel()
        _isBrandStreamLoading.value = true
        brandStreamJob = viewModelScope.launch {
            BrandManagementRepository.observeSessionAndManageableBrandsForCurrentUser()
                .collect { state ->
                    _sessionState.value = state.session
                    _manageableBrandsState.value = state.brands
                    _isBrandStreamLoading.value = false
                }
        }
    }

    suspend fun session() = BrandManagementRepository.currentSession()

    suspend fun manageableBrands(): List<FirestoreBrand> {
        refreshVersion
        val fromStream = _manageableBrandsState.value
        if (fromStream.isNotEmpty()) return fromStream

        val session = BrandManagementRepository.currentSession() ?: return emptyList()
        return when (session.role) {
            AppUserRole.SUPER_ADMIN -> BrandManagementRepository.allBrandsForAdminPanel()
            AppUserRole.BRAND_ADMIN -> BrandManagementRepository.managedBrandsForAdminPanel(
                userId = session.userId,
                managedBrandIds = session.managedBrandIds
            )
            AppUserRole.USER -> emptyList()
        }.also {
            _sessionState.value = session
            _manageableBrandsState.value = it
            _isBrandStreamLoading.value = false
        }
    }

    suspend fun manageableBrandById(brandId: String): FirestoreBrand? {
        val actorId = currentUserId
        val normalizedBrandId = brandId.trim()
        if (actorId.isBlank() || normalizedBrandId.isBlank()) return null

        val canManage = BrandManagementRepository.canUserManageBrand(actorId, normalizedBrandId)
        if (!canManage) return null

        return FirestoreBrandsRepository.getById(normalizedBrandId).getOrNull()
    }

    suspend fun productsForBrand(brandId: String): List<FirestoreProduct> {
        refreshVersion
        val actorId = currentUserId
        if (actorId.isBlank()) return emptyList()
        return BrandManagementRepository.productsForManagedBrand(actorId, brandId)
    }

    suspend fun updateBrand(
        brandId: String,
        name: String,
        description: String,
        country: String,
        city: String,
        website: String,
        instagram: String,
        status: String?
    ): BrandManagementResult {
        val actorId = currentUserId
        if (actorId.isBlank()) {
            return BrandManagementResult.Failure(com.icoffee.app.data.admin.BrandManagementFailureReason.UNAUTHORIZED)
        }
        val result = BrandManagementRepository.updateBrand(
            actorUserId = actorId,
            draft = BrandEditDraft(
                brandId = brandId,
                name = name,
                description = description,
                country = country,
                city = city,
                website = website,
                instagram = instagram,
                status = status
            )
        )
        if (result is BrandManagementResult.Success) refreshVersion += 1
        return result
    }

    suspend fun addProduct(
        brandId: String,
        name: String,
        description: String,
        origin: String,
        roastLevel: String,
        process: String
    ): BrandManagementResult {
        val actorId = currentUserId
        if (actorId.isBlank()) {
            return BrandManagementResult.Failure(com.icoffee.app.data.admin.BrandManagementFailureReason.UNAUTHORIZED)
        }
        val result = BrandManagementRepository.createProduct(
            actorUserId = actorId,
            draft = ProductEditDraft(
                brandId = brandId,
                name = name,
                description = description.ifBlank { null },
                origin = origin.ifBlank { null },
                roastLevel = roastLevel.ifBlank { null },
                process = process.ifBlank { null }
            )
        )
        if (result is BrandManagementResult.Success) refreshVersion += 1
        return result
    }

    suspend fun removeProduct(productId: String): BrandManagementResult {
        val actorId = currentUserId
        if (actorId.isBlank()) {
            return BrandManagementResult.Failure(com.icoffee.app.data.admin.BrandManagementFailureReason.UNAUTHORIZED)
        }
        val result = BrandManagementRepository.deleteProduct(
            actorUserId = actorId,
            productId = productId
        )
        if (result is BrandManagementResult.Success) refreshVersion += 1
        return result
    }

    suspend fun removeBrand(brandId: String): BrandManagementResult {
        val actorId = currentUserId
        if (actorId.isBlank()) {
            return BrandManagementResult.Failure(com.icoffee.app.data.admin.BrandManagementFailureReason.UNAUTHORIZED)
        }
        val result = BrandManagementRepository.softDeleteBrand(
            actorUserId = actorId,
            brandId = brandId
        )
        if (result is BrandManagementResult.Success) refreshVersion += 1
        return result
    }

    suspend fun restoreBrand(brandId: String): BrandManagementResult {
        val actorId = currentUserId
        if (actorId.isBlank()) {
            return BrandManagementResult.Failure(com.icoffee.app.data.admin.BrandManagementFailureReason.UNAUTHORIZED)
        }
        val result = BrandManagementRepository.restoreSoftDeletedBrand(
            actorUserId = actorId,
            brandId = brandId
        )
        if (result is BrandManagementResult.Success) refreshVersion += 1
        return result
    }

    suspend fun assignBrandAdmin(
        brandId: String,
        ownerEmail: String
    ): BrandOwnershipResult {
        val actorId = currentUserId
        if (actorId.isBlank()) {
            return BrandOwnershipResult.Failure(com.icoffee.app.data.admin.BrandOwnershipFailureReason.UNAUTHORIZED)
        }
        val result = BrandManagementRepository.assignBrandAdmin(
            actorUserId = actorId,
            brandId = brandId,
            ownerEmail = ownerEmail
        )
        if (result is BrandOwnershipResult.Success) refreshVersion += 1
        return result
    }

    suspend fun revokeBrandOwnership(brandId: String): BrandOwnershipResult {
        val actorId = currentUserId
        if (actorId.isBlank()) {
            return BrandOwnershipResult.Failure(com.icoffee.app.data.admin.BrandOwnershipFailureReason.UNAUTHORIZED)
        }
        val result = BrandManagementRepository.revokeBrandOwnership(
            actorUserId = actorId,
            brandId = brandId
        )
        if (result is BrandOwnershipResult.Success) refreshVersion += 1
        return result
    }

    suspend fun importBrandFromUrl(url: String): BrandImportPreviewResult {
        return brandUrlImportRepository.importFromUrl(url)
    }

    suspend fun createBrand(
        name: String,
        description: String,
        country: String,
        city: String,
        website: String,
        instagram: String,
        logoUrl: String,
        coverImageUrl: String,
        sourceUrl: String,
        status: String
    ): Result<String> {
        val actorId = currentUserId
        if (actorId.isBlank()) return Result.failure(IllegalStateException("unauthorized"))
        val result = BrandManagementRepository.createBrand(
            actorUserId = actorId,
            draft = BrandCreateDraft(
                name = name,
                description = description,
                country = country,
                city = city,
                website = website.ifBlank { null },
                instagram = instagram.ifBlank { null },
                logoUrl = logoUrl.ifBlank { null },
                coverImageUrl = coverImageUrl.ifBlank { null },
                sourceUrl = sourceUrl.ifBlank { null },
                status = status
            )
        )
        if (result.isSuccess) refreshVersion += 1
        return result
    }

    suspend fun pendingBrandSuggestions(): List<FirestoreBrandSuggestion> {
        val actorId = currentUserId
        if (actorId.isBlank()) return emptyList()
        return BrandManagementRepository.listPendingBrandSuggestions(actorId)
    }

    suspend fun convertSuggestionToDraft(suggestionId: String): Result<String> {
        val actorId = currentUserId
        if (actorId.isBlank()) return Result.failure(IllegalStateException("unauthorized"))
        val result = BrandManagementRepository.convertSuggestionToBrand(
            actorUserId = actorId,
            suggestionId = suggestionId,
            publishAsActive = false
        )
        if (result.isSuccess) refreshVersion += 1
        return result
    }

    suspend fun convertSuggestionToActive(suggestionId: String): Result<String> {
        val actorId = currentUserId
        if (actorId.isBlank()) return Result.failure(IllegalStateException("unauthorized"))
        val result = BrandManagementRepository.convertSuggestionToBrand(
            actorUserId = actorId,
            suggestionId = suggestionId,
            publishAsActive = true
        )
        if (result.isSuccess) refreshVersion += 1
        return result
    }

    suspend fun rejectSuggestion(suggestionId: String): Result<Unit> {
        val actorId = currentUserId
        if (actorId.isBlank()) return Result.failure(IllegalStateException("unauthorized"))
        val result = BrandManagementRepository.updateSuggestionStatus(
            actorUserId = actorId,
            suggestionId = suggestionId,
            status = "rejected"
        )
        if (result.isSuccess) refreshVersion += 1
        return result
    }

    suspend fun mergeSuggestionWithBrand(
        suggestionId: String,
        existingBrandId: String
    ): Result<Unit> {
        val actorId = currentUserId
        if (actorId.isBlank()) return Result.failure(IllegalStateException("unauthorized"))
        val result = BrandManagementRepository.updateSuggestionStatus(
            actorUserId = actorId,
            suggestionId = suggestionId,
            status = "converted",
            convertedBrandId = existingBrandId
        )
        if (result.isSuccess) refreshVersion += 1
        return result
    }

    override fun onCleared() {
        brandStreamJob?.cancel()
        super.onCleared()
    }
}
