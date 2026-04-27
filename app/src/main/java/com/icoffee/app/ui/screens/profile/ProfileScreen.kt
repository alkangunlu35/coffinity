package com.icoffee.app.ui.screens.profile

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.icoffee.app.R
import com.icoffee.app.data.admin.BrandManagementRepository
import com.icoffee.app.data.notifications.NotificationPreferences
import com.icoffee.app.data.notifications.NotificationSettingsRepository
import com.icoffee.app.ui.components.EmptyProfileState
import com.icoffee.app.ui.components.FavoriteChipRow
import com.icoffee.app.ui.components.ProfileHorizontalItemCard
import com.icoffee.app.ui.components.ProfileSectionHeader
import com.icoffee.app.ui.components.ProfileSettingsRow
import com.icoffee.app.ui.components.TasteSummaryCard
import com.icoffee.app.localization.AppLocaleManager
import com.icoffee.app.localization.SupportedLanguage
import com.icoffee.app.ui.theme.CoffeeRadius
import com.icoffee.app.ui.theme.CoffeeSpacing
import com.icoffee.app.viewmodel.AuthViewModel
import com.icoffee.app.viewmodel.ProfileEventItem
import com.icoffee.app.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onRequestSignIn: () -> Unit,
    onOpenBrandManagement: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val languageOptions = remember { SupportedLanguage.pickerLanguages }
    var showLanguagePicker by rememberSaveable { mutableStateOf(false) }
    var showNotificationSettings by rememberSaveable { mutableStateOf(false) }
    var activeLanguage by remember { mutableStateOf(AppLocaleManager.currentLanguage(context)) }
    var canAccessBrandManagement by remember { mutableStateOf(false) }
    var notificationPreferences by remember { mutableStateOf(NotificationPreferences.default()) }
    var notificationPrefsLoaded by remember { mutableStateOf(!authViewModel.isSignedIn) }
    var hasNotificationPermission by remember { mutableStateOf(context.hasNotificationPermission()) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }
    val state = profileViewModel.uiState
    val user = state.user
    val displayName = user?.displayName?.takeIf { it.isNotBlank() }
        ?: user?.email?.substringBefore("@")
        ?: stringResource(R.string.profile_default_name)
    val email = user?.email.orEmpty()
    val photoUrl = user?.photoUrl?.toString()
    val initial = displayName.firstOrNull()?.toString()?.uppercase(AppLocaleManager.currentLocale(context))
        ?: stringResource(R.string.profile_default_initial)

    val topNotesText = state.tasteSummary.topNotes
        .takeIf { it.isNotEmpty() }
        ?.joinToString(stringResource(R.string.profile_list_separator)) { it.asUi(context) }
        ?: stringResource(R.string.profile_taste_learning_notes)
    val roastText = state.tasteSummary.likelyRoast?.asUi(context)
        ?: stringResource(R.string.profile_unknown_roast)
    val acidityText = state.tasteSummary.likelyAcidity?.asUi(context)
        ?: stringResource(R.string.profile_unknown_acidity)
    val milkText = when (state.tasteSummary.likelyMilkFriendly) {
        true -> stringResource(R.string.profile_milk_friendly)
        false -> stringResource(R.string.profile_prefers_black)
        null -> stringResource(R.string.profile_milk_learning)
    }
    val typeText = state.tasteSummary.topCoffeeTypes.firstOrNull()?.asUi(context)
        ?: stringResource(R.string.profile_type_learning)

    LaunchedEffect(authViewModel.isSignedIn) {
        profileViewModel.refresh()
        canAccessBrandManagement = if (authViewModel.isSignedIn) {
            BrandManagementRepository.currentSession()?.canAccessPanel == true
        } else {
            false
        }
    }

    LaunchedEffect(context) {
        activeLanguage = AppLocaleManager.currentLanguage(context)
    }

    LaunchedEffect(showLanguagePicker) {
        if (showLanguagePicker) {
            activeLanguage = AppLocaleManager.currentLanguage(context)
        }
    }

    LaunchedEffect(showNotificationSettings) {
        if (showNotificationSettings) {
            hasNotificationPermission = context.hasNotificationPermission()
        }
    }

    LaunchedEffect(authViewModel.isSignedIn, user?.uid) {
        val uid = user?.uid.orEmpty()
        if (!authViewModel.isSignedIn || uid.isBlank()) {
            notificationPreferences = NotificationPreferences.default()
            notificationPrefsLoaded = true
            return@LaunchedEffect
        }
        notificationPrefsLoaded = false
        NotificationSettingsRepository.observePreferences(uid).collect { prefs ->
            notificationPreferences = prefs
            notificationPrefsLoaded = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF20120D),
                        Color(0xFF2E1B14),
                        Color(0xFF3B251B)
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = CoffeeSpacing.lg,
                end = CoffeeSpacing.lg,
                top = CoffeeSpacing.md,
                bottom = 128.dp
            ),
            verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.lg)
        ) {
            item {
                Text(
                    text = stringResource(R.string.profile_title),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFF8E6D1)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(CoffeeRadius.xl),
                    colors = CardDefaults.cardColors(containerColor = Color(0xC73A2419))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(CoffeeSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .background(Color(0xAA5D3826))
                                .border(1.dp, Color(0x3BF2CEAA), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = stringResource(R.string.profile_avatar_desc),
                                    modifier = Modifier
                                        .size(78.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = initial,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF5E6D3)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFFF8E6D1)
                            )
                            if (email.isNotBlank()) {
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xD7D3B699)
                                )
                            }
                        }

                        if (authViewModel.isSignedIn) {
                            Button(
                                onClick = {
                                    authViewModel.signOut()
                                    profileViewModel.refresh()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4D2F21),
                                    contentColor = Color(0xFFF6E5D1)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = CoffeeSpacing.xs)
                                )
                                Text(stringResource(R.string.profile_sign_out))
                            }
                        } else {
                            Button(
                                onClick = onRequestSignIn,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6A422D),
                                    contentColor = Color(0xFFF5E6D3)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = CoffeeSpacing.xs)
                                )
                                Text(stringResource(R.string.profile_sign_in_sync))
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = CoffeeSpacing.xs),
                            color = Color(0x2EF2CEAA)
                        )

                        ProfileSettingsRow(
                            title = stringResource(R.string.settings_language_title),
                            value = stringResource(activeLanguage.displayNameResId),
                            onClick = { showLanguagePicker = true }
                        )

                        ProfileSettingsRow(
                            title = stringResource(R.string.settings_notifications_title),
                            value = if (!authViewModel.isSignedIn) {
                                stringResource(R.string.settings_notifications_sign_in_required)
                            } else if (!notificationPrefsLoaded) {
                                stringResource(R.string.settings_notifications_loading)
                            } else if (notificationPreferences.notificationsEnabled) {
                                stringResource(R.string.settings_notifications_status_on)
                            } else {
                                stringResource(R.string.settings_notifications_status_off)
                            },
                            onClick = {
                                if (authViewModel.isSignedIn) {
                                    showNotificationSettings = true
                                } else {
                                    onRequestSignIn()
                                }
                            }
                        )

                        if (canAccessBrandManagement) {
                            ProfileSettingsRow(
                                title = stringResource(R.string.settings_brand_management_title),
                                value = stringResource(R.string.settings_brand_management_subtitle),
                                onClick = onOpenBrandManagement
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)) {
                    ProfileSectionHeader(
                        title = stringResource(R.string.profile_section_my_taste),
                        subtitle = stringResource(R.string.profile_section_my_taste_subtitle)
                    )
                    TasteSummaryCard(
                        title = topNotesText,
                        subtitle = stringResource(
                            R.string.profile_taste_summary_subtitle,
                            roastText,
                            milkText,
                            typeText
                        ),
                        footer = stringResource(
                            R.string.profile_scans_analyzed,
                            acidityText,
                            state.scanHistoryCount
                        )
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)) {
                    ProfileSectionHeader(title = stringResource(R.string.profile_section_favorite_notes))
                    FavoriteChipRow(
                        values = state.favoriteNotes.map { it.asUi(context) },
                        emptyText = stringResource(R.string.profile_empty_taste_learning)
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)) {
                    ProfileSectionHeader(title = stringResource(R.string.profile_section_favorite_origins))
                    FavoriteChipRow(
                        values = state.favoriteOrigins,
                        emptyText = stringResource(R.string.profile_empty_origin_trends)
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)) {
                    ProfileSectionHeader(title = stringResource(R.string.profile_section_favorite_types))
                    FavoriteChipRow(
                        values = state.favoriteCoffeeTypes.map { it.asUi(context) },
                        emptyText = stringResource(R.string.profile_empty_format_preference)
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)) {
                    ProfileSectionHeader(title = stringResource(R.string.profile_section_favorite_scans))
                    if (state.favoriteScans.isEmpty()) {
                        EmptyProfileState(stringResource(R.string.profile_empty_favorite_scans))
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.favoriteScans, key = { it.barcode }) { scan ->
                                val brandText = scan.brand ?: stringResource(R.string.profile_value_unavailable)
                                val originText = scan.origin ?: stringResource(R.string.profile_value_unavailable)
                                ProfileHorizontalItemCard(
                                    title = scan.name,
                                    subtitle = stringResource(
                                        R.string.profile_scan_item_subtitle,
                                        brandText,
                                        originText
                                    ),
                                    meta = stringResource(R.string.profile_scan_meta_roast, scan.roast),
                                    imageUrl = scan.imageUrl
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)) {
                    ProfileSectionHeader(title = stringResource(R.string.profile_section_favorite_beans))
                    if (state.favoriteBeans.isEmpty()) {
                        EmptyProfileState(stringResource(R.string.profile_empty_favorite_beans))
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.favoriteBeans, key = { it.id }) { bean ->
                                ProfileHorizontalItemCard(
                                    title = bean.title,
                                    subtitle = bean.origin,
                                    meta = bean.subtitle.ifBlank {
                                        stringResource(R.string.profile_bean_saved_meta)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)) {
                    ProfileSectionHeader(title = stringResource(R.string.profile_section_favorite_menu_picks))
                    if (state.favoriteMenuPicks.isEmpty()) {
                        EmptyProfileState(stringResource(R.string.profile_empty_menu_picks))
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.favoriteMenuPicks, key = { it.id }) { pick ->
                                ProfileHorizontalItemCard(
                                    title = pick.title,
                                    subtitle = pick.subtitle.ifBlank {
                                        stringResource(R.string.profile_menu_saved_subtitle)
                                    },
                                    meta = stringResource(R.string.profile_menu_saved_meta)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)) {
                    ProfileSectionHeader(title = stringResource(R.string.profile_section_events_joined))
                    EventListBlock(
                        events = state.joinedEvents,
                        emptyText = stringResource(R.string.profile_empty_events_joined)
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)) {
                    ProfileSectionHeader(title = stringResource(R.string.profile_section_events_created))
                    EventListBlock(
                        events = state.createdEvents,
                        emptyText = stringResource(R.string.profile_empty_events_created)
                    )
                }
            }

        }

        if (showLanguagePicker) {
            ModalBottomSheet(
                onDismissRequest = { showLanguagePicker = false },
                containerColor = Color(0xFF2D1C14),
                contentColor = Color(0xFFF6E5D1)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CoffeeSpacing.lg, vertical = CoffeeSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_language_picker_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFF8E6D1)
                    )
                    Text(
                        text = stringResource(R.string.settings_language_picker_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xC7D5B99A)
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                        color = Color(0x2EF2CEAA)
                    )
                    languageOptions.forEach { language ->
                        LanguageOptionRow(
                            label = stringResource(language.displayNameResId),
                            selected = activeLanguage == language,
                            onClick = {
                                AppLocaleManager.setLanguage(context, language)
                                activeLanguage = language
                                showLanguagePicker = false
                                context.findActivity()?.recreate()
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        if (showNotificationSettings) {
            ModalBottomSheet(
                onDismissRequest = { showNotificationSettings = false },
                containerColor = Color(0xFF2D1C14),
                contentColor = Color(0xFFF6E5D1)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CoffeeSpacing.lg, vertical = CoffeeSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_notifications_sheet_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFF8E6D1)
                    )
                    Text(
                        text = stringResource(R.string.settings_notifications_sheet_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xC7D5B99A)
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x80332118), RoundedCornerShape(CoffeeRadius.md))
                                .border(1.dp, Color(0x2CF2CEAA), RoundedCornerShape(CoffeeRadius.md))
                                .padding(horizontal = CoffeeSpacing.md, vertical = CoffeeSpacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.settings_notifications_permission_row),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xE6DCC1A3),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                                shape = RoundedCornerShape(CoffeeRadius.pill),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6A422D),
                                    contentColor = Color(0xFFF5E6D3)
                                )
                            ) {
                                Text(stringResource(R.string.settings_notifications_permission_action))
                            }
                        }
                    }

                    fun persistPreferences(updated: NotificationPreferences) {
                        val uid = user?.uid.orEmpty()
                        if (uid.isBlank()) return
                        notificationPreferences = updated
                        coroutineScope.launch {
                            NotificationSettingsRepository.updatePreferences(uid, updated)
                        }
                    }

                    NotificationToggleRow(
                        title = stringResource(R.string.settings_notifications_master),
                        checked = notificationPreferences.notificationsEnabled,
                        onCheckedChange = { checked ->
                            persistPreferences(
                                notificationPreferences.copy(
                                    notificationsEnabled = checked,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    )
                    NotificationToggleRow(
                        title = stringResource(R.string.settings_notifications_meet_participants),
                        checked = notificationPreferences.meetParticipants,
                        enabled = notificationPreferences.notificationsEnabled,
                        onCheckedChange = { checked ->
                            persistPreferences(
                                notificationPreferences.copy(
                                    meetParticipants = checked,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    )
                    NotificationToggleRow(
                        title = stringResource(R.string.settings_notifications_meet_reminders),
                        checked = notificationPreferences.meetReminders,
                        enabled = notificationPreferences.notificationsEnabled,
                        onCheckedChange = { checked ->
                            persistPreferences(
                                notificationPreferences.copy(
                                    meetReminders = checked,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    )
                    NotificationToggleRow(
                        title = stringResource(R.string.settings_notifications_meet_updates),
                        checked = notificationPreferences.meetUpdates,
                        enabled = notificationPreferences.notificationsEnabled,
                        onCheckedChange = { checked ->
                            persistPreferences(
                                notificationPreferences.copy(
                                    meetUpdates = checked,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    )
                    NotificationToggleRow(
                        title = stringResource(R.string.settings_notifications_nearby_meet),
                        checked = notificationPreferences.nearbyMeet,
                        enabled = notificationPreferences.notificationsEnabled,
                        onCheckedChange = { checked ->
                            persistPreferences(
                                notificationPreferences.copy(
                                    nearbyMeet = checked,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    )
                    NotificationToggleRow(
                        title = stringResource(R.string.settings_notifications_recommendations),
                        checked = notificationPreferences.recommendations,
                        enabled = notificationPreferences.notificationsEnabled,
                        onCheckedChange = { checked ->
                            persistPreferences(
                                notificationPreferences.copy(
                                    recommendations = checked,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    )
                    NotificationToggleRow(
                        title = stringResource(R.string.settings_notifications_campaigns),
                        checked = notificationPreferences.campaigns,
                        enabled = notificationPreferences.notificationsEnabled,
                        onCheckedChange = { checked ->
                            persistPreferences(
                                notificationPreferences.copy(
                                    campaigns = checked,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun EventListBlock(
    events: List<ProfileEventItem>,
    emptyText: String
) {
    if (events.isEmpty()) {
        EmptyProfileState(emptyText)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        events.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xCC372218),
                        RoundedCornerShape(CoffeeRadius.md)
                    )
                    .border(
                        1.dp,
                        Color(0x2CF2CEAA),
                        RoundedCornerShape(CoffeeRadius.md)
                    )
                    .padding(horizontal = CoffeeSpacing.md, vertical = CoffeeSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFF7E8D4)
                    )
                    Text(
                        text = stringResource(R.string.profile_event_meta, item.purpose, item.time),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xDDD5B99A)
                    )
                    Text(
                        text = item.location,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xC5C8A98D)
                    )
                }
                Text(
                    text = item.participantsLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFDEB98E)
                )
            }
        }
    }
}

private fun com.icoffee.app.data.model.TasteNote.asUi(context: Context): String = when (this) {
    com.icoffee.app.data.model.TasteNote.CHOCOLATE -> context.getString(R.string.profile_note_chocolate)
    com.icoffee.app.data.model.TasteNote.NUTTY -> context.getString(R.string.profile_note_nutty)
    com.icoffee.app.data.model.TasteNote.FRUITY -> context.getString(R.string.profile_note_fruity)
    com.icoffee.app.data.model.TasteNote.FLORAL -> context.getString(R.string.profile_note_floral)
    com.icoffee.app.data.model.TasteNote.CARAMEL -> context.getString(R.string.profile_note_caramel)
    com.icoffee.app.data.model.TasteNote.BOLD -> context.getString(R.string.profile_note_bold)
    com.icoffee.app.data.model.TasteNote.SMOOTH -> context.getString(R.string.profile_note_smooth)
    com.icoffee.app.data.model.TasteNote.BRIGHT -> context.getString(R.string.profile_note_bright)
    com.icoffee.app.data.model.TasteNote.SMOKY -> context.getString(R.string.profile_note_smoky)
}

private fun com.icoffee.app.data.model.RoastLevel.asUi(context: Context): String = when (this) {
    com.icoffee.app.data.model.RoastLevel.LIGHT -> context.getString(R.string.profile_roast_light)
    com.icoffee.app.data.model.RoastLevel.MEDIUM -> context.getString(R.string.profile_roast_medium)
    com.icoffee.app.data.model.RoastLevel.DARK -> context.getString(R.string.profile_roast_dark)
    com.icoffee.app.data.model.RoastLevel.UNKNOWN -> context.getString(R.string.profile_roast_unknown)
}

private fun com.icoffee.app.data.model.AcidityLevel.asUi(context: Context): String = when (this) {
    com.icoffee.app.data.model.AcidityLevel.LOW -> context.getString(R.string.profile_acidity_low)
    com.icoffee.app.data.model.AcidityLevel.MEDIUM -> context.getString(R.string.profile_acidity_medium)
    com.icoffee.app.data.model.AcidityLevel.HIGH -> context.getString(R.string.profile_acidity_high)
    com.icoffee.app.data.model.AcidityLevel.UNKNOWN -> context.getString(R.string.profile_acidity_unknown)
}

private fun com.icoffee.app.data.model.CoffeeType.asUi(context: Context): String = when (this) {
    com.icoffee.app.data.model.CoffeeType.WHOLE_BEAN -> context.getString(R.string.profile_type_whole_bean)
    com.icoffee.app.data.model.CoffeeType.GROUND -> context.getString(R.string.profile_type_ground)
    com.icoffee.app.data.model.CoffeeType.INSTANT -> context.getString(R.string.profile_type_instant)
    com.icoffee.app.data.model.CoffeeType.CAPSULE -> context.getString(R.string.profile_type_capsule)
    com.icoffee.app.data.model.CoffeeType.READY_TO_DRINK -> context.getString(R.string.profile_type_rtd)
    com.icoffee.app.data.model.CoffeeType.UNKNOWN -> context.getString(R.string.profile_type_unknown)
}

@Composable
private fun LanguageOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> Color(0x7D4C2F22)
            isPressed -> Color(0x403F261B)
            else -> Color.Transparent
        },
        label = "languageOptionContainer"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) Color(0x6AE8C7A6) else Color(0x23F2CEAA),
        label = "languageOptionBorder"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color(0xFFF8E8D6) else Color(0xFFF2DFCA),
        label = "languageOptionText"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CoffeeRadius.md))
            .background(containerColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(CoffeeRadius.md)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = CoffeeSpacing.md, vertical = CoffeeSpacing.sm)
            ,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0x2FEBC9A5))
                    .border(1.dp, Color(0x4FE8C7A6), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFFE9C8A4),
                    modifier = Modifier.size(14.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xCC372218),
                RoundedCornerShape(CoffeeRadius.md)
            )
            .border(
                1.dp,
                Color(0x2CF2CEAA),
                RoundedCornerShape(CoffeeRadius.md)
            )
            .padding(horizontal = CoffeeSpacing.md, vertical = CoffeeSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) Color(0xF2F7E8D4) else Color(0x96D5B99A),
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.hasNotificationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}
