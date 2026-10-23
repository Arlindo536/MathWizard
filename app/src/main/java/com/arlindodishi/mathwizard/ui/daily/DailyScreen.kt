package com.arlindodishi.mathwizard.ui.daily

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arlindodishi.mathwizard.R
import com.arlindodishi.mathwizard.data.DailyResult
import com.arlindodishi.mathwizard.engine.DailyRift
import com.arlindodishi.mathwizard.engine.Streaks
import com.arlindodishi.mathwizard.ui.pixel.PixelButton
import com.arlindodishi.mathwizard.ui.pixel.PixelImage
import com.arlindodishi.mathwizard.ui.pixel.SpireBackdrop
import com.arlindodishi.mathwizard.ui.pixel.Sprites
import com.arlindodishi.mathwizard.ui.pixel.pixelPanel
import com.arlindodishi.mathwizard.ui.schools.schoolUi
import com.arlindodishi.mathwizard.ui.theme.SpireColors
import com.arlindodishi.mathwizard.ui.theme.Spacing
import com.arlindodishi.mathwizard.ui.theme.accent
import com.arlindodishi.mathwizard.ui.upgrades.ScreenHeader
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

@Composable
fun DailyScreen(today: Long, results: List<DailyResult>, onStart: () -> Unit, onBack: () -> Unit) {
    val school = DailyRift.school(today)
    val accent = school.accent()
    val days = results.map { it.epochDay }.toSet()
    val todayResult = results.find { it.epochDay == today }
    Box(Modifier.fillMaxSize().background(SpireColors.Night)) {
        SpireBackdrop(climb = today.toFloat() % 24, torch = accent, modifier = Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            ScreenHeader(title = stringResource(R.string.home_daily), onBack = onBack)
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.l),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.daily_desc), style = MaterialTheme.typography.bodyLarge, color = SpireColors.TextMuted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(Spacing.l))
                Column(
                    Modifier.fillMaxWidth().pixelPanel(accent).padding(Spacing.s),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.daily_today).uppercase(), style = MaterialTheme.typography.labelLarge, color = SpireColors.TextMuted)
                    Spacer(Modifier.height(Spacing.s))
                    PixelImage(Sprites.school(school), Modifier.size(64.dp))
                    Spacer(Modifier.height(Spacing.s))
                    Text(stringResource(schoolUi(school).name).uppercase(), style = MaterialTheme.typography.displayMedium, color = accent)
                    Text(stringResource(schoolUi(school).description), style = MaterialTheme.typography.bodyMedium, color = SpireColors.TextMuted)
                }
                Spacer(Modifier.height(Spacing.m))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.l)) {
                    Text(stringResource(R.string.daily_streak, Streaks.current(days, today)), style = MaterialTheme.typography.titleLarge, color = SpireColors.Gold)
                    Text(stringResource(R.string.daily_best_streak, Streaks.best(days)), style = MaterialTheme.typography.titleLarge, color = SpireColors.TextMuted)
                }
                Spacer(Modifier.height(Spacing.l))
                if (todayResult == null) {
                    PixelButton(onClick = onStart, color = accent, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                        Text(stringResource(R.string.daily_start).uppercase(), style = MaterialTheme.typography.titleLarge)
                    }
                } else {
                    Text(stringResource(R.string.daily_done, todayResult.floor).uppercase(), style = MaterialTheme.typography.headlineMedium, color = SpireColors.Success)
                    Text(stringResource(R.string.daily_come_back), style = MaterialTheme.typography.bodyLarge, color = SpireColors.TextMuted, textAlign = TextAlign.Center)
                }
                if (results.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.xl))
                    Text(stringResource(R.string.daily_history).uppercase(), style = MaterialTheme.typography.titleLarge, color = SpireColors.Gold, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(Spacing.s))
                    results.take(7).forEach { DailyRow(it) }
                }
                Spacer(Modifier.height(Spacing.l))
            }
        }
    }
}

@Composable
private fun DailyRow(result: DailyResult) {
    // An epoch day is midnight UTC, so format it in UTC to get the same calendar date back.
    val date = DateFormat.getDateInstance(DateFormat.MEDIUM).apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(result.epochDay * 86_400_000L))
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelImage(Sprites.school(result.school), Modifier.size(22.dp))
        Spacer(Modifier.width(Spacing.s))
        Text(date, style = MaterialTheme.typography.bodyLarge, color = SpireColors.Text, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.run_floor, result.floor), style = MaterialTheme.typography.titleMedium, color = result.school.accent())
    }
}
