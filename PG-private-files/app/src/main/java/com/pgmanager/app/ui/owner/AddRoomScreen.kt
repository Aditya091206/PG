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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import com.pgmanager.app.domain.model.SharingType
import com.pgmanager.app.theme.ButtonShape
import com.pgmanager.app.theme.ChipShape
import com.pgmanager.app.theme.InputFieldShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRoomScreen(
    viewModel: OwnerViewModel,
    pgId: String,
    onRoomCreated: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.createRoomState.collectAsStateWithLifecycle()
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
                title = { Text("Add Room & Beds") },
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
                value = state.roomNumber,
                onValueChange = viewModel::onRoomNumberChanged,
                label = { Text("Room Identifier / Number *") },
                placeholder = { Text("e.g. 101, 204") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = InputFieldShape
            )

            Text(
                text = "Sharing Configuration (Bed Count)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SharingType.entries.forEach { type ->
                    FilterChip(
                        selected = state.sharingType == type,
                        onClick = { viewModel.onSharingTypeChanged(type) },
                        label = { Text("${type.name} (${type.bedCount} Beds)") },
                        shape = ChipShape
                    )
                }
            }

            OutlinedTextField(
                value = state.rentPerBed,
                onValueChange = viewModel::onRentPerBedChanged,
                label = { Text("Monthly Rent per Bed (INR) *") },
                placeholder = { Text("8500") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = InputFieldShape
            )

            OutlinedTextField(
                value = state.depositPerBed,
                onValueChange = viewModel::onDepositPerBedChanged,
                label = { Text("Security Deposit per Bed (INR)") },
                placeholder = { Text("15000") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = InputFieldShape
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Attached Bathroom")
                Switch(
                    checked = state.hasAttachedBathroom,
                    onCheckedChange = viewModel::onAttachedBathroomChanged
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Air Conditioned (AC)")
                Switch(
                    checked = state.hasAirConditioner,
                    onCheckedChange = viewModel::onAirConditionerChanged
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.createRoom(pgId, onRoomCreated) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = state.roomNumber.isNotBlank() &&
                        state.rentPerBed.isNotBlank() &&
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
                    Text("Provision Room and Individual Beds", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
