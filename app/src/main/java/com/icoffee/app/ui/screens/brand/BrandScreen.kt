package com.icoffee.app.ui.screens.brand

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.icoffee.app.R
import com.icoffee.app.data.model.Brand
import com.icoffee.app.data.model.BrandCategory
import com.icoffee.app.data.model.BrandDiscoverySectionType
import com.icoffee.app.data.model.displayRating
import com.icoffee.app.localization.AppLocaleManager
import com.icoffee.app.ui.components.CoffeeEmptyStateCard
import com.icoffee.app.viewmodel.BrandViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandScreen(
    onBrandClick: (String) -> Unit,
    onSuggestBrand: () -> Unit,
    onOpenMySuggestions: () -> Unit,
    viewModel: BrandViewModel = viewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.refreshBrandCatalog()
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    viewModel.refreshBrandCatalog()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val discovery = viewModel.brandDiscovery()
    val sectionsByType = discovery.sections.associateBy { it.type }
    val allBrands = sectionsByType[BrandDiscoverySectionType.ALL_BRANDS]?.brands.orEmpty()
    val isSearching = viewModel.searchQuery.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E120D),
                        Color(0xFF2A1912),
                        Color(0xFF1E120D)
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = 156.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = stringResource(R.string.brand_title),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFF8E6D1)
                    )
                    Text(
                        text = stringResource(R.string.brand_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xDCD9BBA0)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xBFD9B89B)
                        )
                    },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.brand_search_placeholder),
                            color = Color(0xBFBFA18A)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0x8C3A2419),
                        focusedContainerColor = Color(0xA13A2419),
                        focusedBorderColor = Color(0x88E5C49D),
                        unfocusedBorderColor = Color(0x40E5C49D),
                        focusedTextColor = Color(0xFFF7E6D3),
                        unfocusedTextColor = Color(0xFFF7E6D3),
                        cursorColor = Color(0xFFE5C49D)
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
            }

            item {
                BrandCategoryChips(
                    selectedCategory = viewModel.selectedCategory,
                    availableCategories = discovery.availableCategories,
                    onCategorySelected = viewModel::updateSelectedCategory
                )
            }

            if (!isSearching) {
                item {
                    discovery.locationContextLabel?.let { locationLabel ->
                        Text(
                            text = stringResource(R.string.brand_localized_for_you, locationLabel),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xBFD9B89B)
                        )
                    }
                }

                val topRated = sectionsByType[BrandDiscoverySectionType.TOP_RATED]?.brands.orEmpty()
                if (topRated.isNotEmpty()) {
                    item {
                        BrandHorizontalSection(
                            title = stringResource(R.string.brand_section_top_rated),
                            subtitle = stringResource(R.string.brand_section_top_rated_subtitle),
                            brands = topRated,
                            onBrandClick = onBrandClick
                        )
                    }
                }

                val trending = sectionsByType[BrandDiscoverySectionType.TRENDING]?.brands.orEmpty()
                if (trending.isNotEmpty()) {
                    item {
                        BrandHorizontalSection(
                            title = stringResource(R.string.brand_section_trending),
                            subtitle = stringResource(R.string.brand_section_trending_subtitle),
                            brands = trending,
                            onBrandClick = onBrandClick
                        )
                    }
                }

                val featured = sectionsByType[BrandDiscoverySectionType.FEATURED]?.brands.orEmpty()
                if (featured.isNotEmpty()) {
                    item {
                        BrandHorizontalSection(
                            title = stringResource(R.string.brand_section_featured),
                            subtitle = stringResource(R.string.brand_section_featured_subtitle),
                            badge = stringResource(R.string.brand_section_featured_badge),
                            brands = featured,
                            showFeaturedTag = true,
                            onBrandClick = onBrandClick
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSearching) {
                            stringResource(R.string.brand_section_search_results)
                        } else {
                            stringResource(R.string.brand_section_all_brands)
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFF3DEC7)
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0x2EB4845A))
                            .border(1.dp, Color(0x55E4C29A), RoundedCornerShape(999.dp))
                            .clickable(onClick = onSuggestBrand)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFFF5E2CC)
                        )
                        Text(
                            text = stringResource(R.string.brand_add_brand),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFF5E2CC)
                        )
                    }
                }
            }

            if (viewModel.isUserSignedIn) {
                item {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0x243B2419))
                            .border(1.dp, Color(0x4EE5C49D), RoundedCornerShape(999.dp))
                            .clickable(onClick = onOpenMySuggestions)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFFEFDCC6)
                        )
                        Text(
                            text = stringResource(R.string.brand_my_suggestions_action),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFF1DDC8)
                        )
                    }
                }
            }

            if (allBrands.isEmpty()) {
                item {
                    if (viewModel.isCatalogLoading) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFD0A77A))
                        }
                    } else {
                        EmptyBrandState(
                            title = if (isSearching) {
                                stringResource(R.string.brand_empty_title)
                            } else {
                                stringResource(R.string.brand_catalog_empty_title)
                            },
                            subtitle = if (isSearching) {
                                stringResource(R.string.brand_empty_subtitle)
                            } else {
                                stringResource(R.string.brand_catalog_empty_subtitle)
                            }
                        )
                    }
                }
            } else {
                items(allBrands, key = { it.id }) { brand ->
                    BrandCard(
                        brand = brand,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onBrandClick(brand.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandCategoryChips(
    selectedCategory: BrandCategory?,
    availableCategories: List<BrandCategory>,
    onCategorySelected: (BrandCategory?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 2.dp)
    ) {
        item {
            CategoryChip(
                text = stringResource(R.string.brand_category_all),
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) }
            )
        }
        items(availableCategories, key = { it.name }) { category ->
            CategoryChip(
                text = stringResource(category.toLabelRes()),
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
private fun CategoryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Color(0x3DAA7751) else Color(0x213A2419))
            .border(
                width = 1.dp,
                color = if (selected) Color(0x66E5C49D) else Color(0x30E5C49D),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color(0xFFF6E5D0) else Color(0xD7D0AF90)
        )
    }
}

@Composable
private fun BrandHorizontalSection(
    title: String,
    subtitle: String,
    brands: List<Brand>,
    onBrandClick: (String) -> Unit,
    badge: String? = null,
    showFeaturedTag: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFF3DEC7)
                )
                if (!badge.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0x3B5A3D2A))
                            .border(1.dp, Color(0x44E5C49D), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF2DFC9)
                        )
                    }
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xD3D5B79A)
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 2.dp)
        ) {
            items(brands, key = { it.id }) { brand ->
                BrandCard(
                    brand = brand,
                    modifier = Modifier.width(262.dp),
                    showFeaturedTag = showFeaturedTag,
                    onClick = { onBrandClick(brand.id) }
                )
            }
        }
    }
}

@Composable
private fun BrandCard(
    brand: Brand,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showFeaturedTag: Boolean = false
) {
    val context = LocalContext.current
    val locale = AppLocaleManager.currentLocale(context)
    val hasRating = brand.reviewCount > 0
    val initials = brand.name.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xB0382419))
            .border(1.dp, Color(0x52E5C49D), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(15.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x2C5C3B29))
                        .border(1.dp, Color(0x44E5C49D), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!brand.logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = brand.logoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFF2DFC9)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = brand.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFF8E6D1),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val locationLabel = formatBrandLocation(
                        cityOrArea = brand.cityOrArea,
                        countryOrCode = brand.country,
                        locale = locale
                    )
                    if (locationLabel.isNotBlank()) {
                        Text(
                            text = locationLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xD7D3B293),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (showFeaturedTag) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0x325C3F2A))
                            .border(1.dp, Color(0x44E5C49D), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.brand_section_featured_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF2DEC8)
                        )
                    }
                }
            }

            if (brand.description.isNotBlank()) {
                Text(
                    text = brand.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xCEDBC1A5),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (hasRating) Color(0xFFE6B25D) else Color(0x76CFB191),
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = if (hasRating) {
                            stringResource(
                                R.string.brand_rating_summary,
                                brand.averageRating.displayRating(),
                                brand.reviewCount
                            )
                        } else {
                            stringResource(R.string.brand_no_reviews_yet)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (hasRating) Color(0xFFF0D7BA) else Color(0xC2D6B392)
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0x2A5A3B29))
                        .border(1.dp, Color(0x3CE5C49D), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = stringResource(R.string.brand_product_count, brand.productCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xE6E0C2A4)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBrandState(
    title: String,
    subtitle: String
) {
    CoffeeEmptyStateCard(
        title = title,
        subtitle = subtitle,
        containerColor = Color(0x8F3A2419),
        borderColor = Color(0x44E5C49D),
        titleColor = Color(0xFFF5E4CF),
        subtitleColor = Color(0xD2D5B79A),
        iconContainerColor = Color(0x3A5B3726),
        iconTint = Color(0xFFE5C49D)
    )
}

private fun BrandCategory.toLabelRes(): Int {
    return when (this) {
        BrandCategory.SPECIALTY -> R.string.brand_category_specialty
        BrandCategory.LOCAL_ROASTERS -> R.string.brand_category_local_roasters
        BrandCategory.CHAINS -> R.string.brand_category_chains
        BrandCategory.COMMERCIAL -> R.string.brand_category_commercial
    }
}

@Composable
internal fun ReviewStars(
    rating: Int,
    onRatingChange: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 22.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..5).forEach { index ->
            val selected = index <= rating
            val iconModifier = if (onRatingChange != null) {
                Modifier
                    .size(iconSize)
                    .clickable { onRatingChange(index) }
            } else {
                Modifier.size(iconSize)
            }
            Icon(
                imageVector = if (selected) Icons.Default.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = if (selected) Color(0xFFE2AD5A) else Color(0x7FD4B391),
                modifier = iconModifier
            )
        }
    }
}
