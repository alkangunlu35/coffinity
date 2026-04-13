package com.icoffee.app.ui.screens.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.icoffee.app.R
import com.icoffee.app.data.MeetRepository
import com.icoffee.app.data.menu.MenuScanRepository
import com.icoffee.app.data.model.CoffeeMeet
import com.icoffee.app.data.model.DetectedMenuItem
import com.icoffee.app.data.model.NormalizedCoffeeType
import com.icoffee.app.data.model.TasteInsightState
import com.icoffee.app.data.model.Venue
import com.icoffee.app.data.model.VenueMatchResult
import com.icoffee.app.data.venue.VenueRepository

@Composable
fun MenuScanResultScreen(
    scanId: String,
    onBack: () -> Unit
) {
    val result = remember(scanId) { MenuScanRepository.getByScanId(scanId) }
    var venueMatchResult by remember { mutableStateOf<VenueMatchResult?>(null) }
    var linkedEvents by remember { mutableStateOf<List<CoffeeMeet>>(emptyList()) }

    LaunchedEffect(scanId) {
        if (result != null && result.detectedItems.isNotEmpty()) {
            val matchResult = VenueRepository.matchOrRegister(
                detectedItems = result.detectedItems,
                venueHint = result.venueHint
            )
            venueMatchResult = matchResult

            matchResult.venue?.let { venue ->
                linkedEvents = MeetRepository.getEventsAtVenue(venue.displayName)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5EFE7))
    ) {
        if (result == null || result.detectedItems.isEmpty()) {
            MenuScanEmptyState(onBack = onBack)
            return@Box
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFFEDE3D9), Color(0xFFDDD0C4))
                                    ),
                                    shape = CircleShape
                                )
                                .border(1.dp, Color(0x22000000), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.scan_back),
                                tint = Color(0xFF4A3728)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.menu_scan_result_title),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF2D2018)
                            )
                            if (!result.venueHint.isNullOrBlank()) {
                                Text(
                                    text = result.venueHint,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF746051)
                                )
                            }
                        }
                    }
                }
            }

            // Venue recognition card
            venueMatchResult?.venue?.let { venue ->
                item {
                    VenueRecognitionCard(
                        venue = venue,
                        isNew = venueMatchResult?.isNew ?: false,
                        confidence = venueMatchResult?.confidence ?: 0,
                        linkedEvents = linkedEvents
                    )
                }
            }

            if (result.bestMatch != null) {
                item {
                    Text(
                        text = stringResource(R.string.menu_scan_result_best_match),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF805238),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    MenuItemCard(item = result.bestMatch, isBestMatch = true)
                }
            }

            if (result.alternatives.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.menu_scan_result_alternatives),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF746051),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                items(result.alternatives, key = { it.normalizedType.name }) { item ->
                    MenuItemCard(item = item, isBestMatch = false)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun VenueRecognitionCard(
    venue: Venue,
    isNew: Boolean,
    confidence: Int,
    linkedEvents: List<CoffeeMeet>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFFCF8), Color(0xFFF5EAE0))
                    )
                )
                .border(1.dp, Color(0x33B67A4D), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFB67A4D),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isNew) stringResource(R.string.venue_new)
                               else stringResource(R.string.venue_recognized),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF805238)
                    )
                    if (!isNew && confidence < 100) {
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEDD9C5), RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.venue_confidence, confidence),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF6B4428)
                            )
                        }
                    }
                }
                Text(
                    text = venue.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF2D2018),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.venue_coverage, venue.coffeeCoverage),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9B7A62)
                )
            }
        }

        if (linkedEvents.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.venue_linked_events),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF746051),
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)
            )
            linkedEvents.forEach { event ->
                Spacer(modifier = Modifier.height(6.dp))
                LinkedEventRow(event = event)
            }
        }
    }
}

@Composable
private fun MenuItemCard(
    item: DetectedMenuItem,
    isBestMatch: Boolean
) {
    val borderColor = if (isBestMatch) Color(0xFFB67A4D) else Color(0x22000000)
    val bgColors = if (isBestMatch) {
        listOf(Color(0xFFFFFCF8), Color(0xFFF9EFE4))
    } else {
        listOf(Color(0xFFFFFCF8), Color(0xFFF5EDE3))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(bgColors))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = item.normalizedType.asLocalizedLabel(),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF2D2018)
            )
            Text(
                text = item.rawLine,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF746051),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.matchResult.insight.state.asLocalizedInsightLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9B7A62),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(start = 12.dp)
        ) {
            MatchInsightBadge(state = item.matchResult.insight.state)
        }
    }
}

@Composable
private fun MatchInsightBadge(state: TasteInsightState) {
    val (bg, text) = when (state) {
        TasteInsightState.LIKELY_ALIGNED -> Color(0xFF2E7D32) to Color(0xFFFFFFFF)
        TasteInsightState.PARTIAL_MATCH -> Color(0xFFB67A4D) to Color(0xFFFFFFFF)
        TasteInsightState.POTENTIAL_MISMATCH -> Color(0xFF8D6E63) to Color(0xFFFFFFFF)
        TasteInsightState.NOT_ENOUGH_DATA -> Color(0xFF9E9E9E) to Color(0xFFFFFFFF)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = state.asLocalizedInsightLabel(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = text
        )
    }
}

@Composable
private fun LinkedEventRow(event: CoffeeMeet) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF8F0))
            .border(1.dp, Color(0x22B67A4D), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF2D2018),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = event.time,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9B7A62)
            )
        }
        Text(
            text = stringResource(
                R.string.meet_participant_ratio,
                event.participants.size,
                event.maxParticipants
            ),
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFB67A4D)
        )
    }
}

@Composable
private fun TasteInsightState.asLocalizedInsightLabel(): String = when (this) {
    TasteInsightState.NOT_ENOUGH_DATA -> stringResource(R.string.scan_taste_state_not_enough_title)
    TasteInsightState.PARTIAL_MATCH -> stringResource(R.string.scan_taste_state_partial_title)
    TasteInsightState.LIKELY_ALIGNED -> stringResource(R.string.scan_taste_state_aligned_title)
    TasteInsightState.POTENTIAL_MISMATCH -> stringResource(R.string.scan_taste_state_mismatch_title)
}

@Composable
private fun NormalizedCoffeeType.asLocalizedLabel(): String = when (this) {
    NormalizedCoffeeType.ESPRESSO -> stringResource(R.string.menu_type_espresso)
    NormalizedCoffeeType.AMERICANO -> stringResource(R.string.menu_type_americano)
    NormalizedCoffeeType.CAPPUCCINO -> stringResource(R.string.menu_type_cappuccino)
    NormalizedCoffeeType.LATTE -> stringResource(R.string.menu_type_latte)
    NormalizedCoffeeType.FLAT_WHITE -> stringResource(R.string.menu_type_flat_white)
    NormalizedCoffeeType.MACCHIATO -> stringResource(R.string.menu_type_macchiato)
    NormalizedCoffeeType.MOCHA -> stringResource(R.string.menu_type_mocha)
    NormalizedCoffeeType.CORTADO -> stringResource(R.string.menu_type_cortado)
    NormalizedCoffeeType.RISTRETTO -> stringResource(R.string.menu_type_ristretto)
    NormalizedCoffeeType.LUNGO -> stringResource(R.string.menu_type_lungo)
    NormalizedCoffeeType.FILTER_V60 -> stringResource(R.string.menu_type_v60)
    NormalizedCoffeeType.POUR_OVER -> stringResource(R.string.menu_type_pour_over)
    NormalizedCoffeeType.CHEMEX -> stringResource(R.string.menu_type_chemex)
    NormalizedCoffeeType.AEROPRESS -> stringResource(R.string.menu_type_aeropress)
    NormalizedCoffeeType.COLD_BREW -> stringResource(R.string.menu_type_cold_brew)
    NormalizedCoffeeType.ICED_COFFEE -> stringResource(R.string.menu_type_iced_coffee)
    NormalizedCoffeeType.TURKISH_COFFEE -> stringResource(R.string.menu_type_turkish_coffee)
    NormalizedCoffeeType.FILTER_COFFEE -> stringResource(R.string.menu_type_filter_coffee)
    NormalizedCoffeeType.UNKNOWN -> stringResource(R.string.scan_value_unknown)
}

@Composable
private fun MenuScanEmptyState(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(42.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFEDE3D9), Color(0xFFDDD0C4))
                    ),
                    shape = CircleShape
                )
                .border(1.dp, Color(0x22000000), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.scan_back),
                tint = Color(0xFF4A3728)
            )
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.menu_scan_no_items_detected),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF746051)
            )
        }
    }
}
