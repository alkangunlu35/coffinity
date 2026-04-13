package com.icoffee.app.ui.screens.meet

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.icoffee.app.R
import com.icoffee.app.data.growth.GrowthAnalytics
import com.icoffee.app.data.growth.GrowthEventNames
import com.icoffee.app.data.model.CoffeeMeet
import com.icoffee.app.data.model.EventHostType
import com.icoffee.app.data.model.MeetExploreSort
import com.icoffee.app.data.model.MeetMood
import com.icoffee.app.data.model.formattedPriceOrNull
import com.icoffee.app.ui.components.PrimaryButton
import com.icoffee.app.ui.components.CoffeeEmptyStateCard
import com.icoffee.app.ui.components.coffinityPressMotion
import com.icoffee.app.ui.theme.CoffeeSpacing
import com.icoffee.app.viewmodel.MeetViewModel

@Composable
fun MeetListScreen(
    onBack: () -> Unit,
    onCreateMeet: () -> Unit,
    onEventClick: (String) -> Unit,
    meetViewModel: MeetViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedMoodFilter by rememberSaveable { mutableStateOf("ALL") }
    var selectedSort by rememberSaveable { mutableStateOf(MeetExploreSort.RELEVANCE.name) }
    val moodFilter = MeetMood.entries.firstOrNull { it.name == selectedMoodFilter }
    val activeSort = MeetExploreSort.valueOf(selectedSort)
    val events = meetViewModel.exploreEvents(
        selectedMood = moodFilter ?: MeetMood.CHILL,
        moodFilter = moodFilter,
        sort = activeSort
    )
    val nearbyEvents = remember(
        meetViewModel.meets,
        meetViewModel.userLatitude,
        meetViewModel.userLongitude
    ) {
        meetViewModel.nearbyBoostEvents(limit = 4)
    }

    LaunchedEffect(nearbyEvents.map { it.id }.joinToString("|")) {
        if (nearbyEvents.isNotEmpty()) {
            GrowthAnalytics.log(
                GrowthEventNames.NEARBY_SECTION_VIEWED,
                params = mapOf("count" to nearbyEvents.size, "source" to "meet_list")
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5EFE7))
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = CoffeeSpacing.lg,
            end = CoffeeSpacing.lg,
            top = CoffeeSpacing.lg,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.lg)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.md)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFF0E6DB), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.meet_back),
                            tint = Color(0xFF603D2B)
                        )
                    }

                    Text(
                        text = stringResource(R.string.meet_title),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF2E2018)
                    )

                    Text(
                        text = pluralStringResource(
                            id = R.plurals.meet_nearby_count,
                            count = events.size,
                            events.size
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF7B6658)
                    )
                }

                Text(
                    text = stringResource(R.string.meet_nearby_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF7B6658)
                )

                PrimaryButton(
                    text = stringResource(R.string.meet_create),
                    onClick = onCreateMeet
                )

                Text(
                    text = stringResource(R.string.meet_section_near_you),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF2E2018)
                )

                if (nearbyEvents.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)) {
                        items(nearbyEvents, key = { it.id }) { nearby ->
                            NearbyBoostCard(
                                event = nearby,
                                onClick = {
                                    GrowthAnalytics.log(
                                        GrowthEventNames.NEARBY_EVENT_CLICKED,
                                        params = mapOf("eventId" to nearby.id, "source" to "meet_list")
                                    )
                                    onEventClick(nearby.id)
                                }
                            )
                        }
                    }
                } else {
                    CoffeeEmptyStateCard(
                        title = stringResource(R.string.meet_section_near_you),
                        subtitle = stringResource(R.string.meet_nearby_fallback_empty),
                        icon = Icons.Default.LocationOn,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        ExploreFilterChip(
                            label = stringResource(R.string.meet_filter_all),
                            selected = selectedMoodFilter == "ALL",
                            onClick = { selectedMoodFilter = "ALL" }
                        )
                    }
                    items(MeetMood.entries) { mood ->
                        ExploreFilterChip(
                            label = stringResource(mood.labelRes),
                            selected = selectedMoodFilter == mood.name,
                            onClick = { selectedMoodFilter = mood.name }
                        )
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MeetExploreSort.entries) { sort ->
                        ExploreFilterChip(
                            label = stringResource(sort.labelRes()),
                            selected = selectedSort == sort.name,
                            onClick = { selectedSort = sort.name }
                        )
                    }
                }
            }
        }

        if (events.isEmpty()) {
            item {
                CoffeeEmptyStateCard(
                    title = stringResource(R.string.meet_no_rooms),
                    subtitle = stringResource(R.string.meet_explore_empty),
                    icon = Icons.Default.LocalCafe,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }

        items(events, key = { it.id }) { event ->
            MeetEventCard(
                event = event,
                onSeeEvent = { onEventClick(event.id) },
                onShare = {
                    when (EventShareHelper.shareEvent(context = context, event = event)) {
                        EventShareHelper.ShareLaunchResult.LAUNCHED -> {
                            GrowthAnalytics.log(
                                GrowthEventNames.EVENT_SHARE_CLICKED,
                                params = mapOf("eventId" to event.id, "source" to "event_card")
                            )
                            GrowthAnalytics.log(
                                GrowthEventNames.SHARE_FROM_EVENT_CARD,
                                params = mapOf("eventId" to event.id)
                            )
                            GrowthAnalytics.log(
                                GrowthEventNames.EVENT_SHARE_COMPLETED,
                                params = mapOf("eventId" to event.id, "source" to "event_card")
                            )
                        }

                        EventShareHelper.ShareLaunchResult.THROTTLED -> Unit
                        EventShareHelper.ShareLaunchResult.FAILED -> {
                            Toast.makeText(
                                context,
                                context.getString(R.string.meet_share_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ExploreFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .background(
                if (selected) Color(0xFFDAB694) else Color(0xFFF0E3D5),
                RoundedCornerShape(999.dp)
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
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color(0xFF523423) else Color(0xFF6E594B)
        )
    }
}

@Composable
private fun MeetEventCard(
    event: CoffeeMeet,
    onSeeEvent: () -> Unit,
    onShare: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val participantLabel = stringResource(
        R.string.meet_participant_ratio,
        event.participants.size,
        event.maxParticipants.takeIf { it > 0 } ?: 10
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0x1E1D130D),
                spotColor = Color(0x281D130D)
            )
    ) {
        Column(
            modifier = Modifier.padding(CoffeeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)
        ) {
            LaunchedEffect(event.id) {
                GrowthAnalytics.log(
                    GrowthEventNames.EVENT_CARD_VIEWED,
                    params = mapOf("eventId" to event.id, "source" to "meet_list")
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF2D2018),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier,
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onShare,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0x14B67A4D), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.meet_share_action),
                                tint = Color(0xFF8F5F3A),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFECD8C3), RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = participantLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF66422F)
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(CoffeeSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF9A7352),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = event.time,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6E594B)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF9A7352),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = event.locationName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6E594B),
                        modifier = Modifier.width(138.dp)
                    )
                    IconButton(
                        onClick = { context.openMapFor(event) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0x14B67A4D), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = stringResource(R.string.meet_open_in_maps),
                            tint = Color(0xFF9A7352),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF473327)
            )

            Box(
                modifier = Modifier
                    .background(Color(0x1AB67A4D), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = event.purpose,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF946239)
                )
            }

            if (event.hostType == EventHostType.BUSINESS) {
                Box(
                    modifier = Modifier
                        .background(Color(0x1A8F5F3A), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.meet_business_event),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF8F5F3A)
                    )
                }
            }
            event.businessOffer?.let { offer ->
                Box(
                    modifier = Modifier
                        .background(Color(0x1AB67A4D), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.meet_offer_badge),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF8F5F3A)
                    )
                }
                offer.formattedPriceOrNull()?.let { price ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFECD8C3), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = price,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF66422F)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFFB67A4D), Color(0xFFD9A066))),
                        RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 1.dp, vertical = 1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Transparent, RoundedCornerShape(999.dp))
                        .clickable(onClick = onSeeEvent)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.meet_see_event),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }

            event.brewingType?.let { brew ->
                Text(
                    text = stringResource(R.string.meet_brew_focus, brew),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF946239)
                )
            }

            if (event.isCreatedByUser) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE7F2E7), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.meet_created_by_you),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF3E6A3E)
                    )
                }
            }
        }
    }
}

@Composable
private fun NearbyBoostCard(
    event: CoffeeMeet,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .background(Color(0xFFFFFCF8), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = event.title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF2D2018),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Text(
            text = event.locationName,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6E594B),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Text(
            text = event.time,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF8A6B55)
        )
    }
}

private fun Context.openMapFor(event: CoffeeMeet) {
    val encodedLocation = Uri.encode(event.locationName)
    val uri = Uri.parse("geo:${event.latitude},${event.longitude}?q=${event.latitude},${event.longitude}($encodedLocation)")
    val googleMapsIntent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
    }
    val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)

    try {
        startActivity(googleMapsIntent)
    } catch (_: ActivityNotFoundException) {
        startActivity(fallbackIntent)
    }
}

private fun MeetExploreSort.labelRes(): Int = when (this) {
    MeetExploreSort.RELEVANCE -> R.string.meet_sort_relevance
    MeetExploreSort.DISTANCE -> R.string.meet_sort_nearest
    MeetExploreSort.SOONEST -> R.string.meet_sort_soonest
}
