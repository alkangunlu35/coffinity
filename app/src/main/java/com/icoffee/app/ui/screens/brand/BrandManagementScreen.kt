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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.icoffee.app.data.admin.BrandManagementFailureReason
import com.icoffee.app.data.admin.BrandManagementResult
import com.icoffee.app.data.admin.BrandManagementSession
import com.icoffee.app.data.admin.BrandOwnershipFailureReason
import com.icoffee.app.data.admin.BrandOwnershipResult
import com.icoffee.app.data.firebase.model.FirestoreBrand
import com.icoffee.app.data.firebase.model.FirestoreProduct
import com.icoffee.app.data.model.AppUserRole
import com.icoffee.app.data.model.BrandLifecycleStatus
import com.icoffee.app.ui.components.CoffeeEmptyStateCard
import com.icoffee.app.ui.components.PrimaryButton
import com.icoffee.app.viewmodel.BrandAdminViewModel
import com.icoffee.app.viewmodel.BrandViewModel
import kotlinx.coroutines.launch

@Composable
fun BrandManagementScreen(
    brandId: String,
    onBack: () -> Unit,
    adminViewModel: BrandAdminViewModel = viewModel(),
    brandViewModel: BrandViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var session by remember { mutableStateOf<BrandManagementSession?>(null) }
    var brand by remember { mutableStateOf<FirestoreBrand?>(null) }
    var products by remember { mutableStateOf<List<FirestoreProduct>>(emptyList()) }
    var showImportSheet by rememberSaveable { mutableStateOf(false) }

    var feedbackMessageRes by rememberSaveable { mutableStateOf<Int?>(null) }

    var brandName by rememberSaveable { mutableStateOf("") }
    var brandDescription by rememberSaveable { mutableStateOf("") }
    var brandCountry by rememberSaveable { mutableStateOf("") }
    var brandCity by rememberSaveable { mutableStateOf("") }
    var brandWebsite by rememberSaveable { mutableStateOf("") }
    var brandInstagram by rememberSaveable { mutableStateOf("") }
    var brandStatus by rememberSaveable { mutableStateOf(BrandLifecycleStatus.DRAFT.storageValue) }

    var productName by rememberSaveable { mutableStateOf("") }
    var productDescription by rememberSaveable { mutableStateOf("") }
    var productOrigin by rememberSaveable { mutableStateOf("") }
    var productRoast by rememberSaveable { mutableStateOf("") }
    var productProcess by rememberSaveable { mutableStateOf("") }

    var ownerEmail by rememberSaveable { mutableStateOf("") }
    var showRemoveBrandDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(adminViewModel.refreshVersion, brandId) {
        isLoading = true
        feedbackMessageRes = null
        session = adminViewModel.session()
        brand = adminViewModel.manageableBrandById(brandId)
        products = adminViewModel.productsForBrand(brandId)
        isLoading = false
    }

    LaunchedEffect(brand?.id) {
        val current = brand ?: return@LaunchedEffect
        brandName = current.name
        brandDescription = current.description
        brandCountry = current.country
        brandCity = current.city
        brandWebsite = current.website.orEmpty()
        brandInstagram = current.instagram.orEmpty()
        brandStatus = BrandLifecycleStatus.fromStorage(current.status).storageValue
    }

    if (showImportSheet) {
        BrandProductImportSheet(
            brandId = brandId,
            allowDraftBrandCreation = session?.role == AppUserRole.SUPER_ADMIN,
            viewModel = brandViewModel,
            onDismiss = { showImportSheet = false }
        )
    }

    if (showRemoveBrandDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveBrandDialog = false },
            title = {
                Text(text = stringResource(R.string.brand_management_remove_confirm_title))
            },
            text = {
                Text(text = stringResource(R.string.brand_management_remove_confirm_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveBrandDialog = false
                        scope.launch {
                            val result = adminViewModel.removeBrand(brandId)
                            feedbackMessageRes = when (result) {
                                BrandManagementResult.Success -> R.string.brand_management_brand_removed
                                is BrandManagementResult.Failure -> result.reason.toManagementFailureRes()
                            }
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.brand_management_remove_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveBrandDialog = false }) {
                    Text(text = stringResource(R.string.brand_management_remove_cancel_action))
                }
            }
        )
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 156.dp),
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
                        text = stringResource(R.string.brand_management_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFF7E5D0)
                    )
                }
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFD0A77A))
                    }
                }
            } else if (session == null || session?.canAccessPanel != true) {
                item {
                    ManagementInfoCard(
                        title = stringResource(R.string.brand_admin_panel_unauthorized_title),
                        subtitle = stringResource(R.string.brand_admin_panel_unauthorized_subtitle)
                    )
                }
            } else if (brand == null) {
                item {
                    ManagementInfoCard(
                        title = stringResource(R.string.brand_error_brand_not_found),
                        subtitle = stringResource(R.string.brand_error_brand_not_found)
                    )
                }
            } else {
                item {
                    ManagementBlock(
                        title = stringResource(R.string.brand_management_brand_info_title),
                        subtitle = stringResource(R.string.brand_management_brand_info_subtitle)
                    ) {
                        OutlinedTextField(
                            value = brandName,
                            onValueChange = {
                                brandName = it
                                feedbackMessageRes = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.brand_field_brand_name)) },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = brandDescription,
                            onValueChange = {
                                brandDescription = it
                                feedbackMessageRes = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.brand_field_description_optional)) },
                            minLines = 3,
                            maxLines = 5
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = brandCountry,
                                onValueChange = {
                                    brandCountry = it
                                    feedbackMessageRes = null
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.brand_field_country_optional)) },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = brandCity,
                                onValueChange = {
                                    brandCity = it
                                    feedbackMessageRes = null
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.brand_management_city_optional)) },
                                singleLine = true
                            )
                        }
                        OutlinedTextField(
                            value = brandWebsite,
                            onValueChange = {
                                brandWebsite = it
                                feedbackMessageRes = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.brand_suggest_website_optional)) },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = brandInstagram,
                            onValueChange = {
                                brandInstagram = it
                                feedbackMessageRes = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.brand_suggest_instagram_optional)) },
                            singleLine = true
                        )
                        if (session?.role == AppUserRole.SUPER_ADMIN) {
                            BrandStatusSelector(
                                selectedStatus = brandStatus,
                                onSelected = {
                                    brandStatus = it
                                    feedbackMessageRes = null
                                }
                            )
                        }
                        PrimaryButton(
                            text = stringResource(R.string.brand_management_save_brand),
                            onClick = {
                                scope.launch {
                                    val result = adminViewModel.updateBrand(
                                        brandId = brandId,
                                        name = brandName,
                                        description = brandDescription,
                                        country = brandCountry,
                                        city = brandCity,
                                        website = brandWebsite,
                                        instagram = brandInstagram,
                                        status = if (session?.role == AppUserRole.SUPER_ADMIN) {
                                            brandStatus
                                        } else {
                                            null
                                        }
                                    )
                                    feedbackMessageRes = when (result) {
                                        BrandManagementResult.Success -> R.string.brand_management_saved
                                        is BrandManagementResult.Failure -> result.reason.toManagementFailureRes()
                                    }
                                }
                            }
                        )
                    }
                }

                item {
                    ManagementBlock(
                        title = stringResource(R.string.brand_management_products_title),
                        subtitle = stringResource(R.string.brand_management_products_subtitle)
                    ) {
                        OutlinedTextField(
                            value = productName,
                            onValueChange = {
                                productName = it
                                feedbackMessageRes = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.brand_field_product_name)) },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = productDescription,
                            onValueChange = {
                                productDescription = it
                                feedbackMessageRes = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.brand_field_description_optional)) },
                            minLines = 2,
                            maxLines = 4
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = productOrigin,
                                onValueChange = {
                                    productOrigin = it
                                    feedbackMessageRes = null
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.brand_import_field_origin)) },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = productRoast,
                                onValueChange = {
                                    productRoast = it
                                    feedbackMessageRes = null
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.brand_import_field_roast)) },
                                singleLine = true
                            )
                        }
                        OutlinedTextField(
                            value = productProcess,
                            onValueChange = {
                                productProcess = it
                                feedbackMessageRes = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.brand_import_field_process)) },
                            singleLine = true
                        )
                        PrimaryButton(
                            text = stringResource(R.string.brand_management_add_product),
                            enabled = productName.trim().isNotBlank(),
                            onClick = {
                                scope.launch {
                                    val result = adminViewModel.addProduct(
                                        brandId = brandId,
                                        name = productName,
                                        description = productDescription,
                                        origin = productOrigin,
                                        roastLevel = productRoast,
                                        process = productProcess
                                    )
                                    feedbackMessageRes = when (result) {
                                        BrandManagementResult.Success -> {
                                            productName = ""
                                            productDescription = ""
                                            productOrigin = ""
                                            productRoast = ""
                                            productProcess = ""
                                            R.string.brand_management_product_added
                                        }

                                        is BrandManagementResult.Failure -> result.reason.toManagementFailureRes()
                                    }
                                }
                            }
                        )
                        PrimaryButton(
                            text = stringResource(R.string.brand_management_open_import),
                            onClick = { showImportSheet = true }
                        )
                    }
                }

                if (products.isEmpty()) {
                    item {
                        ManagementInfoCard(
                            title = stringResource(R.string.brand_management_products_empty_title),
                            subtitle = stringResource(R.string.brand_management_products_empty_subtitle)
                        )
                    }
                } else {
                    items(products, key = { it.id }) { product ->
                        ManagedProductRow(
                            product = product,
                            onDelete = {
                                scope.launch {
                                    val result = adminViewModel.removeProduct(product.id)
                                    feedbackMessageRes = when (result) {
                                        BrandManagementResult.Success -> R.string.brand_management_product_removed
                                        is BrandManagementResult.Failure -> result.reason.toManagementFailureRes()
                                    }
                                }
                            }
                        )
                    }
                }

                if (session?.role == AppUserRole.SUPER_ADMIN) {
                    item {
                        ManagementBlock(
                            title = stringResource(R.string.brand_management_owner_title),
                            subtitle = stringResource(R.string.brand_management_owner_subtitle)
                        ) {
                            val ownerLabel = brand?.ownerEmail
                                ?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.brand_admin_owner_unassigned)
                            Text(
                                text = stringResource(R.string.brand_management_current_owner, ownerLabel),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xD8D0B396)
                            )
                            OutlinedTextField(
                                value = ownerEmail,
                                onValueChange = {
                                    ownerEmail = it
                                    feedbackMessageRes = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.brand_management_owner_email_label)) },
                                singleLine = true
                            )
                            PrimaryButton(
                                text = stringResource(R.string.brand_management_assign_owner),
                                onClick = {
                                    scope.launch {
                                        val result = adminViewModel.assignBrandAdmin(
                                            brandId = brandId,
                                            ownerEmail = ownerEmail
                                        )
                                        feedbackMessageRes = when (result) {
                                            BrandOwnershipResult.Success -> {
                                                ownerEmail = ""
                                                R.string.brand_management_owner_assigned
                                            }

                                            is BrandOwnershipResult.Failure -> result.reason.toOwnershipFailureRes()
                                        }
                                    }
                                }
                            )
                            PrimaryButton(
                                text = stringResource(R.string.brand_management_revoke_owner),
                                onClick = {
                                    scope.launch {
                                        val result = adminViewModel.revokeBrandOwnership(brandId)
                                        feedbackMessageRes = when (result) {
                                            BrandOwnershipResult.Success -> R.string.brand_management_owner_revoked
                                            is BrandOwnershipResult.Failure -> result.reason.toOwnershipFailureRes()
                                        }
                                    }
                                }
                            )
                        }
                    }

                    item {
                        ManagementBlock(
                            title = stringResource(R.string.brand_management_remove_brand_title),
                            subtitle = stringResource(R.string.brand_management_remove_brand_subtitle)
                        ) {
                            Text(
                                text = if (brand?.isDeleted == true) {
                                    stringResource(R.string.brand_management_removed_badge)
                                } else {
                                    stringResource(R.string.brand_status_active)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xD8D0B396)
                            )
                            if (brand?.isDeleted == true) {
                                PrimaryButton(
                                    text = stringResource(R.string.brand_management_restore_brand_action),
                                    onClick = {
                                        scope.launch {
                                            val result = adminViewModel.restoreBrand(brandId)
                                            feedbackMessageRes = when (result) {
                                                BrandManagementResult.Success -> R.string.brand_management_brand_restored
                                                is BrandManagementResult.Failure -> result.reason.toManagementFailureRes()
                                            }
                                        }
                                    }
                                )
                            } else {
                                PrimaryButton(
                                    text = stringResource(R.string.brand_management_remove_brand_action),
                                    onClick = { showRemoveBrandDialog = true }
                                )
                            }
                        }
                    }
                }

                if (feedbackMessageRes != null) {
                    item {
                        Text(
                            text = stringResource(feedbackMessageRes!!),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFDAB999)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagementBlock(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xA03A2419))
            .border(1.dp, Color(0x4FE5C49D), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
        content()
    }
}

@Composable
private fun ManagedProductRow(
    product: FirestoreProduct,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x8F2F1F17))
            .border(1.dp, Color(0x3AE5C49D), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFFF6E4D0)
            )
            Text(
                text = listOf(product.roastLevel, product.origin, product.process)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                    .ifBlank { stringResource(R.string.brand_management_product_meta_empty) },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xD8D0B396)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = null,
                tint = Color(0xFFD9A98C)
            )
        }
    }
}

@Composable
private fun ManagementInfoCard(
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

private fun BrandManagementFailureReason.toManagementFailureRes(): Int = when (this) {
    BrandManagementFailureReason.UNAUTHORIZED -> R.string.brand_management_error_unauthorized
    BrandManagementFailureReason.USER_NOT_FOUND -> R.string.brand_management_error_user_not_found
    BrandManagementFailureReason.BRAND_NOT_FOUND -> R.string.brand_error_brand_not_found
    BrandManagementFailureReason.PRODUCT_NOT_FOUND -> R.string.brand_management_error_product_not_found
    BrandManagementFailureReason.INVALID_INPUT -> R.string.brand_management_error_invalid_input
    BrandManagementFailureReason.INVALID_STATUS -> R.string.brand_management_error_invalid_status
    BrandManagementFailureReason.DUPLICATE -> R.string.brand_error_duplicate_existing
    BrandManagementFailureReason.STORE_ERROR -> R.string.brand_import_error_save
}

private fun BrandOwnershipFailureReason.toOwnershipFailureRes(): Int = when (this) {
    BrandOwnershipFailureReason.UNAUTHORIZED -> R.string.brand_management_error_unauthorized
    BrandOwnershipFailureReason.BRAND_NOT_FOUND -> R.string.brand_error_brand_not_found
    BrandOwnershipFailureReason.INVALID_EMAIL -> R.string.brand_management_error_invalid_email
    BrandOwnershipFailureReason.USER_NOT_FOUND -> R.string.brand_management_error_user_not_found
    BrandOwnershipFailureReason.ALREADY_ASSIGNED -> R.string.brand_management_error_already_assigned
    BrandOwnershipFailureReason.STORE_ERROR -> R.string.brand_import_error_save
}
