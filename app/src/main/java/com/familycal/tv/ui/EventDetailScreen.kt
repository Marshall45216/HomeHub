package com.familycal.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.familycal.tv.data.AppState
import com.familycal.tv.ui.theme.*

@Composable
fun EventDetailScreen(appState: AppState, eventId: Long, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val event = appState.events.find { it.id == eventId }
    var showConfirm by remember { mutableStateOf(false) }
    var showSeriesChoice by remember { mutableStateOf(false) }

    if (event == null) { onBack(); return }

    val (emoji, color) = categoryVisual(event.category)

    Box(modifier = Modifier.fillMaxSize().background(HubBackground)) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
            BackButton(onBack)
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .background(HubSurface, RoundedCornerShape(20.dp))
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) { Text(emoji, style = FamilyCalTypography.titleLarge) }
                    Spacer(Modifier.width(14.dp))
                    Text(event.title, style = FamilyCalTypography.headlineLarge)
                }
                Spacer(Modifier.height(16.dp))
                DetailRow("Date", event.date.toString())
                DetailRow("Time", if (event.isAllDay) "All day" else event.time ?: "")
                DetailRow("Location", event.location.ifEmpty { "\u2014" })
                DetailRow("For", event.people.joinToString(", ").ifEmpty { "\u2014" })
                DetailRow("Notes", event.notes.ifEmpty { "\u2014" })

                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Edit") { onEdit(event.id) }
                    DangerButton("Delete") {
                        if (event.seriesId != null) showSeriesChoice = true else showConfirm = true
                    }
                }
            }
        }

        if (showConfirm) {
            ConfirmDialog(
                title = "Delete event?",
                message = "This can't be undone.",
                onCancel = { showConfirm = false },
                onConfirm = { appState.deleteEvent(event.id); onBack() }
            )
        }
        if (showSeriesChoice) {
            SeriesChoiceDialog(
                message = "\"${event.title}\" is part of a repeating series. Delete just this one, or every event in the series?",
                onDismiss = { showSeriesChoice = false },
                onThisOne = { appState.deleteEvent(event.id); onBack() },
                onAllInSeries = { appState.deleteSeries(event.seriesId!!); onBack() }
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = FamilyCalTypography.bodyMedium, color = HubTextSecondary, modifier = Modifier.width(90.dp))
        Text(value, style = FamilyCalTypography.bodyLarge)
    }
}
