package com.icoffee.app.data.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.icoffee.app.data.auth.FirebaseAuthRepository
import com.icoffee.app.data.firebase.FirebaseServiceLocator
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object NotificationSettingsRepository {
    private const val TAG = "FCM_DEBUG"

    private val ioScope = CoroutineScope(Dispatchers.IO)

    private fun settingsDocument(userId: String) = FirebaseServiceLocator.firestore
        .collection("users")
        .document(userId)
        .collection("private")
        .document("settings")

    private fun deviceTokensCollection(userId: String) = settingsDocument(userId)
        .collection("deviceTokens")

    fun observePreferences(userId: String): Flow<NotificationPreferences> = callbackFlow {
        if (userId.isBlank()) {
            trySend(NotificationPreferences.default())
            close()
            return@callbackFlow
        }

        val listener = settingsDocument(userId).addSnapshotListener { snapshot, _ ->
            val current = NotificationPreferences.fromMap(snapshot?.data)
            trySend(current)
            if (snapshot == null || !snapshot.exists()) {
                ioScope.launch { updatePreferences(userId, NotificationPreferences.default()) }
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun updatePreferences(
        userId: String,
        prefs: NotificationPreferences
    ): Result<Unit> = runCatching {
        if (userId.isBlank()) return@runCatching
        settingsDocument(userId)
            .set(prefs.copy(updatedAt = System.currentTimeMillis()).toMap())
            .await()
    }

    suspend fun upsertDeviceToken(
        userId: String,
        token: String
    ): Result<Unit> = runCatching {
        if (userId.isBlank() || token.isBlank()) return@runCatching
        val normalizedToken = token.trim()
        if (normalizedToken.isEmpty()) return@runCatching

        val tokenDocId = sha256(normalizedToken)
        val payload = mapOf(
            "token" to normalizedToken,
            "platform" to "android",
            "updatedAt" to System.currentTimeMillis()
        )
        deviceTokensCollection(userId)
            .document(tokenDocId)
            .set(payload)
            .await()
    }

    fun syncCurrentUserToken() {
        val uid = FirebaseAuthRepository.currentUser?.uid.orEmpty()
        if (uid.isBlank()) {
            Log.d(TAG, "syncCurrentUserToken skipped: no signed-in user")
            return
        }
        Log.d(TAG, "syncCurrentUserToken started for user=$uid")
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isNullOrBlank()) {
                    Log.d(TAG, "syncCurrentUserToken success but token was blank")
                    return@addOnSuccessListener
                }
                val tokenDocId = sha256(token)
                Log.d(TAG, "FCM token retrieved for user=$uid tokenDocId=$tokenDocId")
                ioScope.launch {
                    upsertDeviceToken(uid, token)
                        .onSuccess { Log.d(TAG, "FCM token stored for user=$uid tokenDocId=$tokenDocId") }
                        .onFailure { error ->
                            Log.e(
                                TAG,
                                "FCM token store failed for user=$uid tokenDocId=$tokenDocId message=${error.message}",
                                error
                            )
                        }
                }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "syncCurrentUserToken failed for user=$uid message=${error.message}", error)
            }
    }

    fun onNewToken(token: String) {
        val uid = FirebaseAuthRepository.currentUser?.uid.orEmpty()
        if (uid.isBlank()) {
            Log.d(TAG, "onNewToken skipped: no signed-in user")
            return
        }
        if (token.isBlank()) {
            Log.d(TAG, "onNewToken skipped: blank token")
            return
        }
        val tokenDocId = sha256(token)
        Log.d(TAG, "onNewToken received for user=$uid tokenDocId=$tokenDocId")
        ioScope.launch {
            upsertDeviceToken(uid, token)
                .onSuccess { Log.d(TAG, "onNewToken stored for user=$uid tokenDocId=$tokenDocId") }
                .onFailure { error ->
                    Log.e(
                        TAG,
                        "onNewToken store failed for user=$uid tokenDocId=$tokenDocId message=${error.message}",
                        error
                    )
                }
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { byte -> "%02x".format(byte) }
    }
}

object NotificationTokenSyncManager {
    private var initialized = false
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    fun initialize() {
        if (initialized) return
        initialized = true
        NotificationSettingsRepository.syncCurrentUserToken()
        authStateListener = FirebaseAuthRepository.addAuthStateListener { user ->
            if (user != null) {
                NotificationSettingsRepository.syncCurrentUserToken()
            }
        }
    }
}
