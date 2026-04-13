package com.icoffee.app.ui.screens.meet

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.icoffee.app.data.model.BusinessOffer
import com.icoffee.app.data.model.CoffeeMeet
import com.icoffee.app.data.model.EventHostType
import com.icoffee.app.data.model.OfferPaymentMode
import com.icoffee.app.data.model.UserType
import com.icoffee.app.data.model.formattedPriceOrNull
import com.icoffee.app.ui.theme.CoffeeSpacing
import com.icoffee.app.viewmodel.MeetJoinAttemptResult
import com.icoffee.app.viewmodel.MeetJoinFailureReason
import com.icoffee.app.viewmodel.MeetViewModel
import kotlinx.coroutines.launch

@Composable
fun EventDetailScreen(
    meetId: String,
    onBack: () -> Unit,
    isUserSignedIn: Boolean,
    onRequestSignIn: () -> Unit,
    onEditEvent: (String) -> Unit,
    onOpenPaywall: () -> Unit,
    meetViewModel: MeetViewModel = viewModel()
) {
    val event = meetViewModel.findMeet(meetId)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val joinSuccess = stringResource(R.string.meet_join_success)
    val leaveSuccess = stringResource(R.string.meet_leave_success)
    val fullMessage = stringResource(R.string.meet_full_message)
    val joinLimitMessage = stringResource(R.string.meet_entitlement_monthly_join_limit_reached)
    val genericErrorMessage = stringResource(R.string.meet_entitlement_generic_error)
    var showLeaveConfirm by rememberSaveable { mutableStateOf(false) }
    var showCancelConfirm by rememberSaveable { mutableStateOf(false) }
    val cancelSuccess = stringResource(R.string.meet_cancel_success)

    if (event == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5EFE7)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.meet_event_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF5E3D2A)
                )
                TextButton(onClick = onBack) {
                    Text(
                        text = stringResource(R.string.meet_back),
                        color = Color(0xFF8F5F3A),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        return
    }

    LaunchedEffect(isUserSignedIn) {
        meetViewModel.refreshEntitlements()
    }

    var detailOpenedLogged by rememberSaveable(event.id) { mutableStateOf(false) }
    LaunchedEffect(event.id, detailOpenedLogged) {
        if (!detailOpenedLogged) {
            GrowthAnalytics.log(
                GrowthEventNames.EVENT_DETAIL_OPENED,
                params = mapOf("eventId" to event.id)
            )
            detailOpenedLogged = true
        }
    }

    val isJoined = event.participants.contains(meetViewModel.currentUserId)
    val isFull = meetViewModel.isFull(event)
    val isHost = event.hostId == meetViewModel.currentUserId
    val joinLimitReached = isUserSignedIn && meetViewModel.entitlementLoaded && !meetViewModel.canJoin
    val socialProofItems = deriveSocialProofItems(event)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5EFE7)),
        contentPadding = PaddingValues(horizontal = CoffeeSpacing.lg, vertical = CoffeeSpacing.md),
        verticalArrangement = Arrangement.spacedBy(CoffeeSpacing.lg)
    ) {
        item {
            EventHero(
                event = event,
                onBack = onBack
            )
        }

        item {
            EventMetaBlock(event = event)
        }

        if (socialProofItems.isNotEmpty()) {
            item {
                EventSocialProofRow(items = socialProofItems)
            }
        }

        item {
            EventShareInviteRow(
                onShare = {
                    when (EventShareHelper.shareEvent(context, event)) {
                        EventShareHelper.ShareLaunchResult.LAUNCHED -> {
                            GrowthAnalytics.log(
                                GrowthEventNames.EVENT_SHARE_CLICKED,
                                params = mapOf("eventId" to event.id, "source" to "event_detail")
                            )
                            GrowthAnalytics.log(
                                GrowthEventNames.SHARE_FROM_EVENT_DETAIL,
                                params = mapOf("eventId" to event.id)
                            )
                            GrowthAnalytics.log(
                                GrowthEventNames.EVENT_SHARE_COMPLETED,
                                params = mapOf("eventId" to event.id, "source" to "event_detail")
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

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.meet_description_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF3D281C)
                )
                Text(
                    text = event.description,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF5B4637)
                )
            }
        }

        item {
            LocationCard(
                event = event,
                onOpenMap = { context.openMapFor(event) }
            )
        }

        event.businessOffer?.let { offer ->
            item {
                EventOfferSection(offer = offer)
            }
        }

        item {
            ParticipantsCard(event = event)
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isHost) {
                    HostPrimaryCta(
                        text = stringResource(R.string.meet_edit_event),
                        onClick = { onEditEvent(event.id) }
                    )
                    Text(
                        text = stringResource(R.string.meet_cancel_event),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFF8A6B55),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { showCancelConfirm = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                } else {
                    JoinMeetCta(
                        isJoined = isJoined,
                        isFull = isFull,
                        isLimitReached = joinLimitReached,
                        onClick = {
                            GrowthAnalytics.log(
                                GrowthEventNames.JOIN_CTA_CLICKED,
                                params = mapOf("eventId" to event.id)
                            )
                            if (!isUserSignedIn) {
                                onRequestSignIn()
                                return@JoinMeetCta
                            }

                            if (joinLimitReached && !isJoined) {
                                Toast.makeText(context, joinLimitMessage, Toast.LENGTH_SHORT).show()
                                GrowthAnalytics.log(
                                    GrowthEventNames.PAYWALL_OPENED_FROM_JOIN_LIMIT,
                                    params = mapOf("eventId" to event.id)
                                )
                                onOpenPaywall()
                                return@JoinMeetCta
                            }

                            if (!isJoined) {
                                scope.launch {
                                    when (val result = meetViewModel.joinMeet(event.id)) {
                                        MeetJoinAttemptResult.Success -> {
                                            Toast.makeText(context, joinSuccess, Toast.LENGTH_SHORT).show()
                                            GrowthAnalytics.log(
                                                GrowthEventNames.JOIN_SUCCESS,
                                                params = mapOf("eventId" to event.id)
                                            )
                                            meetViewModel.refreshEntitlements()
                                        }

                                        is MeetJoinAttemptResult.Failure -> {
                                            val message = when (result.reason) {
                                                MeetJoinFailureReason.NOT_SIGNED_IN ->
                                                    context.getString(R.string.meet_sign_in_notice)

                                                MeetJoinFailureReason.EVENT_NOT_FOUND ->
                                                    context.getString(R.string.meet_event_not_found)

                                                MeetJoinFailureReason.ALREADY_JOINED ->
                                                    context.getString(R.string.meet_joined)

                                                MeetJoinFailureReason.EVENT_FULL ->
                                                    fullMessage

                                                MeetJoinFailureReason.MONTHLY_LIMIT_REACHED ->
                                                    joinLimitMessage

                                                MeetJoinFailureReason.IN_PROGRESS ->
                                                    context.getString(R.string.meet_entitlement_request_in_progress)

                                                MeetJoinFailureReason.STORE_ERROR ->
                                                    genericErrorMessage
                                            }

                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                            meetViewModel.refreshEntitlements()

                                            if (result.reason == MeetJoinFailureReason.MONTHLY_LIMIT_REACHED) {
                                                GrowthAnalytics.log(
                                                    GrowthEventNames.PAYWALL_OPENED_FROM_JOIN_LIMIT,
                                                    params = mapOf("eventId" to event.id)
                                                )
                                                onOpenPaywall()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )

                    if (joinLimitReached && !isJoined) {
                        Text(
                            text = stringResource(R.string.meet_entitlement_upgrade_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8A6B55),
                            modifier = Modifier.clickable {
                                GrowthAnalytics.log(
                                    GrowthEventNames.PAYWALL_OPENED_FROM_JOIN_LIMIT,
                                    params = mapOf("eventId" to event.id, "source" to "hint")
                                )
                                onOpenPaywall()
                            }
                        )
                    }

                    if (isJoined) {
                        Text(
                            text = stringResource(R.string.meet_leave_this_meet),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF8A6B55),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable { showLeaveConfirm = true }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        item {
            Box(modifier = Modifier.height(16.dp))
        }
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = {
                Text(
                    text = stringResource(R.string.meet_cancel_confirm_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF3D281C)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.meet_cancel_confirm_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5E4A3C)
                )
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) {
                    Text(
                        text = stringResource(R.string.meet_cancel),
                        color = Color(0xFF7A5D49)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelConfirm = false
                        if (meetViewModel.cancelMeet(event.id)) {
                            Toast.makeText(context, cancelSuccess, Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.meet_cancel_confirm_action),
                        color = Color(0xFF8F5F3A),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            containerColor = Color(0xFFFFFCF8),
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = {
                Text(
                    text = stringResource(R.string.meet_leave_confirm_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF3D281C)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.meet_leave_confirm_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5E4A3C)
                )
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) {
                    Text(
                        text = stringResource(R.string.meet_cancel),
                        color = Color(0xFF7A5D49)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveConfirm = false
                        if (meetViewModel.leaveMeet(event.id)) {
                            Toast.makeText(context, leaveSuccess, Toast.LENGTH_SHORT).show()
                            meetViewModel.refreshEntitlements()
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.meet_leave_action),
                        color = Color(0xFF8F5F3A),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            containerColor = Color(0xFFFFFCF8),
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun HostPrimaryCta(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(999.dp),
                clip = false,
                ambientColor = Color(0x55B67A4D),
                spotColor = Color(0x44B67A4D)
            )
            .clip(RoundedCornerShape(999.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFB67A4D), Color(0xFFE69A3A))))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White
        )
    }
}

@Composable
private fun EventHero(
    event: CoffeeMeet,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(244.dp)
            .shadow(10.dp, RoundedCornerShape(30.dp), clip = false)
            .clip(RoundedCornerShape(30.dp))
    ) {
        Image(
            painter = painterResource(id = heroImageFor(event)),
            contentDescription = event.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x22000000),
                            Color(0x77000000),
                            Color(0xCC000000)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x54000000))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.meet_back),
                    tint = Color(0xFFF5E6D3)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (event.hostType == EventHostType.BUSINESS || event.hostUserType == UserType.BUSINESS) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0x339D6A43))
                            .border(1.dp, Color(0x55F0D4B8), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.meet_business_event),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFF5E6D3)
                        )
                    }
                }
                if (event.businessOffer != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0x336B452C))
                            .border(1.dp, Color(0x55F0D4B8), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.meet_offer_badge),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFF5E6D3)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x40B67A4D))
                    .border(1.dp, Color(0x55F2D1B1), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = event.purpose,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFF5E6D3)
                )
            }
            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EventMetaBlock(event: CoffeeMeet) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFFFFFCF8))
            .border(1.dp, Color(0xFFEADDCF), RoundedCornerShape(22.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetaPill(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Schedule,
            text = event.time
        )
        MetaPill(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Groups,
            text = stringResource(
                R.string.meet_participant_ratio,
                event.participants.size,
                event.maxParticipants.takeIf { it > 0 } ?: 10
            )
        )
    }
}

private data class EventSocialProofItem(
    val label: String,
    val emphasis: Boolean = false
)

@Composable
private fun EventSocialProofRow(items: List<EventSocialProofItem>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (item.emphasis) Color(0x2AB67A4D) else Color(0x14B67A4D))
                    .border(
                        width = 1.dp,
                        color = if (item.emphasis) Color(0x55B67A4D) else Color(0x33B67A4D),
                        shape = RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (item.emphasis) Color(0xFF7D4E2E) else Color(0xFF8A6B55)
                )
            }
        }
    }
}

@Composable
private fun EventShareInviteRow(
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0x1FB67A4D))
                .clickable(onClick = onShare)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = Color(0xFF8F5F3A),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.meet_share_action),
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF8F5F3A)
            )
        }
    }
}

@Composable
private fun deriveSocialProofItems(event: CoffeeMeet): List<EventSocialProofItem> {
    val safeCapacity = event.maxParticipants.takeIf { it > 0 } ?: 10
    val joined = event.participants.size
    val remaining = (safeCapacity - joined).coerceAtLeast(0)
    val occupancy = if (safeCapacity <= 0) 0.0 else joined.toDouble() / safeCapacity.toDouble()

    val items = mutableListOf<EventSocialProofItem>()
    items += EventSocialProofItem(
        label = stringResource(R.string.meet_social_joined_count, joined),
        emphasis = joined >= 5
    )

    when {
        remaining <= 0 -> {
            items += EventSocialProofItem(
                label = stringResource(R.string.meet_social_full),
                emphasis = true
            )
        }

        remaining <= 3 -> {
            items += EventSocialProofItem(
                label = stringResource(R.string.meet_social_spots_left, remaining),
                emphasis = true
            )
        }
    }

    if (occupancy >= 0.7 && remaining > 0) {
        items += EventSocialProofItem(
            label = stringResource(R.string.meet_social_popular),
            emphasis = false
        )
    }

    val createdAt = event.id
        .substringAfter("meet_", "")
        .toLongOrNull()
    if (createdAt != null && (System.currentTimeMillis() - createdAt) <= 24L * 60L * 60L * 1000L) {
        items += EventSocialProofItem(
            label = stringResource(R.string.meet_social_new),
            emphasis = false
        )
    }
    return items.take(3)
}

@Composable
private fun MetaPill(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8EFE6))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF9A7352),
            modifier = Modifier.size(17.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF5F4B3E),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LocationCard(
    event: CoffeeMeet,
    onOpenMap: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFFFFFCF8))
            .border(1.dp, Color(0xFFEADDCF), RoundedCornerShape(22.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFF9A7352),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = event.locationName,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF5E4A3C),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0x1FB67A4D))
                .clickable(onClick = onOpenMap)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.meet_open_map_short),
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF8F5F3A)
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFB67A4D)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = stringResource(R.string.meet_open_in_maps),
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ParticipantsCard(event: CoffeeMeet) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFFFFFCF8))
            .border(1.dp, Color(0xFFEADDCF), RoundedCornerShape(22.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ParticipantAvatarStack(participants = event.participants)
        Text(
            text = stringResource(R.string.meet_joined_count, event.participants.size),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF5C3A28)
        )
    }
}

@Composable
private fun EventOfferSection(offer: BusinessOffer) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.meet_offer_section_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF3D281C)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFFFFFCF8))
                .border(1.dp, Color(0xFFEADDCF), RoundedCornerShape(22.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = offer.offerTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF2E2018)
            )

            offer.offerDescription?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5E4A3C)
                )
            }

            Text(
                text = stringResource(R.string.meet_offer_items_label),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF5B4637)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                offer.includedItems.forEach { item ->
                    Text(
                        text = stringResource(R.string.meet_offer_item_line, item.quantity, item.label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5E4A3C)
                    )
                }
            }

            offer.formattedPriceOrNull()?.let { price ->
                Text(
                    text = stringResource(R.string.meet_offer_price_preview, price),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF8F5F3A)
                )
            }

            Text(
                text = stringResource(
                    R.string.meet_offer_payment_preview,
                    stringResource(offer.paymentMode.labelRes())
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5E4A3C)
            )

            offer.availabilityLimit?.let { limit ->
                Text(
                    text = stringResource(R.string.meet_offer_availability_preview, limit),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7A6353)
                )
            }

            offer.termsNote?.takeIf { it.isNotBlank() }?.let { terms ->
                Text(
                    text = stringResource(R.string.meet_offer_terms_preview, terms),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7A6353)
                )
            }
        }
    }
}

@Composable
private fun ParticipantAvatarStack(participants: List<String>) {
    val visibleParticipants = if (participants.isEmpty()) listOf("host") else participants.take(3)
    Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
        visibleParticipants.forEachIndexed { index, id ->
            val avatarColor = when (index % 4) {
                0 -> Color(0xFFD4B49A)
                1 -> Color(0xFFC89E7B)
                2 -> Color(0xFFB98B64)
                else -> Color(0xFFA47857)
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(avatarColor)
                    .border(2.dp, Color(0xFFFFFCF8), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarLabel(id, index),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF3B271B)
                )
            }
        }
    }
}

private fun avatarLabel(id: String, index: Int): String {
    val clean = id.substringAfter('_', id)
    val fallback = ('A' + (index % 26)).toString()
    return clean.firstOrNull()?.uppercase() ?: fallback
}

@Composable
private fun JoinMeetCta(
    isJoined: Boolean,
    isFull: Boolean,
    isLimitReached: Boolean,
    onClick: () -> Unit
) {
    val enabled = !isJoined && !isFull
    val ctaText = when {
        isJoined -> stringResource(R.string.meet_joined)
        isLimitReached -> stringResource(R.string.meet_entitlement_monthly_limit_short)
        isFull -> stringResource(R.string.meet_full)
        else -> stringResource(R.string.meet_join_meet)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .shadow(
                elevation = if (enabled) 10.dp else 4.dp,
                shape = RoundedCornerShape(999.dp),
                clip = false,
                ambientColor = Color(0x55B67A4D),
                spotColor = Color(0x44B67A4D)
            )
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (isJoined) {
                    Brush.horizontalGradient(listOf(Color(0xFF8E745E), Color(0xFF7A614D)))
                } else if (isFull) {
                    Brush.horizontalGradient(listOf(Color(0xFFCABFB3), Color(0xFFBDB1A5)))
                } else {
                    Brush.horizontalGradient(listOf(Color(0xFFB67A4D), Color(0xFFE69A3A)))
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isJoined) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFFF5E6D3),
                    modifier = Modifier.size(17.dp)
                )
            }
            Text(
                text = ctaText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
        }
    }
}

private fun heroImageFor(event: CoffeeMeet): Int {
    val purpose = event.purpose.lowercase()
    return when {
        "dating" in purpose -> R.drawable.coffee_caramel_cloud
        "study" in purpose || "work" in purpose -> R.drawable.coffee_ethiopian_sidamo
        "network" in purpose -> R.drawable.coffee_espresso
        else -> R.drawable.coffee_highlight_hero
    }
}

private fun OfferPaymentMode.labelRes(): Int = when (this) {
    OfferPaymentMode.PAY_AT_VENUE -> R.string.meet_offer_payment_at_venue
    OfferPaymentMode.INCLUDED_WITH_ATTENDANCE -> R.string.meet_offer_payment_included
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
