// FILE: app/src/main/java/com/icoffee/app/data/MeetRepository.kt
// FULL REPLACEMENT

package com.icoffee.app.data

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.icoffee.app.data.membership.EventMembershipRepository
import com.icoffee.app.data.model.BusinessOffer
import com.icoffee.app.data.model.CoffeeMeet
import com.icoffee.app.data.model.UserType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

data class MeetRawDebugEvent(
    val id: String,
    val title: String,
    val status: String,
    val isDeleted: Boolean,
    val hostId: String,
    val scheduledAt: Long
)

object MeetRepository {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private lateinit var prefs: SharedPreferences

    private val _events = MutableStateFlow<List<CoffeeMeet>>(emptyList())

    private data class SnapshotMeet(
        val meet: CoffeeMeet,
        val isDeleted: Boolean,
        val status: String
    )

    val eventsFlow: Flow<List<CoffeeMeet>> = callbackFlow {
        val listener = db.collection("events")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MEET_DEBUG", "Snapshot error", error)
                    return@addSnapshotListener
                }

                val events = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val participants = doc.get("participants") as? List<String> ?: emptyList()
                        SnapshotMeet(
                            meet = CoffeeMeet(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                description = doc.getString("description") ?: "",
                                locationName = doc.getString("locationName") ?: "",
                                latitude = doc.getDouble("latitude") ?: 0.0,
                                longitude = doc.getDouble("longitude") ?: 0.0,
                                scheduledAt = doc.getLong("scheduledAt") ?: 0L,
                                time = doc.getString("time") ?: "",
                                purpose = doc.getString("purpose") ?: "",
                                participants = participants,
                                maxParticipants = (doc.getLong("maxParticipants") ?: 10).toInt(),
                                hostId = doc.getString("hostId") ?: "",
                                hostUserType = doc.getString("hostUserType")?.let { UserType.valueOf(it) },
                                brewingType = doc.getString("brewingType"),
                                isCreatedByUser = false
                            ),
                            isDeleted = doc.getBoolean("isDeleted") ?: false,
                            status = doc.getString("status")?.trim()?.lowercase() ?: "active"
                        )
                    } catch (_: Exception) {
                        null
                    }
                } ?: emptyList()

                Log.d("MEET_DEBUG", "RAW SNAPSHOT size=${events.size}")
                val now = System.currentTimeMillis()

                val visibleEvents = events
                    .filter {
                        it.isDeleted != true &&
                            it.status == "active" &&
                            isEventCurrentOrFuture(it.meet.scheduledAt, now)
                    }
                    .map { it.meet }

                _events.value = visibleEvents
                trySend(visibleEvents)
            }

        awaitClose { listener.remove() }
    }

    private val _rawDebugEvents = MutableStateFlow<List<MeetRawDebugEvent>>(emptyList())
    val rawDebugEventsFlow: StateFlow<List<MeetRawDebugEvent>> = _rawDebugEvents.asStateFlow()

    fun initialize(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("meet_prefs", Context.MODE_PRIVATE)
        val isDebugBuild = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        if (isDebugBuild) {
            db.collection("events")
                .orderBy("scheduledAt", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        _rawDebugEvents.value = snapshot.documents.map { doc ->
                            MeetRawDebugEvent(
                                id = doc.id,
                                title = doc.getString("title").orEmpty(),
                                status = doc.getString("status")?.trim()?.lowercase() ?: "active",
                                isDeleted = doc.getBoolean("isDeleted") ?: false,
                                hostId = doc.getString("hostId").orEmpty(),
                                scheduledAt = doc.getLong("scheduledAt") ?: 0L
                            )
                        }
                    }
                }
        }
    }

    fun nearbyActivityCount(): Int {
        val allEvents = _events.value
        return MeetDiscoveryEngine.buildSections(
            events = allEvents,
            selectedMood = com.icoffee.app.data.model.MeetMood.CHILL
        ).sumOf { it.events.size }
    }

    fun getEventsAtVenue(venueId: String): List<CoffeeMeet> {
        return _events.value.filter { it.locationName == venueId }
    }

    fun currentUserCityOrAreaHint(): String? {
        return prefs.getString("user_city_hint", null)
    }

    private fun isEventCurrentOrFuture(
        scheduledAt: Long,
        nowMillis: Long
    ): Boolean {
        if (scheduledAt <= 0L) return true

        val eventEnd = scheduledAt + 2 * 60 * 60 * 1000L
        return eventEnd >= nowMillis
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
        hostUserId: String,
        hostUserType: UserType?,
        brewingType: String?,
        businessOffer: BusinessOffer?
    ): String {
        Log.d("MEET_DEBUG", "createMeet called")
        val eventData = mutableMapOf<String, Any?>(
            "title" to title,
            "description" to description,
            "locationName" to locationName,
            "latitude" to latitude,
            "longitude" to longitude,
            "scheduledAt" to scheduledAt,
            "time" to timeLabel,
            "purpose" to purpose,
            "participants" to listOf(hostUserId),
            "maxParticipants" to maxParticipants,
            "hostId" to hostUserId,
            "hostUserType" to hostUserType?.name,
            "brewingType" to brewingType,
            "isDeleted" to false,
            "status" to "active",
            "participantCount" to 1,
            "createdAt" to FieldValue.serverTimestamp()
        )

        businessOffer?.let { offer ->
            eventData["businessOffer"] = mapOf(
                "offerTitle" to offer.offerTitle,
                "offerDescription" to offer.offerDescription,
                "includedItems" to offer.includedItems.map {
                    mapOf("quantity" to it.quantity, "label" to it.label)
                },
                "priceAmount" to offer.priceAmount,
                "currency" to offer.currency,
                "paymentMode" to offer.paymentMode.name,
                "availabilityLimit" to offer.availabilityLimit,
                "termsNote" to offer.termsNote
            )
        }

        val docRef = db.collection("events").document()
        docRef.set(eventData).await()

        val verify = docRef.get().await()
        if (!verify.exists()) {
            throw Exception("CREATE_FAILED_NOT_PERSISTED")
        }

        Log.d("MEET_DEBUG", "Created eventId=${docRef.id}")
        return docRef.id
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
        val updates = mutableMapOf<String, Any?>(
            "title" to title,
            "description" to description,
            "locationName" to locationName,
            "latitude" to latitude,
            "longitude" to longitude,
            "scheduledAt" to scheduledAt,
            "time" to timeLabel,
            "purpose" to purpose,
            "maxParticipants" to maxParticipants,
            "brewingType" to brewingType
        )

        businessOffer?.let { offer ->
            updates["businessOffer"] = mapOf(
                "offerTitle" to offer.offerTitle,
                "offerDescription" to offer.offerDescription,
                "includedItems" to offer.includedItems.map {
                    mapOf("quantity" to it.quantity, "label" to it.label)
                },
                "priceAmount" to offer.priceAmount,
                "currency" to offer.currency,
                "paymentMode" to offer.paymentMode.name,
                "availabilityLimit" to offer.availabilityLimit,
                "termsNote" to offer.termsNote
            )
        } ?: run {
            updates["businessOffer"] = null
        }

        db.collection("events").document(meetId)
            .update(updates)
    }

    suspend fun cancelMeet(eventId: String): Result<Unit> = runCatching {
        Log.d("MEET_DEBUG", "cancelMeet called eventId=$eventId")
        val ref = db.collection("events").document(eventId)
        ref.update(
            mapOf(
                "isDeleted" to true,
                "status" to "cancelled"
            )
        ).await()

        val snapshot = ref.get().await()
        val deleted = snapshot.getBoolean("isDeleted")
        val status = snapshot.getString("status")

        if (deleted != true || status != "cancelled") {
            throw Exception("CANCEL_NOT_PERSISTED")
        }

        Log.d("MEET_DEBUG", "Cancel persisted for event=$eventId")
    }

    suspend fun replaceParticipants(
        eventId: String,
        newParticipants: List<String>
    ) {
        val normalizedParticipants = newParticipants
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val eventRef = db.collection("events").document(eventId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(eventRef)
            if (!snapshot.exists()) {
                throw Exception("EVENT_NOT_FOUND")
            }

            val isDeleted = snapshot.getBoolean("isDeleted") ?: false
            val status = snapshot.getString("status")?.trim()?.lowercase() ?: "active"

            if (isDeleted) throw Exception("EVENT_DELETED")
            if (status != "active") throw Exception("EVENT_NOT_ACTIVE")

            transaction.update(
                eventRef,
                mapOf(
                    "participants" to normalizedParticipants,
                    "participantCount" to normalizedParticipants.size
                )
            )
            null
        }.await()

        val verify = eventRef.get().await()
        val persistedParticipants = (verify.get("participants") as? List<*>)
            ?.mapNotNull { it as? String }
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?: emptyList()
        val persistedCount = (verify.getLong("participantCount") ?: 0L).toInt()

        if (persistedParticipants != normalizedParticipants || persistedCount != normalizedParticipants.size) {
            throw Exception("PARTICIPANTS_NOT_PERSISTED")
        }

        Log.d(
            "MEET_DEBUG",
            "replaceParticipants success eventId=$eventId participantCount=${normalizedParticipants.size}"
        )
    }

    fun joinEvent(
        eventId: String,
        userId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val eventRef = db.collection("events").document(eventId)
        val participantRef = eventRef
            .collection("participants")
            .document(userId)

        db.runTransaction { transaction ->
            val eventSnapshot = transaction.get(eventRef)

            if (!eventSnapshot.exists()) {
                throw Exception("Event bulunamadı")
            }

            val isDeleted = eventSnapshot.getBoolean("isDeleted") ?: false
            val status = eventSnapshot.getString("status") ?: "active"
            val capacity = (eventSnapshot.getLong("maxParticipants") ?: 0).toInt()
            val participants = eventSnapshot.get("participants") as? List<String> ?: emptyList()

            if (isDeleted) throw Exception("Event silinmiş")
            if (status != "active") throw Exception("Event aktif değil")
            if (participants.size >= capacity) throw Exception("Event dolu")

            if (participants.contains(userId)) {
                throw Exception("Zaten katıldınız")
            }

            val newParticipants = participants + userId

            transaction.update(
                eventRef,
                "participants", newParticipants,
                "participantCount", newParticipants.size
            )

            val participantData = hashMapOf(
                "userId" to userId,
                "joinedAt" to FieldValue.serverTimestamp(),
                "status" to "joined"
            )
            transaction.set(participantRef, participantData)

            null
        }
            .addOnSuccessListener {
                Thread {
                    try {
                        runBlocking {
                            EventMembershipRepository.recordEventJoined(userId, eventId)
                        }
                    } catch (_: Exception) {
                    }
                }.start()
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }
}