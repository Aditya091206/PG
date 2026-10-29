package com.pgmanager.app.domain.model

data class PGProperty(
    val pgId: String = "",
    val ownerId: String = "",
    val name: String = "",
    val code: String = "",
    val codeCreatedAt: Long = 0L,
    val gstNumber: String? = null,
    val gstVerificationStatus: String = "NOT_SUBMITTED", // NOT_SUBMITTED, PENDING, VERIFIED, FAILED
    val type: PGType = PGType.CO_LIVING,
    val contactPhone: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val amenities: List<String> = emptyList(),
    val rules: List<String> = emptyList(),
    val photos: List<String> = emptyList(),
    val securityDepositRules: String = "One month rent advance deposit",
    val noticePeriodDays: Int = 30,
    val electricityBillingMode: String = "INCLUDED", // INCLUDED, SPLIT_PER_ROOM, METERED_PER_UNIT
    val wifiSsid: String? = null,
    val wifiPassword: String? = null,
    val curfewTime: String = "22:30",
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val nearbyLandmarks: List<String> = emptyList(),
    val upiId: String? = null,
    val upiQrImageUrl: String? = null,
    val createdAt: Long = 0L
)

enum class PGType {
    BOYS,
    GIRLS,
    CO_LIVING;

    companion object {
        fun fromString(value: String?): PGType {
            return when (value?.uppercase()) {
                "BOYS" -> BOYS
                "GIRLS" -> GIRLS
                else -> CO_LIVING
            }
        }
    }
}

data class EmergencyContact(
    val name: String = "",
    val role: String = "",
    val phone: String = ""
)
