package com.familycal.tv.model

import androidx.compose.ui.graphics.Color
import java.time.LocalDate

/**
 * One person in the family. Avatar can come from three places, checked in
 * this priority order in the UI: an uploaded photo, a chosen DiceBear
 * gallery image, or (fallback) their initial in a colored circle.
 */
data class FamilyMember(
    val displayName: String,
    val email: String,
    val color: Color,
    val birthday: LocalDate? = null,
    val avatarImageUrl: String? = null,
    val photoUri: String? = null,
    val goals: List<Goal> = emptyList(),
    val wishlist: List<String> = emptyList()
)

data class Goal(val text: String, val done: Boolean = false)

/** Broad category used to pick an icon + accent color for an event's chip. */
enum class EventCategory {
    SPORTS, MEAL, READING, BIRTHDAY, TRAVEL, SCHOOL, GENERAL
}

/** How an event repeats. Mirrors the options in the Add/Edit Event screen. */
sealed class RecurrenceRule {
    object None : RecurrenceRule()
    data class Weekdays(val daysOfWeek: Set<Int>) : RecurrenceRule() // 1=Mon..7=Sun (java.time convention)
    object Biweekly : RecurrenceRule()
    data class MonthlyByDate(val dayOfMonth: Int) : RecurrenceRule()
    data class MonthlyByWeekday(val nth: Int, val dayOfWeek: Int) : RecurrenceRule() // nth: 1-4, or -1 for "last"
}

data class RecurrenceEnd(
    val neverEnds: Boolean,
    val endDate: LocalDate? = null
)

/**
 * A single calendar event -- one row per occurrence, even for recurring
 * events. Occurrences that belong to the same series share a seriesId, so
 * "edit/delete this one vs. the whole series" can find its siblings.
 */
data class FamilyEvent(
    val id: Long,
    val title: String,
    val date: LocalDate,
    val isAllDay: Boolean,
    val time: String? = null, // e.g. "5:30 PM" -- null/ignored when isAllDay
    val location: String = "",
    val notes: String = "",
    val people: List<String> = emptyList(), // FamilyMember.displayName values
    val category: EventCategory = EventCategory.GENERAL,
    val seriesId: String? = null,
    val isBirthdaySeriesFor: String? = null // set on auto-generated birthday events
)

/** One pinned countdown card. Birthday-linked countdowns recompute their
 *  target date live instead of storing a fixed one -- see [effectiveDate]. */
data class CountdownItem(
    val label: String,
    val date: LocalDate,
    val category: EventCategory = EventCategory.GENERAL,
    val isBirthdayFor: String? = null,
    val birthdayMonthDay: Pair<Int, Int>? = null // (month, day), used when isBirthdayFor != null
) {
    fun effectiveDate(today: LocalDate): LocalDate {
        if (birthdayMonthDay == null) return date
        val (month, day) = birthdayMonthDay
        var candidate = LocalDate.of(today.year, month, day.coerceAtMost(LocalDate.of(today.year, month, 1).lengthOfMonth()))
        if (candidate.isBefore(today)) {
            candidate = LocalDate.of(today.year + 1, month, day.coerceAtMost(LocalDate.of(today.year + 1, month, 1).lengthOfMonth()))
        }
        return candidate
    }
}

/** One day's dinner-plan entry -- keyed by real calendar date, not a generic weekday. */
data class MealPlanEntry(val name: String, val emoji: String)

/** Live weather for a given day, used in the weather strip. */
data class DayForecast(
    val date: LocalDate,
    val dayLabel: String,
    val highF: Int,
    val lowF: Int,
    val condition: String
)

/** One rotating card shown in the idle/ambient screensaver mode. */
data class NextHoursCard(
    val title: String,
    val subtitle: String,
    val timeLabel: String,
    val category: EventCategory
)

/** One line on the generated grocery list. */
data class GroceryItem(
    val name: String,
    val category: String, // "Produce" | "Meat & Seafood" | "Dairy" | "Pantry" | "Ready-made"
    val checked: Boolean = false
)
