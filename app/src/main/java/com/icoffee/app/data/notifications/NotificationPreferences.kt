package com.icoffee.app.data.notifications

data class NotificationPreferences(
    val notificationsEnabled: Boolean = true,
    val meetParticipants: Boolean = true,
    val meetReminders: Boolean = true,
    val meetUpdates: Boolean = true,
    val nearbyMeet: Boolean = true,
    val recommendations: Boolean = true,
    val campaigns: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "notificationsEnabled" to notificationsEnabled,
        "meetParticipants" to meetParticipants,
        "meetReminders" to meetReminders,
        "meetUpdates" to meetUpdates,
        "nearbyMeet" to nearbyMeet,
        "recommendations" to recommendations,
        "campaigns" to campaigns,
        "updatedAt" to updatedAt
    )

    companion object {
        fun default(): NotificationPreferences = NotificationPreferences()

        fun fromMap(map: Map<String, Any>?): NotificationPreferences {
            if (map == null) return default()
            return NotificationPreferences(
                notificationsEnabled = map["notificationsEnabled"] as? Boolean ?: true,
                meetParticipants = map["meetParticipants"] as? Boolean ?: true,
                meetReminders = map["meetReminders"] as? Boolean ?: true,
                meetUpdates = map["meetUpdates"] as? Boolean ?: true,
                nearbyMeet = map["nearbyMeet"] as? Boolean ?: true,
                recommendations = map["recommendations"] as? Boolean ?: true,
                campaigns = map["campaigns"] as? Boolean ?: false,
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

