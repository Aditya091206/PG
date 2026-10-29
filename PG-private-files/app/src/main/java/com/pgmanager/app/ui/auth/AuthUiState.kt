package com.pgmanager.app.ui.auth

import com.pgmanager.app.domain.model.UserRole

data class AuthUiState(
    val phoneNumber: String = "",
    val countryCode: String = "+91",
    val verificationId: String = "",
    val otpCode: String = "",
    val fullName: String = "",
    val email: String = "",
    val address: String = "",
    val termsAccepted: Boolean = false,
    val selectedRole: UserRole = UserRole.UNASSIGNED,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val resendCountdown: Int = 60,
    val canResend: Boolean = false
)
