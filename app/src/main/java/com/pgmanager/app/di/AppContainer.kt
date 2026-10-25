package com.pgmanager.app.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.pgmanager.app.data.repository.AuthRepositoryImpl
import com.pgmanager.app.domain.repository.AuthRepository
import com.pgmanager.app.domain.usecase.AssignRoleUseCase
import com.pgmanager.app.domain.usecase.DeleteAccountUseCase
import com.pgmanager.app.domain.usecase.LogoutUseCase
import com.pgmanager.app.domain.usecase.ObserveAuthStateUseCase
import com.pgmanager.app.domain.usecase.UpdateProfileUseCase
import com.pgmanager.app.domain.usecase.VerifyOtpUseCase
import com.pgmanager.app.security.EncryptedSessionStore

class AppContainer(val context: Context) {

    val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val functions: FirebaseFunctions by lazy { FirebaseFunctions.getInstance() }
    val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    val sessionStore: EncryptedSessionStore by lazy {
        EncryptedSessionStore(context)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(
            context = context,
            auth = firebaseAuth,
            firestore = firestore,
            functions = functions,
            sessionStore = sessionStore
        )
    }

    val pgRepository: com.pgmanager.app.domain.repository.PGRepository by lazy {
        com.pgmanager.app.data.repository.PGRepositoryImpl(
            firestore = firestore,
            functions = functions
        )
    }

    val operationsRepository: com.pgmanager.app.domain.repository.OperationsRepository by lazy {
        com.pgmanager.app.data.repository.OperationsRepositoryImpl(
            firestore = firestore
        )
    }

    val observeAuthStateUseCase: ObserveAuthStateUseCase by lazy {
        ObserveAuthStateUseCase(authRepository)
    }

    val verifyOtpUseCase: VerifyOtpUseCase by lazy {
        VerifyOtpUseCase(authRepository)
    }

    val assignRoleUseCase: AssignRoleUseCase by lazy {
        AssignRoleUseCase(authRepository)
    }

    val updateProfileUseCase: UpdateProfileUseCase by lazy {
        UpdateProfileUseCase(authRepository)
    }

    val logoutUseCase: LogoutUseCase by lazy {
        LogoutUseCase(authRepository)
    }

    val deleteAccountUseCase: DeleteAccountUseCase by lazy {
        DeleteAccountUseCase(authRepository)
    }
}
