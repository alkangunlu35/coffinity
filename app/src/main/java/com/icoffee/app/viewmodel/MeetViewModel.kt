// FILE: app/src/main/java/com/icoffee/app/viewmodel/MeetViewModel.kt
// FULL REPLACEMENT

package com.icoffee.app.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.icoffee.app.analytics.AnalyticsEvents
import com.icoffee.app.analytics.AnalyticsParams
import com.icoffee.app.analytics.AnalyticsProvider
import com.icoffee.app.data.MeetDiscoveryEngine
import com.icoffee.app.data.MeetRawDebugEvent
import com.icoffee.app.data.MeetRepository
import com.icoffee.app.data.auth.FirebaseAuthRepository
import com.icoffee.app.data.firebase.model.FirestoreCoffeeBuddyInvite
import com.icoffee.app.data.firebase.repository.FirestoreCoffeeBuddyInvitesRepository
import com.icoffee.app.data.firebase.repository.FirestoreCoffeeChatsRepository
import com.icoffee.app.data.firebase.repository.FirestoreUsersRepository
import com.icoffee.app.data.location.LocationProvider
import com.icoffee.app.data.membership.EventEntitlements
import com.icoffee.app.data.membership.EventMembershipRepository
import com.icoffee.app.data.membership.MembershipEntitlementResolver
import com.icoffee.app.data.membership.MembershipPlan
import com.icoffee.app.data.model.BusinessOffer
import com.icoffee.app.data.model.CoffeeBuddyCandidate
import com.icoffee.app.data.model.CoffeeBuddyChatMessageItem
import com.icoffee.app.data.model.CoffeeBuddyDiscoveryResult
import com.icoffee.app.data.model.CoffeeBuddyInviteItem
import com.icoffee.app.data.model.CoffeeMeet
import com.icoffee.app.data.model.MeetDiscoverySection
import com.icoffee.app.data.model.MeetExploreSort
import com.icoffee.app.data.model.MeetMood
import com.icoffee.app.data.model.UserType
import com.icoffee.app.data.profile.UserTasteProfileRepository
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

sealed interface MeetCreateAttemptResult {
    data object Success : MeetCreateAttemptResult
    data class Failure(val reason: MeetCreateFailureReason) : MeetCreateAttemptResult
}

enum class MeetCreateFailureReason {
    NOT_SIGNED_IN,
    MONTHLY_LIMIT_REACHED,
    ATTENDEE_LIMIT_EXCEEDED,
    INVALID_CAPACITY,
    IN_PROGRESS,
    STORE_ERROR
}

sealed interface MeetJoinAttemptResult {
    data object Success : MeetJoinAttemptResult
    data class Failure(val reason: MeetJoinFailureReason) : MeetJoinAttemptResult
}

enum class MeetJoinFailureReason {
    NOT_SIGNED_IN,
    EVENT_NOT_FOUND,
    ALREADY_JOINED,
    EVENT_FULL,
    MONTHLY_LIMIT_REACHED,
    IN_PROGRESS,
    STORE_ERROR
}

class MeetViewModel : ViewModel() {
    companion object {
        private const val TAG = "CreateMeetDebug"
        const val DEFAULT_COFFEE_INVITE_MESSAGE = "Benimle bir kahve içmek ister misin?"
    }

    val currentUserId: String
        get() = FirebaseAuthRepository.currentUser?.uid ?: "guest"

    val currentUserType: UserType
        get() = UserType.NORMAL

    var meets by mutableStateOf<List<CoffeeMeet>>(emptyList())
        private set
    var rawDebugEvents by mutableStateOf<List<MeetRawDebugEvent>>(emptyList())
        private set

    var userLatitude by mutableDoubleStateOf(LocationProvider.FALLBACK_LAT)
        private set
    var userLongitude by mutableDoubleStateOf(LocationProvider.FALLBACK_LON)
        private set

    var currentPlan by mutableStateOf(MembershipPlan.FREE)
        private set
    var joinsUsed by mutableIntStateOf(0)
        private set
    var joinLimit by mutableStateOf<Int?>(MembershipEntitlementResolver.resolve(MembershipPlan.FREE).monthlyJoinLimit)
        private set
    var createsUsed by mutableIntStateOf(0)
        private set
    var createLimit by mutableStateOf<Int?>(MembershipEntitlementResolver.resolve(MembershipPlan.FREE).monthlyCreateLimit)
        private set
    var maxAttendees by mutableIntStateOf(MembershipEntitlementResolver.resolve(MembershipPlan.FREE).maxAttendeesPerEvent)
        private set
    var canJoin by mutableStateOf(false)
        private set
    var canCreate by mutableStateOf(false)
        private set
    var entitlementLoaded by mutableStateOf(false)
        private set

    fun setLocation(latitude: Double, longitude: Double) {
        userLatitude = latitude
        userLongitude = longitude
    }

    fun setLocation(latLng: LatLng, name: String) {
        userLatitude = latLng.latitude
        userLongitude = latLng.longitude
    }

    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private val joinInFlightIds = mutableSetOf<String>()
    private var createInFlight by mutableStateOf(false)
    var createMeetDebugTrace by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            MeetRepository.eventsFlow.collect { list ->
                Log.d("MEET_DEBUG", "VIEWMODEL RECEIVED size=${list.size}")
                meets = list
            }
        }
        viewModelScope.launch {
            MeetRepository.rawDebugEventsFlow.collect { list ->
                rawDebugEvents = list
            }
        }
        observeAuthState()
        refreshEntitlements()
    }

    fun refreshLocation() {
        viewModelScope.launch {
            val loc = LocationProvider.getLastKnownLocation()
                ?: LocationProvider.getCurrentLocation()
            if (loc != null) {
                userLatitude = loc.first
                userLongitude = loc.second
            }
        }
    }

    fun findMeet(meetId: String): CoffeeMeet? = meets.firstOrNull { it.id == meetId }

    fun isHost(meet: CoffeeMeet): Boolean = meet.hostId == currentUserId

    fun canJoin(meet: CoffeeMeet): Boolean =
        canJoin && currentUserId !in meet.participants && !isFull(meet)

    fun isFull(meet: CoffeeMeet): Boolean = meet.participants.size >= resolvedMaxParticipants(meet)

    fun participantLabel(meet: CoffeeMeet): String =
        "${meet.participants.size}/${resolvedMaxParticipants(meet)}"

    fun maxAllowedParticipants(): Int = maxAttendees

    suspend fun canCreateEvent(): Result<Unit> {
        val uid = FirebaseAuthRepository.currentUser?.uid?.trim().orEmpty()

        if (uid.isBlank()) {
            return Result.failure(Exception("Giriş yapmalısınız"))
        }

        val snapshot = try {
            loadMembershipSnapshot(uid)
        } catch (_: Throwable) {
            return Result.failure(Exception("Sistem hatası"))
        }

        applySnapshot(snapshot, allowActions = true)

        return if (!snapshot.canCreate) {
            Result.failure(Exception("Aylık etkinlik oluşturma limitine ulaştınız"))
        } else {
            Result.success(Unit)
        }
    }

    suspend fun joinMeet(meetId: String): MeetJoinAttemptResult {
        val uid = FirebaseAuthRepository.currentUser?.uid?.trim().orEmpty()
        if (uid.isBlank()) {
            trackEventJoinFailed(meetId, MeetJoinFailureReason.NOT_SIGNED_IN)
            return MeetJoinAttemptResult.Failure(MeetJoinFailureReason.NOT_SIGNED_IN)
        }
        val meet = meets.firstOrNull { it.id == meetId }
            ?: run {
                trackEventJoinFailed(meetId, MeetJoinFailureReason.EVENT_NOT_FOUND)
                return MeetJoinAttemptResult.Failure(MeetJoinFailureReason.EVENT_NOT_FOUND)
            }
        if (uid in meet.participants) {
            trackEventJoinFailed(meetId, MeetJoinFailureReason.ALREADY_JOINED)
            return MeetJoinAttemptResult.Failure(MeetJoinFailureReason.ALREADY_JOINED)
        }
        if (isFull(meet)) {
            trackEventJoinFailed(meetId, MeetJoinFailureReason.EVENT_FULL)
            return MeetJoinAttemptResult.Failure(MeetJoinFailureReason.EVENT_FULL)
        }
        if (joinInFlightIds.contains(meetId)) {
            trackEventJoinFailed(meetId, MeetJoinFailureReason.IN_PROGRESS)
            return MeetJoinAttemptResult.Failure(MeetJoinFailureReason.IN_PROGRESS)
        }

        val snapshot = try {
            loadMembershipSnapshot(uid)
        } catch (_: Throwable) {
            trackEventJoinFailed(meetId, MeetJoinFailureReason.STORE_ERROR)
            return MeetJoinAttemptResult.Failure(MeetJoinFailureReason.STORE_ERROR)
        }
        applySnapshot(snapshot, allowActions = true)
        if (!snapshot.canJoin) {
            trackEventJoinFailed(meetId, MeetJoinFailureReason.MONTHLY_LIMIT_REACHED)
            return MeetJoinAttemptResult.Failure(MeetJoinFailureReason.MONTHLY_LIMIT_REACHED)
        }

        joinInFlightIds += meetId
        return try {
            MeetRepository.replaceParticipants(meetId, meet.participants + uid)
            EventMembershipRepository.recordEventJoined(
                userId = uid,
                eventId = meetId
            )
            UserTasteProfileRepository.onEventJoined(meet.purpose)
            refreshEntitlementsInternal()
            val participantCountAfterJoin = (meet.participants + uid).distinct().size
            AnalyticsProvider.tracker.logEvent(
                AnalyticsEvents.EVENT_JOINED,
                mapOf(
                    AnalyticsParams.EVENT_ID to meetId,
                    AnalyticsParams.HOST_ID to meet.hostId.ifBlank { "unknown" },
                    AnalyticsParams.PARTICIPANT_COUNT_AFTER_JOIN to participantCountAfterJoin.toString()
                )
            )
            MeetJoinAttemptResult.Success
        } catch (_: Throwable) {
            trackEventJoinFailed(meetId, MeetJoinFailureReason.STORE_ERROR)
            MeetJoinAttemptResult.Failure(MeetJoinFailureReason.STORE_ERROR)
        } finally {
            joinInFlightIds -= meetId
        }
    }

    suspend fun leaveMeet(meetId: String): Boolean {
        val meet = meets.firstOrNull { it.id == meetId } ?: return false
        val uid = FirebaseAuthRepository.currentUser?.uid?.trim().orEmpty()
        if (uid.isBlank()) return false
        if (uid !in meet.participants) return false

        return try {
            MeetRepository.replaceParticipants(meetId, meet.participants - uid)
            Log.d("MEET_DEBUG", "leaveMeet success eventId=$meetId userId=$uid")
            true
        } catch (error: Throwable) {
            Log.e("MEET_DEBUG", "leaveMeet failed eventId=$meetId userId=$uid", error)
            false
        }
    }

    suspend fun cancelMeet(meetId: String): Boolean {
        val meet = meets.firstOrNull { it.id == meetId } ?: return false
        if (meet.hostId != currentUserId) return false
        Log.d("MEET_DEBUG", "cancelMeet called eventId=$meetId")
        val result = MeetRepository.cancelMeet(meetId)
        if (result.isFailure) {
            Log.e("MEET_DEBUG", "cancelMeet failed for eventId=$meetId", result.exceptionOrNull())
            return false
        }
        return true
    }

    suspend fun createMeet(
        title: String,
        description: String,
        locationName: String,
        latitude: Double,
        longitude: Double,
        scheduledAt: Long,
        timeLabel: String,
        purpose: String,
        maxParticipants: Int,
        hostUserType: UserType?,
        brewingType: String?,
        businessOffer: BusinessOffer?,
        debugBypassEntitlementGate: Boolean = false
    ): MeetCreateAttemptResult {
        Log.d("MEET_DEBUG", "createMeet called")
        createMeetDebugTrace = "start"
        val uid = FirebaseAuthRepository.currentUser?.uid?.trim().orEmpty()
        if (uid.isBlank()) {
            createMeetDebugTrace = "auth_missing_user"
            trackMeetCreateFailed(MeetCreateFailureReason.NOT_SIGNED_IN)
            return MeetCreateAttemptResult.Failure(MeetCreateFailureReason.NOT_SIGNED_IN)
        }
        if (createInFlight) {
            createMeetDebugTrace = "create_in_flight"
            trackMeetCreateFailed(MeetCreateFailureReason.IN_PROGRESS)
            return MeetCreateAttemptResult.Failure(MeetCreateFailureReason.IN_PROGRESS)
        }

        if (!debugBypassEntitlementGate) {
            val snapshot = try {
                createMeetDebugTrace = "load_membership_snapshot"
                loadMembershipSnapshot(uid)
            } catch (error: Throwable) {
                createMeetDebugTrace = "load_membership_snapshot_failed:${error.javaClass.simpleName}:${error.message}"
                Log.e(TAG, "createMeet failed at membership snapshot", error)
                trackMeetCreateFailed(MeetCreateFailureReason.STORE_ERROR)
                return MeetCreateAttemptResult.Failure(MeetCreateFailureReason.STORE_ERROR)
            }
            applySnapshot(snapshot, allowActions = true)
            if (!snapshot.canCreate) {
                createMeetDebugTrace = "membership_limit_reached"
                trackMeetCreateFailed(MeetCreateFailureReason.MONTHLY_LIMIT_REACHED)
                return MeetCreateAttemptResult.Failure(MeetCreateFailureReason.MONTHLY_LIMIT_REACHED)
            }
            if (maxParticipants > snapshot.entitlements.maxAttendeesPerEvent) {
                createMeetDebugTrace = "invalid_capacity_above_plan_limit"
                trackMeetCreateFailed(MeetCreateFailureReason.ATTENDEE_LIMIT_EXCEEDED)
                return MeetCreateAttemptResult.Failure(MeetCreateFailureReason.ATTENDEE_LIMIT_EXCEEDED)
            }
        } else {
            createMeetDebugTrace = "debug_bypass_membership_gate"
        }
        if (maxParticipants < 2) {
            createMeetDebugTrace = "invalid_capacity_below_min"
            trackMeetCreateFailed(MeetCreateFailureReason.INVALID_CAPACITY)
            return MeetCreateAttemptResult.Failure(MeetCreateFailureReason.INVALID_CAPACITY)
        }

        createInFlight = true
        return try {
            createMeetDebugTrace = "repository_create_meet"
            val eventId = MeetRepository.createMeet(
                title = title,
                description = description,
                locationName = locationName,
                latitude = latitude,
                longitude = longitude,
                scheduledAt = scheduledAt,
                timeLabel = timeLabel,
                purpose = purpose,
                maxParticipants = maxParticipants,
                hostUserId = uid,
                hostUserType = hostUserType,
                brewingType = brewingType,
                businessOffer = businessOffer
            )
            createMeetDebugTrace = "repository_create_meet_success:eventId=$eventId"
            AnalyticsProvider.tracker.logEvent(
                AnalyticsEvents.MEET_CREATED,
                mapOf(
                    AnalyticsParams.EVENT_ID to eventId,
                    AnalyticsParams.PURPOSE to purpose.trim().ifBlank { "unknown" },
                    AnalyticsParams.HAS_LOCATION to AnalyticsProvider.boolFlag(locationName.trim().isNotBlank()),
                    AnalyticsParams.HAS_BREWING_TYPE to AnalyticsProvider.boolFlag(!brewingType.isNullOrBlank()),
                    AnalyticsParams.MAX_PARTICIPANTS to maxParticipants.toString(),
                    AnalyticsParams.TIME_BUCKET to resolveMeetTimeBucket(scheduledAt)
                )
            )
            runCatching {
                EventMembershipRepository.recordEventCreated(
                    userId = uid,
                    eventId = eventId
                )
            }
            runCatching { UserTasteProfileRepository.onEventCreated(purpose) }
            runCatching { refreshEntitlementsInternal() }
            createMeetDebugTrace = "success"
            MeetCreateAttemptResult.Success
        } catch (error: Throwable) {
            createMeetDebugTrace = "repository_create_meet_failed:${error.javaClass.simpleName}:${error.message}"
            Log.e(TAG, "createMeet failed at repository stage", error)
            trackMeetCreateFailed(MeetCreateFailureReason.STORE_ERROR)
            MeetCreateAttemptResult.Failure(MeetCreateFailureReason.STORE_ERROR)
        } finally {
            createInFlight = false
        }
    }

    fun updateMeet(
        meetId: String,
        title: String,
        description: String,
        locationName: String,
        latitude: Double,
        longitude: Double,
        scheduledAt: Long,
        timeLabel: String,
        purpose: String,
        maxParticipants: Int,
        brewingType: String?,
        businessOffer: BusinessOffer?
    ) {
        viewModelScope.launch {
            MeetRepository.updateMeet(
                meetId = meetId,
                title = title,
                description = description,
                locationName = locationName,
                latitude = latitude,
                longitude = longitude,
                scheduledAt = scheduledAt,
                timeLabel = timeLabel,
                purpose = purpose,
                maxParticipants = maxParticipants,
                brewingType = brewingType,
                businessOffer = businessOffer
            )
        }
    }

    fun discoverySections(selectedMood: MeetMood): List<MeetDiscoverySection> =
        MeetDiscoveryEngine.buildSections(
            events = meets,
            selectedMood = selectedMood,
            userLatitude = userLatitude,
            userLongitude = userLongitude
        )

    fun exploreEvents(
        selectedMood: MeetMood,
        moodFilter: MeetMood?,
        sort: MeetExploreSort
    ): List<CoffeeMeet> =
        MeetDiscoveryEngine.exploreEvents(
            events = meets,
            selectedMood = selectedMood,
            moodFilter = moodFilter,
            sort = sort,
            userLatitude = userLatitude,
            userLongitude = userLongitude
        )

    suspend fun discoverCoffeeBuddies(selectedMood: MeetMood): CoffeeBuddyDiscoveryResult {
        val users = FirestoreUsersRepository
            .listDiscoverable(limit = 50)
            .getOrDefault(emptyList())

        val candidates = users
            .filter { it.id != currentUserId }
            .map {
                CoffeeBuddyCandidate(
                    userId = it.id,
                    displayName = it.displayName,
                    avatarUrl = it.photoUrl,
                    cityOrArea = null,
                    score = 0,
                    sharedSignals = emptyList(),
                    socialActivityCount = 0,
                    eventId = null
                )
            }

        return CoffeeBuddyDiscoveryResult(
            mood = selectedMood,
            discoverableUserCount = candidates.size,
            candidates = candidates,
            profileReady = true
        )
    }

    suspend fun sendCoffeeBuddyInvite(
        recipientUserId: String,
        placeName: String,
        inviteDate: Long,
        startTime: Long,
        endTime: Long,
        message: String = DEFAULT_COFFEE_INVITE_MESSAGE,
        timeOption: String = "unknown",
        coffeePreference: String = "unknown",
        source: String = "unknown"
    ): Result<Boolean> = runCatching {
        val senderId = currentUserId.trim()
        val recipientId = recipientUserId.trim()
        val normalizedPlaceName = placeName.trim()
        val normalizedMessage = message.trim().ifBlank { DEFAULT_COFFEE_INVITE_MESSAGE }
        val normalizedSource = AnalyticsProvider.normalizeSource(source)
        val inviteId = resolveInviteId(senderId, recipientId)

        Log.e("INVITE_DEBUG", "sendCoffeeBuddyInvite start sender=$senderId recipient=$recipientId")

        if (senderId.isBlank() || senderId == "guest") {
            throw IllegalStateException("not_signed_in")
        }
        if (recipientId.isBlank() || recipientId == senderId) {
            throw IllegalArgumentException("invalid_recipient")
        }
        if (normalizedPlaceName.isBlank()) {
            throw IllegalArgumentException("invalid_place")
        }
        if (inviteDate <= 0L || startTime <= 0L || endTime <= 0L || endTime <= startTime) {
            throw IllegalArgumentException("invalid_time_window")
        }

        val result = FirestoreCoffeeBuddyInvitesRepository
            .sendInvite(
                senderUserId = senderId,
                recipientUserId = recipientId,
                placeName = normalizedPlaceName,
                inviteDate = inviteDate,
                startTime = startTime,
                endTime = endTime,
                message = normalizedMessage
            )
            .getOrThrow()

        Log.e("INVITE_DEBUG", "sendCoffeeBuddyInvite success result=$result")
        if (result) {
            AnalyticsProvider.tracker.logEvent(
                AnalyticsEvents.INVITE_SENT,
                mapOf(
                    AnalyticsParams.INVITE_ID to inviteId,
                    AnalyticsParams.TARGET_USER_ID to recipientId,
                    AnalyticsParams.TIME_OPTION to normalizeInviteTimeOption(timeOption),
                    AnalyticsParams.HAS_NOTE to AnalyticsProvider.boolFlag(normalizedMessage != DEFAULT_COFFEE_INVITE_MESSAGE),
                    AnalyticsParams.HAS_LOCATION to AnalyticsProvider.boolFlag(normalizedPlaceName.isNotBlank()),
                    AnalyticsParams.COFFEE_PREF to normalizeCoffeePreference(coffeePreference),
                    AnalyticsParams.SOURCE to normalizedSource
                )
            )
        } else {
            AnalyticsProvider.tracker.logEvent(
                AnalyticsEvents.INVITE_SEND_FAILED,
                mapOf(
                    AnalyticsParams.REASON to "already_pending",
                    AnalyticsParams.SOURCE to normalizedSource
                )
            )
        }
        result
    }.onFailure { e ->
        Log.e("INVITE_DEBUG", "sendCoffeeBuddyInvite failed -> ${e.message}", e)
        AnalyticsProvider.tracker.logEvent(
            AnalyticsEvents.INVITE_SEND_FAILED,
            mapOf(
                AnalyticsParams.REASON to AnalyticsProvider.normalizeFailureReason(e.message),
                AnalyticsParams.SOURCE to AnalyticsProvider.normalizeSource(source)
            )
        )
    }

    suspend fun incomingCoffeeBuddyInvites(limit: Long = 50): List<CoffeeBuddyInviteItem> {
        val currentUid = currentUserId.trim()
        if (currentUid.isBlank() || currentUid == "guest") return emptyList()
        val now = System.currentTimeMillis()

        val invites = FirestoreCoffeeBuddyInvitesRepository
            .listIncoming(recipientUserId = currentUid, limit = limit)
            .getOrElse { error ->
                Log.e(TAG, "incomingCoffeeBuddyInvites failed", error)
                emptyList()
            }
            .filter { it.status.equals("pending", ignoreCase = true) && !isInviteExpired(it, now) }

        return mapIncomingInviteItems(invites)
    }

    fun observeIncomingCoffeeBuddyInvites(limit: Long = 50): Flow<List<CoffeeBuddyInviteItem>> {
        val currentUid = currentUserId.trim()
        if (currentUid.isBlank() || currentUid == "guest") return flowOf(emptyList())

        return FirestoreCoffeeBuddyInvitesRepository
            .observeIncoming(recipientUserId = currentUid, limit = limit)
            .map { invites ->
                val now = System.currentTimeMillis()
                mapIncomingInviteItems(
                    invites.filter {
                        it.status.equals("pending", ignoreCase = true) &&
                            !isInviteExpired(it, now)
                    }
                )
            }
    }

    fun observeOutgoingCoffeeBuddyInvites(limit: Long = 50): Flow<List<CoffeeBuddyInviteItem>> {
        val currentUid = currentUserId.trim()
        if (currentUid.isBlank() || currentUid == "guest") return flowOf(emptyList())

        return FirestoreCoffeeBuddyInvitesRepository
            .observeOutgoing(senderUserId = currentUid, limit = limit)
            .map { invites ->
                val now = System.currentTimeMillis()
                mapOutgoingInviteItems(
                    invites.filter {
                        it.status.equals("pending", ignoreCase = true) &&
                            !isInviteExpired(it, now)
                    }
                )
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeAcceptedCoffeeBuddyInvites(limit: Long = 50): Flow<List<CoffeeBuddyInviteItem>> {
        val currentUid = currentUserId.trim()
        if (currentUid.isBlank() || currentUid == "guest") return flowOf(emptyList())

        val incomingAccepted = FirestoreCoffeeBuddyInvitesRepository
            .observeIncoming(recipientUserId = currentUid, limit = limit)
            .map { invites ->
                val now = System.currentTimeMillis()
                mapIncomingInviteItems(
                    invites.filter {
                        it.status.equals("accepted", ignoreCase = true) &&
                            !isInviteExpired(it, now)
                    }
                )
            }

        val outgoingAccepted = FirestoreCoffeeBuddyInvitesRepository
            .observeOutgoing(senderUserId = currentUid, limit = limit)
            .map { invites ->
                val now = System.currentTimeMillis()
                mapOutgoingInviteItems(
                    invites.filter {
                        it.status.equals("accepted", ignoreCase = true) &&
                            !isInviteExpired(it, now)
                    }
                )
            }

        val acceptedInvites = combine(incomingAccepted, outgoingAccepted) { incoming, outgoing ->
            (incoming + outgoing)
                .distinctBy { it.id }
        }

        val inviteSummaries = acceptedInvites
            .flatMapLatest { invites ->
                FirestoreCoffeeChatsRepository.observeChatSummariesByInviteIds(
                    inviteIds = invites.map { it.id }
                )
            }

        return combine(acceptedInvites, inviteSummaries) { invites, summaries ->
            invites
                .map { invite ->
                    val summary = summaries[invite.id]
                    if (summary == null) {
                        invite
                    } else {
                        invite.copy(
                            lastMessage = summary.lastMessage,
                            lastMessageAt = summary.lastMessageAt
                        )
                    }
                }
                .sortedByDescending { maxOf(it.lastMessageAt, it.createdAt) }
        }
    }

    suspend fun openCoffeeBuddyChat(
        inviteId: String,
        source: String = "unknown",
        eventId: String? = null
    ): Result<String> = runCatching {
        val currentUid = currentUserId.trim()
        Log.d(
            "COFFEE_CHAT_DEBUG",
            "openCoffeeBuddyChat called inviteId=${inviteId.trim()} currentUserId=$currentUid"
        )
        if (currentUid.isBlank() || currentUid == "guest") {
            throw IllegalStateException("not_signed_in")
        }
        val chatId = FirestoreCoffeeChatsRepository
            .ensureChatForAcceptedInvite(
                inviteId = inviteId,
                requesterUserId = currentUid
            )
            .getOrThrow()
        Log.d(
            "COFFEE_CHAT_DEBUG",
            "openCoffeeBuddyChat success inviteId=${inviteId.trim()} chatId=$chatId"
        )
        trackChatOpened(
            chatId = chatId,
            inviteId = inviteId,
            eventId = eventId,
            source = source
        )
        chatId
    }.onFailure { error ->
        Log.e(
            "COFFEE_CHAT_DEBUG",
            "openCoffeeBuddyChat failed inviteId=${inviteId.trim()} message=${error.message}",
            error
        )
    }

    fun trackChatOpened(
        chatId: String,
        inviteId: String?,
        eventId: String?,
        source: String?
    ) {
        AnalyticsProvider.tracker.logEvent(
            AnalyticsEvents.CHAT_OPENED,
            mapOf(
                AnalyticsParams.CHAT_ID to chatId.trim().ifBlank { "unknown" },
                AnalyticsParams.INVITE_ID to (inviteId?.trim()?.ifBlank { "unknown" } ?: "unknown"),
                AnalyticsParams.EVENT_ID to (eventId?.trim()?.ifBlank { "none" } ?: "none"),
                AnalyticsParams.SOURCE to AnalyticsProvider.normalizeSource(source)
            )
        )
    }

    fun observeCoffeeBuddyChatMessages(chatId: String): Flow<List<CoffeeBuddyChatMessageItem>> {
        if (chatId.isBlank()) return flowOf(emptyList())
        return FirestoreCoffeeChatsRepository
            .observeMessages(chatId)
            .map { messages ->
                messages.map { message ->
                    CoffeeBuddyChatMessageItem(
                        id = message.id,
                        senderUserId = message.senderUserId,
                        text = message.text,
                        createdAt = message.createdAt
                    )
                }
            }
    }

    suspend fun sendCoffeeBuddyChatMessage(
        chatId: String,
        text: String,
        inviteId: String? = null,
        eventId: String? = null,
        messageSource: String = "unknown"
    ): Result<Unit> = runCatching {
        val currentUid = currentUserId.trim()
        if (currentUid.isBlank() || currentUid == "guest") {
            throw IllegalStateException("not_signed_in")
        }
        val normalizedText = text.trim()
        FirestoreCoffeeChatsRepository
            .sendMessage(
                chatId = chatId,
                senderUserId = currentUid,
                text = normalizedText
            )
            .getOrThrow()
        val analyticsMessageSource = normalizeMessageSource(messageSource)
        if (analyticsMessageSource != null) {
            AnalyticsProvider.tracker.logEvent(
                AnalyticsEvents.MESSAGE_SENT,
                mapOf(
                    AnalyticsParams.CHAT_ID to chatId.trim().ifBlank { "unknown" },
                    AnalyticsParams.INVITE_ID to (inviteId?.trim()?.ifBlank { "unknown" } ?: "unknown"),
                    AnalyticsParams.EVENT_ID to (eventId?.trim()?.ifBlank { "unknown" } ?: "unknown"),
                    AnalyticsParams.MESSAGE_SOURCE to analyticsMessageSource,
                    AnalyticsParams.MESSAGE_LENGTH_BUCKET to messageLengthBucket(normalizedText.length)
                )
            )
        }
    }.onFailure { error ->
        AnalyticsProvider.tracker.logEvent(
            AnalyticsEvents.MESSAGE_SEND_FAILED,
            mapOf(
                AnalyticsParams.CHAT_ID to chatId.trim().ifBlank { "unknown" },
                AnalyticsParams.REASON to AnalyticsProvider.normalizeFailureReason(error.message)
            )
        )
    }

    private suspend fun mapIncomingInviteItems(
        invites: List<FirestoreCoffeeBuddyInvite>
    ): List<CoffeeBuddyInviteItem> {
        if (invites.isEmpty()) return emptyList()

        val senderIds = invites
            .map { it.senderUserId.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val usersById = FirestoreUsersRepository
            .listByIds(senderIds)
            .getOrElse { emptyList() }
            .associateBy { it.id }

        return invites.map { invite ->
            val senderUser = usersById[invite.senderUserId]
            val senderDisplayName = senderUser?.displayName
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: senderUser?.email
                    ?.substringBefore("@")
                    ?.trim()
                    ?.takeIf { it.length >= 2 }
                ?: invite.senderUserId.takeLast(6)

            CoffeeBuddyInviteItem(
                id = invite.id,
                senderUserId = invite.senderUserId,
                senderDisplayName = senderDisplayName,
                senderAvatarUrl = senderUser?.photoUrl?.trim()?.takeIf { it.isNotBlank() },
                placeName = invite.placeName,
                inviteDate = invite.inviteDate,
                startTime = invite.startTime,
                endTime = invite.endTime,
                message = invite.message.ifBlank { DEFAULT_COFFEE_INVITE_MESSAGE },
                status = invite.status,
                createdAt = invite.createdAt
            )
        }
    }

    private suspend fun mapOutgoingInviteItems(
        invites: List<FirestoreCoffeeBuddyInvite>
    ): List<CoffeeBuddyInviteItem> {
        if (invites.isEmpty()) return emptyList()

        val recipientIds = invites
            .map { it.recipientUserId.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val usersById = FirestoreUsersRepository
            .listByIds(recipientIds)
            .getOrElse { emptyList() }
            .associateBy { it.id }

        return invites.map { invite ->
            val recipientUser = usersById[invite.recipientUserId]
            val recipientDisplayName = recipientUser?.displayName
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: recipientUser?.email
                    ?.substringBefore("@")
                    ?.trim()
                    ?.takeIf { it.length >= 2 }
                ?: invite.recipientUserId.takeLast(6)

            CoffeeBuddyInviteItem(
                id = invite.id,
                senderUserId = invite.recipientUserId,
                senderDisplayName = recipientDisplayName,
                senderAvatarUrl = recipientUser?.photoUrl?.trim()?.takeIf { it.isNotBlank() },
                placeName = invite.placeName,
                inviteDate = invite.inviteDate,
                startTime = invite.startTime,
                endTime = invite.endTime,
                message = invite.message.ifBlank { DEFAULT_COFFEE_INVITE_MESSAGE },
                status = invite.status,
                createdAt = invite.createdAt
            )
        }
    }

    suspend fun respondCoffeeBuddyInvite(
        inviteId: String,
        accept: Boolean
    ): Result<Unit> = runCatching {
        val normalizedId = inviteId.trim()
        if (normalizedId.isBlank()) throw IllegalArgumentException("invalid_invite_id")
        FirestoreCoffeeBuddyInvitesRepository
            .updateStatus(
                inviteId = normalizedId,
                status = if (accept) "accepted" else "declined"
            )
            .getOrThrow()
        if (accept) {
            val resolvedEventId = FirestoreCoffeeBuddyInvitesRepository
                .resolveLinkedEventId(normalizedId)
                .getOrNull()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: "none"
            AnalyticsProvider.tracker.logEvent(
                AnalyticsEvents.INVITE_ACCEPTED,
                mapOf(
                    AnalyticsParams.INVITE_ID to normalizedId,
                    AnalyticsParams.EVENT_ID to resolvedEventId,
                    AnalyticsParams.ACTOR_USER_ID to currentUserId.trim().ifBlank { "unknown" }
                )
            )
        }
    }.onFailure { error ->
        if (accept) {
            AnalyticsProvider.tracker.logEvent(
                AnalyticsEvents.INVITE_ACCEPT_FAILED,
                mapOf(
                    AnalyticsParams.INVITE_ID to inviteId.trim().ifBlank { "unknown" },
                    AnalyticsParams.REASON to AnalyticsProvider.normalizeFailureReason(error.message)
                )
            )
        }
    }

    fun nearbyBoostEvents(limit: Int = 4): List<CoffeeMeet> {
        if (meets.isEmpty()) return emptyList()
        val distanceOrdered = MeetDiscoveryEngine.exploreEvents(
            events = meets,
            selectedMood = MeetMood.CHILL,
            moodFilter = null,
            sort = MeetExploreSort.DISTANCE,
            userLatitude = userLatitude,
            userLongitude = userLongitude
        )

        val cityHint = MeetRepository.currentUserCityOrAreaHint()
            ?.let(::normalizeLocationToken)
            ?.takeIf { it.isNotBlank() }

        val cityBoosted = if (cityHint == null) {
            emptyList()
        } else {
            distanceOrdered.filter { event ->
                normalizeLocationToken(event.locationName).contains(cityHint)
            }
        }

        val primary = if (cityBoosted.isNotEmpty()) cityBoosted else distanceOrdered
        if (primary.isNotEmpty()) {
            return primary.distinctBy { it.id }.take(limit)
        }

        return meets
            .sortedWith(
                compareByDescending<CoffeeMeet> { it.participants.size }
                    .thenByDescending { it.scheduledAt }
            )
            .take(limit)
    }

    private fun normalizeLocationToken(raw: String): String {
        return raw
            .trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
    }

    private fun isInviteExpired(
        invite: FirestoreCoffeeBuddyInvite,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val resolvedEndTime = when {
            invite.endTime > 0L -> invite.endTime
            invite.startTime > 0L -> invite.startTime + 2 * 60 * 60 * 1000L
            invite.inviteDate > 0L -> invite.inviteDate + 24 * 60 * 60 * 1000L
            else -> Long.MAX_VALUE
        }
        return resolvedEndTime < nowMillis
    }

    private fun trackMeetCreateFailed(reason: MeetCreateFailureReason) {
        AnalyticsProvider.tracker.logEvent(
            AnalyticsEvents.MEET_CREATE_FAILED,
            mapOf(AnalyticsParams.REASON to normalizeMeetCreateFailureReason(reason))
        )
    }

    private fun trackEventJoinFailed(eventId: String, reason: MeetJoinFailureReason) {
        AnalyticsProvider.tracker.logEvent(
            AnalyticsEvents.EVENT_JOIN_FAILED,
            mapOf(
                AnalyticsParams.EVENT_ID to eventId.trim().ifBlank { "unknown" },
                AnalyticsParams.REASON to normalizeMeetJoinFailureReason(reason)
            )
        )
    }

    private fun normalizeMeetCreateFailureReason(reason: MeetCreateFailureReason): String = when (reason) {
        MeetCreateFailureReason.NOT_SIGNED_IN -> "not_signed_in"
        MeetCreateFailureReason.MONTHLY_LIMIT_REACHED -> "limit_reached"
        MeetCreateFailureReason.ATTENDEE_LIMIT_EXCEEDED -> "limit_reached"
        MeetCreateFailureReason.INVALID_CAPACITY -> "invalid_input"
        MeetCreateFailureReason.IN_PROGRESS -> "in_progress"
        MeetCreateFailureReason.STORE_ERROR -> "unknown"
    }

    private fun normalizeMeetJoinFailureReason(reason: MeetJoinFailureReason): String = when (reason) {
        MeetJoinFailureReason.NOT_SIGNED_IN -> "not_signed_in"
        MeetJoinFailureReason.EVENT_NOT_FOUND -> "not_found"
        MeetJoinFailureReason.ALREADY_JOINED -> "already_joined"
        MeetJoinFailureReason.EVENT_FULL -> "event_full"
        MeetJoinFailureReason.MONTHLY_LIMIT_REACHED -> "limit_reached"
        MeetJoinFailureReason.IN_PROGRESS -> "in_progress"
        MeetJoinFailureReason.STORE_ERROR -> "unknown"
    }

    private fun resolveMeetTimeBucket(scheduledAt: Long): String {
        if (scheduledAt <= 0L) return "unknown"
        val now = System.currentTimeMillis()
        val zoneId = ZoneId.systemDefault()
        val eventDate = Instant.ofEpochMilli(scheduledAt).atZone(zoneId).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
        return when {
            scheduledAt <= now + 60 * 60 * 1000L -> "now"
            eventDate == today -> "today"
            eventDate.isAfter(today) -> "future"
            else -> "now"
        }
    }

    private fun resolveInviteId(senderUserId: String, recipientUserId: String): String {
        val sender = senderUserId.trim().replace("/", "_")
        val recipient = recipientUserId.trim().replace("/", "_")
        return "coffee_invite_${sender}_${recipient}"
    }

    private fun normalizeInviteTimeOption(raw: String): String {
        val normalized = raw.trim().lowercase(Locale.ROOT)
        return when (normalized) {
            "now", "bugun_aksam", "today_evening", "tomorrow", "yarin", "custom", "zaman_sec" -> normalized
            else -> "unknown"
        }
    }

    private fun normalizeCoffeePreference(raw: String): String {
        val normalized = raw.trim().lowercase(Locale.ROOT)
        return when (normalized) {
            "fark etmez", "farketmez", "any" -> "any"
            "sade", "black" -> "black"
            "sutlu", "sütlü", "milky" -> "milky"
            "yogun", "yoğun", "strong" -> "strong"
            else -> "unknown"
        }
    }

    private fun normalizeMessageSource(raw: String): String? {
        val normalized = raw.trim().lowercase(Locale.ROOT)
        return when (normalized) {
            "typed", "quick_action" -> normalized
            else -> null
        }
    }

    private fun messageLengthBucket(length: Int): String = when {
        length <= 40 -> "short"
        length in 41..120 -> "medium"
        else -> "long"
    }

    fun refreshEntitlements() {
        viewModelScope.launch {
            refreshEntitlementsInternal()
        }
    }

    private fun observeAuthState() {
        if (authStateListener != null) return
        authStateListener = FirebaseAuthRepository.addAuthStateListener {
            refreshEntitlements()
        }
    }

    private suspend fun refreshEntitlementsInternal() {
        val uid = FirebaseAuthRepository.currentUser?.uid?.trim().orEmpty()
        if (uid.isBlank()) {
            val freeEntitlements = MembershipEntitlementResolver.resolve(MembershipPlan.FREE)
            currentPlan = MembershipPlan.FREE
            joinsUsed = 0
            joinLimit = freeEntitlements.monthlyJoinLimit
            createsUsed = 0
            createLimit = freeEntitlements.monthlyCreateLimit
            maxAttendees = freeEntitlements.maxAttendeesPerEvent
            canJoin = false
            canCreate = false
            entitlementLoaded = true
            return
        }
        runCatching {
            loadMembershipSnapshot(uid)
        }.onSuccess { snapshot ->
            applySnapshot(snapshot, allowActions = true)
        }.onFailure {
            val freeEntitlements = MembershipEntitlementResolver.resolve(MembershipPlan.FREE)
            currentPlan = MembershipPlan.FREE
            joinsUsed = 0
            joinLimit = freeEntitlements.monthlyJoinLimit
            createsUsed = 0
            createLimit = freeEntitlements.monthlyCreateLimit
            maxAttendees = freeEntitlements.maxAttendeesPerEvent
            canJoin = false
            canCreate = false
            entitlementLoaded = true
        }
    }

    private suspend fun loadMembershipSnapshot(userId: String): MembershipSnapshot {
        val plan = EventMembershipRepository.resolveCurrentPlan()
        val entitlements = MembershipEntitlementResolver.resolve(plan)
        val usage = EventMembershipRepository.loadMonthlyUsage(userId)
        val canJoinByPlan = entitlements.monthlyJoinLimit?.let { usage.joinsUsed < it } ?: true
        val canCreateByPlan = entitlements.monthlyCreateLimit?.let { usage.createsUsed < it } ?: true
        return MembershipSnapshot(
            plan = plan,
            entitlements = entitlements,
            joinsUsed = usage.joinsUsed,
            createsUsed = usage.createsUsed,
            canJoin = canJoinByPlan,
            canCreate = canCreateByPlan
        )
    }

    private fun applySnapshot(snapshot: MembershipSnapshot, allowActions: Boolean) {
        currentPlan = snapshot.plan
        joinsUsed = snapshot.joinsUsed
        joinLimit = snapshot.entitlements.monthlyJoinLimit
        createsUsed = snapshot.createsUsed
        createLimit = snapshot.entitlements.monthlyCreateLimit
        maxAttendees = snapshot.entitlements.maxAttendeesPerEvent
        canJoin = allowActions && snapshot.canJoin
        canCreate = allowActions && snapshot.canCreate
        entitlementLoaded = true
    }

    private fun resolvedMaxParticipants(meet: CoffeeMeet): Int {
        return meet.maxParticipants.takeIf { it > 0 } ?: MembershipEntitlementResolver
            .resolve(MembershipPlan.FREE)
            .maxAttendeesPerEvent
    }

    override fun onCleared() {
        authStateListener?.let(FirebaseAuthRepository::removeAuthStateListener)
        authStateListener = null
        super.onCleared()
    }

    private data class MembershipSnapshot(
        val plan: MembershipPlan,
        val entitlements: EventEntitlements,
        val joinsUsed: Int,
        val createsUsed: Int,
        val canJoin: Boolean,
        val canCreate: Boolean
    )
}