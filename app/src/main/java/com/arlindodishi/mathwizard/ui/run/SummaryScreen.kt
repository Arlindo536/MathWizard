package com.arlindodishi.mathwizard.ui.run

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arlindodishi.mathwizard.R
import com.arlindodishi.mathwizard.ui.pixel.IconValue
import com.arlindodishi.mathwizard.ui.pixel.PixelButton
import com.arlindodishi.mathwizard.ui.pixel.SpireBackdrop
import com.arlindodishi.mathwizard.ui.pixel.Sprites
import com.arlindodishi.mathwizard.ui.pixel.pixelPanel
import com.arlindodishi.mathwizard.ui.theme.SpireColors
import com.arlindodishi.mathwizard.ui.theme.Spacing
import com.arlindodishi.mathwizard.ui.theme.accent
import kotlin.math.roundToInt

/** End of a climb. [onAgain] is null for the Daily Rift (one attempt per day). */
@Composable
fun SummaryScreen(ui: RunUiState, onAgain: (() -> Unit)?, onHome: () -> Unit) {
    val stats = ui.run.stats
    val accent = ui.run.problem.school.accent()
    Box(Modifier.fillMaxSize().background(SpireColors.Night)) {
        SpireBackdrop(climb = ui.run.floor.toFloat(), torch = accent, modifier = Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 40.dp, vertical = Spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (ui.daily) {
                Text(stringResource(R.string.home_daily).uppercase(), style = MaterialTheme.typography.titleLarge, color = accent)
            }
            Text(
                stringResource(R.string.summary_title).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = SpireColors.TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.l))
            Text(stringResource(R.string.summary_floor).uppercase(), style = MaterialTheme.typography.titleMedium, color = SpireColors.TextMuted)
            Text(
                ui.run.floor.toString(),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize * 2),
                color = SpireColors.Gold,
            )
            val reward = ui.reward
            Text(
                when {
                    reward == null -> ""
                    ui.daily -> stringResource(R.string.daily_come_back)
                    reward.newRecord -> stringResource(R.string.summary_new_record).uppercase()
                    else -> stringResource(R.string.summary_best, reward.bestFloor)
                },
                style = MaterialTheme.typography.titleLarge,
                color = if (reward?.newRecord == true) SpireColors.Gold else SpireColors.TextMuted,
            )
            Spacer(Modifier.height(Spacing.s))
            if (reward != null) {
                IconValue(
                    Sprites.crystal,
                    pluralStringResource(R.plurals.summary_crystals, reward.crystals, reward.crystals),
                    SpireColors.Crystal,
                    iconSize = 24.dp,
                )
            }
            Spacer(Modifier.height(Spacing.l))
            Row(
                Modifier
                    .fillMaxWidth()
                    .pixelPanel(accent)
                    .padding(vertical = Spacing.s),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Stat(stringResource(R.string.summary_spells), stats.correct.toString())
                Stat(stringResource(R.string.summary_accuracy), "${(stats.accuracy * 100).roundToInt()}%")
                Stat(stringResource(R.string.summary_best_streak), stats.bestStreak.toString())
                Stat(stringResource(R.string.summary_bosses), stats.bossesDefeated.toString())
            }
            Spacer(Modifier.height(Spacing.l))
            if (onAgain != null) {
                PixelButton(onClick = onAgain, color = accent, modifier = Modifier.fillMaxWidth().height(60.dp)) {
                    Text(stringResource(R.string.summary_again).uppercase(), style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(Spacing.s))
            }
            PixelButton(onClick = onHome, color = SpireColors.SurfaceRaised, contentColor = SpireColors.Text, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.summary_home).uppercase(), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 2.dp)) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = SpireColors.Text)
        Text(label, style = MaterialTheme.typography.labelMedium, color = SpireColors.TextMuted, textAlign = TextAlign.Center)
    }
}
