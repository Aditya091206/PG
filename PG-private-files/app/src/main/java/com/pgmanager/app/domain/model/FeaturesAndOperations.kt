package com.pgmanager.app.domain.model

data class MealItem(
    val timeWindow: String = "",
    val items: List<String> = emptyList()
)

data class MealFeedback(
    val studentId: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val mealType: String = "LUNCH"
)

data class DailyMealPlan(
    val date: String = "", // YYYY-MM-DD
    val breakfast: MealItem = MealItem("07:30 - 09:30", listOf("Idli", "Sambar", "Chutney", "Tea/Coffee")),
    val lunch: MealItem = MealItem("12:30 - 14:30", listOf("Rice", "Dal Tadka", "Paneer Butter Masala", "Roti", "Curd")),
    val snacks: MealItem = MealItem("17:00 - 18:00", listOf("Samosa", "Masala Chai")),
    val dinner: MealItem = MealItem("20:00 - 22:00", listOf("Jeera Rice", "Dal Makhani", "Mixed Veg", "Roti", "Gulab Jamun")),
    val isHolidayMenu: Boolean = false,
    val optOutStudentIds: List<String> = emptyList(),
    val feedback: List<MealFeedback> = emptyList()
)

enum class NoticeCategory {
    GENERAL,
    FOOD,
    MAINTENANCE,
    RULES,
    EMERGENCY
}

data class Notice(
    val noticeId: String = "",
    val pgId: String = "",
    val title: String = "",
    val content: String = "",
    val category: NoticeCategory = NoticeCategory.GENERAL,
    val isPinned: Boolean = false,
    val authorId: String = "",
    val createdAt: Long = 0L
)

enum class TicketCategory {
    PLUMBING,
    ELECTRICAL,
    CARPENTRY,
    CLEANING,
    APPLIANCE,
    OTHER
}

enum class TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED
}

data class MaintenanceTicket(
    val ticketId: String = "",
    val pgId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val roomNumber: String = "",
    val category: TicketCategory = TicketCategory.OTHER,
    val title: String = "",
    val description: String = "",
    val photoUrls: List<String> = emptyList(),
    val status: TicketStatus = TicketStatus.OPEN,
    val assignedTo: String? = null,
    val resolutionNotes: String? = null,
    val createdAt: Long = 0L,
    val resolvedAt: Long? = null
)

enum class DueStatus {
    UNPAID,
    PARTIALLY_PAID,
    PAID,
    OVERDUE
}

data class RentDue(
    val dueId: String = "",
    val pgId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val roomNumber: String = "",
    val month: String = "",
    val amount: Long = 0L,
    val lateFee: Long = 0L,
    val totalAmount: Long = 0L,
    val dueDate: Long = 0L,
    val status: DueStatus = DueStatus.UNPAID,
    val amountPaid: Long = 0L,
    val paidAt: Long? = null,
    val paymentMode: String? = null,
    val transactionReference: String? = null,
    val receiptUrl: String? = null,
    val notes: String? = null,
    val createdAt: Long = 0L
)

data class CleaningSlot(
    val cleaningId: String = "",
    val pgId: String = "",
    val areaName: String = "",
    val assignedStaff: String = "",
    val scheduledDate: String = "",
    val isCompleted: Boolean = false,
    val notes: String? = null
)

data class GatePass(
    val passId: String = "",
    val pgId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val roomNumber: String = "",
    val purpose: String = "",
    val outTime: String = "",
    val expectedInTime: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val createdAt: Long = 0L
)

data class FinancialReport(
    val totalBilled: Long = 0L,
    val totalCollected: Long = 0L,
    val totalOverdue: Long = 0L,
    val defaulters: List<RentDue> = emptyList()
)
