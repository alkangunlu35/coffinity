package com.icoffee.app.data.auth

import com.google.firebase.auth.FirebaseUser
import com.icoffee.app.data.firebase.model.FirestoreUser
import com.icoffee.app.data.firebase.repository.FirestoreUsersRepository

object FirestoreUserBootstrapRepository {

    suspend fun ensureUserDocument(
        firebaseUser: FirebaseUser,
        preferredLanguageCode: String?
    ): Result<FirestoreUser> = runCatching {
        val userId = firebaseUser.uid.trim()
        require(userId.isNotEmpty()) { "Authenticated user id is required." }

        val existingUser = FirestoreUsersRepository.getById(userId).getOrThrow()
        val now = System.currentTimeMillis()
        val safeLanguage = preferredLanguageCode
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "en"

        val displayNameSeed = firebaseUser.displayName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: firebaseUser.email
                ?.substringBefore("@")
                ?.trim()
                .orEmpty()

        val normalizedPhotoUrl = firebaseUser.photoUrl?.toString()?.trim()?.takeIf { it.isNotBlank() }
        val normalizedEmail = firebaseUser.email.orEmpty().trim().lowercase()

        if (existingUser == null) {
            val newUser = FirestoreUser(
                id = userId,
                displayName = displayNameSeed,
                email = normalizedEmail,
                photoUrl = normalizedPhotoUrl,
                city = "",
                country = "",
                language = safeLanguage,
                plan = "free",
                role = "user",
                managedBrandIds = emptyList(),
                discoverable = true,
                profileCompleted = false,
                createdAt = now,
                updatedAt = now
            )
            FirestoreUsersRepository.create(newUser).getOrThrow()
            newUser
        } else {
            val repairedUser = existingUser.copy(
                displayName = existingUser.displayName.ifBlank { displayNameSeed },
                email = existingUser.email.ifBlank { normalizedEmail }.trim().lowercase(),
                photoUrl = existingUser.photoUrl ?: normalizedPhotoUrl,
                language = existingUser.language.ifBlank { safeLanguage },
                plan = existingUser.plan.ifBlank { "free" },
                role = existingUser.role.ifBlank { "user" },
                managedBrandIds = existingUser.managedBrandIds
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct(),
                createdAt = existingUser.createdAt.takeIf { it > 0L } ?: now,
                updatedAt = now
            )
            FirestoreUsersRepository.update(repairedUser).getOrThrow()
            repairedUser
        }
    }
}
