package com.icoffee.app.data.model.importer

data class BrandImportPreview(
    val sourceUrl: String,
    val detectedBrandName: String? = null,
    val detectedDescription: String? = null,
    val detectedLogoUrl: String? = null,
    val detectedCoverImageUrl: String? = null,
    val detectedCountry: String? = null,
    val detectedCity: String? = null,
    val detectedWebsite: String? = null,
    val detectedInstagram: String? = null,
    val extractionWarnings: List<String> = emptyList(),
    val extractionConfidenceNotes: List<String> = emptyList()
)

enum class BrandImportFailureReason {
    INVALID_URL,
    UNAUTHORIZED,
    UNREACHABLE,
    NO_DATA,
    UNKNOWN
}

sealed interface BrandImportPreviewResult {
    data class Success(val preview: BrandImportPreview) : BrandImportPreviewResult
    data class Failure(val reason: BrandImportFailureReason) : BrandImportPreviewResult
}
