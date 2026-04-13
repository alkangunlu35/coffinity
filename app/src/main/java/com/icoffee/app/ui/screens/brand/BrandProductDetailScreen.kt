package com.icoffee.app.ui.screens.brand

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.lifecycle.viewmodel.compose.viewModel
import com.icoffee.app.R
import com.icoffee.app.data.BrandRepository
import com.icoffee.app.data.model.ReviewTargetType
import com.icoffee.app.data.model.displayRating
import com.icoffee.app.localization.AppLocaleManager
import com.icoffee.app.ui.components.CoffeeEmptyStateCard
import com.icoffee.app.viewmodel.BrandViewModel
import kotlinx.coroutines.launch
import coil.compose.AsyncImage

@Composable
fun BrandProductDetailScreen(
    brandId: String,
    productId: String,
    onBack: () -> Unit,
    viewModel: BrandViewModel = viewModel()
) {
    val brand = viewModel.brandById(brandId)
    val product = viewModel.productById(productId)
    val reviews = viewModel.reviews(ReviewTargetType.PRODUCT, productId)
    val currentUserReview = viewModel.currentUserReview(ReviewTargetType.PRODUCT, productId)
    val (resolvedAverageRating, resolvedReviewCount) = viewModel.reviewAggregate(
        targetType = ReviewTargetType.PRODUCT,
        targetId = productId,
        fallbackAverageRating = product?.averageRating ?: 0.0,
        fallbackReviewCount = product?.reviewCount ?: 0
    )
    val context = LocalContext.current
    val locale = AppLocaleManager.currentLocale(context)
    val scope = rememberCoroutineScope()

    LaunchedEffect(productId) {
        viewModel.onProductOpened(productId)
    }
    LaunchedEffect(productId, viewModel.refreshVersion) {
        viewModel.refreshReviews(ReviewTargetType.PRODUCT, productId)
    }

    var ratingInput by rememberSaveable { mutableIntStateOf(0) }
    var commentInput by rememberSaveable { mutableStateOf("") }
    var reviewFeedbackRes by rememberSaveable { mutableStateOf<Int?>(null) }
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

    if (brand == null || product == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF20120D)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.brand_product_not_found),
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
                        text = stringResource(R.string.brand_product_detail_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFF7E5D0)
                    )
                }
            }

            item {
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
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
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
                                        imageVector = Icons.Default.Coffee,
                                        contentDescription = null,
                                        tint = Color(0xFFE4BC8A)
                                    )
                                }
                            }
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFFF8E6D1)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = Color(0xD9D5B28E)
                            )
                            Text(
                                text = brand.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xE1D2B091)
                            )
                        }
                        if (locationLabel.isNotBlank()) {
                            Text(
                                text = locationLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xD9DAB999),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        val roast = product.roastLevel.orEmpty()
                        val origin = product.originSummary.orEmpty()
                        if (roast.isNotBlank() || origin.isNotBlank()) {
                            Text(
                                text = listOf(roast, origin).filter { it.isNotBlank() }.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xD9DAB999)
                            )
                        }

                        if (!product.description.isNullOrBlank()) {
                            Text(
                                text = product.description.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xDCE2C7A8),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            item {
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
                                text = if (resolvedReviewCount > 0) {
                                    stringResource(
                                        R.string.brand_rating_summary,
                                        resolvedAverageRating.displayRating(),
                                        resolvedReviewCount
                                    )
                                } else {
                                    stringResource(R.string.brand_no_reviews_yet)
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFFF5E2CC)
                            )
                        }

                        ReviewStars(
                            rating = resolvedAverageRating.toInt().coerceIn(0, 5),
                            iconSize = 19.dp
                        )
                    }
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
                                targetType = ReviewTargetType.PRODUCT,
                                targetId = product.id,
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
                    EmptyProductReviewState()
                }
            } else {
                items(reviews, key = { it.id }) { review ->
                    ProductReviewCard(review = review)
                }
            }
        }
    }
}

@Composable
private fun EmptyProductReviewState() {
    CoffeeEmptyStateCard(
        title = stringResource(R.string.brand_product_reviews_empty_title),
        subtitle = stringResource(R.string.brand_product_reviews_empty_subtitle),
        containerColor = Color(0x80322117),
        borderColor = Color(0x36E5C49D),
        titleColor = Color(0xFFF4DFC8),
        subtitleColor = Color(0xCFD5B697),
        iconContainerColor = Color(0x3A5B3726),
        iconTint = Color(0xFFE5C49D)
    )
}

@Composable
private fun ProductReviewCard(review: com.icoffee.app.data.model.Review) {
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
