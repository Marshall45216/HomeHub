package com.familycal.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.familycal.tv.data.AppState
import com.familycal.tv.model.DayForecast
import com.familycal.tv.ui.CalendarScreen
import com.familycal.tv.ui.Screen
import com.familycal.tv.ui.theme.FamilyCalTypography
import com.familycal.tv.ui.theme.HubBackground
import com.familycal.tv.ui.theme.FamilyCalendarTheme
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Entry point. Runs on sample data for now (see AppState's init block) --
 * swap in real GraphRepository + AuthManager calls once Azure sign-in is
 * wired back in. The Dashboard screen is fully real; other screens are
 * placeholders until their turn in the next build rounds (event add/edit,
 * meal planner, grocery list, profile editing -- in that order).
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

@Composable
fun FamilyCalendarApp() {
    val appState = remember { AppState() }
    var screen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    val forecast = remember { sampleForecast() }

    when (val s = screen) {
        is Screen.Dashboard -> CalendarScreen(
            appState = appState,
            forecast = forecast,
            onNavigate = { screen = it }
        )
        is Screen.EventDetail -> EventDetailScreen(
            appState = appState, eventId = s.eventId,
            onBack = { screen = Screen.Dashboard },
            onEdit = { screen = Screen.AddEditEvent(it) }
        )
        is Screen.AddEditEvent -> AddEditEventScreen(
            appState = appState, editingEventId = s.editingEventId,
            onBack = { screen = Screen.Dashboard }
        )
        is Screen.CountdownDetail -> CountdownDetailScreen(
            appState = appState, index = s.index,
            onBack = { screen = Screen.Dashboard },
            onEdit = { screen = Screen.AddEditCountdown(it) }
        )
        is Screen.AddEditCountdown -> AddEditCountdownScreen(
            appState = appState, editingIndex = s.editingIndex,
            onBack = { screen = Screen.Dashboard }
        )
        else -> PlaceholderScreen(screen = s, onBack = { screen = Screen.Dashboard })
    }
}

/** Shown for any screen not yet built. Confirms navigation works end-to-end
 *  while the real screen is still in progress. */
@Composable
private fun PlaceholderScreen(screen: Screen, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(HubBackground).padding(40.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            "\u2190 Back",
            style = FamilyCalTypography.bodyLarge,
            modifier = Modifier.clickable(onClick = onBack).padding(bottom = 24.dp)
        )
        Text(screenLabel(screen), style = FamilyCalTypography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text("This screen is coming in the next build round.", style = FamilyCalTypography.bodyMedium)
    }
}

private fun screenLabel(screen: Screen): String = when (screen) {
    is Screen.EventDetail -> "Event detail"
    is Screen.PersonProfile -> "${screen.personName}'s profile"
    is Screen.CountdownDetail -> "Countdown"
    is Screen.MealsWeek -> "Weekly meal view"
    is Screen.MonthlyMenu -> "Monthly meal view"
    is Screen.EventsWeek -> "Weekly events view"
    is Screen.EventsMonth -> "Monthly events view"
    is Screen.GroceryList -> "Grocery list"
    is Screen.AddEditEvent -> if (screen.editingEventId != null) "Edit event" else "Add event"
    is Screen.AddEditCountdown -> if (screen.editingIndex != null) "Edit countdown" else "Add countdown"
    is Screen.EditProfile -> if (screen.editingName != null) "Edit profile" else "Add family member"
    is Screen.Dashboard -> "Dashboard"
}

private fun sampleForecast(): List<DayForecast> {
    val today = LocalDate.now()
    val temps = listOf(72 to 52, 74 to 54, 68 to 50, 71 to 53, 66 to 49, 73 to 55, 70 to 52)
    val conditions = listOf("Sunny", "Partly Cloudy", "Cloudy", "Partly Cloudy", "Rain", "Sunny", "Cloudy")
    return (0..6).map { i ->
        val date = today.plusDays(i.toLong())
        DayForecast(
            date = date,
            dayLabel = if (i == 0) "Today" else date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US),
            highF = temps[i].first, lowF = temps[i].second, condition = conditions[i]
        )
    }
}
