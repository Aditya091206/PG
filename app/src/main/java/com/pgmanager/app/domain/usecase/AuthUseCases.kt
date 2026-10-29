package com.pgmanager.app.domain.usecase

import com.pgmanager.app.domain.model.AuthState
import com.pgmanager.app.domain.model.User
import com.pgmanager.app.domain.model.UserRole
import com.pgmanager.app.domain.repository.AuthRepository
import com.google.firebase.auth.PhoneAuthCredential
import kotlinx.coroutines.flow.Flow

class ObserveAuthStateUseCase(private val authRepository: AuthRepository) {
    operator fun invoke(): Flow<AuthState> = authRepository.observeAuthState()
}

class VerifyOtpUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(verificationId: String, otpCode: String): Result<User> {
        return authRepository.verifyOtp(verificationId, otpCode)
    }
}

class VerifyCredentialUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(credential: PhoneAuthCredential): Result<User> {
        return authRepository.verifyCredential(credential)
    }
}

class AssignRoleUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(role: UserRole): Result<Unit> {
        return authRepository.assignRole(role)
    }
}

class UpdateProfileUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        fullName: String,
        email: String?,
        address: String?,
        idProofUrl: String?
    ): Result<Unit> {
        return authRepository.updateProfile(fullName, email, address, idProofUrl)
    }
}

class LogoutUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(): Result<Unit> = authRepository.logout()
}

class DeleteAccountUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(): Result<Unit> = authRepository.deleteAccount()
}
