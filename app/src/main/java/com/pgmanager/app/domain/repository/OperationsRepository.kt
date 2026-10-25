package com.pgmanager.app.domain.repository

import com.pgmanager.app.domain.model.CleaningSlot
import com.pgmanager.app.domain.model.DailyMealPlan
import com.pgmanager.app.domain.model.FinancialReport
import com.pgmanager.app.domain.model.GatePass
import com.pgmanager.app.domain.model.MaintenanceTicket
import com.pgmanager.app.domain.model.MealFeedback
import com.pgmanager.app.domain.model.Notice
import com.pgmanager.app.domain.model.RentDue
import com.pgmanager.app.domain.model.TicketStatus
import kotlinx.coroutines.flow.Flow

interface OperationsRepository {
    // Meals
    fun observeDailyMeal(pgId: String, date: String): Flow<DailyMealPlan?>
    suspend fun saveMealPlan(pgId: String, mealPlan: DailyMealPlan): Result<Unit>
    suspend fun toggleMealOptOut(pgId: String, date: String, studentId: String, optOut: Boolean): Result<Unit>
    suspend fun addMealFeedback(pgId: String, date: String, feedback: MealFeedback): Result<Unit>

    // Notices
    fun observeNotices(pgId: String): Flow<List<Notice>>
    suspend fun createNotice(pgId: String, notice: Notice): Result<String>

    // Tickets
    fun observeTickets(pgId: String, studentIdFilter: String? = null): Flow<List<MaintenanceTicket>>
    suspend fun createTicket(pgId: String, ticket: MaintenanceTicket): Result<String>
    suspend fun updateTicketStatus(pgId: String, ticketId: String, status: TicketStatus, notes: String?): Result<Unit>

    // Dues & Payments
    fun observeDues(pgId: String, studentIdFilter: String? = null): Flow<List<RentDue>>
    suspend fun generateDuesForActiveStudents(pgId: String, month: String): Result<Unit>
    suspend fun recordPayment(
        pgId: String,
        dueId: String,
        amount: Long,
        mode: String,
        transactionRef: String?
    ): Result<Unit>
    fun observeFinancialReport(pgId: String): Flow<FinancialReport>

    // Housekeeping
    fun observeCleaningSchedule(pgId: String): Flow<List<CleaningSlot>>
    suspend fun addCleaningSlot(pgId: String, slot: CleaningSlot): Result<String>
    suspend fun updateCleaningStatus(pgId: String, cleaningId: String, isCompleted: Boolean): Result<Unit>

    // Gate Passes
    fun observeGatePasses(pgId: String, studentIdFilter: String? = null): Flow<List<GatePass>>
    suspend fun requestGatePass(pgId: String, pass: GatePass): Result<String>
    suspend fun updateGatePassStatus(pgId: String, passId: String, status: String): Result<Unit>
}
