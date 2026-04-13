package com.icoffee.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.icoffee.app.data.CoffeeFriendDiscoveryEngine
import com.icoffee.app.data.MeetDiscoveryEngine
import com.icoffee.app.data.MeetRepository
import com.icoffee.app.data.auth.FirebaseAuthRepository
import com.icoffee.app.data.location.LocationProvider
import com.icoffee.app.data.membership.EventMembershipRepository
import com.icoffee.app.data.membership.EventEntitlements
import com.icoffee.app.data.membership.MembershipEntitlementResolver
import com.icoffee.app.data.membership.MembershipPlan
import com.icoffee.app.data.model.BusinessOffer
import com.icoffee.app.data.model.CoffeeBuddyDiscoveryResult
import com.icoffee.app.data.model.CoffeeMeet
import com.icoffee.app.data.model.MeetDiscoverySection
import com.icoffee.app.data.model.MeetExploreSort
import com.icoffee.app.data.model.MeetMood
import com.icoffee.app.data.model.UserType
import com.icoffee.app.data.profile.UserTasteProfileRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale

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

    val currentUserId: String
        get() = FirebaseAuthRepository.currentUser?.uid ?: "guest"

    val currentUserType: UserType
        get() = UserType.NORMAL

    var meets by mutableStateOf<List<CoffeeMeet>>(emptyList())
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

    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private val joinInFlightIds = mutableSetOf<String>()
    private var createInFlight by mutableStateOf(false)

    init {
        viewModelScope.launch {
            MeetRepository.eventsFlow.collect { list ->
                meets = list
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
        if (uid.isBlank()) return MeetJoinAttemptResult.Failure(MeetJoinFailureReason.NOT_SIGNED_IN)
        val meet = meets.firstOrNull { it.id == meetId }
            ?: return MeetJoinAttemptResult.Failure(MeetJoinFailureReason.EVENT_NOT_FOUND)
        if (uid in meet.participants) return MeetJoinAttemptResult.Failure(MeetJoinFailureReason.ALREADY_JOINED)
        if (isFull(meet)) return MeetJoinAttemptResult.Failure(MeetJoinFailureReason.EVENT_FULL)
        if (joinInFlightIds.contains(meetId)) return MeetJoinAttemptResult.Failure(MeetJoinFailureReason.IN_PROGRESS)

        val snapshot = try {
            loadMembershipSnapshot(uid)
        } catch (_: Throwable) {
            return MeetJoinAttemptResult.Failure(MeetJoinFailureReason.STORE_ERROR)
        }
        applySnapshot(snapshot, allowActions = true)
        if (!snapshot.canJoin) {
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
            MeetJoinAttemptResult.Success
        } catch (_: Throwable) {
            MeetJoinAttemptResult.Failure(MeetJoinFailureReason.STORE_ERROR)
        } finally {
            joinInFlightIds -= meetId
        }
    }

    fun leaveMeet(meetId: String): Boolean {
        val meet = meets.firstOrNull { it.id == meetId } ?: return false
        val uid = currentUserId
        if (uid !in meet.participants) return false
        viewModelScope.launch {
            MeetRepository.replaceParticipants(meetId, meet.participants - uid)
        }
        return true
    }

    fun cancelMeet(meetId: String): Boolean {
        val meet = meets.firstOrNull { it.id == meetId } ?: return false
        if (meet.hostId != currentUserId) return false
        viewModelScope.launch {
            MeetRepository.cancelMeet(meetId)
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
        businessOffer: BusinessOffer?
    ): MeetCreateAttemptResult {
        val uid = FirebaseAuthRepository.currentUser?.uid?.trim().orEmpty()
        if (uid.isBlank()) return MeetCreateAttemptResult.Failure(MeetCreateFailureReason.NOT_SIGNED_IN)
        if (createInFlight) return MeetCreateAttemptResult.Failure(MeetCreateFailureReason.IN_PROGRESS)

        val snapshot = try {
            loadMembershipSnapshot(uid)
        } catch (_: Throwable) {
            return MeetCreateAttemptResult.Failure(MeetCreateFailureReason.STORE_ERROR)
        }
        applySnapshot(snapshot, allowActions = true)
        if (!snapshot.canCreate) {
            return MeetCreateAttemptResult.Failure(MeetCreateFailureReason.MONTHLY_LIMIT_REACHED)
        }
        if (maxParticipants < 2) {
            return MeetCreateAttemptResult.Failure(MeetCreateFailureReason.INVALID_CAPACITY)
        }
        if (maxParticipants > snapshot.entitlements.maxAttendeesPerEvent) {
            return MeetCreateAttemptResult.Failure(MeetCreateFailureReason.ATTENDEE_LIMIT_EXCEEDED)
        }

        createInFlight = true
        return try {
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
            EventMembershipRepository.recordEventCreated(
                userId = uid,
                eventId = eventId
            )
            UserTasteProfileRepository.onEventCreated(purpose)
            refreshEntitlementsInternal()
            MeetCreateAttemptResult.Success
        } catch (_: Throwable) {
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

    fun discoverCoffeeBuddies(selectedMood: MeetMood): CoffeeBuddyDiscoveryResult {
        return CoffeeFriendDiscoveryEngine.discover(
            events = meets,
            currentUserId = currentUserId,
            selectedMood = selectedMood,
            currentTasteProfile = UserTasteProfileRepository.currentProfile()
        )
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
