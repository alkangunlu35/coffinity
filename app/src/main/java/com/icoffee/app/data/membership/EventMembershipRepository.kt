package com.icoffee.app.data.membership

import com.google.firebase.firestore.Query
import com.icoffee.app.data.auth.FirebaseAuthRepository
import com.icoffee.app.data.firebase.FirebaseServiceLocator
import com.icoffee.app.data.firebase.firestore.FirestoreCollections
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.tasks.await

private const val ACTION_CREATE = "create"
private const val ACTION_JOIN = "join"

data class MonthlyEventUsage(
    val joinsUsed: Int,
    val createsUsed: Int
)

object EventMembershipRepository {

    private val firestore
        get() = FirebaseServiceLocator.firestore

    private val usersCollection
        get() = firestore.collection(FirestoreCollections.USERS)

    private val usageCollection
        get() = firestore.collection(FirestoreCollections.EVENT_USAGE)

    suspend fun resolveCurrentPlan(): MembershipPlan {
        val uid = FirebaseAuthRepository.currentUser?.uid?.trim().orEmpty()
        if (uid.isBlank()) return MembershipPlan.FREE

        val snapshot = usersCollection.document(uid).get().await()

        val planValue = snapshot.getString("plan")
            ?: snapshot.getString("membershipPlan")

        return MembershipPlan.fromStorage(planValue)
    }

    suspend fun loadMonthlyUsage(
        userId: String,
        nowMillis: Long = System.currentTimeMillis()
    ): MonthlyEventUsage {

        val normalizedUserId = userId.trim()
        if (normalizedUserId.isBlank()) {
            return MonthlyEventUsage(0, 0)
        }

        val range = monthRange(nowMillis)

        val joins = queryUsageCount(
            normalizedUserId,
            ACTION_JOIN,
            range.startMillis,
            range.endMillisExclusive
        )

        val creates = queryUsageCount(
            normalizedUserId,
            ACTION_CREATE,
            range.startMillis,
            range.endMillisExclusive
        )

        return MonthlyEventUsage(
            joinsUsed = joins,
            createsUsed = creates
        )
    }

    suspend fun canJoinEvent(userId: String): Result<Unit> {
        return try {
            val plan = resolveCurrentPlan()
            val entitlements = MembershipEntitlementResolver.resolve(plan)

            val usage = loadMonthlyUsage(userId)

            val limit = entitlements.monthlyJoinLimit

            if (limit != null && usage.joinsUsed >= limit) {
                Result.failure(Exception("Aylık katılım limitine ulaştınız"))
            } else {
                Result.success(Unit)
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordEventJoined(
        userId: String,
        eventId: String,
        joinedAt: Long = System.currentTimeMillis()
    ) {
        val normalizedUserId = userId.trim()
        val normalizedEventId = eventId.trim()

        if (normalizedUserId.isBlank() || normalizedEventId.isBlank()) return

        val monthKey = monthKey(joinedAt)
        val docId = buildJoinDocId(normalizedUserId, normalizedEventId, monthKey)

        val docRef = usageCollection.document(docId)

        if (docRef.get().await().exists()) return

        val payload = mapOf(
            "id" to docId,
            "userId" to normalizedUserId,
            "eventId" to normalizedEventId,
            "actionType" to ACTION_JOIN,
            "monthKey" to monthKey,
            "createdAt" to joinedAt,
            "updatedAt" to System.currentTimeMillis()
        )

        docRef.set(payload).await()
    }

    suspend fun recordEventCreated(
        userId: String,
        eventId: String,
        createdAt: Long = System.currentTimeMillis()
    ) {
        val normalizedUserId = userId.trim()
        val normalizedEventId = eventId.trim()

        if (normalizedUserId.isBlank() || normalizedEventId.isBlank()) return

        val monthKey = monthKey(createdAt)
        val docId = "create_${monthKey}_${normalizedUserId}_${normalizedEventId}"

        val docRef = usageCollection.document(docId)

        if (docRef.get().await().exists()) return

        val payload = mapOf(
            "id" to docId,
            "userId" to normalizedUserId,
            "eventId" to normalizedEventId,
            "actionType" to ACTION_CREATE,
            "monthKey" to monthKey,
            "createdAt" to createdAt,
            "updatedAt" to System.currentTimeMillis()
        )

        docRef.set(payload).await()
    }

    private suspend fun queryUsageCount(
        userId: String,
        actionType: String,
        startMillis: Long,
        endMillisExclusive: Long
    ): Int {
        return usageCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("actionType", actionType)
            .whereGreaterThanOrEqualTo("createdAt", startMillis)
            .whereLessThan("createdAt", endMillisExclusive)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get()
            .await()
            .size()
    }

    private fun buildJoinDocId(userId: String, eventId: String, monthKey: String): String {
        return "join_${monthKey}_${userId}_${eventId}"
    }

    private fun monthKey(nowMillis: Long): String {
        val instant = Instant.ofEpochMilli(nowMillis)
        val dateTime = instant.atZone(ZoneId.systemDefault())
        val yearMonth = YearMonth.of(dateTime.year, dateTime.month)
        return "${yearMonth.year}-${yearMonth.monthValue.toString().padStart(2, '0')}"
    }

    private data class MonthRange(
        val startMillis: Long,
        val endMillisExclusive: Long
    )

    private fun monthRange(nowMillis: Long): MonthRange {
        val now = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault())

        val start = now.withDayOfMonth(1)
            .toLocalDate()
            .atStartOfDay(now.zone)
            .toInstant()
            .toEpochMilli()

        val nextMonthStart = now.withDayOfMonth(1)
            .plusMonths(1)
            .toLocalDate()
            .atStartOfDay(now.zone)
            .toInstant()
            .toEpochMilli()

        return MonthRange(startMillis = start, endMillisExclusive = nextMonthStart)
    }
}