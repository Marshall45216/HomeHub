package com.familycal.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.familycal.tv.data.AppState
import com.familycal.tv.data.generateOccurrenceDates
import com.familycal.tv.model.*
import com.familycal.tv.ui.theme.*
import java.time.LocalDate

private enum class RepeatOption(val label: String) {
    NONE("Does not repeat"),
    WEEKDAYS("Multiple days a week"),
    BIWEEKLY("Every other week"),
    MONTHLY_DATE("Monthly on a date"),
    MONTHLY_WEEKDAY("Monthly on a weekday")
}

private enum class EndOption(val label: String) { NEVER("No end date"), ON_DATE("On a date") }

@Composable
fun AddEditEventScreen(appState: AppState, editingEventId: Long?, onBack: () -> Unit) {
    val existing = remember(editingEventId) { editingEventId?.let { id -> appState.events.find { it.id == id } } }
    val isEditingSeriesMember = existing?.seriesId != null

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var date by remember { mutableStateOf(existing?.date ?: LocalDate.now()) }
    var isAllDay by remember { mutableStateOf(existing?.isAllDay ?: false) }
    var time by remember { mutableStateOf(existing?.time ?: "5:30 PM") }
    var location by remember { mutableStateOf(existing?.location ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var selectedPeople by remember { mutableStateOf(existing?.people?.toSet() ?: emptySet()) }
    var category by remember { mutableStateOf(existing?.category ?: EventCategory.GENERAL) }

    var repeatOption by remember { mutableStateOf(RepeatOption.NONE) }
    var weekdays by remember { mutableStateOf(setOf(date.dayOfWeek.value)) }
    var monthlyDate by remember { mutableStateOf(date.dayOfMonth) }
    var monthlyNth by remember { mutableStateOf(1) }
    var monthlyWeekday by remember { mutableStateOf(date.dayOfWeek.value) }
    var endOption by remember { mutableStateOf(EndOption.NEVER) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }

    var showSeriesChoice by remember { mutableStateOf(false) }
    var pendingApply: ((applyToAll: Boolean) -> Unit)? by remember { mutableStateOf(null) }

    fun buildRule(): RecurrenceRule = when (repeatOption) {
        RepeatOption.NONE -> RecurrenceRule.None
        RepeatOption.WEEKDAYS -> RecurrenceRule.Weekdays(weekdays.ifEmpty { setOf(date.dayOfWeek.value) })
        RepeatOption.BIWEEKLY -> RecurrenceRule.Biweekly
        RepeatOption.MONTHLY_DATE -> RecurrenceRule.MonthlyByDate(monthlyDate)
        RepeatOption.MONTHLY_WEEKDAY -> RecurrenceRule.MonthlyByWeekday(monthlyNth, monthlyWeekday)
    }

    fun doSave() {
        if (title.isBlank()) return
        val finalTime = if (isAllDay) null else time

        if (existing == null) {
            if (repeatOption == RepeatOption.NONE) {
                appState.addEvent(title, date, isAllDay, finalTime, location, notes, selectedPeople.toList(), category)
            } else {
                val dates = generateOccurrenceDates(date, buildRule(), RecurrenceEnd(endOption == EndOption.NEVER, endDate))
                    .ifEmpty { listOf(date) }
                appState.addEvent(title, date, isAllDay, finalTime, location, notes, selectedPeople.toList(), category, dates, "series-${System.currentTimeMillis()}")
            }
            onBack()
            return
        }

        if (!isEditingSeriesMember) {
            if (repeatOption != RepeatOption.NONE) {
                val dates = generateOccurrenceDates(date, buildRule(), RecurrenceEnd(endOption == EndOption.NEVER, endDate)).ifEmpty { listOf(date) }
                appState.deleteEvent(existing.id)
                appState.addEvent(title, date, isAllDay, finalTime, location, notes, selectedPeople.toList(), category, dates, "series-${System.currentTimeMillis()}")
            } else {
                appState.updateSingleEvent(existing.id, title, date, isAllDay, finalTime, location, notes, selectedPeople.toList(), category)
            }
            onBack()
            return
        }

        pendingApply = { applyToAll ->
            if (applyToAll) {
                appState.updateSeriesContent(existing.seriesId!!, title, isAllDay, finalTime, location, notes, selectedPeople.toList(), category)
            } else {
                appState.updateSingleEvent(existing.id, title, date, isAllDay, finalTime, location, notes, selectedPeople.toList(), category)
            }
            onBack()
        }
        showSeriesChoice = true
    }

    Box(modifier = Modifier.fillMaxSize().background(HubBackground)) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
            BackButton(onBack)
            Spacer(Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                item {
                    Text(if (existing == null) "Add event" else "Edit event", style = FamilyCalTypography.headlineLarge)

                    FormLabel("Title")
                    FormTextField(title, { title = it }, "Soccer practice")

                    FormLabel("Date")
                    DatePickerButton(date) { date = it }

                    FormLabel("All day event")
                    FormSwitchRow("All day", isAllDay) { isAllDay = it }

                    if (!isAllDay) {
                        FormLabel("Time")
                        TimePickerButton(time) { time = it }
                    }

                    FormLabel("Location")
                    FormTextField(location, { location = it }, "Riverside Park")

                    FormLabel("Notes")
                    FormTextField(notes, { notes = it }, "Anything else worth remembering")

                    FormLabel("Who's this for?")
                    MultiChipSelectorRow(
                        options = appState.family.map { it.displayName to it.displayName },
                        selected = selectedPeople,
                        onToggle = { name -> selectedPeople = if (name in selectedPeople) selectedPeople - name else selectedPeople + name }
                    )

                    FormLabel("Category")
                    ChipSelectorRow(
                        options = EventCategory.values().map { it to it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                        selected = category,
                        onSelect = { category = it }
                    )

                    if (!isEditingSeriesMember) {
                        FormLabel("Repeats")
                        ChipSelectorRow(
                            options = RepeatOption.values().map { it to it.label },
                            selected = repeatOption,
                            onSelect = { repeatOption = it }
                        )

                        when (repeatOption) {
                            RepeatOption.WEEKDAYS -> {
                                FormLabel("Which days?")
                                MultiChipSelectorRow(
                                    options = (1..7).map { it to weekdayLabel(it) },
                                    selected = weekdays,
                                    onToggle = { d -> weekdays = if (d in weekdays) weekdays - d else weekdays + d }
                                )
                            }
                            RepeatOption.MONTHLY_DATE -> {
                                FormLabel("Day of month: $monthlyDate")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SecondaryButton("\u2212") { if (monthlyDate > 1) monthlyDate-- }
                                    SecondaryButton("+") { if (monthlyDate < 31) monthlyDate++ }
                                }
                            }
                            RepeatOption.MONTHLY_WEEKDAY -> {
                                FormLabel("Which week?")
                                ChipSelectorRow(
                                    options = listOf(1 to "1st", 2 to "2nd", 3 to "3rd", 4 to "4th", -1 to "Last"),
                                    selected = monthlyNth, onSelect = { monthlyNth = it }
                                )
                                FormLabel("Which day?")
                                ChipSelectorRow(
                                    options = (1..7).map { it to weekdayLabel(it) },
                                    selected = monthlyWeekday, onSelect = { monthlyWeekday = it }
                                )
                            }
                            else -> {}
                        }

                        if (repeatOption != RepeatOption.NONE) {
                            FormLabel("Ends")
                            ChipSelectorRow(
                                options = EndOption.values().map { it to it.label },
                                selected = endOption, onSelect = { endOption = it }
                            )
                            if (endOption == EndOption.ON_DATE) {
                                DatePickerButton(endDate) { endDate = it }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Cancel", onBack)
                PrimaryButton(if (existing == null) "Add" else "Save") { doSave() }
            }
        }

        if (showSeriesChoice) {
            SeriesChoiceDialog(
                message = "\"${existing?.title}\" repeats. Save this change to just this event, or every event in the series? (Date changes only apply to this one event.)",
                onDismiss = { showSeriesChoice = false; pendingApply = null },
                onThisOne = { showSeriesChoice = false; pendingApply?.invoke(false) },
                onAllInSeries = { showSeriesChoice = false; pendingApply?.invoke(true) }
            )
        }
    }
}
