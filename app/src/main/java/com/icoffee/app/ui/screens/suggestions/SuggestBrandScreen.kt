package com.icoffee.app.ui.screens.suggestions

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.icoffee.app.data.model.SuggestBrandInput
import com.icoffee.app.data.suggestion.SubmitBrandSuggestionResult
import com.icoffee.app.data.suggestion.SuggestionSubmitFailureReason
import com.icoffee.app.ui.components.PrimaryButton
import com.icoffee.app.viewmodel.SuggestBrandViewModel

@Composable
fun SuggestBrandScreen(
    onBack: () -> Unit,
    onRequestSignIn: () -> Unit,
    onOpenMySuggestions: () -> Unit,
    viewModel: SuggestBrandViewModel = viewModel()
) {
    var brandName by rememberSaveable { mutableStateOf("") }
    var website by rememberSaveable { mutableStateOf("") }
    var instagram by rememberSaveable { mutableStateOf("") }
    var country by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var feedbackRes by rememberSaveable { mutableIntStateOf(0) }

    val isSignedIn = viewModel.isSignedIn

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E120D), Color(0xFF2A1912), Color(0xFF1E120D))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp)),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                    text = stringResource(R.string.brand_suggest_brand_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFF7E5D0)
                )
            }

            Text(
                text = stringResource(R.string.brand_suggest_brand_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xD8D0B396)
            )

            if (!isSignedIn) {
                SuggestionInfoCard(
                    title = stringResource(R.string.brand_error_sign_in_required),
                    subtitle = stringResource(R.string.brand_suggestions_sign_in_required_subtitle)
                )
                PrimaryButton(
                    text = stringResource(R.string.profile_sign_in_sync),
                    onClick = onRequestSignIn
                )
                return@Column
            }

            OutlinedTextField(
                value = brandName,
                onValueChange = {
                    brandName = it
                    feedbackRes = 0
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.brand_field_brand_name)) },
                singleLine = true
            )
            OutlinedTextField(
                value = website,
                onValueChange = {
                    website = it
                    feedbackRes = 0
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.brand_suggest_website_optional)) },
                singleLine = true
            )
            OutlinedTextField(
                value = instagram,
                onValueChange = {
                    instagram = it
                    feedbackRes = 0
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.brand_suggest_instagram_optional)) },
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = country,
                    onValueChange = {
                        country = it
                        feedbackRes = 0
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.brand_field_country_optional)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = {
                        city = it
                        feedbackRes = 0
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.brand_suggest_city_optional)) },
                    singleLine = true
                )
            }
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    feedbackRes = 0
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.brand_suggest_note_optional)) },
                minLines = 3,
                maxLines = 5
            )

            if (feedbackRes != 0) {
                Text(
                    text = stringResource(feedbackRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE7C39A)
                )
            }

            PrimaryButton(
                text = stringResource(R.string.brand_submit_suggestion),
                onClick = {
                    viewModel.submitSuggestion(
                        input = SuggestBrandInput(
                            brandName = brandName,
                            websiteUrl = website,
                            instagramUrl = instagram,
                            country = country,
                            city = city,
                            description = description
                        )
                    ) { result ->
                        feedbackRes = when (result) {
                            is SubmitBrandSuggestionResult.Success -> {
                                brandName = ""
                                website = ""
                                instagram = ""
                                country = ""
                                city = ""
                                description = ""
                                if (result.possibleDuplicate) {
                                    R.string.brand_suggestion_possible_duplicate_warning
                                } else {
                                    R.string.brand_suggestion_success
                                }
                            }

                            is SubmitBrandSuggestionResult.Failure -> {
                                when (result.reason) {
                                    SuggestionSubmitFailureReason.UNAUTHORIZED -> R.string.brand_error_sign_in_required
                                    SuggestionSubmitFailureReason.EMPTY_NAME -> R.string.brand_error_name_required
                                    SuggestionSubmitFailureReason.INVALID_URL -> R.string.brand_error_invalid_url
                                    SuggestionSubmitFailureReason.DUPLICATE_SUBMISSION -> R.string.brand_error_duplicate_pending
                                    SuggestionSubmitFailureReason.STORE_ERROR -> R.string.brand_import_error_save
                                }
                            }
                        }
                    }
                },
                enabled = brandName.trim().isNotEmpty() && !viewModel.isSubmitting
            )

            PrimaryButton(
                text = stringResource(R.string.brand_my_suggestions_action),
                onClick = onOpenMySuggestions,
                enabled = !viewModel.isSubmitting
            )
        }
    }
}

@Composable
private fun SuggestionInfoCard(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xA03A2419))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFFF6E4D0)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xD8D0B396)
        )
    }
}
