package com.icoffee.app.data.model

enum class ReviewTargetType {
    BRAND,
    PRODUCT,
    BREW
}

enum class BrandCategory {
    SPECIALTY,
    LOCAL_ROASTERS,
    CHAINS,
    COMMERCIAL
}

enum class BrandVisibilityTier {
    ORGANIC,
    FEATURED_SUBSCRIBER
}

enum class BrandLifecycleStatus(val storageValue: String) {
    DRAFT("draft"),
    ACTIVE("active"),
    CLAIMED("claimed"),
    BUSINESS("business");

    companion object {
        fun fromStorage(raw: String?): BrandLifecycleStatus {
            val normalized = raw?.trim()?.lowercase().orEmpty()
            return when (normalized) {
                ACTIVE.storageValue -> ACTIVE
                CLAIMED.storageValue -> CLAIMED
                BUSINESS.storageValue -> BUSINESS
                "unmanaged" -> DRAFT
                else -> DRAFT
            }
        }
    }
}

data class Brand(
    val id: String,
    val name: String,
    val slug: String,
    val country: String? = null,
    val cityOrArea: String? = null,
    val category: BrandCategory = BrandCategory.SPECIALTY,
    val visibilityTier: BrandVisibilityTier = BrandVisibilityTier.ORGANIC,
    val description: String,
    val logoUrl: String? = null,
    val website: String? = null,
    val instagram: String? = null,
    val sourceUrl: String? = null,
    val status: String = BrandLifecycleStatus.DRAFT.storageValue,
    val ownerUserId: String? = null,
    val ownerEmail: String? = null,
    val managedByUserIds: List<String> = emptyList(),
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0,
    val productCount: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class BrandProduct(
    val id: String,
    val brandId: String,
    val name: String,
    val slug: String,
    val roastLevel: String? = null,
    val originSummary: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0,
    val affiliateLinks: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class Review(
    val id: String,
    val userId: String,
    val targetType: ReviewTargetType,
    val targetId: String,
    val rating: Int,
    val comment: String,
    val createdAt: Long,
    val updatedAt: Long,
    val authorDisplayName: String? = null,
    val authorAvatarUrl: String? = null
)

enum class SuggestionStatus {
    PENDING,
    APPROVED,
    REJECTED
}

data class BrandSuggestion(
    val id: String,
    val userId: String,
    val name: String,
    val normalizedName: String,
    val country: String?,
    val description: String?,
    val status: SuggestionStatus,
    val createdAt: Long
)

data class ProductSuggestion(
    val id: String,
    val userId: String,
    val brandId: String,
    val name: String,
    val normalizedName: String,
    val originSummary: String?,
    val description: String?,
    val status: SuggestionStatus,
    val createdAt: Long
)

data class BrandDiscoveryContext(
    val cityOrArea: String? = null,
    val countryOrRegion: String? = null
)

enum class BrandDiscoverySectionType {
    TOP_RATED,
    TRENDING,
    FEATURED,
    ALL_BRANDS
}

data class BrandDiscoverySection(
    val type: BrandDiscoverySectionType,
    val brands: List<Brand>,
    val isSponsored: Boolean = false
)

data class BrandDiscoveryFeed(
    val sections: List<BrandDiscoverySection>,
    val availableCategories: List<BrandCategory>,
    val locationContextLabel: String? = null
)

fun Double.displayRating(): String =
    if (this <= 0.0) "0.0" else String.format(java.util.Locale.US, "%.1f", this)
