package com.icoffee.app.ui.screens.brand

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.icoffee.app.R
import com.icoffee.app.data.model.importer.ProductImportDraft
import com.icoffee.app.data.model.importer.ProductImportFailureReason
import com.icoffee.app.data.model.importer.ProductImportPreview
import com.icoffee.app.data.model.importer.ProductImportPreviewResult
import com.icoffee.app.data.model.importer.ProductImportSaveFailureReason
import com.icoffee.app.data.model.importer.ProductImportSaveResult
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
import com.icoffee.app.viewmodel.BrandViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandProductImportSheet(
    brandId: String,
    allowDraftBrandCreation: Boolean,
    viewModel: BrandViewModel,
    onDismiss: () -> Unit
) {
    var sourceUrlInput by rememberSaveable { mutableStateOf("") }
    var importErrorRes by rememberSaveable { mutableStateOf<Int?>(null) }
    var saveFeedbackRes by rememberSaveable { mutableStateOf<Int?>(null) }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    var preview by remember { mutableStateOf<ProductImportPreview?>(null) }
    var productName by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var imageUrl by rememberSaveable { mutableStateOf("") }
    var origin by rememberSaveable { mutableStateOf("") }
    var roastLevel by rememberSaveable { mutableStateOf("") }
    var process by rememberSaveable { mutableStateOf("") }
    var tastingNotesRaw by rememberSaveable { mutableStateOf("") }
    var barcode by rememberSaveable { mutableStateOf("") }
    var createDraftBrandIfMissing by rememberSaveable { mutableStateOf(false) }

    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val loadingLabelRes = remember { mutableIntStateOf(R.string.brand_import_loading) }
    val fieldTextStyle = coffeeFieldTextStyle()
    val fieldColors = coffeeOutlinedTextFieldColors()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CoffeeColorTokens.surfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = CoffeeSpace.lg, vertical = CoffeeSpace.md),
            verticalArrangement = Arrangement.spacedBy(CoffeeSpace.md)
        ) {
            Text(
                text = stringResource(R.string.brand_import_sheet_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = CoffeeColorTokens.textPrimary
            )
            Text(
                text = stringResource(R.string.brand_import_sheet_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = CoffeeColorTokens.textSecondary
            )

            OutlinedTextField(
                value = sourceUrlInput,
                onValueChange = {
                    sourceUrlInput = it
                    importErrorRes = null
                    saveFeedbackRes = null
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = fieldTextStyle,
                label = { CoffeeLabelText(stringResource(R.string.brand_import_url_label)) },
                placeholder = { CoffeePlaceholderText(stringResource(R.string.brand_import_url_placeholder)) },
                colors = fieldColors,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            PrimaryButton(
                text = stringResource(R.string.brand_import_action),
                onClick = {
                    scope.launch {
                        isLoading = true
                        importErrorRes = null
                        saveFeedbackRes = null
                        when (val result = viewModel.importProductFromUrl(sourceUrlInput)) {
                            is ProductImportPreviewResult.Success -> {
                                preview = result.preview
                                productName = result.preview.detectedProductName.orEmpty()
                                description = result.preview.detectedDescription.orEmpty()
                                imageUrl = result.preview.detectedImageUrl.orEmpty()
                                origin = result.preview.detectedOrigin.orEmpty()
                                roastLevel = result.preview.detectedRoastLevel.orEmpty()
                                process = result.preview.detectedProcess.orEmpty()
                                tastingNotesRaw = result.preview.detectedTastingNotes.joinToString(", ")
                                barcode = result.preview.barcode.orEmpty()
                            }

                            is ProductImportPreviewResult.Failure -> {
                                preview = null
                                importErrorRes = when (result.reason) {
                                    ProductImportFailureReason.INVALID_URL -> R.string.brand_import_error_invalid_url
                                    ProductImportFailureReason.UNREACHABLE -> R.string.brand_import_error_unreachable
                                    ProductImportFailureReason.NO_DATA -> R.string.brand_import_error_no_data
                                    ProductImportFailureReason.UNKNOWN -> R.string.brand_import_error_generic
                                }
                            }
                        }
                        isLoading = false
                    }
                },
                enabled = sourceUrlInput.trim().isNotEmpty() && !isLoading
            )

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
            if (activePreview != null) {
                Text(
                    text = stringResource(R.string.brand_import_preview_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = CoffeeColorTokens.textPrimary,
                    modifier = Modifier.padding(top = CoffeeSpace.xl)
                )
                Text(
                    text = stringResource(R.string.brand_import_preview_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = CoffeeColorTokens.textSecondary
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CoffeeColorTokens.surface, RoundedCornerShape(CoffeeRadius.lg))
                        .border(1.dp, CoffeeColorTokens.borderSubtle, RoundedCornerShape(CoffeeRadius.lg))
                        .padding(CoffeeSpace.lg),
                    verticalArrangement = Arrangement.spacedBy(CoffeeSpace.md)
                ) {
                    if (imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CoffeeColorTokens.surfaceSoft, RoundedCornerShape(CoffeeRadius.md))
                                .border(1.dp, CoffeeColorTokens.borderSubtle, RoundedCornerShape(CoffeeRadius.md))
                                .padding(CoffeeSpace.sm)
                        )
                    }

                    if (!activePreview.detectedBrandName.isNullOrBlank()) {
                        Text(
                            text = stringResource(
                                R.string.brand_import_detected_brand,
                                activePreview.detectedBrandName
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = CoffeeColorTokens.textSecondary
                        )
                        val detectedBrandName = activePreview.detectedBrandName.orEmpty()
                        val currentBrandName = viewModel.brandById(brandId)?.name.orEmpty()
                        if (allowDraftBrandCreation &&
                            detectedBrandName.isNotBlank() &&
                            !detectedBrandName.equals(currentBrandName, ignoreCase = true)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        CoffeeColorTokens.accentSecondary.copy(alpha = 0.5f),
                                        RoundedCornerShape(CoffeeRadius.md)
                                    )
                                    .border(
                                        1.dp,
                                        CoffeeColorTokens.borderStrong.copy(alpha = 0.55f),
                                        RoundedCornerShape(CoffeeRadius.md)
                                    )
                                    .padding(horizontal = CoffeeSpace.sm, vertical = CoffeeSpace.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(CoffeeSpace.sm)
                            ) {
                                Checkbox(
                                    checked = createDraftBrandIfMissing,
                                    onCheckedChange = { createDraftBrandIfMissing = it }
                                )
                                Text(
                                    text = stringResource(
                                        R.string.brand_import_create_missing_brand,
                                        detectedBrandName
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CoffeeColorTokens.textPrimary
                                )
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.brand_import_source, activePreview.sourceUrl),
                        style = MaterialTheme.typography.bodySmall,
                        color = CoffeeColorTokens.textSecondary
                    )

                    OutlinedTextField(
                        value = productName,
                        onValueChange = {
                            productName = it
                            saveFeedbackRes = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = fieldTextStyle,
                        colors = fieldColors,
                        label = { CoffeeLabelText(stringResource(R.string.brand_field_product_name)) },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            saveFeedbackRes = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = fieldTextStyle,
                        colors = fieldColors,
                        label = { CoffeeLabelText(stringResource(R.string.brand_field_description_optional)) },
                        minLines = 3,
                        maxLines = 5
                    )

                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = {
                            imageUrl = it
                            saveFeedbackRes = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = fieldTextStyle,
                        colors = fieldColors,
                        label = { CoffeeLabelText(stringResource(R.string.brand_import_field_image_url)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )

                    OutlinedTextField(
                        value = origin,
                        onValueChange = {
                            origin = it
                            saveFeedbackRes = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = fieldTextStyle,
                        colors = fieldColors,
                        label = { CoffeeLabelText(stringResource(R.string.brand_import_field_origin)) },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = roastLevel,
                        onValueChange = {
                            roastLevel = it
                            saveFeedbackRes = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = fieldTextStyle,
                        colors = fieldColors,
                        label = { CoffeeLabelText(stringResource(R.string.brand_import_field_roast)) },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = process,
                        onValueChange = {
                            process = it
                            saveFeedbackRes = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = fieldTextStyle,
                        colors = fieldColors,
                        label = { CoffeeLabelText(stringResource(R.string.brand_import_field_process)) },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = tastingNotesRaw,
                        onValueChange = {
                            tastingNotesRaw = it
                            saveFeedbackRes = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = fieldTextStyle,
                        colors = fieldColors,
                        label = { CoffeeLabelText(stringResource(R.string.brand_import_field_notes)) },
                        placeholder = { CoffeePlaceholderText(stringResource(R.string.brand_import_notes_hint)) },
                        minLines = 2,
                        maxLines = 4
                    )

                    OutlinedTextField(
                        value = barcode,
                        onValueChange = {
                            barcode = it
                            saveFeedbackRes = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = fieldTextStyle,
                        colors = fieldColors,
                        label = { CoffeeLabelText(stringResource(R.string.brand_import_field_barcode)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    if (activePreview.extractionWarnings.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.brand_import_warnings_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = CoffeeColorTokens.textSecondary
                        )
                        activePreview.extractionWarnings.forEach { warningCode ->
                            val warningText = warningCode.asLocalizedImportWarning()
                            Text(
                                text = "• $warningText",
                                style = MaterialTheme.typography.bodySmall,
                                color = CoffeeColorTokens.textSecondary
                            )
                        }
                    }

                    if (activePreview.extractionConfidenceNotes.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.brand_import_confidence_notes_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = CoffeeColorTokens.textSecondary
                        )
                        activePreview.extractionConfidenceNotes.forEach { noteCode ->
                            val noteText = noteCode.asLocalizedConfidenceNote()
                            Text(
                                text = "• $noteText",
                                style = MaterialTheme.typography.bodySmall,
                                color = CoffeeColorTokens.textSecondary
                            )
                        }
                    }

                    if (saveFeedbackRes != null) {
                        CoffeeFeedbackCard(
                            message = stringResource(saveFeedbackRes!!),
                            tone = if (saveFeedbackRes == R.string.brand_import_save_success ||
                                saveFeedbackRes == R.string.brand_import_save_success_with_brand
                            ) {
                                CoffeeFeedbackTone.Success
                            } else if (saveFeedbackRes == R.string.brand_import_duplicate_warning) {
                                CoffeeFeedbackTone.Warning
                            } else {
                                CoffeeFeedbackTone.Error
                            }
                        )
                    }

                    PrimaryButton(
                        text = stringResource(R.string.brand_import_save_action),
                        onClick = {
                            scope.launch {
                                isLoading = true
                                val saveResult = viewModel.saveImportedProduct(
                                    ProductImportDraft(
                                        brandId = if (createDraftBrandIfMissing) null else brandId,
                                        sourceUrl = activePreview.sourceUrl,
                                        name = productName.trim(),
                                        detectedBrandName = activePreview.detectedBrandName,
                                        createDraftBrandIfMissing = createDraftBrandIfMissing,
                                        description = description.trim().ifBlank { null },
                                        imageUrl = imageUrl.trim().ifBlank { null },
                                        origin = origin.trim().ifBlank { null },
                                        roastLevel = roastLevel.trim().ifBlank { null },
                                        process = process.trim().ifBlank { null },
                                        tastingNotes = parseNotes(tastingNotesRaw),
                                        barcode = barcode.trim().ifBlank { null }
                                    )
                                )
                                saveFeedbackRes = when (saveResult) {
                                    is ProductImportSaveResult.Success -> {
                                        if (saveResult.createdDraftBrand) {
                                            R.string.brand_import_save_success_with_brand
                                        } else {
                                            R.string.brand_import_save_success
                                        }
                                    }

                                    is ProductImportSaveResult.Duplicate -> R.string.brand_import_duplicate_warning
                                    is ProductImportSaveResult.Failure -> {
                                        when (saveResult.reason) {
                                            ProductImportSaveFailureReason.UNAUTHORIZED -> R.string.brand_error_sign_in_required
                                            ProductImportSaveFailureReason.INVALID_NAME -> R.string.brand_error_name_required
                                            ProductImportSaveFailureReason.INVALID_BRAND -> R.string.brand_error_brand_not_found
                                            ProductImportSaveFailureReason.STORE_ERROR -> R.string.brand_import_error_save
                                        }
                                    }
                                }
                                isLoading = false
                            }
                        },
                        enabled = productName.trim().isNotEmpty() && !isLoading,
                        modifier = Modifier.padding(top = CoffeeSpace.xs, bottom = CoffeeSpace.md)
                    )
                }
            }
        }
    }
}

private fun parseNotes(raw: String): List<String> {
    return raw
        .split(',', '\n')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
}

@Composable
private fun String.asLocalizedImportWarning(): String = when (this) {
    "missing_product_name" -> stringResource(R.string.brand_import_warning_missing_product_name)
    "missing_description" -> stringResource(R.string.brand_import_warning_missing_description)
    "missing_image_url" -> stringResource(R.string.brand_import_warning_missing_image)
    "missing_origin" -> stringResource(R.string.brand_import_warning_missing_origin)
    "missing_roast_level" -> stringResource(R.string.brand_import_warning_missing_roast)
    "missing_process" -> stringResource(R.string.brand_import_warning_missing_process)
    else -> stringResource(R.string.brand_import_warning_generic)
}

@Composable
private fun String.asLocalizedConfidenceNote(): String = when (this) {
    "structured_data" -> stringResource(R.string.brand_import_confidence_structured_data)
    "open_graph" -> stringResource(R.string.brand_import_confidence_open_graph)
    "fallback_text" -> stringResource(R.string.brand_import_confidence_fallback_text)
    else -> stringResource(R.string.brand_import_confidence_generic)
}
