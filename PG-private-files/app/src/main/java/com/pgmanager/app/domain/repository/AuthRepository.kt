package com.pgmanager.app.domain.repository

import android.app.Activity
import com.google.firebase.auth.PhoneAuthProvider
import com.pgmanager.app.domain.model.AuthState
import com.pgmanager.app.domain.model.User
import com.pgmanager.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeAuthState(): Flow<AuthState>
    suspend fun getCurrentUser(): User?
    fun sendPhoneOtp(
        activity: Activity,
        phoneNumber: String,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    )
    suspend fun verifyOtp(verificationId: String, otpCode: String): Result<User>
    suspend fun signInWithGoogle(activity: Activity): Result<User>
    suspend fun assignRole(role: UserRole): Result<Unit>
    suspend fun updateProfile(fullName: String, email: String?, address: String?, idProofUrl: String?): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
}
