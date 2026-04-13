package com.icoffee.app.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.icoffee.app.R
import com.icoffee.app.data.auth.FirebaseAuthRepository
import com.icoffee.app.data.auth.FirestoreUserBootstrapRepository
import com.icoffee.app.localization.AppLocaleManager
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    var isSignedIn by mutableStateOf(FirebaseAuthRepository.isSignedIn)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var lastBootstrappedUid: String? = null

    init {
        observeAuthState()
        FirebaseAuthRepository.currentUser?.let { user ->
            bootstrapUserDocumentInBackground(user)
        }
    }

    fun getGoogleSignInIntent(context: Context): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(FirebaseAuthRepository.WEB_CLIENT_ID)
            .requestEmail()
            .build()
        return buildGoogleSignInClient(context, gso).signInIntent
    }

    fun handleGoogleSignInResult(
        data: Intent?,
        context: Context,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken ?: throw IllegalStateException("missing_google_id_token")
                FirebaseAuthRepository.signInWithGoogleIdToken(idToken)
                    .onSuccess { user ->
                        completeAuthFlow(
                            user = user,
                            context = context,
                            onSuccess = onSuccess
                        )
                    }
                    .onFailure { e ->
                        errorMessage = friendlyMessage(context, e)
                    }
            } catch (e: ApiException) {
                errorMessage = friendlyMessage(context, e)
            } catch (e: Exception) {
                errorMessage = friendlyMessage(context, e)
            }
            isLoading = false
        }
    }

    fun signUp(
        fullName: String,
        email: String,
        password: String,
        context: Context,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            FirebaseAuthRepository.signUpWithEmail(fullName, email, password)
                .onSuccess { user ->
                    completeAuthFlow(
                        user = user,
                        context = context,
                        onSuccess = onSuccess
                    )
                }
                .onFailure { e ->
                    errorMessage = friendlyMessage(context, e)
                }
            isLoading = false
        }
    }

    fun signIn(
        email: String,
        password: String,
        context: Context,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            FirebaseAuthRepository.signInWithEmail(email, password)
                .onSuccess { user ->
                    completeAuthFlow(
                        user = user,
                        context = context,
                        onSuccess = onSuccess
                    )
                }
                .onFailure { e ->
                    errorMessage = friendlyMessage(context, e)
                }
            isLoading = false
        }
    }

    fun signOut() {
        FirebaseAuthRepository.signOut()
        isSignedIn = false
        lastBootstrappedUid = null
    }

    fun clearError() {
        errorMessage = null
    }

    private fun observeAuthState() {
        if (authStateListener != null) return
        authStateListener = FirebaseAuthRepository.addAuthStateListener { user ->
            isSignedIn = user != null
            if (user == null) {
                lastBootstrappedUid = null
            } else if (lastBootstrappedUid != user.uid) {
                bootstrapUserDocumentInBackground(user)
            }
        }
    }

    private fun bootstrapUserDocumentInBackground(user: FirebaseUser) {
        viewModelScope.launch {
            FirestoreUserBootstrapRepository
                .ensureUserDocument(user, currentLanguageCode())
                .onSuccess { lastBootstrappedUid = user.uid }
        }
    }

    private suspend fun completeAuthFlow(
        user: FirebaseUser,
        context: Context,
        onSuccess: () -> Unit
    ) {
        FirestoreUserBootstrapRepository
            .ensureUserDocument(user, currentLanguageCode())
            .onSuccess {
                lastBootstrappedUid = user.uid
                isSignedIn = true
                onSuccess()
            }
            .onFailure { throwable ->
                FirebaseAuthRepository.signOut()
                isSignedIn = false
                errorMessage = friendlyMessage(context, throwable)
            }
    }

    private fun buildGoogleSignInClient(
        context: Context,
        gso: GoogleSignInOptions
    ): GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    private fun currentLanguageCode(): String {
        return AppLocaleManager.currentLanguage(getApplication()).code
    }

    private fun friendlyMessage(context: Context, throwable: Throwable): String {
        return when (throwable) {
            is FirebaseAuthUserCollisionException ->
                context.getString(R.string.auth_error_email_in_use)

            is FirebaseAuthWeakPasswordException ->
                context.getString(R.string.auth_error_weak_password)

            is FirebaseAuthInvalidUserException ->
                context.getString(R.string.auth_error_user_not_found)

            is FirebaseAuthInvalidCredentialsException -> {
                when {
                    throwable.errorCode.equals("ERROR_INVALID_EMAIL", ignoreCase = true) ->
                        context.getString(R.string.auth_error_invalid_email)

                    throwable.errorCode.equals("ERROR_WRONG_PASSWORD", ignoreCase = true) ||
                        throwable.errorCode.equals("ERROR_INVALID_CREDENTIAL", ignoreCase = true) ->
                        context.getString(R.string.auth_error_wrong_password)

                    else -> context.getString(R.string.auth_error_generic)
                }
            }

            is FirebaseTooManyRequestsException ->
                context.getString(R.string.auth_error_too_many_requests)

            is FirebaseNetworkException ->
                context.getString(R.string.auth_error_network)

            is FirebaseAuthException -> {
                when (throwable.errorCode.uppercase()) {
                    "ERROR_INVALID_EMAIL" -> context.getString(R.string.auth_error_invalid_email)
                    "ERROR_EMAIL_ALREADY_IN_USE" -> context.getString(R.string.auth_error_email_in_use)
                    "ERROR_WEAK_PASSWORD" -> context.getString(R.string.auth_error_weak_password)
                    "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" ->
                        context.getString(R.string.auth_error_wrong_password)

                    "ERROR_USER_NOT_FOUND" -> context.getString(R.string.auth_error_user_not_found)
                    "ERROR_OPERATION_NOT_ALLOWED" ->
                        context.getString(R.string.auth_error_email_signin_disabled)

                    else -> context.getString(R.string.auth_error_generic)
                }
            }

            is ApiException -> {
                if (throwable.statusCode == 12501) {
                    context.getString(R.string.auth_error_signin_cancelled)
                } else {
                    context.getString(R.string.auth_error_generic)
                }
            }

            else -> context.getString(R.string.auth_error_generic)
        }
    }

    override fun onCleared() {
        authStateListener?.let(FirebaseAuthRepository::removeAuthStateListener)
        authStateListener = null
        super.onCleared()
    }
}
