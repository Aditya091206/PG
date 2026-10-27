package com.pgmanager.app.domain.repository

import com.pgmanager.app.domain.model.Bed
import com.pgmanager.app.domain.model.CapacitySummary
import com.pgmanager.app.domain.model.JoinRequest
import com.pgmanager.app.domain.model.PGProperty
import com.pgmanager.app.domain.model.Room
import com.pgmanager.app.domain.model.StudentResident
import kotlinx.coroutines.flow.Flow

interface PGRepository {
    fun observeOwnerProperties(ownerId: String): Flow<List<PGProperty>>
    fun observeProperty(pgId: String): Flow<PGProperty?>
    suspend fun createProperty(property: PGProperty): Result<String>
    suspend fun updateProperty(property: PGProperty): Result<Unit>
    suspend fun generateCode(pgId: String, pgName: String, contactPhone: String): Result<String>
    suspend fun regenerateCode(pgId: String): Result<String>
    suspend fun verifyGst(pgId: String, gstNumber: String): Result<Boolean>
    fun observeRooms(pgId: String): Flow<List<Room>>
    suspend fun createRoomWithBeds(pgId: String, room: Room): Result<String>
    fun observeBeds(pgId: String): Flow<List<Bed>>
    fun observeLiveCapacity(pgId: String): Flow<CapacitySummary>
    fun observeStudents(pgId: String): Flow<List<StudentResident>>
    fun observeJoinRequests(pgId: String): Flow<List<JoinRequest>>
    suspend fun submitJoinRequest(code: String, studentData: Map<String, Any>): Result<String>
    suspend fun reviewJoinRequest(
        pgId: String,
        requestId: String,
        action: String,
        roomId: String?,
        bedId: String?,
        rejectionReason: String?
    ): Result<Unit>
    suspend fun shiftStudentRoom(
        pgId: String,
        studentId: String,
        studentName: String,
        oldBedId: String,
        newRoomId: String,
        newRoomNumber: String,
        newBedId: String,
        newBedIdentifier: String
    ): Result<Unit>
    suspend fun processStudentCheckOut(
        pgId: String,
        studentId: String,
        bedId: String,
        refundAmount: Long
    ): Result<Unit>
}
