package com.pgmanager.app.ui.owner

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pgmanager.app.domain.model.Bed
import com.pgmanager.app.domain.model.BedStatus
import com.pgmanager.app.domain.model.DueStatus
import com.pgmanager.app.domain.model.JoinRequest
import com.pgmanager.app.domain.model.NoticeCategory
import com.pgmanager.app.domain.model.PGProperty
import com.pgmanager.app.domain.model.TicketStatus
import com.pgmanager.app.theme.BedBookedColor
import com.pgmanager.app.theme.BedNoticeColor
import com.pgmanager.app.theme.BedVacantColor
import com.pgmanager.app.theme.ButtonShape
import com.pgmanager.app.theme.CardShape
import com.pgmanager.app.theme.ChipShape
import com.pgmanager.app.theme.InputFieldShape
import com.pgmanager.app.theme.StatusError
import com.pgmanager.app.theme.StatusSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun OwnerDashboardScreen(
    viewModel: OwnerViewModel,
    onNavigateToCreatePg: () -> Unit,
    onNavigateToAddRoom: (String) -> Unit,
    onLogout: () -> Unit
) {
    val properties by viewModel.properties.collectAsStateWithLifecycle()
    val activePgId by viewModel.activePgId.collectAsStateWithLifecycle()
    val capacity by viewModel.capacity.collectAsStateWithLifecycle()
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val beds by viewModel.beds.collectAsStateWithLifecycle()
    val joinRequests by viewModel.joinRequests.collectAsStateWithLifecycle()
    val notices by viewModel.notices.collectAsStateWithLifecycle()
    val tickets by viewModel.tickets.collectAsStateWithLifecycle()
    val dues by viewModel.dues.collectAsStateWithLifecycle()
    val financialReport by viewModel.financialReport.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showPropertyMenu by remember { mutableStateOf(false) }

    // Dialog state for approving student join request
    var selectedRequestToApprove by remember { mutableStateOf<JoinRequest?>(null) }
    var selectedBedForApproval by remember { mutableStateOf<Bed?>(null) }
    var showNewNoticeDialog by remember { mutableStateOf(false) }

    val activeProperty = properties.find { it.pgId == activePgId } ?: properties.firstOrNull()

    LaunchedEffect(properties) {
        if (activePgId == null && properties.isNotEmpty()) {
            viewModel.selectActivePg(properties.first().pgId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activeProperty?.name ?: "PG Manager",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (activeProperty != null) {
                            Text(
                                text = "Code: ${activeProperty.code}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (properties.size > 1) {
                        IconButton(onClick = { showPropertyMenu = true }) {
                            Icon(imageVector = Icons.Default.Apartment, contentDescription = "Switch Property")
                        }
                        DropdownMenu(
                            expanded = showPropertyMenu,
                            onDismissRequest = { showPropertyMenu = false }
                        ) {
                            properties.forEach { prop ->
                                DropdownMenuItem(
                                    text = { Text(prop.name) },
                                    onClick = {
                                        viewModel.selectActivePg(prop.pgId)
                                        showPropertyMenu = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Register New PG") },
                                onClick = {
                                    showPropertyMenu = false
                                    onNavigateToCreatePg()
                                }
                            )
                        }
                    } else {
                        IconButton(onClick = onNavigateToCreatePg) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add New PG")
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (properties.isEmpty()) {
            // Empty State: No PGs created yet
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Apartment,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No PG Properties Added",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Register your first PG accommodation to configure rooms, generate invite codes, and manage tenants.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNavigateToCreatePg,
                    shape = ButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Register Property")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Header: PG Code & Live Capacity Dashboard
                if (activeProperty != null) {
                    PgHeaderCard(
                        property = activeProperty,
                        capacity = capacity,
                        onShareCode = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Join ${activeProperty.name} on PG Manager! Use invite code: ${activeProperty.code}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share PG Invite Code"))
                        },
                        onRegenerateCode = {
                            viewModel.regenerateCode(activeProperty.pgId) {}
                        }
                    )
                }

                // Tabs for Navigation
                val tabs = listOf("Rooms (${rooms.size})", "Requests (${joinRequests.count { it.status.name == "PENDING" }})", "Dues", "Tickets (${tickets.count { it.status == TicketStatus.OPEN }})", "Notices")
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                // Tab Content
                when (selectedTabIndex) {
                    0 -> RoomsTab(
                        rooms = rooms,
                        beds = beds,
                        onAddRoom = { activeProperty?.let { onNavigateToAddRoom(it.pgId) } }
                    )
                    1 -> JoinRequestsTab(
                        requests = joinRequests,
                        onApproveClick = { request -> selectedRequestToApprove = request },
                        onRejectClick = { request ->
                            activeProperty?.let {
                                viewModel.rejectJoinRequest(it.pgId, request.requestId, "Rejected by owner") {}
                            }
                        }
                    )
                    2 -> DuesTab(
                        dues = dues,
                        report = financialReport,
                        onGenerateMonthly = {
                            activeProperty?.let {
                                val currentMonth = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                                viewModel.generateMonthlyDues(it.pgId, currentMonth) {}
                            }
                        },
                        onRecordPayment = { dueId, amount ->
                            activeProperty?.let {
                                viewModel.recordPayment(it.pgId, dueId, amount, "CASH", null) {}
                            }
                        }
                    )
                    3 -> TicketsTab(
                        tickets = tickets,
                        onUpdateStatus = { ticketId, status ->
                            activeProperty?.let {
                                viewModel.updateTicket(it.pgId, ticketId, status, null)
                            }
                        }
                    )
                    4 -> NoticesTab(
                        notices = notices,
                        onAddNotice = { showNewNoticeDialog = true }
                    )
                }
            }
        }
    }

    // Modal Dialog: Approve Join Request and Assign Room/Bed
    if (selectedRequestToApprove != null && activeProperty != null) {
        val vacantBeds = beds.filter { it.status == BedStatus.VACANT }
        AlertDialog(
            onDismissRequest = {
                selectedRequestToApprove = null
                selectedBedForApproval = null
            },
            title = { Text("Approve Student & Assign Bed") },
            text = {
                Column {
                    Text(
                        text = "Applicant: ${selectedRequestToApprove?.fullName}",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Phone: ${selectedRequestToApprove?.phoneNumber}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Select Vacant Bed:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (vacantBeds.isEmpty()) {
                        Text(
                            text = "No vacant beds available in this PG. Please create a room or free a bed first.",
                            color = StatusError,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(vacantBeds) { bed ->
                                val isSelected = selectedBedForApproval?.bedId == bed.bedId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedBedForApproval = bed },
                                    label = { Text("Bed ${bed.bedIdentifier}") },
                                    shape = ChipShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val req = selectedRequestToApprove
                        val bed = selectedBedForApproval
                        if (req != null && bed != null) {
                            viewModel.approveJoinRequest(
                                pgId = activeProperty.pgId,
                                requestId = req.requestId,
                                roomId = bed.roomId,
                                bedId = bed.bedId,
                                onSuccess = {
                                    selectedRequestToApprove = null
                                    selectedBedForApproval = null
                                }
                            )
                        }
                    },
                    enabled = selectedBedForApproval != null,
                    shape = ButtonShape
                ) {
                    Text("Confirm & Admit")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedRequestToApprove = null
                    selectedBedForApproval = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Dialog: Create Announcement Notice
    if (showNewNoticeDialog && activeProperty != null) {
        var noticeTitle by remember { mutableStateOf("") }
        var noticeContent by remember { mutableStateOf("") }
        var noticeCategory by remember { mutableStateOf(NoticeCategory.GENERAL) }

        AlertDialog(
            onDismissRequest = { showNewNoticeDialog = false },
            title = { Text("Publish Announcement") },
            text = {
                Column {
                    OutlinedTextField(
                        value = noticeTitle,
                        onValueChange = { noticeTitle = it },
                        label = { Text("Notice Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = InputFieldShape
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noticeContent,
                        onValueChange = { noticeContent = it },
                        label = { Text("Details & Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = InputFieldShape
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noticeTitle.isNotBlank()) {
                            viewModel.postNotice(
                                pgId = activeProperty.pgId,
                                title = noticeTitle,
                                content = noticeContent,
                                category = noticeCategory,
                                isPinned = false
                            ) {
                                showNewNoticeDialog = false
                            }
                        }
                    },
                    enabled = noticeTitle.isNotBlank(),
                    shape = ButtonShape
                ) {
                    Text("Post & Notify")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewNoticeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PgHeaderCard(
    property: PGProperty,
    capacity: com.pgmanager.app.domain.model.CapacitySummary,
    onShareCode: () -> Unit,
    onRegenerateCode: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Code & Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "STUDENT INVITE CODE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = property.code,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row {
                    OutlinedButton(
                        onClick = onShareCode,
                        shape = ButtonShape,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onRegenerateCode, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Regenerate Code")
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Capacity Dashboard: Live Counts Derived From Bed Records
            Text(
                text = "CAPACITY & OCCUPANCY (LIVE)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CapacityStatBox(label = "Total Beds", count = capacity.totalBeds, color = MaterialTheme.colorScheme.onSurface)
                CapacityStatBox(label = "Booked", count = capacity.bookedBeds, color = BedBookedColor)
                CapacityStatBox(label = "Vacant", count = capacity.vacantBeds, color = BedVacantColor)
                CapacityStatBox(label = "Notice", count = capacity.noticeBeds, color = BedNoticeColor)
            }
        }
    }
}

@Composable
private fun CapacityStatBox(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RoomsTab(
    rooms: List<com.pgmanager.app.domain.model.Room>,
    beds: List<com.pgmanager.app.domain.model.Bed>,
    onAddRoom: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Configured Rooms",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onAddRoom,
                    shape = ButtonShape,
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Room")
                }
            }
        }

        if (rooms.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No rooms configured yet.", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Tap 'Add Room' to define room numbers and create individual bed records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(rooms) { room ->
            val roomBeds = beds.filter { it.roomId == room.roomId }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Room ${room.roomNumber} (Floor ${room.floor})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "INR ${room.rentPerBed}/mo",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "${room.sharingType.name} Sharing | ${if (room.hasAirConditioner) "AC" else "Non-AC"} | Attached Bath: ${if (room.hasAttachedBathroom) "Yes" else "No"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Individual Beds:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        roomBeds.forEach { bed ->
                            val chipColor = when (bed.status) {
                                BedStatus.VACANT -> BedVacantColor
                                BedStatus.BOOKED -> BedBookedColor
                                BedStatus.NOTICE_PERIOD -> BedNoticeColor
                            }
                            Box(
                                modifier = Modifier
                                    .background(chipColor.copy(alpha = 0.15f), ChipShape)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${bed.bedIdentifier}: ${bed.status.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = chipColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JoinRequestsTab(
    requests: List<JoinRequest>,
    onApproveClick: (JoinRequest) -> Unit,
    onRejectClick: (JoinRequest) -> Unit
) {
    val pending = requests.filter { it.status == com.pgmanager.app.domain.model.JoinRequestStatus.PENDING }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Pending Student Requests (${pending.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (pending.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No pending join requests", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Share your PG invite code with prospective tenants to receive admission applications.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(pending) { req ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = req.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Phone: ${req.phoneNumber} | Age: ${req.age}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "College/Office: ${req.collegeOrWorkplace} | City: ${req.homeCity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Guardian: ${req.guardianName} (${req.guardianPhone})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { onRejectClick(req) },
                            shape = ButtonShape,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Reject")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onApproveClick(req) },
                            shape = ButtonShape,
                            modifier = Modifier.height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Assign Bed & Admit")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DuesTab(
    dues: List<com.pgmanager.app.domain.model.RentDue>,
    report: com.pgmanager.app.domain.model.FinancialReport,
    onGenerateMonthly: () -> Unit,
    onRecordPayment: (String, Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("COLLECTION SUMMARY (REAL-TIME)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Billed", style = MaterialTheme.typography.bodySmall)
                            Text("INR ${report.totalBilled}", fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Collected", style = MaterialTheme.typography.bodySmall)
                            Text("INR ${report.totalCollected}", fontWeight = FontWeight.Bold, color = StatusSuccess)
                        }
                        Column {
                            Text("Overdue", style = MaterialTheme.typography.bodySmall)
                            Text("INR ${report.totalOverdue}", fontWeight = FontWeight.Bold, color = StatusError)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Rent Dues List", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(onClick = onGenerateMonthly, shape = ButtonShape, modifier = Modifier.height(36.dp)) {
                    Text("Generate Dues")
                }
            }
        }

        items(dues) { due ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(due.studentName, fontWeight = FontWeight.Bold)
                        Text("Room ${due.roomNumber} - ${due.month}", style = MaterialTheme.typography.bodySmall)
                        Text("INR ${due.totalAmount} (Paid: INR ${due.amountPaid})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    if (due.status != DueStatus.PAID) {
                        Button(
                            onClick = { onRecordPayment(due.dueId, due.totalAmount - due.amountPaid) },
                            shape = ButtonShape,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Mark Paid")
                        }
                    } else {
                        Text("PAID", color = StatusSuccess, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketsTab(
    tickets: List<com.pgmanager.app.domain.model.MaintenanceTicket>,
    onUpdateStatus: (String, TicketStatus) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Maintenance & Repair Tickets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (tickets.isEmpty()) {
            item {
                Text("No maintenance tickets raised.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        items(tickets) { ticket ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(ticket.title, fontWeight = FontWeight.Bold)
                        Text(ticket.status.name, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    Text("Category: ${ticket.category.name} | Room: ${ticket.roomNumber} (${ticket.studentName})", style = MaterialTheme.typography.bodySmall)
                    Text(ticket.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (ticket.status == TicketStatus.OPEN) {
                            Button(onClick = { onUpdateStatus(ticket.ticketId, TicketStatus.IN_PROGRESS) }, shape = ButtonShape, modifier = Modifier.height(32.dp)) {
                                Text("Start Work")
                            }
                        }
                        if (ticket.status != TicketStatus.RESOLVED) {
                            OutlinedButton(onClick = { onUpdateStatus(ticket.ticketId, TicketStatus.RESOLVED) }, shape = ButtonShape, modifier = Modifier.height(32.dp)) {
                                Text("Mark Resolved")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticesTab(
    notices: List<com.pgmanager.app.domain.model.Notice>,
    onAddNotice: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Announcements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(onClick = onAddNotice, shape = ButtonShape, modifier = Modifier.height(36.dp)) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Notice")
                }
            }
        }

        if (notices.isEmpty()) {
            item {
                Text("No notices published.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        items(notices) { notice ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(notice.title, fontWeight = FontWeight.Bold)
                    Text(notice.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
