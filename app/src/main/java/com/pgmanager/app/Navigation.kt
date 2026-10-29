package com.pgmanager.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pgmanager.app.domain.model.AuthState
import com.pgmanager.app.domain.model.UserRole
import com.pgmanager.app.ui.auth.AuthViewModel
import com.pgmanager.app.ui.auth.LoginScreen
import com.pgmanager.app.ui.auth.OtpVerificationScreen
import com.pgmanager.app.ui.auth.RoleSelectionScreen
import com.pgmanager.app.ui.auth.WaitingApprovalScreen
import com.pgmanager.app.ui.legal.LegalDocument
import com.pgmanager.app.ui.legal.LegalScreen
import com.pgmanager.app.ui.owner.OwnerDashboardScreen
import com.pgmanager.app.ui.owner.OwnerViewModel
import com.pgmanager.app.ui.owner.RegisterPgScreen
import com.pgmanager.app.ui.owner.AddRoomScreen
import com.pgmanager.app.ui.student.StudentHomeScreen
import com.pgmanager.app.ui.student.StudentJoinScreen

private enum class EntryRoute {
    LOGIN,
    OTP,
    ROLE,
    TERMS,
    PRIVACY,
    APP
}

@Composable
fun MainNavigation() {
    val container = PGApplication.instance.container
    val authViewModel: AuthViewModel = viewModel {
        AuthViewModel(
            authRepository = container.authRepository,
            observeAuthStateUseCase = container.observeAuthStateUseCase,
            verifyOtpUseCase = container.verifyOtpUseCase,
            verifyCredentialUseCase = container.verifyCredentialUseCase,
            assignRoleUseCase = container.assignRoleUseCase,
            updateProfileUseCase = container.updateProfileUseCase,
            logoutUseCase = container.logoutUseCase,
            deleteAccountUseCase = container.deleteAccountUseCase
        )
    }
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    var route by remember { mutableStateOf(EntryRoute.LOGIN) }
    var demoMode by remember { mutableStateOf(false) }
    var demoRole by remember { mutableStateOf<UserRole?>(null) }

    when (val state = authState) {
        AuthState.Loading -> Unit
        AuthState.Unauthenticated -> {
            BackHandler(enabled = route != EntryRoute.LOGIN) {
                route = when (route) {
                    EntryRoute.OTP,
                    EntryRoute.TERMS,
                    EntryRoute.PRIVACY,
                    EntryRoute.ROLE -> EntryRoute.LOGIN
                    EntryRoute.APP -> EntryRoute.ROLE
                    EntryRoute.LOGIN -> EntryRoute.LOGIN
                }
            }
            when (route) {
                EntryRoute.LOGIN -> LoginScreen(
                    viewModel = authViewModel,
                    onCodeSent = { route = EntryRoute.OTP },
                    onNavigateToTerms = { route = EntryRoute.TERMS },
                    onNavigateToPrivacy = { route = EntryRoute.PRIVACY },
                    onSkipLogin = {
                        demoMode = true
                        route = EntryRoute.ROLE
                    }
                )
                EntryRoute.OTP -> OtpVerificationScreen(
                    viewModel = authViewModel,
                    onVerificationSuccess = {
                        route = EntryRoute.ROLE
                    },
                    onNavigateBack = { route = EntryRoute.LOGIN }
                )
                EntryRoute.TERMS -> LegalScreen(
                    document = LegalDocument.TERMS,
                    onNavigateBack = { route = EntryRoute.LOGIN }
                )
                EntryRoute.PRIVACY -> LegalScreen(
                    document = LegalDocument.PRIVACY,
                    onNavigateBack = { route = EntryRoute.LOGIN }
                )
                EntryRoute.ROLE -> RoleSelectionScreen(
                    viewModel = authViewModel,
                    demoMode = demoMode,
                    onRoleConfirmed = { role ->
                        demoRole = role
                        route = EntryRoute.APP
                    }
                )
                EntryRoute.APP -> when (demoRole) {
                    UserRole.OWNER -> OwnerApp(
                        ownerId = "demo-owner",
                        authViewModel = authViewModel,
                        container = container
                    )
                    UserRole.STUDENT -> {
                        BackHandler {
                            demoRole = null
                            route = EntryRoute.ROLE
                        }
                        StudentJoinScreen(
                            viewModel = viewModel(key = "demo-student") {
                                com.pgmanager.app.ui.student.StudentViewModel(
                                    pgRepository = container.pgRepository,
                                    operationsRepository = container.operationsRepository,
                                    studentId = "demo-student",
                                    initialPgId = null
                                )
                            },
                            onLogout = {
                                demoRole = null
                                demoMode = false
                                route = EntryRoute.LOGIN
                            }
                        )
                    }
                    else -> {
                        route = EntryRoute.ROLE
                    }
                }
                else -> {
                    route = EntryRoute.LOGIN
                }
            }
        }
        is AuthState.Authenticated -> {
            val user = state.user
            when {
                user.role == UserRole.UNASSIGNED -> RoleSelectionScreen(
                    viewModel = authViewModel,
                    onRoleConfirmed = { route = EntryRoute.APP }
                )
                user.role == UserRole.OWNER -> OwnerApp(
                    ownerId = user.userId,
                    authViewModel = authViewModel,
                    container = container
                )
                user.role == UserRole.STUDENT && user.activePgId != null -> StudentHomeScreen(
                    viewModel = viewModel(key = user.userId) {
                        com.pgmanager.app.ui.student.StudentViewModel(
                            pgRepository = container.pgRepository,
                            operationsRepository = container.operationsRepository,
                            studentId = user.userId,
                            initialPgId = user.activePgId
                        )
                    },
                    studentName = user.fullName,
                    onLogout = { authViewModel.logout { } }
                )
                else -> StudentJoinScreen(
                    viewModel = viewModel(key = user.userId) {
                        com.pgmanager.app.ui.student.StudentViewModel(
                            pgRepository = container.pgRepository,
                            operationsRepository = container.operationsRepository,
                            studentId = user.userId,
                            initialPgId = user.activePgId
                        )
                    },
                    onLogout = { authViewModel.logout { } }
                )
            }
        }
        is AuthState.AwaitingApproval -> WaitingApprovalScreen(
            pgName = state.pgName,
            onRefreshStatus = { },
            onLogout = { authViewModel.logout { } }
        )
    }
}

@Composable
private fun OwnerApp(
    ownerId: String,
    authViewModel: AuthViewModel,
    container: com.pgmanager.app.di.AppContainer
) {
    var showRegister by remember { mutableStateOf(false) }
    var addRoomForPg by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = showRegister || addRoomForPg != null) {
        if (addRoomForPg != null) {
            addRoomForPg = null
        } else {
            showRegister = false
        }
    }
    val ownerViewModel: OwnerViewModel = viewModel(key = ownerId) {
        OwnerViewModel(
            pgRepository = container.pgRepository,
            operationsRepository = container.operationsRepository,
            ownerId = ownerId
        )
    }

    when {
        showRegister -> RegisterPgScreen(
            viewModel = ownerViewModel,
            onPgRegistered = { showRegister = false },
            onNavigateBack = { showRegister = false }
        )
        addRoomForPg != null -> AddRoomScreen(
            viewModel = ownerViewModel,
            pgId = addRoomForPg!!,
            onRoomCreated = { addRoomForPg = null },
            onNavigateBack = { addRoomForPg = null }
        )
        else -> OwnerDashboardScreen(
            viewModel = ownerViewModel,
            onNavigateToCreatePg = { showRegister = true },
            onNavigateToAddRoom = { addRoomForPg = it },
            onLogout = { authViewModel.logout { } }
        )
    }
}
