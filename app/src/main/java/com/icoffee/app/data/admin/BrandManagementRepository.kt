package com.icoffee.app.data.admin

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ListenerRegistration
import com.icoffee.app.data.auth.FirebaseAuthRepository
import com.icoffee.app.data.firebase.FirebaseServiceLocator
import com.icoffee.app.data.firebase.firestore.FirestoreCollections
import com.icoffee.app.data.firebase.model.FirestoreBrand
import com.icoffee.app.data.firebase.model.FirestoreBrandSuggestion
import com.icoffee.app.data.firebase.model.FirestoreProduct
import com.icoffee.app.data.firebase.model.FirestoreUser
import com.icoffee.app.data.firebase.model.toFirestoreBrand
import com.icoffee.app.data.firebase.model.toFirestoreUser
import com.icoffee.app.data.firebase.repository.FirestoreBrandsRepository
import com.icoffee.app.data.firebase.repository.FirestoreBrandSuggestionsRepository
import com.icoffee.app.data.firebase.repository.FirestoreProductsRepository
import com.icoffee.app.data.firebase.repository.FirestoreUsersRepository
import com.icoffee.app.data.importer.normalizeSlug
import com.icoffee.app.data.model.BrandSuggestionStatus
import com.icoffee.app.data.model.BrandLifecycleStatus
import com.icoffee.app.data.model.AppUserRole
import com.icoffee.app.data.suggestion.SubmitBrandSuggestionResult
import com.icoffee.app.data.suggestion.SuggestionSubmitFailureReason
import com.icoffee.app.data.suggestion.SuggestionAdminActionResult
import com.icoffee.app.data.suggestion.SuggestionApprovalDraft
import com.icoffee.app.data.suggestion.SuggestionRepository
import com.icoffee.app.data.model.SuggestBrandInput
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class BrandManagementSession(
    val userId: String,
    val role: AppUserRole,
    val managedBrandIds: Set<String>
) {
    val canAccessPanel: Boolean
        get() = role == AppUserRole.SUPER_ADMIN || role == AppUserRole.BRAND_ADMIN
}

data class BrandEditDraft(
    val brandId: String,
    val name: String,
    val description: String,
    val country: String,
    val city: String,
    val website: String? = null,
    val instagram: String? = null,
    val status: String? = null
)

data class BrandCreateDraft(
    val name: String,
    val description: String,
    val country: String,
    val city: String,
    val website: String? = null,
    val instagram: String? = null,
    val logoUrl: String? = null,
    val coverImageUrl: String? = null,
    val sourceUrl: String? = null,
    val status: String = BrandLifecycleStatus.ACTIVE.storageValue
)

data class ProductEditDraft(
    val brandId: String,
    val name: String,
    val description: String? = null,
    val origin: String? = null,
    val roastLevel: String? = null,
    val process: String? = null,
    val tastingNotes: List<String> = emptyList(),
    val imageUrl: String? = null,
    val barcode: String? = null,
    val sourceUrl: String? = null,
    val importedVia: String? = null
)

enum class BrandManagementFailureReason {
    UNAUTHORIZED,
    USER_NOT_FOUND,
    BRAND_NOT_FOUND,
    PRODUCT_NOT_FOUND,
    INVALID_INPUT,
    INVALID_STATUS,
    DUPLICATE,
    STORE_ERROR
}

sealed interface BrandManagementResult {
    data object Success : BrandManagementResult
    data class Failure(val reason: BrandManagementFailureReason) : BrandManagementResult
}

enum class BrandOwnershipFailureReason {
    UNAUTHORIZED,
    BRAND_NOT_FOUND,
    INVALID_EMAIL,
    USER_NOT_FOUND,
    ALREADY_ASSIGNED,
    STORE_ERROR
}

sealed interface BrandOwnershipResult {
    data object Success : BrandOwnershipResult
    data class Failure(val reason: BrandOwnershipFailureReason) : BrandOwnershipResult
}

data class BrandSuggestionDraft(
    val brandName: String,
    val website: String? = null,
    val instagram: String? = null,
    val country: String? = null,
    val city: String? = null,
    val note: String? = null
)

enum class BrandSuggestionFailureReason {
    UNAUTHORIZED,
    EMPTY_NAME,
    DUPLICATE_EXISTING,
    DUPLICATE_PENDING,
    STORE_ERROR
}

sealed interface BrandSuggestionSubmissionResult {
    data object Success : BrandSuggestionSubmissionResult
    data class Failure(val reason: BrandSuggestionFailureReason) : BrandSuggestionSubmissionResult
}

data class ImportedBrandResolution(
    val brandId: String,
    val createdDraftBrand: Boolean
)

data class BrandManagementRealtimeState(
    val session: BrandManagementSession?,
    val brands: List<FirestoreBrand>
)

object BrandManagementRepository {

    fun observeSessionAndManageableBrandsForCurrentUser(): Flow<BrandManagementRealtimeState> = callbackFlow {
        val authUser = FirebaseAuthRepository.currentUser
        if (authUser == null) {
            trySend(BrandManagementRealtimeState(session = null, brands = emptyList()))
            close()
            return@callbackFlow
        }

        val db = FirebaseServiceLocator.firestore
        val usersCollection = db.collection(FirestoreCollections.USERS)
        val brandsCollection = db.collection(FirestoreCollections.BRANDS)

        var brandListener: ListenerRegistration? = null

        fun startBrandsListener(session: BrandManagementSession) {
            brandListener?.remove()
            if (!session.canAccessPanel) {
                trySend(BrandManagementRealtimeState(session = session, brands = emptyList()))
                return
            }

            brandListener = brandsCollection
                .limit(500)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    val docs = snapshot?.documents.orEmpty()
                    val allBrands = docs
                        .mapNotNull { it.toFirestoreBrand() }
                        .map { it.copy(status = it.status.normalizeBrandStatus()) }
                        .distinctBy { it.id }

                    val manageableBrands = when (session.role) {
                        AppUserRole.SUPER_ADMIN -> allBrands
                        AppUserRole.BRAND_ADMIN -> {
                            val managedIds = session.managedBrandIds.toMutableSet()
                            allBrands
                                .filter { brand ->
                                    brand.ownerUserId == session.userId ||
                                        brand.managedByUserIds.any { it == session.userId }
                                }
                                .forEach { managedIds.add(it.id) }
                            if (managedIds.isEmpty()) {
                                emptyList()
                            } else {
                                allBrands.filter { it.id in managedIds }
                            }
                        }

                        AppUserRole.USER -> emptyList()
                    }.sortedBy { it.name.lowercase() }

                    trySend(
                        BrandManagementRealtimeState(
                            session = session,
                            brands = manageableBrands
                        )
                    )
                }
        }

        val userListener = usersCollection.document(authUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val firestoreUser = snapshot?.toFirestoreUser()
                if (firestoreUser == null) {
                    brandListener?.remove()
                    brandListener = null
                    trySend(BrandManagementRealtimeState(session = null, brands = emptyList()))
                    return@addSnapshotListener
                }
                val session = firestoreUser.toSession()
                startBrandsListener(session)
            }

        awaitClose {
            brandListener?.remove()
            userListener.remove()
        }
    }

    suspend fun currentSession(): BrandManagementSession? {
        val authUser = FirebaseAuthRepository.currentUser ?: return null
        val firestoreUser = FirestoreUsersRepository.getById(authUser.uid).getOrNull() ?: return null
        return firestoreUser.toSession()
    }

    suspend fun canCurrentUserManageBrand(brandId: String): Boolean {
        val authUser = FirebaseAuthRepository.currentUser ?: return false
        return canUserManageBrand(authUser.uid, brandId)
    }

    suspend fun canUserManageBrand(
        userId: String,
        brandId: String
    ): Boolean {
        val normalizedUserId = userId.trim()
        val normalizedBrandId = brandId.trim()
        if (normalizedUserId.isBlank() || normalizedBrandId.isBlank()) return false

        val user = FirestoreUsersRepository.getById(normalizedUserId).getOrNull() ?: return false
        val role = AppUserRole.fromStorage(user.role)
        if (role == AppUserRole.SUPER_ADMIN) return true
        if (role != AppUserRole.BRAND_ADMIN) return false

        if (user.managedBrandIds.any { it.equals(normalizedBrandId, ignoreCase = true) }) {
            return true
        }

        val brand = FirestoreBrandsRepository.getById(normalizedBrandId).getOrNull() ?: return false
        return brand.ownerUserId == normalizedUserId || brand.managedByUserIds.any { it == normalizedUserId }
    }

    suspend fun submitBrandSuggestion(
        actorUserId: String,
        draft: BrandSuggestionDraft
    ): BrandSuggestionSubmissionResult {
        val result = SuggestionRepository.submitBrandSuggestion(
            actorUserId = actorUserId,
            input = SuggestBrandInput(
                brandName = draft.brandName,
                websiteUrl = draft.website,
                instagramUrl = draft.instagram,
                country = draft.country,
                city = draft.city,
                description = draft.note
            )
        )
        return when (result) {
            is SubmitBrandSuggestionResult.Success -> BrandSuggestionSubmissionResult.Success
            is SubmitBrandSuggestionResult.Failure -> {
                when (result.reason) {
                    SuggestionSubmitFailureReason.UNAUTHORIZED ->
                        BrandSuggestionSubmissionResult.Failure(BrandSuggestionFailureReason.UNAUTHORIZED)
                    SuggestionSubmitFailureReason.EMPTY_NAME ->
                        BrandSuggestionSubmissionResult.Failure(BrandSuggestionFailureReason.EMPTY_NAME)
                    SuggestionSubmitFailureReason.INVALID_URL ->
                        BrandSuggestionSubmissionResult.Failure(BrandSuggestionFailureReason.STORE_ERROR)
                    SuggestionSubmitFailureReason.DUPLICATE_SUBMISSION ->
                        BrandSuggestionSubmissionResult.Failure(BrandSuggestionFailureReason.DUPLICATE_PENDING)
                    SuggestionSubmitFailureReason.STORE_ERROR ->
                        BrandSuggestionSubmissionResult.Failure(BrandSuggestionFailureReason.STORE_ERROR)
                }
            }
        }
    }

    suspend fun listPendingBrandSuggestions(
        actorUserId: String
    ): List<FirestoreBrandSuggestion> {
        return SuggestionRepository.loadAdminSuggestions(
            actorUserId = actorUserId,
            statusFilter = BrandSuggestionStatus.PENDING.storageValue,
            query = ""
        )
    }

    suspend fun createBrand(
        actorUserId: String,
        draft: BrandCreateDraft
    ): Result<String> = runCatching {
        val actor = FirestoreUsersRepository.getById(actorUserId.trim()).getOrNull()
            ?: error("unauthorized")
        if (AppUserRole.fromStorage(actor.role) != AppUserRole.SUPER_ADMIN) {
            error("unauthorized")
        }
        val normalizedName = draft.name.trim()
        require(normalizedName.isNotBlank()) { "invalid_name" }

        val lifecycle = BrandLifecycleStatus.fromStorage(draft.status).storageValue
        val duplicate = findLikelyBrandDuplicate(
            name = normalizedName,
            website = draft.website,
            instagram = draft.instagram,
            sourceUrl = draft.sourceUrl
        )
        if (duplicate != null) {
            throw IllegalStateException("duplicate:${duplicate.id}")
        }

        val brandId = "brand_${UUID.randomUUID().toString().replace("-", "")}"
        val now = System.currentTimeMillis()
        val payload = FirestoreBrand(
            id = brandId,
            name = normalizedName,
            slug = normalizeSlug(normalizedName),
            description = draft.description.trim(),
            country = draft.country.trim(),
            city = draft.city.trim(),
            logoUrl = draft.logoUrl.normalizeUrlOrNull(),
            coverImageUrl = draft.coverImageUrl.normalizeUrlOrNull(),
            website = draft.website.normalizeUrlOrNull(),
            instagram = draft.instagram.normalizeInstagramOrNull(),
            sourceUrl = draft.sourceUrl.normalizeUrlOrNull(),
            category = "specialty",
            status = lifecycle,
            ownerUserId = null,
            ownerEmail = null,
            managedByUserIds = emptyList(),
            verified = false,
            featured = false,
            averageRating = 0.0,
            reviewCount = 0,
            productCount = 0,
            createdAt = now,
            updatedAt = now
        )
        FirestoreBrandsRepository.create(payload).getOrThrow()
        brandId
    }

    suspend fun updateSuggestionStatus(
        actorUserId: String,
        suggestionId: String,
        status: String,
        convertedBrandId: String? = null
    ): Result<Unit> = runCatching {
        when (status.trim().lowercase()) {
            BrandSuggestionStatus.UNDER_REVIEW.storageValue -> {
                val result = SuggestionRepository.markUnderReview(
                    actorUserId = actorUserId,
                    suggestionId = suggestionId,
                    notes = null
                )
                require(result is SuggestionAdminActionResult.Success) { "update_failed" }
            }
            BrandSuggestionStatus.REJECTED.storageValue -> {
                val result = SuggestionRepository.rejectSuggestion(
                    actorUserId = actorUserId,
                    suggestionId = suggestionId,
                    rejectionReason = "Rejected",
                    notes = null
                )
                require(result is SuggestionAdminActionResult.Success) { "update_failed" }
            }
            "converted", BrandSuggestionStatus.MERGED_EXISTING_BRAND.storageValue -> {
                val target = convertedBrandId?.trim()?.takeIf { it.isNotBlank() } ?: error("invalid_target")
                val result = SuggestionRepository.mergeIntoExistingBrand(
                    actorUserId = actorUserId,
                    suggestionId = suggestionId,
                    targetBrandId = target,
                    notes = null
                )
                require(result is SuggestionAdminActionResult.Success) { "update_failed" }
            }
            else -> error("invalid_status")
        }
    }

    suspend fun convertSuggestionToBrand(
        actorUserId: String,
        suggestionId: String,
        publishAsActive: Boolean
    ): Result<String> = runCatching {
        val suggestion = FirestoreBrandSuggestionsRepository.getById(suggestionId).getOrNull()
            ?: error("suggestion_not_found")
        val status = if (publishAsActive) {
            BrandLifecycleStatus.ACTIVE.storageValue
        } else {
            BrandLifecycleStatus.DRAFT.storageValue
        }
        val result = SuggestionRepository.approveAsNewBrand(
            actorUserId = actorUserId,
            suggestionId = suggestionId,
            draft = SuggestionApprovalDraft(
                brandName = suggestion.brandName,
                description = suggestion.description,
                websiteUrl = suggestion.websiteUrl,
                instagramUrl = suggestion.instagramUrl,
                country = suggestion.country,
                city = suggestion.city,
                status = status
            ),
            notes = null
        )
        require(result is SuggestionAdminActionResult.Success) { "approve_failed" }
        val refreshed = FirestoreBrandSuggestionsRepository.getById(suggestionId).getOrNull()
            ?: error("suggestion_not_found")
        refreshed.createdBrandId ?: error("created_brand_missing")
    }

    suspend fun createOrReuseDraftBrandForImportedProduct(
        actorUserId: String,
        detectedBrandName: String,
        sourceUrl: String?
    ): Result<ImportedBrandResolution> = runCatching {
        val actor = FirestoreUsersRepository.getById(actorUserId.trim()).getOrNull()
            ?: error("unauthorized")
        if (AppUserRole.fromStorage(actor.role) != AppUserRole.SUPER_ADMIN) {
            error("unauthorized")
        }

        val normalizedName = detectedBrandName.trim()
        require(normalizedName.isNotBlank()) { "invalid_name" }
        val duplicate = findLikelyBrandDuplicate(
            name = normalizedName,
            website = null,
            instagram = null,
            sourceUrl = sourceUrl
        )
        if (duplicate != null) {
            return@runCatching ImportedBrandResolution(
                brandId = duplicate.id,
                createdDraftBrand = false
            )
        }

        val createdId = createBrand(
            actorUserId = actorUserId,
            draft = BrandCreateDraft(
                name = normalizedName,
                description = "",
                country = "",
                city = "",
                sourceUrl = sourceUrl,
                status = BrandLifecycleStatus.DRAFT.storageValue
            )
        ).getOrThrow()
        ImportedBrandResolution(
            brandId = createdId,
            createdDraftBrand = true
        )
    }

    suspend fun manageableBrandsForCurrentUser(): List<FirestoreBrand> {
        val authUser = FirebaseAuthRepository.currentUser ?: return emptyList()
        return manageableBrandsForUser(authUser)
    }

    suspend fun allBrandsForAdminPanel(): List<FirestoreBrand> {
        return loadAllBrandsForManagement()
            .sortedBy { it.name.lowercase() }
    }

    suspend fun managedBrandsForAdminPanel(
        userId: String,
        managedBrandIds: Collection<String>
    ): List<FirestoreBrand> {
        val normalizedUserId = userId.trim()
        if (normalizedUserId.isBlank()) return emptyList()

        val normalizedManagedIds = managedBrandIds
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableSet()

        val allBrands = loadAllBrandsForManagement()
        allBrands
            .filter { brand ->
                brand.ownerUserId == normalizedUserId || brand.managedByUserIds.any { it == normalizedUserId }
            }
            .forEach { normalizedManagedIds.add(it.id) }

        if (normalizedManagedIds.isEmpty()) return emptyList()
        return FirestoreBrandsRepository.listByIds(normalizedManagedIds.toList())
            .getOrDefault(emptyList())
            .sortedBy { it.name.lowercase() }
    }

    suspend fun updateBrand(
        actorUserId: String,
        draft: BrandEditDraft
    ): BrandManagementResult {
        val normalizedName = draft.name.trim()
        if (normalizedName.isBlank()) {
            return BrandManagementResult.Failure(BrandManagementFailureReason.INVALID_INPUT)
        }

        val canManage = canUserManageBrand(actorUserId, draft.brandId)
        if (!canManage) {
            return BrandManagementResult.Failure(BrandManagementFailureReason.UNAUTHORIZED)
        }
        val actor = FirestoreUsersRepository.getById(actorUserId.trim()).getOrNull()
            ?: return BrandManagementResult.Failure(BrandManagementFailureReason.UNAUTHORIZED)
        val actorRole = AppUserRole.fromStorage(actor.role)

        val currentBrand = FirestoreBrandsRepository.getById(draft.brandId).getOrNull()
            ?: return BrandManagementResult.Failure(BrandManagementFailureReason.BRAND_NOT_FOUND)

        val requestedStatus = if (actorRole == AppUserRole.SUPER_ADMIN) {
            draft.status?.trim()?.takeIf { it.isNotBlank() }
                ?.let { BrandLifecycleStatus.fromStorage(it).storageValue }
                ?: currentBrand.status.normalizeBrandStatus()
        } else {
            currentBrand.status.normalizeBrandStatus()
        }

        val updated = currentBrand.copy(
            name = normalizedName,
            slug = normalizeSlug(normalizedName),
            description = draft.description.trim(),
            country = draft.country.trim(),
            city = draft.city.trim(),
            website = draft.website.normalizeUrlOrNull(),
            instagram = draft.instagram.normalizeInstagramOrNull(),
            status = requestedStatus,
            updatedAt = System.currentTimeMillis()
        )

        return if (FirestoreBrandsRepository.update(updated).isSuccess) {
            BrandManagementResult.Success
        } else {
            BrandManagementResult.Failure(BrandManagementFailureReason.STORE_ERROR)
        }
    }

    suspend fun softDeleteBrand(
        actorUserId: String,
        brandId: String
    ): BrandManagementResult {
        val normalizedActorId = actorUserId.trim()
        val normalizedBrandId = brandId.trim()
        if (normalizedActorId.isBlank() || normalizedBrandId.isBlank()) {
            return BrandManagementResult.Failure(BrandManagementFailureReason.INVALID_INPUT)
        }

        val actor = FirestoreUsersRepository.getById(normalizedActorId).getOrNull()
            ?: return BrandManagementResult.Failure(BrandManagementFailureReason.UNAUTHORIZED)
        if (AppUserRole.fromStorage(actor.role) != AppUserRole.SUPER_ADMIN) {
            return BrandManagementResult.Failure(BrandManagementFailureReason.UNAUTHORIZED)
        }

        val currentBrand = FirestoreBrandsRepository.getById(normalizedBrandId).getOrNull()
            ?: return BrandManagementResult.Failure(BrandManagementFailureReason.BRAND_NOT_FOUND)
        if (currentBrand.isDeleted) {
            return BrandManagementResult.Success
        }

        val now = System.currentTimeMillis()
        val updated = currentBrand.copy(
            isDeleted = true,
            deletedAt = now,
            deletedByUserId = normalizedActorId,
            updatedAt = now
        )
        return if (FirestoreBrandsRepository.update(updated).isSuccess) {
            BrandManagementResult.Success
        } else {
            BrandManagementResult.Failure(BrandManagementFailureReason.STORE_ERROR)
        }
    }

    suspend fun restoreSoftDeletedBrand(
        actorUserId: String,
        brandId: String
    ): BrandManagementResult {
        val normalizedActorId = actorUserId.trim()
        val normalizedBrandId = brandId.trim()
        if (normalizedActorId.isBlank() || normalizedBrandId.isBlank()) {
            return BrandManagementResult.Failure(BrandManagementFailureReason.INVALID_INPUT)
        }

        val actor = FirestoreUsersRepository.getById(normalizedActorId).getOrNull()
            ?: return BrandManagementResult.Failure(BrandManagementFailureReason.UNAUTHORIZED)
        if (AppUserRole.fromStorage(actor.role) != AppUserRole.SUPER_ADMIN) {
            return BrandManagementResult.Failure(BrandManagementFailureReason.UNAUTHORIZED)
        }

        val currentBrand = FirestoreBrandsRepository.getById(normalizedBrandId).getOrNull()
            ?: return BrandManagementResult.Failure(BrandManagementFailureReason.BRAND_NOT_FOUND)

        if (!currentBrand.isDeleted) {
            return BrandManagementResult.Success
        }

        val updated = currentBrand.copy(
            isDeleted = false,
            deletedAt = null,
            deletedByUserId = null,
            updatedAt = System.currentTimeMillis()
        )
        return if (FirestoreBrandsRepository.update(updated).isSuccess) {
            BrandManagementResult.Success
        } else {
            BrandManagementResult.Failure(BrandManagementFailureReason.STORE_ERROR)
        }
    }

    suspend fun createProduct(
        actorUserId: String,
        draft: ProductEditDraft
    ): BrandManagementResult {
        val normalizedName = draft.name.trim()
        val normalizedBrandId = draft.brandId.trim()
        if (normalizedName.isBlank() || normalizedBrandId.isBlank()) {
            return BrandManagementResult.Failure(BrandManagementFailureReason.INVALID_INPUT)
        }

        val canManage = canUserManageBrand(actorUserId, normalizedBrandId)
        if (!canManage) {
            return BrandManagementResult.Failure(BrandManagementFailureReason.UNAUTHORIZED)
        }

        val brand = FirestoreBrandsRepository.getById(normalizedBrandId).getOrNull()
            ?: return BrandManagementResult.Failure(BrandManagementFailureReason.BRAND_NOT_FOUND)

        val slug = normalizeSlug(normalizedName)
        val duplicate = FirestoreProductsRepository.findByBrandAndSlug(normalizedBrandId, slug).getOrNull()
        if (duplicate != null) {
            return BrandManagementResult.Failure(BrandManagementFailureReason.DUPLICATE)
        }

        val now = System.currentTimeMillis()
        val productId = "product_${UUID.randomUUID().toString().replace("-", "")}".lowercase()
        val payload = FirestoreProduct(
            id = productId,
            brandId = normalizedBrandId,
            name = normalizedName,
            slug = slug,
            description = draft.description?.trim().orEmpty(),
            imageUrl = draft.imageUrl?.trim()?.takeIf { it.isNotBlank() },
            barcode = draft.barcode?.trim()?.takeIf { it.isNotBlank() },
            origin = draft.origin?.trim().orEmpty(),
            roastLevel = draft.roastLevel?.trim().orEmpty(),
            process = draft.process?.trim().orEmpty(),
            tastingNotes = draft.tastingNotes
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct(),
            sourceUrl = draft.sourceUrl?.trim()?.takeIf { it.isNotBlank() },
            importedVia = draft.importedVia?.trim()?.takeIf { it.isNotBlank() },
            averageRating = 0.0,
            reviewCount = 0,
            createdAt = now,
            updatedAt = now
        )

        if (FirestoreProductsRepository.create(payload).isFailure) {
            return BrandManagementResult.Failure(BrandManagementFailureReason.STORE_ERROR)
        }

        val nextCount = (brand.productCount + 1).coerceAtLeast(1)
        FirestoreBrandsRepository.update(
            brand.copy(productCount = nextCount, updatedAt = System.currentTimeMillis())
        )
        return BrandManagementResult.Success
    }

    suspend fun deleteProduct(
        actorUserId: String,
        productId: String
    ): BrandManagementResult {
        val normalizedProductId = productId.trim()
        if (normalizedProductId.isBlank()) {
            return BrandManagementResult.Failure(BrandManagementFailureReason.INVALID_INPUT)
        }

        val product = FirestoreProductsRepository.getById(normalizedProductId).getOrNull()
            ?: return BrandManagementResult.Failure(BrandManagementFailureReason.PRODUCT_NOT_FOUND)

        val canManage = canUserManageBrand(actorUserId, product.brandId)
        if (!canManage) {
            return BrandManagementResult.Failure(BrandManagementFailureReason.UNAUTHORIZED)
        }

        if (FirestoreProductsRepository.deleteById(normalizedProductId).isFailure) {
            return BrandManagementResult.Failure(BrandManagementFailureReason.STORE_ERROR)
        }

        val brand = FirestoreBrandsRepository.getById(product.brandId).getOrNull()
        if (brand != null) {
            FirestoreBrandsRepository.update(
                brand.copy(
                    productCount = (brand.productCount - 1).coerceAtLeast(0),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        return BrandManagementResult.Success
    }

    suspend fun assignBrandAdmin(
        actorUserId: String,
        brandId: String,
        ownerEmail: String
    ): BrandOwnershipResult {
        val actor = FirestoreUsersRepository.getById(actorUserId.trim()).getOrNull()
            ?: return BrandOwnershipResult.Failure(BrandOwnershipFailureReason.UNAUTHORIZED)
        if (AppUserRole.fromStorage(actor.role) != AppUserRole.SUPER_ADMIN) {
            return BrandOwnershipResult.Failure(BrandOwnershipFailureReason.UNAUTHORIZED)
        }

        val normalizedBrandId = brandId.trim()
        if (normalizedBrandId.isBlank()) {
            return BrandOwnershipResult.Failure(BrandOwnershipFailureReason.BRAND_NOT_FOUND)
        }

        val normalizedEmail = ownerEmail.trim().lowercase()
        if (normalizedEmail.isBlank()) {
            return BrandOwnershipResult.Failure(BrandOwnershipFailureReason.INVALID_EMAIL)
        }

        val targetUser = FirestoreUsersRepository.getByEmail(normalizedEmail).getOrNull()
            ?: return BrandOwnershipResult.Failure(BrandOwnershipFailureReason.USER_NOT_FOUND)

        val brand = FirestoreBrandsRepository.getById(normalizedBrandId).getOrNull()
            ?: return BrandOwnershipResult.Failure(BrandOwnershipFailureReason.BRAND_NOT_FOUND)

        if (brand.ownerUserId == targetUser.id &&
            brand.managedByUserIds.any { it == targetUser.id } &&
            targetUser.managedBrandIds.any { it == normalizedBrandId }
        ) {
            return BrandOwnershipResult.Failure(BrandOwnershipFailureReason.ALREADY_ASSIGNED)
        }

        val now = System.currentTimeMillis()
        val oldOwnerId = brand.ownerUserId?.trim()?.takeIf { it.isNotBlank() }
        val oldOwner = oldOwnerId
            ?.takeIf { it != targetUser.id }
            ?.let { FirestoreUsersRepository.getById(it).getOrNull() }

        val targetRole = AppUserRole.fromStorage(targetUser.role)
        val updatedTarget = targetUser.copy(
            role = if (targetRole == AppUserRole.SUPER_ADMIN) {
                AppUserRole.SUPER_ADMIN.storageValue
            } else {
                AppUserRole.BRAND_ADMIN.storageValue
            },
            managedBrandIds = (targetUser.managedBrandIds + normalizedBrandId).distinct(),
            updatedAt = now
        )

        val updatedBrand = brand.copy(
            ownerUserId = targetUser.id,
            ownerEmail = targetUser.email.ifBlank { normalizedEmail },
            managedByUserIds = (brand.managedByUserIds - (oldOwnerId ?: "") + targetUser.id).distinct(),
            status = if (brand.status.normalizeBrandStatus() == BrandLifecycleStatus.BUSINESS.storageValue) {
                BrandLifecycleStatus.BUSINESS.storageValue
            } else {
                BrandLifecycleStatus.CLAIMED.storageValue
            },
            updatedAt = now
        )

        val updatedOldOwner = oldOwner?.let { owner ->
            val remaining = owner.managedBrandIds.filterNot { it == normalizedBrandId }
            val oldRole = AppUserRole.fromStorage(owner.role)
            owner.copy(
                role = if (oldRole == AppUserRole.SUPER_ADMIN) {
                    AppUserRole.SUPER_ADMIN.storageValue
                } else if (remaining.isEmpty()) {
                    AppUserRole.USER.storageValue
                } else {
                    AppUserRole.BRAND_ADMIN.storageValue
                },
                managedBrandIds = remaining,
                updatedAt = now
            )
        }

        return runCatching {
            val db = FirebaseServiceLocator.firestore
            db.runBatch { batch ->
                val brandRef = db.collection(FirestoreCollections.BRANDS).document(updatedBrand.id)
                val targetRef = db.collection(FirestoreCollections.USERS).document(updatedTarget.id)
                batch.set(brandRef, updatedBrand.toMap())
                batch.set(targetRef, updatedTarget.toMap())
                if (updatedOldOwner != null) {
                    val oldRef = db.collection(FirestoreCollections.USERS).document(updatedOldOwner.id)
                    batch.set(oldRef, updatedOldOwner.toMap())
                }
            }.await()
        }.fold(
            onSuccess = { BrandOwnershipResult.Success },
            onFailure = { BrandOwnershipResult.Failure(BrandOwnershipFailureReason.STORE_ERROR) }
        )
    }

    suspend fun revokeBrandOwnership(
        actorUserId: String,
        brandId: String
    ): BrandOwnershipResult {
        val actor = FirestoreUsersRepository.getById(actorUserId.trim()).getOrNull()
            ?: return BrandOwnershipResult.Failure(BrandOwnershipFailureReason.UNAUTHORIZED)
        if (AppUserRole.fromStorage(actor.role) != AppUserRole.SUPER_ADMIN) {
            return BrandOwnershipResult.Failure(BrandOwnershipFailureReason.UNAUTHORIZED)
        }

        val normalizedBrandId = brandId.trim()
        if (normalizedBrandId.isBlank()) {
            return BrandOwnershipResult.Failure(BrandOwnershipFailureReason.BRAND_NOT_FOUND)
        }

        val brand = FirestoreBrandsRepository.getById(normalizedBrandId).getOrNull()
            ?: return BrandOwnershipResult.Failure(BrandOwnershipFailureReason.BRAND_NOT_FOUND)

        val ownerId = brand.ownerUserId?.trim()?.takeIf { it.isNotBlank() }
        val owner = ownerId?.let { FirestoreUsersRepository.getById(it).getOrNull() }
        val now = System.currentTimeMillis()

        val updatedBrand = brand.copy(
            ownerUserId = null,
            ownerEmail = null,
            managedByUserIds = ownerId?.let { id -> brand.managedByUserIds.filterNot { it == id } }
                ?: brand.managedByUserIds,
            status = BrandLifecycleStatus.ACTIVE.storageValue,
            updatedAt = now
        )

        val updatedOwner = owner?.let {
            val remaining = it.managedBrandIds.filterNot { managed -> managed == normalizedBrandId }
            val role = AppUserRole.fromStorage(it.role)
            it.copy(
                role = if (role == AppUserRole.SUPER_ADMIN) {
                    AppUserRole.SUPER_ADMIN.storageValue
                } else if (remaining.isEmpty()) {
                    AppUserRole.USER.storageValue
                } else {
                    AppUserRole.BRAND_ADMIN.storageValue
                },
                managedBrandIds = remaining,
                updatedAt = now
            )
        }

        return runCatching {
            val db = FirebaseServiceLocator.firestore
            db.runBatch { batch ->
                val brandRef = db.collection(FirestoreCollections.BRANDS).document(updatedBrand.id)
                batch.set(brandRef, updatedBrand.toMap())
                if (updatedOwner != null) {
                    val ownerRef = db.collection(FirestoreCollections.USERS).document(updatedOwner.id)
                    batch.set(ownerRef, updatedOwner.toMap())
                }
            }.await()
        }.fold(
            onSuccess = { BrandOwnershipResult.Success },
            onFailure = { BrandOwnershipResult.Failure(BrandOwnershipFailureReason.STORE_ERROR) }
        )
    }

    suspend fun productsForManagedBrand(
        actorUserId: String,
        brandId: String
    ): List<FirestoreProduct> {
        if (!canUserManageBrand(actorUserId, brandId)) return emptyList()
        return FirestoreProductsRepository.listByBrand(brandId).getOrDefault(emptyList())
            .sortedByDescending { it.updatedAt }
    }

    private suspend fun manageableBrandsForUser(authUser: FirebaseUser): List<FirestoreBrand> {
        val user = FirestoreUsersRepository.getById(authUser.uid).getOrNull() ?: return emptyList()
        val role = AppUserRole.fromStorage(user.role)

        return when (role) {
            AppUserRole.SUPER_ADMIN -> {
                allBrandsForAdminPanel()
            }

            AppUserRole.BRAND_ADMIN -> {
                managedBrandsForAdminPanel(
                    userId = user.id,
                    managedBrandIds = user.managedBrandIds
                )
            }

            AppUserRole.USER -> emptyList()
        }
    }

    private suspend fun loadAllBrandsForManagement(): List<FirestoreBrand> {
        val ordered = FirestoreBrandsRepository.list(limit = 500).getOrDefault(emptyList())
        val unfiltered = FirestoreBrandsRepository.getAll(limit = 500).getOrDefault(emptyList())
        // Merge ordered + unfiltered to include docs missing updatedAt.
        return (ordered + unfiltered)
            .map { it.copy(status = it.status.normalizeBrandStatus()) }
            .distinctBy { it.id }
    }

    private fun FirestoreUser.toSession(): BrandManagementSession {
        return BrandManagementSession(
            userId = id,
            role = AppUserRole.fromStorage(role),
            managedBrandIds = managedBrandIds.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        )
    }

    private suspend fun findLikelyBrandDuplicate(
        name: String,
        website: String?,
        instagram: String?,
        sourceUrl: String?
    ): FirestoreBrand? {
        val normalizedSlug = normalizeSlug(name)
        val normalizedDomain = website.normalizeDomainOrNull()
        val normalizedInstagram = instagram.normalizeInstagramOrNull()
        val normalizedSourceDomain = sourceUrl.normalizeDomainOrNull()

        val all = loadAllBrandsForManagement()
        return all.firstOrNull { existing ->
            val sameSlug = existing.slug.equals(normalizedSlug, ignoreCase = true) ||
                normalizeSlug(existing.name) == normalizedSlug
            val sameWebsiteDomain = normalizedDomain != null &&
                existing.website.normalizeDomainOrNull() == normalizedDomain
            val sameInstagram = normalizedInstagram != null &&
                existing.instagram.normalizeInstagramOrNull() == normalizedInstagram
            val sameSourceDomain = normalizedSourceDomain != null &&
                existing.sourceUrl.normalizeDomainOrNull() == normalizedSourceDomain
            sameSlug || sameWebsiteDomain || sameInstagram || sameSourceDomain
        }
    }

    private fun String?.normalizeBrandStatus(): String {
        return BrandLifecycleStatus.fromStorage(this).storageValue
    }

    private fun String?.normalizeTextOrNull(): String? {
        return this?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun String?.normalizeUrlOrNull(): String? {
        val raw = this?.trim().orEmpty()
        if (raw.isBlank()) return null
        val candidate = if (raw.startsWith("http://", ignoreCase = true) ||
            raw.startsWith("https://", ignoreCase = true)
        ) {
            raw
        } else {
            "https://$raw"
        }
        val uri = runCatching { URI(candidate) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase().orEmpty()
        val host = uri.host?.lowercase().orEmpty()
        if (scheme !in setOf("http", "https") || host.isBlank()) return null
        return uri.toString()
    }

    private fun String?.normalizeDomainOrNull(): String? {
        val url = this.normalizeUrlOrNull() ?: return null
        val host = runCatching { URI(url).host }.getOrNull()?.trim()?.lowercase().orEmpty()
        if (host.isBlank()) return null
        return host.removePrefix("www.")
    }

    private fun String?.normalizeInstagramOrNull(): String? {
        val raw = this?.trim().orEmpty()
        if (raw.isBlank()) return null
        val lowered = raw.lowercase()
        val compact = lowered
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .removePrefix("instagram.com/")
            .removePrefix("@")
            .trim()
            .trim('/')
        return compact.takeIf { it.isNotBlank() }?.let { "@$it" }
    }
}
