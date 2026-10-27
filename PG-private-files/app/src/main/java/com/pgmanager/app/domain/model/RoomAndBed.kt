package com.pgmanager.app.domain.model

enum class SharingType(val bedCount: Int) {
    SINGLE(1),
    DOUBLE(2),
    TRIPLE(3),
    FOUR_PLUS(4);

    companion object {
        fun fromString(value: String?): SharingType {
            return when (value?.uppercase()) {
                "SINGLE" -> SINGLE
                "DOUBLE" -> DOUBLE
                "TRIPLE" -> TRIPLE
                else -> FOUR_PLUS
            }
        }
    }
}

data class Room(
    val roomId: String = "",
    val pgId: String = "",
    val roomNumber: String = "",
    val floor: Int = 1,
    val sharingType: SharingType = SharingType.DOUBLE,
    val totalBeds: Int = 2,
    val rentPerBed: Long = 0L,
    val depositPerBed: Long = 0L,
    val hasAttachedBathroom: Boolean = true,
    val hasAirConditioner: Boolean = false,
    val createdAt: Long = 0L
)

enum class BedStatus {
    VACANT,
    BOOKED,
    NOTICE_PERIOD;

    companion object {
        fun fromString(value: String?): BedStatus {
            return when (value?.uppercase()) {
                "BOOKED" -> BOOKED
                "NOTICE_PERIOD" -> NOTICE_PERIOD
                else -> VACANT
            }
        }
    }
}

data class Bed(
    val bedId: String = "",
    val pgId: String = "",
    val roomId: String = "",
    val roomNumber: String = "",
    val bedIdentifier: String = "", // e.g. "101-A"
    val status: BedStatus = BedStatus.VACANT,
    val currentStudentId: String? = null,
    val currentStudentName: String? = null,
    val noticeEndDate: Long? = null,
    val updatedAt: Long = 0L
)

data class CapacitySummary(
    val totalBeds: Int = 0,
    val bookedBeds: Int = 0,
    val vacantBeds: Int = 0,
    val noticeBeds: Int = 0
) {
    val occupancyRatePercentage: Int
        get() = if (totalBeds > 0) ((bookedBeds * 100) / totalBeds) else 0
}
