package com.pgmanager.app.domain.model

enum class JoinRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    EXPIRED;

    companion object {
        fun fromString(value: String?): JoinRequestStatus {
            return when (value?.uppercase()) {
                "APPROVED" -> APPROVED
                "REJECTED" -> REJECTED
                "EXPIRED" -> EXPIRED
                else -> PENDING
            }
        }
    }
}

data class JoinRequest(
    val requestId: String = "",
    val pgId: String = "",
    val pgName: String = "",
    val studentId: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val age: Int = 0,
    val collegeOrWorkplace: String = "",
    val homeCity: String = "",
    val guardianName: String = "",
    val guardianPhone: String = "",
    val idProofUrl: String = "",
    val status: JoinRequestStatus = JoinRequestStatus.PENDING,
    val assignedRoomId: String? = null,
    val assignedBedId: String? = null,
    val rejectionReason: String? = null,
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val reviewedAt: Long? = null
)

data class StudentResident(
    val studentId: String = "",
    val pgId: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val collegeOrWorkplace: String = "",
    val homeCity: String = "",
    val guardianName: String = "",
    val guardianPhone: String = "",
    val idProofUrl: String = "",
    val roomId: String = "",
    val roomNumber: String = "",
    val bedId: String = "",
    val bedIdentifier: String = "",
    val monthlyRent: Long = 0L,
    val securityDeposit: Long = 0L,
    val checkInDate: Long = 0L,
    val checkOutDate: Long? = null,
    val status: String = "ACTIVE", // ACTIVE, ON_NOTICE, CHECKED_OUT
    val depositRefunded: Boolean = false,
    val depositRefundAmount: Long? = null
)
