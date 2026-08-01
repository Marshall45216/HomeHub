package com.familycal.tv.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.familycal.tv.model.NextHoursCard
import com.familycal.tv.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Shown after a period of remote inactivity (wire this up in MainActivity with
 * a simple "last interaction" timestamp + a coroutine that checks it every
 * few seconds). Rotates through upcoming events instead of the full dashboard
 * or going fully dark, so the calendar is still glanceable from across the room.
 */
@Composable
fun IdleAmbientScreen(cards: List<NextHoursCard>, rotateEveryMillis: Long = 6000L) {
    if (cards.isEmpty()) return
    var index by remember { mutableStateOf(0) }

    LaunchedEffect(cards) {
        while (true) {
            delay(rotateEveryMillis)
            index = (index + 1) % cards.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HubBackground)
            .padding(24.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(HubSurface)
                .padding(20.dp)
        ) {
            Text(
                "NEXT 24 HOURS",
                style = FamilyCalTypography.bodyMedium,
                color = HubTextSecondary
            )
            Spacer(Modifier.height(12.dp))

            AnimatedContent(targetState = index, label = "idle-card") { i ->
                NextHoursCardView(cards[i])
            }

            Spacer(Modifier.height(12.dp))
            DotIndicator(count = cards.size, activeIndex = index)
        }
    }
}

@Composable
private fun NextHoursCardView(card: NextHoursCard) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(HubSurfaceRaised)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(HubAccent)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(card.title, style = FamilyCalTypography.titleLarge)
                Text(card.timeLabel, style = FamilyCalTypography.bodyMedium, color = HubTextSecondary)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(card.subtitle, style = FamilyCalTypography.bodyMedium)
    }
}

@Composable
private fun DotIndicator(count: Int, activeIndex: Int) {
    Row {
        repeat(count) { i ->
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(if (i == activeIndex) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (i == activeIndex) HubAccent else HubDivider)
            )
        }
    }
}
