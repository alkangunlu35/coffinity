package com.icoffee.app.ui.screens.suggestions

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.icoffee.app.R
import com.icoffee.app.data.firebase.model.FirestoreBrandSuggestion
import com.icoffee.app.data.model.BrandSuggestionStatus
import com.icoffee.app.ui.components.CoffeeEmptyStateCard
import com.icoffee.app.ui.components.PrimaryButton
import com.icoffee.app.viewmodel.SuggestBrandViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun MySuggestionsScreen(
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit,
    viewModel: SuggestBrandViewModel = viewModel()
) {
    LaunchedEffect(viewModel.isSignedIn) {
        viewModel.loadMySuggestions()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E120D), Color(0xFF2A1912), Color(0xFF1E120D))
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        text = stringResource(R.string.brand_my_suggestions_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFF7E5D0)
                    )
                }
            }

            if (!viewModel.isSignedIn) {
                item {
                    SuggestionStateCard(
                        title = stringResource(R.string.brand_error_sign_in_required),
                        subtitle = stringResource(R.string.brand_suggestions_sign_in_required_subtitle)
                    )
                }
                item {
                    PrimaryButton(
                        text = stringResource(R.string.profile_sign_in_sync),
                        onClick = onRequestSignIn
                    )
                }
                return@LazyColumn
            }

            if (viewModel.isLoadingMySuggestions) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFD0A77A))
                    }
                }
            } else if (viewModel.mySuggestions.isEmpty()) {
                item {
                    SuggestionStateCard(
                        title = stringResource(R.string.brand_my_suggestions_empty_title),
                        subtitle = stringResource(R.string.brand_my_suggestions_empty_subtitle)
                    )
                }
            } else {
                items(viewModel.mySuggestions, key = { it.id }) { suggestion ->
                    UserSuggestionRow(suggestion = suggestion)
                }
            }
        }
    }
}

@Composable
private fun UserSuggestionRow(
    suggestion: FirestoreBrandSuggestion
) {
    val status = BrandSuggestionStatus.fromStorage(suggestion.status)
    val dateLabel = rememberDateLabel(suggestion.createdAt)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xA03A2419))
            .border(1.dp, Color(0x4FE5C49D), RoundedCornerShape(16.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = suggestion.brandName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFFF6E4D0)
            )
            SuggestionStatusBadge(status = status)
        }
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xCFCBAA8C)
        )
        Text(
            text = suggestionResultSummary(suggestion, status),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xD8D0B396)
        )
    }
}

@Composable
private fun SuggestionStatusBadge(status: BrandSuggestionStatus) {
    val palette: Triple<Color, Color, Int> = when (status) {
        BrandSuggestionStatus.PENDING -> Triple(Color(0x244F6A7C), Color(0x669CCCE4), R.string.suggestion_status_pending)
        BrandSuggestionStatus.UNDER_REVIEW -> Triple(Color(0x2D6A5324), Color(0x7FD9B56F), R.string.suggestion_status_under_review)
        BrandSuggestionStatus.APPROVED_NEW_BRAND -> Triple(Color(0x244E6A3C), Color(0x66A8D38E), R.string.suggestion_status_approved_new_brand)
        BrandSuggestionStatus.MERGED_EXISTING_BRAND -> Triple(Color(0x24525D70), Color(0x668FB5DD), R.string.suggestion_status_merged_existing_brand)
        BrandSuggestionStatus.REJECTED -> Triple(Color(0x3A6A2F24), Color(0x7FE3A58C), R.string.suggestion_status_rejected)
    }
    val bg = palette.first
    val border = palette.second
    val labelRes = palette.third
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFF2DFC9)
        )
    }
}

@Composable
private fun SuggestionStateCard(
    title: String,
    subtitle: String
) {
    CoffeeEmptyStateCard(
        title = title,
        subtitle = subtitle,
        containerColor = Color(0xA03A2419),
        borderColor = Color(0x4FE5C49D),
        titleColor = Color(0xFFF6E4D0),
        subtitleColor = Color(0xD8D0B396),
        iconContainerColor = Color(0x3A5B3726),
        iconTint = Color(0xFFE5C49D)
    )
}

@Composable
private fun rememberDateLabel(epochMillis: Long): String {
    if (epochMillis <= 0L) return stringResource(R.string.brand_unknown_date)
    val formatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
    return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(formatter)
}

@Composable
private fun suggestionResultSummary(
    suggestion: FirestoreBrandSuggestion,
    status: BrandSuggestionStatus
): String {
    return when (status) {
        BrandSuggestionStatus.PENDING -> stringResource(R.string.suggestion_status_pending_summary)
        BrandSuggestionStatus.UNDER_REVIEW -> stringResource(R.string.suggestion_status_under_review_summary)
        BrandSuggestionStatus.APPROVED_NEW_BRAND -> suggestion.createdBrandId
            ?.takeIf { it.isNotBlank() }
            ?.let { stringResource(R.string.suggestion_status_approved_summary_with_brand, it) }
            ?: stringResource(R.string.suggestion_status_approved_summary)
        BrandSuggestionStatus.MERGED_EXISTING_BRAND -> suggestion.mergedIntoBrandId
            ?.takeIf { it.isNotBlank() }
            ?.let { stringResource(R.string.suggestion_status_merged_summary_with_brand, it) }
            ?: stringResource(R.string.suggestion_status_merged_summary)
        BrandSuggestionStatus.REJECTED -> suggestion.rejectionReason
            ?.takeIf { it.isNotBlank() }
            ?.let { stringResource(R.string.suggestion_status_rejected_summary_with_reason, it) }
            ?: stringResource(R.string.suggestion_status_rejected_summary)
    }
}
