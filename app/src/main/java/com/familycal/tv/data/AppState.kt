package com.familycal.tv.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
import com.familycal.tv.model.*
import java.time.LocalDate

val FamilyColorCycle = listOf(
    Color(0xFFE85D9C), Color(0xFF4FC3F7), Color(0xFFB07CF0),
    Color(0xFFFFA552), Color(0xFF57D6A5), Color(0xFFF7C948)
)

private val DefaultRotation = listOf(
    "Taco Night", "Pasta Primavera", "Grilled Chicken", "Sheet Pan Salmon",
    "Pizza Night", "Family BBQ", "Breakfast for Dinner"
)

/**
 * Holds everything the dashboard (and, in later rounds, every other screen)
 * reads and mutates. This is intentionally a plain observable holder rather
 * than a full ViewModel/repository split for now -- matches the sandbox's
 * single `state` object so porting behavior over stays a direct translation.
 * Swapping this for Firebase-backed storage is the planned next big step.
 */
class AppState {
    val family = mutableStateListOf<FamilyMember>()
    val events = mutableStateListOf<FamilyEvent>()
    val mealPlan = mutableStateMapOf<LocalDate, MealPlanEntry>()
    val countdowns = mutableStateListOf<CountdownItem>()
    var nextEventId = 100L

    init {
        val today = LocalDate.now()
        family.addAll(
            listOf(
                FamilyMember("Emma", "emma@example.com", FamilyColorCycle[0],
                    goals = listOf(Goal("Finish reading log for school")),
                    wishlist = listOf("Nintendo Switch game", "New soccer cleats")),
                FamilyMember("Jack", "jack@example.com", FamilyColorCycle[1],
                    wishlist = listOf("Lego set")),
                FamilyMember("Beth", "beth@example.com", FamilyColorCycle[2],
                    goals = listOf(Goal("Sign up for the 5k", done = true))),
                FamilyMember("Austin", "austin@example.com", FamilyColorCycle[3]),
                FamilyMember("Liam", "liam@example.com", FamilyColorCycle[4])
            )
        )
        events.addAll(
            listOf(
                FamilyEvent(1, "Soccer Practice", today, false, "5:30 PM", "Riverside Park \u00B7 Field 3", people = listOf("Emma"), category = EventCategory.SPORTS),
                FamilyEvent(2, "Family Dinner", today, false, "7:00 PM", "At Home", people = family.map { it.displayName }, category = EventCategory.MEAL),
                FamilyEvent(3, "Reading Time", today, false, "8:30 PM", notes = "All together in the living room", people = listOf("Jack"), category = EventCategory.READING),
                FamilyEvent(4, "Dentist Appointment", today.plusDays(3), false, "2:00 PM", "Bradenton Dental", people = listOf("Jack")),
                FamilyEvent(5, "Beth's Book Club", today.plusDays(9), false, "7:00 PM", "Neighbor's house", people = listOf("Beth"))
            )
        )
        countdowns.addAll(
            listOf(
                CountdownItem("Birthday", today.plusDays(6), EventCategory.BIRTHDAY),
                CountdownItem("Beach Trip", today.plusDays(18), EventCategory.TRAVEL),
                CountdownItem("First Day of School", LocalDate.of(today.year, 8, 17), EventCategory.SCHOOL)
            )
        )
    }

    fun mealFor(date: LocalDate): MealPlanEntry {
        mealPlan[date]?.let { return it }
        val name = DefaultRotation[date.dayOfWeek.value % DefaultRotation.size]
        return MealPlanEntry(name, categoryEmojiForMeal(name))
    }

    fun eventsOn(date: LocalDate): List<FamilyEvent> = events.filter { it.date == date }

    fun eventsInRange(start: LocalDate, end: LocalDate): List<FamilyEvent> =
        events.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
            .sortedWith(compareBy({ it.date }, { it.time ?: "" }))

    fun personEventsOn(personName: String, date: LocalDate): List<FamilyEvent> =
        eventsOn(date).filter { personName in it.people }

    fun deleteEvent(id: Long) { events.removeAll { it.id == id } }

    fun deleteSeries(seriesId: String) { events.removeAll { it.seriesId == seriesId } }

    /** Adds one or more occurrences (for a repeating event, one per generated date). */
    fun addEvent(
        title: String, date: LocalDate, isAllDay: Boolean, time: String?, location: String,
        notes: String, people: List<String>, category: EventCategory,
        occurrenceDates: List<LocalDate> = listOf(date), seriesId: String? = null
    ) {
        occurrenceDates.forEach { d ->
            nextEventId++
            events.add(
                FamilyEvent(
                    id = nextEventId, title = title, date = d, isAllDay = isAllDay, time = time,
                    location = location, notes = notes, people = people, category = category, seriesId = seriesId
                )
            )
        }
    }

    /** Updates a single event's content in place (date can change; only used when it's not part of a series). */
    fun updateSingleEvent(id: Long, title: String, date: LocalDate, isAllDay: Boolean, time: String?, location: String, notes: String, people: List<String>, category: EventCategory) {
        val idx = events.indexOfFirst { it.id == id }
        if (idx >= 0) events[idx] = events[idx].copy(title = title, date = date, isAllDay = isAllDay, time = time, location = location, notes = notes, people = people, category = category)
    }

    /** Updates content across every event sharing [seriesId] -- date is intentionally left alone since each occurrence keeps its own. */
    fun updateSeriesContent(seriesId: String, title: String, isAllDay: Boolean, time: String?, location: String, notes: String, people: List<String>, category: EventCategory) {
        events.replaceAll { ev ->
            if (ev.seriesId == seriesId) ev.copy(title = title, isAllDay = isAllDay, time = time, location = location, notes = notes, people = people, category = category)
            else ev
        }
    }

    fun addCountdown(item: CountdownItem) { countdowns.add(item) }
    fun updateCountdown(index: Int, item: CountdownItem) { if (index in countdowns.indices) countdowns[index] = item }
    fun deleteCountdown(index: Int) { if (index in countdowns.indices) countdowns.removeAt(index) }

    fun addFamilyMember(member: FamilyMember) {
        family.add(member)
        syncBirthday(member)
    }

    fun updateFamilyMember(oldName: String, updated: FamilyMember) {
        val idx = family.indexOfFirst { it.displayName == oldName }
        if (idx >= 0) family[idx] = updated
        syncBirthday(updated, previousName = oldName.takeIf { it != updated.displayName })
    }

    fun removeFamilyMember(name: String) {
        family.removeAll { it.displayName == name }
        events.replaceAll { ev -> ev.copy(people = ev.people.filter { it != name }) }
        events.removeAll { it.isBirthdaySeriesFor == name }
        countdowns.removeAll { it.isBirthdayFor == name }
    }

    /** Regenerates the recurring birthday event series + auto-restarting
     *  countdown for [member], clearing out any previous ones first. */
    private fun syncBirthday(member: FamilyMember, previousName: String? = null) {
        val namesToClear = listOfNotNull(member.displayName, previousName).distinct()
        events.removeAll { it.isBirthdaySeriesFor in namesToClear }
        countdowns.removeAll { it.isBirthdayFor in namesToClear }

        val birthday = member.birthday ?: return
        val seriesId = "birthday-${member.displayName}-${System.currentTimeMillis()}"
        val dates = generateYearlyBirthdayDates(birthday.monthValue, birthday.dayOfMonth, LocalDate.now().year, years = 5)
        dates.forEach { date ->
            nextEventId++
            events.add(
                FamilyEvent(
                    id = nextEventId, title = "${member.displayName}'s Birthday", date = date, isAllDay = true,
                    people = listOf(member.displayName), category = EventCategory.BIRTHDAY,
                    seriesId = seriesId, isBirthdaySeriesFor = member.displayName
                )
            )
        }
        countdowns.add(
            CountdownItem(
                label = "${member.displayName}'s Birthday", date = birthday, category = EventCategory.BIRTHDAY,
                isBirthdayFor = member.displayName, birthdayMonthDay = birthday.monthValue to birthday.dayOfMonth
            )
        )
    }
}

private fun categoryEmojiForMeal(name: String): String {
    val n = name.lowercase()
    return when {
        "costco" in n -> "\uD83D\uDECD\uFE0F"
        "chicken" in n -> "\uD83C\uDF57"
        "beef" in n || "steak" in n || "meatball" in n -> "\uD83E\uDD69"
        "pork" in n || "ribs" in n -> "\uD83E\uDD53"
        "salmon" in n || "cod" in n || "tilapia" in n || "fish" in n -> "\uD83D\uDC1F"
        "shrimp" in n -> "\uD83E\uDD90"
        "taco" in n || "burrito" in n || "fajita" in n -> "\uD83C\uDF2E"
        "pasta" in n || "spaghetti" in n || "alfredo" in n -> "\uD83C\uDF5D"
        "pizza" in n -> "\uD83C\uDF55"
        "soup" in n || "chili" in n || "stew" in n -> "\uD83C\uDF72"
        "salad" in n -> "\uD83E\uDD57"
        else -> "\uD83C\uDF7D"
    }
}
