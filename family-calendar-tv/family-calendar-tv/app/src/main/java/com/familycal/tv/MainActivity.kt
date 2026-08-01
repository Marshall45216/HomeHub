package com.familycal.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.familycal.tv.model.*
import com.familycal.tv.ui.CalendarScreen
import com.familycal.tv.ui.IdleAmbientScreen
import com.familycal.tv.ui.theme.FamilyCalendarTheme
import com.familycal.tv.ui.theme.FamilyVividPalette
import kotlinx.coroutines.delay

/**
 * Entry point. Right now this renders with SAMPLE data so you can see the
 * full UI immediately without wiring up Azure/MSAL first. Swap `sampleXxx()`
 * calls for real GraphRepository + AuthManager calls once you've done the
 * Azure app registration in README.md.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FamilyCalendarTheme {
                FamilyCalendarApp()
            }
        }
    }
}

private const val IDLE_TIMEOUT_MILLIS = 90_000L // 90s of no remote input -> ambient mode

@Composable
fun FamilyCalendarApp() {
    val familyMembers = remember { sampleFamilyMembers() }
    val events = remember { sampleEvents(familyMembers) }
    val countdowns = remember { sampleCountdowns() }
    val weeklyMenu = remember { sampleWeeklyMenu() }
    val forecast = remember { sampleForecast() }
    val idleCards = remember { sampleIdleCards() }

    var activeFilter by remember { mutableStateOf<FamilyMember?>(null) }
    var voiceTranscript by remember { mutableStateOf<String?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var lastInteractionAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var isIdle by remember { mutableStateOf(false) }

    // Idle-mode watcher: any filter tap or mic press resets the clock via
    // the interaction callbacks below; this loop just checks elapsed time.
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            isIdle = System.currentTimeMillis() - lastInteractionAt > IDLE_TIMEOUT_MILLIS
        }
    }

    fun markInteraction() {
        lastInteractionAt = System.currentTimeMillis()
        isIdle = false
    }

    if (isIdle) {
        IdleAmbientScreen(cards = idleCards)
    } else {
        CalendarScreen(
            familyMembers = familyMembers,
            events = events,
            countdowns = countdowns,
            weeklyMenu = weeklyMenu,
            forecast = forecast,
            activeFilter = activeFilter,
            voiceTranscript = voiceTranscript,
            isListening = isListening,
            onFilterSelected = {
                markInteraction()
                activeFilter = it
            },
            onMicPressed = {
                markInteraction()
                // Hook point: launch RecognizerIntent / SpeechRecognizer here,
                // set isListening = true while capturing, then parse the
                // resulting phrase into a NewGraphEvent and call
                // GraphRepository.createEvent(...). See README "Voice quick-add".
                isListening = !isListening
                voiceTranscript = if (isListening) null else "Add soccer practice Saturday 10am for Emma"
            }
        )
    }
}

// --- Sample data, matching the reference mockup -------------------------

private fun sampleFamilyMembers(): List<FamilyMember> {
    val names = listOf("Emma", "Jack", "Beth", "Austin", "Liam")
    return names.mapIndexed { i, n ->
        FamilyMember(
            displayName = n,
            email = "$n@example.com",
            color = FamilyVividPalette[i % FamilyVividPalette.size]
        )
    }
}

private fun sampleEvents(members: List<FamilyMember>): List<FamilyEvent> {
    val now = System.currentTimeMillis()
    fun at(hour: Int, minute: Int = 0) = now - (now % 86_400_000L) + (hour * 3_600_000L) + (minute * 60_000L)
    val emma = members.first { it.displayName == "Emma" }
    val all = members
    return listOf(
        FamilyEvent(
            id = "1", subject = "Soccer Practice",
            startEpochMillis = at(17, 30), endEpochMillis = at(19, 0),
            isAllDay = false, location = "Riverside Park \u00B7 Field 3",
            owner = emma, category = EventCategory.SPORTS
        ),
        FamilyEvent(
            id = "2", subject = "Family Dinner",
            startEpochMillis = at(19, 0), endEpochMillis = at(20, 0),
            isAllDay = false, location = "At Home",
            owner = all.first { it.displayName == "Beth" }, category = EventCategory.MEAL
        ),
        FamilyEvent(
            id = "3", subject = "Reading Time",
            startEpochMillis = at(20, 30), endEpochMillis = at(21, 0),
            isAllDay = false, location = "All Together",
            owner = all.first { it.displayName == "Jack" }, category = EventCategory.READING
        )
    )
}

private fun sampleCountdowns(): List<CountdownItem> = listOf(
    CountdownItem("Birthday", 6, "May 30", EventCategory.BIRTHDAY),
    CountdownItem("Beach Trip", 18, "Jun 11", EventCategory.TRAVEL),
    CountdownItem("First Day of School", 24, "Aug 17", EventCategory.SCHOOL)
)

private fun sampleWeeklyMenu(): List<MenuDay> = listOf(
    MenuDay("Mon", "Taco Night", "\uD83C\uDF2E"),
    MenuDay("Tue", "Pasta Primavera", "\uD83C\uDF5D"),
    MenuDay("Wed", "Grilled Chicken", "\uD83C\uDF57"),
    MenuDay("Thu", "Sheet Pan Salmon", "\uD83C\uDF63"),
    MenuDay("Fri", "Pizza Night", "\uD83C\uDF55"),
    MenuDay("Sat", "Family BBQ", "\uD83C\uDF56"),
    MenuDay("Sun", "Breakfast for Dinner", "\uD83E\uDD5E")
)

private fun sampleForecast(): List<DayForecast> = listOf(
    DayForecast("Today", "May 24", 72, 52, "Sunny"),
    DayForecast("Sun", "May 25", 74, 54, "Partly Cloudy"),
    DayForecast("Mon", "May 26", 68, 50, "Cloudy"),
    DayForecast("Tue", "May 27", 71, 53, "Partly Cloudy"),
    DayForecast("Wed", "May 28", 66, 49, "Rain"),
    DayForecast("Thu", "May 29", 73, 55, "Sunny"),
    DayForecast("Fri", "May 30", 70, 52, "Cloudy")
)

private fun sampleIdleCards(): List<NextHoursCard> = listOf(
    NextHoursCard("Soccer Practice", "Riverside Park \u00B7 Field 3", "Today \u00B7 5:30 PM", EventCategory.SPORTS),
    NextHoursCard("Family Dinner", "At Home", "Today \u00B7 7:00 PM", EventCategory.MEAL),
    NextHoursCard("Tomorrow's Dinner: Pasta Primavera", "At Home", "Tomorrow \u00B7 6:00 PM", EventCategory.MEAL)
)
