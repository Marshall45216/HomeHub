package com.familycal.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.familycal.tv.data.AppState
import com.familycal.tv.model.*
import com.familycal.tv.ui.theme.*
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

enum class HeroRange { TODAY, WEEK, MONTH }

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
            .padding(horizontal = 40.dp, vertical = 28.dp)
    ) {
        TopBar(appState = appState, today = today, onNavigate = onNavigate)
        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.weight(1f, fill = false)) {
            Box(modifier = Modifier.weight(0.62f)) {
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
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(0.38f)) {
                FamilyRow(appState = appState, onNavigate = onNavigate)
                Spacer(Modifier.height(16.dp))
                CountdownGrid(appState = appState, today = today, onNavigate = onNavigate)
            }
        }

        Spacer(Modifier.height(20.dp))
        WeatherWeekStrip(forecast)

        Spacer(Modifier.height(20.dp))
        VoiceQuickAddBar(onMicPressed = { onNavigate(Screen.AddEditEvent()) })
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
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(HubAccent),
                contentAlignment = Alignment.Center
            ) { Text("H", style = FamilyCalTypography.titleLarge) }
            Spacer(Modifier.width(12.dp))
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
            Spacer(Modifier.height(4.dp))
            Text(caption, style = FamilyCalTypography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items((0..6).toList()) { i ->
                    val date = weekStart.plusDays(i.toLong())
                    val meal = appState.mealFor(date)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onNavigate(Screen.MealsWeek) }) {
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
            .clip(RoundedCornerShape(20.dp))
            .border(width = 1.dp, color = HubDivider, shape = RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) { Text(label, style = FamilyCalTypography.bodyMedium) }
}

@Composable
private fun TodayHeroPanel(
    appState: AppState, today: LocalDate, range: HeroRange,
    onRangeSelected: (HeroRange) -> Unit, onAddEvent: () -> Unit, onEventClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(HeroGradient).padding(28.dp)
    ) {
        Text(today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US).uppercase(), style = FamilyCalTypography.bodyMedium, color = Color(0xFFCBD3FF))
        Text(today.month.getDisplayName(TextStyle.FULL, Locale.US) + " " + today.dayOfMonth, style = FamilyCalTypography.displayLarge)

        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                when (range) { HeroRange.TODAY -> "Today"; HeroRange.WEEK -> "This Week"; HeroRange.MONTH -> "This Month" },
                style = FamilyCalTypography.headlineLarge, color = Color(0xFFB9C2FF)
            )
            Row {
                RangePill("Today", range == HeroRange.TODAY) { onRangeSelected(HeroRange.TODAY) }
                Spacer(Modifier.width(6.dp))
                RangePill("Week", range == HeroRange.WEEK) { onRangeSelected(HeroRange.WEEK) }
                Spacer(Modifier.width(6.dp))
                RangePill("Month", range == HeroRange.MONTH) { onRangeSelected(HeroRange.MONTH) }
            }
        }

        val todaysEvents = appState.eventsOn(today)
        if (todaysEvents.isEmpty()) {
            Text("Nothing on the calendar today", color = Color(0xFFCBD3FF))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                todaysEvents.forEach { ev -> HeroEventRow(ev, onClick = { onEventClick(ev.id) }) }
            }
        }

        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .clickable(onClick = onAddEvent)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) { Text("+ Add Event", style = FamilyCalTypography.bodyLarge) }
    }
}

@Composable
private fun RangePill(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) Color.White else Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(label, style = FamilyCalTypography.bodyMedium, color = if (active) Color(0xFF2B3E8C) else Color.White)
    }
}

@Composable
private fun HeroEventRow(event: FamilyEvent, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        val (emoji, color) = categoryVisual(event.category)
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) { Text(emoji) }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(if (event.isAllDay) "All day" else event.time ?: "", style = FamilyCalTypography.bodyMedium, color = Color(0xFFCBD3FF))
            Text(event.title, style = FamilyCalTypography.titleLarge)
        }
    }
}

@Composable
private fun FamilyRow(appState: AppState, onNavigate: (Screen) -> Unit) {
    Text("FAMILY", style = FamilyCalTypography.bodyMedium)
    Spacer(Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(appState.family) { person ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onNavigate(Screen.PersonProfile(person.displayName)) }) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(HubSurfaceRaised).border(2.dp, person.color, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text(person.displayName.take(1), style = FamilyCalTypography.titleLarge) }
                Text(person.displayName, style = FamilyCalTypography.bodyMedium)
            }
        }
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onNavigate(Screen.EditProfile()) }) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).border(1.dp, HubDivider, CircleShape),
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        appState.countdowns.chunked(2).forEachIndexed { rowIdx, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { colIdx, item ->
                    val index = rowIdx * 2 + colIdx
                    CountdownCard(item, today, Modifier.weight(1f).clickable { onNavigate(Screen.CountdownDetail(index)) })
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CountdownCard(item: CountdownItem, today: LocalDate, modifier: Modifier = Modifier) {
    val (_, color) = categoryVisual(item.category)
    val effectiveDate = item.effectiveDate(today)
    val days = java.time.temporal.ChronoUnit.DAYS.between(today, effectiveDate).toInt()
    Column(modifier = modifier.clip(RoundedCornerShape(18.dp)).background(color.copy(alpha = 0.22f)).padding(16.dp)) {
        Text(item.label, style = FamilyCalTypography.bodyMedium)
        Text(if (days <= 0) "Today" else "$days", style = FamilyCalTypography.displayLarge, color = color)
        if (days > 0) Text("days", style = FamilyCalTypography.bodyMedium)
    }
}

@Composable
private fun WeatherWeekStrip(forecast: List<DayForecast>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(forecast) { f ->
            Column(modifier = Modifier.width(150.dp).clip(RoundedCornerShape(16.dp)).background(HubSurfaceRaised).padding(14.dp)) {
                Text(f.dayLabel, style = FamilyCalTypography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(weatherEmoji(f.condition), style = FamilyCalTypography.headlineLarge)
                Spacer(Modifier.height(8.dp))
                Row { Text("${f.highF}\u00B0", style = FamilyCalTypography.bodyLarge); Spacer(Modifier.width(8.dp)); Text("${f.lowF}\u00B0", style = FamilyCalTypography.bodyMedium) }
            }
        }
    }
}

@Composable
private fun VoiceQuickAddBar(onMicPressed: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(HubSurface).padding(horizontal = 20.dp, vertical = 16.dp).clickable(onClick = onMicPressed),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(HubAccent), contentAlignment = Alignment.Center) { Text("\uD83C\uDF99\uFE0F") }
        Spacer(Modifier.width(16.dp))
        Text("Hold the mic button and say \u201CAdd soccer practice Saturday 10am for Emma\u201D", style = FamilyCalTypography.bodyLarge, modifier = Modifier.weight(1f))
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
