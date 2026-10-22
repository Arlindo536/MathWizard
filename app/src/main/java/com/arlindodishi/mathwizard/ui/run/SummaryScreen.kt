package com.arlindodishi.mathwizard.ui.run

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arlindodishi.mathwizard.R
import com.arlindodishi.mathwizard.ui.theme.SpireColors
import com.arlindodishi.mathwizard.ui.theme.Spacing
import kotlin.math.roundToInt

@Composable
fun SummaryScreen(ui: RunUiState, onAgain: () -> Unit, onHome: () -> Unit) {
    val stats = ui.run.stats
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SpireColors.NightRaised, SpireColors.Night)))
            .safeDrawingPadding()
            .padding(Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.summary_title),
            style = MaterialTheme.typography.headlineMedium,
            color = SpireColors.TextMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.l))
        Text(stringResource(R.string.summary_floor), style = MaterialTheme.typography.titleMedium, color = SpireColors.TextMuted)
        Text(
            ui.run.floor.toString(),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize * 2),
            color = SpireColors.Gold,
        )
        Text(
            if (ui.newRecord) stringResource(R.string.summary_new_record) else stringResource(R.string.summary_best, ui.bestFloor),
            style = MaterialTheme.typography.titleLarge,
            color = if (ui.newRecord) SpireColors.Gold else SpireColors.TextMuted,
        )
        Spacer(Modifier.height(Spacing.xl))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SpireColors.Surface)
                .padding(Spacing.m),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Stat(stringResource(R.string.summary_spells), stats.correct.toString())
            Stat(stringResource(R.string.summary_accuracy), "${(stats.accuracy * 100).roundToInt()}%")
            Stat(stringResource(R.string.summary_best_streak), stats.bestStreak.toString())
        }
        Spacer(Modifier.height(Spacing.xl))
        Button(onClick = onAgain, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(stringResource(R.string.summary_again), style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(Spacing.s))
        OutlinedButton(onClick = onHome, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(stringResource(R.string.summary_home), style = MaterialTheme.typography.titleLarge, color = SpireColors.Text)
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = SpireColors.Text)
        Text(label, style = MaterialTheme.typography.labelMedium, color = SpireColors.TextMuted, textAlign = TextAlign.Center)
    }
}
