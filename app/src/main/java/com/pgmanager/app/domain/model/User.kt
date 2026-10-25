package com.pgmanager.app.domain.model

data class User(
    val userId: String = "",
    val phoneNumber: String = "",
    val email: String? = null,
    val fullName: String = "",
    val role: UserRole = UserRole.UNASSIGNED,
    val profilePhotoUrl: String? = null,
    val address: String? = null,
    val idProofUrl: String? = null,
    val ownedPgIds: List<String> = emptyList(),
    val activePgId: String? = null,
    val pendingJoinRequestId: String? = null,
    val pendingPgName: String? = null,
    val pendingExpiresAt: String? = null
)
