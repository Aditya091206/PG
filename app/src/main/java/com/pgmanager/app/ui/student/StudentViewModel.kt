package com.pgmanager.app.ui.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pgmanager.app.domain.model.DailyMealPlan
import com.pgmanager.app.domain.model.GatePass
import com.pgmanager.app.domain.model.MaintenanceTicket
import com.pgmanager.app.domain.model.MealFeedback
import com.pgmanager.app.domain.model.Notice
import com.pgmanager.app.domain.model.PGProperty
import com.pgmanager.app.domain.model.RentDue
import com.pgmanager.app.domain.model.StudentResident
import com.pgmanager.app.domain.model.TicketCategory
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class JoinPgFormState(
    val code: String = "",
    val fullName: String = "",
    val age: String = "",
    val collegeOrWorkplace: String = "",
    val homeCity: String = "",
    val guardianName: String = "",
    val guardianPhone: String = "",
    val idProofUrl: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class CreateTicketFormState(
    val category: TicketCategory = TicketCategory.OTHER,
    val title: String = "",
    val description: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class StudentViewModel(
    private val pgRepository: PGRepository,
    private val operationsRepository: OperationsRepository,
    val studentId: String,
    val initialPgId: String?
) : ViewModel() {

    private val _activePgId = MutableStateFlow(initialPgId)
    val activePgId: StateFlow<String?> = _activePgId.asStateFlow()

    private val _joinFormState = MutableStateFlow(JoinPgFormState())
    val joinFormState: StateFlow<JoinPgFormState> = _joinFormState.asStateFlow()

    private val _createTicketState = MutableStateFlow(CreateTicketFormState())
    val createTicketState: StateFlow<CreateTicketFormState> = _createTicketState.asStateFlow()

    val property: StateFlow<PGProperty?> = _activePgId.flatMapLatest { id ->
        if (id != null) pgRepository.observeProperty(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayDateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val todayMeals: StateFlow<DailyMealPlan?> = _activePgId.flatMapLatest { id ->
        if (id != null) operationsRepository.observeDailyMeal(id, todayDateString) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val notices: StateFlow<List<Notice>> = _activePgId.flatMapLatest { id ->
        if (id != null) operationsRepository.observeNotices(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myTickets: StateFlow<List<MaintenanceTicket>> = _activePgId.flatMapLatest { id ->
        if (id != null) operationsRepository.observeTickets(id, studentId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myDues: StateFlow<List<RentDue>> = _activePgId.flatMapLatest { id ->
        if (id != null) operationsRepository.observeDues(id, studentId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myGatePasses: StateFlow<List<GatePass>> = _activePgId.flatMapLatest { id ->
        if (id != null) operationsRepository.observeGatePasses(id, studentId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- JOIN PG FLOW ---
    fun onCodeChanged(code: String) {
        val clean = code.uppercase().filter { it.isLetterOrDigit() || it == '-' }.take(20)
        _joinFormState.update { it.copy(code = clean, errorMessage = null) }
    }

    fun onFullNameChanged(name: String) {
        _joinFormState.update { it.copy(fullName = name, errorMessage = null) }
    }

    fun onAgeChanged(age: String) {
        _joinFormState.update { it.copy(age = age.filter { it.isDigit() }.take(2)) }
    }

    fun onCollegeChanged(college: String) {
        _joinFormState.update { it.copy(collegeOrWorkplace = college) }
    }

    fun onHomeCityChanged(city: String) {
        _joinFormState.update { it.copy(homeCity = city) }
    }

    fun onGuardianNameChanged(name: String) {
        _joinFormState.update { it.copy(guardianName = name) }
    }

    fun onGuardianPhoneChanged(phone: String) {
        val clean = phone.filter { it.isDigit() }.take(10)
        _joinFormState.update { it.copy(guardianPhone = clean) }
    }

    fun submitJoinRequest(onSubmitted: (String) -> Unit) {
        val form = _joinFormState.value
        if (form.code.isBlank()) {
            _joinFormState.update { it.copy(errorMessage = "Enter PG Invite Code") }
            return
        }
        if (form.fullName.isBlank()) {
            _joinFormState.update { it.copy(errorMessage = "Full Name is required") }
            return
        }
        if (form.guardianPhone.isNotBlank() && form.guardianPhone.length != 10) {
            _joinFormState.update { it.copy(errorMessage = "Guardian mobile number must be 10 digits") }
            return
        }

        _joinFormState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val studentData = mapOf(
                "fullName" to SecurityGuard.sanitizeInput(form.fullName),
                "age" to (form.age.toIntOrNull() ?: 20),
                "collegeOrWorkplace" to SecurityGuard.sanitizeInput(form.collegeOrWorkplace),
                "homeCity" to SecurityGuard.sanitizeInput(form.homeCity),
                "guardianName" to SecurityGuard.sanitizeInput(form.guardianName),
                "guardianPhone" to if (form.guardianPhone.isNotBlank()) "+91${form.guardianPhone}" else "",
                "idProofUrl" to form.idProofUrl
            )
            val result = pgRepository.submitJoinRequest(form.code.trim(), studentData)
            result.onSuccess { reqId ->
                _joinFormState.update { it.copy(isLoading = false) }
                onSubmitted(reqId)
            }.onFailure { error ->
                _joinFormState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to submit join request. Check code."
                    )
                }
            }
        }
    }

    // --- MEAL ACTIONS ---
    fun toggleMealOptOut(optOut: Boolean) {
        val pgId = _activePgId.value ?: return
        viewModelScope.launch {
            operationsRepository.toggleMealOptOut(pgId, todayDateString, studentId, optOut)
        }
    }

    fun submitMealFeedback(rating: Int, comment: String, mealType: String) {
        val pgId = _activePgId.value ?: return
        viewModelScope.launch {
            val fb = MealFeedback(
                studentId = studentId,
                rating = rating,
                comment = SecurityGuard.sanitizeInput(comment),
                mealType = mealType
            )
            operationsRepository.addMealFeedback(pgId, todayDateString, fb)
        }
    }

    // --- TICKET CREATION ---
    fun onTicketCategoryChanged(cat: TicketCategory) {
        _createTicketState.update { it.copy(category = cat) }
    }

    fun onTicketTitleChanged(title: String) {
        _createTicketState.update { it.copy(title = title, errorMessage = null) }
    }

    fun onTicketDescriptionChanged(desc: String) {
        _createTicketState.update { it.copy(description = desc, errorMessage = null) }
    }

    fun submitTicket(studentName: String, roomNumber: String, onSuccess: () -> Unit) {
        val form = _createTicketState.value
        val pgId = _activePgId.value ?: return
        if (form.title.isBlank()) {
            _createTicketState.update { it.copy(errorMessage = "Issue title is required") }
            return
        }

        _createTicketState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val ticket = MaintenanceTicket(
                pgId = pgId,
                studentId = studentId,
                studentName = studentName,
                roomNumber = roomNumber,
                category = form.category,
                title = SecurityGuard.sanitizeInput(form.title),
                description = SecurityGuard.sanitizeInput(form.description, 1000)
            )
            val result = operationsRepository.createTicket(pgId, ticket)
            result.onSuccess {
                _createTicketState.update { CreateTicketFormState() }
                onSuccess()
            }.onFailure { error ->
                _createTicketState.update {
                    it.copy(isLoading = false, errorMessage = error.localizedMessage ?: "Failed to create ticket")
                }
            }
        }
    }

    // --- GATE PASS REQUEST ---
    fun submitGatePass(purpose: String, outTime: String, expectedInTime: String, roomNumber: String, studentName: String, onSuccess: () -> Unit) {
        val pgId = _activePgId.value ?: return
        viewModelScope.launch {
            val pass = GatePass(
                pgId = pgId,
                studentId = studentId,
                studentName = studentName,
                roomNumber = roomNumber,
                purpose = SecurityGuard.sanitizeInput(purpose),
                outTime = outTime,
                expectedInTime = expectedInTime
            )
            operationsRepository.requestGatePass(pgId, pass).onSuccess { onSuccess() }
        }
    }

    fun clearErrors() {
        _joinFormState.update { it.copy(errorMessage = null) }
        _createTicketState.update { it.copy(errorMessage = null) }
    }
}
