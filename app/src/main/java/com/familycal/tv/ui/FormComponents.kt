package com.familycal.tv.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.familycal.tv.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

@Composable
fun FormLabel(text: String) {
    Text(text.uppercase(), style = FamilyCalTypography.bodyMedium, color = HubTextSecondary, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
}

@Composable
fun FormTextField(value: String, onValueChange: (String) -> Unit, placeholder: String = "", singleLine: Boolean = true) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HubSurfaceRaised, RoundedCornerShape(8.dp))
            .border(1.dp, HubDivider, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (value.isEmpty()) Text(placeholder, style = FamilyCalTypography.bodyLarge, color = HubTextSecondary)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(color = HubTextPrimary, fontSize = FamilyCalTypography.bodyLarge.fontSize),
            cursorBrush = SolidColor(HubAccent),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun FormButtonRow(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HubSurfaceRaised)
            .tvFocusable(RoundedCornerShape(8.dp), onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, style = FamilyCalTypography.bodyLarge)
    }
}

/** Opens Android's native date picker -- reliable, remote-navigable out of the box, no need to hand-build a calendar widget. */
@Composable
fun DatePickerButton(date: LocalDate?, onDateSelected: (LocalDate) -> Unit) {
    val context = LocalContext.current
    val label = date?.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")) ?: "Choose a date"
    FormButtonRow(label) {
        val base = date ?: LocalDate.now()
        DatePickerDialog(
            context,
            { _, year, month, day -> onDateSelected(LocalDate.of(year, month + 1, day)) },
            base.year, base.monthValue - 1, base.dayOfMonth
        ).show()
    }
}

/** Opens Android's native time picker (12-hour). Returns a formatted "5:30 PM" style string. */
@Composable
fun TimePickerButton(time: String?, onTimeSelected: (String) -> Unit) {
    val context = LocalContext.current
    FormButtonRow(time ?: "Choose a time") {
        val base = time?.let { runCatching { LocalTime.parse(it, DateTimeFormatter.ofPattern("h:mm a")) }.getOrNull() } ?: LocalTime.of(17, 30)
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val formatted = LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern("h:mm a"))
                onTimeSelected(formatted)
            },
            base.hour, base.minute, false
        ).show()
    }
}

/** A row of selectable chips -- used for category, repeat type, weekday picking, etc. */
@Composable
fun <T> ChipSelectorRow(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .background(if (isSelected) HubAccent else HubSurfaceRaised)
                    .tvFocusable(RoundedCornerShape(20.dp)) { onSelect(value) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) { Text(label, style = FamilyCalTypography.bodyMedium, color = if (isSelected) HubTextPrimary else HubTextSecondary) }
        }
    }
}

/** Multi-select chip row, e.g. for weekdays or family members. */
@Composable
fun <T> MultiChipSelectorRow(options: List<Pair<T, String>>, selected: Set<T>, onToggle: (T) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            val isSelected = value in selected
            Box(
                modifier = Modifier
                    .background(if (isSelected) HubAccent else HubSurfaceRaised)
                    .tvFocusable(RoundedCornerShape(20.dp)) { onToggle(value) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) { Text(label, style = FamilyCalTypography.bodyMedium, color = if (isSelected) HubTextPrimary else HubTextSecondary) }
        }
    }
}

@Composable
fun FormSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(RoundedCornerShape(8.dp)) { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = FamilyCalTypography.bodyLarge)
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (checked) HubAccent else HubDivider)
        ) {
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(HubTextPrimary)
                    .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
            )
        }
    }
}

fun weekdayLabel(isoDayOfWeek: Int): String =
    java.time.DayOfWeek.of(isoDayOfWeek).getDisplayName(JTextStyle.SHORT, Locale.US)

@Composable
fun PrimaryButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(HubAccent)
            .tvFocusable(RoundedCornerShape(8.dp), onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) { Text(label, style = FamilyCalTypography.bodyLarge) }
}

@Composable
fun SecondaryButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .border(1.dp, HubDivider, RoundedCornerShape(8.dp))
            .tvFocusable(RoundedCornerShape(8.dp), onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) { Text(label, style = FamilyCalTypography.bodyLarge) }
}

@Composable
fun DangerButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .border(1.dp, EventColorBirthday, RoundedCornerShape(8.dp))
            .tvFocusable(RoundedCornerShape(8.dp), onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) { Text(label, style = FamilyCalTypography.bodyLarge, color = EventColorBirthday) }
}

@Composable
fun BackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(HubSurfaceRaised)
            .border(1.dp, HubDivider, RoundedCornerShape(10.dp))
            .tvFocusable(RoundedCornerShape(10.dp), onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) { Text("\u2190 Back", style = FamilyCalTypography.bodyLarge) }
}

/** A modal overlay -- used for confirm dialogs and the this-event-vs-series choice.
 *  Custom rather than the platform AlertDialog so its buttons use the same
 *  tvFocusable focus styling as the rest of the app instead of a different look. */
@Composable
fun ModalOverlay(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f))
            .tvFocusable(RoundedCornerShape(0.dp), onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 300.dp, max = 420.dp)
                .background(HubSurface, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column { content() }
        }
    }
}

@Composable
fun ConfirmDialog(title: String, message: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    ModalOverlay(onDismiss = onCancel) {
        Text(title, style = FamilyCalTypography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(message, style = FamilyCalTypography.bodyMedium)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Cancel", onCancel)
            DangerButton("Delete", onConfirm)
        }
    }
}

@Composable
fun SeriesChoiceDialog(message: String, onDismiss: () -> Unit, onThisOne: () -> Unit, onAllInSeries: () -> Unit) {
    ModalOverlay(onDismiss = onDismiss) {
        Text("Repeating event", style = FamilyCalTypography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(message, style = FamilyCalTypography.bodyMedium)
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("This event only", onThisOne)
            SecondaryButton("All events in the series", onAllInSeries)
            SecondaryButton("Cancel", onDismiss)
        }
    }
}
