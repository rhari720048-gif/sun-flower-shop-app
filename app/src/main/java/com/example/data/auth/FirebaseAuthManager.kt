package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.domain.AppLanguage
import com.example.domain.UserProfile
import com.example.domain.UserRole
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class FirebaseAuthStatus(
    val isFirebaseConfigured: Boolean,
    val projectId: String?,
    val hasWebClientId: Boolean
) {
    val isGoogleSignInReady: Boolean get() = isFirebaseConfigured && hasWebClientId
}

/**
 * Firebase Authentication manager — Google Sign-In ONLY (via Jetpack Credential Manager).
 * All business data is stored in Neon PostgreSQL (Firebase is used strictly for Auth).
 */
class FirebaseAuthManager(private val context: Context) {

    fun inspectConfiguration(): FirebaseAuthStatus {
        val apps = runCatching { FirebaseApp.getApps(context) }.getOrDefault(emptyList())
        val app = apps.firstOrNull()
        val projectId = app?.options?.projectId?.takeIf {
            it.isNotBlank() && it != "remixed-project-id"
        }
        val webClientId = resolveDefaultWebClientId()
        return FirebaseAuthStatus(
            isFirebaseConfigured = projectId != null,
            projectId = projectId,
            hasWebClientId = !webClientId.isNullOrBlank()
        )
    }

    fun getCurrentFirebaseUser(): UserProfile? {
        if (!inspectConfiguration().isFirebaseConfigured) return null
        val user = runCatching { Firebase.auth.currentUser }.getOrNull() ?: return null
        return UserProfile(
            id = user.uid,
            firebaseUid = user.uid,
            name = user.displayName?.takeIf { it.isNotBlank() } ?: "Sun Flowers Guest",
            phone = user.phoneNumber.orEmpty(),
            email = user.email,
            role = resolveRoleForEmail(user.email),
            language = AppLanguage.EN
        )
    }

    /**
     * Interactive Google Sign-In via Jetpack Credential Manager (single option per request).
     */
    suspend fun signInWithGoogleInteractive(activity: Activity): Result<UserProfile> {
        val status = inspectConfiguration()
        val webClientId = resolveDefaultWebClientId()
        if (!status.isFirebaseConfigured) {
            return Result.failure(
                IllegalStateException(
                    "Firebase is not initialized. Verify app/google-services.json is present."
                )
            )
        }
        if (webClientId.isNullOrBlank()) {
            return Result.failure(
                IllegalStateException(
                    "Google OAuth client is missing in google-services.json. Add the app's SHA-1 & SHA-256 in Firebase Console (${status.projectId}), then re-download google-services.json."
                )
            )
        }

        return try {
            val credentialManager = CredentialManager.create(activity)
            val googleOption = GetSignInWithGoogleOption.Builder(webClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()
            val response = credentialManager.getCredential(activity, request)
            val idTokenCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
            val firebaseCred = GoogleAuthProvider.getCredential(idTokenCredential.idToken, null)
            val authResult = Firebase.auth.signInWithCredential(firebaseCred).await()
            val fbUser = authResult.user
                ?: return Result.failure(IllegalStateException("Google Sign-In returned empty user"))
            Result.success(
                UserProfile(
                    id = fbUser.uid,
                    firebaseUid = fbUser.uid,
                    name = fbUser.displayName?.takeIf { it.isNotBlank() } ?: "Flower Lover",
                    phone = fbUser.phoneNumber.orEmpty(),
                    email = fbUser.email,
                    role = resolveRoleForEmail(fbUser.email)
                )
            )
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Interactive Google Sign-In cancelled by user or SHA mismatch", e)
            Result.failure(IllegalStateException("Sign-in was cancelled. Please select a Google account to continue."))
        } catch (e: NoCredentialException) {
            Log.w("Auth", "No Google credentials found on device", e)
            Result.failure(IllegalStateException("No Google account found on this device."))
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    /**
     * Silent auto sign-in using GetGoogleIdOption in its own single-option request.
     */
    suspend fun attemptSilentGoogleSignIn(activity: Activity): UserProfile? {
        val status = inspectConfiguration()
        val webClientId = resolveDefaultWebClientId()
        if (!status.isGoogleSignInReady || webClientId.isNullOrBlank()) return null

        return try {
            val credentialManager = CredentialManager.create(activity)
            val silentOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(silentOption)
                .build()
            val response = credentialManager.getCredential(activity, request)
            val tokenCred = GoogleIdTokenCredential.createFrom(response.credential.data)
            val fbCred = GoogleAuthProvider.getCredential(tokenCred.idToken, null)
            val fbUser = Firebase.auth.signInWithCredential(fbCred).await().user ?: return null
            UserProfile(
                id = fbUser.uid,
                firebaseUid = fbUser.uid,
                name = fbUser.displayName ?: "Flower Lover",
                phone = fbUser.phoneNumber.orEmpty(),
                email = fbUser.email,
                role = resolveRoleForEmail(fbUser.email)
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun signOut() {
        withContext(NonCancellable) {
            if (inspectConfiguration().isFirebaseConfigured) {
                runCatching { Firebase.auth.signOut() }
            }
            runCatching {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            }
        }
    }

    suspend fun deleteFirebaseUserIfPresent() {
        withContext(NonCancellable) {
            if (inspectConfiguration().isFirebaseConfigured) {
                runCatching { Firebase.auth.currentUser?.delete()?.await() }
            }
        }
    }

    private fun resolveDefaultWebClientId(): String? {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId == 0) return null
        return runCatching { context.getString(resId) }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    companion object {
        const val OWNER_ADMIN_EMAIL = "flowershopsun03@gmail.com"

        fun resolveRoleForEmail(email: String?): UserRole {
            return if (email != null && email.trim().equals(OWNER_ADMIN_EMAIL, ignoreCase = true)) {
                UserRole.ADMIN
            } else {
                UserRole.CUSTOMER
            }
        }
    }
}
