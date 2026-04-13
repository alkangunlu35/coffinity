package com.icoffee.app.data.model

enum class BrandSuggestionStatus(val storageValue: String, val isFinal: Boolean) {
    PENDING("pending", false),
    UNDER_REVIEW("under_review", false),
    APPROVED_NEW_BRAND("approved_new_brand", true),
    MERGED_EXISTING_BRAND("merged_existing_brand", true),
    REJECTED("rejected", true);

    companion object {
        fun fromStorage(raw: String?): BrandSuggestionStatus {
            val normalized = raw?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.storageValue == normalized } ?: PENDING
        }
    }
}

enum class SuggestionActionType(val storageValue: String) {
    SUBMIT("submit"),
    MARK_UNDER_REVIEW("mark_under_review"),
    APPROVE_NEW_BRAND("approve_new_brand"),
    MERGE_EXISTING_BRAND("merge_existing_brand"),
    REJECT("reject");
}

data class BrandSuggestionFlags(
    val possibleDuplicate: Boolean = false,
    val hasWebsite: Boolean = false,
    val hasInstagram: Boolean = false,
    val lowQualityText: Boolean = false
)

data class SuggestBrandInput(
    val brandName: String,
    val websiteUrl: String? = null,
    val instagramUrl: String? = null,
    val country: String? = null,
    val city: String? = null,
    val description: String? = null
)
