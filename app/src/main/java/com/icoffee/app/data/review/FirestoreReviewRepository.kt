package com.icoffee.app.data.review

import com.icoffee.app.data.BrandRepository
import com.icoffee.app.data.PhaseOneRepository
import com.icoffee.app.data.firebase.repository.FirestoreBrandsRepository
import com.icoffee.app.data.firebase.repository.FirestoreProductsRepository
import com.icoffee.app.data.firebase.repository.FirestoreReviewsRepository
import com.icoffee.app.data.firebase.repository.FirestoreUsersRepository
import com.icoffee.app.data.model.Review
import com.icoffee.app.data.model.ReviewTargetType
import java.util.Locale

object FirestoreReviewRepository {

    suspend fun listReviews(
        targetType: ReviewTargetType,
        targetId: String
    ): List<Review> {
        val normalizedTargetId = targetId.trim()
        if (normalizedTargetId.isBlank()) return emptyList()

        val targetTypeStorage = targetType.toStorageValue()
        val reviewEntries = FirestoreReviewsRepository.listByTarget(
            targetType = targetTypeStorage,
            targetId = normalizedTargetId,
            limit = 300
        ).getOrDefault(emptyList())

        if (reviewEntries.isEmpty()) return emptyList()

        val usersById = FirestoreUsersRepository.listByIds(
            reviewEntries.map { it.userId }.distinct()
        ).getOrDefault(emptyList()).associateBy { it.id }

        return reviewEntries.map { entry ->
            val author = usersById[entry.userId]
            val authorDisplayName = author?.displayName
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: author?.email
                    ?.substringBefore("@")
                    ?.takeIf { it.isNotBlank() }

            Review(
                id = entry.id,
                userId = entry.userId,
                targetType = targetType,
                targetId = entry.targetId,
                rating = entry.rating.coerceIn(1, 5),
                comment = entry.comment,
                createdAt = entry.createdAt,
                updatedAt = entry.updatedAt,
                authorDisplayName = authorDisplayName,
                authorAvatarUrl = author?.photoUrl
            )
        }.sortedByDescending { it.updatedAt }
    }

    suspend fun submitReview(
        userId: String,
        targetType: ReviewTargetType,
        targetId: String,
        rating: Int,
        comment: String
    ): BrandRepository.ReviewSubmissionResult {
        val normalizedUserId = userId.trim()
        if (normalizedUserId.isBlank()) {
            return BrandRepository.ReviewSubmissionResult.Failure(
                BrandRepository.ReviewFailureReason.UNAUTHORIZED
            )
        }
        if (rating !in 1..5) {
            return BrandRepository.ReviewSubmissionResult.Failure(
                BrandRepository.ReviewFailureReason.INVALID_RATING
            )
        }

        val normalizedComment = comment.trim().replace(Regex("\\s+"), " ")
        if (normalizedComment.isNotBlank() && normalizedComment.length < 3) {
            return BrandRepository.ReviewSubmissionResult.Failure(
                BrandRepository.ReviewFailureReason.INVALID_COMMENT
            )
        }

        val normalizedTargetId = targetId.trim()
        if (normalizedTargetId.isBlank() || !targetExists(targetType, normalizedTargetId)) {
            return BrandRepository.ReviewSubmissionResult.Failure(
                BrandRepository.ReviewFailureReason.TARGET_NOT_FOUND
            )
        }

        val brandId = when (targetType) {
            ReviewTargetType.BRAND -> normalizedTargetId
            ReviewTargetType.PRODUCT -> FirestoreProductsRepository.getById(normalizedTargetId)
                .getOrNull()
                ?.brandId
                .orEmpty()
            ReviewTargetType.BREW -> ""
        }

        return if (
            FirestoreReviewsRepository.upsertByUserAndTarget(
                userId = normalizedUserId,
                targetType = targetType.toStorageValue(),
                targetId = normalizedTargetId,
                rating = rating,
                comment = normalizedComment,
                brandId = brandId
            ).isSuccess
        ) {
            BrandRepository.ReviewSubmissionResult.Success
        } else {
            BrandRepository.ReviewSubmissionResult.Failure(
                BrandRepository.ReviewFailureReason.STORE_ERROR
            )
        }
    }

    suspend fun deleteOwnReview(
        userId: String,
        targetType: ReviewTargetType,
        targetId: String
    ): Boolean {
        val normalizedUserId = userId.trim()
        val normalizedTargetId = targetId.trim()
        if (normalizedUserId.isBlank() || normalizedTargetId.isBlank()) return false

        return FirestoreReviewsRepository.deleteByUserAndTarget(
            userId = normalizedUserId,
            targetType = targetType.toStorageValue(),
            targetId = normalizedTargetId
        ).isSuccess
    }

    private suspend fun targetExists(
        targetType: ReviewTargetType,
        targetId: String
    ): Boolean {
        return when (targetType) {
            ReviewTargetType.BRAND -> {
                FirestoreBrandsRepository.getById(targetId).getOrNull() != null ||
                    BrandRepository.getBrandById(targetId) != null
            }

            ReviewTargetType.PRODUCT -> {
                FirestoreProductsRepository.getById(targetId).getOrNull() != null ||
                    BrandRepository.getProductById(targetId) != null
            }

            ReviewTargetType.BREW -> {
                PhaseOneRepository.brewingMethods.any { it.id == targetId }
            }
        }
    }

    private fun ReviewTargetType.toStorageValue(): String = name.lowercase(Locale.ROOT)
}
