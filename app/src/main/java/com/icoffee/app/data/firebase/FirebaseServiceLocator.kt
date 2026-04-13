package com.icoffee.app.data.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object FirebaseServiceLocator {

    private var initialized = false

    val auth: FirebaseAuth
        get() {
            ensureInitialized()
            return FirebaseAuth.getInstance()
        }

    val firestore: FirebaseFirestore
        get() {
            ensureInitialized()
            return FirebaseFirestore.getInstance()
        }

    val storage: FirebaseStorage
        get() {
            ensureInitialized()
            return FirebaseStorage.getInstance()
        }

    fun initialize(context: Context) {
        if (initialized) return
        requireNotNull(FirebaseApp.initializeApp(context.applicationContext)) {
            "FirebaseApp initialization failed. Ensure app/google-services.json is valid."
        }
        initialized = true
    }

    private fun ensureInitialized() {
        check(initialized) {
            "FirebaseServiceLocator.initialize(context) must be called before usage."
        }
    }
}
