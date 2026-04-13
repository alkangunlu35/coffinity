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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.icoffee.app.R
import com.icoffee.app.data.BrandRepository
import com.icoffee.app.data.model.Brand
import com.icoffee.app.data.model.BrandProduct
import com.icoffee.app.data.model.Review
import com.icoffee.app.data.model.ReviewTargetType
import com.icoffee.app.data.model.displayRating
import com.icoffee.app.localization.AppLocaleManager
import com.icoffee.app.ui.components.CoffeeEmptyStateCard
import com.icoffee.app.ui.components.PrimaryButton
import com.icoffee.app.viewmodel.BrandViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandDetailScreen(
    brandId: String,
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    onManageBrand: (String) -> Unit,
    viewModel: BrandViewModel = viewModel()
) {
    val brand = viewModel.brandById(brandId)
    val products = viewModel.productsByBrand(brandId)
    val reviews = viewModel.reviews(ReviewTargetType.BRAND, brandId)
    val currentUserReview = viewModel.currentUserReview(ReviewTargetType.BRAND, brandId)
    val (resolvedAverageRating, resolvedReviewCount) = viewModel.reviewAggregate(
        targetType = ReviewTargetType.BRAND,
        targetId = brandId,
        fallbackAverageRating = brand?.averageRating ?: 0.0,
        fallbackReviewCount = brand?.reviewCount ?: 0
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(brandId) {
        viewModel.onBrandOpened(brandId)
    }
    LaunchedEffect(brandId, viewModel.refreshVersion) {
        viewModel.refreshReviews(ReviewTargetType.BRAND, brandId)
    }

    var ratingInput by rememberSaveable { mutableIntStateOf(0) }
    var commentInput by rememberSaveable { mutableStateOf("") }
    var reviewFeedbackRes by rememberSaveable { mutableStateOf<Int?>(null) }
    var canManageBrand by rememberSaveable(brandId, viewModel.currentUserId) { mutableStateOf(false) }

    var showSuggestProductSheet by rememberSaveable { mutableStateOf(false) }
    var suggestionName by rememberSaveable { mutableStateOf("") }
    var suggestionOrigin by rememberSaveable { mutableStateOf("") }
    var suggestionDescription by rememberSaveable { mutableStateOf("") }
    var suggestionFeedbackRes by rememberSaveable { mutableStateOf<Int?>(null) }

    LaunchedEffect(brandId, viewModel.currentUserId, viewModel.refreshVersion) {
        canManageBrand = viewModel.canManageBrand(brandId)
    }
    LaunchedEffect(currentUserReview?.id, viewModel.currentUserId) {
        if (viewModel.isUserSignedIn) {
            ratingInput = currentUserReview?.rating ?: 0
            commentInput = currentUserReview?.comment.orEmpty()
        } else {
            ratingInput = 0
            commentInput = ""
        }
        reviewFeedbackRes = null
    }

    if (showSuggestProductSheet && brand != null) {
        ModalBottomSheet(
            onDismissRequest = { showSuggestProductSheet = false },
            containerColor = Color(0xFFFFFCF8)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.brand_suggest_product_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF2C1A12)
                )
                Text(
                    text = stringResource(R.string.brand_suggest_product_subtitle, brand.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF7A5E4D)
                )

                OutlinedTextField(
                    value = suggestionName,
                    onValueChange = {
                        suggestionName = it
                        suggestionFeedbackRes = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.brand_field_product_name)) },
                    singleLine = true
                )

                OutlinedTextField(
                    value = suggestionOrigin,
                    onValueChange = {
                        suggestionOrigin = it
                        suggestionFeedbackRes = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.brand_field_origin_optional)) },
                    singleLine = true
                )

                OutlinedTextField(
                    value = suggestionDescription,
                    onValueChange = {
                        suggestionDescription = it
                        suggestionFeedbackRes = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.brand_field_description_optional)) },
                    minLines = 3,
                    maxLines = 5
                )

                if (suggestionFeedbackRes != null) {
                    Text(
                        text = stringResource(suggestionFeedbackRes!!),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (suggestionFeedbackRes == R.string.brand_suggestion_success) {
                            Color(0xFF4F6B4C)
                        } else {
                            Color(0xFF9B5A42)
                        }
                    )
                }

                PrimaryButton(
                    text = stringResource(R.string.brand_submit_suggestion),
                    onClick = {
                        val result = viewModel.submitProductSuggestion(
                            brandId = brand.id,
                            name = suggestionName,
                            originSummary = suggestionOrigin,
                            description = suggestionDescription
                        )
                        suggestionFeedbackRes = when (result) {
                            BrandRepository.SuggestionSubmissionResult.Success -> {
                                suggestionName = ""
                                suggestionOrigin = ""
                                suggestionDescription = ""
                                R.string.brand_suggestion_success
                            }

                            is BrandRepository.SuggestionSubmissionResult.Failure -> {
                                when (result.reason) {
                                    BrandRepository.SuggestionFailureReason.UNAUTHORIZED ->
                                        R.string.brand_error_sign_in_required

                                    BrandRepository.SuggestionFailureReason.EMPTY_NAME ->
                                        R.string.brand_error_name_required

                                    BrandRepository.SuggestionFailureReason.BRAND_NOT_FOUND ->
                                        R.string.brand_error_brand_not_found

                                    BrandRepository.SuggestionFailureReason.DUPLICATE_EXISTING ->
                                        R.string.brand_error_duplicate_existing

                                    BrandRepository.SuggestionFailureReason.DUPLICATE_PENDING ->
                                        R.string.brand_error_duplicate_pending
                                }
                            }
                        }
                    },
                    enabled = suggestionName.trim().isNotEmpty(),
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )
            }
        }
    }

    if (brand == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF20120D)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.brand_not_found),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFF8E6D1)
            )
        }
        return
    }

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
                top = 8.dp,
                bottom = 156.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x6B422A1D))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color(0xFFF5E2CC)
                        )
                    }
                    Text(
                        text = stringResource(R.string.brand_detail_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFF7E5D0)
                    )
                }
            }

            item {
                BrandHeroCard(brand = brand)
            }

            item {
                RatingSummaryCard(
                    averageRating = resolvedAverageRating,
                    reviewCount = resolvedReviewCount
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.brand_products_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFF2DDC5)
                        )
                        Text(
                            text = stringResource(R.string.brand_product_count, products.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xD0D5B79A)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    ) {
                        BrandInlineActionChip(
                            text = stringResource(R.string.brand_suggest_product_action),
                            icon = Icons.Default.Add,
                            emphasized = true,
                            onClick = { showSuggestProductSheet = true }
                        )

                        if (canManageBrand) {
                            BrandInlineActionChip(
                                text = stringResource(R.string.brand_manage_brand_action),
                                icon = Icons.Default.Settings,
                                emphasized = false,
                                onClick = { onManageBrand(brandId) }
                            )
                        }
                    }
                }
            }

            if (products.isEmpty()) {
                item {
                    EmptySectionCard(
                        title = stringResource(R.string.brand_products_empty_title),
                        subtitle = stringResource(R.string.brand_products_empty_subtitle)
                    )
                }
            } else {
                items(products, key = { it.id }) { product ->
                    BrandProductCard(
                        product = product,
                        onClick = {
                            viewModel.onProductOpened(product.id)
                            onProductClick(product.id)
                        }
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.brand_reviews_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFF2DDC5)
                )
            }

            item {
                ReviewComposerCard(
                    isSignedIn = viewModel.isUserSignedIn,
                    isEditing = currentUserReview != null,
                    rating = ratingInput,
                    comment = commentInput,
                    feedbackResId = reviewFeedbackRes,
                    onRatingChange = {
                        ratingInput = it
                        reviewFeedbackRes = null
                    },
                    onCommentChange = {
                        commentInput = it
                        reviewFeedbackRes = null
                    },
                    onSubmit = {
                        scope.launch {
                            val result = viewModel.submitReview(
                                targetType = ReviewTargetType.BRAND,
                                targetId = brand.id,
                                rating = ratingInput,
                                comment = commentInput
                            )
                            reviewFeedbackRes = when (result) {
                                BrandRepository.ReviewSubmissionResult.Success -> {
                                    R.string.brand_review_success
                                }

                                is BrandRepository.ReviewSubmissionResult.Failure -> {
                                    when (result.reason) {
                                        BrandRepository.ReviewFailureReason.UNAUTHORIZED ->
                                            R.string.brand_error_sign_in_required

                                        BrandRepository.ReviewFailureReason.INVALID_RATING ->
                                            R.string.brand_error_invalid_rating

                                        BrandRepository.ReviewFailureReason.INVALID_COMMENT ->
                                            R.string.brand_error_comment_short

                                        BrandRepository.ReviewFailureReason.TARGET_NOT_FOUND ->
                                            R.string.brand_error_target_not_found

                                        BrandRepository.ReviewFailureReason.STORE_ERROR ->
                                            R.string.brand_error_review_save_failed
                                    }
                                }
                            }
                        }
                    }
                )
            }

            if (reviews.isEmpty()) {
                item {
                    EmptySectionCard(
                        title = stringResource(R.string.brand_reviews_empty_title),
                        subtitle = stringResource(R.string.brand_reviews_empty_subtitle)
                    )
                }
            } else {
                items(reviews, key = { it.id }) { review ->
                    ReviewCard(review = review)
                }
            }
        }
    }
}

@Composable
private fun BrandInlineActionChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    emphasized: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (emphasized) Color(0x2EB4845A) else Color(0x173A2419))
            .border(
                1.dp,
                if (emphasized) Color(0x55E4C29A) else Color(0x4CE4C29A),
                RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (emphasized) Color(0xFFF5E2CC) else Color(0xE8D2B8A0)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (emphasized) Color(0xFFF5E2CC) else Color(0xE8D2B8A0)
        )
    }
}

@Composable
private fun BrandHeroCard(brand: Brand) {
    val context = LocalContext.current
    val locale = AppLocaleManager.currentLocale(context)
    val locationLabel = formatBrandLocation(
        cityOrArea = brand.cityOrArea,
        countryOrCode = brand.country,
        locale = locale
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xC2482E20), Color(0xB038241A))
                )
            )
            .border(1.dp, Color(0x4DE5C49D), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = Color(0xFFE4BC8A)
                )
                Text(
                    text = brand.name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFF8E6D1)
                )
            }

            if (locationLabel.isNotBlank()) {
                Text(
                    text = locationLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xE1D2B091),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (brand.description.isNotBlank()) {
                Text(
                    text = brand.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xDCE2C7A8)
                )
            }
        }
    }
}

@Composable
private fun RatingSummaryCard(
    averageRating: Double,
    reviewCount: Int
) {
    val hasRating = reviewCount > 0
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xA03B261A))
            .border(1.dp, Color(0x44E5C49D), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = stringResource(R.string.brand_average_rating),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xD9D9BA98)
                )
                Text(
                    text = if (hasRating) {
                        stringResource(
                            R.string.brand_rating_summary,
                            averageRating.displayRating(),
                            reviewCount
                        )
                    } else {
                        stringResource(R.string.brand_no_reviews_yet)
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFF5E2CC)
                )
            }

            ReviewStars(
                rating = averageRating.toInt().coerceIn(0, 5),
                iconSize = 19.dp
            )
        }
    }
}

@Composable
private fun BrandProductCard(
    product: BrandProduct,
    onClick: () -> Unit
) {
    val hasRating = product.reviewCount > 0
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x8C3A2419))
            .border(1.dp, Color(0x40E5C49D), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x2A23160F)),
                contentAlignment = Alignment.Center
            ) {
                if (!product.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = Color(0xFFE3BA87)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFF8E6D1)
                )
                val roast = product.roastLevel.orEmpty()
                val origin = product.originSummary.orEmpty()
                if (roast.isNotBlank() || origin.isNotBlank()) {
                    Text(
                        text = listOf(roast, origin).filter { it.isNotBlank() }.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xD8CFAC8C)
                    )
                }
                if (!product.description.isNullOrBlank()) {
                    Text(
                        text = product.description.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xCFE0C5A6),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = if (hasRating) {
                        stringResource(
                            R.string.brand_rating_summary,
                            product.averageRating.displayRating(),
                            product.reviewCount
                        )
                    } else {
                        stringResource(R.string.brand_no_reviews_yet)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xE8E5C9A8)
                )
            }

            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (hasRating) Color(0xFFE2AD5A) else Color(0x70D4B391)
            )
        }
    }
}

@Composable
internal fun ReviewComposerCard(
    isSignedIn: Boolean,
    isEditing: Boolean,
    rating: Int,
    comment: String,
    feedbackResId: Int?,
    onRatingChange: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xA03A251A))
            .border(1.dp, Color(0x44E5C49D), RoundedCornerShape(18.dp))
            .padding(15.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text(
                text = stringResource(
                    if (isEditing) R.string.brand_update_review else R.string.brand_leave_review_title
                ),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFFF6E4CE)
            )

            if (!isSignedIn) {
                Text(
                    text = stringResource(R.string.brand_sign_in_to_review),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xD2D4B391)
                )
            } else {
                ReviewStars(
                    rating = rating,
                    onRatingChange = onRatingChange
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = onCommentChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.brand_field_comment_optional)) },
                    minLines = 3,
                    maxLines = 6
                )
                if (feedbackResId != null) {
                    Text(
                        text = stringResource(feedbackResId),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (feedbackResId == R.string.brand_review_success) {
                            Color(0xFF557550)
                        } else {
                            Color(0xFF9B5A42)
                        }
                    )
                }
                PrimaryButton(
                    text = stringResource(
                        if (isEditing) {
                            R.string.brand_update_review
                        } else {
                            R.string.brand_submit_review
                        }
                    ),
                    onClick = onSubmit,
                    enabled = rating in 1..5
                )
            }
        }
    }
}

@Composable
private fun ReviewCard(review: Review) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x88372419))
            .border(1.dp, Color(0x36E5C49D), RoundedCornerShape(16.dp))
            .padding(13.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.authorDisplayName ?: stringResource(R.string.brand_review_anonymous),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFF4DFC8)
                )
                Text(
                    text = review.updatedAt.asReviewDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xC5C7A98B)
                )
            }

            ReviewStars(
                rating = review.rating,
                iconSize = 18.dp
            )

            if (review.comment.isNotBlank()) {
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xDDE0C3A3)
                )
            }
        }
    }
}

@Composable
private fun EmptySectionCard(
    title: String,
    subtitle: String
) {
    CoffeeEmptyStateCard(
        title = title,
        subtitle = subtitle,
        containerColor = Color(0x80322117),
        borderColor = Color(0x36E5C49D),
        titleColor = Color(0xFFF4DFC8),
        subtitleColor = Color(0xCFD5B697),
        iconContainerColor = Color(0x3A5B3726),
        iconTint = Color(0xFFE5C49D)
    )
}

internal fun Long.asReviewDate(): String {
    return runCatching {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        formatter.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate())
    }.getOrDefault("")
}
