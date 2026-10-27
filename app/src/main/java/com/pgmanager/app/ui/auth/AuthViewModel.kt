package com.pgmanager.app.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.pgmanager.app.domain.model.AuthState
import com.pgmanager.app.domain.model.User
import com.pgmanager.app.domain.model.UserRole
import com.pgmanager.app.domain.repository.AuthRepository
import com.pgmanager.app.domain.usecase.AssignRoleUseCase
import com.pgmanager.app.domain.usecase.DeleteAccountUseCase
import com.pgmanager.app.domain.usecase.LogoutUseCase
import com.pgmanager.app.domain.usecase.ObserveAuthStateUseCase
import com.pgmanager.app.domain.usecase.UpdateProfileUseCase
import com.pgmanager.app.domain.usecase.VerifyOtpUseCase
import com.pgmanager.app.domain.usecase.VerifyCredentialUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val verifyCredentialUseCase: VerifyCredentialUseCase,
    private val assignRoleUseCase: AssignRoleUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase
) : ViewModel() {

    val authState: StateFlow<AuthState> = observeAuthStateUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    fun onPhoneNumberChanged(number: String) {
        val filtered = number.filter { it.isDigit() }.take(10)
        _uiState.update { it.copy(phoneNumber = filtered, errorMessage = null) }
    }

    fun signInWithGoogle(activity: Activity, onSuccess: (User) -> Unit) {
        if (!_uiState.value.termsAccepted) {
            _uiState.update {
                it.copy(errorMessage = "Please accept the Terms and Privacy Policy to continue")
            }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            authRepository.signInWithGoogle(activity)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess(it)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Google Sign-In failed"
                        )
                    }
                }
        }
    }

    fun onTermsAcceptedChanged(accepted: Boolean) {
        _uiState.update { it.copy(termsAccepted = accepted, errorMessage = null) }
    }

    fun onOtpCodeChanged(code: String) {
        val filtered = code.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(otpCode = filtered, errorMessage = null) }
    }

    fun onFullNameChanged(name: String) {
        _uiState.update { it.copy(fullName = name, errorMessage = null) }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onAddressChanged(address: String) {
        _uiState.update { it.copy(address = address, errorMessage = null) }
    }

    fun selectRole(role: UserRole) {
        _uiState.update { it.copy(selectedRole = role, errorMessage = null) }
    }

    fun sendOtp(activity: Activity, onCodeSent: () -> Unit) {
        if (_uiState.value.isLoading) return
        val phone = _uiState.value.phoneNumber
        if (phone.length != 10) {
            _uiState.update { it.copy(errorMessage = "Enter a valid 10-digit mobile number") }
            return
        }
        if (!_uiState.value.termsAccepted) {
            _uiState.update { it.copy(errorMessage = "Please accept the Terms and Privacy Policy to continue") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        val fullPhoneNumber = "${_uiState.value.countryCode}$phone"

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                val code = credential.smsCode
                _uiState.update { it.copy(isLoading = true, otpCode = code ?: it.otpCode) }
                viewModelScope.launch {
                    verifyCredentialUseCase(credential)
                        .onSuccess { user ->
                            _uiState.update { it.copy(isLoading = false) }
                            onCodeSent()
                        }
                        .onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = error.localizedMessage
                                        ?: "Automatic verification failed. Enter the SMS code manually."
                                )
                            }
                        }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = firebaseAuthErrorMessage(e)
                    )
                }
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        verificationId = verificationId,
                        canResend = false,
                        resendCountdown = 60
                    )
                }
                startResendTimer()
                onCodeSent()
            }
        }

        try {
            authRepository.sendPhoneOtp(activity, fullPhoneNumber, callbacks)
        } catch (error: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = error.localizedMessage ?: "Unable to start phone verification."
                )
            }
        }
    }

    private fun firebaseAuthErrorMessage(error: FirebaseException): String {
        return when ((error as? com.google.firebase.auth.FirebaseAuthException)?.errorCode) {
            "ERROR_INVALID_PHONE_NUMBER" -> "Enter a valid phone number, including the country code."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please wait a while and try again."
            "ERROR_QUOTA_EXCEEDED" -> "SMS quota exceeded. Please try again later."
            "ERROR_APP_NOT_AUTHORIZED" ->
                "This app is not authorized for phone sign-in. Check Firebase app verification settings."
            else -> error.localizedMessage ?: "Failed to send verification code. Try again."
        }
    }

    private fun startResendTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (i in 60 downTo 1) {
                _uiState.update { it.copy(resendCountdown = i) }
                delay(1000)
            }
            _uiState.update { it.copy(canResend = true, resendCountdown = 0) }
        }
    }

    fun verifyOtp(onSuccess: (User) -> Unit) {
        val verificationId = _uiState.value.verificationId
        val code = _uiState.value.otpCode
        if (code.length != 6) {
            _uiState.update { it.copy(errorMessage = "Enter the 6-digit code received via SMS") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = verifyOtpUseCase(verificationId, code)
            result.onSuccess { user ->
                _uiState.update { it.copy(isLoading = false) }
                onSuccess(user)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Invalid verification code. Please check and re-enter."
                    )
                }
            }
        }
    }

    fun confirmRole(role: UserRole, onSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = assignRoleUseCase(role)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, selectedRole = role) }
                onSuccess()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to set role. Please try again."
                    )
                }
            }
        }
    }

    fun saveProfile(onSuccess: () -> Unit) {
        val name = _uiState.value.fullName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Full name is required") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = updateProfileUseCase(
                fullName = name,
                email = _uiState.value.email.ifBlank { null },
                address = _uiState.value.address.ifBlank { null },
                idProofUrl = null
            )
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to save profile. Try again."
                    )
                }
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            _uiState.update { AuthUiState() }
            onSuccess()
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            deleteAccountUseCase().onSuccess {
                _uiState.update { AuthUiState() }
                onSuccess()
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
