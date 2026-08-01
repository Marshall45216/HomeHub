package com.familycal.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.familycal.tv.data.AppState
import com.familycal.tv.ui.theme.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@Composable
fun CountdownDetailScreen(appState: AppState, index: Int, onBack: () -> Unit, onEdit: (Int) -> Unit) {
    val item = appState.countdowns.getOrNull(index)
    var showConfirm by remember { mutableStateOf(false) }
    if (item == null) { onBack(); return }

    val today = LocalDate.now()
    val effectiveDate = item.effectiveDate(today)
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(effectiveDate) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }

    val target = effectiveDate.atStartOfDay()
    val isToday = effectiveDate == today
    val millisLeft = ChronoUnit.MILLIS.between(now, target)

    Box(modifier = Modifier.fillMaxSize().background(HubBackground)) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
            BackButton(onBack)
            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier.widthIn(max = 460.dp).background(HubSurface, RoundedCornerShape(20.dp)).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(item.label, style = FamilyCalTypography.headlineLarge)
                Spacer(Modifier.height(4.dp))
                Text(effectiveDate.toString(), style = FamilyCalTypography.bodyMedium, color = HubTextSecondary)
                Spacer(Modifier.height(20.dp))

                if (isToday || millisLeft <= 0) {
                    Text("It's today!", style = FamilyCalTypography.headlineLarge, color = HubAccent)
                } else {
                    val days = millisLeft / 86_400_000
                    val hours = (millisLeft % 86_400_000) / 3_600_000
                    val mins = (millisLeft % 3_600_000) / 60_000
                    val secs = (millisLeft % 60_000) / 1000
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TimeUnitBox(days.toString(), "Days")
                        TimeUnitBox(hours.toString().padStart(2, '0'), "Hours")
                        TimeUnitBox(mins.toString().padStart(2, '0'), "Min")
                        TimeUnitBox(secs.toString().padStart(2, '0'), "Sec")
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Edit") { onEdit(index) }
                    DangerButton("Delete") { showConfirm = true }
                }
            }
        }

        if (showConfirm) {
            ConfirmDialog(
                title = "Delete countdown?",
                message = "This can't be undone.",
                onCancel = { showConfirm = false },
                onConfirm = { appState.deleteCountdown(index); onBack() }
            )
        }
    }
}

@Composable
private fun TimeUnitBox(value: String, label: String) {
    Column(
        modifier = Modifier.background(HubSurfaceRaised, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = FamilyCalTypography.headlineLarge)
        Text(label, style = FamilyCalTypography.bodyMedium, color = HubTextSecondary)
    }
}
