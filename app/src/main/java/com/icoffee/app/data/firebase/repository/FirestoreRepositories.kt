package com.icoffee.app.data.firebase.repository

import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.icoffee.app.data.firebase.FirebaseServiceLocator
import com.icoffee.app.data.firebase.firestore.FirestoreCollections
import com.icoffee.app.data.firebase.model.FirestoreBrand
import com.icoffee.app.data.firebase.model.FirestoreBrandSuggestion
import com.icoffee.app.data.firebase.model.FirestoreClaimRequest
import com.icoffee.app.data.firebase.model.FirestoreEvent
import com.icoffee.app.data.firebase.model.FirestoreProduct
import com.icoffee.app.data.firebase.model.FirestoreReview
import com.icoffee.app.data.firebase.model.FirestoreSuggestionActionLog
import com.icoffee.app.data.firebase.model.FirestoreUser
import com.icoffee.app.data.firebase.model.toFirestoreBrand
import com.icoffee.app.data.firebase.model.toFirestoreBrandSuggestion
import com.icoffee.app.data.firebase.model.toFirestoreClaimRequest
import com.icoffee.app.data.firebase.model.toFirestoreEvent
import com.icoffee.app.data.firebase.model.toFirestoreProduct
import com.icoffee.app.data.firebase.model.toFirestoreReview
import com.icoffee.app.data.firebase.model.toFirestoreSuggestionActionLog
import com.icoffee.app.data.firebase.model.toFirestoreUser
import java.util.Locale
import kotlinx.coroutines.tasks.await

private const val DEFAULT_LIST_LIMIT = 100L

object FirestoreUsersRepository {
    private val collection
        get() = FirebaseServiceLocator.firestore.collection(FirestoreCollections.USERS)

    suspend fun create(user: FirestoreUser): Result<Unit> = runCatching {
        require(user.id.isNotBlank()) { "User id is required." }
        val now = System.currentTimeMillis()
        val payload = user.copy(
            createdAt = user.createdAt.takeIf { it > 0L } ?: now,
            updatedAt = now
        )
        collection.document(payload.id).set(payload.toMap()).await()
    }

    suspend fun getById(id: String): Result<FirestoreUser?> = runCatching {
        if (id.isBlank()) return@runCatching null
        collection.document(id).get().await().toFirestoreUser()
    }

    suspend fun list(limit: Long = DEFAULT_LIST_LIMIT): Result<List<FirestoreUser>> = runCatching {
        collection.limit(limit).get().await().documents.mapNotNull { it.toFirestoreUser() }
    }

    suspend fun listByIds(ids: List<String>): Result<List<FirestoreUser>> = runCatching {
        val normalizedIds = ids.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (normalizedIds.isEmpty()) return@runCatching emptyList()
        normalizedIds.mapNotNull { id ->
            collection.document(id).get().await().toFirestoreUser()
        }
    }

    suspend fun getByEmail(email: String): Result<FirestoreUser?> = runCatching {
        val rawEmail = email.trim()
        val normalizedEmail = rawEmail.lowercase()
        if (normalizedEmail.isBlank()) return@runCatching null

        collection
            .whereEqualTo("email", normalizedEmail)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toFirestoreUser()
            ?: if (rawEmail == normalizedEmail) {
                null
            } else {
                collection
                    .whereEqualTo("email", rawEmail)
                    .limit(1)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
                    ?.toFirestoreUser()
            }
    }

    suspend fun update(user: FirestoreUser): Result<Unit> = runCatching {
        require(user.id.isNotBlank()) { "User id is required." }
        val payload = user.copy(updatedAt = System.currentTimeMillis())
        collection.document(payload.id).set(payload.toMap(), SetOptions.merge()).await()
    }
}

object FirestoreBrandsRepository {
    private val collection
        get() = FirebaseServiceLocator.firestore.collection(FirestoreCollections.BRANDS)

    suspend fun create(brand: FirestoreBrand): Result<Unit> = runCatching {
        require(brand.id.isNotBlank()) { "Brand id is required." }
        val now = System.currentTimeMillis()
        val payload = brand.copy(
            createdAt = brand.createdAt.takeIf { it > 0L } ?: now,
            updatedAt = now
        )
        collection.document(payload.id).set(payload.toMap()).await()
    }

    suspend fun getById(id: String): Result<FirestoreBrand?> = runCatching {
        if (id.isBlank()) return@runCatching null
        collection.document(id).get().await().toFirestoreBrand()
    }

    suspend fun list(limit: Long = DEFAULT_LIST_LIMIT): Result<List<FirestoreBrand>> = runCatching {
        collection
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { it.toFirestoreBrand() }
    }

    suspend fun listPublic(limit: Long = DEFAULT_LIST_LIMIT): Result<List<FirestoreBrand>> = runCatching {
        collection
            .whereIn("status", listOf("active", "claimed", "business"))
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { it.toFirestoreBrand() }
            .filterNot { it.isDeleted }
            .sortedByDescending { it.updatedAt }
    }

    suspend fun getAll(limit: Long = DEFAULT_LIST_LIMIT): Result<List<FirestoreBrand>> = runCatching {
        collection
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { it.toFirestoreBrand() }
    }

    suspend fun listByIds(ids: List<String>): Result<List<FirestoreBrand>> = runCatching {
        val normalizedIds = ids.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (normalizedIds.isEmpty()) return@runCatching emptyList()
        normalizedIds.mapNotNull { id ->
            collection.document(id).get().await().toFirestoreBrand()
        }
    }

    suspend fun update(brand: FirestoreBrand): Result<Unit> = runCatching {
        require(brand.id.isNotBlank()) { "Brand id is required." }
        val payload = brand.copy(updatedAt = System.currentTimeMillis())
        collection.document(payload.id).set(payload.toMap(), SetOptions.merge()).await()
    }

    suspend fun findBySlug(slug: String): Result<FirestoreBrand?> = runCatching {
        val normalized = slug.trim()
        if (normalized.isBlank()) return@runCatching null
        collection
            .whereEqualTo("slug", normalized)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toFirestoreBrand()
    }
}

object FirestoreBrandSuggestionsRepository {
    private val collection
        get() = FirebaseServiceLocator.firestore.collection(FirestoreCollections.BRAND_SUGGESTIONS)

    suspend fun create(suggestion: FirestoreBrandSuggestion): Result<Unit> = runCatching {
        require(suggestion.id.isNotBlank()) { "Suggestion id is required." }
        require(suggestion.submittedByUserId.isNotBlank()) { "submittedByUserId is required." }
        require(suggestion.brandName.isNotBlank()) { "brandName is required." }
        val now = System.currentTimeMillis()
        val payload = suggestion.copy(
            createdAt = suggestion.createdAt.takeIf { it > 0L } ?: now,
            updatedAt = now
        )
        collection.document(payload.id).set(payload.toMap()).await()
    }

    suspend fun getById(id: String): Result<FirestoreBrandSuggestion?> = runCatching {
        if (id.isBlank()) return@runCatching null
        collection.document(id).get().await().toFirestoreBrandSuggestion()
    }

    suspend fun list(limit: Long = DEFAULT_LIST_LIMIT): Result<List<FirestoreBrandSuggestion>> = runCatching {
        collection
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { it.toFirestoreBrandSuggestion() }
    }

    suspend fun listByUser(
        userId: String,
        limit: Long = DEFAULT_LIST_LIMIT
    ): Result<List<FirestoreBrandSuggestion>> = runCatching {
        val normalizedUserId = userId.trim()
        if (normalizedUserId.isBlank()) return@runCatching emptyList()
        collection
            .whereEqualTo("submittedByUserId", normalizedUserId)
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { it.toFirestoreBrandSuggestion() }
            .sortedByDescending { it.createdAt }
    }

    suspend fun listByStatus(
        status: String,
        limit: Long = DEFAULT_LIST_LIMIT
    ): Result<List<FirestoreBrandSuggestion>> = runCatching {
        val normalized = status.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return@runCatching emptyList()
        collection
            .whereEqualTo("status", normalized)
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { it.toFirestoreBrandSuggestion() }
            .sortedByDescending { it.updatedAt }
    }

    suspend fun update(suggestion: FirestoreBrandSuggestion): Result<Unit> = runCatching {
        require(suggestion.id.isNotBlank()) { "Suggestion id is required." }
        val payload = suggestion.copy(updatedAt = System.currentTimeMillis())
        collection.document(payload.id).set(payload.toMap(), SetOptions.merge()).await()
    }
}

object FirestoreSuggestionActionLogsRepository {
    private val collection
        get() = FirebaseServiceLocator.firestore.collection(FirestoreCollections.SUGGESTION_ACTION_LOGS)

    suspend fun create(log: FirestoreSuggestionActionLog): Result<Unit> = runCatching {
        require(log.id.isNotBlank()) { "Log id is required." }
        require(log.suggestionId.isNotBlank()) { "suggestionId is required." }
        val payload = if (log.createdAt > 0L) log else log.copy(createdAt = System.currentTimeMillis())
        collection.document(payload.id).set(payload.toMap()).await()
    }

    suspend fun listBySuggestionId(
        suggestionId: String,
        limit: Long = DEFAULT_LIST_LIMIT
    ): Result<List<FirestoreSuggestionActionLog>> = runCatching {
        val normalizedId = suggestionId.trim()
        if (normalizedId.isBlank()) return@runCatching emptyList()
        collection
            .whereEqualTo("suggestionId", normalizedId)
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { it.toFirestoreSuggestionActionLog() }
            .sortedByDescending { it.createdAt }
    }
}

object FirestoreProductsRepository {
    private val collection
        get() = FirebaseServiceLocator.firestore.collection(FirestoreCollections.PRODUCTS)

    suspend fun create(product: FirestoreProduct): Result<Unit> = runCatching {
        require(product.id.isNotBlank()) { "Product id is required." }
        require(product.brandId.isNotBlank()) { "Product brandId is required." }
        val now = System.currentTimeMillis()
        val payload = product.copy(
            createdAt = product.createdAt.takeIf { it > 0L } ?: now,
            updatedAt = now
        )
        collection.document(payload.id).set(payload.toMap()).await()
    }

    suspend fun getById(id: String): Result<FirestoreProduct?> = runCatching {
        if (id.isBlank()) return@runCatching null
        collection.document(id).get().await().toFirestoreProduct()
    }

    suspend fun list(limit: Long = DEFAULT_LIST_LIMIT): Result<List<FirestoreProduct>> = runCatching {
        collection
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { it.toFirestoreProduct() }
    }

    suspend fun listByBrand(
        brandId: String,
        limit: Long = DEFAULT_LIST_LIMIT
    ): Result<List<FirestoreProduct>> = runCatching {
        if (brandId.isBlank()) return@runCatching emptyList()
        collection
            .whereEqualTo("brandId", brandId)
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { it.toFirestoreProduct() }
    }

    suspend fun findByBarcode(barcode: String): Result<FirestoreProduct?> = runCatching {
        val normalized = barcode.trim()
        if (normalized.isBlank()) return@runCatching null
        collection
            .whereEqualTo("barcode", normalized)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toFirestoreProduct()
    }

    suspend fun findBySourceUrl(sourceUrl: String): Result<FirestoreProduct?> = runCatching {
        val normalized = sourceUrl.trim()
        if (normalized.isBlank()) return@runCatching null
        collection
            .whereEqualTo("sourceUrl", normalized)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toFirestoreProduct()
    }

    suspend fun findByBrandAndSlug(
        brandId: String,
        slug: String
    ): Result<FirestoreProduct?> = runCatching {
        val normalizedBrandId = brandId.trim()
        val normalizedSlug = slug.trim()
        if (normalizedBrandId.isBlank() || normalizedSlug.isBlank()) return@runCatching null

        // Keep query index-light in early-stage environments.
        collection
            .whereEqualTo("brandId", normalizedBrandId)
            .limit(DEFAULT_LIST_LIMIT)
            .get()
            .await()
            .documents
            .mapNotNull { it.toFirestoreProduct() }
            .firstOrNull { it.slug.equals(normalizedSlug, ignoreCase = true) }
    }

    suspend fun update(product: FirestoreProduct): Result<Unit> = runCatching {
        require(product.id.isNotBlank()) { "Product id is required." }
        val payload = product.copy(updatedAt = System.currentTimeMillis())
        collection.document(payload.id).set(payload.toMap(), SetOptions.merge()).await()
    }

    suspend fun deleteById(id: String): Result<Unit> = runCatching {
        require(id.isNotBlank()) { "Product id is required." }
        collection.document(id).delete().await()
    }
}

object FirestoreReviewsRepository {
    private val collection
        get() = FirebaseServiceLocator.firestore.collection(FirestoreCollections.REVIEWS)

    fun reviewDocumentId(
        userId: String,
        targetType: String,
        targetId: String
    ): String {
        val normalizedUserId = userId.trim().replace("/", "_")
        val normalizedTargetType = targetType.trim().lowercase(Locale.ROOT)
        val normalizedTargetId = targetId.trim().replace("/", "_")
        return "${normalizedUserId}_${normalizedTargetType}_${normalizedTargetId}"
    }

    suspend fun create(review: FirestoreReview): Result<Unit> = runCatching {
        require(review.id.isNotBlank()) { "Review id is required." }
        require(review.userId.isNotBlank()) { "Review userId is required." }
        require(review.targetType.isNotBlank()) { "Review targetType is required." }
        require(review.targetId.isNotBlank()) { "Review targetId is required." }
        val now = System.currentTimeMillis()
        val payload = review.copy(
            createdAt = review.createdAt.takeIf { it > 0L } ?: now,
            updatedAt = now
        )
        collection.document(payload.id).set(payload.toMap()).await()
    }

    suspend fun getById(id: String): Result<FirestoreReview?> = runCatching {
        if (id.isBlank()) return@runCatching null
        collection.document(id).get().await().toFirestoreReview()
    }

    suspend fun list(limit: Long = DEFAULT_LIST_LIMIT): Result<List<FirestoreReview>> = runCatching {
        collection
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { it.toFirestoreReview() }
    }

    suspend fun listByTarget(
        targetType: String,
        targetId: String,
        limit: Long = DEFAULT_LIST_LIMIT
    ): Result<List<FirestoreReview>> = runCatching {
        val normalizedType = targetType.trim().lowercase(Locale.ROOT)
        val normalizedTargetId = targetId.trim()
        if (normalizedType.isBlank() || normalizedTargetId.isBlank()) return@runCatching emptyList()

        val lowercaseResults = collection
            .whereEqualTo("targetType", normalizedType)
            .whereEqualTo("targetId", normalizedTargetId)
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { it.toFirestoreReview() }

        val legacyUppercaseResults = if (lowercaseResults.isEmpty()) {
            collection
                .whereEqualTo("targetType", normalizedType.uppercase(Locale.ROOT))
                .whereEqualTo("targetId", normalizedTargetId)
                .limit(limit)
                .get()
                .await()
                .documents
                .mapNotNull { it.toFirestoreReview() }
        } else {
            emptyList()
        }

        (lowercaseResults + legacyUppercaseResults)
            .distinctBy { it.id }
            .sortedByDescending { it.updatedAt }
    }

    suspend fun getByUserAndTarget(
        userId: String,
        targetType: String,
        targetId: String
    ): Result<FirestoreReview?> = runCatching {
        val normalizedUserId = userId.trim()
        val normalizedType = targetType.trim().lowercase(Locale.ROOT)
        val normalizedTargetId = targetId.trim()
        if (normalizedUserId.isBlank() || normalizedType.isBlank() || normalizedTargetId.isBlank()) {
            return@runCatching null
        }

        val deterministicId = reviewDocumentId(
            userId = normalizedUserId,
            targetType = normalizedType,
            targetId = normalizedTargetId
        )
        val direct = collection.document(deterministicId).get().await().toFirestoreReview()
        if (direct != null) return@runCatching direct

        val legacyMatch = collection
            .whereEqualTo("userId", normalizedUserId)
            .whereEqualTo("targetType", normalizedType)
            .whereEqualTo("targetId", normalizedTargetId)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toFirestoreReview()

        legacyMatch ?: collection
            .whereEqualTo("userId", normalizedUserId)
            .whereEqualTo("targetType", normalizedType.uppercase(Locale.ROOT))
            .whereEqualTo("targetId", normalizedTargetId)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toFirestoreReview()
    }

    suspend fun upsertByUserAndTarget(
        userId: String,
        targetType: String,
        targetId: String,
        rating: Int,
        comment: String,
        brandId: String = ""
    ): Result<FirestoreReview> = runCatching {
        val normalizedUserId = userId.trim()
        val normalizedType = targetType.trim().lowercase(Locale.ROOT)
        val normalizedTargetId = targetId.trim()
        require(normalizedUserId.isNotBlank()) { "Review userId is required." }
        require(normalizedType.isNotBlank()) { "Review targetType is required." }
        require(normalizedTargetId.isNotBlank()) { "Review targetId is required." }
        require(rating in 1..5) { "Rating must be between 1 and 5." }

        val now = System.currentTimeMillis()
        val normalizedComment = comment.trim()
        val deterministicId = reviewDocumentId(
            userId = normalizedUserId,
            targetType = normalizedType,
            targetId = normalizedTargetId
        )
        val docRef = collection.document(deterministicId)
        val existing = docRef.get().await().toFirestoreReview()

        val payload = FirestoreReview(
            id = deterministicId,
            userId = normalizedUserId,
            targetType = normalizedType,
            targetId = normalizedTargetId,
            brandId = brandId.trim(),
            rating = rating,
            comment = normalizedComment,
            createdAt = existing?.createdAt?.takeIf { it > 0L } ?: now,
            updatedAt = now
        )

        docRef.set(payload.toMap()).await()
        payload
    }

    suspend fun deleteByUserAndTarget(
        userId: String,
        targetType: String,
        targetId: String
    ): Result<Unit> = runCatching {
        val existing = getByUserAndTarget(
            userId = userId,
            targetType = targetType,
            targetId = targetId
        ).getOrNull() ?: return@runCatching

        collection.document(existing.id).delete().await()
    }

    suspend fun update(review: FirestoreReview): Result<Unit> = runCatching {
        require(review.id.isNotBlank()) { "Review id is required." }
        val payload = review.copy(updatedAt = System.currentTimeMillis())
        collection.document(payload.id).set(payload.toMap(), SetOptions.merge()).await()
    }
}

object FirestoreEventsRepository {
    private val collection
        get() = FirebaseServiceLocator.firestore.collection(FirestoreCollections.EVENTS)

    suspend fun create(event: FirestoreEvent): Result<Unit> = runCatching {
        require(event.id.isNotBlank()) { "Event id is required." }
        val now = System.currentTimeMillis()
        val payload = event.copy(
            createdAt = event.createdAt.takeIf { it > 0L } ?: now,
            updatedAt = now
        )
        collection.document(payload.id).set(payload.toMap()).await()
    }

    suspend fun getById(id: String): Result<FirestoreEvent?> = runCatching {
        if (id.isBlank()) return@runCatching null
        collection.document(id).get().await().toFirestoreEvent()
    }

    suspend fun list(limit: Long = DEFAULT_LIST_LIMIT): Result<List<FirestoreEvent>> = runCatching {
        collection
            .orderBy("startAt", Query.Direction.ASCENDING)
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { it.toFirestoreEvent() }
    }

    suspend fun update(event: FirestoreEvent): Result<Unit> = runCatching {
        require(event.id.isNotBlank()) { "Event id is required." }
        val payload = event.copy(updatedAt = System.currentTimeMillis())
        collection.document(payload.id).set(payload.toMap(), SetOptions.merge()).await()
    }
}

object FirestoreClaimRequestsRepository {
    private val collection
        get() = FirebaseServiceLocator.firestore.collection(FirestoreCollections.CLAIM_REQUESTS)

    suspend fun create(request: FirestoreClaimRequest): Result<Unit> = runCatching {
        require(request.id.isNotBlank()) { "Claim request id is required." }
        require(request.brandId.isNotBlank()) { "Claim request brandId is required." }
        require(request.userId.isNotBlank()) { "Claim request userId is required." }
        val now = System.currentTimeMillis()
        val payload = request.copy(
            createdAt = request.createdAt.takeIf { it > 0L } ?: now,
            updatedAt = now
        )
        collection.document(payload.id).set(payload.toMap()).await()
    }

    suspend fun getById(id: String): Result<FirestoreClaimRequest?> = runCatching {
        if (id.isBlank()) return@runCatching null
        collection.document(id).get().await().toFirestoreClaimRequest()
    }

    suspend fun list(limit: Long = DEFAULT_LIST_LIMIT): Result<List<FirestoreClaimRequest>> = runCatching {
        collection
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { it.toFirestoreClaimRequest() }
    }

    suspend fun update(request: FirestoreClaimRequest): Result<Unit> = runCatching {
        require(request.id.isNotBlank()) { "Claim request id is required." }
        val payload = request.copy(updatedAt = System.currentTimeMillis())
        collection.document(payload.id).set(payload.toMap(), SetOptions.merge()).await()
    }
}
