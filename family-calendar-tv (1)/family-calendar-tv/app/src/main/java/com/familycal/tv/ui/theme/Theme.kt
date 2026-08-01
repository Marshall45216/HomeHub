package com.familycal.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme

// "HomeHub" dark theme: deep navy/indigo base with a subtle purple-blue
// gradient behind the hero panel, and saturated (not pastel) accent colors
// per family member so avatar rings and event dots read clearly on dark.

val HubBackground = Color(0xFF0E1526)        // near-black navy, app background
val HubSurface = Color(0xFF161F35)           // card surface
val HubSurfaceRaised = Color(0xFF1D2841)     // weekly menu / week strip cards
val HubTextPrimary = Color(0xFFF4F6FB)
val HubTextSecondary = Color(0xFF9AA5C0)
val HubDivider = Color(0xFF2A3552)
val HubAccent = Color(0xFF6C7CF7)            // primary CTA / mic accent (indigo-violet)

val HeroGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF2B3E8C), Color(0xFF5B3A99))
)

// Saturated per-person palette, ordered for assignment as members are added.
val FamilyVividPalette = listOf(
    Color(0xFFE85D9C), // pink
    Color(0xFF4FC3F7), // sky blue
    Color(0xFFB07CF0), // violet
    Color(0xFFFFA552), // orange
    Color(0xFF57D6A5), // mint
    Color(0xFFF7C948)  // gold
)

// Event-type accent colors (icon circle backgrounds), independent of person color
val EventColorSports = Color(0xFF6C7CF7)
val EventColorMeal = Color(0xFF57D6A5)
val EventColorReading = Color(0xFFFFA552)
val EventColorBirthday = Color(0xFFE85D9C)
val EventColorTravel = Color(0xFF4FC3F7)
val EventColorSchool = Color(0xFFF7C948)

val FamilyCalTypography = Typography(
    displayLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Bold, fontSize = 60.sp, color = HubTextPrimary
    ),
    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.SemiBold, fontSize = 32.sp, color = HubTextPrimary
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.SemiBold, fontSize = 26.sp, color = HubTextPrimary
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Medium, fontSize = 22.sp, color = HubTextPrimary
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Normal, fontSize = 17.sp, color = HubTextSecondary
    )
)

@Composable
fun FamilyCalendarTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        background = HubBackground,
        surface = HubSurface,
        surfaceVariant = HubSurfaceRaised,
        onBackground = HubTextPrimary,
        onSurface = HubTextPrimary,
        primary = HubAccent,
        onPrimary = HubTextPrimary
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = FamilyCalTypography,
        content = content
    )
}
