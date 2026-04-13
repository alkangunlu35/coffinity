package com.icoffee.app.ui.screens.meet

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.icoffee.app.R
import com.icoffee.app.data.growth.GrowthAnalytics
import com.icoffee.app.data.growth.GrowthEventNames
import com.icoffee.app.data.model.CoffeeBuddyDiscoveryResult
import com.icoffee.app.data.model.CoffeeBuddySignal
import com.icoffee.app.data.model.CoffeeBuddySignalType
import com.icoffee.app.data.model.CoffeeMeet
import com.icoffee.app.data.model.EventHostType
import com.icoffee.app.data.model.MeetDiscoverySection
import com.icoffee.app.data.model.MeetDiscoverySectionType
import com.icoffee.app.data.model.MeetMood
import com.icoffee.app.data.model.formattedPriceOrNull
import com.icoffee.app.ui.components.coffinityPressMotion
import com.icoffee.app.ui.components.CoffeeEmptyStateCard
import com.icoffee.app.ui.components.PremiumRoomCard
import com.icoffee.app.ui.theme.CoffeeRadius
import com.icoffee.app.ui.theme.CoffeeSpacing
import com.icoffee.app.ui.theme.meetAmber
import com.icoffee.app.ui.theme.meetBgDeep
import com.icoffee.app.ui.theme.meetBgWarm
import com.icoffee.app.ui.theme.meetBorderWarm
import com.icoffee.app.ui.theme.meetCaramel
import com.icoffee.app.ui.theme.meetCream
import com.icoffee.app.ui.theme.meetMutedTan
import com.icoffee.app.ui.theme.meetSurface
import com.icoffee.app.ui.theme.meetSurfaceElevated
import com.icoffee.app.viewmodel.MeetViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun MeetScreen(
    isUserSignedIn: Boolean,
    onCreateMeet: () -> Unit,
    onRequestSignIn: () -> Unit,
    onExploreAllEvents: () -> Unit,
    onEventClick: (String) -> Unit,
    meetViewModel: MeetViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedMood by rememberSaveable { mutableStateOf(MeetMood.CHILL.name) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var discoveryResult by remember { mutableStateOf<CoffeeBuddyDiscoveryResult?>(null) }
    val scope = rememberCoroutineScope()
    val activeMood = MeetMood.valueOf(selectedMood)
    val sections = meetViewModel.discoverySections(activeMood)

    Box(modifier = Modifier.fillMaxSize()) {
        MeetBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                start = CoffeeSpacing.md,
                end = CoffeeSpacing.md,
                top = CoffeeSpacing.lg,
                bottom = 156.dp
            ),
            verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.lg)
        ) {
            item {
                MeetHeader()
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)
                ) {
                    items(MeetMood.entries) { mood ->
                        PremiumMoodChip(
                            label = stringResource(mood.labelRes),
                            selected = mood == activeMood,
                            onClick = { selectedMood = mood.name }
                        )
                    }
                }
            }

            item {
                ActiveRoomsRow(
                    isUserSignedIn = isUserSignedIn,
                    onCreateMeet = onCreateMeet,
                    onRequestSignIn = onRequestSignIn
                )
            }

            sections.forEach { section ->
                item(key = "section_header_${section.id}") {
                    if (section.type == MeetDiscoverySectionType.NEAR_YOU && section.events.isNotEmpty()) {
                        LaunchedEffect(section.events.map { it.id }.joinToString("|")) {
                            GrowthAnalytics.log(
                                GrowthEventNames.NEARBY_SECTION_VIEWED,
                                params = mapOf("count" to section.events.size, "source" to "meet_hub")
                            )
                        }
                    }
                    MeetDiscoverySectionHeader(section = section)
                }
                items(
                    items = section.events,
                    key = { event -> "${section.id}_${event.id}" }
                ) { event ->
                    LaunchedEffect(event.id) {
                        GrowthAnalytics.log(
                            GrowthEventNames.EVENT_CARD_VIEWED,
                            params = mapOf(
                                "eventId" to event.id,
                                "source" to "meet_hub",
                                "section" to section.type.name.lowercase(Locale.ROOT)
                            )
                        )
                    }
                    val participantRatio = stringResource(
                        R.string.meet_participant_ratio,
                        event.participants.size,
                        event.maxParticipants.takeIf { it > 0 } ?: 10
                    )
                    val offerPrice = event.businessOffer?.formattedPriceOrNull()
                    val subtitle = if (offerPrice != null) {
                        stringResource(R.string.meet_participant_price_preview, participantRatio, offerPrice)
                    } else {
                        participantRatio
                    }
                    val cardTag = when {
                        event.businessOffer != null -> stringResource(R.string.meet_offer_badge)
                        event.hostType == EventHostType.BUSINESS -> stringResource(R.string.meet_business_event)
                        else -> event.purpose
                    }
                    Box(
                        modifier = Modifier.clickable {
                            if (section.type == MeetDiscoverySectionType.NEAR_YOU) {
                                GrowthAnalytics.log(
                                    GrowthEventNames.NEARBY_EVENT_CLICKED,
                                    params = mapOf("eventId" to event.id, "source" to "meet_hub")
                                )
                            }
                            onEventClick(event.id)
                        }
                    ) {
                        PremiumRoomCard(
                            title = event.title,
                            subtitle = subtitle,
                            tag = cardTag,
                            imageRes = roomCardImageRes(event),
                            actionLabel = stringResource(R.string.meet_see_event),
                            onJoinClick = {
                                if (section.type == MeetDiscoverySectionType.NEAR_YOU) {
                                    GrowthAnalytics.log(
                                        GrowthEventNames.NEARBY_EVENT_CLICKED,
                                        params = mapOf("eventId" to event.id, "source" to "meet_hub")
                                    )
                                }
                                onEventClick(event.id)
                            }
                        )
                        EventCardShareButton(
                            modifier = Modifier.align(Alignment.TopEnd),
                            onClick = {
                                when (EventShareHelper.shareEvent(context, event)) {
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

            if (sections.isEmpty()) {
                item {
                    CoffeeEmptyStateCard(
                        title = stringResource(R.string.meet_no_rooms),
                        subtitle = stringResource(R.string.meet_explore_empty),
                        icon = Icons.Default.LocalCafe,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CoffeeRadius.lg)),
                        containerColor = meetSurface.copy(alpha = 0.7f),
                        borderColor = meetBorderWarm.copy(alpha = 0.75f),
                        titleColor = meetCream,
                        subtitleColor = meetCream.copy(alpha = 0.84f),
                        iconContainerColor = meetBgWarm.copy(alpha = 0.8f),
                        iconTint = meetAmber
                    )
                }
            }

            item {
                ExploreAllEventsCard(onExploreAllEvents = onExploreAllEvents)
            }

            if (!isUserSignedIn) {
                item {
                    PremiumSignInGateCard(onRequestSignIn = onRequestSignIn)
                }
            }

            item {
                PremiumFindSomeoneNowCard(
                    searching = searching,
                    enabled = isUserSignedIn && !searching,
                    onClick = {
                        if (!isUserSignedIn) {
                            onRequestSignIn()
                            return@PremiumFindSomeoneNowCard
                        }
                        if (searching) return@PremiumFindSomeoneNowCard
                        searching = true
                        discoveryResult = null
                        scope.launch {
                            delay(320)
                            discoveryResult = meetViewModel.discoverCoffeeBuddies(activeMood)
                            searching = false
                        }
                    }
                )
            }

            item {
                AnimatedContent(
                    targetState = discoveryResult,
                    transitionSpec = {
                        (
                            fadeIn(tween(220)) +
                                slideInVertically(
                                    initialOffsetY = { it / 8 },
                                    animationSpec = tween(220)
                                )
                            ) togetherWith (
                            fadeOut(tween(140)) +
                                slideOutVertically(
                                    targetOffsetY = { -it / 8 },
                                    animationSpec = tween(140)
                                )
                            )
                    },
                    label = "meetMatchCard"
                ) { result ->
                    if (result == null) {
                        Box(modifier = Modifier.height(4.dp))
                    } else {
                        CoffeeBuddyDiscoveryCard(
                            result = result,
                            onEventClick = onEventClick
                        )
                    }
                }
            }

            item {
                Box(modifier = Modifier.height(12.dp))
            }
        }
    }
}

private fun roomCardImageRes(meet: CoffeeMeet): Int {
    val brewing = meet.brewingType.orEmpty()
    return when {
        brewing.contains("espresso", ignoreCase = true) -> R.drawable.espresso
        brewing.contains("pour over", ignoreCase = true) -> R.drawable.v60
        brewing.contains("cold brew", ignoreCase = true) -> R.drawable.cold_brew
        meet.purpose.contains("deep", ignoreCase = true) -> R.drawable.french_press
        meet.purpose.contains("network", ignoreCase = true) -> R.drawable.flat_white
        else -> R.drawable.latte
    }
}

@Composable
private fun EventCardShareButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .padding(top = CoffeeSpacing.sm, end = CoffeeSpacing.sm)
            .clip(CircleShape)
            .background(meetBgDeep.copy(alpha = 0.62f))
            .border(1.dp, meetBorderWarm.copy(alpha = 0.7f), CircleShape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = stringResource(R.string.meet_share_action),
            tint = meetCream,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun MeetBackground(modifier: Modifier = Modifier) {
    val glowPulse by rememberInfiniteTransition(label = "meetBgPulse").animateFloat(
        initialValue = 0.18f,
        targetValue = 0.26f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "meetBgGlow"
    )

    Box(modifier = modifier) {
        Image(
            painter = painterResource(id = R.drawable.coffinity_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter
        )
        Image(
            painter = painterResource(id = R.drawable.coffinity_bg),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(10.dp),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            alpha = 0.2f
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            meetBgDeep.copy(alpha = 0.84f),
                            meetBgWarm.copy(alpha = 0.58f),
                            meetBgDeep.copy(alpha = 0.9f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x38F2C38D),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            meetAmber.copy(alpha = glowPulse),
                            Color.Transparent
                        ),
                        radius = 920f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x24000000),
                            Color.Transparent,
                            Color(0x33000000)
                        )
                    )
                )
        )
    }
}

@Composable
private fun MeetHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 146.dp)
            .padding(top = CoffeeSpacing.sm)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(148.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            meetAmber.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        radius = 520f
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = CoffeeSpacing.md),
            verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.xs)
        ) {
            Text(
                text = stringResource(R.string.meet_title),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFFFF3E5)
            )
            Text(
                text = stringResource(R.string.meet_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = meetCream.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}

@Composable
private fun PremiumMoodChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.98f
            selected -> 1.02f
            else -> 1f
        },
        animationSpec = tween(durationMillis = if (pressed) 90 else 220),
        label = "meetMoodScale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) Color(0xFFD49B66) else meetBgDeep.copy(alpha = 0.8f),
        animationSpec = tween(220),
        label = "meetMoodBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) meetCream else meetMutedTan,
        animationSpec = tween(220),
        label = "meetMoodText"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (selected) 10.dp else 5.dp,
                shape = RoundedCornerShape(CoffeeRadius.pill),
                clip = false,
                ambientColor = if (selected) meetAmber.copy(alpha = 0.36f) else Color.Black,
                spotColor = if (selected) meetAmber.copy(alpha = 0.32f) else Color.Black
            )
            .clip(RoundedCornerShape(CoffeeRadius.pill))
            .background(bgColor)
            .border(
                1.dp,
                if (selected) Color(0x66FCE2C5) else meetBorderWarm.copy(alpha = 0.9f),
                RoundedCornerShape(CoffeeRadius.pill)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(CoffeeRadius.pill))
                .background(
                    Brush.verticalGradient(
                        colors = if (selected) {
                            listOf(Color(0x26FFF4E6), Color.Transparent)
                        } else {
                            listOf(Color(0x1AF2D2B2), Color.Transparent)
                        }
                    )
                )
        )
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = textColor)
    }
}

@Composable
private fun ActiveRoomsRow(
    isUserSignedIn: Boolean,
    onCreateMeet: () -> Unit,
    onRequestSignIn: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.meet_active_rooms),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = meetCream
        )

        Box(
            modifier = Modifier
                .shadow(6.dp, RoundedCornerShape(CoffeeRadius.pill), clip = false)
                .clip(RoundedCornerShape(CoffeeRadius.pill))
                .background(
                    if (isUserSignedIn) {
                        Brush.horizontalGradient(listOf(meetAmber, meetCaramel))
                    } else {
                        Brush.horizontalGradient(listOf(meetSurfaceElevated, meetSurface))
                    }
                )
                .clickable {
                    if (isUserSignedIn) onCreateMeet() else onRequestSignIn()
                }
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                text = stringResource(R.string.meet_create),
                style = MaterialTheme.typography.labelLarge,
                color = if (isUserSignedIn) Color(0xFF2B1A11) else meetCream.copy(alpha = 0.84f)
            )
        }
    }
}

@Composable
private fun MeetDiscoverySectionHeader(section: MeetDiscoverySection) {
    val (title, subtitle) = when (section.type) {
        MeetDiscoverySectionType.SMART_PICKS -> stringResource(R.string.meet_section_smart_picks) to
            stringResource(R.string.meet_section_smart_picks_subtitle)
        MeetDiscoverySectionType.NEAR_YOU -> stringResource(R.string.meet_section_near_you) to
            stringResource(R.string.meet_section_near_you_subtitle)
        MeetDiscoverySectionType.HAPPENING_SOON -> stringResource(R.string.meet_section_happening_soon) to
            stringResource(R.string.meet_section_happening_soon_subtitle)
        MeetDiscoverySectionType.MOOD_BASED -> {
            val moodName = section.mood?.let { stringResource(it.labelRes) } ?: stringResource(R.string.meet_active_rooms)
            stringResource(R.string.meet_section_mood_title, moodName) to
                stringResource(R.string.meet_section_mood_subtitle)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = meetCream
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = meetMutedTan.copy(alpha = 0.88f)
        )
    }
}

@Composable
private fun ExploreAllEventsCard(onExploreAllEvents: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(CoffeeRadius.lg), clip = false)
            .clip(RoundedCornerShape(CoffeeRadius.lg))
            .background(
                Brush.linearGradient(
                    listOf(
                        meetSurfaceElevated.copy(alpha = 0.68f),
                        meetSurface.copy(alpha = 0.78f)
                    )
                )
            )
            .border(1.dp, meetBorderWarm.copy(alpha = 0.9f), RoundedCornerShape(CoffeeRadius.lg))
            .clickable(onClick = onExploreAllEvents)
            .padding(horizontal = CoffeeSpacing.md, vertical = CoffeeSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = stringResource(R.string.meet_explore_all_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = meetCream
            )
            Text(
                text = stringResource(R.string.meet_explore_all_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = meetMutedTan.copy(alpha = 0.86f)
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(CoffeeRadius.pill))
                .background(Brush.horizontalGradient(listOf(meetAmber, meetCaramel)))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = stringResource(R.string.meet_explore_all_action),
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF2A1B12)
            )
        }
    }
}

@Composable
private fun PremiumSignInGateCard(onRequestSignIn: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 9.dp,
                shape = RoundedCornerShape(CoffeeRadius.lg),
                clip = false,
                ambientColor = Color(0x3820120C),
                spotColor = Color(0x4220120C)
            )
            .clip(RoundedCornerShape(CoffeeRadius.lg))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        meetSurfaceElevated.copy(alpha = 0.64f),
                        meetSurface.copy(alpha = 0.74f)
                    )
                )
            )
            .border(1.dp, meetBorderWarm, RoundedCornerShape(CoffeeRadius.lg))
            .padding(horizontal = CoffeeSpacing.md, vertical = CoffeeSpacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CoffeeSpacing.xs)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0x66F2CEAA)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalCafe,
                        contentDescription = null,
                        tint = meetAmber,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = stringResource(R.string.meet_sign_in_line1),
                        style = MaterialTheme.typography.bodySmall,
                        color = meetCream.copy(alpha = 0.9f)
                    )
                    Text(
                        text = stringResource(R.string.meet_sign_in_line2),
                        style = MaterialTheme.typography.bodySmall,
                        color = meetCream.copy(alpha = 0.9f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(CoffeeRadius.pill))
                    .background(Brush.horizontalGradient(listOf(meetAmber, meetCaramel)))
                    .clickable(onClick = onRequestSignIn)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = stringResource(R.string.meet_sign_in_action),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF22150E)
                )
            }
        }
    }
}

@Composable
private fun PremiumFindSomeoneNowCard(
    searching: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shadow by animateDpAsState(
        targetValue = if (pressed) 8.dp else 15.dp,
        animationSpec = tween(durationMillis = if (pressed) 90 else 220),
        label = "meetCtaShadow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .coffinityPressMotion(
                interactionSource = interactionSource,
                pressedScale = if (enabled) 0.985f else 1f,
                pressedAlpha = if (enabled) 0.96f else 1f
            )
            .shadow(
                elevation = shadow,
                shape = RoundedCornerShape(CoffeeRadius.pill),
                clip = false,
                ambientColor = meetAmber.copy(alpha = 0.4f),
                spotColor = meetAmber.copy(alpha = 0.34f)
            )
            .clip(RoundedCornerShape(CoffeeRadius.pill))
            .background(
                if (enabled) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFE7BD8C),
                            meetCaramel,
                            Color(0xFFB66F34)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            meetSurfaceElevated,
                            meetSurface
                        )
                    )
                }
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            .border(
                width = 1.dp,
                color = if (enabled) Color(0x73FFE7CA) else meetBorderWarm.copy(alpha = 0.65f),
                shape = RoundedCornerShape(CoffeeRadius.pill)
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0x66FFF3E4))
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (searching) {
                            stringResource(R.string.meet_searching)
                        } else {
                            stringResource(R.string.meet_find_now)
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (enabled) Color(0xFF2B1A11) else meetCream.copy(alpha = 0.85f)
                    )
                    if (!searching) {
                        Text(
                            text = stringResource(R.string.meet_find_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (enabled) Color(0xCC2B1A11) else meetCream.copy(alpha = 0.75f)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (enabled) Brush.linearGradient(listOf(Color(0x52FFF3E1), Color(0x28FFE8CB))) else Brush.linearGradient(listOf(Color(0x26FFFFFF), Color(0x1AFFFFFF)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = if (enabled) Color(0xFF2E1B11) else meetCream.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun CoffeeBuddyDiscoveryCard(
    result: CoffeeBuddyDiscoveryResult,
    onEventClick: (String) -> Unit
) {
    val hasDiscoverableProfiles = result.discoverableUserCount > 0 && result.candidates.isNotEmpty()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(CoffeeRadius.lg),
                clip = false,
                ambientColor = Color(0x3520120C),
                spotColor = Color(0x4020120C)
            )
            .clip(RoundedCornerShape(CoffeeRadius.lg))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        meetSurfaceElevated.copy(alpha = 0.6f),
                        meetSurface.copy(alpha = 0.7f)
                    )
                )
            )
            .border(1.dp, meetBorderWarm, RoundedCornerShape(CoffeeRadius.lg))
            .padding(CoffeeSpacing.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(CoffeeSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(meetAmber, meetCaramel))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalCafe,
                        contentDescription = null,
                        tint = Color(0xFF2A1B12)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (hasDiscoverableProfiles) {
                            stringResource(
                                R.string.meet_discovery_results_title,
                                result.discoverableUserCount
                            )
                        } else {
                            stringResource(R.string.meet_discovery_empty_title)
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = meetCream
                    )
                    Text(
                        text = if (hasDiscoverableProfiles) {
                            stringResource(R.string.meet_discovery_results_subtitle)
                        } else {
                            stringResource(R.string.meet_discovery_empty_subtitle)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = meetCream.copy(alpha = 0.82f)
                    )
                }
            }

            if (hasDiscoverableProfiles) {
                Column(verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.xs)) {
                    result.candidates.take(4).forEach { candidate ->
                        val displayName = candidate.displayName?.takeIf { it.isNotBlank() }
                            ?: stringResource(
                                R.string.meet_discovery_member_fallback,
                                candidate.userId.takeLast(4).uppercase(Locale.getDefault())
                            )
                        val cityOrArea = candidate.cityOrArea
                            ?: stringResource(R.string.meet_discovery_city_fallback)
                        val sharedSignals = candidate.sharedSignals.take(2)
                        val rowModifier = if (candidate.eventId != null) {
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(CoffeeRadius.md))
                                .background(meetSurface.copy(alpha = 0.6f))
                                .border(
                                    1.dp,
                                    meetBorderWarm.copy(alpha = 0.8f),
                                    RoundedCornerShape(CoffeeRadius.md)
                                )
                                .clickable { onEventClick(candidate.eventId) }
                                .padding(horizontal = CoffeeSpacing.sm, vertical = CoffeeSpacing.sm)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(CoffeeRadius.md))
                                .background(meetSurface.copy(alpha = 0.6f))
                                .border(
                                    1.dp,
                                    meetBorderWarm.copy(alpha = 0.8f),
                                    RoundedCornerShape(CoffeeRadius.md)
                                )
                                .padding(horizontal = CoffeeSpacing.sm, vertical = CoffeeSpacing.sm)
                        }
                        Row(
                            modifier = rowModifier,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(meetAmber.copy(alpha = 0.24f))
                                        .border(
                                            1.dp,
                                            meetAmber.copy(alpha = 0.6f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initialsFromName(displayName),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = meetCream
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = meetCream,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = cityOrArea,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = meetMutedTan.copy(alpha = 0.92f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (sharedSignals.isNotEmpty()) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            sharedSignals.forEach { signal ->
                                                Text(
                                                    text = signalLabel(signal),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = meetCream.copy(alpha = 0.86f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.meet_discovery_match_score,
                                        candidate.score
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = meetCream.copy(alpha = 0.9f)
                                )
                                if (candidate.eventId != null) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = meetCream.copy(alpha = 0.74f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = if (result.profileReady) {
                        stringResource(R.string.meet_discovery_empty_hint)
                    } else {
                        stringResource(R.string.meet_discovery_profile_hint)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = meetMutedTan.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun signalLabel(signal: CoffeeBuddySignal): String = when (signal.type) {
    CoffeeBuddySignalType.SAME_CITY -> stringResource(
        R.string.meet_discovery_signal_same_city,
        signal.detail ?: stringResource(R.string.meet_discovery_city_fallback)
    )
    CoffeeBuddySignalType.SAME_MOOD -> stringResource(R.string.meet_discovery_signal_same_mood)
    CoffeeBuddySignalType.TASTE_SIMILARITY -> stringResource(R.string.meet_discovery_signal_taste)
    CoffeeBuddySignalType.SHARED_BREW -> stringResource(
        R.string.meet_discovery_signal_shared_brew,
        signal.detail ?: stringResource(R.string.meet_discovery_signal_shared_brew_fallback)
    )
    CoffeeBuddySignalType.SHARED_ORIGIN -> stringResource(
        R.string.meet_discovery_signal_shared_origin,
        signal.detail ?: stringResource(R.string.meet_discovery_signal_shared_origin_fallback)
    )
    CoffeeBuddySignalType.SHARED_ACTIVITY -> stringResource(R.string.meet_discovery_signal_activity)
}

private fun initialsFromName(name: String): String {
    val tokens = name
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    if (tokens.isEmpty()) return "C"
    if (tokens.size == 1) {
        return tokens.first().take(1).uppercase(Locale.getDefault())
    }
    return (tokens.first().take(1) + tokens.last().take(1)).uppercase(Locale.getDefault())
}
