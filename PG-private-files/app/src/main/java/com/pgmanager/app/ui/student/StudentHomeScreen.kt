package com.pgmanager.app.ui.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pgmanager.app.domain.model.DailyMealPlan
import com.pgmanager.app.domain.model.Notice
import com.pgmanager.app.domain.model.RentDue
import com.pgmanager.app.domain.model.TicketStatus

@Composable
fun StudentHomeScreen(
    viewModel: StudentViewModel,
    studentName: String,
    onLogout: () -> Unit
) {
    val property by viewModel.property.collectAsStateWithLifecycle()
    val meals by viewModel.todayMeals.collectAsStateWithLifecycle()
    val notices by viewModel.notices.collectAsStateWithLifecycle()
    val tickets by viewModel.myTickets.collectAsStateWithLifecycle()
    val dues by viewModel.myDues.collectAsStateWithLifecycle()

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Hello, ${studentName.ifBlank { "resident" }}", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        property?.name ?: "Your accommodation",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item { MealCard(meals) }
            item { DuesCard(dues) }
            item {
                SummaryCard(
                    title = "Maintenance",
                    body = if (tickets.isEmpty()) {
                        "No repair requests"
                    } else {
                        "${tickets.count { it.status != TicketStatus.RESOLVED }} open request(s)"
                    }
                )
            }
            item {
                Text("Notices", style = MaterialTheme.typography.titleLarge)
            }
            if (notices.isEmpty()) {
                item {
                    Text(
                        "No notices yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(notices.take(5), key = Notice::noticeId) { notice ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(notice.title, style = MaterialTheme.typography.titleMedium)
                            Text(notice.content, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item {
                Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                    Text("Log out")
                }
            }
        }
    }
}

@Composable
private fun MealCard(meals: DailyMealPlan?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Today's meals", style = MaterialTheme.typography.titleLarge)
            if (meals == null) {
                Text("Menu not published yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                MealLine("Breakfast", meals.breakfast.timeWindow, meals.breakfast.items)
                MealLine("Lunch", meals.lunch.timeWindow, meals.lunch.items)
                MealLine("Dinner", meals.dinner.timeWindow, meals.dinner.items)
            }
        }
    }
}

@Composable
private fun MealLine(label: String, time: String, items: List<String>) {
    Text("$label · $time", style = MaterialTheme.typography.titleSmall)
    Text(
        items.ifEmpty { listOf("Menu not published") }.joinToString(", "),
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun DuesCard(dues: List<RentDue>) {
    val outstanding = dues.filter { it.status.name != "PAID" }
    SummaryCard(
        title = "Rent",
        body = if (outstanding.isEmpty()) {
            "No outstanding dues"
        } else {
            "${outstanding.size} outstanding due(s)"
        }
    )
}

@Composable
private fun SummaryCard(title: String, body: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
