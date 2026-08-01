package com.familycal.tv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.material3.*
import com.familycal.tv.model.*
import com.familycal.tv.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main dashboard screen, laid out to match the HomeHub reference design:
 *   [ logo/title ......................... weekly menu strip ]
 *   [ today hero panel (wide) | family avatars + countdowns  ]
 *   [ 7-day weather strip, full width                        ]
 *   [ voice quick-add bar, full width                        ]
 */
@Composable
fun CalendarScreen(
    familyMembers: List<FamilyMember>,
    events: List<FamilyEvent>,
    countdowns: List<CountdownItem>,
    weeklyMenu: List<MenuDay>,
    forecast: List<DayForecast>,
    activeFilter: FamilyMember?,
    voiceTranscript: String?,
    isListening: Boolean,
    onFilterSelected: (FamilyMember?) -> Unit,
    onMicPressed: () -> Unit
) {
    val visibleEvents = remember(events, activeFilter) {
        if (activeFilter == null) events else events.filter { it.owner == activeFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HubBackground)
            .padding(horizontal = 40.dp, vertical = 28.dp)
    ) {
        TopBar(weeklyMenu = weeklyMenu)

        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.weight(1f, fill = false)) {
            Box(modifier = Modifier.weight(0.62f)) {
                TodayHeroPanel(events = visibleEvents.filter { it.isToday() })
            }
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(0.38f)) {
                FamilyFilterRow(
                    members = familyMembers,
                    active = activeFilter,
                    onSelect = onFilterSelected
                )
                Spacer(Modifier.height(16.dp))
                CountdownGrid(countdowns)
            }
        }

        Spacer(Modifier.height(20.dp))
        WeatherWeekStrip(forecast)

        Spacer(Modifier.height(20.dp))
        VoiceQuickAddBar(
            transcript = voiceTranscript,
            isListening = isListening,
            onMicPressed = onMicPressed
        )
    }
}

// --- Top bar: logo + weekly menu -------------------------------------------

@Composable
private fun TopBar(weeklyMenu: List<MenuDay>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(HubAccent),
                contentAlignment = Alignment.Center
            ) {
                Text("\uD83C\uDFE0", fontSize = androidx.compose.ui.unit.TextUnit.Unspecified)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("HomeHub", style = FamilyCalTypography.titleLarge)
                Text("Smart Family Planner", style = FamilyCalTypography.bodyMedium)
            }
        }

        if (weeklyMenu.isNotEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "WEEKLY MENU",
                    style = FamilyCalTypography.bodyMedium,
                    color = HubTextSecondary
                )
                Spacer(Modifier.height(6.dp))
                TvLazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    items(weeklyMenu.size) { i ->
                        val m = weeklyMenu[i]
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(m.dayLabel, style = FamilyCalTypography.bodyMedium)
                            Text("${m.emoji}  ${m.mealName}", style = FamilyCalTypography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

// --- Today hero panel --------------------------------------------------

@Composable
private fun TodayHeroPanel(events: List<FamilyEvent>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(HeroGradient)
            .padding(28.dp)
    ) {
        Text(
            SimpleDateFormat("EEEE", Locale.US).format(Date()).uppercase(),
            style = FamilyCalTypography.bodyMedium,
            color = Color(0xFFCBD3FF)
        )
        Text(
            SimpleDateFormat("MMMM d", Locale.US).format(Date()),
            style = FamilyCalTypography.displayLarge
        )
        Text("Today", style = FamilyCalTypography.headlineLarge, color = Color(0xFFB9C2FF))

        Spacer(Modifier.height(20.dp))

        if (events.isEmpty()) {
            Text("Nothing on the calendar today", color = Color(0xFFCBD3FF))
        } else {
            TvLazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(events.size) { i -> HeroEventRow(events[i]) }
            }
        }
    }
}

@Composable
private fun HeroEventRow(event: FamilyEvent) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CategoryIcon(event.category)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = if (event.isAllDay) "All day" else timeFormatter().format(Date(event.startEpochMillis)),
                style = FamilyCalTypography.bodyMedium,
                color = Color(0xFFCBD3FF)
            )
            Text(event.subject, style = FamilyCalTypography.titleLarge)
            event.location?.let {
                Text(it, style = FamilyCalTypography.bodyMedium, color = Color(0xFFCBD3FF))
            }
        }
    }
}

@Composable
private fun CategoryIcon(category: EventCategory) {
    val (emoji, color) = categoryVisual(category)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji)
    }
}

private fun categoryVisual(category: EventCategory): Pair<String, Color> = when (category) {
    EventCategory.SPORTS -> "\u26BD" to EventColorSports
    EventCategory.MEAL -> "\uD83C\uDF7D" to EventColorMeal
    EventCategory.READING -> "\uD83D\uDCD6" to EventColorReading
    EventCategory.BIRTHDAY -> "\uD83C\uDF82" to EventColorBirthday
    EventCategory.TRAVEL -> "\uD83C\uDFD6" to EventColorTravel
    EventCategory.SCHOOL -> "\uD83C\uDF92" to EventColorSchool
    EventCategory.GENERAL -> "\uD83D\uDCC5" to HubAccent
}

// --- Family avatar filter row --------------------------------------------

@Composable
private fun FamilyFilterRow(
    members: List<FamilyMember>,
    active: FamilyMember?,
    onSelect: (FamilyMember?) -> Unit
) {
    TvLazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(members.size) { i ->
            val m = members[i]
            AvatarCircle(
                label = m.displayName,
                color = m.color,
                photoUrl = m.photoUrl,
                selected = active == m,
                onClick = { onSelect(if (active == m) null else m) }
            )
        }
        item {
            AvatarCircle(
                label = "All",
                color = HubTextSecondary,
                photoUrl = null,
                selected = active == null,
                onClick = { onSelect(null) }
            )
        }
    }
}

@Composable
private fun AvatarCircle(
    label: String,
    color: Color,
    photoUrl: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(HubSurfaceRaised)
                .border(width = if (selected) 3.dp else 2.dp, color = color, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // photoUrl support left as a hook -- wire up Coil's AsyncImage here
            // once you have real family photo URLs; falls back to initial.
            Text(label.take(1), style = FamilyCalTypography.titleLarge)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = FamilyCalTypography.bodyMedium)
    }
}

// --- Countdown chips (grid, matches the 3-card mockup layout) -----------

@Composable
private fun CountdownGrid(countdowns: List<CountdownItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        countdowns.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { item -> CountdownCard(item, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CountdownCard(item: CountdownItem, modifier: Modifier = Modifier) {
    val (emoji, color) = categoryVisual(item.category)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.22f))
            .padding(16.dp)
    ) {
        Text("$emoji  ${item.label}", style = FamilyCalTypography.bodyMedium)
        Text("${item.daysAway}", style = FamilyCalTypography.displayLarge, color = color)
        Text("days", style = FamilyCalTypography.bodyMedium)
        Text(item.dateLabel, style = FamilyCalTypography.bodyMedium)
    }
}

// --- Weather week strip ---------------------------------------------------

@Composable
private fun WeatherWeekStrip(forecast: List<DayForecast>) {
    TvLazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(forecast.size) { i ->
            val f = forecast[i]
            Column(
                modifier = Modifier
                    .width(150.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(HubSurfaceRaised)
                    .padding(14.dp)
            ) {
                Text(f.dayLabel, style = FamilyCalTypography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(f.dateLabel, style = FamilyCalTypography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(weatherEmoji(f.condition), style = FamilyCalTypography.headlineLarge)
                Spacer(Modifier.height(8.dp))
                Row {
                    Text("${f.highF}°", style = FamilyCalTypography.bodyLarge)
                    Spacer(Modifier.width(8.dp))
                    Text("${f.lowF}°", style = FamilyCalTypography.bodyMedium)
                }
            }
        }
    }
}

private fun weatherEmoji(condition: String): String = when (condition.lowercase()) {
    "sunny", "clear" -> "\u2600\uFE0F"
    "partly cloudy" -> "\u26C5"
    "cloudy" -> "\u2601\uFE0F"
    "rain", "rainy" -> "\uD83C\uDF27\uFE0F"
    "storm" -> "\u26C8\uFE0F"
    "snow" -> "\u2744\uFE0F"
    else -> "\u26C5"
}

// --- Voice quick-add bar -----------------------------------------------

@Composable
private fun VoiceQuickAddBar(
    transcript: String?,
    isListening: Boolean,
    onMicPressed: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(HubSurface)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isListening) EventColorMeal else HubAccent)
                .then(Modifier),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onMicPressed) {
                Text("\uD83C\uDF99\uFE0F")
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = transcript ?: "Hold the mic button and say \u201cAdd soccer practice Saturday 10am for Emma\u201d",
            style = FamilyCalTypography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            "Press mic on remote to speak",
            style = FamilyCalTypography.bodyMedium
        )
    }
}

// --- date/format helpers ------------------------------------------------

private fun timeFormatter() = SimpleDateFormat("h:mm a", Locale.US)

private fun isSameDay(a: Date, b: Date): Boolean {
    val ca = Calendar.getInstance().apply { time = a }
    val cb = Calendar.getInstance().apply { time = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

private fun FamilyEvent.isToday() = isSameDay(Date(startEpochMillis), Date())
