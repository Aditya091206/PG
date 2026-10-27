package com.pgmanager.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.pgmanager.app.domain.model.CleaningSlot
import com.pgmanager.app.domain.model.DailyMealPlan
import com.pgmanager.app.domain.model.DueStatus
import com.pgmanager.app.domain.model.FinancialReport
import com.pgmanager.app.domain.model.GatePass
import com.pgmanager.app.domain.model.MaintenanceTicket
import com.pgmanager.app.domain.model.MealFeedback
import com.pgmanager.app.domain.model.MealItem
import com.pgmanager.app.domain.model.Notice
import com.pgmanager.app.domain.model.NoticeCategory
import com.pgmanager.app.domain.model.RentDue
import com.pgmanager.app.domain.model.TicketCategory
import com.pgmanager.app.domain.model.TicketStatus
import com.pgmanager.app.domain.repository.OperationsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class OperationsRepositoryImpl(
    private val firestore: FirebaseFirestore
) : OperationsRepository {

    // --- MEALS ---
    override fun observeDailyMeal(pgId: String, date: String): Flow<DailyMealPlan?> = callbackFlow {
        val docRef = firestore.collection("pgs").document(pgId).collection("meals").document(date)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(DailyMealPlan(date = date))
                return@addSnapshotListener
            }
            val data = snapshot.data ?: emptyMap()
            val breakfastMap = data["breakfast"] as? Map<*, *>
            val lunchMap = data["lunch"] as? Map<*, *>
            val snacksMap = data["snacks"] as? Map<*, *>
            val dinnerMap = data["dinner"] as? Map<*, *>
            val optOuts = (data["optOutStudentIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val feedbackList = (data["feedback"] as? List<*>)?.filterIsInstance<Map<*, *>>()?.map {
                MealFeedback(
                    studentId = it["studentId"]?.toString() ?: "",
                    rating = (it["rating"] as? Number)?.toInt() ?: 5,
                    comment = it["comment"]?.toString() ?: "",
                    mealType = it["mealType"]?.toString() ?: "LUNCH"
                )
            } ?: emptyList()

            val mealPlan = DailyMealPlan(
                date = date,
                breakfast = MealItem(
                    timeWindow = breakfastMap?.get("time")?.toString() ?: "07:30 - 09:30",
                    items = (breakfastMap?.get("items") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                ),
                lunch = MealItem(
                    timeWindow = lunchMap?.get("time")?.toString() ?: "12:30 - 14:30",
                    items = (lunchMap?.get("items") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                ),
                snacks = MealItem(
                    timeWindow = snacksMap?.get("time")?.toString() ?: "17:00 - 18:00",
                    items = (snacksMap?.get("items") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                ),
                dinner = MealItem(
                    timeWindow = dinnerMap?.get("time")?.toString() ?: "20:00 - 22:00",
                    items = (dinnerMap?.get("items") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                ),
                isHolidayMenu = data["isHolidayMenu"] as? Boolean ?: false,
                optOutStudentIds = optOuts,
                feedback = feedbackList
            )
            trySend(mealPlan)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun saveMealPlan(pgId: String, mealPlan: DailyMealPlan): Result<Unit> {
        return try {
            val docRef = firestore.collection("pgs").document(pgId).collection("meals").document(mealPlan.date)
            val data = mapOf(
                "date" to mealPlan.date,
                "breakfast" to mapOf("time" to mealPlan.breakfast.timeWindow, "items" to mealPlan.breakfast.items),
                "lunch" to mapOf("time" to mealPlan.lunch.timeWindow, "items" to mealPlan.lunch.items),
                "snacks" to mapOf("time" to mealPlan.snacks.timeWindow, "items" to mealPlan.snacks.items),
                "dinner" to mapOf("time" to mealPlan.dinner.timeWindow, "items" to mealPlan.dinner.items),
                "isHolidayMenu" to mealPlan.isHolidayMenu,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data, com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleMealOptOut(
        pgId: String,
        date: String,
        studentId: String,
        optOut: Boolean
    ): Result<Unit> {
        return try {
            val docRef = firestore.collection("pgs").document(pgId).collection("meals").document(date)
            val updateOp = if (optOut) {
                FieldValue.arrayUnion(studentId)
            } else {
                FieldValue.arrayRemove(studentId)
            }
            docRef.set(mapOf("optOutStudentIds" to updateOp), com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addMealFeedback(pgId: String, date: String, feedback: MealFeedback): Result<Unit> {
        return try {
            val docRef = firestore.collection("pgs").document(pgId).collection("meals").document(date)
            val feedbackMap = mapOf(
                "studentId" to feedback.studentId,
                "rating" to feedback.rating,
                "comment" to feedback.comment,
                "mealType" to feedback.mealType
            )
            docRef.set(mapOf("feedback" to FieldValue.arrayUnion(feedbackMap)), com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- NOTICES ---
    override fun observeNotices(pgId: String): Flow<List<Notice>> = callbackFlow {
        val query = firestore.collection("pgs").document(pgId).collection("notices")
            .orderBy("isPinned", Query.Direction.DESCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val notices = snapshot?.documents?.mapNotNull { doc ->
                val categoryStr = doc.getString("category")
                val category = NoticeCategory.entries.find { it.name.equals(categoryStr, true) } ?: NoticeCategory.GENERAL
                Notice(
                    noticeId = doc.id,
                    pgId = pgId,
                    title = doc.getString("title") ?: "",
                    content = doc.getString("content") ?: "",
                    category = category,
                    isPinned = doc.getBoolean("isPinned") ?: false,
                    authorId = doc.getString("authorId") ?: "",
                    createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                )
            } ?: emptyList()
            trySend(notices)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun createNotice(pgId: String, notice: Notice): Result<String> {
        return try {
            val docRef = firestore.collection("pgs").document(pgId).collection("notices").document()
            val data = mapOf(
                "noticeId" to docRef.id,
                "pgId" to pgId,
                "title" to notice.title,
                "content" to notice.content,
                "category" to notice.category.name,
                "isPinned" to notice.isPinned,
                "authorId" to notice.authorId,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- TICKETS ---
    override fun observeTickets(pgId: String, studentIdFilter: String?): Flow<List<MaintenanceTicket>> = callbackFlow {
        var query = firestore.collection("pgs").document(pgId).collection("tickets")
            .orderBy("createdAt", Query.Direction.DESCENDING)
        if (studentIdFilter != null) {
            query = query.whereEqualTo("studentId", studentIdFilter)
        }

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val tickets = snapshot?.documents?.mapNotNull { doc ->
                val catStr = doc.getString("category")
                val category = TicketCategory.entries.find { it.name.equals(catStr, true) } ?: TicketCategory.OTHER
                val statusStr = doc.getString("status")
                val status = TicketStatus.entries.find { it.name.equals(statusStr, true) } ?: TicketStatus.OPEN
                MaintenanceTicket(
                    ticketId = doc.id,
                    pgId = pgId,
                    studentId = doc.getString("studentId") ?: "",
                    studentName = doc.getString("studentName") ?: "",
                    roomNumber = doc.getString("roomNumber") ?: "",
                    category = category,
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    photoUrls = (doc.get("photoUrls") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    status = status,
                    assignedTo = doc.getString("assignedTo"),
                    resolutionNotes = doc.getString("resolutionNotes"),
                    createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                    resolvedAt = doc.getTimestamp("resolvedAt")?.toDate()?.time
                )
            } ?: emptyList()
            trySend(tickets)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun createTicket(pgId: String, ticket: MaintenanceTicket): Result<String> {
        return try {
            val docRef = firestore.collection("pgs").document(pgId).collection("tickets").document()
            val data = mapOf(
                "ticketId" to docRef.id,
                "pgId" to pgId,
                "studentId" to ticket.studentId,
                "studentName" to ticket.studentName,
                "roomNumber" to ticket.roomNumber,
                "category" to ticket.category.name,
                "title" to ticket.title,
                "description" to ticket.description,
                "photoUrls" to ticket.photoUrls,
                "status" to TicketStatus.OPEN.name,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTicketStatus(
        pgId: String,
        ticketId: String,
        status: TicketStatus,
        notes: String?
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any?>(
                "status" to status.name,
                "resolutionNotes" to notes,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (status == TicketStatus.RESOLVED) {
                updates["resolvedAt"] = FieldValue.serverTimestamp()
            }
            firestore.collection("pgs").document(pgId).collection("tickets").document(ticketId)
                .update(updates.filterValues { it != null })
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- DUES & PAYMENTS ---
    override fun observeDues(pgId: String, studentIdFilter: String?): Flow<List<RentDue>> = callbackFlow {
        var query = firestore.collection("pgs").document(pgId).collection("dues")
            .orderBy("dueDate", Query.Direction.DESCENDING)
        if (studentIdFilter != null) {
            query = query.whereEqualTo("studentId", studentIdFilter)
        }

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val dues = snapshot?.documents?.mapNotNull { doc ->
                val statusStr = doc.getString("status")
                val status = DueStatus.entries.find { it.name.equals(statusStr, true) } ?: DueStatus.UNPAID
                RentDue(
                    dueId = doc.id,
                    pgId = pgId,
                    studentId = doc.getString("studentId") ?: "",
                    studentName = doc.getString("studentName") ?: "",
                    roomNumber = doc.getString("roomNumber") ?: "",
                    month = doc.getString("month") ?: "",
                    amount = doc.getLong("amount") ?: 0L,
                    lateFee = doc.getLong("lateFee") ?: 0L,
                    totalAmount = doc.getLong("totalAmount") ?: 0L,
                    dueDate = doc.getTimestamp("dueDate")?.toDate()?.time ?: 0L,
                    status = status,
                    amountPaid = doc.getLong("amountPaid") ?: 0L,
                    paidAt = doc.getTimestamp("paidAt")?.toDate()?.time,
                    paymentMode = doc.getString("paymentMode"),
                    transactionReference = doc.getString("transactionReference"),
                    receiptUrl = doc.getString("receiptUrl"),
                    notes = doc.getString("notes"),
                    createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                )
            } ?: emptyList()
            trySend(dues)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun generateDuesForActiveStudents(pgId: String, month: String): Result<Unit> {
        return try {
            val studentsSnapshot = firestore.collection("pgs").document(pgId).collection("students")
                .whereEqualTo("status", "ACTIVE")
                .get()
                .await()

            val batch = firestore.batch()
            val duesCollection = firestore.collection("pgs").document(pgId).collection("dues")
            val dueCalendar = java.util.Calendar.getInstance()
            dueCalendar.add(java.util.Calendar.DAY_OF_MONTH, 5) // Due in 5 days

            for (doc in studentsSnapshot.documents) {
                val studentId = doc.id
                val rent = doc.getLong("monthlyRent") ?: 0L
                val name = doc.getString("fullName") ?: ""
                val roomNumber = doc.getString("roomNumber") ?: ""

                val dueRef = duesCollection.document()
                batch.set(
                    dueRef,
                    mapOf(
                        "dueId" to dueRef.id,
                        "pgId" to pgId,
                        "studentId" to studentId,
                        "studentName" to name,
                        "roomNumber" to roomNumber,
                        "month" to month,
                        "amount" to rent,
                        "lateFee" to 0L,
                        "totalAmount" to rent,
                        "dueDate" to com.google.firebase.Timestamp(dueCalendar.time),
                        "status" to DueStatus.UNPAID.name,
                        "amountPaid" to 0L,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordPayment(
        pgId: String,
        dueId: String,
        amount: Long,
        mode: String,
        transactionRef: String?
    ): Result<Unit> {
        return try {
            val dueRef = firestore.collection("pgs").document(pgId).collection("dues").document(dueId)
            val doc = dueRef.get().await()
            val currentTotal = doc.getLong("totalAmount") ?: 0L
            val currentPaid = doc.getLong("amountPaid") ?: 0L
            val newTotalPaid = currentPaid + amount
            val newStatus = if (newTotalPaid >= currentTotal) DueStatus.PAID.name else DueStatus.PARTIALLY_PAID.name

            dueRef.update(
                mapOf(
                    "amountPaid" to newTotalPaid,
                    "status" to newStatus,
                    "paymentMode" to mode,
                    "transactionReference" to transactionRef,
                    "paidAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeFinancialReport(pgId: String): Flow<FinancialReport> = callbackFlow {
        val query = firestore.collection("pgs").document(pgId).collection("dues")
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(FinancialReport())
                return@addSnapshotListener
            }
            var billed = 0L
            var collected = 0L
            var overdue = 0L
            val defaulters = mutableListOf<RentDue>()

            val now = System.currentTimeMillis()

            for (doc in snapshot.documents) {
                val total = doc.getLong("totalAmount") ?: 0L
                val paid = doc.getLong("amountPaid") ?: 0L
                val dueDate = doc.getTimestamp("dueDate")?.toDate()?.time ?: 0L
                val statusStr = doc.getString("status")
                val status = DueStatus.entries.find { it.name.equals(statusStr, true) } ?: DueStatus.UNPAID

                billed += total
                collected += paid

                if (paid < total && dueDate < now) {
                    overdue += (total - paid)
                    defaulters.add(
                        RentDue(
                            dueId = doc.id,
                            pgId = pgId,
                            studentId = doc.getString("studentId") ?: "",
                            studentName = doc.getString("studentName") ?: "",
                            roomNumber = doc.getString("roomNumber") ?: "",
                            month = doc.getString("month") ?: "",
                            amount = doc.getLong("amount") ?: 0L,
                            totalAmount = total,
                            dueDate = dueDate,
                            status = DueStatus.OVERDUE,
                            amountPaid = paid
                        )
                    )
                }
            }

            trySend(
                FinancialReport(
                    totalBilled = billed,
                    totalCollected = collected,
                    totalOverdue = overdue,
                    defaulters = defaulters
                )
            )
        }
        awaitClose { registration.remove() }
    }

    // --- HOUSEKEEPING ---
    override fun observeCleaningSchedule(pgId: String): Flow<List<CleaningSlot>> = callbackFlow {
        val query = firestore.collection("pgs").document(pgId).collection("cleaning")
            .orderBy("scheduledDate", Query.Direction.ASCENDING)
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val slots = snapshot?.documents?.mapNotNull { doc ->
                CleaningSlot(
                    cleaningId = doc.id,
                    pgId = pgId,
                    areaName = doc.getString("areaName") ?: "",
                    assignedStaff = doc.getString("assignedStaff") ?: "",
                    scheduledDate = doc.getString("scheduledDate") ?: "",
                    isCompleted = doc.getBoolean("isCompleted") ?: false,
                    notes = doc.getString("notes")
                )
            } ?: emptyList()
            trySend(slots)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun addCleaningSlot(pgId: String, slot: CleaningSlot): Result<String> {
        return try {
            val docRef = firestore.collection("pgs").document(pgId).collection("cleaning").document()
            val data = mapOf(
                "cleaningId" to docRef.id,
                "pgId" to pgId,
                "areaName" to slot.areaName,
                "assignedStaff" to slot.assignedStaff,
                "scheduledDate" to slot.scheduledDate,
                "isCompleted" to slot.isCompleted,
                "notes" to slot.notes,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCleaningStatus(
        pgId: String,
        cleaningId: String,
        isCompleted: Boolean
    ): Result<Unit> {
        return try {
            firestore.collection("pgs").document(pgId).collection("cleaning").document(cleaningId)
                .update("isCompleted", isCompleted)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- GATE PASSES ---
    override fun observeGatePasses(pgId: String, studentIdFilter: String?): Flow<List<GatePass>> = callbackFlow {
        var query = firestore.collection("pgs").document(pgId).collection("gate_passes")
            .orderBy("createdAt", Query.Direction.DESCENDING)
        if (studentIdFilter != null) {
            query = query.whereEqualTo("studentId", studentIdFilter)
        }

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val passes = snapshot?.documents?.mapNotNull { doc ->
                GatePass(
                    passId = doc.id,
                    pgId = pgId,
                    studentId = doc.getString("studentId") ?: "",
                    studentName = doc.getString("studentName") ?: "",
                    roomNumber = doc.getString("roomNumber") ?: "",
                    purpose = doc.getString("purpose") ?: "",
                    outTime = doc.getString("outTime") ?: "",
                    expectedInTime = doc.getString("expectedInTime") ?: "",
                    status = doc.getString("status") ?: "PENDING",
                    createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                )
            } ?: emptyList()
            trySend(passes)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun requestGatePass(pgId: String, pass: GatePass): Result<String> {
        return try {
            val docRef = firestore.collection("pgs").document(pgId).collection("gate_passes").document()
            val data = mapOf(
                "passId" to docRef.id,
                "pgId" to pgId,
                "studentId" to pass.studentId,
                "studentName" to pass.studentName,
                "roomNumber" to pass.roomNumber,
                "purpose" to pass.purpose,
                "outTime" to pass.outTime,
                "expectedInTime" to pass.expectedInTime,
                "status" to "PENDING",
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateGatePassStatus(pgId: String, passId: String, status: String): Result<Unit> {
        return try {
            firestore.collection("pgs").document(pgId).collection("gate_passes").document(passId)
                .update("status", status)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
