package com.familycal.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.familycal.tv.data.AppState
import com.familycal.tv.model.CountdownItem
import com.familycal.tv.model.EventCategory
import com.familycal.tv.ui.theme.*
import java.time.LocalDate

@Composable
fun AddEditCountdownScreen(appState: AppState, editingIndex: Int?, onBack: () -> Unit) {
    val existing = editingIndex?.let { appState.countdowns.getOrNull(it) }
    var label by remember { mutableStateOf(existing?.label ?: "") }
    var date by remember { mutableStateOf(existing?.date ?: LocalDate.now().plusDays(7)) }
    var showConfirm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(HubBackground)) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
            BackButton(onBack)
            Spacer(Modifier.height(16.dp))

            Text(if (existing == null) "Add countdown" else "Edit countdown", style = FamilyCalTypography.headlineLarge)

            FormLabel("What's it called?")
            FormTextField(label, { label = it }, "Beach Trip")

            FormLabel("Date")
            DatePickerButton(date) { date = it }

            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Cancel", onBack)
                PrimaryButton(if (existing == null) "Add" else "Save") {
                    if (label.isBlank()) return@PrimaryButton
                    // Manually editing detaches it from birthday auto-sync (if it was
                    // linked) so it doesn't fight with future birthday edits later.
                    val item = CountdownItem(label = label, date = date, category = EventCategory.GENERAL)
                    if (editingIndex != null) appState.updateCountdown(editingIndex, item) else appState.addCountdown(item)
                    onBack()
                }
                if (existing != null) {
                    DangerButton("Delete") { showConfirm = true }
                }
            }
        }

        if (showConfirm) {
            ConfirmDialog(
                title = "Delete countdown?",
                message = "This can't be undone.",
                onCancel = { showConfirm = false },
                onConfirm = { appState.deleteCountdown(editingIndex!!); onBack() }
            )
        }
    }
}
