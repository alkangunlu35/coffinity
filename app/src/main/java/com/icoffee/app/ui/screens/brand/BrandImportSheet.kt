package com.icoffee.app.ui.screens.brand

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.icoffee.app.R
import com.icoffee.app.data.model.BrandLifecycleStatus
import com.icoffee.app.data.model.importer.BrandImportFailureReason
import com.icoffee.app.data.model.importer.BrandImportPreview
import com.icoffee.app.data.model.importer.BrandImportPreviewResult
import com.icoffee.app.ui.components.CoffeeFeedbackCard
import com.icoffee.app.ui.components.CoffeeFeedbackTone
import com.icoffee.app.ui.components.CoffeeLabelText
import com.icoffee.app.ui.components.CoffeePlaceholderText
import com.icoffee.app.ui.components.PrimaryButton
import com.icoffee.app.ui.components.coffeeFieldTextStyle
import com.icoffee.app.ui.components.coffeeOutlinedTextFieldColors
import com.icoffee.app.ui.theme.CoffeeColorTokens
import com.icoffee.app.ui.theme.CoffeeRadius
import com.icoffee.app.ui.theme.CoffeeSpace
import com.icoffee.app.viewmodel.BrandAdminViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandImportSheet(
    viewModel: BrandAdminViewModel,
    onDismiss: () -> Unit
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var sourceUrlInput by rememberSaveable { mutableStateOf("") }
    var importErrorRes by rememberSaveable { mutableStateOf<Int?>(null) }
    var saveFeedbackRes by rememberSaveable { mutableStateOf<Int?>(null) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    val loadingLabelRes = remember { mutableIntStateOf(R.string.brand_import_loading) }

    var preview by remember { mutableStateOf<BrandImportPreview?>(null) }
    var brandName by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var logoUrl by rememberSaveable { mutableStateOf("") }
    var coverImageUrl by rememberSaveable { mutableStateOf("") }
    var website by rememberSaveable { mutableStateOf("") }
    var instagram by rememberSaveable { mutableStateOf("") }
    var country by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }

    val readableFieldTextStyle = coffeeFieldTextStyle()
    val readableFieldColors = coffeeOutlinedTextFieldColors()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CoffeeColorTokens.surfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = CoffeeSpace.lg, vertical = CoffeeSpace.md),
            verticalArrangement = Arrangement.spacedBy(CoffeeSpace.lg)
        ) {
            SheetTopHandle()

            ImportSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(CoffeeSpace.sm)) {
                    Text(
                        text = stringResource(R.string.brand_import_brand_sheet_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = CoffeeColorTokens.textPrimary
                    )
                    Text(
                        text = stringResource(R.string.brand_import_brand_sheet_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoffeeColorTokens.textSecondary
                    )
                }
            }

            ImportSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(CoffeeSpace.md)) {
                    Text(
                        text = stringResource(R.string.brand_import_url_label_generic),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = CoffeeColorTokens.textPrimary
                    )
                    OutlinedTextField(
                        value = sourceUrlInput,
                        onValueChange = {
                            sourceUrlInput = it
                            importErrorRes = null
                            saveFeedbackRes = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = readableFieldTextStyle,
                        label = { CoffeeLabelText(stringResource(R.string.brand_import_url_label_generic)) },
                        placeholder = { CoffeePlaceholderText(stringResource(R.string.brand_import_url_placeholder_generic)) },
                        colors = readableFieldColors,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )

                    PrimaryButton(
                        text = stringResource(R.string.brand_import_brand_action),
                        onClick = {
                            scope.launch {
                                isLoading = true
                                importErrorRes = null
                                saveFeedbackRes = null
                                when (val result = viewModel.importBrandFromUrl(sourceUrlInput)) {
                                    is BrandImportPreviewResult.Success -> {
                                        preview = result.preview
                                        brandName = result.preview.detectedBrandName.orEmpty()
                                        description = result.preview.detectedDescription.orEmpty()
                                        logoUrl = result.preview.detectedLogoUrl.orEmpty()
                                        coverImageUrl = result.preview.detectedCoverImageUrl.orEmpty()
                                        website = result.preview.detectedWebsite.orEmpty()
                                        instagram = result.preview.detectedInstagram.orEmpty()
                                        country = result.preview.detectedCountry.orEmpty()
                                        city = result.preview.detectedCity.orEmpty()
                                    }

                                    is BrandImportPreviewResult.Failure -> {
                                        preview = null
                                        importErrorRes = when (result.reason) {
                                            BrandImportFailureReason.INVALID_URL -> R.string.brand_import_error_invalid_url
                                            BrandImportFailureReason.UNAUTHORIZED -> R.string.brand_management_error_unauthorized
                                            BrandImportFailureReason.UNREACHABLE -> R.string.brand_import_error_unreachable
                                            BrandImportFailureReason.NO_DATA -> R.string.brand_import_error_no_data
                                            BrandImportFailureReason.UNKNOWN -> R.string.brand_import_error_generic
                                        }
                                    }
                                }
                                isLoading = false
                            }
                        },
                        enabled = sourceUrlInput.trim().isNotEmpty() && !isLoading
                    )
                }
            }

            if (isLoading) {
                CoffeeFeedbackCard(
                    message = stringResource(loadingLabelRes.intValue),
                    tone = CoffeeFeedbackTone.Info
                )
            }

            if (importErrorRes != null) {
                CoffeeFeedbackCard(
                    message = stringResource(importErrorRes!!),
                    tone = CoffeeFeedbackTone.Error
                )
            }

            val activePreview = preview
            if (activePreview == null && !isLoading && importErrorRes == null) {
                CoffeeFeedbackCard(
                    message = stringResource(R.string.brand_import_brand_sheet_subtitle),
                    tone = CoffeeFeedbackTone.Info
                )
            }

            AnimatedVisibility(
                visible = activePreview != null,
                enter = fadeIn(animationSpec = tween(durationMillis = 220)) +
                    expandVertically(animationSpec = tween(durationMillis = 220)),
                exit = fadeOut(animationSpec = tween(durationMillis = 180)) +
                    shrinkVertically(animationSpec = tween(durationMillis = 180))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(CoffeeSpace.md)) {
                    Text(
                        text = stringResource(R.string.brand_import_brand_preview_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = CoffeeColorTokens.textPrimary,
                        modifier = Modifier.padding(top = CoffeeSpace.xs)
                    )
                    Text(
                        text = stringResource(R.string.brand_import_preview_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = CoffeeColorTokens.textSecondary
                    )

                    ImportSectionCard {
                        Column(verticalArrangement = Arrangement.spacedBy(CoffeeSpace.md)) {
                            if (!logoUrl.isBlank()) {
                                AsyncImage(
                                    model = logoUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = CoffeeColorTokens.surfaceSoft,
                                            shape = RoundedCornerShape(CoffeeRadius.md)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = CoffeeColorTokens.borderSubtle,
                                            shape = RoundedCornerShape(CoffeeRadius.md)
                                        )
                                        .padding(CoffeeSpace.sm)
                                )
                            }

                            Text(
                                text = stringResource(R.string.brand_import_source, activePreview?.sourceUrl.orEmpty()),
                                style = MaterialTheme.typography.bodySmall,
                                color = CoffeeColorTokens.textSecondary
                            )

                            OutlinedTextField(
                                value = brandName,
                                onValueChange = {
                                    brandName = it
                                    saveFeedbackRes = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = readableFieldTextStyle,
                                label = { CoffeeLabelText(stringResource(R.string.brand_field_brand_name)) },
                                colors = readableFieldColors,
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = description,
                                onValueChange = {
                                    description = it
                                    saveFeedbackRes = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = readableFieldTextStyle,
                                label = { CoffeeLabelText(stringResource(R.string.brand_field_description_optional)) },
                                colors = readableFieldColors,
                                minLines = 3,
                                maxLines = 5
                            )

                            OutlinedTextField(
                                value = logoUrl,
                                onValueChange = {
                                    logoUrl = it
                                    saveFeedbackRes = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = readableFieldTextStyle,
                                label = { CoffeeLabelText(stringResource(R.string.brand_import_field_logo_url)) },
                                colors = readableFieldColors,
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = coverImageUrl,
                                onValueChange = {
                                    coverImageUrl = it
                                    saveFeedbackRes = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = readableFieldTextStyle,
                                label = { CoffeeLabelText(stringResource(R.string.brand_import_field_cover_url)) },
                                colors = readableFieldColors,
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = website,
                                onValueChange = {
                                    website = it
                                    saveFeedbackRes = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = readableFieldTextStyle,
                                label = { CoffeeLabelText(stringResource(R.string.brand_suggest_website_optional)) },
                                colors = readableFieldColors,
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = instagram,
                                onValueChange = {
                                    instagram = it
                                    saveFeedbackRes = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = readableFieldTextStyle,
                                label = { CoffeeLabelText(stringResource(R.string.brand_suggest_instagram_optional)) },
                                colors = readableFieldColors,
                                singleLine = true
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(CoffeeSpace.sm)) {
                                OutlinedTextField(
                                    value = country,
                                    onValueChange = {
                                        country = it
                                        saveFeedbackRes = null
                                    },
                                    modifier = Modifier.weight(1f),
                                    textStyle = readableFieldTextStyle,
                                    label = { CoffeeLabelText(stringResource(R.string.brand_field_country_optional)) },
                                    colors = readableFieldColors,
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = city,
                                    onValueChange = {
                                        city = it
                                        saveFeedbackRes = null
                                    },
                                    modifier = Modifier.weight(1f),
                                    textStyle = readableFieldTextStyle,
                                    label = { CoffeeLabelText(stringResource(R.string.brand_suggest_city_optional)) },
                                    colors = readableFieldColors,
                                    singleLine = true
                                )
                            }

                            if (saveFeedbackRes != null) {
                                CoffeeFeedbackCard(
                                    message = stringResource(saveFeedbackRes!!),
                                    tone = if (saveFeedbackRes == R.string.brand_import_save_success) {
                                        CoffeeFeedbackTone.Success
                                    } else {
                                        CoffeeFeedbackTone.Error
                                    }
                                )
                            }

                            PrimaryButton(
                                text = stringResource(R.string.brand_import_brand_save_action),
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        val saveResult = viewModel.createBrand(
                                            name = brandName.trim(),
                                            description = description.trim(),
                                            country = country.trim(),
                                            city = city.trim(),
                                            website = website.trim(),
                                            instagram = instagram.trim(),
                                            logoUrl = logoUrl.trim(),
                                            coverImageUrl = coverImageUrl.trim(),
                                            sourceUrl = activePreview?.sourceUrl.orEmpty(),
                                            status = BrandLifecycleStatus.ACTIVE.storageValue
                                        )
                                        saveFeedbackRes = if (saveResult.isSuccess) {
                                            R.string.brand_import_save_success
                                        } else {
                                            val message = saveResult.exceptionOrNull()?.message.orEmpty()
                                            if (message.startsWith("duplicate:")) {
                                                R.string.brand_error_duplicate_existing
                                            } else if (message == "unauthorized") {
                                                R.string.brand_management_error_unauthorized
                                            } else {
                                                R.string.brand_import_error_save
                                            }
                                        }
                                        isLoading = false
                                    }
                                },
                                enabled = brandName.trim().isNotEmpty() && !isLoading,
                                modifier = Modifier.padding(top = CoffeeSpace.sm, bottom = CoffeeSpace.xs)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetTopHandle() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(4.dp)
                .background(
                    color = CoffeeColorTokens.borderStrong.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(CoffeeRadius.pill)
                )
        )
    }
}

@Composable
private fun ImportSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = CoffeeColorTokens.surface,
                shape = RoundedCornerShape(CoffeeRadius.lg)
            )
            .border(
                width = 1.dp,
                color = CoffeeColorTokens.borderSubtle,
                shape = RoundedCornerShape(CoffeeRadius.lg)
            )
            .padding(CoffeeSpace.lg),
        verticalArrangement = Arrangement.spacedBy(CoffeeSpace.sm),
        content = content
    )
}
