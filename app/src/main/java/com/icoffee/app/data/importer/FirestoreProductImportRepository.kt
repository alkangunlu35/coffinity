package com.icoffee.app.data.importer

import com.icoffee.app.data.admin.BrandManagementRepository
import com.icoffee.app.data.firebase.model.FirestoreProduct
import com.icoffee.app.data.firebase.repository.FirestoreBrandsRepository
import com.icoffee.app.data.firebase.repository.FirestoreProductsRepository
import com.icoffee.app.data.model.importer.ProductImportDraft
import com.icoffee.app.data.model.importer.ProductImportDuplicateReason
import com.icoffee.app.data.model.importer.ProductImportSaveFailureReason
import com.icoffee.app.data.model.importer.ProductImportSaveResult
import java.util.UUID

object FirestoreProductImportRepository {

    suspend fun saveImportedProduct(
        actorUserId: String,
        draft: ProductImportDraft
    ): ProductImportSaveResult {
        val normalizedName = draft.name.trim()
        if (normalizedName.isBlank()) {
            return ProductImportSaveResult.Failure(ProductImportSaveFailureReason.INVALID_NAME)
        }

        val requestedBrandId = draft.brandId?.trim().orEmpty()
        var resolvedBrandId = requestedBrandId
        var createdDraftBrand = false
        if (draft.createDraftBrandIfMissing) {
            val detectedBrandName = draft.detectedBrandName?.trim().orEmpty()
            if (detectedBrandName.isBlank()) {
                return ProductImportSaveResult.Failure(ProductImportSaveFailureReason.INVALID_BRAND)
            }
            val brandResolution = BrandManagementRepository.createOrReuseDraftBrandForImportedProduct(
                actorUserId = actorUserId,
                detectedBrandName = detectedBrandName,
                sourceUrl = draft.sourceUrl
            ).getOrNull()
            if (brandResolution == null) {
                return ProductImportSaveResult.Failure(ProductImportSaveFailureReason.UNAUTHORIZED)
            }
            createdDraftBrand = brandResolution.createdDraftBrand
            resolvedBrandId = brandResolution.brandId
        }

        if (resolvedBrandId.isBlank()) {
            return ProductImportSaveResult.Failure(ProductImportSaveFailureReason.INVALID_BRAND)
        }

        val canManageBrand = BrandManagementRepository.canUserManageBrand(actorUserId, resolvedBrandId)
        if (!canManageBrand) {
            return ProductImportSaveResult.Failure(ProductImportSaveFailureReason.UNAUTHORIZED)
        }

        val brand = FirestoreBrandsRepository.getById(resolvedBrandId).getOrNull()
            ?: return ProductImportSaveResult.Failure(ProductImportSaveFailureReason.INVALID_BRAND)

        val slug = normalizeSlug(normalizedName)

        val barcodeDuplicate = draft.barcode
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { barcode ->
                FirestoreProductsRepository.findByBarcode(barcode).getOrNull()
            }
        if (barcodeDuplicate != null) {
            return ProductImportSaveResult.Duplicate(
                existingProductId = barcodeDuplicate.id,
                reason = ProductImportDuplicateReason.SAME_BARCODE
            )
        }

        val sourceUrlDuplicate = draft.sourceUrl
            .trim()
            .takeIf { it.isNotBlank() }
            ?.let { sourceUrl ->
                FirestoreProductsRepository.findBySourceUrl(sourceUrl).getOrNull()
            }
        if (sourceUrlDuplicate != null) {
            return ProductImportSaveResult.Duplicate(
                existingProductId = sourceUrlDuplicate.id,
                reason = ProductImportDuplicateReason.SAME_SOURCE_URL
            )
        }

        val brandSlugDuplicate = FirestoreProductsRepository.findByBrandAndSlug(resolvedBrandId, slug)
            .getOrNull()
        if (brandSlugDuplicate != null) {
            return ProductImportSaveResult.Duplicate(
                existingProductId = brandSlugDuplicate.id,
                reason = ProductImportDuplicateReason.SAME_BRAND_AND_NAME
            )
        }

        val now = System.currentTimeMillis()
        val productId = "product_${UUID.randomUUID().toString().replace("-", "")}".lowercase()
        val payload = FirestoreProduct(
            id = productId,
            brandId = resolvedBrandId,
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
                .filter { it.isNotBlank() },
            sourceUrl = draft.sourceUrl.trim().takeIf { it.isNotBlank() },
            importedVia = "url_import",
            averageRating = 0.0,
            reviewCount = 0,
            createdAt = now,
            updatedAt = now
        )

        if (FirestoreProductsRepository.create(payload).isFailure) {
            return ProductImportSaveResult.Failure(ProductImportSaveFailureReason.STORE_ERROR)
        }

        FirestoreBrandsRepository.update(
            brand.copy(
                productCount = (brand.productCount + 1).coerceAtLeast(1),
                updatedAt = System.currentTimeMillis()
            )
        )

        return if (createdDraftBrand) {
            ProductImportSaveResult.Success(
                productId = productId,
                resolvedBrandId = resolvedBrandId,
                createdDraftBrand = true
            )
        } else {
            ProductImportSaveResult.Success(
                productId = productId,
                resolvedBrandId = resolvedBrandId,
                createdDraftBrand = false
            )
        }
    }
}
