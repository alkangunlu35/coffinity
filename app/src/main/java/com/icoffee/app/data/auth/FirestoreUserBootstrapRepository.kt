package com.icoffee.app.data.auth

import com.google.firebase.auth.FirebaseUser
import com.icoffee.app.data.firebase.model.FirestoreUser
import com.icoffee.app.data.firebase.repository.FirestoreUsersRepository
import java.util.Locale

object FirestoreUserBootstrapRepository {

    private const val DEFAULT_DISPLAY_NAME = "Coffee Friend"

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

        val googleDisplayName = normalizeDisplayName(firebaseUser.displayName)
        val existingDisplayName = normalizeDisplayName(existingUser?.displayName)
        val emailDisplaySeed = normalizeEmailDisplaySeed(firebaseUser.email)
        val resolvedDisplayName = resolveDisplayName(
            googleDisplayName = googleDisplayName,
            existingDisplayName = existingDisplayName,
            emailDisplaySeed = emailDisplaySeed
        )

        val normalizedPhotoUrl = firebaseUser.photoUrl?.toString()?.trim()?.takeIf { it.isNotBlank() }
        val normalizedEmail = firebaseUser.email.orEmpty().trim().lowercase()

        if (existingUser == null) {
            val newUser = FirestoreUser(
                id = userId,
                displayName = resolvedDisplayName,
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
                displayName = resolvedDisplayName,
                email = normalizedEmail.ifBlank { existingUser.email }.trim().lowercase(),
                photoUrl = normalizedPhotoUrl ?: existingUser.photoUrl,
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

    private fun resolveDisplayName(
        googleDisplayName: String?,
        existingDisplayName: String?,
        emailDisplaySeed: String?
    ): String {
        if (!isWeakDisplayName(googleDisplayName)) return googleDisplayName!!
        if (!isWeakDisplayName(existingDisplayName)) return existingDisplayName!!
        if (!isWeakDisplayName(emailDisplaySeed)) return emailDisplaySeed!!
        return DEFAULT_DISPLAY_NAME
    }

    private fun normalizeDisplayName(raw: String?): String? {
        val normalized = raw
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?.takeIf { it.isNotBlank() }
        return normalized
    }

    private fun normalizeEmailDisplaySeed(rawEmail: String?): String? {
        val localPart = rawEmail
            ?.trim()
            ?.substringBefore("@")
            ?.replace(Regex("[._+\\-]+"), " ")
            ?.replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            .orEmpty()

        if (localPart.isBlank()) return null

        val readable = localPart
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                token.lowercase(Locale.ROOT)
                    .replaceFirstChar { first ->
                        if (first.isLowerCase()) first.titlecase(Locale.ROOT) else first.toString()
                    }
            }
            .trim()

        return readable.takeIf { !isWeakDisplayName(it) }
    }

    private fun isWeakDisplayName(raw: String?): Boolean {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return true

        val compact = value.replace(" ", "")
        if (compact.length <= 1) return true
        if (compact.matches(Regex("^[A-Za-z0-9_-]{20,}$"))) return true

        return false
    }
}
