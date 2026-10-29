package com.pgmanager.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.pgmanager.app.domain.model.Bed
import com.pgmanager.app.domain.model.BedStatus
import com.pgmanager.app.domain.model.CapacitySummary
import com.pgmanager.app.domain.model.EmergencyContact
import com.pgmanager.app.domain.model.JoinRequest
import com.pgmanager.app.domain.model.JoinRequestStatus
import com.pgmanager.app.domain.model.PGProperty
import com.pgmanager.app.domain.model.PGType
import com.pgmanager.app.domain.model.Room
import com.pgmanager.app.domain.model.SharingType
import com.pgmanager.app.domain.model.StudentResident
import com.pgmanager.app.domain.repository.PGRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PGRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : PGRepository {

    override fun observeOwnerProperties(ownerId: String): Flow<List<PGProperty>> = callbackFlow {
        val query = firestore.collection("pgs").whereEqualTo("ownerId", ownerId)
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val properties = snapshot?.documents?.mapNotNull { doc ->
                mapDocumentToProperty(doc.id, doc.data ?: emptyMap())
            } ?: emptyList()
            trySend(properties)
        }
        awaitClose { registration.remove() }
    }

    override fun observeProperty(pgId: String): Flow<PGProperty?> = callbackFlow {
        val docRef = firestore.collection("pgs").document(pgId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(null)
                return@addSnapshotListener
            }
            trySend(mapDocumentToProperty(snapshot.id, snapshot.data ?: emptyMap()))
        }
        awaitClose { registration.remove() }
    }

    override suspend fun createProperty(property: PGProperty): Result<String> {
        return try {
            val docRef = firestore.collection("pgs").document()
            val pgId = docRef.id

            val data = mutableMapOf<String, Any?>(
                "pgId" to pgId,
                "ownerId" to property.ownerId,
                "name" to property.name,
                "type" to property.type.name,
                "contactPhone" to property.contactPhone,
                "address" to property.address,
                "latitude" to property.latitude,
                "longitude" to property.longitude,
                "gstNumber" to property.gstNumber,
                "gstVerificationStatus" to if (property.gstNumber.isNullOrBlank()) "NOT_SUBMITTED" else "PENDING",
                "amenities" to property.amenities,
                "rules" to property.rules,
                "photos" to property.photos,
                "securityDepositRules" to property.securityDepositRules,
                "noticePeriodDays" to property.noticePeriodDays,
                "electricityBillingMode" to property.electricityBillingMode,
                "wifiSsid" to property.wifiSsid,
                "wifiPassword" to property.wifiPassword,
                "curfewTime" to property.curfewTime,
                "emergencyContacts" to property.emergencyContacts.map {
                    mapOf("name" to it.name, "role" to it.role, "phone" to it.phone)
                },
                "nearbyLandmarks" to property.nearbyLandmarks,
                "upiId" to property.upiId,
                "createdAt" to FieldValue.serverTimestamp()
            )

            docRef.set(data.filterValues { it != null }).await()

            // Generate unique HMAC PG invite code via Cloud Function
            try {
                generateCode(pgId, property.name, property.contactPhone)
            } catch (_: Exception) {
                // Non-blocking fallback; owner can regenerate from dashboard
            }

            // Append PG to owner's managed list
            firestore.collection("users").document(property.ownerId).update(
                "ownedPgIds", FieldValue.arrayUnion(pgId),
                "activePgId", pgId
            ).await()

            Result.success(pgId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProperty(property: PGProperty): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any?>(
                "name" to property.name,
                "type" to property.type.name,
                "contactPhone" to property.contactPhone,
                "address" to property.address,
                "amenities" to property.amenities,
                "rules" to property.rules,
                "photos" to property.photos,
                "securityDepositRules" to property.securityDepositRules,
                "noticePeriodDays" to property.noticePeriodDays,
                "electricityBillingMode" to property.electricityBillingMode,
                "wifiSsid" to property.wifiSsid,
                "wifiPassword" to property.wifiPassword,
                "curfewTime" to property.curfewTime,
                "upiId" to property.upiId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("pgs").document(property.pgId)
                .update(updates.filterValues { it != null })
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateCode(pgId: String, pgName: String, contactPhone: String): Result<String> {
        return try {
            val result = functions.getHttpsCallable("generatePgCodeForProperty")
                .call(
                    mapOf(
                        "pgId" to pgId,
                        "pgName" to pgName,
                        "contactPhone" to contactPhone
                    )
                ).await()
            val data = result.data as? Map<*, *>
            val code = data?.get("code") as? String ?: throw IllegalStateException("No code returned")
            Result.success(code)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun regenerateCode(pgId: String): Result<String> {
        return try {
            val result = functions.getHttpsCallable("regeneratePgCode")
                .call(mapOf("pgId" to pgId))
                .await()
            val data = result.data as? Map<*, *>
            val code = data?.get("code") as? String ?: throw IllegalStateException("No code returned")
            Result.success(code)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyGst(pgId: String, gstNumber: String): Result<Boolean> {
        return try {
            val result = functions.getHttpsCallable("verifyGst")
                .call(mapOf("pgId" to pgId, "gstNumber" to gstNumber))
                .await()
            val data = result.data as? Map<*, *>
            val isVerified = (data?.get("isChecksumValid") == true) && (data.get("isApiVerified") == true)
            Result.success(isVerified)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeRooms(pgId: String): Flow<List<Room>> = callbackFlow {
        val query = firestore.collection("pgs").document(pgId).collection("rooms")
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val rooms = snapshot?.documents?.mapNotNull { doc ->
                Room(
                    roomId = doc.id,
                    pgId = pgId,
                    roomNumber = doc.getString("roomNumber") ?: "",
                    floor = doc.getLong("floor")?.toInt() ?: 1,
                    sharingType = SharingType.fromString(doc.getString("sharingType")),
                    totalBeds = doc.getLong("totalBeds")?.toInt() ?: 2,
                    rentPerBed = doc.getLong("rentPerBed") ?: 0L,
                    depositPerBed = doc.getLong("depositPerBed") ?: 0L,
                    hasAttachedBathroom = doc.getBoolean("hasAttachedBathroom") ?: true,
                    hasAirConditioner = doc.getBoolean("hasAirConditioner") ?: false,
                    createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                )
            } ?: emptyList()
            trySend(rooms.sortedBy { it.roomNumber })
        }
        awaitClose { registration.remove() }
    }

    override suspend fun createRoomWithBeds(pgId: String, room: Room): Result<String> {
        return try {
            val roomRef = firestore.collection("pgs").document(pgId).collection("rooms").document()
            val roomId = roomRef.id
            val bedsCollection = firestore.collection("pgs").document(pgId).collection("beds")

            val batch = firestore.batch()
            batch.set(
                roomRef,
                mapOf(
                    "roomId" to roomId,
                    "pgId" to pgId,
                    "roomNumber" to room.roomNumber,
                    "floor" to room.floor,
                    "sharingType" to room.sharingType.name,
                    "totalBeds" to room.totalBeds,
                    "rentPerBed" to room.rentPerBed,
                    "depositPerBed" to room.depositPerBed,
                    "hasAttachedBathroom" to room.hasAttachedBathroom,
                    "hasAirConditioner" to room.hasAirConditioner,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            // Individually track each bed (e.g., "101-A", "101-B")
            val bedSuffixes = listOf("A", "B", "C", "D", "E", "F")
            for (i in 0 until room.totalBeds) {
                val bedDoc = bedsCollection.document()
                val suffix = bedSuffixes.getOrElse(i) { "${i + 1}" }
                val identifier = "${room.roomNumber}-$suffix"
                batch.set(
                    bedDoc,
                    mapOf(
                        "bedId" to bedDoc.id,
                        "pgId" to pgId,
                        "roomId" to roomId,
                        "roomNumber" to room.roomNumber,
                        "bedIdentifier" to identifier,
                        "status" to BedStatus.VACANT.name,
                        "currentStudentId" to null,
                        "currentStudentName" to null,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(roomId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeBeds(pgId: String): Flow<List<Bed>> = callbackFlow {
        val query = firestore.collection("pgs").document(pgId).collection("beds")
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val beds = snapshot?.documents?.mapNotNull { doc ->
                Bed(
                    bedId = doc.id,
                    pgId = pgId,
                    roomId = doc.getString("roomId") ?: "",
                    roomNumber = doc.getString("roomNumber") ?: "",
                    bedIdentifier = doc.getString("bedIdentifier") ?: "",
                    status = BedStatus.fromString(doc.getString("status")),
                    currentStudentId = doc.getString("currentStudentId"),
                    currentStudentName = doc.getString("currentStudentName"),
                    noticeEndDate = doc.getTimestamp("noticeEndDate")?.toDate()?.time,
                    updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                )
            } ?: emptyList()
            trySend(beds.sortedBy { it.bedIdentifier })
        }
        awaitClose { registration.remove() }
    }

    override fun observeLiveCapacity(pgId: String): Flow<CapacitySummary> = callbackFlow {
        val query = firestore.collection("pgs").document(pgId).collection("beds")
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(CapacitySummary())
                return@addSnapshotListener
            }
            val total = snapshot.size()
            var booked = 0
            var vacant = 0
            var notice = 0

            for (doc in snapshot.documents) {
                when (doc.getString("status")?.uppercase()) {
                    "BOOKED" -> booked++
                    "NOTICE_PERIOD" -> notice++
                    else -> vacant++
                }
            }
            trySend(
                CapacitySummary(
                    totalBeds = total,
                    bookedBeds = booked,
                    vacantBeds = vacant,
                    noticeBeds = notice
                )
            )
        }
        awaitClose { registration.remove() }
    }

    override fun observeStudents(pgId: String): Flow<List<StudentResident>> = callbackFlow {
        val query = firestore.collection("pgs").document(pgId).collection("students")
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val students = snapshot?.documents?.mapNotNull { doc ->
                StudentResident(
                    studentId = doc.id,
                    pgId = pgId,
                    fullName = doc.getString("fullName") ?: "",
                    phoneNumber = doc.getString("phoneNumber") ?: "",
                    collegeOrWorkplace = doc.getString("collegeOrWorkplace") ?: "",
                    homeCity = doc.getString("homeCity") ?: "",
                    guardianName = doc.getString("guardianName") ?: "",
                    guardianPhone = doc.getString("guardianPhone") ?: "",
                    idProofUrl = doc.getString("idProofUrl") ?: "",
                    roomId = doc.getString("roomId") ?: "",
                    roomNumber = doc.getString("roomNumber") ?: "",
                    bedId = doc.getString("bedId") ?: "",
                    bedIdentifier = doc.getString("bedIdentifier") ?: "",
                    monthlyRent = doc.getLong("monthlyRent") ?: 0L,
                    securityDeposit = doc.getLong("securityDeposit") ?: 0L,
                    checkInDate = doc.getTimestamp("checkInDate")?.toDate()?.time ?: 0L,
                    status = doc.getString("status") ?: "ACTIVE",
                    depositRefunded = doc.getBoolean("depositRefunded") ?: false
                )
            } ?: emptyList()
            trySend(students)
        }
        awaitClose { registration.remove() }
    }

    override fun observeJoinRequests(pgId: String): Flow<List<JoinRequest>> = callbackFlow {
        val query = firestore.collection("pgs").document(pgId).collection("join_requests")
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val requests = snapshot?.documents?.mapNotNull { doc ->
                JoinRequest(
                    requestId = doc.id,
                    pgId = pgId,
                    pgName = doc.getString("pgName") ?: "",
                    studentId = doc.getString("studentId") ?: "",
                    fullName = doc.getString("fullName") ?: "",
                    phoneNumber = doc.getString("phoneNumber") ?: "",
                    age = doc.getLong("age")?.toInt() ?: 0,
                    collegeOrWorkplace = doc.getString("collegeOrWorkplace") ?: "",
                    homeCity = doc.getString("homeCity") ?: "",
                    guardianName = doc.getString("guardianName") ?: "",
                    guardianPhone = doc.getString("guardianPhone") ?: "",
                    idProofUrl = doc.getString("idProofUrl") ?: "",
                    status = JoinRequestStatus.fromString(doc.getString("status")),
                    assignedRoomId = doc.getString("assignedRoomId"),
                    assignedBedId = doc.getString("assignedBedId"),
                    rejectionReason = doc.getString("rejectionReason"),
                    createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                    expiresAt = doc.getTimestamp("expiresAt")?.toDate()?.time ?: 0L,
                    reviewedAt = doc.getTimestamp("reviewedAt")?.toDate()?.time
                )
            } ?: emptyList()
            trySend(requests.sortedByDescending { it.createdAt })
        }
        awaitClose { registration.remove() }
    }

    override suspend fun submitJoinRequest(code: String, studentData: Map<String, Any>): Result<String> {
        return try {
            val result = functions.getHttpsCallable("submitJoinRequest")
                .call(mapOf("code" to code, "studentData" to studentData))
                .await()
            val data = result.data as? Map<*, *>
            val reqId = data?.get("requestId") as? String ?: throw IllegalStateException("Request submission failed")
            Result.success(reqId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reviewJoinRequest(
        pgId: String,
        requestId: String,
        action: String,
        roomId: String?,
        bedId: String?,
        rejectionReason: String?
    ): Result<Unit> {
        return try {
            functions.getHttpsCallable("reviewJoinRequest")
                .call(
                    mapOf(
                        "pgId" to pgId,
                        "requestId" to requestId,
                        "action" to action,
                        "assignedRoomId" to roomId,
                        "assignedBedId" to bedId,
                        "rejectionReason" to rejectionReason
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun shiftStudentRoom(
        pgId: String,
        studentId: String,
        studentName: String,
        oldBedId: String,
        newRoomId: String,
        newRoomNumber: String,
        newBedId: String,
        newBedIdentifier: String
    ): Result<Unit> {
        return try {
            val batch = firestore.batch()
            // Free old bed
            val oldBedRef = firestore.collection("pgs").document(pgId).collection("beds").document(oldBedId)
            batch.update(
                oldBedRef,
                "status", BedStatus.VACANT.name,
                "currentStudentId", null,
                "currentStudentName", null,
                "updatedAt", FieldValue.serverTimestamp()
            )

            // Book new bed
            val newBedRef = firestore.collection("pgs").document(pgId).collection("beds").document(newBedId)
            batch.update(
                newBedRef,
                "status", BedStatus.BOOKED.name,
                "currentStudentId", studentId,
                "currentStudentName", studentName,
                "updatedAt", FieldValue.serverTimestamp()
            )

            // Update student record
            val studentRef = firestore.collection("pgs").document(pgId).collection("students").document(studentId)
            batch.update(
                studentRef,
                "roomId", newRoomId,
                "roomNumber", newRoomNumber,
                "bedId", newBedId,
                "bedIdentifier", newBedIdentifier,
                "updatedAt", FieldValue.serverTimestamp()
            )

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun processStudentCheckOut(
        pgId: String,
        studentId: String,
        bedId: String,
        refundAmount: Long
    ): Result<Unit> {
        return try {
            val batch = firestore.batch()
            // Vacate bed
            val bedRef = firestore.collection("pgs").document(pgId).collection("beds").document(bedId)
            batch.update(
                bedRef,
                "status", BedStatus.VACANT.name,
                "currentStudentId", null,
                "currentStudentName", null,
                "updatedAt", FieldValue.serverTimestamp()
            )

            // Mark student record as checked out
            val studentRef = firestore.collection("pgs").document(pgId).collection("students").document(studentId)
            batch.update(
                studentRef,
                "status", "CHECKED_OUT",
                "checkOutDate", FieldValue.serverTimestamp(),
                "depositRefunded", true,
                "depositRefundAmount", refundAmount
            )

            // Clear student active PG
            val userRef = firestore.collection("users").document(studentId)
            batch.update(userRef, "activePgId", null)

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapDocumentToProperty(id: String, data: Map<String, Any?>): PGProperty {
        val emergencyRaw = (data["emergencyContacts"] as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
        val emergencyContacts = emergencyRaw.map {
            EmergencyContact(
                name = it["name"]?.toString() ?: "",
                role = it["role"]?.toString() ?: "",
                phone = it["phone"]?.toString() ?: ""
            )
        }

        return PGProperty(
            pgId = id,
            ownerId = data["ownerId"]?.toString() ?: "",
            name = data["name"]?.toString() ?: "",
            code = data["code"]?.toString() ?: "",
            codeCreatedAt = (data["codeCreatedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L,
            gstNumber = data["gstNumber"]?.toString(),
            gstVerificationStatus = data["gstVerificationStatus"]?.toString() ?: "NOT_SUBMITTED",
            type = PGType.fromString(data["type"]?.toString()),
            contactPhone = data["contactPhone"]?.toString() ?: "",
            address = data["address"]?.toString() ?: "",
            latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
            longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0,
            amenities = (data["amenities"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            rules = (data["rules"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            photos = (data["photos"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            securityDepositRules = data["securityDepositRules"]?.toString() ?: "One month deposit",
            noticePeriodDays = (data["noticePeriodDays"] as? Number)?.toInt() ?: 30,
            electricityBillingMode = data["electricityBillingMode"]?.toString() ?: "INCLUDED",
            wifiSsid = data["wifiSsid"]?.toString(),
            wifiPassword = data["wifiPassword"]?.toString(),
            curfewTime = data["curfewTime"]?.toString() ?: "22:30",
            emergencyContacts = emergencyContacts,
            nearbyLandmarks = (data["nearbyLandmarks"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            upiId = data["upiId"]?.toString(),
            createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
        )
    }
}
