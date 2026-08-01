package com.familycal.tv.ui

/**
 * Every screen the app can navigate to. Mirrors state.view in the HTML
 * sandbox. Screens marked "not yet built" render a placeholder for now --
 * each arrives in a follow-up round, same order we built them in the sandbox:
 * event add/edit, meal planner, grocery list, then profile editing.
 */
sealed class Screen {
    object Dashboard : Screen()
    data class EventDetail(val eventId: Long) : Screen()
    data class PersonProfile(val personName: String) : Screen()
    data class CountdownDetail(val index: Int) : Screen()
    object MealsWeek : Screen()
    object MonthlyMenu : Screen()
    object EventsWeek : Screen()
    object EventsMonth : Screen()
    object GroceryList : Screen()
    data class AddEditEvent(val editingEventId: Long? = null) : Screen()
    data class AddEditCountdown(val editingIndex: Int? = null) : Screen()
    data class EditProfile(val editingName: String? = null) : Screen() // null = adding new
}
