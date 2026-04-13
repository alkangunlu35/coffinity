package com.icoffee.app.data

import android.content.Context
import android.content.SharedPreferences
import com.icoffee.app.data.model.Brand
import com.icoffee.app.data.model.BrandCategory
import com.icoffee.app.data.model.BrandDiscoveryContext
import com.icoffee.app.data.model.BrandDiscoveryFeed
import com.icoffee.app.data.model.BrandDiscoverySection
import com.icoffee.app.data.model.BrandDiscoverySectionType
import com.icoffee.app.data.model.BrandProduct
import com.icoffee.app.data.model.BrandSuggestion
import com.icoffee.app.data.model.BrandVisibilityTier
import com.icoffee.app.data.model.ProductSuggestion
import com.icoffee.app.data.model.Review
import com.icoffee.app.data.model.ReviewTargetType
import com.icoffee.app.data.model.SuggestionStatus
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

object BrandRepository {

    sealed interface ReviewSubmissionResult {
        data object Success : ReviewSubmissionResult
        data class Failure(val reason: ReviewFailureReason) : ReviewSubmissionResult
    }

    enum class ReviewFailureReason {
        UNAUTHORIZED,
        INVALID_RATING,
        INVALID_COMMENT,
        TARGET_NOT_FOUND,
        STORE_ERROR
    }

    sealed interface SuggestionSubmissionResult {
        data object Success : SuggestionSubmissionResult
        data class Failure(val reason: SuggestionFailureReason) : SuggestionSubmissionResult
    }

    enum class SuggestionFailureReason {
        UNAUTHORIZED,
        EMPTY_NAME,
        BRAND_NOT_FOUND,
        DUPLICATE_EXISTING,
        DUPLICATE_PENDING
    }

    private const val PREFS_NAME = "coffinity_brand_platform"
    private const val KEY_REVIEWS = "brand_reviews"
    private const val KEY_BRAND_SUGGESTIONS = "brand_suggestions"
    private const val KEY_PRODUCT_SUGGESTIONS = "product_suggestions"
    private const val KEY_BRAND_ACTIVITY = "brand_activity"

    private const val TOP_RATED_MIN_REVIEWS = 2
    private const val TOP_RATED_LIMIT = 8
    private const val TRENDING_LIMIT = 8
    private const val FEATURED_SLOT_LIMIT = 6
    private const val FEATURED_ROTATION_WINDOW_MS = 30 * 60 * 1000L
    private const val TRENDING_DECAY_WINDOW_MS = 72 * 60 * 60 * 1000L

    private lateinit var prefs: SharedPreferences

    private data class BrandActivitySnapshot(
        val brandId: String,
        val profileViews: Int = 0,
        val saves: Int = 0,
        val interactions: Int = 0,
        val lastViewedAt: Long = 0L,
        val lastSavedAt: Long = 0L,
        val lastInteractionAt: Long = 0L
    )

    private val baseBrands = listOf(
        Brand(
            id = "brand_starbucks",
            name = "Starbucks",
            slug = "starbucks",
            country = "United States",
            cityOrArea = "Seattle",
            category = BrandCategory.CHAINS,
            visibilityTier = BrandVisibilityTier.FEATURED_SUBSCRIBER,
            description = "Global coffeehouse brand known for espresso drinks and seasonal blends.",
            createdAt = 1_704_067_200_000L,
            updatedAt = 1_704_067_200_000L
        ),
        Brand(
            id = "brand_nespresso",
            name = "Nespresso",
            slug = "nespresso",
            country = "Switzerland",
            cityOrArea = "Lausanne",
            category = BrandCategory.COMMERCIAL,
            visibilityTier = BrandVisibilityTier.FEATURED_SUBSCRIBER,
            description = "Capsule-focused specialty coffee brand with origin-driven collections.",
            createdAt = 1_704_067_200_000L,
            updatedAt = 1_704_067_200_000L
        ),
        Brand(
            id = "brand_lavazza",
            name = "Lavazza",
            slug = "lavazza",
            country = "Italy",
            cityOrArea = "Turin",
            category = BrandCategory.COMMERCIAL,
            visibilityTier = BrandVisibilityTier.FEATURED_SUBSCRIBER,
            description = "Historic Italian roaster known for espresso blends and café classics.",
            createdAt = 1_704_067_200_000L,
            updatedAt = 1_704_067_200_000L
        ),
        Brand(
            id = "brand_illy",
            name = "Illy",
            slug = "illy",
            country = "Italy",
            cityOrArea = "Trieste",
            category = BrandCategory.COMMERCIAL,
            description = "Premium Italian coffee brand centered on smooth, balanced espresso.",
            createdAt = 1_704_067_200_000L,
            updatedAt = 1_704_067_200_000L
        ),
        Brand(
            id = "brand_blue_bottle",
            name = "Blue Bottle Coffee",
            slug = "blue-bottle-coffee",
            country = "United States",
            cityOrArea = "Oakland",
            category = BrandCategory.SPECIALTY,
            visibilityTier = BrandVisibilityTier.FEATURED_SUBSCRIBER,
            description = "Specialty coffee company focused on fresh roasts and clean filter profiles.",
            createdAt = 1_704_067_200_000L,
            updatedAt = 1_704_067_200_000L
        ),
        Brand(
            id = "brand_coffee_collective",
            name = "Coffee Collective",
            slug = "coffee-collective",
            country = "Denmark",
            cityOrArea = "Copenhagen",
            category = BrandCategory.LOCAL_ROASTERS,
            visibilityTier = BrandVisibilityTier.FEATURED_SUBSCRIBER,
            description = "Nordic specialty roaster with transparent sourcing and bright single origins.",
            createdAt = 1_704_067_200_000L,
            updatedAt = 1_704_067_200_000L
        ),
        Brand(
            id = "brand_tchibo",
            name = "Tchibo",
            slug = "tchibo",
            country = "Germany",
            cityOrArea = "Hamburg",
            category = BrandCategory.CHAINS,
            visibilityTier = BrandVisibilityTier.FEATURED_SUBSCRIBER,
            description = "Long-standing European coffee brand offering everyday and premium lines.",
            createdAt = 1_704_067_200_000L,
            updatedAt = 1_704_067_200_000L
        ),
        Brand(
            id = "brand_kurukahveci",
            name = "Kurukahveci Mehmet Efendi",
            slug = "kurukahveci-mehmet-efendi",
            country = "Turkey",
            cityOrArea = "Istanbul",
            category = BrandCategory.LOCAL_ROASTERS,
            visibilityTier = BrandVisibilityTier.FEATURED_SUBSCRIBER,
            description = "Iconic Turkish coffee brand rooted in traditional roast and grind craft.",
            createdAt = 1_704_067_200_000L,
            updatedAt = 1_704_067_200_000L
        )
    )

    private val baseProducts = listOf(
        BrandProduct(
            id = "product_starbucks_veranda",
            brandId = "brand_starbucks",
            name = "Veranda Blend",
            slug = "veranda-blend",
            roastLevel = "Light",
            originSummary = "Latin America",
            description = "Soft cocoa and toasted nut notes in a mellow daily cup.",
            affiliateLinks = emptyList(),
            createdAt = 1_704_067_200_000L,
            updatedAt = 1_704_067_200_000L
        ),
        BrandProduct(
            id = "product_starbucks_pike",
            brandId = "brand_starbucks",
            name = "Pike Place Roast",
            slug = "pike-place-roast",
            roastLevel = "Medium",
            originSummary = "Latin America",
            description = "Balanced body with chocolate and toasted nut sweetness.",
            affiliateLinks = emptyList(),
            createdAt = 1_704_067_200_000L,
            updatedAt = 1_704_067_200_000L
        ),
        BrandProduct(
            id = "product_nespresso_arpeggio",
            brandId = "brand_nespresso",
            name = "Arpeggio",
            slug = "arpeggio",
            roastLevel = "Dark",
            originSummary = "Costa Rica • Brazil",
            description = "Dense and creamy capsule espresso with cocoa depth.",
            affiliateLinks = emptyList(),
            createdAt = 1_704_067_200_000L,
            updatedAt = 1_704_067_200_000L
        ),
        BrandProduct(
            id = "product_nespresso_volluto",
            brandId = "brand_nespresso",
            name = "Volluto",
            slug = "volluto",
            roastLevel = "Light",
            originSummary = "Brazil • Colombia",
            description = "Sweet biscuit profile with delicate fruit brightness.",
            affiliateLinks = emptyList(),
            createdAt = 1_704_067_200_000L,
            updatedAt = 1_704_067_200_000L
        ),
        BrandProduct(
            id = "product_lavazza_oro",
            brandId = "brand_lavazza",
            name = "Qualità Oro",
            slug = "qualita-oro",
            roastLevel = "Medium",
            originSummary = "Central America • Africa",
            description = "Floral aroma and smooth body for everyday espresso or moka.",
            affiliateLinks = emptyList(),
            createdAt = 1_704_067_200_000L,
            updatedAt = 1_704_067_200_000L
        ),
        BrandProduct(
            id = "product_lavazza_rossa",
            brandId = "brand_lavazza",
            name = "Qualità Rossa",
            slug = "qualita-rossa",
            roastLevel = "Medium-Dark",
            originSummary = "South America • Africa",
            description = "Chocolate-forward blend with fuller body and gentle smokiness.",
            affiliateLinks = emptyList(),
            createdAt = 1_704_067_200_000L,
            updatedAt = 1_704_067_200_000L
        ),
        BrandProduct(
            id = "product_illy_classico",
            brandId = "brand_illy",
            name = "Classico",
            slug = "classico",
            roastLevel = "Medium",
            originSummary = "Multi-origin Arabica",
            description = "Silky and balanced cup with caramel and floral tones.",
            affiliateLinks = emptyList(),
            createdAt = 1_704_067_200_000L,
            updatedAt = 1_704_067_200_000L
        ),
        BrandProduct(
            id = "product_illy_intenso",
            brandId = "brand_illy",
            name = "Intenso",
            slug = "intenso",
            roastLevel = "Dark",
            originSummary = "Multi-origin Arabica",
            description = "Bold roast expression with deep cocoa and toasted notes.",
            affiliateLinks = emptyList(),
            createdAt = 1_704_067_200_000L,
            updatedAt = 1_704_067_200_000L
        ),
        BrandProduct(
            id = "product_blue_bottle_bella",
            brandId = "brand_blue_bottle",
            name = "Bella Donovan",
            slug = "bella-donovan",
            roastLevel = "Medium",
            originSummary = "Ethiopia • Sumatra",
            description = "Berry aromatics with chocolate body in a comfort-forward blend.",
            affiliateLinks = emptyList(),
            createdAt = 1_704_067_200_000L,
            updatedAt = 1_704_067_200_000L
        ),
        BrandProduct(
            id = "product_blue_bottle_three_africas",
            brandId = "brand_blue_bottle",
            name = "Three Africas",
            slug = "three-africas",
            roastLevel = "Light",
            originSummary = "Ethiopia • Uganda",
            description = "Fruity and bright profile suited for pour-over lovers.",
            affiliateLinks = emptyList(),
            createdAt = 1_704_067_200_000L,
            updatedAt = 1_704_067_200_000L
        ),
        BrandProduct(
            id = "product_collective_jaen",
            brandId = "brand_coffee_collective",
            name = "Jaén",
            slug = "jaen",
            roastLevel = "Light",
            originSummary = "Peru",
            description = "Stone fruit sweetness with transparent acidity in filter brews.",
            affiliateLinks = emptyList(),
            createdAt = 1_704_067_200_000L,
            updatedAt = 1_704_067_200_000L
        ),
        BrandProduct(
            id = "product_collective_kieni",
            brandId = "brand_coffee_collective",
            name = "Kieni",
            slug = "kieni",
            roastLevel = "Light",
            originSummary = "Kenya",
            description = "Juicy currant cup with sparkling acidity and clean finish.",
            affiliateLinks = emptyList(),
            createdAt = 1_704_067_200_000L,
            updatedAt = 1_704_067_200_000L
        ),
        BrandProduct(
            id = "product_tchibo_baristra_crema",
            brandId = "brand_tchibo",
            name = "Barista Caffè Crema",
            slug = "barista-caffe-crema",
            roastLevel = "Medium",
            originSummary = "Brazil • Honduras",
            description = "Balanced whole bean profile with caramel sweetness.",
            affiliateLinks = emptyList(),
            createdAt = 1_704_067_200_000L,
            updatedAt = 1_704_067_200_000L
        ),
        BrandProduct(
            id = "product_tchibo_espresso_milano",
            brandId = "brand_tchibo",
            name = "Barista Espresso Milano",
            slug = "barista-espresso-milano",
            roastLevel = "Dark",
            originSummary = "Latin America",
            description = "Dense crema and deeper roast notes for milk drinks.",
            affiliateLinks = emptyList(),
            createdAt = 1_704_067_200_000L,
            updatedAt = 1_704_067_200_000L
        ),
        BrandProduct(
            id = "product_mehmet_efendi_turkish",
            brandId = "brand_kurukahveci",
            name = "Turkish Coffee Classic",
            slug = "turkish-coffee-classic",
            roastLevel = "Medium-Dark",
            originSummary = "Brazil • Central America",
            description = "Traditional Turkish style cup with rich aroma and velvety foam.",
            affiliateLinks = emptyList(),
            createdAt = 1_704_067_200_000L,
            updatedAt = 1_704_067_200_000L
        )
    )

    private var reviews: MutableList<Review> = mutableListOf()
    private var brandSuggestions: MutableList<BrandSuggestion> = mutableListOf()
    private var productSuggestions: MutableList<ProductSuggestion> = mutableListOf()
    private var brandActivityById: MutableMap<String, BrandActivitySnapshot> = mutableMapOf()

    fun initialize(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Reviews are now sourced from Firestore. Keep local state empty to avoid stale
        // on-device sentiment leaking into live rating surfaces.
        reviews = mutableListOf()
        brandSuggestions = loadBrandSuggestions().toMutableList()
        productSuggestions = loadProductSuggestions().toMutableList()
        brandActivityById = loadBrandActivity().associateBy { it.brandId }.toMutableMap()
    }

    fun getBrands(searchQuery: String = ""): List<Brand> {
        val discovery = getBrandDiscovery(searchQuery = searchQuery)
        return discovery.sections
            .firstOrNull { it.type == BrandDiscoverySectionType.ALL_BRANDS }
            ?.brands
            .orEmpty()
    }

    fun getBrandDiscovery(
        searchQuery: String = "",
        selectedCategory: BrandCategory? = null,
        discoveryContext: BrandDiscoveryContext = BrandDiscoveryContext()
    ): BrandDiscoveryFeed {
        ensureInitialized()
        val now = System.currentTimeMillis()
        val query = searchQuery.trim().lowercase(Locale.ROOT)
        val enrichedBrands = baseBrands.map(::enrichBrand)
        val filteredBrands = enrichedBrands.filter { brand ->
            matchesCategory(brand, selectedCategory) && matchesSearch(brand, query)
        }
        val organicTopRated = rankTopRated(filteredBrands).take(TOP_RATED_LIMIT)
        val organicTopRatedIds = organicTopRated.map { it.id }.toSet()

        val trendingBrands = rankTrending(
            candidates = filteredBrands.filterNot { it.id in organicTopRatedIds },
            now = now
        ).take(TRENDING_LIMIT)
        val reservedIds = (organicTopRatedIds + trendingBrands.map { it.id }).toSet()

        val featuredBrands = pickFeaturedBrands(
            candidates = filteredBrands,
            excludedBrandIds = reservedIds,
            context = discoveryContext,
            now = now
        )

        val allBrands = rankAllBrands(
            candidates = filteredBrands,
            query = query,
            context = discoveryContext
        )

        val sections = if (query.isNotBlank()) {
            listOf(
                BrandDiscoverySection(
                    type = BrandDiscoverySectionType.ALL_BRANDS,
                    brands = allBrands,
                    isSponsored = false
                )
            )
        } else {
            listOf(
                BrandDiscoverySection(
                    type = BrandDiscoverySectionType.TOP_RATED,
                    brands = organicTopRated,
                    isSponsored = false
                ),
                BrandDiscoverySection(
                    type = BrandDiscoverySectionType.TRENDING,
                    brands = trendingBrands,
                    isSponsored = false
                ),
                BrandDiscoverySection(
                    type = BrandDiscoverySectionType.FEATURED,
                    brands = featuredBrands,
                    isSponsored = true
                ),
                BrandDiscoverySection(
                    type = BrandDiscoverySectionType.ALL_BRANDS,
                    brands = allBrands,
                    isSponsored = false
                )
            )
        }

        val locationLabel = discoveryContext.cityOrArea
            ?: discoveryContext.countryOrRegion

        return BrandDiscoveryFeed(
            sections = sections,
            availableCategories = BrandCategory.entries,
            locationContextLabel = locationLabel
        )
    }

    fun recordBrandViewed(brandId: String) {
        ensureInitialized()
        if (baseBrands.none { it.id == brandId }) return
        val current = brandActivityById[brandId] ?: BrandActivitySnapshot(brandId = brandId)
        val now = System.currentTimeMillis()
        brandActivityById[brandId] = current.copy(
            profileViews = current.profileViews + 1,
            interactions = current.interactions + 1,
            lastViewedAt = now,
            lastInteractionAt = now
        )
        persistBrandActivity()
    }

    fun recordProductViewed(productId: String) {
        ensureInitialized()
        val brandId = baseProducts.firstOrNull { it.id == productId }?.brandId ?: return
        val current = brandActivityById[brandId] ?: BrandActivitySnapshot(brandId = brandId)
        val now = System.currentTimeMillis()
        brandActivityById[brandId] = current.copy(
            interactions = current.interactions + 1,
            lastInteractionAt = now
        )
        persistBrandActivity()
    }

    fun recordBrandSaved(brandId: String) {
        ensureInitialized()
        if (baseBrands.none { it.id == brandId }) return
        val current = brandActivityById[brandId] ?: BrandActivitySnapshot(brandId = brandId)
        val now = System.currentTimeMillis()
        brandActivityById[brandId] = current.copy(
            saves = current.saves + 1,
            lastSavedAt = now,
            lastInteractionAt = now
        )
        persistBrandActivity()
    }

    fun getBrandById(brandId: String): Brand? {
        ensureInitialized()
        val brand = baseBrands.firstOrNull { it.id == brandId } ?: return null
        return enrichBrand(brand)
    }

    fun getProductsByBrand(brandId: String): List<BrandProduct> {
        ensureInitialized()
        return baseProducts
            .filter { it.brandId == brandId }
            .map { enrichProduct(it) }
            .sortedWith(
                compareByDescending<BrandProduct> { it.averageRating }
                    .thenByDescending { it.reviewCount }
                    .thenBy { it.name }
            )
    }

    fun getProductById(productId: String): BrandProduct? {
        ensureInitialized()
        val product = baseProducts.firstOrNull { it.id == productId } ?: return null
        return enrichProduct(product)
    }

    fun getReviews(targetType: ReviewTargetType, targetId: String): List<Review> {
        ensureInitialized()
        return reviews
            .filter { it.targetType == targetType && it.targetId == targetId }
            .sortedByDescending { it.updatedAt }
    }

    fun submitReview(
        userId: String,
        authorDisplayName: String?,
        authorAvatarUrl: String?,
        targetType: ReviewTargetType,
        targetId: String,
        rating: Int,
        comment: String
    ): ReviewSubmissionResult {
        ensureInitialized()
        if (userId.isBlank()) {
            return ReviewSubmissionResult.Failure(ReviewFailureReason.UNAUTHORIZED)
        }
        if (rating !in 1..5) {
            return ReviewSubmissionResult.Failure(ReviewFailureReason.INVALID_RATING)
        }

        val sanitizedComment = comment.trim().replace(Regex("\\s+"), " ")
        if (sanitizedComment.isNotBlank() && sanitizedComment.length < 3) {
            return ReviewSubmissionResult.Failure(ReviewFailureReason.INVALID_COMMENT)
        }

        val targetExists = when (targetType) {
            ReviewTargetType.BRAND -> baseBrands.any { it.id == targetId }
            ReviewTargetType.PRODUCT -> baseProducts.any { it.id == targetId }
            ReviewTargetType.BREW -> PhaseOneRepository.brewingMethods.any { it.id == targetId }
        }
        if (!targetExists) {
            return ReviewSubmissionResult.Failure(ReviewFailureReason.TARGET_NOT_FOUND)
        }

        val now = System.currentTimeMillis()
        val existingIndex = reviews.indexOfFirst {
            it.userId == userId && it.targetType == targetType && it.targetId == targetId
        }

        if (existingIndex >= 0) {
            val existing = reviews[existingIndex]
            reviews[existingIndex] = existing.copy(
                rating = rating,
                comment = sanitizedComment,
                updatedAt = now,
                authorDisplayName = authorDisplayName,
                authorAvatarUrl = authorAvatarUrl
            )
        } else {
            reviews.add(
                Review(
                    id = "review_${targetType.name.lowercase(Locale.ROOT)}_${targetId}_${userId}",
                    userId = userId,
                    targetType = targetType,
                    targetId = targetId,
                    rating = rating,
                    comment = sanitizedComment,
                    createdAt = now,
                    updatedAt = now,
                    authorDisplayName = authorDisplayName,
                    authorAvatarUrl = authorAvatarUrl
                )
            )
        }

        persistReviews()
        return ReviewSubmissionResult.Success
    }

    fun submitBrandSuggestion(
        userId: String,
        name: String,
        country: String,
        description: String
    ): SuggestionSubmissionResult {
        ensureInitialized()
        if (userId.isBlank()) {
            return SuggestionSubmissionResult.Failure(SuggestionFailureReason.UNAUTHORIZED)
        }
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            return SuggestionSubmissionResult.Failure(SuggestionFailureReason.EMPTY_NAME)
        }
        val normalized = normalizeSlug(cleanName)

        if (baseBrands.any { it.slug == normalized || it.name.equals(cleanName, ignoreCase = true) }) {
            return SuggestionSubmissionResult.Failure(SuggestionFailureReason.DUPLICATE_EXISTING)
        }

        if (brandSuggestions.any { it.normalizedName == normalized && it.status == SuggestionStatus.PENDING }) {
            return SuggestionSubmissionResult.Failure(SuggestionFailureReason.DUPLICATE_PENDING)
        }

        brandSuggestions.add(
            BrandSuggestion(
                id = "brand_suggestion_${System.currentTimeMillis()}",
                userId = userId,
                name = cleanName,
                normalizedName = normalized,
                country = country.trim().ifBlank { null },
                description = description.trim().ifBlank { null },
                status = SuggestionStatus.PENDING,
                createdAt = System.currentTimeMillis()
            )
        )
        persistBrandSuggestions()
        return SuggestionSubmissionResult.Success
    }

    fun submitProductSuggestion(
        userId: String,
        brandId: String,
        name: String,
        originSummary: String,
        description: String
    ): SuggestionSubmissionResult {
        ensureInitialized()
        if (userId.isBlank()) {
            return SuggestionSubmissionResult.Failure(SuggestionFailureReason.UNAUTHORIZED)
        }
        if (baseBrands.none { it.id == brandId }) {
            return SuggestionSubmissionResult.Failure(SuggestionFailureReason.BRAND_NOT_FOUND)
        }

        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            return SuggestionSubmissionResult.Failure(SuggestionFailureReason.EMPTY_NAME)
        }

        val normalized = normalizeSlug(cleanName)
        if (baseProducts.any { it.brandId == brandId && it.slug == normalized }) {
            return SuggestionSubmissionResult.Failure(SuggestionFailureReason.DUPLICATE_EXISTING)
        }

        if (productSuggestions.any {
                it.brandId == brandId && it.normalizedName == normalized && it.status == SuggestionStatus.PENDING
            }
        ) {
            return SuggestionSubmissionResult.Failure(SuggestionFailureReason.DUPLICATE_PENDING)
        }

        productSuggestions.add(
            ProductSuggestion(
                id = "product_suggestion_${System.currentTimeMillis()}",
                userId = userId,
                brandId = brandId,
                name = cleanName,
                normalizedName = normalized,
                originSummary = originSummary.trim().ifBlank { null },
                description = description.trim().ifBlank { null },
                status = SuggestionStatus.PENDING,
                createdAt = System.currentTimeMillis()
            )
        )
        persistProductSuggestions()
        return SuggestionSubmissionResult.Success
    }

    private fun matchesCategory(brand: Brand, category: BrandCategory?): Boolean {
        return category == null || brand.category == category
    }

    private fun matchesSearch(brand: Brand, query: String): Boolean {
        if (query.isBlank()) return true
        return brand.name.lowercase(Locale.ROOT).contains(query) ||
            brand.country.orEmpty().lowercase(Locale.ROOT).contains(query) ||
            brand.cityOrArea.orEmpty().lowercase(Locale.ROOT).contains(query) ||
            brand.description.lowercase(Locale.ROOT).contains(query) ||
            baseProducts.any { product ->
                product.brandId == brand.id && product.name.lowercase(Locale.ROOT).contains(query)
            }
    }

    private fun rankTopRated(candidates: List<Brand>): List<Brand> {
        val byBrandReviews = reviews
            .filter { it.targetType == ReviewTargetType.BRAND }
            .groupBy { it.targetId }
        return candidates
            .filter { it.reviewCount >= TOP_RATED_MIN_REVIEWS }
            .sortedWith(
                compareByDescending<Brand> { brand ->
                    val reviewSet = byBrandReviews[brand.id].orEmpty()
                    val normalizedRating = (brand.averageRating / 5.0).coerceIn(0.0, 1.0)
                    val volumeScore = (ln((brand.reviewCount + 1).toDouble()) / ln(18.0)).coerceIn(0.0, 1.0)
                    val qualityScore = qualityRatio(reviewSet)
                    normalizedRating * 0.68 + volumeScore * 0.22 + qualityScore * 0.10
                }
                    .thenByDescending { it.averageRating }
                    .thenByDescending { it.reviewCount }
                    .thenBy { it.name }
            )
    }

    private fun rankTrending(
        candidates: List<Brand>,
        now: Long
    ): List<Brand> {
        val reviewsByBrand = buildMap<String, List<Review>> {
            val directBrandReviews = reviews
                .filter { it.targetType == ReviewTargetType.BRAND }
                .groupBy { it.targetId }
            val productReviewsByBrand = reviews
                .filter { it.targetType == ReviewTargetType.PRODUCT }
                .groupBy { review ->
                    baseProducts.firstOrNull { it.id == review.targetId }?.brandId.orEmpty()
                }
                .filterKeys { it.isNotBlank() }
            baseBrands.forEach { brand ->
                put(
                    brand.id,
                    directBrandReviews[brand.id].orEmpty() + productReviewsByBrand[brand.id].orEmpty()
                )
            }
        }

        return candidates
            .filter { brand ->
                val activity = brandActivityById[brand.id]
                val hasRecentReview = reviewsByBrand[brand.id].orEmpty().any { now - it.updatedAt <= TRENDING_DECAY_WINDOW_MS * 3 }
                val hasInteraction = activity != null && (activity.interactions > 0 || activity.profileViews > 0 || activity.saves > 0)
                hasRecentReview || hasInteraction
            }
            .sortedWith(
                compareByDescending<Brand> { brand ->
                    val activity = brandActivityById[brand.id]
                    val relatedReviews = reviewsByBrand[brand.id].orEmpty()
                    val reviewSignal = relatedReviews.sumOf { review ->
                        recentDecay(now = now, timestamp = review.updatedAt) * 2.2
                    }
                    val visitSignal = activity?.let {
                        it.profileViews * recentDecay(now = now, timestamp = it.lastViewedAt) * 0.85
                    } ?: 0.0
                    val saveSignal = activity?.let {
                        it.saves * recentDecay(now = now, timestamp = it.lastSavedAt) * 1.15
                    } ?: 0.0
                    val interactionSignal = activity?.let {
                        it.interactions * recentDecay(now = now, timestamp = it.lastInteractionAt) * 0.65
                    } ?: 0.0
                    reviewSignal + visitSignal + saveSignal + interactionSignal
                }
                    .thenByDescending { it.averageRating }
                    .thenByDescending { it.reviewCount }
                    .thenBy { it.name }
            )
    }

    private fun pickFeaturedBrands(
        candidates: List<Brand>,
        excludedBrandIds: Set<String>,
        context: BrandDiscoveryContext,
        now: Long
    ): List<Brand> {
        val eligible = candidates.filter { it.visibilityTier == BrandVisibilityTier.FEATURED_SUBSCRIBER }
        if (eligible.isEmpty()) return emptyList()

        val prioritized = eligible.sortedWith(
            compareByDescending<Brand> { locationBoost(it, context) }
                .thenByDescending { it.averageRating }
                .thenByDescending { it.reviewCount }
                .thenBy { it.name }
        )
        val preferredPool = prioritized.filterNot { it.id in excludedBrandIds }
        val fallbackPool = prioritized.filter { it.id in excludedBrandIds }
        val combinedPool = preferredPool + fallbackPool
        val rotated = rotateFeaturedPool(combinedPool, context, now)
        return rotated.take(FEATURED_SLOT_LIMIT)
    }

    private fun rotateFeaturedPool(
        pool: List<Brand>,
        context: BrandDiscoveryContext,
        now: Long
    ): List<Brand> {
        if (pool.isEmpty()) return emptyList()
        val windowIndex = now / FEATURED_ROTATION_WINDOW_MS
        val contextSeed = normalizeToken("${context.cityOrArea.orEmpty()}|${context.countryOrRegion.orEmpty()}").hashCode()
        val startIndex = Math.floorMod((windowIndex + contextSeed).toInt(), pool.size)
        return List(pool.size) { offset ->
            pool[(startIndex + offset) % pool.size]
        }
    }

    private fun rankAllBrands(
        candidates: List<Brand>,
        query: String,
        context: BrandDiscoveryContext
    ): List<Brand> {
        return candidates.sortedWith(
            compareByDescending<Brand> { queryRelevance(it, query) }
                .thenByDescending { locationBoost(it, context) }
                .thenByDescending { it.averageRating }
                .thenByDescending { it.reviewCount }
                .thenBy { it.name }
        )
    }

    private fun queryRelevance(brand: Brand, query: String): Int {
        if (query.isBlank()) return 0
        val normalizedName = brand.name.lowercase(Locale.ROOT)
        val normalizedCountry = brand.country.orEmpty().lowercase(Locale.ROOT)
        val normalizedCity = brand.cityOrArea.orEmpty().lowercase(Locale.ROOT)
        return when {
            normalizedName == query -> 120
            normalizedName.startsWith(query) -> 96
            normalizedName.contains(query) -> 80
            normalizedCity.contains(query) -> 68
            normalizedCountry.contains(query) -> 52
            baseProducts.any {
                it.brandId == brand.id && it.name.lowercase(Locale.ROOT).contains(query)
            } -> 44
            else -> 0
        }
    }

    private fun locationBoost(brand: Brand, context: BrandDiscoveryContext): Int {
        val cityMatch = !context.cityOrArea.isNullOrBlank() &&
            normalizeToken(brand.cityOrArea) == normalizeToken(context.cityOrArea)
        if (cityMatch) return 120

        val countryMatch = !context.countryOrRegion.isNullOrBlank() &&
            normalizeToken(brand.country) == normalizeToken(context.countryOrRegion)
        if (countryMatch) return 80

        return 24
    }

    private fun qualityRatio(reviewSet: List<Review>): Double {
        if (reviewSet.isEmpty()) return 0.0
        val meaningful = reviewSet.count { review ->
            review.comment.trim().length >= 20
        }
        return (meaningful.toDouble() / reviewSet.size.toDouble()).coerceIn(0.0, 1.0)
    }

    private fun recentDecay(now: Long, timestamp: Long): Double {
        if (timestamp <= 0L || now <= timestamp) return 1.0
        val ageRatio = (now - timestamp).toDouble() / TRENDING_DECAY_WINDOW_MS.toDouble()
        return exp(-ageRatio).coerceIn(0.0, 1.0)
    }

    private fun enrichBrand(brand: Brand): Brand {
        val ratingReviews = reviews.filter {
            it.targetType == ReviewTargetType.BRAND && it.targetId == brand.id
        }
        return brand.copy(
            averageRating = ratingReviews.averageRating(),
            reviewCount = ratingReviews.size,
            productCount = baseProducts.count { it.brandId == brand.id },
            updatedAt = maxOf(brand.updatedAt, ratingReviews.maxOfOrNull { it.updatedAt } ?: brand.updatedAt)
        )
    }

    private fun enrichProduct(product: BrandProduct): BrandProduct {
        val ratingReviews = reviews.filter {
            it.targetType == ReviewTargetType.PRODUCT && it.targetId == product.id
        }
        return product.copy(
            averageRating = ratingReviews.averageRating(),
            reviewCount = ratingReviews.size,
            updatedAt = maxOf(product.updatedAt, ratingReviews.maxOfOrNull { it.updatedAt } ?: product.updatedAt)
        )
    }

    private fun List<Review>.averageRating(): Double {
        if (isEmpty()) return 0.0
        val avg = map { it.rating }.average()
        return ((avg * 10.0).roundToInt() / 10.0)
    }

    private fun loadReviews(): List<Review> {
        val raw = prefs.getString(KEY_REVIEWS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id")
                    val userId = item.optString("userId")
                    val targetId = item.optString("targetId")
                    val rating = item.optInt("rating", 0)
                    val targetType = item.optString("targetType")
                        .let { value -> ReviewTargetType.entries.firstOrNull { it.name == value } }
                        ?: continue
                    if (id.isBlank() || userId.isBlank() || targetId.isBlank()) continue
                    if (rating !in 1..5) continue

                    add(
                        Review(
                            id = id,
                            userId = userId,
                            targetType = targetType,
                            targetId = targetId,
                            rating = rating,
                            comment = item.optString("comment"),
                            createdAt = item.optLong("createdAt", 0L),
                            updatedAt = item.optLong("updatedAt", 0L),
                            authorDisplayName = item.optString("authorDisplayName").ifBlank { null },
                            authorAvatarUrl = item.optString("authorAvatarUrl").ifBlank { null }
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun loadBrandSuggestions(): List<BrandSuggestion> {
        val raw = prefs.getString(KEY_BRAND_SUGGESTIONS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id")
                    val userId = item.optString("userId")
                    val name = item.optString("name")
                    val normalized = item.optString("normalizedName")
                    val status = item.optString("status")
                        .let { value -> SuggestionStatus.entries.firstOrNull { it.name == value } }
                        ?: SuggestionStatus.PENDING
                    if (id.isBlank() || userId.isBlank() || name.isBlank() || normalized.isBlank()) continue

                    add(
                        BrandSuggestion(
                            id = id,
                            userId = userId,
                            name = name,
                            normalizedName = normalized,
                            country = item.optString("country").ifBlank { null },
                            description = item.optString("description").ifBlank { null },
                            status = status,
                            createdAt = item.optLong("createdAt", 0L)
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun loadProductSuggestions(): List<ProductSuggestion> {
        val raw = prefs.getString(KEY_PRODUCT_SUGGESTIONS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id")
                    val userId = item.optString("userId")
                    val brandId = item.optString("brandId")
                    val name = item.optString("name")
                    val normalized = item.optString("normalizedName")
                    val status = item.optString("status")
                        .let { value -> SuggestionStatus.entries.firstOrNull { it.name == value } }
                        ?: SuggestionStatus.PENDING
                    if (id.isBlank() || userId.isBlank() || brandId.isBlank() || name.isBlank() || normalized.isBlank()) continue

                    add(
                        ProductSuggestion(
                            id = id,
                            userId = userId,
                            brandId = brandId,
                            name = name,
                            normalizedName = normalized,
                            originSummary = item.optString("originSummary").ifBlank { null },
                            description = item.optString("description").ifBlank { null },
                            status = status,
                            createdAt = item.optLong("createdAt", 0L)
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun loadBrandActivity(): List<BrandActivitySnapshot> {
        val raw = prefs.getString(KEY_BRAND_ACTIVITY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val brandId = item.optString("brandId")
                    if (brandId.isBlank()) continue
                    if (baseBrands.none { it.id == brandId }) continue

                    add(
                        BrandActivitySnapshot(
                            brandId = brandId,
                            profileViews = item.optInt("profileViews", 0).coerceAtLeast(0),
                            saves = item.optInt("saves", 0).coerceAtLeast(0),
                            interactions = item.optInt("interactions", 0).coerceAtLeast(0),
                            lastViewedAt = item.optLong("lastViewedAt", 0L),
                            lastSavedAt = item.optLong("lastSavedAt", 0L),
                            lastInteractionAt = item.optLong("lastInteractionAt", 0L)
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun persistReviews() {
        val array = JSONArray()
        reviews.forEach { review ->
            array.put(
                JSONObject().apply {
                    put("id", review.id)
                    put("userId", review.userId)
                    put("targetType", review.targetType.name)
                    put("targetId", review.targetId)
                    put("rating", review.rating)
                    put("comment", review.comment)
                    put("createdAt", review.createdAt)
                    put("updatedAt", review.updatedAt)
                    put("authorDisplayName", review.authorDisplayName)
                    put("authorAvatarUrl", review.authorAvatarUrl)
                }
            )
        }
        prefs.edit().putString(KEY_REVIEWS, array.toString()).apply()
    }

    private fun persistBrandSuggestions() {
        val array = JSONArray()
        brandSuggestions.forEach { suggestion ->
            array.put(
                JSONObject().apply {
                    put("id", suggestion.id)
                    put("userId", suggestion.userId)
                    put("name", suggestion.name)
                    put("normalizedName", suggestion.normalizedName)
                    put("country", suggestion.country)
                    put("description", suggestion.description)
                    put("status", suggestion.status.name)
                    put("createdAt", suggestion.createdAt)
                }
            )
        }
        prefs.edit().putString(KEY_BRAND_SUGGESTIONS, array.toString()).apply()
    }

    private fun persistProductSuggestions() {
        val array = JSONArray()
        productSuggestions.forEach { suggestion ->
            array.put(
                JSONObject().apply {
                    put("id", suggestion.id)
                    put("userId", suggestion.userId)
                    put("brandId", suggestion.brandId)
                    put("name", suggestion.name)
                    put("normalizedName", suggestion.normalizedName)
                    put("originSummary", suggestion.originSummary)
                    put("description", suggestion.description)
                    put("status", suggestion.status.name)
                    put("createdAt", suggestion.createdAt)
                }
            )
        }
        prefs.edit().putString(KEY_PRODUCT_SUGGESTIONS, array.toString()).apply()
    }

    private fun persistBrandActivity() {
        val array = JSONArray()
        brandActivityById.values
            .filter { activity -> baseBrands.any { it.id == activity.brandId } }
            .forEach { activity ->
                array.put(
                    JSONObject().apply {
                        put("brandId", activity.brandId)
                        put("profileViews", activity.profileViews)
                        put("saves", activity.saves)
                        put("interactions", activity.interactions)
                        put("lastViewedAt", activity.lastViewedAt)
                        put("lastSavedAt", activity.lastSavedAt)
                        put("lastInteractionAt", activity.lastInteractionAt)
                    }
                )
        }
        prefs.edit().putString(KEY_BRAND_ACTIVITY, array.toString()).apply()
    }

    private fun normalizeToken(value: String?): String {
        if (value.isNullOrBlank()) return ""
        return value
            .trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "")
    }

    private fun normalizeSlug(value: String): String {
        val normalized = value
            .trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        return normalized.ifBlank { "item" }
    }

    private fun ensureInitialized() {
        check(::prefs.isInitialized) {
            "BrandRepository.initialize(context) must be called before use."
        }
    }
}
