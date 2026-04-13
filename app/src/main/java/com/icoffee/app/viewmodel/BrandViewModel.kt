package com.icoffee.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.icoffee.app.data.BrandRepository
import com.icoffee.app.data.MeetRepository
import com.icoffee.app.data.admin.BrandSuggestionDraft
import com.icoffee.app.data.admin.BrandSuggestionSubmissionResult
import com.icoffee.app.data.admin.BrandManagementRepository
import com.icoffee.app.data.auth.FirebaseAuthRepository
import com.icoffee.app.data.firebase.model.FirestoreBrand
import com.icoffee.app.data.firebase.model.FirestoreProduct
import com.icoffee.app.data.firebase.repository.FirestoreBrandsRepository
import com.icoffee.app.data.firebase.repository.FirestoreProductsRepository
import com.icoffee.app.data.importer.FirestoreProductImportRepository
import com.icoffee.app.data.importer.ProductUrlImportRepository
import com.icoffee.app.data.model.Brand
import com.icoffee.app.data.model.BrandCategory
import com.icoffee.app.data.model.BrandDiscoveryContext
import com.icoffee.app.data.model.BrandDiscoveryFeed
import com.icoffee.app.data.model.BrandDiscoverySectionType
import com.icoffee.app.data.model.BrandLifecycleStatus
import com.icoffee.app.data.model.BrandProduct
import com.icoffee.app.data.model.Review
import com.icoffee.app.data.model.ReviewTargetType
import com.icoffee.app.data.model.importer.ProductImportDraft
import com.icoffee.app.data.model.importer.ProductImportPreviewResult
import com.icoffee.app.data.model.importer.ProductImportSaveFailureReason
import com.icoffee.app.data.model.importer.ProductImportSaveResult
import com.icoffee.app.data.review.FirestoreReviewRepository
import java.util.Locale
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class BrandViewModel : ViewModel() {

    var searchQuery by mutableStateOf("")
        private set

    var selectedCategory by mutableStateOf<BrandCategory?>(null)
        private set

    var refreshVersion by mutableIntStateOf(0)
        private set

    var isCatalogLoading by mutableStateOf(false)
        private set

    private var firestoreBrands by mutableStateOf<List<Brand>>(emptyList())
    private var firestoreProductsByBrand by mutableStateOf<Map<String, List<BrandProduct>>>(emptyMap())
    private var firestoreProductById by mutableStateOf<Map<String, BrandProduct>>(emptyMap())
    private var reviewsByTargetKey by mutableStateOf<Map<String, List<Review>>>(emptyMap())

    private val productUrlImportRepository = ProductUrlImportRepository()

    private val currentUser
        get() = FirebaseAuthRepository.currentUser

    val isUserSignedIn: Boolean
        get() = currentUser != null

    val currentUserId: String
        get() = currentUser?.uid.orEmpty()

    init {
        viewModelScope.launch {
            refreshBrandCatalog()
        }
    }

    fun updateSearchQuery(value: String) {
        searchQuery = value
    }

    fun updateSelectedCategory(value: BrandCategory?) {
        selectedCategory = value
    }

    fun brandDiscovery(): BrandDiscoveryFeed {
        refreshVersion
        val discoveryContext = currentDiscoveryContext()
        val query = searchQuery.trim().lowercase(Locale.ROOT)
        val filteredBrands = firestoreBrands.filter { brand ->
            val matchesCategory = selectedCategory == null || brand.category == selectedCategory
            val matchesSearch = query.isBlank() || matchesBrandQuery(brand, query)
            val isPublic = brand.status in PUBLIC_BRAND_STATUSES
            matchesCategory && matchesSearch && isPublic
        }

        val topRated = filteredBrands
            .filter { it.reviewCount > 0 }
            .sortedWith(
                compareByDescending<Brand> { it.averageRating }
                    .thenByDescending { it.reviewCount }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
            )
            .take(8)

        val trendingBase = filteredBrands
            .sortedWith(
                compareByDescending<Brand> { it.updatedAt }
                    .thenByDescending { it.reviewCount }
                    .thenByDescending { it.averageRating }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
            )

        val featuredBase = filteredBrands
            .filter { it.visibilityTier == com.icoffee.app.data.model.BrandVisibilityTier.FEATURED_SUBSCRIBER }
            .sortedWith(
                compareByDescending<Brand> { it.reviewCount }
                    .thenByDescending { it.averageRating }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
            )

        val topRatedIds = topRated.map { it.id }.toSet()
        val trending = pickSectionWithDedup(
            base = trendingBase,
            alreadyShown = topRatedIds,
            limit = 8
        )
        val shownForFeatured = topRatedIds + trending.map { it.id }.toSet()
        val featured = pickSectionWithDedup(
            base = featuredBase,
            alreadyShown = shownForFeatured,
            limit = 8
        )

        val allBrands = filteredBrands.sortedWith(
            compareBy<Brand> { if (query.isBlank()) 1 else 0 }
                .thenByDescending { queryRelevance(it, query) }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )

        val sections = if (query.isNotBlank()) {
            listOf(
                com.icoffee.app.data.model.BrandDiscoverySection(
                    type = BrandDiscoverySectionType.ALL_BRANDS,
                    brands = allBrands
                )
            )
        } else {
            listOfNotNull(
                if (topRated.isNotEmpty()) {
                    com.icoffee.app.data.model.BrandDiscoverySection(
                        type = BrandDiscoverySectionType.TOP_RATED,
                        brands = topRated
                    )
                } else null,
                if (trending.isNotEmpty()) {
                    com.icoffee.app.data.model.BrandDiscoverySection(
                        type = BrandDiscoverySectionType.TRENDING,
                        brands = trending
                    )
                } else null,
                if (featured.isNotEmpty()) {
                    com.icoffee.app.data.model.BrandDiscoverySection(
                        type = BrandDiscoverySectionType.FEATURED,
                        brands = featured,
                        isSponsored = true
                    )
                } else null,
                com.icoffee.app.data.model.BrandDiscoverySection(
                    type = BrandDiscoverySectionType.ALL_BRANDS,
                    brands = allBrands
                )
            )
        }

        val availableCategories = firestoreBrands
            .map { it.category }
            .distinct()
            .ifEmpty { BrandCategory.entries }

        return BrandDiscoveryFeed(
            sections = sections,
            availableCategories = availableCategories,
            locationContextLabel = discoveryContext.cityOrArea ?: discoveryContext.countryOrRegion
        )
    }

    fun brands(): List<Brand> {
        return brandDiscovery().sections
            .firstOrNull { it.type == BrandDiscoverySectionType.ALL_BRANDS }
            ?.brands
            .orEmpty()
    }

    fun brandById(brandId: String): Brand? {
        refreshVersion
        return firestoreBrands.firstOrNull { it.id == brandId }
    }

    fun productById(productId: String): BrandProduct? {
        refreshVersion
        return firestoreProductById[productId]
    }

    fun productsByBrand(brandId: String): List<BrandProduct> {
        refreshVersion
        return firestoreProductsByBrand[brandId].orEmpty()
    }

    suspend fun refreshBrandCatalog() {
        isCatalogLoading = true
        val brands = loadFirestoreBrands()
        val products = loadFirestoreProducts()
        val groupedProducts = products
            .groupBy { it.brandId }
            .mapValues { (_, value) ->
                value.sortedWith(
                    compareByDescending<BrandProduct> { it.reviewCount }
                        .thenByDescending { it.averageRating }
                        .thenBy { it.name.lowercase(Locale.ROOT) }
                )
            }
        val productByIdMap = products.associateBy { it.id }

        firestoreProductsByBrand = groupedProducts
        firestoreProductById = productByIdMap
        firestoreBrands = brands
            .map { brand ->
                val computedProductCount = groupedProducts[brand.id]?.size ?: 0
                brand.copy(productCount = maxOf(brand.productCount, computedProductCount))
            }
            .sortedBy { it.name.lowercase(Locale.ROOT) }
        isCatalogLoading = false
        refreshVersion += 1
    }

    suspend fun ensureBrandLoaded(brandId: String) {
        if (brandId.isBlank()) return
        if (firestoreBrands.any { it.id == brandId }) return
        val fetched = FirestoreBrandsRepository.getById(brandId).getOrNull()?.toDomainBrand() ?: return
        firestoreBrands = (firestoreBrands + fetched).distinctBy { it.id }
            .sortedBy { it.name.lowercase(Locale.ROOT) }
        if (!firestoreProductsByBrand.containsKey(brandId)) {
            val fetchedProducts = FirestoreProductsRepository.listByBrand(brandId)
                .getOrDefault(emptyList())
                .map { it.toDomainProduct() }
            if (fetchedProducts.isNotEmpty()) {
                firestoreProductsByBrand = firestoreProductsByBrand.toMutableMap().apply {
                    put(brandId, fetchedProducts)
                }
                firestoreProductById = (firestoreProductById + fetchedProducts.associateBy { it.id })
            }
        }
        refreshVersion += 1
    }

    suspend fun ensureProductLoaded(productId: String) {
        if (productId.isBlank()) return
        if (firestoreProductById.containsKey(productId)) return
        val fetched = FirestoreProductsRepository.getById(productId).getOrNull()?.toDomainProduct() ?: return
        firestoreProductById = firestoreProductById + (fetched.id to fetched)
        firestoreProductsByBrand = firestoreProductsByBrand.toMutableMap().apply {
            val current = get(fetched.brandId).orEmpty()
            put(fetched.brandId, (current + fetched).distinctBy { it.id })
        }
        if (!firestoreBrands.any { it.id == fetched.brandId }) {
            ensureBrandLoaded(fetched.brandId)
        } else {
            refreshVersion += 1
        }
    }

    fun reviews(targetType: ReviewTargetType, targetId: String): List<Review> {
        return reviewsByTargetKey[reviewKey(targetType, targetId)].orEmpty()
    }

    fun currentUserReview(targetType: ReviewTargetType, targetId: String): Review? {
        val userId = currentUserId
        if (userId.isBlank()) return null
        return reviews(targetType, targetId).firstOrNull { it.userId == userId }
    }

    fun reviewAggregate(
        targetType: ReviewTargetType,
        targetId: String,
        fallbackAverageRating: Double = 0.0,
        fallbackReviewCount: Int = 0
    ): Pair<Double, Int> {
        val key = reviewKey(targetType, targetId)
        val targetReviews = reviewsByTargetKey[key] ?: return fallbackAverageRating to fallbackReviewCount
        if (targetReviews.isEmpty()) return 0.0 to 0
        val average = targetReviews.map { it.rating }.average()
        val roundedAverage = ((average * 10.0).roundToInt() / 10.0)
        return roundedAverage to targetReviews.size
    }

    suspend fun refreshReviews(targetType: ReviewTargetType, targetId: String) {
        val key = reviewKey(targetType, targetId)
        val loaded = FirestoreReviewRepository.listReviews(targetType = targetType, targetId = targetId)
        reviewsByTargetKey = reviewsByTargetKey.toMutableMap().apply {
            put(key, loaded)
        }
    }

    fun onBrandOpened(brandId: String) {
        viewModelScope.launch {
            ensureBrandLoaded(brandId)
        }
    }

    fun onProductOpened(productId: String) {
        viewModelScope.launch {
            ensureProductLoaded(productId)
        }
    }

    suspend fun submitReview(
        targetType: ReviewTargetType,
        targetId: String,
        rating: Int,
        comment: String
    ): BrandRepository.ReviewSubmissionResult {
        val result = FirestoreReviewRepository.submitReview(
            userId = currentUserId,
            targetType = targetType,
            targetId = targetId,
            rating = rating,
            comment = comment
        )
        if (result is BrandRepository.ReviewSubmissionResult.Success) {
            refreshReviews(targetType = targetType, targetId = targetId)
            refreshVersion += 1
        }
        return result
    }

    suspend fun submitBrandSuggestion(
        name: String,
        website: String,
        instagram: String,
        country: String,
        city: String,
        note: String
    ): BrandSuggestionSubmissionResult {
        val result = BrandManagementRepository.submitBrandSuggestion(
            actorUserId = currentUserId,
            draft = BrandSuggestionDraft(
                brandName = name,
                website = website,
                instagram = instagram,
                country = country,
                city = city,
                note = note
            )
        )
        if (result is BrandSuggestionSubmissionResult.Success) {
            refreshVersion += 1
        }
        return result
    }

    fun submitProductSuggestion(
        brandId: String,
        name: String,
        originSummary: String,
        description: String
    ): BrandRepository.SuggestionSubmissionResult {
        val result = BrandRepository.submitProductSuggestion(
            userId = currentUserId,
            brandId = brandId,
            name = name,
            originSummary = originSummary,
            description = description
        )
        if (result is BrandRepository.SuggestionSubmissionResult.Success) {
            refreshVersion += 1
        }
        return result
    }

    suspend fun importProductFromUrl(url: String): ProductImportPreviewResult {
        return productUrlImportRepository.importFromUrl(url)
    }

    suspend fun saveImportedProduct(draft: ProductImportDraft): ProductImportSaveResult {
        if (currentUserId.isBlank()) {
            return ProductImportSaveResult.Failure(ProductImportSaveFailureReason.UNAUTHORIZED)
        }
        val result = FirestoreProductImportRepository.saveImportedProduct(
            actorUserId = currentUserId,
            draft = draft
        )
        if (result is ProductImportSaveResult.Success) {
            refreshVersion += 1
        }
        return result
    }

    suspend fun canManageBrand(brandId: String): Boolean {
        if (brandId.isBlank()) return false
        return BrandManagementRepository.canCurrentUserManageBrand(brandId)
    }

    private fun currentDiscoveryContext(): BrandDiscoveryContext {
        val cityOrArea = MeetRepository.currentUserCityOrAreaHint()
        val displayLocale = Locale.getDefault()
        val region = Locale.getDefault().country
            .takeIf { it.isNotBlank() }
            ?.let { countryCode ->
                Locale("", countryCode).getDisplayCountry(displayLocale).ifBlank { null }
            }
        return BrandDiscoveryContext(
            cityOrArea = cityOrArea,
            countryOrRegion = region
        )
    }

    private fun reviewKey(
        targetType: ReviewTargetType,
        targetId: String
    ): String = "${targetType.name}:${targetId.trim()}"

    private suspend fun loadFirestoreBrands(): List<Brand> {
        val source = FirestoreBrandsRepository.listPublic(limit = 1000)
            .getOrDefault(emptyList())
        return source.map { it.toDomainBrand() }
    }

    private suspend fun loadFirestoreProducts(): List<BrandProduct> {
        return FirestoreProductsRepository.list(limit = 4000)
            .getOrDefault(emptyList())
            .map { it.toDomainProduct() }
    }

    private fun matchesBrandQuery(brand: Brand, query: String): Boolean {
        if (query.isBlank()) return true
        if (brand.name.lowercase(Locale.ROOT).contains(query)) return true
        if (brand.country.orEmpty().lowercase(Locale.ROOT).contains(query)) return true
        if (brand.cityOrArea.orEmpty().lowercase(Locale.ROOT).contains(query)) return true
        if (brand.description.lowercase(Locale.ROOT).contains(query)) return true
        return productsByBrand(brand.id).any { product ->
            product.name.lowercase(Locale.ROOT).contains(query)
        }
    }

    private fun queryRelevance(brand: Brand, query: String): Int {
        if (query.isBlank()) return 0
        val name = brand.name.lowercase(Locale.ROOT)
        val country = brand.country.orEmpty().lowercase(Locale.ROOT)
        val city = brand.cityOrArea.orEmpty().lowercase(Locale.ROOT)
        return when {
            name == query -> 100
            name.startsWith(query) -> 80
            name.contains(query) -> 60
            city.contains(query) -> 40
            country.contains(query) -> 30
            productsByBrand(brand.id).any { it.name.lowercase(Locale.ROOT).contains(query) } -> 20
            else -> 0
        }
    }

    private fun pickSectionWithDedup(
        base: List<Brand>,
        alreadyShown: Set<String>,
        limit: Int
    ): List<Brand> {
        if (base.isEmpty()) return emptyList()
        val unseen = base.filterNot { it.id in alreadyShown }
        if (unseen.size >= minOf(3, limit)) {
            return unseen.take(limit)
        }
        return (unseen + base)
            .distinctBy { it.id }
            .take(limit)
    }

    private fun FirestoreBrand.toDomainBrand(): Brand {
        return Brand(
            id = id,
            name = name,
            slug = slug.ifBlank { id },
            country = country.ifBlank { null },
            cityOrArea = city.ifBlank { null },
            category = category.toDomainCategory(),
            visibilityTier = if (featured) {
                com.icoffee.app.data.model.BrandVisibilityTier.FEATURED_SUBSCRIBER
            } else {
                com.icoffee.app.data.model.BrandVisibilityTier.ORGANIC
            },
            description = description.ifBlank { "" },
            logoUrl = logoUrl,
            website = website,
            instagram = instagram,
            sourceUrl = sourceUrl,
            status = BrandLifecycleStatus.fromStorage(status).storageValue,
            ownerUserId = ownerUserId,
            ownerEmail = ownerEmail,
            managedByUserIds = managedByUserIds,
            averageRating = averageRating,
            reviewCount = reviewCount,
            productCount = productCount,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun FirestoreProduct.toDomainProduct(): BrandProduct {
        return BrandProduct(
            id = id,
            brandId = brandId,
            name = name,
            slug = slug.ifBlank { id },
            roastLevel = roastLevel.ifBlank { null },
            originSummary = origin.ifBlank { null },
            description = description.ifBlank { null },
            imageUrl = imageUrl,
            averageRating = averageRating,
            reviewCount = reviewCount,
            affiliateLinks = emptyList(),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun String.toDomainCategory(): BrandCategory {
        val normalized = trim().lowercase(Locale.ROOT)
        return when (normalized) {
            "specialty", "specialty_coffee" -> BrandCategory.SPECIALTY
            "local", "local_roasters", "local_roaster" -> BrandCategory.LOCAL_ROASTERS
            "chain", "chains" -> BrandCategory.CHAINS
            "commercial", "commercial_brands" -> BrandCategory.COMMERCIAL
            else -> BrandCategory.SPECIALTY
        }
    }

    private companion object {
        val PUBLIC_BRAND_STATUSES = setOf(
            BrandLifecycleStatus.ACTIVE.storageValue,
            BrandLifecycleStatus.CLAIMED.storageValue,
            BrandLifecycleStatus.BUSINESS.storageValue
        )
    }
}
