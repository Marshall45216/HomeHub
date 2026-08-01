package com.familycal.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.familycal.tv.data.AppState
import com.familycal.tv.model.*
import com.familycal.tv.ui.theme.*
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

enum class HeroRange { TODAY, WEEK, MONTH }

/**
 * Every interactive element on this screen goes through this instead of a
 * plain Modifier.clickable. Two reasons:
 *  1. clickable()'s default indication only really shows up on press --
 *     on a TV remote you need a clear "this is what's currently focused"
 *     state as the D-pad moves BEFORE you press select.
 *  2. It gives every clickable element the same strong, consistent look:
 *     a bright border + light fill while focused.
 */
@Composable
fun Modifier.tvFocusable(shape: Shape, onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    return this
        .clip(shape)
        .then(
            if (isFocused)
                Modifier
                    .background(HubAccent.copy(alpha = 0.30f))
                    .border(width = 3.dp, color = Color.White, shape = shape)
            else Modifier
        )
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}

@Composable
fun CalendarScreen(
    appState: AppState,
    forecast: List<DayForecast>,
    onNavigate: (Screen) -> Unit
) {
    val today = remember { LocalDate.now() }
    var heroRange by remember { mutableStateOf(HeroRange.TODAY) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HubBackground)
            .padding(horizontal = 36.dp, vertical = 20.dp)
    ) {
        TopBar(appState = appState, today = today, onNavigate = onNavigate)
        Spacer(Modifier.height(14.dp))

        // The hero/events + family/countdown row gets ALL the leftover
        // vertical space -- weather and the voice bar are deliberately
        // kept compact below so this row never gets starved and clipped.
        Row(modifier = Modifier.weight(1f, fill = true)) {
            Box(modifier = Modifier.weight(0.68f)) {
                TodayHeroPanel(
                    appState = appState, today = today, range = heroRange,
                    onRangeSelected = { r ->
                        when (r) {
                            HeroRange.TODAY -> heroRange = r
                            HeroRange.WEEK -> onNavigate(Screen.EventsWeek)
                            HeroRange.MONTH -> onNavigate(Screen.EventsMonth)
                        }
                    },
                    onAddEvent = { onNavigate(Screen.AddEditEvent()) },
                    onEventClick = { onNavigate(Screen.EventDetail(it)) }
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(0.32f).fillMaxHeight()) {
                FamilyRow(appState = appState, onNavigate = onNavigate)
                Spacer(Modifier.height(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    CountdownGrid(appState = appState, today = today, onNavigate = onNavigate)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        WeatherWeekStrip(forecast)
    }
}

@Composable
private fun TopBar(appState: AppState, today: LocalDate, onNavigate: (Screen) -> Unit) {
    val weekStart = today.with(java.time.DayOfWeek.MONDAY)
    val weekEnd = weekStart.plusDays(6)
    val caption = if (weekStart.month == weekEnd.month) {
        weekStart.month.getDisplayName(TextStyle.FULL, Locale.US) + " " + weekStart.year
    } else {
        "${weekStart.month.getDisplayName(TextStyle.SHORT, Locale.US)} - " +
            "${weekEnd.month.getDisplayName(TextStyle.SHORT, Locale.US)} ${weekEnd.year}"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(HubAccent),
                contentAlignment = Alignment.Center
            ) { Text("H", style = FamilyCalTypography.titleLarge) }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("HomeHub", style = FamilyCalTypography.titleLarge)
                Text("Smart Family Planner", style = FamilyCalTypography.bodyMedium)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TopBarButton("Weekly View") { onNavigate(Screen.MealsWeek) }
                Spacer(Modifier.width(8.dp))
                TopBarButton("Monthly View") { onNavigate(Screen.MonthlyMenu) }
                Spacer(Modifier.width(8.dp))
                TopBarButton("Grocery List") { onNavigate(Screen.GroceryList) }
            }
            Spacer(Modifier.height(2.dp))
            Text(caption, style = FamilyCalTypography.bodyMedium)
            Spacer(Modifier.height(2.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items((0..6).toList()) { i ->
                    val date = weekStart.plusDays(i.toLong())
                    val meal = appState.mealFor(date)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(4.dp)
                            .tvFocusable(RoundedCornerShape(8.dp)) { onNavigate(Screen.MealsWeek) }
                            .padding(6.dp)
                    ) {
                        Text(
                            "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)} ${date.dayOfMonth}",
                            style = FamilyCalTypography.bodyMedium
                        )
                        Text("${meal.emoji} ${meal.name}", style = FamilyCalTypography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBarButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .border(width = 1.dp, color = HubDivider, shape = RoundedCornerShape(20.dp))
            .tvFocusable(RoundedCornerShape(20.dp), onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) { Text(label, style = FamilyCalTypography.bodyMedium) }
}

@Composable
private fun TodayHeroPanel(
    appState: AppState, today: LocalDate, range: HeroRange,
    onRangeSelected: (HeroRange) -> Unit, onAddEvent: () -> Unit, onEventClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)).background(HeroGradient).padding(20.dp)
    ) {
        // Fixed-height header -- always visible regardless of how much room is left.
        Text(today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US).uppercase(), style = FamilyCalTypography.bodyMedium, color = Color(0xFFCBD3FF))
        Text(today.month.getDisplayName(TextStyle.SHORT, Locale.US) + " " + today.dayOfMonth, style = FamilyCalTypography.headlineLarge)

        Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                when (range) { HeroRange.TODAY -> "Today"; HeroRange.WEEK -> "This Week"; HeroRange.MONTH -> "This Month" },
                style = FamilyCalTypography.titleLarge, color = Color(0xFFB9C2FF)
            )
            Row {
                RangePill("Today", range == HeroRange.TODAY) { onRangeSelected(HeroRange.TODAY) }
                Spacer(Modifier.width(6.dp))
                RangePill("Week", range == HeroRange.WEEK) { onRangeSelected(HeroRange.WEEK) }
                Spacer(Modifier.width(6.dp))
                RangePill("Month", range == HeroRange.MONTH) { onRangeSelected(HeroRange.MONTH) }
            }
        }

        // Events area gets exactly whatever room is left between the header
        // above and the Add button below -- and SCROLLS internally if there
        // are more events than fit, instead of the excess just disappearing.
        val todaysEvents = appState.eventsOn(today)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (todaysEvents.isEmpty()) {
                Text("Nothing on the calendar today", color = Color(0xFFCBD3FF))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(todaysEvents) { ev -> HeroEventRow(ev, onClick = { onEventClick(ev.id) }) }
                }
            }
        }

        Box(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.15f))
                .tvFocusable(RoundedCornerShape(10.dp), onAddEvent)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) { Text("+ Add Event", style = FamilyCalTypography.bodyMedium) }
    }
}

@Composable
private fun RangePill(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (active) Color.White else Color.White.copy(alpha = 0.1f))
            .tvFocusable(RoundedCornerShape(20.dp), onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(label, style = FamilyCalTypography.bodyMedium, color = if (active) Color(0xFF2B3E8C) else Color.White)
    }
}

@Composable
private fun HeroEventRow(event: FamilyEvent, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().tvFocusable(RoundedCornerShape(12.dp), onClick).padding(4.dp)
    ) {
        val (emoji, color) = categoryVisual(event.category)
        Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) { Text(emoji, style = FamilyCalTypography.bodyMedium) }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(if (event.isAllDay) "All day" else event.time ?: "", style = FamilyCalTypography.bodyMedium, color = Color(0xFFCBD3FF))
            Text(event.title, style = FamilyCalTypography.bodyLarge)
        }
    }
}

@Composable
private fun FamilyRow(appState: AppState, onNavigate: (Screen) -> Unit) {
    Text("FAMILY", style = FamilyCalTypography.bodyMedium)
    Spacer(Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(appState.family) { person ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(2.dp).tvFocusable(RoundedCornerShape(16.dp)) { onNavigate(Screen.PersonProfile(person.displayName)) }.padding(4.dp)
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(HubSurfaceRaised).border(2.dp, person.color, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text(person.displayName.take(1), style = FamilyCalTypography.titleLarge) }
                Text(person.displayName, style = FamilyCalTypography.bodyMedium)
            }
        }
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(2.dp).tvFocusable(RoundedCornerShape(16.dp)) { onNavigate(Screen.EditProfile()) }.padding(4.dp)
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).border(1.dp, HubDivider, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text("+", style = FamilyCalTypography.headlineLarge, color = HubTextSecondary) }
                Text("Add", style = FamilyCalTypography.bodyMedium)
            }
        }
    }
}

@Composable
private fun CountdownGrid(appState: AppState, today: LocalDate, onNavigate: (Screen) -> Unit) {
    Text("COUNTDOWNS", style = FamilyCalTypography.bodyMedium)
    Spacer(Modifier.height(6.dp))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        appState.countdowns.chunked(2).forEachIndexed { rowIdx, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { colIdx, item ->
                    val index = rowIdx * 2 + colIdx
                    CountdownCard(item, today, Modifier.weight(1f)) { onNavigate(Screen.CountdownDetail(index)) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CountdownCard(item: CountdownItem, today: LocalDate, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val (_, color) = categoryVisual(item.category)
    val effectiveDate = item.effectiveDate(today)
    val days = java.time.temporal.ChronoUnit.DAYS.between(today, effectiveDate).toInt()
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.22f))
            .tvFocusable(RoundedCornerShape(16.dp), onClick)
            .padding(10.dp)
    ) {
        Text(item.label, style = FamilyCalTypography.bodyMedium)
        Text(if (days <= 0) "Today" else "$days", style = FamilyCalTypography.titleLarge, color = color)
        if (days > 0) Text("days", style = FamilyCalTypography.bodyMedium)
    }
}

@Composable
private fun WeatherWeekStrip(forecast: List<DayForecast>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(forecast) { f ->
            Column(modifier = Modifier.width(84.dp).clip(RoundedCornerShape(12.dp)).background(HubSurfaceRaised).padding(6.dp)) {
                Text(f.dayLabel, style = FamilyCalTypography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(weatherEmoji(f.condition), style = FamilyCalTypography.bodyMedium)
                Spacer(Modifier.height(2.dp))
                Row { Text("${f.highF}\u00B0", style = FamilyCalTypography.bodyMedium); Spacer(Modifier.width(4.dp)); Text("${f.lowF}\u00B0", style = FamilyCalTypography.bodyMedium, color = HubTextSecondary) }
            }
        }
    }
}

fun categoryVisual(category: EventCategory): Pair<String, Color> = when (category) {
    EventCategory.SPORTS -> "\u26BD" to EventColorSports
    EventCategory.MEAL -> "\uD83C\uDF7D" to EventColorMeal
    EventCategory.READING -> "\uD83D\uDCD6" to EventColorReading
    EventCategory.BIRTHDAY -> "\uD83C\uDF82" to EventColorBirthday
    EventCategory.TRAVEL -> "\uD83C\uDFD6" to EventColorTravel
    EventCategory.SCHOOL -> "\uD83C\uDF92" to EventColorSchool
    EventCategory.GENERAL -> "\uD83D\uDCC5" to HubAccent
}

fun weatherEmoji(condition: String): String = when (condition.lowercase()) {
    "sunny", "clear" -> "\u2600\uFE0F"
    "partly cloudy" -> "\u26C5"
    "cloudy" -> "\u2601\uFE0F"
    "rain", "rainy" -> "\uD83C\uDF27\uFE0F"
    "storm" -> "\u26C8\uFE0F"
    "snow" -> "\u2744\uFE0F"
    else -> "\u26C5"
}
