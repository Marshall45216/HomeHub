package com.familycal.tv.data

import com.familycal.tv.model.RecurrenceEnd
import com.familycal.tv.model.RecurrenceRule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

// Safety caps so a mistaken "never ends" recurrence can't generate an
// unbounded or absurdly large number of events -- mirrors the sandbox's
// RECURRENCE_HORIZON_DAYS / RECURRENCE_MAX_EVENTS.
private const val RECURRENCE_HORIZON_DAYS = 730L // 2 years
private const val RECURRENCE_MAX_EVENTS = 250

/**
 * Expands a recurrence rule into concrete occurrence dates, starting from
 * [start] (inclusive). Used both for a brand-new recurring event and for
 * "turn this existing single event into a series" when editing.
 */
fun generateOccurrenceDates(
    start: LocalDate,
    rule: RecurrenceRule,
    end: RecurrenceEnd
): List<LocalDate> {
    val horizon = if (!end.neverEnds && end.endDate != null) end.endDate
        else start.plusDays(RECURRENCE_HORIZON_DAYS)

    val dates = mutableListOf<LocalDate>()

    when (rule) {
        is RecurrenceRule.None -> dates.add(start)

        is RecurrenceRule.Weekdays -> {
            var cursor = start
            while (!cursor.isAfter(horizon) && dates.size < RECURRENCE_MAX_EVENTS) {
                if (cursor.dayOfWeek.value in rule.daysOfWeek) dates.add(cursor)
                cursor = cursor.plusDays(1)
            }
        }

        is RecurrenceRule.Biweekly -> {
            var cursor = start
            while (!cursor.isAfter(horizon) && dates.size < RECURRENCE_MAX_EVENTS) {
                dates.add(cursor)
                cursor = cursor.plusDays(14)
            }
        }

        is RecurrenceRule.MonthlyByDate -> {
            var monthCursor = start.withDayOfMonth(1)
            while (dates.size < RECURRENCE_MAX_EVENTS) {
                val lastDay = monthCursor.lengthOfMonth()
                val day = rule.dayOfMonth.coerceAtMost(lastDay)
                val candidate = monthCursor.withDayOfMonth(day)
                if (candidate.isAfter(horizon)) break
                if (!candidate.isBefore(start)) dates.add(candidate)
                monthCursor = monthCursor.plusMonths(1)
            }
        }

        is RecurrenceRule.MonthlyByWeekday -> {
            var monthCursor = start.withDayOfMonth(1)
            while (dates.size < RECURRENCE_MAX_EVENTS) {
                val candidate = nthWeekdayOfMonth(monthCursor.year, monthCursor.monthValue, rule.nth, rule.dayOfWeek)
                if (candidate != null) {
                    if (candidate.isAfter(horizon)) break
                    if (!candidate.isBefore(start)) dates.add(candidate)
                }
                monthCursor = monthCursor.plusMonths(1)
                if (monthCursor.year > horizon.year + 1) break
            }
        }
    }

    return dates
}

/**
 * The [nth] occurrence of [dayOfWeekIso] (1=Mon..7=Sun) in the given month,
 * or the last occurrence when [nth] is -1. Returns null if there's no such
 * occurrence (e.g. asking for a 5th Monday that doesn't exist).
 */
fun nthWeekdayOfMonth(year: Int, month: Int, nth: Int, dayOfWeekIso: Int): LocalDate? {
    val dow = DayOfWeek.of(dayOfWeekIso)
    val firstOfMonth = LocalDate.of(year, month, 1)
    return if (nth == -1) {
        firstOfMonth.with(TemporalAdjusters.lastInMonth(dow))
    } else {
        val first = firstOfMonth.with(TemporalAdjusters.firstInMonth(dow))
        val candidate = first.plusWeeks((nth - 1).toLong())
        if (candidate.monthValue == month) candidate else null
    }
}

/** Generates one date per year for [years] years, starting with the current year's occurrence. */
fun generateYearlyBirthdayDates(month: Int, day: Int, startYear: Int, years: Int): List<LocalDate> {
    return (0 until years).map { i ->
        val year = startYear + i
        val lastDay = LocalDate.of(year, month, 1).lengthOfMonth()
        LocalDate.of(year, month, day.coerceAtMost(lastDay))
    }
}
