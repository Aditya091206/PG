package com.pgmanager.app.ui.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class LegalDocument(val title: String) {
    TERMS("Terms of Service"),
    PRIVACY("Privacy Policy")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(
    document: LegalDocument,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(document.title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (document) {
                LegalDocument.TERMS -> {
                    LegalSection(
                        heading = "Using PG Manager",
                        body = "PG Manager helps property owners manage accommodation operations and helps residents request access to a property. You must provide accurate information and keep your account secure."
                    )
                    LegalSection(
                        heading = "Owner and resident responsibilities",
                        body = "Owners are responsible for their property information, approvals, charges, and notices. Residents may access property information only after approval and must follow the property rules."
                    )
                    LegalSection(
                        heading = "Account termination",
                        body = "You may stop using the service or request account deletion from account settings. Access may be suspended when information is fraudulent, unsafe, or used to bypass property controls."
                    )
                }
                LegalDocument.PRIVACY -> {
                    LegalSection(
                        heading = "Information collected",
                        body = "The service may collect your phone number, profile details, identity documents, property information, photos, location details, payment references, and device notification token."
                    )
                    LegalSection(
                        heading = "Why it is used",
                        body = "Information is used for phone authentication, property access approval, accommodation operations, support, notifications, payments, fraud prevention, and legal compliance."
                    )
                    LegalSection(
                        heading = "Storage and sharing",
                        body = "Data is stored using Firebase services configured by the property operator. Identity and payment documents are private and should be accessed only for the operational purpose for which they were submitted. Service providers process data under their own terms."
                    )
                    LegalSection(
                        heading = "Your choices",
                        body = "You can request correction or deletion of your account and data, subject to records that must be retained for legal or financial obligations. Contact the property operator or the support contact shown in the app."
                    )
                }
            }
        }
    }
}

@Composable
private fun LegalSection(heading: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(heading, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}
