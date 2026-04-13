package com.icoffee.app.ui.screens.brand

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.icoffee.app.data.firebase.model.FirestoreBrand
import com.icoffee.app.data.firebase.model.FirestoreBrandSuggestion
import com.icoffee.app.data.model.AppUserRole
import com.icoffee.app.data.model.BrandSuggestionStatus
import com.icoffee.app.ui.components.CoffeeEmptyStateCard
import com.icoffee.app.ui.components.PrimaryButton
import com.icoffee.app.ui.components.coffinityPressMotion
import com.icoffee.app.viewmodel.BrandAdminViewModel
import com.icoffee.app.viewmodel.SuggestionAdminViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private enum class AdminPanelSection {
    BRANDS,
    SUGGESTIONS,
    IMPORTS,
    CREATE
}

private const val STATUS_FILTER_ALL = "all"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandAdminPanelScreen(
    onBack: () -> Unit,
    onOpenBrand: (String) -> Unit,
    viewModel: BrandAdminViewModel = viewModel(),
    suggestionViewModel: SuggestionAdminViewModel = viewModel()
) {
    val isLoading by viewModel.isBrandStreamLoading.collectAsState()
    val session by viewModel.sessionState.collectAsState()
    val brands by viewModel.manageableBrandsState.collectAsState()
    var feedbackRes by remember { mutableStateOf<Int?>(null) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showImportSheet by remember { mutableStateOf(false) }
    var selectedSectionKey by rememberSaveable { mutableStateOf(AdminPanelSection.BRANDS.name) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf(STATUS_FILTER_ALL) }
    var suggestionQuery by rememberSaveable { mutableStateOf("") }
    var suggestionStatusFilter by rememberSaveable { mutableStateOf("all") }
    var selectedSuggestionId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedMergeBrandId by rememberSaveable { mutableStateOf("") }
    var suggestionNotes by rememberSaveable { mutableStateOf("") }
    var suggestionRejectReason by rememberSaveable { mutableStateOf("") }
    var approvalName by rememberSaveable { mutableStateOf("") }
    var approvalDescription by rememberSaveable { mutableStateOf("") }
    var approvalWebsite by rememberSaveable { mutableStateOf("") }
    var approvalInstagram by rememberSaveable { mutableStateOf("") }
    var approvalCountry by rememberSaveable { mutableStateOf("") }
    var approvalCity by rememberSaveable { mutableStateOf("") }
    var publishAsActive by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel.refreshVersion, session?.role, selectedSectionKey, suggestionQuery, suggestionStatusFilter) {
        if (session?.role == AppUserRole.SUPER_ADMIN &&
            selectedSectionKey == AdminPanelSection.SUGGESTIONS.name
        ) {
            suggestionViewModel.refreshSuggestions(
                statusFilter = suggestionStatusFilter.takeIf { it != "all" },
                query = suggestionQuery
            )
        }
    }

    val suggestions = suggestionViewModel.suggestions
    val selectedSuggestion = suggestions.firstOrNull { it.id == selectedSuggestionId }

    LaunchedEffect(selectedSuggestion?.id) {
        val suggestion = selectedSuggestion ?: return@LaunchedEffect
        selectedMergeBrandId = suggestion.duplicateCandidateBrandIds.firstOrNull().orEmpty()
        suggestionNotes = suggestion.adminNotes.orEmpty()
        suggestionRejectReason = suggestion.rejectionReason.orEmpty()
        approvalName = suggestion.brandName
        approvalDescription = suggestion.description.orEmpty()
        approvalWebsite = suggestion.websiteUrl.orEmpty()
        approvalInstagram = suggestion.instagramUrl.orEmpty()
        approvalCountry = suggestion.country.orEmpty()
        approvalCity = suggestion.city.orEmpty()
        publishAsActive = false
        suggestionViewModel.loadLogs(suggestion.id)
    }

    val role = session?.role
    val availableSections = remember(role) {
        when (role) {
            AppUserRole.SUPER_ADMIN -> listOf(
                AdminPanelSection.BRANDS,
                AdminPanelSection.SUGGESTIONS,
                AdminPanelSection.IMPORTS,
                AdminPanelSection.CREATE
            )
            AppUserRole.BRAND_ADMIN -> listOf(
                AdminPanelSection.BRANDS,
                AdminPanelSection.IMPORTS
            )
            else -> emptyList()
        }
    }

    LaunchedEffect(availableSections) {
        val selected = availableSections.firstOrNull { it.name == selectedSectionKey }
        if (selected == null) {
            selectedSectionKey = availableSections.firstOrNull()?.name ?: AdminPanelSection.BRANDS.name
        }
    }

    val selectedSection = availableSections.firstOrNull { it.name == selectedSectionKey }

    if (showCreateSheet) {
        BrandCreateSheet(
            viewModel = viewModel,
            onDismiss = { showCreateSheet = false }
        )
    }

    if (showImportSheet) {
        BrandImportSheet(
            viewModel = viewModel,
            onDismiss = { showImportSheet = false }
        )
    }

    if (selectedSuggestion != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedSuggestionId = null },
            containerColor = Color(0xFFFFFCF8)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.brand_admin_suggestion_detail_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF2C1A12)
                )
                Text(
                    text = selectedSuggestion.brandName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF3C2318)
                )
                Text(
                    text = selectedSuggestion.status.asSuggestionStatusLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF705442)
                )

                val submittedMeta = listOfNotNull(
                    selectedSuggestion.submittedByDisplayName.takeIf { it.isNotBlank() },
                    selectedSuggestion.submittedByEmail.takeIf { it.isNotBlank() }
                ).joinToString(" • ")
                if (submittedMeta.isNotBlank()) {
                    Text(
                        text = submittedMeta,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7A5E4D)
                    )
                }

                val candidateBrands = brands.filter { it.id in selectedSuggestion.duplicateCandidateBrandIds }
                if (candidateBrands.isNotEmpty()) {
                    Text(
                        text = stringResource(
                            R.string.brand_admin_duplicate_candidates_count,
                            candidateBrands.size
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF6B4C38)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(candidateBrands, key = { it.id }) { brand ->
                            AdminActionChip(
                                text = brand.name,
                                onClick = { selectedMergeBrandId = brand.id }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = suggestionNotes,
                    onValueChange = { suggestionNotes = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    label = { Text(stringResource(R.string.brand_admin_suggestion_notes)) }
                )

                OutlinedTextField(
                    value = approvalName,
                    onValueChange = { approvalName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.brand_field_brand_name)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = approvalDescription,
                    onValueChange = { approvalDescription = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    label = { Text(stringResource(R.string.brand_field_description_optional)) }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = approvalWebsite,
                        onValueChange = { approvalWebsite = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.brand_suggest_website_optional)) }
                    )
                    OutlinedTextField(
                        value = approvalInstagram,
                        onValueChange = { approvalInstagram = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.brand_suggest_instagram_optional)) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = approvalCountry,
                        onValueChange = { approvalCountry = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.brand_field_country_optional)) }
                    )
                    OutlinedTextField(
                        value = approvalCity,
                        onValueChange = { approvalCity = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.brand_suggest_city_optional)) }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusFilterChip(
                        text = stringResource(R.string.brand_status_draft),
                        selected = !publishAsActive,
                        onClick = { publishAsActive = false }
                    )
                    StatusFilterChip(
                        text = stringResource(R.string.brand_status_active),
                        selected = publishAsActive,
                        onClick = { publishAsActive = true }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminActionChip(
                        text = stringResource(R.string.brand_admin_action_mark_under_review),
                        onClick = {
                            suggestionViewModel.markUnderReview(
                                suggestionId = selectedSuggestion.id,
                                notes = suggestionNotes
                            ) { result ->
                                feedbackRes = if (result is com.icoffee.app.data.suggestion.SuggestionAdminActionResult.Success) {
                                    R.string.brand_admin_suggestion_under_review
                                } else {
                                    R.string.brand_import_error_save
                                }
                                suggestionViewModel.refreshSuggestions(
                                    statusFilter = suggestionStatusFilter.takeIf { it != "all" },
                                    query = suggestionQuery
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    AdminActionChip(
                        text = stringResource(R.string.brand_admin_action_convert_draft),
                        onClick = {
                            suggestionViewModel.approveAsNewBrand(
                                suggestion = selectedSuggestion,
                                brandName = approvalName,
                                description = approvalDescription,
                                websiteUrl = approvalWebsite,
                                instagramUrl = approvalInstagram,
                                country = approvalCountry,
                                city = approvalCity,
                                publishAsActive = publishAsActive,
                                notes = suggestionNotes
                            ) { result ->
                                feedbackRes = if (result is com.icoffee.app.data.suggestion.SuggestionAdminActionResult.Success) {
                                    if (publishAsActive) {
                                        R.string.brand_admin_suggestion_converted_active
                                    } else {
                                        R.string.brand_admin_suggestion_converted_draft
                                    }
                                } else {
                                    R.string.brand_import_error_save
                                }
                                suggestionViewModel.refreshSuggestions(
                                    statusFilter = suggestionStatusFilter.takeIf { it != "all" },
                                    query = suggestionQuery
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = selectedMergeBrandId,
                    onValueChange = { selectedMergeBrandId = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.brand_admin_merge_target_brand_id)) }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminActionChip(
                        text = stringResource(R.string.brand_admin_action_merge),
                        onClick = {
                            if (selectedMergeBrandId.isBlank()) {
                                feedbackRes = R.string.brand_admin_error_select_merge_target
                            } else {
                                suggestionViewModel.mergeIntoExistingBrand(
                                    suggestionId = selectedSuggestion.id,
                                    targetBrandId = selectedMergeBrandId,
                                    notes = suggestionNotes
                                ) { result ->
                                    feedbackRes = if (result is com.icoffee.app.data.suggestion.SuggestionAdminActionResult.Success) {
                                        R.string.brand_admin_suggestion_merged
                                    } else {
                                        R.string.brand_import_error_save
                                    }
                                    suggestionViewModel.refreshSuggestions(
                                        statusFilter = suggestionStatusFilter.takeIf { it != "all" },
                                        query = suggestionQuery
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    AdminActionChip(
                        text = stringResource(R.string.brand_admin_action_reject),
                        onClick = {
                            suggestionViewModel.reject(
                                suggestionId = selectedSuggestion.id,
                                rejectionReason = suggestionRejectReason,
                                notes = suggestionNotes
                            ) { result ->
                                feedbackRes = if (result is com.icoffee.app.data.suggestion.SuggestionAdminActionResult.Success) {
                                    R.string.brand_admin_suggestion_rejected
                                } else {
                                    R.string.brand_import_error_save
                                }
                                suggestionViewModel.refreshSuggestions(
                                    statusFilter = suggestionStatusFilter.takeIf { it != "all" },
                                    query = suggestionQuery
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = suggestionRejectReason,
                    onValueChange = { suggestionRejectReason = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    label = { Text(stringResource(R.string.brand_admin_rejection_reason)) }
                )

                val logs = suggestionViewModel.actionLogs
                if (logs.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.brand_admin_suggestion_action_history),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF3A2318)
                    )
                    logs.forEach { log ->
                        Text(
                            text = "• ${log.actionType} (${log.previousStatus.orEmpty()} → ${log.nextStatus.orEmpty()})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6A4F3E)
                        )
                    }
                }
            }
        }
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.brand_admin_panel_v2_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFF7E5D0)
                        )
                        val scopeLabel = when (role) {
                            AppUserRole.SUPER_ADMIN -> stringResource(R.string.brand_admin_scope_super)
                            AppUserRole.BRAND_ADMIN -> stringResource(R.string.brand_admin_scope_brand)
                            else -> null
                        }
                        if (scopeLabel != null) {
                            Text(
                                text = scopeLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xD8D0B396)
                            )
                        }
                    }
                }
            }

            when {
                isLoading -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFD0A77A))
                        }
                    }
                }

                session == null || session?.canAccessPanel != true -> {
                    item {
                        ManagementInfoCard(
                            title = stringResource(R.string.brand_admin_panel_unauthorized_title),
                            subtitle = stringResource(R.string.brand_admin_panel_unauthorized_subtitle)
                        )
                    }
                }

                else -> {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableSections, key = { it.name }) { section ->
                                AdminSectionChip(
                                    title = stringResource(section.titleRes()),
                                    selected = selectedSection == section,
                                    onClick = { selectedSectionKey = section.name }
                                )
                            }
                        }
                    }

                    when (selectedSection ?: AdminPanelSection.BRANDS) {
                        AdminPanelSection.BRANDS -> {
                            item {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    label = { Text(stringResource(R.string.brand_admin_search_label)) },
                                    placeholder = { Text(stringResource(R.string.brand_admin_search_placeholder)) }
                                )
                            }

                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(
                                        listOf(
                                            STATUS_FILTER_ALL,
                                            "draft",
                                            "active",
                                            "claimed",
                                            "business"
                                        ),
                                        key = { it }
                                    ) { filter ->
                                        StatusFilterChip(
                                            text = filter.asStatusFilterLabel(),
                                            selected = statusFilter == filter,
                                            onClick = { statusFilter = filter }
                                        )
                                    }
                                }
                            }

                            val filteredBrands = brands.filter { brand ->
                                val query = searchQuery.trim().lowercase()
                                val matchesQuery = query.isBlank() ||
                                    brand.name.lowercase().contains(query) ||
                                    brand.city.lowercase().contains(query) ||
                                    brand.country.lowercase().contains(query)
                                val normalizedStatus = brand.status.normalizeBrandStatus()
                                val matchesStatus = statusFilter == STATUS_FILTER_ALL ||
                                    normalizedStatus == statusFilter
                                matchesQuery && matchesStatus
                            }

                            if (filteredBrands.isEmpty()) {
                                item {
                                    ManagementInfoCard(
                                        title = stringResource(R.string.brand_admin_brands_empty_title),
                                        subtitle = stringResource(R.string.brand_admin_brands_empty_subtitle)
                                    )
                                }
                            } else {
                                items(filteredBrands, key = { it.id }) { brand ->
                                    BrandManageRowV2(
                                        brand = brand,
                                        onClick = { onOpenBrand(brand.id) }
                                    )
                                }
                            }
                        }

                        AdminPanelSection.SUGGESTIONS -> {
                            item {
                                Text(
                                    text = stringResource(R.string.brand_admin_review_suggestions),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFFF7E5D0)
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = suggestionQuery,
                                    onValueChange = { suggestionQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    label = { Text(stringResource(R.string.brand_admin_suggestions_search_label)) },
                                    placeholder = { Text(stringResource(R.string.brand_admin_suggestions_search_placeholder)) }
                                )
                            }

                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(
                                        listOf(
                                            "all",
                                            BrandSuggestionStatus.PENDING.storageValue,
                                            BrandSuggestionStatus.UNDER_REVIEW.storageValue,
                                            BrandSuggestionStatus.APPROVED_NEW_BRAND.storageValue,
                                            BrandSuggestionStatus.MERGED_EXISTING_BRAND.storageValue,
                                            BrandSuggestionStatus.REJECTED.storageValue
                                        ),
                                        key = { it }
                                    ) { filter ->
                                        StatusFilterChip(
                                            text = filter.asSuggestionStatusLabel(),
                                            selected = suggestionStatusFilter == filter,
                                            onClick = { suggestionStatusFilter = filter }
                                        )
                                    }
                                }
                            }

                            if (suggestions.isEmpty()) {
                                item {
                                    ManagementInfoCard(
                                        title = stringResource(R.string.brand_admin_suggestions_empty_title),
                                        subtitle = stringResource(R.string.brand_admin_suggestions_empty_subtitle)
                                    )
                                }
                            } else {
                                items(suggestions, key = { it.id }) { suggestion ->
                                    BrandSuggestionReviewRow(
                                        suggestion = suggestion,
                                        onOpenDetail = { selectedSuggestionId = suggestion.id }
                                    )
                                }
                            }
                        }

                        AdminPanelSection.IMPORTS -> {
                            item {
                                ManagementInfoCard(
                                    title = stringResource(R.string.brand_admin_imports_title),
                                    subtitle = stringResource(R.string.brand_admin_imports_subtitle)
                                )
                            }

                            if (role == AppUserRole.SUPER_ADMIN) {
                                item {
                                    PrimaryButton(
                                        text = stringResource(R.string.brand_admin_import_brand),
                                        onClick = { showImportSheet = true }
                                    )
                                }
                            }

                            if (brands.isEmpty()) {
                                item {
                                    ManagementInfoCard(
                                        title = stringResource(R.string.brand_admin_imports_empty_title),
                                        subtitle = stringResource(R.string.brand_admin_imports_empty_subtitle)
                                    )
                                }
                            } else {
                                items(brands, key = { it.id }) { brand ->
                                    BrandImportEntryRow(
                                        brand = brand,
                                        onClick = { onOpenBrand(brand.id) }
                                    )
                                }
                            }
                        }

                        AdminPanelSection.CREATE -> {
                            item {
                                ManagementInfoCard(
                                    title = stringResource(R.string.brand_admin_create_title),
                                    subtitle = stringResource(R.string.brand_admin_create_subtitle)
                                )
                            }
                            item {
                                PrimaryButton(
                                    text = stringResource(R.string.brand_admin_create_brand),
                                    onClick = { showCreateSheet = true }
                                )
                            }
                        }
                    }

                    if (feedbackRes != null) {
                        item {
                            Text(
                                text = stringResource(feedbackRes!!),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD8D0B396)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSectionChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Color(0x3F5A3D2A) else Color(0x203A2419))
            .border(
                width = 1.dp,
                color = if (selected) Color(0x7BE5C49D) else Color(0x3AE5C49D),
                shape = RoundedCornerShape(999.dp)
            )
            .coffinityPressMotion(
                interactionSource = interactionSource,
                pressedScale = 0.98f,
                pressedAlpha = 0.98f
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFF2DFC9)
        )
    }
}

@Composable
private fun StatusFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Color(0x375E3D2A) else Color(0x163A2419))
            .border(
                width = 1.dp,
                color = if (selected) Color(0x6FE5C49D) else Color(0x30E5C49D),
                shape = RoundedCornerShape(999.dp)
            )
            .coffinityPressMotion(
                interactionSource = interactionSource,
                pressedScale = 0.98f,
                pressedAlpha = 0.98f
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 11.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFEFDCC6)
        )
    }
}

@Composable
private fun BrandManageRowV2(
    brand: FirestoreBrand,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xA03A2419))
            .border(1.dp, Color(0x4FE5C49D), RoundedCornerShape(16.dp))
            .coffinityPressMotion(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Storefront,
                contentDescription = null,
                tint = Color(0xFFE9C29A)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = brand.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFF6E4D0)
                    )
                    AdminMiniBadge(text = brand.status.normalizeBrandStatus().asStatusFilterLabel())
                }
                val locationLine = listOfNotNull(
                    brand.city.takeIf { it.isNotBlank() },
                    brand.country.takeIf { it.isNotBlank() }
                ).joinToString(", ")
                if (locationLine.isNotBlank()) {
                    Text(
                        text = locationLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xD8D0B396)
                    )
                }
                Text(
                    text = brand.ownerEmail?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.brand_admin_owner_unassigned),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xCFCDAE8E)
                )
                val ratingLine = if (brand.reviewCount > 0) {
                    stringResource(
                        R.string.brand_rating_summary,
                        String.format("%.1f", brand.averageRating),
                        brand.reviewCount
                    )
                } else {
                    stringResource(R.string.brand_no_reviews_yet)
                }
                Text(
                    text = "$ratingLine • ${stringResource(R.string.brand_product_count, brand.productCount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xC6C5A989)
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xD8D0B396)
        )
    }
}

@Composable
private fun BrandImportEntryRow(
    brand: FirestoreBrand,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xA03A2419))
            .border(1.dp, Color(0x4FE5C49D), RoundedCornerShape(16.dp))
            .coffinityPressMotion(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = brand.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFF6E4D0)
                )
                AdminMiniBadge(text = brand.status.normalizeBrandStatus().asStatusFilterLabel())
            }
            Text(
                text = stringResource(R.string.brand_admin_imports_brand_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xD8D0B396)
            )
        }
        Text(
            text = stringResource(R.string.brand_admin_imports_open_brand_tools),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFFE8C39D)
        )
    }
}

@Composable
private fun AdminMiniBadge(text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0x2D5A3D2A))
            .border(1.dp, Color(0x44E5C49D), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFEBD3BC)
        )
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

@Composable
private fun BrandSuggestionReviewRow(
    suggestion: FirestoreBrandSuggestion,
    onOpenDetail: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xA03A2419))
            .border(1.dp, Color(0x4FE5C49D), RoundedCornerShape(16.dp))
            .coffinityPressMotion(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onOpenDetail
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
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
            AdminMiniBadge(text = suggestion.status.asSuggestionStatusLabel())
        }
        val meta = listOfNotNull(
            suggestion.city?.takeIf { it.isNotBlank() },
            suggestion.country?.takeIf { it.isNotBlank() },
            suggestion.websiteUrl?.takeIf { it.isNotBlank() }
        ).joinToString(" • ")
        if (meta.isNotBlank()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xD8D0B396)
            )
        }
        val submittedMeta = listOfNotNull(
            suggestion.submittedByDisplayName.takeIf { it.isNotBlank() },
            suggestion.submittedByEmail.takeIf { it.isNotBlank() },
            formatSuggestionDate(suggestion.createdAt)
        ).joinToString(" • ")
        if (submittedMeta.isNotBlank()) {
            Text(
                text = submittedMeta,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xCFCBAA8C)
            )
        }
        if (suggestion.flagsPossibleDuplicate) {
            Text(
                text = stringResource(
                    R.string.brand_admin_duplicate_candidates_count,
                    suggestion.duplicateCandidateBrandIds.size
                ),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE6C39D)
            )
        }
    }
}

@Composable
private fun AdminActionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0x2B5A3D2A))
            .border(1.dp, Color(0x44E5C49D), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFF2DFC9)
        )
    }
}

@Composable
private fun String.asStatusFilterLabel(): String = when (this.normalizeBrandStatus()) {
    STATUS_FILTER_ALL -> stringResource(R.string.brand_admin_filter_all_statuses)
    "draft" -> stringResource(R.string.brand_status_draft)
    "active" -> stringResource(R.string.brand_status_active)
    "claimed" -> stringResource(R.string.brand_status_claimed)
    "business" -> stringResource(R.string.brand_status_business)
    else -> stringResource(R.string.brand_status_draft)
}

@Composable
private fun String.asSuggestionStatusLabel(): String = when (trim().lowercase()) {
    "all" -> stringResource(R.string.brand_admin_filter_all_statuses)
    BrandSuggestionStatus.PENDING.storageValue -> stringResource(R.string.suggestion_status_pending)
    BrandSuggestionStatus.UNDER_REVIEW.storageValue -> stringResource(R.string.suggestion_status_under_review)
    BrandSuggestionStatus.APPROVED_NEW_BRAND.storageValue -> stringResource(R.string.suggestion_status_approved_new_brand)
    BrandSuggestionStatus.MERGED_EXISTING_BRAND.storageValue -> stringResource(R.string.suggestion_status_merged_existing_brand)
    BrandSuggestionStatus.REJECTED.storageValue -> stringResource(R.string.suggestion_status_rejected)
    else -> stringResource(R.string.suggestion_status_pending)
}

private fun String.normalizeBrandStatus(): String = trim().lowercase().ifBlank { "draft" }

private fun AdminPanelSection.titleRes(): Int = when (this) {
    AdminPanelSection.BRANDS -> R.string.brand_admin_section_brands
    AdminPanelSection.SUGGESTIONS -> R.string.brand_admin_section_suggestions
    AdminPanelSection.IMPORTS -> R.string.brand_admin_section_imports
    AdminPanelSection.CREATE -> R.string.brand_admin_section_create
}

private fun formatSuggestionDate(epochMillis: Long): String {
    if (epochMillis <= 0L) return ""
    val formatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
    return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(formatter)
}
