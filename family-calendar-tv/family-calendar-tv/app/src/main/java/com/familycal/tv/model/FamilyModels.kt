package com.familycal.tv.model

import androidx.compose.ui.graphics.Color

/**
 * One person in the family whose Outlook calendar we pull in.
 * photoUrl is optional -- if a family member has no photo set, the UI falls
 * back to a colored initial circle instead (see AvatarCircle in CalendarScreen).
 */
data class FamilyMember(
    val displayName: String,
    val email: String,
    val color: Color,
    val photoUrl: String? = null
)

/** Broad category used to pick an icon + accent color for an event's chip. */
enum class EventCategory {
    SPORTS, MEAL, READING, BIRTHDAY, TRAVEL, SCHOOL, GENERAL
}

/** Normalized event shape, independent of the raw Graph API JSON. */
data class FamilyEvent(
    val id: String,
    val subject: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val isAllDay: Boolean,
    val location: String?,
    val owner: FamilyMember,
    val category: EventCategory = EventCategory.GENERAL
)

/** One pinned "N days" countdown card (birthdays, trips, first day of school, etc). */
data class CountdownItem(
    val label: String,
    val daysAway: Int,
    val dateLabel: String,
    val category: EventCategory
)

/** One day's dinner-plan entry in the top weekly menu strip. */
data class MenuDay(
    val dayLabel: String,
    val mealName: String,
    val emoji: String
)

/** Live weather for "today", used in the week strip's per-day forecast cards. */
data class DayForecast(
    val dayLabel: String,
    val dateLabel: String,
    val highF: Int,
    val lowF: Int,
    val condition: String // maps to a simple emoji/icon in the UI
)

/** One rotating card shown in the idle/ambient screensaver mode. */
data class NextHoursCard(
    val title: String,
    val subtitle: String,
    val timeLabel: String,
    val category: EventCategory
)
