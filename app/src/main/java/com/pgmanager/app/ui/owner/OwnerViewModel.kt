package com.pgmanager.app.ui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pgmanager.app.domain.model.Bed
import com.pgmanager.app.domain.model.CapacitySummary
import com.pgmanager.app.domain.model.CleaningSlot
import com.pgmanager.app.domain.model.DailyMealPlan
import com.pgmanager.app.domain.model.FinancialReport
import com.pgmanager.app.domain.model.GatePass
import com.pgmanager.app.domain.model.JoinRequest
import com.pgmanager.app.domain.model.MaintenanceTicket
import com.pgmanager.app.domain.model.Notice
import com.pgmanager.app.domain.model.NoticeCategory
import com.pgmanager.app.domain.model.PGProperty
import com.pgmanager.app.domain.model.PGType
import com.pgmanager.app.domain.model.RentDue
import com.pgmanager.app.domain.model.Room
import com.pgmanager.app.domain.model.SharingType
import com.pgmanager.app.domain.model.StudentResident
import com.pgmanager.app.domain.model.TicketStatus
import com.pgmanager.app.domain.repository.OperationsRepository
import com.pgmanager.app.domain.repository.PGRepository
import com.pgmanager.app.security.SecurityGuard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class CreatePgFormState(
    val name: String = "",
    val gstNumber: String = "",
    val contactPhone: String = "",
    val address: String = "",
    val type: PGType = PGType.CO_LIVING,
    val amenities: List<String> = listOf("High-Speed Wi-Fi", "RO Drinking Water", "Daily Housekeeping", "CCTV Security"),
    val rules: List<String> = listOf("Curfew at 10:30 PM", "No smoking or alcohol on premises", "Visitors restricted to common lounge"),
    val photos: List<String> = emptyList(),
    val curfewTime: String = "22:30",
    val upiId: String = "",
    val isGstValidFormat: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class CreateRoomFormState(
    val roomNumber: String = "",
    val floor: Int = 1,
    val sharingType: SharingType = SharingType.DOUBLE,
    val rentPerBed: String = "",
    val depositPerBed: String = "",
    val hasAttachedBathroom: Boolean = true,
    val hasAirConditioner: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerViewModel(
    private val pgRepository: PGRepository,
    private val operationsRepository: OperationsRepository,
    val ownerId: String
) : ViewModel() {

    // Properties list for this owner
    val properties: StateFlow<List<PGProperty>> = pgRepository.observeOwnerProperties(ownerId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _activePgId = MutableStateFlow<String?>(null)
    val activePgId: StateFlow<String?> = _activePgId.asStateFlow()

    // Form states
    private val _createPgState = MutableStateFlow(CreatePgFormState())
    val createPgState: StateFlow<CreatePgFormState> = _createPgState.asStateFlow()

    private val _createRoomState = MutableStateFlow(CreateRoomFormState())
    val createRoomState: StateFlow<CreateRoomFormState> = _createRoomState.asStateFlow()

    // Live capacity derived strictly from bed records
    val capacity: StateFlow<CapacitySummary> = _activePgId.flatMapLatest { id ->
        if (id != null) pgRepository.observeLiveCapacity(id) else flowOf(CapacitySummary())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CapacitySummary())

    // Rooms & Beds
    val rooms: StateFlow<List<Room>> = _activePgId.flatMapLatest { id ->
        if (id != null) pgRepository.observeRooms(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val beds: StateFlow<List<Bed>> = _activePgId.flatMapLatest { id ->
        if (id != null) pgRepository.observeBeds(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Students
    val students: StateFlow<List<StudentResident>> = _activePgId.flatMapLatest { id ->
        if (id != null) pgRepository.observeStudents(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Join Requests
    val joinRequests: StateFlow<List<JoinRequest>> = _activePgId.flatMapLatest { id ->
        if (id != null) pgRepository.observeJoinRequests(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notices
    val notices: StateFlow<List<Notice>> = _activePgId.flatMapLatest { id ->
        if (id != null) operationsRepository.observeNotices(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tickets
    val tickets: StateFlow<List<MaintenanceTicket>> = _activePgId.flatMapLatest { id ->
        if (id != null) operationsRepository.observeTickets(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dues & Financial Report
    val dues: StateFlow<List<RentDue>> = _activePgId.flatMapLatest { id ->
        if (id != null) operationsRepository.observeDues(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val financialReport: StateFlow<FinancialReport> = _activePgId.flatMapLatest { id ->
        if (id != null) operationsRepository.observeFinancialReport(id) else flowOf(FinancialReport())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialReport())

    // Cleaning Schedule
    val cleaningSlots: StateFlow<List<CleaningSlot>> = _activePgId.flatMapLatest { id ->
        if (id != null) operationsRepository.observeCleaningSchedule(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Gate Passes
    val gatePasses: StateFlow<List<GatePass>> = _activePgId.flatMapLatest { id ->
        if (id != null) operationsRepository.observeGatePasses(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectActivePg(pgId: String) {
        _activePgId.value = pgId
    }

    // --- PG REGISTRATION ---
    fun onPgNameChanged(name: String) {
        _createPgState.update { it.copy(name = name, errorMessage = null) }
    }

    fun onGstNumberChanged(gst: String) {
        val clean = gst.uppercase().filter { it.isLetterOrDigit() }.take(15)
        val isValid = SecurityGuard.validateGstNumber(clean)
        _createPgState.update { it.copy(gstNumber = clean, isGstValidFormat = isValid, errorMessage = null) }
    }

    fun onContactPhoneChanged(phone: String) {
        val clean = phone.filter { it.isDigit() }.take(10)
        _createPgState.update { it.copy(contactPhone = clean, errorMessage = null) }
    }

    fun onAddressChanged(address: String) {
        _createPgState.update { it.copy(address = address, errorMessage = null) }
    }

    fun onPgTypeChanged(type: PGType) {
        _createPgState.update { it.copy(type = type) }
    }

    fun onUpiIdChanged(upi: String) {
        _createPgState.update { it.copy(upiId = upi) }
    }

    fun registerPg(onSuccess: (String) -> Unit) {
        val form = _createPgState.value
        if (form.name.isBlank()) {
            _createPgState.update { it.copy(errorMessage = "PG Name is required") }
            return
        }
        if (form.contactPhone.length != 10) {
            _createPgState.update { it.copy(errorMessage = "Enter a valid 10-digit contact number") }
            return
        }
        if (form.address.isBlank()) {
            _createPgState.update { it.copy(errorMessage = "Address is required") }
            return
        }
        if (form.gstNumber.isNotBlank() && !form.isGstValidFormat) {
            _createPgState.update { it.copy(errorMessage = "Invalid 15-character GST format or checksum failure") }
            return
        }

        _createPgState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val property = PGProperty(
                ownerId = ownerId,
                name = SecurityGuard.sanitizeInput(form.name),
                gstNumber = form.gstNumber.ifBlank { null },
                gstVerificationStatus = if (form.gstNumber.isBlank()) "NOT_SUBMITTED" else "PENDING",
                type = form.type,
                contactPhone = "+91${form.contactPhone}",
                address = SecurityGuard.sanitizeInput(form.address),
                amenities = form.amenities,
                rules = form.rules,
                curfewTime = form.curfewTime,
                upiId = form.upiId.ifBlank { null }
            )

            val result = pgRepository.createProperty(property)
            result.onSuccess { pgId ->
                _createPgState.update { CreatePgFormState() }
                _activePgId.value = pgId
                onSuccess(pgId)
            }.onFailure { error ->
                _createPgState.update {
                    it.copy(isLoading = false, errorMessage = error.localizedMessage ?: "Failed to create PG")
                }
            }
        }
    }

    // --- REGENERATE CODE ---
    fun regenerateCode(pgId: String, onNewCode: (String) -> Unit) {
        viewModelScope.launch {
            pgRepository.regenerateCode(pgId).onSuccess(onNewCode)
        }
    }

    // --- ROOM CREATION ---
    fun onRoomNumberChanged(num: String) {
        _createRoomState.update { it.copy(roomNumber = num, errorMessage = null) }
    }

    fun onFloorChanged(floor: Int) {
        _createRoomState.update { it.copy(floor = floor) }
    }

    fun onSharingTypeChanged(sharing: SharingType) {
        _createRoomState.update { it.copy(sharingType = sharing) }
    }

    fun onRentPerBedChanged(rent: String) {
        _createRoomState.update { it.copy(rentPerBed = rent.filter { c -> c.isDigit() }) }
    }

    fun onDepositPerBedChanged(deposit: String) {
        _createRoomState.update { it.copy(depositPerBed = deposit.filter { c -> c.isDigit() }) }
    }

    fun onAttachedBathroomChanged(attached: Boolean) {
        _createRoomState.update { it.copy(hasAttachedBathroom = attached) }
    }

    fun onAirConditionerChanged(ac: Boolean) {
        _createRoomState.update { it.copy(hasAirConditioner = ac) }
    }

    fun createRoom(pgId: String, onSuccess: () -> Unit) {
        val form = _createRoomState.value
        if (form.roomNumber.isBlank()) {
            _createRoomState.update { it.copy(errorMessage = "Room number is required") }
            return
        }
        val rent = form.rentPerBed.toLongOrNull() ?: 0L
        if (rent <= 0) {
            _createRoomState.update { it.copy(errorMessage = "Enter valid rent per bed") }
            return
        }
        val deposit = form.depositPerBed.toLongOrNull() ?: 0L

        _createRoomState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val room = Room(
                pgId = pgId,
                roomNumber = form.roomNumber.trim().uppercase(),
                floor = form.floor,
                sharingType = form.sharingType,
                totalBeds = form.sharingType.bedCount,
                rentPerBed = rent,
                depositPerBed = deposit,
                hasAttachedBathroom = form.hasAttachedBathroom,
                hasAirConditioner = form.hasAirConditioner
            )
            val result = pgRepository.createRoomWithBeds(pgId, room)
            result.onSuccess {
                _createRoomState.update { CreateRoomFormState() }
                onSuccess()
            }.onFailure { error ->
                _createRoomState.update {
                    it.copy(isLoading = false, errorMessage = error.localizedMessage ?: "Failed to add room")
                }
            }
        }
    }

    // --- JOIN REQUEST REVIEW ---
    fun approveJoinRequest(
        pgId: String,
        requestId: String,
        roomId: String,
        bedId: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            pgRepository.reviewJoinRequest(
                pgId = pgId,
                requestId = requestId,
                action = "APPROVE",
                roomId = roomId,
                bedId = bedId,
                rejectionReason = null
            ).onSuccess { onSuccess() }
        }
    }

    fun rejectJoinRequest(
        pgId: String,
        requestId: String,
        reason: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            pgRepository.reviewJoinRequest(
                pgId = pgId,
                requestId = requestId,
                action = "REJECT",
                roomId = null,
                bedId = null,
                rejectionReason = reason
            ).onSuccess { onSuccess() }
        }
    }

    // --- OPERATIONS: NOTICES, TICKETS, DUES ---
    fun postNotice(pgId: String, title: String, content: String, category: NoticeCategory, isPinned: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val notice = Notice(
                pgId = pgId,
                title = SecurityGuard.sanitizeInput(title),
                content = SecurityGuard.sanitizeInput(content, 2000),
                category = category,
                isPinned = isPinned,
                authorId = ownerId
            )
            operationsRepository.createNotice(pgId, notice).onSuccess { onSuccess() }
        }
    }

    fun updateTicket(pgId: String, ticketId: String, status: TicketStatus, notes: String?) {
        viewModelScope.launch {
            operationsRepository.updateTicketStatus(pgId, ticketId, status, notes)
        }
    }

    fun generateMonthlyDues(pgId: String, month: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            operationsRepository.generateDuesForActiveStudents(pgId, month).onSuccess { onSuccess() }
        }
    }

    fun recordPayment(pgId: String, dueId: String, amount: Long, mode: String, refNumber: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            operationsRepository.recordPayment(pgId, dueId, amount, mode, refNumber).onSuccess { onSuccess() }
        }
    }

    fun saveMealPlan(pgId: String, plan: DailyMealPlan, onSuccess: () -> Unit) {
        viewModelScope.launch {
            operationsRepository.saveMealPlan(pgId, plan).onSuccess { onSuccess() }
        }
    }

    fun addCleaningSlot(pgId: String, area: String, staff: String, date: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val slot = CleaningSlot(
                pgId = pgId,
                areaName = area,
                assignedStaff = staff,
                scheduledDate = date
            )
            operationsRepository.addCleaningSlot(pgId, slot).onSuccess { onSuccess() }
        }
    }

    fun updateGatePass(pgId: String, passId: String, status: String) {
        viewModelScope.launch {
            operationsRepository.updateGatePassStatus(pgId, passId, status)
        }
    }

    fun clearErrors() {
        _createPgState.update { it.copy(errorMessage = null) }
        _createRoomState.update { it.copy(errorMessage = null) }
    }
}
