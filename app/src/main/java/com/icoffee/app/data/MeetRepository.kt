package com.icoffee.app.data

import android.content.Context
import android.content.SharedPreferences
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

object MeetRepository {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private lateinit var prefs: SharedPreferences

    private val _events = MutableStateFlow<List<CoffeeMeet>>(emptyList())
    val eventsFlow: StateFlow<List<CoffeeMeet>> = _events.asStateFlow()

    fun initialize(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("meet_prefs", Context.MODE_PRIVATE)

        db.collection("events")
            .whereEqualTo("isDeleted", false)
            .orderBy("scheduledAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val eventList = snapshot.documents.mapNotNull { doc ->
                        try {
                            val participants = doc.get("participants") as? List<String> ?: emptyList()
                            CoffeeMeet(
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
                                isCreatedByUser = false // This will be set by the ViewModel if needed
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    _events.value = eventList
                }
            }
    }

    fun nearbyActivityCount(): Int {
        val allEvents = _events.value
        // Using default lat/lon from MeetDiscoveryEngine if real location not available
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

    fun createMeet(
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

        val docRef = runBlocking {
            com.google.android.gms.tasks.Tasks.await(db.collection("events").add(eventData))
        }
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

    fun cancelMeet(
        eventId: String
    ) {
        db.collection("events").document(eventId)
            .update("isDeleted", true, "status", "cancelled")
    }

    fun replaceParticipants(
        eventId: String,
        newParticipants: List<String>
    ) {
        db.collection("events").document(eventId)
            .update(
                "participants", newParticipants,
                "participantCount", newParticipants.size
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

            // Also keep sub-collection for legacy/query purposes if needed
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
                    } catch (_: Exception) {}
                }.start()
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }
}
