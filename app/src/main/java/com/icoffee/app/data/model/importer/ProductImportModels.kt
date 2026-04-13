package com.icoffee.app.data.model.importer

data class ProductImportPreview(
    val sourceUrl: String,
    val detectedBrandName: String? = null,
    val detectedProductName: String? = null,
    val detectedDescription: String? = null,
    val detectedImageUrl: String? = null,
    val detectedOrigin: String? = null,
    val detectedRoastLevel: String? = null,
    val detectedProcess: String? = null,
    val detectedTastingNotes: List<String> = emptyList(),
    val barcode: String? = null,
    val extractionWarnings: List<String> = emptyList(),
    val extractionConfidenceNotes: List<String> = emptyList()
)

data class ProductImportDraft(
    val brandId: String? = null,
    val sourceUrl: String,
    val name: String,
    val detectedBrandName: String? = null,
    val createDraftBrandIfMissing: Boolean = false,
    val description: String? = null,
    val imageUrl: String? = null,
    val origin: String? = null,
    val roastLevel: String? = null,
    val process: String? = null,
    val tastingNotes: List<String> = emptyList(),
    val barcode: String? = null
)

enum class ProductImportFailureReason {
    INVALID_URL,
    UNREACHABLE,
    NO_DATA,
    UNKNOWN
}

sealed interface ProductImportPreviewResult {
    data class Success(val preview: ProductImportPreview) : ProductImportPreviewResult
    data class Failure(val reason: ProductImportFailureReason) : ProductImportPreviewResult
}

enum class ProductImportDuplicateReason {
    SAME_BRAND_AND_NAME,
    SAME_BARCODE,
    SAME_SOURCE_URL
}

enum class ProductImportSaveFailureReason {
    UNAUTHORIZED,
    INVALID_BRAND,
    INVALID_NAME,
    STORE_ERROR
}

sealed interface ProductImportSaveResult {
    data class Success(
        val productId: String,
        val resolvedBrandId: String,
        val createdDraftBrand: Boolean = false
    ) : ProductImportSaveResult
    data class Duplicate(
        val existingProductId: String,
        val reason: ProductImportDuplicateReason
    ) : ProductImportSaveResult

    data class Failure(val reason: ProductImportSaveFailureReason) : ProductImportSaveResult
}
