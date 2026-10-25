package com.pgmanager.app.ui.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pgmanager.app.domain.model.PGType
import com.pgmanager.app.theme.ButtonShape
import com.pgmanager.app.theme.ChipShape
import com.pgmanager.app.theme.InputFieldShape
import com.pgmanager.app.theme.StatusError
import com.pgmanager.app.theme.StatusSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterPgScreen(
    viewModel: OwnerViewModel,
    onPgRegistered: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.createPgState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearErrors()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Register New PG Accommodation") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onPgNameChanged,
                label = { Text("Property Name *") },
                placeholder = { Text("e.g. Sunrise Executive Residence") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = InputFieldShape
            )

            // PG Accommodation Type
            Text(
                text = "Accommodation Type",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PGType.entries.forEach { type ->
                    FilterChip(
                        selected = state.type == type,
                        onClick = { viewModel.onPgTypeChanged(type) },
                        label = { Text(type.name.replace("_", " ")) },
                        shape = ChipShape
                    )
                }
            }

            // GST Number with validation
            OutlinedTextField(
                value = state.gstNumber,
                onValueChange = viewModel::onGstNumberChanged,
                label = { Text("GSTIN Number (Optional)") },
                placeholder = { Text("e.g. 29AAAAA0000A1Z5") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = InputFieldShape,
                trailingIcon = {
                    if (state.gstNumber.isNotEmpty()) {
                        if (state.isGstValidFormat) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Valid GST Format",
                                tint = StatusSuccess
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Invalid GST Format",
                                tint = StatusError
                            )
                        }
                    }
                },
                supportingText = {
                    if (state.gstNumber.isNotEmpty() && !state.isGstValidFormat) {
                        Text("Invalid 15-character GST checksum format", color = StatusError)
                    } else if (state.isGstValidFormat) {
                        Text("Checksum verified. Will be verified via GST gateway upon saving.", color = StatusSuccess)
                    }
                }
            )

            OutlinedTextField(
                value = state.contactPhone,
                onValueChange = viewModel::onContactPhoneChanged,
                label = { Text("Public Contact Mobile *") },
                placeholder = { Text("9876543210") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = InputFieldShape
            )

            OutlinedTextField(
                value = state.address,
                onValueChange = viewModel::onAddressChanged,
                label = { Text("Address & Landmark *") },
                placeholder = { Text("12th Main Road, Indiranagar, Bengaluru") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = InputFieldShape
            )

            OutlinedTextField(
                value = state.upiId,
                onValueChange = viewModel::onUpiIdChanged,
                label = { Text("UPI ID for Rent Collection") },
                placeholder = { Text("e.g. sunrisepg@okaxis") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = InputFieldShape
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.registerPg(onPgRegistered) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = state.name.isNotBlank() &&
                        state.contactPhone.length == 10 &&
                        state.address.isNotBlank() &&
                        (state.gstNumber.isBlank() || state.isGstValidFormat) &&
                        !state.isLoading,
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Register Accommodation & Generate Code", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
