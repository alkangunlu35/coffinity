package com.icoffee.app.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.icoffee.app.data.firebase.FirebaseServiceLocator
import kotlinx.coroutines.tasks.await

object FirebaseAuthRepository {

    const val WEB_CLIENT_ID =
        "736493185688-mib1g2a3v60eedp8b2akr7p0j69lhiq1.apps.googleusercontent.com"

    private val auth: FirebaseAuth
        get() = FirebaseServiceLocator.auth

    val currentUser: FirebaseUser? get() = auth.currentUser
    val isSignedIn: Boolean get() = currentUser != null

    suspend fun signUpWithEmail(
        fullName: String,
        email: String,
        password: String
    ): Result<FirebaseUser> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user!!
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(fullName.trim())
            .build()
        user.updateProfile(profileUpdates).await()
        user
    }

    suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<FirebaseUser> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        result.user!!
    }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<FirebaseUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = auth.signInWithCredential(credential).await()
        authResult.user!!
    }

    fun signOut() {
        auth.signOut()
    }

    fun addAuthStateListener(onChanged: (FirebaseUser?) -> Unit): FirebaseAuth.AuthStateListener {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            onChanged(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        return listener
    }

    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.removeAuthStateListener(listener)
    }
}
