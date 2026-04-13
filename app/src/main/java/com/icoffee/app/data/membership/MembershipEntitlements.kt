package com.icoffee.app.data.membership

enum class MembershipPlan(val storageValue: String) {
    FREE("free"),
    PREMIUM("premium"),
    BUSINESS("business");

    companion object {
        fun fromStorage(raw: String?): MembershipPlan {
            val normalized = raw?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.storageValue == normalized } ?: FREE
        }
    }
}

data class EventEntitlements(
    val monthlyJoinLimit: Int?,
    val monthlyCreateLimit: Int?,
    val maxAttendeesPerEvent: Int
)

object MembershipEntitlementResolver {
    fun resolve(plan: MembershipPlan): EventEntitlements = when (plan) {
        MembershipPlan.FREE -> EventEntitlements(
            monthlyJoinLimit = 4,
            monthlyCreateLimit = 1,
            maxAttendeesPerEvent = 10
        )

        MembershipPlan.PREMIUM -> EventEntitlements(
            monthlyJoinLimit = null,
            monthlyCreateLimit = 10,
            maxAttendeesPerEvent = 20
        )

        MembershipPlan.BUSINESS -> EventEntitlements(
            monthlyJoinLimit = null,
            monthlyCreateLimit = null,
            maxAttendeesPerEvent = 100
        )
    }
}
