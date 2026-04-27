package com.icoffee.app.ui.screens.detail

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.icoffee.app.R
import com.icoffee.app.data.BrandRepository
import com.icoffee.app.data.PhaseOneRepository
import com.icoffee.app.data.model.BrewingMethod
import com.icoffee.app.data.model.Review
import com.icoffee.app.data.model.ReviewTargetType
import com.icoffee.app.ui.brewing.localizedBrewingMethod
import com.icoffee.app.ui.components.CoffeeEmptyStateCard
import com.icoffee.app.ui.components.PrimaryButton
import com.icoffee.app.ui.screens.brand.ReviewStars
import com.icoffee.app.viewmodel.BrandViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

@Composable
fun BrewingDetailScreen(
    methodId: String,
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit,
    viewModel: BrandViewModel = viewModel()
) {
    val method = localizedBrewingMethod(PhaseOneRepository.methodById(methodId))
    val reviews = viewModel.reviews(ReviewTargetType.BREW, method.id)
    val currentUserReview = viewModel.currentUserReview(ReviewTargetType.BREW, method.id)
    val (averageRating, reviewCount) = viewModel.reviewAggregate(
        targetType = ReviewTargetType.BREW,
        targetId = method.id,
        fallbackAverageRating = 0.0,
        fallbackReviewCount = 0
    )
    val scope = rememberCoroutineScope()

    var ratingInput by rememberSaveable { mutableIntStateOf(0) }
    var commentInput by rememberSaveable { mutableStateOf("") }
    var reviewFeedbackRes by rememberSaveable { mutableStateOf<Int?>(null) }

    LaunchedEffect(method.id, viewModel.refreshVersion) {
        viewModel.refreshReviews(ReviewTargetType.BREW, method.id)
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
                bottom = 120.dp
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
                            contentDescription = stringResource(R.string.scan_back),
                            tint = Color(0xFFF5E2CC)
                        )
                    }
                    Text(
                        text = method.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFF7E5D0),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            item {
                BrewHeroCard(method = method)
            }

            item {
                BrewInfoSection(
                    title = stringResource(R.string.brew_section_how_it_works),
                    lines = listOf(method.howItWorks)
                )
            }

            item {
                BrewInfoSection(
                    title = stringResource(R.string.brew_section_how_to_brew),
                    lines = method.howToBrew
                )
            }

            item {
                BrewInfoSection(
                    title = stringResource(R.string.brew_section_characteristics),
                    lines = method.brewCharacteristics
                )
            }

            if (method.bestFor.isNotEmpty()) {
                item {
                    BrewInfoSection(
                        title = stringResource(R.string.brew_section_best_for),
                        lines = method.bestFor.map {
                            stringResource(R.string.brew_best_for_item, it)
                        }
                    )
                }
            }

            method.homeMachineNote?.takeIf { it.isNotBlank() }?.let { tip ->
                item {
                    BrewInfoSection(
                        title = stringResource(R.string.brew_section_home_tips),
                        lines = listOf(tip)
                    )
                }
            }

            item {
                BrewReviewSummaryCard(
                    averageRating = averageRating,
                    reviewCount = reviewCount
                )
            }

            item {
                BrewReviewComposerCard(
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
                    onRequestSignIn = onRequestSignIn,
                    onSubmit = {
                        scope.launch {
                            val result = viewModel.submitReview(
                                targetType = ReviewTargetType.BREW,
                                targetId = method.id,
                                rating = ratingInput,
                                comment = commentInput
                            )
                            reviewFeedbackRes = when (result) {
                                BrandRepository.ReviewSubmissionResult.Success ->
                                    R.string.brew_review_success

                                is BrandRepository.ReviewSubmissionResult.Failure -> when (result.reason) {
                                    BrandRepository.ReviewFailureReason.UNAUTHORIZED ->
                                        R.string.brew_sign_in_to_review

                                    else ->
                                        R.string.brand_error_review_save_failed
                                }
                            }
                            if (result is BrandRepository.ReviewSubmissionResult.Success) {
                                viewModel.refreshReviews(ReviewTargetType.BREW, method.id)
                            }
                        }
                    }
                )
            }

            item {
                Text(
                    text = stringResource(R.string.brew_reviews_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFF2DDC5)
                )
            }

            if (reviews.isEmpty()) {
                item {
                    BrewEmptyReviewCard()
                }
            } else {
                items(reviews, key = { it.id }) { review ->
                    BrewReviewCard(review = review)
                }
            }
        }
    }
}

@Composable
private fun BrewHeroCard(method: BrewingMethod) {
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
    ) {
        Image(
            painter = painterResource(id = method.imageRes),
            contentDescription = method.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(214.dp),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(214.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x0D0D0806),
                            Color(0x660D0806),
                            Color(0xD90D0806)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color(0xFFE5C49D),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = method.brewTime,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFE5C49D)
                )
            }
            Text(
                text = method.cardSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xD9D5B18D)
            )
            Text(
                text = method.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xE6F3DEC7),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BrewInfoSection(
    title: String,
    lines: List<String>
) {
    if (lines.isEmpty()) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xA03A251A))
            .border(1.dp, Color(0x44E5C49D), RoundedCornerShape(18.dp))
            .padding(15.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFFF6E4CE)
            )
            lines.forEach { line ->
                Text(
                    text = "• $line",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xDCE2C7A8)
                )
            }
        }
    }
}

@Composable
private fun BrewReviewSummaryCard(
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.brew_review_summary_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFFF6E4CE)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.brew_review_average_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xD2D4B391)
                    )
                    Text(
                        text = if (hasRating) {
                            String.format(java.util.Locale.getDefault(), "%.1f (%d)", averageRating, reviewCount)
                        } else {
                            stringResource(R.string.brew_no_reviews_yet)
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
}

@Composable
private fun BrewReviewComposerCard(
    isSignedIn: Boolean,
    isEditing: Boolean,
    rating: Int,
    comment: String,
    feedbackResId: Int?,
    onRatingChange: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onRequestSignIn: () -> Unit,
    onSubmit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xA03A251A))
            .border(1.dp, Color(0x44E5C49D), RoundedCornerShape(18.dp))
            .clickable(enabled = !isSignedIn, onClick = onRequestSignIn)
            .padding(15.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text(
                text = stringResource(
                    if (isEditing) R.string.brew_update_review else R.string.brew_leave_review_title
                ),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFFF6E4CE)
            )

            if (!isSignedIn) {
                Text(
                    text = stringResource(R.string.brew_sign_in_to_review),
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
                    label = { Text(stringResource(R.string.brew_review_comment_optional)) },
                    minLines = 3,
                    maxLines = 6
                )
                if (feedbackResId != null) {
                    Text(
                        text = stringResource(feedbackResId),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (feedbackResId == R.string.brew_review_success) {
                            Color(0xFF557550)
                        } else {
                            Color(0xFF9B5A42)
                        }
                    )
                }
                PrimaryButton(
                    text = stringResource(
                        if (isEditing) R.string.brew_update_review else R.string.brew_submit_review
                    ),
                    onClick = onSubmit,
                    enabled = rating in 1..5
                )
            }
        }
    }
}

@Composable
private fun BrewEmptyReviewCard() {
    CoffeeEmptyStateCard(
        title = stringResource(R.string.brew_reviews_empty_title),
        subtitle = stringResource(R.string.brew_reviews_empty_subtitle),
        containerColor = Color(0x80322117),
        borderColor = Color(0x36E5C49D),
        titleColor = Color(0xFFF4DFC8),
        subtitleColor = Color(0xCFD5B697),
        iconContainerColor = Color(0x3A5B3726),
        iconTint = Color(0xFFE5C49D)
    )
}

@Composable
private fun BrewReviewCard(review: Review) {
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
                    text = review.authorDisplayName ?: stringResource(R.string.brew_review_anonymous),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFF4DFC8)
                )
                Text(
                    text = review.updatedAt.asBrewReviewDate(),
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

private fun Long.asBrewReviewDate(): String {
    return runCatching {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        formatter.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate())
    }.getOrDefault("")
}
