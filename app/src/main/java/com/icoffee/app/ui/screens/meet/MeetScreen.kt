package com.icoffee.app.ui.screens.meet

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import coil.compose.AsyncImage
import com.icoffee.app.R
import com.icoffee.app.analytics.AnalyticsEvents
import com.icoffee.app.analytics.AnalyticsParams
import com.icoffee.app.analytics.AnalyticsProvider
import com.icoffee.app.data.MeetRawDebugEvent
import com.icoffee.app.data.growth.GrowthAnalytics
import com.icoffee.app.data.growth.GrowthEventNames
import com.icoffee.app.data.model.CoffeeBuddyCandidate
import com.icoffee.app.data.model.CoffeeBuddyDiscoveryResult
import com.icoffee.app.data.model.CoffeeBuddyChatMessageItem
import com.icoffee.app.data.model.CoffeeBuddyInviteItem
import com.icoffee.app.data.model.CoffeeBuddySignal
import com.icoffee.app.data.model.CoffeeBuddySignalType
import com.icoffee.app.data.model.CoffeeMeet
import com.icoffee.app.data.model.EventHostType
import com.icoffee.app.data.model.MeetDiscoverySection
import com.icoffee.app.data.model.MeetDiscoverySectionType
import com.icoffee.app.data.model.MeetMood
import com.icoffee.app.data.model.formattedPriceOrNull
import com.icoffee.app.notifications.NotificationTapRouter
import com.icoffee.app.ui.components.coffinityPressMotion
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
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MeetScreen(
    isUserSignedIn: Boolean,
    onCreateMeet: () -> Unit,
    onRequestSignUp: () -> Unit,
    onRequestSignIn: () -> Unit,
    onExploreAllEvents: () -> Unit,
    onEventClick: (String) -> Unit,
    meetViewModel: MeetViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedMood by rememberSaveable { mutableStateOf(MeetMood.CHILL.name) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var discoveryResult by remember { mutableStateOf<CoffeeBuddyDiscoveryResult?>(null) }
    var inviteTarget by remember { mutableStateOf<CoffeeBuddyCandidate?>(null) }
    var invitePlaceName by rememberSaveable { mutableStateOf("") }
    var inviteDate by rememberSaveable { mutableStateOf(startOfDayMillis(System.currentTimeMillis())) }
    var inviteStartTime by rememberSaveable { mutableStateOf(nextRoundedTimeMillis(System.currentTimeMillis())) }
    var inviteEndTime by rememberSaveable { mutableStateOf(nextRoundedTimeMillis(System.currentTimeMillis()) + 2 * 60 * 60 * 1000L) }
    var inviteMessage by rememberSaveable { mutableStateOf("") }
    var inviteTimeOption by rememberSaveable { mutableStateOf(InviteQuickTimeOption.NOW.name) }
    var inviteCoffeePreference by rememberSaveable { mutableStateOf(INVITE_COFFEE_PREF_DEFAULT) }
    var inviteSending by remember { mutableStateOf(false) }
    val incomingInvitesFlow = remember(isUserSignedIn) {
        meetViewModel.observeIncomingCoffeeBuddyInvites()
    }
    val incomingInvites by incomingInvitesFlow.collectAsState(initial = emptyList())
    val outgoingInvitesFlow = remember(isUserSignedIn) {
        meetViewModel.observeOutgoingCoffeeBuddyInvites()
    }
    val outgoingInvites by outgoingInvitesFlow.collectAsState(initial = emptyList())
    val acceptedInvitesFlow = remember(isUserSignedIn) {
        meetViewModel.observeAcceptedCoffeeBuddyInvites()
    }
    val acceptedInvites by acceptedInvitesFlow.collectAsState(initial = emptyList())
    var activeChatInvite by remember { mutableStateOf<CoffeeBuddyInviteItem?>(null) }
    var activeChatId by remember { mutableStateOf<String?>(null) }
    var chatDraft by rememberSaveable { mutableStateOf("") }
    var chatDraftSource by rememberSaveable { mutableStateOf("typed") }
    val chatMessagesFlow = remember(activeChatId) {
        activeChatId?.let { meetViewModel.observeCoffeeBuddyChatMessages(it) } ?: flowOf(emptyList())
    }
    val chatMessages by chatMessagesFlow.collectAsState(initial = emptyList())
    val pendingChatDestination by NotificationTapRouter.pendingChatDestination.collectAsState()
    val scope = rememberCoroutineScope()
    val activeMood = MeetMood.valueOf(selectedMood)
    val sections = meetViewModel.discoverySections(activeMood)
    var socialScreenViewTracked by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(socialScreenViewTracked, pendingChatDestination?.deliveryKey) {
        if (socialScreenViewTracked) return@LaunchedEffect
        val entrySource = if (pendingChatDestination != null) "push" else "tab"
        AnalyticsProvider.tracker.logEvent(
            AnalyticsEvents.SOCIAL_SCREEN_VIEW,
            mapOf(AnalyticsParams.ENTRY_SOURCE to entrySource)
        )
        socialScreenViewTracked = true
    }

    LaunchedEffect(pendingChatDestination?.deliveryKey) {
        val destination = pendingChatDestination ?: return@LaunchedEffect
        if (activeChatId != destination.chatId) {
            activeChatInvite = CoffeeBuddyInviteItem(
                id = destination.inviteId?.ifBlank { destination.chatId } ?: destination.chatId,
                senderUserId = destination.senderId.orEmpty(),
                senderDisplayName = destination.senderId?.takeIf { it.isNotBlank() }?.let {
                    "Kullanıcı ${it.takeLast(6)}"
                } ?: "Sohbet",
                senderAvatarUrl = null,
                placeName = "",
                inviteDate = 0L,
                startTime = 0L,
                endTime = 0L,
                message = "",
                status = "accepted",
                createdAt = System.currentTimeMillis()
            )
            activeChatId = destination.chatId
            Log.d(
                "NotificationRouter",
                "NOTIF_ROUTE_NAVIGATED key=${destination.deliveryKey} destination=chat_dialog chatId=${destination.chatId}"
            )
        }
        NotificationTapRouter.consumePendingChatDestination(destination.deliveryKey)
    }

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
                bottom = 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MeetHeader()
            }

            item {
                LazyRow(
                    modifier = Modifier.padding(top = 8.dp),
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
                Box(modifier = Modifier.padding(top = 8.dp)) {
                    ActiveRoomsRow(
                        isUserSignedIn = isUserSignedIn,
                        onCreateMeet = onCreateMeet,
                        onRequestSignUp = onRequestSignUp,
                        onRequestSignIn = onRequestSignIn
                    )
                }
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
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box {
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
                }
            }

            if (sections.isEmpty()) {
                item {
                    MeetEmptyActionCard(
                        onCreateMeet = onCreateMeet,
                        onExploreAllEvents = onExploreAllEvents
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
                            onInviteClick = { candidate ->
                                if (!isUserSignedIn) {
                                    onRequestSignIn()
                                    return@CoffeeBuddyDiscoveryCard
                                }
                                val baseTime = nextRoundedTimeMillis(System.currentTimeMillis())
                                val defaultWindow = resolveInviteWindowForOption(
                                    option = InviteQuickTimeOption.NOW,
                                    anchorMillis = baseTime
                                )
                                inviteTarget = candidate
                                invitePlaceName = ""
                                inviteDate = defaultWindow.inviteDate
                                inviteStartTime = defaultWindow.startTime
                                inviteEndTime = defaultWindow.endTime
                                inviteMessage = ""
                                inviteTimeOption = InviteQuickTimeOption.NOW.name
                                inviteCoffeePreference = INVITE_COFFEE_PREF_DEFAULT
                                inviteSending = false
                            }
                        )
                    }
                }
            }

            if (isUserSignedIn && incomingInvites.isNotEmpty()) {
                item {
                    CoffeeInviteInboxCard(
                        invites = incomingInvites,
                        onAccept = { invite ->
                            scope.launch {
                                meetViewModel.respondCoffeeBuddyInvite(invite.id, accept = true)
                                    .onSuccess {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.meet_coffee_invite_accepted),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .onFailure {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.auth_error_generic),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        },
                        onDecline = { invite ->
                            scope.launch {
                                meetViewModel.respondCoffeeBuddyInvite(invite.id, accept = false)
                                    .onSuccess {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.meet_coffee_invite_declined),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .onFailure {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.auth_error_generic),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        }
                    )
                }
            }

            if (isUserSignedIn && outgoingInvites.isNotEmpty()) {
                item {
                    CoffeeInviteOutgoingCard(
                        invites = outgoingInvites
                    )
                }
            }

            if (isUserSignedIn && acceptedInvites.isNotEmpty()) {
                item {
                    AcceptedInviteChatEntryCard(
                        invites = acceptedInvites,
                        onOpenChat = { invite ->
                            Log.d(
                                "COFFEE_CHAT_DEBUG",
                                "mesajlas tap inviteId=${invite.id} senderUserId=${invite.senderUserId} resolvedCurrentUserId=${meetViewModel.currentUserId}"
                            )
                            scope.launch {
                                meetViewModel.openCoffeeBuddyChat(
                                    inviteId = invite.id,
                                    source = "social",
                                    eventId = null
                                )
                                    .onSuccess { chatId ->
                                        Log.d(
                                            "COFFEE_CHAT_DEBUG",
                                            "mesajlas open success inviteId=${invite.id} resolvedChatId=$chatId"
                                        )
                                        activeChatInvite = invite
                                        activeChatId = chatId
                                    }
                                    .onFailure { error ->
                                        Log.e(
                                            "COFFEE_CHAT_DEBUG",
                                            "mesajlas open failure inviteId=${invite.id} message=${error.message}",
                                            error
                                        )
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.auth_error_generic),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        inviteTarget?.let { target ->
            CoffeeInviteComposerDialog(
                targetDisplayName = target.displayName
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: target.userId.takeLast(6),
                targetCompatibility = target.sharedSignals
                    .firstOrNull()
                    ?.detail
                    ?.trim()
                    ?.takeIf { it.isNotBlank() },
                targetDistance = target.cityOrArea
                    ?.trim()
                    ?.takeIf { it.isNotBlank() },
                selectedTimeOption = inviteTimeOption
                    .let { raw ->
                        InviteQuickTimeOption.entries.firstOrNull { it.name == raw } ?: InviteQuickTimeOption.NOW
                    },
                placeName = invitePlaceName,
                inviteDate = inviteDate,
                startTime = inviteStartTime,
                endTime = inviteEndTime,
                message = inviteMessage,
                coffeePreference = inviteCoffeePreference,
                isSending = inviteSending,
                onTimeOptionChange = { option ->
                    inviteTimeOption = option.name
                    val window = resolveInviteWindowForOption(
                        option = option,
                        anchorMillis = System.currentTimeMillis(),
                        customInviteDate = inviteDate,
                        customStartTime = inviteStartTime,
                        customEndTime = inviteEndTime
                    )
                    inviteDate = window.inviteDate
                    inviteStartTime = window.startTime
                    inviteEndTime = window.endTime
                },
                onPlaceNameChange = { invitePlaceName = it },
                onDateClick = {
                    showDatePicker(
                        context = context,
                        initialDateMillis = inviteDate,
                        onDateSelected = { selectedDate ->
                            inviteDate = selectedDate
                            inviteStartTime = mergeDateAndTime(selectedDate, inviteStartTime)
                            inviteEndTime = mergeDateAndTime(selectedDate, inviteEndTime)
                        }
                    )
                },
                onStartTimeClick = {
                    showTimePicker(
                        context = context,
                        initialTimeMillis = inviteStartTime,
                        onTimeSelected = { selectedTime ->
                            inviteStartTime = mergeDateAndTime(inviteDate, selectedTime)
                        }
                    )
                },
                onEndTimeClick = {
                    showTimePicker(
                        context = context,
                        initialTimeMillis = inviteEndTime,
                        onTimeSelected = { selectedTime ->
                            inviteEndTime = mergeDateAndTime(inviteDate, selectedTime)
                        }
                    )
                },
                onMessageChange = { inviteMessage = it },
                onCoffeePreferenceChange = { inviteCoffeePreference = it },
                onDismiss = {
                    if (!inviteSending) inviteTarget = null
                },
                onSend = {
                    if (inviteSending) return@CoffeeInviteComposerDialog
                    val normalizedPlace = invitePlaceName.trim()
                    if (normalizedPlace.isBlank()) {
                        Toast.makeText(context, "Konum giriniz", Toast.LENGTH_SHORT).show()
                        return@CoffeeInviteComposerDialog
                    }
                    if (inviteEndTime <= inviteStartTime) {
                        Toast.makeText(context, "Bitiş saati başlangıçtan sonra olmalı", Toast.LENGTH_SHORT).show()
                        return@CoffeeInviteComposerDialog
                    }
                    inviteSending = true
                    scope.launch {
                        val sendResult = meetViewModel.sendCoffeeBuddyInvite(
                            recipientUserId = target.userId,
                            placeName = normalizedPlace,
                            inviteDate = inviteDate,
                            startTime = inviteStartTime,
                            endTime = inviteEndTime,
                            message = inviteMessage,
                            timeOption = inviteTimeOption,
                            coffeePreference = inviteCoffeePreference,
                            source = "social"
                        )
                        sendResult
                            .onSuccess { sent ->
                                if (sent) {
                                    Toast.makeText(context, "Davet gönderildi", Toast.LENGTH_SHORT).show()
                                    inviteSending = false
                                    inviteTarget = null
                                } else {
                                    inviteSending = false
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.meet_coffee_invite_already_pending),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .onFailure { error ->
                                inviteSending = false
                                val normalizedMessage = error.message
                                    ?.trim()
                                    ?.lowercase(Locale.ROOT)
                                    .orEmpty()
                                when {
                                    normalizedMessage == "not_signed_in" -> {
                                        inviteTarget = null
                                        onRequestSignIn()
                                    }
                                    normalizedMessage == "invalid_recipient" ||
                                        normalizedMessage.contains("same") -> Unit
                                    normalizedMessage == "invalid_place" -> {
                                        Toast.makeText(context, "Konum giriniz", Toast.LENGTH_SHORT).show()
                                    }
                                    normalizedMessage == "invalid_time_window" -> {
                                        Toast.makeText(
                                            context,
                                            "Geçerli bir saat aralığı seçiniz",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    normalizedMessage.contains("pending") ||
                                        normalizedMessage.contains("already") -> {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.meet_coffee_invite_already_pending),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        inviteTarget = null
                                    }
                                    else -> {
                                        Toast.makeText(
                                            context,
                                            "Davet gönderilemedi",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                    }
                }
            )
        }

        if (activeChatInvite != null && !activeChatId.isNullOrBlank()) {
            CoffeeBuddyChatDialog(
                counterpartName = activeChatInvite!!.senderDisplayName,
                currentUserId = meetViewModel.currentUserId,
                messages = chatMessages,
                draft = chatDraft,
                showQuickActions = activeChatInvite!!
                    .status
                    .trim()
                    .equals("accepted", ignoreCase = true),
                onDraftChange = {
                    chatDraft = it
                    chatDraftSource = "typed"
                },
                onQuickActionSelected = { quickAction ->
                    chatDraft = appendQuickChatAction(chatDraft, quickAction)
                    chatDraftSource = "quick_action"
                },
                onSend = {
                    val text = chatDraft.trim()
                    if (text.isBlank()) return@CoffeeBuddyChatDialog
                    scope.launch {
                        meetViewModel.sendCoffeeBuddyChatMessage(
                            chatId = activeChatId.orEmpty(),
                            text = text,
                            inviteId = activeChatInvite?.id,
                            eventId = null,
                            messageSource = chatDraftSource
                        ).onSuccess {
                            chatDraft = ""
                            chatDraftSource = "typed"
                        }.onFailure {
                            Toast.makeText(
                                context,
                                context.getString(R.string.auth_error_generic),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                onDismiss = {
                    activeChatInvite = null
                    activeChatId = null
                    chatDraft = ""
                    chatDraftSource = "typed"
                }
            )
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
private fun MeetEmptyActionCard(
    onCreateMeet: () -> Unit,
    onExploreAllEvents: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CoffeeRadius.lg))
            .background(meetSurface.copy(alpha = 0.7f))
            .border(1.dp, meetBorderWarm.copy(alpha = 0.75f), RoundedCornerShape(CoffeeRadius.lg))
            .padding(CoffeeSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(meetBgWarm.copy(alpha = 0.8f))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalCafe,
                    contentDescription = null,
                    tint = meetAmber
                )
            }
            Text(
                text = "Bu mod için henüz aktif oda yok",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = meetCream
            )
        }

        Text(
            text = "Yakın çevrende bu filtreye uygun etkinlik bulunamadı.",
            style = MaterialTheme.typography.bodySmall,
            color = meetCream.copy(alpha = 0.84f)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CoffeeRadius.pill))
                .background(Brush.horizontalGradient(listOf(meetAmber, meetCaramel)))
                .clickable(onClick = onCreateMeet)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Etkinlik Oluştur",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF2A1B12)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CoffeeRadius.pill))
                .background(meetSurfaceElevated.copy(alpha = 0.62f))
                .border(1.dp, meetBorderWarm.copy(alpha = 0.85f), RoundedCornerShape(CoffeeRadius.pill))
                .clickable(onClick = onExploreAllEvents)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Tüm Etkinlikleri Keşfet",
                style = MaterialTheme.typography.labelLarge,
                color = meetCream
            )
        }
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
    onRequestSignUp: () -> Unit,
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
                    if (isUserSignedIn) onCreateMeet() else onRequestSignUp()
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
            .shadow(8.dp, RoundedCornerShape(CoffeeRadius.lg), clip = false)
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
            .padding(horizontal = CoffeeSpacing.md, vertical = 18.dp),
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
                elevation = 7.dp,
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
            .padding(horizontal = CoffeeSpacing.md, vertical = 10.dp)
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
                        .size(28.dp)
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
                    .padding(horizontal = 10.dp, vertical = 6.dp)
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
        targetValue = if (pressed) 7.dp else 11.dp,
        animationSpec = tween(durationMillis = if (pressed) 90 else 220),
        label = "meetCtaShadow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
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
                        .height(30.dp)
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
                    .size(38.dp)
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
    onInviteClick: (com.icoffee.app.data.model.CoffeeBuddyCandidate) -> Unit
) {
    LaunchedEffect(result.candidates.map { it.userId }.joinToString("|")) {
        Log.d(
            "COFFEE_BUDDY_DEBUG",
            "final rendered userIds=${result.candidates.map { it.userId }.joinToString(",")}"
        )
    }
    val discoveredCount = result.candidates.size
    val hasDiscoverableProfiles = discoveredCount > 0
    val visibleCandidates = if (discoveredCount >= 4) {
        result.candidates.take(3)
    } else {
        result.candidates
    }
    val hiddenCount = (discoveredCount - visibleCandidates.size).coerceAtLeast(0)
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
                                discoveredCount
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
                    visibleCandidates.forEach { candidate ->
                        val displayName = candidate.displayName
                            ?.trim()
                            ?.takeIf { it.length >= 2 }
                            ?: stringResource(
                                R.string.meet_discovery_member_fallback,
                                candidate.userId.takeLast(4).uppercase(Locale.getDefault())
                            )
                        val cityOrArea = candidate.cityOrArea
                            ?: stringResource(R.string.meet_discovery_city_fallback)
                        val sharedSignals = candidate.sharedSignals.take(2)
                        val rowModifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CoffeeRadius.md))
                            .background(meetSurface.copy(alpha = 0.6f))
                            .border(
                                1.dp,
                                meetBorderWarm.copy(alpha = 0.8f),
                                RoundedCornerShape(CoffeeRadius.md)
                            )
                            .clickable { onInviteClick(candidate) }
                            .padding(horizontal = CoffeeSpacing.sm, vertical = CoffeeSpacing.sm)
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
                                if (!candidate.avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = candidate.avatarUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .border(
                                                1.dp,
                                                meetAmber.copy(alpha = 0.6f),
                                                CircleShape
                                            ),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
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
                                    text = stringResource(R.string.meet_coffee_invite_action),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = meetAmber,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(CoffeeRadius.pill))
                                        .clickable { onInviteClick(candidate) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
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
                    if (hiddenCount > 0) {
                        Text(
                            text = "+$hiddenCount kahve sever daha",
                            style = MaterialTheme.typography.labelSmall,
                            color = meetCream.copy(alpha = 0.82f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp)
                        )
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
private fun CoffeeInviteInboxCard(
    invites: List<CoffeeBuddyInviteItem>,
    onAccept: (CoffeeBuddyInviteItem) -> Unit,
    onDecline: (CoffeeBuddyInviteItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CoffeeRadius.lg))
            .background(
                Brush.linearGradient(
                    listOf(
                        meetSurfaceElevated.copy(alpha = 0.58f),
                        meetSurface.copy(alpha = 0.72f)
                    )
                )
            )
            .border(1.dp, meetBorderWarm, RoundedCornerShape(CoffeeRadius.lg))
            .padding(CoffeeSpacing.md),
        verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)
    ) {
        Text(
            text = stringResource(R.string.meet_coffee_invite_incoming_title),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = meetCream
        )

        invites.take(5).forEach { invite ->
            val placeLine = formatInvitePlaceLine(invite.placeName)
            val showNote = shouldShowInviteNote(invite.message)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CoffeeRadius.md))
                    .background(meetSurface.copy(alpha = 0.68f))
                    .border(1.dp, meetBorderWarm.copy(alpha = 0.8f), RoundedCornerShape(CoffeeRadius.md))
                    .padding(horizontal = CoffeeSpacing.sm, vertical = CoffeeSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InviteAvatar(
                        displayName = invite.senderDisplayName,
                        avatarUrl = invite.senderAvatarUrl
                    )
                    Text(
                        text = invite.senderDisplayName,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = meetCream,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = formatInviteTimeWindow(invite),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = meetCream.copy(alpha = 0.96f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                placeLine?.let { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = meetMutedTan.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (showNote) {
                    Text(
                        text = "Not: ${invite.message.trim()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = meetMutedTan.copy(alpha = 0.95f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CoffeeSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(CoffeeRadius.pill))
                            .background(meetBgDeep.copy(alpha = 0.45f))
                            .border(1.dp, meetBorderWarm.copy(alpha = 0.6f), RoundedCornerShape(CoffeeRadius.pill))
                            .clickable { onDecline(invite) }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.meet_coffee_invite_decline),
                            style = MaterialTheme.typography.labelSmall,
                            color = meetMutedTan
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(CoffeeRadius.pill))
                            .background(meetAmber.copy(alpha = 0.22f))
                            .border(1.dp, meetAmber.copy(alpha = 0.65f), RoundedCornerShape(CoffeeRadius.pill))
                            .clickable { onAccept(invite) }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.meet_coffee_invite_accept),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = meetAmber
                        )
                    }
                }

                Text(
                    text = "Kabul ederseniz direkt sohbet başlar",
                    style = MaterialTheme.typography.labelSmall,
                    color = meetMutedTan.copy(alpha = 0.88f)
                )
            }
        }
    }
}

@Composable
private fun CoffeeInviteOutgoingCard(
    invites: List<CoffeeBuddyInviteItem>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CoffeeRadius.lg))
            .background(
                Brush.linearGradient(
                    listOf(
                        meetSurfaceElevated.copy(alpha = 0.58f),
                        meetSurface.copy(alpha = 0.72f)
                    )
                )
            )
            .border(1.dp, meetBorderWarm, RoundedCornerShape(CoffeeRadius.lg))
            .padding(CoffeeSpacing.md),
        verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)
    ) {
        Text(
            text = "Gönderilen kahve davetleri",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = meetCream
        )

        invites.take(5).forEach { invite ->
            val placeLine = formatInvitePlaceLine(invite.placeName)
            val showNote = shouldShowInviteNote(invite.message)
            val normalizedStatus = invite.status.trim().lowercase(Locale.ROOT)
            val statusText = when (normalizedStatus) {
                "accepted" -> stringResource(R.string.meet_coffee_invite_accepted)
                "declined" -> stringResource(R.string.meet_coffee_invite_declined)
                else -> stringResource(R.string.suggestion_status_pending)
            }
            val statusColor = when (normalizedStatus) {
                "accepted" -> meetAmber
                "declined" -> meetMutedTan
                else -> meetCream.copy(alpha = 0.9f)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CoffeeRadius.md))
                    .background(meetSurface.copy(alpha = 0.68f))
                    .border(1.dp, meetBorderWarm.copy(alpha = 0.8f), RoundedCornerShape(CoffeeRadius.md))
                    .padding(horizontal = CoffeeSpacing.sm, vertical = CoffeeSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InviteAvatar(
                        displayName = invite.senderDisplayName,
                        avatarUrl = invite.senderAvatarUrl
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = invite.senderDisplayName,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = meetCream,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(CoffeeRadius.pill))
                            .background(statusColor.copy(alpha = 0.14f))
                            .border(1.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(CoffeeRadius.pill))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                    }
                }

                Text(
                    text = formatInviteTimeWindow(invite),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = meetCream.copy(alpha = 0.96f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                placeLine?.let { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = meetMutedTan.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (showNote) {
                    Text(
                        text = "Not: ${invite.message.trim()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = meetMutedTan.copy(alpha = 0.95f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AcceptedInviteChatEntryCard(
    invites: List<CoffeeBuddyInviteItem>,
    onOpenChat: (CoffeeBuddyInviteItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CoffeeRadius.lg))
            .background(
                Brush.linearGradient(
                    listOf(
                        meetSurfaceElevated.copy(alpha = 0.58f),
                        meetSurface.copy(alpha = 0.72f)
                    )
                )
            )
            .border(1.dp, meetBorderWarm, RoundedCornerShape(CoffeeRadius.lg))
            .padding(CoffeeSpacing.md),
        verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)
    ) {
        Text(
            text = "Kabul edilen davetler",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = meetCream
        )

        invites.take(5).forEach { invite ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CoffeeRadius.md))
                    .background(meetSurface.copy(alpha = 0.68f))
                    .border(1.dp, meetBorderWarm.copy(alpha = 0.8f), RoundedCornerShape(CoffeeRadius.md))
                    .padding(horizontal = CoffeeSpacing.sm, vertical = CoffeeSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = invite.senderDisplayName,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = meetCream,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Yer: ${invite.placeName.ifBlank { "-" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = meetMutedTan.copy(alpha = 0.95f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Zaman: ${formatInviteTimeWindow(invite)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = meetMutedTan.copy(alpha = 0.95f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (invite.lastMessage.isNotBlank()) {
                        Text(
                            text = "Son mesaj: ${invite.lastMessage}",
                            style = MaterialTheme.typography.bodySmall,
                            color = meetMutedTan.copy(alpha = 0.95f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatChatPreviewTime(invite.lastMessageAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = meetMutedTan.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = "Mesajlaş",
                    style = MaterialTheme.typography.labelSmall,
                    color = meetAmber,
                    modifier = Modifier
                        .clip(RoundedCornerShape(CoffeeRadius.pill))
                        .clickable { onOpenChat(invite) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CoffeeBuddyChatDialog(
    counterpartName: String,
    currentUserId: String,
    messages: List<CoffeeBuddyChatMessageItem>,
    draft: String,
    showQuickActions: Boolean,
    onDraftChange: (String) -> Unit,
    onQuickActionSelected: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = counterpartName,
                color = meetCream
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        val isMine = message.senderUserId == currentUserId
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(CoffeeRadius.md))
                                    .background(
                                        if (isMine) meetAmber.copy(alpha = 0.22f)
                                        else meetSurface.copy(alpha = 0.8f)
                                    )
                                    .border(
                                        1.dp,
                                        meetBorderWarm.copy(alpha = 0.7f),
                                        RoundedCornerShape(CoffeeRadius.md)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = meetCream
                                )
                            }
                        }
                    }
                }

                if (showQuickActions) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CoffeeSpacing.xs)
                    ) {
                        items(CHAT_QUICK_ACTION_CHIPS) { quickAction ->
                            InviteSelectionChip(
                                label = quickAction,
                                selected = false,
                                onClick = { onQuickActionSelected(quickAction) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = onDraftChange,
                        modifier = Modifier.weight(1f),
                        label = { Text("Mesaj") },
                        maxLines = 3
                    )
                    Text(
                        text = "Gönder",
                        style = MaterialTheme.typography.labelSmall,
                        color = meetAmber,
                        modifier = Modifier
                            .clip(RoundedCornerShape(CoffeeRadius.pill))
                            .clickable(onClick = onSend)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        },
        containerColor = meetSurface,
        textContentColor = meetCream
    )
}

@Composable
private fun CoffeeInviteComposerDialog(
    targetDisplayName: String,
    targetCompatibility: String?,
    targetDistance: String?,
    selectedTimeOption: InviteQuickTimeOption,
    placeName: String,
    inviteDate: Long,
    startTime: Long,
    endTime: Long,
    message: String,
    coffeePreference: String,
    isSending: Boolean,
    onTimeOptionChange: (InviteQuickTimeOption) -> Unit,
    onPlaceNameChange: (String) -> Unit,
    onDateClick: () -> Unit,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    onMessageChange: (String) -> Unit,
    onCoffeePreferenceChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Kahveye davet et",
                color = meetCream
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)
            ) {
                Text(
                    text = "Hızlıca saat seç, davet gönder",
                    style = MaterialTheme.typography.bodySmall,
                    color = meetMutedTan
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(meetBgDeep.copy(alpha = 0.4f))
                        .border(1.dp, meetBorderWarm.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = targetDisplayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = meetCream
                    )
                    targetCompatibility?.let { summary ->
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = meetMutedTan
                        )
                    }
                    targetDistance?.let { distance ->
                        Text(
                            text = distance,
                            style = MaterialTheme.typography.bodySmall,
                            color = meetMutedTan
                        )
                    }
                }
                Text(
                    text = "Zaman",
                    style = MaterialTheme.typography.labelMedium,
                    color = meetCream
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(CoffeeSpacing.xs)
                ) {
                    items(InviteQuickTimeOption.entries) { option ->
                        InviteSelectionChip(
                            label = option.label,
                            selected = option == selectedTimeOption,
                            onClick = { onTimeOptionChange(option) }
                        )
                    }
                }
                if (selectedTimeOption == InviteQuickTimeOption.CUSTOM) {
                    OutlinedTextField(
                        value = formatDateForField(inviteDate),
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onDateClick),
                        label = { Text("Tarih") },
                        readOnly = true,
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(CoffeeSpacing.sm)) {
                        OutlinedTextField(
                            value = formatTimeForField(startTime),
                            onValueChange = {},
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onStartTimeClick),
                            label = { Text("Başlangıç") },
                            readOnly = true,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = formatTimeForField(endTime),
                            onValueChange = {},
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onEndTimeClick),
                            label = { Text("Bitiş") },
                            readOnly = true,
                            singleLine = true
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = formatInvitePreviewTimeWindow(
                            selectedTimeOption = selectedTimeOption,
                            inviteDate = inviteDate,
                            startTime = startTime,
                            endTime = endTime
                        ),
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Seçili zaman") },
                        readOnly = true,
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = placeName,
                    onValueChange = onPlaceNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Konum (opsiyonel)") },
                    placeholder = { Text(INVITE_DEFERRED_LOCATION_LABEL) },
                    singleLine = true
                )
                Text(
                    text = "Kahve tercihin (opsiyonel)",
                    style = MaterialTheme.typography.labelMedium,
                    color = meetCream
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(CoffeeSpacing.xs)
                ) {
                    items(INVITE_COFFEE_PREF_OPTIONS) { option ->
                        InviteSelectionChip(
                            label = option,
                            selected = option == coffeePreference,
                            onClick = { onCoffeePreferenceChange(option) }
                        )
                    }
                }
                OutlinedTextField(
                    value = message,
                    onValueChange = onMessageChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Not (opsiyonel)") },
                    placeholder = { Text("Kısa bir not ekle (opsiyonel)") },
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onSend,
                    enabled = !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = meetCream
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Gönderiliyor...")
                    } else {
                        Text(text = "Davet Gönder")
                    }
                }
                Text(
                    text = "Karşı taraf kabul ederse mesajlaşmaya geçersiniz",
                    style = MaterialTheme.typography.labelSmall,
                    color = meetMutedTan
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSending
            ) {
                Text(text = "Vazgeç")
            }
        },
        containerColor = meetSurface,
        textContentColor = meetCream
    )
}

@Composable
private fun InviteSelectionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(CoffeeRadius.pill))
            .background(if (selected) meetCaramel.copy(alpha = 0.28f) else meetBgDeep.copy(alpha = 0.45f))
            .border(
                width = 1.dp,
                color = if (selected) meetAmber.copy(alpha = 0.6f) else meetBorderWarm.copy(alpha = 0.35f),
                shape = RoundedCornerShape(CoffeeRadius.pill)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) meetCream else meetMutedTan
        )
    }
}

private enum class InviteQuickTimeOption(val label: String) {
    NOW("Şimdi"),
    TONIGHT("Bugün akşam"),
    TOMORROW("Yarın"),
    CUSTOM("Zaman seç")
}

private data class InviteWindow(
    val inviteDate: Long,
    val startTime: Long,
    val endTime: Long
)

private const val INVITE_DEFERRED_LOCATION_LABEL = "Mekanı sonra belirleyelim"
private const val INVITE_COFFEE_PREF_DEFAULT = "Fark etmez"
private val INVITE_COFFEE_PREF_OPTIONS = listOf(
    INVITE_COFFEE_PREF_DEFAULT,
    "Sade",
    "Sütlü",
    "Yoğun"
)
private val CHAT_QUICK_ACTION_CHIPS = listOf(
    "Bugün uygunum",
    "Yarın olur",
    "Saat seçelim",
    "Mekan öner"
)

private fun resolveInviteWindowForOption(
    option: InviteQuickTimeOption,
    anchorMillis: Long,
    customInviteDate: Long = startOfDayMillis(anchorMillis),
    customStartTime: Long = nextRoundedTimeMillis(anchorMillis),
    customEndTime: Long = nextRoundedTimeMillis(anchorMillis) + 2 * 60 * 60 * 1000L
): InviteWindow {
    val nowRounded = nextRoundedTimeMillis(anchorMillis)
    val today = Calendar.getInstance().apply { timeInMillis = anchorMillis }
    val tonightStart = Calendar.getInstance().apply {
        timeInMillis = anchorMillis
        set(Calendar.HOUR_OF_DAY, 19)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val tomorrow = Calendar.getInstance().apply {
        timeInMillis = anchorMillis
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 15)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    return when (option) {
        InviteQuickTimeOption.NOW -> InviteWindow(
            inviteDate = startOfDayMillis(nowRounded),
            startTime = nowRounded,
            endTime = nowRounded + 2 * 60 * 60 * 1000L
        )

        InviteQuickTimeOption.TONIGHT -> InviteWindow(
            inviteDate = startOfDayMillis(today.timeInMillis),
            startTime = tonightStart,
            endTime = tonightStart + 2 * 60 * 60 * 1000L
        )

        InviteQuickTimeOption.TOMORROW -> InviteWindow(
            inviteDate = startOfDayMillis(tomorrow),
            startTime = tomorrow,
            endTime = tomorrow + 2 * 60 * 60 * 1000L
        )

        InviteQuickTimeOption.CUSTOM -> InviteWindow(
            inviteDate = customInviteDate,
            startTime = customStartTime,
            endTime = customEndTime
        )
    }
}

private fun appendQuickChatAction(currentDraft: String, quickAction: String): String {
    val normalizedAction = quickAction.trim()
    if (normalizedAction.isBlank()) return currentDraft

    val trimmedEnd = currentDraft.trimEnd()
    if (trimmedEnd.endsWith(normalizedAction)) {
        return currentDraft
    }
    if (trimmedEnd.isBlank()) {
        return normalizedAction
    }
    val separator = if (trimmedEnd.endsWith("\n") || trimmedEnd.endsWith(" ")) "" else " "
    return trimmedEnd + separator + normalizedAction
}

private fun showDatePicker(
    context: android.content.Context,
    initialDateMillis: Long,
    onDateSelected: (Long) -> Unit
) {
    val initial = Calendar.getInstance().apply { timeInMillis = initialDateMillis }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selected = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onDateSelected(selected.timeInMillis)
        },
        initial.get(Calendar.YEAR),
        initial.get(Calendar.MONTH),
        initial.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun showTimePicker(
    context: android.content.Context,
    initialTimeMillis: Long,
    onTimeSelected: (Long) -> Unit
) {
    val initial = Calendar.getInstance().apply { timeInMillis = initialTimeMillis }
    TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val selected = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onTimeSelected(selected.timeInMillis)
        },
        initial.get(Calendar.HOUR_OF_DAY),
        initial.get(Calendar.MINUTE),
        true
    ).show()
}

private fun startOfDayMillis(sourceMillis: Long): Long {
    val calendar = Calendar.getInstance().apply { timeInMillis = sourceMillis }
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun nextRoundedTimeMillis(sourceMillis: Long): Long {
    val calendar = Calendar.getInstance().apply { timeInMillis = sourceMillis }
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val minute = calendar.get(Calendar.MINUTE)
    val remainder = minute % 30
    if (remainder != 0) {
        calendar.add(Calendar.MINUTE, 30 - remainder)
    }
    return calendar.timeInMillis
}

private fun mergeDateAndTime(dateMillis: Long, timeMillis: Long): Long {
    val date = Calendar.getInstance().apply { timeInMillis = dateMillis }
    val time = Calendar.getInstance().apply { timeInMillis = timeMillis }
    val merged = Calendar.getInstance().apply { timeInMillis = dateMillis }
    merged.set(Calendar.YEAR, date.get(Calendar.YEAR))
    merged.set(Calendar.MONTH, date.get(Calendar.MONTH))
    merged.set(Calendar.DAY_OF_MONTH, date.get(Calendar.DAY_OF_MONTH))
    merged.set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY))
    merged.set(Calendar.MINUTE, time.get(Calendar.MINUTE))
    merged.set(Calendar.SECOND, 0)
    merged.set(Calendar.MILLISECOND, 0)
    return merged.timeInMillis
}

private fun formatDateForField(dateMillis: Long): String {
    return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(dateMillis))
}

private fun formatTimeForField(timeMillis: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMillis))
}

private fun formatInvitePreviewTimeWindow(
    selectedTimeOption: InviteQuickTimeOption,
    inviteDate: Long,
    startTime: Long,
    endTime: Long
): String {
    if (startTime <= 0L || endTime <= 0L || endTime <= startTime) return "-"
    val startLabel = formatTimeForField(startTime)
    val endLabel = formatTimeForField(endTime)
    val durationMinutes = ((endTime - startTime) / 60_000L).toInt().coerceAtLeast(1)
    val durationLabel = when {
        durationMinutes >= 60 && durationMinutes % 60 == 0 ->
            "Önümüzdeki ${durationMinutes / 60} saat"
        durationMinutes >= 60 -> {
            val hours = durationMinutes / 60
            val minutes = durationMinutes % 60
            "Önümüzdeki ${hours}s ${minutes}dk"
        }
        else -> "Önümüzdeki ${durationMinutes} dk"
    }
    val dateLabel = SimpleDateFormat("d MMMM", Locale("tr", "TR")).format(Date(inviteDate.takeIf { it > 0L } ?: startTime))
    return when (selectedTimeOption) {
        InviteQuickTimeOption.NOW -> "Hemen • $durationLabel"
        InviteQuickTimeOption.TONIGHT -> "Bugün akşam • $startLabel - $endLabel"
        InviteQuickTimeOption.TOMORROW -> "Yarın • $startLabel - $endLabel"
        InviteQuickTimeOption.CUSTOM -> "$dateLabel • $startLabel - $endLabel"
    }
}

@Composable
private fun InviteAvatar(
    displayName: String,
    avatarUrl: String?
) {
    if (!avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(1.dp, meetAmber.copy(alpha = 0.6f), CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(meetAmber.copy(alpha = 0.24f))
                .border(1.dp, meetAmber.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initialsFromName(displayName),
                style = MaterialTheme.typography.labelSmall,
                color = meetCream
            )
        }
    }
}

private fun formatInvitePlaceLine(placeName: String): String? {
    val normalized = placeName.trim()
    if (normalized.isBlank()) return null
    if (normalized.equals(INVITE_DEFERRED_LOCATION_LABEL, ignoreCase = true)) return null
    return "Yer: $normalized"
}

private fun shouldShowInviteNote(message: String): Boolean {
    val normalized = message.trim()
    if (normalized.isBlank()) return false
    val defaultMessage = MeetViewModel.DEFAULT_COFFEE_INVITE_MESSAGE
    if (normalized == defaultMessage) return false
    if (normalized.startsWith("$defaultMessage\nKahve tercihim:", ignoreCase = true)) return false
    return true
}

private fun formatInviteTimeWindow(invite: CoffeeBuddyInviteItem): String {
    val start = invite.startTime
    val end = invite.endTime
    if (start <= 0L || end <= 0L || end <= start) return "-"
    val now = System.currentTimeMillis()
    val effectiveDate = invite.inviteDate.takeIf { it > 0L } ?: start
    val startLabel = formatTimeForField(start)
    val endLabel = formatTimeForField(end)

    val nowWindowStart = start - 30 * 60 * 1000L
    if (now in nowWindowStart..end) {
        val remainingMinutes = ((end - maxOf(now, start)) / 60_000L).toInt().coerceAtLeast(1)
        val remainingLabel = when {
            remainingMinutes >= 60 && remainingMinutes % 60 == 0 ->
                "Önümüzdeki ${remainingMinutes / 60} saat"
            remainingMinutes >= 60 -> {
                val hours = remainingMinutes / 60
                val minutes = remainingMinutes % 60
                "Önümüzdeki ${hours}s ${minutes}dk"
            }
            else -> "Önümüzdeki ${remainingMinutes} dk"
        }
        return "Hemen • $remainingLabel"
    }

    val today = Calendar.getInstance()
    val compare = Calendar.getInstance().apply { timeInMillis = effectiveDate }
    val sameYear = today.get(Calendar.YEAR) == compare.get(Calendar.YEAR)
    val dayDiff = compare.get(Calendar.DAY_OF_YEAR) - today.get(Calendar.DAY_OF_YEAR)
    val startHour = Calendar.getInstance().apply { timeInMillis = start }.get(Calendar.HOUR_OF_DAY)

    return when {
        sameYear && dayDiff == 0 && startHour >= 18 -> "Bugün akşam • $startLabel - $endLabel"
        sameYear && dayDiff == 1 -> "Yarın • $startLabel - $endLabel"
        sameYear && dayDiff == 0 -> "Bugün • $startLabel - $endLabel"
        else -> {
            val dateLabel = SimpleDateFormat("d MMMM", Locale("tr", "TR")).format(Date(effectiveDate))
            "$dateLabel • $startLabel - $endLabel"
        }
    }
}

private fun formatChatPreviewTime(timestamp: Long): String {
    if (timestamp <= 0L) return "-"
    val today = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = timestamp }
    val isToday = today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    return if (isToday) {
        "Bugün ${formatTimeForField(timestamp)}"
    } else {
        "${formatDateForField(timestamp)} ${formatTimeForField(timestamp)}"
    }
}

@Composable
private fun DebugMeetTruthPanel(
    rawFirestoreCount: Int,
    visibleAfterFiltersCount: Int,
    showRawFeed: Boolean,
    onToggleRawFeed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x33220F08))
            .border(1.dp, Color(0x55DBA15E), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "DEBUG FEED TRUTH",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFFFD8AE)
        )
        Text(
            text = "rawFirestoreCount=$rawFirestoreCount  visibleAfterFiltersCount=$visibleAfterFiltersCount",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFF2D4B3)
        )
        Text(
            text = if (showRawFeed) "Hide raw Firestore events" else "Show raw Firestore events",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFFFD8AE),
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onToggleRawFeed)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun DebugRawMeetCard(raw: MeetRawDebugEvent) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x2E2A130A))
            .border(1.dp, Color(0x66DBA15E), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(
            text = "RAW • id=${raw.id} | title=${raw.title} | status=${raw.status} | isDeleted=${raw.isDeleted} | hostId=${raw.hostId} | scheduledAt=${raw.scheduledAt}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFEFD6BA)
        )
    }
}

@Composable
private fun DebugVisibleMeetMeta(
    event: CoffeeMeet,
    raw: MeetRawDebugEvent?
) {
    Text(
        text = "VISIBLE • id=${event.id} | title=${event.title} | status=${raw?.status ?: "missing"} | isDeleted=${raw?.isDeleted ?: "missing"} | hostId=${event.hostId} | scheduledAt=${event.scheduledAt}",
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFFE7C9A6),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x26261109))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    )
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
