package com.pgmanager.app.data.repository

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.pgmanager.app.domain.model.AuthState
import com.pgmanager.app.domain.model.User
import com.pgmanager.app.domain.model.UserRole
import com.pgmanager.app.domain.repository.AuthRepository
import com.pgmanager.app.security.EncryptedSessionStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.concurrent.TimeUnit

class AuthRepositoryImpl(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val sessionStore: EncryptedSessionStore
) : AuthRepository {

    override fun observeAuthState(): Flow<AuthState> = callbackFlow {
        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                sessionStore.clearAll()
                trySend(AuthState.Unauthenticated)
            } else {
                val userDocRef = firestore.collection("users").document(firebaseUser.uid)
                val snapshotRegistration = userDocRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(AuthState.Unauthenticated)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val roleStr = snapshot.getString("role")
                        val role = UserRole.fromString(roleStr)
                        val fullName = snapshot.getString("fullName") ?: ""
                        val email = snapshot.getString("email")
                        val profilePhotoUrl = snapshot.getString("profilePhotoUrl")
                        val address = snapshot.getString("address")
                        val idProofUrl = snapshot.getString("idProofUrl")
                        val activePgId = snapshot.getString("activePgId")
                        val ownedPgIds = (snapshot.get("ownedPgIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                        // Pending Join Request check
                        val pendingReq = snapshot.get("pendingJoinRequest") as? Map<*, *>
                        val pendingReqId = pendingReq?.get("requestId") as? String
                        val pendingPgName = pendingReq?.get("pgName") as? String
                        val pendingExpiresAt = pendingReq?.get("expiresAt")?.toString()

                        val user = User(
                            userId = firebaseUser.uid,
                            phoneNumber = firebaseUser.phoneNumber ?: "",
                            email = email,
                            fullName = fullName,
                            role = role,
                            profilePhotoUrl = profilePhotoUrl,
                            address = address,
                            idProofUrl = idProofUrl,
                            ownedPgIds = ownedPgIds,
                            activePgId = activePgId,
                            pendingJoinRequestId = pendingReqId,
                            pendingPgName = pendingPgName,
                            pendingExpiresAt = pendingExpiresAt
                        )

                        sessionStore.saveSession(
                            userId = user.userId,
                            phoneNumber = user.phoneNumber,
                            role = user.role,
                            activePgId = user.activePgId
                        )

                        if (role == UserRole.STUDENT && activePgId == null && pendingReqId != null) {
                            trySend(
                                AuthState.AwaitingApproval(
                                    user = user,
                                    requestId = pendingReqId,
                                    pgName = pendingPgName ?: "PG Accommodation",
                                    expiresAt = pendingExpiresAt ?: ""
                                )
                            )
                        } else {
                            trySend(AuthState.Authenticated(user))
                        }
                    } else {
                        // User profile doc does not exist yet (first-time registration)
                        val initialUser = User(
                            userId = firebaseUser.uid,
                            phoneNumber = firebaseUser.phoneNumber ?: "",
                            role = UserRole.UNASSIGNED
                        )
                        trySend(AuthState.Authenticated(initialUser))
                    }
                }
            }
        }

        auth.addAuthStateListener(authListener)

        awaitClose {
            auth.removeAuthStateListener(authListener)
        }
    }

    override suspend fun getCurrentUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) {
                val role = UserRole.fromString(doc.getString("role"))
                val ownedPgIds = (doc.get("ownedPgIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                User(
                    userId = uid,
                    phoneNumber = auth.currentUser?.phoneNumber ?: "",
                    email = doc.getString("email"),
                    fullName = doc.getString("fullName") ?: "",
                    role = role,
                    profilePhotoUrl = doc.getString("profilePhotoUrl"),
                    address = doc.getString("address"),
                    idProofUrl = doc.getString("idProofUrl"),
                    ownedPgIds = ownedPgIds,
                    activePgId = doc.getString("activePgId")
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }

    override fun sendPhoneOtp(
        activity: Activity,
        phoneNumber: String,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    override suspend fun verifyOtp(verificationId: String, otpCode: String): Result<User> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase user is null")

            // Ensure user record exists in Firestore
            val userRef = firestore.collection("users").document(firebaseUser.uid)
            val doc = userRef.get().await()
            if (!doc.exists()) {
                userRef.set(
                    mapOf(
                        "userId" to firebaseUser.uid,
                        "phoneNumber" to (firebaseUser.phoneNumber ?: ""),
                        "role" to "unassigned",
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val user = getCurrentUser() ?: User(
                userId = firebaseUser.uid,
                phoneNumber = firebaseUser.phoneNumber ?: ""
            )
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(activity: Activity): Result<User> {
        return try {
            val clientId = context.getString(com.pgmanager.app.R.string.google_web_client_id)
            if (clientId.isBlank()) {
                throw IllegalStateException(
                    "Google Sign-In needs the Firebase Web client ID in google_web_client_id."
                )
            }
            val googleOption = GetGoogleIdOption.Builder()
                .setServerClientId(clientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()
            val credentialManager = CredentialManager.create(activity)
            val result = credentialManager.getCredential(activity, request)
            val googleCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val firebaseCredential = com.google.firebase.auth.GoogleAuthProvider
                .getCredential(googleCredential.idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase user is null")
            val userRef = firestore.collection("users").document(firebaseUser.uid)
            val doc = userRef.get().await()
            if (!doc.exists()) {
                userRef.set(
                    mapOf(
                        "userId" to firebaseUser.uid,
                        "email" to firebaseUser.email,
                        "fullName" to (firebaseUser.displayName ?: ""),
                        "profilePhotoUrl" to firebaseUser.photoUrl?.toString(),
                        "phoneNumber" to (firebaseUser.phoneNumber ?: ""),
                        "role" to "unassigned",
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                ).await()
            }
            Result.success(getCurrentUser() ?: User(
                userId = firebaseUser.uid,
                email = firebaseUser.email,
                fullName = firebaseUser.displayName ?: "",
                phoneNumber = firebaseUser.phoneNumber ?: ""
            ))
        } catch (e: GetCredentialException) {
            Result.failure(IllegalStateException("Google Sign-In was cancelled or unavailable", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun assignRole(role: UserRole): Result<Unit> {
        return try {
            val roleName = when (role) {
                UserRole.OWNER -> "owner"
                UserRole.STUDENT -> "student"
                UserRole.UNASSIGNED -> "unassigned"
            }
            functions.getHttpsCallable("assignUserRole")
                .call(mapOf("role" to roleName))
                .await()

            // Force refresh token to update claims immediately
            auth.currentUser?.getIdToken(true)?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(
        fullName: String,
        email: String?,
        address: String?,
        idProofUrl: String?
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        return try {
            val updates = mutableMapOf<String, Any?>(
                "fullName" to fullName,
                "email" to email,
                "address" to address,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            if (idProofUrl != null) {
                updates["idProofUrl"] = idProofUrl
            }
            firestore.collection("users").document(uid).update(updates.filterValues { it != null }).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                try {
                    firestore.collection("users").document(uid).update("fcmToken", null).await()
                } catch (_: Exception) {
                    // Best effort token removal
                }
            }
            sessionStore.clearAll()
            // Clear local cached files
            try {
                context.cacheDir.deleteRecursively()
            } catch (_: Exception) {
                // Best effort cache deletion
            }
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            functions.getHttpsCallable("deleteUserAccount").call().await()
            sessionStore.clearAll()
            context.cacheDir.deleteRecursively()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
