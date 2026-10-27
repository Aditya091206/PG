package com.pgmanager.app.domain.model

sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val user: User) : AuthState
    data class AwaitingApproval(
        val user: User,
        val requestId: String,
        val pgName: String,
        val expiresAt: String
    ) : AuthState
}
