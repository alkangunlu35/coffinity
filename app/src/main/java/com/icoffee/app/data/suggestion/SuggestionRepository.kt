package com.icoffee.app.data.suggestion

import com.google.firebase.firestore.SetOptions
import com.icoffee.app.data.firebase.FirebaseServiceLocator
import com.icoffee.app.data.firebase.firestore.FirestoreCollections
import com.icoffee.app.data.firebase.model.FirestoreBrand
import com.icoffee.app.data.firebase.model.FirestoreBrandSuggestion
import com.icoffee.app.data.firebase.model.FirestoreSuggestionActionLog
import com.icoffee.app.data.firebase.repository.FirestoreBrandSuggestionsRepository
import com.icoffee.app.data.firebase.repository.FirestoreBrandsRepository
import com.icoffee.app.data.firebase.repository.FirestoreSuggestionActionLogsRepository
import com.icoffee.app.data.firebase.repository.FirestoreUsersRepository
import com.icoffee.app.data.model.AppUserRole
import com.icoffee.app.data.model.BrandLifecycleStatus
import com.icoffee.app.data.model.BrandSuggestionStatus
import com.icoffee.app.data.model.SuggestBrandInput
import com.icoffee.app.data.model.SuggestionActionType
import com.icoffee.app.util.BrandNormalization
import com.icoffee.app.util.UrlUtils
import java.util.UUID
import kotlinx.coroutines.tasks.await

enum class SuggestionSubmitFailureReason {
    UNAUTHORIZED,
    EMPTY_NAME,
    INVALID_URL,
    DUPLICATE_SUBMISSION,
    STORE_ERROR
}

sealed interface SubmitBrandSuggestionResult {
    data class Success(
        val suggestionId: String,
        val possibleDuplicate: Boolean
    ) : SubmitBrandSuggestionResult
    data class Failure(val reason: SuggestionSubmitFailureReason) : SubmitBrandSuggestionResult
}

enum class SuggestionAdminFailureReason {
    UNAUTHORIZED,
    NOT_FOUND,
    FINAL_STATE,
    INVALID_INPUT,
    TARGET_BRAND_NOT_FOUND,
    STORE_ERROR
}

sealed interface SuggestionAdminActionResult {
    data object Success : SuggestionAdminActionResult
    data class Failure(val reason: SuggestionAdminFailureReason) : SuggestionAdminActionResult
}

data class SuggestionApprovalDraft(
    val brandName: String,
    val description: String? = null,
    val websiteUrl: String? = null,
    val instagramUrl: String? = null,
    val country: String? = null,
    val city: String? = null,
    val status: String = BrandLifecycleStatus.DRAFT.storageValue
)

object SuggestionRepository {

    private const val DUPLICATE_CHECK_VERSION = 1
    private const val DUPLICATE_SPAM_WINDOW_MS = 6 * 60 * 60 * 1000L

    suspend fun submitBrandSuggestion(
        actorUserId: String,
        input: SuggestBrandInput
    ): SubmitBrandSuggestionResult {
        val user = FirestoreUsersRepository.getById(actorUserId.trim()).getOrNull()
            ?: return SubmitBrandSuggestionResult.Failure(SuggestionSubmitFailureReason.UNAUTHORIZED)

        val brandName = input.brandName.trim()
        if (brandName.isBlank()) {
            return SubmitBrandSuggestionResult.Failure(SuggestionSubmitFailureReason.EMPTY_NAME)
        }

        val websiteUrl = UrlUtils.normalizeHttpUrlOrNull(input.websiteUrl)
        if (!input.websiteUrl.isNullOrBlank() && websiteUrl == null) {
            return SubmitBrandSuggestionResult.Failure(SuggestionSubmitFailureReason.INVALID_URL)
        }

        val normalizedName = BrandNormalization.normalizeBrandName(brandName)
        val slugCandidate = BrandNormalization.slugify(brandName)
        val instagramHandle = UrlUtils.normalizeInstagramHandleOrNull(input.instagramUrl)
        val websiteHost = UrlUtils.extractWebsiteHost(websiteUrl)

        val existingUserSuggestions = FirestoreBrandSuggestionsRepository
            .listByUser(actorUserId, limit = 50)
            .getOrDefault(emptyList())

        val now = System.currentTimeMillis()
        val duplicateRecent = existingUserSuggestions.any { suggestion ->
            suggestion.normalizedBrandName == normalizedName &&
                now - suggestion.createdAt <= DUPLICATE_SPAM_WINDOW_MS
        }
        if (duplicateRecent) {
            return SubmitBrandSuggestionResult.Failure(SuggestionSubmitFailureReason.DUPLICATE_SUBMISSION)
        }

        val duplicateBrandIds = findDuplicateBrandCandidates(
            normalizedBrandName = normalizedName,
            slugCandidate = slugCandidate,
            websiteHost = websiteHost
        )

        val suggestionId = "brand_suggestion_${UUID.randomUUID().toString().replace("-", "")}"
        val description = input.description?.trim()?.takeIf { it.isNotBlank() }
        val suggestion = FirestoreBrandSuggestion(
            id = suggestionId,
            submittedByUserId = user.id,
            submittedByDisplayName = user.displayName.trim(),
            submittedByEmail = user.email.trim(),
            brandName = brandName,
            normalizedBrandName = normalizedName,
            slugCandidate = slugCandidate,
            websiteUrl = websiteUrl,
            instagramUrl = instagramHandle,
            country = input.country?.trim()?.takeIf { it.isNotBlank() },
            city = input.city?.trim()?.takeIf { it.isNotBlank() },
            description = description,
            sourceType = "manual_user_suggestion",
            status = BrandSuggestionStatus.PENDING.storageValue,
            duplicateCandidateBrandIds = duplicateBrandIds,
            duplicateCheckVersion = DUPLICATE_CHECK_VERSION,
            adminNotes = null,
            rejectionReason = null,
            resolvedByUserId = null,
            resolvedAt = null,
            createdBrandId = null,
            mergedIntoBrandId = null,
            lastActionType = SuggestionActionType.SUBMIT.storageValue,
            flagsPossibleDuplicate = duplicateBrandIds.isNotEmpty(),
            flagsHasWebsite = websiteUrl != null,
            flagsHasInstagram = instagramHandle != null,
            flagsLowQualityText = isLowQualitySuggestion(brandName, description),
            createdAt = now,
            updatedAt = now
        )

        val created = FirestoreBrandSuggestionsRepository.create(suggestion).isSuccess
        return if (created) {
            FirestoreSuggestionActionLogsRepository.create(
                FirestoreSuggestionActionLog(
                    id = "suggestion_action_${UUID.randomUUID().toString().replace("-", "")}",
                    suggestionId = suggestion.id,
                    actionType = SuggestionActionType.SUBMIT.storageValue,
                    actorUserId = user.id,
                    actorRole = AppUserRole.fromStorage(user.role).storageValue,
                    createdAt = now,
                    previousStatus = null,
                    nextStatus = suggestion.status,
                    notes = null,
                    rejectionReason = null,
                    createdBrandId = null,
                    mergedIntoBrandId = null,
                    snapshotBrandName = suggestion.brandName,
                    snapshotNormalizedBrandName = suggestion.normalizedBrandName,
                    snapshotWebsiteUrl = suggestion.websiteUrl,
                    snapshotCountry = suggestion.country,
                    snapshotCity = suggestion.city
                )
            )
            SubmitBrandSuggestionResult.Success(
                suggestionId = suggestionId,
                possibleDuplicate = duplicateBrandIds.isNotEmpty()
            )
        } else {
            SubmitBrandSuggestionResult.Failure(SuggestionSubmitFailureReason.STORE_ERROR)
        }
    }

    suspend fun loadUserSuggestions(userId: String): List<FirestoreBrandSuggestion> {
        return FirestoreBrandSuggestionsRepository
            .listByUser(userId.trim(), limit = 200)
            .getOrDefault(emptyList())
            .sortedByDescending { it.createdAt }
    }

    suspend fun loadAdminSuggestions(
        actorUserId: String,
        statusFilter: String?,
        query: String
    ): List<FirestoreBrandSuggestion> {
        if (!isSuperAdmin(actorUserId)) return emptyList()
        val all = FirestoreBrandSuggestionsRepository.list(limit = 600)
            .getOrDefault(emptyList())
        val normalizedStatus = statusFilter?.trim()?.lowercase().orEmpty()
        val normalizedQuery = query.trim().lowercase()
        return all.filter { suggestion ->
            val statusMatches = normalizedStatus.isBlank() || normalizedStatus == "all" ||
                suggestion.status == normalizedStatus
            val queryMatches = normalizedQuery.isBlank() ||
                suggestion.brandName.lowercase().contains(normalizedQuery) ||
                suggestion.websiteUrl.orEmpty().lowercase().contains(normalizedQuery) ||
                suggestion.city.orEmpty().lowercase().contains(normalizedQuery)
            statusMatches && queryMatches
        }.sortedByDescending { it.createdAt }
    }

    suspend fun getSuggestionById(
        actorUserId: String,
        suggestionId: String
    ): FirestoreBrandSuggestion? {
        val suggestion = FirestoreBrandSuggestionsRepository.getById(suggestionId).getOrNull() ?: return null
        if (isSuperAdmin(actorUserId) || suggestion.submittedByUserId == actorUserId.trim()) {
            return suggestion
        }
        return null
    }

    suspend fun loadActionLogs(
        actorUserId: String,
        suggestionId: String
    ): List<FirestoreSuggestionActionLog> {
        if (!isSuperAdmin(actorUserId)) return emptyList()
        return FirestoreSuggestionActionLogsRepository
            .listBySuggestionId(suggestionId, limit = 120)
            .getOrDefault(emptyList())
    }

    suspend fun markUnderReview(
        actorUserId: String,
        suggestionId: String,
        notes: String?
    ): SuggestionAdminActionResult {
        if (!isSuperAdmin(actorUserId)) {
            return SuggestionAdminActionResult.Failure(SuggestionAdminFailureReason.UNAUTHORIZED)
        }
        val suggestion = FirestoreBrandSuggestionsRepository.getById(suggestionId).getOrNull()
            ?: return SuggestionAdminActionResult.Failure(SuggestionAdminFailureReason.NOT_FOUND)
        val currentStatus = BrandSuggestionStatus.fromStorage(suggestion.status)
        if (currentStatus.isFinal) {
            return SuggestionAdminActionResult.Failure(SuggestionAdminFailureReason.FINAL_STATE)
        }

        return updateSuggestionWithLog(
            actorUserId = actorUserId,
            suggestion = suggestion,
            actionType = SuggestionActionType.MARK_UNDER_REVIEW,
            nextStatus = BrandSuggestionStatus.UNDER_REVIEW.storageValue,
            notes = notes,
            rejectionReason = null,
            createdBrandId = null,
            mergedIntoBrandId = null,
            patch = suggestion.copy(
                status = BrandSuggestionStatus.UNDER_REVIEW.storageValue,
                adminNotes = notes?.trim()?.takeIf { it.isNotBlank() },
                lastActionType = SuggestionActionType.MARK_UNDER_REVIEW.storageValue,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun approveAsNewBrand(
        actorUserId: String,
        suggestionId: String,
        draft: SuggestionApprovalDraft,
        notes: String?
    ): SuggestionAdminActionResult {
        if (!isSuperAdmin(actorUserId)) {
            return SuggestionAdminActionResult.Failure(SuggestionAdminFailureReason.UNAUTHORIZED)
        }

        val suggestion = FirestoreBrandSuggestionsRepository.getById(suggestionId).getOrNull()
            ?: return SuggestionAdminActionResult.Failure(SuggestionAdminFailureReason.NOT_FOUND)
        val currentStatus = BrandSuggestionStatus.fromStorage(suggestion.status)
        if (currentStatus.isFinal) {
            return SuggestionAdminActionResult.Failure(SuggestionAdminFailureReason.FINAL_STATE)
        }

        val name = draft.brandName.trim()
        if (name.isBlank()) {
            return SuggestionAdminActionResult.Failure(SuggestionAdminFailureReason.INVALID_INPUT)
        }

        val website = UrlUtils.normalizeHttpUrlOrNull(draft.websiteUrl)
        if (!draft.websiteUrl.isNullOrBlank() && website == null) {
            return SuggestionAdminActionResult.Failure(SuggestionAdminFailureReason.INVALID_INPUT)
        }
        val instagram = UrlUtils.normalizeInstagramHandleOrNull(draft.instagramUrl)

        val slug = resolveUniqueBrandSlug(BrandNormalization.slugify(name))
        val brandId = "brand_${UUID.randomUUID().toString().replace("-", "")}"
        val now = System.currentTimeMillis()
        val targetStatus = BrandLifecycleStatus.fromStorage(draft.status).storageValue

        val brandPayload = FirestoreBrand(
            id = brandId,
            name = name,
            slug = slug,
            description = draft.description?.trim().orEmpty(),
            country = draft.country?.trim().orEmpty(),
            city = draft.city?.trim().orEmpty(),
            logoUrl = null,
            coverImageUrl = null,
            website = website,
            instagram = instagram,
            sourceUrl = suggestion.websiteUrl,
            category = "specialty",
            status = targetStatus,
            source = "suggestion_approved",
            sourceSuggestionId = suggestion.id,
            mergedSuggestionIds = emptyList(),
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

        val nextSuggestion = suggestion.copy(
            status = BrandSuggestionStatus.APPROVED_NEW_BRAND.storageValue,
            adminNotes = notes?.trim()?.takeIf { it.isNotBlank() },
            rejectionReason = null,
            resolvedByUserId = actorUserId,
            resolvedAt = now,
            createdBrandId = brandId,
            mergedIntoBrandId = null,
            lastActionType = SuggestionActionType.APPROVE_NEW_BRAND.storageValue,
            updatedAt = now
        )

        return runCatching {
            val db = FirebaseServiceLocator.firestore
            db.runBatch { batch ->
                val brandRef = db.collection(FirestoreCollections.BRANDS).document(brandId)
                val suggestionRef = db.collection(FirestoreCollections.BRAND_SUGGESTIONS).document(suggestion.id)
                val logRef = db.collection(FirestoreCollections.SUGGESTION_ACTION_LOGS)
                    .document("suggestion_action_${UUID.randomUUID().toString().replace("-", "")}")
                batch.set(brandRef, brandPayload.toMap())
                batch.set(suggestionRef, nextSuggestion.toMap(), SetOptions.merge())
                batch.set(
                    logRef,
                    buildActionLog(
                        suggestion = suggestion,
                        actionType = SuggestionActionType.APPROVE_NEW_BRAND.storageValue,
                        actorUserId = actorUserId,
                        actorRole = AppUserRole.SUPER_ADMIN.storageValue,
                        previousStatus = suggestion.status,
                        nextStatus = nextSuggestion.status,
                        notes = notes,
                        rejectionReason = null,
                        createdBrandId = brandId,
                        mergedIntoBrandId = null
                    ).toMap()
                )
            }.await()
        }.fold(
            onSuccess = { SuggestionAdminActionResult.Success },
            onFailure = { SuggestionAdminActionResult.Failure(SuggestionAdminFailureReason.STORE_ERROR) }
        )
    }

    suspend fun mergeIntoExistingBrand(
        actorUserId: String,
        suggestionId: String,
        targetBrandId: String,
        notes: String?
    ): SuggestionAdminActionResult {
        if (!isSuperAdmin(actorUserId)) {
            return SuggestionAdminActionResult.Failure(SuggestionAdminFailureReason.UNAUTHORIZED)
        }
        val suggestion = FirestoreBrandSuggestionsRepository.getById(suggestionId).getOrNull()
            ?: return SuggestionAdminActionResult.Failure(SuggestionAdminFailureReason.NOT_FOUND)
        val currentStatus = BrandSuggestionStatus.fromStorage(suggestion.status)
        if (currentStatus.isFinal) {
            return SuggestionAdminActionResult.Failure(SuggestionAdminFailureReason.FINAL_STATE)
        }
        val brand = FirestoreBrandsRepository.getById(targetBrandId.trim()).getOrNull()
            ?: return SuggestionAdminActionResult.Failure(SuggestionAdminFailureReason.TARGET_BRAND_NOT_FOUND)

        val now = System.currentTimeMillis()
        val nextSuggestion = suggestion.copy(
            status = BrandSuggestionStatus.MERGED_EXISTING_BRAND.storageValue,
            adminNotes = notes?.trim()?.takeIf { it.isNotBlank() },
            rejectionReason = null,
            resolvedByUserId = actorUserId,
            resolvedAt = now,
            createdBrandId = null,
            mergedIntoBrandId = brand.id,
            lastActionType = SuggestionActionType.MERGE_EXISTING_BRAND.storageValue,
            updatedAt = now
        )
        val nextBrand = brand.copy(
            mergedSuggestionIds = (brand.mergedSuggestionIds + suggestion.id).distinct(),
            updatedAt = now
        )

        return runCatching {
            val db = FirebaseServiceLocator.firestore
            db.runBatch { batch ->
                val suggestionRef = db.collection(FirestoreCollections.BRAND_SUGGESTIONS).document(suggestion.id)
                val brandRef = db.collection(FirestoreCollections.BRANDS).document(brand.id)
                val logRef = db.collection(FirestoreCollections.SUGGESTION_ACTION_LOGS)
                    .document("suggestion_action_${UUID.randomUUID().toString().replace("-", "")}")
                batch.set(suggestionRef, nextSuggestion.toMap(), SetOptions.merge())
                batch.set(brandRef, nextBrand.toMap(), SetOptions.merge())
                batch.set(
                    logRef,
                    buildActionLog(
                        suggestion = suggestion,
                        actionType = SuggestionActionType.MERGE_EXISTING_BRAND.storageValue,
                        actorUserId = actorUserId,
                        actorRole = AppUserRole.SUPER_ADMIN.storageValue,
                        previousStatus = suggestion.status,
                        nextStatus = nextSuggestion.status,
                        notes = notes,
                        rejectionReason = null,
                        createdBrandId = null,
                        mergedIntoBrandId = brand.id
                    ).toMap()
                )
            }.await()
        }.fold(
            onSuccess = { SuggestionAdminActionResult.Success },
            onFailure = { SuggestionAdminActionResult.Failure(SuggestionAdminFailureReason.STORE_ERROR) }
        )
    }

    suspend fun rejectSuggestion(
        actorUserId: String,
        suggestionId: String,
        rejectionReason: String,
        notes: String?
    ): SuggestionAdminActionResult {
        if (!isSuperAdmin(actorUserId)) {
            return SuggestionAdminActionResult.Failure(SuggestionAdminFailureReason.UNAUTHORIZED)
        }
        val reason = rejectionReason.trim()
        if (reason.isBlank()) {
            return SuggestionAdminActionResult.Failure(SuggestionAdminFailureReason.INVALID_INPUT)
        }
        val suggestion = FirestoreBrandSuggestionsRepository.getById(suggestionId).getOrNull()
            ?: return SuggestionAdminActionResult.Failure(SuggestionAdminFailureReason.NOT_FOUND)
        val currentStatus = BrandSuggestionStatus.fromStorage(suggestion.status)
        if (currentStatus.isFinal) {
            return SuggestionAdminActionResult.Failure(SuggestionAdminFailureReason.FINAL_STATE)
        }
        val now = System.currentTimeMillis()
        val nextSuggestion = suggestion.copy(
            status = BrandSuggestionStatus.REJECTED.storageValue,
            adminNotes = notes?.trim()?.takeIf { it.isNotBlank() },
            rejectionReason = reason,
            resolvedByUserId = actorUserId,
            resolvedAt = now,
            createdBrandId = null,
            mergedIntoBrandId = null,
            lastActionType = SuggestionActionType.REJECT.storageValue,
            updatedAt = now
        )

        return updateSuggestionWithLog(
            actorUserId = actorUserId,
            suggestion = suggestion,
            actionType = SuggestionActionType.REJECT,
            nextStatus = nextSuggestion.status,
            notes = notes,
            rejectionReason = reason,
            createdBrandId = null,
            mergedIntoBrandId = null,
            patch = nextSuggestion
        )
    }

    private suspend fun updateSuggestionWithLog(
        actorUserId: String,
        suggestion: FirestoreBrandSuggestion,
        actionType: SuggestionActionType,
        nextStatus: String,
        notes: String?,
        rejectionReason: String?,
        createdBrandId: String?,
        mergedIntoBrandId: String?,
        patch: FirestoreBrandSuggestion
    ): SuggestionAdminActionResult {
        return runCatching {
            val db = FirebaseServiceLocator.firestore
            db.runBatch { batch ->
                val suggestionRef = db.collection(FirestoreCollections.BRAND_SUGGESTIONS).document(suggestion.id)
                val logRef = db.collection(FirestoreCollections.SUGGESTION_ACTION_LOGS)
                    .document("suggestion_action_${UUID.randomUUID().toString().replace("-", "")}")
                batch.set(suggestionRef, patch.toMap(), SetOptions.merge())
                batch.set(
                    logRef,
                    buildActionLog(
                        suggestion = suggestion,
                        actionType = actionType.storageValue,
                        actorUserId = actorUserId,
                        actorRole = AppUserRole.SUPER_ADMIN.storageValue,
                        previousStatus = suggestion.status,
                        nextStatus = nextStatus,
                        notes = notes,
                        rejectionReason = rejectionReason,
                        createdBrandId = createdBrandId,
                        mergedIntoBrandId = mergedIntoBrandId
                    ).toMap()
                )
            }.await()
        }.fold(
            onSuccess = { SuggestionAdminActionResult.Success },
            onFailure = { SuggestionAdminActionResult.Failure(SuggestionAdminFailureReason.STORE_ERROR) }
        )
    }

    private fun buildActionLog(
        suggestion: FirestoreBrandSuggestion,
        actionType: String,
        actorUserId: String,
        actorRole: String,
        previousStatus: String?,
        nextStatus: String?,
        notes: String?,
        rejectionReason: String?,
        createdBrandId: String?,
        mergedIntoBrandId: String?
    ): FirestoreSuggestionActionLog {
        return FirestoreSuggestionActionLog(
            id = "suggestion_action_${UUID.randomUUID().toString().replace("-", "")}",
            suggestionId = suggestion.id,
            actionType = actionType,
            actorUserId = actorUserId,
            actorRole = actorRole,
            createdAt = System.currentTimeMillis(),
            previousStatus = previousStatus,
            nextStatus = nextStatus,
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            rejectionReason = rejectionReason?.trim()?.takeIf { it.isNotBlank() },
            createdBrandId = createdBrandId,
            mergedIntoBrandId = mergedIntoBrandId,
            snapshotBrandName = suggestion.brandName,
            snapshotNormalizedBrandName = suggestion.normalizedBrandName,
            snapshotWebsiteUrl = suggestion.websiteUrl,
            snapshotCountry = suggestion.country,
            snapshotCity = suggestion.city
        )
    }

    private suspend fun resolveUniqueBrandSlug(baseSlug: String): String {
        val sanitizedBase = baseSlug.ifBlank { "brand" }
        var index = 1
        var candidate = sanitizedBase
        while (true) {
            val existing = FirestoreBrandsRepository.findBySlug(candidate).getOrNull()
            if (existing == null) return candidate
            index += 1
            candidate = "$sanitizedBase-$index"
        }
    }

    private suspend fun findDuplicateBrandCandidates(
        normalizedBrandName: String,
        slugCandidate: String,
        websiteHost: String?
    ): List<String> {
        val allBrands = FirestoreBrandsRepository.getAll(limit = 1200).getOrDefault(emptyList())
        return allBrands.filter { brand ->
            val byName = BrandNormalization.normalizeBrandName(brand.name) == normalizedBrandName
            val bySlug = brand.slug.trim().lowercase() == slugCandidate
            val byHost = websiteHost != null && UrlUtils.extractWebsiteHost(brand.website) == websiteHost
            byName || bySlug || byHost
        }.map { it.id }.distinct()
    }

    private suspend fun isSuperAdmin(userId: String): Boolean {
        val user = FirestoreUsersRepository.getById(userId.trim()).getOrNull() ?: return false
        return AppUserRole.fromStorage(user.role) == AppUserRole.SUPER_ADMIN
    }

    private fun isLowQualitySuggestion(name: String, description: String?): Boolean {
        val normalizedName = BrandNormalization.normalizeBrandName(name)
        val cleanDescription = description?.trim().orEmpty()
        return normalizedName.length < 3 || cleanDescription.length in 1..7
    }
}
